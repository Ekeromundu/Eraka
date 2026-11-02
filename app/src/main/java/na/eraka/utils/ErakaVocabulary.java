package na.eraka.utils;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Eraka's corrections to OTTAA's built-in vocabulary (pictogram audit, 13 Oct 2026).
 *
 * Read from assets/eraka_corrections/vocabulary_corrections.json and applied every time
 * the vocabulary is loaded (Json.initJsonArrays and the setters used by backup/restore),
 * so they reach new users, existing users and restored backups alike. OTTAA's own files
 * are not edited; the pictures themselves are unchanged.
 *
 *  - hide:           removed everywhere: tiles, suggestion row, exploration, gallery, voice.
 *  - caregiver_only: not offered to the child (tiles, suggestions, exploration, voice),
 *                    but still in the gallery groups for adults (e.g. abuse, medicines).
 *  - rename:         new English label, applied only while the label is still OTTAA's
 *                    original, so a label a parent changed is never overwritten.
 *  - add (22 Oct):   new pictograms for words parents asked for (sleep, hurt, phone...),
 *                    with an ARASAAC drawing in res/drawable. Added to the pictogram list
 *                    and to their groups (and "ALL") when missing, and only if the drawing
 *                    exists in the app, so a missing drawing never shows an empty tile.
 *  - photo (22 Oct): a real photograph instead of OTTAA's drawing (e.g. a Namibian dog),
 *                    applied only while the picture is still OTTAA's original, so a picture a
 *                    parent chose is never replaced, and only if the photo exists in the app.
 */
public final class ErakaVocabulary {

    private static final String TAG = "ErakaVocabulary";
    public static final String ASSET = "eraka_corrections/vocabulary_corrections.json";

    private static volatile boolean loaded = false;
    private static Set<Integer> hidden = Collections.emptySet();
    private static Set<Integer> caregiverOnly = Collections.emptySet();
    private static Map<Integer, String[]> renames = Collections.emptyMap(); // id -> {from, to}
    private static Map<Integer, JSONObject> additions = Collections.emptyMap(); // id -> {en, tipo, picto, groups}
    private static Map<Integer, String[]> photos = Collections.emptyMap();      // id -> {from, to}
    private static Map<Integer, JSONObject> newGroups = Collections.emptyMap(); // id -> {en, tipo, picto, fallback, members}
    /** "ALL" group of the starter vocabulary: every picture belongs to it. */
    private static final int ALL_GROUP_ID = 24;

    private ErakaVocabulary() {
    }

