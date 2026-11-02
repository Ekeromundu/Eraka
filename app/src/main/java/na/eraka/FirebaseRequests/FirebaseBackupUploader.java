package na.eraka.FirebaseRequests;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import na.eraka.FirebaseRequests.UploadFiles.UploadPhrase;
import na.eraka.FirebaseRequests.UploadFiles.UploadingPictos;
import na.eraka.utils.constants.Constants;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import androidx.annotation.NonNull;

public class FirebaseBackupUploader {

    private final DatabaseReference mDatabase;
    private final StorageReference mStorageRef;
    private final FirebaseAuth mAuth;
    private FileInputStream mPictosStream, mGruposStream,mFrasesStream,mFotosStream;
    private final Context mContext;
    private final String TAGFrases = "SubirBackFireb_frases";
    private final String TAGPictos = "SubirBackFireb_pictos";
    private final String TAGGrupos = "SubirBackFireb_grupos";
    private final String TAGFotos = "SubirBackFireb_fotos";

    private final String TAGJuegos = "SubirBackFireb_fotos";



    public FirebaseBackupUploader(Context mContext) {
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.mStorageRef = FirebaseStorage.getInstance().getReference();
        this.mAuth = FirebaseAuth.getInstance();
        this.mContext = mContext;
    }



    public Boolean subirFrasesFirebase(final DatabaseReference mDatabase, StorageReference mStorageRef){
        new UploadPhrase(mContext,mDatabase,mStorageRef);
        return  false;
    }


    public Boolean subirPictosFirebase(final DatabaseReference mDatabase, StorageReference mStorageRef){
        new UploadingPictos(mContext,mDatabase,mStorageRef);
        return  false;
    }



    public Boolean subirGruposFirebase(final DatabaseReference mDatabase, StorageReference mStorageRef) {
        new UploadPhrase(mContext,mDatabase,mStorageRef);
        return false;
    }

    public Boolean subirFotosBackupFirebase(final DatabaseReference mDatabase, StorageReference mStorageRef) {

        final StorageReference referenciaFotos = mStorageRef;

        try {
            mFotosStream = mContext.openFileInput(Constants.ARCHIVO_FOTO_BACKUP);


        // Eraka: through ErakaFileStore (Realtime Database for text backups; photos stay on phone)
        na.eraka.FirebaseRequests.ErakaFileStore.upload(mContext, referenciaFotos.getPath(), mFotosStream).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri downloadUri = task.getResult();
                    String urlFotoUpload = downloadUri.toString();
                    Log.e("DownloadUrlFotoTask",""+urlFotoUpload);
                    mDatabase.setValue(urlFotoUpload, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(DatabaseError databaseError, DatabaseReference referenciaGrupos) {
                            if(referenciaFotos!=null){
                                Log.e(TAGFotos, "Se guardo correctamente url Fotos Backup");
                            }else
                            {
                                Log.e(TAGFotos, "Error al subir url Fotos");
                            }

                        }
                    });

                } else {
                    Log.e(TAGFotos,"Subida Fallida: "+task.getException());
                }
            }
        });
            mFotosStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return false;

        }



}
