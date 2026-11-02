package na.eraka.utils.Accesibilidad.scrollActions;

import android.content.Context;
import android.os.Message;

import na.eraka.PictogramGalleryActivity;

import androidx.annotation.NonNull;

public class ScrollFunctionGaleriaPictos extends ScrollFunction{
    private final PictogramGalleryActivity galeriaPictos3;
    public ScrollFunctionGaleriaPictos(Context mContext,PictogramGalleryActivity galeriaGrupos2) {
        super(mContext);
        this.galeriaPictos3=galeriaGrupos2;
    }
    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what){
            case HACER_CLICK:
                galeriaPictos3.OnClickBarrido();
                break;
            default:
                super.handleMessage(msg);
        }
    }
}
