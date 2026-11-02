package na.eraka.Activities.Eraka;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import na.eraka.JSONutils.Json;
import na.eraka.R;
import na.eraka.idioma.AppLanguage;
import na.eraka.utils.CaregiverGate;
import na.eraka.utils.Phrases.CustomFavoritePhrases;
import na.eraka.utils.SpeechOutput;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Favourite phrases in the Eraka style. Replaces OTTAA's FavoritePhrases screen.
 * Uses the same storage (favoritePhrases.txt via CustomFavoritePhrases), so favourites
 * are still backed up by "Backup & restore".
 *  - Tap a card: speak it.
 *  - Add / remove: behind the caregiver lock.
 */
public class ErakaFavouritesActivity extends AppCompatActivity {

    private CustomFavoritePhrases favourites;
    private SpeechOutput tts;
    private LinearLayout list;
    private float dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eraka_favourites);
        ((TextView) findViewById(R.id.erakaPageTitle)).setText(R.string.fav_title);
        findViewById(R.id.erakaBackButton).setOnClickListener(v -> finish());

        dp = getResources().getDisplayMetrics().density;
        list = findViewById(R.id.favList);
        tts = SpeechOutput.getInstance(this);
        favourites = new CustomFavoritePhrases(this);
        findViewById(R.id.favAddButton).setOnClickListener(v ->
                CaregiverGate.run(this, this::showAddDialog));
        render();
    }

    // ---------------- list ----------------

    /** Favourites for the current language (older items without a language are shown too). */
    private List<JSONObject> visibleFavourites() {
        List<JSONObject> out = new ArrayList<>();
        JSONArray all = favourites.getPhrases();
        String lang = AppLanguage.getLanguaje();
        for (int i = 0; i < all.length(); i++) {
            JSONObject o = all.optJSONObject(i);
            if (o == null || o.optString("frase", "").trim().isEmpty()) continue;
            if (!o.has("locale") || o.optString("locale").equalsIgnoreCase(lang)) out.add(o);
        }
        return out;
    }

    private void render() {
        list.removeAllViews();
        List<JSONObject> items = visibleFavourites();
        findViewById(R.id.favEmpty).setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
        for (JSONObject item : items) list.addView(card(item));
    }

    private View card(JSONObject item) {
        String text = item.optString("frase");

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_eraka_place_tile);
        row.setPadding(px(14), px(14), px(8), px(14));
        row.setMinimumHeight(px(76));
        LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rp.topMargin = px(12);
        row.setLayoutParams(rp);
        row.setOnClickListener(v -> speak(text));

        ImageView speaker = new ImageView(this);
        speaker.setImageResource(R.drawable.ic_eraka_speaker);
        speaker.setColorFilter(ContextCompat.getColor(this, R.color.eraka_green_dark));
        speaker.setBackgroundResource(R.drawable.bg_eraka_icon_badge);
        speaker.setPadding(px(9), px(9), px(9), px(9));
        row.addView(speaker, new LinearLayout.LayoutParams(px(44), px(44)));

        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(19);
        label.setTextColor(ContextCompat.getColor(this, R.color.eraka_text_dark));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lp.setMarginStart(px(14));
        row.addView(label, lp);

        ImageButton remove = new ImageButton(this);
        remove.setImageResource(R.drawable.ic_eraka_delete);
        remove.setColorFilter(ContextCompat.getColor(this, R.color.eraka_text_muted));
        remove.setBackgroundResource(android.R.color.transparent);
        remove.setContentDescription(getString(R.string.fav_remove_desc));
        remove.setOnClickListener(v -> CaregiverGate.run(this, () -> confirmRemove(item)));
        row.addView(remove, new LinearLayout.LayoutParams(px(44), px(44)));
        return row;
    }

    private void speak(String text) {
        try {
            tts.hablar(text);
        } catch (Exception e) {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmRemove(JSONObject item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.fav_remove_title)
                .setMessage(item.optString("frase"))
                .setPositiveButton(R.string.fav_remove_yes, (d, w) -> {
                    favourites.removeFavoritePhrase(item);
                    save();
                    render();
                })
                .setNegativeButton(R.string.gate_cancel, null)
                .show();
    }

    // ---------------- add ----------------

    private void showAddDialog() {
        Dialog dialog = new Dialog(this, R.style.ErakaDialog);
        dialog.setContentView(R.layout.dialog_eraka_add_phrase);
        EditText input = dialog.findViewById(R.id.addPhraseText);
        LinearLayout suggestions = dialog.findViewById(R.id.addPhraseSuggestions);

        Runnable addTyped = () -> {
            String t = input.getText().toString().trim();
            if (!t.isEmpty() && add(t)) input.setText("");
        };
        dialog.findViewById(R.id.addPhraseButton).setOnClickListener(v -> addTyped.run());
        input.setOnEditorActionListener((v, actionId, e) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) { addTyped.run(); return true; }
            return false;
        });
        dialog.findViewById(R.id.addPhraseClose).setOnClickListener(v -> dialog.dismiss());

        List<String> history = builtSentences();
        if (!history.isEmpty()) {
            suggestions.addView(heading(getString(R.string.fav_from_history)));
            for (String s : history) suggestions.addView(suggestion(s));
        }
        suggestions.addView(heading(getString(R.string.fav_starters)));
        for (String s : getResources().getStringArray(R.array.fav_starter_phrases)) suggestions.addView(suggestion(s));

        dialog.setOnDismissListener(d -> render());
        dialog.show();
    }

    /** Most-used sentences the child has built on this phone (top 15). */
    private List<String> builtSentences() {
        List<JSONObject> phrases = new ArrayList<>();
        try {
            JSONArray all = Json.getInstance().setmContext(this).getPhrasesByLanguage();
            for (int i = 0; i < all.length(); i++) {
                JSONObject o = all.optJSONObject(i);
                if (o != null && !o.optString("frase", "").trim().isEmpty()) phrases.add(o);
            }
        } catch (Exception ignored) { }
        Collections.sort(phrases, (a, b) -> b.optInt("frecuencia", 0) - a.optInt("frecuencia", 0));
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(15, phrases.size()); i++) out.add(phrases.get(i).optString("frase").trim());
        return out;
    }

    private TextView heading(String text) {
        TextView h = new TextView(this);
        h.setText(text);
        h.setTextSize(14);
        h.setTypeface(h.getTypeface(), android.graphics.Typeface.BOLD);
        h.setTextColor(ContextCompat.getColor(this, R.color.eraka_green_dark));
        h.setPadding(0, px(12), 0, px(4));
        return h;
    }

    private View suggestion(String text) {
        TextView t = new TextView(this);
        t.setText("+  " + text);
        t.setTextSize(16);
        t.setTextColor(ContextCompat.getColor(this, R.color.eraka_text_dark));
        t.setBackgroundResource(R.drawable.bg_eraka_place_tile);
        t.setPadding(px(14), px(12), px(14), px(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(6);
        t.setLayoutParams(lp);
        t.setOnClickListener(v -> {
            if (add(text)) {
                v.setEnabled(false);
                v.setAlpha(0.45f);
            }
        });
        return t;
    }

    /** Adds a text phrase in the same format OTTAA uses for sentences. */
    private boolean add(String text) {
        try {
            JSONObject phrase = new JSONObject();
            phrase.put("frase", text);
            phrase.put("frecuencia", 0);
            phrase.put("locale", AppLanguage.getLanguaje());
            JSONObject complejidad = new JSONObject();
            complejidad.put("valor", 0);
            complejidad.put("pictos componentes", new JSONArray());
            phrase.put("complejidad", complejidad);
            phrase.put("fecha", System.currentTimeMillis());
            if (favourites.addFavoritePhrase(phrase)) {
                save();
                Toast.makeText(this, R.string.fav_added, Toast.LENGTH_SHORT).show();
                return true;
            }
            Toast.makeText(this, R.string.fav_exists, Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            Toast.makeText(this, R.string.loader_something_went_wrong, Toast.LENGTH_SHORT).show();
        }
        return false;
    }

    /** Writes the file directly (Json.guardarJson skips empty lists, so removing the last one wouldn't stick). */
    private void save() {
        favourites.saveFavoritePhrases();
        Json.getInstance().setmJSonArrayFrasesFavoritas(favourites.getPhrases());
    }

    private int px(int v) {
        return (int) (v * dp);
    }
}
