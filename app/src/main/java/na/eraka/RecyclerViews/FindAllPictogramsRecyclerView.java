package na.eraka.RecyclerViews;

import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import na.eraka.Adapters.FindAllPictogramsAdapter;
import na.eraka.ArasaacClient;
import na.eraka.Dialogos.DialogUtils.ProgressDialogOptions;
import na.eraka.Helper.ItemTouchHelperAdapter;
import na.eraka.Helper.RecyclerItemClickListener;
import na.eraka.Interfaces.FindAllPictogramsInterface;
import na.eraka.Interfaces.ArasaacSearchListener;
import na.eraka.Interfaces.LabelReadyListener;
import na.eraka.R;
import na.eraka.idioma.AppLanguage;
import na.eraka.utils.ConnectionDetector;
import na.eraka.utils.Pictures.DownloadPictogram;
import na.eraka.utils.constants.Constants;
import na.eraka.utils.IntentCode;
import na.eraka.utils.JSONutils;
import na.eraka.utils.Translates.PictogramLabel;
import com.stonefacesoft.pictogramslibrary.Classes.Pictogram;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FindAllPictogramsRecyclerView extends BaseRecyclerView implements ItemTouchHelperAdapter, LabelReadyListener {

    private final ArasaacClient buscarArasaac;
    private FindAllPictogramsAdapter findAllPictogramsAdapter;
    private JSONObject arasaac;
    private JSONArray arasaacPictogramsResult;
    private int id;
    private final ProgressDialogOptions progress_dialog_options;
    private JSONObject selectedObject;
    private na.eraka.utils.Translates.PictogramLabel traducirTexto;
    public FindAllPictogramsRecyclerView(AppCompatActivity appCompatActivity, FirebaseAuth mAuth) {
        super(appCompatActivity, mAuth);
        buscarArasaac = new ArasaacClient();
        progress_dialog_options = new ProgressDialogOptions(appCompatActivity,mActivity.getResources().getString(R.string.pref_login_wait),mActivity.getResources().getString(R.string.searching_araasac));

    }



    public void setArray() {
        array = json.getHijosGrupo2(json.getPosPicto(json.getmJSONArrayTodosLosGrupos(),24));
        createRecyclerLayoutManager();
        findAllPictogramsAdapter = new FindAllPictogramsAdapter(mActivity, R.layout.grid_item_layout, array, true);
        mRecyclerView.setAdapter(findAllPictogramsAdapter);
        mRecyclerView.addOnItemTouchListener(listener());
    }


    @Override
    public void onPictosFiltrados() {
        filtrarPictogramas();
    }

    @Override
    public void onPictosNoFiltrados() {
        findAllPictogramsAdapter.setmArrayPictos(array);
        findAllPictogramsAdapter.setEsFiltrado(false);
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }

    public void filtrarPictogramas() {
        findAllPictogramsAdapter.setmArrayPictos(arrayAux);
        findAllPictogramsAdapter.setEsFiltrado(true);
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }



    @Override
    public boolean onQueryTextSubmit(String query) {
        if(!progress_dialog_options.isShowing()){
            progress_dialog_options.setMessage(mActivity.getResources().getString(R.string.searching_araasac)+" araasac");
            progress_dialog_options.mostrarDialogo();
        }
        analyticsFirebase.customEvents("Touch", "Editar Grupos", "Search Pictogram");
        if (mSearchView.getQuery().length() > 0) {
            mSearchView.setIconified(true);
            arrayAux = new JSONArray();
            int cant = array.length() / 200;
            int cont = 0;
            do {
                if ((cont * 200 + 200) < array.length())
                    recorrerListado(cont * 200, (cont * 200 + 200), query);
                else
                    recorrerListado(cont * 200, array.length(), query);
                cont++;
            } while (cont <= cant);
            //Nos aseguramos que es vincular o no, si lo es seteamos el array nuevo de pictos filtrados
            //para que el adapter trabaje con ese array.

            if(ConnectionDetector.isNetworkAvailable(mActivity)){
                new HTTPRequest(query).execute();
                mSearchView.setQuery(query, false);
            }else{
                progress_dialog_options.destruirDialogo();
                onPictosFiltrados();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onItemMove(int fromIndex, int toIndex) {

    }

    @Override
    public void onItemDismiss(int position) {

    }

    @Override
    public void onDropItem() {

    }



    private RecyclerItemClickListener listener() {
        return new RecyclerItemClickListener(mRecyclerView, mActivity, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                try {
                    selectedObject = findAllPictogramsAdapter.getmArrayPictos().getJSONObject(position);
                    int idPicto =  json.getId(selectedObject);
                    if (id != idPicto) {
                        id = idPicto;
                        String say = na.eraka.utils.ErakaHomeWords.speakPicto(mActivity, selectedObject,
                                JSONutils.getNombre(selectedObject, AppLanguage.getLanguaje()));   // Eraka: home word / voice
                        if (say != null) myTTS.hablar(say);
                    } else {
                        int posPicto = json.getPosPicto(json.getmJSONArrayTodosLosPictos(),id);
                        Log.e(TAG, "position pictogram: " + posPicto);
                        if(posPicto!=-1){
                            Intent databack = new Intent();
                            databack.putExtra("ID", id);
                            mActivity.setResult(IntentCode.SEARCH_ALL_PICTOGRAMS.getCode(), databack);
                            mActivity.finish();
                        }else{
                            translateText();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onDoubleTap(View view, int position) {

            }

            @Override
            public void onLongClickListener(View view, int position) {
                int idPicto = 0;
                try {
                    idPicto = json.getId(findAllPictogramsAdapter.getmArrayPictos().getJSONObject(position));
                    if (id != idPicto) {
                        id = idPicto;
                        JSONObject held = findAllPictogramsAdapter.getmArrayPictos().getJSONObject(position);
                        String say = na.eraka.utils.ErakaHomeWords.speakPicto(mActivity, held, JSONutils.getNombre(held, AppLanguage.getLanguaje()));
                        if (say != null) myTTS.hablar(say);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    public void onTextoTraducido(boolean traduccion) {
        if(traduccion){
            try {
                if(progress_dialog_options.isShowing()){
                    progress_dialog_options.destruirDialogo();
                }
                selectedObject.put("relacion",new JSONArray());
                JSONutils.setNombre(selectedObject,JSONutils.getNombre(selectedObject,AppLanguage.getLanguaje()),traducirTexto.getTexto(),AppLanguage.getLanguaje(),"en");
                json.addAraasacPictogramFromInternet(selectedObject);

                Pictogram pictogram = new Pictogram(selectedObject,AppLanguage.getLanguaje());

                new DownloadPictogram(mActivity, pictogram.getName(), pictogram.getType(), selectedObject, new FindAllPictogramsInterface() {
                    @Override
                    public void downloadPictogram(Pictogram result) {
                        try {
                            JSONObject relacion = new JSONObject();
                            relacion.put("id",json.getId(result.toJsonObject()));
                            relacion.put("frec",0);
                            json.addPictogramToAll(relacion);
                            //  databack.putExtra("Boton", button);
                            json.guardarJson(Constants.ARCHIVO_GRUPOS);
                            json.guardarJson(Constants.ARCHIVO_PICTOS);
                            subirPictos();
                            subirGrupos();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        Intent databack = new Intent();
                        databack.putExtra("ID", id);
                        mActivity.setResult(IntentCode.SEARCH_ALL_PICTOGRAMS.getCode(), databack);
                        mActivity.finish();
                    }
                }).execute(pictogram.getUrl());


            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    }

    private class HTTPRequest extends AsyncTask<Void, Void, Void> implements ArasaacSearchListener {

        //        private ProgressDialog progressDialog = new ProgressDialog(ArasaacSearchActivity.this);
        String texto;
        boolean useVolley;

        public HTTPRequest(String texto) {
            buscarArasaac.setSearchAraasacPictogram(this);
            this.texto = texto;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            buscarArasaac.HacerBusqueda(texto, AppLanguage.getLanguaje(), mActivity);
            return null;
        }

        /**
         * Una vez que paso el tiempo de espera ilumino la parte adecuando
         *
         * @param unused
         */
        protected void onPostExecute(final Void unused) {
            try {
                if (arasaacPictogramsResult!= null) {

                    for (int i = 0; i < arasaacPictogramsResult.length()-1; i++) {
                        JSONObject pictogram = arasaacPictogramsResult.getJSONObject(i);
                        Pictogram picto = new Pictogram();
                        String url = JSONutils.getUriByApi(pictogram);
                        String text = JSONutils.getStringByApi(pictogram);
                        int tipo = JSONutils.getTypeAsInteger(pictogram);
                        picto.setLocale(sharedPrefsDefault.getString(mActivity.getResources().getString(R.string.str_idioma), "en"));
                        picto.setName_en(text);
                        picto.setName(text);
                        picto.setUrl(url);
                        picto.setEditedPictogram(url);
                        picto.setType(tipo);
                        picto.setId(na.eraka.utils.JSONutils.newId());   // Eraka: positive id (see JSONutils.newId)
                        arrayAux.put(arrayAux.length(), picto.toJsonObject());
                    }
                    onPictosFiltrados();
                }
            } catch (JSONException e) {
                e.printStackTrace();
                Toast.makeText(mActivity, R.string.problema_inet, Toast.LENGTH_LONG).show();
                onPictosFiltrados();
            }
            if(progress_dialog_options!=null)
                progress_dialog_options.destruirDialogo();
        }

        @Override
        public void findPictogramsJsonObject(JSONObject value) {
            arasaac = value;
            try {
                arasaacPictogramsResult = arasaac.getJSONArray("symbols");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            onPostExecute(null);
        }

        @Override
        public void findPictogramsJsonArray(JSONArray value) {
            arasaacPictogramsResult = value;
            onPostExecute(null);
        }
    }

    public ProgressDialogOptions getProgress_dialog_options() {
        return progress_dialog_options;
    }
    public void translateText(){
        traducirTexto = new PictogramLabel(mActivity);
        String textoPicto =JSONutils.getNombre(selectedObject,AppLanguage.getLanguaje());
        traducirTexto.traducirIdioma(FindAllPictogramsRecyclerView.this::onTextoTraducido,textoPicto, sharedPrefsDefault.getString(mActivity.getResources().getString(R.string.str_idioma), "en"), "en", ConnectionDetector.isNetworkAvailable(mActivity));
        if (ConnectionDetector.isNetworkAvailable(mActivity)) {
            // pd.setTitle(getString(R.string.translating_languaje));
            progress_dialog_options.setCancelable(false);
            progress_dialog_options.mostrarDialogo();
            progress_dialog_options.setMessage(mActivity.getResources().getString(R.string.translating_languaje) + textoPicto);
            //pd.show();
        }

    }


}
