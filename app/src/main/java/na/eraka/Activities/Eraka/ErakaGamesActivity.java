package na.eraka.Activities.Eraka;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import na.eraka.Games.GameSelector;
import na.eraka.Games.TellAStory;
import na.eraka.R;
import na.eraka.utils.IntentCode;

/**
 * Eraka games menu. Replaces the old MainJuegos screen (whose code was switched off)
 * and opens the existing games exactly the way the old games gallery did.
 */
public class ErakaGamesActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eraka_games);
        na.eraka.utils.ErakaSkinTone.applyTagged(findViewById(android.R.id.content));   // children in the chosen skin tone
        ((TextView) findViewById(R.id.erakaPageTitle)).setText(R.string.games_title);
        findViewById(R.id.erakaBackButton).setOnClickListener(v -> finish());

        findViewById(R.id.gameCard1).setOnClickListener(v -> open(GameSelector.class, "notigames"));
        findViewById(R.id.gameCard2).setOnClickListener(v -> open(GameSelector.class, "seleccionar_palabras"));
        findViewById(R.id.gameCard3).setOnClickListener(v -> open(GameSelector.class, "descripciones"));
        findViewById(R.id.gameCard4).setOnClickListener(v -> open(TellAStory.class, "history"));
    }

    private void open(Class<?> game, String name) {
        Intent intent = new Intent(this, game);
        intent.putExtra("name_game", name);
        startActivityForResult(intent, IntentCode.NOTIGAMES.getCode());
    }
}
