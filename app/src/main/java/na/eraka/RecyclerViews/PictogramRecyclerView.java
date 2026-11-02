package na.eraka.RecyclerViews;

import android.content.Intent;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import na.eraka.Adapters.PictogramGalleryAdapter;
import na.eraka.Dialogos.DialogUtils.YesNoDialog;
import na.eraka.EditPictogramActivity;
import na.eraka.Helper.RecyclerItemClickListener;
import na.eraka.JSONutils.Json;
import na.eraka.R;
import na.eraka.utils.IntentCode;
import na.eraka.utils.JSONutils;
import na.eraka.utils.PopupMenuUtils;
import na.eraka.utils.constants.Constants;
import na.eraka.utils.preferences.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class PictogramRecyclerView extends BaseRecyclerView {
    private int button;
    private PictogramGalleryAdapter galeriaPictos2;
    private int id=-1;
    private final String TAG="Picto_Recycler_View";
    private boolean editarPicto;

    public PictogramRecyclerView(AppCompatActivity mActivity, FirebaseAuth mAuth) {
        super(mActivity, mAuth);

    }



    //Load
    public void setArray(int position){
        this.button=position;
        array=json.getHijosGrupo2(position);
        arrayAux=new JSONArray();
        createRecyclerLayoutManager();
        galeriaPictos2=new PictogramGalleryAdapter(mActivity,array, R.layout.grid_item_layout,mAuth);
        mRecyclerView.setAdapter(galeriaPictos2);
        mRecyclerView.addOnItemTouchListener(listener());
    }


    @Override
    public void sincronizeData() {
        json=Json.getInstance();
        json.setmContext(mActivity);
        array=json.getHijosGrupo2(button);
        galeriaPictos2.setmArrayPictos(array);
    }

    public void filterChildren(){
        galeriaPictos2.setmArrayPictos(arrayAux);
        galeriaPictos2.notifyDataSetChanged();
        mRecyclerView.invalidate();
    }


    /**
     * this method set up the visibility around a recycler view
     *
     * */


    public JSONArray getArray() {
        return array;
    }


    @Override
    public void onPictosFiltrados() {
        filterChildren();
    }

    @Override
    public void onPictosNoFiltrados() {
        sincronizeData();
    }

    private RecyclerItemClickListener listener(){
        return new RecyclerItemClickListener(mRecyclerView,mActivity,new RecyclerItemClickListener.OnItemClickListener(){
            @Override
            public void onItemClick(View view, int position) {
                try {
                    // Eraka (16 Oct 2026): a tap only says the word, so a child can listen
                    // without changing the message. Long press -> "Add to sentence".
                    id = json.getId(galeriaPictos2.getmArrayPictos().getJSONObject(position));
                    // Eraka 27 Oct 2026: the parent's recording or home word, if there is one
                    JSONObject tapped = galeriaPictos2.getmArrayPictos().getJSONObject(position);
                    String say = na.eraka.utils.ErakaHomeWords.speakPicto(mActivity, tapped,
                            JSONutils.getNombre(tapped, sharedPrefsDefault.getString(mActivity.getString(R.string.str_idioma), "en")));
                    if (say != null) myTTS.hablar(say);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onDoubleTap(View view, int position) {

                }

            @Override
            public void onLongClickListener(View view, int position) {
                editarPicto = sharedPrefsDefault.getBoolean(mActivity.getString(R.string.str_editar_picto), true);
                try {
                    id = json.getId(galeriaPictos2.getmArrayPictos().getJSONObject(position));
                    JSONObject held = galeriaPictos2.getmArrayPictos().getJSONObject(position);
                    String say = na.eraka.utils.ErakaHomeWords.speakPicto(mActivity, held,
                            JSONutils.getNombre(held, sharedPrefsDefault.getString(mActivity.getString(R.string.str_idioma), "en")));
                    if (say != null) myTTS.hablar(say);
                    ShowpopMenu(view, position);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        } );
    }

    public void ShowpopMenu(View view,int position) {
        PopupMenuUtils popupMenuUtils = new PopupMenuUtils(mActivity,view);
        popupMenuUtils.addClickListener( new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {

                if (menuItem.getItemId() == PopupMenuUtils.ITEM_ADD_TO_SENTENCE) {
                    addToSentence();
                    return true;
                }
                switch (menuItem.getItemId()) {
                    case R.id.item_edit:
                        if (galeriaPictos2.getmArrayPictos() != null) {
                            // JSONObject aux = pictosDelGrupo.get(position);
                            analyticsFirebase.customEvents("Touch","Galeria Pictos","Edit Pictogram");
                            if (id != -1) {
                                if(User.getInstance(mActivity).isPremium()){
                                    mActivity.startActivityForResult(startEditAction(position), IntentCode.EDITARPICTO.getCode());
                                }
                            }
                        }
                        return true;
                    case R.id.item_delete:
                        analyticsFirebase.customEvents("Touch","Galeria Pictos","Delete Pictogram");
                        AlertBorrar();
                        return true;
                }
                return false;
            }
        });
        popupMenuUtils.inflateWithAddToSentence(editarPicto);

    }

    /** Eraka: puts the selected pictogram in the message and goes back to the board. */
    private void addToSentence() {
        Intent databack = new Intent();
        databack.putExtra("ID", id);
        databack.putExtra("Boton", button);
        mActivity.setResult(IntentCode.GALERIA_PICTOS.getCode(), databack);
        mActivity.finish();
    }

    @Override
    public void AlertBorrar() {
        YesNoDialog dialogo1 = new YesNoDialog(mActivity);
        dialogo1.setTitle(mActivity.getResources().getString(R.string.pref_important_alert)).setMessage(mActivity.getResources().getString(R.string.pref_text4_alert));
        dialogo1.setOnClick(dialogo1.getObject(R.id.yes_button), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONArray pictosGrupos=json.getmJSONArrayTodosLosGrupos();
                try {
                    JSONutils.setHijosGrupo2(pictosGrupos, array, button);
                    JSONutils.desvincularJson(pictosGrupos.getJSONObject(button), id);
                    array = JSONutils.getHijosGrupo2(json.getmJSONArrayTodosLosPictos(),pictosGrupos.getJSONObject(button));
                    galeriaPictos2.setmArrayPictos(array);
                    galeriaPictos2.notifyDataSetChanged();
                    json.setmJSONArrayTodosLosGrupos(pictosGrupos);
                    if (!json.guardarJson(Constants.ARCHIVO_GRUPOS))
                        Log.e(TAG, "Error al guardar el json");
                    subirGrupos();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                dialogo1.cancelarDialogo();

            }
        });
        dialogo1.setOnClick(dialogo1.getObject(R.id.no_button), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogo1.cancelarDialogo();

            }
        });

        dialogo1.mostrarDialogo();
    }
    /** Eraka 28 Oct 2026: after removeFromGroup (background), show the group's pictures again. */
    public void showPictures() {
        galeriaPictos2.setmArrayPictos(array);
        galeriaPictos2.notifyDataSetChanged();
    }

    /** Eraka 28 Oct 2026: the pictures of this group, for "Select and delete". */
    public JSONArray getPictures() { return array; }

    /**
     * Eraka 28 Oct 2026: removes several pictures from this group at once (the same as the
     * single "Delete": the pictures stay in the app and in other groups).
     */
    public void removeFromGroup(java.util.List<Integer> ids) {
        JSONArray pictosGrupos = json.getmJSONArrayTodosLosGrupos();
        try {
            JSONutils.setHijosGrupo2(pictosGrupos, array, button);
            JSONObject group = pictosGrupos.getJSONObject(button);
            for (int pictoId : ids) JSONutils.desvincularJson(group, pictoId);
            array = JSONutils.getHijosGrupo2(json.getmJSONArrayTodosLosPictos(), group);
            json.setmJSONArrayTodosLosGrupos(pictosGrupos);
            if (!json.guardarJson(Constants.ARCHIVO_GRUPOS)) Log.e(TAG, "Error al guardar el json");
            subirGrupos();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void guardarDatosGrupo() {
        JSONArray pictosGrupos = json.getmJSONArrayTodosLosGrupos();
        JSONutils.setHijosGrupo2(pictosGrupos, array, button);
        json.setmJSONArrayTodosLosGrupos(pictosGrupos);
        if (!json.guardarJson(Constants.ARCHIVO_GRUPOS))
            Log.e(TAG, "Error al guardar el json");
        subirGrupos();
    }

    public void guardarDatosGrupoOnlyId() {
        JSONArray pictosGrupos = json.getmJSONArrayTodosLosGrupos();
        JSONArray aux = new JSONArray();
        for (int i = 0; i <array.length() ; i++) {
            try {
                JSONObject child = new JSONObject();
                child.put("id",array.getJSONObject(i));
                aux.put(child);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        JSONutils.setHijosGrupo2(pictosGrupos, aux, button);
        json.setmJSONArrayTodosLosGrupos(pictosGrupos);
        if (!json.guardarJson(Constants.ARCHIVO_GRUPOS))
            Log.e(TAG, "Error al guardar el json");
        subirGrupos();
    }


    @Override
    protected Intent startEditAction(int position) {
        Intent intent = new Intent(mActivity, EditPictogramActivity.class);
        loadPictogramsValue(intent,position);
        myTTS.hablar(mActivity.getString(R.string.editar_pictogram));
        json.setmJSONArrayTodosLosGrupos(json.getmJSONArrayTodosLosGrupos());
        if (!json.guardarJson(Constants.ARCHIVO_GRUPOS)) {
            Log.i(TAG, "onMenuItemClick: no se pudo guardar el mensaje");
        }
        return intent;
    }

    public void loadPictogramsValue(Intent intent,int position){
        intent.putExtra("PositionPadre", button);
        intent.putExtra("PositionHijo", position);
        intent.putExtra("PictoID", id);

        try {
            intent.putExtra("Texto", JSONutils.getNombre(galeriaPictos2.getmArrayPictos().getJSONObject(position),sharedPrefsDefault.getString(mActivity.getString(R.string.str_idioma), "en")));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        intent.putExtra("esGrupo", false);
    }

}
