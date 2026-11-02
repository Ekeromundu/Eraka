package na.eraka.erakaprediction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Exploration slots for the suggestion row (step 5a).
 *
 * The first slots of the row show what the model is confident about (exploitation).
 * The last {@link #EXPLORE_SLOTS} slots show pictures the child has not (or hardly)
 * used yet, so new words get a chance to be discovered (exploration).
 *
 * Which new pictures? A UCB score (Upper Confidence Bound, Auer et al. 2002):
 *
 *   score(w) = (picks(w) + 1) / (shows(w) + 2)                  how often it was taken when shown
 *            + C * sqrt( ln(decisions + 2) / (shows(w) + 1) )   bonus for pictures rarely shown
 *
 *  - shows(w):  how often w was in an exploration slot when the child tapped something
 *  - picks(w):  how often the child then tapped w
 *  - decisions: how many taps happened while exploration slots were on screen
 *
 * A picture never shown has the largest bonus, so new pictures take turns (rotation).
 * A picture shown often and never taken slowly loses its place. A picture the child takes
 * keeps a high first part; once used it is learned by the n-gram model and moves to the
 * normal slots. Ties keep the candidates' order (context first, then base model, then
 * core words), so the choice is predictable and explainable.
 *
 * Pure Java (no Android), unit-tested on the PC.
 */
public final class ErakaExplorer {

    /** How many of the row's slots are used for exploration (Keneke, 13 Oct 2026: two). */
    public static final int EXPLORE_SLOTS = 3;   // 22 Oct 2026: 3 of the row's 6 places (was 2), Keneke
    /** Strength of the exploration bonus (standard UCB1 uses about 1.4; tunable). */
    public static double C = 1.0;
    /** A picture the child has used this many times is no longer "new". 21 Oct 2026: 1 (was 3),
     *  so the exploration slots only show pictures the child has never chosen (Keneke's test:
     *  with 3, pictures used once or twice came back and looked like old favourites). */
    public static final int NEW_UNTIL_USES = 1;

    private final Map<Integer, int[]> stats = new HashMap<>(); // id -> {shows, picks}
    private int decisions = 0;

    /** Records one tap made while these exploration pictures were on screen. */
    public void recordDecision(int[] shownExplore, int tappedId) {
        if (shownExplore == null || shownExplore.length == 0) return;
        decisions++;
        for (int id : shownExplore) {
            int[] s = stats.get(id);
            if (s == null) {
                s = new int[2];
                stats.put(id, s);
            }
            s[0]++;
            if (id == tappedId) s[1]++;
        }
    }

    public double score(int id) {
        int[] s = stats.get(id);
        int shows = s == null ? 0 : s[0];
        int picks = s == null ? 0 : s[1];
        double mean = (picks + 1.0) / (shows + 2.0);
        double bonus = C * Math.sqrt(Math.log(decisions + 2.0) / (shows + 1.0));
        return mean + bonus;
    }

    /**
     * Picks up to k exploration pictures.
     *
     * @param candidates pictures to choose from, in order of preference (duplicates ignored)
     * @param exclude    pictures that must not appear (tiles, other slots, the message)
     * @param timesUsed  how often the child has used each picture (missing = 0)
     */
    public List<Integer> pick(Collection<Integer> candidates, Set<Integer> exclude,
                              Map<Integer, Integer> timesUsed, int k) {
        List<Integer> pool = new ArrayList<>();
        for (Integer id : new LinkedHashSet<>(candidates)) {
            if (id == null || id < 0 || (exclude != null && exclude.contains(id))) continue;
            Integer used = timesUsed == null ? null : timesUsed.get(id);
            if (used != null && used >= NEW_UNTIL_USES) continue; // not new any more
            pool.add(id);
        }
        // stable sort: equal scores keep the candidates' order
        java.util.Collections.sort(pool, (a, b) -> Double.compare(score(b), score(a)));
        return pool.size() > k ? new ArrayList<>(pool.subList(0, k)) : pool;
    }

    public int decisions() {
        return decisions;
    }

    public int shows(int id) {
        int[] s = stats.get(id);
        return s == null ? 0 : s[0];
    }

    public int picks(int id) {
        int[] s = stats.get(id);
        return s == null ? 0 : s[1];
    }
}
