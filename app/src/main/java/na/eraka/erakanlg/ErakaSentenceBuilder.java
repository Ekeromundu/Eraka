package na.eraka.erakanlg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import simplenlg.features.Feature;
import simplenlg.features.Form;
import simplenlg.features.InterrogativeType;
import simplenlg.framework.CoordinatedPhraseElement;
import simplenlg.framework.NLGElement;
import simplenlg.framework.NLGFactory;
import simplenlg.lexicon.Lexicon;
import simplenlg.phrasespec.NPPhraseSpec;
import simplenlg.phrasespec.PPPhraseSpec;
import simplenlg.phrasespec.SPhraseSpec;
import simplenlg.phrasespec.VPPhraseSpec;
import simplenlg.realiser.english.Realiser;

/**
 * Turns the child's pictogram message into grammatical sentence suggestions.
 *
 *   1. Roles: each pictogram gets a role from its word type in the vocabulary ("tipo":
 *      1 person, 2 thing, 3 action, 4 describing word, 5 social, 6 other), corrected by
 *      small word lists (e.g. "want"/"need" = WANT, places, mass nouns, numbers).
 *   2. Templates decide the STRUCTURE from the roles (e.g. [person] + WANT + THING).
 *      Several templates can fit one message; they are returned in a default order and
 *      ErakaTemplateStats re-orders them by what this child accepts.
 *   3. SimpleNLG does the GRAMMAR: a/an, "to" before a second verb, am/is/wants,
 *      plurals, questions, imperatives.
 *
 * Only the child's own pictograms supply content words; the templates add grammar words
 * only. When no template fits (e.g. prepositions, two actions), nothing is suggested and
 * the child's words are spoken as they are.
 *
 * Pure Java (SimpleNLG has no Android code), unit-tested on the PC.
 */
public final class ErakaSentenceBuilder {

    // ------------------------------------------------------------------ inputs and outputs

    /** One pictogram of the message, as the builder needs it. */
    public static final class Word {
        public final int id;
        public final String label; // English label as shown
        public final int tipo;     // OTTAA word type 1..6
        /** The picture's category from the board's groups (FOOD, TOY, CLOTHING, ANIMAL, COLOR,
         *  FEELING, PLACE, OTHER) or null when unknown. Decides e.g. "I want an apple." (food)
         *  versus "Look, a horse!" (animal). */
        public final String category;

        public Word(int id, String label, int tipo) {
            this(id, label, tipo, null);
        }

        public Word(int id, String label, int tipo, String category) {
            this.id = id;
            this.label = label == null ? "" : label.trim();
            this.tipo = tipo;
            this.category = category;
        }
    }

    /** One suggested sentence and the template that produced it. */
    public static final class Candidate {
        public final String templateId;
        public final String text;
        /** Template + the kinds of pictures it was used for, e.g. "want_action@ACTION+THING".
         *  Learning is per key, so ignoring a sentence for "drink water" does not change
         *  the choice for "go school". */
        public final String key;

        Candidate(String templateId, String text) {
            this(templateId, text, templateId);
        }

        Candidate(String templateId, String text, String key) {
            this.templateId = templateId;
            this.text = text;
            this.key = key;
        }

        @Override
        public String toString() {
            return templateId + ": " + text;
        }
    }

    enum Role { PERSON, WANT, LIKE, DISLIKE, BE, ACTION, DESCRIBE, THING, PLACE, NUMBER, SOCIAL, HELP, QUESTION, OTHER }

    public static final String CAT_FOOD = "FOOD", CAT_TOY = "TOY", CAT_CLOTHING = "CLOTHING", CAT_ANIMAL = "ANIMAL",
            CAT_COLOR = "COLOR", CAT_FEELING = "FEELING", CAT_PLACE = "PLACE", CAT_OTHER = "OTHER";

    // ------------------------------------------------------------------ word lists

