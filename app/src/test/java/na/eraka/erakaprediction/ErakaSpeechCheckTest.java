package na.eraka.erakaprediction;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ErakaSpeechCheckTest {

    @Test
    public void exactWordIsRecognised() {
        assertTrue(ErakaSpeechCheck.recognised("water", "water"));
        assertTrue(ErakaSpeechCheck.recognised("i want water please", "water"));
    }

    @Test
    public void pluralIsRecognised() {
        assertTrue(ErakaSpeechCheck.recognised("dogs", "dog"));
    }

    @Test
    public void soundAlikeIsNotRecognised() {
        // Whisper's typical mishearing of a child saying "water"
        assertFalse(ErakaSpeechCheck.recognised("what a", "water"));
        // part of a longer word does not count
        assertFalse(ErakaSpeechCheck.recognised("dadda", "dad"));
    }

    @Test
    public void multiWordLabelNeedsAllWordsInOrder() {
        assertTrue(ErakaSpeechCheck.recognised("i am very happy", "very happy"));
        assertFalse(ErakaSpeechCheck.recognised("happy very", "very happy"));
    }

    @Test
    public void emptyIsNotRecognised() {
        assertFalse(ErakaSpeechCheck.recognised("", "water"));
        assertFalse(ErakaSpeechCheck.recognised(null, "water"));
        assertFalse(ErakaSpeechCheck.recognised("water", ""));
    }

    @Test
    public void rankIsOneBasedAndZeroWhenMissing() {
        int[] shown = {495, 500, 342};
        assertEquals(2, ErakaSpeechCheck.rankOf(shown, 500));
        assertEquals(0, ErakaSpeechCheck.rankOf(shown, 996));
        assertEquals(0, ErakaSpeechCheck.rankOf(null, 500));
    }

    @Test
    public void tenFixedWords() {
        assertEquals(10, ErakaSpeechCheck.WORD_IDS.length);
    }
}
