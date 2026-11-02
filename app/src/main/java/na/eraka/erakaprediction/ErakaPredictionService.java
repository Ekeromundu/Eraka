package na.eraka.erakaprediction;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import na.eraka.erakaprediction.ErakaPredictor.Query;
import na.eraka.erakaprediction.ErakaPredictor.Suggestion;
import na.eraka.erakaprediction.ErakaPredictor.Tap;
import na.eraka.utils.ErakaPlaces;
import na.eraka.utils.ErakaUsageLog;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Connects the app to the prediction model (step 3).
 *
 *  - One service per child account, holding that child's ErakaPredictor.
 *  - reload(): trains a fresh model from the usage log on the log's writer thread,
 *    so it always includes every tap saved so far, and never blocks the screen.
 *  - onTap(): updates the model live after each tap (no recounting).
 *  - Settings switch "eraka_prediction_enabled": Eraka model (on) or OTTAA's original
 *    order (off). The log records which one was active, for the comparison.
 *
 * Thread safety: the board is sorted on a background thread and the smart row on the
 * main thread, so every access to the model goes through one lock.
 */
public final class ErakaPredictionService {

    private static final String TAG = "ErakaPrediction";

    public static final String PREF_ENABLED = "eraka_prediction_enabled";
    public static final String MODEL_ERAKA = "eraka";
    public static final String MODEL_OTTAA = "ottaa";

    private static final int MAX_PENDING = 200;

    private static ErakaPredictionService instance;
    private static String instanceUid;

    private final Context appContext;
    private final ErakaUsageLog log;
    private final Object lock = new Object();

    private ErakaPredictor predictor = new ErakaPredictor(); // guarded by lock
    private boolean ready = false;                            // guarded by lock
    // live taps kept briefly, so taps made while a reload runs are not lost
    private final List<Tap> pending = new ArrayList<>();      // guarded by lock
    // base model from the starter messages; loaded once, shared by every reload
    private ErakaPredictor baseModel;                         // only touched on the log thread
    private boolean baseLoaded = false;
    // exploration slots (step 5a): statistics, and the pictures shown in them right now
    private ErakaExplorer explorer = new ErakaExplorer();     // guarded by lock
    // ignored suggestions (step 5b)
    private ErakaIgnoreStats ignoreStats = new ErakaIgnoreStats(); // guarded by lock
    private volatile int[] currentExplore = new int[0];

    public static synchronized ErakaPredictionService getInstance(Context context, String userUid) {
        String u = userUid == null ? "" : userUid;
        if (instance == null || !u.equals(instanceUid)) {
            instance = new ErakaPredictionService(context.getApplicationContext(), u);
            instanceUid = u;
        }
        return instance;
    }

    private ErakaPredictionService(Context appContext, String uid) {
        this.appContext = appContext;
        this.log = ErakaUsageLog.getInstance(appContext, uid);
    }

    // ------------------------------------------------------------------ settings

    /** True = Eraka model; false = OTTAA's original order (for comparison). */
    public boolean isEnabled() {
        return prefs().getBoolean(PREF_ENABLED, true);
    }

    /** The name written to the usage log with each tap. */
    public String activeModel() {
        return isEnabled() ? MODEL_ERAKA : MODEL_OTTAA;
    }

    /** True once loaded and there is something to predict from: the child's taps or the base model. */
    public boolean hasData() {
        synchronized (lock) {
            return ready && (predictor.tapsUsed() > 0 || predictor.hasBaseModel());
        }
    }

    // ------------------------------------------------------------------ learning