    static final Set<String> WANT_WORDS = set("want", "need");
    static final Set<String> PRONOUN_SUBJECTS = set("i", "you", "he", "she", "we", "they");
    static final Set<String> PLACES = set("school", "house", "home", "hospital", "market", "supermarket", "park",
            "garden", "bathroom", "kitchen", "room", "restaurant", "city", "airport", "field", "square", "yard",
            "church", "shop", "bar", "dining room", "pizza shop", "burger shop", "poultry shop", "greengrocery",
            "bike shop", "movie theater", "bed", "toilet", "class", "playground", "clinic");
    /** Places said without "the": "to school", "to bed". */
    static final Set<String> PLACES_NO_ARTICLE = set("school", "home", "bed", "church", "class");
    static final Set<String> MASS = set("water", "milk", "juice", "tea", "coffee", "rice", "soup", "bread", "meat",
            "beef", "sugar", "salt", "oil", "butter", "cheese", "pasta", "porridge", "oshifima", "mahangu",
            "chocolate", "ice cream", "yoghurt", "fruit", "food", "candy", "popcorn", "ketchup", "mayo", "money",
            "music", "salad", "vegetables", "chocolate milk", "fish", "chicken", "poultry", "toast", "jelly",
            "cappuccino", "soda", "help", "paper", "glue", "medicine", "syrup", "kapana", "dessert", "chess",
            "cooldrink", "porridge", "cereal", "oats", "weetbix", "pap", "biltong", "droewors", "braai", "homework");
    static final Set<String> NUMBERS = set("one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
            "more");                                   // "more food", "I want more."
    static final Set<String> FEELINGS = set("happy", "sad", "tired", "angry", "scared", "bored", "nervous", "worried",
            "upset", "anxious", "sick", "irritated", "embarrassed", "good", "bad", "so so", "hungry", "thirsty",
            "cold", "hot", "funny", "sorry", "hurt", "finished");
    /** Verbs a person can be asked to do for the child: "Mom, give me juice." */
    static final Set<String> REQUEST_VERBS = set("give", "open", "close", "turn on", "turn off", "bring", "show", "come");
    /** Request verbs that hand something to the child: "give ME juice". */
    static final Set<String> GIVE_VERBS = set("give", "bring", "show");
    /** Things a child HAS, not wants: "I have a headache.", "I am in pain." */
    static final Set<String> ACHES = set("pain", "headache", "toothache", "earache", "stomachache", "chest pain",
            "backache", "fever", "cough", "cold", "flu");
    static final Set<String> COLORS = set("yellow", "blue", "red", "green", "white", "black", "pink", "orange",
            "purple", "brown", "gray", "grey", "gold", "silver");
    /** Social words that open the sentence ("Yes, I want it."); the others close it ("..., please."). */
    static final Set<String> LEAD_WORDS = set("yes", "no", "hello", "hi", "okay", "ok");
    /** Greetings that can be said to one person or thing: "Bye, horse!", "Hello, mom!" */
    static final Set<String> GREETINGS = set("hello", "hi", "bye", "goodbye", "good morning", "good afternoon",
            "good night", "thank you", "thanks", "see you", "stop");
    static final Set<String> QUESTION_WORDS = set("where", "who", "what");
    /** Time words (24 Oct 2026): "yesterday" puts the sentence in the past, "tomorrow" in the
     *  future; the word itself goes at the end ("I went to church yesterday."). */
    static final Set<String> PAST_WORDS = set("yesterday", "last night", "this morning");
    static final Set<String> FUTURE_WORDS = set("tomorrow", "later", "tonight");
    static final Set<String> NOW_WORDS = set("today", "now");
    /** Labels whose OTTAA word type is wrong for sentence building. */
    static final Set<String> FORCE_ACTION = set("cook");
    static final Set<String> FORCE_DESCRIBE = set("hungry", "sorry", "finished");

    private static Set<String> set(String... words) {
        return new HashSet<>(Arrays.asList(words));
    }

    static Role roleOf(Word w) {
        String l = key(w.label);
        if (l.isEmpty()) return Role.OTHER;
        if (l.equals("help") || l.equals("help me")) return Role.HELP;   // verb, noun or social word in OTTAA;
                                                                          // "Help" (385) is renamed "help me"
        if (QUESTION_WORDS.contains(l)) return Role.QUESTION;
        if (WANT_WORDS.contains(l)) return Role.WANT;
        if (l.equals("i like")) return Role.LIKE;
        if (l.equals("i don't like")) return Role.DISLIKE;
        if (l.equals("be")) return Role.BE;
        if (FORCE_ACTION.contains(l)) return Role.ACTION;
        if (FORCE_DESCRIBE.contains(l)) return Role.DESCRIBE;
        if (NUMBERS.contains(l)) return Role.NUMBER;
        if (PRONOUN_SUBJECTS.contains(l)) return Role.PERSON;
        if (PLACES.contains(l)) return Role.PLACE;
        switch (w.tipo) {
            case 1: return Role.PERSON;
            case 2: return Role.THING;
            case 3: return Role.ACTION;
            case 4: return Role.DESCRIBE;
            case 5: return Role.SOCIAL;
            default: return Role.OTHER;
        }
    }

    // ------------------------------------------------------------------ SimpleNLG

    private final NLGFactory f;
    private final Realiser r;
    private final Lexicon lexicon;

    /** Loading the lexicon takes about 2 s on a low-cost phone: create once, in the background. */
    public ErakaSentenceBuilder() {
        this(Lexicon.getDefaultLexicon());
    }

