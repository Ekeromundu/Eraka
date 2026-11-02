package na.eraka.FirebaseRequests;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Eraka: replaces Firebase Cloud Storage, which needs the paid Blaze plan.
 *
 * OTTAA stored three kinds of files in Cloud Storage. Eraka now handles them like this:
 *
 *  1. STARTER files ("Archivos_Paises/...", "Archivos_Sugerencias/...": the default
 *     pictogram list, groups, suggestion files). They are the same for everyone, so they
 *     are packed INSIDE the app (assets/eraka_seed/<same path>) and copied from there.
 *     No internet needed, instant first start.
 *
 *  2. A family's BACKUP files ("Archivos_Usuarios/<Folder>/<name>.txt": their board,
 *     groups, phrases, favourite phrases, games). These are small JSON text files, so
 *     they are stored as text in the free Realtime Database under
 *        Backups/<uid>/<Folder>/<language>     e.g. Backups/abc123/Pictos/en
 *     Security rules: only that uid can read/write.
 *
 *  3. PHOTOS and avatars (binary files). They stay on the phone only. Uploads/downloads
 *     of these fail with PhotosStayOnPhoneException, which callers treat as "skip".
 *
 * The methods keep the same shape as the Storage calls they replace: they take the
 * storage path (StorageReference.getPath() still works without Storage access) and
 * return a Task, so the existing success/failure listeners keep working.
 */
public final class ErakaFileStore {

    private static final String TAG = "ErakaFileStore";
    public static final String SEED_ASSET_DIR = "eraka_seed/";
    public static final String BACKUP_NODE = "Backups";

    /** Thrown (as a Task failure) for photos/avatars, which are never uploaded. */
    public static class PhotosStayOnPhoneException extends Exception {
        public PhotosStayOnPhoneException(String path) {
            super("Photos stay on the phone (no cloud storage): " + path);
        }
    }

    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    private ErakaFileStore() { }

    // ------------------------------------------------------------------ paths

    static String clean(String storagePath) {
        if (storagePath == null) return "";
        String p = storagePath.trim();
        while (p.startsWith("/")) p = p.substring(1);
        return p;
    }

    public static boolean isStarterFile(String storagePath) {
        String p = clean(storagePath);
        return p.startsWith("Archivos_Paises/") || p.startsWith("Archivos_Sugerencias/");
    }

    /** Archivos_Usuarios/<Folder>/<name>.txt (not Fotos/Avatar, which are photos). */
    public static boolean isUserTextFile(String storagePath) {
        String p = clean(storagePath);
        if (!p.startsWith("Archivos_Usuarios/")) return false;
        String[] parts = p.split("/");
        if (parts.length != 3) return false;               // Fotos/<uid>/<img> has 4 parts
        String folder = parts[1];
        if (folder.equalsIgnoreCase("Fotos") || folder.equalsIgnoreCase("Avatar")) return false;
        return parts[2].endsWith(".txt");
    }

