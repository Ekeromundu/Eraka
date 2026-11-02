package na.eraka.Views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

/**
 * Eraka 28 Oct 2026: the picture inside a picture tile (Imagen_Picto in pictogram.xml).
 *
 * The tile's coloured frame has rounded corners, but a photo is a rectangle and, depending on
 * how the picture library loads it, could be drawn over the frame. This view never draws outside
 * a rounded rectangle inside its padding, whatever was loaded into it, with or without hardware
 * drawing. Corner radius 13 dp = the frame's 20 dp corner minus the 7 dp padding, so the photo
 * follows the frame's curve.
 */
public class ErakaFramedImageView extends AppCompatImageView {

    private final Path clip = new Path();
    private final RectF box = new RectF();

    public ErakaFramedImageView(Context context) { super(context); setUp(); }
    public ErakaFramedImageView(Context context, AttributeSet attrs) { super(context, attrs); setUp(); }
    public ErakaFramedImageView(Context context, AttributeSet attrs, int defStyle) { super(context, attrs, defStyle); setUp(); }

    private void setUp() {
        setCropToPadding(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int l = getPaddingLeft(), t = getPaddingTop();
        int r = getWidth() - getPaddingRight(), b = getHeight() - getPaddingBottom();
        if (r <= l || b <= t) { super.onDraw(canvas); return; }
        float radius = 13 * getResources().getDisplayMetrics().density;
        box.set(l, t, r, b);
        clip.reset();
        clip.addRoundRect(box, radius, radius, Path.Direction.CW);
        int save = canvas.save();
        canvas.clipPath(clip);
        super.onDraw(canvas);
        canvas.restoreToCount(save);
    }
}
