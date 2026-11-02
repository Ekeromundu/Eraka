package na.eraka.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Eraka 27 Oct 2026: word endings used when a whole spoken sentence is turned into pictures. */
public class VoiceDictionarySentenceTest {

    @Test
    public void stemsFindTheBaseWord() {
        assertTrue(VoiceDictionary.stems("wants").contains("want"));
        assertTrue(VoiceDictionary.stems("dishes").contains("dish"));
        assertTrue(VoiceDictionary.stems("going").contains("go"));
        assertTrue(VoiceDictionary.stems("making").contains("make"));
        assertTrue(VoiceDictionary.stems("swimming").contains("swim"));
        assertTrue(VoiceDictionary.stems("played").contains("play"));
        assertTrue(VoiceDictionary.stems("liked").contains("like"));
    }

    @Test
    public void shortWordsAreLeftAlone() {
        assertEquals(0, VoiceDictionary.stems("is").size());
        assertTrue(VoiceDictionary.stems("glass").isEmpty() || !VoiceDictionary.stems("glass").contains("glas"));
    }

    @Test
    public void fillersAreStripped() {
        assertEquals("i want go home", VoiceDictionary.stripFillers("uh i want to go home"));
    }
}
