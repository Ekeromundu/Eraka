package na.eraka.utils.Pictures;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import na.eraka.Bitmap.UriFiles;
import na.eraka.Bitmap.PhotoDownloader;
import na.eraka.FirebaseRequests.FirebaseUtils;
import na.eraka.GroupGalleryActivity;
import na.eraka.Interfaces.FirebaseSuccessListener;
import na.eraka.utils.constants.Constants;
import na.eraka.utils.preferences.User;

import java.io.File;

import static na.eraka.GroupGalleryActivity.downloadDialog;

public class DownloadFirebasePictures {
    private String name;
    private String StoragePath;
    private String texto;
    private String id;
    private final FirebaseUtils firebaseUtils;
    private final AppCompatActivity mActivity;
    private final User firebaseUser;
    private PhotoDownloader bajar;
    private File mDirectorio;
    private final GroupGalleryActivity.ShowDismissDialog showDismissDialog;
    private final FirebaseSuccessListener mFbSuccessListenerInterfaz;


    public DownloadFirebasePictures(AppCompatActivity mActivity, GroupGalleryActivity.ShowDismissDialog showDismissDialog, FirebaseSuccessListener mFbSuccessListenerInterfaz) {
        this.mActivity = mActivity;
        firebaseUtils = FirebaseUtils.getInstance();
        //firebaseUtils.setmContext(this.mActivity);
        firebaseUser = new User(this.mActivity);
        this.showDismissDialog = showDismissDialog;
        this.mFbSuccessListenerInterfaz = mFbSuccessListenerInterfaz;
        requestPermission();
    }


    public void downloadFile() {
        showDismissDialog.sendMessage(1);
        String uid = firebaseUser.getUserUid();
        if(uid !=null ||uid.isEmpty()){
            firebaseUtils.getmDatabase().child(Constants.FOTOSUSUARIO).child(firebaseUser.getUserUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    int size = (int) dataSnapshot.getChildrenCount();
                    if (downloadDialog != null) {
                        downloadDialog.setMax((int) dataSnapshot.getChildrenCount());
                    }
                    showDismissDialog.sendMessage(0);
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String child = snapshot.getValue().toString();
                        bajar.setFileDirectory(mDirectorio);
                        bajar.bajarFoto(child, !dataSnapshot.getChildren().iterator().hasNext(),firebaseUtils);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    showDismissDialog.sendMessage(0);
                }
            });
        }
    }

    public void requestPermission() {
        bajar = new PhotoDownloader();
        bajar.setInterfaz(mFbSuccessListenerInterfaz);
        mDirectorio = new UriFiles(mActivity.getApplicationContext()).dir();
        String[] text = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        requestActivityPermission();
    }

    public void setUpDirectory() {
        if (mDirectorio.exists()) {
            bajar.setFileDirectory(mDirectorio);
            downloadFile();
        }
    }


    public File getmDirectorio() {
        return mDirectorio;
    }

    public PhotoDownloader getBajar() {
        return bajar;
    }

    public void requestActivityPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat
                .checkSelfPermission(mActivity.getApplicationContext(), android.Manifest
                        .permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission
                (mActivity.getApplicationContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mActivity.requestPermissions(new String[]{
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                }, Constants.EXTERNAL_STORAGE);

            }
        }
    }

}