    public ErakaSentenceBuilder(Lexicon lexicon) {
        this.lexicon = lexicon;
        f = new NLGFactory(lexicon);
        r = new Realiser(lexicon);
    }

    // ------------------------------------------------------------------ building

    /** Time word of the message being built ("yesterday"), or null; set by build(). */
    private String timeWord;
    /** simplenlg.features.Tense.PAST / FUTURE, or null for the present. */
    private Object timeTense;

    /**
     * Realises a clause in the message's tense. Questions are not made about yesterday or
     * tomorrow ("Can I have an apple yesterday?"), so they are dropped when a time word is used.
     */
    private String realiseS(NLGElement e) {
        if (timeWord != null && e instanceof SPhraseSpec) {
            SPhraseSpec s = (SPhraseSpec) e;
            if (s.getFeature(Feature.INTERROGATIVE_TYPE) != null) return "";
            // "I want milk tomorrow.", not "I will want milk tomorrow."
            boolean wish = false;
            if (timeTense == simplenlg.features.Tense.FUTURE && s.getVerb() != null) {
                NLGElement verb = s.getVerb();
                String v = verb instanceof simplenlg.framework.WordElement
                        ? ((simplenlg.framework.WordElement) verb).getBaseForm() : "";
                wish = v.equals("want") || v.equals("need") || v.equals("like");
            }
            if (timeTense != null && !wish) s.setFeature(Feature.TENSE, timeTense);
        }
        return r.realiseSentence(e);
    }

    /**
     * All sentences that fit the message, best default first. Empty when no template fits
     * (the child's words are then spoken as they are).
     */
    public synchronized List<Candidate> build(List<Word> message) {
        timeWord = null;
        timeTense = null;
        if (message == null || message.isEmpty()) return new ArrayList<>();
        List<Word> rest = new ArrayList<>();
        for (Word w : message) {
            String k = key(w.label);
            boolean past = PAST_WORDS.contains(k), future = FUTURE_WORDS.contains(k), now = NOW_WORDS.contains(k);
            if (past || future || now) {
                if (timeWord != null) return new ArrayList<>();      // two time words: too complex
                timeWord = k;
                timeTense = past ? simplenlg.features.Tense.PAST : future ? simplenlg.features.Tense.FUTURE : null;
            } else {
                rest.add(w);
            }
        }
        List<Candidate> built = buildMessage(rest);
        if (timeWord == null) return built;
        // with a time word, saying what happens ("I went to church yesterday.") beats wanting it
        List<Candidate> first = new ArrayList<>(), later = new ArrayList<>();
        for (Candidate c : built) (c.templateId.startsWith("do_") || c.templateId.equals("see_thing") ? first : later).add(c);
        first.addAll(later);
        return first;
    }

    private List<Candidate> buildMessage(List<Word> message) {
        List<Candidate> out = new ArrayList<>();
        if (message == null || message.isEmpty()) return out;

        List<Word> words = new ArrayList<>();
        List<Role> roles = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (Word w : message) {
            if (!seen.add(w.id)) continue;             // repeated picture: counted once
            Role ro = roleOf(w);
            if (ro == Role.OTHER) return out;          // prepositions, conjunctions...: too complex
            words.add(w);
            roles.add(ro);
        }

        // only social expressions: "Hello!", "Thank you."
        boolean allSocial = true;
        for (Role ro : roles) allSocial &= ro == Role.SOCIAL;
        if (allSocial) {
            StringBuilder b = new StringBuilder();
            for (Word w : words) b.append(b.length() == 0 ? "" : ", ").append(w.label);
            out.add(new Candidate("social", finish(b.toString(), "Hello".equalsIgnoreCase(words.get(0).label) ? "!" : ".")));
            return out;
        }

        // a greeting said to one person or thing: "Bye, horse!", "Hello, mom!"
        if (words.size() == 2) {
            int g = roles.get(0) == Role.SOCIAL ? 0 : roles.get(1) == Role.SOCIAL ? 1 : -1;
            if (g >= 0) {
                Word other = words.get(1 - g);
                Role otherRole = roles.get(1 - g);
                String greet = key(words.get(g).label);
                if (GREETINGS.contains(greet) && (otherRole == Role.PERSON || otherRole == Role.THING)
                        && !PRONOUN_SUBJECTS.contains(key(other.label))) {
                    out.add(new Candidate("greet", capital(greet) + ", " + key(other.label) + "!", "greet@" + otherRole.name()));
                    return out;
                }
            }
        }

        // social words: yes/no/hello open the sentence ("Yes, I want it."), the others
        // close it ("I want water, please.")
        String lead = null, tail = null;
        for (int i = words.size() - 1; i >= 0; i--) {
            if (roles.get(i) != Role.SOCIAL) continue;
            String k = key(words.get(i).label);
            if (LEAD_WORDS.contains(k)) {
                if (lead != null) return out;
                lead = k;
            } else {
                if (tail != null) return out;
                tail = k;
            }
            words.remove(i);
            roles.remove(i);
        }
        List<Candidate> core = buildCore(words, roles, tail, lead);
        if (lead == null) return core;
        for (Candidate c : core) out.add(new Candidate(c.templateId, capital(lead) + ", " + lowerFirst(c.text), c.key));
        return out;
    }

