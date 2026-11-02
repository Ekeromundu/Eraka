package na.eraka.erakaprediction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/** Unit tests for the exploration slots (step 5a). Run on the PC like ErakaPredictorTest. */
public class ErakaExplorerTest {

    private static final List<Integer> CANDIDATES = Arrays.asList(10, 11, 12, 13, 14);

    @Test
    public void newChildGetsTheFirstCandidatesInOrder() {
        ErakaExplorer e = new ErakaExplorer();
        // nothing shown yet: all scores equal, so the preference order is kept
        assertEquals(Arrays.asList(10, 11), e.pick(CANDIDATES, null, null, 2));
    }

    @Test
    public void ignoredPicturesMakeRoomForOthers() {
        ErakaExplorer e = new ErakaExplorer();
        e.recordDecision(new int[]{10, 11}, 99);   // shown, child tapped something else
        // 10 and 11 were shown once and not taken: never-shown 12 and 13 come first now
        assertEquals(Arrays.asList(12, 13), e.pick(CANDIDATES, null, null, 2));
        e.recordDecision(new int[]{12, 13}, 99);
        e.recordDecision(new int[]{14, 10}, 99);
        // every picture had a turn; the least-shown ones come back first (rotation)
        List<Integer> next = e.pick(CANDIDATES, null, null, 2);
        assertFalse(next.contains(10));            // 10 was shown twice
        assertEquals(3, e.decisions());
    }

    @Test
    public void takenPictureScoresHigherThanIgnoredOne() {
        ErakaExplorer e = new ErakaExplorer();
        e.recordDecision(new int[]{10, 11}, 11);   // child took 11
        assertEquals(1, e.picks(11));
        assertTrue(e.score(11) > e.score(10));
    }

    @Test
    public void excludedAndWellKnownPicturesAreSkipped() {
        ErakaExplorer e = new ErakaExplorer();
        Map<Integer, Integer> used = new HashMap<>();
        used.put(10, ErakaExplorer.NEW_UNTIL_USES);  // the child already uses 10: not new
        used.put(11, 1);                              // used once: not new any more (21 Oct 2026)
        List<Integer> picked = e.pick(CANDIDATES, new HashSet<>(Collections.singletonList(12)), used, 2);
        assertEquals(Arrays.asList(13, 14), picked);
    }

    @Test
    public void duplicatesAndNegativeIdsAreIgnored() {
        ErakaExplorer e = new ErakaExplorer();
        assertEquals(Arrays.asList(5, 6), e.pick(Arrays.asList(-1, 5, 5, 6, 6), null, null, 3));
    }
}
