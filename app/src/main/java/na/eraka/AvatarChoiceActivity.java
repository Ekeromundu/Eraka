package na.eraka;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.Target;
import com.canhub.cropper.CropImage;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import na.eraka.Activities.Pictures.AvatarPictureCropper;
import na.eraka.FirebaseRequests.FirebaseUtils;
import na.eraka.utils.ErakaLoader;
import na.eraka.utils.AvatarPackage.AvatarPhotoStore;
import na.eraka.utils.ActivityStatusUtils;
import na.eraka.utils.AvatarPackage.SelectedAvatar;
import na.eraka.utils.ConnectionDetector;
import na.eraka.utils.Firebase.AnalyticsFirebase;
import na.eraka.utils.ImmersiveMode;
import na.eraka.utils.IntentCode;
import na.eraka.utils.constants.Constants;
import com.stonefacesoft.pictogramslibrary.utils.ValidateContext;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class AvatarChoiceActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "LoginActivityAvatar";
    private SharedPreferences preferences;
    //UI elemetns
    ImageView imageViewOrangeBanner;
    ImageView imageViewThreePeople;
    ImageView imageViewAvatar;
    private final ValueEventListener firebaseEventListener = new ValueEventListener() {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {

            if (snapshot.hasChild("url_foto")) {
                Log.d(TAG, "onDataChange:" + snapshot.child("url_foto"));
                Glide.with(getApplicationContext()).load(Uri.parse(snapshot.child("url_foto").getValue().toString())).into(imageViewAvatar);
            } else if (preferences != null && !preferences.getString("userAvatar", "").isEmpty()) {
                setAvatarByName(preferences.getString("userAvatar", ""));   // Eraka: this phone's choice first
            } else if (snapshot.exists()) {
                String name = snapshot.getValue().toString();
                Log.d(TAG, "onDataChange:" + name);
                name = name.replace("avatar", "ic_avatar");
                setAvatarByName(name);
            } else {
                setAvatarByName(savedOrDefaultAvatar());
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    };
    TextView textViewLoginBig;
    TextView textViewLoginSmall;
    Button buttonNext;
    Button buttonPrevious;
    ImageButton imageButtonAvatar11;
    ImageButton imageButtonSelectAvatarSource;
    ConstraintLayout constraintSourceButtons;
    //User variables
    private FirebaseAuth mAuth;
    private Bitmap bitmap;
    private int avatarId;
    private String avatarName;
    private boolean uploadAvatar;
    private DatabaseReference childDatabase;
    private AnalyticsFirebase mFirebaseAnalytics;
    private boolean comingFromMainActivity;
    private ErakaLoader loader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        new ImmersiveMode(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity_avatar);
        Intent intento = getIntent();
        if (intento != null) {
            if (intento.hasExtra("comingFromMainActivity"))
                comingFromMainActivity = true;
        }
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        mAuth = FirebaseAuth.getInstance();

        mFirebaseAnalytics = new AnalyticsFirebase(this);
        loader = new ErakaLoader(this);
        bindUI();
        animateEntrance();
    }

    private void bindUI() {
        imageViewOrangeBanner = findViewById(R.id.orangeBanner2);
        imageViewThreePeople = findViewById(R.id.imagen3personas);
        imageViewAvatar = findViewById(R.id.imgAvatar);

        textViewLoginBig = findViewById(R.id.textLoginBig);
        textViewLoginSmall = findViewById(R.id.textLoginSmall);

        buttonNext = findViewById(R.id.nextButton);
        buttonNext.setOnClickListener(this);
        buttonPrevious = findViewById(R.id.backButton);
        buttonPrevious.setOnClickListener(this);
        if (comingFromMainActivity){
            // Eraka 27 Oct 2026: opened from the menu -> a Cancel button that leaves everything
            // as it was (avatar, own photo and the pictures' skin colour)
            buttonPrevious.setText(R.string.avatar_cancel);
            rememberStartState();
        }
        imageButtonAvatar11 = findViewById(R.id.avatar11);
        // Eraka 27 Oct 2026: no colour names (Light / Medium / Dark / Mixed) any more; the grid
        // shows children of every skin colour and the chosen avatar sets the pictures' colour
        showAvatars();
        na.eraka.utils.ErakaScrollHintHolder.attach(this, R.id.avatarScrollView);   // Eraka: "More below"
        imageButtonSelectAvatarSource = findViewById(R.id.buttonSelectAvatarSource);

        constraintSourceButtons = findViewById(R.id.constraintSourceButtons);
        getFirebaseAvatar();


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

        TranslateAnimation translateAnimation2 = new TranslateAnimation(0, 0, 700, 0);
        translateAnimation2.setRepeatMode(Animation.ABSOLUTE);
        translateAnimation2.setInterpolator(new AccelerateDecelerateInterpolator());
        translateAnimation2.setDuration(1000);
        translateAnimation2.setFillAfter(true);
        imageViewThreePeople.startAnimation(translateAnimation2);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.nextButton:
                buttonNext.setEnabled(false);
                if (uploadAvatar) {
                    if (avatarId == -1) {
                        uploadFirebaseAvatar();
                    } else {
                        uploadFirebaseAvatarName();
                    }
                }
                Intent databack = new Intent();
                if (comingFromMainActivity) {
                    setResult(IntentCode.AVATAR.getCode(), databack);
                    finishAfterTransition();
                }
                if (!comingFromMainActivity) {
                    mFirebaseAnalytics.customEvents("Touch", "LoginActivity2Avatar", "Next3");
                    // MainBoardActivity takes a while to build the board - tell the user what's happening
                    loader.show(R.string.loader_preparing_board);
                    // Small delay so the loader is actually drawn before MainBoardActivity starts its heavy work
                    buttonNext.postDelayed(() -> {
                        Intent intent = new Intent(AvatarChoiceActivity.this, MainBoardActivity.class);
                        startActivity(intent);
                        finishAfterTransition();
                    }, 200);
                }

                break;
            case R.id.backButton:
                if (comingFromMainActivity) {
                    cancelChanges();
                } else {
                    mFirebaseAnalytics.customEvents("Touch", "LoginActivity2Avatar", "Back3");
                    goBackToForm();
                }
                break;
            case R.id.buttonSelectAvatarSource:
                mFirebaseAnalytics.customEvents("Touch", "LoginActivity2Avatar", "buttonSelectAvatarSource");
                openGallery();
                break;
            case R.id.buttonSourceCamera:
                mFirebaseAnalytics.customEvents("Touch", "LoginActivity2Avatar", "ButtonSourceCamera");
                takePictureSetup();
                break;
            case R.id.buttonSourceGallery:
                mFirebaseAnalytics.customEvents("Touch", "LoginActivity2Avatar", "ButtonSourceGallery");
                openGallery();
                break;
            default:
                if (view instanceof ImageView) {
                    uploadAvatar = true;
                    avatarId = id;
                    AvatarPhotoStore.clear(this); // a character replaces the child's own photo
                    avatarName = view.getTag().toString();
                    preferences.edit().putString("userAvatar","ic_"+avatarName).apply();
                    SelectedAvatar.getInstance().setName("ic_"+avatarName);
                    Log.d(TAG, "onClick: " + avatarName);
                    mFirebaseAnalytics.customEvents("Touch", "LoginActivity2Avatar", "Avatar: " + avatarName);
                    Glide.with(this).load(((ImageView) view).getDrawable()).into(imageViewAvatar);
                    // Eraka 27 Oct 2026: the pictures follow the chosen avatar's skin colour
                    String avatarTone = na.eraka.utils.ErakaSkinTone.toneOfAvatar("ic_" + avatarName);
                    // 28 Oct 2026: unless the parent already tapped a colour on the profile page
                    na.eraka.utils.ErakaSkinTone.setToneFromAvatar(this, avatarTone);

                }
        }
    }

    /**
     * Eraka 27 Oct 2026: each of the 15 places shows one child (ErakaSkinTone.displayAvatar):
     * five light, five medium and five dark skinned, mixed over the grid and never labelled.
     * The tag becomes e.g. "avatar14_light", so the click handler stores "ic_avatar14_light"
     * and toneOfAvatar() gives the pictures the same skin colour.
     */
    private void showAvatars() {
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 5; col++) {
                String slot = "avatar" + row + col;
                int viewId = getResources().getIdentifier(slot, "id", getPackageName());
                View v = viewId == 0 ? null : findViewById(viewId);
                if (!(v instanceof ImageView)) continue;
                String name = na.eraka.utils.ErakaSkinTone.displayAvatar(slot);
                int res = getResources().getIdentifier(name, "drawable", getPackageName());
                if (res == 0) continue;
                ((ImageView) v).setImageResource(res);
                v.setTag(name.substring(3));
            }
        }
    }

    // ---- Eraka 27 Oct 2026: Cancel when the page was opened from the menu
    private String startAvatar, startTone;
    private java.io.File startPhoto;

    private void rememberStartState() {
        startAvatar = preferences.getString("userAvatar", "");
        startTone = na.eraka.utils.ErakaSkinTone.tone(this);
        startPhoto = AvatarPhotoStore.backup(this);
    }

    /** Puts back the avatar, own photo and skin colour from when the page opened, then closes it. */
    private void cancelChanges() {
        uploadAvatar = false;
        if (startAvatar != null) {
            if (startAvatar.isEmpty()) preferences.edit().remove("userAvatar").apply();
            else preferences.edit().putString("userAvatar", startAvatar).apply();
            SelectedAvatar.getInstance().setName(startAvatar.isEmpty() ? "ic_avatar_default" : startAvatar);
            AvatarPhotoStore.restore(this, startPhoto);
            if (startTone != null) na.eraka.utils.ErakaSkinTone.setTone(this, startTone);
        }
        setResult(RESULT_CANCELED);
        finishAfterTransition();
    }

    /** Back from the avatar step returns to the child form (ChildProfileActivity), not the tutorial. */
    private void goBackToForm() {
        Intent intent = new Intent(AvatarChoiceActivity.this, ChildProfileActivity.class);
        startActivity(intent);
        finishAfterTransition();
    }

    @Override
    public void onBackPressed() {
        // The system back gesture/button follows the same route as the on-screen Back button
        if (!comingFromMainActivity) {
            goBackToForm();
        } else {
            cancelChanges();   // Eraka: back from the menu = Cancel
        }
    }

    @Override
    protected void onDestroy() {
        if (loader != null) loader.dismiss();
        super.onDestroy();
    }

    /** Opens the phone's photo picker; falls back to the generic file chooser on phones without one. */
    private void openGallery() {
        Intent pick = new Intent(Intent.ACTION_PICK);
        pick.setType("image/*");
        try {
            startActivityForResult(pick, IntentCode.PICK_IMAGE.getCode());
        } catch (android.content.ActivityNotFoundException e) {
            Intent getContent = new Intent(Intent.ACTION_GET_CONTENT);
            getContent.setType("image/*");
            startActivityForResult(Intent.createChooser(getContent, getString(R.string.avatar_upload_photo)),
                    IntentCode.PICK_IMAGE.getCode());
        }
    }

    private void doScaleAnimation() {
        ScaleAnimation scaleAnimation = new ScaleAnimation(0f, 1f, 1f, 1f);
        scaleAnimation.setRepeatMode(Animation.ABSOLUTE);
        scaleAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnimation.setDuration(500);
        scaleAnimation.setFillAfter(true);
        constraintSourceButtons.setVisibility(View.VISIBLE);
        constraintSourceButtons.startAnimation(scaleAnimation);
    }

    //source https://stackoverflow.com/questions/8017374/how-to-pass-a-uri-to-an-intent

    /**
     * this method receiving a result from this activity or another
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            Log.d(TAG, "onActivityResult: Cropped Image: ");
            // User backed out of the cropper, or cropping failed: data/uri can be null -> don't crash
            Uri uri = (data != null && resultCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
                    ? data.getParcelableExtra("imageUri") : null;
            if (uri != null && AvatarPhotoStore.saveFromUri(this, uri)) {
                avatarId = -1;
                uploadAvatar = true;
                Log.d(TAG, "onActivityResult: saved own avatar photo from " + uri);
                AvatarPhotoStore.loadInto(this, imageViewAvatar);
            } else if (resultCode != RESULT_CANCELED) {
                Toast.makeText(this, R.string.avatar_photo_failed, Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
            Log.d(TAG, "onActivityResult: Canceled by user");

        }
        //Source https://stackoverflow.com/questions/38352148/get-image-from-the-gallery-and-show-in-imageview
        if (requestCode == IntentCode.PICK_IMAGE.getCode()) {
            Log.d(TAG, "onActivityResult: Pick image done");
            if (resultCode == RESULT_OK) {
                final Uri pickedImageUri = data.getData();
                Intent intent = new Intent(AvatarChoiceActivity.this, AvatarPictureCropper.class);
                intent.putExtra("pickedImageUri", pickedImageUri);
                startActivityForResult(intent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE);
            } else {
                Toast.makeText(this, "You haven't picked Image", Toast.LENGTH_LONG).show();
            }
        }

        if (requestCode == IntentCode.CAMARA.getCode()) {
            Log.d(TAG, "onActivityResult: Camera image done");
            if (resultCode == RESULT_OK) {
                Bundle extras = data != null ? data.getExtras() : null;
                Bitmap imageBitmap = extras != null ? (Bitmap) extras.get("data") : null;
                if (imageBitmap == null) {
                    Toast.makeText(this, R.string.avatar_photo_failed, Toast.LENGTH_LONG).show();
                    return;
                }
                File tempFile = null;
                try {
                    tempFile = File.createTempFile("cam", "jpg");
                    FileOutputStream fosTemp = new FileOutputStream(tempFile);
                    imageBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fosTemp);
                } catch (IOException e) {
                    Log.d(TAG, "onActivityResult: ERROR saving bitmap");
                }
                Intent intent = new Intent(AvatarChoiceActivity.this, AvatarPictureCropper.class);
                intent.putExtra("pickedImageUri", Uri.fromFile(tempFile));
                startActivityForResult(intent, CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE);
            } else {
                Toast.makeText(this, "You didn't take a picture", Toast.LENGTH_LONG).show();
            }
        }

    }

    /**
     * this method call the camera function
     */
    private void takePictureSetup() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, IntentCode.CAMARA.getCode());
        }
    }

    /**
     * this method upload  to  firebase the selected avatar picture
     */
    private void uploadFirebaseAvatar() {
        FirebaseUtils firebaseUtils = FirebaseUtils.getInstance();
        //firebaseUtils.setmContext(this);
        final String name = System.currentTimeMillis() + ".png";
        StorageReference reference = FirebaseStorage.getInstance().getReference().child("Archivos_Usuarios").child(Constants.AVATAR).child(mAuth.getCurrentUser().getUid()).child(name);
        if (!AvatarPhotoStore.hasPhoto(this)) return;
        // Upload the photo saved on the device (works even if the ImageView is still loading)
        reference.putFile(Uri.fromFile(AvatarPhotoStore.file(this))).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @NonNull
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful())
                    throw task.getException();
                return reference.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (!task.isSuccessful() || task.getResult() == null) {
                    // e.g. no internet - the photo is still saved on the device and shown offline
                    Log.w(TAG, "uploadFirebaseAvatar: upload failed, keeping local photo only");
                    return;
                }
                Uri downloadUri = task.getResult();
                Map<String, Object> avatar = new HashMap<>();
                String url = downloadUri.toString();
                avatar.put("name", name);
                avatar.put("url_foto", url);
                firebaseUtils.getmDatabase().child(Constants.AVATAR).child(mAuth.getCurrentUser().getUid()).updateChildren(avatar);
            }
        });

    }

    /**
     * this method upload  to  firebase the selected avatar name
     */
    private void uploadFirebaseAvatarName() {
        FirebaseUtils utils = FirebaseUtils.getInstance();
        //utils.setmContext(this);

        FirebaseUtils.getInstance().getmDatabase().child(Constants.AVATAR).child(mAuth.getCurrentUser().getUid()).setValue(avatarName);
    }

    /**
     * This method return the user avatar
     */
    public void getFirebaseAvatar() {
        if (AvatarPhotoStore.hasPhoto(this)) {
            AvatarPhotoStore.loadInto(this, imageViewAvatar);
            return;
        }
        ConnectionDetector connectionDetector = new ConnectionDetector(this);
        if (connectionDetector.isConnectedToInternet()) {
            String uid = "";
            if(ValidateContext.isValidContext(this)){
                try{
                    uid = mAuth.getCurrentUser().getUid();
                }catch (Exception ex){
                    uid ="";
                }
            }
            if(!uid.isEmpty()){
                childDatabase = FirebaseUtils.getInstance().getmDatabase().child(Constants.AVATAR).child(uid);
                childDatabase.addListenerForSingleValueEvent(firebaseEventListener);
            }
        } else {
            setAvatarByName(savedOrDefaultAvatar());
        }

    }

    /** Eraka 27 Oct 2026: the avatar chosen on this phone, else the neutral default (no face). */
    private String savedOrDefaultAvatar() {
        String saved = preferences != null ? preferences.getString("userAvatar", "") : "";
        return saved.isEmpty() ? "ic_avatar_default" : saved;
    }

    /**
     * this method shows the avatar picture on the ImageViewAvatar
     */
    public void setAvatarByName(String name) {
        if(ValidateContext.isValidContext(AvatarChoiceActivity.this)) {
            int resId = AvatarChoiceActivity.this.getResources().getIdentifier(name, "drawable", AvatarChoiceActivity.this.getPackageName());
            if (resId == 0) resId = R.drawable.ic_avatar_default;   // Eraka: unknown name -> neutral default
            Drawable drawable = AvatarChoiceActivity.this.getResources().getDrawable(resId);
            Glide.with(AvatarChoiceActivity.this).load(drawable).into(imageViewAvatar);
        }
    }


}