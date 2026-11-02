package na.eraka.utils.Accesibilidad.scrollActions;

import android.content.Context;
import android.os.Message;

import na.eraka.MainBoardActivity;

import androidx.annotation.NonNull;

public class ScrollFunctionMainActivity extends ScrollFunction{
    private final MainBoardActivity principal;
    public ScrollFunctionMainActivity(Context mContext, MainBoardActivity principal) {
        super(mContext);
        this.principal=principal;
    }

    @Override
    public void handleMessage(@NonNull Message msg) {
        switch (msg.what){
            case HACER_CLICK:
                principal.OnClickBarrido();
                break;
            default:
                super.handleMessage(msg);
        }
    }
}
