package na.eraka.erakanlg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Offline "Tell a story" game text (19 Oct 2026). Run on the PC, no phone needed. */
public class ErakaStoryMakerTest {

    @Test
    public void usesAllFourPictures() {
        String s = ErakaStoryMaker.make("Dog", "happy", "play", "park");
        System.out.println(s);
        assertTrue(s.contains("dog"));
        assertTrue(s.contains("happy"));
        assertTrue(s.contains("play"));
        assertTrue(s.contains("park"));
        assertFalse(s.contains("{"));
    }

    @Test
    public void sameChoicesGiveTheSameStory() {
        assertEquals(ErakaStoryMaker.make("cat", "red", "eat", "school"),
                ErakaStoryMaker.make("cat", "red", "eat", "school"));
    }

    @Test
    public void anBeforeAVowel() {
        for (String who : new String[]{"dog", "cat", "mom", "ball", "apple", "teacher"}) {
            String s = ErakaStoryMaker.make(who, "orange", "jump", "shop");
            System.out.println(s);
            assertTrue(s, s.contains("an orange") || s.contains("An orange"));
        }
    }

    @Test
    public void homeHasNoThe() {
        for (String who : new String[]{"dog", "cat", "mom", "ball", "apple", "teacher"}) {
            String s = ErakaStoryMaker.make(who, "big", "sleep", "home");
            assertFalse(s, s.contains("the home"));
        }
    }

    @Test
    public void toIsNotDoubled() {
        for (String who : new String[]{"dog", "cat", "mom"}) {
            String s = ErakaStoryMaker.make(who, "big", "to eat", "park");
            assertFalse(s, s.contains("to to"));
        }
    }

    @Test
    public void emptySlotsNeverBreakTheStory() {
        String s = ErakaStoryMaker.make("", null, "  ", "");
        System.out.println(s);
        assertFalse(s.contains("{"));
        assertFalse(s.contains("  "));
        assertTrue(s.contains("friend"));
    }

    @Test
    public void everyTemplateIsComplete() {
        // try many inputs so every template is used at least once
        boolean[] seen = new boolean[ErakaStoryMaker.TEMPLATES.length];
        String[] whos = {"dog", "cat", "mom", "dad", "ball", "apple", "teacher", "bird", "car", "baby"};
        for (String w : whos) {
            String s = ErakaStoryMaker.make(w, "happy", "run", "beach");
            assertFalse(s, s.contains("{") || s.contains("}"));
            for (int i = 0; i < seen.length; i++) {
                String start = ErakaStoryMaker.TEMPLATES[i].substring(0, 5);
                if (s.startsWith(start)) seen[i] = true;
            }
        }
        for (boolean b : seen) assertTrue("a template was never used", b);
    }
}
