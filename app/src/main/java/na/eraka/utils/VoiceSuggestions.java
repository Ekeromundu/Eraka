package na.eraka.utils;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import na.eraka.JSONutils.Json;
import na.eraka.R;
import com.stonefacesoft.pictogramslibrary.Classes.Pictogram;
import com.stonefacesoft.pictogramslibrary.utils.GlideAttatcher;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * UI for the personal voice dictionary:
 *  - showSuggestions(): "Did you mean...?" with up to 4 big pictogram tiles.
 *    Nothing is spoken until the child taps a tile.
 *  - showPictoPicker(): searchable list of the child's pictograms, used by the
 *    caregiver to teach or correct a sound.
 */
public final class VoiceSuggestions {

    public interface Listener {
        void onPicked(VoiceDictionary.Candidate candidate);
        void onNoneOfThese();
        void onUseWords();
        void onTeach();
    }

    public interface OnPictoPicked { void onPicked(JSONObject picto); }

    /** Eraka 27 Oct 2026: the child chose the whole spoken message ([I] [want] [go] [home]). */
    public interface OnSequencePicked { void onPicked(List<VoiceDictionary.Candidate> sequence); }

    private VoiceSuggestions() { }

    public static Dialog showSuggestions(Activity activity, String heard,
                                         List<VoiceDictionary.Candidate> candidates,
                                         String lang, Listener listener) {
        return showSuggestions(activity, heard, candidates, null, lang, listener, null);
    }

