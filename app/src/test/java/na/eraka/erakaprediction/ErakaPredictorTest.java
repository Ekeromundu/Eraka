package na.eraka.erakaprediction;

import static na.eraka.erakaprediction.ErakaPredictor.START;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import na.eraka.erakaprediction.ErakaPredictor.Query;
import na.eraka.erakaprediction.ErakaPredictor.Suggestion;
import na.eraka.erakaprediction.ErakaPredictor.Tap;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for the Eraka prediction model. They run on the PC (no phone needed):
 * right-click this file in Android Studio -> Run 'ErakaPredictorTest'.
 */
public class ErakaPredictorTest {

    private static final long DAY = 24L * 60 * 60 * 1000;
    private static final long NOW = 1791303768000L; // 6 Oct 2026, 17:00 (the first real tap)

    // picture ids for the hand-made examples
    private static final int I = 1, WANT = 2, WATER = 3, JUICE = 4;

    /** Adds one message (a list of picture ids) as taps, like ErakaUsageLog records them. */
    private static void message(List<Tap> taps, long ts, int hour, String place, int... ids) {
        for (int i = 0; i < ids.length; i++) {
            int prev1 = i >= 1 ? ids[i - 1] : START;
            int prev2 = i >= 2 ? ids[i - 2] : START;
            taps.add(new Tap(ids[i], prev1, prev2, ts + i * 1000, hour, place, false));
        }
    }

    private static double sum(Map<Integer, Double> probs) {
        double s = 0;
        for (double p : probs.values()) s += p;
        return s;
    }

    // ------------------------------------------------------------------

    /** The textbook example: "I want water" twice, "I want juice" once. */
    @Test
    public void afterIWant_waterBeatsJuice_twoThirdsVsOneThird() {
        List<Tap> taps = new ArrayList<>();
        message(taps, NOW, 17, "HOME", I, WANT, WATER);
        message(taps, NOW + 60_000, 17, "HOME", I, WANT, JUICE);
        message(taps, NOW + 120_000, 17, "HOME", I, WANT, WATER);
        ErakaPredictor model = new ErakaPredictor();
        model.train(taps);

        Query afterIWant = new Query(WANT, I, 17, "HOME");
        List<Suggestion> top = model.rank(afterIWant, null, null, 4);
        System.out.println("After 'I want': " + top);
        System.out.println(model.explain(afterIWant, WATER));
        System.out.println(model.explain(afterIWant, JUICE));

        assertEquals(WATER, top.get(0).pictoId);
        assertEquals(JUICE, top.get(1).pictoId);
        // the trigram part alone is exactly 2/3 for water (taps are minutes apart, so recency barely matters)
        assertTrue(model.explain(afterIWant, WATER).contains("P3=0.667"));
        assertTrue(model.explain(afterIWant, JUICE).contains("P3=0.333"));
        assertEquals(1.0, sum(model.probabilities(afterIWant)), 1e-9);
    }

    /** Keneke's real log from 6 Oct 2026 (12 taps, 4 messages; 662 and 30 were undone). */
    @Test
    public void realLog_after643and44_suggests118() {
        List<Tap> taps = new ArrayList<>();
        message(taps, NOW, 17, "HOME", 643, 44, 118, 482);
        message(taps, NOW + 20_000, 17, "HOME", 628, 14, 427);
        message(taps, NOW + 35_000, 17, "HOME", 377, 378);
        taps.add(new Tap(44, START, START, NOW + 45_000, 17, "HOME", false));
        taps.add(new Tap(30, 44, START, NOW + 48_000, 17, "HOME", true));   // undone
        taps.add(new Tap(662, 30, 44, NOW + 50_000, 17, "HOME", true));     // undone
        ErakaPredictor model = new ErakaPredictor();
        model.train(taps);

        Query q = new Query(44, 643, 17, "HOME");
        List<Suggestion> top = model.rank(q, null, null, 4);
        System.out.println("Real log, after 643 44: " + top);
        System.out.println(model.explain(q, 118));
        assertEquals(118, top.get(0).pictoId);
        assertEquals(10, model.tapsUsed()); // 12 taps minus the 2 undone
    }

    /** Undone taps are mistakes: the model must not learn them. */
    @Test
    public void undoneTaps_areIgnored() {
        List<Tap> taps = new ArrayList<>();
        message(taps, NOW, 17, "HOME", I, WANT, WATER);
        taps.add(new Tap(JUICE, WANT, I, NOW + 5_000, 17, "HOME", true));
        ErakaPredictor model = new ErakaPredictor();
        model.train(taps);
        assertFalse(model.probabilities(new Query(WANT, I, 17, "HOME")).containsKey(JUICE));
    }

    /** Recent taps count more: juice was popular two months ago, water is today's word. */
    @Test
    public void recency_recentUseBeatsOldUse() {
        List<Tap> taps = new ArrayList<>();
        for (int i = 0; i < 3; i++) message(taps, NOW - 60 * DAY + i * 60_000, 17, "HOME", I, WANT, JUICE);
        message(taps, NOW, 17, "HOME", I, WANT, WATER);
        ErakaPredictor model = new ErakaPredictor(); // half-life 14 days
        model.train(taps);

        Query q = new Query(WANT, I, 17, "HOME");
        List<Suggestion> top = model.rank(q, null, null, 2);
        System.out.println("Recency: " + top);
        assertEquals(WATER, top.get(0).pictoId); // 1 new tap > 3 taps that are 60 days old
    }

