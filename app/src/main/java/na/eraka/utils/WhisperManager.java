package na.eraka.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import na.eraka.R;
import com.whispercpp.whisper.WhisperContext;

public class WhisperManager {
    private static final int SAMPLE_RATE = 16000; // must match WhisperRecorder's SAMPLE_RATE

    // Heuristic plausibility filter (no native confidence available yet).
    // Rejects results that are implausibly long/short for how much audio was captured —
    // catches the most common Whisper hallucination pattern: short/quiet audio producing
    // a long, fluent-sounding sentence.
    private static final long MIN_AUDIO_MS_FOR_ANY_RESULT = 300;   // below this, treat as noise no matter what
    private static final double MAX_WORDS_PER_SECOND = 4.0;        // tune against real recordings

    // Eraka: Whisper's own "this was not speech" score. Humming ("hmm"), breathing and
    // background noise can otherwise come back as made-up sentences
    // ("oh, so is it your home"). 0.6 is whisper.cpp's own default; tune from the logs.
    private static final float NO_SPEECH_THRESHOLD = 0.6f;

    public static final String MODEL_NAME = "ggml-tiny.en-q5_1.bin";

    // true from the moment audio is handed to Whisper until the native call returns.
    // whisper.cpp runs one job at a time, so a new recording while busy would only
    // queue behind the old one and time out as well.
    private volatile boolean transcribing = false;

    // Eraka: the child's picture words, passed to Whisper as its "initial prompt"
    private volatile String vocabularyHint = "";

    private final Context context;
    private WhisperContext whisperContext;
    private WhisperRecorder recorder;
    private boolean isRecording = false;
    private volatile boolean modelReady = false;
    private volatile boolean modelLoadAttempted = false;
    private volatile boolean modelLoadFailed = false;
    private volatile boolean supportedRuntime = true;

    public WhisperManager(Context context) {
        this.context = context.getApplicationContext();

        new Thread(() -> {
            modelLoadAttempted = true;
            // tiny.en (~32 MB) is several times faster than base.en on low-end phones
            // (Galaxy A13: base.en took minutes). Single words are the main use case.
            String modelName = MODEL_NAME;
            String modelPath = WhisperHelper.getModelPath(this.context, modelName);
            try {
                if (modelPath == null) {
                    String refreshed = WhisperHelper.refreshModelPath(this.context, modelName);
                    if (refreshed == null) {
                        modelLoadFailed = true;
                        showModelError();
                        return;
                    }
                    modelPath = refreshed;
                }
                long perfT0 = ErakaPerf.start();
                whisperContext = WhisperBridge.createContext(modelPath);
                ErakaPerf.end("whisper_load", perfT0, "model=" + modelName);
                modelReady = true;
                modelLoadFailed = false;
            } catch (UnsatisfiedLinkError e) {
                supportedRuntime = false;
                modelLoadFailed = true;
                showModelError();
            } catch (RuntimeException e) {
                modelLoadFailed = true;
                showModelError();
            }
        }).start();
    }

    public boolean isModelReady() { return modelReady; }
    public boolean isModelLoadAttempted() { return modelLoadAttempted; }
    public boolean isSupportedRuntime() { return supportedRuntime; }
    public boolean hasModelLoadFailed() { return modelLoadFailed; }

    // Listener for the CURRENT recording. The recorder is created once, so it must not
    // capture the first listener forever (teach mode and normal mode use different ones).
    private volatile OnResultListener currentListener;

    // If native transcription never returns (very slow phone / debug native build / native
    // error), give the button back instead of spinning forever.
    private static final long TRANSCRIBE_TIMEOUT_MS = 90_000;

    public boolean isTranscribing() { return transcribing; }

    /** Words Whisper should expect, e.g. "water, mom, play, eat". Empty = no hint. */
    public void setVocabularyHint(String hint) {
        vocabularyHint = hint == null ? "" : hint;
    }