    /** Realtime Database location for a family's backup file, or null if not signed in. */
    static DatabaseReference backupRef(String storagePath) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return null;
        String[] parts = clean(storagePath).split("/");
        String folder = parts[1];
        // Key by language only (e.g. "en"): the uid in the path already identifies the family,
        // and OTTAA's file names contain the e-mail, which is sometimes empty in some code
        // paths - that would make backup and restore look under different keys.
        String name = parts[2];
        int us = name.lastIndexOf('_');
        int dot = name.lastIndexOf('.');
        String key = (us >= 0 && dot > us) ? name.substring(us + 1, dot)
                : name.replace('.', ',').replace('#', '_').replace('$', '_').replace('[', '_').replace(']', '_');
        return FirebaseDatabase.getInstance().getReference()
                .child(BACKUP_NODE).child(user.getUid()).child(folder).child(key);
    }

    /**
     * Archivos_Usuarios/Pictos/pictos_<email>_<lang>.txt -> Archivos_Paises/pictos/pictos_<lang>.txt
     * Archivos_Usuarios/Grupos/grupos_<email>_<lang>.txt -> Archivos_Paises/grupos/grupos_<lang>.txt
     * Other backups (phrases, games...) have no starter version: returns null.
     */
    static String starterFallback(String userPath) {
        String[] parts = clean(userPath).split("/");
        if (parts.length != 3) return null;
        String folder = parts[1].toLowerCase();
        String name = parts[2];
        int us = name.lastIndexOf('_');
        int dot = name.lastIndexOf('.');
        if (us < 0 || dot < us) return null;
        String lang = name.substring(us + 1, dot);
        if (folder.equals("pictos")) return "Archivos_Paises/pictos/pictos_" + lang + ".txt";
        if (folder.equals("grupos")) return "Archivos_Paises/grupos/grupos_" + lang + ".txt";
        return null;
    }

    // --------------------------------------------------------------- download

    /**
     * Replaces storageRef.getFile(dest). Completes with the number of bytes written.
     */
    public static Task<Long> download(Context ctx, String storagePath, File dest) {
        final TaskCompletionSource<Long> tcs = new TaskCompletionSource<>();
        final String path = clean(storagePath);
        final Context app = ctx.getApplicationContext();

        if (isStarterFile(path)) {
            IO.execute(() -> {
                try (InputStream in = app.getAssets().open(SEED_ASSET_DIR + path)) {
                    long n = copy(in, dest);
                    Log.d(TAG, "starter file from app: " + path + " (" + n + " bytes)");
                    tcs.setResult(n);
                } catch (IOException e) {
                    // Only English (and Spanish) are bundled. Try the closest bundled file:
                    // 1) OTTAA's Spanish board per gender "pictos/es/pictos_es_f.txt" (not
                    //    bundled; a Spanish tablet got stuck on the name page) -> "pictos/pictos_es.txt"
                    // 2) any other language -> English.
                    java.util.List<String> tries = new java.util.ArrayList<>();
                    String general = path.replaceAll("/([a-z]{2})/(\\w+)_\\1_\\w+\\.txt$", "/$2_$1.txt");
                    if (!general.equals(path)) tries.add(general);
                    String en = (general.equals(path) ? path : general).replaceAll("_[a-z]{2}\\.txt$", "_en.txt");
                    if (!en.equals(path) && !tries.contains(en)) tries.add(en);
                    for (String alt : tries) {
                        try (InputStream in2 = app.getAssets().open(SEED_ASSET_DIR + alt)) {
                            long n = copy(in2, dest);
                            Log.w(TAG, "starter " + path + " not bundled, used " + alt);
                            tcs.setResult(n);
                            return;
                        } catch (IOException ignored) { }
                    }
                    Log.e(TAG, "starter file missing in app: " + path);
                    tcs.setException(new FileNotFoundException("Not bundled in app: " + path));
                }
            });
        } else if (isUserTextFile(path)) {
            DatabaseReference ref = backupRef(path);
            if (ref == null) {
                tcs.setException(new IllegalStateException("Not signed in"));
            } else {
                ref.get().addOnSuccessListener(snapshot -> {
                    Object value = snapshot.getValue();
                    if (!(value instanceof String)) {
                        // No backup in the database (e.g. an old OTTAA account whose backup
                        // lived in Cloud Storage). For the board and groups, fall back to the
                        // starter files in the app so sign-in never hangs.
                        String starter = starterFallback(path);
                        if (starter != null) {
                            Log.w(TAG, "no backup for " + path + ", using starter " + starter);
                            download(app, starter, dest)
                                    .addOnSuccessListener(tcs::setResult)
                                    .addOnFailureListener(tcs::setException);
                        } else {
                            tcs.setException(new FileNotFoundException("No backup yet: " + path));
                        }
                        return;
                    }
                    IO.execute(() -> {
                        try (OutputStream out = new FileOutputStream(dest)) {
                            byte[] bytes = ((String) value).getBytes(StandardCharsets.UTF_8);
                            out.write(bytes);
                            Log.d(TAG, "backup restored from database: " + path + " (" + bytes.length + " bytes)");
                            tcs.setResult((long) bytes.length);
                        } catch (IOException e) {
                            tcs.setException(e);
                        }
                    });
                }).addOnFailureListener(tcs::setException);
            }
        } else {
            tcs.setException(new PhotosStayOnPhoneException(path));
        }
        return tcs.getTask();
    }

    // ----------------------------------------------------------------- upload

    /**
     * Replaces storageRef.putStream(in) + getDownloadUrl(). Completes with a pseudo URL
     * ("eraka-db://<path>") that callers store in the database exactly like the old
     * download URL (OTTAA only checks that the URL_... field exists).
     */
    public static Task<Uri> upload(Context ctx, String storagePath, InputStream in) {
        final TaskCompletionSource<Uri> tcs = new TaskCompletionSource<>();
        final String path = clean(storagePath);
        if (!isUserTextFile(path)) {
            tcs.setException(new PhotosStayOnPhoneException(path));
            return tcs.getTask();
        }
        final DatabaseReference ref = backupRef(path);
        if (ref == null) {
            tcs.setException(new IllegalStateException("Not signed in"));
            return tcs.getTask();
        }
        // Read now (on the caller's thread): some OTTAA callers close the stream right
        // after this call returns. The files are small JSON text (a few hundred KB at most).
        final String text;
        try {
            text = readAll(in);
        } catch (IOException e) {
            tcs.setException(e);
            return tcs.getTask();
        }
        ref.setValue(text).addOnSuccessListener(v -> {
            Log.d(TAG, "backup saved to database: " + path + " (" + text.length() + " chars)");
            tcs.setResult(Uri.parse("eraka-db://" + path));
        }).addOnFailureListener(tcs::setException);
        return tcs.getTask();
    }

    /** Same as upload() but for a file on disk. */
    public static Task<Uri> upload(Context ctx, String storagePath, File file) {
        try {
            return upload(ctx, storagePath, new java.io.FileInputStream(file));
        } catch (FileNotFoundException e) {
            TaskCompletionSource<Uri> tcs = new TaskCompletionSource<>();
            tcs.setException(e);
            return tcs.getTask();
        }
    }

    // ---------------------------------------------------------------- helpers

    private static long copy(InputStream in, File dest) throws IOException {
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        long total = 0;
        try (OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[16 * 1024];
            int r;
            while ((r = in.read(buf)) > 0) {
                out.write(buf, 0, r);
                total += r;
            }
        }
        return total;
    }

    private static String readAll(InputStream in) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[16 * 1024];
        int r;
        while ((r = in.read(buf)) > 0) bos.write(buf, 0, r);
        in.close();
        return new String(bos.toByteArray(), StandardCharsets.UTF_8);
    }
}