    private List<Candidate> buildCore(List<Word> words, List<Role> roles, String tail, String lead) {
        List<Candidate> out = new ArrayList<>();
        if (words.isEmpty()) return out;
        boolean negate = "no".equals(lead);                       // "No, I don't want milk."
        boolean answer = "yes".equals(lead) || negate;

        // subject: a person at the start ("mom want juice"); otherwise "I"
        String subject = "I";
        String person = null;                          // first non-subject person (vocative / object)
        int start = 0;
        if (roles.get(0) == Role.PERSON) {
            subject = norm(words.get(0).label);
            start = 1;
        } else if (roles.contains(Role.DESCRIBE) && count(roles, Role.PERSON) == 1) {
            // order does not matter for describing words: "happy mom" = "mom happy"
            int p = roles.indexOf(Role.PERSON);
            subject = norm(words.get(p).label);
            words.add(0, words.remove(p));
            roles.add(0, roles.remove(p));
            start = 1;
        }
        String pattern = pattern(roles);
        Role want = null;
        String action = null;
        List<String> things = new ArrayList<>();
        List<Boolean> thingIsMass = new ArrayList<>();
        List<String> thingCat = new ArrayList<>();
        String place = null;
        String describe = null;
        String describeCat = null;
        String number = null;
        String question = null;
        boolean be = false;
        boolean help = false;
        for (int i = start; i < words.size(); i++) {
            String l = norm(words.get(i).label);
            switch (roles.get(i)) {
                case WANT: case LIKE: case DISLIKE:
                    if (want != null) return out;
                    want = roles.get(i);
                    break;
                case BE: be = true; break;
                case ACTION:
                    if (action != null) return out;    // two actions: too complex
                    action = l;
                    break;
                case DESCRIBE:
                    if (describe != null) return out;
                    describe = l;
                    describeCat = words.get(i).category;
                    break;
                case THING:
                    things.add(l);
                    thingIsMass.add(isMass(l));
                    thingCat.add(words.get(i).category);
                    break;
                case PERSON:
                    if (person != null) return out;
                    person = l;
                    break;
                case PLACE:
                    if (place != null) return out;
                    place = l;
                    break;
                case NUMBER: number = l; break;
                case HELP:
                    if (help) return out;
                    help = true;
                    break;
                case QUESTION:
                    if (question != null) return out;
                    question = key(l);
                    break;
                default: return out;
            }
        }
        boolean childSubject = subject.equalsIgnoreCase("I");

        // ----- question: "Where is the milk?", "Where is mom?", "What is it?"
        if (question != null) {
            if (want != null || action != null || describe != null || help || number != null || be) return out;
            int n = things.size() + (person != null ? 1 : 0) + (place != null ? 1 : 0) + (start == 1 ? 1 : 0);
            if (n > 1) return out;
            String target = start == 1 ? subject : person != null ? person : !things.isEmpty() ? things.get(0) : place;
            if (target == null) {
                add(out, pattern, "question_it", tail, capital(question) + " is it?");
            } else if (question.equals("where")) {
                boolean bare = start == 1 || person != null;
                if (target.equals("I")) add(out, pattern, "where_is", tail, "Where am I?");
                else if (target.equals("you")) add(out, pattern, "where_is", tail, "Where are you?");
                else add(out, pattern, "where_is", tail, "Where " + (isPlural(target) && !bare ? "are " : "is ")
                        + (bare ? target : the(target)) + "?");
            }
            return out;                                // "what ball": too unclear
        }

        boolean personObject = false;
        if (person != null && !help && (action == null || !REQUEST_VERBS.contains(action))) {
            things.add(person);                        // "I want mom."
            thingIsMass.add(true);                     // no article before a person
            thingCat.add(null);
            person = null;
            personObject = true;
        }
        if (things.size() > 2) return out;

        // ----- help: "Can you help me with the oven?", "Mom, help me open the door."
        if (help) {
            if ((want != null && want != Role.WANT) || describe != null || place != null || number != null || be
                    || things.size() > 1) return out;
            String helper = !childSubject ? subject : person;
            if (!childSubject && person != null) return out;
            String what = things.isEmpty() ? null : things.get(0);
            String rest = action != null ? " " + action + (what == null ? "" : " " + the(what))
                    : what != null ? " with " + the(what) : "";
            if (negate) {
                add(out, pattern, "help_need", tail, "I don't need help" + (action != null ? " to" + rest : rest) + ".");
            } else if (helper != null) {
                add(out, pattern, "help_request", tail, capital(helper) + ", help me" + rest + ".");
                add(out, pattern, "help_can_you", tail, capital(helper) + ", can you help me" + rest + "?");
            } else if (action == null && what == null) {
                add(out, pattern, "help_me", tail, tail == null ? "Help me, please." : "Help me.");
                add(out, pattern, "help_need", tail, "I need help.");
                add(out, pattern, "help_can_you", tail, "Can you help me?");
            } else {
                add(out, pattern, "help_can_you", tail, "Can you help me" + rest + "?");
                add(out, pattern, "help_need", tail, "I need help" + (action != null ? " to" + rest : rest) + ".");
            }
            return out;
        }

        // ----- describing word: "I am tired." / "It is yellow." / "I want a yellow ball."
        if (describe != null) {
            if (action != null || place != null || want == Role.LIKE || want == Role.DISLIKE) return out;
            boolean feeling = FEELINGS.contains(describe) || CAT_FEELING.equals(describeCat);
            if (!things.isEmpty()) {
                if (things.size() > 1 || personObject || !childSubject) return out;
                String t = things.get(0);
                NPPhraseSpec n = np(t, thingIsMass.get(0), number, false);
                n.addPreModifier(describe);
                SPhraseSpec w = f.createClause("I", "want", n);
                if (negate) w.setFeature(Feature.NEGATED, true);
                String is = capital(the(t)) + (isPlural(t) ? " are " : " is ") + (negate ? "not " : "") + describe + ".";
                if (want != null || wantable(thingIsMass.get(0), thingCat.get(0))) {
                    add(out, pattern, "want_describe_thing", tail, realiseS(w));
                    if (want == null) add(out, pattern, "thing_is", tail, is);
                } else {
                    add(out, pattern, "thing_is", tail, is);
                }
                return out;
            }
            if (want != null) return out;
            if (start == 0 && !be && !feeling) {       // a colour or size is not how the child is
                add(out, pattern, "it_describe", tail, "It is " + (negate ? "not " : "") + describe + ".");
                return out;
            }
            boolean color = COLORS.contains(describe) || CAT_COLOR.equals(describeCat);
            if (color && childSubject && !be) {        // "I + yellow": a favourite colour, not "I am yellow"
                add(out, pattern, "like_color", tail, negate ? "I don't like " + describe + "." : "I like " + describe + ".");
                return out;
            }
            add(out, pattern, "be_describe", tail, clause(subject, "be", null, describe, negate));
            if (!be && FEELINGS.contains(describe) && !describe.equals("sorry"))
                add(out, pattern, "feel_describe", tail, clause(subject, "feel", null, describe, negate));
            return out;
        }
        if (be) return out;                            // "be" without a describing word

        // ----- like / don't like: "I like apples."
        if (want == Role.LIKE || want == Role.DISLIKE) {
            if (place != null || (action == null && things.isEmpty())) return out;
            SPhraseSpec s = f.createClause("I", "like");
            if (action != null) s.addComplement(infinitive(action, objects(things, thingIsMass, number, true)));
            else s.setObject(objects(things, thingIsMass, number, true));
            if (want == Role.DISLIKE) s.setFeature(Feature.NEGATED, true);
            add(out, pattern, want == Role.LIKE ? "like" : "dislike", tail, realiseS(s));
            return out;
        }

        // ----- request to a person: "Mom, give me juice."
        if (person != null && action != null && REQUEST_VERBS.contains(action) && place == null) {
            add(out, pattern, "request", tail, capital(person) + ", " + lowerFirst(request(action, things, thingIsMass, number, thingCat)));
            return out;
        }
        // person first + request verb ("dad give juice"): the person is asked, not the subject
        if (!childSubject && action != null && REQUEST_VERBS.contains(action) && want == null && place == null) {
            add(out, pattern, "request", tail, capital(subject) + ", " + lowerFirst(request(action, things, thingIsMass, number, thingCat)));
            add(out, pattern, "do_action", tail, clause(subject, action, things.isEmpty() ? null : objects(things, thingIsMass, number, false), null, false));
            return out;
        }
        // request verb with no person: "Give me milk and cookies." / "Can you give me milk and cookies?"
        if (childSubject && action != null && REQUEST_VERBS.contains(action) && want == null && place == null
                && !negate && !personObject && (!things.isEmpty() || !GIVE_VERBS.contains(action))) {
            add(out, pattern, "request_me", tail, request(action, things, thingIsMass, number, thingCat));
            SPhraseSpec q = f.createClause("you", verbHead(action));
            if (GIVE_VERBS.contains(action)) q.setIndirectObject("me");
            if (!things.isEmpty()) q.setObject(requestObject(action, things, thingIsMass, number, thingCat));
            addRest(q, action);
            if (action.equals("come") && things.isEmpty()) q.addComplement("here");
            q.setFeature(Feature.MODAL, "can");
            q.setFeature(Feature.INTERROGATIVE_TYPE, InterrogativeType.YES_NO);
            add(out, pattern, "can_you", tail, realiseS(q));
            return out;
        }

        // ----- action (with or without "want"): "I want to eat an apple." / "I eat an apple."
        if (action != null) {
            NLGElement obj = things.isEmpty() ? null : objects(things, thingIsMass, number, false);
            NLGElement where = place == null ? null : toPlace(place);
            if (want == Role.WANT || want == null) {
                SPhraseSpec s = f.createClause(subject, "want");
                VPPhraseSpec v = infinitive(action, obj);
                if (where != null) v.addComplement(where);
                s.addComplement(v);
                if (negate) s.setFeature(Feature.NEGATED, true);
                add(out, pattern, "want_action", tail, realiseS(s));
            }
            if (want == null && !negate) {
                SPhraseSpec s = f.createClause(subject, verbHead(action));
                addRest(s, action);
                if (obj != null && action.equals("play")) s.addComplement(f.createPrepositionPhrase("with", obj));
                else if (obj != null) s.setObject(obj);
                if (where != null) s.addComplement(where);
                add(out, pattern, "do_action", tail, realiseS(s));
            }
            return out;
        }

        // ----- place only: "I want to go to school."
        if (place != null) {
            if (!things.isEmpty()) return out;
            SPhraseSpec s = f.createClause(subject, "want");
            VPPhraseSpec v = f.createVerbPhrase("go");
            v.setFeature(Feature.FORM, Form.INFINITIVE);
            v.addComplement(toPlace(place));
            s.addComplement(v);
            if (negate) s.setFeature(Feature.NEGATED, true);
            add(out, pattern, "want_go_place", tail, realiseS(s));
            if (timeTense != null) {                   // "I went to church yesterday."
                SPhraseSpec g = f.createClause(subject, "go");
                g.addComplement(toPlace(place));
                if (negate) g.setFeature(Feature.NEGATED, true);
                add(out, pattern, "do_go_place", tail, realiseS(g));
            }
            return out;
        }

        // ----- aches: "I have a headache." / "I am in pain." (not "I want a headache.")
        if (things.size() == 1 && ACHES.contains(things.get(0)) && want == null && !personObject) {
            String t = things.get(0);
            String sentence;
            if (t.equals("pain")) sentence = clause(subject, "be", null, "in pain", negate);
            else sentence = clause(subject, "have", np(t, t.equals("fever") || t.equals("flu"), number, false), null, negate);
            add(out, pattern, "have_ache", tail, sentence);
            return out;
        }

        // ----- things: "I want water." / "Can I have an apple?" / "Look, a horse!"
        if (!things.isEmpty()) {
            NLGElement obj = objects(things, thingIsMass, number, false);
            boolean canAsk = want != null || personObject || !childSubject;
            for (int i = 0; i < things.size(); i++) canAsk |= wantable(thingIsMass.get(i), thingCat.get(i));
            if (!canAsk) {
                // an animal or an object the child is not likely to ask for: comment on it
                if (negate || things.size() > 1) return out;
                if (timeWord != null) {
                    add(out, pattern, "see_thing", tail, realiseS(f.createClause("I", "see",
                            np(things.get(0), thingIsMass.get(0), number, false))));
                    return out;
                }
                String np = r.realise(np(things.get(0), thingIsMass.get(0), number, false)).getRealisation();
                add(out, pattern, "look_thing", tail, "Look, " + np + "!");
                add(out, pattern, "see_thing", tail, "I see " + np + ".");
                return out;
            }
            SPhraseSpec s = f.createClause(subject, "want", obj);
            if (negate) s.setFeature(Feature.NEGATED, true);
            add(out, pattern, "want_thing", tail, realiseS(s));
            if (answer) return out;                    // "Yes, I want milk." – the answer is enough
            if (childSubject && !personObject) {
                SPhraseSpec q = f.createClause("I", "have", objects(things, thingIsMass, number, false));
                q.setFeature(Feature.MODAL, "can");
                q.setFeature(Feature.INTERROGATIVE_TYPE, InterrogativeType.YES_NO);
                add(out, pattern, "can_have", tail, realiseS(q));
                if (want == null && tail == null && number == null && things.size() == 1 && timeWord == null) {
                    out.add(new Candidate("thing_please", capital(things.get(0)) + ", please.", "thing_please@" + pattern));
                }
            } else if (want == null) {
                add(out, pattern, "vocative_want", tail, capital(subject) + ", " + lowerFirst(realiseS(f.createClause("I", "want", objects(things, thingIsMass, number, false)))));
            }
            return out;
        }

        // ----- "more" with nothing else: "I want more."
        if ("more".equals(number) && action == null && place == null && childSubject) {
            add(out, pattern, "want_more", tail, negate ? "I don't want more." : "I want more.");
            return out;
        }

        // ----- "want" alone, as an answer: "Yes, I want it." / "No, I don't want it."
        if (want == Role.WANT && answer && childSubject) {
            add(out, pattern, "want_it", tail, negate ? "I don't want it." : "I want it.");
        }
        return out;                                    // e.g. "want" alone: no sentence
    }

