package na.eraka.welcome;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import na.eraka.FirebaseRequests.DownloadJsonFromFirebase;
import na.eraka.FirebaseRequests.FirebaseUploader;
import na.eraka.JSONutils.Json;
import na.eraka.SignInActivity;
import na.eraka.MainBoardActivity;
import na.eraka.R;
import na.eraka.idioma.AppLanguage;
import na.eraka.utils.AvatarPackage.SelectedAvatar;
import na.eraka.utils.constants.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class SplashActivity extends Activity {

    //Declaro el manejador de preferencia
    private SharedPreferences sharedPrefsDefault;
    private static final String TAG = "SplashActivity";
    private ProgressBar mProgressBar;
    private TextView txtCargando;
    private Animation beat;
    private FirebaseAuth mAuth;
    private DownloadJsonFromFirebase mDownloadJsonFirebase;
    private ChangeText changeName;
    /** Eraka: true once the next screen was opened, so it can never be opened twice. */
    private boolean nextScreenOpened = false;

    /** Opens the next screen once, and never from a splash that is already closing. */
    private void openNext(Class<?> next) {
        if (nextScreenOpened || isFinishing() || isDestroyed()) return;
        nextScreenOpened = true;
        startActivity(new Intent(SplashActivity.this, next));
        finish();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d("Splash", "Splash here!");
        super.onCreate(savedInstanceState);
        Log.d("Splash", "Splash here!");
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.spl_screen_nuevo);
        initComponents();
        txtCargando.setText(getApplicationContext().getText(R.string.splash_loading));
        changeName=new ChangeText();
        new sharedPreferencesLoad(this).execute();

    }


    private void accessDashboard() {
        if (sharedPrefsDefault.getBoolean("usuario logueado", false)) {
            openNext(MainBoardActivity.class);
        } else {
            openNext(SignInActivity.class);
        }
    }
