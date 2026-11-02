package na.eraka.Activities;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.security.ProviderInstaller;
import com.google.firebase.perf.metrics.AddTrace;
import na.eraka.FirebaseRequests.FirebaseUtils;
import na.eraka.FirebaseRequests.FirebaseUploader;
import na.eraka.R;
import na.eraka.RecyclerViews.FindAllPictogramsRecyclerView;
import na.eraka.utils.preferences.User;
import na.eraka.utils.SpeechOutput;

public class FindAllPictograms extends AppCompatActivity {
    private RecyclerView recyclerView;
    private FindAllPictogramsRecyclerView findAllPictograms_recycler_view;
    private User mUser;
    private SearchView searchView;
    private SharedPreferences sharedPrefsDefault;
    private SpeechOutput myTTS;
    private FirebaseUploader uploadFile;
    private FirebaseUtils firebaseUtils;

    @AddTrace(name = "FindAllPictograms", enabled = true /* optional */)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.findallpictograms);
        mUser= new User(this);
        initComponents();
    }

    public void initComponents(){
        sharedPrefsDefault= PreferenceManager.getDefaultSharedPreferences(this);
        recyclerView=findViewById(R.id.recyclerView);
        searchView=findViewById(R.id.searchView);
        myTTS = SpeechOutput.getInstance(this);
        firebaseUtils=FirebaseUtils.getInstance();
       // //firebaseUtils.setmContext(this);
        uploadFile=new FirebaseUploader(this);
        setUpFindAllPictograms();
    }

    public void setUpFindAllPictograms(){
        findAllPictograms_recycler_view= new FindAllPictogramsRecyclerView(this,mUser.getmAuth());
        findAllPictograms_recycler_view.setSearchView(searchView);
        findAllPictograms_recycler_view.setSharedPrefsDefault(sharedPrefsDefault);
        findAllPictograms_recycler_view.setMyTTS(myTTS);
        findAllPictograms_recycler_view.setArray();
        findAllPictograms_recycler_view.setUploadFirebaseFile(uploadFile);
        findAllPictograms_recycler_view.showRecyclerView(false);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        findAllPictograms_recycler_view.getProgress_dialog_options().destruirDialogo();
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }
}
