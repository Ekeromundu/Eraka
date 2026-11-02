package na.eraka.erakaprediction;

/**
 * Eraka speech check (13 Oct 2026): the fixed word list and the scoring rules.
 *
 * Once at the start and once at the end of each week the caregiver shows the child the
 * same 10 pictures and the child says each word. For every word we keep
 *   - recognised: did Whisper write down the word itself?  (speech recognition accuracy)
 *   - rank: where the picture appeared in the voice suggestions (1 = first, 0 = not offered)
 *           (end-to-end accuracy: what the child would actually get on screen)
 * Comparing start and end of week shows whether the personal voice dictionary helps.
 *
 * Pure Java (no Android), so it is unit-tested on the PC (ErakaSpeechCheckTest).
 * Text passed in must already be normalised with VoiceDictionary.normalize().
 */
public final class ErakaSpeechCheck {

    /** OTTAA picture ids: water, milk, apple, ball, dog, mom, dad, eat, drink, happy. */
    public static final int[] WORD_IDS = {500, 495, 474, 996, 342, 628, 632, 118, 51, 171};

    public static final String PHASE_START = "start";
    public static final String PHASE_END = "end";

    private ErakaSpeechCheck() { }

    /**
     * True if the label's words appear, in order and next to each other, as whole words in
     * what was heard. A plural "s" is accepted ("dogs" counts for "dog"), nothing else is:
     * "what a" does NOT count for "water". That is on purpose: this measures Whisper itself;
     * the fuzzy matching is measured separately by the rank.
     */
    public static boolean recognised(String heardNorm, String labelNorm) {
        if (heardNorm == null || labelNorm == null) return false;
        String h = heardNorm.trim();
        String l = labelNorm.trim();
        if (h.isEmpty() || l.isEmpty()) return false;
        String[] heard = h.split("\\s+");
        String[] label = l.split("\\s+");
        for (int start = 0; start + label.length <= heard.length; start++) {
            boolean all = true;
            for (int k = 0; k < label.length; k++) {
                if (!sameWord(heard[start + k], label[k])) { all = false; break; }
            }
            if (all) return true;
        }
        return false;
    }

    static boolean sameWord(String heard, String label) {
        return heard.equals(label) || heard.equals(label + "s") || heard.equals(label + "es");
    }

    /** 1-based place of target in the suggested ids, 0 if it is not there. */
    public static int rankOf(int[] suggestedIds, int target) {
        if (suggestedIds == null) return 0;
        for (int i = 0; i < suggestedIds.length; i++) if (suggestedIds[i] == target) return i + 1;
        return 0;
    }
}
