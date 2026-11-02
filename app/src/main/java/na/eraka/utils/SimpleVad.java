package na.eraka.utils;

/**
 * Lightweight energy-based Voice Activity Detector.
 * Works on 16-bit PCM mono audio (the format whisper.cpp expects).
 * No native deps — cheap enough to run on every AudioRecord.read() chunk.
 *
 * Uses an adaptive threshold: the first CALIBRATION_MS of each session are
 * treated as the ambient noise floor (never counted as voice), and the voice
 * threshold is set relative to that floor rather than a fixed absolute RMS.
 * This matters for quiet or atypical vocalizations (e.g. murmuring) that may
 * fall below a threshold tuned for typical adult speech volume.
 */
public class SimpleVad {

    // How long to sample ambient noise before we start deciding voice vs silence.
    private static final long CALIBRATION_MS = 300;

    // Voice threshold = noiseFloor * MULTIPLIER, clamped to [MIN_THRESHOLD, MAX_THRESHOLD].
    // Lower multiplier = more sensitive to quiet speech, but more prone to false triggers
    // on breath noise / handling noise. Tune against real recordings from target users.
    private static final double THRESHOLD_MULTIPLIER = 2.2;
    private static final double MIN_THRESHOLD = 150.0;   // floor: never trigger below this even in dead silence
    private static final double MAX_THRESHOLD = 800.0;   // ceiling: never require more than this even in loud rooms

    // Eraka: 1500 -> 900 ms. AAC input is mostly single words; waiting 1.5 s of
    // silence added ~0.6 s of extra audio for Whisper to process on every attempt.
    private static final int SILENCE_HANG_MS = 900;      // silence duration before we call it "done talking"

    private long sessionStartMs = -1;
    private long lastVoiceTimestampMs = -1;
    private boolean voiceEverDetected = false;

    private double noiseFloorSum = 0;
    private int noiseFloorSamples = 0;
    private double voiceThreshold = MIN_THRESHOLD;
    private boolean calibrated = false;

    /**
     * Feed one chunk of 16-bit PCM samples as read from AudioRecord.
     * @param samples the buffer (may be longer than the valid data)
     * @param length  number of valid samples in this chunk
     * @return true if this chunk contains voice-level energy
     */
    public boolean processChunk(short[] samples, int length) {
        if (length <= 0) return false;
        double rms = computeRms(samples, length);

        if (sessionStartMs < 0) {
            sessionStartMs = System.currentTimeMillis();
        }
        long elapsed = System.currentTimeMillis() - sessionStartMs;

        if (!calibrated) {
            if (elapsed < CALIBRATION_MS) {
                // Still in the calibration window — treat this as noise floor, not voice.
                noiseFloorSum += rms;
                noiseFloorSamples++;
                return false;
            } else {
                double noiseFloor = noiseFloorSamples > 0 ? (noiseFloorSum / noiseFloorSamples) : MIN_THRESHOLD;
                voiceThreshold = clamp(noiseFloor * THRESHOLD_MULTIPLIER, MIN_THRESHOLD, MAX_THRESHOLD);
                calibrated = true;
            }
        }

        boolean isVoice = rms > voiceThreshold;
        if (isVoice) {
            voiceEverDetected = true;
            lastVoiceTimestampMs = System.currentTimeMillis();
        }
        return isVoice;
    }

    /**
     * Call after each processChunk() to decide whether to auto-stop.
     * True only once real voice was heard AND silence has persisted
     * long enough afterward, so a mid-sentence pause doesn't cut a child off.
     */
    public boolean shouldAutoStop() {
        if (!voiceEverDetected) return false;
        return (System.currentTimeMillis() - lastVoiceTimestampMs) > SILENCE_HANG_MS;
    }

    /** Did we ever hear anything above the noise floor this session? */
    public boolean hadAnyVoice() {
        return voiceEverDetected;
    }

    /** Exposed for logging/debugging — the threshold this session settled on. */
    public double getVoiceThreshold() {
        return voiceThreshold;
    }

    public void reset() {
        sessionStartMs = -1;
        lastVoiceTimestampMs = -1;
        voiceEverDetected = false;
        noiseFloorSum = 0;
        noiseFloorSamples = 0;
        voiceThreshold = MIN_THRESHOLD;
        calibrated = false;
    }

    private double computeRms(short[] samples, int length) {
        double sumSquares = 0;
        for (int i = 0; i < length; i++) {
            sumSquares += (double) samples[i] * samples[i];
        }
        return Math.sqrt(sumSquares / length);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}