package na.eraka.Activities.Eraka;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import na.eraka.JSONutils.Json;
import na.eraka.R;
import na.eraka.utils.AvatarPackage.AvatarPhotoStore;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Offline progress report, free for everyone.
 * Replaces ActivityInformes, which depended on the original OTTAA cloud servers
 * and a premium subscription. Everything here is computed from the sentences
 * saved on this phone (frases.txt: frase, frecuencia, fecha, complejidad).
 */
public class ErakaReportActivity extends AppCompatActivity {

    private static final String TAG = "ErakaReport";
    private static final long DAY_MS = 24L * 60 * 60 * 1000;

    private static class Phrase {
        String text;
        int count;
        int pictos;
    }

    private String childName = "";
    private int week, total, unique;
    private float wordsPer;
    private final int[] perDay = new int[7];   // index 6 = today
    private final List<Phrase> phrases = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eraka_report);
        ((TextView) findViewById(R.id.erakaPageTitle)).setText(R.string.report_title);
        findViewById(R.id.erakaBackButton).setOnClickListener(v -> finish());

        bindChild();
        computeStats();
        render();
        findViewById(R.id.reportShare).setOnClickListener(v -> share());
    }

    private void bindChild() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String full = prefs.getString("name", "").trim();
        childName = full.contains(" ") ? full.substring(0, full.indexOf(' ')) : full;
        if (!childName.isEmpty())
            childName = childName.substring(0, 1).toUpperCase() + childName.substring(1);
        ((TextView) findViewById(R.id.reportChildName)).setText(childName.isEmpty() ? getString(R.string.app_name) : childName);

        ImageView avatar = findViewById(R.id.reportAvatar);
        if (AvatarPhotoStore.hasPhoto(this)) {
            AvatarPhotoStore.loadInto(this, avatar);
        } else {
            int id = getResources().getIdentifier(prefs.getString("userAvatar", "ic_avatar_default"), "drawable", getPackageName());
            if (id != 0) avatar.setImageResource(id);
        }
    }

    private void computeStats() {
        JSONArray all;
        try {
            Json json = Json.getInstance().setmContext(this);
            all = json.getmJSONArrayTodasLasFrases();
            if (all == null || all.length() == 0) {
                json.initJsonArrays();
                all = json.getmJSONArrayTodasLasFrases();
            }
        } catch (Exception e) {
            Log.e(TAG, "computeStats: could not read phrases", e);
            all = new JSONArray();
        }
        if (all == null) all = new JSONArray();

        Calendar startOfToday = Calendar.getInstance();
        startOfToday.set(Calendar.HOUR_OF_DAY, 0);
        startOfToday.set(Calendar.MINUTE, 0);
        startOfToday.set(Calendar.SECOND, 0);
        startOfToday.set(Calendar.MILLISECOND, 0);
        long todayStart = startOfToday.getTimeInMillis();
        long weekStart = todayStart - 6 * DAY_MS;

        Set<Integer> words = new HashSet<>();
        int pictoSum = 0, phraseCount = 0;

        for (int i = 0; i < all.length(); i++) {
            JSONObject o = all.optJSONObject(i);
            if (o == null) continue;
            Phrase p = new Phrase();
            p.text = o.optString("frase", "").trim();
            if (p.text.isEmpty()) continue;

            // "fecha" is a single timestamp, or an array of them once a sentence is repeated
            List<Long> dates = new ArrayList<>();
            Object fecha = o.opt("fecha");
            if (fecha instanceof JSONArray) {
                JSONArray arr = (JSONArray) fecha;
                for (int j = 0; j < arr.length(); j++) dates.add(toMillis(arr.optLong(j)));
            } else if (fecha != null) {
                dates.add(toMillis(o.optLong("fecha")));
            }
            p.count = Math.max(o.optInt("frecuencia", 1), dates.size());
            total += p.count;

            for (long d : dates) {
                if (d >= weekStart) {
                    week++;
                    int day = (int) ((d - weekStart) / DAY_MS);
                    if (day >= 0 && day < 7) perDay[day]++;
                }
            }

            JSONObject complejidad = o.optJSONObject("complejidad");
            JSONArray ids = complejidad != null ? complejidad.optJSONArray("pictos componentes") : null;
            if (ids != null && ids.length() > 0) {
                p.pictos = ids.length();
                pictoSum += ids.length();
                phraseCount++;
                for (int j = 0; j < ids.length(); j++) words.add(ids.optInt(j));
            }
            phrases.add(p);
        }
        unique = words.size();
        wordsPer = phraseCount == 0 ? 0 : (float) pictoSum / phraseCount;
        Collections.sort(phrases, (a, b) -> b.count - a.count);
    }

    /** Old data may be stored in seconds instead of milliseconds. */
    private static long toMillis(long t) {
        return t > 0 && t < 100_000_000_000L ? t * 1000 : t;
    }

    private void render() {
        boolean empty = phrases.isEmpty();
        findViewById(R.id.reportEmpty).setVisibility(empty ? View.VISIBLE : View.GONE);
        ((TextView) findViewById(R.id.reportWeek)).setText(String.valueOf(week));
        ((TextView) findViewById(R.id.reportTotal)).setText(String.valueOf(total));
        ((TextView) findViewById(R.id.reportWordsPer)).setText(String.format(Locale.getDefault(), "%.1f", wordsPer));
        ((TextView) findViewById(R.id.reportUnique)).setText(String.valueOf(unique));
        renderBars();
        renderTop();
    }

    private void renderBars() {
        LinearLayout bars = findViewById(R.id.reportBars);
        bars.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;
        int max = 1;
        for (int v : perDay) max = Math.max(max, v);
        SimpleDateFormat dayName = new SimpleDateFormat("EEE", Locale.getDefault());
        Calendar c = Calendar.getInstance();
        c.add(Calendar.DAY_OF_YEAR, -6);

        for (int i = 0; i < 7; i++) {
            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);

            TextView value = new TextView(this);
            value.setText(String.valueOf(perDay[i]));
            value.setTextSize(12);
            value.setTextColor(ContextCompat.getColor(this, R.color.eraka_text_dark));
            col.addView(value);

            View bar = new View(this);
            int h = Math.max((int) (4 * dp), (int) (96 * dp * perDay[i] / max));
            bar.setBackgroundResource(perDay[i] == 0 ? R.drawable.bg_eraka_bar_track : R.drawable.bg_eraka_bar_fill);
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams((int) (22 * dp), h);
            bp.topMargin = (int) (4 * dp);
            col.addView(bar, bp);

            TextView label = new TextView(this);
            label.setText(i == 6 ? getString(R.string.report_today) : dayName.format(c.getTime()));
            label.setTextSize(11);
            label.setTextColor(ContextCompat.getColor(this, i == 6 ? R.color.eraka_green_dark : R.color.eraka_text_muted));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = (int) (4 * dp);
            col.addView(label, lp);

            bars.addView(col, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));
            c.add(Calendar.DAY_OF_YEAR, 1);
        }
    }

    private void renderTop() {
        LinearLayout top = findViewById(R.id.reportTop);
        top.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;
        for (int i = 0; i < Math.min(5, phrases.size()); i++) {
            Phrase p = phrases.get(i);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, (int) (10 * dp), 0, 0);

            TextView text = new TextView(this);
            text.setText(p.text);
            text.setTextSize(15);
            text.setTextColor(ContextCompat.getColor(this, R.color.eraka_text_dark));
            row.addView(text, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView count = new TextView(this);
            count.setText(getString(R.string.report_times, p.count));
            count.setTextSize(13);
            count.setTextColor(ContextCompat.getColor(this, R.color.eraka_text_on_mustard));
            count.setBackgroundResource(R.drawable.bg_eraka_icon_badge);
            count.setPadding((int) (10 * dp), (int) (4 * dp), (int) (10 * dp), (int) (4 * dp));
            row.addView(count);
            top.addView(row);
        }
        if (phrases.isEmpty()) {
            TextView none = new TextView(this);
            none.setText("–");
            none.setPadding(0, (int) (8 * dp), 0, 0);
            top.addView(none);
        }
    }

    /** Plain-text summary a parent can send to a teacher or therapist. */
    private void share() {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.report_share_heading, childName.isEmpty() ? "…" : childName)).append("\n\n");
        sb.append(getString(R.string.report_week)).append(": ").append(week).append('\n');
        sb.append(getString(R.string.report_total)).append(": ").append(total).append('\n');
        sb.append(getString(R.string.report_words_per)).append(": ").append(String.format(Locale.getDefault(), "%.1f", wordsPer)).append('\n');
        sb.append(getString(R.string.report_unique)).append(": ").append(unique).append("\n\n");
        if (!phrases.isEmpty()) {
            sb.append(getString(R.string.report_top)).append(":\n");
            for (int i = 0; i < Math.min(5, phrases.size()); i++)
                sb.append("• ").append(phrases.get(i).text).append(" (").append(phrases.get(i).count).append("×)\n");
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.report_share_heading, childName));
        send.putExtra(Intent.EXTRA_TEXT, sb.toString());
        startActivity(Intent.createChooser(send, getString(R.string.report_share)));
    }
}
