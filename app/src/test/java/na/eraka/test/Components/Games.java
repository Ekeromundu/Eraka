package na.eraka.test.Components;

import na.eraka.JSONutils.Json;

import org.json.JSONObject;

public class Games {
    private final Json json;
    public Games(Json json){
        this.json=json;
    }

    public JSONObject createGame(int id,int levelId){
        JSONObject game=new JSONObject();
        return game;
    }

}