    /** "Give me milk." / "Open the door." (imperative, no subject) */
    private String request(String action, List<String> things, List<Boolean> mass, String number, List<String> cats) {
        SPhraseSpec s = f.createClause();
        s.setVerb(verbHead(action));
        if (action.equals("come") && things.isEmpty()) s.addComplement("here");
        if (GIVE_VERBS.contains(action)) s.setIndirectObject("me");
        if (!things.isEmpty()) s.setObject(requestObject(action, things, mass, number, cats));
        addRest(s, action);
        s.setFeature(Feature.FORM, Form.IMPERATIVE);
        return realiseS(s);
    }

    /** "give me AN apple" but "open THE door": things handed over are any one, things acted on are that one. */
    private NLGElement requestObject(String action, List<String> things, List<Boolean> mass, String number,
                                     List<String> cats) {
        // "give me AN apple" (any one), but "give me THE phone" (an object, not food or a toy)
        boolean definite = true;
        for (int i = 0; i < things.size(); i++) definite &= !wantable(mass.get(i), cats.get(i)) && !mass.get(i);
        if (number != null || (GIVE_VERBS.contains(action) && !definite)) return objects(things, mass, number, false);
        if (things.size() == 1) {
            NPPhraseSpec n = f.createNounPhrase(things.get(0));
            n.setDeterminer("the");
            return n;
        }
        CoordinatedPhraseElement c = f.createCoordinatedPhrase();
        for (String t : things) {
            NPPhraseSpec n = f.createNounPhrase(t);
            n.setDeterminer("the");
            c.addCoordinate(n);
        }
        return c;
    }

