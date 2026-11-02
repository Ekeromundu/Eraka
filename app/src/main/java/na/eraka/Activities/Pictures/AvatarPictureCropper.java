package na.eraka.Activities.Pictures;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.canhub.cropper.CropImage;
import com.canhub.cropper.CropImageView;
import na.eraka.R;

import java.io.File;
import java.io.IOException;

/** Portrait, Eraka-styled crop screen for the child's avatar photo. */
public class AvatarPictureCropper extends PictureCropper {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TAG = "AvatarPictureCropper";
        initComponents();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_avatar_cropper;
    }

    @Override
    protected void initComponents() {
        cropImageView.setCropShape(CropImageView.CropShape.OVAL);
        cropImageView.setAspectRatio(1,1);
        cropImageView.setGuidelines(CropImageView.Guidelines.OFF);
        cropImageView.setOnCropImageCompleteListener(new CropImageView.OnCropImageCompleteListener() {
            @Override
            public void onCropImageComplete(CropImageView view, CropImageView.CropResult result) {
                Log.d(TAG, "onCropImageComplete: Creating the intent with uri: "+croppedImageUri.toString());
                Intent resultIntent = new Intent();
                resultIntent.putExtra("imageUri",croppedImageUri);
                setResult(CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE,resultIntent);
                finish();
            }
        });

        try {
            File tempFile = File.createTempFile("avatar","jpg");
            croppedImageUri = Uri.fromFile(tempFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.buttonRotate) {
            cropImageView.rotateImage(90);
        } else if (id == R.id.back_button) {
            cancelCrop();
        } else {
            super.onClick(view); // btnTalk = crop and return the photo
        }
    }

    /** The phone's back button also cancels (the base class ignores it). */
    @Override
    public void onBackPressed() {
        cancelCrop();
    }

    /** Plain "cancelled" result, so the avatar page returns quietly without an error message. */
    private void cancelCrop() {
        setResult(RESULT_CANCELED);
        finish();
    }
}
