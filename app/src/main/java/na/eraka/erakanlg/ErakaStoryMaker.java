package na.eraka.erakanlg;

import java.util.Locale;

/**
 * Offline story for the "Tell a story" game (Eraka, 19 Oct 2026).
 *
 * OTTAA sent the four chosen pictures to ChatGPT through a Firebase cloud function, which
 * Eraka does not have (free Spark plan, offline app). This class builds a short, predictable
 * story from fixed templates instead. Pure Java (no Android), so it is unit-tested on the PC.
 *
 * The game's four slots (see TellAStory):
 *   1 who   - people, animals, food, toys     ("dog")
 *   2 what  - colours, describing words, feelings ("happy")
 *   3 does  - actions                          ("play")
 *   4 where - places and shops                 ("park")
 *
 * The same four pictures always give the same story (children with autism often like to
 * hear the same story again); different pictures can pick a different template.
 */
public final class ErakaStoryMaker {

    private ErakaStoryMaker() { }

    static final String[] TEMPLATES = {
            "Once upon a time, there was {a_what} {what} {who}. One day, the {who} wanted to {does}. "
                    + "So the {who} went {to_where} and had lots of fun. The end!",
            "This is {a_what} {what} {who}. The {who} likes to {does}. "
                    + "Today the {who} is {at_where}. Hooray!",
            "Look! {A_what} {what} {who} is {at_where}. "
                    + "The {who} wants to {does}. What a great day. The end!"
    };

    /** Builds the story. Empty slots get a neutral word so the story is never broken. */
    public static String make(String who, String what, String does, String where) {
        String w = clean(who, "friend");
        String a = clean(what, "happy");
        String d = verb(clean(does, "play"));
        String p = clean(where, "park");
        String t = TEMPLATES[Math.floorMod((w + a + d + p).hashCode(), TEMPLATES.length)];
        String article = article(a);
        return t.replace("{a_what}", article)
                .replace("{A_what}", capitalise(article))
                .replace("{what}", a)
                .replace("{who}", w)
                .replace("{does}", d)
                .replace("{to_where}", toPlace(p))
                .replace("{at_where}", atPlace(p));
    }

    /** Lower case, trimmed, no end punctuation; falls back when the slot is empty. */
    static String clean(String s, String fallback) {
        if (s == null) return fallback;
        String c = s.trim().replaceAll("[.!?]+$", "").trim();
        if (c.isEmpty()) return fallback;
        return c.toLowerCase(Locale.ROOT);
    }

    /** "to eat" -> "eat" (the template already says "to"). */
    static String verb(String action) {
        return action.startsWith("to ") ? action.substring(3) : action;
    }

    static String article(String word) {
        return "aeiou".indexOf(word.charAt(0)) >= 0 ? "an" : "a";
    }

    /** "home" -> "home" (went home), otherwise "to the park". */
    static String toPlace(String place) {
        return place.equals("home") ? "home" : "to the " + place;
    }

    /** "home" -> "at home", otherwise "at the park". */
    static String atPlace(String place) {
        return place.equals("home") ? "at home" : "at the " + place;
    }

    static String capitalise(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