    /** Retrains from the whole log in the background (app start, after an undo). */
    public void reload() {
        log.runAfterWrites(() -> {
            long perfT0 = na.eraka.utils.ErakaPerf.start();
            List<Tap> taps = log.loadTaps();
            ErakaPredictor fresh = new ErakaPredictor();
            fresh.setBaseModel(loadBaseModel());
            fresh.setPlaceWords(na.eraka.utils.ErakaPlaceWords.forPredictor(appContext));   // Eraka 28 Oct 2026
            fresh.train(taps);
            ErakaExplorer freshExplorer = new ErakaExplorer();
            for (int[] e : log.loadExploreEvents()) {
                freshExplorer.recordDecision(java.util.Arrays.copyOfRange(e, 1, e.length), e[0]);
            }
            ErakaIgnoreStats freshIgnore = new ErakaIgnoreStats();
            for (long[] e : log.loadSuggestionEvents()) {
                int[] shown = new int[e.length - 3];
                for (int i = 0; i < shown.length; i++) shown[i] = (int) e[3 + i];
                freshIgnore.record((int) e[1], shown, (int) e[2], e[0]);
            }
            long lastLoadedTs = taps.isEmpty() ? Long.MIN_VALUE : taps.get(taps.size() - 1).ts;
            synchronized (lock) {
                // taps that happened after the history was read are added again
                for (Tap t : pending) if (t.ts > lastLoadedTs) fresh.addTap(t);
                predictor = fresh;
                explorer = freshExplorer;
                ignoreStats = freshIgnore;
                ready = true;
            }
            na.eraka.utils.ErakaPerf.end("model_build", perfT0, "taps=" + fresh.tapsUsed());
            Log.d(TAG, "model trained on " + fresh.tapsUsed() + " taps; base model "
                    + (fresh.hasBaseModel() ? "on, share " + String.format(java.util.Locale.ROOT, "%.2f", fresh.baseShare()) : "off"));
        });
    }

    /** Reads assets/eraka_base/base_messages.csv once (null if missing or empty). */
    private ErakaPredictor loadBaseModel() {
        if (baseLoaded) return baseModel;
        baseLoaded = true;
        try (InputStream in = appContext.getAssets().open(ErakaBaseModel.ASSET_PATH)) {
            baseModel = ErakaBaseModel.load(in);
            Log.d(TAG, "base model: " + (baseModel == null ? "no messages" : baseModel.tapsUsed() + " starter taps"));
        } catch (Exception e) {
            Log.d(TAG, "base model not found (" + e.getMessage() + ")");
            baseModel = null;
        }
        return baseModel;
    }

    /** Learns from one tap immediately (call right after the tap is logged). */
    public void onTap(int pictoId, int prev1, int prev2) {
        onTap(pictoId, prev1, prev2, false);
    }

    /**
     * Same, but a repeat (picture already in the message) is not learned: the log keeps
     * it, the model skips it, exactly as loadTaps() marks it on the next reload.
     */
    public void onTap(int pictoId, int prev1, int prev2, boolean repeat) {
        if (repeat) return;
        Tap t = new Tap(pictoId, prev1, prev2, System.currentTimeMillis(), currentHour(), currentPlace(), false);
        synchronized (lock) {
            predictor.addTap(t);
            pending.add(t);
            if (pending.size() > MAX_PENDING) pending.remove(0);
        }
    }

    // ------------------------------------------------------------------ exploration (step 5a)

    /**
     * Chooses up to k "new" pictures for the exploration slots (UCB, see ErakaExplorer).
     * Candidates in order of preference: pictures linked to the previous picture, then the
     * base model's pictures, then the given core words. Pictures the child already uses
     * often are skipped (the model shows those anyway).
     */
    public List<Integer> pickExplore(Collection<Integer> linkedToPrevious, Collection<Integer> coreWords,
                                     java.util.Set<Integer> exclude, int k) {
        synchronized (lock) {
            List<Integer> candidates = new ArrayList<>();
            if (linkedToPrevious != null) candidates.addAll(linkedToPrevious);
            candidates.addAll(predictor.basePictosByFrequency());
            if (coreWords != null) candidates.addAll(coreWords);
            return explorer.pick(candidates, exclude, predictor.timesUsed(), k);
        }
    }

    /** The smart row tells us which of its pictures are exploration slots right now. */
    public void setCurrentExplore(int[] ids) {
        currentExplore = ids == null ? new int[0] : ids.clone();
    }

