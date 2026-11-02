package na.eraka.RecyclerViews;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.firebase.auth.FirebaseAuth;
import na.eraka.Adapters.MostUsedFavoritePhrasesAdapter;
import na.eraka.Interfaces.ProgressBarListener;

public class MostUsedPhrasesRecyclerView extends BaseRecyclerView {
    private MostUsedFavoritePhrasesAdapter mostUsedFavoritePhrasesAdapter;

    public MostUsedPhrasesRecyclerView(AppCompatActivity mActivity, FirebaseAuth mAuth) {
        super(mActivity, mAuth);
        setArray();
    }

    public void setArray() {
        array = json.getmJSonArrayFrasesFavoritas();
        arrayAux = json.getmJSonArrayFrasesFavoritas();
        createRecyclerLayoutManager();
        mostUsedFavoritePhrasesAdapter = new MostUsedFavoritePhrasesAdapter(mActivity, new ProgressBarListener() {
            @Override
            public void initProgressDialog() {

            }

            @Override
            public void setMessageProgressDialog(String messageProgressDialog) {

            }

            @Override
            public void setTittleProgressDialog(String tittleProgressDialog) {

            }

            @Override
            public void dismisProgressBar() {
                mRecyclerView.setAdapter(mostUsedFavoritePhrasesAdapter);
            }
        });

    }

    @Override
    public void onPictosFiltrados() {


    }

    @Override
    public void onPictosNoFiltrados() {

    }

    @Override
    public void sincronizeData() {
        getPositionItem.updateSize(mostUsedFavoritePhrasesAdapter.getItemCount());
    }

    @Override
    public void scrollTo(boolean add) {
        if (createReturnPositionItem()) {
            if (add) {
                getPositionItem.add();
            } else {
                getPositionItem.subtract();
            }
            if (mRecyclerView.getAdapter().getItemCount() > 0)
                mRecyclerView.scrollToPosition(getPositionItem.getPosition());
        }
    }


    @Override
    protected void createRecyclerLayoutManager() {
        ScrollManager manager = new ScrollManager(mActivity, 1, LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(manager);
    }

    @Override
    public void talkAtPosition() {
        if (createReturnPositionItem()) {
            int value = getPositionItem.getPosition();
            if (validatePosition(value))
                mostUsedFavoritePhrasesAdapter.getMyTTs().hablar(mostUsedFavoritePhrasesAdapter.getmFavImagesArrayList().get(value).getTexto());
        }
    }

}
