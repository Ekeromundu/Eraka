package na.eraka.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Eraka 28 Oct 2026: gives pictures with a negative id a positive one.
 *
 * OTTAA numbered a new picture with {@code (int) System.currentTimeMillis()}. The time in
 * milliseconds no longer fits in an int, so since about 2025 that number can be negative
 * (today's are around -2,100,000,000). Eraka uses -1 to mean "no picture" and skips negative
 * ids in several places, so a parent's own picture was never played with its recording, never
 * matched by speech and never explored. New pictures now get a positive id
 * ({@link JSONutils#newId}); this class renumbers the ones made before, once, when the files
 * are loaded: the picture itself, every link to it (in pictures and groups), its home word and
 * its recording. Old usage-log lines keep the old number.
 */
public final class ErakaIdFix {

    private static final String TAG = "ErakaIdFix";

    private ErakaIdFix() {}

    /** Renumbers negative picture ids in place; returns how many were changed. */
    public static int fixNegativeIds(Context c, JSONArray pictos, JSONArray groups) {
        if (pictos == null) return 0;
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < pictos.length(); i++) {
            JSONObject p = pictos.optJSONObject(i);
            if (p != null) used.add(p.optInt("id", 0));
        }
        Map<Integer, Integer> moved = new HashMap<>();
        for (int i = 0; i < pictos.length(); i++) {
            JSONObject p = pictos.optJSONObject(i);
            if (p == null) continue;
            int old = p.optInt("id", 0);
            if (old >= 0) continue;
            int id = JSONutils.positive(old);
            while (used.contains(id)) id++;
            used.add(id);
            try {
                p.put("id", id);
                moved.put(old, id);
            } catch (Exception e) {
                Log.e(TAG, "id " + old + ": " + e.getMessage());
            }
        }
        if (moved.isEmpty()) return 0;
        relink(pictos, moved);
        relink(groups, moved);
        for (Map.Entry<Integer, Integer> e : moved.entrySet()) {
            ErakaHomeWords.moveId(c, e.getKey(), e.getValue());
            Log.d(TAG, "picture " + e.getKey() + " -> " + e.getValue());
        }
        return moved.size();
    }

    /** Points every "relacion" entry that names an old id to the new one. */
    private static void relink(JSONArray items, Map<Integer, Integer> moved) {
        if (items == null) return;
        for (int i = 0; i < items.length(); i++) {
            JSONObject item = items.optJSONObject(i);
            JSONArray rel = item == null ? null : item.optJSONArray("relacion");
            for (int j = 0; rel != null && j < rel.length(); j++) {
                JSONObject r = rel.optJSONObject(j);
                if (r == null || r.isNull("id")) continue;
                Integer to = moved.get(r.optInt("id", 0));
                if (to != null) {
                    try { r.put("id", to); } catch (Exception ignored) { }
                }
            }
        }
    }
}
