package na.eraka.utils;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.res.ColorStateList;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import na.eraka.R;

/**
 * Eraka: shows what Whisper is doing on the Voice button, so a child (and caregiver)
 * can see the app is working instead of looking frozen.
 *   IDLE      - white button, mic icon, label "Voice"
 *   LISTENING - mustard button that gently pulses, label "Listening…"
 *   THINKING  - green button with a spinning icon, label "Thinking…"
 */
public final class WhisperButtonAnimator {

    public enum State { IDLE, LISTENING, THINKING }

    private final FloatingActionButton fab;
    private final TextView label;
    private ObjectAnimator current;
    private State state = State.IDLE;

    public WhisperButtonAnimator(FloatingActionButton fab) {
        this.fab = fab;
        TextView found = null;
        if (fab != null && fab.getParent() instanceof ViewGroup) {
            ViewGroup parent = (ViewGroup) fab.getParent();
            for (int i = 0; i < parent.getChildCount(); i++) {
                View child = parent.getChildAt(i);
                if (child instanceof TextView) { found = (TextView) child; break; }
            }
        }
        this.label = found;
    }

    public State getState() { return state; }

    public void setState(State newState) {
        if (fab == null) return;
        state = newState;
        stop();
        switch (newState) {
            case LISTENING:
                fab.setEnabled(true);
                fab.setImageResource(android.R.drawable.ic_btn_speak_now);
                tint(R.color.eraka_mustard, R.color.eraka_text_on_mustard);
                setLabel(R.string.whisper_state_listening);
                current = ObjectAnimator.ofPropertyValuesHolder(fab,
                        PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.18f),
                        PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.18f));
                current.setDuration(550);
                current.setRepeatCount(ValueAnimator.INFINITE);
                current.setRepeatMode(ValueAnimator.REVERSE);
                current.start();
                break;
            case THINKING:
                fab.setEnabled(false);
                fab.setImageResource(android.R.drawable.ic_popup_sync);
                tint(R.color.eraka_green_light, R.color.eraka_green_dark);
                setLabel(R.string.whisper_state_thinking);
                current = ObjectAnimator.ofFloat(fab, View.ROTATION, 0f, 360f);
                current.setDuration(1100);
                current.setInterpolator(new LinearInterpolator());
                current.setRepeatCount(ValueAnimator.INFINITE);
                current.start();
                break;
            case IDLE:
            default:
                fab.setEnabled(true);
                fab.setImageResource(android.R.drawable.ic_btn_speak_now);
                tint(R.color.White, R.color.eraka_green_dark);
                setLabel(R.string.whisper_state_idle);
                break;
        }
    }

    private void stop() {
        if (current != null) {
            current.cancel();
            current = null;
        }
        fab.setScaleX(1f);
        fab.setScaleY(1f);
        fab.setRotation(0f);
    }

    private void tint(int bgColorRes, int iconColorRes) {
        fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(fab.getContext(), bgColorRes)));
        fab.setImageTintList(ColorStateList.valueOf(ContextCompat.getColor(fab.getContext(), iconColorRes)));
    }

    private void setLabel(int textRes) {
        if (label != null) label.setText(textRes);
    }
}
