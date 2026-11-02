package na.eraka.utils;

import android.app.Activity;
import android.view.View;
import android.widget.ScrollView;

import na.eraka.R;

/** Finds a screen's ScrollView and its "scrollHint" view and connects them (27 Oct 2026). */
public final class ErakaScrollHintHolder {
    private ErakaScrollHintHolder() {}

    public static void attach(Activity a, int scrollViewId) {
        View s = a.findViewById(scrollViewId);
        if (s instanceof ScrollView) ErakaScrollHint.attach((ScrollView) s, a.findViewById(R.id.scrollHint));
    }
}
