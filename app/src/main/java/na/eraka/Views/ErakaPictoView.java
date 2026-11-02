package na.eraka.Views;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.View;

import com.stonefacesoft.pictogramslibrary.view.PictoView;

import na.eraka.R;
import na.eraka.utils.ErakaSkinTone;

/**
 * OTTAA's PictoView (from the PictogramsLibrary), but the picture is shown in the skin tone chosen
 * in the child profile (see {@link ErakaSkinTone}). Used in the layouts instead of PictoView (22 Oct 2026).
 */
public class ErakaPictoView extends PictoView {
    public ErakaPictoView(Context context) { super(context); keepPictureInsideFrame(); }
    public ErakaPictoView(Context context, AttributeSet attrs) { super(context, attrs); keepPictureInsideFrame(); }
    public ErakaPictoView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); keepPictureInsideFrame(); }

    @Override
    protected Drawable findResource(String name) {
        Drawable d = ErakaSkinTone.resource(getContext(), name);
        return d != null ? d : super.findResource(name);
    }

    /**
     * Eraka 28 Oct 2026: keeps the coloured frame (color_Picto) above the picture. The picture
     * itself is clipped to the frame's rounded inside by ErakaFramedImageView (pictogram.xml).
     */
    private void keepPictureInsideFrame() {
        View frame = findViewById(R.id.color_Picto);
        if (frame != null) frame.bringToFront();
    }
}
