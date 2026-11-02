package na.eraka.FirebaseRequests.DownloadFile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import na.eraka.FirebaseRequests.ErakaFileStore;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import na.eraka.R;
import na.eraka.idioma.AppLanguage;
import na.eraka.utils.Firebase.CrashlyticsUtils;
import na.eraka.utils.ObservableInteger;
import na.eraka.utils.constants.Constants;

import java.io.File;

public class DownloadGroups extends DownloadFile{
    public DownloadGroups(Context mContext, DatabaseReference mDatabase, StorageReference mStorageReference, SharedPreferences sharedPreferences, ObservableInteger observableInteger,String locale) {
        super(mContext, mDatabase, mStorageReference, sharedPreferences, observableInteger,locale);
        TAG ="DownloadGroups";

    }

    public void syncGroups(){
        final File gruposUsuarioFile = new File(rootPath, Constants.ARCHIVO_GRUPOS);

        mDatabase.child(Constants.Grupos).child(uid).child("URL_grupos_" + sharedPrefsDefault.getString(mContext.getString(R.string.str_idioma), locale)).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                mStorageReference = FirebaseStorage.getInstance().getReference().child("Archivos_Usuarios").child("Grupos").child("grupos_" + email + "_" + AppLanguage.getLanguaje()+ "." + "txt");


                ErakaFileStore.download(mContext, mStorageReference.getPath(), gruposUsuarioFile).addOnSuccessListener(new OnSuccessListener<Long>() {
                    @Override
                    public void onSuccess(Long taskSnapshot) {
                        Log.d("BAF_descGYPN", "Tama&ntildeoArchivoGrupo :" + taskSnapshot);
                        Log.d("BAF_descGYPN", "NombreArchivo:" + gruposUsuarioFile);

                        boolean areSamefile = false;
                        try {
                            areSamefile = json.verifyFiles(Constants.ARCHIVO_GRUPOS,gruposUsuarioFile);
                            Log.d(TAG, "onSuccess: are the same file"+areSamefile);

                        } catch (Exception ex){
                            Log.d(TAG, "error: "+ex.getMessage());
                        }
                        if(areSamefile){
                            observableInteger.incrementValue();
                        }else{
                            try {
                                if (!getStringFromFile(gruposUsuarioFile
                                        .getAbsolutePath()).equals("[]") &&
                                        gruposUsuarioFile.length() > 0 ) {
                                    json.setmJSONArrayTodosLosGrupos(json.readJSONArrayFromFile(gruposUsuarioFile.getAbsolutePath()));
                                    if (!json.guardarJson(Constants.ARCHIVO_GRUPOS))
                                        Log.e(TAG, "Error al guardar Json");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.e("printStackTrace", "" + e);

                            }finally {
                                observableInteger.incrementValue();
                            }
                        }


                    }
                }).addOnFailureListener(new com.google.android.gms.tasks.OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Storage download failed (missing file, network error, etc.) -
                        // without this the chain stalled here silently, same as the
                        // empty onCancelled() below.
                        Log.e(TAG, "syncGroups storage download failed: " + e.getMessage());
                        CrashlyticsUtils.getInstance().getCrashlytics().log(
                                "syncGroups storage download failed: " + e.getMessage());
                        observableInteger.incrementValue();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Previously empty - a denied/invalid-credentials read (e.g. the
                // "Provided authentication credentials are invalid" RTDB error)
                // never fired onDataChange, so observableInteger never advanced
                // and the sync dialog hung on this step forever.
                Log.e(TAG, "syncGroups onCancelled: " + error.getMessage());
                CrashlyticsUtils.getInstance().getCrashlytics().log(
                        "syncGroups onCancelled: " + error.getMessage());
                observableInteger.incrementValue();
            }
        });
    }

    public void downloadOldOrNewGroups(){
        mDatabase.child(Constants.Grupos).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                Log.e(TAG, "bajarGrupos: entrando " );
                String child = "URL_" + Constants.Grupos.toLowerCase() + "_" + locale;
                Log.e(TAG, "DownloadGroups child " + child);
                if (dataSnapshot.hasChild(child)) {
                    Log.e(TAG, "There is a child, DownloadGroups" );
                    mStorageReference = FirebaseStorage.getInstance().getReference().child("Archivos_Usuarios").child(Constants.Grupos).child(Constants.Grupos.toLowerCase() + "_" +email + "_" + locale + "." + "txt");
                    Log.e(TAG, "DownloadGroups mStorageReference" + mStorageReference);
                    final File gruposUsuarioFile = new File(rootPath, Constants.ARCHIVO_GRUPOS);
                    ErakaFileStore.download(mContext, mStorageReference.getPath(), gruposUsuarioFile).addOnSuccessListener(new OnSuccessListener<Long>() {
                        @Override
                        public void onSuccess(Long taskSnapshot) {
                            try {
                                Log.e(TAG, "Successful DownloadGroups" );
                                json.setmJSONArrayTodosLosGrupos(json.readJSONArrayFromFile(gruposUsuarioFile.getAbsolutePath()));
                                if (!json.guardarJson(Constants.ARCHIVO_GRUPOS)) {
                                    Log.e(TAG, "Fallo al guardar json");
                                }else{
                                    Log.e(TAG, "Groups saved");
                                    gruposUsuarioFile.delete();
                                }

                                observableInteger.set(observableInteger.get() + 1);
                            } catch (Exception ex) {
                                Log.e(TAG, "Fallo al guardar Grupo json 1" + ex.getMessage());
                                CrashlyticsUtils.getInstance().getCrashlytics().log(ex.getMessage());
                            }
                        }
                    }).addOnFailureListener(DownloadGroups.super::onFailure);

                }
                else {
                    mStorageReference = FirebaseStorage.getInstance().getReference().child("Archivos_Paises/grupos/" + "grupos_" + locale + "." + "txt");
                    final File gruposUsuarioFile = new File(rootPath, Constants.ARCHIVO_GRUPOS);
                    ErakaFileStore.download(mContext, mStorageReference.getPath(), gruposUsuarioFile).addOnSuccessListener(new OnSuccessListener<Long>() {
                        @Override
                        public void onSuccess(Long taskSnapshot) {
                            try {
                                json.setmJSONArrayTodosLosGrupos(json.readJSONArrayFromFile(gruposUsuarioFile.getAbsolutePath()));
                                if (!json.guardarJson(Constants.ARCHIVO_GRUPOS)) {
                                    Log.e(TAG, "Fallo al guardar json");
                                }else
                                    Log.e(TAG, "Grupo json");
                                observableInteger.set(observableInteger.get() + 1);
                            } catch (Exception ex) {
                                Log.e(TAG, "ex :" +ex.getMessage());

                            }
                        }

                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onCancelled: "+ databaseError.getMessage() );
            }




        });
    }
}