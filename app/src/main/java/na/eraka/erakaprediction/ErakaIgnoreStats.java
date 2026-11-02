package na.eraka.erakaprediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Learning from ignored suggestions (step 5b).
 *
 * A suggestion the child sees but does not tap is weak evidence that it is not wanted
 * in that situation. For every (previous picture, suggested picture) pair we count, over
 * the last {@link #WINDOW_DAYS} days, how often it was shown in the exploitation slots
 * of the suggestion row and how often it was taken. Its score is then multiplied by
 *
 *     m = (taken + 1) / (taken + 1 + GAMMA * ignored)        (0 < m <= 1)
 *
 *  - never ignored:            m = 1    (no change)
 *  - ignored 4 times, never taken: m = 0.5
 *  - ignored 12 times:         m = 0.25
 *  - taken once more:          m rises again
 *
 * Per previous picture, because "pizza ignored after 'I want'" says nothing about pizza
 * after "eat". Only the suggestion row is affected: the 4 tiles keep their positions,
 * which matters for children who learn where pictures are (motor planning).
 *
 * Pure Java (no Android), unit-tested on the PC.
 */
public final class ErakaIgnoreStats {

    public static double GAMMA = 0.25;
    public static double WINDOW_DAYS = 14.0;

    private final Map<Long, List<long[]>> events = new HashMap<>(); // key -> {ts, taken 0/1}

    private static long key(int prev1, int pictoId) {
        return (((long) prev1) << 32) ^ (pictoId & 0xffffffffL);
    }

    /** One tap: these pictures were shown as suggestions (exploitation slots); tappedId was chosen. */
    public void record(int prev1, int[] shown, int tappedId, long ts) {
        if (shown == null) return;
        for (int id : shown) {
            if (id < 0) continue;
            long k = key(prev1, id);
            List<long[]> list = events.get(k);
            if (list == null) {
                list = new ArrayList<>();
                events.put(k, list);
            }
            list.add(new long[]{ts, id == tappedId ? 1 : 0});
        }
    }

    /** Score multiplier for this picture after prev1, counting only the last WINDOW_DAYS. */
    public double multiplier(int prev1, int pictoId, long now) {
        List<long[]> list = events.get(key(prev1, pictoId));
        if (list == null) return 1.0;
        long from = now - (long) (WINDOW_DAYS * 24 * 60 * 60 * 1000);
        int taken = 0, ignored = 0;
        for (long[] e : list) {
            if (e[0] < from || e[0] > now) continue;
            if (e[1] == 1) taken++;
            else ignored++;
        }
        return (taken + 1.0) / (taken + 1.0 + GAMMA * ignored);
    }

    public int ignored(int prev1, int pictoId, long now) {
        List<long[]> list = events.get(key(prev1, pictoId));
        if (list == null) return 0;
        long from = now - (long) (WINDOW_DAYS * 24 * 60 * 60 * 1000);
        int n = 0;
        for (long[] e : list) if (e[0] >= from && e[0] <= now && e[1] == 0) n++;
        return n;
    }
}
