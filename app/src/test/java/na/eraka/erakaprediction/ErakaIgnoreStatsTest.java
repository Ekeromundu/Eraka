package na.eraka.erakaprediction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Unit tests for learning from ignored suggestions (step 5b). Run on the PC. */
public class ErakaIgnoreStatsTest {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long NOW = 1791900000000L;
    private static final int START = ErakaPredictor.START, WANT = 44, WATER = 500, PIZZA = 7, EAT = 118;

    @Test
    public void neverShownMeansNoChange() {
        assertEquals(1.0, new ErakaIgnoreStats().multiplier(WANT, PIZZA, NOW), 1e-12);
    }

    @Test
    public void ignoredFourTimesHalvesTheScore() {
        ErakaIgnoreStats s = new ErakaIgnoreStats();
        for (int i = 0; i < 4; i++) s.record(WANT, new int[]{PIZZA, WATER}, WATER, NOW - i * 1000);
        assertEquals(0.5, s.multiplier(WANT, PIZZA, NOW), 1e-12);   // 1 / (1 + 0.25*4)
        assertTrue(s.multiplier(WANT, WATER, NOW) == 1.0 * 5 / 5);   // taken 4, ignored 0
    }

    @Test
    public void onlyTheSameContextIsAffected() {
        ErakaIgnoreStats s = new ErakaIgnoreStats();
        for (int i = 0; i < 8; i++) s.record(WANT, new int[]{PIZZA}, WATER, NOW);
        assertTrue(s.multiplier(WANT, PIZZA, NOW) < 0.5);
        assertEquals(1.0, s.multiplier(EAT, PIZZA, NOW), 1e-12);    // after "eat" pizza is untouched
        assertEquals(1.0, s.multiplier(START, PIZZA, NOW), 1e-12);
    }

    @Test
    public void takingItAgainRaisesTheScore() {
        ErakaIgnoreStats s = new ErakaIgnoreStats();
        for (int i = 0; i < 4; i++) s.record(WANT, new int[]{PIZZA}, WATER, NOW);
        double before = s.multiplier(WANT, PIZZA, NOW);
        s.record(WANT, new int[]{PIZZA}, PIZZA, NOW);
        assertTrue(s.multiplier(WANT, PIZZA, NOW) > before);
    }

    @Test
    public void oldIgnoresAreForgotten() {
        ErakaIgnoreStats s = new ErakaIgnoreStats();
        for (int i = 0; i < 8; i++) s.record(WANT, new int[]{PIZZA}, WATER, NOW - 20 * DAY);
        assertEquals(1.0, s.multiplier(WANT, PIZZA, NOW), 1e-12);    // older than 14 days
        assertEquals(0, s.ignored(WANT, PIZZA, NOW));
    }
}