    /** Reads the corrections once (safe to call often). */
    public static synchronized void ensureLoaded(Context context) {
        if (loaded || context == null) return;
        try (InputStream in = context.getApplicationContext().getAssets().open(ASSET)) {
            byte[] buf = new byte[in.available()];
            int n = 0;
            while (n < buf.length) {
                int r = in.read(buf, n, buf.length - n);
                if (r < 0) break;
                n += r;
            }
            JSONObject doc = new JSONObject(new String(buf, 0, n, StandardCharsets.UTF_8));
            hidden = idsOf(doc.optJSONObject("hide"));
            caregiverOnly = idsOf(doc.optJSONObject("caregiver_only"));
            Map<Integer, String[]> map = new HashMap<>();
            JSONObject ren = doc.optJSONObject("rename");
            if (ren != null) {
                Iterator<String> keys = ren.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    JSONObject r = ren.optJSONObject(k);
                    if (r == null) continue;
                    try {
                        map.put(Integer.parseInt(k), new String[]{r.optString("from"), r.optString("to")});
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            renames = map;
            Map<Integer, JSONObject> adds = new java.util.LinkedHashMap<>();
            JSONObject add = doc.optJSONObject("add");
            if (add != null) {
                Iterator<String> keys = add.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    JSONObject a = add.optJSONObject(k);
                    if (a == null) continue;
                    try {
                        adds.put(Integer.parseInt(k), a);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            additions = adds;
            Map<Integer, String[]> ph = new HashMap<>();
            JSONObject photo = doc.optJSONObject("photo");
            if (photo != null) {
                Iterator<String> keys = photo.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    JSONObject r = photo.optJSONObject(k);
                    if (r == null) continue;
                    try {
                        ph.put(Integer.parseInt(k), new String[]{r.optString("from"), r.optString("to")});
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            photos = ph;
            Map<Integer, JSONObject> ng = new java.util.LinkedHashMap<>();
            JSONObject addGroups = doc.optJSONObject("add_groups");
            if (addGroups != null) {
                Iterator<String> keys = addGroups.keys();
                while (keys.hasNext()) {
                    String k = keys.next();
                    JSONObject g = addGroups.optJSONObject(k);
                    if (g == null) continue;
                    try {
                        ng.put(Integer.parseInt(k), g);
                    } catch (NumberFormatException ignored) {      // "_about"
                    }
                }
            }
            newGroups = ng;
            Log.d(TAG, "corrections loaded: hide " + hidden.size() + ", caregiver only "
                    + caregiverOnly.size() + ", rename " + renames.size());
        } catch (Exception e) {
            Log.e(TAG, "corrections not loaded: " + e.getMessage());
        }
        loaded = true;
    }

    private static Set<Integer> idsOf(JSONObject obj) {
        Set<Integer> ids = new HashSet<>();
        if (obj == null) return ids;
        Iterator<String> keys = obj.keys();
        while (keys.hasNext()) {
            try {
                ids.add(Integer.parseInt(keys.next()));
            } catch (NumberFormatException ignored) {
            }
        }
        return ids;
    }

    /** Hidden everywhere. */
    public static boolean isHidden(int id) {
        return hidden.contains(id);
    }

    /** Never offered to the child: hidden or caregiver-only. */
    public static boolean isHiddenForChild(int id) {
        return hidden.contains(id) || caregiverOnly.contains(id);
    }

    /**
     * Applies the corrections to the full pictogram list: renames labels (English, only
     * while still OTTAA's original) and removes hidden / caregiver-only pictures from every
     * picture's links ("relacion"), so they never appear on the 4 tiles. Returns the
     * number of changes.
     */
    public static int applyToPictos(Context context, JSONArray pictos) {
        ensureLoaded(context);
        if (pictos == null) return 0;
        int changes = 0;
        for (int i = 0; i < pictos.length(); i++) {
            JSONObject p = pictos.optJSONObject(i);
            if (p == null) continue;
            String[] r = renames.get(p.optInt("id", -1));
            JSONObject texto = p.optJSONObject("texto");
            if (r != null && texto != null && r[0].equalsIgnoreCase(texto.optString("en", "").trim())) {
                try {
                    texto.put("en", r[1]);
                    changes++;
                } catch (Exception ignored) {
                }
            }
            changes += removeLinks(p.optJSONArray("relacion"), true);
            String[] ph = photos.get(p.optInt("id", -1));
            JSONObject imagen = p.optJSONObject("imagen");
            if (ph != null && imagen != null && ph[0].equals(imagen.optString("picto", ""))
                    && imagen.optString("pictoEditado", "").isEmpty() && hasDrawable(context, ph[1])) {
                try {
                    imagen.put("picto", ph[1]);
                    changes++;
                } catch (Exception ignored) {
                }
            }
        }
        changes += addPictos(context, pictos);
        return changes;
    }

    /** Appends the "add" pictograms that are not in the list yet (and whose drawing exists). */
    private static int addPictos(Context context, JSONArray pictos) {
        if (additions.isEmpty()) return 0;
        Set<Integer> have = new HashSet<>();
        for (int i = 0; i < pictos.length(); i++) {
            JSONObject p = pictos.optJSONObject(i);
            if (p != null) have.add(p.optInt("id", -1));
        }
        int added = 0;
        for (Map.Entry<Integer, JSONObject> e : additions.entrySet()) {
            if (have.contains(e.getKey()) || !hasDrawing(context, e.getValue())) continue;
            try {
                JSONObject a = e.getValue();
                JSONObject texto = new JSONObject();
                texto.put("en", a.optString("en"));
                texto.put("es", a.optString("en"));
                JSONObject imagen = new JSONObject();
                imagen.put("picto", a.optString("picto"));
                JSONObject p = new JSONObject();
                p.put("texto", texto);
                p.put("relacion", new JSONArray());
                p.put("tipo", a.optInt("tipo", 2));
                p.put("imagen", imagen);
                p.put("frec", 0);
                p.put("id", e.getKey());
                p.put("gps", 0);
                p.put("agenda", 0);
                pictos.put(p);
                added++;
            } catch (Exception ex) {
                Log.e(TAG, "add " + e.getKey() + ": " + ex.getMessage());
            }
        }
        if (added > 0) Log.d(TAG, "added " + added + " new pictograms");
        return added;
    }

    /** Adds each new pictogram to its groups and to "ALL", once (and only if its drawing exists). */
    private static int addToGroups(Context context, JSONArray groups) {
        if (additions.isEmpty()) return 0;
        int added = 0;
        for (int i = 0; i < groups.length(); i++) {
            JSONObject g = groups.optJSONObject(i);
            if (g == null) continue;
            int gid = g.optInt("id", Integer.MIN_VALUE);
            JSONArray rel = g.optJSONArray("relacion");
            if (rel == null) continue;
            for (Map.Entry<Integer, JSONObject> e : additions.entrySet()) {
                if (!hasDrawing(context, e.getValue())) continue;
                boolean wanted = gid == ALL_GROUP_ID;
                JSONArray targets = e.getValue().optJSONArray("groups");
                for (int t = 0; !wanted && targets != null && t < targets.length(); t++) {
                    wanted = targets.optInt(t, Integer.MIN_VALUE) == gid;
                }
                if (!wanted || contains(rel, e.getKey())) continue;
                try {
                    JSONObject link = new JSONObject();
                    link.put("frec", 1);
                    link.put("id", e.getKey());
                    rel.put(link);
                    added++;
                } catch (Exception ignored) {
                }
            }
        }
        return added;
    }

    private static boolean contains(JSONArray rel, int id) {
        for (int j = 0; j < rel.length(); j++) {
            JSONObject link = rel.optJSONObject(j);
            if (link != null && link.optInt("id", Integer.MIN_VALUE) == id) return true;
        }
        return false;
    }

    private static boolean hasDrawing(Context context, JSONObject add) {
        return hasDrawable(context, add.optString("picto", ""));
    }

    private static boolean hasDrawable(Context context, String name) {
        if (context == null || name == null || name.isEmpty()) return false;
        return context.getResources().getIdentifier(name, "drawable", context.getPackageName()) != 0;
    }

    /**
     * Removes hidden pictures from every group (the gallery, including "ALL"). Caregiver-only
     * pictures stay in the groups so adults can still find them.
     */
    public static int applyToGroups(Context context, JSONArray groups) {
        ensureLoaded(context);
        if (groups == null) return 0;
        int changes = 0;
        for (int i = 0; i < groups.length(); i++) {
            JSONObject g = groups.optJSONObject(i);
            if (g != null) changes += removeLinks(g.optJSONArray("relacion"), false);
        }
        changes += addToGroups(context, groups);
        changes += addGroups(context, groups);
        return changes;
    }

    /**
     * New groups such as "My day" (23 Oct 2026). A group is created once its picture (or the
     * fallback picture) exists. Its members are kept in the listed order - the order of a
     * child's day - with pictures whose drawing is not in the app yet left out until it is;
     * pictures a parent added to the group stay after them.
     */
    private static int addGroups(Context context, JSONArray groups) {
        if (newGroups.isEmpty()) return 0;
        int changes = 0;
        for (Map.Entry<Integer, JSONObject> e : newGroups.entrySet()) {
            int gid = e.getKey();
            JSONObject spec = e.getValue();
            String picto = spec.optString("picto", "");
            if (!hasDrawable(context, picto)) picto = spec.optString("fallback", "");
            if (!hasDrawable(context, picto)) continue;
            try {
                JSONObject group = null;
                for (int i = 0; i < groups.length(); i++) {
                    JSONObject g = groups.optJSONObject(i);
                    if (g != null && g.optInt("id", Integer.MIN_VALUE) == gid) { group = g; break; }
                }
                if (group == null) {
                    group = new JSONObject();
                    JSONObject texto = new JSONObject();
                    texto.put("en", spec.optString("en"));
                    texto.put("es", spec.optString("en"));
                    group.put("texto", texto);
                    group.put("tipo", spec.optInt("tipo", 0));
                    JSONObject imagen = new JSONObject();
                    imagen.put("picto", picto);
                    group.put("imagen", imagen);
                    group.put("id", gid);
                    group.put("frec", 0);
                    group.put("relacion", new JSONArray());
                    // shown first in the group gallery: move it to the front of the list
                    java.util.List<Object> rest = new java.util.ArrayList<>();
                    for (int i = 0; i < groups.length(); i++) rest.add(groups.get(i));
                    for (int i = groups.length() - 1; i >= 0; i--) groups.remove(i);
                    groups.put(group);
                    for (Object o : rest) groups.put(o);
                    changes++;
                } else {
                    JSONObject imagen = group.optJSONObject("imagen");
                    // the real group picture arrived after the group was made with the fallback
                    if (imagen != null && imagen.optString("picto").equals(spec.optString("fallback"))
                            && !picto.equals(imagen.optString("picto"))) {
                        imagen.put("picto", picto);
                        changes++;
                    }
                }
                JSONArray old = group.optJSONArray("relacion");
                if (old == null) old = new JSONArray();
                Map<Integer, JSONObject> links = new java.util.LinkedHashMap<>();
                for (int j = 0; j < old.length(); j++) {
                    JSONObject l = old.optJSONObject(j);
                    if (l != null && !l.isNull("id")) links.put(l.optInt("id"), l);
                }
                JSONArray rel = new JSONArray();
                Set<Integer> placed = new HashSet<>();
                JSONArray members = spec.optJSONArray("members");
                for (int m = 0; members != null && m < members.length(); m++) {
                    int id = members.optInt(m, Integer.MIN_VALUE);
                    if (id == Integer.MIN_VALUE || isHidden(id) || placed.contains(id)) continue;
                    JSONObject add = additions.get(id);
                    if (add != null && !hasDrawing(context, add)) continue;   // its picture is not here yet
                    JSONObject link = links.get(id);
                    if (link == null) {
                        link = new JSONObject();
                        link.put("frec", 1);
                        link.put("id", id);
                    }
                    rel.put(link);
                    placed.add(id);
                }
                for (Map.Entry<Integer, JSONObject> l : links.entrySet()) {   // the parent's own additions
                    if (!placed.contains(l.getKey())) rel.put(l.getValue());
                }
                if (!rel.toString().equals(old.toString())) changes++;
                group.put("relacion", rel);
            } catch (Exception ex) {
                Log.e(TAG, "add group " + gid + ": " + ex.getMessage());
            }
        }
        return changes;
    }

    private static int removeLinks(JSONArray rel, boolean forChild) {
        if (rel == null) return 0;
        int removed = 0;
        for (int j = rel.length() - 1; j >= 0; j--) {
            JSONObject link = rel.optJSONObject(j);
            if (link == null || link.isNull("id")) continue;
            int id = link.optInt("id", -1);
            if (forChild ? isHiddenForChild(id) : isHidden(id)) {
                rel.remove(j);
                removed++;
            }
        }
        return removed;
    }
}
