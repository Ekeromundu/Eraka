package na.eraka.Dialogos.DialogUtils;

import android.content.Context;
import android.view.View;

import na.eraka.R;

public class YesNoOtherDialog extends DialogUtils {

    public YesNoOtherDialog(Context mContext, String title, String message) {
        super(mContext, title, message);
        ocultarElementos();

    }

    public YesNoOtherDialog(Context mContext) {
        super(mContext);
        ocultarElementos();
    }


    @Override
    protected void ocultarElementos() {
        changeVisibilityView(R.id.progressBar2, View.GONE);
    }


}