    /** Food, toys and clothes are things a child asks for; animals and other objects are not. */
    private static boolean wantable(boolean mass, String category) {
        if (category == null) return true;             // unknown category: as before
        return category.equals(CAT_FOOD) || category.equals(CAT_TOY) || category.equals(CAT_CLOTHING);
    }

    // ------------------------------------------------------------------ helpers

    private NLGElement objects(List<String> things, List<Boolean> mass, String number, boolean likeForm) {
        if (things.size() == 1) return np(things.get(0), mass.get(0), number, likeForm);
        CoordinatedPhraseElement c = f.createCoordinatedPhrase();
        for (int i = 0; i < things.size(); i++) c.addCoordinate(np(things.get(i), mass.get(i), i == 0 ? number : null, likeForm));
        return c;
    }

    private NPPhraseSpec np(String noun, boolean mass, String number, boolean likeForm) {
        NPPhraseSpec n = f.createNounPhrase(noun);
        // "shoes": the lexicon turns it into "shoe", so ask for the plural again; words it does
        // not know ("chestnuts") are kept exactly as written
        if (isPlural(noun) && !noun.contains(" and ") && !noun.contains(" ")
                && lexicon.hasWordFromVariant(noun) && !lexicon.hasWord(noun)) n.setPlural(true);
        if ("more".equals(number) && mass) {
            n.setDeterminer("more");                   // "more food", "more water"
        } else if (number != null && !mass) {
            if (!number.equals("one")) {
                n.setDeterminer(number);
                n.setPlural(true);
            } else {
                n.setDeterminer("one");
            }
        } else if (likeForm && !mass) {
            n.setPlural(true);                         // "I like apples."
        } else if (!mass) {
            n.setDeterminer("a");                      // "a" becomes "an" before a vowel
        }
        return n;
    }

