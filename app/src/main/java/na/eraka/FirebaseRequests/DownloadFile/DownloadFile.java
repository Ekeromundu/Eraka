package na.eraka.FirebaseRequests.DownloadFile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageException;
import com.google.firebase.storage.StorageReference;
import na.eraka.FirebaseRequests.FirebaseUtils;
import na.eraka.JSONutils.Json;
import na.eraka.R;
import na.eraka.utils.ObservableInteger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class DownloadFile implements OnFailureListener {
    protected ObservableInteger observableInteger;
    protected File file;
    protected DatabaseReference mDatabase;
    protected SharedPreferences sharedPrefsDefault;
    protected StorageReference mStorageReference;
    protected String TAG="DownloadError";
    protected Context mContext;
    protected Json json;
    protected String locale;
    protected String email="";
    protected String uid ="";
    protected File rootPath;
    public DownloadFile(Context mContext,DatabaseReference mDatabase,StorageReference mStorageReference,SharedPreferences sharedPreferences,ObservableInteger observableInteger,String  locale){
        this.mContext = mContext;
        this.mDatabase = mDatabase;
        this.mStorageReference = mStorageReference;
        this.sharedPrefsDefault = sharedPreferences;
        this.observableInteger = observableInteger;
        this.json = Json.getInstance();
        this.locale = sharedPrefsDefault.getString(mContext.getString(R.string.str_idioma), locale);
        this.uid = FirebaseUtils.getInstance().getUid();
        this.email = FirebaseUtils.getInstance().getEmail();
        rootPath = new File(this.mContext.getCacheDir(), "Archivos_OTTAA");
        if (!rootPath.exists()) {
            rootPath.mkdirs();//si no existe el directorio lo creamos
        }
    }


    protected static String getStringFromFile (String filePath) throws Exception {
        File fl = new File(filePath);
        FileInputStream fin = new FileInputStream(fl);
        String ret = convertStreamToString(fin);
        //Make sure you close all streams.
        fin.close();
        return ret;
    }
    protected static String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }

    @Override
    public void onFailure(@NonNull Exception e) {
        Log.e(TAG, "Failed 1");
        // Eraka: failures now come from ErakaFileStore (app assets / Realtime Database),
        // not only StorageException, so don't cast.
        int errorCode = (e instanceof StorageException) ? ((StorageException) e).getErrorCode() : -1;
        Log.e(TAG, "onFailure: " + errorCode + " :" + e.getMessage());
    }
}
