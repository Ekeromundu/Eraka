package na.eraka;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.content.res.Configuration;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.Locale;

/**
 * Eraka application class (16 Oct 2026).
 *
 * Tablets (smallest screen width >= 600 dp) are used in landscape, so every screen is
 * switched to landscape there (either landscape direction, following the sensor).
 * Phones keep the orientation set in the manifest (portrait for the Eraka screens).
 */
public class ErakaApp extends Application {

    /**
     * Size of the whole physical screen, not of the app's window: when Android shows a
     * portrait app letterboxed on a landscape tablet, the window is narrow and would wrongly
     * look like a phone.
     */
    static boolean isTablet(Activity activity) {
        android.util.DisplayMetrics dm = new android.util.DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        float smallestDp = Math.min(dm.widthPixels, dm.heightPixels) / dm.density;
        return smallestDp >= 600;
    }

    static void landscapeOnTablet(Activity activity) {
        boolean tablet = isTablet(activity);
        // The splash and the main board have no orientation in the manifest (so tablets open
        // them in landscape straight away, without a rebuild); on phones they are kept portrait
        // here, like every other Eraka screen.
        boolean noManifestOrientation =
                activity instanceof na.eraka.welcome.SplashActivity
                || activity instanceof MainBoardActivity;
        if (!tablet && !noManifestOrientation) return;
        try {
            activity.setRequestedOrientation(tablet
                    ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } catch (Exception e) {
            // Android 8.0 refuses this for translucent screens: keep their orientation
            Log.w("ErakaApp", "orientation: " + e.getMessage());
        }
    }

    /** Eraka is English-only: voice (Whisper tiny.en), vocabulary fixes, base model, sentences. */
    public static final String APP_LANGUAGE = "en";

    /**
     * Eraka (19 Oct 2026): always English, whatever the device language. OTTAA chose the
     * board file and texts from the device language, so a Spanish tablet got Spanish
     * pictograms while Eraka's features only work in English.
     * - Locale.setDefault: every Locale.getDefault() in OTTAA's code (board files, sign-up)
     *   now says "en".
     * - pref "idioma" = "en": the screens that wrap their context with this pref use English.
     * Android resets the default locale when the device configuration changes, so this is
     * applied again in onConfigurationChanged and before every screen is created.
     */
    void forceEnglish() {
        if (!APP_LANGUAGE.equals(Locale.getDefault().getLanguage())) {
            Locale.setDefault(Locale.ENGLISH);
        }
        try {
            String key = getString(R.string.str_idioma);
            android.content.SharedPreferences p =
                    android.preference.PreferenceManager.getDefaultSharedPreferences(this);
            if (!APP_LANGUAGE.equals(p.getString(key, ""))) {
                p.edit().putString(key, APP_LANGUAGE).apply();
            }
        } catch (Exception e) {
            Log.w("ErakaApp", "language pref: " + e.getMessage());
        }
        na.eraka.idioma.AppLanguage.setLanguage(APP_LANGUAGE);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        forceEnglish();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        forceEnglish();
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            /** Android 10+: runs BEFORE the screen's window is made, so no restart is needed. */
            @Override
            public void onActivityPreCreated(Activity activity, Bundle savedInstanceState) {
                forceEnglish();
                landscapeOnTablet(activity);
            }
            /** Older Android: there is no "pre" step, so set it here (as early as possible). */
            @Override
            public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) forceEnglish();
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
                    landscapeOnTablet(activity);
                }
            }
            @Override public void onActivityStarted(Activity activity) { }
            @Override public void onActivityResumed(Activity activity) { }
            @Override public void onActivityPaused(Activity activity) { }
            @Override public void onActivityStopped(Activity activity) { }
            @Override public void onActivitySaveInstanceState(Activity activity, Bundle outState) { }
            @Override public void onActivityDestroyed(Activity activity) { }
        });
    }
}
