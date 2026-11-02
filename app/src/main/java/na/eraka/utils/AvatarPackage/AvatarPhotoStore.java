package na.eraka.utils.AvatarPackage;

import android.content.Context;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.signature.ObjectKey;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Keeps the child's own avatar photo on the device, so it shows everywhere
 * (avatar page, side menu, floating avatar) even with no internet.
 * Firebase upload still happens separately as a backup.
 */
public final class AvatarPhotoStore {

    private static final String TAG = "AvatarPhotoStore";
    private static final String PREF_HAS_PHOTO = "userAvatarPhoto";
    private static final String FILE_NAME = "avatar_photo.png";

    private AvatarPhotoStore() { }

    public static File file(Context ctx) {
        return new File(ctx.getFilesDir(), FILE_NAME);
    }

    /** True when the child chose their own photo (instead of an illustrated avatar). */
    public static boolean hasPhoto(Context ctx) {
        return PreferenceManager.getDefaultSharedPreferences(ctx).getBoolean(PREF_HAS_PHOTO, false)
                && file(ctx).exists();
    }

    /** Copies the cropped picture into app storage and marks it as the current avatar. */
    public static boolean saveFromUri(Context ctx, Uri source) {
        if (source == null) return false;
        File target = file(ctx);
        try (InputStream in = ctx.getContentResolver().openInputStream(source);
             OutputStream out = new FileOutputStream(target)) {
            if (in == null) return false;
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        } catch (Exception e) {
            Log.e(TAG, "saveFromUri: could not copy avatar photo", e);
            return false;
        }
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(PREF_HAS_PHOTO, true).apply();
        return true;
    }

    /** Called when the child picks an illustrated avatar instead. */
    /** Eraka 27 Oct 2026: a copy of the current photo, so "Cancel" on the avatar page can undo. */
    public static File backup(Context ctx) {
        if (!hasPhoto(ctx)) return null;
        File b = new File(ctx.getCacheDir(), "avatar_photo_backup.png");
        try (java.io.InputStream in = new java.io.FileInputStream(file(ctx));
             java.io.OutputStream out = new java.io.FileOutputStream(b)) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    /** Puts a backup made by {@link #backup} back, or removes the photo when there was none. */
    public static void restore(Context ctx, File backup) {
        if (backup == null || !backup.exists()) { clear(ctx); return; }
        try (java.io.InputStream in = new java.io.FileInputStream(backup);
             java.io.OutputStream out = new java.io.FileOutputStream(file(ctx))) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(PREF_HAS_PHOTO, true).apply();
        } catch (Exception e) {
            clear(ctx);
        }
    }

    public static void clear(Context ctx) {
        PreferenceManager.getDefaultSharedPreferences(ctx).edit().putBoolean(PREF_HAS_PHOTO, false).apply();
        //noinspection ResultOfMethodCallIgnored
        file(ctx).delete();
    }

    /** Shows the saved photo as a circle. The signature makes Glide refresh when the photo changes. */
    public static void loadInto(Context ctx, ImageView target) {
        File f = file(ctx);
        Glide.with(ctx)
                .load(f)
                .signature(new ObjectKey(f.lastModified()))
                .circleCrop()
                .into(target);
    }
}
