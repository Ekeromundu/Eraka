package na.eraka.FirebaseRequests;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import na.eraka.BuildConfig;
import na.eraka.R;

/**
 * Eraka developer switch (DEBUG builds only): choose LIVE Firebase or the laptop EMULATOR.
 * Opened by long-pressing the logo on the sign-in screen.
 * Saving signs out (the two servers have different accounts) and restarts the app.
 */
public final class FirebaseServerDialog {

    private FirebaseServerDialog() { }

    public static void show(final Activity activity) {
        if (!BuildConfig.DEBUG || activity == null || activity.isFinishing()) return;
        float dp = activity.getResources().getDisplayMetrics().density;
        final Dialog dialog = new Dialog(activity, R.style.ErakaDialog);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_eraka_dialog);
        int pad = (int) (20 * dp);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(activity);
        title.setText("Firebase server (developer)");
        title.setTextSize(20);
        title.setTypeface(title.getTypeface(), Typeface.BOLD);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_dark));
        root.addView(title);

        TextView current = new TextView(activity);
        current.setText("Now using: " + FirebaseServerConfig.describe(activity));
        current.setGravity(Gravity.CENTER);
        current.setTextSize(13);
        current.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_muted));
        root.addView(current);

        final RadioGroup group = new RadioGroup(activity);
        group.setPadding(0, (int) (12 * dp), 0, 0);
        final RadioButton live = new RadioButton(activity);
        live.setId(View.generateViewId());
        live.setText("Live (online Firebase project)");
        final RadioButton emu = new RadioButton(activity);
        emu.setId(View.generateViewId());
        emu.setText("Emulator (on my laptop)");
        group.addView(live);
        group.addView(emu);
        root.addView(group);

        final EditText host = new EditText(activity);
        host.setHint("Laptop IPv4, e.g. 192.168.1.23");
        host.setSingleLine(true);
        host.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        host.setText(FirebaseServerConfig.getHost(activity));
        root.addView(host, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        final TextView help = new TextView(activity);
        help.setText("Emulator needs: same Wi-Fi as the laptop, laptop awake, and "
                + "\"firebase emulators:start\" running (ports auth " + FirebaseServerConfig.PORT_AUTH
                + ", database " + FirebaseServerConfig.PORT_DATABASE + ", storage "
                + FirebaseServerConfig.PORT_STORAGE + "). Find the IP with ipconfig.");
        help.setTextSize(12);
        help.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_muted));
        root.addView(help);

        boolean isEmu = FirebaseServerConfig.MODE_EMULATOR.equals(FirebaseServerConfig.getMode(activity));
        group.check(isEmu ? emu.getId() : live.getId());
        host.setEnabled(isEmu);
        group.setOnCheckedChangeListener((g, id) -> host.setEnabled(id == emu.getId()));

        LinearLayout buttons = new LinearLayout(activity);
        buttons.setOrientation(LinearLayout.HORIZONTAL);
        buttons.setGravity(Gravity.END);
        buttons.setPadding(0, (int) (14 * dp), 0, 0);
        TextView cancel = textButton(activity, "Cancel");
        TextView save = textButton(activity, "Save & restart");
        buttons.addView(cancel);
        buttons.addView(save);
        root.addView(buttons);

        cancel.setOnClickListener(v -> dialog.dismiss());
        save.setOnClickListener(v -> {
            boolean wantEmu = group.getCheckedRadioButtonId() == emu.getId();
            String ip = host.getText().toString().trim();
            if (wantEmu && !ip.matches("[0-9a-zA-Z.:-]+")) {
                Toast.makeText(activity, "Enter the laptop's IP address first", Toast.LENGTH_SHORT).show();
                return;
            }
            FirebaseServerConfig.save(activity,
                    wantEmu ? FirebaseServerConfig.MODE_EMULATOR : FirebaseServerConfig.MODE_LIVE, ip);
            try { FirebaseAuth.getInstance().signOut(); } catch (Exception ignored) { }
            dialog.dismiss();
            restartApp(activity);
        });

        dialog.setContentView(root);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private static TextView textButton(Activity a, String text) {
        TextView b = new TextView(a);
        b.setText(text);
        b.setTextSize(15);
        b.setTypeface(b.getTypeface(), Typeface.BOLD);
        b.setTextColor(ContextCompat.getColor(a, R.color.eraka_green_dark));
        int p = (int) (12 * a.getResources().getDisplayMetrics().density);
        b.setPadding(p, p, p, p);
        return b;
    }

    /** Firebase can only switch server in a fresh process, so restart the whole app. */
    private static void restartApp(Activity activity) {
        Intent launch = activity.getPackageManager().getLaunchIntentForPackage(activity.getPackageName());
        if (launch != null) {
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            activity.startActivity(launch);
        }
        activity.finishAffinity();
        Runtime.getRuntime().exit(0);
    }
}
