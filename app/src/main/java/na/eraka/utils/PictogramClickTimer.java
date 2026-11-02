package na.eraka.utils;

import android.content.Context;
import android.os.Handler;
import android.preference.PreferenceManager;

import na.eraka.PictogramView;
import na.eraka.R;


/**
 *
 * @author  gonzalo Juarez
 * This class was created in order to wait a piece of time when you touch one pictogram
 **/
public class PictogramClickTimer {

    private boolean click=false;
    private final Handler handler;
    private final Context mContext;
    public PictogramClickTimer(Context mContext){
        this.mContext=mContext;
        this.handler=new Handler();
    }

    public void resetClick(){
        int time=devolverTiempo();
           handler.postDelayed(new Runnable() {
               @Override
               public void run() {
                   click = false;
               }
           }, time);

    }


    public void callLongClick(){

    }
    public boolean isClicked(){
        return click;
    }

    public void disableClick(){
        click=true;
    }

    private int devolverTiempo(){
        return PreferenceManager.getDefaultSharedPreferences(mContext).getInt(mContext.getResources().getString(R.string.str_time_click),4)*250;
    }

}
