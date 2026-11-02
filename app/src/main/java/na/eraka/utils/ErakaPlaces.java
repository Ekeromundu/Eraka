package na.eraka.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import na.eraka.JSONutils.Json;
import na.eraka.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Offline "Where are we?" picker. Replaces the old GPS / Google Places feature.
 * The chosen place is passed to the same prediction code the GPS version used
 * (Json.setPlaceName -> calcularPosicion), so suggestions adapt to the place.
 */
public final class ErakaPlaces {

    public interface OnPlaceChosen { void onPlaceChosen(Place place); }

    public static final class Place {
        public final String key;      // value understood by Json.calcularPosicion() ("HOME" = general words)
        @StringRes public final int label;
        public final String emoji;
        Place(String key, int label, String emoji) { this.key = key; this.label = label; this.emoji = emoji; }
    }

    private static final String PREF_PLACE = "eraka_place";

    public static final List<Place> PLACES = new ArrayList<>();
    static {
        // 22 Oct 2026: Namibian everyday places (taxi, market, village/farm) added and the zoo
        // removed; most everyday first. A child whose saved place was ZOO gets HOME.
        // Emoji chosen to display on Android 8 too (no newer emoji such as the hut).
        PLACES.add(new Place("HOME", R.string.place_home, "🏠"));
        PLACES.add(new Place("SCHOOL", R.string.place_school, "🏫"));
        PLACES.add(new Place("TAXI", R.string.place_taxi, "🚕"));
        PLACES.add(new Place("STORE", R.string.place_shop, "🛒"));
        PLACES.add(new Place("MARKET", R.string.place_market, "🍅"));
        PLACES.add(new Place("CHURCH", R.string.place_church, "⛪"));
        PLACES.add(new Place("VILLAGE", R.string.place_village, "🌾"));
        PLACES.add(new Place("PARK", R.string.place_park, "🌳"));
        PLACES.add(new Place("HOSPITAL", R.string.place_clinic, "🏥"));
        PLACES.add(new Place("RESTAURANT", R.string.place_restaurant, "🍽️"));
    }

    private ErakaPlaces() { }

    public static Place current(Context ctx) {
        String key = PreferenceManager.getDefaultSharedPreferences(ctx).getString(PREF_PLACE, "HOME");
        for (Place p : PLACES) if (p.key.equals(key)) return p;
        return PLACES.get(0);
    }

    /** Saves the place and hands it to the word-prediction code. */
    public static void apply(Context ctx, Place place) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        prefs.edit().putString(PREF_PLACE, place.key).apply();
        Json.getInstance().setPlaceName(place.key);
        // Eraka 28 Oct 2026: the "Here" group shows this place's pictures. It is saved in the
        // background (writing the groups file made the place tiles feel slow).
        final Context app = ctx.getApplicationContext();
        new Thread(() -> ErakaPlaceWords.updateHereGroup(app), "eraka-here-group").start();
    }

    /** Like apply(), then runs {@code done} on the main thread once the "Here" group is ready. */
    private static void applyThen(Activity activity, Place place, Runnable done) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        prefs.edit().putString(PREF_PLACE, place.key).apply();
        Json.getInstance().setPlaceName(place.key);
        final Context app = activity.getApplicationContext();
        new Thread(() -> {
            ErakaPlaceWords.updateHereGroup(app);
            activity.runOnUiThread(done);
        }, "eraka-here-group").start();
    }

    public static void showPicker(Activity activity, OnPlaceChosen listener) {
        if (activity == null || activity.isFinishing()) return;
        Dialog dialog = new Dialog(activity, R.style.ErakaDialog);
        dialog.setContentView(R.layout.dialog_eraka_places);
        LinearLayout grid = dialog.findViewById(R.id.placesGrid);

        float dp = activity.getResources().getDisplayMetrics().density;
        Place selected = current(activity);
        List<TextView> tiles = new ArrayList<>();
        LinearLayout row = null;
        for (int i = 0; i < PLACES.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(activity);
                row.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams rp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, (int) (84 * dp));
                if (i > 0) rp.topMargin = (int) (10 * dp);
                grid.addView(row, rp);
            }
            Place place = PLACES.get(i);
            TextView tile = new TextView(activity);
            tile.setText(place.emoji + "\n" + activity.getString(place.label));
            tile.setGravity(Gravity.CENTER);
            tile.setTextSize(15);
            tile.setLineSpacing(4 * dp, 1f);
            tile.setTextColor(ContextCompat.getColor(activity, R.color.eraka_text_dark));
            tile.setBackgroundResource(R.drawable.bg_eraka_place_tile);
            tile.setSelected(place.key.equals(selected.key));
            LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f);
            if (i % 2 == 1) tp.setMarginStart((int) (10 * dp));
            row.addView(tile, tp);
            tiles.add(tile);
            tile.setOnClickListener(v -> {
                for (TextView t : tiles) t.setSelected(false);
                v.setSelected(true);
                // Eraka 28 Oct 2026: the tile lights up, the window closes, and a loader shows
                // until the place's pictures (suggestions and "Here" group) are ready
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    dialog.dismiss();
                    final ErakaLoader loader = new ErakaLoader(activity);
                    loader.show(activity.getString(R.string.loader_changing_place, activity.getString(place.label)));
                    final long shownAt = System.currentTimeMillis();
                    applyThen(activity, place, () -> {
                        if (listener != null) listener.onPlaceChosen(place);
                        // keep it up at least briefly, so it does not just flash
                        long wait = Math.max(0, 600 - (System.currentTimeMillis() - shownAt));
                        new Handler(Looper.getMainLooper()).postDelayed(loader::dismiss, wait);
                    });
                }, 250);
            });
        }
        dialog.show();
    }
}
