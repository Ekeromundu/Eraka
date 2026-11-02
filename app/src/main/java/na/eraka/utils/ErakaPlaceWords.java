package na.eraka.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import na.eraka.JSONutils.Json;
import na.eraka.R;
import na.eraka.utils.constants.Constants;

/**
 * Eraka 28 Oct 2026: starter pictures for each "Where are we?" place
 * (assets/eraka_places/place_words.json; church: pray, sing, listen, sit down, toilet, …).
 *
 * Used in two ways:
 *  1. the suggestion row: the prediction model gives the chosen place a share of the score from
 *     the first day (ErakaPredictor.setPlaceWords); the child's own taps at that place take over;
 *  2. the "Here" group: one group, always first in the Groups list, whose pictures follow the
 *     chosen place ("Here: Church"). At home it shows everyday words.
 */
public final class ErakaPlaceWords {

    private static final String TAG = "ErakaPlaceWords";
    private static final String ASSET = "eraka_places/place_words.json";
    /** Id of the "Here" group (other Eraka groups: My day 6001). */
    public static final int HERE_GROUP_ID = 6100;

    private static Map<String, List<Integer>> lists;

    private ErakaPlaceWords() {}

    private static synchronized Map<String, List<Integer>> load(Context c) {
        if (lists != null) return lists;
        lists = new HashMap<>();
        try (InputStream in = c.getApplicationContext().getAssets().open(ASSET)) {
            byte[] b = new byte[in.available()];
            int n = in.read(b);
            JSONObject o = new JSONObject(new String(b, 0, Math.max(n, 0), StandardCharsets.UTF_8));
            for (Iterator<String> it = o.keys(); it.hasNext(); ) {
                String key = it.next();
                JSONArray a = o.optJSONArray(key);
                if (key.startsWith("_") || a == null) continue;
                List<Integer> ids = new ArrayList<>();
                for (int i = 0; i < a.length(); i++) ids.add(a.optInt(i));
                lists.put(key, ids);
            }
        } catch (Exception e) {
            Log.e(TAG, "load: " + e.getMessage());
        }
        return lists;
    }

    /** The place's pictures that exist on this phone and are not hidden for children. */
    public static List<Integer> idsFor(Context c, String place) {
        List<Integer> out = new ArrayList<>();
        List<Integer> ids = load(c).get(place);
        if (ids == null) return out;
        Json json = Json.getInstance();
        for (int id : ids) {
            if (ErakaVocabulary.isHiddenForChild(id) || out.contains(id)) continue;
            try {
                if (json.getmJSONArrayTodosLosPictos() != null && json.getPictoFromId2(id) == null) continue;
            } catch (Exception ignored) { }
            out.add(id);
        }
        return out;
    }

    /** Word lists for the prediction model: every place except home (home = the normal model). */
    public static Map<String, List<Integer>> forPredictor(Context c) {
        Map<String, List<Integer>> m = new HashMap<>();
        for (String place : load(c).keySet()) {
            if ("HOME".equals(place)) continue;
            List<Integer> ids = idsFor(c, place);
            if (!ids.isEmpty()) m.put(place, ids);
        }
        return m;
    }

    /**
     * Creates or refreshes the "Here" group for the current place (first in the Groups list).
     * Saves the groups file only when something changed.
     */
    public static synchronized void updateHereGroup(Context c) {
        try {
            Json json = Json.getInstance();
            JSONArray groups = json.getmJSONArrayTodosLosGrupos();
            if (groups == null || groups.length() == 0) return;
            ErakaPlaces.Place place = ErakaPlaces.current(c);
            List<Integer> ids = idsFor(c, place.key);
            if (ids.isEmpty()) return;

            JSONObject group = null;
            for (int i = 0; i < groups.length(); i++) {
                JSONObject g = groups.optJSONObject(i);
                if (g != null && g.optInt("id", Integer.MIN_VALUE) == HERE_GROUP_ID) { group = g; break; }
            }
            String title = c.getString(R.string.here_group, c.getString(place.label));
            String picture = pictureOf(json, ids.get(0));
            String before = group == null ? "" : group.toString();
            if (group == null) {
                group = new JSONObject();
                group.put("id", HERE_GROUP_ID);
                group.put("tipo", 0);
                group.put("frec", 0);
                // first in the Groups list
                List<Object> rest = new ArrayList<>();
                for (int i = 0; i < groups.length(); i++) rest.add(groups.get(i));
                for (int i = groups.length() - 1; i >= 0; i--) groups.remove(i);
                groups.put(group);
                for (Object o : rest) groups.put(o);
            }
            JSONObject texto = new JSONObject();
            texto.put("en", title);
            texto.put("es", title);
            group.put("texto", texto);
            JSONObject imagen = new JSONObject();
            imagen.put("picto", picture);
            group.put("imagen", imagen);
            JSONArray rel = new JSONArray();
            for (int id : ids) {
                JSONObject link = new JSONObject();
                link.put("id", id);
                link.put("frec", 1);
                rel.put(link);
            }
            group.put("relacion", rel);
            if (!group.toString().equals(before)) {
                json.setmJSONArrayTodosLosGrupos(groups);
                json.guardarJson(Constants.ARCHIVO_GRUPOS);
                Log.d(TAG, "Here group: " + title + ", " + ids.size() + " pictures");
            }
        } catch (Exception e) {
            Log.e(TAG, "Here group: " + e.getMessage());
        }
    }

    private static String pictureOf(Json json, int id) {
        try {
            JSONObject p = json.getPictoFromId2(id);
            JSONObject img = p == null ? null : p.optJSONObject("imagen");
            String name = img == null ? "" : img.optString("picto", "");
            if (!name.isEmpty()) return name;
        } catch (Exception ignored) { }
        return "ic_agregar_nuevo";
    }
}
