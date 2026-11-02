package na.eraka.utils;

import android.content.Context;
import android.media.MediaPlayer;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Home words and the parent's own voice (22 Oct 2026).
 *
 * Eraka speaks English, but many Namibian children hear other words at home: "meme" for mom,
 * "tate" for dad, the family's word for porridge. A parent can give any picture
 *  - a home word, which Eraka says instead of the English label (when the picture is tapped,
 *    in the message and in the suggested sentence), and
 *  - a short recording of their own voice, which is played when the child taps the picture.
 *
 * The English label is kept: the prediction model, the sentence builder and the study data keep
 * working with it, and the home word is put into the finished sentence afterwards
 * ("Mom, I want water." -> "Meme, I want water.").
 *
 * Everything stays on the phone (files/eraka_home_words.json and files/eraka_voice/ID.m4a);
 * nothing is uploaded or exported.
 */
public final class ErakaHomeWords {

    private static final String TAG = "ErakaHomeWords";
    private static final String FILE = "eraka_home_words.json";
    private static final String VOICE_DIR = "eraka_voice";

    private static Map<Integer, String> words;          // picture id -> home word
    private static MediaPlayer player;

    private ErakaHomeWords() {}

    // ---------------------------------------------------------------- home words

    private static synchronized Map<Integer, String> load(Context c) {
        if (words != null) return words;
        words = new HashMap<>();
        File f = new File(c.getApplicationContext().getFilesDir(), FILE);
        if (!f.exists()) return words;
        try (FileInputStream in = new FileInputStream(f)) {
            byte[] b = new byte[(int) f.length()];
            int n = in.read(b);
            JSONObject o = new JSONObject(new String(b, 0, Math.max(n, 0), StandardCharsets.UTF_8));
            for (Iterator<String> it = o.keys(); it.hasNext(); ) {
                String k = it.next();
                String w = o.optString(k, "").trim();
                if (!w.isEmpty()) words.put(Integer.parseInt(k), w);
            }
        } catch (Exception e) {
            Log.e(TAG, "load: " + e.getMessage());
        }
        return words;
    }

    private static synchronized void save(Context c) {
        try {
            JSONObject o = new JSONObject();
            for (Map.Entry<Integer, String> e : load(c).entrySet()) o.put(String.valueOf(e.getKey()), e.getValue());
            File f = new File(c.getApplicationContext().getFilesDir(), FILE);
            try (FileOutputStream out = new FileOutputStream(f)) {
                out.write(o.toString().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            Log.e(TAG, "save: " + e.getMessage());
        }
    }

    /** The home word for picture {@code id}, or "" if there is none. */
    public static String word(Context c, int id) {
        String w = load(c).get(id);
        return w == null ? "" : w;
    }

    /** All home words of this child (for the speech recogniser's word hint). */
    public static java.util.List<String> allWords(Context c) {
        return new java.util.ArrayList<>(load(c).values());
    }

    /** Sets (or, with an empty word, removes) the home word of picture {@code id}. */
    public static synchronized void setWord(Context c, int id, String word) {
        String w = word == null ? "" : word.trim();
        if (w.isEmpty()) load(c).remove(id); else load(c).put(id, w);
        save(c);
    }

    /** Eraka 28 Oct 2026: a picture got a new number (ErakaIdFix): move its home word and recording. */
    public static synchronized void moveId(Context c, int oldId, int newId) {
        String w = load(c).remove(oldId);
        if (w != null) { load(c).put(newId, w); save(c); }
        File from = voiceFile(c, oldId), to = voiceFile(c, newId);
        if (from.exists() && !to.exists() && !from.renameTo(to)) Log.e(TAG, "could not move the recording of " + oldId);
    }

    /** What to say for a picture: its home word if it has one, else its label. */
    public static String spoken(Context c, int id, String label) {
        String w = word(c, id);
        return w.isEmpty() ? label : w;
    }

    /**
     * Puts the home words into a sentence built from the English labels:
     * "Mom, I want water." with mom -> meme gives "Meme, I want water.".
     * The label is matched as a whole word, also with a plural -s/-es; a capital at the start
     * of the sentence is kept.
     */
    public static String substitute(Context c, String sentence, List<JSONObject> message) {
        if (sentence == null || message == null || load(c).isEmpty()) return sentence;
        String out = sentence;
        for (JSONObject p : message) {
            int id = p.optInt("id", -1);
            String home = word(c, id);
            if (home.isEmpty()) continue;
            String label = na.eraka.utils.JSONutils.getNombre(p, "en");
            if (label == null || label.trim().isEmpty()) continue;
            Matcher m = Pattern.compile("(?i)\\b" + Pattern.quote(label.trim()) + "(e?s)?\\b").matcher(out);
            StringBuffer sb = new StringBuffer();
            while (m.find()) {
                boolean capital = Character.isUpperCase(m.group().charAt(0));
                String rep = capital ? home.substring(0, 1).toUpperCase(Locale.ROOT) + home.substring(1) : home;
                m.appendReplacement(sb, Matcher.quoteReplacement(rep));
            }
            m.appendTail(sb);
            out = fixArticle(sb.toString(), home);
        }
        return out;
    }

    /** "an meme" -> "a meme", "a ondjamba" -> "an ondjamba" after a word was swapped. */
    private static String fixArticle(String text, String home) {
        boolean vowel = "aeiouAEIOU".indexOf(home.charAt(0)) >= 0;
        Matcher m = Pattern.compile("\\b([Aa])n?(\\s+)(?=" + Pattern.quote(home) + "\\b)", Pattern.CASE_INSENSITIVE).matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1) + (vowel ? "n" : "") + m.group(2)));
        m.appendTail(sb);
        return sb.toString();
    }

    // ---------------------------------------------------------------- the parent's voice

    public static File voiceFile(Context c, int id) {
        File dir = new File(c.getApplicationContext().getFilesDir(), VOICE_DIR);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, id + ".m4a");
    }

    /** Where a new recording goes until the parent presses Save. */
    public static File pendingVoiceFile(Context c) {
        File dir = new File(c.getApplicationContext().getFilesDir(), VOICE_DIR);
        if (!dir.exists()) dir.mkdirs();
        return new File(dir, "pending.m4a");
    }

    public static boolean hasVoice(Context c, int id) {
        File f = voiceFile(c, id);
        return f.exists() && f.length() > 0;
    }

    /** Plays a recording; returns false if it could not be played. */
    public static synchronized boolean play(Context c, File f) {
        if (f == null || !f.exists() || f.length() == 0) return false;
        try {
            stop();
            player = new MediaPlayer();
            player.setDataSource(f.getAbsolutePath());
            player.setOnCompletionListener(mp -> stop());
            player.prepare();
            player.start();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "play: " + e.getMessage());
            stop();
            return false;
        }
    }

    /** Plays the parent's recording for picture {@code id}; false if there is none. */
    public static boolean playVoice(Context c, int id) {
        return hasVoice(c, id) && play(c, voiceFile(c, id));
    }

    /**
     * For screens that say a picture when it is tapped (gallery, search): plays the parent's
     * recording and returns null, or returns the text to speak (the home word or the label).
     */
    public static String speakPicto(Context c, JSONObject picto, String label) {
        int id = picto == null ? -1 : picto.optInt("id", -1);
        if (id != -1 && playVoice(c, id)) return null;
        return id != -1 ? spoken(c, id, label) : label;
    }

    public static synchronized void stop() {
        if (player != null) {
            try { player.release(); } catch (Exception ignored) { }
            player = null;
        }
    }
}
