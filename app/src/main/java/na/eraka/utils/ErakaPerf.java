package na.eraka.utils;

import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.util.Locale;

/**
 * Timing lines for the thesis (Technical Performance). Only writes to logcat, never changes
 * what the app does. Every line looks like
 *
 *     ErakaPerf: model_build ms=84.2 taps=412
 *
 * Collect them with:  adb logcat -s ErakaPerf whisper ActivityTaskManager > perf.txt
 * and summarise with: python tools/eraka_perf_report.py perf.txt
 */
public final class ErakaPerf {
    public static final String TAG = "ErakaPerf";
    private static boolean boardReadyLogged = false;

    private ErakaPerf() { }

    /** Start a timer: {@code long t0 = ErakaPerf.start();} */
    public static long start() {
        return System.nanoTime();
    }

    /** Logs the milliseconds since {@code t0} (from {@link #start()}). */
    public static void end(String what, long t0) {
        end(what, t0, "");
    }

    public static void end(String what, long t0, String detail) {
        double ms = (System.nanoTime() - t0) / 1e6;
        Log.i(TAG, what + String.format(Locale.ROOT, " ms=%.1f", ms) + (detail.isEmpty() ? "" : " " + detail));
    }

    /** Once per process: time from the process starting to the board being ready to use. */
    public static void boardReady() {
        if (boardReadyLogged) return;
        boardReadyLogged = true;
        long ms = SystemClock.elapsedRealtime() - Process.getStartElapsedRealtime();
        Log.i(TAG, "start_to_board ms=" + ms);
    }
}
