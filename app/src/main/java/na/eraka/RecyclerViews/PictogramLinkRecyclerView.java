package na.eraka.RecyclerViews;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import na.eraka.Adapters.LinkPictogramsAdapter;
import na.eraka.JSONutils.Json;
import na.eraka.R;
import na.eraka.utils.JSONutils;

import org.json.JSONArray;
import org.json.JSONException;

public class PictogramLinkRecyclerView extends BaseRecyclerView  {
    private JSONArray arrayVincular;
    private LinkPictogramsAdapter galeriaPictos2;
    private final String TAG="Picto_Vincular_R_V";
    public PictogramLinkRecyclerView(AppCompatActivity mActivity, FirebaseAuth mAuth) {
        super(mActivity, mAuth);
    }


    public void setArray(){

            array= Json.getInstance().getmJSONArrayTodosLosPictos();
            arrayVincular=new JSONArray();
        createRecyclerLayoutManager();
        galeriaPictos2=new LinkPictogramsAdapter(mActivity, R.layout.grid_item_layout,array,false).initGlideAttatcher();
        mRecyclerView.setAdapter(galeriaPictos2);
    }

    public void changeData(){
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }

    public void filtrarPictogramas(){
        galeriaPictos2.setmVincularArray(arrayAux);
        galeriaPictos2.setEsFiltrado(true);
        mRecyclerView.getAdapter().notifyDataSetChanged();
    }
    public void pictogramasNoFiltrados(){
        galeriaPictos2.setEsFiltrado(false);
        try {
            array=JSONutils.getHijosGrupo2(json.getmJSONArrayTodosLosPictos(),json.getGrupoFromId(24));
            arrayAux=new JSONArray();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        galeriaPictos2.setmVincularArray(array);
        mRecyclerView.getAdapter().notifyDataSetChanged();

    }

    public JSONArray getArray() {
        return array;
    }



    public JSONArray getmArrayFiltrados() {
        return arrayAux;
    }


    @Override
    public void onPictosFiltrados() {
        filtrarPictogramas();
    }

    @Override
    public void onPictosNoFiltrados() {
        pictogramasNoFiltrados();
    }

    public LinkPictogramsAdapter getGaleriaPictos2() {
        return galeriaPictos2;
    }


}
