package na.eraka.utils.Ttsutils;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import na.eraka.Interfaces.TTSListener;
import na.eraka.R;
import na.eraka.idioma.AppLanguage;
import na.eraka.utils.CustomToast;
import na.eraka.utils.Firebase.CrashlyticsUtils;
import na.eraka.utils.InstalledPackageCheck;

import java.io.File;
import java.util.HashMap;
import java.util.Locale;
/**
 * @author Gonzalo Juarez
 * <h3>Objetive</h3>
 * <p>This class implement the action to speak in high voice the phrase or the pictogram name.</p>
 * <h3>How to declare</h3>
 *   <code>
 *       Declare UtilsTTS utilTts=new UtilsTTS(Context,CustomToast,SharedPreferences);
 *   </code>
 *   <h3>Examples of implementation</h3>
 *     <h4>Prepare the tts engine</h4>
*      <code>utilsTts.preparEngineTTS();</code>
 *      <h4>Talk with dialog</h4>
 *      <code>String word="hello";</code> <br>
 *      <code>utilsTts.hablarConDialogo(world);</code>
 *      <h4>Talk without the dialog screen</h4>
 *      <code>String word="hello";</code> <br>
 *      <code>utilsTts.hablar(world);</code>
 *
 * */
public class UtilsTTS {
    protected SharedPreferences sharedPrefsDefault;
    protected TextToSpeech mTTS;
    protected Context mContext;
    protected CustomToast alerta;
    protected boolean speak;
    private TTSListener ttsListener;
    private final String id="TTSOTTAAID";
    private static UtilsTTS _UtilsTTS;
    /** Eraka 28 Oct 2026: the engine is set up off the main thread (getVoices() is slow). */
    private static final java.util.concurrent.ExecutorService SETUP =
            java.util.concurrent.Executors.newSingleThreadExecutor();
    private static final String GOOGLE_TTS = "com.google.android.tts";
    private volatile boolean shutDown;
    private volatile String appliedSettings = "";
    private final UtteranceProgressListener utteranceProgressListener = new UtteranceProgressListener() {
        @Override
        public void onStart(String utteranceId) {

        }

        @Override
        public void onDone(String utteranceId) {
            if(utteranceId.equalsIgnoreCase(id)&&ttsListener!=null){
                ttsListener.TTSonDone();
                ttsListener= null;
            }
        }

        @Override
        public void onError(String utteranceId) {
            if(ttsListener!=null) {
                ttsListener.TTSonError();
                ttsListener =null;
            }
        }
    };



