package na.eraka.Activities.Eraka;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import na.eraka.FirebaseRequests.DownloadJsonFromFirebase;
import na.eraka.FirebaseRequests.FirebaseUtils;
import na.eraka.FirebaseRequests.FirebaseUploader;
import na.eraka.Interfaces.FirebaseSuccessListener;
import na.eraka.JSONutils.Json;
import na.eraka.R;
import na.eraka.utils.ConnectionDetector;
import na.eraka.utils.ErakaLoader;
import na.eraka.utils.constants.Constants;

import java.util.Date;

/**
 * Manual backup (upload board, groups, sentences, favourites) and restore (download them again),
 * using the app's existing Firebase upload/sync code.
 */
public class ErakaBackupActivity extends AppCompatActivity implements FirebaseSuccessListener {

    private static final String TAG = "ErakaBackup";
    private static final String PREF_LAST_BACKUP = "eraka_last_backup";

    private SharedPreferences prefs;
    private FirebaseAuth auth;
    private ErakaLoader loader;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eraka_backup);
        ((TextView) findViewById(R.id.erakaPageTitle)).setText(R.string.backup_title);
        findViewById(R.id.erakaBackButton).setOnClickListener(v -> finish());

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        FirebaseUtils.getInstance().initFirebaseAndEmulator();
        auth = FirebaseAuth.getInstance();
        loader = new ErakaLoader(this);

        FirebaseUser user = auth.getCurrentUser();
        ((TextView) findViewById(R.id.backupAccount)).setText(user != null && user.getEmail() != null
                ? getString(R.string.backup_account, user.getEmail())
                : getString(R.string.backup_not_signed_in));
        showLastBackup();

        findViewById(R.id.backupNowButton).setOnClickListener(v -> backUpNow());
        findViewById(R.id.backupRestoreButton).setOnClickListener(v -> confirmRestore());
    }

    private void showLastBackup() {
        long last = prefs.getLong(PREF_LAST_BACKUP, 0);
        String text = last == 0 ? getString(R.string.backup_never)
                : getString(R.string.backup_last, DateFormat.getMediumDateFormat(this).format(new Date(last))
                + " " + DateFormat.getTimeFormat(this).format(new Date(last)));
        ((TextView) findViewById(R.id.backupLast)).setText(text);
    }

    private boolean ready() {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, R.string.backup_not_signed_in, Toast.LENGTH_LONG).show();
            return false;
        }
        if (!ConnectionDetector.isNetworkAvailable(this)) {
            Toast.makeText(this, R.string.backup_offline, Toast.LENGTH_LONG).show();
            return false;
        }
        FirebaseUtils.getInstance().setUid(auth.getCurrentUser().getUid());
        return true;
    }

    private void backUpNow() {
        if (!ready()) return;
        loader.show(R.string.backup_uploading);
        try {
            FirebaseUploader upload = new FirebaseUploader(this);
            upload.subirPictosFirebase(upload.getmDatabase(auth, Constants.PICTOS), upload.getmStorageRef(auth, Constants.PICTOS));
            upload.subirGruposFirebase(upload.getmDatabase(auth, Constants.Grupos), upload.getmStorageRef(auth, Constants.Grupos));
            upload.subirFrasesFirebase(upload.getmDatabase(auth, Constants.Frases), upload.getmStorageRef(auth, Constants.Frases));
            upload.uploadFavoritePhrases(upload.getmDatabase(auth, Constants.FrasesFavoritas), upload.getmStorageRef(auth, Constants.FrasesFavoritas));
        } catch (Exception e) {
            Log.e(TAG, "backUpNow failed", e);
            loader.dismiss();
            Toast.makeText(this, R.string.loader_something_went_wrong, Toast.LENGTH_LONG).show();
            return;
        }
        // The uploads run in the background without a single "finished" callback,
        // so confirm after a short moment and record the time.
        handler.postDelayed(() -> {
            loader.dismiss();
            prefs.edit().putLong(PREF_LAST_BACKUP, System.currentTimeMillis()).apply();
            showLastBackup();
            Toast.makeText(this, R.string.backup_done, Toast.LENGTH_LONG).show();
        }, 2500);
    }

    private void confirmRestore() {
        if (!ready()) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.backup_restore_confirm_t)
                .setMessage(R.string.backup_restore_confirm_b)
                .setPositiveButton(R.string.backup_restore_confirm_yes, (d, w) -> restore())
                .setNegativeButton(R.string.gate_cancel, null)
                .show();
    }

    private void restore() {
        loader.showWithTimeout(R.string.backup_restoring, 90000, () ->
                Toast.makeText(this, R.string.loader_something_went_wrong, Toast.LENGTH_LONG).show());
        try {
            DownloadJsonFromFirebase download = new DownloadJsonFromFirebase(prefs, auth, getApplicationContext());
            download.setInterfaz(this);
            download.syncFiles();
        } catch (Exception e) {
            Log.e(TAG, "restore failed", e);
            loader.dismiss();
            Toast.makeText(this, R.string.loader_something_went_wrong, Toast.LENGTH_LONG).show();
        }
    }

    // ---- FirebaseSuccessListener ----
    @Override
    public void onDescargaCompleta(int descargaCompleta) {
        if (descargaCompleta != Constants.TODO_DESCARGADO) return;
        runOnUiThread(() -> {
            loader.dismiss();
            Json.getInstance().setmContext(this).initJsonArrays(); // reload the restored board
            Toast.makeText(this, R.string.backup_restored, Toast.LENGTH_LONG).show();
        });
    }

    @Override public void onDatosEncontrados(int datosEncontrados) { }
    @Override public void onFotoDescargada(int fotosDescargadas) { }
    @Override public void onArchivosSubidos(boolean subidos) { }
    @Override public void onPictosSugeridosBajados(boolean descargado) { }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        loader.dismiss();
        super.onDestroy();
    }
}
