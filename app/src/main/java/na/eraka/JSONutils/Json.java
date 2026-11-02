package na.eraka.JSONutils;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Size;

import com.google.android.libraries.places.api.model.Place;
import na.eraka.BuildConfig;
import na.eraka.DrawableManager;
import na.eraka.Interfaces.DrawableInterface;
import na.eraka.Interfaces.SortPictogramsInterface;
import na.eraka.JSONutils.sortPictogramsUtils.SortPictograms;
import na.eraka.Prediction.Weather;
import na.eraka.Prediction.AgeGroup;
import na.eraka.Prediction.TimeOfDay;
import na.eraka.Prediction.Position;
import na.eraka.Prediction.Gender;
import na.eraka.R;
import na.eraka.idioma.AppLanguage;
import na.eraka.utils.JSONutils;
import na.eraka.utils.constants.Constants;
import na.eraka.utils.exceptions.FiveMbException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author morro
 * @author Hector Costa
 * @author Gonzalo Juarez
 * @version 2.0
 * Edited by Hector on 27/11/2018
 * Edited by Hector on 04/01/2019
 * <h3>Objectives</h3>
 * <p>Load,Read and process all Json Object</p>pictos
 * <h3>How to declare</h3>
 * <code>Json json=Json.getInstance();</code>
 * <br>
 * <code>json.setmContext(Context);</code>
 * <h3>Examples of Implementation</h3>
 * <h4>Get id</h4>
 * <code>{@link JSONObject object=getmJSONArrayTodosLosPictos.getJSONObject(position);}</code><br>
 * <code>int id=json.getid(object);</code>
 * <h4>How to add pictogram</h4>
 * <code>json.crearPicto(mArrayListGrupo, mArrayListTodosLosPictos,idPadre,textLanguage,textoEnglis,UrlImage,tipe,urlFirebase,pushKeyPictogram) </code>
 * <h4>How to edit pictogram</h4>
 * <p>
 * {@link #setmJSONArrayTodosLosPictos(JSONArray)}
 * @since 8/5/2016.
 */
public class Json  {


    private static final String TAG = "Json";
    //Declaro el manejador de preferencia
    protected static SharedPreferences sharedPrefsDefault;
    //Json singleton
    private static Json _instance;

    /**
     * Eraka (step 3): optional hook that reorders a pictogram's children after OTTAA's
     * own sort. Set by MainBoardActivity when the Eraka model is on; null = OTTAA order only.
     */
    public interface ChildrenRanker {
        /**
         * Reorders the children (given in the group's fixed order) by the Eraka model.
         * @return true if it ranked them; false = Eraka off or no data, keep OTTAA's order
         */
        boolean reorder(ArrayList<JSONObject> children);
    }

    private volatile ChildrenRanker childrenRanker;

    public void setChildrenRanker(ChildrenRanker ranker) {
        this.childrenRanker = ranker;
    }
    private static boolean fallaJson;
    private final int idGps = 0;
    // Arraylist de Json
    private ArrayList<JSONObject> mArrayListTodasLasFotosBackup;
    private JSONArray mJSONArrayPictosSugeridos;
    private JSONArray mJSONArrayTodosLosGrupos;
    private JSONArray mJSONArrayTodasLasFrases;
    private JSONArray mJSONArrayTodosLosPictos;
    private JSONArray mJSONArrayTodasLasFotosBackup;
    private JSONArray mJSonArrayJuegos;
    private JSONArray mJSonArrayFrasesFavoritas;
    private final String eventoActual = "none";
    private Context mContext;
    private String textoTags;
    private String mListPlaceName = "";
    private ArrayList<Place> placesNames;
    private int cantFallas;

    private static final String MD5_ALGORITHM = "MD5";

    //JSONArray


    //JSON es ahora un singleton
    private Json() {

        this.mJSONArrayTodosLosPictos = new JSONArray();
        this.mJSONArrayPictosSugeridos = new JSONArray();
        this.mJSONArrayTodosLosGrupos = new JSONArray();
        this.mJSONArrayTodasLasFrases = new JSONArray();
        this.mJSONArrayTodasLasFotosBackup = new JSONArray();
        this.mJSonArrayJuegos = new JSONArray();
        this.mJSonArrayFrasesFavoritas = new JSONArray();
        //Implemento el manejador de preferencias
    }

    /**
     * Singleton Instance
     */
    public synchronized static Json getInstance() {
        if (_instance == null) {

            synchronized (Json.class) {
                if (_instance == null) {
                    _instance = new Json();

                }
            }
        }

        return _instance;
    }

    /**
     * set the context to the json object.
     *
     * @param context Activity or context where implements the object
     */
    public Json setmContext(Context context) {
        this.mContext = context;
        sharedPrefsDefault = PreferenceManager.getDefaultSharedPreferences(mContext);
        return this;
    }

    /**
     * Load all the jsonArray objects
     */
    public void initJsonArrays() {
        // Eraka 27 Oct 2026 (speed): the main board called this on every return from another
        // screen, re-reading and parsing all six files (about 4 s on the A13) even when nothing
        // had changed. Skip it when every file is exactly as it was when we last read or wrote it.
        if (filesUnchangedSinceLoad()) {
            Log.d(TAG, "initJsonArrays: files unchanged, kept in memory");
            return;
        }
        long perfT0 = na.eraka.utils.ErakaPerf.start();
        readAllJsonArrays();
        rememberFileStamps();
        na.eraka.utils.ErakaPerf.end("json_load", perfT0);
    }

    // ---- Eraka: which version of each file is in memory (last-modified time + size) ----
    private final java.util.Map<String, String> fileStamps = new java.util.HashMap<>();
    private static final String[] LOADED_FILES = {Constants.ARCHIVO_PICTOS, Constants.ARCHIVO_GRUPOS,
            Constants.ARCHIVO_FRASES, Constants.ARCHIVO_PICTOS_DATABASE, Constants.ARCHIVO_FRASES_FAVORITAS,
            Constants.ARCHIVO_JUEGO};

    private String stampOf(String name) {
        File f = new File(mContext.getFilesDir(), name);
        return f.exists() ? f.lastModified() + "/" + f.length() : "none";
    }

    private synchronized void rememberFileStamps() {
        for (String n : LOADED_FILES) fileStamps.put(n, stampOf(n));
    }

    private synchronized boolean filesUnchangedSinceLoad() {
        if (mContext == null || fileStamps.isEmpty()) return false;
        if (mJSONArrayTodosLosPictos == null || mJSONArrayTodosLosPictos.length() == 0) return false;
        for (String n : LOADED_FILES) if (!stampOf(n).equals(fileStamps.get(n))) return false;
        return true;
    }

    /** Reads all files again, even when they look unchanged. */
    public void forceReloadJsonArrays() {
        synchronized (this) { fileStamps.clear(); }
        initJsonArrays();
    }

    private void readAllJsonArrays() {
        //Cargo por unica vez los archivos al array
        try {
            long tRead = na.eraka.utils.ErakaPerf.start();
            mJSONArrayTodosLosPictos = readJSONArrayFromFile(Constants.ARCHIVO_PICTOS);
            na.eraka.utils.ErakaPerf.end("json_load_pictos_read", tRead, "n=" + mJSONArrayTodosLosPictos.length());
            long tVoc = na.eraka.utils.ErakaPerf.start();
            // Eraka pictogram audit (13 Oct 2026): hide / rename OTTAA pictures for children
            na.eraka.utils.ErakaVocabulary.applyToPictos(mContext, mJSONArrayTodosLosPictos);
            na.eraka.utils.ErakaPerf.end("json_load_pictos_corrections", tVoc);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FiveMbException e) {
            e.printStackTrace();
        }
        try {
            long tSort = na.eraka.utils.ErakaPerf.start();
            new SortPictograms().quickSort(mJSONArrayTodosLosPictos,0,mJSONArrayTodosLosPictos.length()-1);
            na.eraka.utils.ErakaPerf.end("json_load_pictos_sort", tSort);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            long tGroups = na.eraka.utils.ErakaPerf.start();
            mJSONArrayTodosLosGrupos = readJSONArrayFromFile(Constants.ARCHIVO_GRUPOS);
            na.eraka.utils.ErakaVocabulary.applyToGroups(mContext, mJSONArrayTodosLosGrupos);
            na.eraka.utils.ErakaPerf.end("json_load_groups", tGroups, "n=" + mJSONArrayTodosLosGrupos.length());
            GroupManagerClass.getInstance().setmGroup(mJSONArrayTodosLosGrupos);
            // Eraka 28 Oct 2026: pictures made with a negative number get a positive one (ErakaIdFix)
            if (na.eraka.utils.ErakaIdFix.fixNegativeIds(mContext, mJSONArrayTodosLosPictos, mJSONArrayTodosLosGrupos) > 0) {
                new SortPictograms().quickSort(mJSONArrayTodosLosPictos, 0, mJSONArrayTodosLosPictos.length() - 1);
                guardarJson(Constants.ARCHIVO_PICTOS);
                guardarJson(Constants.ARCHIVO_GRUPOS);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FiveMbException e) {
            e.printStackTrace();
        }
        try {
            mJSONArrayTodasLasFrases = readJSONArrayFromFile(Constants.ARCHIVO_FRASES);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FiveMbException e) {
            e.printStackTrace();
        }
        try {
            mJSONArrayPictosSugeridos = readJSONArrayFromFile(Constants.ARCHIVO_PICTOS_DATABASE);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FiveMbException e) {
            e.printStackTrace();
        }
        try {
            mJSonArrayFrasesFavoritas = readJSONArrayFromFile(Constants.ARCHIVO_FRASES_FAVORITAS);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FiveMbException e) {
            e.printStackTrace();
        }
        try {
            mJSonArrayJuegos = readJSONArrayFromFile(Constants.ARCHIVO_JUEGO);
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (FiveMbException e) {
            e.printStackTrace();
        }

    }

    /**
     * Update the JsonArray
     */
    public void refreshJsonArrays(){
        forceReloadJsonArrays();
    }

    /**
     *
     */
    public JSONArray getmJSONArrayTodosLosPictos() {
        return mJSONArrayTodosLosPictos;
    }

    public void setmJSONArrayTodosLosPictos(JSONArray mJSONArrayTodosLosPictos) {
        this.mJSONArrayTodosLosPictos = mJSONArrayTodosLosPictos;
        na.eraka.utils.ErakaVocabulary.applyToPictos(mContext, mJSONArrayTodosLosPictos);
        try {
            if(mJSONArrayTodosLosPictos != null)
               new SortPictograms().quickSort(this.mJSONArrayTodosLosPictos,0,mJSONArrayTodosLosPictos.length()-1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONArray getmJSONArrayPictosSugeridos() {
        return mJSONArrayPictosSugeridos;
    }

    public void setmJSONArrayPictosSugeridos(JSONArray mJSONArrayPictosSugeridos) {
        this.mJSONArrayPictosSugeridos = mJSONArrayPictosSugeridos;
        try {
            new SortPictograms().quickSort(mJSONArrayPictosSugeridos,0,mJSONArrayPictosSugeridos.length()-1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONArray getmJSONArrayTodasLasFotosBackup() {
        return mJSONArrayTodasLasFotosBackup;
    }

    public void setmJSONArrayTodasLasFotosBackup(JSONArray mJSONArrayTodasLasFotosBackup) {
        this.mJSONArrayTodasLasFotosBackup = mJSONArrayTodasLasFotosBackup;
    }

    public JSONArray getmJSONArrayTodosLosGrupos() {
        return GroupManagerClass.getInstance().getmGroup();
    }

    public JSONArray getmJSonArrayGroupsGame(){
        return GroupManagerClass.getInstance().getmGroup();
    }

    public  void setmJSONArrayTodosLosGrupos(JSONArray mJSONArrayTodosLosGrupos) {
        this.mJSONArrayTodosLosGrupos = mJSONArrayTodosLosGrupos;
        na.eraka.utils.ErakaVocabulary.applyToGroups(mContext, mJSONArrayTodosLosGrupos);
        GroupManagerClass.getInstance().setmGroup(this.mJSONArrayTodosLosGrupos);
    }

    public  JSONArray getmJSONArrayTodasLasFrases() {
        return mJSONArrayTodasLasFrases;
    }

    public JSONArray getPhrasesByLanguage(){
        JSONArray aux = mJSONArrayTodasLasFrases;
        JSONArray result = new JSONArray();
        for (int i = 0; i < aux.length(); i++) {
            try {
                if(aux.getJSONObject(i).has("locale")){
                    if(aux.getJSONObject(i).getString("locale").equalsIgnoreCase(AppLanguage.getLanguaje())) {
                        result.put(aux.getJSONObject(i));
                    }
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    public void setmJSONArrayTodasLasFrases(JSONArray mJSONArrayTodasLasFrases) {
        this.mJSONArrayTodasLasFrases = mJSONArrayTodasLasFrases;
    }


    public JSONArray getmJSonArrayJuegos() {
        return mJSonArrayJuegos;
    }

    public void setmJSonArrayJuegos(JSONArray mJSonArrayJuegos) {
        this.mJSonArrayJuegos = mJSonArrayJuegos;
    }

    public Json initSharedPrefs() {
        sharedPrefsDefault = PreferenceManager.getDefaultSharedPreferences(mContext);
        return getInstance();
    }

    public ArrayList<JSONObject> getmArrayListTodasLasFotosBackup() throws FiveMbException {
        mArrayListTodasLasFotosBackup.clear();
        String textoFotosBackup = readFromFile(Constants.ARCHIVO_FOTO_BACKUP);
        if (textoFotosBackup != null) {
            try {
                JSONArray array = new JSONArray(textoFotosBackup);
                for (int i = 0; i < array.length(); i++) {
                    mArrayListTodasLasFotosBackup.add(array.getJSONObject(i));
                }
                Log.d(TAG, "getmArrayListTodasLasFotosBackup: Tenemos el array");
            } catch (JSONException e) {
                Log.e(TAG, "getmArrayListTodasLasFotosBackup: No tenemos el array");
                e.printStackTrace();
            }
        } else {
            Log.e(TAG, "getmArrayListTodasLasFotosBackup: Backup Error");
        }
        return mArrayListTodasLasFotosBackup;
    }

    public Drawable getIcono(JSONObject object) {
        try {
            JSONObject jsonObjectImage = JSONutils.getImagen(object);

            switch (jsonObjectImage.getInt("type")) {
                case 1:
                    Drawable draw = AbrirBitmap(jsonObjectImage.getString("picto"),0);
                    return draw;
                case 2:
                    // Eraka 22 Oct 2026: shown in the skin tone chosen in the child profile
                    return na.eraka.utils.ErakaSkinTone.resource(mContext, jsonObjectImage.getString("picto"));
                case 3:
                    return null;
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }



    public Drawable getIconWithNullOption(JSONObject object) {
        try {
            JSONObject jsonObjectImage = JSONutils.getImagen(object);

            switch (jsonObjectImage.getInt("type")) {
                case 1:
                    Drawable draw = AbrirBitmap(jsonObjectImage.getString("picto"),1);
                    return draw;
                case 2:
                    // Eraka 22 Oct 2026: shown in the skin tone chosen in the child profile
                    return na.eraka.utils.ErakaSkinTone.resource(mContext, jsonObjectImage.getString("picto"));
                case 3:
                    DrawableManager drawableManager = new DrawableManager();
                    return drawableManager.fetchDrawable(jsonObjectImage.getString("urlFoto"), new DrawableInterface() {
                        @Override
                        public Drawable getDrawable(Drawable drawable) {
                            return drawable;
                        }

                        @Override
                        public void fetchDrawable(Drawable drawable) {

                        }

                        @Override
                        public void fetchDrawable(Drawable drawable, int position) {

                        }
                    });
            }
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }catch (Exception ex){
            return null;
        }
        return null;
    }

    public Drawable getBitmap(String path) throws Exception {
        Drawable d = Drawable.createFromPath(path);
        if (d != null)
            return d;
        else {
            Log.d(TAG, "getBitmap: isEmpty");
            Bitmap bitmap = BitmapFactory.decodeFile(path);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                bitmap = mContext.getContentResolver().loadThumbnail(Uri.parse(path), new Size(500, 500), null);
            }
            Canvas canvas = new Canvas(bitmap);
            d.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
            d.draw(canvas);
        }
        return d;
    }

    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = mContext.getContentResolver().query(uri, projection, null, null, null);
        if (cursor != null) {
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        } else return null;
    }

    public Drawable AbrirBitmap(String path,int opt) {
        Drawable d = null;
        if (!path.isEmpty()) {
            try {
                d = getBitmap(path);
            } catch (Exception ex) {
                ex.printStackTrace();
                if(opt ==0)
                    d = mContext.getResources().getDrawable(R.drawable.ic_baseline_cloud_download_24_big);
                else
                    d = null;
            }
        }
        return d;
    }

    private String getAgenda() {
        return eventoActual;
    }

    public void setAgenda(JSONObject ob) {
        JSONArray arrayAgenda = new JSONArray();
        try {
            if (ob.getJSONArray(Constants.CALENDARIO) != null) {
                arrayAgenda = ob.getJSONArray(Constants.CALENDARIO);
                Log.d(TAG, "setAgenda: Not null");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        try {
            arrayAgenda.put(arrayAgenda.length(), JSONutils.getNombre(ob, AppLanguage.getLanguaje()).toUpperCase());
            ob.put(Constants.CALENDARIO, arrayAgenda);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public JSONObject getPictoFromId2(int idABuscar) {
        return JSONutils.getPictoFromId2(getmJSONArrayTodosLosPictos(), idABuscar);
    }


    public JSONArray getHijosGrupo2(int pos) {
        Log.e(TAG, "Calling JSONUtils 1");
        try {
            return JSONutils.getHijosGrupo2(getmJSONArrayTodosLosPictos(), getmJSONArrayTodosLosGrupos().getJSONObject(pos));
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONArray();
        }
    }

    private double score(JSONObject json, boolean esSugerencia) {

        return JSONutils.score(json, esSugerencia, getAgenda(), obtenerSexo(), calcularHora().toString(), obtenerEdad(), calcularPosicion().toString());
    }


    //TODO hasta aca revisado

    public String obtenerSexo() {
        return sharedPrefsDefault.getString("prefSexo", "NotDefined");
    }

    public boolean tieneTag(JSONObject jsonObject, String tag) {

        String enumTag;

        switch (tag) {

            case Constants.HORA:
                enumTag = Arrays.toString(TimeOfDay.values());
                break;

            case Constants.UBICACION:
                enumTag = Arrays.toString(Position.values());
                break;

            case Constants.EDAD:
                enumTag = Arrays.toString(AgeGroup.values());
                break;

            case Constants.CLIMA:
                enumTag = Arrays.toString(Weather.values());
                break;
            case Constants.SEXO:
                enumTag = Arrays.toString(Gender.values());
                break;

            default:
                enumTag = "error";
                break;
        }

        try {
            // Eraka 27 Oct 2026: most pictures have no "tags"; asking with getJSONArray threw an
            // exception and printed a stack trace for each one (about 3 ms each on the A13)
            JSONArray array = jsonObject.optJSONArray("tags");
            if (array == null) return false;
            for (int i = 0; i < array.length(); i++) {
                if (enumTag.contains(array.get(i).toString()))
                    return true;

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void setPlaceName(String name) {
        this.mListPlaceName = "TYPE_" + name;
    }

    public String getCantidadDePlaces() {
        return mListPlaceName;
    }

    public Position calcularPosicion() {

        switch (mListPlaceName) {
            case "TYPE_RESTAURANT":
                return Position.RESTAURANT;

            case "TYPE_BAKERY":
                return Position.PANADERIA;

            case "TYPE_PARK":
                return Position.PARQUE;

            case "TYPE_STORE":
                return Position.TIENDA;

            case "TYPE_SHOPPING_MALL":
                return Position.SHOPPING;

            case "TYPE_AIRPORT":
                return Position.AEROPUERTO;

            case "TYPE_ATM":
                return Position.CAJERO;

            case "TYPE_BANK":
                return Position.BANCO;

            case "TYPE_BAR":
                return Position.BAR;

            case "TYPE_INSURANCE_AGENCY":
                return Position.AGENCIADESEGUROS;

            case "TYPE_SUBWAY_STATION":
                return Position.SUBTE;

            case "TYPE_JEWELRY_STORE":
                return Position.JOLLERIA;

            case "TYPE_BUS_STATION":
                return Position.ESTACIONDEBUS;

            case "TYPE_TAXI_STAND":
                return Position.PARADATAXI;

            case "TYPE_STADIUM":
                return Position.ESTADIO;

            case "TYPE_HOSPITAL":
                return Position.HOSPITAL;

            case "TYPE_MEAL_DELIVERY":
                return Position.DELIVERY;

            case "TYPE_HAIR_CARE":
                return Position.PELUQUERIA;

            case "TYPE_MUSEUM":
                return Position.MUSEO;

            case "TYPE_TRAIN_STATION":
                return Position.ESTACIONDETREN;

            case "TYPE_MOVIE_THEATER":
                return Position.CINE;

            case "TYPE_CAFE":
                return Position.CAFE;

            case "TYPE_SCHOOL":
                return Position.ESCUELA;

            case "TYPE_ZOO":
                return Position.ZOOLOGICO;

            case "TYPE_LAUNDRY":
                return Position.LAVANDERIA;

            case "TYPE_LODGING":
                return Position.ALOJAMIENTO;

            case "TYPE_PET_STORE":
                return Position.VETERINARIA;

            case "TYPE_PHARMACY":
                return Position.FARMACIA;

            case "TYPE_DENTIST":
                return Position.DENTISTA;

            case "TYPE_NIGHT_CLUB":
                return Position.BOLICHE;

            case "TYPE_PHYSIOTHERAPIST":
                return Position.FISIOTERAPIA;

            case "TYPE_AMUSEMENT_PARK":
                return Position.PARQUE;

            case "TYPE_CLOTHING_STORE":
                return Position.TIENDADEROPA;

            case "TYPE_CHURCH":
                return Position.IGLESIA;

            case "TYPE_CONVENIENCE_STORE":
                return Position.TIENDADECONVENIENCIA;

            default:
                return Position.NADA;

        }
    }

    private TimeOfDay calcularHora() {
        SimpleDateFormat df = new SimpleDateFormat("H");
        Calendar SystemTime = Calendar.getInstance();
        try{
            int time = Integer.parseInt(df.format(SystemTime.getTime()));
            if (time >= 5 && time <= 11) {
                return TimeOfDay.MANANA;
            } else if (time > 11 && time <= 14) {
                return TimeOfDay.MEDIODIA;
            } else if (time > 14 && time < 20) {
                return TimeOfDay.TARDE;
            } else {
                return TimeOfDay.NOCHE;
            }
        }catch (Exception ex){
            return TimeOfDay.ISEMPTY;
        }
    }

    public String obtenerEdad() {
        return sharedPrefsDefault.getString("prefEdad", "NINO");
    }

    public boolean useChatGPT() {
        return sharedPrefsDefault.getBoolean("key_chat_gpt", false);
    }

    public int compareTo(double frec1, double frec2) {
        // descending order
        if (frec1 > frec2){
            return -1;
        }
        if (frec2 > frec1){
            return 1;
        }
        return (int) (frec2 - frec1);
    }

    public JSONObject getGrupoFromId(int idABuscar) {
        for (int i = 0; i < GroupManagerClass.getInstance().getmGroup().length(); i++) {
            try {

                if (GroupManagerClass.getInstance().getmGroup().getJSONObject(i).getInt("id") == idABuscar) {
                    return GroupManagerClass.getInstance().getmGroup().getJSONObject(i);
                }

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return null;
    }

    public int getPosPicto(JSONArray listado, int idABuscar) {
        if(listado == null)
            return -1;
        for (int i = 0; i < listado.length(); i++) {
            try {
                if (listado.getJSONObject(i).getInt("id") == idABuscar) {
                    return i;
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "getPosPicto: Error 1");
                return -1;
            }
        }

        return -1;
    }
    public int getPosPictoBinarySearch(JSONArray listado, int idABuscar) {
        if(listado == null)
            return -1;
        return JSONutils.getPositionPicto2(listado,idABuscar);
    }


    public synchronized boolean guardarJson(String archivo) {
        JSONArray jsonArrayAGuardar = new JSONArray();
        switch (archivo) {
            case Constants.ARCHIVO_PICTOS:
                jsonArrayAGuardar = mJSONArrayTodosLosPictos;
                break;
            case Constants.ARCHIVO_GRUPOS:
                jsonArrayAGuardar = GroupManagerClass.getInstance().getmGroup();
                break;
            case Constants.ARCHIVO_FRASES:
                jsonArrayAGuardar = mJSONArrayTodasLasFrases;
                break;
            case Constants.ARCHIVO_PICTOS_DATABASE:
                jsonArrayAGuardar = mJSONArrayPictosSugeridos;
                break;
            case Constants.ARCHIVO_JUEGO:
                jsonArrayAGuardar = mJSonArrayJuegos;
                break;
            case Constants.ARCHIVO_FRASES_FAVORITAS:
                jsonArrayAGuardar = mJSonArrayFrasesFavoritas;
                break;

        }

        try {
            if (isFileOk(jsonArrayAGuardar)) {
                if (jsonArrayAGuardar.length() > 0) {
                    FileOutputStream outputStream;
                    try {
                        outputStream = mContext.openFileOutput(archivo, Context.MODE_PRIVATE);
                        outputStream.write(jsonArrayAGuardar.toString().getBytes());
                        outputStream.close();
                        // Eraka: memory and file are the same again, so no reload is needed
                        synchronized (this) {
                            if (fileStamps.containsKey(archivo)) fileStamps.put(archivo, stampOf(archivo));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return true;
                } else
                    return false;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isFileOk(JSONArray jsonArray) throws JSONException, OutOfMemoryError {
        if (jsonArray != null) {
            int tamArray = jsonArray.toString().getBytes().length;
            if (tamArray > Constants.CINCO_MEGAS) {
                int randomObj = (int) Math.floor(Math.random() * jsonArray.length());
                for (int j = 0; j <= 1; j++) {
                    JSONObject randomJsonObj = jsonArray.getJSONObject(randomObj);
                    for (int i = randomObj + 1; i < jsonArray.length(); i++) {
                        if (jsonArray.getJSONObject(i).equals(randomJsonObj)) {
                            return false;
                        }
                    }
                }

                return true;
            } else {
                return true;
            }
        } else
            return false;
    }

    //Ya esta
    public ArrayList<JSONObject> getArrayListFromTipo(String tipoABuscar, ArrayList<JSONObject> arrayListABuscar) {
        ArrayList<JSONObject> arrayListADevolver = new ArrayList<>();
        for (JSONObject jsonObject : arrayListABuscar) {
            try {
                if (jsonObject.getString("tipo").equals(tipoABuscar)) {
                    arrayListADevolver.add(jsonObject);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return arrayListADevolver;
    }

    public JSONObject getJsonByPosition(JSONArray array,int position){
        try{
            return array.getJSONObject(position);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    //Ya esta
    public JSONObject getPictoFromCustomArrayById(ArrayList<JSONObject> arrayList, int idABuscar) {
        int id = -1;
        for (JSONObject jsonObject : arrayList) {
            try {
                if (getId(jsonObject) == idABuscar) {
                    return jsonObject;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public int getId(JSONObject jsonObject) throws JSONException {
        return JSONutils.getId(jsonObject);
    }


    //metodo encargado de devolver la existencia de un picto dentro de un grupo por medio de la relacion
    private boolean tienePicto(JSONArray arreglo, int id) {
        //primero recorro el arreglo
        for (int i = 0; i < arreglo.length(); i++) {
            try {
                //si tiene el id devuelvo verdadero
                if (arreglo.getJSONObject(i).getInt("id") == id) {
                    return true;
                }
                // en caso de no tenerlo o que salte alguna excepcion devuelve falso

            } catch (JSONException e) {
                return false;
            }
        }
        return false;
    }

    public boolean getFallJson() {
        return fallaJson;
    }



    private String readFromFile(String fileName) {
        File archivo = new File(mContext.getFilesDir(), fileName);
        if (archivo.length() > Constants.CINCO_MEGAS) {
            Log.d(TAG, "readFromFile:  Bigger than 5Mb");
        } else {
            BufferedReader reader = null;
            StringBuilder builder = new StringBuilder();
            FileInputStream fis;
            try {
                if (fileName.equals(Constants.ARCHIVO_PICTOS) || fileName.equals(Constants.ARCHIVO_GRUPOS) || fileName.equals(Constants.ARCHIVO_FRASES) || fileName.equals(Constants.ARCHIVO_PICTOS_DATABASE) || fileName.equals(Constants.ARCHIVO_FRASES_JUEGOS) || fileName.equals(Constants.ARCHIVO_JUEGO) || fileName.equals(Constants.ARCHIVO_JUEGO_DESCRIPCION) || fileName.equals(Constants.ARCHIVO_FRASES_FAVORITAS))
                    fis = mContext.openFileInput(fileName);
                else
                    fis = new FileInputStream(new File(fileName));

                reader = new BufferedReader(new InputStreamReader(fis, StandardCharsets.UTF_8));
                String line;

                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
                fis.close();
            } catch (IOException e) {
                Log.e(TAG, "readFromFile: " + e);
                e.printStackTrace();
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "readFromFile: " + e);

                    }
                }
            }
            return builder.toString();
        }
        return null;
    }

    public byte[] readFromFileBytes(String fileName) {
        // Create a BufferedInputStream object to read the file.
        BufferedInputStream reader = null;

        try {
            FileInputStream fis;

            if (fileName.equals(Constants.ARCHIVO_PICTOS) || fileName.equals(Constants.ARCHIVO_GRUPOS) || fileName.equals(Constants.ARCHIVO_FRASES) || fileName.equals(Constants.ARCHIVO_PICTOS_DATABASE) || fileName.equals(Constants.ARCHIVO_FRASES_JUEGOS) || fileName.equals(Constants.ARCHIVO_JUEGO) || fileName.equals(Constants.ARCHIVO_JUEGO_DESCRIPCION) || fileName.equals(Constants.ARCHIVO_FRASES_FAVORITAS))
                fis = mContext.openFileInput(fileName);
            else
                fis = new FileInputStream(new File(fileName));
            // Open the file for reading.
            reader = new BufferedInputStream(fis);

            // Read the contents of the file into a byte array.
            byte[] contents = new byte[reader.available()];
            reader.read(contents);

            // Close the file.
            reader.close();

            // Return the contents of the file.
            return contents;
        } catch (IOException e) {
            // Handle the exception.
            e.printStackTrace();
        } finally {
            // Close the reader, if it is not null.
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    // Handle the exception.
                    e.printStackTrace();
                }
            }
        }

        return null;

    }

    public boolean verifyFiles(String file1, File file2) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(MD5_ALGORITHM);

        byte[] bytes1 = new byte[1024];
        int numBytesRead1;
        try (FileInputStream fis1 = mContext.openFileInput(file1)) {
            while ((numBytesRead1 = fis1.read(bytes1)) != -1) {
                md.update(bytes1, 0, numBytesRead1);
            }
        }

        byte[] bytes2 = new byte[1024];
        int numBytesRead2;
        try (FileInputStream fis2 = new FileInputStream(file2)) {
            while ((numBytesRead2 = fis2.read(bytes2)) != -1) {
                md.update(bytes2, 0, numBytesRead2);
            }
        }

        byte[] hash1 = md.digest();
        byte[] hash2 = md.digest();

        return Arrays.equals(hash1, hash2);
    }


    //Ya esta hecho
    public JSONObject getJsonObjectFromTexto(ArrayList<JSONObject> arrayList, String stringABuscar) {
        int id = -1;
        for (JSONObject jsonObject : arrayList) {
            try {
                if (jsonObject.getJSONObject("texto").getString("es").equals(stringABuscar)) {
                    return jsonObject;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        //TODO return JSON error
        return null;
    }

    //Este metodo reemplaza a los getmArraylist
    public JSONArray readJSONArrayFromFile(String constantFileName) throws JSONException, FiveMbException {
        return new JSONArray(readFromFile(constantFileName));
    }


    private JSONArray elegirHijos2(JSONObject padre, boolean esSugerencia) throws JSONException {
            JSONArray array = padre.getJSONArray("relacion");
          //  new SortArraysByScore().quickSort(array,0,array.length()-1);
            ArrayList<JSONObject> relacion = new ArrayList<>();
            for (int i = 0; i < array.length(); i++) {
                relacion.add(array.getJSONObject(i));
            }
            // Eraka: the order as stored in the group, before OTTAA sorts by its counts
            final ArrayList<JSONObject> fixedOrder = new ArrayList<>(relacion);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                relacion.sort(new Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject json1, JSONObject json2) {
                        return compareValues(json1,json2,esSugerencia);
                    }
                });
            } else {
                Collections.sort(relacion, new Comparator<JSONObject>() {
                    @Override
                    public int compare(JSONObject json1, JSONObject json2) {
                      return compareValues(json1,json2,esSugerencia);
                    }
                });
            }
            Log.d(TAG, "elegirHijos2: Ordenado");
            // Eraka: rank with the child's own prediction model, starting from the group's
            // fixed order, so pictures the model scores equally (e.g. unknown, score 0) keep
            // that stable order, not OTTAA's counts (which also count undone taps). 7 Oct 2026.
            ChildrenRanker ranker = childrenRanker;
            if (ranker != null) {
                try {
                    ArrayList<JSONObject> ranked = new ArrayList<>(fixedOrder);
                    if (ranker.reorder(ranked)) relacion = ranked;
                } catch (Exception e) {
                    Log.e(TAG, "elegirHijos2: Eraka reorder failed, keeping OTTAA order: " + e.getMessage());
                }
            }
        JSONArray result = new JSONArray();
        for (JSONObject obj : relacion) {
            result.put(obj);
        }

        Log.d(TAG, "elegirHijos2 result: " + result.toString());
        return result;
            //return new JSONArray(relacion.toString());

        //return array;
    }



    public int compareValues(JSONObject json1,JSONObject json2,boolean esSugerencia){
        double frec1 = 0;
        double frec2 = 0;
        try {
            frec1 = getScore(json1, esSugerencia);
            frec2 = getScore(json2, esSugerencia);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return compareTo(frec1, frec2);
    }

    public JSONObject getPictoFromCustomArrayById2(JSONArray jsonArray, int idABuscar) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject object = jsonArray.getJSONObject(i);
            if (object.getInt("id") == idABuscar)
                return object;
        }
        return null;
    }

    public void loadPictogramsInsideArray(JSONObject father,JSONArray array,JSONArray relationShip,int lasPosition) throws JSONException {
        for (int i = 0; i <4 ; i++) {
            int position = i+lasPosition;
            if(isLessThanArray(relationShip,position)){
                Log.e(TAG, "Less than array");
                addChildrenToArray(array,relationShip,position,false);
            }else{
                Log.e(TAG, "Else");
                int lastLocation = position - array.length()+i;
                if(getValueBiggerOrEquals0(lastLocation)){
                    if(itIsASuggestedLanguage()){
                        if(mostrarSugerencias(father,lastLocation,array)){
                            int posPadre = getPosPictoBinarySearch(getmJSONArrayPictosSugeridos(),getId(father));
                            upgradeIndexOfLoadOptions((relationShip.length() + mJSONArrayPictosSugeridos.getJSONObject(posPadre).getJSONArray("relacion").length()) / 4);
                        }else{
                            array.put(i,createAnEmptyObject());
                            upgradeIndexOfLoadOptions(Constants.VUELTAS_CARRETE+1);
                        }
                    }else{
                        array.put(i,createAnEmptyObject());
                        upgradeIndexOfLoadOptions(Constants.VUELTAS_CARRETE+1);
                    }
                }else{
                   addChildrenToArray(array,relationShip,position,false);
                }
            }
        }
    }

    public void upgradeIndexOfLoadOptions(int value){
        if(Constants.VUELTAS_CARRETE != value)
            Constants.VUELTAS_CARRETE = value;
    }

    public boolean isLessThanArray(JSONArray array,int value){
        return value < array.length();
    }
    public boolean getValueBiggerOrEquals0(int value){
        return value >= 0;
    }

    public void addChildrenToArray(JSONArray array, JSONArray relationShip, int position,boolean isSuggested) throws JSONException {
        array.put(getPictoFromId2(relationShip.getJSONObject(position).getInt("id")));
        array.getJSONObject(array.length() - 1).put("esSugerencia", isSuggested);
    }

    public boolean itIsASuggestedLanguage(){
       return  (AppLanguage.getLanguaje().equals("es") || AppLanguage.getLanguaje().equals("ca") || AppLanguage.getLanguaje().equals("en")) && mJSONArrayPictosSugeridos.length() != 0 && sharedPrefsDefault.getBoolean("bool_sugerencias", false);
    }

    public JSONObject createAnEmptyObject() throws JSONException {
        JSONObject object = new JSONObject();
        object.put("id", "-1");
        return object;
    }




    //TODO ver si se puede optimizar
    /*
       variable i : i es un desplazamiento que va de 0 a 3 , ultima posicion es el indicador desde donde comenzar
       variable ultimaposicion : indica la ultima posicion
     *
     * */
    public void cargarOpciones(JSONObject padre, int cuentaMasPictos, SortPictogramsInterface sortPictograms) {
        Log.e(TAG, "cargarOpciones");
        //mJSONArrayTodosLosPictos = readJSONArrayFromFile(Constants.ARCHIVO_PICTOS);// leo los pictos
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
             try {
                 if (!consultarPago())
                     Log.e(TAG, "!consultarPago");
                     sharedPrefsDefault.edit().putBoolean("bool_sugerencias", consultarPago()).apply();
                 JSONArray relacion = elegirHijos2(padre, false); //selecciono el picto padre
                 Log.e(TAG, "relacion" + relacion);
                JSONArray jsonElegidos = new JSONArray();
                int ultimaPosicion = cuentaMasPictos * 4; //posicion del picto
                loadPictogramsInsideArray(padre,jsonElegidos,relacion,ultimaPosicion);
                sortPictograms.pictogramsAreSorted(jsonElegidos);
            }catch (JSONException ex){
                 Log.e(TAG, "exception: "+ ex.getMessage() );
             }
            }
        });
        thread.start();

    }



    /**
     * Este metodo se encarga de buscar los pictos relacionados en sugerencia con un picto determinado
     *
     * @param padre             objeto json padre
     * @param pos               picto en una posicion determinada
     * @param listadoDeElegidos json array a devolver en una posicion determinada
     *                          capturamos las excepciones para ver que hacer
     */
    private boolean mostrarSugerencias(JSONObject padre, int pos, JSONArray listadoDeElegidos) {

        try {
            int positionPadre = getPosPicto(mJSONArrayPictosSugeridos, padre.getInt("id"));
            while (tienePicto(padre.getJSONArray("relacion"), mJSONArrayPictosSugeridos.getJSONObject(positionPadre).getJSONArray("relacion").getJSONObject(pos).getInt("id"))) {
                JSONutils.desvincularJson(mJSONArrayPictosSugeridos.getJSONObject(positionPadre), mJSONArrayPictosSugeridos.getJSONObject(positionPadre).getJSONArray("relacion").getJSONObject(pos).getInt("id"));
            }
            JSONArray opciones = mJSONArrayPictosSugeridos.getJSONObject(positionPadre).getJSONArray("relacion");
            listadoDeElegidos.put(getPictoFromId2(opciones.getJSONObject(pos).getInt("id")));
            listadoDeElegidos.getJSONObject(listadoDeElegidos.length() - 1).put("esSugerencia", true);
            return true;
        } catch (IndexOutOfBoundsException | JSONException ex) {
            Log.e(TAG, "mostrarSugerencias: " + ex.getMessage());
            return false;
        }


    }



    public void cargarPictosSugeridosJson() {
        Log.e(TAG, "cargarPictosSugeridosJson ");
        // Eraka 28 Oct 2026 (speed): this file was read again on every return to the board
        // (0.5 s on the A13) although initJsonArrays had just loaded it. Skip it when unchanged.
        if (mJSONArrayPictosSugeridos != null && mJSONArrayPictosSugeridos.length() > 0 && filesUnchangedSinceLoad()) return;
        try {
            mJSONArrayPictosSugeridos = readJSONArrayFromFile(Constants.ARCHIVO_PICTOS_DATABASE);
            Log.e(TAG, "Able to read from  ARCHIVO_PICTOS_DATABASE");
        } catch (Exception e) {
            e.printStackTrace();
        }
        //pregunto por la posicion y el id
    }

    public void crearFrase(String frase, ArrayList<JSONObject> historial, long fecha) throws JSONException {
        if (!frase.isEmpty()) {
            int pos = fraseExist(frase);
            if (pos != -1) {
                mJSONArrayTodasLasFrases.getJSONObject(pos).put("frecuencia", mJSONArrayTodasLasFrases.getJSONObject(pos).getInt("frecuencia") + 1);
                mJSONArrayTodasLasFrases.getJSONObject(pos).accumulate("fecha", fecha);
                if (!mJSONArrayTodasLasFrases.getJSONObject(pos).has("id")) {
                    int id = getThePhraseLastId() + 1;
                    mJSONArrayTodasLasFrases.getJSONObject(pos).put("id", id);
                }
                if (!mJSONArrayTodasLasFrases.getJSONObject(pos).has("locale"))
                    mJSONArrayTodasLasFrases.getJSONObject(pos).put("locale", AppLanguage.getLanguaje());
                guardarJson(Constants.ARCHIVO_FRASES);
            } else {
                JSONObject nuevaFrase = new JSONObject();
                nuevaFrase.put("frase", frase);
                nuevaFrase.put("frecuencia", 1);
                nuevaFrase.put("complejidad", getComplejidad(historial));
                nuevaFrase.accumulate("fecha", fecha);
                nuevaFrase.put("locale", AppLanguage.getLanguaje());
                int id = getThePhraseLastId() + 1;
                nuevaFrase.put("id", id);
                mJSONArrayTodasLasFrases.put(nuevaFrase);
                guardarJson(Constants.ARCHIVO_FRASES);
            }
        }
    }

    public int getThePhraseLastId() {
        int id = -1;
        try {
            mJSONArrayTodasLasFrases.getJSONObject(0).getInt("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        for (int i = 0; i < mJSONArrayTodasLasFrases.length(); i++) {
            try {
                JSONObject object = mJSONArrayTodasLasFrases.getJSONObject(i);
                if (mJSONArrayTodasLasFrases.getJSONObject(i).has("id")) {
                    int idFrase = mJSONArrayTodasLasFrases.getJSONObject(i).getInt("id");
                    if (id < idFrase)
                        id = idFrase;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return id;
    }

    private int fraseExist(String frase) throws JSONException {

        if (mJSONArrayTodasLasFrases.toString().contains(frase)) {
            for (int i = 0; i < mJSONArrayTodasLasFrases.length(); i++) {
                if (mJSONArrayTodasLasFrases.getJSONObject(i).getString("frase").equals(frase)) {
                    return i;
                }
            }
        }

        return -1;
    }

    private JSONObject getComplejidad(ArrayList<JSONObject> historial) {
        JSONObject complejidad = new JSONObject();
        try {
            complejidad.put("valor", 0);
            complejidad.put("pictos componentes", new JSONArray(getIdFromHistorial(historial)));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return complejidad;
    }

    private ArrayList<JSONObject> getIdFromHistorial(ArrayList<JSONObject> arrayList) {
        if (arrayList != null) {
            ArrayList<JSONObject> historial;
            historial = JSONutils.stringToArrayList(arrayList.toString());
            if (historial.size() > 0) {

                for (JSONObject jsonObject : historial) {
                    jsonObject.remove("texto");
                    jsonObject.remove("tipo");
                    jsonObject.remove("imagen");
                    jsonObject.remove("relacion");
                    jsonObject.remove("agenda");
                    jsonObject.remove("gps");
                }
            }
            return historial;
        } else {
            Log.d(TAG, "getIdFromHistorial: arrayList == null");
            return null;
        }
    }

    public void resetearError() {
        fallaJson = false;
        cantFallas = 0;
    }

    public void sumarFallas() {
        cantFallas++;
    }

    public int getCantFallas() {
        return cantFallas;
    }



    public boolean estaEditado(JSONObject object) {
        try {
            if (object == null)
                return false;
            if(object.has("imagen")){
                if (object.getJSONObject("imagen").has("pictoEditado"))
                    return true;
            }
            else
                return false;
        } catch (JSONException e) {
            e.printStackTrace();
            return false;
        }
        return false;
    }


    public void agregarJuego(JSONObject object) {
        try {

            mJSonArrayJuegos.getJSONObject(0).getJSONObject(object.getInt("game") + "").put(object.getInt("levelId") + "", object);
        } catch (JSONException e) {
            e.printStackTrace();
            mJSonArrayJuegos.put(new JSONObject());
            try {
                JSONObject game = new JSONObject();
                game.accumulate(object.getInt("levelId") + "", object);
                mJSonArrayJuegos.getJSONObject(0).put(object.getInt("game") + "", game);
            } catch (JSONException ex) {
                ex.printStackTrace();
            }
        }
    }

    public JSONObject getGame(int idGame, int leveId) {
        try {
            return mJSonArrayJuegos.getJSONObject(0).getJSONObject(idGame + "").getJSONObject(leveId + "");
        } catch (JSONException e) {
            return null;
        }
    }

    public JSONObject getObjectPuntaje(JSONObject object) {
        try {
            if(object.has("puntaje"))
                return object.getJSONObject("puntaje");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private boolean consultarPago() {
        return true; // Eraka is free: every feature is unlocked
    }

    public int devolverCantidadGruposUsados(int id) {
        int cant = 0;
        try {
            cant = mJSonArrayJuegos.getJSONObject(0).getJSONObject(id + "").length();

        } catch (JSONException e) {
            e.printStackTrace();
            cant = 0;
            Log.e(TAG, "devolverCantidadGruposUsados: " + e.getMessage());

        }
        return cant;
    }



    public int getScore(JSONObject object, boolean isSugerencia) {
        try {
            int score = (int) score(object, isSugerencia);
            return (int) score(object, isSugerencia);
        } catch (Exception ex) {
            Log.e(TAG, "getScore: " + ex.getMessage());
            return 0;
        }
    }



    public JSONArray getmJSonArrayFrasesFavoritas() {
        return mJSonArrayFrasesFavoritas;
    }

    public void setmJSonArrayFrasesFavoritas(JSONArray mJSonArrayFrasesFavoritas) {
        this.mJSonArrayFrasesFavoritas = mJSonArrayFrasesFavoritas;
    }

    public void addAraasacPictogramFromInternet(JSONObject pictogram) {
        mJSONArrayTodosLosPictos.put(pictogram);
    }

    public void addPictogramToAll(JSONObject object) {
        try {
            for (int i = 0; i < mJSONArrayTodosLosGrupos.length(); i++) {
                if (mJSONArrayTodosLosGrupos.getJSONObject(i).optJSONObject("texto").optString("en").equalsIgnoreCase("ALL") || mJSONArrayTodosLosGrupos.getJSONObject(i).optJSONObject("texto").optString("en").equalsIgnoreCase("EveryThing")) {
                    JSONArray relacion = mJSONArrayTodosLosGrupos.getJSONObject(i).getJSONArray("relacion");
                    relacion.put(relacion.length(), object);
                    break;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Context getmContext() {
        return mContext;
    }
}