    public void toggleRecording(OnResultListener listener) {
        if (!modelReady) return;
        if (transcribing) {
            Toast.makeText(context, R.string.whisper_busy, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isRecording) start(listener);
        else stop();
    }

    private void start(OnResultListener listener) {
        currentListener = listener;
        if (recorder == null) {
            recorder = new WhisperRecorder(new WhisperRecorder.OnAudioDataCapturedListener() {
                @Override
                public void onAudioDataCaptured(float[] audioData) {
                    isRecording = false;
                    final OnResultListener l = currentListener;
                    final Handler main = new Handler(Looper.getMainLooper());
                    // free the microphone (VAD auto-stop does not release AudioRecord)
                    main.post(WhisperManager.this::releaseMic);
                    if (l == null) return;

                    final java.util.concurrent.atomic.AtomicBoolean delivered =
                            new java.util.concurrent.atomic.AtomicBoolean(false);
                    main.post(l::onTranscribingStarted);
                    main.postDelayed(() -> {
                        if (delivered.compareAndSet(false, true)) {
                            android.util.Log.e("whisper", "Transcription timed out after "
                                    + TRANSCRIBE_TIMEOUT_MS + "ms");
                            Toast.makeText(context, R.string.whisper_timeout, Toast.LENGTH_LONG).show();
                            l.onResult(null);
                        }
                    }, TRANSCRIBE_TIMEOUT_MS);

                    transcribing = true;
                    Thread t = new Thread(() -> {
                        String finalText = null;
                        try {
                            long startTime = System.currentTimeMillis();
                            android.util.Log.d("whisper", "Model=" + MODEL_NAME + ", threads available="
                                    + Runtime.getRuntime().availableProcessors());
                            android.util.Log.d("whisper", "Starting transcribe, audio length=" + audioData.length
                                    + " (" + (audioData.length * 1000L / SAMPLE_RATE) + "ms)");
                            WhisperBridge.setPrompt(whisperContext, vocabularyHint);
                            android.util.Log.d("whisper", "Vocabulary hint: [" + vocabularyHint + "]");
                            String result = WhisperBridge.transcribe(whisperContext, audioData);
                            long elapsed = System.currentTimeMillis() - startTime;
                            android.util.Log.d("whisper", "Transcribe finished in " + elapsed
                                    + "ms, result=[" + result + "]");
                            float noSpeech = 0f;
                            try {
                                noSpeech = WhisperBridge.noSpeechProb(whisperContext);
                            } catch (Throwable nsErr) {
                                android.util.Log.w("whisper", "no-speech score unavailable", nsErr);
                            }
                            android.util.Log.d("whisper", "No-speech probability: " + noSpeech);
                            if (noSpeech > NO_SPEECH_THRESHOLD) {
                                android.util.Log.d("whisper", "Rejected: probably not speech (" + noSpeech + ")");
                                finalText = null;
                            } else {
                                finalText = filterImplausibleResult(result, audioData.length);
                            }
                        } catch (Throwable e) {
                            // Throwable, not RuntimeException: OutOfMemoryError / native errors
                            // used to kill this thread silently and leave the spinner on.
                            android.util.Log.e("whisper", "Whisper transcription failed", e);
                        } finally {
                            transcribing = false;
                        }
                        final String text = finalText;
                        main.post(() -> {
                            if (delivered.compareAndSet(false, true)) l.onResult(text);
                        });
                    }, "WhisperTranscribe");
                    t.start();
                }

                @Override
                public void onNoSpeechDetected() {
                    isRecording = false;
                    final OnResultListener l = currentListener;
                    Handler main = new Handler(Looper.getMainLooper());
                    main.post(WhisperManager.this::releaseMic);
                    if (l != null) main.post(l::onNoSpeechDetected);
                }
            });
        }
        recorder.start();
        isRecording = true;
        Toast.makeText(context, R.string.whisper_listening, Toast.LENGTH_SHORT).show();
    }

    private void releaseMic() {
        if (recorder != null) recorder.stop();
    }

    /**
     * Heuristic plausibility check — no real confidence score available without native
     * changes, so this compares result length against audio duration to catch the most
     * common Whisper failure mode: short/quiet/noisy audio producing an implausibly long
     * or fluent-sounding transcription (hallucination).
     *
     * @return the trimmed, accepted text, or null if the result should be discarded
     */
    private String filterImplausibleResult(String rawResult, int audioSampleCount) {
        if (rawResult == null || rawResult.trim().isEmpty()) {
            return null;
        }
        String text = collapseRepeats(rawResult.trim());
        if (text.isEmpty()) return null;

        long audioDurationMs = (audioSampleCount * 1000L) / SAMPLE_RATE;

        if (audioDurationMs < MIN_AUDIO_MS_FOR_ANY_RESULT) {
            android.util.Log.d("whisper", "Rejected: audio too short (" + audioDurationMs + "ms) for any result");
            return null;
        }

        int wordCount = text.split("\\s+").length;
        double audioDurationSeconds = audioDurationMs / 1000.0;
        double maxPlausibleWords = audioDurationSeconds * MAX_WORDS_PER_SECOND;

        if (wordCount > maxPlausibleWords) {
            android.util.Log.d("whisper", "Rejected: " + wordCount + " words implausible for "
                    + audioDurationMs + "ms of audio (max plausible: " + maxPlausibleWords + ")");
            return null;
        }

        return text;
    }

    /**
     * Whisper's typical failure on short/quiet audio is a loop like
     * "water, water, water" or "grandpa, good night, good night, good night".
     * For AAC the repeated item is usually what was said, so keep each distinct
     * word/phrase once (in order) instead of throwing the whole result away.
     *  - with commas: each comma-separated phrase is one item ("good night")
     *  - without commas: each word is one item ("oi la oi la" -> "oi la")
     */
    static String collapseRepeats(String text) {
        String[] parts = text.contains(",") ? text.split(",") : text.split("\\s+");
        java.util.LinkedHashMap<String, String> unique = new java.util.LinkedHashMap<>();
        for (String part : parts) {
            String clean = part.trim().replaceAll("[.!?]+$", "").trim();
            String key = clean.toLowerCase(java.util.Locale.ROOT).replaceAll("[^\\p{L}\\p{N} ]", "")
                    .replaceAll("\\s+", " ").trim();
            if (key.isEmpty() || unique.containsKey(key)) continue;
            unique.put(key, clean);
        }
        String result = android.text.TextUtils.join(text.contains(",") ? ", " : " ", unique.values());
        if (!result.equals(text)) android.util.Log.d("whisper", "Collapsed repeats: [" + text + "] -> [" + result + "]");
        return result;
    }

    public void stop() {
        if (recorder != null) recorder.stop();
        isRecording = false;
    }

    public boolean isRecording() { return isRecording; }

    public void release() {
        stop();
        if (whisperContext != null) {
            WhisperBridge.release(whisperContext);
        }
        modelReady = false;
    }

    private void showModelError() {
        new Handler(Looper.getMainLooper()).post(() ->
                Toast.makeText(this.context, R.string.whisper_model_error, Toast.LENGTH_LONG).show());
    }

    public interface OnResultListener {
        void onTranscribingStarted();
        void onResult(String text);
        void onNoSpeechDetected();
    }
}