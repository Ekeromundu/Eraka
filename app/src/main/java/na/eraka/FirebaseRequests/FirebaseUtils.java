package na.eraka.FirebaseRequests;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;

public final class FirebaseUtils {

    private static FirebaseUtils instance;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private DatabaseReference rootRef;

    private boolean initialized = false;

    private String Uid = "";
    private String email = "";

    // Set UID
    public void setUid(String uid) {
        this.Uid = uid;
    }

    // Get UID
    public String getUid() {
        return Uid;
    }

    // Set email
    public void setEmail(String email) {
        this.email = email;
    }

    // Get email
    public String getEmail() {
        return email;
    }


    // Set FirebaseAuth manually
    public FirebaseUtils setAuth(FirebaseAuth auth) {
        this.auth = auth;
        return this;
    }

    // Set FirebaseStorage manually
    public FirebaseUtils setStorage(FirebaseStorage storage) {
        this.storage = storage;
        return this;
    }

    // If you want, you can also add setDatabase(FirebaseDatabase db)
// similar to old code, though usually initFirebaseAndEmulator() handles it.
    public FirebaseUtils setDatabase(FirebaseDatabase db) {
        this.database = db;
        this.rootRef = db.getReference();
        return this;
    }

    // 🔒 Private constructor
    private FirebaseUtils() {}

    // ✅ Thread-safe singleton
    public static synchronized FirebaseUtils getInstance() {
        if (instance == null) {
            instance = new FirebaseUtils();
        }
        return instance;
    }

    /**
     * MUST be called ONCE before any Firebase usage
     *
     * FIX: the emulator host used to be a hardcoded IP string in this method, which
     * silently broke every time the dev machine's LAN IP changed (new Wi-Fi, DHCP
     * renewal, different location) — Firebase calls would just hang/fail with no
     * obvious error on the device side. It now reads from
     * BuildConfig.FIREBASE_EMULATOR_HOST (set in app/build.gradle), so there's exactly
     * one place to update when the IP changes, instead of hunting through source files.
     */
    public synchronized void initFirebaseAndEmulator() {
        if (initialized) return;

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        // Eraka: LIVE or EMULATOR is chosen in FirebaseServerConfig (gradle.properties default,
        // changeable on the phone in debug builds: long-press the logo on the sign-in screen).
        android.content.Context ctx = null;
        try {
            ctx = com.google.firebase.FirebaseApp.getInstance().getApplicationContext();
        } catch (Exception e) {
            Log.e("FirebaseUtils", "No FirebaseApp context, using LIVE Firebase", e);
        }
        if (ctx != null && FirebaseServerConfig.useEmulator(ctx)) {
            String host = FirebaseServerConfig.getHost(ctx);
            Log.d("FirebaseUtils", "Using Firebase EMULATOR at " + host);
            try {
                auth.useEmulator(host, FirebaseServerConfig.PORT_AUTH);
                database.useEmulator(host, FirebaseServerConfig.PORT_DATABASE);
                storage.useEmulator(host, FirebaseServerConfig.PORT_STORAGE);
            } catch (IllegalStateException e) {
                // Firebase was already used before this call; the choice applies after a restart.
                Log.e("FirebaseUtils", "Emulator must be set before first Firebase use", e);
            }
        } else {
            Log.d("FirebaseUtils", "Using LIVE Firebase");
        }

        rootRef = database.getReference();
        initialized = true;
    }

    public DatabaseReference checkFirebaseNetworkConnection(){
        return FirebaseDatabase.getInstance().getReference(".info/connected");
    }

    public FirebaseAuth auth() {
        ensureInitialized();
        return auth;
    }

    public FirebaseDatabase database() {
        ensureInitialized();
        return database;
    }

    public FirebaseStorage storage() {
        ensureInitialized();
        return storage;
    }

    public DatabaseReference root() {
        ensureInitialized();
        return rootRef;
    }

    public DatabaseReference getmDatabase() {
        return root();
    }

    private void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException(
                    "FirebaseUtils not initialized. Call initialize() first."
            );
        }
    }
}
