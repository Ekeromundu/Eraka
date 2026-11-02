package na.eraka.utils.Ttsutils;

import android.content.Context;
import android.content.SharedPreferences;
import android.speech.tts.TextToSpeech;

import na.eraka.Interfaces.PictogramLockListener;
import na.eraka.utils.CustomToast;

public class UtilsGamesTTS extends UtilsTTS  {
    public PictogramLockListener lockUnlockedPictograms;
    public UtilsGamesTTS(Context mContext, TextToSpeech mTTS, CustomToast alerta, SharedPreferences sharedPrefsDefault,PictogramLockListener lock_unlocked_pictograms) {
        super(mContext, alerta, sharedPrefsDefault);
        this.lockUnlockedPictograms=lock_unlocked_pictograms;
    }

    @Override
    public void hablar(String frase) {
        super.hablar(frase);
        while (mTTS.isSpeaking())
            lockUnlockedPictograms.lockPictogram(mTTS.isSpeaking());
        lockUnlockedPictograms.unlockPictogram();
    }
    public void stop(){
        mTTS.shutdown();
    }
}
