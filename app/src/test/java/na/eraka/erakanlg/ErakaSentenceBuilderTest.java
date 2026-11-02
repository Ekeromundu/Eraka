package na.eraka.erakanlg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import na.eraka.erakanlg.ErakaSentenceBuilder.Candidate;
import na.eraka.erakanlg.ErakaSentenceBuilder.Word;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Sentence templates + SimpleNLG. Run on the PC like the other tests. */
public class ErakaSentenceBuilderTest {

    private static ErakaSentenceBuilder b;

    @BeforeClass
    public static void load() {
        b = new ErakaSentenceBuilder();
    }

    // real ids, labels and word types from the vocabulary (tipo 1 person, 2 thing, 3 action, 4 describing, 5 social, 6 other)
    static final Word I = new Word(643, "I", 1), YOU = new Word(642, "you", 1), MOM = new Word(628, "mom", 1),
            DAD = new Word(632, "dad", 1), WANT = new Word(44, "want", 3), EAT = new Word(118, "eat", 3),
            DRINK = new Word(51, "drink", 3), PLAY = new Word(34, "play", 3), GO = new Word(30, "go", 3),
            GIVE = new Word(14, "give", 3), BE = new Word(22, "be", 3), BRUSH = new Word(11, "brush my teeth", 3),
            WATER = new Word(500, "water", 2), APPLE = new Word(474, "apple", 2), JUICE = new Word(497, "juice", 2),
            BALL = new Word(996, "ball", 2), MILK = new Word(495, "milk", 2), DOG = new Word(342, "dog", 2),
            SCHOOL = new Word(662, "school", 2), HOUSE = new Word(653, "house", 2), MARKET = new Word(675, "market", 2),
            HAPPY = new Word(171, "happy", 4), TIRED = new Word(137, "tired", 4), HELLO = new Word(377, "Hello", 5),
            PLEASE = new Word(383, "Please", 5), THANKS = new Word(384, "Thank you", 5), TWO = new Word(837, "two", 6),
            WITH = new Word(528, "with", 6), ILIKE = new Word(901, "I like", 6), IDONT = new Word(907, "I don't like", 6);

    private static List<String> texts(Word... w) {
        List<String> out = new ArrayList<>();
        for (Candidate c : b.build(Arrays.asList(w))) out.add(c.text);
        System.out.println(Arrays.toString(w.length == 0 ? new Word[0] : w).length() > 0 ? label(w) + " -> " + out : "");
        return out;
    }

    private static String label(Word... w) {
        StringBuilder s = new StringBuilder();
        for (Word x : w) s.append(s.length() == 0 ? "" : " + ").append(x.label);
        return s.toString();
    }

    private static String first(Word... w) {
        List<String> t = texts(w);
        return t.isEmpty() ? null : t.get(0);
    }

    @Test public void wantThing() {
        assertEquals("I want water.", first(I, WANT, WATER));
        assertEquals("I want an apple.", first(I, WANT, APPLE));
        assertEquals("I want a dog.", first(WANT, DOG));               // no "I": the child is the subject
        assertEquals("I want milk.", first(MILK));                     // thing alone
        assertTrue(texts(APPLE).contains("Can I have an apple?"));
        assertTrue(texts(APPLE).contains("Apple, please."));
    }

    @Test public void wantAction() {
        assertEquals("I want to eat.", first(I, WANT, EAT));
        assertEquals("I want to eat.", first(EAT));
        assertEquals("I want to play with a ball.", first(I, WANT, PLAY, BALL));
        assertEquals("I want to drink water.", first(DRINK, WATER));
        assertTrue(texts(DRINK, WATER).contains("I drink water."));
        assertEquals("I want to brush my teeth.", first(BRUSH));
    }

    @Test public void neverWantToWant() {
        assertTrue(texts(I, WANT).isEmpty());
        assertTrue(texts(WANT).isEmpty());
        for (String s : texts(WANT, EAT)) assertTrue(s, !s.contains("want to want"));
    }

    @Test public void places() {
        assertEquals("I want to go to school.", first(I, WANT, GO, SCHOOL));
        assertEquals("I want to go to school.", first(GO, SCHOOL));
        assertEquals("I want to go to the market.", first(MARKET));
        assertTrue(texts(GO, SCHOOL).contains("I go to school."));
    }

