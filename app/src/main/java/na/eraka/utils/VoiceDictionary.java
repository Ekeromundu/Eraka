package na.eraka.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Eraka personal voice dictionary.
 *
 * Turns what Whisper heard into pictogram suggestions for ONE child, and learns
 * from every confirmation. Everything stays on the phone:
 *   files/voice_dictionary_<uid>.json  - learned "sound -> pictogram" links
 *   files/voice_eval_<uid>.csv         - one line per attempt, for the thesis evaluation
 * No audio is ever stored here, only the text Whisper produced.
 *
 * Matching combines two signals:
 *   1. Learned links: this exact (or very similar) transcript was confirmed as a
 *      pictogram before. Grows with use, weighted by count and recency.
 *   2. Label matching: the transcript against the child's pictogram names
 *      (exact word, spelling similarity, first-sound prefix, sounds-alike code).
 * Signal 1 lets the app understand a child's own approximations ("wa" = water)
 * even when Whisper never produces the real word.
 */
public final class VoiceDictionary {

    private static final String TAG = "VoiceDictionary";
    public static final int DEFAULT_LIMIT = 4;

    // Weights (kept here so they can be reported in the methodology)
    private static final double W_LEARNED_EXACT = 10.0;
    private static final double W_LEARNED_FUZZY = 6.0;
    private static final double W_LABEL_EXACT = 8.0;
    private static final double W_LABEL_WORD = 6.0;
    private static final double W_LABEL_SIMILAR = 5.5;
    private static final double W_LABEL_PREFIX = 3.0;
    private static final double W_LABEL_PHONETIC = 3.0;
    private static final double MIN_SIMILARITY = 0.6;
    // Eraka (v2, after device test): only show pictures we are reasonably sure about.
    // With a board of hundreds of pictures, loose rules always found "something"
    // for fillers like "well", "or", "mmm", so "No picture found" never appeared.
    private static final double MIN_SCORE = 3.0;
    // Drop weak candidates that score less than this share of the best one
    private static final double RELATIVE_CUTOFF = 0.6;

    /**
     * Sounds Whisper often outputs for noise, hesitation or unclear speech.
     * They are ignored for name matching (a caregiver can still TEACH one of
     * them, e.g. a child's "mmm" meaning "more": learned links use the full text).
     */
    private static final java.util.Set<String> FILLERS = new java.util.HashSet<>(java.util.Arrays.asList(
            "uh", "um", "umm", "uhm", "hmm", "hm", "mm", "mmm", "mmmm", "ah", "aah", "oh", "ooh", "eh",
            "ha", "haha", "hah", "huh", "er", "erm", "well", "so", "and", "or", "but", "the", "a", "an",
            "of", "to", "it", "is", "that", "this", "okay", "ok", "yeah", "yep", "like a", "you know",
            "thank you", "thanks", "bye", "music", "applause", "laughter", "silence", "blank audio"));
    private static final long RECENCY_HALF_LIFE_MS = 14L * 24 * 60 * 60 * 1000; // 2 weeks

    public static final String SOURCE_LEARNED = "learned";
    public static final String SOURCE_LABEL = "label";

    public static final class Candidate {
        public final JSONObject picto;
        public final int id;
        public final double score;
        public final String source;
        Candidate(JSONObject picto, int id, double score, String source) {
            this.picto = picto; this.id = id; this.score = score; this.source = source;
        }
    }

    private static VoiceDictionary instance;

    private final Context appContext;
    private String userId = "";
    /** normalized transcript -> (pictoId -> {count, last}) */
    private JSONObject entries = new JSONObject();

    private VoiceDictionary(Context ctx) { this.appContext = ctx.getApplicationContext(); }

    public static synchronized VoiceDictionary getInstance(Context ctx, String userId) {
        if (instance == null) instance = new VoiceDictionary(ctx);
        String uid = userId == null ? "" : userId;
        if (!uid.equals(instance.userId) || instance.entries == null) {
            instance.userId = uid;
            instance.load();
        }
        return instance;
    }