    /** The exploration pictures on screen right now (for the log). */
    public int[] currentExplore() {
        return currentExplore.clone();
    }

    /** Learns from one tap made while the given exploration pictures were shown. */
    public void onExploreDecision(int[] shownExplore, int tappedId) {
        synchronized (lock) {
            explorer.recordDecision(shownExplore, tappedId);
        }
    }

    // ------------------------------------------------------------------ ignored suggestions (step 5b)

    /** Learns from one tap: these exploitation suggestions were shown, tappedId was chosen. */
    public void onSuggestionDecision(int prev1, int[] shownExploit, int tappedId) {
        synchronized (lock) {
            ignoreStats.record(prev1, shownExploit, tappedId, System.currentTimeMillis());
        }
    }

    /**
     * Ranking for the suggestion row: the model's probability times the ignored-suggestion
     * multiplier (ErakaIgnoreStats). Looks further down the model's list, so a picture that
     * keeps being ignored can make room for the next one. The 4 tiles do not use this.
     */
    public List<Suggestion> rankSuggestions(Query q, Collection<Integer> exclude, int k) {
        synchronized (lock) {
            if (!ready) return Collections.emptyList();
            long perfT0 = na.eraka.utils.ErakaPerf.start();
            List<Suggestion> pool = predictor.rank(q, null, exclude, Math.max(k * 3, k + 6));
            final long now = System.currentTimeMillis();
            final Map<Integer, Double> score = new java.util.HashMap<>();
            for (Suggestion s : pool) score.put(s.pictoId, s.probability * ignoreStats.multiplier(q.prev1, s.pictoId, now));
            List<Suggestion> sorted = new ArrayList<>(pool);
            Collections.sort(sorted, (a, b) -> Double.compare(score.get(b.pictoId), score.get(a.pictoId)));
            List<Suggestion> out = sorted.size() > k ? new ArrayList<>(sorted.subList(0, k)) : sorted;
            na.eraka.utils.ErakaPerf.end("predict_row", perfT0, "k=" + k);
            return out;
        }
    }

    // ------------------------------------------------------------------ predicting

    /** The situation now, from the pictures already in the message (oldest first). */
    public Query queryFor(int[] messageIds) {
        int n = messageIds == null ? 0 : messageIds.length;
        int prev1 = n >= 1 ? messageIds[n - 1] : ErakaPredictor.START;
        int prev2 = n >= 2 ? messageIds[n - 2] : ErakaPredictor.START;
        return new Query(prev1, prev2, currentHour(), currentPlace());
    }

    public List<Suggestion> rank(Query q, Collection<Integer> candidates, Collection<Integer> exclude, int k) {
        synchronized (lock) {
            if (!ready) return Collections.emptyList();
            long perfT0 = na.eraka.utils.ErakaPerf.start();
            List<Suggestion> out = predictor.rank(q, candidates, exclude, k);
            na.eraka.utils.ErakaPerf.end("predict_rank", perfT0, "k=" + k);
            return out;
        }
    }

    public Map<Integer, Double> probabilities(Query q) {
        synchronized (lock) {
            if (!ready) return Collections.emptyMap();
            long perfT0 = na.eraka.utils.ErakaPerf.start();
            Map<Integer, Double> out = predictor.probabilities(q);
            na.eraka.utils.ErakaPerf.end("predict_probs", perfT0, "n=" + out.size());
            return out;
        }
    }

    public String explain(Query q, int pictoId) {
        synchronized (lock) {
            return predictor.explain(q, pictoId);
        }
    }

    // ------------------------------------------------------------------ helpers

    private SharedPreferences prefs() {
        return PreferenceManager.getDefaultSharedPreferences(appContext);
    }

    private static int currentHour() {
        return Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
    }

    private String currentPlace() {
        try {
            return ErakaPlaces.current(appContext).key;
        } catch (Exception e) {
            return "";
        }
    }
}