    @Test public void feelings() {
        assertEquals("I am tired.", first(I, TIRED));
        assertEquals("Mom is happy.", first(MOM, HAPPY));
        assertTrue(texts(I, HAPPY).contains("I feel happy."));
        assertEquals("I am tired.", first(I, BE, TIRED));
    }

    @Test public void otherPeople() {
        assertEquals("Dad, give me juice.", first(DAD, GIVE, JUICE));
        assertEquals("Mom wants water.", first(MOM, WANT, WATER));
        assertTrue(texts(MOM, WATER).contains("Mom, I want water."));
        assertEquals("You want an apple.", first(YOU, WANT, APPLE));
        assertEquals("I want mom.", first(I, WANT, MOM));
        assertTrue(!texts(I, WANT, MOM).contains("Can I have mom?"));
    }

    @Test public void numbersAndTwoThings() {
        assertEquals("I want two apples.", first(WANT, TWO, APPLE));
        assertEquals("I want water and an apple.", first(WANT, WATER, APPLE));
    }

    @Test public void social() {
        assertEquals("Hello!", first(HELLO));
        assertEquals("Thank you.", first(THANKS));
        assertEquals("I want water, please.", first(WANT, WATER, PLEASE));
    }

    @Test public void likeAndDislike() {
        assertEquals("I like apples.", first(ILIKE, APPLE));
        assertEquals("I don't like milk.", first(IDONT, MILK));
    }

    @Test public void tooComplexGivesNothing() {
        assertTrue(texts(I, PLAY, WITH, BALL).isEmpty());   // preposition
        assertTrue(texts(EAT, DRINK).isEmpty());             // two actions
    }

    @Test public void repeatsAreIgnored() {
        assertEquals("I want water.", first(WANT, WATER, WATER, WATER));
    }

    @Test public void statsReorder() {
        ErakaTemplateStats s = new ErakaTemplateStats();
        List<Candidate> c = b.build(Arrays.asList(DRINK, WATER));
        assertEquals("want_action", c.get(0).templateId);
        s.record(c.get(0).key, false);
        s.record(c.get(0).key, false);
        s.record(c.get(1).key, true);
        assertEquals("do_action", s.order(c).get(0).templateId);
        // a different kind of message keeps its own default order
        List<Candidate> go = b.build(Arrays.asList(GO, SCHOOL));
        assertEquals("want_action", s.order(go).get(0).templateId);
    }

    @Test public void orderDoesNotMatterForDescribingWords() {
        assertEquals("Mom is happy.", first(HAPPY, MOM));
        assertEquals("I am tired.", first(TIRED, I));
    }

    @Test public void pluralThingsHaveNoArticle() {
        Word MILKCOOKIES = new Word(427, "milk and cookies", 2), CHESTNUTS = new Word(402, "chestnuts", 2);
        assertEquals("Dad, give me milk and cookies.", first(DAD, GIVE, MILKCOOKIES));
        assertEquals("I want chestnuts.", first(WANT, CHESTNUTS));
        assertEquals("I want a glass.", first(WANT, new Word(605, "glass", 2)));
    }

    // ------------------------------------------------------------------ 21 Oct 2026: Keneke's test on the A13

    static Word cat(int id, String label, int tipo, String category) {
        return new Word(id, label, tipo, category);
    }

    @Test
    public void requestVerbWithoutPersonAsksForIt() {
        Word milkCookies = cat(427, "milk and cookies", 2, ErakaSentenceBuilder.CAT_FOOD);
        assertEquals("Give me milk and cookies.", first(GIVE, milkCookies));
        assertTrue(texts(GIVE, milkCookies).contains("Can you give me milk and cookies?"));
        Word open = new Word(60, "open", 3), door = cat(800, "door", 2, ErakaSentenceBuilder.CAT_OTHER);
        assertEquals("Open the door.", first(open, door));          // no "open me", "the" not "a"
    }

    @Test
    public void yesAndNoAnswer() {
        Word yes = new Word(388, "Yes", 5), no = new Word(389, "No", 5);
        assertEquals("Yes, I want it.", first(yes, WANT));
        assertEquals("Yes, I want it.", first(WANT, yes));
        assertEquals("No, I don't want it.", first(no, WANT));
        assertEquals("No, I don't want milk.", first(no, MILK));
        assertEquals("No, I don't want to eat.", first(no, EAT));
        assertEquals(1, texts(yes, MILK).size());                     // just "Yes, I want milk."
    }