    /**
     * Same window; when {@code sequence} has pictures, a "Use this message" row with the
     * pictures in spoken order comes first, above the single-picture tiles.
     */
    public static Dialog showSuggestions(Activity activity, String heard,
                                         List<VoiceDictionary.Candidate> candidates,
                                         List<VoiceDictionary.Candidate> sequence,
                                         String lang, Listener listener, OnSequencePicked onSequence) {
        if (activity == null || activity.isFinishing()) return null;
        float dp = activity.getResources().getDisplayMetrics().density;
        Dialog dialog = new Dialog(activity, R.style.ErakaDialog);

        LinearLayout root = column(activity, dp);
        root.addView(title(activity, activity.getString(R.string.voice_dict_title)));
        TextView sub = subtitle(activity, activity.getString(R.string.voice_dict_heard, heard));
        root.addView(sub);

        if (sequence != null && sequence.size() >= 2) {
            root.addView(sequenceRow(activity, dialog, sequence, lang, dp, onSequence));
        }

        LinearLayout row = new LinearLayout(activity);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (130 * dp));
        rp.topMargin = (int) (14 * dp);
        if (candidates != null && !candidates.isEmpty()) root.addView(row, rp);
        if (candidates == null) candidates = new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {
            VoiceDictionary.Candidate c = candidates.get(i);
            LinearLayout tile = new LinearLayout(activity);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setBackgroundResource(R.drawable.bg_eraka_place_tile);
            tile.setPadding((int) (6 * dp), (int) (6 * dp), (int) (6 * dp), (int) (6 * dp));
            ImageView iv = new ImageView(activity);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            tile.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            TextView label = new TextView(activity);
            label.setText(JSONutils.getNombre(c.picto, lang));
            label.setGravity(Gravity.CENTER);
            label.setTextSize(14);
            label.setMaxLines(2);
            label.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_dark));
            tile.addView(label);
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            if (i > 0) tp.setMarginStart((int) (8 * dp));
            row.addView(tile, tp);
            bindImage(activity, c.picto, lang, iv);
            tile.setOnClickListener(v -> {
                v.setSelected(true);
                dialog.dismiss();
                if (listener != null) listener.onPicked(c);
            });
        }

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.topMargin = (int) (14 * dp);
        root.addView(actions, ap);
        actions.addView(action(activity, R.string.voice_dict_none, v -> {
            dialog.dismiss();
            if (listener != null) listener.onNoneOfThese();
        }), weight());
        actions.addView(action(activity, R.string.voice_dict_use_words, v -> {
            dialog.dismiss();
            if (listener != null) listener.onUseWords();
        }), weight());
        actions.addView(action(activity, R.string.voice_dict_teach, v -> {
            dialog.dismiss();
            if (listener != null) listener.onTeach();
        }), weight());

        dialog.setContentView(root);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(d -> { if (listener != null) listener.onNoneOfThese(); });
        dialog.show();
        return dialog;
    }

    /**
     * One big tappable box: "Use this message" and the pictures in the order spoken.
     * Small tiles (they all have to fit), arrows between them to show the order.
     */
    private static View sequenceRow(Activity activity, Dialog dialog, List<VoiceDictionary.Candidate> sequence,
                                    String lang, float dp, OnSequencePicked onSequence) {
        LinearLayout box = new LinearLayout(activity);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_eraka_button_mustard);
        int pad = (int) (8 * dp);
        box.setPadding(pad, pad, pad, pad);
        box.setClickable(true);
        box.setFocusable(true);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.topMargin = (int) (14 * dp);
        box.setLayoutParams(bp);

        TextView head = new TextView(activity);
        head.setText("\uD83D\uDCAC  " + activity.getString(R.string.voice_dict_use_message));
        head.setGravity(Gravity.CENTER);
        head.setTextSize(16);
        head.setTypeface(head.getTypeface(), android.graphics.Typeface.BOLD);
        head.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_on_mustard));
        box.addView(head);

        android.widget.HorizontalScrollView scroll = new android.widget.HorizontalScrollView(activity);
        scroll.setHorizontalScrollBarEnabled(false);
        LinearLayout strip = new LinearLayout(activity);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        strip.setGravity(Gravity.CENTER_VERTICAL);
        scroll.addView(strip);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.gravity = Gravity.CENTER_HORIZONTAL;
        sp.topMargin = (int) (6 * dp);
        box.addView(scroll, sp);

        for (int i = 0; i < sequence.size(); i++) {
            VoiceDictionary.Candidate c = sequence.get(i);
            if (i > 0) {
                TextView arrow = new TextView(activity);
                arrow.setText("\u203A");
                arrow.setTextSize(20);
                arrow.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_on_mustard));
                arrow.setPadding((int) (2 * dp), 0, (int) (2 * dp), 0);
                strip.addView(arrow);
            }
            LinearLayout tile = new LinearLayout(activity);
            tile.setOrientation(LinearLayout.VERTICAL);
            tile.setGravity(Gravity.CENTER);
            tile.setBackgroundResource(R.drawable.bg_eraka_place_tile);
            tile.setPadding((int) (3 * dp), (int) (3 * dp), (int) (3 * dp), (int) (3 * dp));
            ImageView iv = new ImageView(activity);
            iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
            tile.addView(iv, new LinearLayout.LayoutParams((int) (52 * dp), (int) (52 * dp)));
            TextView label = new TextView(activity);
            label.setText(JSONutils.getNombre(c.picto, lang));
            label.setGravity(Gravity.CENTER);
            label.setTextSize(11);
            label.setMaxLines(1);
            label.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_dark));
            tile.addView(label, new LinearLayout.LayoutParams((int) (58 * dp), ViewGroup.LayoutParams.WRAP_CONTENT));
            strip.addView(tile);
            bindImage(activity, c.picto, lang, iv);
            tile.setDuplicateParentStateEnabled(true);
        }
        View.OnClickListener pick = v -> {
            dialog.dismiss();
            if (onSequence != null) onSequence.onPicked(sequence);
        };
        box.setOnClickListener(pick);
        strip.setOnClickListener(pick);   // taps on the pictures themselves count too
        for (int i = 0; i < strip.getChildCount(); i++) strip.getChildAt(i).setOnClickListener(pick);
        return box;
    }

    public interface NoMatchListener {
        void onTryAgain();
        void onTeach();
        void onUseWords();
        void onClosed();
    }

    /**
     * Shown when Whisper heard something but none of the child's pictures match.
     * Replaces the old text-only "Is this what you said?" box, which a child who
     * can't read can't use. Every miss becomes a chance to teach the dictionary.
     */
    public static Dialog showNoMatch(Activity activity, String heard, NoMatchListener listener) {
        if (activity == null || activity.isFinishing()) return null;
        float dp = activity.getResources().getDisplayMetrics().density;
        Dialog dialog = new Dialog(activity, R.style.ErakaDialog);
        final boolean[] handled = {false};

        LinearLayout root = column(activity, dp);
        root.addView(title(activity, activity.getString(R.string.voice_nomatch_title)));
        root.addView(subtitle(activity, activity.getString(R.string.voice_dict_heard, heard)));

        TextView tryAgain = bigButton(activity, "🎤  " + activity.getString(R.string.voice_nomatch_try_again),
                R.drawable.bg_eraka_button_mustard, R.color.eraka_text_on_mustard);
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (56 * dp));
        p1.topMargin = (int) (18 * dp);
        root.addView(tryAgain, p1);

        TextView teach = bigButton(activity, "✋  " + activity.getString(R.string.voice_nomatch_teach),
                R.drawable.bg_eraka_place_tile, R.color.eraka_text_dark);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, (int) (56 * dp));
        p2.topMargin = (int) (10 * dp);
        root.addView(teach, p2);

        TextView words = action(activity, R.string.voice_dict_use_words, null);
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p3.topMargin = (int) (6 * dp);
        root.addView(words, p3);

        tryAgain.setOnClickListener(v -> {
            if (handled[0]) return;
            handled[0] = true;
            dialog.dismiss();
            if (listener != null) listener.onTryAgain();
        });
        teach.setOnClickListener(v -> {
            if (handled[0]) return;
            handled[0] = true;
            dialog.dismiss();
            if (listener != null) listener.onTeach();
        });
        words.setOnClickListener(v -> {
            if (handled[0]) return;
            handled[0] = true;
            dialog.dismiss();
            if (listener != null) listener.onUseWords();
        });
        dialog.setOnDismissListener(d -> {
            if (!handled[0]) {
                handled[0] = true;
                if (listener != null) listener.onClosed();
            }
        });

        dialog.setContentView(root);
        dialog.setCancelable(true);
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    // ------------------------------------------------------------------ Teach a word

    /** What the "Teach a word" page shows while the activity records (TeachController.record). */
    public interface TeachUi {
        void listening();
        void thinking();
        void heard(String text);
        void nothingHeard();
    }

    /** The activity does the recording and the saving; the page only shows what is going on. */
    public interface TeachController {
        /** Start recording for this picture (results go to ui), or stop when already recording. */
        void record(JSONObject picto, TeachUi ui);
        /** The page closed: stop any recording. */
        void close();
        /** Caregiver tapped Save: link this sound to this picture. */
        void save(JSONObject picto, String heard);
    }

    /**
     * Eraka 28 Oct 2026: "Teach a word" as its own page instead of a toast and the board's mic.
     * 1) choose the picture, 2) tap Record and let the child say it their own way, tap Stop,
     * 3) check what was heard and Save (or Try again). The button and the line under it always
     * say what is happening (Record / Stop / Listening... / Writing down... / Heard: "wa").
     */
    public static Dialog showTeachWord(Activity activity, JSONArray allPictos, String lang,
                                       TeachController controller) {
        if (activity == null || activity.isFinishing()) return null;
        float dp = activity.getResources().getDisplayMetrics().density;
        Dialog dialog = new Dialog(activity, R.style.ErakaDialog);
        final JSONObject[] picto = {null};
        final String[] heard = {null};
        final boolean[] recording = {false}, busy = {false};

        LinearLayout root = column(activity, dp);
        root.addView(title(activity, activity.getString(R.string.teach_title)));
        TextView steps = subtitle(activity, activity.getString(R.string.teach_steps));
        steps.setPadding(0, (int) (6 * dp), 0, 0);
        root.addView(steps);

        // 1) the picture
        LinearLayout tile = new LinearLayout(activity);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setBackgroundResource(R.drawable.bg_eraka_place_tile);
        tile.setPadding((int) (8 * dp), (int) (8 * dp), (int) (8 * dp), (int) (8 * dp));
        final ImageView image = new ImageView(activity);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setImageResource(R.drawable.ic_placeholder_picto);
        tile.addView(image, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        final TextView label = new TextView(activity);
        label.setText(R.string.teach_choose_picture);
        label.setGravity(Gravity.CENTER);
        label.setTextSize(15);
        label.setTypeface(label.getTypeface(), android.graphics.Typeface.BOLD);
        label.setTextColor(ContextCompat.getColor(activity, R.color.eraka_green_dark));
        tile.addView(label);
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams((int) (170 * dp), (int) (170 * dp));
        tp.gravity = Gravity.CENTER_HORIZONTAL;
        tp.topMargin = (int) (14 * dp);
        root.addView(tile, tp);

        // 2) record
        final TextView record = bigButton(activity, "", R.drawable.bg_eraka_button_mustard, R.color.eraka_text_on_mustard);
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (56 * dp));
        rp.topMargin = (int) (16 * dp);
        root.addView(record, rp);
        final TextView status = subtitle(activity, "");
        status.setTextSize(15);
        status.setPadding(0, (int) (8 * dp), 0, 0);
        root.addView(status);

        // 3) cancel / save
        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.topMargin = (int) (10 * dp);
        root.addView(actions, ap);
        final TextView cancel = action(activity, R.string.teach_cancel, v -> dialog.dismiss());
        final TextView save = action(activity, R.string.teach_save, null);
        actions.addView(cancel, weight());
        actions.addView(save, weight());

        final Runnable refresh = () -> {
            boolean hasPicture = picto[0] != null;
            boolean hasSound = heard[0] != null && !heard[0].isEmpty();
            if (recording[0]) {
                record.setText("\u23F9  " + activity.getString(R.string.teach_stop));
            } else if (hasSound) {
                record.setText("\uD83C\uDFA4  " + activity.getString(R.string.teach_try_again));
            } else {
                record.setText("\uD83C\uDFA4  " + activity.getString(R.string.teach_record));
            }
            record.setEnabled(hasPicture && !busy[0]);
            record.setAlpha(record.isEnabled() ? 1f : 0.45f);
            save.setEnabled(hasPicture && hasSound && !recording[0] && !busy[0]);
            save.setAlpha(save.isEnabled() ? 1f : 0.35f);
            if (!hasPicture) status.setText(R.string.teach_status_choose);
        };

        final TeachUi ui = new TeachUi() {
            @Override public void listening() {
                if (!dialog.isShowing()) return;
                recording[0] = true; busy[0] = false; heard[0] = null;
                status.setText(R.string.teach_status_listening);
                refresh.run();
            }
            @Override public void thinking() {
                if (!dialog.isShowing()) return;
                recording[0] = false; busy[0] = true;
                status.setText(R.string.teach_status_thinking);
                refresh.run();
            }
            @Override public void heard(String text) {
                if (!dialog.isShowing()) return;
                recording[0] = false; busy[0] = false; heard[0] = text;
                status.setText(activity.getString(R.string.teach_status_heard, text,
                        JSONutils.getNombre(picto[0], lang)));
                refresh.run();
            }
            @Override public void nothingHeard() {
                if (!dialog.isShowing()) return;
                recording[0] = false; busy[0] = false; heard[0] = null;
                status.setText(R.string.teach_status_nothing);
                refresh.run();
            }
        };

        tile.setOnClickListener(v -> {
            if (recording[0] || busy[0]) return;
            showPictoPicker(activity, activity.getString(R.string.teach_which_picture), null, allPictos, lang, chosen -> {
                picto[0] = chosen;
                heard[0] = null;
                label.setText(JSONutils.getNombre(chosen, lang));
                bindImage(activity, chosen, lang, image);
                status.setText(R.string.teach_status_ready);
                refresh.run();
            });
        });
        record.setOnClickListener(v -> {
            if (picto[0] == null || busy[0]) return;
            if (controller != null) controller.record(picto[0], ui);
        });
        save.setOnClickListener(v -> {
            if (!save.isEnabled()) return;
            if (controller != null) controller.save(picto[0], heard[0]);
            dialog.dismiss();
        });
        dialog.setOnDismissListener(d -> { if (controller != null) controller.close(); });

        dialog.setContentView(root);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);
        refresh.run();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.92f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        return dialog;
    }

    private static TextView bigButton(Activity a, String text, int bgRes, int colorRes) {
        TextView b = new TextView(a);
        b.setText(text);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(17);
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        b.setTextColor(ContextCompat.getColor(a, colorRes));
        b.setBackgroundResource(bgRes);
        b.setClickable(true);
        b.setFocusable(true);
        return b;
    }

    /** Searchable list of the child's own pictograms. */
    public static void showPictoPicker(Activity activity, String titleText, String subtitleText,
                                       JSONArray allPictos, String lang, OnPictoPicked callback) {
        if (activity == null || activity.isFinishing() || allPictos == null) return;
        float dp = activity.getResources().getDisplayMetrics().density;
        Dialog dialog = new Dialog(activity, R.style.ErakaDialog);
        LinearLayout root = column(activity, dp);
        root.addView(title(activity, titleText));
        if (subtitleText != null) root.addView(subtitle(activity, subtitleText));

        EditText search = new EditText(activity);
        search.setHint(R.string.voice_dict_search_hint);
        search.setSingleLine(true);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        sp.topMargin = (int) (10 * dp);
        root.addView(search, sp);

        final List<JSONObject> all = new ArrayList<>();
        for (int i = 0; i < allPictos.length(); i++) {
            JSONObject p = allPictos.optJSONObject(i);
            if (p != null && !JSONutils.getNombre(p, lang).isEmpty()) all.add(p);
        }
        final List<JSONObject> shown = new ArrayList<>(all);
        final List<String> names = new ArrayList<>();
        for (JSONObject p : shown) names.add(JSONutils.getNombre(p, lang));
        ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, android.R.layout.simple_list_item_1, names);
        ListView list = new ListView(activity);
        list.setAdapter(adapter);
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (int) (320 * dp)));

        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) {
                String q = s.toString().trim().toLowerCase(Locale.ROOT);
                shown.clear();
                names.clear();
                for (JSONObject p : all) {
                    String n = JSONutils.getNombre(p, lang);
                    if (q.isEmpty() || n.toLowerCase(Locale.ROOT).contains(q)) { shown.add(p); names.add(n); }
                }
                adapter.notifyDataSetChanged();
            }
        });
        list.setOnItemClickListener((AdapterView<?> parent, View view, int position, long id) -> {
            if (position < 0 || position >= shown.size()) return;
            JSONObject picked = shown.get(position);
            dialog.dismiss();
            if (callback != null) callback.onPicked(picked);
        });

        dialog.setContentView(root);
        dialog.show();
    }



    /** Same image rules as MainBoardActivity.loadDrawable (bundled icon, edited photo or URL). */
    public static void bindImage(Activity activity, JSONObject picto, String lang, ImageView iv) {
        try {
            GlideAttatcher attatcher = new GlideAttatcher(activity);
            Pictogram pictogram = new Pictogram(picto, lang);
            if (pictogram.getEditedPictogram().isEmpty()) {
                if (!pictogram.getPictogram().startsWith("https://")) {
                    Drawable d = Json.getInstance().getIcono(picto);
                    if (d != null) attatcher.UseCornerRadius(true).loadDrawable(d, iv);
                } else {
                    attatcher.UseCornerRadius(true).loadDrawable(Uri.parse(pictogram.getPictogram()), iv);
                }
            } else {
                File f = new File(pictogram.getEditedPictogram());
                if (f.exists()) attatcher.UseCornerRadius(true).loadDrawable(f, iv);
                else attatcher.UseCornerRadius(true).loadDrawable(Uri.parse(pictogram.getUrl()), iv);
            }
        } catch (Exception ignored) {
            // label under the tile is still shown
        }
    }

    private static LinearLayout column(Activity a, float dp) {
        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_eraka_dialog);
        int pad = (int) (20 * dp);
        root.setPadding(pad, pad, pad, pad);
        return root;
    }

    private static TextView title(Activity a, String text) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(22);
        t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        t.setTextColor(ContextCompat.getColor(a, R.color.eraka_text_dark));
        return t;
    }

    private static TextView subtitle(Activity a, String text) {
        TextView t = new TextView(a);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(13);
        t.setTextColor(ContextCompat.getColor(a, R.color.eraka_text_muted));
        return t;
    }

    private static TextView action(Activity a, int textRes, View.OnClickListener l) {
        TextView b = new TextView(a);
        b.setText(textRes);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(13);
        float dp = a.getResources().getDisplayMetrics().density;
        b.setPadding((int) (4 * dp), (int) (10 * dp), (int) (4 * dp), (int) (10 * dp));
        b.setTextColor(ContextCompat.getColor(a, R.color.eraka_green_dark));
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        b.setOnClickListener(l);
        return b;
    }

    private static LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
    }
}
