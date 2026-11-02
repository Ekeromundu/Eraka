package na.eraka.FirebaseRequests.DownloadFile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.tasks.OnCanceledListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DatabaseReference;
import na.eraka.FirebaseRequests.ErakaFileStore;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.StorageReference;
import na.eraka.Interfaces.FirebaseSuccessListener;
import na.eraka.utils.ObservableInteger;
import na.eraka.utils.constants.Constants;

import java.io.File;

public class DownloadPredictionsPictograms extends DownloadFile{
    public DownloadPredictionsPictograms(Context mContext, DatabaseReference mDatabase, StorageReference mStorageReference, SharedPreferences sharedPreferences, ObservableInteger observableInteger,String locate) {
        super(mContext, mDatabase, mStorageReference, sharedPreferences, observableInteger,locate);
        TAG ="DownloadPredictionsPictograms";

    }

    public void downloadPictograms(FirebaseSuccessListener successListener){
        File pictosDatabaseFile = new File(rootPath, Constants.ARCHIVO_PICTOS_DATABASE);
        ErakaFileStore.download(mContext, mStorageReference.getPath(), pictosDatabaseFile).addOnSuccessListener(new OnSuccessListener<Long>() {
            @Override
            public void onSuccess(Long taskSnapshot) {

                Log.e("md5Hash", "onSuccess: Se bajo pictosDatabase ");

                try {
                    if (!getStringFromFile(pictosDatabaseFile.getAbsolutePath()).equals("[]") && pictosDatabaseFile.length() > 0) {
                        Log.e("prueba", getStringFromFile(pictosDatabaseFile.getAbsolutePath()));
                        json.setmJSONArrayPictosSugeridos(json.readJSONArrayFromFile(pictosDatabaseFile.getAbsolutePath()));
                        Log.e("pruebSsa", "Code reaches this part");
                        if (!json.guardarJson(Constants.ARCHIVO_PICTOS_DATABASE))
                            Log.e(TAG, "Error al guardar Json");
                        successListener.onPictosSugeridosBajados(true);
                    }

                } catch (Exception e1) {
                    e1.printStackTrace();
                    successListener.onPictosSugeridosBajados(true);
                }

            }

        }).addOnFailureListener(e -> {
            // Eraka: e.g. a suggestion file not bundled for this profile - never block the flow
            Log.e(TAG, "suggestion file not available: " + e.getMessage());
            successListener.onPictosSugeridosBajados(true);
        }).addOnCanceledListener(new OnCanceledListener() {
            @Override
            public void onCanceled() {
                successListener.onPictosSugeridosBajados(true);
            }
        });
    }
}
