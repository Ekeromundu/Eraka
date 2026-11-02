package na.eraka.Views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import com.stonefacesoft.pictogramslibrary.view.GameGroupView;

import na.eraka.utils.ErakaSkinTone;

/**
 * OTTAA's GameGroupView (from the PictogramsLibrary), but the picture is shown in the skin tone chosen
 * in the child profile (see {@link ErakaSkinTone}). Used in the layouts instead of GameGroupView (22 Oct 2026).
 */
public class ErakaGameGroupView extends GameGroupView {
    public ErakaGameGroupView(Context context) { super(context); }
    public ErakaGameGroupView(Context context, AttributeSet attrs) { super(context, attrs); }
    public ErakaGameGroupView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); }

    @Override
    protected Drawable findResource(String name) {
        Drawable d = ErakaSkinTone.resource(getContext(), name);
        return d != null ? d : super.findResource(name);
    }
}
