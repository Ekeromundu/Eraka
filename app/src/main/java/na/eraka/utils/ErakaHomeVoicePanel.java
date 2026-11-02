package na.eraka.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;

import na.eraka.R;

/**
 * The "Word at home" and "Your voice" part of the picture editor (22 Oct 2026); see
 * {@link ErakaHomeWords}. Changes are kept aside until the parent presses Save
 * ({@link #commit}); Cancel throws them away ({@link #discard}).
 */
public class ErakaHomeVoicePanel {

    private static final String TAG = "ErakaHomeVoice";
    private static final int MAX_MS = 10_000;          // a word or a short phrase
    public static final int REQUEST_MIC = 4711;

    private final Activity activity;
    private final int pictoId;                          // 0 for a new picture
    private EditText homeWord;
    private Button btnRecord, btnPlay, btnDelete;
    private MediaRecorder recorder;
    private boolean hasPending;                         // a new recording waiting for Save
    private boolean deleteSaved;                        // Delete pressed: remove the saved one on Save

    public ErakaHomeVoicePanel(Activity activity, int pictoId, boolean isGroup) {
        this.activity = activity;
        this.pictoId = pictoId;
        View section = activity.findViewById(R.id.homeWordSection);
        if (section == null) return;                    // a layout without the section
        if (isGroup) { section.setVisibility(View.GONE); return; }
        homeWord = activity.findViewById(R.id.editHomeWord);
        btnRecord = activity.findViewById(R.id.btnHomeVoiceRecord);
        btnPlay = activity.findViewById(R.id.btnHomeVoicePlay);
        btnDelete = activity.findViewById(R.id.btnHomeVoiceDelete);
        ErakaHomeWords.pendingVoiceFile(activity).delete();
        if (homeWord != null && pictoId != 0) homeWord.setText(ErakaHomeWords.word(activity, pictoId));
        if (btnRecord != null) btnRecord.setOnClickListener(v -> { if (recorder == null) startRecording(); else stopRecording(); });
        if (btnPlay != null) btnPlay.setOnClickListener(v -> play());
        if (btnDelete != null) btnDelete.setOnClickListener(v -> delete());
        refresh();
    }

    private boolean hasSaved() {
        return pictoId != 0 && !deleteSaved && ErakaHomeWords.hasVoice(activity, pictoId);
    }

    private void refresh() {
        boolean any = hasPending || hasSaved();
        if (btnRecord != null) btnRecord.setText(recorder != null ? R.string.home_voice_stop
                : (any ? R.string.home_voice_record_again : R.string.home_voice_record));
        if (btnPlay != null) btnPlay.setEnabled(any && recorder == null);
        if (btnDelete != null) btnDelete.setEnabled(any && recorder == null);
    }

    private void startRecording() {
        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_MIC);
            Toast.makeText(activity, R.string.home_voice_allow_mic, Toast.LENGTH_LONG).show();
            return;
        }
        ErakaHomeWords.stop();
        File f = ErakaHomeWords.pendingVoiceFile(activity);
        try {
            recorder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? new MediaRecorder(activity) : new MediaRecorder();
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            recorder.setAudioSamplingRate(44100);
            recorder.setAudioEncodingBitRate(96000);
            recorder.setMaxDuration(MAX_MS);
            recorder.setOnInfoListener((mr, what, extra) -> {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) stopRecording();
            });
            recorder.setOutputFile(f.getAbsolutePath());
            recorder.prepare();
            recorder.start();
            Toast.makeText(activity, R.string.home_voice_speak_now, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "record: " + e.getMessage());
            releaseRecorder();
            Toast.makeText(activity, R.string.home_voice_failed, Toast.LENGTH_LONG).show();
        }
        refresh();
    }

    private void stopRecording() {
        if (recorder == null) return;
        boolean ok = true;
        try { recorder.stop(); } catch (Exception e) { ok = false; }   // too short: nothing recorded
        releaseRecorder();
        File f = ErakaHomeWords.pendingVoiceFile(activity);
        hasPending = ok && f.exists() && f.length() > 0;
        if (!hasPending) {
            f.delete();
            Toast.makeText(activity, R.string.home_voice_too_short, Toast.LENGTH_SHORT).show();
        } else {
            ErakaHomeWords.play(activity, f);           // let the parent hear it straight away
        }
        refresh();
    }

    private void releaseRecorder() {
        if (recorder != null) {
            try { recorder.release(); } catch (Exception ignored) { }
            recorder = null;
        }
    }

    /** The edit screen's speaker button: true if a recording (new or saved) was played. */
    public boolean playIfAny() {
        if (recorder != null) return false;
        if (hasPending) return ErakaHomeWords.play(activity, ErakaHomeWords.pendingVoiceFile(activity));
        return hasSaved() && ErakaHomeWords.playVoice(activity, pictoId);
    }

    /** The home word typed in the field, or {@code label} if it is empty. */
    public String homeWordOr(String label) {
        String w = homeWord == null ? "" : homeWord.getText().toString().trim();
        return w.isEmpty() ? label : w;
    }

    private void play() {
        if (hasPending) ErakaHomeWords.play(activity, ErakaHomeWords.pendingVoiceFile(activity));
        else if (hasSaved()) ErakaHomeWords.playVoice(activity, pictoId);
    }

    private void delete() {
        ErakaHomeWords.stop();
        if (hasPending) {
            ErakaHomeWords.pendingVoiceFile(activity).delete();
            hasPending = false;
        } else {
            deleteSaved = true;
        }
        refresh();
    }

    /** Save pressed: store the home word and the recording for picture {@code id}. */
    public void commit(int id) {
        if (homeWord == null || id == 0) { discard(); return; }
        if (recorder != null) stopRecording();
        ErakaHomeWords.setWord(activity, id, homeWord.getText().toString());
        File saved = ErakaHomeWords.voiceFile(activity, id);
        if (hasPending) {
            saved.delete();
            if (!ErakaHomeWords.pendingVoiceFile(activity).renameTo(saved)) Log.e(TAG, "could not keep the recording");
        } else if (deleteSaved) {
            saved.delete();
        }
        hasPending = false;
        deleteSaved = false;
    }

    /** Cancel / leaving the screen: forget what was not saved. */
    public void discard() {
        releaseRecorder();
        ErakaHomeWords.stop();
        ErakaHomeWords.pendingVoiceFile(activity).delete();
        hasPending = false;
    }
}