    @Test
    public void coloursDescribeThingsNotTheChild() {
        Word yellow = cat(733, "Yellow", 4, ErakaSentenceBuilder.CAT_COLOR);
        Word ball = cat(996, "ball", 2, ErakaSentenceBuilder.CAT_TOY), car = cat(700, "car", 2, ErakaSentenceBuilder.CAT_OTHER);
        assertEquals("It is yellow.", first(yellow));
        assertEquals("I want a yellow ball.", first(yellow, ball));
        assertTrue(texts(yellow, ball).contains("The ball is yellow."));
        assertEquals("The car is yellow.", first(yellow, car));
        assertEquals("I like yellow.", first(I, yellow));
        assertEquals("I am tired.", first(TIRED));                    // feelings still describe the child
    }

    @Test
    public void onlyFoodToysAndClothesAreAskedFor() {
        Word oven = cat(1025, "oven", 2, ErakaSentenceBuilder.CAT_OTHER), horse = cat(293, "horse", 2, ErakaSentenceBuilder.CAT_ANIMAL);
        assertEquals("Look, an oven!", first(oven));
        assertEquals("Look, a horse!", first(horse));
        assertTrue(texts(horse).contains("I see a horse."));
        assertEquals("I want an oven.", first(WANT, oven));            // the child chose "want"
        assertEquals("I want shoes.", first(cat(900, "shoes", 2, ErakaSentenceBuilder.CAT_CLOTHING)));
        assertEquals("I want milk.", first(MILK));                     // no category: as before
    }

    @Test
    public void help() {
        Word help = new Word(703, "help", 6), helpSocial = new Word(385, "Help", 5);
        Word oven = cat(1025, "oven", 2, ErakaSentenceBuilder.CAT_OTHER);
        assertEquals("Can you help me with the oven?", first(oven, help));
        assertEquals("Can you help me with the oven?", first(oven, helpSocial));
        assertTrue(texts(help, oven).contains("I need help with the oven."));
        assertEquals("Help me, please.", first(help));
        assertEquals("Mom, help me.", first(MOM, help));
        assertEquals("Mom, help me with the oven.", first(MOM, help, oven));
    }

    @Test
    public void greetingToOnePersonOrThing() {
        Word bye = new Word(382, "bye", 5), horse = cat(293, "horse", 2, ErakaSentenceBuilder.CAT_ANIMAL);
        assertEquals("Bye, horse!", first(horse, bye));
        assertEquals("Hello, mom!", first(HELLO, MOM));
        assertEquals("Hello, I want milk.", first(HELLO, WANT, MILK));
    }

    @Test
    public void questionWords() {
        Word where = new Word(595, "Where?", 6), what = new Word(913, "what?", 6);
        assertEquals("Where is the milk?", first(where, MILK));
        assertEquals("Where is mom?", first(where, MOM));
        assertEquals("Where is mom?", first(MOM, where));
        assertEquals("Where is it?", first(where));
        assertEquals("What is it?", first(what));
    }

    // ------------------------------------------------------------------ 22 Oct 2026: parents' starter messages

    @Test
    public void wordsFromParentsMessages() {
        Word helpMe = new Word(385, "help me", 5);                       // OTTAA's "Help", renamed
        assertEquals("Help me, please.", first(helpMe));
        assertEquals("Mom, help me.", first(helpMe, MOM));
        Word porridge = cat(5005, "porridge", 2, ErakaSentenceBuilder.CAT_FOOD);
        assertEquals("I want porridge.", first(porridge));
        assertEquals("I want cooldrink.", first(cat(501, "cooldrink", 2, ErakaSentenceBuilder.CAT_FOOD)));
        assertEquals("I want noodles.", first(cat(5008, "noodles", 2, ErakaSentenceBuilder.CAT_FOOD)));
        assertEquals("I am hurt.", first(new Word(5002, "hurt", 4)));
        assertEquals("I want to sleep.", first(new Word(5001, "sleep", 3)));
        Word phone = cat(5003, "phone", 2, ErakaSentenceBuilder.CAT_OTHER);
        assertEquals("Give me the phone.", first(GIVE, phone));
        assertEquals("Mom, give me the phone.", first(MOM, GIVE, phone));
        assertEquals("I want an apple.", first(I, WANT, APPLE));         // food: still "an"
        Word sorry = new Word(5004, "sorry", 5);
        assertEquals("I am sorry.", first(sorry));
        assertEquals("I am sorry.", first(I, sorry));
        assertEquals("I have a headache.", first(new Word(827, "headache", 2)));   // not "I want a headache."
        assertEquals("I am in pain.", first(new Word(821, "pain", 2)));
        assertEquals("Mom has a stomachache.", first(MOM, new Word(825, "stomachache", 2)));
    }