    // ------------------------------------------------------------------ text

    /** Lower-case, drop Whisper tags like [BLANK_AUDIO] / (music) / *coughs*, drop punctuation. */
    public static String normalize(String text) {
        if (text == null) return "";
        String t = text.toLowerCase(Locale.ROOT)
                .replaceAll("\\[[^\\]]*\\]", " ")
                .replaceAll("\\([^)]*\\)", " ")
                .replaceAll("\\*[^*]*\\*", " ")
                .replaceAll("[^\\p{L}\\p{N}' ]", " ")
                .replace("'", "")
                .replaceAll("\\s+", " ")
                .trim();
        return t;
    }

    /** True when Whisper returned only noise tags / nothing usable. */
    public static boolean isEmptyTranscript(String text) { return normalize(text).isEmpty(); }

    static double similarity(String a, String b) {
        if (a.equals(b)) return 1.0;
        int max = Math.max(a.length(), b.length());
        if (max == 0) return 1.0;
        return 1.0 - (double) levenshtein(a, b) / max;
    }

    static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[b.length()];
    }

    /** Classic Soundex: words that sound alike get the same 4-char code. */
    static String soundex(String s) {
        if (s == null || s.isEmpty()) return "";
        String codes = "01230120022455012623010202"; // a..z
        StringBuilder out = new StringBuilder();
        char first = s.charAt(0);
        if (first < 'a' || first > 'z') return "";
        out.append(Character.toUpperCase(first));
        char last = codes.charAt(first - 'a');
        for (int i = 1; i < s.length() && out.length() < 4; i++) {
            char c = s.charAt(i);
            if (c < 'a' || c > 'z') continue;
            char code = codes.charAt(c - 'a');
            if (code != '0' && code != last) out.append(code);
            if (c != 'h' && c != 'w') last = code;
        }
        while (out.length() < 4) out.append('0');
        return out.toString();
    }

    // ------------------------------------------------------------- matching

    /**
     * @param allPictos the child's full pictogram list (Json.getmJSONArrayTodosLosPictos())
     * @param lang      language key used in the pictogram "texto" object
     */
    public List<Candidate> match(String transcript, JSONArray allPictos, String lang, int limit) {
        List<Candidate> result = new ArrayList<>();
        String norm = normalize(transcript);
        if (norm.isEmpty() || allPictos == null) return result;
        String[] tokens = norm.split(" ");
        long now = System.currentTimeMillis();

        // 1) learned links
        Map<Integer, Double> learned = new HashMap<>();
        Iterator<String> keys = entries.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            double sim = similarity(norm, key);
            double weight;
            if (sim >= 0.999) weight = W_LEARNED_EXACT;
            else if (sim >= MIN_SIMILARITY) weight = W_LEARNED_FUZZY * sim;
            else continue;
            JSONObject links = entries.optJSONObject(key);
            if (links == null) continue;
            Iterator<String> ids = links.keys();
            while (ids.hasNext()) {
                String idStr = ids.next();
                JSONObject link = links.optJSONObject(idStr);
                if (link == null) continue;
                int count = link.optInt("count", 0);
                if (count <= 0) continue;
                long age = Math.max(0, now - link.optLong("last", now));
                double recency = Math.pow(0.5, (double) age / RECENCY_HALF_LIFE_MS);
                double s = weight * (1.0 + Math.log(count)) * (0.6 + 0.4 * recency);
                try {
                    int id = Integer.parseInt(idStr);
                    Double old = learned.get(id);
                    if (old == null || s > old) learned.put(id, s);
                } catch (NumberFormatException ignored) { }
            }
        }

        // 2) label matching over the child's own pictograms, ignoring filler sounds
        String filtered = stripFillers(norm);
        String[] labelTokens = filtered.isEmpty() ? new String[0] : filtered.split(" ");
        for (int i = 0; i < allPictos.length(); i++) {
            JSONObject picto = allPictos.optJSONObject(i);
            if (picto == null) continue;
            int id = picto.optInt("id", -1);
            if (id < 0) continue;
            if (ErakaVocabulary.isHiddenForChild(id)) continue; // pictogram audit: never offered to the child
            String label = normalize(JSONutils.getNombre(picto, lang));
            double labelScore = (label.isEmpty() || filtered.isEmpty()) ? 0 : labelScore(filtered, labelTokens, label);
            // Eraka 24 Oct 2026: the family's home word counts like the label ("meme" -> mom)
            String home = normalize(ErakaHomeWords.word(appContext, id));
            if (!home.isEmpty() && !filtered.isEmpty())
                labelScore = Math.max(labelScore, labelScore(filtered, labelTokens, home));
            Double l = learned.get(id);
            double learnedScore = l == null ? 0 : l;
            double total = learnedScore + labelScore;
            if (total >= MIN_SCORE) {
                result.add(new Candidate(picto, id, total,
                        learnedScore >= labelScore ? SOURCE_LEARNED : SOURCE_LABEL));
            }
        }

        Collections.sort(result, (a, b) -> Double.compare(b.score, a.score));
        if (!result.isEmpty()) {
            double cutoff = result.get(0).score * RELATIVE_CUTOFF;
            List<Candidate> kept = new ArrayList<>();
            for (Candidate c : result) if (c.score >= cutoff) kept.add(c);
            result = kept;
        }
        if (result.size() > limit) return new ArrayList<>(result.subList(0, limit));
        return result;
    }

    // ------------------------------------------------------ whole sentences

    /** A sentence needs at least this many spoken words before it is offered as a message. */
    static final int SENTENCE_MIN_WORDS = 3;
    /** Longest picture name tried as one unit ("brush teeth", "get dressed"). */
    private static final int SENTENCE_MAX_SPAN = 3;
    /** At most this many pictures in one spoken message. */
    private static final int SENTENCE_MAX_PICTURES = 6;
    /** Spelling must be at least this close (and start with the same letter) for a word that is not an exact picture name. */
    private static final double SENTENCE_MIN_SIMILARITY = 0.6;

    /**
     * Eraka 27 Oct 2026: when the child says a whole sentence ("I want to go home"), finds a
     * picture for each word, in the order spoken: [I] [want] [go] [home]. Small words and
     * fillers are skipped, a word said twice gives its picture once, and words without a
     * picture are left out. Returns an empty list unless at least 2 pictures were found in
     * a transcript of at least 3 words, so single words keep the normal "Did you mean" tiles.
     *
     * Per word (or 2-3 word span, longest first): exact picture name or home word, then a
     * learned link for exactly that word, then the word without -s/-es/-ing/-ed, then a close
     * spelling (4+ letters, same first letter, similarity >= 0.6). No guessing beyond that: one wrong picture in
     * a message is worse than a missing one.
     */
    public List<Candidate> matchSequence(String transcript, JSONArray allPictos, String lang) {
        List<Candidate> out = new ArrayList<>();
        String norm = normalize(transcript);
        if (norm.isEmpty() || allPictos == null) return out;
        if (norm.split(" ").length < SENTENCE_MIN_WORDS) return out;
        String filtered = stripFillers(norm);
        if (filtered.isEmpty()) return out;
        String[] words = filtered.split(" ");

        // name (label or home word) -> picture, first visible picture wins
        Map<String, JSONObject> byName = new HashMap<>();
        Map<Integer, JSONObject> byId = new HashMap<>();
        for (int i = 0; i < allPictos.length(); i++) {
            JSONObject picto = allPictos.optJSONObject(i);
            if (picto == null) continue;
            int id = picto.optInt("id", -1);
            if (id < 0 || ErakaVocabulary.isHiddenForChild(id)) continue;
            byId.put(id, picto);
            String home = normalize(ErakaHomeWords.word(appContext, id));
            if (!home.isEmpty() && !byName.containsKey(home)) byName.put(home, picto);
            String label = normalize(JSONutils.getNombre(picto, lang));
            if (!label.isEmpty() && !byName.containsKey(label)) byName.put(label, picto);
        }

        java.util.Set<Integer> used = new java.util.HashSet<>();
        int i = 0;
        while (i < words.length && out.size() < SENTENCE_MAX_PICTURES) {
            JSONObject found = null;
            int span = 1;
            double score = 0;
            // 1) exact name, longest span first ("get dressed" before "get")
            for (int n = Math.min(SENTENCE_MAX_SPAN, words.length - i); n >= 1 && found == null; n--) {
                StringBuilder sb = new StringBuilder();
                for (int k = 0; k < n; k++) { if (k > 0) sb.append(' '); sb.append(words[i + k]); }
                JSONObject p = byName.get(sb.toString());
                if (p != null) { found = p; span = n; score = W_LABEL_EXACT; }
            }
            String w = words[i];
            // 2) a learned link for exactly this word (the child's own "wa" = water)
            if (found == null) {
                int id = strongestLearned(w);
                if (id >= 0 && byId.containsKey(id)) { found = byId.get(id); score = W_LEARNED_EXACT; }
            }
            // 3) simple word endings: wants, going, played, dishes
            if (found == null) {
                for (String stem : stems(w)) {
                    JSONObject p = byName.get(stem);
                    if (p != null) { found = p; score = W_LABEL_WORD; break; }
                }
            }
            // 4) close spelling of a one-word name ("wata" -> water), never for short words
            if (found == null && w.length() >= 4) {
                double best = SENTENCE_MIN_SIMILARITY;
                for (Map.Entry<String, JSONObject> e : byName.entrySet()) {
                    String name = e.getKey();
                    if (name.indexOf(' ') >= 0 || name.length() < 4 || name.charAt(0) != w.charAt(0)) continue;
                    double sim = similarity(w, name);
                    if (sim > best || (sim == best && found == null)) { best = sim; found = e.getValue(); score = W_LABEL_SIMILAR * sim; }
                }
            }
            if (found != null) {
                int id = found.optInt("id", -1);
                if (used.add(id)) out.add(new Candidate(found, id, score, SOURCE_LABEL));
            }
            i += span;
        }
        if (out.size() < 2) out.clear();
        return out;
    }

    /** The picture most often confirmed for exactly this sound, or -1. */
    private int strongestLearned(String sound) {
        JSONObject links = entries.optJSONObject(sound);
        if (links == null) return -1;
        int best = -1, bestCount = 0;
        Iterator<String> ids = links.keys();
        while (ids.hasNext()) {
            String idStr = ids.next();
            JSONObject link = links.optJSONObject(idStr);
            int c = link == null ? 0 : link.optInt("count", 0);
            if (c > bestCount) {
                try { best = Integer.parseInt(idStr); bestCount = c; } catch (NumberFormatException ignored) { }
            }
        }
        return best;
    }

    /** Possible base forms of a word: wants->want, dishes->dish, going->go, eating->eat/eate, played->play. */
    static List<String> stems(String w) {
        List<String> s = new ArrayList<>();
        if (w.length() > 3 && w.endsWith("es")) s.add(w.substring(0, w.length() - 2));
        if (w.length() > 2 && w.endsWith("s") && !w.endsWith("ss")) s.add(w.substring(0, w.length() - 1));
        if (w.length() > 4 && w.endsWith("ing")) {
            String b = w.substring(0, w.length() - 3);
            s.add(b);
            s.add(b + "e");                                                        // making -> make
            if (b.length() > 2 && b.charAt(b.length() - 1) == b.charAt(b.length() - 2))
                s.add(b.substring(0, b.length() - 1));                             // swimming -> swim
        }
        if (w.length() > 3 && w.endsWith("ed")) {
            String b = w.substring(0, w.length() - 2);
            s.add(b);
            s.add(b + "e");                                                        // liked -> like
        }
        return s;
    }

    /** Removes filler words/phrases ("uh", "well", "like a") from normalized text. */
    static String stripFillers(String norm) {
        if (FILLERS.contains(norm)) return "";
        StringBuilder out = new StringBuilder();
        for (String t : norm.split(" ")) {
            if (t.isEmpty() || FILLERS.contains(t)) continue;
            if (out.length() > 0) out.append(' ');
            out.append(t);
        }
        return out.toString();
    }

    /**
     * v2 rules (stricter). Two-letter sounds only match a picture exactly;
     * everything looser needs at least 3-4 letters.
     */
    private static double labelScore(String norm, String[] tokens, String label) {
        if (norm.equals(label)) return W_LABEL_EXACT;
        // the same word split up by Whisper ("me me" for "meme", "ta te" for "tate")
        String joined = norm.replace(" ", "");
        if (joined.length() >= 4 && joined.equals(label.replace(" ", ""))) return W_LABEL_EXACT - 1.0;
        // whole label appears as words inside the transcript ("i want water" -> water)
        if ((" " + norm + " ").contains(" " + label + " ")) return W_LABEL_WORD;
        String[] labelTokens = label.split(" ");
        double best = 0;
        String labelHead = labelTokens[0];
        String labelSx = soundex(labelHead);
        for (String t : tokens) {
            if (t.length() < 3) continue;
            double sim = similarity(t, labelHead);
            // spelling close enough ("wata" -> water); needs 4+ letters
            if (t.length() >= 4 && sim >= MIN_SIMILARITY) best = Math.max(best, W_LABEL_SIMILAR * sim);
            // start of the word ("wat" -> water), at least half of it
            if (labelHead.length() > t.length() && labelHead.startsWith(t)
                    && (double) t.length() / labelHead.length() >= 0.5)
                best = Math.max(best, 3.5);
            // sounds alike AND reasonably similar spelling
            if (!labelSx.isEmpty() && labelSx.equals(soundex(t)) && sim >= 0.5)
                best = Math.max(best, W_LABEL_PHONETIC - 0.5 + sim);
        }
        return best;
    }

    // ------------------------------------------------------------- learning

    /** Child tapped this pictogram after speaking: strengthen the link. */
    public void learn(String transcript, int pictoId) { add(transcript, pictoId, 1); }

    /** Caregiver deliberately taught this sound for this pictogram: stronger link. */
    public void teach(String transcript, int pictoId) { add(transcript, pictoId, 3); }

    private synchronized void add(String transcript, int pictoId, int amount) {
        String norm = normalize(transcript);
        if (norm.isEmpty() || pictoId < 0) return;
        try {
            JSONObject links = entries.optJSONObject(norm);
            if (links == null) { links = new JSONObject(); entries.put(norm, links); }
            String key = String.valueOf(pictoId);
            JSONObject link = links.optJSONObject(key);
            if (link == null) { link = new JSONObject(); links.put(key, link); }
            link.put("count", link.optInt("count", 0) + amount);
            link.put("last", System.currentTimeMillis());
            save();
        } catch (Exception e) {
            Log.e(TAG, "add: " + e.getMessage());
        }
    }

    /** Remove one learned link (caregiver says "that was wrong"). */
    public synchronized void forget(String transcript, int pictoId) {
        JSONObject links = entries.optJSONObject(normalize(transcript));
        if (links == null) return;
        links.remove(String.valueOf(pictoId));
        if (links.length() == 0) entries.remove(normalize(transcript));
        save();
    }

    /** Picture ids this child has confirmed or been taught, most-used first. */
    public List<Integer> learnedPictoIds() {
        final Map<Integer, Integer> counts = new HashMap<>();
        Iterator<String> keys = entries.keys();
        while (keys.hasNext()) {
            JSONObject links = entries.optJSONObject(keys.next());
            if (links == null) continue;
            Iterator<String> ids = links.keys();
            while (ids.hasNext()) {
                String idStr = ids.next();
                try {
                    int id = Integer.parseInt(idStr);
                    JSONObject link = links.optJSONObject(idStr);
                    int c = link == null ? 0 : link.optInt("count", 0);
                    Integer old = counts.get(id);
                    counts.put(id, (old == null ? 0 : old) + c);
                } catch (NumberFormatException ignored) { }
            }
        }
        List<Integer> out = new ArrayList<>(counts.keySet());
        Collections.sort(out, (a, b) -> Integer.compare(counts.get(b), counts.get(a)));
        return out;
    }

    public int learnedCount() {
        int n = 0;
        Iterator<String> keys = entries.keys();
        while (keys.hasNext()) {
            JSONObject links = entries.optJSONObject(keys.next());
            if (links != null) n += links.length();
        }
        return n;
    }

    // ----------------------------------------------------------- evaluation

    /**
     * One CSV line per voice attempt:
     * timestamp,transcript,shown_ids,chosen_id,rank,source,mode
     * rank = 1..4 when the child picked a suggestion, 0 = picked outside the list
     * (caregiver correction), -1 = nothing chosen.
     */
    public void logAttempt(String transcript, List<Candidate> shown, int chosenId, String mode) {
        try {
            File f = evalFile();
            boolean header = !f.exists();
            StringBuilder ids = new StringBuilder();
            int rank = chosenId < 0 ? -1 : 0;
            String source = "";
            for (int i = 0; i < shown.size(); i++) {
                if (i > 0) ids.append(' ');
                ids.append(shown.get(i).id);
                if (shown.get(i).id == chosenId) { rank = i + 1; source = shown.get(i).source; }
            }
            try (FileWriter w = new FileWriter(f, true)) {
                if (header) w.write("timestamp,transcript,shown_ids,chosen_id,rank,source,mode\n");
                w.write(System.currentTimeMillis() + ",\"" + normalize(transcript).replace("\"", "") + "\","
                        + ids + "," + chosenId + "," + rank + "," + source + "," + mode + "\n");
            }
        } catch (IOException e) {
            Log.e(TAG, "logAttempt: " + e.getMessage());
        }
    }

    public File evalFile() { return new File(appContext.getFilesDir(), "voice_eval_" + safeUid() + ".csv"); }

    // -------------------------------------------------------------- storage

    private String safeUid() {
        String u = userId == null || userId.isEmpty() ? "local" : userId;
        return u.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    private File dictFile() { return new File(appContext.getFilesDir(), "voice_dictionary_" + safeUid() + ".json"); }

    private void load() {
        entries = new JSONObject();
        File f = dictFile();
        if (!f.exists()) return;
        try {
            byte[] data = new byte[(int) f.length()];
            try (java.io.FileInputStream in = new java.io.FileInputStream(f)) {
                int read = 0;
                while (read < data.length) {
                    int r = in.read(data, read, data.length - read);
                    if (r < 0) break;
                    read += r;
                }
            }
            JSONObject root = new JSONObject(new String(data, StandardCharsets.UTF_8));
            JSONObject e = root.optJSONObject("entries");
            if (e != null) entries = e;
        } catch (Exception e) {
            Log.e(TAG, "load: " + e.getMessage());
        }
    }

    private void save() {
        try {
            JSONObject root = new JSONObject();
            root.put("version", 1);
            root.put("entries", entries);
            File tmp = new File(dictFile().getPath() + ".tmp");
            try (FileOutputStream out = new FileOutputStream(tmp)) {
                out.write(root.toString().getBytes(StandardCharsets.UTF_8));
            }
            if (!tmp.renameTo(dictFile())) Log.e(TAG, "save: rename failed");
        } catch (Exception e) {
            Log.e(TAG, "save: " + e.getMessage());
        }
    }
}
