package na.eraka.FirebaseRequests.DownloadFile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import na.eraka.FirebaseRequests.ErakaFileStore;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import na.eraka.idioma.AppLanguage;
import na.eraka.utils.Firebase.CrashlyticsUtils;
import na.eraka.utils.ObservableInteger;
import na.eraka.utils.constants.Constants;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;

public class DownloadPictograms extends DownloadFile implements OnFailureListener {
    public DownloadPictograms(Context mContext, DatabaseReference mDatabase, StorageReference mStorageReference, SharedPreferences sharedPreferences, ObservableInteger observableInteger,String locale) {
        super(mContext, mDatabase, mStorageReference,sharedPreferences,observableInteger,locale);
        TAG ="DownloadPictograms";

    }

    public void syncPictograms() {
        mDatabase.child(Constants.PICTOS).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String child = "URL_pictos_"+locale;
                if(snapshot.hasChild(child)){
                    final File pictosUsuarioFile = new File(rootPath,Constants.ARCHIVO_PICTOS);
                    mStorageReference = FirebaseStorage.getInstance().getReference().child("Archivos_Usuarios").child(Constants.PICTOS).child(Constants.PICTOS.toLowerCase() + "_" + email+ "_" + locale + "." + "txt");
                    ErakaFileStore.download(mContext, mStorageReference.getPath(), pictosUsuarioFile).addOnSuccessListener(new OnSuccessListener<Long>() {
                        @Override
                        public void onSuccess(Long taskSnapshot) {

                            Log.d("BAF_descGYPN", "Tama&ntildeoArchivoPicto:" + taskSnapshot);
                            Log.d("BAF_descGYPN", "NombreArchivo:" + pictosUsuarioFile);
                            Log.d("BAF_descGYPN", "Tama&ntildeoArchivoss :" + pictosUsuarioFile.length());
                            boolean aresamefile = false;
                            try {
                                aresamefile = json.verifyFiles(Constants.ARCHIVO_PICTOS,pictosUsuarioFile);
                            } catch (Exception e) {
                                Log.d(TAG, "onSuccess: " + e.getMessage());
                                e.printStackTrace();
                            }
                            if(aresamefile){
                                observableInteger.incrementValue();
                            }else{
                                try{
                                    if (pictosUsuarioFile.length() > 0 && !getStringFromFile
                                            (pictosUsuarioFile.getAbsolutePath()).equals("[]") &&
                                            getStringFromFile(pictosUsuarioFile.getAbsolutePath()
                                            ) != null) {
                                        json.setmJSONArrayTodosLosPictos(json.readJSONArrayFromFile(pictosUsuarioFile.getAbsolutePath()));
                                        if (!json.guardarJson(Constants.ARCHIVO_PICTOS))
                                            Log.e(TAG, "Error al guardar Json");
                                        else {
                                            Log.d(TAG, "Pictogram Saved");
                                        }
                                    }
                                    observableInteger.incrementValue();
                                }catch (Exception ex){
                                    // Even on a read/parse failure, the sync chain must still
                                    // advance or syncFiles() hangs forever waiting on this step.
                                    Log.e(TAG, "syncPictograms onSuccess: parse/save failed, advancing chain anyway: " + ex.getMessage());
                                    observableInteger.incrementValue();
                                }
                            }

                        }

                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            // Storage download failed (network, permissions, missing file, etc.)
                            // — advance the chain instead of leaving syncFiles() stuck on this step.
                            Log.e(TAG, "syncPictograms: storage download failed, advancing chain anyway: " + e.getMessage());
                            observableInteger.incrementValue();
                        }
                    });

                } else {
                    // No pictos node recorded for this user/locale yet — nothing to sync,
                    // but the chain must still advance or syncFiles() hangs forever.
                    Log.d(TAG, "syncPictograms: no " + child + " node for this user, skipping sync");
                    observableInteger.incrementValue();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Previously empty. A cancelled/denied read (auth failure, permission-denied,
                // invalid credentials, etc.) left observableInteger stuck forever here, which is
                // why the "synchronizing" dialog in EditPictogramActivity never dismissed — the
                // syncFiles() chain in DownloadJsonFromFirebase never advances past this step.
                // Log the failure and advance the chain anyway so the UI doesn't hang, even
                // though this step's pictogram data wasn't synced this time.
                Log.e(TAG, "syncPictograms onCancelled: " + error.getMessage());
                observableInteger.incrementValue();
            }
        });


    }

    public void downloadPictogramsWithNullOption(){
        mDatabase.child(Constants.PICTOS).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                String child = "URL_" + Constants.PICTOS.toLowerCase() + "_" + locale;
                Log.e(TAG, "bajar Pictos: "+ child );

                if (dataSnapshot.hasChild(child)) {
                    Log.e(TAG, "bajar Pictos: Has child" );
                    mStorageReference = FirebaseStorage.getInstance().getReference().child("Archivos_Usuarios").child(Constants.PICTOS).child(Constants.PICTOS.toLowerCase() + "_" + email+ "_" + locale + "." + "txt");
                    Log.e(TAG, "Email: "+ email );
                    Log.e(TAG, "mStorageReference: "+ mStorageReference );
                    final File pictosUsuariosFile = new File(rootPath, Constants.ARCHIVO_PICTOS);
                    Log.e(TAG, "rootPath:"+ rootPath );
                    Log.e(TAG, "pictosUsuariosFile:"+ pictosUsuariosFile );
                    ErakaFileStore.download(mContext, mStorageReference.getPath(), pictosUsuariosFile).addOnSuccessListener(new OnSuccessListener<Long>() {
                        @Override
                        public void onSuccess(Long taskSnapshot) {
                            Log.e(TAG, "Successful Pictos");
                            try {
                                json.setmJSONArrayTodosLosPictos(json.readJSONArrayFromFile(pictosUsuariosFile.getAbsolutePath()));
                                Log.e(TAG, "File stored at:" + pictosUsuariosFile.getAbsolutePath());
                                if (!json.guardarJson(Constants.ARCHIVO_PICTOS)) {
                                    Log.e(TAG, "Fallo al guardar json");
                                }else
                                    Log.e(TAG, "Pictos Usuarios json");

                            } catch (Exception ex) {
                                Log.e(TAG, "Fallo al guardar pictos json 1");
                                CrashlyticsUtils.getInstance().getCrashlytics().log(ex.getMessage());

                            }
                            observableInteger.set(observableInteger.get() + 1);
                        }

                    }).addOnFailureListener(e -> {
                        DownloadPictograms.super.onFailure(e);
                        useBundledEnglish("download failed: " + e.getMessage());
                    });
                } else {
                    mStorageReference = FirebaseStorage.getInstance().getReference().child("Archivos_Paises/pictos/" + "pictos_" + locale + "." + "txt");
                    Log.e(TAG, "Storage: "+ mStorageReference);
                    String gender = getGenderByValue();
                    Log.e(TAG, "Gender onDataChange: "+ gender);
                    if(locale.equals("es"))
                        mStorageReference = FirebaseStorage.getInstance().getReference().child("Archivos_Paises/pictos/es/pictos_es_"+gender+".txt");

                    final File pictosUsuariosFile = new File(rootPath, Constants.ARCHIVO_PICTOS);
                    ErakaFileStore.download(mContext, mStorageReference.getPath(), pictosUsuariosFile).addOnSuccessListener(new OnSuccessListener<Long>() {
                        @Override
                        public void onSuccess(Long taskSnapshot) {
                            try {
                                Log.d(TAG, "Download successful");

                                json.setmJSONArrayTodosLosPictos(json.readJSONArrayFromFile(pictosUsuariosFile.getAbsolutePath()));
                                if (!json.guardarJson(Constants.ARCHIVO_PICTOS)) {
                                    Log.e(TAG, "Fallo al guardar json");
                                }else{
                                    Log.d(TAG,"Pictogram Saved");
                                }
                                observableInteger.set(observableInteger.get() + 1);
                            } catch (Exception ex) {
                                // was observableInteger.set(-1): the sign-up then waited forever
                                useBundledEnglish("could not read " + pictosUsuariosFile + ": " + ex.getMessage());
                            }
                        }

                    }).addOnFailureListener(e -> {
                        DownloadPictograms.super.onFailure(e);
                        useBundledEnglish("download failed: " + e.getMessage());
                    });

                }
            }



            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "onCancelled: "+ databaseError.getMessage() );
                useBundledEnglish("database read cancelled");
            }
        });
    }

    /**
     * Eraka (18 Oct 2026): last resort so the sign-up never hangs on "Saving your answers…".
     * Loads the English starter board packed in the app and lets the sign-up continue;
     * if even that fails, the sign-up still continues (the board loads its defaults later).
     */
    private void useBundledEnglish(String why) {
        Log.e(TAG, "pictos: " + why + " -> using the English starter board from the app");
        final File f = new File(rootPath, Constants.ARCHIVO_PICTOS);
        ErakaFileStore.download(mContext, "Archivos_Paises/pictos/pictos_en.txt", f)
                .addOnSuccessListener(n -> {
                    try {
                        json.setmJSONArrayTodosLosPictos(json.readJSONArrayFromFile(f.getAbsolutePath()));
                        json.guardarJson(Constants.ARCHIVO_PICTOS);
                    } catch (Exception ex) {
                        Log.e(TAG, "English starter board: " + ex.getMessage());
                    }
                    observableInteger.set(observableInteger.get() + 1);
                })
                .addOnFailureListener(e -> observableInteger.set(observableInteger.get() + 1));
    }

    private String getGenderByValue(){
        String value = sharedPrefsDefault.getString(Constants.GENERO,"other");
        Log.d(TAG, "getGenderByValue: "+value);
        switch (value.toLowerCase()){
            case "femenino":
                return "f";
            case "masculino":
                return "m";
            default:
                return "gen";
        }
    }



}