    /** Place matters when the sequence gives no clue: at school, "teacher"; at home, "mom". */
    @Test
    public void place_changesTheFirstPicture() {
        final int TEACHER = 20, MOM = 21;
        List<Tap> taps = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            message(taps, NOW + i * 60_000, 9, "SCHOOL", TEACHER, WATER);
            message(taps, NOW + i * 60_000 + 30_000, 9, "HOME", MOM, WATER);
        }
        ErakaPredictor model = new ErakaPredictor();
        model.train(taps);

        Query startAtSchool = new Query(START, START, 9, "SCHOOL");
        Query startAtHome = new Query(START, START, 9, "HOME");
        assertEquals(TEACHER, model.rank(startAtSchool, null, null, 1).get(0).pictoId);
        assertEquals(MOM, model.rank(startAtHome, null, null, 1).get(0).pictoId);
    }

    /** A brand-new child: no history and no prior gives an empty list (the app falls back to OTTAA), never a crash. */
    @Test
    public void coldStart_noData_isEmptyNotCrash() {
        ErakaPredictor model = new ErakaPredictor();
        assertTrue(model.rank(new Query(START, START, 9, "HOME"), null, null, 4).isEmpty());
    }

    /** A brand-new child with a prior (e.g. core words) gets the prior's order; real taps then take over. */
    @Test
    public void coldStart_priorFirst_thenRealTapsTakeOver() {
        Map<Integer, Double> prior = new HashMap<>();
        prior.put(WANT, 3.0);
        prior.put(I, 1.0);
        ErakaPredictor model = new ErakaPredictor();
        model.setPrior(prior);
        Query start = new Query(START, START, 9, "HOME");
        assertEquals(WANT, model.rank(start, null, null, 1).get(0).pictoId);

        List<Tap> taps = new ArrayList<>();
        for (int i = 0; i < 5; i++) message(taps, NOW + i * 60_000, 9, "HOME", I, WATER);
        model.train(taps);
        assertEquals(I, model.rank(start, null, null, 1).get(0).pictoId);
    }

    /** Candidates and exclusions are respected (e.g. only this group, never what is already on screen). */
    @Test
    public void rank_respectsCandidatesAndExclusions() {
        List<Tap> taps = new ArrayList<>();
        message(taps, NOW, 17, "HOME", I, WANT, WATER);
        message(taps, NOW + 60_000, 17, "HOME", I, WANT, JUICE);
        ErakaPredictor model = new ErakaPredictor();
        model.train(taps);
        Query q = new Query(WANT, I, 17, "HOME");

        List<Integer> onScreen = new ArrayList<>();
        onScreen.add(WATER);
        for (Suggestion s : model.rank(q, null, onScreen, 4)) assertTrue(s.pictoId != WATER);

        List<Integer> group = new ArrayList<>();
        group.add(JUICE);
        group.add(I);
        for (Suggestion s : model.rank(q, group, null, 4)) assertTrue(s.pictoId == JUICE || s.pictoId == I);
    }

    @Test
    public void timeBuckets_matchOttaaBoundaries() {
        assertEquals("MORNING", ErakaPredictor.timeBucket(5));
        assertEquals("MIDDAY", ErakaPredictor.timeBucket(12));
        assertEquals("AFTERNOON", ErakaPredictor.timeBucket(17));
        assertEquals("EVENING", ErakaPredictor.timeBucket(22));
        assertEquals("EVENING", ErakaPredictor.timeBucket(2));
    }

    // ------------------------------------------------------------------ base model (7 Oct)

    private static ErakaPredictor baseFrom(String csv) throws Exception {
        return ErakaBaseModel.load(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    private static final String BASE_CSV =
            "# test\n"
            + "source,time,place,picto_ids,text\n"
            + "parent,MORNING,HOME,1 2 3,I want water\n"
            + "parent,MORNING,HOME,1 2 3,I want water\n"
            + "teacher,,SCHOOL,1 2 4,I want juice, please\n";

    @Test
    public void baseModelParsesMessagesIntoTapSequences() throws Exception {
        List<Tap> taps = ErakaBaseModel.parse(new ByteArrayInputStream(BASE_CSV.getBytes(StandardCharsets.UTF_8)));
        assertEquals(9, taps.size());
        Tap third = taps.get(2);              // "water" after "I want"
        assertEquals(WATER, third.pictoId);
        assertEquals(WANT, third.prev1);
        assertEquals(I, third.prev2);
        assertEquals(8, third.hour);          // MORNING -> 8
        assertEquals(START, taps.get(3).prev1); // next message starts fresh
        assertEquals(-1, taps.get(6).hour);   // no time given
        assertEquals("SCHOOL", taps.get(6).place);
    }

    @Test
    public void newChildGetsTheBaseModelOnly() throws Exception {
        ErakaPredictor child = new ErakaPredictor();
        child.setBaseModel(baseFrom(BASE_CSV));
        assertEquals(1.0, child.baseShare(), 1e-9);
        Query q = new Query(WANT, I, 9, "HOME");
        List<Suggestion> top = child.rank(q, null, null, 2);
        assertEquals(WATER, top.get(0).pictoId); // 2 of 3 starter messages say water
        assertEquals(1.0, sum(child.probabilities(q)), 1e-9);
    }

    @Test
    public void childsOwnHabitTakesOverFromTheBaseModel() throws Exception {
        ErakaPredictor.Config cfg = new ErakaPredictor.Config();
        cfg.baseStrength = 30;
        ErakaPredictor child = new ErakaPredictor(cfg);
        child.setBaseModel(baseFrom(BASE_CSV));
        Query q = new Query(WANT, I, 9, "HOME");

        List<Tap> taps = new ArrayList<>();
        message(taps, NOW, 9, "HOME", I, WANT, JUICE);       // 3 taps: base still leads
        child.train(taps);
        assertEquals(30.0 / 33.0, child.baseShare(), 1e-9);
        assertEquals(WATER, child.rank(q, null, null, 1).get(0).pictoId);

        for (int d = 1; d <= 20; d++) {                       // 60 more taps of juice
            List<Tap> more = new ArrayList<>();
            message(more, NOW + d * 1000L * 60, 9, "HOME", I, WANT, JUICE);
            for (Tap t : more) child.addTap(t);
        }
        assertTrue(child.baseShare() < 0.35);
        assertEquals(JUICE, child.rank(q, null, null, 1).get(0).pictoId);
        assertEquals(1.0, sum(child.probabilities(q)), 1e-9);
    }

    @Test
    public void unknownTimeAndPlaceAreNotCounted() throws Exception {
        ErakaPredictor base = baseFrom("source,time,place,picto_ids,text\nteacher,,,5 6,x\n");
        // no time/place tables: probabilities still sum to 1 at any hour and place
        assertEquals(1.0, sum(base.probabilities(new Query(START, START, 22, "PARK"))), 1e-9);
    }

    @Test
    public void emptyBaseFileGivesNoBaseModel() throws Exception {
        assertEquals(null, baseFrom("source,time,place,picto_ids,text\n"));
        ErakaPredictor child = new ErakaPredictor();
        child.setBaseModel(null);
        assertFalse(child.hasBaseModel());
        assertEquals(0.0, child.baseShare(), 1e-9);
    }

    @Test
    public void repeatedTapsAreLearnedOnce() {
        // "I want water water water" must count the same as "I want water"
        List<Tap> spam = new ArrayList<>();
        spam.add(new Tap(I, START, START, NOW, 17, "HOME", false));
        spam.add(new Tap(WANT, I, START, NOW + 1_000, 17, "HOME", false));
        spam.add(new Tap(WATER, WANT, I, NOW + 2_000, 17, "HOME", false));
        spam.add(new Tap(WATER, WATER, WANT, NOW + 3_000, 17, "HOME", false, true)); // repeat
        spam.add(new Tap(WATER, WATER, WATER, NOW + 4_000, 17, "HOME", false, true)); // repeat
        List<Tap> once = new ArrayList<>();
        message(once, NOW, 17, "HOME", I, WANT, WATER);

        ErakaPredictor a = new ErakaPredictor();
        a.train(spam);
        ErakaPredictor b = new ErakaPredictor();
        b.train(once);
        assertEquals(3, a.tapsUsed());
        Query q = new Query(WANT, I, 17, "HOME");
        assertEquals(b.probabilities(q).get(WATER), a.probabilities(q).get(WATER), 1e-12);
        // and water does not become the favourite after "water"
        assertTrue(a.probabilities(new Query(WATER, WANT, 17, "HOME")).getOrDefault(WATER, 0.0)
                <= b.probabilities(new Query(WATER, WANT, 17, "HOME")).getOrDefault(WATER, 0.0) + 1e-12);
    }

    @Test
    public void findRepeatsFollowsWhatIsOnScreen() {
        // message 1: water, water (repeat), undo, water again at the freed position (not a repeat)
        // message 2: water (new message, not a repeat)
        long[] session = {1, 1, 1, 2};
        int[] position = {0, 1, 1, 0};
        int[] picto = {WATER, WATER, WATER, WATER};
        boolean[] r = ErakaPredictor.findRepeats(session, position, picto);
        assertFalse(r[0]);
        assertTrue(r[1]);
        assertTrue(r[2]);   // position 1 again: the first water (position 0) is still on screen
        assertFalse(r[3]);

        // I, water, undo water, juice at position 1, then water: not a repeat (water was undone)
        boolean[] r2 = ErakaPredictor.findRepeats(new long[]{1, 1, 1, 1},
                new int[]{0, 1, 1, 2}, new int[]{I, WATER, JUICE, WATER});
        assertFalse(r2[3]);
    }
}
