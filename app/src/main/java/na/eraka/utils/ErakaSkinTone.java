package na.eraka.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.LruCache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Skin colour of the people in the pictograms (22 Oct 2026).
 *
 * Most bundled pictures come from ARASAAC, which draws every person with one light skin colour
 * (#F5E5DE). ARASAAC itself offers its pictograms in other skin tones by swapping exactly that
 * colour, so Eraka does the same when it loads a picture: the light skin colour (and the pixels
 * where it blends into the black outline or the white background) is replaced by the tone chosen
 * in the child profile. Nothing on disk changes, so the child can switch back at any time.
 *
 *  28 Oct 2026: the five skin shades of the phone keyboard's emojis (Keneke's choice):
 *  light        = ARASAAC's original (#F5E5DE, almost the emoji's #F7DECE)   (default)
 *  medium_light = #F3D2A2
 *  medium       = #D5AB88
 *  medium_dark  = #AF7E57
 *  dark         = #7C533E
 *  mixed        = (kept for phones that saved it before) each picture gets light, medium or dark
 *
 * Real photos (eraka_* except the drawings listed below), avatars and the child's own photos
 * are never changed.
 */
public final class ErakaSkinTone {

    public static final String PREF_TONE = "eraka_skin_tone";
    public static final String LIGHT = "light", MEDIUM = "medium", DARK = "dark", MIXED = "mixed";
    public static final String MEDIUM_LIGHT = "medium_light", MEDIUM_DARK = "medium_dark";
    /** The five choices on the child-profile page, lightest first. */
    public static final String[] CHOICES = {LIGHT, MEDIUM_LIGHT, MEDIUM, MEDIUM_DARK, DARK};
    /** True once the parent tapped a colour; a character chosen later then no longer changes it. */
    private static final String PREF_TONE_PICKED = "eraka_skin_tone_picked";
    /** 28 Oct 2026: nothing chosen = the pictures as ARASAAC draws them (the choice is optional). */
    public static final String DEFAULT = LIGHT;

    private static final int[] SKIN = {0xF5, 0xE5, 0xDE};   // ARASAAC default (white) skin
    private static final int[] TONE_MEDIUM_LIGHT = {0xF3, 0xD2, 0xA2};
    private static final int[] TONE_MEDIUM = {0xD5, 0xAB, 0x88};
    private static final int[] TONE_MEDIUM_DARK = {0xAF, 0x7E, 0x57};
    private static final int[] TONE_DARK = {0x7C, 0x53, 0x3E};

    private static int[] rgbOf(String tone) {
        if (MEDIUM_LIGHT.equals(tone)) return TONE_MEDIUM_LIGHT;
        if (MEDIUM.equals(tone)) return TONE_MEDIUM;
        if (MEDIUM_DARK.equals(tone)) return TONE_MEDIUM_DARK;
        return TONE_DARK;
    }
    private static final String[] MIX = {LIGHT, MEDIUM, DARK};
    /** Pictures Eraka added that are ARASAAC drawings with people in them (the other eraka_ ones
     *  are photos, whose bread or porridge colours must not be touched). */
    private static final Set<String> ERAKA_DRAWINGS = new HashSet<>(java.util.Arrays.asList(
            "eraka_hurt", "eraka_laundry"));
    /**
     * 27 Oct 2026: pictures without people whose own colours happen to match ARASAAC's skin
     * colour (a duck's breast, a plate, a fish). They always keep their own colours. The animals
     * are every picture of the Animals group; the rest were found by scanning all pictures
     * (eraka_base_data/skin_scan).
     */
    private static final Set<String> NOT_PEOPLE = new HashSet<>(java.util.Arrays.asList(
            "abeja", "animales", "asno", "atun", "ballena", "caballo", "cabra", "camello", "canario",
            "cangrejo", "caracol", "cerdo", "conejo", "gallo", "gato", "grillo", "jirafa", "leon",
            "mariposa", "oso", "oveja", "pajarito", "pajaro", "paloma", "pato", "peces", "perro", "rana",
            "ternera", "tigre", "tortuga", "vaca", "vibora",
            "anana", "lata_de_sardinas", "lata_de_atun"));

