package na.eraka;



import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Build;
import android.widget.LinearLayout;
import android.os.Process;
import na.eraka.SmartRow.SmartRowItem;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;


import com.airbnb.lottie.LottieAnimationView;
import com.google.android.exoplayer2.transformer.Transformer;
import com.google.android.gms.common.api.internal.ConnectionCallbacks;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;
import com.google.firebase.installations.FirebaseInstallations;
import com.google.firebase.perf.metrics.AddTrace;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import na.eraka.Activities.Phrases.FavoritePhrases;
import na.eraka.Activities.Phrases.MostUsedPhrases;
import na.eraka.Backup.BackupGroups;
import na.eraka.Backup.BackupPhotos;
import na.eraka.Backup.BackupPhrases;
import na.eraka.Backup.BackupPictograms;
import na.eraka.Dialogos.DialogUtils.ProgressDialogOptions;
import na.eraka.Dialogos.DialogUtils.YesNoDialog;
import na.eraka.Dialogos.newsDialog.NewsDialogs;
import na.eraka.FirebaseRequests.DownloadJsonFromFirebase;
import na.eraka.FirebaseRequests.FirebaseUtils;
import na.eraka.FirebaseRequests.FirebaseUploader;
import na.eraka.FirebaseRequests.FirebaseBackupUploader;
import na.eraka.Interfaces.AudioTransformationListener;
import na.eraka.Interfaces.FailReadPictogramOrigin;
import na.eraka.Interfaces.FirebaseSuccessListener;
import na.eraka.Interfaces.LoadPictograms;
import na.eraka.Interfaces.TimedClickListener;
import na.eraka.Interfaces.PlaceSuccessListener;
import na.eraka.Interfaces.SortPictogramsInterface;
import na.eraka.Interfaces.TTSListener;
import na.eraka.JSONutils.Json;
import na.eraka.JSONutils.JsonRecovery;
import na.eraka.SmartRow.ErakaSmartRowDataSource;
import na.eraka.erakaprediction.ErakaPredictionService;
import na.eraka.SmartRow.SmartRowView;
import na.eraka.idioma.AppLanguage;
import na.eraka.idioma.LanguageContextWrapper;
import na.eraka.utils.Accesibilidad.ScreenScanning;
import na.eraka.utils.Accesibilidad.Gesture;
import na.eraka.utils.Accesibilidad.SayActivityName;
import na.eraka.utils.Accesibilidad.devices.PrincipalControls;
import na.eraka.utils.Accesibilidad.scrollActions.ScrollFunctionMainActivity;
import na.eraka.utils.Audio.FileEncoder;
import na.eraka.utils.AvatarPackage.Avatar;
import na.eraka.utils.AvatarPackage.AvatarUtils;
import na.eraka.utils.ConnectionDetector;
import na.eraka.utils.CustomToast;
import na.eraka.utils.EnumImageView;
import na.eraka.utils.Firebase.AnalyticsFirebase;
import na.eraka.utils.Firebase.CrashlyticsUtils;
import na.eraka.utils.Handlers.MainBoardHandler;
import na.eraka.utils.ImmersiveMode;
import na.eraka.utils.IntentCode;
import na.eraka.utils.JSONutils;
import na.eraka.utils.UserLicence.UserLicence;
import na.eraka.utils.MovableFloatingActionButton;
import na.eraka.utils.ObservableInteger;
import na.eraka.utils.PopupMenuUtils;
import na.eraka.utils.RemoteConfigUtils;
import na.eraka.utils.TalkActions.MessageHistory;
import na.eraka.utils.constants.Constants;
import na.eraka.utils.constants.ConstantsAnalyticsValues;
import na.eraka.utils.constants.ConstantsMainActivity;
import na.eraka.utils.exceptions.FiveMbException;
import na.eraka.utils.location.PlacesImplementation;
import na.eraka.utils.preferences.User;
import na.eraka.utils.WhisperManager;
import na.eraka.utils.ErakaUsageLog;
import na.eraka.utils.SpeechOutput;
import na.eraka.utils.PictogramClickTimer;
import com.stonefacesoft.pictogramslibrary.Classes.Pictogram;
import com.stonefacesoft.pictogramslibrary.CloudStorageManager;
import com.stonefacesoft.pictogramslibrary.utils.GlideAttatcher;
import com.stonefacesoft.pictogramslibrary.utils.ValidateContext;
import com.stonefacesoft.pictogramslibrary.view.PictoView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import na.eraka.Activities.Eraka.ErakaAboutActivity;
import na.eraka.Activities.Eraka.ErakaBackupActivity;
import na.eraka.Activities.Eraka.ErakaFavouritesActivity;
import na.eraka.Activities.Eraka.ErakaGamesActivity;
import na.eraka.Activities.Eraka.ErakaVoiceActivity;
import na.eraka.Activities.Eraka.ErakaHelpActivity;
import na.eraka.Activities.Eraka.ErakaReportActivity;
import na.eraka.utils.CaregiverGate;
import na.eraka.utils.ErakaPlaces;
import na.eraka.utils.VoiceDictionary;
import na.eraka.utils.VoiceSuggestions;
import na.eraka.utils.WhisperButtonAnimator;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * V93 en produccion con hotfix
 */
