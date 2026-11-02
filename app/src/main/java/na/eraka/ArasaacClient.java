package na.eraka;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.DiskBasedCache;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.datatransport.cct.internal.LogEvent;
import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.security.ProviderInstaller;
import com.google.api.LogDescriptor;
import na.eraka.Interfaces.ArasaacSearchListener;
import na.eraka.idioma.AppLanguage;
import na.eraka.utils.HttpsTrustManager;
import na.eraka.utils.JSONutils;
import na.eraka.utils.StringFormatter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;


/**
 * Created by Cristian on 21/6/2016.
 */
public class ArasaacClient {
    private final String TAG = "BuscarArasaac";
    ArasaacSearchListener searchAraasacPictogram;
    private boolean useVolley;

    public void setSearchAraasacPictogram(ArasaacSearchListener searchAraasacPictogram) {
        this.searchAraasacPictogram = searchAraasacPictogram;
    }

    public void HacerBusqueda(String texto, String lang, Context context){
        String direction;
        //String key = context.getResources().getString(R.string.galeria_araasac_api_key);
        String aux =texto.replaceAll(" ","_");
        String q;
        try { q = java.net.URLEncoder.encode(texto.trim(), "UTF-8"); } catch (Exception e) { q = texto.trim(); }
        String globalsymbol = "https://globalsymbols.com/api/v1/labels/search?query="+q+"&symbolset=arasaac&language="+ AppLanguage.getLanguageIso6393(lang);
        // Eraka 27 Oct 2026: ARASAAC's own search is the fallback when Global Symbols fails
        // or finds nothing (the picker showed an empty screen)
        fallbackUrl = "https://api.arasaac.org/v1/pictograms/" + (lang == null || lang.isEmpty() ? "en" : lang) + "/search/" + q.replace("+", "%20");
        //String araasac = "http://old.arasaac.org/api/index.php?callback=json&language="+lang+"&word="+aux+"%&catalog=colorpictos&thumbnailsize=150&TXTlocate=4&KEY="+key;
        /*if(Build.VERSION.SDK_INT<= Build.VERSION_CODES.M){
            JSONutils.setUseVolley(false);
            requestHttp(araasac);
        }
        else{*/
        HttpsTrustManager.allowAllSSL();
        JSONutils.setUseVolley(true);
        requestVolley(context,globalsymbol);
        //}
    }


    private String fallbackUrl;

    private void requestVolley(Context context,String request){
        RequestQueue requestQueue = Volley.newRequestQueue(context,new HurlStack());
        requestQueue.start();
        StringRequest stringRequest = new StringRequest(Request.Method.GET, request, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                JSONArray object = null;
                try {
                    object = new JSONArray(response);
                } catch (JSONException e) {
                    Log.e("BuscarAraasac", "Global Symbols answer not a list: " + e.getMessage());
                }
                if (object != null && object.length() > 0) searchAraasacPictogram.findPictogramsJsonArray(object);
                else requestArasaac(requestQueue);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("BuscarAraasac","Global Symbols error: " + error.getMessage());
                requestArasaac(requestQueue);
            }
        });

        requestQueue.add(stringRequest);
    }

    /**
     * Eraka 27 Oct 2026: ARASAAC's own API. Its results are turned into the Global Symbols
     * shape the picker already reads: {"text": word, "picto": {"image_url": ..., "part_of_speech": ...}}.
     * Always answers the listener (an empty list when nothing is found or there is no
     * connection), so the waiting dialog closes.
     */
    private void requestArasaac(RequestQueue queue) {
        if (fallbackUrl == null) { searchAraasacPictogram.findPictogramsJsonArray(new JSONArray()); return; }
        StringRequest r = new StringRequest(Request.Method.GET, fallbackUrl, response -> {
            JSONArray out = new JSONArray();
            try {
                JSONArray in = new JSONArray(response);
                for (int i = 0; i < in.length() && i < 60; i++) {
                    JSONObject p = in.getJSONObject(i);
                    int id = p.getInt("_id");
                    JSONArray kws = p.optJSONArray("keywords");
                    JSONObject kw = kws != null && kws.length() > 0 ? kws.getJSONObject(0) : null;
                    String word = kw != null ? kw.optString("keyword", "") : "";
                    int type = kw != null ? kw.optInt("type", 2) : 2;   // ARASAAC: 1 name, 2 noun, 3 verb, 4 describing, 5 social, 6 misc
                    JSONObject picto = new JSONObject();
                    picto.put("image_url", "https://static.arasaac.org/pictograms/" + id + "/" + id + "_500.png");
                    picto.put("part_of_speech", type == 3 ? "verb" : type == 4 ? "adjective" : type == 1 ? "subject" : "noun");
                    JSONObject o = new JSONObject();
                    o.put("text", word);
                    o.put("picto", picto);
                    out.put(o);
                }
            } catch (JSONException e) {
                Log.e("BuscarAraasac", "ARASAAC answer: " + e.getMessage());
            }
            searchAraasacPictogram.findPictogramsJsonArray(out);
        }, error -> {
            Log.e("BuscarAraasac", "ARASAAC error: " + error.getMessage());
            searchAraasacPictogram.findPictogramsJsonArray(new JSONArray());
        });
        queue.add(r);
    }




    private void requestHttp(String request){
        try {
            StringBuilder resultado = new StringBuilder();
            String encodeUrl = request.replaceAll("%","%25");
            URL url = new URL(encodeUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String response;
            while ((response = reader.readLine())!=null){
                resultado.append(response);
            }
            reader.close();
            JSONObject object = new JSONObject(resultado.toString());
            searchAraasacPictogram.findPictogramsJsonObject(object);
        } catch (Exception ex) {
            Log.e("error_BuscarAraasac", ex.toString());
        }
    }

    private HostnameVerifier mVerifier(String host){
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            @Override
            public boolean verify(String hostname, SSLSession session) {
                HostnameVerifier hv = HttpsURLConnection.getDefaultHostnameVerifier();
                return hv.verify(host,session);
            }
        };
        return hostnameVerifier;
    }




}
