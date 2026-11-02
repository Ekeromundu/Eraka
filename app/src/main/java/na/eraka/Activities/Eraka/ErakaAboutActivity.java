package na.eraka.Activities.Eraka;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import na.eraka.R;

/** About Eraka: what it is, privacy & safety, version and contact. */
public class ErakaAboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eraka_about);
        ((TextView) findViewById(R.id.erakaPageTitle)).setText(R.string.about_title);
        findViewById(R.id.erakaBackButton).setOnClickListener(v -> finish());

        TextView version = findViewById(R.id.aboutVersion);
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            //noinspection deprecation
            version.setText(getString(R.string.about_version, info.versionName, info.versionCode));
        } catch (Exception e) {
            version.setText("");
        }

        findViewById(R.id.aboutEmailButton).setOnClickListener(v -> {
            Intent email = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + getString(R.string.eraka_contact_email)));
            email.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.about_email_subject) + " (" + version.getText() + ")");
            try {
                startActivity(email);
            } catch (ActivityNotFoundException e) {
                Toast.makeText(this, getString(R.string.eraka_contact_email), Toast.LENGTH_LONG).show();
            }
        });
    }
}
