package na.eraka.Dialogos.DialogUtils;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import na.eraka.Dialogos.BaseDialog;
import na.eraka.R;
import na.eraka.utils.ErakaLoader;

/*
* @Author gonzalo Juarez
*
* Eraka 28 Oct 2026: OTTAA's "please wait" window (orange title, spinner) is now shown as
* Eraka's own loader (ErakaLoader: the bouncing mascot on a blurred background, the same as at
* sign-in and when saving a picture). Every screen that used ProgressDialogOptions (syncing,
* ARASAAC search, saving…) gets it without changes. Without an Activity the old window is used.
* */
public class ProgressDialogOptions extends DialogUtils {
    private ErakaLoader loader;
    private String text;

    public ProgressDialogOptions(Context mContext) {
        super(mContext);
        ocultarElementos();
    }

    public ProgressDialogOptions(Context mContext, String title, String message) {
        super(mContext, title, message);
       ocultarElementos();
    }

    @Override
    protected void ocultarElementos() {
        changeVisibilityView(R.id.unknow_Button, View.INVISIBLE);
        changeVisibilityView(R.id.no_button,View.INVISIBLE);
        changeVisibilityView(R.id.yes_button,View.INVISIBLE);
    }

    private static Activity activityOf(Context c) {
        while (c instanceof ContextWrapper) {
            if (c instanceof Activity) return (Activity) c;
            c = ((ContextWrapper) c).getBaseContext();
        }
        return null;
    }

    @Override
    public BaseDialog setTitle(String titulo) {
        super.setTitle(titulo);
        if (text == null) text = titulo;
        return this;
    }

    @Override
    public BaseDialog setMessage(String mensaje) {
        super.setMessage(mensaje);
        if (mensaje != null && !mensaje.trim().isEmpty()) text = mensaje;
        if (loader != null && loader.isShowing() && text != null) loader.setMessage(text);
        return this;
    }

    @Override
    public void mostrarDialogo() {
        Activity a = activityOf(mContext);
        if (a == null) { super.mostrarDialogo(); return; }
        if (loader == null) loader = new ErakaLoader(a);
        if (text != null) loader.show(text);
        else loader.show(R.string.loader_please_wait);
    }

    @Override
    public void destruirDialogo() {
        if (loader != null) { loader.dismiss(); return; }
        super.destruirDialogo();
    }

    @Override
    public void cancelarDialogo() {
        if (loader != null) { loader.dismiss(); return; }
        super.cancelarDialogo();
    }

    @Override
    public boolean isShowing() {
        if (loader != null) return loader.isShowing();
        return super.isShowing();
    }
}
