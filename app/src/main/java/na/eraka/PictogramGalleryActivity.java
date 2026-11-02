package na.eraka;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.perf.metrics.AddTrace;
import na.eraka.FirebaseRequests.FirebaseUploader;
import na.eraka.Interfaces.TimedClickListener;
import na.eraka.JSONutils.Json;
import na.eraka.RecyclerViews.PictogramSortRecyclerView;
import na.eraka.RecyclerViews.PictogramRecyclerView;
import na.eraka.RecyclerViews.PictogramLinkRecyclerView;
import na.eraka.Viewpagers.PictogramGalleryViewPager;
import na.eraka.utils.Accesibilidad.ScreenScanning;
import na.eraka.utils.Accesibilidad.devices.GaleriaPictosControls;
import na.eraka.utils.Accesibilidad.scrollActions.ScrollFunctionGaleriaPictos;
import na.eraka.utils.constants.Constants;
import na.eraka.utils.Firebase.AnalyticsFirebase;
import na.eraka.utils.IntentCode;
import na.eraka.utils.JSONutils;
import na.eraka.utils.SpeechOutput;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;

public class PictogramGalleryActivity extends AppCompatActivity implements View.OnClickListener, View.OnTouchListener, TimedClickListener {
    private static final String TAG = "GaleriaPictos3";
    public int mState = 1;
    protected SearchView mSearchView;
    //Common Objects
    protected Json json;
    private FirebaseAuth mAuth;
    private FirebaseUploader uploadFile;
    protected boolean esVincular;
    protected boolean showViewPager;
    protected boolean isSorter;
    protected SharedPreferences sharedPrefsDefault;
    private Toolbar toolbar;
    protected MenuItem menuItem;
    protected Menu menu;
    private MenuItem item;
    private FirebaseAnalytics mFirebaseAnalytics;

    //TextToSpeech
    protected SpeechOutput myTTS;
    //RecyclerViewComponents
    protected PictogramLinkRecyclerView mVincularRecyclerView;
    protected PictogramRecyclerView mPictoRecyclerView;
    protected PictogramSortRecyclerView mPictoSortRecyclerView;
    private JSONArray pictosDelGrupoFiltrado;
    private JSONArray pictosDelGrupo;
    //ViewPager
    protected PictogramGalleryViewPager PictogramGalleryViewPager;
    //ShowViewPager
    //Menu

    //Id Grupo Padre
    protected int boton;
    protected String nombre;
    //ImageButton
    private ImageButton previous, foward, exit, edit_button;
    private Button btnBarrido;
    private FloatingActionButton btnTalk;
    private ScreenScanning barridoPantalla;
    protected ScrollFunctionGaleriaPictos function_scroll;
    private AnalyticsFirebase analyticsFirebase;
    private GaleriaPictosControls navigationControl;
    private boolean editarPicto;


    @AddTrace(name = "GaleriaPictos3", enabled = true /* optional */)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Intent intento = getIntent();
        boolean status_bar = intento.getBooleanExtra("status_bar", false);
        if (!status_bar) {
            supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
        analyticsFirebase = new AnalyticsFirebase(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_galeria_grupos2);
        sharedPrefsDefault = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        editarPicto = sharedPrefsDefault.getBoolean(getString(R.string.str_editar_picto), true);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setUpViewPager();
        initButtons();
        initComponents();
        navigationControl = new GaleriaPictosControls(this);
    }

    private void cargarAdapter() {
        isVincular(esVincular).isSorted(isSorter).isDefault();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_galeria_pictos, menu);

