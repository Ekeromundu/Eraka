package na.eraka.Activities.Eraka;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.Voice;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import na.eraka.R;
import na.eraka.utils.SpeechOutput;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Simple Voice & speech page: test sentence, speed, pitch and which offline voice to use.
 * Uses the same settings keys as the Settings screen (bool_tts, velocidad_tts, pitch_tts:
 * whole numbers where 10 = normal), plus "eraka_tts_voice" for the chosen voice
 * (applied on start-up in UtilsTTS.setUpTTSVOICE).
 */
public class ErakaVoiceActivity extends AppCompatActivity {

    public static final String PREF_VOICE = "eraka_tts_voice";
    private static final int MIN = 5;          // slider 0..15 -> 5..20 (0.5x .. 2x)

    private SharedPreferences prefs;
    private TextToSpeech tts;
    private SeekBar speed, pitch;
    private RadioGroup voiceList;
    private TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private int voiceAttempts = 0;
    private final int defaultRadioId = View.generateViewId();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eraka_voice);
        ((TextView) findViewById(R.id.erakaPageTitle)).setText(R.string.voice_title);
        findViewById(R.id.erakaBackButton).setOnClickListener(v -> finish());

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        tts = SpeechOutput.getInstance(this).getTTS();
        speed = findViewById(R.id.voiceSpeed);
        pitch = findViewById(R.id.voicePitch);
        voiceList = findViewById(R.id.voiceList);
        status = findViewById(R.id.voiceStatus);

        speed.setProgress(prefs.getInt(getString(R.string.str_velocidad_tts), 10) - MIN);
        pitch.setProgress(prefs.getInt(getString(R.string.str_pitch_tts), 10) - MIN);
        SeekBar.OnSeekBarChangeListener save = new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) { if (fromUser) saveRateAndPitch(); }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { speakTest(); }
        };
        speed.setOnSeekBarChangeListener(save);
        pitch.setOnSeekBarChangeListener(save);

        findViewById(R.id.voiceTestButton).setOnClickListener(v -> speakTest());
        findViewById(R.id.voiceReset).setOnClickListener(v -> resetToNormal());
        loadVoices();
    }

    private void saveRateAndPitch() {
        int rate = speed.getProgress() + MIN;
        int tone = pitch.getProgress() + MIN;
        prefs.edit()
                .putBoolean(getString(R.string.bool_TTS), true)   // "custom voice settings" on
                .putInt(getString(R.string.str_velocidad_tts), rate)
                .putInt(getString(R.string.str_pitch_tts), tone)
                .apply();
        if (tts != null) {
            tts.setSpeechRate(rate / 10f);
            tts.setPitch(tone / 10f);
        }
    }

    private void speakTest() {
        if (tts == null) return;
        String text = ((EditText) findViewById(R.id.voiceTestText)).getText().toString().trim();
        if (text.isEmpty()) text = getString(R.string.voice_test_sample);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "erakaVoiceTest");
    }

    private void resetToNormal() {
        speed.setProgress(10 - MIN);
        pitch.setProgress(10 - MIN);
        saveRateAndPitch();
        prefs.edit().remove(PREF_VOICE).apply();
        if (tts != null) {
            Voice current = tts.getVoice();
            tts.setLanguage(current != null ? current.getLocale() : Locale.getDefault());
        }
        voiceList.check(defaultRadioId); // first radio = phone default
        speakTest();
    }

    /** The TTS engine can take a moment to start, so retry a few times. */
    private void loadVoices() {
        Set<Voice> voices = null;
        try {
            voices = tts != null ? tts.getVoices() : null;
        } catch (Exception ignored) { }
        if ((voices == null || voices.isEmpty()) && voiceAttempts++ < 8) {
            handler.postDelayed(this::loadVoices, 500);
            return;
        }
        voiceList.removeAllViews();
        String saved = prefs.getString(PREF_VOICE, "");

        RadioButton def = radio(getString(R.string.voice_default), defaultRadioId);
        voiceList.addView(def);
        def.setChecked(saved.isEmpty());
        def.setOnClickListener(v -> {
            prefs.edit().remove(PREF_VOICE).apply();
            Voice current = tts.getVoice();
            tts.setLanguage(current != null ? current.getLocale() : Locale.getDefault());
            speakTest();
        });

        List<Voice> offline = new ArrayList<>();
        if (voices != null) {
            Voice current = tts.getVoice();
            String lang = current != null ? current.getLocale().getLanguage() : Locale.getDefault().getLanguage();
            for (Voice v : voices) {
                boolean notInstalled = v.getFeatures() != null
                        && v.getFeatures().contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED);
                if (!v.isNetworkConnectionRequired() && !notInstalled
                        && v.getLocale().getLanguage().equals(lang)) {
                    offline.add(v);
                }
            }
            Collections.sort(offline, (a, b) -> a.getName().compareTo(b.getName()));
        }

        Voice za = na.eraka.utils.Ttsutils.UtilsTTS.preferredEnglishVoice(tts);
        boolean anyMale = false;
        for (int i = 0; i < offline.size(); i++) {
            Voice v = offline.get(i);
            int gender = voiceGender(v.getName());
            anyMale |= gender == R.string.voice_male;
            String label = getString(R.string.voice_numbered, i + 1) + " · " + v.getLocale().getDisplayName()
                    + (gender != 0 ? " · " + getString(gender) : "")
                    + (za != null && v.getName().equals(za.getName()) ? " " + getString(R.string.voice_recommended) : "");
            RadioButton rb = radio(label, View.generateViewId());
            rb.setChecked(v.getName().equals(saved));
            rb.setOnClickListener(x -> {
                tts.setVoice(v);
                prefs.edit().putString(PREF_VOICE, v.getName()).apply();
                speakTest();
            });
            voiceList.addView(rb);
        }
        status.setVisibility(offline.isEmpty() ? View.VISIBLE : View.GONE);
        if (!offline.isEmpty() && !anyMale) {
            // Eraka 27 Oct 2026: Google installs its male voices separately
            TextView male = new TextView(this);
            male.setText(R.string.voice_male_tip);
            male.setTextSize(14);
            male.setTextColor(ContextCompat.getColor(this, R.color.eraka_text_dark));
            male.setPadding(0, 24, 0, 8);
            voiceList.addView(male);
        }
        status.setText(R.string.voice_none);
        if (za == null) {
            // no South African or British voice on this phone: explain how to get one (22 Oct 2026)
            TextView tip = new TextView(this);
            tip.setText(R.string.voice_za_tip);
            tip.setTextSize(14);
            tip.setTextColor(ContextCompat.getColor(this, R.color.eraka_text_dark));
            tip.setPadding(0, 24, 0, 8);
            voiceList.addView(tip);
            TextView get = new TextView(this);
            get.setText(R.string.voice_za_get);
            get.setTextSize(15);
            get.setTextColor(ContextCompat.getColor(this, R.color.eraka_green));
            get.setPadding(0, 8, 0, 16);
            get.setOnClickListener(x -> {
                try {
                    startActivity(new android.content.Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA));
                } catch (Exception e) {
                    android.widget.Toast.makeText(this, R.string.voice_za_tip, android.widget.Toast.LENGTH_LONG).show();
                }
            });
            voiceList.addView(get);
        }
    }

    private RadioButton radio(String text, int id) {
        RadioButton rb = new RadioButton(this);
        rb.setId(id);
        rb.setText(text);
        rb.setTextSize(15);
        rb.setTextColor(ContextCompat.getColor(this, R.color.eraka_text_dark));
        rb.setButtonTintList(ContextCompat.getColorStateList(this, R.color.eraka_green));
        rb.setPadding(0, 12, 0, 12);
        return rb;
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /**
     * Eraka 27 Oct 2026: Android does not say whether a voice is male or female, so the voices
     * all looked the same ("Voice 1, Voice 2"). Google's offline voices have fixed codes in their
     * names (e.g. "en-gb-x-gbd-local"); the known ones are labelled, others stay unlabelled.
     */
    private static int voiceGender(String name) {
        if (name == null) return 0;
        String n = name.toLowerCase(Locale.ROOT);
        String[] male = {"-gbb-", "-gbd-", "-rjs-", "-iol-", "-iom-", "-tpd-", "-aub-", "-aud-"};
        String[] female = {"-gba-", "-gbc-", "-gbg-", "-sfg-", "-iob-", "-iog-", "-tpc-", "-tpf-", "-aua-", "-auc-"};
        for (String m : male) if (n.contains(m)) return R.string.voice_male;
        for (String f : female) if (n.contains(f)) return R.string.voice_female;
        return 0;
    }
}
