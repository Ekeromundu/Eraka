package na.eraka.erakanlg;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Runs the SimpleNLG feasibility probe on the PC (no phone needed):
 * right-click this file in Android Studio -> Run 'ErakaNlgProbeTest'.
 * Prints every sentence and fails if any sentence differs from the expected one.
 */
public class ErakaNlgProbeTest {

    @Test
    public void simpleNlgBuildsTheSentencesErakaNeeds() {
        ErakaNlgProbe.Report rep = ErakaNlgProbe.run();
        for (String line : rep.lines()) System.out.println(line);
        StringBuilder diffs = new StringBuilder();
        for (ErakaNlgProbe.Case c : rep.cases) {
            if (!c.ok()) diffs.append("\n").append(c.name).append(": \"").append(c.actual)
                    .append("\" (expected \"").append(c.expected).append("\")");
        }
        assertEquals("sentences that differ:" + diffs, rep.cases.size(), rep.passed());
    }
}
