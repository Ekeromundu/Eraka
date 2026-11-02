package na.eraka.RecyclerViews;

import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;

import com.google.firebase.auth.FirebaseAuth;
import na.eraka.Adapters.GroupGalleryAdapter;
import na.eraka.Helper.SimpleItemTouchHelperCallback;
import na.eraka.R;
import na.eraka.utils.constants.Constants;

public class GroupSortRecyclerView extends BaseRecyclerView {
    protected static final String TAG ="Grupo_R_V_sort" ;
    protected GroupGalleryAdapter mGaleriaGruposAdapter;
    protected final int mPosition = -1;
    public GroupSortRecyclerView(AppCompatActivity mActivity, FirebaseAuth mAuth) {
        super(mActivity, mAuth);
        array=json.getmJSONArrayTodosLosGrupos();
        arrayAux=json.getmJSONArrayTodosLosGrupos();
        createRecyclerLayoutManager();

    }
    public void cargarGrupo(){

        mGaleriaGruposAdapter = new GroupGalleryAdapter(mActivity, R.layout.grid_group_layout_2, mAuth);
        DefaultItemAnimator itemAnimator = new DefaultItemAnimator();
        mRecyclerView.setItemAnimator(itemAnimator);
        mRecyclerView.setAdapter(mGaleriaGruposAdapter);
        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mGaleriaGruposAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mRecyclerView);
    }


    @Override
    public void onPictosFiltrados() {

    }

    @Override
    public void onPictosNoFiltrados() {

    }


    public void guardarOrden() {
        json.setmJSONArrayTodosLosGrupos(mGaleriaGruposAdapter.getmArrayGrupos());
        if (!json.guardarJson(Constants.Grupos))
            Log.e(TAG, "Error al guardar los grupos guardarOrden: ");
        uploadFirebaseFile.subirGruposFirebase(uploadFirebaseFile.getmDatabase(mAuth, Constants.Grupos), uploadFirebaseFile.getmStorageRef(mAuth, Constants.Grupos));
    }

    public void cancelOrder() {
        json.setmJSONArrayTodosLosGrupos(arrayAux);
        if (!json.guardarJson(Constants.Grupos))
            Log.e(TAG, "Error al guardar los grupos guardarOrden: ");
        uploadFirebaseFile.subirGruposFirebase(uploadFirebaseFile.getmDatabase(mAuth, Constants.Grupos), uploadFirebaseFile.getmStorageRef(mAuth, Constants.Grupos));
    }


}