    @Test
    public void coreWordsAddedFromParentsMessages() {
        Word more = new Word(5010, "more", 6), stop = new Word(5011, "stop", 5), finished = new Word(5012, "finished", 4),
                come = new Word(5013, "come", 3), we = new Word(5014, "we", 1), shop = cat(5015, "shop", 2, ErakaSentenceBuilder.CAT_PLACE),
                food = cat(1014, "food", 2, ErakaSentenceBuilder.CAT_FOOD);
        assertEquals("I want more.", first(more));
        assertEquals("I want more food.", first(more, food));
        assertEquals("I want more apples.", first(WANT, more, APPLE));
        assertEquals("Stop, mom!", first(stop, MOM));
        assertEquals("I am finished.", first(finished));
        assertEquals("Come here.", first(come));
        assertEquals("Mom, come here.", first(MOM, come));
        assertEquals("We want to go to the shop.", first(we, GO, shop));
        assertEquals("I want to go to the shop.", first(shop));
        assertEquals("I want biltong.", first(cat(5019, "biltong", 2, ErakaSentenceBuilder.CAT_FOOD)));
        assertEquals("I want a vetkoek.", first(cat(5020, "vetkoek", 2, ErakaSentenceBuilder.CAT_FOOD)));
        assertEquals("I want chips.", first(cat(5021, "chips", 2, ErakaSentenceBuilder.CAT_FOOD)));
        assertEquals("Look, a dog!", first(cat(342, "dog", 2, ErakaSentenceBuilder.CAT_ANIMAL)));
    }

    @Test
    public void namibianRoutinesAndFamily() {
        Word grandma = new Word(5027, "grandma", 1), aunt = new Word(5028, "aunt", 1),
                wakeUp = new Word(5032, "wake up", 3), fetchWater = new Word(5039, "fetch water", 3),
                braai = cat(5045, "braai", 2, ErakaSentenceBuilder.CAT_FOOD),
                clinic = cat(5044, "clinic", 2, ErakaSentenceBuilder.CAT_PLACE),
                church = cat(5043, "church", 2, ErakaSentenceBuilder.CAT_PLACE),
                toilet = cat(5033, "toilet", 2, ErakaSentenceBuilder.CAT_PLACE);
        assertEquals("Grandma wants water.", first(grandma, WATER));
        assertEquals("Aunt, come here.", first(aunt, new Word(5013, "come", 3)));
        assertEquals("I want to wake up.", first(wakeUp));
        assertEquals("I want to fetch water.", first(fetchWater));
        assertEquals("I want braai.", first(braai));
        assertEquals("I want to go to the clinic.", first(clinic));
        assertEquals("I want to go to church.", first(church));
        assertEquals("I want to go to the toilet.", first(toilet));
        assertEquals("Grandma wants to go to church.", first(grandma, GO, church));
    }

    @Test
    public void yesterdayAndTomorrow() {
        Word yesterday = new Word(811, "yesterday", 6), tomorrow = new Word(815, "tomorrow", 6),
                church = cat(5043, "church", 2, ErakaSentenceBuilder.CAT_PLACE),
                horse = cat(293, "horse", 2, ErakaSentenceBuilder.CAT_ANIMAL), no = new Word(389, "No", 5);
        assertEquals("I went to church yesterday.", first(yesterday, GO, church));
        assertEquals("Mom went to church yesterday.", first(MOM, GO, church, yesterday));
        assertEquals("I ate an apple yesterday.", first(yesterday, EAT, APPLE));
        assertEquals("I saw a horse yesterday.", first(yesterday, horse));
        assertEquals("I was tired yesterday.", first(I, TIRED, yesterday));
        assertEquals("I will go to school tomorrow.", first(tomorrow, SCHOOL));
        assertEquals("I want milk tomorrow.", first(WANT, MILK, tomorrow));
        assertEquals("No, I don't want to go to school tomorrow.", first(no, GO, SCHOOL, tomorrow));
        assertTrue(new ErakaSentenceBuilder().build(java.util.Arrays.asList(yesterday, tomorrow)).isEmpty());
        assertEquals("I want to go to church.", first(GO, church));          // no time word: as before
    }
}
