package na.eraka.utils;

import android.annotation.SuppressLint;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

/**
 * Utility class to record audio in the format required by Whisper (16kHz, Mono, 16-bit PCM).
 */
public class WhisperRecorder {
    private static final String TAG = "whisper";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int MAX_RECORD_SECONDS = 30;
    // Padding kept around detected speech when trimming silence (soft starts/endings of words)
    private static final int PAD_BEFORE_SAMPLES = SAMPLE_RATE * 250 / 1000;  // 250 ms
    private static final int PAD_AFTER_SAMPLES = SAMPLE_RATE * 300 / 1000;   // 300 ms

    // Target peak amplitude (in [0,1] float range) after gain normalization.
    // Quiet/murmured speech may peak well below this; boosting it toward the
    // level Whisper's feature extraction was trained against improves recognition.
    private static final float NORMALIZE_TARGET_PEAK = 0.9f;
    private static final float MAX_GAIN = 8.0f; // avoid amplifying near-silence into noise

    private AudioRecord audioRecord;
    private int bufferSize;
    private volatile boolean isRecording = false;
    private Thread recordingThread;
    private SimpleVad vad = new SimpleVad();

    public interface OnAudioDataCapturedListener {
        void onAudioDataCaptured(float[] audioData);
        void onNoSpeechDetected();
    }

    private OnAudioDataCapturedListener listener;

    public WhisperRecorder(OnAudioDataCapturedListener listener) {
        this.listener = listener;
        this.bufferSize = Math.max(
                AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT),
                SAMPLE_RATE
        );
    }

    @SuppressLint("MissingPermission")
    public void start() {
        if (isRecording) return;
        if (bufferSize <= 0) {
            Log.e(TAG, "Invalid AudioRecord buffer size");
            return;
        }

        vad.reset();

        audioRecord = new AudioRecord(
                MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                bufferSize
        );

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord initialization failed");
            return;
        }

        AudioRecord activeAudioRecord = audioRecord;
        activeAudioRecord.startRecording();
        isRecording = true;

        recordingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                short[] buffer = new short[Math.max(1, bufferSize / 2)];
                float[] allSamples = new float[SAMPLE_RATE * MAX_RECORD_SECONDS];
                int totalSamples = 0;
                // Eraka: remember where speech starts/ends so silence can be cut off
                int firstVoiceSample = -1;
                int lastVoiceSample = -1;

                while (isRecording && totalSamples < allSamples.length) {
                    int readSize = activeAudioRecord.read(buffer, 0, Math.min(buffer.length, allSamples.length - totalSamples));
                    if (readSize > 0) {
                        for (int i = 0; i < readSize; i++) {
                            allSamples[totalSamples + i] = buffer[i] / 32768.0f;
                        }
                        totalSamples += readSize;

                        boolean voice = vad.processChunk(buffer, readSize);
                        if (voice) {
                            if (firstVoiceSample < 0) firstVoiceSample = totalSamples - readSize;
                            lastVoiceSample = totalSamples;
                        }
                        if (vad.shouldAutoStop()) {
                            Log.d(TAG, "VAD: silence after speech, auto-stopping");
                            isRecording = false;
                            break;
                        }
                    } else if (readSize < 0) {
                        Log.e(TAG, "AudioRecord read failed: " + readSize);
                        break;
                    }
                }

                if (listener != null) {
                    if (!vad.hadAnyVoice()) {
                        Log.d(TAG, "VAD: no voice detected, skipping Whisper");
                        listener.onNoSpeechDetected();
                    } else {
                        // Keep only the speech plus a little padding: less audio = faster Whisper.
                        int start = 0, end = totalSamples;
                        if (firstVoiceSample >= 0 && lastVoiceSample > firstVoiceSample) {
                            start = Math.max(0, firstVoiceSample - PAD_BEFORE_SAMPLES);
                            end = Math.min(totalSamples, lastVoiceSample + PAD_AFTER_SAMPLES);
                        }
                        float[] finalData = new float[end - start];
                        System.arraycopy(allSamples, start, finalData, 0, end - start);
                        Log.d(TAG, "Trimmed audio " + (totalSamples * 1000L / SAMPLE_RATE) + "ms -> "
                                + (finalData.length * 1000L / SAMPLE_RATE) + "ms");
                        normalizeGain(finalData);
                        listener.onAudioDataCaptured(finalData);
                    }
                }
            }
        });
        recordingThread.setName("WhisperRecorder");
        recordingThread.start();
        Log.d(TAG, "Recording started");
    }

    /**
     * Boosts quiet audio toward NORMALIZE_TARGET_PEAK before it reaches Whisper.
     * Murmured or soft speech may peak at a small fraction of full scale; Whisper's
     * feature extraction performs better when the signal is closer to the level
     * it saw during training. Gain is capped at MAX_GAIN so near-silent buffers
     * (e.g. mostly noise floor) don't get amplified into noise.
     */
    private void normalizeGain(float[] samples) {
        if (samples.length == 0) return;

        float peak = 0f;
        for (float s : samples) {
            float abs = Math.abs(s);
            if (abs > peak) peak = abs;
        }
        if (peak <= 0f) return; // dead silence, nothing to scale

        float gain = Math.min(NORMALIZE_TARGET_PEAK / peak, MAX_GAIN);
        if (gain <= 1.0f) return; // already loud enough, don't attenuate

        for (int i = 0; i < samples.length; i++) {
            samples[i] = samples[i] * gain;
        }
        Log.d(TAG, "Applied gain normalization: " + gain + "x (peak was " + peak + ")");
    }

    public void stop() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                if (audioRecord.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING) {
                    audioRecord.stop();
                }
            } catch (IllegalStateException e) {
                Log.e(TAG, "Error stopping AudioRecord", e);
            }
            audioRecord.release();
            audioRecord = null;
        }
        if (recordingThread != null) {
            try {
                recordingThread.join(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            recordingThread = null;
        }
        Log.d(TAG, "Recording stopped");
    }
}