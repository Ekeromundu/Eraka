package na.eraka.utils.TalkActions;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import na.eraka.FirebaseRequests.FirebaseUtils;
import na.eraka.JSONutils.Json;
import na.eraka.idioma.AppLanguage;
import na.eraka.utils.ConnectionDetector;
import na.eraka.utils.JSONutils;
import na.eraka.utils.RemoteConfigUtils;
import na.eraka.utils.preferences.DataUser;
import na.eraka.utils.preferences.User;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
/**
 * Make a follow up about the selected pictograms
 */
/** Eraka 19 Oct 2026: OTTAA NLG / ChatGPT prompt builders removed (experimental mode). */
public class MessageHistory {



    private ArrayList<JSONObject> listOfPictograms = new ArrayList<>();
    private JSONObject father;
    private final Json json;
    private final String TAG = "Historial";



    public MessageHistory(Json json) {
        this.json = json;
        listOfPictograms = new ArrayList<>();
    }

    public ArrayList<JSONObject> getListadoPictos() {
        return listOfPictograms;
    }
    public void addPictograma(JSONObject object){
        listOfPictograms.add(object);
    }
    public void removePictogram(){
        if(listOfPictograms.size()>0) {
            listOfPictograms.remove(listOfPictograms.size() - 1);
        }
    }

    public boolean hasPictogram(JSONObject object){
        return listOfPictograms.toString().contains(object.toString());
    }


    public void clear(){
        listOfPictograms=new ArrayList<>();
    }



    public JSONObject getFather(){
        try {
            if(!listOfPictograms.isEmpty())
                father=listOfPictograms.get(listOfPictograms.size()-1);
            else
                father=json.getPictoFromId2(0);

        }catch (Exception ex){
            father=json.getPictoFromId2(0);
        }
        return father;
    }

    public JSONObject removePictograms(boolean clearAll){
        if(clearAll)
            clear();
        else
            removePictogram();

        return getFather();
    }

    // NEW — supports removing a word at an arbitrary position (e.g. tapping a
// chip in the sentence strip), not just the most recent one. Does not
// touch removePictogram()/removePictograms(), so MainBoardActivity.java's existing
// undo behavior (last-word-only) is unaffected.
    public JSONObject removePictogramAt(int index) {
        if (index >= 0 && index < listOfPictograms.size()) {
            listOfPictograms.remove(index);
        }
        return getFather();
    }


}