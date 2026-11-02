package na.eraka.utils;

import android.app.Activity;
import android.app.Dialog;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import na.eraka.R;

/**
 * Eraka 28 Oct 2026: choose several pictures or groups and remove them in one go.
 * A list with a tick box, the picture and its name for each item; "Select all"; Delete (n)
 * asks once to confirm. Used by the Pictures screen (remove from this group) and the Groups
 * screen (delete groups).
 */
public final class ErakaMultiDelete {

    public interface OnDelete { void delete(List<Integer> ids); }

    /**
     * Eraka 28 Oct 2026: work in two parts so a loader can show while deleting:
     * {@code work} runs in the background (change and save the files), {@code done} on the
     * main thread afterwards (refresh the screen, say how many were deleted).
     */
    public interface BackgroundDelete {
        void work(List<Integer> ids);
        void done(List<Integer> ids);
    }

    public static void show(Activity activity, int titleRes, JSONArray items, Set<Integer> skipIds,
                            String lang, int confirmText, BackgroundDelete job) {
        show(activity, titleRes, items, skipIds, lang, confirmText, (OnDelete) ids -> {
            final ErakaLoader loader = new ErakaLoader(activity);
            loader.show(R.string.loader_deleting);
            new Thread(() -> {
                try {
                    job.work(ids);
                } catch (Exception e) {
                    android.util.Log.e("ErakaMultiDelete", "delete: " + e.getMessage());
                }
                activity.runOnUiThread(() -> {
                    loader.dismiss();
                    job.done(ids);
                });
            }, "eraka-delete").start();
        });
    }

    private ErakaMultiDelete() {}

