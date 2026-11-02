package na.eraka.utils;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * What the child profile form ("Let's get to know you") stores, and how the rest of Eraka
 * reads it (21 Oct 2026). Everything stays on the phone; nothing here is uploaded.
 *
 *  - communication level: its own setting (it used to be saved in OTTAA's "gender" field,
 *    which nothing read, and was lost when the profile was edited);
 *  - interests: food, animals, family, music, outdoors, school. They steer the "new picture"
 *    places of the suggestion row towards the groups the child enjoys;
 *  - age in whole years, for the exported study data (the birth date itself is not exported).
 */
public final class ErakaChildProfile {

    public static final String PREF_COMM_LEVEL = "eraka_comm_level";
    public static final String PREF_INTERESTS = "eraka_interests";      // e.g. "food,animals"
    /** Birth date in ms, stored by the form under OTTAA's key. */
    public static final String PREF_BIRTH_DATE = na.eraka.utils.constants.Constants.FECHACUMPLE;

    /** Interest keys, in the order of the chips on the form. */
    public static final List<String> INTERESTS = Collections.unmodifiableList(Arrays.asList(
            "food", "animals", "family", "music", "outdoors", "school"));

    /**
     * Words young children usually learn early, per interest, most familiar first (informed by
     * early-vocabulary inventories such as the MacArthur-Bates CDI and by the parents' messages).
     * Pictures in an interest group are offered in this order; the rest follow in the group's
     * stored order, which is OTTAA's Spanish alphabetical order (abeja = bee first).
     * Labels missing from the vocabulary are simply skipped.
     */
    public static final java.util.Map<String, List<String>> FAMILIAR;
    static {
        java.util.Map<String, List<String>> m = new java.util.HashMap<>();
        m.put("food", Arrays.asList("milk", "water", "juice", "banana", "apple", "bread", "cookie", "chips", "chicken",
                "rice", "egg", "yoghurt", "porridge", "oshifima", "cereal", "meat", "fish", "pasta", "cheese", "orange",
                "cake", "soup", "vetkoek", "biltong", "muffin", "noodles", "cooldrink", "tea", "sausage"));
        m.put("animals", Arrays.asList("dog", "cat", "cow", "chicken", "goat", "bird", "fish", "horse", "donkey",
                "duck", "pig", "sheep", "elephant", "lion", "giraffe", "rabbit", "frog", "butterfly"));
        m.put("family", Arrays.asList("mom", "dad", "baby", "brother", "sister", "grandma", "grandpa", "friends",
                "teacher", "boy", "girl"));
        m.put("music", Arrays.asList("sing", "music", "drum", "guitar", "piano", "dance", "listen"));
        m.put("outdoors", Arrays.asList("park", "playground", "ball", "car", "bus", "taxi", "garden", "house", "shop",
                "tree", "sun"));
        m.put("school", Arrays.asList("teacher", "book", "pencil", "draw", "read", "write", "bag", "paint", "friends",
                "play"));
        FAMILIAR = Collections.unmodifiableMap(m);
    }

    /** Which interest a group belongs to (first match), or null. */
    public static String interestOfGroup(int groupId, String groupName, Set<String> interests) {
        if (interests == null) return null;
        for (String k : INTERESTS) {
            if (interests.contains(k) && groupMatches(groupId, groupName, Collections.singleton(k))) return k;
        }
        return null;
    }

    private ErakaChildProfile() { }

    public static String commLevel(SharedPreferences p) {
        return p == null ? "" : p.getString(PREF_COMM_LEVEL, "");
    }

    public static Set<String> interests(SharedPreferences p) {
        Set<String> out = new LinkedHashSet<>();
        if (p == null) return out;
        for (String s : p.getString(PREF_INTERESTS, "").split(",")) {
            String k = s.trim().toLowerCase(Locale.ROOT);
            if (INTERESTS.contains(k)) out.add(k);
        }
        return out;
    }

    public static void save(SharedPreferences p, String commLevel, Set<String> interests) {
        List<String> ordered = new ArrayList<>();
        for (String k : INTERESTS) if (interests != null && interests.contains(k)) ordered.add(k);
        p.edit().putString(PREF_COMM_LEVEL, commLevel == null ? "" : commLevel)
                .putString(PREF_INTERESTS, android.text.TextUtils.join(",", ordered))
                .apply();
    }

    /** Whole years from a birth date in ms; -1 when unknown. */
    public static int ageYears(long birthMs) {
        if (birthMs <= 0) return -1;
        Calendar now = Calendar.getInstance();
        Calendar birth = Calendar.getInstance();
        birth.setTimeInMillis(birthMs);
        int age = now.get(Calendar.YEAR) - birth.get(Calendar.YEAR);
        if (now.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) age--;
        return age;
    }

    /**
     * Does a group (by its id or English name) belong to one of these interests? Group ids are
     * those of the starter vocabulary; names cover renamed or caregiver-made groups.
     */
    public static boolean groupMatches(int groupId, String groupName, Set<String> interests) {
        if (interests == null || interests.isEmpty()) return false;
        String n = groupName == null ? "" : groupName.toLowerCase(Locale.ROOT).trim();
        for (String k : interests) {
            switch (k) {
                case "food":
                    if (groupId == 3 || n.equals("food") || n.equals("breakfast") || n.equals("lunch")
                            || n.equals("snack") || n.equals("dinner") || n.equals("drinks")) return true;
                    break;
                case "animals":
                    if (groupId == 2 || n.equals("animals")) return true;
                    break;
                case "family":
                    if (groupId == 17 || n.equals("people") || n.equals("family")) return true;
                    break;
                case "music":
                    if (groupId == 10 || groupId == 34 || n.equals("music instruments") || n.equals("music")
                            || n.equals("music lesson")) return true;
                    break;
                case "outdoors":
                    if (groupId == 13 || groupId == 14 || groupId == 23 || n.equals("places") || n.equals("nature")
                            || n.equals("transport") || n.equals("outdoors")) return true;
                    break;
                case "school":
                    if (groupId == 5 || n.equals("school")) return true;
                    break;
                default:
                    break;
            }
        }
        return false;
    }
}
