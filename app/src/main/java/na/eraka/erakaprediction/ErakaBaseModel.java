package na.eraka.erakaprediction;

import na.eraka.erakaprediction.ErakaPredictor.Tap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The base model: what every new child starts with (cold start).
 *
 * Built from starter messages written by Namibian teachers and parents
 * ("I want water", "go to school"). The importer script (tools/eraka_base_import.py)
 * turns each message into picture IDs and writes the app asset
 * assets/eraka_base/base_messages.csv, one message per line:
 *
 *     source,time,place,picto_ids,text
 *     parent,MORNING,HOME,643 44 500,I want water
 *     teacher,,SCHOOL,643 44 34,I want to play
 *
 *  - time:  MORNING / MIDDAY / AFTERNOON / EVENING, or empty = unknown
 *  - place: an ErakaPlaces key (HOME, SCHOOL, ...), or empty = unknown
 *  - picto_ids: space-separated, in the order the child would tap them
 *  - text: the original message (kept for checking; may contain commas)
 *
 * Each message becomes a short sequence of taps (the first one after START), exactly
 * like a child tapping it, so the base model learns trigrams, bigrams, time and place
 * with the same code as the child's own model. Recency is switched off: an old starter
 * message is not "older" than another.
 *
 * Pure Java (no Android classes): ErakaPredictionService opens the asset and passes the stream.
 */
public final class ErakaBaseModel {

    public static final String ASSET_PATH = "eraka_base/base_messages.csv";

    private ErakaBaseModel() { }

    /** Reads the CSV and returns the base model, or null if it holds no usable message. */
    public static ErakaPredictor load(InputStream in) throws IOException {
        List<Tap> taps = parse(in);
        if (taps.isEmpty()) return null;
        ErakaPredictor.Config cfg = new ErakaPredictor.Config();
        cfg.halfLifeDays = Double.POSITIVE_INFINITY; // every starter message counts the same
        ErakaPredictor base = new ErakaPredictor(cfg);
        base.train(taps);
        return base;
    }

    /** Turns every message line into taps (prev1/prev2 filled in, START at the beginning). */
    public static List<Tap> parse(InputStream in) throws IOException {
        List<Tap> taps = new ArrayList<>();
        BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        String line;
        while ((line = r.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#") || line.startsWith("source,")) continue;
            String[] f = line.split(",", 5);
            if (f.length < 4) continue;
            int hour = hourFor(f[1].trim());
            String place = f[2].trim().toUpperCase(Locale.ROOT);
            int prev1 = ErakaPredictor.START, prev2 = ErakaPredictor.START;
            for (String idText : f[3].trim().split("\\s+")) {
                if (idText.isEmpty()) continue;
                int id;
                try {
                    id = Integer.parseInt(idText);
                } catch (NumberFormatException e) {
                    continue; // skip anything that is not a picture ID
                }
                taps.add(new Tap(id, prev1, prev2, 0L, hour, place, false));
                prev2 = prev1;
                prev1 = id;
            }
        }
        return taps;
    }

    /** A typical hour inside each time-of-day bucket (see ErakaPredictor.timeBucket). */
    static int hourFor(String bucket) {
        switch (bucket.toUpperCase(Locale.ROOT)) {
            case "MORNING": return 8;
            case "MIDDAY": return 13;
            case "AFTERNOON": return 17;
            case "EVENING": return 20;
            default: return -1; // unknown: not counted for time of day
        }
    }
}