    private static final LruCache<String, Bitmap> CACHE =
            new LruCache<String, Bitmap>((int) Math.min(Runtime.getRuntime().maxMemory() / 8, 48L << 20)) {
                @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount(); }
            };
    /** Pictures that have no skin in them: not worth looking at again. */
    private static final Set<String> NO_SKIN = Collections.synchronizedSet(new HashSet<>());
    private static String cachedTone;

    private ErakaSkinTone() {}

    public static String tone(Context c) {
        if (cachedTone == null) {
            SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext());
            cachedTone = p.getString(PREF_TONE, DEFAULT);
        }
        return cachedTone;
    }

    public static void setTone(Context c, String tone) {
        PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext())
                .edit().putString(PREF_TONE, tone).apply();
        if (!tone.equals(cachedTone)) CACHE.evictAll();
        cachedTone = tone;
    }

    /**
     * The avatar in the given tone (22 Oct 2026). Each of the 15 avatars exists as ic_avatarNN (the
     * mixed set) and as ic_avatarNN_light / _medium / _dark. "ic_avatar22" with "dark" gives
     * "ic_avatar22_dark"; with "mixed" it gives "ic_avatar22". Other names (own photo) are returned unchanged.
     */
    public static String avatarName(String name, String tone) {
        if (name == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^ic_avatar(\\d\\d)(_light|_medium|_dark)?$").matcher(name);
        if (!m.matches()) return name;
        String base = "ic_avatar" + m.group(1);
        return (tone == null || MIXED.equals(tone)) ? base : base + "_" + tone;
    }

    /**
     * 27 Oct 2026: the avatar screen shows no colour names. Its 15 places show 15 different
     * children, five in each skin colour, spread over the grid; the child (or parent) simply
     * picks the one who looks like them and the pictures follow that avatar's skin colour.
     */
    private static final java.util.Map<String, String> DISPLAY_TONE = new java.util.HashMap<>();
    static {
        for (String n : new String[]{"14", "15", "21", "24", "32"}) DISPLAY_TONE.put(n, LIGHT);
        for (String n : new String[]{"13", "22", "23", "33", "34"}) DISPLAY_TONE.put(n, MEDIUM);
        for (String n : new String[]{"11", "12", "25", "31", "35"}) DISPLAY_TONE.put(n, DARK);
    }

    /** The avatar shown in grid place "avatarNN", e.g. "ic_avatar14_light". */
    public static String displayAvatar(String slot) {
        String nn = slot.replaceAll("\\D", "");
        String tone = DISPLAY_TONE.get(nn);
        return tone == null ? "ic_" + slot : "ic_avatar" + nn + "_" + tone;
    }

    /** Skin of the 15 avatars of the mixed set (ic_avatarNN), see gen_avatars.js. */
    private static final java.util.Map<String, String> MIXED_SET_SKIN = new java.util.HashMap<>();
    static {
        for (String n : new String[]{"24", "32"}) MIXED_SET_SKIN.put(n, LIGHT);
        for (String n : new String[]{"15", "23", "33"}) MIXED_SET_SKIN.put(n, MEDIUM);
    }

    /**
     * The skin tone of an avatar (27 Oct 2026), so the pictures can follow the child's avatar:
     * "ic_avatar22_light" -> light; a mixed-set avatar by its drawing; anything else
     * (own photo, the neutral default) -> null.
     */
    public static String toneOfAvatar(String name) {
        if (name == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("^ic_avatar(\\d\\d)(?:_(light|medium|dark))?$").matcher(name);
        if (!m.matches()) return null;
        if (m.group(2) != null) return m.group(2);
        String t = MIXED_SET_SKIN.get(m.group(1));
        return t != null ? t : DARK;
    }

    /**
     * Eraka 28 Oct 2026: the first recolouring after the app starts took 430 ms on the A13
     * (reading the remembered "no skin" list, starting the picture decoder). Done once in the
     * background when the board opens, before the first pictures are needed.
     */
    public static void warmUp(Context c) {
        if (c == null) return;
        final Context app = c.getApplicationContext();
        new Thread(() -> {
            long t0 = ErakaPerf.start();
            loadNoSkin(app);
            for (String n : new String[]{"ic_hola", "ic_quiero", "ic_mama", "ic_gracias"}) resource(app, n);
            ErakaPerf.end("skin_warmup", t0);
        }, "eraka-skin-warmup").start();
    }

    /** The drawable resource {@code name}, with the chosen skin tone; null if there is no such resource. */
    public static Drawable resource(Context c, String name) {
        if (c == null || name == null || name.isEmpty()) return null;
        int id = c.getResources().getIdentifier(name, "drawable", c.getPackageName());
        if (id == 0) return null;
        // Speed (27 Oct 2026): decide first whether this picture needs a new skin colour at all;
        // only then decode it - at half size, which is still sharper than a tile on the screen -
        // and recolour it. Pictures without people are remembered on the phone (NO_SKIN) so
        // they are never examined again.
        String target = targetTone(c, name);
        if (target == null) return c.getResources().getDrawable(id);
        String key = target + "/" + name;
        Bitmap done = CACHE.get(key);
        if (done != null) return new BitmapDrawable(c.getResources(), done);
        long t0 = ErakaPerf.start();
        android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
        o.inScaled = false;          // the file as it is (500 px), not scaled up or down for the screen
        o.inSampleSize = 2;          // -> 250 px: a quarter of the pixels to recolour and to keep in memory
        Bitmap src = null;
        try {
            src = android.graphics.BitmapFactory.decodeResource(c.getResources(), id, o);
        } catch (Throwable ignored) { }
        if (src == null) return c.getResources().getDrawable(id);   // e.g. a vector drawing
        done = recolour(src, rgbOf(target));
        ErakaPerf.end("skin_recolour", t0, name + (done == null ? " no skin" : ""));
        if (done == null) {
            rememberNoSkin(c, name);
            return c.getResources().getDrawable(id);
        }
        CACHE.put(key, done);
        return new BitmapDrawable(c.getResources(), done);
    }

    /** The tone a picture must be shown in ("medium"/"dark"), or null when it stays as it is. */
    private static String targetTone(Context c, String name) {
        if (name.startsWith("ic_avatar")) return null;
        if (NOT_PEOPLE.contains(name)) return null;
        if (name.startsWith("eraka_") && !name.startsWith("eraka_d_") && !ERAKA_DRAWINGS.contains(name)) return null;
        loadNoSkin(c);
        if (NO_SKIN.contains(name)) return null;
        String tone = tone(c);
        if (MIXED.equals(tone)) tone = MIX[(name.hashCode() & 0x7fffffff) % MIX.length];
        return LIGHT.equals(tone) ? null : tone;
    }

    private static boolean noSkinLoaded;
    private static final String PREF_NO_SKIN = "eraka_skin_none";

    private static synchronized void loadNoSkin(Context c) {
        if (noSkinLoaded) return;
        noSkinLoaded = true;
        NO_SKIN.addAll(PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext())
                .getStringSet(PREF_NO_SKIN, Collections.<String>emptySet()));
    }

    private static synchronized void rememberNoSkin(Context c, String name) {
        if (!NO_SKIN.add(name)) return;
        PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext()).edit()
                .putStringSet(PREF_NO_SKIN, new HashSet<>(NO_SKIN)).apply();
    }

    /** Eraka 28 Oct 2026: true once a skin colour is saved (tapped by the parent or from a character). */
    public static boolean isChosen(Context c) {
        return PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext()).contains(PREF_TONE);
    }

    /** The parent tapped a colour on the child-profile page. */
    public static void pickTone(Context c, String tone) {
        setTone(c, tone);
        PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext())
                .edit().putBoolean(PREF_TONE_PICKED, true).apply();
    }

    /** A character was chosen: the pictures follow it only if the parent has not tapped a colour. */
    public static void setToneFromAvatar(Context c, String tone) {
        if (tone == null) return;
        if (PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext())
                .getBoolean(PREF_TONE_PICKED, false)) return;
        setTone(c, tone);
    }

    /**
     * Eraka 28 Oct 2026: a picture in a given tone, whatever tone is chosen now - for the example
     * tiles on the avatar page ("Which pictures look most like you?").
     */
    public static Drawable example(Context c, String name, String tone) {
        int id = c.getResources().getIdentifier(name, "drawable", c.getPackageName());
        if (id == 0) return null;
        if (tone == null || LIGHT.equals(tone)) return c.getResources().getDrawable(id);
        String key = "example/" + tone + "/" + name;
        Bitmap done = CACHE.get(key);
        if (done == null) {
            android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
            o.inScaled = false;
            o.inSampleSize = 2;
            Bitmap src = android.graphics.BitmapFactory.decodeResource(c.getResources(), id, o);
            if (src == null) return c.getResources().getDrawable(id);
            done = recolour(src, rgbOf(tone));
            if (done == null) return c.getResources().getDrawable(id);
            CACHE.put(key, done);
        }
        return new BitmapDrawable(c.getResources(), done);
    }

    /**
     * Recolours every ImageView under {@code root} whose tag is "skin:&lt;drawable name&gt;"
     * (24 Oct 2026): the ARASAAC drawings on the games screen and in the "add a picture" window
     * follow the chosen skin tone like the board pictures.
     */
    public static void applyTagged(android.view.View root) {
        if (root == null) return;
        Object tag = root.getTag();
        if (root instanceof android.widget.ImageView && tag instanceof String && ((String) tag).startsWith("skin:")) {
            Drawable d = resource(root.getContext(), ((String) tag).substring(5));
            if (d != null) ((android.widget.ImageView) root).setImageDrawable(d);
        }
        if (root instanceof android.view.ViewGroup) {
            android.view.ViewGroup g = (android.view.ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) applyTagged(g.getChildAt(i));
        }
    }

    /** Recolours a bundled picture (by resource name) if a non-light tone is chosen. */
    public static Drawable apply(Context c, String name, Drawable d) {
        if (c == null || name == null || !(d instanceof BitmapDrawable)) return d;
        if (name.startsWith("ic_avatar")) return d;
        // eraka_d_* are ARASAAC drawings (23 Oct 2026 on); other eraka_* pictures are real photos
        if (name.startsWith("eraka_") && !name.startsWith("eraka_d_") && !ERAKA_DRAWINGS.contains(name)) return d;
        String tone = tone(c);
        if (MIXED.equals(tone)) tone = MIX[(name.hashCode() & 0x7fffffff) % MIX.length];
        if (LIGHT.equals(tone) || NO_SKIN.contains(name)) return d;
        int[] target = rgbOf(tone);
        String key = tone + "/" + name;
        Bitmap done = CACHE.get(key);
        if (done == null) {
            Bitmap src = ((BitmapDrawable) d).getBitmap();
            if (src == null) return d;
            done = recolour(src, target);
            if (done == null) { rememberNoSkin(c, name); return d; }
            CACHE.put(key, done);
        }
        return new BitmapDrawable(c.getResources(), done);
    }

    /** Returns a recoloured copy, or null when the picture has no skin colour in it. */
    static Bitmap recolour(Bitmap src, int[] t) {
        int w = src.getWidth(), h = src.getHeight();
        int[] px = new int[w * h];
        src.getPixels(px, 0, w, 0, 0, w, h);
        int core = recolourPixels(px, w, h, t);
        if (core < Math.max(8, w * h / 8000)) return null;   // a few stray pixels: leave it
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        return out;
    }

    private static final int NONE = 0, CORE = 1, EDGE = 2;

    /**
     * Pure pixel work (no Android classes), so it can be unit-tested. Returns the number of
     * real skin pixels found (0 = leave the picture alone).
     *
     * 27 Oct 2026, second version: only the skin colour itself counts as skin ("core"). Its
     * blends into the black outline or the white background ("edge") are recoloured only within
     * 2 pixels of real skin. Before, dark warm greys (a black cat's fur) and pale beiges anywhere
     * in a picture looked like skin blended into the outline and turned brown.
     */
    static int recolourPixels(int[] px, int w, int h, int[] t) {
        final int s0 = SKIN[0], s1 = SKIN[1], s2 = SKIN[2], sum = s0 + s1 + s2;
        final int n = px.length;
        byte[] kind = new byte[n];
        int[] newRgb = new int[n];
        // each colour is worked out once (a drawing has few colours)
        final int[] seenRgb = new int[4096], seenKind = new int[4096], seenOut = new int[4096];
        java.util.Arrays.fill(seenRgb, -1);
        int cores = 0;
        for (int i = 0; i < n; i++) {
            int p = px[i];
            if ((p >>> 24) == 0) continue;
            int rgb = p & 0xffffff, slot = (rgb * 0x9E3779B1) >>> 20;
            if (seenRgb[slot] != rgb) {
                int[] res = classify(rgb, t, s0, s1, s2, sum);
                seenRgb[slot] = rgb; seenKind[slot] = res[0]; seenOut[slot] = res[1];
            }
            kind[i] = (byte) seenKind[slot];
            newRgb[i] = seenOut[slot];
            if (kind[i] == CORE) cores++;
        }
        if (cores == 0) return 0;
        // "near real skin": grow the skin area by 2 pixels (4-neighbour steps)
        boolean[] near = new boolean[n];
        for (int i = 0; i < n; i++) near[i] = kind[i] == CORE;
        for (int step = 0; step < 2; step++) {
            boolean[] grown = near.clone();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int i = y * w + x;
                    if (!near[i]) continue;
                    if (x > 0) grown[i - 1] = true;
                    if (x < w - 1) grown[i + 1] = true;
                    if (y > 0) grown[i - w] = true;
                    if (y < h - 1) grown[i + w] = true;
                }
            }
            near = grown;
        }
        for (int i = 0; i < n; i++) {
            if (kind[i] == CORE || (kind[i] == EDGE && near[i])) px[i] = (px[i] & 0xff000000) | newRgb[i];
        }
        return cores;
    }

    /** {kind, new colour} for one colour: CORE = the skin colour, EDGE = its blend into outline or background. */
    private static int[] classify(int rgbIn, int[] t, int s0, int s1, int s2, int sum) {
        int r = (rgbIn >> 16) & 0xff, g = (rgbIn >> 8) & 0xff, b = rgbIn & 0xff;
        int rgb = r + g + b;
        // skin, or skin blended into the black outline: p = k * skin
        float k = rgb / (float) sum;
        if (k > 0.2f && k <= 1.03f) {
            float tol = 7f * Math.max(k, 0.5f);
            if (Math.abs(r - k * s0) <= tol && Math.abs(g - k * s1) <= tol && Math.abs(b - k * s2) <= tol) {
                int out = (clamp(k * t[0]) << 16) | (clamp(k * t[1]) << 8) | clamp(k * t[2]);
                return new int[]{k >= 0.85f ? CORE : EDGE, out};
            }
        }
        // skin blended into a white background: p = skin + u * (white - skin)
        float u = (rgb - sum) / (float) (765 - sum);
        if (u > 0f && u < 0.85f
                && Math.abs(r - (s0 + u * (255 - s0))) <= 7
                && Math.abs(g - (s1 + u * (255 - s1))) <= 7
                && Math.abs(b - (s2 + u * (255 - s2))) <= 7) {
            int out = (clamp(t[0] + u * (255 - t[0])) << 16)
                    | (clamp(t[1] + u * (255 - t[1])) << 8) | clamp(t[2] + u * (255 - t[2]));
            return new int[]{EDGE, out};
        }
        return new int[]{NONE, 0};
    }

    private static int clamp(float v) {
        int x = Math.round(v);
        return x < 0 ? 0 : (x > 255 ? 255 : x);
    }
}
