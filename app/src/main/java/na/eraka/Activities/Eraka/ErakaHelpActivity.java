package na.eraka.Activities.Eraka;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import na.eraka.R;

/** Help & tips for caregivers - replaces the old tutorial. Content lives in strings_eraka_menu.xml. */
public class ErakaHelpActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eraka_help);
        ((TextView) findViewById(R.id.erakaPageTitle)).setText(R.string.tips_title);
        findViewById(R.id.erakaBackButton).setOnClickListener(v -> finish());
    }
}