        MenuItem item;
        item = menu.findItem(R.id.action_settings);
        item.setVisible(false);
        item = menu.findItem(R.id.tipe_view);
        if (!showViewPager)
            item.setIcon(R.drawable.ic_baseline_view_carousel_white_24);
        else
            item.setIcon(R.drawable.ic_baseline_apps_white_24);
        Log.d(TAG, "onCreateOptionsMenu: esVincular: " + esVincular);
        if (esVincular || isSorter) {
            item.setVisible(false);
            item = menu.findItem(R.id.vincular);
            item.setVisible(false);
            item = menu.findItem(R.id.nuevo);
            item.setVisible(false);
            item = menu.findItem(R.id.order_items);
            item.setVisible(false);

        } else {


            item = menu.findItem(R.id.action_search);
            item.setVisible(true);
        }
        if (esVincular) {
            item = menu.findItem(R.id.action_search);
            item.setVisible(true);
        } else if (isSorter) {
            item = menu.findItem(R.id.action_search);
            item.setVisible(false);

        }
        item = menu.findItem(R.id.bajarFotos);
        item.setVisible(false);
        // Eraka 27 Oct 2026: no sorting or view toggle here. "+" (a brand-new picture) stays,
        // except when this screen is used to choose existing pictures or to sort them.
        menu.findItem(R.id.order_items).setVisible(false);
        menu.findItem(R.id.tipe_view).setVisible(false);
        menu.findItem(R.id.nuevo).setVisible(!(esVincular || isSorter));
        // Eraka 28 Oct 2026: shown on the normal Pictures screen. (It was also tied to the picture
        // list existing, but the list is made after the toolbar, so the bin never appeared.)
        menu.findItem(R.id.select_delete).setVisible(!(esVincular || isSorter));
        // Eraka 28 Oct 2026: no "add existing pictures to this group" button (Keneke)
        menu.findItem(R.id.vincular).setVisible(false);
        if (menu.findItem(R.id.action_search).isVisible()) {
            menuItem = menu.findItem(R.id.action_search);
            mSearchView = (SearchView) menuItem.getActionView();
            mSearchView.setFocusable(true);
            mSearchView.setQueryHint("Search");
            mSearchView.onActionViewCollapsed();
            mSearchView.setSubmitButtonEnabled(true);
            setmSearchView();
        }
        return true;
    }

    public void setmSearchView() {
        if (mVincularRecyclerView != null)
            mVincularRecyclerView.setSearchView(mSearchView);
        if (mPictoRecyclerView != null)
            mPictoRecyclerView.setSearchView(mSearchView);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch (id) {

            case R.id.action_settings:
                return true;
            case R.id.nuevo:
                analyticsFirebase.customEvents("Touch", "Galeria Pictos", "Add Pictogram");
                if(editarPicto){
                   if (true /* Eraka: all features are free */) {

                       Bundle bundle = new Bundle();
                       bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "Nuevo Picto");
                       mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);

                       Intent intent = new Intent(PictogramGalleryActivity.this, EditPictogramActivity.class);
                       intent.putExtra("esNuevo", true);
                       intent.putExtra("Padre", boton);
                       intent.putExtra("esGrupo", false);
                       intent.putExtra("Texto","");
                       myTTS.hablar(getString(R.string.add_pictograma));
                       Log.d(TAG, "onOptionsItemSelected: Creando un nuevo picto");

                       startActivityForResult(intent, IntentCode.EDITARPICTO.getCode());
                       return true;
                   }
                }

                return true;

            case R.id.select_delete:
                // Eraka 28 Oct 2026: remove several pictures from this group at once
                if (mPictoRecyclerView != null) {
                    na.eraka.utils.ErakaMultiDelete.show(this, R.string.multi_title_pictures,
                            mPictoRecyclerView.getPictures(), null,
                            sharedPrefsDefault.getString(getString(R.string.str_idioma), "en"),
                            R.string.multi_confirm_pictures, new na.eraka.utils.ErakaMultiDelete.BackgroundDelete() {
                                @Override public void work(java.util.List<Integer> ids) { mPictoRecyclerView.removeFromGroup(ids); }
                                @Override public void done(java.util.List<Integer> ids) {
                                    mPictoRecyclerView.showPictures();
                                    android.widget.Toast.makeText(PictogramGalleryActivity.this, getString(R.string.multi_done_pictures, ids.size()),
                                            android.widget.Toast.LENGTH_SHORT).show();
                                }
                            });
                }
                return true;
            case R.id.vincular:
                analyticsFirebase.customEvents("Touch", "Galeria Pictos", "Vinculate Pictograms");
                if(editarPicto){
                 if (true /* Eraka: all features are free */) {

                     Bundle bundle = new Bundle();
                     bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "Vincular");
                     mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);

                     Intent intent2 = new Intent(PictogramGalleryActivity.this, PictogramGalleryActivity.class);
                     intent2.putExtra("Boton", boton);
                     intent2.putExtra("esVincular", true);
                     intent2.putExtra("Nombre", nombre);


                     startActivityForResult(intent2, IntentCode.VINCULAR.getCode());
                 }
                }
                return true;
            case R.id.order_items:
                analyticsFirebase.customEvents("Touch", "Galeria Pictos", "Sort Pictograms");
                if (true /* Eraka: all features are free */) {

                    Bundle bundle = new Bundle();
                    bundle.putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "Ordenar");
                    mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT, bundle);

                    Intent intent2 = new Intent(PictogramGalleryActivity.this, PictogramGalleryActivity.class);
                    intent2.putExtra("Boton", boton);
                    intent2.putExtra("esOrdenar", true);
                    intent2.putExtra("Nombre", nombre);


                    startActivityForResult(intent2, IntentCode.ORDENAR.getCode());
                }
                return true;
            case R.id.tipe_view:
                analyticsFirebase.customEvents("Touch", "Galeria Pictos", "Change View");
                showViewPager = !showViewPager;
                if (!showViewPager)
                    item.setIcon(R.drawable.ic_baseline_view_carousel_white_24);
                else
                    item.setIcon(R.drawable.ic_baseline_apps_white_24);
                sharedPrefsDefault.edit().putBoolean("showViewPager_pictos", showViewPager).apply();
                PictogramGalleryViewPager.showViewPager(showViewPager);
                if (mPictoRecyclerView != null)
                    mPictoRecyclerView.showRecyclerView(showViewPager);
                showView(edit_button, showViewPager);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IntentCode.VINCULAR.getCode() || requestCode == IntentCode.ORDENAR.getCode() || requestCode == IntentCode.EDITARPICTO.getCode()) {
            mPictoRecyclerView.sincronizeData();
            mPictoRecyclerView.changeData();
            mPictoRecyclerView.guardarDatosGrupo();
            PictogramGalleryViewPager.setArray(mPictoRecyclerView.getArray());
        }
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.edit_button:
                if (barridoPantalla.isBarridoActivado()) {
                    analyticsFirebase.customEvents("Accessibility", "Galeria Pictos", "Edit Pictogram");
                }
                if (isSorter) {
                    analyticsFirebase.customEvents("Touch", "Ordenar Pictogramas", "Save child sort");
                    mPictoSortRecyclerView.guardarOrden();
                    mPictoSortRecyclerView.subirGrupos();
                    Intent databack = new Intent();
                    setResult(IntentCode.ORDENAR.getCode(), databack);
                    finish();
                } else if (esVincular) {
                    analyticsFirebase.customEvents("Touch", "Vincular Pictogramas", "Vinculate child");
                    guardarVincular();
                } else {
                    if (showViewPager) {
                        PictogramGalleryViewPager.editItem(true /* Eraka: all features are free */);
                    }
                }

                break;
            case R.id.up_button:
                scrollNextButton(false);
                break;
            case R.id.down_button:
                scrollNextButton(true);
                break;
            case R.id.back_button:
                if (barridoPantalla.isBarridoActivado())
                    analyticsFirebase.customEvents("Accessibility", "Galeria Pictos", "Backpress Button");
                else
                    analyticsFirebase.customEvents("Touch", "Galeria Pictos", "Backpress Button");
                onBackPressed();
                break;
            case R.id.btnTalk:
                if (showViewPager) {
                    if (!barridoPantalla.isBarridoActivado())
                        analyticsFirebase.customEvents("Touch", "Galeria Pictos", "Select Pictogram with button talk");
                    else
                        analyticsFirebase.customEvents("Accessibility", "Galeria Pictos", "Select Pictogram with button talk");

                    PictogramGalleryViewPager.OnClickItem();
                }
                break;
            case R.id.btnBarrido:
                if (barridoPantalla.isBarridoActivado() && barridoPantalla.isAvanzarYAceptar()) {
                    Log.d(TAG, "onClick() returned: Barrido Pantalla");

                } else if (barridoPantalla.isBarridoActivado() && !barridoPantalla.isAvanzarYAceptar()) {
                    int position = barridoPantalla.getPosicionBarrido();
                    if (position != -1)
                        barridoPantalla.getmListadoVistas().get(position).callOnClick();
                }
                break;
        }
    }

    protected void initComponents() {
        mAuth = FirebaseAuth.getInstance();
        uploadFile = new FirebaseUploader(getApplicationContext());
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        json = Json.getInstance();
        json.setmContext(this);





        if (esVincular || isSorter) {
            edit_button.setImageDrawable(getResources().getDrawable(R.drawable.ic_baseline_save_white_24));
        }

        function_scroll = new ScrollFunctionGaleriaPictos(this, this);
        iniciarBarrido();
        PictogramGalleryViewPager.showViewPager(showViewPager);
        cargarAdapter();
        showView(edit_button, showViewPager);

    }
    
    protected void initButtons(){
        previous = findViewById(R.id.down_button);
        foward = findViewById(R.id.up_button);
        exit = findViewById(R.id.back_button);
        edit_button = findViewById(R.id.edit_button);
        btnTalk = findViewById(R.id.btnTalk);
        btnBarrido = findViewById(R.id.btnBarrido);
        btnBarrido.setVisibility(View.GONE);
        btnBarrido.setOnClickListener(this);
        btnBarrido.setOnTouchListener(this);
        previous.setOnClickListener(this);
        exit.setOnClickListener(this);
        edit_button.setOnClickListener(this);
        btnTalk.setOnClickListener(this);
        foward.setOnClickListener(this);
    }

    protected void setUpViewPager(){
        Log.e(TAG, "Crashing in galeria" );
        myTTS = SpeechOutput.getInstance(this);
        PictogramGalleryViewPager = new PictogramGalleryViewPager(this, myTTS, boton);
        Bundle extras = getIntent().getExtras();
        showViewPager = false;   // Eraka 27 Oct 2026: always the grid (the view toggle was removed)
        if (extras != null) {
            esVincular = extras.getBoolean("esVincular", false);
            isSorter = extras.getBoolean("esOrdenar", false);
            boton = extras.getInt("Boton", 0);
            nombre = extras.getString("Nombre");
            if (esVincular || isSorter )
                showViewPager = false;
        }
    }

    private void guardarVincular() {
        JSONArray mSelectedPictos = mVincularRecyclerView.getGaleriaPictos2().getmSelectedPictos();
        JSONArray todosLosGrupos = json.getmJSONArrayTodosLosGrupos();
        if (mSelectedPictos != null) {
            for (int i = 0; i < mSelectedPictos.length(); i++) {
                try {
                    int id = mSelectedPictos.getJSONObject(i).getInt("id");
                    JSONutils.relacionarConGrupo2(todosLosGrupos, boton, id);
                    Log.d(TAG, "guardarVincular: " + mSelectedPictos);
                } catch (JSONException e) {
                    Log.e(TAG, "guardarVincular: Error: " + e.getMessage());
                }
            }
        }
        json.setmJSONArrayTodosLosGrupos(todosLosGrupos);
        if (!json.guardarJson(Constants.ARCHIVO_GRUPOS))
            Log.d(TAG, "guardarVincular: ");
        Intent databack = new Intent();
        databack.putExtra("nuevosPictos", mSelectedPictos.length());
        setResult(IntentCode.VINCULAR.getCode(), databack);
        finish();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent databack = new Intent();
        databack.putExtra("ID", 0);
        databack.putExtra("Boton", -1);
        setResult(3, databack);
        finish();
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        //esto soluciona el error que salta en el nokia 8 por que si no elimina la vista
        super.onSaveInstanceState(outState, outPersistentState);
    }

    private void iniciarBarrido() {
        ArrayList<View> listadoObjetosBarrido = new ArrayList<>();
        if (!esVincular) {
            listadoObjetosBarrido.add(previous);
            listadoObjetosBarrido.add(exit);
            listadoObjetosBarrido.add(btnTalk);
            listadoObjetosBarrido.add(foward);
        }
        barridoPantalla = new ScreenScanning(this, listadoObjetosBarrido);
        if (barridoPantalla.isBarridoActivado() && barridoPantalla.devolverpago()) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    // Stuff that updates the UI
                    if (!esVincular) {
                        showViewPager = true;
                        btnBarrido.setVisibility(View.VISIBLE);
                    }
                }
            });
        } else {
            btnBarrido.setVisibility(View.GONE);
        }

    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {
        return navigationControl.makeClick(event);
    }


    @Override
    public void OnClickBarrido() {
        if (function_scroll.isClickEnabled() && barridoPantalla.getmListadoVistas().get(barridoPantalla.getPosicionBarrido()).getId() == R.id.btnTodosLosPictos)
            onClick(barridoPantalla.getmListadoVistas().get(barridoPantalla.getPosicionBarrido()));
        else if (!function_scroll.isClickEnabled()) {
            onClick(barridoPantalla.getmListadoVistas().get(barridoPantalla.getPosicionBarrido()));
        }
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (0 != (event.getSource() & InputDevice.SOURCE_CLASS_POINTER)) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_SCROLL:

                    if (barridoPantalla.isScrollMode() || barridoPantalla.isScrollModeClicker()) {
                        if (event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f) {
                            if (barridoPantalla.isScrollMode())
                                function_scroll.HacerClickEnTiempo();
                            barridoPantalla.avanzarBarrido();
                        } else {
                            if (barridoPantalla.isScrollMode())
                                function_scroll.HacerClickEnTiempo();
                            barridoPantalla.volverAtrasBarrido();

                        }
                    }
                    return true;
            }
        }
        return super.onGenericMotionEvent(event);
    }

    public void showView(View view, boolean showItem) {
        if (isSorter || esVincular)
            showItem = !showItem;
        if (showItem)
            view.setVisibility(View.VISIBLE);
        else
            view.setVisibility(View.INVISIBLE);
    }

    public ScreenScanning getBarridoPantalla() {
        return barridoPantalla;
    }

    public ScrollFunctionGaleriaPictos getFunction_scroll() {
        return function_scroll;
    }

    /// load the adapter at this section
    public PictogramGalleryActivity isVincular(boolean isVincular) {
        if (isVincular) {
            mVincularRecyclerView = new PictogramLinkRecyclerView(this, mAuth);
            mVincularRecyclerView.setSharedPrefsDefault(sharedPrefsDefault);
            mVincularRecyclerView.setArray();
            mVincularRecyclerView.setMyTTS(myTTS);
            mVincularRecyclerView.showRecyclerView(showViewPager);
        }
        return this;
    }

    public PictogramGalleryActivity isSorted(boolean isSorter) {
        if (isSorter) {
            mPictoSortRecyclerView = new PictogramSortRecyclerView(this, mAuth);
            mPictoSortRecyclerView.setSharedPrefsDefault(sharedPrefsDefault);
            mPictoSortRecyclerView.setArray(boton);
            mPictoSortRecyclerView.setMyTTS(myTTS);
            mPictoSortRecyclerView.setUploadFirebaseFile(uploadFile);
            mPictoSortRecyclerView.showRecyclerView(showViewPager);
        }
        return this;
    }

    public PictogramGalleryActivity isDefault() {
        if (!isSorter && !esVincular ) {
            mPictoRecyclerView = new PictogramRecyclerView(this, mAuth);
            mPictoRecyclerView.setSharedPrefsDefault(sharedPrefsDefault);
            mPictoRecyclerView.setArray(boton);
            mPictoRecyclerView.setMyTTS(myTTS);
            mPictoRecyclerView.setUploadFirebaseFile(uploadFile);
            mPictoRecyclerView.showRecyclerView(showViewPager);
        }
        return this;
    }


    // recyclerViewMethods
    public void scrollNextButton(boolean isNextButton) {
        isNextButton(isNextButton).scrollViewPager(isNextButton).scrollSorter(isNextButton).scrollVincular(isNextButton).scrollDefault(isNextButton);
    }

    public PictogramGalleryActivity scrollViewPager(boolean nextButton) {
        if (showViewPager)
            PictogramGalleryViewPager.scrollPosition(nextButton);
        return this;
    }

    public PictogramGalleryActivity scrollSorter(boolean nextButton) {
        if (isSorter) {
            mPictoSortRecyclerView.scrollTo(nextButton);
        }
        return this;
    }

    public PictogramGalleryActivity scrollDefault(boolean nextButton) {
        if (!showViewPager && !isSorter && !esVincular ) {
            mPictoRecyclerView.scrollTo(nextButton);
        }
        return this;
    }

    public PictogramGalleryActivity scrollVincular(boolean nextButton) {
        if (esVincular) {
            mVincularRecyclerView.scrollTo(nextButton);
        }
        return this;
    }


    public PictogramGalleryActivity isNextButton(boolean nextButton) {
        if (barridoPantalla.isBarridoActivado()) {
            analyticsFirebase.customEvents("Touch", "Galeria Pictos", "Next Button");
        } else {
            analyticsFirebase.customEvents("Touch", "Galeria Pictos", "Previous Button");
        }
        return this;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (barridoPantalla.isBarridoActivado()) {

            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                event.startTracking();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                event.startTracking();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                event.startTracking();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                event.startTracking();
                return true;
            }
            if(keyCode == KeyEvent.KEYCODE_BACK){
                if(event.getSource() == InputDevice.SOURCE_MOUSE)
                    barridoPantalla.getmListadoVistas().get(barridoPantalla.getPosicionBarrido()).callOnClick();
                else
                    onBackPressed();
                return true;
            }

        }
        return false;
    }
}
