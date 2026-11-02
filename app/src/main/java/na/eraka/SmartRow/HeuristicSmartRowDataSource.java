package na.eraka.SmartRow;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import na.eraka.JSONutils.Json;
import na.eraka.utils.JSONutils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HeuristicSmartRowDataSource implements SmartRowView.SmartRowDataSource {

    private static final int SUGGESTION_LIMIT = 6;
    // how many to return; ErakaSmartRowDataSource asks for more, because it drops
    // the ones the model already chose and pictures already in the message
    private int limit = SUGGESTION_LIMIT;

    public void setLimit(int limit) {
        this.limit = Math.max(1, limit);
    }
    // Fallback "frec" used when a pictogram has never been selected from any
    // parent (no relation entries reference it yet) — e.g. brand-new custom
    // pictograms. Keeps them from permanently scoring at absolute zero.
    private static final int NO_USAGE_FREC_FLOOR = 5;

    // Flat score bonus applied when a pictogram's English label matches a
    // published AAC core-vocabulary word.
    private static final int CORE_VOCABULARY_BONUS = 20;

    private static final Set<String> CORE_VOCABULARY = new HashSet<>(Arrays.asList(
            "i", "you", "it", "that", "this",
            "want", "like", "need", "help", "stop", "go", "come", "look", "give", "get", "put", "make", "do",
            "more", "again", "all done", "finished", "different", "same",
            "yes", "no", "not",
            "good", "bad", "happy", "sad", "hurt", "sick", "tired",
            "big", "small", "hot", "cold",
            "in", "out", "up", "down", "on", "off",
            "eat", "drink", "play",
            "mom", "dad",
            "please", "thank you", "sorry", "wait",
            "who", "what", "where", "when", "why", "how"
    ));

    private final Context context;

    // IDs currently visible in the 4 main tiles — the smart row should never
    // suggest one of these, since the whole point is to offer something the
    // user can't already see. Updated by MainBoardActivity whenever the main tiles
    // change (see MainBoardActivity.refreshSmartRow()).
    private volatile Set<Integer> excludedIds = Collections.emptySet();

    public HeuristicSmartRowDataSource(Context context) {
        this.context = context;
    }

    public void setExcludedIds(Set<Integer> excludedIds) {
        this.excludedIds = (excludedIds != null) ? excludedIds : Collections.emptySet();
    }

    @Override
    public void getSuggestions(SmartRowView.Scope scope, long timestamp, String userId, Callback callback) {
        JSONArray allPictograms = Json.getInstance().getmJSONArrayTodosLosPictos();

        String hora = getHoraBucket(timestamp);
        String agenda = getCurrentAgenda();
        String posicion = getCurrentPosicion();
        String sexo = getUserSexo(userId);
        String edad = getUserEdad(userId);

        Set<Integer> excluded = excludedIds;

        // "frec" lives per parent->child relationship (see JSONutils.aumentarFrec),
        // not as a standalone property on each pictogram. To rank pictograms
        // globally (independent of the current parent), aggregate every relation
        // entry across the whole set into a single per-pictogram usage total.
        Map<Integer, Integer> globalFrec = computeGlobalFrec(allPictograms);

        List<ScoredItem> scored = new ArrayList<>();

        for (int i = 0; i < allPictograms.length(); i++) {
            try {
                JSONObject picto = allPictograms.getJSONObject(i);
                int id = JSONutils.getId(picto);
                if (id == -1) continue;
                if (excluded.contains(id)) continue; // already visible in a main tile — skip

                int frec = globalFrec.containsKey(id) ? globalFrec.get(id) : NO_USAGE_FREC_FLOOR;

                JSONObject scoringProxy = new JSONObject();
                scoringProxy.put("id", id);
                scoringProxy.put("frec", frec);

                double score = JSONutils.score(scoringProxy, false, agenda, sexo, hora, edad, posicion);

                String label = JSONutils.getNombre(picto, "en");
                if (label != null && CORE_VOCABULARY.contains(label.toLowerCase(Locale.ROOT).trim())) {
                    score += CORE_VOCABULARY_BONUS;
                }

                scored.add(new ScoredItem(picto, score));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        Collections.sort(scored, (a, b) -> Double.compare(b.score, a.score));

        List<SmartRowItem> results = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scored.size()); i++) {
            JSONObject picto = scored.get(i).item;
            try {
                String id = String.valueOf(JSONutils.getId(picto));
                if (na.eraka.utils.ErakaVocabulary.isHiddenForChild(JSONutils.getId(picto))) continue; // pictogram audit
                String label = JSONutils.getNombre(picto, "en");
                if (label == null || label.isEmpty()) continue;

                JSONObject imagenInfo = JSONutils.getImagen(picto);
                String imageUrl = imagenInfo.optString("picto", "");

                results.add(new SmartRowItem(id, label, imageUrl));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        new Handler(Looper.getMainLooper()).post(() -> callback.onResult(results));
    }

    private Map<Integer, Integer> computeGlobalFrec(JSONArray allPictograms) {
        Map<Integer, Integer> totals = new HashMap<>();
        for (int i = 0; i < allPictograms.length(); i++) {
            try {
                JSONObject parent = allPictograms.getJSONObject(i);
                if (!parent.has("relacion")) continue;

                JSONArray relacion = parent.getJSONArray("relacion");
                for (int j = 0; j < relacion.length(); j++) {
                    JSONObject entry = relacion.getJSONObject(j);
                    if (!entry.has("id") || !entry.has("frec")) continue;

                    int childId = entry.getInt("id");
                    int frec = entry.getInt("frec");
                    Integer existing = totals.get(childId);
                    totals.put(childId, existing == null ? frec : existing + frec);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return totals;
    }

    private String getHoraBucket(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        int hour = cal.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) return "MANANA";
        if (hour >= 12 && hour < 15) return "MEDIODIA";
        if (hour >= 15 && hour < 20) return "TARDE";
        return "NOCHE";
    }

    private String getCurrentAgenda() { return ""; }
    private String getCurrentPosicion() { return ""; }
    private String getUserSexo(String userId) { return ""; }
    private String getUserEdad(String userId) { return ""; }

    private static class ScoredItem {
        final JSONObject item;
        final double score;

        ScoredItem(JSONObject item, double score) {
            this.item = item;
            this.score = score;
        }
    }
}