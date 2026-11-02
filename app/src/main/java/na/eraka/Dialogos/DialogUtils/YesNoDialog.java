package na.eraka.Dialogos.DialogUtils;

import android.content.Context;
import android.view.View;

import na.eraka.R;

public class YesNoDialog extends DialogUtils {


    public YesNoDialog(Context mContext) {
        super(mContext);
        ocultarElementos();

    }

    public YesNoDialog(Context mContext, String title, String message) {
        super(mContext, title, message);
        ocultarElementos();
    }

    @Override
    protected void ocultarElementos() {
        changeVisibilityView(R.id.progressBar2,View.GONE);
        changeVisibilityView(R.id.unknow_Button,View.GONE);
    }

}
