package na.eraka;

import na.eraka.utils.ErakaChildProfile;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import na.eraka.FirebaseRequests.DownloadJsonFromFirebase;
import na.eraka.FirebaseRequests.FirebaseDatabaseRequest;
import na.eraka.FirebaseRequests.FirebaseUtils;
import na.eraka.Interfaces.CalendarChangeEvent;
import na.eraka.Interfaces.FirebaseSuccessListener;
import na.eraka.Interfaces.LoadUserInformation;
import na.eraka.utils.ConnectionDetector;
import na.eraka.utils.DateTextWatcher;
import na.eraka.utils.ErakaLoader;
import na.eraka.utils.Firebase.AnalyticsFirebase;
import na.eraka.utils.ImmersiveMode;
import na.eraka.utils.ObservableInteger;
import na.eraka.utils.constants.Constants;
import na.eraka.utils.preferences.DataUser;
import na.eraka.utils.preferences.PreferencesUtil;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Pattern;

public class ChildProfileActivity extends AppCompatActivity implements View.OnClickListener, DatePickerDialog.OnDateSetListener, AdapterView.OnItemSelectedListener, FirebaseSuccessListener, LoadUserInformation, CalendarChangeEvent {
    private static final String TAG = "LoginActivity2Step2";
    private static final DateFormat FORMATTER = SimpleDateFormat.getDateInstance();
    //UI elemetns
    ImageView imageViewOrangeBanner;
//    ImageView imageViewThreePeople;
    TextView textViewLoginBig;
    TextView textViewLoginSmall;
    Button buttonNext;
    Button buttonPrevious;
    EditText editTextName;
    EditText editTextBirthday;
    /** Communication level dropdown (the view was OTTAA's gender selector). */
    Spinner genderSelector;
    boolean convert;
    DataUser userData;
    /** Selected communication level ("Nonverbal", "Minimally verbal", "Emerging verbal"). */
    private String commLevel = "";
    /** Interest chips -> interest key (see ErakaChildProfile.INTERESTS). */
    private final java.util.Map<Integer, String> interestChips = new java.util.LinkedHashMap<>();
    /** Skin-tone chips -> tone key (see ErakaSkinTone). */
    private final java.util.Map<Integer, String> skinChips = new java.util.LinkedHashMap<>();
    private PreferencesUtil preferencesUtil;
    private StorageReference mStorageRef;

    //User variables
    private FirebaseAuth mAuth;
    private FirebaseDatabaseRequest databaseRequest;
    private AnalyticsFirebase mAnalyticsFirebase;
    private FirebaseUtils firebaseUtils;
    private DateTextWatcher dateTextWatcher;
    private DownloadJsonFromFirebase downloadJsonFromFirebase;
    private ErakaLoader loader;
    /** true when opened from the side menu ("Child profile") to edit, not during onboarding. */
    private boolean editingFromMenu;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new ImmersiveMode(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity_2);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferencesUtil = new PreferencesUtil(preferences);
        firebaseUtils = FirebaseUtils.getInstance();
        firebaseUtils.initFirebaseAndEmulator(); // ensures mAuth, mDatabase, mStorage are initialized
        mAuth = firebaseUtils.auth();
        mStorageRef = firebaseUtils.storage().getReference();

        //mAuth = firebaseUtils.getAuth();
        mAnalyticsFirebase = new AnalyticsFirebase(this);



        loader = new ErakaLoader(this);
        editingFromMenu = getIntent() != null && getIntent().getBooleanExtra("comingFromMainActivity", false);
        bindUI();
        if (editingFromMenu) prefillFromPhone();

