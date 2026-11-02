package na.eraka.utils.Accesibilidad.scrollActions;

import android.content.Context;
import android.os.Message;

import na.eraka.GroupGalleryActivity;
import na.eraka.Views.GroupGalleryNavigator;

import androidx.annotation.NonNull;

public class ScrollFunctionGaleriaGrupos extends ScrollFunction {
    private final GroupGalleryNavigator navigator;
    public ScrollFunctionGaleriaGrupos(Context mContext,GroupGalleryNavigator navigator) {
        super(mContext);
        this.navigator= navigator;
    }
    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what){
            case HACER_CLICK:
                navigator.OnClickBarrido();
                break;
            default:
                super.handleMessage(msg);
        }
    }
}
