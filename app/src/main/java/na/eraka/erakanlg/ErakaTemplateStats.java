package na.eraka.erakanlg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Learns which sentence templates this child (or caregiver) accepts.
 * Counts are kept per template AND kind of message (Candidate.key, e.g.
 * "want_action@ACTION+THING"), so a choice for "drink water" does not change "go school".
 *
 * Each time a suggested sentence is shown and then accepted (spoken with its own button)
 * or ignored (dismissed with x), the template's counts change. Candidates are ordered by
 *     score = (accepted + 1) / (shown + 2)
 * so a new template starts at 0.5 and the builder's default order decides ties.
 * Pure Java, unit-tested on the PC.
 */
public final class ErakaTemplateStats {

    private final Map<String, int[]> stats = new HashMap<>(); // template -> {shown, accepted}

    public void record(String templateId, boolean accepted) {
        int[] s = stats.get(templateId);
        if (s == null) {
            s = new int[2];
            stats.put(templateId, s);
        }
        s[0]++;
        if (accepted) s[1]++;
    }

    public double score(String templateId) {
        int[] s = stats.get(templateId);
        return s == null ? 0.5 : (s[1] + 1.0) / (s[0] + 2.0);
    }

    /** Stable re-order: better-accepted templates first, default order for ties. */
    public List<ErakaSentenceBuilder.Candidate> order(List<ErakaSentenceBuilder.Candidate> candidates) {
        List<ErakaSentenceBuilder.Candidate> out = new ArrayList<>(candidates);
        java.util.Collections.sort(out, (a, b) -> Double.compare(score(b.key), score(a.key)));
        return out;
    }
}
