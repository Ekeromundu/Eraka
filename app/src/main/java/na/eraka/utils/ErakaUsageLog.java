package na.eraka.utils;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;
import android.util.Log;

import na.eraka.erakaprediction.ErakaPredictor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Eraka usage log (step 1 of the Eraka prediction model).
 *
 * Records every pictogram the child adds to a message, together with the
 * context the prediction model will learn from:
 *   - the two previous pictograms in the message (for the bigram / trigram),
 *   - the time (hour, weekday) and the place chosen in "Where are we?",
 *   - how the picture was chosen (board tile, smart row, voice, gallery),
 *   - which suggestions were on screen and whether the tapped one was among them
 *     (needed for top-k accuracy and for learning from ignored suggestions).
 *
 * It also records message events (spoken, cleared, undo).
 *
 * One SQLite database per child account ("eraka_usage_<uid>.db"), stored only on
 * the phone. Writes run on a background thread so the board never waits.
 * exportCsv() writes both tables as CSV files for the offline evaluation script.
 */
public final class ErakaUsageLog extends SQLiteOpenHelper {

    private static final String TAG = "ErakaUsageLog";
    // v2 (6 Oct 2026): tap_events.model = which suggestion model was active (eraka / ottaa)
    // v3 (13 Oct 2026): tap_events.explore_ids = the smart-row pictures that were exploration slots
    // v4 (13 Oct 2026): message_events.template + sentence = suggested sentence (accepted / ignored / not used)
    // v5 (13 Oct 2026): speech_check table = the caregiver's weekly speech check (10 fixed words)
    private static final int DB_VERSION = 5;

    // How the pictogram was chosen
    public static final String SOURCE_BOARD = "board";
    public static final String SOURCE_SMART_ROW = "smart_row";
    public static final String SOURCE_VOICE = "voice";
    public static final String SOURCE_VOICE_CORRECTED = "voice_corrected";
    public static final String SOURCE_GALLERY = "gallery";

    // Message events
    public static final String MSG_SPOKEN = "spoken";
    public static final String MSG_CLEARED = "cleared";
    public static final String MSG_UNDO = "undo";
    // Sentence suggestions (v4): spoken with its own button / hidden with x / words spoken instead
    public static final String MSG_SENTENCE_ACCEPTED = "sentence_accepted";
    public static final String MSG_SENTENCE_IGNORED = "sentence_ignored";
    public static final String MSG_SENTENCE_NOT_USED = "sentence_not_used";

    public static final int NONE = -1;

    private static final String T_TAPS = "tap_events";
    private static final String T_MESSAGES = "message_events";
    private static final String T_SPEECH = "speech_check";

    private static ErakaUsageLog instance;
    private static String instanceUid;

    private final Context appContext;
    private final String uid;
    private final ExecutorService writer = Executors.newSingleThreadExecutor();

    public static synchronized ErakaUsageLog getInstance(Context context, String userUid) {
        String u = safeUid(userUid);
        if (instance == null || !u.equals(instanceUid)) {
            instance = new ErakaUsageLog(context.getApplicationContext(), u);
            instanceUid = u;
        }
        return instance;
    }

