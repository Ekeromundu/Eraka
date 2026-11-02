package na.eraka.utils;

import android.app.Activity;
import android.app.Dialog;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;

import na.eraka.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * "Grown-ups only" check before settings, profile, report and sign out.
 * Asks a simple sum with three big answer buttons. Once answered, it stays
 * unlocked for a few minutes so a caregiver can move around freely.
 *
 * Usage: CaregiverGate.run(activity, () -> openSettings());
 */
public final class CaregiverGate {

    private static final long UNLOCK_WINDOW_MS = 3 * 60 * 1000; // 3 minutes
    private static long unlockedUntil = 0;
    private static final Random RANDOM = new Random();

    private CaregiverGate() { }

    public static void run(Activity activity, Runnable onPassed) {
        if (System.currentTimeMillis() < unlockedUntil) {
            onPassed.run();
            return;
        }
        if (activity == null || activity.isFinishing()) return;

        Dialog dialog = new Dialog(activity, R.style.ErakaDialog);
        dialog.setContentView(R.layout.dialog_eraka_gate);
        dialog.setCanceledOnTouchOutside(true);

        TextView question = dialog.findViewById(R.id.gateQuestion);
        TextView error = dialog.findViewById(R.id.gateError);
        LinearLayout answers = dialog.findViewById(R.id.gateAnswers);
        dialog.findViewById(R.id.gateCancel).setOnClickListener(v -> dialog.dismiss());

        int a = 3 + RANDOM.nextInt(7);   // 3..9
        int b = 2 + RANDOM.nextInt(8);   // 2..9
        int correct = a + b;
        question.setText(activity.getString(R.string.gate_question, a, b));

        List<Integer> options = new ArrayList<>();
        options.add(correct);
        while (options.size() < 3) {
            int wrong = correct + (RANDOM.nextBoolean() ? 1 : -1) * (1 + RANDOM.nextInt(3));
            if (wrong > 0 && !options.contains(wrong)) options.add(wrong);
        }
        Collections.shuffle(options);

        float dp = activity.getResources().getDisplayMetrics().density;
        for (int i = 0; i < options.size(); i++) {
            int value = options.get(i);
            AppCompatButton button = new AppCompatButton(activity);
            button.setText(String.valueOf(value));
            button.setTextSize(22);
            button.setAllCaps(false);
            button.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_dark));
            button.setBackgroundResource(R.drawable.bg_eraka_place_tile);
            button.setStateListAnimator(null);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) lp.setMarginStart((int) (10 * dp));
            button.setLayoutParams(lp);
            button.setOnClickListener(v -> {
                if (value == correct) {
                    unlockedUntil = System.currentTimeMillis() + UNLOCK_WINDOW_MS;
                    dialog.dismiss();
                    onPassed.run();
                } else {
                    error.setVisibility(View.VISIBLE);
                    v.setEnabled(false);
                    v.setAlpha(0.4f);
                }
            });
            answers.addView(button);
        }
        dialog.show();
    }

    /** Lock again straight away (e.g. after signing out). */
    public static void lockNow() {
        unlockedUntil = 0;
    }
}