public class MainBoardActivity extends AppCompatActivity implements View
        .OnClickListener,
        View.OnLongClickListener,
        OnMenuItemClickListener,
        FirebaseSuccessListener, NavigationView.OnNavigationItemSelectedListener, PlaceSuccessListener, ConnectionCallbacks, View.OnTouchListener, TimedClickListener , SortPictogramsInterface, LoadPictograms, AudioTransformationListener {

    private static final String TAG = "Principal";
    public static boolean cerrarSession = false;// use this variable to notify when the session is closed

    private final Handler handlerHablar = new Handler();
    public volatile Json json;
    // JSONObject que usamoss
    JSONObject pictoPadre, opcion1, opcion2, opcion3, opcion4, onLongOpcion;
    FirebaseUploader subirArchivos;
    String timeStamp;
    private Avatar avatar;
    private MovableFloatingActionButton movableFloatingActionButton;
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView navigationView;
    private PrincipalControls navigationControls;
    //Declaracion de los botones

    private PictoView Opcion1;
    private PictoView Opcion2;
    private PictoView Opcion3;
    private PictoView Opcion4;
    private SmartRowView smartRowView;
    private PictogramClickTimer Opcion1_clicker;
    private PictogramClickTimer Opcion2_clicker;
    private PictogramClickTimer Opcion3_clicker;
    private PictogramClickTimer Opcion4_clicker;
    private ErakaSmartRowDataSource smartRowDataSource;

    // Whisper integration
    private WhisperManager whisperManager;
    private FloatingActionButton btnWhisper;

    //Declaracion de variables del TTS
    private User user;
    private boolean isSettings;
    private Button btnBarrido;
    private MenuItem locationItem;
    private ScrollFunctionMainActivity function_scroll;
    private FloatingActionButton talk;
    private DownloadJsonFromFirebase downloadJsonFromFirebase;
    private boolean mute; // this boolean show the sound status
    private SharedPreferences sharedPrefs, sharedPrefsDefault;
    private MessageHistory historial;
    private boolean doubleBackToExitPressedOnce = false;
    private String Oracion = "";
    private int versionCode;
    //Indica el proximo lugar en la seleccion
    private int CantClicks;
    //Handler para animar el Hablar cuando pasa cierto tiempo
    private final Runnable animarHablar = new Runnable() {
        @Override
        public void run() {
            AnimarHablar();
            handlerHablar.postDelayed(this, 4000);
        }
    };
    private PictoView Agregar;

    private boolean editarPicto;

    private int cuentaMasPictos;
    private int placeTypeActual;
    private int placeActual;
    private boolean vibrar = false;
    private StorageReference mStorageRef;
    private LottieAnimationView animationView;
    private SpeechOutput myTTS;
    private FirebaseBackupUploader mFirebaseBackup;
    private Toolbar toolbar;
    private int mCheckDescarga, mCheckDatos;
    private LinearLayout constraintBotonera;
    private LocationRequest mLastLocationRequest;
    private ScreenScanning barridoPantalla;
    private AnalyticsFirebase analitycsFirebase;
    private FirebaseUtils firebaseUtils;
    private TextView toolbarPlace;
    private PlacesImplementation placesImplementation;
    private ProgressDialogOptions firebaseDialog;

    private RemoteConfigUtils remoteConfigUtils;

    private ImmersiveMode inmersiveMode;
    private Gesture gesture;
    private boolean TutoFlag;
    private ImageView menuAvatarIcon;
    private AvatarUtils avatarUtils;
    private ImageButton borrar;
    private ImageButton masPictos;
    private ImageButton todosLosPictos;



    public void setOracion(String oracion) {
        if(oracion.startsWith("."))
            this.Oracion = oracion.replaceFirst(".","");
        else
            this.Oracion = oracion;
    }

    @Override
    public void onFotoDescargada(int fotosDescargadas) {

    }

    @Override
    public void onArchivosSubidos(boolean subidos) {

    }

    @Override
    public void onPictosSugeridosBajados(boolean descargado) {
        if (descargado) {
            descargado = false;
            Log.e("PictosDownload", "onPictosSugeridosBajados Images successfully downloaded");
            if (getFirebaseDialog().isShowing())
                getFirebaseDialog().destruirDialogo();

            try {
                json.setmJSONArrayPictosSugeridos(json.readJSONArrayFromFile(Constants.ARCHIVO_PICTOS_DATABASE));
            } catch (JSONException | FiveMbException e) {
                e.printStackTrace();
            }
        }
    }

    private void explicarSugerencias() {
        PackageInfo pInfo = null;
        try {
            pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            versionCode = pInfo.versionCode;
            if (versionCode >= 54) {
                new NewsDialogs(this).showSugerenciasDialog(getString(R.string.string_sugerencias_dialog));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDatosEncontrados(int datosEncontrados) {
        mCheckDatos += datosEncontrados;
        Log.d(TAG, "onDatosEncontrados: " + mCheckDatos);

        if (mCheckDatos == Constants.TODO_ENCONTRADO) {
            Log.d(TAG, "Todo Encontrado: "+ mCheckDatos);
            mCheckDatos = 0;
            try {
                // Eraka 28 Oct 2026: OTTAA downloaded the whole account from the cloud every time the
                // board opened (and first parsed the whole pictures file just to see that it was
                // not empty), all on the main thread: "Syncing…", a black screen and "Eraka isn't
                // responding" on the A13, and the cloud copy could overwrite changes made offline.
                // Eraka works from the phone's own files; the cloud copy is only used when this
                // phone has no pictures yet (or with Menu -> Backup & restore).
                java.io.File localPictos = new java.io.File(getFilesDir(), Constants.ARCHIVO_PICTOS);
                if (localPictos.length() > 2) {
                    Log.d(TAG, "onDatosEncontrados: pictures are on the phone, no start-up sync");
                    return;
                }
                if (!this.isFinishing() && ConnectionDetector.isNetworkAvailable(this)) {
                    downloadJsonFromFirebase.setInterfaz(this);
                    getFirebaseDialog().setTitle(getApplicationContext().getResources().getString(R.string.edit_sync)).setMessage(getApplicationContext().getResources().getString(R.string.edit_sync_pict)).setCancelable(false);
                    getFirebaseDialog().mostrarDialogo();
                    downloadJsonFromFirebase.syncFiles();
                }
            } catch (Exception exe){   // Eraka: the file is no longer parsed here, so only this one
                Log.e(TAG, "onDatosEncontrados: "+exe.getMessage() );
                exe.printStackTrace();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "principal");
        Log.d("ABI_CHECK", "Supported ABIs: " + java.util.Arrays.toString(Build.SUPPORTED_ABIS));
        Log.d("ABI_CHECK", "Is 64-bit process: " + Process.is64Bit());
        Intent intent = getIntent();
        boolean status_bar = intent.getBooleanExtra("status_bar", false);
        if (!status_bar) {
            Log.e(TAG, "Not status bar");
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        Log.e(TAG, "Not status bar 1");
        super.onCreate(savedInstanceState);
        Log.e(TAG, "Not status bar 2");
        sharedPrefsDefault = PreferenceManager.getDefaultSharedPreferences(this);
        Log.e(TAG, "Not status bar 3");
        long tLayout = na.eraka.utils.ErakaPerf.start();
        prepareLayout();
        na.eraka.utils.ErakaPerf.end("board_init_layout", tLayout);
        Log.e(TAG, "Not status bar 4");
        //Facebook analytics

        FirebaseInstallations.getInstance().getId().addOnCompleteListener(new OnCompleteListener<String>() {
            @Override
            public void onComplete(@NonNull Task<String> task) {
                Log.e(TAG, "Firebase installations");
                if (task.isSuccessful()) {
                    Log.d("Installations", "Installation ID: " + task.getResult());
                } else {
                    Log.e("Installations", "Unable to get Installation ID");
                }
            }
        });
        initComponents();
        System.gc();
    }

    @Override
    public void onDescargaCompleta(int termino) {
        if (termino == Constants.TODO_DESCARGADO) {
            mCheckDescarga = 0;
            // Eraka 28 Oct 2026: after the first sign-in the downloaded files are read in the
            // background (5-6 s on the A13), while the "please wait" dialog stays up. On the main
            // thread this froze the screen and Android showed "Eraka isn't responding".
            new Thread(() -> {
                json.refreshJsonArrays();
                runOnUiThread(() -> {
                    if (isFinishing()) return;
                    initFirstPictograms();
                    getFirebaseDialog().destruirDialogo();
                });
            }, "eraka-load-downloaded").start();
            return;
        }
        getFirebaseDialog().destruirDialogo();


    }

    private boolean resolveIntent(String action) {
        PackageManager pm = getPackageManager();
        Intent installIntent = new Intent();
        installIntent.setAction(action);
        ResolveInfo resolveInfo = pm.resolveActivity(installIntent, PackageManager.MATCH_DEFAULT_ONLY);

        return resolveInfo != null;
    }

    /**
     * Crea una dialogo con el mensaje de error deseado
     */
    public void Alert() {
        AlertDialog.Builder dialogo1 = new AlertDialog.Builder(MainBoardActivity.this);
        dialogo1.setTitle(getResources().getString(R.string.pref_important_alert));
        dialogo1.setCancelable(false);
        dialogo1.setMessage(getResources().getString(R.string.pref_error_112));
        dialogo1.setPositiveButton(getResources().getString(R.string.pref_yes_alert), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogo1, int id) {
                finish();
            }
        });
        AlertDialog dialog = dialogo1.create();
        dialog.show();
    }

    ///////////////////////// Funcion para asignar el ancho a seleccion /////////////////

    private void AjustarAncho(int Rid) {
        ImageButton view_instance = findViewById(Rid);
        android.view.ViewGroup.LayoutParams params = view_instance.getLayoutParams();
        params.width = view_instance.getLayoutParams().height;
        view_instance.setLayoutParams(params);
        ImageButton button = new ImageButton(getApplicationContext());
        button.setLayoutParams(params);
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.item_edit:
                getAnalyticsFirebase().customEvents("Touch", "Principal", "Edit Pictogram");
                // Premium gate removed: any user can edit an existing pictogram now,
                // not just premium (consistent with pictogram/group creation above).
                if (onLongOpcion == null) {
                    return true;
                }
                Intent intent = new Intent(MainBoardActivity.this, EditPictogramActivity.class);
                intent.putExtra("principal", true);
                try {
                    intent.putExtra("PictoID", json.getId(onLongOpcion));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                intent.putExtra("Texto", JSONutils.getNombre(onLongOpcion, sharedPrefsDefault.getString(getString(R.string.str_idioma), "en")));
                intent.putExtra("Sel", 1);
                intent.putExtra("Nombre", JSONutils.getNombre(onLongOpcion, sharedPrefsDefault.getString(getString(R.string.str_idioma), "en")));
                intent.putExtra("Color", cargarColor(JSONutils.getTipo(onLongOpcion)));
                myTTS.hablar(getString(R.string.editar_pictogram));

                //Abrir pantalla de edicion de pictograma
                isSettings = false;
                startActivityForResult(intent, IntentCode.EDITARPICTO.getCode());
                return true;
            case R.id.item_delete:
                getAnalyticsFirebase().customEvents("Touch", "Principal", "Delete Pictogram");
                try {
                    if (onLongOpcion != null)
                        AlertBorrar(json.getId(onLongOpcion));
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                return true;

        }
        return false;
    }



    private void setPrimerBackupTimeLocal() {
        //Nos fijamos si el permiso de escribir en el storage esta dado para hacer el backup local.
        BackupPictograms backupPictograms = new BackupPictograms(this, Constants.ARCHIVO_PICTOS);
        Log.e(TAG, "Backed up pictos " );
        BackupGroups backupGroups = new BackupGroups(this, Constants.ARCHIVO_GRUPOS);
        Log.e(TAG, "Backed up grupos " );
        BackupPhrases backupPhrases = new BackupPhrases(this, Constants.ARCHIVO_FRASES);
        Log.e(TAG, "Backing up frases " );
        BackupPhotos backupPhotos = new BackupPhotos(this, Constants.ARCHIVO_PICTOS);
        backupPictograms.prepareFirstLocalBackup(timeStamp);
        backupGroups.prepareFirstLocalBackup(timeStamp);
        backupPhrases.prepareFirstLocalBackup(timeStamp);
        backupPhotos.prepareFirstLocalBackup(timeStamp);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (sharedPrefsDefault.getBoolean("bool_ubicacion", false)) {
            startLocationUpdate();
            //Reset();
        }
    }

    private void initLocationRequest() {
        mLastLocationRequest = LocationRequest.create();
        mLastLocationRequest.setSmallestDisplacement(10);
        mLastLocationRequest.setInterval(1000); // Update location every 1 minute
        mLastLocationRequest.setFastestInterval(1000);
        mLastLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    private void startLocationUpdate() {
        initLocationRequest();
        if (placesImplementation != null && sharedPrefsDefault.getBoolean(getString(R.string.bool_ubicacion), false)) {
            if (!placesImplementation.isStarted())
                placesImplementation.iniciarClientePlaces();
            else{
                placesImplementation.locationRequest();
                useLocation();
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    private String getTimeStamp() {
        Long tslong = System.currentTimeMillis() / 1000;
        return tslong.toString();
    }

    //////////////////////////////  Barrido Fin  ////////////////////////////////////////////

    private void guardarBackupLocal() {
        BackupPictograms backupPictograms = new BackupPictograms(this, Constants.ARCHIVO_PICTOS);
        BackupGroups backupGroups = new BackupGroups(this, Constants.ARCHIVO_GRUPOS);
        BackupPhrases backupPhrases = new BackupPhrases(this, Constants.ARCHIVO_FRASES);
        BackupPhotos backupPhotos = new BackupPhotos(this, Constants.ARCHIVO_PICTOS);

        backupPictograms.preparelocalBackup(timeStamp);
        backupGroups.preparelocalBackup(timeStamp);
        backupPhrases.preparelocalBackup(timeStamp);
        backupPhotos.preparelocalBackup(timeStamp);
    }



    @Override
    protected void onStart() {
        super.onStart();
        if (user!= null){
            if(!user.isConnected())
                user.connectClient();
        }


    }

    @Override
    protected void onResume() {
        Log.e(TAG, "onResume");
        CargarJson();
        Log.e(TAG, "onResume method");
        super.onResume();
        myTTS = SpeechOutput.getInstance(this);
        getFirebaseDialog().destruirDialogo();
        updateDrawerGreeting(); // name may have been edited in settings
        if (placesImplementation != null) {
            placesImplementation.locationRequest();
        }
    }



    @Override
    protected void onRestart() {
        Log.e(TAG, "onRestart");
        super.onRestart();
        // Eraka 27 Oct 2026 (speed): no CargarJson() here - onResume() always follows onRestart()
        // and loads it, so the board was loaded twice on every return from another screen.
        startLocationUpdate();

    }

    @Override
    protected void onPause() {
        if (json.getmJSONArrayTodosLosPictos().length() > 0)
            if (!json.guardarJson(Constants.ARCHIVO_PICTOS)) {
                Log.e(TAG, "onPause: Error al guardar el json");
            }
        getFirebaseDialog().destruirDialogo();
        // Eraka: keep a CSV copy of the usage log up to date for the evaluation
        // (Android/data/<package>/files/eraka_usage/, readable over USB)
        final ErakaUsageLog log = usageLog();
        new Thread(() -> {
            java.io.File dir = log.exportCsv();
            Log.d(TAG, "usage log exported to " + dir);
        }).start();
        super.onPause();
    }


    /**
     * Implementa este metodo de TTS interface, cuando se inicializa el TTS lo setea por defecto
     */

    @Override
    protected void onStop() {
        if(json == null)
            json =  Json.getInstance();
        if (!json.getFallJson() && user != null) {
            if(user.getmAuth().getCurrentUser() != null)
                if (isSettings) {
                    subirArchivos.new getTiempoGoogle().execute();
                }
        }

        if (json.getmJSONArrayTodosLosPictos().length() > 0)
            if (!json.guardarJson(Constants.ARCHIVO_PICTOS)) {
                Log.e(TAG, "onStop: Error al guardar el json");
            }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        getFirebaseDialog().destruirDialogo();
        if (whisperManager != null) {
            whisperManager.release();
        }
        super.onDestroy();
    }

    /**
     * Crea el menu del ActionBar
     *
     * @param menu (Menu)
     * @return
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        if (mute) {
            menu.getItem(1).setIcon(getResources().getDrawable(R.drawable.ic_volume_up_white_24dp));
        } else {
            menu.getItem(1).setIcon(getResources().getDrawable(R.drawable.ic_volume_off_white_24dp));
        }


        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Reset();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 500);
    }

    /**
     * Carga el texto del picto seleccionado en la oracion final
     *
     * @param jsonObject (Picto)
     */


    void CargarOracion(JSONObject jsonObject, String idioma) {
        // Eraka 22 Oct 2026: the home word, if the parent gave one ("meme" for mom)
        Oracion = Oracion + na.eraka.utils.ErakaHomeWords.spoken(this, idOf(jsonObject),
                JSONutils.getNombre(jsonObject, idioma)) + " ";
        Log.d(TAG, "CargarOracion: " + Oracion);
    }

    /**
     * Carga el picto seleccionado en la barra de seleccion
     *
     * @param opcion
     */
    private void CargarSeleccion(JSONObject opcion) {
        Pictogram pictogram = new Pictogram(opcion, AppLanguage.getLanguaje());
        int value = CantClicks+1;
        if(CantClicks<=9){
            loadDrawable( pictogram, CantClicks);
            ImageView aux = getImageView(value);
            if(aux != null)
                aux.setImageDrawable(getResources().getDrawable(R.drawable.icono_ottaa));
        }
    }

    /**
     * Inicializa la barra de seleccion poniendo la imagen por defecto
     */
    private void inicializar_seleccion() {
        for (int i = 0; i < 10; i++) {
            getImageView(i).setImageDrawable(Agregar.getCustom_Imagen());
            getImageView(i).startAnimation(AnimationUtils.loadAnimation(this, R.anim.overshoot_arriba));
        }
    }

    private void loadOptions(Json json, JSONObject padre) {
        Log.e(TAG, "Loading Options");
        Animation alphaAnimation = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.alpha_show);

        Opcion1.setEnabled(true);
        Opcion2.setEnabled(true);
        Opcion3.setEnabled(true);
        Opcion4.setEnabled(true);
        if (json.getCantFallas() == 0) {
            Log.e(TAG, "json.getCantFallas() ==0 ");
            loadChilds(padre, alphaAnimation);
        }
        if(json.getCantFallas()<4 && json.getCantFallas()>0)
            downloadFailedFile(3);

    }

    public void loadChildOption(JSONArray opciones, int index, Animation alphaAnimation) {
        try {
            if (opciones.getJSONObject(index).getInt("id") != -1) {
                Log.d(TAG, "loadChildOption: Value"+index);
                selectJsonOption(index, opciones.getJSONObject(index), alphaAnimation);
            } else {
                Log.d(TAG, "loadChildOption: null ");
                loadOptionValue(index, null);
                addOpcionNull(returnOption(index), alphaAnimation);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void selectJsonOption(int position, JSONObject object, Animation alphaAnimation) {
        loadOptionValue(position, object);
        switch (position) {
            case 0:
                addOption(opcion1, returnOption(position), alphaAnimation);
                break;
            case 1:
                addOption(opcion2, returnOption(position), alphaAnimation);
                break;
            case 2:
                addOption(opcion3, returnOption(position), alphaAnimation);
                break;
            case 3:
                addOption(opcion4, returnOption(position), alphaAnimation);
                break;
        }
    }

    private PictoView returnOption(int position) {
        switch (position) {
            case 0:
                return Opcion1;
            case 1:
                return Opcion2;
            case 2:
                return Opcion3;
            case 3:
                return Opcion4;
            default:
                return Opcion1;
        }
    }

    private void loadOptionValue(int position, JSONObject value) {
        switch (position) {
            case 0:
                opcion1 = value;
                break;
            case 1:
                opcion2 = value;
                break;
            case 2:
                opcion3 = value;
                break;
            case 3:
                opcion4 = value;
                break;
        }
    }

    private Integer cargarColor(int tipo) {
        switch (tipo) {
            case 1:
                return getResources().getColor(R.color.Yellow);
            case 2:
                return getResources().getColor(R.color.Orange);
            case 3:
                return getResources().getColor(R.color.YellowGreen);
            case 4:
                return getResources().getColor(R.color.DodgerBlue);
            case 5:
                return getResources().getColor(R.color.Magenta);
            case 6:
                return getResources().getColor(R.color.Black);
            default:
                return getResources().getColor(R.color.Black);
        }
    }

    //Esta funcion vuelve al estado inicial
    private void Reset() {
        pictoPadre = historial.removePictograms(true);
        ResetSeleccion();
        cuentaMasPictos = 0;
        loadOptions(json, pictoPadre);
    }

    public void ResetSeleccion() {
        Log.d(TAG, "ResetSeleccion: ");
        Oracion = "";
        int situacionActual = 0;
        CantClicks = 0;
        inicializar_seleccion();
        if (!historial.getListadoPictos().isEmpty()) {
            for (int i = 0; i < historial.getListadoPictos().size(); i++) {
                CargarOracion(historial.getListadoPictos().get(i), sharedPrefsDefault.getString(getString(R.string.str_idioma), "en"));
            }
        }
    }



    private void volver() {
        pictoPadre = historial.removePictograms(false);
        ResetSeleccion();
        cuentaMasPictos = 0;
        for (int i = 0; i < historial.getListadoPictos().size(); i++) {
            CargarSeleccion(historial.getListadoPictos().get(i));
            CantClicks++;
        }
        loadOptions(json, pictoPadre);
    }

    private void click(JSONObject opcion) {
        click(opcion, ErakaUsageLog.SOURCE_BOARD);
    }

    private void click(JSONObject opcion, String source) {
        cuentaMasPictos = 0;
        if (opcion == null || pictoPadre == null) {
            Log.d(TAG, "click: Opcion es null");
            return;
        }
        addPictogramToPhrase(opcion, source);


    }

    private void addPictogramToPhrase(JSONObject opcion, String source){
        boolean addPicto = false;
        boolean hasPicto = historial.hasPictogram(opcion);
        if(sharedPrefsDefault.getBoolean(getResources().getString(R.string.repeat_pictogram_name_key),false)){
            addPicto = true;
        }else {
            if(hasPicto){
                addPicto = false;
                myTTS.mostrarAlerta(getString(R.string.repeat_pictogram));
            }else{
                addPicto = true;
            }
        }
        if(addPicto) {
            long perfT0 = na.eraka.utils.ErakaPerf.start();
            logTapEvent(opcion, source); // Eraka: before adding, so the previous pictures are still the context
            historial.addPictograma(opcion);
            createRelationShip(opcion, this);
            // Eraka 22 Oct 2026: the parent's own recording if there is one; otherwise the
            // home word or the label (only when "say the picture's name" is switched on)
            int tappedId = idOf(opcion);
            if (quietPictureAdd) {
                // a whole spoken message is being added: no name per picture
            } else if (!na.eraka.utils.ErakaHomeWords.playVoice(this, tappedId)) {
                sayPictogramName(na.eraka.utils.ErakaHomeWords.spoken(this, tappedId,
                        JSONutils.getNombre(opcion, sharedPrefsDefault.getString(getResources().getString(R.string.str_idioma), "en"))));
            }
            na.eraka.utils.ErakaPerf.end("tap_to_board", perfT0, "source=" + source);
        }
    }

    //    To track the relationship between pictograms and update their
//    frequency of being selected in sequence enabling smarter
//    predictions and personalization.
    private void createRelationShip(JSONObject opcion,LoadPictograms loadPictograms){
        try {
            int pos = JSONutils.getPositionPicto2(json.getmJSONArrayTodosLosPictos(), pictoPadre.getInt("id"));
            if(pos != -1) {
                JSONutils.aumentarFrec(pictoPadre, opcion);
                json.getmJSONArrayTodosLosPictos().put(pos, pictoPadre);
                // Eraka 27 Oct 2026 (speed): OTTAA wrote the whole pictures file to storage on
                // every tap (about half a second on the A13). The counts are kept in memory and
                // the file is saved in onPause()/onStop(), as before.
                json.setmJSONArrayTodosLosPictos(json.getmJSONArrayTodosLosPictos());
            }
        } catch (JSONException e) {
            CrashlyticsUtils.getInstance().getCrashlytics().recordException(e.getCause());
        }
        pictoPadre = opcion;
        loadPictograms.loadSelection(pictoPadre);
    }
    private void onSmartRowItemClicked(SmartRowItem item) {
        try {
            int id = Integer.parseInt(item.pictogramId);
            JSONObject picto = json.getPictoFromId2(id);
            if (picto != null) {
                addPictogramToPhrase(picto, ErakaUsageLog.SOURCE_SMART_ROW);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }
    // ------------------------------------------------------------------
    // Eraka usage log (step 1 of the prediction model): see utils/ErakaUsageLog
    // ------------------------------------------------------------------

    private ErakaUsageLog usageLog() {
        return ErakaUsageLog.getInstance(this, user != null ? user.getUserUid() : "");
    }

    /** Logs the picture being added, its two previous pictures and what was on screen. */
    private void logTapEvent(JSONObject opcion, String source) {
        try {
            List<JSONObject> message = historial.getListadoPictos();
            int n = message.size();
            int prev1 = n >= 1 ? idOf(message.get(n - 1)) : ErakaUsageLog.NONE;
            int prev2 = n >= 2 ? idOf(message.get(n - 2)) : ErakaUsageLog.NONE;
            int[] board = {idOf(opcion1), idOf(opcion2), idOf(opcion3), idOf(opcion4)};
            int[] smart = new int[0];
            if (smartRowView != null) {
                List<SmartRowItem> items = smartRowView.getCurrentItems();
                smart = new int[items.size()];
                for (int i = 0; i < items.size(); i++) {
                    try {
                        smart[i] = Integer.parseInt(items.get(i).pictogramId);
                    } catch (NumberFormatException e) {
                        smart[i] = ErakaUsageLog.NONE;
                    }
                }
            }
            ErakaPredictionService service = predictionService();
            int[] explore = service.currentExplore();  // which smart-row pictures were exploration slots
            usageLog().logTap(idOf(opcion), prev1, prev2, n, source, board, smart, service.activeModel(), explore);
            service.onExploreDecision(explore, idOf(opcion));
            // step 5b: the other suggestion-row pictures (not exploration) that were not chosen
            Set<Integer> exploreSet = new HashSet<>();
            for (int e : explore) exploreSet.add(e);
            int[] exploit = new int[smart.length];
            int ne = 0;
            for (int s : smart) if (!exploreSet.contains(s)) exploit[ne++] = s;
            service.onSuggestionDecision(prev1, java.util.Arrays.copyOf(exploit, ne), idOf(opcion));
            int id = idOf(opcion);
            boolean repeat = false;                  // picture already in the message?
            for (JSONObject m : message) if (idOf(m) == id) { repeat = true; break; }
            service.onTap(id, prev1, prev2, repeat); // learned straight away, unless a repeat
        } catch (Exception e) {
            Log.e(TAG, "logTapEvent: " + e.getMessage());
        }
    }

    /** Logs a message event (spoken, cleared, undo) with the pictures in the message. */
    private void logMessageEvent(String type) {
        try {
            List<JSONObject> message = historial.getListadoPictos();
            int[] ids = new int[message.size()];
            for (int i = 0; i < ids.length; i++) ids[i] = idOf(message.get(i));
            usageLog().logMessage(type, ids);
            // an undone tap was a mistake: retrain from the log, which now marks it undone
            if (ErakaUsageLog.MSG_UNDO.equals(type)) predictionService().reload();
        } catch (Exception e) {
            Log.e(TAG, "logMessageEvent: " + e.getMessage());
        }
    }

    private ErakaPredictionService predictionService() {
        return ErakaPredictionService.getInstance(this, user != null ? user.getUserUid() : "");
    }

    /** Ids of the pictures in the message, oldest first (safe to call from any thread). */
    private int[] currentMessageIds() {
        try {
            List<JSONObject> message = new ArrayList<>(historial.getListadoPictos());
            int[] ids = new int[message.size()];
            for (int i = 0; i < ids.length; i++) ids[i] = idOf(message.get(i));
            return ids;
        } catch (Exception e) {
            return new int[0];
        }
    }

    /**
     * Called by Json.elegirHijos2() (background thread) after OTTAA has sorted a picture's
     * children: reorders them by the Eraka model's probability for the current message,
     * time and place. Pictures already in the message go last. The sort is stable, so
     * pictures the model scores equally keep OTTAA's order.
     */
    /**
     * Sorts a picture's children by the Eraka model. The list arrives in the group's fixed
     * order and the sort is stable, so ties (e.g. pictures the model knows nothing about)
     * keep that fixed order. Returns false only when Eraka is switched off (OTTAA order is used).
     */
    private boolean rankChildrenWithEraka(ArrayList<JSONObject> children) {
        ErakaPredictionService service = predictionService();
        if (!service.isEnabled()) return false;              // switch off = OTTAA's order (comparison mode)
        int[] message = currentMessageIds();
        // no data yet (new child, no base model): p is empty and every picture ties,
        // so the group's fixed order is kept - OTTAA's counts are never used while Eraka is on
        final java.util.Map<Integer, Double> p = service.hasData()
                ? service.probabilities(service.queryFor(message))
                : java.util.Collections.<Integer, Double>emptyMap();
        final Set<Integer> inMessage = new HashSet<>();
        for (int id : message) inMessage.add(id);
        // 13 Oct 2026: a picture already in the message is not shown again on the tiles
        // (e.g. water tapped in the smart row, then water appeared as a tile). If that leaves
        // fewer than 4 pictures, the free tiles get the model's next best pictures, then core words.
        children.removeIf(c -> inMessage.contains(c.optInt("id", -1)));
        if (children.size() < 4) topUpTiles(children, message, inMessage, p);
        java.util.Collections.sort(children, (a, b) -> {
            int ia = a.optInt("id", -1);
            int ib = b.optInt("id", -1);
            boolean saidA = inMessage.contains(ia);
            boolean saidB = inMessage.contains(ib);
            if (saidA != saidB) return saidA ? 1 : -1;
            return Double.compare(p.getOrDefault(ib, 0.0), p.getOrDefault(ia, 0.0));
        });
        return true;
    }

    /** Fills the first page of tiles up to 4 with new pictures (model first, then core words). */
    private void topUpTiles(ArrayList<JSONObject> children, int[] message, Set<Integer> inMessage,
                            java.util.Map<Integer, Double> p) {
        Set<Integer> have = new HashSet<>(inMessage);
        for (JSONObject c : children) have.add(c.optInt("id", -1));
        List<Integer> extra = new ArrayList<>();
        ErakaPredictionService service = predictionService();
        if (service.hasData()) {
            for (na.eraka.erakaprediction.ErakaPredictor.Suggestion s
                    : service.rank(service.queryFor(message), null, have, 4)) {
                extra.add(s.pictoId);
            }
        }
        if (smartRowDataSource != null) extra.addAll(smartRowDataSource.coreWordIds());
        for (int id : extra) {
            if (children.size() >= 4) break;
            if (!have.add(id)) continue;
            if (na.eraka.utils.ErakaVocabulary.isHiddenForChild(id)) continue; // pictogram audit
            try {
                if (Json.getInstance().getPictoFromId2(id) == null) continue;
                children.add(new JSONObject().put("id", id).put("frec", 0));
            } catch (Exception e) {
                // skip pictures that cannot be loaded
            }
        }
    }

    // ------------------------------------------------------------------ sentence suggestions (13 Oct 2026)
    // The pictures in the message -> ErakaSentenceBuilder (templates decide the structure,
    // SimpleNLG the grammar) -> one suggested sentence under the strip. The speaker button
    // says it (accepted), x hides it (ignored); both are logged and teach ErakaTemplateStats
    // which templates this child prefers. The lexicon loads once, in the background (~2 s).

    private static volatile na.eraka.erakanlg.ErakaSentenceBuilder sentenceBuilder;
    private static volatile boolean sentenceBuilderLoading = false;
    private final na.eraka.erakanlg.ErakaTemplateStats templateStats =
            new na.eraka.erakanlg.ErakaTemplateStats();        // UI thread only
    private na.eraka.erakanlg.ErakaSentenceBuilder.Candidate shownSentence;
    private String sentenceMessageKey = "";
    private boolean sentenceDismissed = false;

    private void initSentenceSuggestions() {
        View bar = findViewById(R.id.erakaSentenceBar);
        if (bar == null) return;
        findViewById(R.id.erakaSentenceSay).setOnClickListener(v -> acceptSentence());
        findViewById(R.id.erakaSentenceDismiss).setOnClickListener(v -> ignoreSentence());
        if (sentenceBuilder == null && !sentenceBuilderLoading) {
            sentenceBuilderLoading = true;
            new Thread(() -> {
                long t0 = System.currentTimeMillis();
                try {
                    sentenceBuilder = new na.eraka.erakanlg.ErakaSentenceBuilder();
                    Log.d("ErakaNLG", "sentence builder ready in " + (System.currentTimeMillis() - t0) + " ms");
                } catch (Throwable t) {
                    Log.e("ErakaNLG", "sentence builder failed: " + t.getMessage());
                }
                runOnUiThread(this::updateSentenceSuggestion);
            }, "eraka-sentences").start();
        }
        // what this child accepted or ignored before (from the log)
        final ErakaUsageLog log = usageLog();
        log.runAfterWrites(() -> {
            final List<String[]> events = log.loadSentenceEvents();
            runOnUiThread(() -> {
                for (String[] e : events) templateStats.record(e[0], ErakaUsageLog.MSG_SENTENCE_ACCEPTED.equals(e[1]));
                updateSentenceSuggestion();
            });
        });
    }

    private void updateSentenceSuggestion() {
        View bar = findViewById(R.id.erakaSentenceBar);
        if (bar == null) return;
        // The tiles can finish loading before the message history exists (e.g. while the
        // screen is being recreated after a rotation): nothing to suggest yet.
        if (historial == null || historial.getListadoPictos() == null) {
            bar.setVisibility(View.GONE);
            return;
        }
        List<JSONObject> message = historial.getListadoPictos();
        StringBuilder key = new StringBuilder();
        List<na.eraka.erakanlg.ErakaSentenceBuilder.Word> words = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        for (JSONObject p : message) {
            int id = idOf(p);
            String label = JSONutils.getNombre(p, "en");
            key.append(id).append(' ');
            words.add(new na.eraka.erakanlg.ErakaSentenceBuilder.Word(id, label, JSONutils.getTipo(p), pictureCategory(id)));
            plain.append(label == null ? "" : label).append(' ');
        }
        if (!key.toString().equals(sentenceMessageKey)) {  // new message content: a new suggestion may be shown
            sentenceMessageKey = key.toString();
            sentenceDismissed = false;
        }
        shownSentence = null;
        na.eraka.erakanlg.ErakaSentenceBuilder b = sentenceBuilder;
        if (b != null && !sentenceDismissed && !words.isEmpty()) {
            try {
                List<na.eraka.erakanlg.ErakaSentenceBuilder.Candidate> c =
                        templateStats.order(b.build(words));
                if (!c.isEmpty() && !sameWords(c.get(0).text, plain.toString())) shownSentence = c.get(0);
            } catch (Exception e) {
                Log.e("ErakaNLG", "build: " + e.getMessage());
            }
        }
        TextView text = findViewById(R.id.erakaSentenceText);
        if (shownSentence != null && text != null) {
            text.setText(sentenceToSay());
            bar.setVisibility(View.VISIBLE);
        } else {
            bar.setVisibility(View.GONE);
        }
    }

    // Picture categories for the sentence builder, from the board's groups: food, toys and
    // clothes are things a child asks for ("I want an apple."); animals and other objects are
    // commented on ("Look, a horse!"); colours are not how the child is ("It is yellow.").
    private JSONArray categoryGroups;                  // the groups array the map was built from
    private int categoryGroupsLength = -1;
    private final java.util.Map<Integer, String> categoryById = new java.util.HashMap<>();

    private String pictureCategory(int id) {
        JSONArray groups = json != null ? json.getmJSONArrayTodosLosGrupos() : null;
        if (groups == null) return null;
        if (groups != categoryGroups || groups.length() != categoryGroupsLength) {
            categoryById.clear();
            categoryGroups = groups;
            categoryGroupsLength = groups.length();
            // stronger categories first: a picture in Colors and Art lesson is a colour
            String[] order = {
                    na.eraka.erakanlg.ErakaSentenceBuilder.CAT_COLOR, na.eraka.erakanlg.ErakaSentenceBuilder.CAT_FEELING,
                    na.eraka.erakanlg.ErakaSentenceBuilder.CAT_FOOD, na.eraka.erakanlg.ErakaSentenceBuilder.CAT_TOY,
                    na.eraka.erakanlg.ErakaSentenceBuilder.CAT_CLOTHING, na.eraka.erakanlg.ErakaSentenceBuilder.CAT_ANIMAL,
                    na.eraka.erakanlg.ErakaSentenceBuilder.CAT_PLACE, na.eraka.erakanlg.ErakaSentenceBuilder.CAT_OTHER};
            java.util.Map<Integer, Integer> rank = new java.util.HashMap<>();
            for (int g = 0; g < groups.length(); g++) {
                JSONObject group = groups.optJSONObject(g);
                if (group == null) continue;
                String cat = groupCategory(group.optInt("id", -1), JSONutils.getNombre(group, "en"));
                if (cat == null) continue;
                int r = java.util.Arrays.asList(order).indexOf(cat);
                JSONArray rel = group.optJSONArray("relacion");
                if (rel == null) continue;
                for (int i = 0; i < rel.length(); i++) {
                    JSONObject o = rel.optJSONObject(i);
                    if (o == null) continue;
                    int pid = o.optInt("id", -1);
                    Integer old = rank.get(pid);
                    if (old == null || r < old) {
                        rank.put(pid, r);
                        categoryById.put(pid, cat);
                    }
                }
            }
        }
        return categoryById.get(id);                   // null: not in any category group
    }

    /** Category of one of the starter groups, by id or by name (groups can be renamed). */
    private static String groupCategory(int groupId, String name) {
        String n = name == null ? "" : name.toLowerCase(java.util.Locale.ROOT).trim();
        if (groupId == 6 || n.equals("colors") || n.equals("colours")) return na.eraka.erakanlg.ErakaSentenceBuilder.CAT_COLOR;
        if (groupId == 20 || n.equals("feelings")) return na.eraka.erakanlg.ErakaSentenceBuilder.CAT_FEELING;
        if (groupId == 3 || n.equals("food") || n.equals("breakfast") || n.equals("lunch") || n.equals("snack")
                || n.equals("dinner") || n.equals("drinks")) return na.eraka.erakanlg.ErakaSentenceBuilder.CAT_FOOD;
        if (groupId == 12 || n.equals("toys")) return na.eraka.erakanlg.ErakaSentenceBuilder.CAT_TOY;
        if (groupId == 16 || n.equals("clothing") || n.equals("clothes")) return na.eraka.erakanlg.ErakaSentenceBuilder.CAT_CLOTHING;
        if (groupId == 2 || n.equals("animals")) return na.eraka.erakanlg.ErakaSentenceBuilder.CAT_ANIMAL;
        if (groupId == 13 || n.equals("places")) return na.eraka.erakanlg.ErakaSentenceBuilder.CAT_PLACE;
        // objects a child talks about rather than asks for
        if (groupId == 4 || groupId == 9 || groupId == 23 || groupId == 7 || groupId == 10
                || n.equals("house") || n.equals("body") || n.equals("transport") || n.equals("concepts")
                || n.equals("music instruments")) return na.eraka.erakanlg.ErakaSentenceBuilder.CAT_OTHER;
        return null;                                   // "ALL", books, lessons: say nothing
    }

    /** True when the sentence adds nothing to the child's own words (e.g. "Hello!" for "Hello"). */
    private static boolean sameWords(String sentence, String words) {
        String a = sentence.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z ]", "").trim();
        String w = words.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z ]", "").trim().replaceAll("\\s+", " ");
        return a.equals(w);
    }

    /** The suggested sentence with the parent's home words put in (Eraka 22 Oct 2026). */
    private String sentenceToSay() {
        if (shownSentence == null) return "";
        List<JSONObject> message = historial != null ? historial.getListadoPictos() : null;
        return na.eraka.utils.ErakaHomeWords.substitute(this, shownSentence.text, message);
    }

    private void acceptSentence() {
        if (shownSentence == null) return;
        if (myTTS != null) myTTS.hablar(sentenceToSay());
        templateStats.record(shownSentence.key, true);
        logSentenceEvent(ErakaUsageLog.MSG_SENTENCE_ACCEPTED, shownSentence);
    }

    private void ignoreSentence() {
        if (shownSentence == null) return;
        templateStats.record(shownSentence.key, false);
        logSentenceEvent(ErakaUsageLog.MSG_SENTENCE_IGNORED, shownSentence);
        sentenceDismissed = true;                 // hidden until the message changes
        updateSentenceSuggestion();
    }

    private void logSentenceNotUsed() {
        View bar = findViewById(R.id.erakaSentenceBar);
        if (shownSentence != null && bar != null && bar.getVisibility() == View.VISIBLE) {
            logSentenceEvent(ErakaUsageLog.MSG_SENTENCE_NOT_USED, shownSentence);
        }
    }

    private void logSentenceEvent(String type, na.eraka.erakanlg.ErakaSentenceBuilder.Candidate c) {
        try {
            List<JSONObject> message = historial.getListadoPictos();
            int[] ids = new int[message.size()];
            for (int i = 0; i < ids.length; i++) ids[i] = idOf(message.get(i));
            usageLog().logMessage(type, ids, c.key, c.text);
        } catch (Exception e) {
            Log.e(TAG, "logSentenceEvent: " + e.getMessage());
        }
    }

    private static int idOf(JSONObject picto) {
        return picto == null ? ErakaUsageLog.NONE : picto.optInt("id", ErakaUsageLog.NONE);
    }

    private void refreshSmartRow() {
        long perfT0 = na.eraka.utils.ErakaPerf.start();
        refreshSmartRowNow();
        na.eraka.utils.ErakaPerf.end("smart_row", perfT0);
    }

    private void refreshSmartRowNow() {
        if (smartRowView != null && user != null && smartRowDataSource != null) {
            Set<Integer> visibleIds = new HashSet<>();
            addVisibleId(visibleIds, opcion1);
            addVisibleId(visibleIds, opcion2);
            addVisibleId(visibleIds, opcion3);
            addVisibleId(visibleIds, opcion4);
            smartRowDataSource.setExcludedIds(visibleIds);
            smartRowView.loadSuggestions(SmartRowView.Scope.GENERIC, user.getUserUid());
        }
        updateSentenceSuggestion(); // the message changed: offer a sentence for it (if one fits)
    }

    private void addVisibleId(Set<Integer> set, JSONObject opcion) {
        if (opcion == null) return;
        try {
            set.add(json.getId(opcion));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void cargarMasPictos() {
        cuentaMasPictos++;
        if (cuentaMasPictos > Constants.VUELTAS_CARRETE) {
            cuentaMasPictos = 0;
        }
        loadOptions(json, pictoPadre);
    }

    public void AlertBorrar(final int pos) {
        vibrar = false;

        YesNoDialog dialogs = new YesNoDialog(this);
        dialogs.setTitle(getResources().getString(R.string.pref_important_alert));
        dialogs.setMessage(getResources().getString(R.string.pref_text4_alert));
        dialogs.setCancelable(true);
        dialogs.setOnClick(dialogs.getObject(R.id.yes_button), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if (sharedPrefsDefault.getBoolean("esmoderador", false)) {
                        JSONArray pictosSugeridos = json.readJSONArrayFromFile(Constants.ARCHIVO_PICTOS_DATABASE);
                        JSONutils.desvincularJson(pictosSugeridos.getJSONObject(pictoPadre.getInt("id")), pos);
                        json.setmJSONArrayPictosSugeridos(pictosSugeridos);
                        if (!json.guardarJson(Constants.ARCHIVO_PICTOS_DATABASE))
                            Log.e(TAG, "onClick: eliminar: Error al guardar pictos sugeridos");
                    }

                } catch (JSONException | FiveMbException e) {
                    e.printStackTrace();
                }
                try {
                    JSONutils.desvincularJson(pictoPadre, pos);
                    JSONutils.setJsonEditado2(json.getmJSONArrayTodosLosPictos(), pictoPadre);
                    if (!json.guardarJson(Constants.ARCHIVO_PICTOS))
                        Log.e(TAG, "onClick: Error al guardar pictos sugeridos");
                    cargarMasPictos();

                } catch (JSONException e) {
                    e.printStackTrace();
                }catch (Exception ex){

                }
                dialogs.cancelarDialogo();
                loadOptions(json, pictoPadre);
            }
        });

        dialogs.setOnClick(dialogs.getObject(R.id.no_button), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.Option1).clearAnimation();
                findViewById(R.id.Option2).clearAnimation();
                findViewById(R.id.Option3).clearAnimation();
                findViewById(R.id.Option4).clearAnimation();
                dialogs.cancelarDialogo();
            }
        });
        dialogs.mostrarDialogo();


    }


    /**
     * Shows the NLG-cleaned Whisper transcription to the user for confirmation
     * before it's added to the sentence bar or spoken. Whisper transcriptions
     * (even after cleanup) may be wrong, so this is deliberately not auto-committed —
     * important in an AAC context where the tool shouldn't put words in the
     * child's mouth without a chance to correct them.
     */
    public void confirmWhisperPhrase(final String phrase) {
        if (phrase == null || phrase.trim().isEmpty()) {
            return;
        }
        final String cleanPhrase = phrase.trim();

        YesNoDialog dialog = new YesNoDialog(this);
        dialog.setTitle(getResources().getString(R.string.whisper_confirm_title));
        dialog.setMessage(cleanPhrase);
        dialog.setCancelable(true);
        dialog.setOnClick(dialog.getObject(R.id.yes_button), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setOracion(Oracion + cleanPhrase + " ");
                if (mute) {
                    myTTS.hablar(cleanPhrase);
                }
                dialog.cancelarDialogo();
            }
        });
        dialog.setOnClick(dialog.getObject(R.id.no_button), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancelarDialogo();
            }
        });
        dialog.mostrarDialogo();
    }

    public void speak() {
        if (!Oracion.isEmpty()) {
            if (!myTTS.getTTS().isSpeaking()) {
                logSentenceNotUsed(); // a sentence was offered, but the words were spoken instead
                logMessageEvent(ErakaUsageLog.MSG_SPOKEN);
                speakBlock();
            }
        }
    }

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.btn_borrar) {
            getAnalyticsFirebase().customEvents("Erase", "Principal", "Erase all pictograms");
            logMessageEvent(ErakaUsageLog.MSG_CLEARED);
            Reset();
        }
        if (editarPicto) {
            switch (v.getId()) {
                case R.id.Option1:
                    longClick(Opcion1, opcion1);
                    break;
                case R.id.Option2:
                    longClick(Opcion2, opcion2);
                    break;
                case R.id.Option3:
                    longClick(Opcion3, opcion3);
                    break;
                case R.id.Option4:
                    longClick(Opcion4, opcion4);
                    break;
                default:
                    onClick(v);
                    break;
            }
        } else {
            onClick(v);
        }
        return true;
    }


    public Context getContext() {
        return this;
    }

    private void AnimarHablar() {
        if(CantClicks<9){
            getImageView(CantClicks).startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
        }
    }

    /**
     * Speak button. OTTAA's "experimental mode" (its old sentence maker NLG, ChatGPT and online
     * translation) was removed on 19 Oct 2026: Eraka's own sentence bar replaces it, and the
     * app must work offline. The name is kept because several buttons call it.
     */
    public void hablarModoExperimental() {
        speak();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    @Override
    public void takeKeyEvents(boolean get) {
        super.takeKeyEvents(get);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (requestScreenScanningIsEnabled()) {

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                event.startTracking();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                event.startTracking();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                event.startTracking();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                event.startTracking();
                return true;
            }
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.getSource() == InputDevice.SOURCE_MOUSE)
                    barridoPantalla.getmListadoVistas().get(barridoPantalla.getPosicionBarrido()).callOnClick();
                else
                    onBackPressed();
                return true;
            }

        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_SPEECH_CHECK_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechCheck();
            } else {
                Toast.makeText(this, R.string.whisper_permission_required, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (requestCode == REQ_TEACH_WORD_MIC) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startTeachWord();
            } else {
                Toast.makeText(this, R.string.whisper_permission_required, Toast.LENGTH_SHORT).show();
            }
            return;
        }
        if (requestCode == 101) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                toggleWhisper();
            } else {
                Toast.makeText(this, R.string.whisper_permission_required, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.Option1:
                onClickOption(opcion1, Opcion1_clicker);
                break;
            case R.id.Option2:
                onClickOption(opcion2, Opcion2_clicker);
                break;
            case R.id.Option3:
                onClickOption(opcion3, Opcion3_clicker);
                break;
            case R.id.Option4:
                onClickOption(opcion4, Opcion4_clicker);
                break;
            case R.id.btn_borrar:
                analyticsAction(ConstantsAnalyticsValues.TOUCH, ConstantsAnalyticsValues.ERASE, "Principal", ConstantsAnalyticsValues.DELETEONEPICTOGRAM);
                logMessageEvent(ErakaUsageLog.MSG_UNDO);
                volver();
                break;
            case R.id.btnTalk:
                analyticsAction(ConstantsAnalyticsValues.ACCESSIBILITY, ConstantsAnalyticsValues.TOUCH, "Principal", ConstantsAnalyticsValues.TALK);
                hablarModoExperimental();
                break;
            case R.id.btnMasPictos:
                analyticsAction(ConstantsAnalyticsValues.ACCESSIBILITY, ConstantsAnalyticsValues.TOUCH, "Principal", ConstantsAnalyticsValues.MOREOPTIONS);
                cargarMasPictos();
                break;
            case R.id.btnTodosLosPictos:
                analyticsAction("Accessibility", "Touch", "Principal", "Group Gallery");
                startGroupGallery();
                break;
            case R.id.btnBarrido:
                barridoPantalla.getmListadoVistas().get(barridoPantalla.getPosicionBarrido()).callOnClick();
                break;
            case R.id.avatarIconMain:
                startIconSelector();
                break;
            case R.id.btnWhisper:
                toggleWhisper();
                break;
            default:
                hablarModoExperimental();
                break;
        }
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        //defino la configuracion al principio
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(newBase);
        boolean preferencesm = preferences.getBoolean("firstrun", true);
        Log.d(TAG, "attachBaseContext: " + newBase.getString(R.string.str_idioma));
        //configuro el idioma por defecto
        new AppLanguage(newBase, preferences.getString(newBase.getString(R.string.str_idioma), "en"));
        //adjunto el contexto base de la aplicacion
        super.attachBaseContext(LanguageContextWrapper.wrap(newBase, preferences.getString(newBase.getString(R.string.str_idioma), "en")));

    }



    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //LanguageContextWrapper.setLocale(sharedPrefsDefault.getString("en","en"));
    }

    /**
     * Realiza las tareas correspodientes cuando vuelve de un Activity
     *
     * @param requestCode (int)
     * @param resultCode  (int)
     * @param data        (Intent)
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case ConstantsMainActivity.GALERIA_GRUPOS:
                galeriaGruposResult(data);
                break;
            case ConstantsMainActivity.EDITARPICTO:
                editPictoResult(data);
                break;
            case ConstantsMainActivity.CONFIG_SCREEN:
                myTTS =SpeechOutput.getInstance(this);
                if(barridoPantalla != null)
                    barridoPantalla.updateSharePrefs(sharedPrefsDefault).cambiarEstadoBarrido();
                boolean isEnableScreenScanning = enableDisableScreenScanning();
                editarPicto = sharedPrefsDefault.getBoolean(getString(R.string.str_editar_picto), true);
                if (data != null && data.getExtras() != null) {
                    Bundle extras = data.getExtras();
                    if (extras.getBoolean(getString(R.string.boolean_cambio_idioma), false) || extras.getBoolean(getString(R.string.boolean_cambio_mano), false) || !isEnableScreenScanning) {
                        json.setmJSONArrayTodosLosPictos(json.getmJSONArrayTodosLosPictos());
                        Reset();
                        prepareLayout();
                        recreate();
                    }
                }
                CustomToast.getInstance(this).updateToastMessageLetters();
                break;
            case ConstantsMainActivity.LOGIN_ACTIVITY:
                Log.d(TAG, "onActivityResult: enter LoginActivity");
                break;
            case ConstantsMainActivity.AVATAR:
                loadAvatar();
                break;
            case ConstantsMainActivity.MY_DATA_CHECK_CODE:
                myTTS = SpeechOutput.getInstance(this);
                break;
        }
    }

    private void consultarPago() {
        // Eraka is free: no payment check. OTTAA used to read a "pago" flag from its server and,
        // for unpaid users, switch off premium features AND screen scanning (an accessibility aid).
        // UserLicence is still created because its sign-in listener returns the user to the
        // login screen after signing out.
        new UserLicence(getApplicationContext());
        sharedPrefsDefault.edit().putInt(Constants.PREMIUM, 1).apply();
    }

    public void shareText(){
        MessageSharer compartirArchivos = new MessageSharer(this, myTTS,this);
        compartirArchivos.setHistorial(historial.getListadoPictos());
        compartirArchivos.seleccionarFormato(Oracion);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (barridoPantalla!= null)
            if(barridoPantalla.isBarridoActivado()) {
                tocarTeclaAcordeUbicacion(event, keyCode, sharedPrefsDefault.getInt("orientacion_joystick", 0));
            }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        return navigationControls.makeClick(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (0 != (event.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_SCROLL:

                    if (barridoPantalla.isScrollMode() || barridoPantalla.isScrollModeClicker()) {
                        if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                            if (barridoPantalla.isScrollMode())
                                getFunction_scroll().HacerClickEnTiempo();
                            barridoPantalla.avanzarBarrido();
                        } else {
                            if (barridoPantalla.isScrollMode())
                                getFunction_scroll().HacerClickEnTiempo();
                            barridoPantalla.volverAtrasBarrido();

                        }
                    }
                    return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    //metodo encargado de pasar una vista a un formato semitransparente
    private void formatoTransparencia(View v, JSONObject opcion) {
        // Eraka (22 Oct 2026): OTTAA showed "suggested" tiles 65% transparent and, the first
        // time, a tip about them (explicarSugerencias). A faded tile can look as if it can't be
        // tapped, and the tip described OTTAA, so every tile is shown the same and the tip is gone.
        v.setAlpha(1f);
    }

    @Override
    public void onTrimMemory(int level) {
        super.onTrimMemory(level);
        switch (level) {
            case MainBoardActivity.TRIM_MEMORY_UI_HIDDEN:
                break;
            case MainBoardActivity.TRIM_MEMORY_RUNNING_MODERATE:
            case MainBoardActivity.TRIM_MEMORY_RUNNING_LOW:
            case MainBoardActivity.TRIM_MEMORY_RUNNING_CRITICAL:
                break;
            case MainBoardActivity.TRIM_MEMORY_BACKGROUND:
            case MainBoardActivity.TRIM_MEMORY_MODERATE:
                break;
            default:
        }
    }

    public void initBarrido() {
        /*
         * Preparo el inicio del barrido de pantalla, para eso es necesario pasarle el listado de objetos
         * */
        ArrayList<View> listadoObjetosBarrido = new ArrayList<>();
        barridoPantalla = new ScreenScanning(this, addObjects(listadoObjetosBarrido));
        if (barridoPantalla.isBarridoActivado() && barridoPantalla.devolverpago()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Stuff that updates the UI
                    btnBarrido.setVisibility(View.VISIBLE);
                }
            });
        } else {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Stuff that updates the UI
                    btnBarrido.setVisibility(View.GONE);
                }
            });
        }
        if (barridoPantalla.isBarridoActivado())
            editarPicto = false;
    }

    private ArrayList<View> addObjects(ArrayList<View> listadoObjetosBarrido){
        listadoObjetosBarrido.add(Opcion1);
        listadoObjetosBarrido.add(Opcion2);
        listadoObjetosBarrido.add(Opcion3);
        listadoObjetosBarrido.add(Opcion4);
        listadoObjetosBarrido.add(borrar);
        listadoObjetosBarrido.add(talk);
        listadoObjetosBarrido.add(masPictos);
        listadoObjetosBarrido.add(todosLosPictos);
        return listadoObjetosBarrido;
    }

    public void CargarJson() {
        Log.e(TAG, "CargarJson ");
        initJson();
        json.initJsonArrays();
        json.cargarPictosSugeridosJson();
        na.eraka.utils.ErakaPlaceWords.updateHereGroup(this);   // Eraka 28 Oct 2026: "Here" group for the current place

        if (json.getmJSONArrayTodosLosPictos() != null && json.getmJSONArrayTodosLosPictos().length() > 0) {
            try {
                if (pictoPadre == null || pictoPadre.getInt("id") == 0)
                    pictoPadre = json.getPictoFromId2(0);
                cuentaMasPictos = 0;
                if (pictoPadre != null) {
                    Log.e(TAG, "Parent pictogram exists, loading children ");
                    loadOptions(json, pictoPadre);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void mostrarMensajeSinConexionInternet() {
        Toast.makeText(this, "Check internet connection", Toast
                        .LENGTH_SHORT)
                .show();
    }

    public void chatGptError() {
        Toast.makeText(this, "Chat GPT Error", Toast
                        .LENGTH_SHORT)
                .show();
    }

    public void intentarDeNuevo() {
        Toast.makeText(this, R.string.tryAgain, Toast.LENGTH_SHORT).show();
    }

    public boolean tocarTeclaAcordeUbicacion(KeyEvent event, int keyCode, int ubic) {
        switch (ubic) {
            case 0:
                if (event.isTracking() && keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    barridoPantalla.volverAtrasBarrido();
                    return true;
                } else if (event.isTracking() && (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)) {
                    onClick(barridoPantalla.getmListadoVistas().get(barridoPantalla.getPosicionBarrido()));
                    return true;
                } else if (event.isTracking() && keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                    barridoPantalla.avanzarBarrido();
                    return true;
                }
                return false;
            case 1:
                if (event.isTracking() && keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                    barridoPantalla.avanzarBarrido();
                    return true;
                } else if (event.isTracking() && (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
                    onClick(barridoPantalla.getmListadoVistas().get(barridoPantalla.getPosicionBarrido()));
                    return true;
                } else if (event.isTracking() && keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                    barridoPantalla.volverAtrasBarrido();
                    return true;
                }
                return false;
        }
        return false;
    }

    private void addOption(JSONObject opcion, PictoView picto, Animation animation) {
        if(ValidateContext.isValidContext(this)){
            Pictogram pictogram = new Pictogram(opcion, AppLanguage.getLanguaje());
            picto.setUpGlideAttatcher(this);
            picto.setUpContext(this);
            picto.setPictogramsLibraryPictogram(pictogram);
            picto.setVisibility(View.VISIBLE);
            formatoTransparencia(picto, opcion);
            picto.startAnimation(animation);
        }
    }

    /**
     * use custom_picto opcion , animation alphaAnimation
     */
    private void addOpcionNull(PictoView Opcion, Animation alphaAnimation) {
        Opcion.setCustom_Img(getResources().getDrawable(R.drawable.ic_agregar_nuevo));
        Opcion.setCustom_Texto(getResources().getString(R.string.agregar_picto));
        Opcion.setCustom_Color(getResources().getColor(R.color.Black));
        Opcion.startAnimation(alphaAnimation);
        Opcion.setAlpha((float) 1);
        this.cuentaMasPictos = -1;// linea encargada de indicar que el contador esta en 0
        Animation animation = AnimationUtils.loadAnimation(getApplicationContext(),
                R.anim.alpha_dismiss);


        if (isNullObject(opcion1)) {
            hideAnimation(animation,1);
        } else if (isNullObject(opcion2)) {
            hideAnimation(animation,2);
        } else if (isNullObject(opcion3)) {
            hideAnimation(animation,3);
        } else if (isNullObject(opcion4) && Opcion3.getVisibility() == View.VISIBLE) {
            hideAnimation(animation,4);
        }
    }

    public void hideAnimation(Animation animation,int id){
        switch (id){
            case 1:
                setInvisibleOption(Opcion2,animation);
                setInvisibleOption(Opcion3,animation);
                setInvisibleOption(Opcion4,animation);
                break;
            case 2:
                setInvisibleOption(Opcion3,animation);
                setInvisibleOption(Opcion4,animation);
                break;
            case 3:
                setInvisibleOption(Opcion4,animation);
                break;
            case 4:
                Opcion4.setVisibility(View.VISIBLE);
                break;
        }
    }

    private boolean isNullObject(JSONObject object){
        return object == null;
    }

    private void setInvisibleOption(PictoView option,Animation animation){
        option.startAnimation(animation);
        option.setVisibility(View.INVISIBLE);
        option.setEnabled(false);
    }



    private void function_clickOption(JSONObject pictoPadre, PictogramClickTimer pictogram_clicker) {
        if (!pictogram_clicker.isClicked()) {
            pictogram_clicker.disableClick();
            click(pictoPadre);
            pictogram_clicker.resetClick();
        }
    }


    @Override
    public void OnClickBarrido() {
        if (getFunction_scroll().isClickEnabled() && barridoPantalla.getmListadoVistas().get(barridoPantalla.getPosicionBarrido()).getId() == R.id.btnTodosLosPictos)
            onClick(barridoPantalla.getmListadoVistas().get(barridoPantalla.getPosicionBarrido()));
        else if (!getFunction_scroll().isClickEnabled()) {
            onClick(barridoPantalla.getmListadoVistas().get(barridoPantalla.getPosicionBarrido()));
        }
    }

    @Override
    public void onPostCreate(@androidx.annotation.Nullable Bundle savedInstanceState, @androidx.annotation.Nullable PersistableBundle persistentState) {
        super.onPostCreate(savedInstanceState, persistentState);
        toggle.syncState();
    }


    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle presses on the action bar items
        Bundle bundle = new Bundle();
        switch (item.getItemId()) {
            case R.id.action_parar:
                //Registo que uso un funcion que nos interesa que use
                getAnalyticsFirebase().customEvents("Touch", "Principal", "Silence");
                if (mute) {
                    item.setIcon(getResources().getDrawable(R.drawable.ic_volume_off_white_24dp));
                    mute = false;
                } else {
                    item.setIcon(getResources().getDrawable(R.drawable.ic_volume_up_white_24dp));
                    mute = true;
                }
                myTTS.mute();
                sharedPrefs.edit().putBoolean("mBoolMute", mute).apply();
                break;
            case R.id.action_settings:
                // Grown-ups only
                CaregiverGate.run(this, () -> {
                    getAnalyticsFirebase().customEvents("Touch", "Principal", "Settings");
                    isSettings = true;
                    Intent intent12 = new Intent(MainBoardActivity.this, SettingsActivity.class);
                    startActivityForResult(intent12, IntentCode.CONFIG_SCREEN.getCode());
                });
                return true;
            case R.id.ubic:
                // Offline "Where are we?" picker (replaces GPS / Google Places)
                getAnalyticsFirebase().customEvents("Touch", "Principal", "Places");
                ErakaPlaces.showPicker(this, place -> {
                    updatePlaceMenuTitle();
                    refreshSmartRow();   // Eraka 28 Oct 2026: the new place's words in the suggestions
                });
                return true;
            case R.id.change_avatar:
                getAnalyticsFirebase().customEvents("Touch", "Principal", "ChangeAvatar");
                startIconSelector();
                break;
            case R.id.child_profile:
                CaregiverGate.run(this, () -> {
                    getAnalyticsFirebase().customEvents("Touch", "Principal", "ChildProfile");
                    Intent profile = new Intent(MainBoardActivity.this, ChildProfileActivity.class);
                    profile.putExtra("comingFromMainActivity", true);
                    startActivity(profile);
                });
                break;
            case R.id.exit:
                this.doubleBackToExitPressedOnce = true;
                super.onBackPressed();
                break;
            case R.id.logout:
                CaregiverGate.run(this, () -> {
                    getAnalyticsFirebase().customEvents("Touch", "Principal", "LogOut");
                    CaregiverGate.lockNow();
                    user.logOut();
                });
                break;
            case R.id.report:
                // Free for everyone now, computed offline on the phone
                CaregiverGate.run(this, () -> {
                    getAnalyticsFirebase().customEvents("Touch", "Principal", "Report");
                    startActivity(new Intent(MainBoardActivity.this, ErakaReportActivity.class));
                });
                break;
            case R.id.favourite_phrases:
                getAnalyticsFirebase().customEvents("Touch", "Principal", "FavouritePhrases");
                startActivity(new Intent(MainBoardActivity.this, ErakaFavouritesActivity.class));
                break;
            case R.id.games:
                getAnalyticsFirebase().customEvents("Touch", "Principal", "Games");
                startActivity(new Intent(MainBoardActivity.this, ErakaGamesActivity.class));
                break;
            case R.id.voice_speech:
                CaregiverGate.run(this, () -> {
                    getAnalyticsFirebase().customEvents("Touch", "Principal", "VoiceSpeech");
                    startActivity(new Intent(MainBoardActivity.this, ErakaVoiceActivity.class));
                });
                break;
            case R.id.teach_word:
                CaregiverGate.run(this, () -> {
                    getAnalyticsFirebase().customEvents("Touch", "Principal", "TeachWord");
                    startTeachWord();
                });
                break;
            case R.id.speech_check:
                CaregiverGate.run(this, this::startSpeechCheck);
                break;
            case R.id.send_data:
                CaregiverGate.run(this, this::sendUsageData);
                break;
            case R.id.backup_restore:
                CaregiverGate.run(this, () -> {
                    getAnalyticsFirebase().customEvents("Touch", "Principal", "BackupRestore");
                    startActivity(new Intent(MainBoardActivity.this, ErakaBackupActivity.class));
                });
                break;
            case R.id.help_tips:
                getAnalyticsFirebase().customEvents("Touch", "Principal", "HelpTips");
                startActivity(new Intent(MainBoardActivity.this, ErakaHelpActivity.class));
                break;
            case R.id.about:
                getAnalyticsFirebase().customEvents("Touch", "Principal", "About that");
                startActivity(new Intent(MainBoardActivity.this, ErakaAboutActivity.class));
                break;
        }

        return true;
    }

    private void useLocation() {
        if (placesImplementation != null) {
            if (placesImplementation.typesSizes() > 0) {
                Place place = placesImplementation.getPlace();
                String name = placesImplementation.getPlaceName(place);
                String placeType = placesImplementation.getPlaceType(place);
                locationItem.setTitle(name + " : " + placesImplementation.getPlaceName(placeType));
                //  myTTS.hablar(name);
                json.setPlaceName(placeType);
            }else{
                placesImplementation.locationRequest();
                if(myTTS != null)
                    myTTS.mostrarAlerta(getResources().getString(R.string.no_location));
            }
        }
    }

    public ScreenScanning getBarridoPantalla() {
        return barridoPantalla;
    }

    public ScrollFunctionMainActivity getFunction_scroll() {
        if(function_scroll == null)
            function_scroll = new ScrollFunctionMainActivity(this,this);
        return function_scroll;
    }

    public void loadDrawable( Pictogram pictogram,int position) {
        GlideAttatcher attatcher = new GlideAttatcher(this);
        if (pictogram.getEditedPictogram().isEmpty()) {
            if(!pictogram.getPictogram().startsWith("https://")){
                Drawable drawable = Json.getInstance().getIcono(pictogram.getObject());
                if(drawable != null)
                    attatcher.UseCornerRadius(true).loadDrawable(drawable, getImageView(position));
            }else{
                attatcher.UseCornerRadius(true).loadDrawable(Uri.parse(pictogram.getPictogram()), getImageView(position));
            }
        } else {
            File picto = new File(pictogram.getEditedPictogram());
            if (picto.exists())
                attatcher.UseCornerRadius(true).loadDrawable(picto, getImageView(position));
            else
                attatcher.UseCornerRadius(true).loadDrawable(Uri.parse(pictogram.getUrl()), getImageView(position));
        }
        getImageView(position).startAnimation(AnimationUtils.loadAnimation(this, R.anim.overshoot_arriba));
    }

    public void showAvatar() {
        remoteConfigUtils.setActivateDeactivateConfig(MainBoardActivity.this, new OnCompleteListener<Boolean>() {
            @Override
            public void onComplete(@NonNull Task<Boolean> task) {
                if (task.isSuccessful()) {
                    if (remoteConfigUtils.enableAvatar()) {
                        movableFloatingActionButton.setVisibility(View.VISIBLE);
                        String phrase = remoteConfigUtils.avatarMessages();
                        myTTS.getUtilsTTS().setTtsListener(new TTSListener() {
                            @Override
                            public void TTSonDone() {
                                MainBoardActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        avatar.finishTalking();
                                    }
                                });
                            }

                            @Override
                            public void TTSOnAudioAvailable() {

                            }

                            @Override
                            public void TTSonError() {

                            }
                        });
                        Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(!validateMessages(phrase)){
                                    myTTS.hablar(avatar.animateTalk(phrase));
                                    sharedPrefsDefault.edit().putString("avatarMessage",phrase).apply();
                                }
                                else{
                                    avatar.finishTalking();
                                }

                            }
                        }, 10500);

                    }
                }

            }
        });
    }

    public void shareAction() {
        getAnalyticsFirebase().customEvents("Touch", "Principal", "Share");
        if (historial.getListadoPictos().size() > 0) {
            // OTTAA's experimental-mode branch (NLG / ChatGPT / online translation) removed 19 Oct 2026
            if (myTTS != null) {
                MessageSharer compartirArchivos = new MessageSharer(this, myTTS,this);
                compartirArchivos.setHistorial(historial.getListadoPictos());
                compartirArchivos.seleccionarFormato(Oracion);
            }
        }else{
            myTTS.mostrarAlerta(getResources().getString(R.string.createPhrasesAlert));
        }
    }

    private void startGroupGallery() {
        Intent intent2 = new Intent(MainBoardActivity.this, GroupGalleryActivity.class);
        intent2.putExtra("Boton", 0);
        startActivityForResult(intent2, IntentCode.GALERIA_GRUPOS.getCode());
    }

    /**
     * Opens EditPictogramActivity to create a brand-new pictogram (custom image + caption)
     * as a child of the currently displayed parent. This is the "+" tile shown by
     * addOpcionNull() when fewer than 4 children exist for the current parent —
     * previously this tile routed to the group gallery instead of pictogram creation.
     */
    private void startCreatePictogram() {
        Intent intent = new Intent(MainBoardActivity.this, EditPictogramActivity.class);
        intent.putExtra("esNuevo", true);
        intent.putExtra("esGrupo", false);
        int padreId = 0;
        try {
            if (pictoPadre != null)
                padreId = json.getId(pictoPadre);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        intent.putExtra("Padre", padreId);
        intent.putExtra("Texto", "");
        if (myTTS != null)
            myTTS.hablar(getString(R.string.add_pictograma));
        startActivityForResult(intent, IntentCode.EDITARPICTO.getCode());
    }

    private void startIconSelector() {
        Intent intent1 = new Intent(this, AvatarChoiceActivity.class);
        intent1.putExtra("comingFromMainActivity", true);
        startActivityForResult(intent1, IntentCode.AVATAR.getCode());
    }

    private void startFavoritePhrases() {
        initCustomFavoritePhrase();
    }

    private void analyticsAction(String event0, String event1, String activity, String action) {
        if (barridoPantalla.isBarridoActivado())
            getAnalyticsFirebase().customEvents(event0, activity, action);
        else
            getAnalyticsFirebase().customEvents(event1, activity, action);
    }

    private void sayPictogramName(String name) {
        if (sharedPrefsDefault.getBoolean(getResources().getString(R.string.say_pictogram_name_key), false)) {
            myTTS.hablar(name);
        }
    }
    @Override
    public void onLowMemory() {
        super.onLowMemory();
        this.onTrimMemory(TRIM_MEMORY_RUNNING_LOW);
    }

    @Override
    public void onPlaceEncontrado(int placeEncontrado) {

    }

    private void onClickOption(JSONObject option, PictogramClickTimer clicker) {
        if (option == null) {
            analyticsAction("Accessibility", "Touch", "Principal", "Add Pictogram");
            startGroupGallery();
        } else {
            analyticsAction("Accessibility", "Touch", "Principal", "Add Pictogram");
            if (!barridoPantalla.isBarridoActivado())
                function_clickOption(option, clicker);
            else
                click(option);
        }
    }

    public void initCustomFavoritePhrase() {
        int option = sharedPrefsDefault.getInt("favoritePhrase", 0);
        switch (option) {
            case 0:
                analyticsAction("Accessibility", "Touch", "Principal", "More Used Phrases");
                SayActivityName.getInstance(this).sayTitle(getResources().getString(R.string.frases_musadas));
                startActivity(new Intent(this, MostUsedPhrases.class));
                break;
            case 1:
                analyticsAction("Accessibility", "Touch", "Principal", "Favorite Phrases");
                SayActivityName.getInstance(this).sayTitle(getResources().getString(R.string.favorite_phrases));
                startActivity(new Intent(this, ErakaFavouritesActivity.class));
                break;
        }
    }

    @AddTrace(name = "GaleriaGruposResult",enabled = true)
    private void galeriaGruposResult(Intent data) {
        if (data != null) {
            if(json==null) {
                json = Json.getInstance();
            }
            json.setmJSONArrayTodosLosPictos(Json.getInstance().getmJSONArrayTodosLosPictos());
            Bundle extras = data.getExtras();
            if (extras != null) {
                int Picto = extras.getInt("ID");
                if (Picto != 0) {
                    click(json.getPictoFromId2(Picto), ErakaUsageLog.SOURCE_GALLERY);
                }
            }
        }
    }

    /**
     * Handles the result of EditPictogramActivity, both for editing an existing
     * pictogram and for creating a brand-new one (esNuevo=true).
     *
     * When a new pictogram was created as a child of pictoPadre, refresh
     * pictoPadre from the (now-updated) json array before reloading options,
     * so its "relacion" reflects the newly linked child. NOTE: this assumes
     * JSONutils.crearPicto() links the new pictogram into the parent's
     * "relacion" array by id — worth confirming against that implementation
     * if the new pictogram doesn't show up immediately after creation.
     */
    private void editPictoResult(Intent data) {
        try {
            if (data != null && data.getBooleanExtra("esNuevo", false) && pictoPadre != null) {
                pictoPadre = json.getPictoFromId2(json.getId(pictoPadre));
            }
            loadOptions(json, pictoPadre);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadAvatar() {
        Context context = this;
        AtomicBoolean validContext = new AtomicBoolean(false);
        Executor executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            validContext.set(ValidateContext.isValidContext(context));
            avatarUtils = new AvatarUtils(context, menuAvatarIcon);
            handler.post(() -> {
                        if(validContext.get()) {
                            avatarUtils.getFirebaseAvatar();
                            CustomToast.getInstance(context).updateToastIcon(context);
                        }
                    }
            );
        });
    }

    private void prepareLayout() {
        if (sharedPrefsDefault.getBoolean("skillHand", false)) {
            setContentView(R.layout.activity_main_rigth);
        } else {
            setContentView(R.layout.activity_main);
        }
    }

    private boolean enableDisableScreenScanning() {
        Log.e(TAG, "enableDisableScreenScanning visibility: " + btnBarrido.getVisibility());
        if (!barridoPantalla.isBarridoActivado()) {
            if (btnBarrido.getVisibility() == View.GONE) {
                Log.e(TAG, "enableDisableScreenScanning visibility: " + btnBarrido.getVisibility());
                return true;
            } else {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // Stuff that updates the UI
                        Log.e(TAG, "enableDisableScreenScanning visibility: " + btnBarrido.getVisibility());
                        btnBarrido.setVisibility(View.GONE);
                    }
                });
                return false;
            }
        } else if (barridoPantalla.isBarridoActivado() && btnBarrido.getVisibility() == View.GONE) {
            runOnUiThread(() -> {
                // Stuff that updates the UI
                Log.e(TAG, "enableDisableScreenScanning visibility: " + btnBarrido.getVisibility());
                btnBarrido.setVisibility(View.VISIBLE);
            });
            return true;
        }

        return true;
    }

    @AddTrace(name = "InitComponents", enabled = true /* optional */)
    private void initComponents() {
        // Eraka 28 Oct 2026: each step timed (ErakaPerf board_init_*) to find what keeps the
        // board blank when it opens; the pictures' skin colours are prepared in the background
        na.eraka.utils.ErakaSkinTone.warmUp(this);
        long t = na.eraka.utils.ErakaPerf.start();
        Log.e(TAG, "Init components");
        initFlags();
        t = boardStep("flags", t);
        initBackUp();
        t = boardStep("backup", t);
        initMenu();
        t = boardStep("menu", t);
        initSelectionComponents();
        t = boardStep("selection", t);
        initActionButtons();
        t = boardStep("action_buttons", t);
        initPictograms();
        t = boardStep("pictograms", t);
        initAvatar();
        t = boardStep("avatar", t);
        initTTS();
        t = boardStep("tts", t);
        initBarrido();
        t = boardStep("scanning", t);
        initWhisper();
        boardStep("whisper", t);
        new initComponentsClass().execute();
    }

    /** Eraka 28 Oct 2026: logs how long one start-up step took; returns the time for the next one. */
    private long boardStep(String name, long t0) {
        na.eraka.utils.ErakaPerf.end("board_init_" + name, t0);
        return na.eraka.utils.ErakaPerf.start();
    }

    private void initLocationIcon() {
        locationItem = navigationView.getMenu().findItem(R.id.ubic);
        updatePlaceMenuTitle();
    }

    /** Menu shows the chosen place, e.g. "Where are we? · School". */
    private void updatePlaceMenuTitle() {
        if (locationItem == null) return;
        ErakaPlaces.Place place = ErakaPlaces.current(this);
        locationItem.setTitle(getString(R.string.menu_places_current, getString(place.label)));
    }

    private void initFirebaseComponents() {
        user = new User(this);
        mStorageRef = FirebaseStorage.getInstance().getReference();
        firebaseUtils = FirebaseUtils.getInstance();
        //firebaseUtils.setmContext(this);
        firebaseUtils.initFirebaseAndEmulator();
        analitycsFirebase = new AnalyticsFirebase(this);
        CloudStorageManager.getInstance().setStorage(mStorageRef.getStorage());
    }

    private void initDefaultSettings(Context context) {
        sharedPrefsDefault.edit().putBoolean("esmoderador", false).apply();
        AppLanguage.setLanguage(sharedPrefsDefault.getString(getString(R.string.str_idioma), "en"));
        Log.d(TAG, "ConfigurarIdioma : " + AppLanguage.getLanguaje());
        new AppLanguage(getApplicationContext(), AppLanguage.getLanguaje());
        initJson(context);
        json.initSharedPrefs();
        Log.d(TAG, "hashCode: " + json.hashCode());
        timeStamp = getTimeStamp();
        historial = new MessageHistory(json);
        sharedPrefsDefault.edit().putBoolean("usuario logueado", true).apply();
        cuentaMasPictos = 0;
        placeTypeActual = 0;
        placeActual = 0;
    }

    private void initJson(Context context){
        Json.getInstance().setmContext(context);
        json = Json.getInstance();
    }

    private void initJson(){
        Json.getInstance().setmContext(this);
        json = Json.getInstance();
    }

    private void initFlags() {
        sharedPrefs = getSharedPreferences(sharedPrefsDefault.getString(getString(R.string.str_userMail), "error"), Context.MODE_PRIVATE);
        TutoFlag = sharedPrefs.getBoolean("PrimerUso", true);
        mute = sharedPrefsDefault.getBoolean("mBoolMute", true);
        editarPicto = sharedPrefsDefault.getBoolean(getString(R.string.str_editar_picto), true);
    }

    private void initPlaceImplementationClass() {
        // GPS / Google Places is no longer used (needed a paid API key and tracked location).
        // Restore the place chosen by hand in "Where are we?" so suggestions still fit.
        ErakaPlaces.apply(this, ErakaPlaces.current(this));
        updatePlaceMenuTitle();
    }

    private void firstUserAuthVerification() {
        if (!user.getUserUid().isEmpty()) {
            subirArchivos = new FirebaseUploader(this);
            downloadJsonFromFirebase = new DownloadJsonFromFirebase(sharedPrefsDefault, user.getmAuth(), getApplicationContext());
            consultarPago();
            subirArchivos.subirFotosOffline();

        } else {
            Log.e(TAG, "onCreate: CurrentUser == null");
            Intent mainIntent = new Intent().setClass(
                    MainBoardActivity.this, SignInActivity.class);
            startActivity(mainIntent);
            finish();
        }
    }

    private void initBackUp() {
        mFirebaseBackup = new FirebaseBackupUploader(this);
        setPrimerBackupTimeLocal();
        guardarBackupLocal();
    }

    /**
     * Eraka 24 Oct 2026: the tab on the board's edge that shows there is a menu. Tapping it
     * opens the menu (the swipe still works); it fades out while the menu slides out, and on
     * the first three starts it gives a small nudge so new users notice it.
     */
    private void setUpMenuTab() {
        final View tab = findViewById(R.id.erakaMenuTab);
        if (tab == null || drawerLayout == null) return;
        final int side = sharedPrefsDefault.getBoolean("skillHand", false) ? GravityCompat.END : GravityCompat.START;
        tab.setOnClickListener(v -> drawerLayout.openDrawer(side));
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                tab.setAlpha(1f - slideOffset);
            }
        });
        int shown = sharedPrefsDefault.getInt("eraka_menu_tab_hints", 0);
        if (shown < 3) {
            sharedPrefsDefault.edit().putInt("eraka_menu_tab_hints", shown + 1).apply();
            float dir = side == GravityCompat.START ? 1f : -1f;
            float d = 10f * getResources().getDisplayMetrics().density * dir;
            tab.postDelayed(() -> tab.animate().translationX(d).setDuration(250).withEndAction(() ->
                    tab.animate().translationX(0).setDuration(250).withEndAction(() ->
                            tab.animate().translationX(d).setDuration(250).withEndAction(() ->
                                    tab.animate().translationX(0).setDuration(250)))), 1500);
        }
    }

    private void initMenu() {
        handlerHablar.postDelayed(animarHablar, 4000);
        navigationView = findViewById(R.id.nav_view);
        toolbar = findViewById(R.id.toolbar);
        drawerLayout = findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawerLayout.setOnClickListener(this);
        drawerLayout.addDrawerListener(toggle);
        toggle.setDrawerIndicatorEnabled(true);
        toggle.syncState();
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.bringToFront();
        setUpMenuTab();
        navigationView.setOnApplyWindowInsetsListener((view, windowInsets) -> windowInsets);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        constraintBotonera = findViewById(R.id.constraintRightButtons);
        inmersiveMode = new ImmersiveMode(this);
        gesture = new Gesture(drawerLayout);
        initLocationIcon();
    }


    private void initSelectionComponents() {
        for (int i = 0; i < 10; i++) {
            int id = getResources().getIdentifier("Seleccion"+(i+1),"id",getPackageName());
            getSelectedImage(i).setImageview(findViewById(id));
            AjustarAncho(id);
        }
    }

    @AddTrace(name = "setOnLongClickListener", enabled = true /* optional */)
    private void setOnLongClickListener(){
        Log.e(TAG, "setOnLongClickListener" );
        talk.setOnClickListener(this);
        Log.e(TAG, "talk" );
        for (int i = 0; i < 10; i++) {
            Log.e(TAG, "for loop" );
            setClickLongListener(getImageView(i));
        }
        setClickLongListener(borrar);
        setClickLongListener(masPictos);
        setClickLongListener(todosLosPictos);
        setClickOnTouchListener(btnBarrido);
        setClickLongListener(Opcion1);
        setClickLongListener(Opcion2);
        setClickLongListener(Opcion3);
        setClickLongListener(Opcion4);
    }

    private void initActionButtons() {
        borrar = findViewById(R.id.btn_borrar);
        masPictos = findViewById(R.id.btnMasPictos);
        todosLosPictos = findViewById(R.id.btnTodosLosPictos);
        btnBarrido = findViewById(R.id.btnBarrido);
        btnBarrido.setVisibility(View.GONE);
        talk = findViewById(R.id.btnTalk);
        animationView = findViewById(R.id.lottieAnimationView);
    }

    private void initPictograms() {
        Opcion1 = findViewById(R.id.Option1);
        Opcion2 = findViewById(R.id.Option2);
        Opcion3 = findViewById(R.id.Option3);
        Opcion4 = findViewById(R.id.Option4);
        Agregar = new PictoView(this);
        Agregar.setCustom_Color(R.color.Black);
        Agregar.setCustom_Texto("");
        Agregar.setCustom_Img(getDrawable(R.drawable.agregar_picto_transp));
        Agregar.setIdPictogram(0);
        Opcion1_clicker = new PictogramClickTimer(this);
        Opcion2_clicker = new PictogramClickTimer(this);
        Opcion3_clicker = new PictogramClickTimer(this);
        Opcion4_clicker = new PictogramClickTimer(this);
    }

    private void initFirstPictograms() {
        new loadPictograms().execute();
    }

    private void initAvatar() {
        menuAvatarIcon = navigationView.getHeaderView(0).findViewById(R.id.avatarIconMain);
        // Eraka developer switch (debug only): long-press the menu header to choose
        // LIVE Firebase or the laptop EMULATOR (same dialog as on the sign-in screen).
        if (na.eraka.BuildConfig.DEBUG) {
            navigationView.getHeaderView(0).setOnLongClickListener(v -> {
                na.eraka.FirebaseRequests.FirebaseServerDialog.show(MainBoardActivity.this);
                return true;
            });
        }
        menuAvatarIcon.setOnClickListener(this);
        movableFloatingActionButton = findViewById(R.id.movableButton);
        movableFloatingActionButton.setOnClickListener(this);
        avatar = new Avatar(this, movableFloatingActionButton);
        updateDrawerGreeting();
    }

    private void updateDrawerGreeting() {
        if (navigationView == null || navigationView.getHeaderCount() == 0) return;
        View header = navigationView.getHeaderView(0);
        TextView greeting = header.findViewById(R.id.navHeaderGreeting);
        TextView nameView = header.findViewById(R.id.navHeaderName);
        if (greeting == null || nameView == null) return;

        String fullName = PreferenceManager.getDefaultSharedPreferences(this).getString("name", "");
        String firstName = fullName == null ? "" : fullName.trim();
        if (firstName.contains(" ")) firstName = firstName.substring(0, firstName.indexOf(' '));

        if (firstName.isEmpty()) {
            greeting.setVisibility(View.GONE);
            nameView.setText(R.string.nav_welcome_back_no_name);
        } else {
            // Capitalise the first letter
            firstName = firstName.substring(0, 1).toUpperCase() + firstName.substring(1);
            greeting.setVisibility(View.VISIBLE);
            nameView.setText(getString(R.string.nav_welcome_back_name, firstName));
        }
    }

    private void initTTS() {
        try {
            if (resolveIntent(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA)) {
                Intent checkTTSIntent = new Intent();
                checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                startActivityForResult(checkTTSIntent, IntentCode.MY_DATA_CHECK_CODE.getCode());
            } else {

                Alert();
            }
        } catch (Exception e) {
            CrashlyticsUtils.getInstance().getCrashlytics().recordException(e);
            Alert();
        }
    }

    private WhisperButtonAnimator whisperAnimator;

    private void initWhisper() {
        whisperManager = new WhisperManager(this);
        btnWhisper = findViewById(R.id.btnWhisper);
        whisperAnimator = new WhisperButtonAnimator(btnWhisper);
        if (btnWhisper != null) {
            if (!whisperManager.isSupportedRuntime()) {
                btnWhisper.setVisibility(View.GONE);
                return;
            }
            btnWhisper.setOnClickListener(this);
        }
    }

    private void toggleWhisper() {
        if (whisperManager == null) {
            return;
        }
        if (!whisperManager.isSupportedRuntime()) {
            Toast.makeText(this, "Voice recognition is not supported on this device.", Toast.LENGTH_LONG).show();
            return;
        }
        if (whisperManager.hasModelLoadFailed()) {
            Toast.makeText(this, R.string.whisper_model_error, Toast.LENGTH_LONG).show();
            return;
        }
        if (!whisperManager.isModelReady()) {
            Toast.makeText(this, R.string.whisper_model_loading, Toast.LENGTH_SHORT).show();
            return;
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            if (shouldShowRequestPermissionRationale(android.Manifest.permission.RECORD_AUDIO)) {
                Toast.makeText(this, R.string.whisper_permission_required, Toast.LENGTH_LONG).show();
            }
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, 101);
            return;
        }

        if (!whisperManager.isRecording()) {
            // Eraka: tell Whisper which words this child's board can say
            whisperManager.setVocabularyHint(buildVocabularyHint());
        }
        whisperManager.toggleRecording(new WhisperManager.OnResultListener() {
            @Override
            public void onTranscribingStarted() {
                if (whisperAnimator != null) whisperAnimator.setState(WhisperButtonAnimator.State.THINKING);
            }

            @Override
            public void onResult(String text) {
                if (whisperAnimator != null) whisperAnimator.setState(WhisperButtonAnimator.State.IDLE);
                if (text != null && !VoiceDictionary.isEmptyTranscript(text)) {
                    // Personal voice dictionary: sound -> pictogram suggestions (offline)
                    handleWhisperTranscript(text.trim());
                } else {
                    whisperTeachMode = false;
                    Toast.makeText(MainBoardActivity.this, "Couldn't understand audio, try again", Toast.LENGTH_LONG).show();
                }
            }
            public void onNoSpeechDetected() {
                whisperTeachMode = false;
                if (whisperAnimator != null) whisperAnimator.setState(WhisperButtonAnimator.State.IDLE);
                Toast.makeText(MainBoardActivity.this, "No speech detected, try again", Toast.LENGTH_SHORT).show();
            }
        });
        if (whisperAnimator != null && whisperManager.isRecording()) {
            whisperAnimator.setState(WhisperButtonAnimator.State.LISTENING);
        }
    }

    /**
     * Eraka: up to ~30 of the child's own picture words, most-used first, starting with
     * the 4 on screen. Passed to Whisper as its "initial prompt" so it leans towards words
     * the child can actually say with the board ("water" rather than "what a").
     */
    private String buildVocabularyHint() {
        try {
            String lang = voiceLang();
            java.util.LinkedHashSet<String> words = new java.util.LinkedHashSet<>();
            // 0) Eraka 24 Oct 2026: the family's home words ("meme", "tate"), which an English
            //    model would not expect otherwise
            for (String w : na.eraka.utils.ErakaHomeWords.allWords(this)) {
                if (words.size() >= 8) break;
                addHintText(words, w);
            }
            // 1) pictures this child has taught/confirmed by voice (e.g. "water"):
            //    helps Whisper hear the child's real words instead of "what"
            for (Integer id : voiceDictionary().learnedPictoIds()) {
                if (words.size() >= 10) break;
                addHintWord(words, json.getPictoFromId2(id), lang);
            }
            // 2) the 4 on screen
            JSONObject[] onScreen = {opcion1, opcion2, opcion3, opcion4};
            for (JSONObject o : onScreen) addHintWord(words, o, lang);

            JSONArray all = json.getmJSONArrayTodosLosPictos();
            List<JSONObject> list = new ArrayList<>();
            final java.util.HashMap<JSONObject, Integer> use = new java.util.HashMap<>();
            for (int i = 0; all != null && i < all.length(); i++) {
                JSONObject p = all.optJSONObject(i);
                if (p == null) continue;
                int sum = 0;
                JSONArray rel = p.optJSONArray("relacion");
                for (int j = 0; rel != null && j < rel.length(); j++) {
                    JSONObject r = rel.optJSONObject(j);
                    if (r != null) sum += r.optInt("frec", 0);
                }
                use.put(p, sum);
                list.add(p);
            }
            java.util.Collections.sort(list, (a, b) -> Integer.compare(use.get(b), use.get(a)));
            for (JSONObject p : list) {
                if (words.size() >= 30) break;
                addHintWord(words, p, lang);
            }
            return android.text.TextUtils.join(", ", words);
        } catch (Exception e) {
            return "";
        }
    }

    private void addHintWord(java.util.Set<String> words, JSONObject picto, String lang) {
        if (picto == null) return;
        String name = JSONutils.getNombre(picto, lang);
        if (name == null) return;
        addHintText(words, na.eraka.utils.ErakaHomeWords.word(this, idOf(picto)));   // home word first
        addHintText(words, name);
    }

    private void addHintText(java.util.Set<String> words, String name) {
        if (name == null) return;
        name = name.trim().toLowerCase(java.util.Locale.ROOT);
        // short, plain words only: long phrases make the prompt slow and confuse Whisper
        if (name.isEmpty() || name.length() > 20 || name.split("\\s+").length > 2) return;
        if (!name.matches("[\\p{L} '-]+")) return;
        words.add(name);
    }

    // ================= Personal voice dictionary (Whisper -> pictograms) =================

    /** true while a caregiver is teaching a new sound (next recording is a teaching sample). */
    private boolean whisperTeachMode = false;

    private String voiceLang() {
        return sharedPrefsDefault.getString(getResources().getString(R.string.str_idioma), "en");
    }

    private VoiceDictionary voiceDictionary() {
        return VoiceDictionary.getInstance(this, user != null ? user.getUserUid() : "");
    }

    /**
     * Called with every Whisper transcript. Shows the child's most likely pictograms
     * (learned sounds first, then matching names). Nothing is said until the child taps.
     */
    /** True while a whole spoken message is added, so the pictures' names are not said one by one. */
    private boolean quietPictureAdd;

    private void handleWhisperTranscript(final String heard) {
        if (whisperTeachMode) {
            whisperTeachMode = false;
            askCaregiverWhichPicto(heard, null);
            return;
        }
        final VoiceDictionary dict = voiceDictionary();
        final List<VoiceDictionary.Candidate> candidates =
                dict.match(heard, json.getmJSONArrayTodosLosPictos(), voiceLang(), VoiceDictionary.DEFAULT_LIMIT);
        // Eraka 27 Oct 2026: a whole sentence ("I want to go home") -> [I] [want] [go] [home]
        final List<VoiceDictionary.Candidate> sequence =
                dict.matchSequence(heard, json.getmJSONArrayTodosLosPictos(), voiceLang());
        if (candidates.isEmpty() && sequence.isEmpty()) {
            dict.logAttempt(heard, candidates, -1, "no_match");
            // Eraka: no text-only Yes/No box for a child who can't read.
            // Offer Try again / Teach (caregiver) / Use the words instead.
            VoiceSuggestions.showNoMatch(this, heard, new VoiceSuggestions.NoMatchListener() {
                @Override
                public void onTryAgain() {
                    toggleWhisper();
                }

                @Override
                public void onTeach() {
                    // empty "shown" list: logged as a correction and the picture is added to the sentence
                    CaregiverGate.run(MainBoardActivity.this,
                            () -> askCaregiverWhichPicto(heard, new ArrayList<>()));
                }

                @Override
                public void onUseWords() {
                    useWhisperWords(heard);
                }

                @Override
                public void onClosed() {
                    // nothing: the miss is already logged
                }
            });
            return;
        }
        final boolean[] handled = {false};
        VoiceSuggestions.OnSequencePicked onSequence = seq -> {
            if (handled[0]) return;
            handled[0] = true;
            dict.logAttempt(heard, seq, -1, "sentence");
            quietPictureAdd = true;
            try {
                for (VoiceDictionary.Candidate c : seq) addPictogramToPhrase(c.picto, ErakaUsageLog.SOURCE_VOICE);
            } finally {
                quietPictureAdd = false;
            }
        };
        VoiceSuggestions.showSuggestions(this, heard, candidates, sequence, voiceLang(), new VoiceSuggestions.Listener() {
            @Override
            public void onPicked(VoiceDictionary.Candidate c) {
                if (handled[0]) return;
                handled[0] = true;
                dict.learn(heard, c.id);
                dict.logAttempt(heard, candidates, c.id, "child");
                addPictogramToPhrase(c.picto, ErakaUsageLog.SOURCE_VOICE);
                // show that the tap also taught the dictionary (Keneke's test, 21 Oct 2026)
                Toast.makeText(MainBoardActivity.this,
                        getString(R.string.voice_dict_learned, heard, JSONutils.getNombre(c.picto, voiceLang())),
                        Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNoneOfThese() {
                if (handled[0]) return;
                handled[0] = true;
                dict.logAttempt(heard, candidates, -1, "none");
            }

            @Override
            public void onUseWords() {
                if (handled[0]) return;
                handled[0] = true;
                dict.logAttempt(heard, candidates, -1, "words");
                useWhisperWords(heard);
            }

            @Override
            public void onTeach() {
                if (handled[0]) return;
                handled[0] = true;
                CaregiverGate.run(MainBoardActivity.this, () -> askCaregiverWhichPicto(heard, candidates));
            }
        }, onSequence);
    }

    /** Caregiver chooses the pictogram the child meant; the sound is learned for next time. */
    private void askCaregiverWhichPicto(final String heard, final List<VoiceDictionary.Candidate> shown) {
        VoiceSuggestions.showPictoPicker(this,
                getString(R.string.voice_dict_which_picto),
                getString(R.string.voice_dict_heard, heard),
                json.getmJSONArrayTodosLosPictos(), voiceLang(), picto -> {
                    int id = picto.optInt("id", -1);
                    VoiceDictionary dict = voiceDictionary();
                    dict.teach(heard, id);
                    dict.logAttempt(heard, shown != null ? shown : new ArrayList<>(), id,
                            shown != null ? "corrected" : "taught");
                    Toast.makeText(MainBoardActivity.this,
                            getString(R.string.voice_dict_learned, heard, JSONutils.getNombre(picto, voiceLang())),
                            Toast.LENGTH_LONG).show();
                    if (shown != null) addPictogramToPhrase(picto, ErakaUsageLog.SOURCE_VOICE_CORRECTED);
                });
    }

    /** Menu: caregiver records the child's own sound for a word. */

    // ================= Eraka study tools: send usage data + speech check (13 Oct 2026) =================

    private static final int REQ_SPEECH_CHECK_MIC = 102;

    /**
     * Caregiver menu "Send usage data": asks once for the child code (e.g. C01), then packs
     * the usage log, speech checks and voice evaluation into one zip and opens the share
     * sheet (WhatsApp, Gmail...). See ErakaDataExport.
     */
    private void sendUsageData() {
        final android.widget.EditText code = new android.widget.EditText(this);
        code.setHint(R.string.eraka_child_code_hint);
        code.setSingleLine(true);
        code.setText(na.eraka.utils.ErakaDataExport.childCode(this));
        code.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);

        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setPadding(pad, pad / 2, pad, 0);
        TextView explain = new TextView(this);
        explain.setText(R.string.eraka_send_data_explain);
        box.addView(explain);
        TextView label = new TextView(this);
        label.setText(R.string.eraka_child_code_title);
        label.setPadding(0, pad / 2, 0, 0);
        box.addView(label);
        box.addView(code);

        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_send_data)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.eraka_send_data_button, (d, w) -> {
                    na.eraka.utils.ErakaDataExport.setChildCode(this, code.getText().toString());
                    na.eraka.utils.ErakaDataExport.build(this, usageLog(),
                            voiceDictionary().evalFile(), zip -> {
                                if (zip == null || isFinishing()) {
                                    Toast.makeText(this, R.string.eraka_send_data_failed, Toast.LENGTH_LONG).show();
                                    return;
                                }
                                Log.d("ErakaExport", "zip ready: " + zip.getName() + " " + zip.length() + " bytes");
                                try {
                                    na.eraka.utils.ErakaDataExport.share(this, zip,
                                            getString(R.string.eraka_send_data_chooser));
                                } catch (Exception e) {
                                    Log.e("ErakaExport", "share: " + e.getMessage());
                                    Toast.makeText(this, R.string.eraka_send_data_failed, Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .show();
    }

    /** One run through the 10 speech-check words. */
    private final class SpeechCheckRun {
        final long checkId = System.currentTimeMillis();
        final String phase;
        final List<JSONObject> pictos = new ArrayList<>();
        int index = 0;
        int recognisedCount = 0;
        int answered = 0;
        long recordStart = 0;
        boolean waitingForResult = false;
        boolean finished = false;
        AlertDialog dialog;
        android.widget.ImageView image;
        TextView word, status;
        android.widget.Button record, skip;

        SpeechCheckRun(String phase) { this.phase = phase; }
    }

    private SpeechCheckRun speechCheck;

    /** true if Whisper can record now; asks for the microphone if needed. */
    private boolean whisperReadyFor(int permissionRequest) {
        if (whisperManager == null || !whisperManager.isSupportedRuntime()) {
            Toast.makeText(this, "Voice recognition is not supported on this device.", Toast.LENGTH_LONG).show();
            return false;
        }
        if (whisperManager.hasModelLoadFailed()) {
            Toast.makeText(this, R.string.whisper_model_error, Toast.LENGTH_LONG).show();
            return false;
        }
        if (!whisperManager.isModelReady()) {
            Toast.makeText(this, R.string.whisper_model_loading, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (whisperManager.isRecording() || whisperManager.isTranscribing()) {
            Toast.makeText(this, R.string.whisper_busy, Toast.LENGTH_SHORT).show();
            return false;
        }
        if (checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, permissionRequest);
            return false;
        }
        return true;
    }

    /**
     * Caregiver menu "Speech check": the child says the same 10 words at the start and the
     * end of each week. For each word we log what Whisper heard, whether the word itself was
     * heard, and where the picture came in the voice suggestions. Nothing is taught to the
     * voice dictionary and no audio is kept.
     */
    private void startSpeechCheck() {
        if (!whisperReadyFor(REQ_SPEECH_CHECK_MIC)) return;
        final String[] phases = {na.eraka.erakaprediction.ErakaSpeechCheck.PHASE_START,
                na.eraka.erakaprediction.ErakaSpeechCheck.PHASE_END};
        String[] names = {getString(R.string.eraka_speech_phase_start), getString(R.string.eraka_speech_phase_end)};
        new AlertDialog.Builder(this)
                .setTitle(R.string.eraka_speech_check_phase)
                .setItems(names, (d, which) -> runSpeechCheck(phases[which]))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void runSpeechCheck(String phase) {
        final SpeechCheckRun run = new SpeechCheckRun(phase);
        for (int id : na.eraka.erakaprediction.ErakaSpeechCheck.WORD_IDS) {
            JSONObject p = json.getPictoFromId2(id);
            if (p != null && !na.eraka.utils.ErakaVocabulary.isHiddenForChild(id)) run.pictos.add(p);
        }
        if (run.pictos.isEmpty()) return;
        speechCheck = run;

        float dp = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * dp);
        android.widget.LinearLayout box = new android.widget.LinearLayout(this);
        box.setOrientation(android.widget.LinearLayout.VERTICAL);
        box.setGravity(android.view.Gravity.CENTER_HORIZONTAL);
        box.setPadding(pad, pad / 2, pad, 0);

        TextView intro = new TextView(this);
        intro.setText(R.string.eraka_speech_check_intro);
        box.addView(intro);

        run.image = new android.widget.ImageView(this);
        int size = (int) (160 * dp);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(size, size);
        lp.topMargin = pad / 2;
        box.addView(run.image, lp);

        run.word = new TextView(this);
        run.word.setTextSize(28);
        run.word.setGravity(android.view.Gravity.CENTER);
        box.addView(run.word);

        run.status = new TextView(this);
        run.status.setGravity(android.view.Gravity.CENTER);
        run.status.setPadding(0, pad / 3, 0, pad / 3);
        box.addView(run.status);

        android.widget.LinearLayout buttons = new android.widget.LinearLayout(this);
        buttons.setOrientation(android.widget.LinearLayout.HORIZONTAL);
        buttons.setGravity(android.view.Gravity.CENTER);
        run.skip = new android.widget.Button(this);
        run.skip.setText(R.string.eraka_speech_skip);
        run.record = new android.widget.Button(this);
        run.record.setText(R.string.eraka_speech_record);
        buttons.addView(run.skip);
        buttons.addView(run.record);
        box.addView(buttons);

        run.record.setOnClickListener(v -> onSpeechCheckRecord(run));
        run.skip.setOnClickListener(v -> {
            if (run.waitingForResult) return;
            JSONObject p = run.pictos.get(run.index);
            usageLog().logSpeechCheck(run.checkId, run.phase, idOf(p), speechLabel(p), "", false, 0, -1);
            nextSpeechWord(run);
        });

        run.dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.menu_speech_check)
                .setView(box)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        run.dialog.setCanceledOnTouchOutside(false);
        run.dialog.setOnDismissListener(d -> {
            run.finished = true;
            if (whisperManager != null && whisperManager.isRecording()) whisperManager.stop();
            if (speechCheck == run) speechCheck = null;
        });
        run.dialog.show();
        showSpeechWord(run);
    }

    private String speechLabel(JSONObject picto) {
        String name = JSONutils.getNombre(picto, voiceLang());
        return name == null ? "" : name.trim();
    }

    private void showSpeechWord(SpeechCheckRun run) {
        JSONObject p = run.pictos.get(run.index);
        try {
            android.graphics.drawable.Drawable d = Json.getInstance().getIcono(p);
            run.image.setImageDrawable(d);
        } catch (Exception e) {
            run.image.setImageDrawable(null);
        }
        run.word.setText(speechLabel(p));
        run.status.setText((run.index + 1) + " / " + run.pictos.size());
        run.record.setText(R.string.eraka_speech_record);
        run.record.setEnabled(true);
        run.skip.setEnabled(true);
    }

    private void onSpeechCheckRecord(final SpeechCheckRun run) {
        if (run.finished || whisperManager == null) return;
        if (whisperManager.isRecording()) {           // caregiver pressed Stop
            whisperManager.toggleRecording(null);
            return;
        }
        if (run.waitingForResult) return;
        final JSONObject target = run.pictos.get(run.index);
        whisperManager.setVocabularyHint(buildVocabularyHint()); // same hint as normal use
        run.waitingForResult = true;
        run.recordStart = System.currentTimeMillis();
        run.skip.setEnabled(false);
        run.record.setText(R.string.eraka_speech_stop);
        run.status.setText(R.string.eraka_speech_listening);
        whisperManager.toggleRecording(new WhisperManager.OnResultListener() {
            @Override
            public void onTranscribingStarted() {
                if (run.finished) return;
                run.record.setEnabled(false);
                run.status.setText(R.string.eraka_speech_thinking);
            }

            @Override
            public void onResult(String text) {
                finishSpeechWord(run, target, text);
            }

            public void onNoSpeechDetected() {
                finishSpeechWord(run, target, "");
            }
        });
    }

    private void finishSpeechWord(SpeechCheckRun run, JSONObject target, String text) {
        if (!run.waitingForResult) return;
        run.waitingForResult = false;
        long ms = System.currentTimeMillis() - run.recordStart;
        String heard = text == null ? "" : text.trim();
        String label = speechLabel(target);
        int targetId = idOf(target);

        boolean ok = na.eraka.erakaprediction.ErakaSpeechCheck.recognised(
                VoiceDictionary.normalize(heard), VoiceDictionary.normalize(label));
        int rank = 0;
        if (!VoiceDictionary.isEmptyTranscript(heard)) {
            // the same suggestions the child would get on the board (read only: nothing is learned)
            List<VoiceDictionary.Candidate> cands = voiceDictionary().match(heard,
                    json.getmJSONArrayTodosLosPictos(), voiceLang(), VoiceDictionary.DEFAULT_LIMIT);
            int[] ids = new int[cands.size()];
            for (int i = 0; i < ids.length; i++) ids[i] = cands.get(i).id;
            rank = na.eraka.erakaprediction.ErakaSpeechCheck.rankOf(ids, targetId);
        }
        usageLog().logSpeechCheck(run.checkId, run.phase, targetId, label, heard, ok, rank, ms);
        Log.d("ErakaSpeech", run.phase + " " + label + " heard='" + heard + "' ok=" + ok + " rank=" + rank + " ms=" + ms);
        run.answered++;
        if (ok) run.recognisedCount++;
        if (run.finished) return;

        run.status.setText("Heard: \"" + (heard.isEmpty() ? "-" : heard) + "\"" + (ok ? "  ✓" : ""));
        run.record.setEnabled(false);
        run.skip.setEnabled(false);
        new Handler().postDelayed(() -> nextSpeechWord(run), 1500);
    }

    private void nextSpeechWord(SpeechCheckRun run) {
        if (run.finished) return;
        run.index++;
        if (run.index < run.pictos.size()) {
            showSpeechWord(run);
            return;
        }
        Toast.makeText(this, getString(R.string.eraka_speech_done, run.recognisedCount, run.answered),
                Toast.LENGTH_LONG).show();
        run.dialog.dismiss();
    }

    private static final int REQ_TEACH_WORD_MIC = 103;

    /**
     * Menu "Teach a word" (28 Oct 2026): opens its own page (VoiceSuggestions.showTeachWord) with
     * the picture, a Record/Stop button and a line that says what is happening, instead of a toast
     * that used the board's mic. Nothing is added to the child's message; the sound is only learned.
     */
    private void startTeachWord() {
        if (!whisperReadyFor(REQ_TEACH_WORD_MIC)) return;
        whisperTeachMode = false;
        final String lang = voiceLang();
        VoiceSuggestions.showTeachWord(this, json.getmJSONArrayTodosLosPictos(), lang,
                new VoiceSuggestions.TeachController() {
                    @Override
                    public void record(JSONObject picto, VoiceSuggestions.TeachUi ui) {
                        if (whisperManager == null) return;
                        if (whisperManager.isRecording()) {          // caregiver pressed Stop
                            whisperManager.toggleRecording(null);
                            return;
                        }
                        if (whisperManager.isTranscribing()) return;
                        // the chosen word first, so Whisper leans towards it; then the usual hint
                        String word = JSONutils.getNombre(picto, lang);
                        String hint = buildVocabularyHint();
                        whisperManager.setVocabularyHint(word == null || word.isEmpty() ? hint : word + ", " + hint);
                        whisperManager.toggleRecording(new WhisperManager.OnResultListener() {
                            @Override
                            public void onTranscribingStarted() { ui.thinking(); }

                            @Override
                            public void onResult(String text) {
                                if (text == null || VoiceDictionary.isEmptyTranscript(text)) ui.nothingHeard();
                                else ui.heard(text.trim());
                            }

                            public void onNoSpeechDetected() { ui.nothingHeard(); }
                        });
                        if (whisperManager.isRecording()) ui.listening();
                    }

                    @Override
                    public void close() {
                        if (whisperManager != null && whisperManager.isRecording()) whisperManager.stop();
                    }

                    @Override
                    public void save(JSONObject picto, String heard) {
                        int id = picto.optInt("id", -1);
                        VoiceDictionary dict = voiceDictionary();
                        dict.teach(heard, id);
                        dict.logAttempt(heard, new ArrayList<>(), id, "taught");
                        Toast.makeText(MainBoardActivity.this,
                                getString(R.string.voice_dict_learned, heard, JSONutils.getNombre(picto, lang)),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    /** Old text path ("Use the words"); the online ChatGPT clean-up was removed 19 Oct 2026. */
    private void useWhisperWords(String cleanText) {
        confirmWhisperPhrase(cleanText);
    }

    private void showMenu() {
        Log.e(TAG, "Show menu");
        try {
            if (!sharedPrefsDefault.getBoolean(getString(R.string.skillhand), false) && sharedPrefsDefault.getInt(getString(R.string.showmenu), 3) > 0) {
                Log.e(TAG, "Breaking 1");
                drawerLayout.open();
                Log.e(TAG, "Breaking 2");
                int value = sharedPrefsDefault.getInt(getString(R.string.showmenu), 4);
                value--;
                sharedPrefsDefault.edit().putInt(getString(R.string.showmenu), value).apply();
                new Handler().postDelayed(() -> drawerLayout.close(), 5000);
            }
        } catch (Exception ex) {
            Log.e(TAG, "showMenu: " + ex.getMessage());
        }
    }

    private void setClickLongListener(View v) {
        v.setOnClickListener(this);
        v.setOnLongClickListener(this);
    }

    private void setClickOnTouchListener(View v) {
        v.setOnClickListener(this);
        v.setOnTouchListener(this);
    }

    private void uploadFiles() {
        Log.e(TAG, "uploadFiles 1" );
        if (subirArchivos != null) {
            Log.e(TAG, "subirArchivos != null" );
            subirArchivos.setInterfaz(this);
        }
        if(ConnectionDetector.isNetworkAvailable(this)){
            Log.e(TAG, "ConnectionDetector.isNetworkAvailable" );
            if (subirArchivos != null) {
                Log.e(TAG, "subirArchivos != null" );
                subirArchivos.userDataExists(subirArchivos.getmDatabase(user.getmAuth(), "Pictos"), subirArchivos.getmDatabase(user.getmAuth(), "Grupos"), subirArchivos.getmDatabase(user.getmAuth(), "Frases"));
            }
        }
    }

    private void longClick(PictoView pictoView, JSONObject json) {
        onLongOpcion = json;
        if (pictoView.getAlpha() != (0.65) || sharedPrefsDefault.getBoolean(getString(R.string.ismoderator), false)) {
            PopupMenuUtils menuUtils = new PopupMenuUtils(this,pictoView);
            menuUtils.addClickListener(this);
            menuUtils.inflateIt();
        }
    }


    public void loadChilds(JSONObject padre, Animation alphaAnimation) {
        Log.e(TAG, "Load Child ");
        if (padre != null) {
            Log.e(TAG, "Parent not empty");
            Log.e(TAG, "padre:" + padre);
            json.cargarOpciones(padre, cuentaMasPictos, this);
        } else {
            Log.e(TAG, "Parent is empty");
            if (ConnectionDetector.isNetworkAvailable(getApplicationContext())) {
                json.sumarFallas();
                loadOptions(json, padre);
            }
        }
    }

    public void downloadFailedFile(int size) {
        Log.d("Failed_file", "Downloading failed file");
        getFirebaseDialog().setTitle(getString(R.string.edit_sync)).setMessage(getString(R.string.edit_sync_pict));
        getFirebaseDialog().mostrarDialogo();
        ObservableInteger observableInteger = loadObservableInteger(size);
        observableInteger.set(0);
        downloadJsonFromFirebase.bajarJuego(AppLanguage.getLanguaje());
        downloadJsonFromFirebase.bajarFrasesFavoritas(AppLanguage.getLanguaje());
    }

    public ObservableInteger loadObservableInteger(int size) {
        ObservableInteger observableInteger = new ObservableInteger();
        observableInteger.setOnIntegerChangeListener(new ObservableInteger.OnIntegerChangeListener() {
            @Override
            public void onIntegerChanged(int newValue) {
                Log.d("BAF_descGYPN", "NloadObservableInteger");
                switch (newValue){
                    case 0:
                        Log.d("BAF_descGYPN", "NloadObservableInteger10");
                        downloadJsonFromFirebase.bajarPictos(AppLanguage.getLanguaje(), observableInteger);
                        break;
                    case 1:
                        Log.d("BAF_descGYPN", "NloadObservableInteger1");
                        downloadJsonFromFirebase.bajarGrupos(AppLanguage.getLanguaje(), observableInteger);
                        break;
                    case 2:
                        Log.d("BAF_descGYPN", "NloadObservableInteger2");
                        downloadJsonFromFirebase.bajarFrases(AppLanguage.getLanguaje(), observableInteger);
                        break;
                }
                if (observableInteger.get() == size) {
                    getFirebaseDialog().destruirDialogo();
                    json.resetearError();
                    CargarJson();
                }
            }
        });
        return observableInteger;
    }

    public void speakBlock() {
        handlerHablar.removeCallbacks(animarHablar);
        if (mute) {
            speakAction();
        } else {
            myTTS.mostrarAlerta(Oracion, getAnalyticsFirebase());
        }
        handlerHablar.postDelayed(animarHablar, 10000);
        uploadUserPhrases();
    }

    public void speakAction() {
        myTTS.hablar(Oracion, getAnalyticsFirebase());
        savePhrases(Oracion, historial);
        resetSpeakAction();
    }

    public void uploadUserPhrases() {
        if (user.getmAuth().getCurrentUser() != null) {
            FirebaseUploader subirArchivos = new FirebaseUploader(getApplicationContext());
            subirArchivos.subirPictosFirebase(subirArchivos.getmDatabase(user.getmAuth(), Constants.PICTOS), subirArchivos.getmStorageRef(user.getmAuth(), Constants.PICTOS));
            subirArchivos.subirFrasesFirebase(subirArchivos.getmDatabase(user.getmAuth(), Constants.Frases), subirArchivos.getmStorageRef(user.getmAuth(), Constants.Frases));
        }
    }

    public void savePhrases(String phrase, MessageHistory historial) {
        try {
            json.crearFrase(phrase, historial.getListadoPictos(), Calendar.getInstance().getTimeInMillis());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (json.getmJSONArrayTodasLasFrases().length() > 0)
            if (!json.guardarJson(Constants.ARCHIVO_FRASES))
                Log.e(TAG, "speak: Error al guardar Frases");
    }

    public void resetSpeakAction() {
        if (sharedPrefsDefault.getBoolean(getString(R.string.hablarborrar), true)) {
            getAnalyticsFirebase().customEvents("Talk", "Principal", "Talk and Erase");
            Reset();
        }
    }

    public AnalyticsFirebase getAnalyticsFirebase(){
        if(analitycsFirebase == null)
            analitycsFirebase = new AnalyticsFirebase(this);
        return analitycsFirebase;
    }


    public boolean validateMessages(String message){
        if(message != null)
            return sharedPrefsDefault.getString(getString(R.string.avatarmessage),"").equals(message);
        return false;
    }

    public ProgressDialogOptions getFirebaseDialog(){
        if(firebaseDialog == null)
            firebaseDialog = new ProgressDialogOptions(this);
        return firebaseDialog;
    }

    @Override
    public void pictogramsAreSorted(JSONArray array) {
        JSONArray opciones = array;
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                Animation alphaAnimation = AnimationUtils.loadAnimation(getApplicationContext(),
                        R.anim.alpha_show);
                loadChildOption(opciones, 0, alphaAnimation);
                loadChildOption(opciones, 1, alphaAnimation);
                loadChildOption(opciones, 2, alphaAnimation);
                loadChildOption(opciones, 3, alphaAnimation);
                refreshSmartRow();
            }
        });
    }

    @Override
    public void loadSelection(JSONObject jsonObject) {
        CargarOracion(jsonObject, sharedPrefsDefault.getString(getString(R.string.str_idioma), "en"));
        CargarSeleccion(jsonObject);
        CantClicks++;
        loadOptions(json, jsonObject);
    }

    @Override
    public void startAudioTransformation(Transformer.Listener listener, String pathFile, String locationPath) {
        new FileEncoder(this).encodeAudioFile(listener,pathFile,locationPath);
    }

    public class initComponentsClass extends AsyncTask<Void,Void,Void>{
        boolean used = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            initFirebaseComponents();
            firstUserAuthVerification();
            initDefaultSettings(MainBoardActivity.this);
            return null;
        }

        @Override
        protected void onPostExecute(Void unused) {
            super.onPostExecute(unused);
            Log.e(TAG, "initComponentsClass" );
            user.connectClient();
            Log.e(TAG, "connectClient" );
            initFirstPictograms();
            Log.e(TAG, "initFirstPictograms" );
            uploadFiles();
            Log.e(TAG, "uploadFiles" );
            initPlaceImplementationClass();
            Log.e(TAG, "initPlaceImplementationClass" );
            showMenu();
            Log.e(TAG, "showMenu" );
            if (TutoFlag) {
                Log.e(TAG, "TutoFlag" );
                sharedPrefs.edit().putBoolean("PrimerUso", false).apply();
                Log.e(TAG, "Done" );
            }
            navigationControls = new PrincipalControls(MainBoardActivity.this);
            Log.e(TAG, "navigationControls" );
            movableFloatingActionButton.setIcon();
            remoteConfigUtils = RemoteConfigUtils.getInstance();
            loadAvatar();
            showAvatar();
            setOnLongClickListener();
            smartRowView = findViewById(R.id.smartRowView);
            Log.d(TAG, "smartRow view found: " + (smartRowView != null));
            // Eraka prediction model (step 3): train from this child's log in the background,
            // and let it reorder the 4 tiles (OTTAA order stays the fallback / tie-break)
            predictionService().reload();
            initSentenceSuggestions(); // sentence templates + SimpleNLG (13 Oct 2026); replaces the feasibility probe
            json.setChildrenRanker(MainBoardActivity.this::rankChildrenWithEraka);
            if (smartRowView != null) {
                smartRowDataSource = new ErakaSmartRowDataSource(MainBoardActivity.this, predictionService(),
                        MainBoardActivity.this::currentMessageIds);
                smartRowView.setDataSource(smartRowDataSource);
                smartRowView.setOnItemClickListener(MainBoardActivity.this::onSmartRowItemClicked);
                // Fix: the first load used to run without the tiles excluded (a tile could
                // appear twice). refreshSmartRow() sets the exclusions first.
                refreshSmartRow();
            }
        }
    }


    public class loadPictograms implements FailReadPictogramOrigin {
        boolean used;

        public void execute(){
            Log.e(TAG, "Executing: ");
            ExecutorService executorService = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    if (json.getmJSONArrayTodosLosPictos() != null && json.getmJSONArrayTodosLosPictos().length() > 0) {
                        pictoPadre = json.getPictoFromId2(0);
                        pictoPadre = json.getPictoFromId2(0);
                        Log.e(TAG, "pictoPadre: " + pictoPadre);
                        if(pictoPadre != null)
                            Log.e(TAG, "pictoPadre != null");
                        new JsonRecovery().backupPictogram0(getApplicationContext(),pictoPadre,loadPictograms.this);
                    }
                    if(pictoPadre == null){
                        Log.e(TAG, "pictoPadre == null");
                        new JsonRecovery().restorePictogram0(getApplicationContext(),loadPictograms.this);
                    }
                    Log.e(TAG, "Done 2");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            if(used) {
                                used = false;
                                ResetSeleccion();
                            }
                            na.eraka.utils.ErakaPerf.boardReady();
                        }
                    });
                }
            });
        }



        @Override
        public void setParent(JSONObject parent) {
            json.getmJSONArrayTodosLosPictos().put(parent);
            json.setmJSONArrayTodosLosPictos(json.getmJSONArrayTodosLosPictos());
            json.guardarJson(Constants.ARCHIVO_PICTOS);
            json.refreshJsonArrays();
            pictoPadre =  json.getPictoFromId2(0);
            loadDialog();
        }

        @Override
        public void loadDialog() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (pictoPadre != null) {
                        if (historial == null)
                            loadOptions(json, pictoPadre);   // y despues cargamos las opciones con el orden correspondiente
                        used = true;
                    }
                }
            });
        }
    }

    private boolean requestScreenScanningIsEnabled(){
        if(barridoPantalla != null)
            return barridoPantalla.isBarridoActivado();
        return false;
    }

    private EnumImageView getSelectedImage(int option){
        switch (option){
            case 0:
                return EnumImageView.ImageView1;
            case 1:
                return EnumImageView.ImageView2;
            case 2:
                return EnumImageView.ImageView3;
            case 3:
                return EnumImageView.ImageView4;
            case 4:
                return EnumImageView.ImageView5;
            case 5:
                return EnumImageView.ImageView6;
            case 6:
                return EnumImageView.ImageView7;
            case 7:
                return EnumImageView.ImageView8;
            case 8:
                return EnumImageView.ImageView9;
            case 9:
                return EnumImageView.ImageView10;
            default:
                return null;
        }
    }
    private ImageView getImageView(int position){
        EnumImageView aux = getSelectedImage(position);
        if(aux != null)
            return aux.getImageview();
        return null;
    }
}