package na.eraka.RecyclerViews;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.google.firebase.auth.FirebaseAuth;
import na.eraka.Adapters.PictogramGalleryAdapter;
import na.eraka.Helper.SimpleItemTouchHelperCallback;
import na.eraka.R;
import na.eraka.utils.constants.Constants;
import na.eraka.utils.JSONutils;

import org.json.JSONArray;

public class PictogramSortRecyclerView extends BaseRecyclerView{
    private int button;
    private PictogramGalleryAdapter galeriaPictos2;
    private final String TAG="Picto_R_View_sort";
    public PictogramSortRecyclerView(AppCompatActivity mActivity, FirebaseAuth mAuth) {
        super(mActivity, mAuth);
    }
    public void setArray(int position){
        this.button=position;
        array=json.getHijosGrupo2(position);
        arrayAux=new JSONArray();
        createRecyclerLayoutManager();
        galeriaPictos2=new PictogramGalleryAdapter(mActivity,array, R.layout.grid_item_layout,mAuth);
        mRecyclerView.setAdapter(galeriaPictos2);
        itemTouchHelperCallback = new SimpleItemTouchHelperCallback(galeriaPictos2);
        ItemTouchHelper.Callback callback = itemTouchHelperCallback;
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mRecyclerView);
    }
    @Override
    public void onPictosFiltrados() {

    }

    @Override
    public void onPictosNoFiltrados() {

    }

    public void guardarOrden() {
        JSONArray grupos = json.getmJSONArrayTodosLosGrupos();
        JSONutils.setHijosGrupo2(grupos, galeriaPictos2.getmArrayPictos(), button);
        if (!json.guardarJson(Constants.Grupos))
            Log.e(TAG, "Error al guardar los grupos guardarOrden: ");
    }
}
