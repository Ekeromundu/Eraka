package na.eraka.FirebaseRequests;

import android.content.Context;
import android.content.SharedPreferences;

import na.eraka.BuildConfig;

/**
 * Eraka: which Firebase the app talks to.
 *
 *  LIVE     = the real online project (otta-project-1ee57). Needs internet only.
 *  EMULATOR = the Firebase emulators running on the developer's laptop
 *             (same Wi-Fi, laptop awake, "firebase emulators:start" running).
 *
 * The starting value comes from gradle.properties (erakaFirebaseMode / erakaEmulatorHost).
 * In DEBUG builds it can be changed on the phone: long-press the logo on the sign-in
 * screen (FirebaseServerDialog). The phone's choice wins over gradle.properties.
 * RELEASE builds (the APK for participants) are always LIVE.
 *
 * Firebase only lets you choose the server BEFORE the first database/auth call,
 * so a change takes effect after the app restarts.
 */
public final class FirebaseServerConfig {

    public static final String MODE_LIVE = "live";
    public static final String MODE_EMULATOR = "emulator";

    // Ports must match firebase.json -> "emulators"
    public static final int PORT_AUTH = 9099;
    public static final int PORT_DATABASE = 9000;
    public static final int PORT_STORAGE = 9198;

    private static final String PREFS = "eraka_firebase_server";
    private static final String KEY_MODE = "mode";
    private static final String KEY_HOST = "host";

    private FirebaseServerConfig() { }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** true only in debug builds when EMULATOR is chosen and a host is set. */
    public static boolean useEmulator(Context ctx) {
        if (!BuildConfig.DEBUG) return false;
        return MODE_EMULATOR.equals(getMode(ctx)) && !getHost(ctx).isEmpty();
    }

    public static String getMode(Context ctx) {
        if (!BuildConfig.DEBUG) return MODE_LIVE;
        String def = BuildConfig.FIREBASE_MODE_DEFAULT;
        if (!MODE_EMULATOR.equals(def)) def = MODE_LIVE;
        return prefs(ctx).getString(KEY_MODE, def);
    }

    public static String getHost(Context ctx) {
        String def = BuildConfig.FIREBASE_EMULATOR_HOST == null ? "" : BuildConfig.FIREBASE_EMULATOR_HOST;
        String host = prefs(ctx).getString(KEY_HOST, def);
        return host == null ? "" : host.trim();
    }

    /** Saves synchronously (commit), because the app is restarted right after. */
    public static void save(Context ctx, String mode, String host) {
        prefs(ctx).edit()
                .putString(KEY_MODE, MODE_EMULATOR.equals(mode) ? MODE_EMULATOR : MODE_LIVE)
                .putString(KEY_HOST, host == null ? "" : host.trim())
                .commit();
    }

    /** Short text for toasts/logs, e.g. "LIVE Firebase" or "EMULATOR 192.168.1.23". */
    public static String describe(Context ctx) {
        return useEmulator(ctx) ? "EMULATOR " + getHost(ctx) : "LIVE Firebase";
    }
}