    private ErakaUsageLog(Context appContext, String uid) {
        super(appContext, "eraka_usage_" + uid + ".db", null, DB_VERSION);
        this.appContext = appContext;
        this.uid = uid;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_TAPS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "ts INTEGER NOT NULL,"            // milliseconds since 1970
                + "session_id INTEGER NOT NULL,"    // one message = one session
                + "position INTEGER NOT NULL,"      // 0 = first picture of the message
                + "picto_id INTEGER NOT NULL,"
                + "prev1_id INTEGER NOT NULL,"      // picture just before (-1 = none)
                + "prev2_id INTEGER NOT NULL,"      // picture two before (-1 = none)
                + "source TEXT NOT NULL,"
                + "place TEXT,"                     // ErakaPlaces key, e.g. HOME, SCHOOL
                + "hour INTEGER,"                   // 0-23
                + "weekday INTEGER,"                // 1 = Sunday ... 7 = Saturday
                + "shown_board TEXT,"               // ids of the 4 tiles, space separated
                + "shown_smart TEXT,"               // ids in the smart row
                + "rank_board INTEGER,"             // 1-4 if tapped tile was on the board, else 0
                + "rank_smart INTEGER,"             // 1-n if it was in the smart row, else 0
                + "model TEXT,"                     // suggestion model active at the tap: eraka / ottaa (v2)
                + "explore_ids TEXT)");             // smart-row pictures shown as exploration slots (v3)
        db.execSQL("CREATE TABLE " + T_MESSAGES + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "ts INTEGER NOT NULL,"
                + "session_id INTEGER NOT NULL,"
                + "type TEXT NOT NULL,"             // spoken / cleared / undo
                + "length INTEGER,"
                + "picto_ids TEXT,"
                + "place TEXT,"
                + "hour INTEGER,"
                + "template TEXT,"                  // sentence template id (v4)
                + "sentence TEXT)");                // suggested sentence text (v4)
        db.execSQL("CREATE INDEX idx_taps_session ON " + T_TAPS + "(session_id)");
        createSpeechTable(db);
    }

    /** v5: one row per word in a caregiver speech check. */
    private static void createSpeechTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + T_SPEECH + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "ts INTEGER NOT NULL,"
                + "check_id INTEGER NOT NULL,"      // one run through the 10 words = one check
                + "phase TEXT,"                     // start / end (of the week)
                + "picto_id INTEGER NOT NULL,"
                + "label TEXT,"                     // the word the child was asked to say
                + "heard TEXT,"                     // what Whisper wrote down ("" = nothing / skipped)
                + "recognised INTEGER,"             // 1 if the word itself is in what was heard
                + "rank INTEGER,"                   // place of the picture in the voice suggestions (0 = not there)
                + "ms INTEGER)");                   // time from Record to result
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Never drop tables here: the logged data is the thesis evidence. Only add.
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + T_TAPS + " ADD COLUMN model TEXT");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + T_TAPS + " ADD COLUMN explore_ids TEXT");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + T_MESSAGES + " ADD COLUMN template TEXT");
            db.execSQL("ALTER TABLE " + T_MESSAGES + " ADD COLUMN sentence TEXT");
        }
        if (oldVersion < 5) {
            createSpeechTable(db);
        }
    }

    // ------------------------------------------------------------------ logging

    /**
     * Logs one pictogram added to the message.
     *
     * @param pictoId    the pictogram chosen
     * @param prev1      the pictogram just before it in this message (NONE if first)
     * @param prev2      the pictogram two before it (NONE if fewer than two)
     * @param position   how many pictures were already in the message (0 = first)
     * @param source     one of the SOURCE_ constants
     * @param shownBoard ids of the pictures on the 4 main tiles at the moment of the tap
     * @param shownSmart ids of the pictures in the smart row at the moment of the tap
     * @param model      which suggestion model was active ("eraka" or "ottaa")
     */
    public void logTap(int pictoId, int prev1, int prev2, int position, String source,
                       int[] shownBoard, int[] shownSmart, String model) {
        logTap(pictoId, prev1, prev2, position, source, shownBoard, shownSmart, model, null);
    }

    /** Same, plus the smart-row pictures that were exploration slots at the tap (step 5a). */
    public void logTap(int pictoId, int prev1, int prev2, int position, String source,
                       int[] shownBoard, int[] shownSmart, String model, int[] exploreIds) {
        final long ts = System.currentTimeMillis();
        final long session = position == 0 ? nextSession() : currentSession();
        final String place = currentPlace();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts);

        final ContentValues v = new ContentValues();
        v.put("ts", ts);
        v.put("session_id", session);
        v.put("position", position);
        v.put("picto_id", pictoId);
        v.put("prev1_id", prev1);
        v.put("prev2_id", prev2);
        v.put("source", source);
        v.put("place", place);
        v.put("hour", cal.get(Calendar.HOUR_OF_DAY));
        v.put("weekday", cal.get(Calendar.DAY_OF_WEEK));
        v.put("shown_board", join(shownBoard));
        v.put("shown_smart", join(shownSmart));
        v.put("rank_board", rankOf(pictoId, shownBoard));
        v.put("rank_smart", rankOf(pictoId, shownSmart));
        v.put("model", model);
        v.put("explore_ids", join(exploreIds));
        insert(T_TAPS, v);
    }

    /** Logs a message event: spoken, cleared or undo. */
    public void logMessage(String type, int[] pictoIds) {
        logMessage(type, pictoIds, null, null);
    }

    /** Same, with the suggested sentence and its template (sentence events, v4). */
    public void logMessage(String type, int[] pictoIds, String template, String sentence) {
        final long ts = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts);
        final ContentValues v = new ContentValues();
        v.put("ts", ts);
        v.put("session_id", currentSession());
        v.put("type", type);
        v.put("length", pictoIds == null ? 0 : pictoIds.length);
        v.put("picto_ids", join(pictoIds));
        v.put("place", currentPlace());
        v.put("hour", cal.get(Calendar.HOUR_OF_DAY));
        if (template != null) v.put("template", template);
        if (sentence != null) v.put("sentence", sentence);
        insert(T_MESSAGES, v);
    }

    /**
     * Logs one word of a caregiver speech check (v5).
     *
     * @param checkId    the same number for all words of one check (use the start time)
     * @param phase      "start" or "end" of the week
     * @param heard      Whisper's transcript, "" if nothing was heard or the word was skipped
     * @param recognised whether the target word itself appears in the transcript
     * @param rank       1-n = place of the target picture in the voice suggestions, 0 = not offered
     * @param ms         milliseconds from pressing Record to the result (-1 if skipped)
     */
    public void logSpeechCheck(long checkId, String phase, int pictoId, String label,
                               String heard, boolean recognised, int rank, long ms) {
        final ContentValues v = new ContentValues();
        v.put("ts", System.currentTimeMillis());
        v.put("check_id", checkId);
        v.put("phase", phase);
        v.put("picto_id", pictoId);
        v.put("label", label);
        v.put("heard", heard == null ? "" : heard);
        v.put("recognised", recognised ? 1 : 0);
        v.put("rank", rank);
        v.put("ms", ms);
        insert(T_SPEECH, v);
    }

    private void insert(final String table, final ContentValues v) {
        writer.execute(() -> {
            try {
                getWritableDatabase().insert(table, null, v);
            } catch (Exception e) {
                Log.e(TAG, "insert " + table + ": " + e.getMessage());
            }
        });
    }

    /**
     * Runs a job on the writer thread, i.e. after every log entry queued so far has been
     * saved. Used to retrain the model from the log (e.g. after an undo).
     */
    public void runAfterWrites(Runnable job) {
        writer.execute(() -> {
            try {
                job.run();
            } catch (Exception e) {
                Log.e(TAG, "runAfterWrites: " + e.getMessage());
            }
        });
    }

    // ------------------------------------------------------------------ reading

    /**
     * All logged taps, oldest first, ready for ErakaPredictor.train().
     * Taps the child removed again with the erase button (undo) are marked undone,
     * so the model does not learn from mistakes. Taps of a picture that was already in
     * the message are marked repeat, so tapping "water" five times counts once.
     * Blocking; call off the main thread.
     */
    public List<ErakaPredictor.Tap> loadTaps() {
        List<long[]> rows = new ArrayList<>(); // id, ts, session, position, picto, prev1, prev2, hour
        List<String> places = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT id, ts, session_id, position, picto_id, prev1_id, prev2_id, hour, place FROM "
                        + T_TAPS + " ORDER BY ts, id", null)) {
            while (c.moveToNext()) {
                rows.add(new long[]{c.getLong(0), c.getLong(1), c.getLong(2), c.getLong(3),
                        c.getLong(4), c.getLong(5), c.getLong(6), c.getLong(7)});
                places.add(c.isNull(8) ? "" : c.getString(8));
            }
        }

        // Each undo event lists the message BEFORE the last picture was removed:
        // the removed tap is the latest one in that session at position length-1
        // with that picture, made before the undo.
        Set<Long> undoneIds = new HashSet<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT session_id, length, picto_ids, ts FROM " + T_MESSAGES
                        + " WHERE type = ?", new String[]{MSG_UNDO})) {
            while (c.moveToNext()) {
                long session = c.getLong(0);
                long position = c.getLong(1) - 1;
                String[] ids = c.isNull(2) ? new String[0] : c.getString(2).trim().split(" ");
                if (position < 0 || ids.length == 0 || ids[ids.length - 1].isEmpty()) continue;
                long removed = Long.parseLong(ids[ids.length - 1]);
                long undoTs = c.getLong(3);
                long bestId = -1, bestTs = Long.MIN_VALUE;
                for (long[] r : rows) {
                    if (r[2] == session && r[3] == position && r[4] == removed && r[1] <= undoTs && r[1] > bestTs) {
                        bestId = r[0];
                        bestTs = r[1];
                    }
                }
                if (bestId >= 0) undoneIds.add(bestId);
            }
        }

        long[] sessions = new long[rows.size()];
        int[] positions = new int[rows.size()];
        int[] pictos = new int[rows.size()];
        for (int i = 0; i < rows.size(); i++) {
            sessions[i] = rows.get(i)[2];
            positions[i] = (int) rows.get(i)[3];
            pictos[i] = (int) rows.get(i)[4];
        }
        boolean[] repeats = ErakaPredictor.findRepeats(sessions, positions, pictos);

        List<ErakaPredictor.Tap> taps = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            long[] r = rows.get(i);
            taps.add(new ErakaPredictor.Tap((int) r[4], (int) r[5], (int) r[6], r[1], (int) r[7],
                    places.get(i), undoneIds.contains(r[0]), repeats[i]));
        }
        return taps;
    }

    /**
     * Exploration history for ErakaExplorer: for every tap made while exploration slots
     * were shown, {tapped picture, explore id 1, explore id 2, ...}. Oldest first.
     * Blocking; call off the main thread.
     */
    public List<int[]> loadExploreEvents() {
        List<int[]> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT picto_id, explore_ids FROM " + T_TAPS
                        + " WHERE explore_ids IS NOT NULL AND explore_ids != '' ORDER BY ts, id", null)) {
            while (c.moveToNext()) {
                String[] parts = c.getString(1).trim().split(" ");
                int[] row = new int[parts.length + 1];
                row[0] = c.getInt(0);
                for (int i = 0; i < parts.length; i++) {
                    try {
                        row[i + 1] = Integer.parseInt(parts[i]);
                    } catch (NumberFormatException e) {
                        row[i + 1] = NONE;
                    }
                }
                out.add(row);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadExploreEvents: " + e.getMessage());
        }
        return out;
    }

    /**
     * Suggestion history for ErakaIgnoreStats (step 5b): for every tap,
     * {ts, prev1, tapped picture, exploitation suggestions shown...}. Exploitation = the
     * smart-row pictures that were not exploration slots. Oldest first. Blocking.
     */
    public List<long[]> loadSuggestionEvents() {
        List<long[]> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT ts, prev1_id, picto_id, shown_smart, explore_ids FROM " + T_TAPS
                        + " WHERE shown_smart IS NOT NULL AND shown_smart != '' ORDER BY ts, id", null)) {
            while (c.moveToNext()) {
                Set<String> explore = new HashSet<>();
                if (!c.isNull(4)) for (String e : c.getString(4).trim().split(" ")) explore.add(e);
                List<Long> shown = new ArrayList<>();
                for (String s : c.getString(3).trim().split(" ")) {
                    if (s.isEmpty() || explore.contains(s)) continue;
                    try {
                        shown.add(Long.parseLong(s));
                    } catch (NumberFormatException ignored) {
                    }
                }
                long[] row = new long[3 + shown.size()];
                row[0] = c.getLong(0);
                row[1] = c.getLong(1);
                row[2] = c.getLong(2);
                for (int i = 0; i < shown.size(); i++) row[3 + i] = shown.get(i);
                out.add(row);
            }
        } catch (Exception e) {
            Log.e(TAG, "loadSuggestionEvents: " + e.getMessage());
        }
        return out;
    }

    /** Sentence decisions, oldest first: {template, type} for accepted and ignored. Blocking. */
    public List<String[]> loadSentenceEvents() {
        List<String[]> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().rawQuery(
                "SELECT template, type FROM " + T_MESSAGES + " WHERE type IN (?, ?) AND template IS NOT NULL ORDER BY ts, id",
                new String[]{MSG_SENTENCE_ACCEPTED, MSG_SENTENCE_IGNORED})) {
            while (c.moveToNext()) out.add(new String[]{c.getString(0), c.getString(1)});
        } catch (Exception e) {
            Log.e(TAG, "loadSentenceEvents: " + e.getMessage());
        }
        return out;
    }

    /** Number of logged taps (blocking; call off the main thread). */
    public long countTaps() {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM " + T_TAPS, null)) {
            return c.moveToFirst() ? c.getLong(0) : 0;
        }
    }

    /**
     * Writes tap_events and message_events as CSV files into the app's external
     * files folder (Android/data/<package>/files/eraka_usage/), which can be copied
     * to a computer over USB. Blocking; call off the main thread.
     *
     * @return the folder, or null on failure
     */
    public synchronized File exportCsv() {
        File base = appContext.getExternalFilesDir(null);
        if (base == null) base = appContext.getFilesDir();
        File dir = new File(base, "eraka_usage");
        if (!dir.exists() && !dir.mkdirs()) return null;
        try {
            writeCsv(T_TAPS, new File(dir, "tap_events_" + uid + ".csv"));
            writeCsv(T_MESSAGES, new File(dir, "message_events_" + uid + ".csv"));
            writeCsv(T_SPEECH, new File(dir, "speech_check_" + uid + ".csv"));
            return dir;
        } catch (IOException e) {
            Log.e(TAG, "exportCsv: " + e.getMessage());
            return null;
        }
    }

    private void writeCsv(String table, File out) throws IOException {
        try (Cursor c = getReadableDatabase().rawQuery("SELECT * FROM " + table + " ORDER BY id", null);
             FileWriter w = new FileWriter(out, false)) {
            String[] cols = c.getColumnNames();
            w.write(String.join(",", cols));
            w.write("\n");
            while (c.moveToNext()) {
                for (int i = 0; i < cols.length; i++) {
                    if (i > 0) w.write(",");
                    String s = c.isNull(i) ? "" : c.getString(i);
                    w.write(s.replace(",", " ").replace("\n", " ").replace("\r", " "));
                }
                w.write("\n");
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private long currentSession() {
        return prefs().getLong(sessionKey(), 0);
    }

    private long nextSession() {
        long next = currentSession() + 1;
        prefs().edit().putLong(sessionKey(), next).apply();
        return next;
    }

    private String sessionKey() { return "eraka_usage_session_" + uid; }

    /** The cleaned account id used in this log's file names. */
    public String uid() { return uid; }

    private SharedPreferences prefs() { return PreferenceManager.getDefaultSharedPreferences(appContext); }

    private String currentPlace() {
        try {
            return ErakaPlaces.current(appContext).key;
        } catch (Exception e) {
            return "";
        }
    }

    private static int rankOf(int id, int[] shown) {
        if (shown == null) return 0;
        for (int i = 0; i < shown.length; i++) if (shown[i] == id) return i + 1;
        return 0;
    }

    /** Space-separated ids; -1 marks an empty tile so positions stay readable. */
    private static String join(int[] ids) {
        if (ids == null) return "";
        StringBuilder b = new StringBuilder();
        for (int id : ids) {
            if (b.length() > 0) b.append(' ');
            b.append(id);
        }
        return b.toString();
    }

    private static String safeUid(String u) {
        String s = u == null || u.isEmpty() ? "local" : u;
        return s.replaceAll("[^A-Za-z0-9_-]", "_");
    }
}
