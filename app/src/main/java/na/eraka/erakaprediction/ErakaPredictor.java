package na.eraka.erakaprediction;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Eraka next-picture prediction model (step 2).
 *
 * A personalised trigram model with Jelinek-Mercer (linear) interpolation,
 * extended with time-decayed counts (recency) and time-of-day and place context:
 *
 *   P(w | h) = (1 - muT - muP) * [ l3*P3(w | prev2,prev1) + l2*P2(w | prev1) + l1*P1(w) ]
 *            +  muT * P(w | time of day)
 *            +  muP * P(w | place)
 *
 * - One predictor per child, trained only on that child's own taps (ErakaUsageLog).
 * - Every count is a weight that halves every {@link Config#halfLifeDays} days,
 *   so recent use matters more.
 * - Levels with no data for the current context are left out and the remaining
 *   weights are rescaled, so the result is always a proper probability.
 * - Taps the child undid are ignored (they were mistakes, not choices).
 * - Repeated taps are ignored: a picture already in the message ("water water water")
 *   is learned only once per message, so repeated tapping cannot inflate its counts.
 *   Every tap is still logged, so repeat rates can be reported.
 * - Base model (cold start): a second ErakaPredictor learned from the starter
 *   messages of teachers and parents (ErakaBaseModel). The child's own model and the
 *   base model are mixed:
 *
 *       P(w | h) = (1 - b) * P_child(w | h) + b * P_base(w | h),   b = K / (K + n)
 *
 *   n = the child's taps so far, K = Config.baseStrength ("the base is worth K taps").
 *   A new child (n = 0) gets the base model only; as n grows the child's own habits
 *   take over (n = K -> half and half; n = 9K -> 90 % the child's own).
 *
 * Pure Java: no Android classes, so it can be unit-tested on the PC.
 */
public final class ErakaPredictor {

    /** "No picture before" = start of the message (same value as ErakaUsageLog.NONE). */
    public static final int START = -1;

    // ------------------------------------------------------------------ settings

    /** All tunable numbers in one place (reported in the methodology, tuned in step 5). */
    public static final class Config {
        /** Interpolation weights for the sequence part (should add up to 1). */
        public double lambdaTrigram = 0.5;
        public double lambdaBigram = 0.3;
        public double lambdaUnigram = 0.2;
        /** Share of the final score given to time of day and to place. */
        public double muTime = 0.1;
        public double muPlace = 0.1;
        /** A tap loses half its weight every this many days. */
        public double halfLifeDays = 14.0;
        /** How many "pretend taps" the cold-start prior is worth in the unigram. */
        public double priorWeight = 1.0;
        /** How many of the child's own taps the base model is worth (K in b = K / (K + n)). */
        public double baseStrength = 30.0;
        /**
         * Eraka 28 Oct 2026: place words. When a place has a starter word list (church: pray,
         * sing, …), the place gets this share of the score instead of muPlace, and the list is
         * worth this many "pretend taps" at that place (the child's own taps there take over).
         */
        public double muPlaceWords = 0.3;
        public double placeWordsWeight = 5.0;
    }

    // ------------------------------------------------------------------ inputs and outputs

    /** One logged tap, as the model needs it. */
    public static final class Tap {
        public final int pictoId;
        public final int prev1;      // picture just before (START if first)
        public final int prev2;      // picture two before (START if fewer than two)
        public final long ts;        // milliseconds
        public final int hour;       // 0-23, or -1 = unknown (base messages without a time)
        public final String place;   // ErakaPlaces key, e.g. HOME; "" = unknown
        public final boolean undone; // removed again with the erase button
        public final boolean repeat; // picture was already in the message (tapped again)

        public Tap(int pictoId, int prev1, int prev2, long ts, int hour, String place, boolean undone) {
            this(pictoId, prev1, prev2, ts, hour, place, undone, false);
        }

        public Tap(int pictoId, int prev1, int prev2, long ts, int hour, String place,
                   boolean undone, boolean repeat) {
            this.pictoId = pictoId;
            this.prev1 = prev1;
            this.prev2 = prev2;
            this.ts = ts;
            this.hour = hour;
            this.place = place == null ? "" : place;
            this.undone = undone;
            this.repeat = repeat;
        }
    }

    /** The situation we predict for: what is already in the message, when and where. */
    public static final class Query {
        public final int prev1;
        public final int prev2;
        public final int hour;
        public final String place;

        public Query(int prev1, int prev2, int hour, String place) {
            this.prev1 = prev1;
            this.prev2 = prev2;
            this.hour = hour;
            this.place = place == null ? "" : place;
        }
    }

    /** One ranked suggestion. */
    public static final class Suggestion {
        public final int pictoId;
        public final double probability;

        Suggestion(int pictoId, double probability) {
            this.pictoId = pictoId;
            this.probability = probability;
        }

        @Override
        public String toString() {
            return String.format(Locale.ROOT, "%d (%.3f)", pictoId, probability);
        }
    }

    // ------------------------------------------------------------------ count tables

    /** A table "picture -> weighted count" with its total, for one context. */
    private static final class Counts {
        final Map<Integer, Double> byPicto = new HashMap<>();
        double total = 0;

        void add(int pictoId, double weight) {
            Double old = byPicto.get(pictoId);
            byPicto.put(pictoId, (old == null ? 0 : old) + weight);
            total += weight;
        }

        /** Relative frequency: count(picture) / total. */
        double p(int pictoId) {
            if (total <= 0) return 0;
            Double c = byPicto.get(pictoId);
            return c == null ? 0 : c / total;
        }
    }

    private final Config cfg;
    private final Map<Long, Counts> trigram = new HashMap<>();   // (prev2, prev1) -> next
    private final Map<Integer, Counts> bigram = new HashMap<>(); // prev1 -> next
    private final Counts unigram = new Counts();                 // all taps
    private final Map<String, Counts> byTime = new HashMap<>();  // time-of-day bucket -> picture
    private final Map<String, Counts> byPlace = new HashMap<>(); // place -> picture
    private final Map<Integer, Double> prior = new HashMap<>();  // cold start, sums to 1
    private long referenceTs = Long.MIN_VALUE;
    private int tapsUsed = 0;
    private final Map<Integer, Integer> uses = new HashMap<>();  // picture -> real taps (no decay)
    private ErakaPredictor base;                                 // starter messages; may be null
    private final Map<String, List<Integer>> placeWords = new HashMap<>(); // place -> starter words

    /** Eraka 28 Oct 2026: the starter word list of each place (ErakaPlaceWords). */
    public void setPlaceWords(Map<String, List<Integer>> words) {
        placeWords.clear();
        if (words == null) return;
        for (Map.Entry<String, List<Integer>> e : words.entrySet()) {
            if (e.getValue() != null && !e.getValue().isEmpty()) placeWords.put(e.getKey(), new ArrayList<>(e.getValue()));
        }
    }

    /** P(picture | place): the child's taps at this place, blended with the place's word list. */
    private double pPlace(String place, int id) {
        Counts c = byPlace.get(place);
        List<Integer> words = placeWords.get(place);
        double beta = words == null ? 0 : cfg.placeWordsWeight;
        double total = (c == null ? 0 : c.total) + beta;
        if (total <= 0) return 0;
        Double own = c == null ? null : c.byPicto.get(id);
        double prior = words != null && words.contains(id) ? 1.0 / words.size() : 0;
        return ((own == null ? 0 : own) + beta * prior) / total;
    }

    public ErakaPredictor() {
        this(new Config());
    }

    public ErakaPredictor(Config cfg) {
        this.cfg = cfg;
    }

    // ------------------------------------------------------------------ learning

    /**
     * Finds repeated taps in a log: taps whose picture was already in the message on
     * screen. Inputs are parallel arrays in time order: message (session) id, position
     * in the message, picture id. Position tells us what was on screen: after an undo
     * the next tap reuses the freed position, so the message is cut back to it first.
     */
    public static boolean[] findRepeats(long[] session, int[] position, int[] picto) {
        boolean[] repeat = new boolean[picto.length];
        Map<Long, List<Integer>> onScreen = new HashMap<>();
        for (int i = 0; i < picto.length; i++) {
            List<Integer> msg = onScreen.get(session[i]);
            if (msg == null) {
                msg = new ArrayList<>();
                onScreen.put(session[i], msg);
            }
            int pos = Math.max(0, Math.min(position[i], msg.size()));
            msg.subList(pos, msg.size()).clear();      // what an undo removed is gone
            repeat[i] = msg.contains(picto[i]);
            msg.add(picto[i]);
        }
        return repeat;
    }

    /** Learns from a whole history (e.g. ErakaUsageLog.loadTaps()). */
    public void train(List<Tap> taps) {
        List<Tap> sorted = new ArrayList<>(taps);
        Collections.sort(sorted, (a, b) -> Long.compare(a.ts, b.ts));
        for (Tap t : sorted) addTap(t);
    }

    /**
     * Learns from one new tap (called live after each tap, so we never recount everything).
     *
     * Recency trick: instead of shrinking every old count each day, a new tap gets a
     * weight that doubles every half-life after the first tap (2^(age/halfLife)).
     * Only ratios of counts are used, so this gives exactly the same probabilities as
     * "old taps halve every half-life", but each tap is added once and never revisited.
     */
    public void addTap(Tap t) {
        if (t == null || t.undone || t.repeat) return; // mistakes and repeats are not learned
        if (referenceTs == Long.MIN_VALUE) referenceTs = t.ts;
        double halfLifeMs = cfg.halfLifeDays * 24 * 60 * 60 * 1000;
        double weight = Math.pow(2.0, (t.ts - referenceTs) / halfLifeMs);

        counts(trigram, key(t.prev2, t.prev1)).add(t.pictoId, weight);
        counts(bigram, t.prev1).add(t.pictoId, weight);
        unigram.add(t.pictoId, weight);
        if (t.hour >= 0) counts(byTime, timeBucket(t.hour)).add(t.pictoId, weight);
        if (!t.place.isEmpty()) counts(byPlace, t.place).add(t.pictoId, weight);
        tapsUsed++;
        Integer u = uses.get(t.pictoId);
        uses.put(t.pictoId, u == null ? 1 : u + 1);
    }

    /**
     * Cold-start prior: pictures a new child is likely to need (e.g. core words, or
     * OTTAA's starter counts). Any positive weights; they are rescaled to sum to 1.
     */
    public void setPrior(Map<Integer, Double> weights) {
        prior.clear();
        double sum = 0;
        for (double w : weights.values()) if (w > 0) sum += w;
        if (sum <= 0) return;
        for (Map.Entry<Integer, Double> e : weights.entrySet()) {
            if (e.getValue() > 0) prior.put(e.getKey(), e.getValue() / sum);
        }
    }

    /**
     * Sets the base model used for cold start (null = none). It should be trained on the
     * starter messages with recency switched off (halfLifeDays = infinity), see ErakaBaseModel.
     */
    public void setBaseModel(ErakaPredictor baseModel) {
        this.base = baseModel;
    }

    public boolean hasBaseModel() {
        return base != null && base.tapsUsed() > 0;
    }

    /** b = K / (K + n): how much the base model counts right now (1 = only base, 0 = only the child). */
    public double baseShare() {
        if (!hasBaseModel()) return 0;
        if (tapsUsed == 0) return 1;
        return cfg.baseStrength / (cfg.baseStrength + tapsUsed);
    }

    // ------------------------------------------------------------------ predicting

    /**
     * Probability of every known picture in this situation (sums to 1, or empty if nothing is known):
     * the child's own model mixed with the base model, (1 - b) * P_child + b * P_base.
     */
    public Map<Integer, Double> probabilities(Query q) {
        Map<Integer, Double> p = blendedProbabilities(q);
        List<Integer> words = placeWords.get(q.place);
        if (words == null) return p;
        // Eraka 28 Oct 2026: a place with a word list (church, clinic, taxi…) gets a fixed share
        // of the score from the first day, for a new child as much as for one with many taps:
        // P = (1 - muPlaceWords) * P_model + muPlaceWords * P(picture | place)
        double mu = cfg.muPlaceWords;
        Map<Integer, Double> result = new HashMap<>();
        for (Map.Entry<Integer, Double> e : p.entrySet()) result.put(e.getKey(), (1 - mu) * e.getValue());
        Set<Integer> ids = new HashSet<>(words);
        Counts c = byPlace.get(q.place);
        if (c != null) ids.addAll(c.byPicto.keySet());
        for (int id : ids) {
            double pp = pPlace(q.place, id);
            if (pp <= 0) continue;
            Double old = result.get(id);
            result.put(id, (old == null ? 0 : old) + mu * pp);
        }
        return result;
    }

    /** The child's model mixed with the base model, before the place words. */
    private Map<Integer, Double> blendedProbabilities(Query q) {
        double b = baseShare();
        if (b <= 0) return ownProbabilities(q);
        Map<Integer, Double> fromBase = base.probabilities(q);
        if (b >= 1) return fromBase;
        Map<Integer, Double> own = ownProbabilities(q);
        if (own.isEmpty()) return fromBase;
        if (fromBase.isEmpty()) return own;
        Map<Integer, Double> result = new HashMap<>();
        for (Map.Entry<Integer, Double> e : own.entrySet()) result.put(e.getKey(), (1 - b) * e.getValue());
        for (Map.Entry<Integer, Double> e : fromBase.entrySet()) {
            Double old = result.get(e.getKey());
            result.put(e.getKey(), (old == null ? 0 : old) + b * e.getValue());
        }
        return result;
    }

    /** The child's own model only (no base model). */
    private Map<Integer, Double> ownProbabilities(Query q) {
        Map<Integer, Double> result = new HashMap<>();
        Set<Integer> vocabulary = new HashSet<>(unigram.byPicto.keySet());
        vocabulary.addAll(prior.keySet());
        if (vocabulary.isEmpty()) return result;

        Weights w = weightsFor(q);
        for (int id : vocabulary) {
            double p = combine(q, w, id);
            if (p > 0) result.put(id, p);
        }
        return result;
    }

    /**
     * The k best pictures for this situation.
     *
     * @param candidates only rank these (e.g. the pictures in the current group); null = all known
     * @param exclude    never suggest these (e.g. already on screen or already in the message)
     */
    public List<Suggestion> rank(Query q, Collection<Integer> candidates, Collection<Integer> exclude, int k) {
        Map<Integer, Double> probs = probabilities(q);
        List<Suggestion> list = new ArrayList<>();
        Collection<Integer> pool = candidates != null ? candidates : probs.keySet();
        Set<Integer> seen = new HashSet<>();
        for (int id : pool) {
            if (!seen.add(id)) continue;
            if (exclude != null && exclude.contains(id)) continue;
            Double p = probs.get(id);
            if (p != null && p > 0) list.add(new Suggestion(id, p));
        }
        // highest probability first; equal scores by id so the order is always the same
        Collections.sort(list, (a, b) -> a.probability != b.probability
                ? Double.compare(b.probability, a.probability)
                : Integer.compare(a.pictoId, b.pictoId));
        return list.size() > k ? new ArrayList<>(list.subList(0, k)) : list;
    }

    /** Human-readable breakdown of one picture's score (for testing, debugging and the thesis). */
    public String explain(Query q, int pictoId) {
        Weights w = weightsFor(q);
        Counts tri = trigram.get(key(q.prev2, q.prev1));
        Counts bi = bigram.get(q.prev1);
        Counts time = byTime.get(timeBucket(q.hour));
        Counts place = byPlace.get(q.place);
        double b = baseShare();
        if (b > 0) {
            Double pb = base.probabilities(q).get(pictoId);
            Double pa = probabilities(q).get(pictoId);
            return String.format(Locale.ROOT,
                    "picto %d: base model %.3f (share b=%.2f, child has %d taps)  |  child: %s  =>  P=%.3f",
                    pictoId, pb == null ? 0 : pb, b, tapsUsed,
                    tapsUsed == 0 ? "no taps yet" : ownExplain(q, w, tri, bi, time, place, pictoId),
                    pa == null ? 0 : pa);
        }
        return ownExplain(q, w, tri, bi, time, place, pictoId);
    }

    private String ownExplain(Query q, Weights w, Counts tri, Counts bi, Counts time, Counts place, int pictoId) {
        return String.format(Locale.ROOT,
                "picto %d: P3=%.3f (w %.2f)  P2=%.3f (w %.2f)  P1=%.3f (w %.2f)  Ptime=%.3f (w %.2f)  Pplace=%.3f (w %.2f)  =>  P=%.3f",
                pictoId,
                tri == null ? 0 : tri.p(pictoId), w.l3,
                bi == null ? 0 : bi.p(pictoId), w.l2,
                pUnigram(pictoId), w.l1,
                time == null ? 0 : time.p(pictoId), w.mt,
                place == null ? 0 : place.p(pictoId), w.mp,
                combine(q, w, pictoId));
    }

    /** How many times the child really used each picture (repeats/undone not counted). */
    public Map<Integer, Integer> timesUsed() {
        return new HashMap<>(uses);
    }

    /** All pictures this model has seen, most used first (ties: lower id first). */
    public List<Integer> pictosByFrequency() {
        List<Integer> ids = new ArrayList<>(unigram.byPicto.keySet());
        Collections.sort(ids, (a, b) -> {
            int c = Double.compare(unigram.byPicto.get(b), unigram.byPicto.get(a));
            return c != 0 ? c : Integer.compare(a, b);
        });
        return ids;
    }

    /** The base model's pictures, most common first (empty without a base model). */
    public List<Integer> basePictosByFrequency() {
        return base == null ? new ArrayList<>() : base.pictosByFrequency();
    }

    public int tapsUsed() {
        return tapsUsed;
    }

    /** Time-of-day buckets (same boundaries as OTTAA: 5-12, 12-15, 15-20, else). */
    public static String timeBucket(int hour) {
        if (hour >= 5 && hour < 12) return "MORNING";
        if (hour >= 12 && hour < 15) return "MIDDAY";
        if (hour >= 15 && hour < 20) return "AFTERNOON";
        return "EVENING";
    }

    // ------------------------------------------------------------------ internals

    /** The final weights for one query, after leaving out levels that have no data. */
    private static final class Weights {
        double l3, l2, l1, mt, mp;
    }

    private Weights weightsFor(Query q) {
        Weights w = new Weights();
        boolean hasTri = has(trigram.get(key(q.prev2, q.prev1)));
        boolean hasBi = has(bigram.get(q.prev1));
        boolean hasUni = unigram.total > 0 || !prior.isEmpty();
        double l3 = hasTri ? cfg.lambdaTrigram : 0;
        double l2 = hasBi ? cfg.lambdaBigram : 0;
        double l1 = hasUni ? cfg.lambdaUnigram : 0;
        double sum = l3 + l2 + l1;

        double mt = has(byTime.get(timeBucket(q.hour))) ? cfg.muTime : 0;
        double mp = has(byPlace.get(q.place)) ? cfg.muPlace : 0;
        double seqShare = sum > 0 ? 1.0 - mt - mp : 0;
        if (sum <= 0) {               // no sequence data at all: context only
            double ctx = mt + mp;
            if (ctx > 0) { mt /= ctx; mp /= ctx; }
        }
        w.l3 = sum > 0 ? seqShare * l3 / sum : 0;
        w.l2 = sum > 0 ? seqShare * l2 / sum : 0;
        w.l1 = sum > 0 ? seqShare * l1 / sum : 0;
        w.mt = mt;
        w.mp = mp;
        return w;
    }

    private double combine(Query q, Weights w, int id) {
        Counts tri = trigram.get(key(q.prev2, q.prev1));
        Counts bi = bigram.get(q.prev1);
        Counts time = byTime.get(timeBucket(q.hour));
        Counts place = byPlace.get(q.place);
        return w.l3 * (tri == null ? 0 : tri.p(id))
                + w.l2 * (bi == null ? 0 : bi.p(id))
                + w.l1 * pUnigram(id)
                + w.mt * (time == null ? 0 : time.p(id))
                + w.mp * (place == null ? 0 : place.p(id));
    }

    /** Overall use, blended with the cold-start prior (the prior fades as real taps grow). */
    private double pUnigram(int id) {
        double beta = prior.isEmpty() ? 0 : cfg.priorWeight;
        double denom = unigram.total + beta;
        if (denom <= 0) return 0;
        Double c = unigram.byPicto.get(id);
        Double pr = prior.get(id);
        return ((c == null ? 0 : c) + beta * (pr == null ? 0 : pr)) / denom;
    }

    private static boolean has(Counts c) {
        return c != null && c.total > 0;
    }

    private static <K> Counts counts(Map<K, Counts> map, K key) {
        Counts c = map.get(key);
        if (c == null) {
            c = new Counts();
            map.put(key, c);
        }
        return c;
    }

    /** Packs the two previous pictures into one number to use as a map key. */
    private static long key(int prev2, int prev1) {
        return ((long) prev2 << 32) ^ (prev1 & 0xffffffffL);
    }
}