    public UtilsTTS(Context mContext, CustomToast alerta, SharedPreferences sharedPrefsDefault){
        // Eraka 28 Oct 2026: the application context, because the engine is now kept for the whole
        // app (an activity context would keep a closed screen in memory)
        this.mContext=mContext.getApplicationContext();
        this.alerta=alerta;
        this.sharedPrefsDefault=sharedPrefsDefault;
        TextToSpeech.OnInitListener onInit = new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    speak=true;
                    SETUP.execute(UtilsTTS.this::preparEngineTTS);
                } else if (status == TextToSpeech.ERROR) {
                    speak=false;
                }
            }
        };
        // Eraka: ask for Google's engine straight away. OTTAA connected to the default engine and
        // then called setEngineByPackageName(Google) in onInit, which connects a second time.
        boolean google = new InstalledPackageCheck(this.mContext).estaInstalado(GOOGLE_TTS);
        this.mTTS = google ? new TextToSpeech(this.mContext, onInit, GOOGLE_TTS)
                           : new TextToSpeech(this.mContext, onInit);
        this.mTTS.setOnUtteranceProgressListener(utteranceProgressListener);
    }
    /**
     * <h5>Objetive :</h5>
     * <p> Prepare the Text to Speech Engine.</p>
     * */
    public void preparEngineTTS(){
        try {

            // Primero se fija en que idioma est&aacute configurada la traducci&oacuten para poner el TTS en el idioma correcto
//            Locale localeTTS = new Locale(sharedPrefsDefault.getString(mContext.getString(R
//                    .string.str_idioma),Locale.getDefault().getLanguage()),"en");
            Locale localeTTS = new Locale(sharedPrefsDefault.getString(mContext.getString(R.string
                    .str_idioma), "en"));
            Log.d("texToSpeech_refreshtts", "GetCountry: " + localeTTS.getCountry());
            Log.d("texToSpeech_refreshtts", "GetLanguage: " + localeTTS.getLanguage());

            switch (AppLanguage.getLanguaje()) {
                case "ar":
                    Log.d("texToSpeech_refreshtts", "ARABE");
//                    TODO chequear primero si ya esta insltalada
                   /* boolean instaladoAr = new InstalledPackageCheck(mContext).estaInstalado("com.acapelagroup.android.tts");
                    if (!instaladoAr) {
                        Log.d("instalarAcapella", "si");
                        mContext.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.acapelagroup.android.tts")).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    } else {
                        Log.d("instalarAcapella", "no");

                        mTTS.setEngineByPackageName("com.acapelagroup.android.tts");
                    }*/
                    localeTTS = new Locale("ar");
                    break;

                case "ca":
                    Log.d("texToSpeech_refreshtts", "CATALA");
//                    TODO chequear primero si ya esta insltalada
                    boolean instaladoCat = new InstalledPackageCheck(mContext).estaInstalado("com.acapelagroup.android.tts");
                    if (!instaladoCat) {
                        Intent goToMarketCa = new Intent(Intent.ACTION_VIEW)
                                .setData(Uri.parse("market://details?id=com.acapelagroup.android.tts"));
                        mContext.startActivity(goToMarketCa);
                    } else {
                        mTTS.setEngineByPackageName("com.acapelagroup.android.tts");
                    }
                    break;

                case "es":
                    Log.d("texToSpeech_refreshtts", "ESPANOL");
                    localeTTS = new Locale("spa");
                    break;

                default:

                    if (mTTS.isLanguageAvailable(localeTTS) >= TextToSpeech.LANG_AVAILABLE) {
                        Log.d("texToSpeech_refreshtts", "OTRO IDIOMA " + localeTTS);
                        // Eraka: Google's engine was already chosen when connecting (no second connection)
                        break;
                    } else {
                        Log.d("texToSpeech_refreshtts", "IDIOMA NO DISPONIBLE ");

                        Intent installTTSIntent = new Intent();
                        installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        installTTSIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);   // application context
                        mContext.startActivity(installTTSIntent);
                    }
            }

            mTTS.setLanguage(localeTTS);

        } catch (Exception e) {
            Log.d("texToSpeech_refreshtts", e.getMessage());
            CrashlyticsUtils.getInstance().getCrashlytics().recordException(e);
            // Eraka: this now runs off the main thread; a toast must be shown on the main thread
            new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                    alerta.mostrarFrase(mContext.getString(R.string.pref_error_TTS)));
        }
        // Seteamos los valores de velocidad y tono por defecto
        setUpTTSVOICE();
    }

    private void setUpTTSVOICE(){
        float pitch = sharedPrefsDefault.getInt(mContext.getString(R.string.str_pitch_tts), 10);
        float vel = sharedPrefsDefault.getInt(mContext.getString(R.string.str_velocidad_tts), 10);
        //verificamos si el tts personalizado no esta activado, en caso de no estarlo se setea la velocidad del  tts a su velocidad estandar
        if (!sharedPrefsDefault.getBoolean(mContext.getString(R.string.bool_TTS), false)) {
            pitch = 10;
            vel = 10;
        }
        // esto se encarga de establecer la velocidad del tts  y el tono de voz
        mTTS.setPitch(pitch / 10);
        mTTS.setSpeechRate(vel / 10);
        applySavedVoice();
        appliedSettings = settingsKey();
    }

    /** Pitch, speed and voice as saved now: when they change, the kept engine is set up again. */
    private String settingsKey() {
        return sharedPrefsDefault.getInt(mContext.getString(R.string.str_pitch_tts), 10) + "/"
                + sharedPrefsDefault.getInt(mContext.getString(R.string.str_velocidad_tts), 10) + "/"
                + sharedPrefsDefault.getBoolean(mContext.getString(R.string.bool_TTS), false) + "/"
                + sharedPrefsDefault.getString("eraka_tts_voice", "") + "/"
                + sharedPrefsDefault.getString(mContext.getString(R.string.str_idioma), "en");
    }

    /**
     * Eraka 28 Oct 2026: a screen asked for the voice again. Keep the connection, use the new
     * screen's toast, and only set the engine up again if a voice setting changed.
     */
    public void refresh(CustomToast alerta, SharedPreferences prefs) {
        if (alerta != null) this.alerta = alerta;
        if (prefs != null) this.sharedPrefsDefault = prefs;
        if (speak && !settingsKey().equals(appliedSettings)) SETUP.execute(this::setUpTTSVOICE);
    }

    public boolean isShutDown() { return shutDown; }

    public void shutdown() {
        shutDown = true;
        if (mTTS != null) mTTS.shutdown();
    }

    /** Set once Eraka has chosen the preferred (South African / British) voice by itself (never chosen again after
     *  that, so a caregiver who switches back to another voice keeps their choice). */
    public static final String PREF_ZA_AUTO = "eraka_tts_za_auto_done";

    /** English accents closest to Namibian English, best first. Google's engine has no South
     *  African voice (its English voices: AU, IN, NG, GB, US), so in practice this is British
     *  English, which like Namibian English is non-rhotic ("wata", not "water-r"). */
    private static final String[] PREFERRED_COUNTRIES = {"ZA", "GB"};

    /**
     * The installed offline English voice closest to Namibian English (South African if an
     * engine offers it, otherwise British), or null. Sorted by name, so the choice is stable.
     */
    public static android.speech.tts.Voice preferredEnglishVoice(TextToSpeech tts) {
        try {
            java.util.Set<android.speech.tts.Voice> voices = tts == null ? null : tts.getVoices();
            if (voices == null) return null;
            for (String country : PREFERRED_COUNTRIES) {
                android.speech.tts.Voice best = null;
                for (android.speech.tts.Voice v : voices) {
                    boolean notInstalled = v.getFeatures() != null
                            && v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
                    if (v.isNetworkConnectionRequired() || notInstalled) continue;
                    Locale l = v.getLocale();
                    if (!"en".equals(l.getLanguage()) || !country.equalsIgnoreCase(l.getCountry())) continue;
                    if (best == null || v.getName().compareTo(best.getName()) < 0) best = v;
                }
                if (best != null) return best;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /** Voice picked on the Eraka "Voice & speech" page (empty = phone default). */
    private void applySavedVoice() {
        try {
            String name = sharedPrefsDefault.getString("eraka_tts_voice", "");
            if (name.isEmpty() && !sharedPrefsDefault.getBoolean(PREF_ZA_AUTO, false)) {
                // first use: South African or British English if the phone has it, else the default
                android.speech.tts.Voice za = preferredEnglishVoice(mTTS);
                if (za != null) {
                    mTTS.setVoice(za);
                    sharedPrefsDefault.edit().putString("eraka_tts_voice", za.getName())
                            .putBoolean(PREF_ZA_AUTO, true).apply();
                    Log.d("texToSpeech_voice", "Preferred English voice chosen: " + za.getName() + " " + za.getLocale());
                }
                return;
            }
            if (name.isEmpty()) return;
            android.speech.tts.Voice current = mTTS.getVoice();
            if (current != null && name.equals(current.getName())) return;   // already this voice
            if (mTTS.getVoices() == null) return;
            for (android.speech.tts.Voice v : mTTS.getVoices()) {
                if (v.getName().equals(name)) {
                    mTTS.setVoice(v);
                    return;
                }
            }
        } catch (Exception e) {
            Log.d("texToSpeech_voice", "applySavedVoice: " + e.getMessage());
        }
    }

    /**
     * <h5>Objetive :</h5>
     * <p>Speak the phrase without dialog.</p>
     * @param frase Text to said in high voice with the TTS.
     * */
    public void hablar(String frase){
        speak=true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle bundle = new Bundle();
            bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"");
            mTTS.speak(frase, TextToSpeech.QUEUE_FLUSH, bundle, id);
        } else {
            HashMap<String, String> hashTts = new HashMap<String, String>();
            hashTts.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id);
            mTTS.speak(frase, TextToSpeech.QUEUE_FLUSH, hashTts);
        }
    }
    /**
     * <h5>Objetive :</h5>
     * <p>Speak the phrase with dialog screen.</p>
     * @param frase Text to said in high voice with the TTS.
     * */
    public void hablarConDialogo(String frase){
        Log.d("texToSpeech_hablar", "Hablar");
        alerta.mostrarFrase(frase);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle bundle = new Bundle();
            bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"");
            mTTS.speak(frase, TextToSpeech.QUEUE_FLUSH, bundle, id);
        } else {
            HashMap<String, String> hashTts = new HashMap<String, String>();
            hashTts.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id);
            mTTS.speak(frase, TextToSpeech.QUEUE_FLUSH, hashTts);
        }
    }


    public void SyntetizeFile(String frase, File file){
        alerta.mostrarFrase(frase);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Bundle bundle = new Bundle();
            bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"");
            bundle.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, TextToSpeech.Engine.DEFAULT_STREAM);
            bundle.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME,0.5f);
            bundle.putFloat(TextToSpeech.Engine.KEY_PARAM_PAN,0);
            mTTS.synthesizeToFile(frase, bundle,file, id);
        } else {
            HashMap<String, String> hashTts = new HashMap<String, String>();
            hashTts.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, id);
            mTTS.synthesizeToFile(frase,hashTts,file.getAbsolutePath());
        }
    }


    public TextToSpeech getmTTS() {
        return mTTS;
    }

    public boolean isSpeak() {
        return speak;
    }

    public void setTtsListener(TTSListener ttsListener){
        this.ttsListener = ttsListener;
    }


}
