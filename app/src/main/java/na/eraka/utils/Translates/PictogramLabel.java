package na.eraka.utils.Translates;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import na.eraka.Interfaces.LabelReadyListener;
import na.eraka.R;
import na.eraka.utils.CustomToast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


public class PictogramLabel {

    private final Context mContext;

    private String texto="";
    private String mTarget="";
    private String mSource="";
    private String traduction;
    private LabelReadyListener anInterface;
    private final String TAG="TraducirTexto";

    public PictogramLabel(Context context) {
        mContext=context;
    }





    /**
     * Eraka (19 Oct 2026): offline and English-only, so nothing is translated any more.
     * OTTAA sent the text to an online service (libretranslate.com) when the two languages
     * differed; now the text is kept as typed and the caller is told straight away.
     * The class stays because the edit screens read getTexto()/getmSource()/getmTarget().
     */
    public void traducirIdioma(LabelReadyListener interfaces,String textoATraducir, String idiomaSource, String idiomaTarget,boolean traducir){
        this.texto = textoATraducir;
        this.mSource = idiomaSource;
        this.mTarget = idiomaTarget;
        anInterface=interfaces;
        anInterface.onTextoTraducido(true);
    }
    // can use UI thread here

    protected void onPostExecute(final Void unused) {
        this.texto=traduction;
    }



    public String getTexto() {
        return texto;
    }

   /* @Override
    protected void onPreExecute() {
        super.onPreExecute();

    }*/

    public String getmTarget() {
        return mTarget;
    }

    public void setmTarget(String mTarget) {
        this.mTarget = mTarget;
    }

    public String getmSource() {
        return mSource;
    }

    public void setmSource(String mSource) {
        this.mSource = mSource;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }
}
