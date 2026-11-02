package na.eraka.Views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.stonefacesoft.pictogramslibrary.view.GroupView;

import na.eraka.utils.ErakaSkinTone;

/**
 * OTTAA's GroupView (from the PictogramsLibrary), but the picture is shown in the skin tone chosen
 * in the child profile (see {@link ErakaSkinTone}). Used in the layouts instead of GroupView (22 Oct 2026).
 */
public class ErakaGroupView extends GroupView {
    public ErakaGroupView(Context context) { super(context); }
    public ErakaGroupView(Context context, AttributeSet attrs) { super(context, attrs); }
    public ErakaGroupView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }

    @Override
    protected Drawable findResource(String name) {
        Drawable d = ErakaSkinTone.resource(getContext(), name);
        return d != null ? d : super.findResource(name);
    }
}