    private PPPhraseSpec toPlace(String place) {
        NPPhraseSpec n = f.createNounPhrase(place);
        if (!PLACES_NO_ARTICLE.contains(place)) n.setDeterminer("the");
        return f.createPrepositionPhrase("to", n);
    }

    /** "brush my teeth" -> verb "brush" + complement "my teeth". */
    private VPPhraseSpec infinitive(String action, NLGElement object) {
        VPPhraseSpec v = f.createVerbPhrase(verbHead(action));
        String rest = verbRest(action);
        if (rest != null) v.addComplement(rest);
        if (object != null && action.equals("play")) v.addComplement(f.createPrepositionPhrase("with", object)); // "play with a ball"
        else if (object != null) v.setObject(object);
        v.setFeature(Feature.FORM, Form.INFINITIVE);
        return v;
    }

    private void addRest(SPhraseSpec s, String action) {
        String rest = verbRest(action);
        if (rest != null) s.addComplement(rest);
    }

    private static String verbHead(String action) {
        int sp = action.indexOf(' ');
        return sp < 0 ? action : action.substring(0, sp);
    }

    private static String verbRest(String action) {
        int sp = action.indexOf(' ');
        return sp < 0 ? null : action.substring(sp + 1);
    }

    private String clause(String subject, String verb, NLGElement object, String complement, boolean negate) {
        SPhraseSpec s = f.createClause(subject, verbHead(verb));
        addRest(s, verb);
        if (object != null) s.setObject(object);
        if (complement != null) s.addComplement(complement);
        if (negate) s.setFeature(Feature.NEGATED, true);
        return realiseS(s);
    }

