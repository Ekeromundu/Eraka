package na.eraka.utils;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.RenderEffect;
import android.graphics.Shader;
import android.os.Build;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.StringRes;

import na.eraka.R;

public class ErakaLoader {

    private final Activity activity;
    private Dialog dialog;
    private TextView message;
    private ObjectAnimator bounce;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable timeoutRunnable;

    // Background blur/fade
    private static final float BLUR_RADIUS_PX = 22f;
    private static final long FADE_MS = 250;
    private View blurFallback;
    private View scrim;
    private View card;
    private ValueAnimator blurAnimator;
    private Bitmap snapshot;

    public ErakaLoader(Activity activity) {
        this.activity = activity;
    }

    private boolean activityAlive() {
        return activity != null && !activity.isFinishing() && !activity.isDestroyed();
    }

    private void build() {
        dialog = new Dialog(activity, R.style.ErakaLoaderDialog);
        dialog.setContentView(R.layout.dialog_eraka_loader);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        message = dialog.findViewById(R.id.loaderMessage);
        blurFallback = dialog.findViewById(R.id.loaderBlurFallback);
        scrim = dialog.findViewById(R.id.loaderScrim);
        card = dialog.findViewById(R.id.loaderCard);

        // Cover the whole screen (the scrim + blur replace the default grey dim)
        Window w = dialog.getWindow();
        if (w != null) {
            w.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            w.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
            w.setWindowAnimations(0); // we animate ourselves
        }

        ImageView mascot = dialog.findViewById(R.id.loaderMascot);
        float dp = activity.getResources().getDisplayMetrics().density;
        bounce = ObjectAnimator.ofFloat(mascot, View.TRANSLATION_Y, 0f, -14f * dp);
        bounce.setDuration(550);
        bounce.setRepeatMode(ValueAnimator.REVERSE);
        bounce.setRepeatCount(ValueAnimator.INFINITE);
        bounce.setInterpolator(new AccelerateDecelerateInterpolator());
    }

    public void show(@StringRes int text) {
        if (!activityAlive()) return;
        if (dialog == null) build();
        message.setText(text);
        if (!dialog.isShowing()) {
            applyBackgroundBlur();
            dialog.show();
            fadeIn();
            bounce.start();
        }
    }

    /** Eraka 28 Oct 2026: show with a text that is not a string resource (old OTTAA dialogs). */
    public void show(CharSequence text) {
        if (!activityAlive()) return;
        if (dialog == null) build();
        message.setText(text);
        if (!dialog.isShowing()) {
            applyBackgroundBlur();
            dialog.show();
            fadeIn();
            bounce.start();
        }
    }

    public void setMessage(CharSequence text) {
        if (message != null) message.setText(text);
    }

    /** Show, and if still showing after timeoutMs, hide and run onTimeout. */
    public void showWithTimeout(@StringRes int text, long timeoutMs, Runnable onTimeout) {
        show(text);
        cancelTimeout();
        timeoutRunnable = () -> {
            if (isShowing()) {
                dismiss();
                if (onTimeout != null && activityAlive()) onTimeout.run();
            }
        };
        handler.postDelayed(timeoutRunnable, timeoutMs);
    }

    /** Change the text while the loader is already visible. */
    public void setMessage(@StringRes int text) {
        if (message != null) message.setText(text);
    }

    public boolean isShowing() {
        return dialog != null && dialog.isShowing();
    }

    private void cancelTimeout() {
        if (timeoutRunnable != null) {
            handler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }
    }

    /** The activity's own content (everything under the loader). */
    private View contentView() {
        return activity.findViewById(android.R.id.content);
    }