//    /**
//     * Use this method if you want to delete old pictograms from the json object
//     *
//     * */
//
    private void borrarPictosViejos() {
        //poner un shared que i
        if (!sharedPrefsDefault.getBoolean("pictosEliminados", false)) {
            ArrayList<Integer> pictos = new ArrayList<>();
            //Listado con los id de los pictos a borrar
            InputStream inputStream = getResources().openRawResource(R.raw.pictoid);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            int k;
            try {
                k = inputStream.read();
                while (k != -1) {
                    byteArrayOutputStream.write(k);
                    k = inputStream.read();
                }
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                JSONArray array = new JSONArray(byteArrayOutputStream.toString());
                for (int j = 0; j < array.length(); j++) {
                    pictos.add(array.getJSONObject(j).getInt("id"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            JSONArray pictosUsuario = Json.getInstance().getmJSONArrayTodosLosPictos();
            JSONArray grupos = Json.getInstance().getmJSONArrayTodosLosGrupos();
            JSONArray pictosSugeridos = Json.getInstance().getmJSONArrayPictosSugeridos();
            //recorrer el listado de pictos y borrar los que se tienen q eliminar
            Log.d(TAG, "borrarPictosViejos:  tamanio antes" + pictosUsuario.length());
            Log.d(TAG, "borrarPictosViejos:  tamanio picto" + pictos.size());
            borrarPictos(pictosUsuario,pictos);
            Log.d(TAG, "borrarPictosViejos:  tamanio despues " + pictosUsuario.length());
            //recorer el listado de nuevo y preguntar si las relaciones contienen algun elemento de los que se deben borrar, si contiene entrar y borrar sino seguir con el siguiente
            for (int i = 0; i < pictosUsuario.length(); i++) {
                try {
                    if (pictosUsuario.getJSONObject(i).getJSONArray("relacion").length() > 0) {
                        for (int j = 0; j < pictosUsuario.getJSONObject(i).getJSONArray("relacion").length(); j++) {
                            //ver como funciona este metodo en cuanto tiempo
                            int id = pictosUsuario.getJSONObject(i).getJSONArray("relacion").getJSONObject(j).getInt("id");
                            boolean estaEditado = Json.getInstance().estaEditado(Json.getInstance().getPictoFromId2(id));
                            if (pictos.lastIndexOf(id) != -1&&!estaEditado) {
                                pictosUsuario.getJSONObject(i).getJSONArray("relacion").remove(j);
                                Log.d(TAG, "borrarPictosViejos: " + id);
                            }
                        }
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "borrarPictosViejos: Error" + e.getMessage());
                }

            }

            try {
                for (int i = 0; i < pictosSugeridos.length(); i++) {
                    if (pictosSugeridos.getJSONObject(i).getJSONArray("relacion").length() > 0) {
                        for (int j = 0; j < pictosSugeridos.getJSONObject(i).getJSONArray("relacion").length(); j++) {
                            int id = pictosSugeridos.getJSONObject(i).getJSONArray("relacion").getJSONObject(j).getInt("id");
                            if (pictos.lastIndexOf(id) != -1) {
                                pictosSugeridos.getJSONObject(i).getJSONArray("relacion").remove(j);
                                Log.d(TAG, "Picto Padre : " + i + " borrarPictosViejos: " + id);
                            }
                        }
                    }
                }
            } catch (JSONException ex) {
                Log.e(TAG, "borrarPictosViejos: Error" + ex.getMessage());
            }
            JSONArray gruposAux=new JSONArray();
            try {
                for (int i = 0; i < grupos.length(); i++) {
                    JSONArray relacionGrupo = new JSONArray();
                    if (grupos.getJSONObject(i).getJSONArray("relacion").length() > 0) {

                        for (int j = 0; j < grupos.getJSONObject(i).getJSONArray("relacion").length(); j++) {
                            //ver como funciona este metodo en cuanto tiempo
                            int id = grupos.getJSONObject(i).getJSONArray("relacion").getJSONObject(j).getInt("id");
                            boolean estaEditado = Json.getInstance().estaEditado(Json.getInstance().getPictoFromId2(id));
                            int esta=pictos.lastIndexOf(id);
                            if(estaEditado)
                                esta=-1;
                           if (esta == -1) {
                                relacionGrupo.put(grupos.getJSONObject(i).getJSONArray("relacion").getJSONObject(j));
                            } else {
                                Log.d(TAG, "Grupo Padre : " + i + " se pudo borrar : " + id);

                            }
                        }
                        grupos.getJSONObject(i).put("relacion", relacionGrupo);
                    }
                }

                for (int i = 0; i <grupos.length() ; i++) {
                    if (grupos.getJSONObject(i).getInt("id") != 14)
                        gruposAux.put(grupos.getJSONObject(i));
                  if (Json.getInstance().estaEditado(grupos.getJSONObject(i))&&grupos.getJSONObject(i).getInt("id") == 14) {
                          gruposAux.put(grupos.getJSONObject(i));
                  }
                }
            } catch (JSONException ex) {
                Log.e(TAG, "borrarPictosViejos: Error" + ex.getMessage());
            }
            Json.getInstance().setmJSONArrayTodosLosPictos(pictosUsuario);
            Json.getInstance().setmJSONArrayPictosSugeridos(pictosSugeridos);
            Json.getInstance().setmJSONArrayTodosLosGrupos(gruposAux);
            Json.getInstance().guardarJson(Constants.ARCHIVO_PICTOS);
            Json.getInstance().guardarJson(Constants.ARCHIVO_GRUPOS);
            Json.getInstance().guardarJson(Constants.ARCHIVO_PICTOS_DATABASE);
            FirebaseAuth mAuth = FirebaseAuth.getInstance();

            mAuth.addAuthStateListener(firebaseAuth -> {
                if (firebaseAuth.getCurrentUser() != null) {
                    FirebaseUploader subirArchivosFirebase = new FirebaseUploader(SplashActivity.this);
                    subirArchivosFirebase.subirPictosFirebase(subirArchivosFirebase.getmDatabase(mAuth, Constants.PICTOS), subirArchivosFirebase.getmStorageRef(mAuth, Constants.PICTOS));
                    subirArchivosFirebase.subirGruposFirebase(subirArchivosFirebase.getmDatabase(mAuth, Constants.Grupos), subirArchivosFirebase.getmStorageRef(mAuth, Constants.Grupos));
                    sharedPrefsDefault.edit().putBoolean("pictosEliminados", true).apply();

                }
            });

        }
    }

    private void borrarPictos(JSONArray pictosUsuario, ArrayList<Integer> pictos) {
        for (int i = 0; i < pictos.size(); i++) {

            int pos = Json.getInstance().getPosPicto(pictosUsuario, pictos.get(i));
            boolean estaEditado = false;

            try {
                estaEditado = Json.getInstance().estaEditado(pictosUsuario.getJSONObject(pos));
            } catch (JSONException e) {
                Log.e(TAG, "borrarPictosViejos: Error" + e.getMessage());
            }
            if (pos != -1 && !estaEditado)
                pictosUsuario.remove(pos);
        }
    }

    public class borrarPictos  {

        public void execute(){
            Executor executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                borrarPictosViejos();
                handler.post(() -> {
                    mProgressBar.setVisibility(View.GONE);
                    openNext(MainBoardActivity.class);
                });
            });
        }

    }

    public class preLoadSplashScreen {


        public void execute(){
            Executor executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                Json.getInstance().setmContext(getApplicationContext()).initJsonArrays();
                handler.post(SplashActivity.this::accessDashboard);
            });
        }
    }

    private void cargarDatos() {
        if (mAuth.getCurrentUser() != null) {
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                Json.getInstance().setmContext(SplashActivity.this);
                int hashcode = Json.getInstance().hashCode();
                Log.d(TAG, "hashJson: " + hashcode);
                 changeName.cambiarPosicion();
                 String name = sharedPrefsDefault.getString("userAvatar","ic_avatar_default");
                SelectedAvatar.getInstance().setName(name);
                new preLoadSplashScreen().execute();
        }, 2500);
       }else{
           openNext(SignInActivity.class);
       }
    }
    public  class sharedPreferencesLoad {
        private final Context mContext;
        public sharedPreferencesLoad(Context mContext){
            this.mContext=mContext;
        }
        public void execute(){
            Executor executor = Executors.newSingleThreadExecutor();
            Handler handler = new Handler(Looper.getMainLooper());
            executor.execute(() -> {
                sharedPrefsDefault = PreferenceManager.getDefaultSharedPreferences(mContext);
                if (sharedPrefsDefault.getString(getApplicationContext().getResources().getString(R.string.str_idioma), "en").contains("mainTable")) {
                    sharedPrefsDefault.edit().putString(getString(R.string.str_idioma), Locale.getDefault().getLanguage()).apply();
                    AppLanguage.setLanguage(sharedPrefsDefault.getString(getString(R.string.str_idioma),"en"));

                }
                if (!sharedPrefsDefault.contains("idioma")) {
                    sharedPrefsDefault.edit().putString(getString(R.string.str_idioma), Locale.getDefault().getLanguage()).apply();
                    AppLanguage.setLanguage(sharedPrefsDefault.getString(getString(R.string.str_idioma),"en"));

                }
                changeName.cambiarPosicion();
                // Ensure Firebase is initialized with emulators in debug mode
                na.eraka.FirebaseRequests.FirebaseUtils.getInstance().initFirebaseAndEmulator();
                mAuth = na.eraka.FirebaseRequests.FirebaseUtils.getInstance().auth();
                changeName.cambiarPosicion();
                handler.post(() -> cargarDatos());
            });
        }

    }

    private void initComponents(){
        mProgressBar = findViewById(R.id.progress_circular);
        if(mProgressBar!=null)
            mProgressBar.setIndeterminate(true);
        txtCargando = findViewById(R.id.txtCargando);
        beat = AnimationUtils.loadAnimation(SplashActivity.this, R.anim.heartbeat);


    }

    class ChangeText extends Handler{
        private int position;
        private void removeCreatedMessages(int value){
            if(super.hasMessages(value))
                super.removeMessages(value);
        }
        private void removeAllMessages()
        {
            removeCreatedMessages(position);
        }
        @Override
        public void handleMessage(@NonNull Message msg) {
                hacerAccion(position);
        }

        public android.os.Handler getHandler()
        {
            return this;
        }
        public void cambiarPosicion(){
            removeAllMessages();
            position++;
            super.sendMessage(getHandler().obtainMessage(position));
        }
    }

    public void hacerAccion(int position){
        //setVisibleButton();
        switch (position){
            case 1:
                findViewById(R.id.logo_inicial).startAnimation(beat);
                break;
            case 2:
                txtCargando.setText(getApplicationContext().getText(R.string.splash_loading));
            break;
            case 3:
                txtCargando.setText(getApplicationContext().getText(R.string.loadingdata));
                break;
        }
    }
//    public void setVisibleButton(){
//        if(txtCargando.getVisibility()==View.INVISIBLE||txtCargando.getVisibility()==View.GONE)
//        txtCargando.setVisibility(View.VISIBLE);
//    }
}
