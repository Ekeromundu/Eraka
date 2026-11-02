package na.eraka.erakanlg;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import simplenlg.features.Feature;
import simplenlg.features.Form;
import simplenlg.features.InterrogativeType;
import simplenlg.features.Tense;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.PPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;
import simplenlg.realiser.english.Realiser;

/**
 * SimpleNLG feasibility probe (11 Oct 2026).
 *
 * Question it answers: can SimpleNLG (already shipped in app/libs by OTTAA, v4.4.8)
 * build the grammatical sentences Eraka needs, fast enough on a low-cost phone?
 * No templates yet: each test sentence is built directly in code from the roles
 * a template would supply (subject, verb, object, complement).
 *
 * Pure Java (no Android classes): runs as a JUnit test on the PC and, in debug
 * builds, once in the background on the phone (results in Logcat, tag ErakaNLG).
 */
public final class ErakaNlgProbe {

    /** One test sentence: what we expect and what SimpleNLG produced. */
    public static final class Case {
        public final String name, expected;
        public String actual;
        public double ms;
        Case(String name, String expected) { this.name = name; this.expected = expected; }
        public boolean ok() { return expected.equals(actual); }
    }

    /** Everything one run measured. */
    public static final class Report {
        public double lexiconLoadMs;
        public final List<Case> cases = new ArrayList<>();
        public int passed() { int n = 0; for (Case c : cases) if (c.ok()) n++; return n; }
        public List<String> lines() {
            List<String> out = new ArrayList<>();
            out.add(String.format(Locale.ROOT, "lexicon load %.0f ms; %d/%d sentences as expected",
                    lexiconLoadMs, passed(), cases.size()));
            for (Case c : cases) out.add(String.format(Locale.ROOT, "%s %-22s %6.1f ms  \"%s\"%s",
                    c.ok() ? "OK  " : "DIFF", c.name, c.ms, c.actual,
                    c.ok() ? "" : "   (expected \"" + c.expected + "\")"));
            return out;
        }
    }

    private final NLGFactory f;
    private final Realiser r;

    private ErakaNlgProbe(Lexicon lexicon) {
        f = new NLGFactory(lexicon);
        r = new Realiser(lexicon);
    }

    /** Loads SimpleNLG's built-in English word list (the slow part) and runs all cases. */
    public static Report run() {
        Report rep = new Report();
        long t0 = System.nanoTime();
        Lexicon lexicon = Lexicon.getDefaultLexicon();
        rep.lexiconLoadMs = (System.nanoTime() - t0) / 1e6;
        ErakaNlgProbe p = new ErakaNlgProbe(lexicon);
        p.add(rep, "want + dog", "I want a dog.", p.wantThing("dog", "a"));
        p.add(rep, "want + apple", "I want an apple.", p.wantThing("apple", "a"));
        p.add(rep, "want + water", "I want water.", p.wantThing("water", null));
        p.add(rep, "want + eat", "I want to eat.", p.wantAction("eat"));
        p.add(rep, "mom + be + happy", "Mom is happy.", p.isState("mom", "happy"));
        p.add(rep, "I + be + tired", "I am tired.", p.isState("I", "tired"));
        p.add(rep, "I + feel + happy", "I feel happy.", p.clause("I", "feel", null, "happy"));
        p.add(rep, "mom: give me meat", "Mom, give me meat.", p.request("mom", "give", "meat"));
        p.add(rep, "dad + go + shop", "Dad goes to the shop.", p.goTo("dad", "shop"));
        p.add(rep, "can I have apple?", "Can I have an apple?", p.canIHave("apple"));
        p.add(rep, "dad + want + 2 apple", "Dad wants two apples.", p.wantsMany("dad", "apple", "two"));
        p.add(rep, "past: I + eat + apple", "I ate an apple.", p.past("I", "eat", "apple"));
        return rep;
    }

    // ---- one builder per sentence shape (later: one per template) ----

    private interface Build { String go(); }

    private void add(Report rep, String name, String expected, Build b) {
        Case c = new Case(name, expected);
        long t = System.nanoTime();
        try {
            c.actual = b.go();
        } catch (RuntimeException e) {
            c.actual = "ERROR " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        c.ms = (System.nanoTime() - t) / 1e6;
        rep.cases.add(c);
    }

    private NPPhraseSpec np(String noun, String det) {
        NPPhraseSpec n = f.createNounPhrase(noun);
        if (det != null) n.setDeterminer(det); // "a" becomes "an" before a vowel automatically
        return n;
    }

    /** "I want a dog." / "I want water." (det = null: no article, e.g. water, milk) */
    private Build wantThing(String noun, String det) {
        return () -> {
            SPhraseSpec s = f.createClause("I", "want", np(noun, det));
            return r.realiseSentence(s);
        };
    }

    /** "I want to eat.": the second verb becomes an infinitive ("to eat"). */
    private Build wantAction(String verb) {
        return () -> {
            SPhraseSpec s = f.createClause("I", "want");
            VPPhraseSpec v = f.createVerbPhrase(verb);
            v.setFeature(Feature.FORM, Form.INFINITIVE);
            s.addComplement(v);
            return r.realiseSentence(s);
        };
    }

    /** "Mom is happy." / "I am tired.": SimpleNLG picks am/is/are from the subject. */
    private Build isState(String subject, String adj) {
        return clause(subject, "be", null, adj);
    }

    private Build clause(String subject, String verb, String object, String complement) {
        return () -> {
            SPhraseSpec s = f.createClause(subject, verb);
            if (object != null) s.setObject(object);
            if (complement != null) s.addComplement(complement);
            return r.realiseSentence(s);
        };
    }

    /** "Mom, give me meat.": an imperative addressed to a person. */
    private Build request(String person, String verb, String object) {
        return () -> {
            SPhraseSpec s = f.createClause();
            s.setVerb(verb);
            s.setIndirectObject("me");
            s.setObject(object);
            s.setFeature(Feature.FORM, Form.IMPERATIVE);
            String body = r.realiseSentence(s);
            String p = Character.toUpperCase(person.charAt(0)) + person.substring(1);
            return p + ", " + Character.toLowerCase(body.charAt(0)) + body.substring(1);
        };
    }

    /** "Dad goes to the shop." */
    private Build goTo(String subject, String place) {
        return () -> {
            SPhraseSpec s = f.createClause(subject, "go");
            PPPhraseSpec pp = f.createPrepositionPhrase("to", np(place, "the"));
            s.addComplement(pp);
            return r.realiseSentence(s);
        };
    }

    /** "Can I have an apple?" */
    private Build canIHave(String noun) {
        return () -> {
            SPhraseSpec s = f.createClause("I", "have", np(noun, "a"));
            s.setFeature(Feature.MODAL, "can");
            s.setFeature(Feature.INTERROGATIVE_TYPE, InterrogativeType.YES_NO);
            return r.realiseSentence(s);
        };
    }

    /** "Dad wants two apples." */
    private Build wantsMany(String subject, String noun, String number) {
        return () -> {
            NPPhraseSpec n = np(noun, number);
            n.setPlural(true);
            return r.realiseSentence(f.createClause(subject, "want", n));
        };
    }

    /** "I ate an apple." */
    private Build past(String subject, String verb, String noun) {
        return () -> {
            SPhraseSpec s = f.createClause(subject, verb, np(noun, "a"));
            s.setFeature(Feature.TENSE, Tense.PAST);
            return r.realiseSentence(s);
        };
    }

    /** Lets you run the probe on the PC without JUnit: java ... ErakaNlgProbe */
    public static void main(String[] args) {
        for (String line : run().lines()) System.out.println(line);
    }
}