        animateEntrance();

    }

    private void bindUI() {
        imageViewOrangeBanner = findViewById(R.id.brandBanner2);
        textViewLoginBig = findViewById(R.id.textOnboardingBig);
        textViewLoginSmall = findViewById(R.id.textOnboardingSmall);

        buttonNext = findViewById(R.id.nextButton);
        buttonNext.setOnClickListener(this);
        buttonPrevious = findViewById(R.id.backButton);
        buttonPrevious.setOnClickListener(this);
        buttonPrevious.setVisibility(editingFromMenu ? View.VISIBLE : View.INVISIBLE);
        if (editingFromMenu) buttonNext.setText(R.string.places_done);

        editTextName = findViewById(R.id.editTextName);
        editTextName.setInputType(InputType.TYPE_NULL);
        editTextBirthday = findViewById(R.id.editTextBirthday);
        dateTextWatcher = new DateTextWatcher(editTextBirthday,this);
        genderSelector = findViewById(R.id.selectorCommLevel);
        genderSelector.setOnItemSelectedListener(this);
        interestChips.put(R.id.chipFood, "food");
        interestChips.put(R.id.chipAnimals, "animals");
        interestChips.put(R.id.chipFamily, "family");
        interestChips.put(R.id.chipMusic, "music");
        interestChips.put(R.id.chipOutdoors, "outdoors");
        interestChips.put(R.id.chipSchool, "school");
        // Eraka 28 Oct 2026: "What colour should the pictures be?" - optional, five example tiles
        setupToneChoice();
        // Eraka 27 Oct 2026: "More below" while the form goes on below the screen
        na.eraka.utils.ErakaScrollHintHolder.attach(this, R.id.onboardingScrollView);
        databaseRequest = new FirebaseDatabaseRequest(this);
        convert = true;
        databaseRequest.FillUserInformation(this);
    }

    @Override
    protected void onDestroy() {
        if (loader != null) loader.dismiss();
        super.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    private void animateEntrance() {
        TranslateAnimation translateAnimation = new TranslateAnimation(-700, 0, 0, 0);
        translateAnimation.setRepeatMode(Animation.ABSOLUTE);
        translateAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        translateAnimation.setDuration(1000);
        translateAnimation.setFillAfter(true);
        imageViewOrangeBanner.startAnimation(translateAnimation);
        textViewLoginBig.startAnimation(translateAnimation);
        textViewLoginSmall.startAnimation(translateAnimation);

        // translateAnimation2 was only ever used to animate imageViewThreePeople,
        // which no longer exists in this layout — dead code, commented out
        /*
        TranslateAnimation translateAnimation2 = new TranslateAnimation(0, 0, 700, 0);
        translateAnimation2.setRepeatMode(Animation.ABSOLUTE);
        translateAnimation2.setInterpolator(new AccelerateDecelerateInterpolator());
        translateAnimation2.setDuration(1000);
        translateAnimation2.setFillAfter(true);
        */

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.nextButton:
                if (availableUserData() && editingFromMenu) {
                    // Editing the profile from the menu: just save and go back to the board
                    setUpUserData();
                    finish();
                } else if (availableUserData()) {
                    // Show a friendly loader while we save + download; re-enable if it takes too long
                    buttonNext.setEnabled(false);
                    loader.showWithTimeout(R.string.loader_saving_answers, 60000, () -> {
                        buttonNext.setEnabled(true);
                        Toast.makeText(this, R.string.loader_something_went_wrong, Toast.LENGTH_LONG).show();
                    });
                    setUpUserData();
                    DownloadJsonFromFirebase bajarJsonFirebase = new DownloadJsonFromFirebase(preferencesUtil.getPreferences(), mAuth, this);
                    bajarJsonFirebase.setInterfaz(this);
                    // Eraka: suggestion files are packed in the app now, so load them even offline
                    if (true) {

                        final StorageReference mPredictionRef = mStorageRef.child("Archivos_Sugerencias").child("pictos_" + preferencesUtil.getStringValue("prefSexo", "FEMENINO") + "_" + preferencesUtil.getStringValue("prefEdad", "JOVEN") + ".txt");
                        Log.e(TAG, "mPredictionRef " + mPredictionRef);
                        bajarJsonFirebase.descargarPictosDatabase(mPredictionRef);
                    }else{
                        onPictosSugeridosBajados(true);
                    }

                } else {
                    Toast.makeText(this, this.getResources().getText(R.string.prediction_data), Toast.LENGTH_LONG).show();
                }
                break;
            case R.id.backButton:
                if (editingFromMenu) {
                    finish();
                    break;
                }
                /*
                Log.e(TAG, "onClick: BackButton stept 1" );
                mAnalyticsFirebase.customEvents("Touch", "LoginActivityStep2", "Back1");
                Intent intent2 = new Intent(ChildProfileActivity.this, ChildProfileActivity.class);
                startActivity(intent2);
                finishAfterTransition();*/
                break;
            case R.id.buttonCalendarDialog:
                mAnalyticsFirebase.customEvents("Touch", "LoginActivityStep2", "calendarButton");
                calendarDialog();
                break;
        }
    }

    @Override
    public void getUserInformation(DataUser dataUser) {
            userData = dataUser;
            fillUserData();
    }

    @Override
    public void fillUserData() {
        if (mAuth.getCurrentUser() != null) {
            getUserData();
            if(!userData.getFirstAndLastName().isEmpty())
                editTextName.setText(userData.getFirstAndLastName());
            else
                editTextName.setText(mAuth.getCurrentUser().getDisplayName());
            if(userData.getBirthDate()>0)
                setUpDateUser(userData.getBirthDate());
            if (genderSelector.getSelectedItemPosition() == 0 && !userData.getGender().isEmpty()) {
                selectIndicator(userData.getGender());   // level saved by an older version
            }
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    editTextName.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
                }
            }, 2500);

            }

    }

    /** Child profile opened from the menu: show what's saved on the phone straight away (works offline). */
    private void prefillFromPhone() {
        String savedName = preferencesUtil.getStringValue("name", "");
        if (!savedName.isEmpty()) editTextName.setText(savedName);
        editTextName.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME);
        long birth = preferencesUtil.getPreferences().getLong(Constants.FECHACUMPLE, 0);
        if (birth > 0) setUpDateUser(birth);
        // communication level: its own setting; profiles saved before 21 Oct 2026 kept it in
        // OTTAA's "gender" field, so that is read once as a fallback
        String level = ErakaChildProfile.commLevel(preferencesUtil.getPreferences());
        if (level.isEmpty()) level = preferencesUtil.getStringValue(Constants.GENERO, "");
        if (!level.isEmpty()) selectIndicator(level);
        java.util.Set<String> interests = ErakaChildProfile.interests(preferencesUtil.getPreferences());
        for (java.util.Map.Entry<Integer, String> e : interestChips.entrySet()) {
            android.widget.CompoundButton chip = findViewById(e.getKey());
            if (chip != null) chip.setChecked(interests.contains(e.getValue()));
        }
    }

    // ---- Eraka 28 Oct 2026: colour of the people in the pictures
    // Five tiles, each the same ARASAAC picture (a waving child) in one of the emoji skin shades,
    // lightest first. No colour names. Optional: with nothing tapped the pictures keep ARASAAC's
    // colour, or follow the character chosen on the next page.
    private static final String TONE_EXAMPLE = "ic_hola";
    private final java.util.Map<String, View> toneTiles = new java.util.HashMap<>();

    private void setupToneChoice() {
        final String[] tones = na.eraka.utils.ErakaSkinTone.CHOICES;
        final int[] tiles = {R.id.tone1, R.id.tone2, R.id.tone3, R.id.tone4, R.id.tone5};
        final int[] images = {R.id.tone1Img, R.id.tone2Img, R.id.tone3Img, R.id.tone4Img, R.id.tone5Img};
        for (int i = 0; i < tiles.length; i++) {
            View tile = findViewById(tiles[i]);
            if (tile == null) return;            // right-to-left layout has no tiles
            final String tone = tones[i];
            toneTiles.put(tone, tile);
            tile.setOnClickListener(v -> {
                na.eraka.utils.ErakaSkinTone.pickTone(this, tone);
                markTone(tone);
            });
        }
        if (na.eraka.utils.ErakaSkinTone.isChosen(this)) markTone(na.eraka.utils.ErakaSkinTone.tone(this));
        // Recolouring five small pictures takes a few milliseconds each: off the main thread
        final android.content.Context app = getApplicationContext();
        new Thread(() -> {
            final android.graphics.drawable.Drawable[] d = new android.graphics.drawable.Drawable[tones.length];
            for (int i = 0; i < tones.length; i++)
                d[i] = na.eraka.utils.ErakaSkinTone.example(app, TONE_EXAMPLE, tones[i]);
            runOnUiThread(() -> {
                if (isFinishing()) return;
                for (int i = 0; i < images.length; i++) {
                    View v = findViewById(images[i]);
                    if (v instanceof android.widget.ImageView && d[i] != null)
                        ((android.widget.ImageView) v).setImageDrawable(d[i]);
                }
            });
        }, "eraka-tone-examples").start();
    }

    /** Thick mustard border on the chosen tile. */
    private void markTone(String tone) {
        for (java.util.Map.Entry<String, View> e : toneTiles.entrySet())
            e.getValue().setSelected(e.getKey().equals(tone));
    }

    /** Ticks the saved skin tone (Mixed until the parent chooses). */
    private void showSkinTone() {
        String tone = na.eraka.utils.ErakaSkinTone.tone(this);
        for (java.util.Map.Entry<Integer, String> e : skinChips.entrySet()) {
            android.widget.CompoundButton chip = findViewById(e.getKey());
            if (chip != null) chip.setChecked(tone.equals(e.getValue()));
        }
    }

    /** Selects the saved communication level in the dropdown (first entry = "Select level"). */
    private void selectIndicator(String level) {
        for (int i = 1; i < genderSelector.getCount(); i++) {
            Object item = genderSelector.getItemAtPosition(i);
            if (item != null && item.toString().equalsIgnoreCase(level.trim())) {
                genderSelector.setSelection(i);
                commLevel = item.toString();
                return;
            }
        }
        genderSelector.setSelection(0);              // e.g. an old OTTAA gender value
    }

    public void setUpDateUser(long time){
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(time);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH)+1;
        int year = calendar.get(Calendar.YEAR);
        String value =getValue(day)+""+getValue(month)+""+year;
        editTextBirthday.setText(value);
        dateTextWatcher.validateTextWatcher();
    }



    public String  getValue(int n) {
        return (n<=9) ? ("0"+n) : String.valueOf(n);
    }

    public void calendarDialog() {
        final Calendar cldr = Calendar.getInstance();
        int day = cldr.get(Calendar.DAY_OF_MONTH);
        int month = cldr.get(Calendar.MONTH);
        int year = cldr.get(Calendar.YEAR);
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, R.style.MyDatePickerStyle, this, year, month, day);
        datePickerDialog.show();

    }

    /**
     * Saves the profile ON THE PHONE only (21 Oct 2026). OTTAA uploaded the child's name, birth
     * date, age group and the level (in the gender field) to Firebase; Eraka does not use them
     * there, so nothing personal is uploaded any more (data minimisation).
     */
    public void setUpUserData() {
        getUserData(); // make sure userData exists even if Firebase never answered (offline)
        userData.setFirstAndLastName(editTextName.getText().toString());
        preferencesUtil.applyStringValue("name", userData.getFirstAndLastName());
        preferencesUtil.applyLongValue(Constants.FECHACUMPLE, userData.getBirthDate());
        java.util.Set<String> interests = new java.util.HashSet<>();
        for (java.util.Map.Entry<Integer, String> e : interestChips.entrySet()) {
            android.widget.CompoundButton chip = findViewById(e.getKey());
            if (chip != null && chip.isChecked()) interests.add(e.getValue());
        }
        // a level saved by an earlier version is kept (the field is hidden now)
        String level = commLevel.isEmpty() ? ErakaChildProfile.commLevel(preferencesUtil.getPreferences()) : commLevel;
        ErakaChildProfile.save(preferencesUtil.getPreferences(), level, interests);
        setUserAgePrediction();
    }


    @Override
    public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
        convert = false;
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, dayOfMonth);
        editTextBirthday.setText(getValue(dayOfMonth)+""+getValue(month + 1)+""+year);
        dateTextWatcher.validateTextWatcher();
    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        commLevel = position != 0 ? genderSelector.getSelectedItem().toString() : "";
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void setUserAgePrediction() {
        int age = userData.getUserAge();
        Log.d(TAG, "setUserAgePrediction: " + age);
        String[] ageArray = getResources().getStringArray(R.array.list_Edad_valores);
        if (age < 12) {
            preferencesUtil.applyStringValue(Constants.EDAD, ageArray[0]);
        } else if (age >= 12 && age < 20) {
            preferencesUtil.applyStringValue(Constants.EDAD, ageArray[1]);
        } else {
            preferencesUtil.applyStringValue(Constants.EDAD, ageArray[2]);
            Log.d(TAG, "setUserAgePrediction: " + ageArray[2]);
        }
    }

    public boolean availableUserData() {
        return !editTextName.getText().toString().isEmpty() && validateGenderSelection() && validateJavaDate(editTextBirthday.getText().toString());
    }

    private boolean validateGenderSelection() {
        // Eraka 27 Oct 2026: the communication level is no longer asked at sign-up (it did not
        // change the app; the researcher asks it in the parent questionnaire), so it is not required.
        return true;
    }

    /**
     * https://howtodoinjava.com/java/regex/java-regex-date-format-validation/
     */
    private boolean validateJavaDate(String strDate) {
        /* Check if date is 'null' */
        String regex = "^(1[0-9]|0[1-9]|3[0-1]|2[0-9])/(0[1-9]|1[0-2])/[0-9]{4}$";
        String aux = strDate.replaceAll("٠","0").replaceAll("١","1").replaceAll("٢","2").
                replaceAll("٣","3").replaceAll("٤","4").replaceAll("٥","5").replaceAll("٦","6")
                .replaceAll("٧","7").replaceAll("٨","8").replaceAll("٩","9");
        Pattern pattern = Pattern.compile(regex);

        if (aux.isEmpty())
            return false;
        else
            return pattern.matcher(aux).matches();
    }

    @Override
    public void onDescargaCompleta(int descargaCompleta) {

    }

    @Override
    public void onDatosEncontrados(int datosEncontrados) {

    }

    @Override
    public void onFotoDescargada(int fotosDescargadas) {

    }

    @Override
    public void onArchivosSubidos(boolean subidos) {

    }

    @Override
    public void onPictosSugeridosBajados(boolean descargado) {
        if (loader != null) loader.setMessage(R.string.loader_downloading_words);
        downloadFiles();
    }

    public void toogleKeyBoard(TextView view) {

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm.isActive()) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } else {
            imm.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT);
        }
    }


    @Override
    public void setCalendarDate(long value) {
        if(userData != null)
            userData.setBirthDate(value);
    }

    public DataUser getUserData() {
        if(userData == null)
            userData = new DataUser();
        return userData;
    }
    public void downloadFiles() {
        String locale = Locale.getDefault().getLanguage();
        File rootPath = new File(getApplicationContext().getCacheDir(), "Archivos_OTTAA");
        if (!rootPath.exists()) {
            rootPath.mkdirs();//si no existe el directorio lo creamos
        }
        if(downloadJsonFromFirebase == null)
            downloadJsonFromFirebase = new DownloadJsonFromFirebase(preferencesUtil.getPreferences(),mAuth,this);
        ObservableInteger observableInteger = new ObservableInteger();
        observableInteger.setOnIntegerChangeListener(new ObservableInteger.OnIntegerChangeListener() {
            @Override
            public void onIntegerChanged(int newValue) {
                if(newValue == 1) {
                    Intent intent = new Intent(ChildProfileActivity.this, AvatarChoiceActivity.class);
                    startActivity(intent);
                    finishAfterTransition();
                    mAnalyticsFirebase.customEvents("Touch", "LoginActivityStep2", "Next1 ");
                }
            }
        });
        downloadJsonFromFirebase.bajarPictos(locale, observableInteger);


    }

}



