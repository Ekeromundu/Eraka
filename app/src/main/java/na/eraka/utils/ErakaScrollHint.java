package na.eraka.utils;

import android.view.View;
import android.widget.ScrollView;

/**
 * "More below ⌄" on a scrolling form (27 Oct 2026). Keneke's test: on the child profile the
 * skin-colour choice sat below the screen edge and nothing showed that the page went on.
 *
 * The hint shows while there is content below the visible part, hides at the bottom (or
 * when everything fits), bobs gently so it is noticed, and scrolls down when tapped.
 */
public final class ErakaScrollHint {

    private ErakaScrollHint() {}

    public static void attach(final ScrollView scroll, final View hint) {
        if (scroll == null || hint == null) return;
        final Runnable update = () -> {
            boolean more = scroll.canScrollVertically(1);
            if (more && hint.getVisibility() != View.VISIBLE) {
                hint.setVisibility(View.VISIBLE);
                bob(hint);
            } else if (!more && hint.getVisibility() == View.VISIBLE) {
                hint.animate().cancel();
                hint.setTranslationY(0);
                hint.setVisibility(View.GONE);
            }
        };
        scroll.getViewTreeObserver().addOnScrollChangedListener(update::run);
        scroll.getViewTreeObserver().addOnGlobalLayoutListener(update::run);
        hint.setOnClickListener(v -> scroll.smoothScrollBy(0, Math.max(scroll.getHeight() * 2 / 3, 1)));
        scroll.post(update);
    }

    /** A small up-and-down movement, repeated while the hint is visible. */
    private static void bob(final View hint) {
        if (hint.getVisibility() != View.VISIBLE) return;
        float d = 6f * hint.getResources().getDisplayMetrics().density;
        hint.animate().translationY(d).setDuration(450).withEndAction(() ->
                hint.animate().translationY(0).setDuration(450).withEndAction(() ->
                        hint.postDelayed(() -> bob(hint), 600)));
    }
}