    /** "the oven", but "mom", "school", "bed" without "the". */
    private static String the(String noun) {
        if (PLACES_NO_ARTICLE.contains(noun) || noun.equals("I") || PRONOUN_SUBJECTS.contains(noun)) return noun;
        return "the " + noun;
    }

    static boolean isPlural(String noun) {
        if (noun.contains(" and ")) return true;
        String last = noun.substring(noun.lastIndexOf(' ') + 1);
        return last.length() > 3 && last.endsWith("s") && !last.endsWith("ss") && !last.endsWith("us");
    }

    /** Lower case without a trailing "?" or "!": "Where?" -> "where". */
    static String key(String label) {
        return label == null ? "" : label.toLowerCase(Locale.ROOT).replaceAll("[?!.]+$", "").trim();
    }

    private void add(List<Candidate> out, String pattern, String id, String tail, String sentence) {
        if (sentence == null || sentence.isEmpty()) return;
        sentence = sentence.replace("do not ", "don't ").replace("does not ", "doesn't ")    // child-friendly
                .replace("did not ", "didn't ").replace("will not ", "won't ");
        if (timeWord != null) {
            String body = sentence.replaceAll("[.!?]$", "");
            sentence = body + " " + timeWord + (sentence.endsWith("!") ? "!" : ".");
        }
        if (tail != null) {
            String body = sentence.replaceAll("[.!?]$", "");
            String end = sentence.endsWith("?") ? "?" : ".";
            sentence = body + ", " + tail + end;
        }
        out.add(new Candidate(id, sentence, id + "@" + pattern));
    }

    private static int count(List<Role> roles, Role r) {
        int n = 0;
        for (Role x : roles) if (x == r) n++;
        return n;
    }

    /** The kinds of pictures in the message, e.g. "ACTION+THING" (order and repeats ignored). */
    private static String pattern(List<Role> roles) {
        java.util.TreeSet<String> set = new java.util.TreeSet<>();
        for (Role r : roles) if (r != Role.SOCIAL) set.add(r.name());
        return String.join("+", set);
    }

    static boolean isMass(String noun) {
        if (MASS.contains(noun)) return true;
        if (noun.contains(" and ")) return true;                    // "milk and cookies"
        String last = noun.substring(noun.lastIndexOf(' ') + 1);
        if (last.length() > 3 && last.endsWith("s") && !last.endsWith("ss") && !last.endsWith("us")) return true; // plural: "cookies", "chestnuts"
        int sp = noun.lastIndexOf(' ');
        return sp >= 0 && MASS.contains(noun.substring(sp + 1));   // "apple juice"
    }

    private static String norm(String label) {
        return label.equals("I") ? "I" : label.toLowerCase(Locale.ROOT).trim();
    }

    private static String capital(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static String lowerFirst(String s) {
        if (s.isEmpty() || s.startsWith("I ") || s.startsWith("I'")) return s;
        return Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    private static String finish(String s, String end) {
        s = capital(s.trim());
        return s.matches(".*[.!?]$") ? s : s + end;
    }
}
