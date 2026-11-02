package na.eraka.utils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Eraka "Send usage data" (13 Oct 2026).
 *
 * Packs everything the evaluation needs into ONE zip file and hands it to Android's
 * share sheet, so a parent can send it to the researcher by WhatsApp or e-mail:
 *
 *   info.txt              child code, app version, phone, date
 *   tap_events.csv        every picture tapped (ErakaUsageLog)
 *   message_events.csv    spoken / cleared / undo / sentence events
 *   speech_check.csv      the caregiver speech checks
 *   voice_eval.csv        every voice suggestion and what was picked (VoiceDictionary)
 *
 * The files inside the zip are named by the child CODE (e.g. C01), not by the account id,
 * so the researcher's copy is pseudonymous. No audio, photos or names are included.
 */
public final class ErakaDataExport {

    private static final String TAG = "ErakaDataExport";
    public static final String PREF_CHILD_CODE = "eraka_child_code";

    public interface Callback {
        /** Called on the main thread; zip is null if something went wrong. */
        void onReady(File zip);
    }

    private ErakaDataExport() { }

    public static String childCode(Context ctx) {
        return prefs(ctx).getString(PREF_CHILD_CODE, "");
    }

    public static void setChildCode(Context ctx, String code) {
        prefs(ctx).edit().putString(PREF_CHILD_CODE, cleanCode(code)).apply();
    }

    /** "c 01!" -> "C01": letters, digits, - and _ only, upper case, max 12 characters. */
    public static String cleanCode(String code) {
        if (code == null) return "";
        String s = code.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "");
        return s.length() > 12 ? s.substring(0, 12) : s;
    }

    /**
     * Builds the zip in the background. It runs on the usage log's writer thread, so every
     * tap that was logged before pressing "Send" is already saved when the CSVs are written.
     */
    public static void build(final Context context, final ErakaUsageLog log,
                             final File voiceEval, final Callback callback) {
        final Context app = context.getApplicationContext();
        final Handler main = new Handler(Looper.getMainLooper());
        log.runAfterWrites(() -> {
            File zip = null;
            try {
                zip = buildNow(app, log, voiceEval);
            } catch (Exception e) {
                Log.e(TAG, "build: " + e.getMessage());
            }
            final File result = zip;
            main.post(() -> callback.onReady(result));
        });
    }

    private static File buildNow(Context app, ErakaUsageLog log, File voiceEval) throws IOException {
        File csvDir = log.exportCsv();
        if (csvDir == null) return null;

        String code = childCode(app);
        if (code.isEmpty()) code = "NOCODE";
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.ROOT).format(new Date());

        File outDir = new File(app.getCacheDir(), "eraka_share");
        if (!outDir.exists() && !outDir.mkdirs()) return null;
        // keep only the newest zip on the phone
        File[] old = outDir.listFiles();
        if (old != null) for (File f : old) //noinspection ResultOfMethodCallIgnored
            f.delete();

        File zip = new File(outDir, "eraka_" + code + "_" + stamp + ".zip");
        try (ZipOutputStream out = new ZipOutputStream(new java.io.FileOutputStream(zip))) {
            addText(out, "info.txt", info(app, code));
            // tap_events_<uid>.csv -> tap_events.csv (no account id in the shared copy)
            String[] tables = {"tap_events", "message_events", "speech_check"};
            for (String t : tables) {
                File f = new File(csvDir, t + "_" + log.uid() + ".csv");
                if (f.exists()) addFile(out, t + ".csv", f);
            }
            if (voiceEval != null && voiceEval.exists()) addFile(out, "voice_eval.csv", voiceEval);
        }
        return zip;
    }

    /** Opens Android's share sheet (WhatsApp, Gmail, Bluetooth...) for the zip. */
    public static void share(Context context, File zip, String chooserTitle) {
        Uri uri = FileProvider.getUriForFile(context,
                context.getPackageName() + ".fileprovider", zip);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/zip");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.putExtra(Intent.EXTRA_SUBJECT, "Eraka usage data " + childCode(context));
        send.putExtra(Intent.EXTRA_TEXT, "Eraka usage data for child " + childCode(context)
                + " (" + zip.getName() + ")");
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        Intent chooser = Intent.createChooser(send, chooserTitle);
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        context.startActivity(chooser);
    }

    // ------------------------------------------------------------------ helpers

    private static String info(Context app, String code) {
        String version = "?";
        try {
            android.content.pm.PackageInfo pi = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
            version = pi.versionName + " (" + pi.versionCode + ")";
        } catch (Exception ignored) { }
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).format(new Date());
        return "child_code=" + code + "\n"
                + "app_version=" + version + "\n"
                + "exported_at=" + now + "\n"
                + "phone=" + Build.MANUFACTURER + " " + Build.MODEL + "\n"
                + "android=" + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n"
                + "place_now=" + safePlace(app) + "\n"
                + profileLines(app);
    }

    /**
     * The child's communication level, age in whole years and interests, to describe the
     * participants. Never the name or the birth date (21 Oct 2026).
     */
    private static String profileLines(Context app) {
        try {
            android.content.SharedPreferences p = android.preference.PreferenceManager.getDefaultSharedPreferences(app);
            int age = ErakaChildProfile.ageYears(p.getLong(ErakaChildProfile.PREF_BIRTH_DATE, 0));
            return "comm_level=" + ErakaChildProfile.commLevel(p) + "\n"
                    + "age_years=" + (age >= 0 ? String.valueOf(age) : "") + "\n"
                    + "interests=" + android.text.TextUtils.join(",", ErakaChildProfile.interests(p)) + "\n";
        } catch (Exception e) {
            return "";
        }
    }

    private static String safePlace(Context app) {
        try { return ErakaPlaces.current(app).key; } catch (Exception e) { return ""; }
    }

    private static void addText(ZipOutputStream out, String name, String text) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(text.getBytes(StandardCharsets.UTF_8));
        out.closeEntry();
    }

    private static void addFile(ZipOutputStream out, String name, File f) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        byte[] buf = new byte[8192];
        try (InputStream in = new FileInputStream(f)) {
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        out.closeEntry();
    }

    private static SharedPreferences prefs(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx.getApplicationContext());
    }
}