    /**
     * Android 12+: real blur on the page via RenderEffect, animated in.
     * Older Android: snapshot the page, blur a tiny copy, and show it behind the scrim.
     */
    private void applyBackgroundBlur() {
        View content = contentView();
        if (content == null) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            blurFallback.setVisibility(View.GONE);
            if (blurAnimator != null) blurAnimator.cancel();
            blurAnimator = ValueAnimator.ofFloat(0.01f, BLUR_RADIUS_PX);
            blurAnimator.setDuration(FADE_MS);
            blurAnimator.addUpdateListener(a -> {
                float r = (float) a.getAnimatedValue();
                content.setRenderEffect(RenderEffect.createBlurEffect(r, r, Shader.TileMode.CLAMP));
            });
            blurAnimator.start();
        } else {
            Bitmap blurred = blurredSnapshot(content);
            if (blurred != null) {
                ((ImageView) blurFallback).setImageBitmap(blurred);
                blurFallback.setVisibility(View.VISIBLE);
            }
        }
    }

    private void clearBackgroundBlur() {
        if (blurAnimator != null) blurAnimator.cancel();
        View content = contentView();
        if (content != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            content.setRenderEffect(null);
        }
        if (blurFallback != null) {
            ((ImageView) blurFallback).setImageDrawable(null);
            blurFallback.setVisibility(View.GONE);
        }
        if (snapshot != null) {
            snapshot.recycle();
            snapshot = null;
        }
    }

    private void fadeIn() {
        scrim.setAlpha(0f);
        blurFallback.setAlpha(0f);
        card.setAlpha(0f);
        card.setScaleX(0.9f);
        card.setScaleY(0.9f);
        scrim.animate().alpha(1f).setDuration(FADE_MS).start();
        blurFallback.animate().alpha(1f).setDuration(FADE_MS).start();
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(FADE_MS)
                .setInterpolator(new AccelerateDecelerateInterpolator()).start();
    }

    /** Cheap blur for Android 8-11: draw the page at 1/8 size and box-blur it; scaling back up smooths it further. */
    private Bitmap blurredSnapshot(View content) {
        try {
            int w = content.getWidth(), h = content.getHeight();
            if (w == 0 || h == 0) return null;
            final int scale = 8;
            int sw = Math.max(1, w / scale), sh = Math.max(1, h / scale);
            Bitmap small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(small);
            c.scale(1f / scale, 1f / scale);
            content.draw(c);
            boxBlur(small, 3);
            boxBlur(small, 3);
            snapshot = small;
            return small;
        } catch (Throwable t) {
            return null; // out of memory etc. - the cream scrim alone still fades the page
        }
    }

    private static void boxBlur(Bitmap bmp, int radius) {
        int w = bmp.getWidth(), h = bmp.getHeight();
        int[] src = new int[w * h];
        int[] tmp = new int[w * h];
        bmp.getPixels(src, 0, w, 0, 0, w, h);
        blurPass(src, tmp, w, h, radius, true);
        blurPass(tmp, src, w, h, radius, false);
        bmp.setPixels(src, 0, w, 0, 0, w, h);
    }

    private static void blurPass(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int outer = horizontal ? h : w, inner = horizontal ? w : h;
        for (int o = 0; o < outer; o++) {
            for (int i = 0; i < inner; i++) {
                int a = 0, rr = 0, g = 0, b = 0, n = 0;
                for (int k = -r; k <= r; k++) {
                    int j = Math.min(inner - 1, Math.max(0, i + k));
                    int px = horizontal ? in[o * w + j] : in[j * w + o];
                    a += px >>> 24; rr += (px >> 16) & 0xFF; g += (px >> 8) & 0xFF; b += px & 0xFF; n++;
                }
                int idx = horizontal ? o * w + i : i * w + o;
                out[idx] = ((a / n) << 24) | ((rr / n) << 16) | ((g / n) << 8) | (b / n);
            }
        }
    }

    public void dismiss() {
        cancelTimeout();
        if (bounce != null) bounce.cancel();
        clearBackgroundBlur();
        if (dialog != null && dialog.isShowing()) {
            try {
                dialog.dismiss();
            } catch (IllegalArgumentException ignored) {
                // window already detached
            }
        }
    }
}