    /**
     * @param items        pictures or groups (each with "id")
     * @param skipIds      ids that cannot be removed (e.g. the "ALL" group); not listed
     * @param confirmText  string with %d, e.g. "Remove %d pictures from this group? …"
     */
    public static void show(Activity activity, int titleRes, JSONArray items, Set<Integer> skipIds,
                            String lang, int confirmText, OnDelete onDelete) {
        if (activity == null || activity.isFinishing() || items == null) return;
        final List<JSONObject> list = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject o = items.optJSONObject(i);
            if (o == null) continue;
            int id = o.optInt("id", -1);
            if (id == -1 || (skipIds != null && skipIds.contains(id))) continue;
            list.add(o);
        }
        if (list.isEmpty()) {
            android.widget.Toast.makeText(activity, R.string.multi_nothing, android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        final Set<Integer> chosen = new LinkedHashSet<>();
        float dp = activity.getResources().getDisplayMetrics().density;
        Dialog dialog = new Dialog(activity, R.style.ErakaDialog);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_eraka_dialog);
        int pad = (int) (18 * dp);
        root.setPadding(pad, pad, pad, pad / 2);

        TextView title = new TextView(activity);
        title.setText(titleRes);
        title.setTextSize(20);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_dark));
        root.addView(title);
        TextView hint = new TextView(activity);
        hint.setText(R.string.multi_hint);
        hint.setGravity(Gravity.CENTER);
        hint.setTextSize(13);
        hint.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_muted));
        hint.setPadding(0, (int) (4 * dp), 0, (int) (8 * dp));
        root.addView(hint);

        // Eraka 28 Oct 2026: search box, so a picture or group is easy to find in a long list
        final android.widget.EditText search = new android.widget.EditText(activity);
        search.setHint(R.string.multi_search_hint);
        search.setSingleLine(true);
        search.setCompoundDrawablesRelativeWithIntrinsicBounds(android.R.drawable.ic_menu_search, 0, 0, 0);
        root.addView(search, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final List<JSONObject> shown = new ArrayList<>(list);

        final ListView listView = new ListView(activity);
        root.addView(listView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                Math.min((int) (380 * dp), (int) (activity.getResources().getDisplayMetrics().heightPixels * 0.55f))));

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(actions, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        final TextView all = action(activity, activity.getString(R.string.multi_select_all), R.color.eraka_green_dark);
        final TextView cancel = action(activity, activity.getString(android.R.string.cancel), R.color.eraka_text_muted);
        final TextView delete = action(activity, "", R.color.eraka_delete_red);
        LinearLayout.LayoutParams w = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        actions.addView(all, w);
        actions.addView(cancel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(delete, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        final BaseAdapter adapter = new BaseAdapter() {
            @Override public int getCount() { return shown.size(); }
            @Override public Object getItem(int p) { return shown.get(p); }
            @Override public long getItemId(int p) { return shown.get(p).optInt("id", p); }
            @Override public View getView(int p, View convert, ViewGroup parent) {
                LinearLayout row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(0, (int) (4 * dp), 0, (int) (4 * dp));
                JSONObject o = shown.get(p);
                CheckBox box = new CheckBox(activity);
                box.setChecked(chosen.contains(o.optInt("id", -1)));
                box.setClickable(false);
                box.setFocusable(false);
                row.addView(box);
                ImageView img = new ImageView(activity);
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams((int) (48 * dp), (int) (48 * dp));
                ip.setMarginEnd((int) (10 * dp));
                row.addView(img, ip);
                try { VoiceSuggestions.bindImage(activity, o, lang, img); } catch (Exception ignored) { }
                TextView name = new TextView(activity);
                name.setText(JSONutils.getNombre(o, lang));
                name.setTextSize(16);
                name.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_dark));
                row.addView(name, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                return row;
            }
        };
        listView.setAdapter(adapter);

        final Runnable refresh = () -> {
            delete.setText(activity.getString(R.string.multi_delete_button, chosen.size()));
            delete.setEnabled(!chosen.isEmpty());
            delete.setAlpha(chosen.isEmpty() ? 0.35f : 1f);
            boolean allShownChosen = !shown.isEmpty();
            for (JSONObject o : shown) if (!chosen.contains(o.optInt("id", -1))) { allShownChosen = false; break; }
            all.setText(allShownChosen ? R.string.multi_select_none : R.string.multi_select_all);
            adapter.notifyDataSetChanged();
        };
        listView.setOnItemClickListener((parent, view, p, idL) -> {
            int id = shown.get(p).optInt("id", -1);
            if (!chosen.remove(id)) chosen.add(id);
            refresh.run();
        });
        // "Select all" works on what the search shows
        all.setOnClickListener(v -> {
            boolean allShownChosen = !shown.isEmpty();
            for (JSONObject o : shown) if (!chosen.contains(o.optInt("id", -1))) { allShownChosen = false; break; }
            for (JSONObject o : shown) {
                if (allShownChosen) chosen.remove(o.optInt("id", -1)); else chosen.add(o.optInt("id", -1));
            }
            refresh.run();
        });
        search.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(android.text.Editable s) {
                String q = s.toString().trim().toLowerCase(java.util.Locale.ROOT);
                shown.clear();
                for (JSONObject o : list) {
                    if (q.isEmpty() || JSONutils.getNombre(o, lang).toLowerCase(java.util.Locale.ROOT).contains(q)) shown.add(o);
                }
                refresh.run();
            }
        });
        cancel.setOnClickListener(v -> dialog.dismiss());
        delete.setOnClickListener(v -> {
            if (chosen.isEmpty()) return;
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.multi_confirm_title)
                    .setMessage(activity.getString(confirmText, chosen.size()))
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(R.string.multi_confirm_yes, (d, which) -> {
                        dialog.dismiss();
                        if (onDelete != null) onDelete.delete(new ArrayList<>(chosen));
                    })
                    .show();
        });

        dialog.setContentView(root);
        refresh.run();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout((int) (activity.getResources().getDisplayMetrics().widthPixels * 0.92f),
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private static TextView action(Activity a, String text, int colorRes) {
        TextView b = new TextView(a);
        b.setText(text);
        b.setGravity(Gravity.CENTER);
        b.setTextSize(14);
        float dp = a.getResources().getDisplayMetrics().density;
        b.setPadding((int) (4 * dp), (int) (12 * dp), (int) (4 * dp), (int) (12 * dp));
        b.setTextColor(ContextCompat.getColor(a, colorRes));
        b.setTypeface(b.getTypeface(), android.graphics.Typeface.BOLD);
        return b;
    }
}
