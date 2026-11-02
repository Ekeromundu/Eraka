package na.eraka.Interfaces;

import android.view.MotionEvent;
import android.view.View;

public interface TimedClickListener {
    void OnClickBarrido();

    boolean onTouch(View v, MotionEvent event);
}
