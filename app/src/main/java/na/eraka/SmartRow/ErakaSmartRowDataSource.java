package na.eraka.SmartRow;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import na.eraka.JSONutils.Json;
import na.eraka.erakaprediction.ErakaExplorer;
import na.eraka.erakaprediction.ErakaPredictionService;
import na.eraka.erakaprediction.ErakaPredictor;
import na.eraka.utils.JSONutils;

import org.json.JSONException;
import org.json.JSONObject;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Smart row driven by the Eraka prediction model (step 3).
 *
 * - Eraka switched on and the child has history: the model's best pictures for the
 *   current message, time and place, never repeating what is on the 4 tiles or already
 *   in the message. Free slots are filled with core words in a fixed order (7 Oct 2026:
 *   no OTTAA counts while Eraka is on).
 * - Eraka switched on, no data yet: core words only.
 * - Eraka switched off: the old heuristic only (OTTAA-style baseline, for the comparison).
 *
 * Exploration (step 5a, 13 Oct 2026): with Eraka on, the row has 6 slots:
 *   slots 1-3  exploitation: the model's best pictures (free slots: core words),
 *   slots 4-6  exploration:  pictures the child has never used, chosen by ErakaExplorer
 *              (UCB). Slot 4: pictures linked to the previous picture, then the rest of its
 *              groups. Slots 5-6: the groups matching the interests in the child profile (if
 *              any were chosen), familiar words first. Without interests all three follow the
 *              message. Then the base model and the core words. (3 + 3 since 22 Oct 2026.)
 *              Logged as explore_ids.
 */
public class ErakaSmartRowDataSource implements SmartRowView.SmartRowDataSource {

    /**
     * Core words used to fill free slots, in a fixed priority order (requesting, regulating,
     * answering, then common actions and feelings). Same order every time = predictable.
     */
    static final List<String> CORE_WORDS = Arrays.asList(
            "want", "more", "help", "stop", "yes", "no", "i", "you",
            "go", "eat", "drink", "play", "i like", "finished", "all done", "come",
            "look", "give", "happy", "sad", "tired", "hurt");

    private Map<String, Integer> coreIdsByName; // filled on first use

    /** Gives the pictures currently in the message, oldest first. */
    public interface MessageProvider {
        int[] currentMessageIds();
    }

    private static final int SUGGESTION_LIMIT = 6;

    private final HeuristicSmartRowDataSource fallback;
    private final MessageProvider messageProvider;
    private final ErakaPredictionService service;
    private volatile Set<Integer> excludedIds = new HashSet<>();

    private final Context appContext;

    public ErakaSmartRowDataSource(Context context, ErakaPredictionService service, MessageProvider messageProvider) {
        this.appContext = context.getApplicationContext();
        this.fallback = new HeuristicSmartRowDataSource(context);
        this.service = service;
        this.messageProvider = messageProvider;
    }

    /** Pictures on the 4 main tiles: never suggested again in the smart row. */
    public void setExcludedIds(Set<Integer> ids) {
        Set<Integer> copy = ids != null ? new HashSet<>(ids) : new HashSet<>();
        excludedIds = copy;
        fallback.setExcludedIds(copy);
    }

    @Override
    public void getSuggestions(SmartRowView.Scope scope, long timestamp, String userId, Callback callback) {
        if (service == null || !service.isEnabled()) {
            if (service != null) service.setCurrentExplore(null); // no exploration in OTTAA mode
            fallback.getSuggestions(scope, timestamp, userId, extra -> // baseline, first 6 only
                    callback.onResult(extra.size() > SUGGESTION_LIMIT
                            ? new ArrayList<>(extra.subList(0, SUGGESTION_LIMIT)) : extra));
            return;
        }

        int[] message = messageProvider != null ? messageProvider.currentMessageIds() : new int[0];
        Set<Integer> exclude = new HashSet<>(excludedIds);
        for (int id : message) exclude.add(id); // don't suggest what the child already said

        int exploitSlots = SUGGESTION_LIMIT - ErakaExplorer.EXPLORE_SLOTS; // 6 - 3 = 3
        List<SmartRowItem> items = new ArrayList<>();
        Set<String> used = new HashSet<>();
        if (service.hasData()) {
            ErakaPredictor.Query q = service.queryFor(message);
            // step 5b: suggestions the child keeps ignoring in this context move down
            for (ErakaPredictor.Suggestion s : service.rankSuggestions(q, exclude, exploitSlots)) {
                SmartRowItem item = toItem(s.pictoId);
                if (item != null && used.add(item.pictogramId)) items.add(item);
            }
        }
        // free exploitation slots: core words in their fixed order (no OTTAA counts while Eraka is on)
        fillWithCoreWords(items, used, exclude, exploitSlots);

        // exploration slots: new pictures (never what is already on screen or in the message)
        Set<Integer> notThese = new HashSet<>(exclude);
        for (String id : used) notThese.add(parse(id));
        int prev = message.length > 0 ? message[message.length - 1] : -1;
        List<Integer> core = new ArrayList<>();
        for (String word : CORE_WORDS) {
            Integer id = coreIds().get(word);
            if (id != null) core.add(id);
        }
        List<Integer> explore = new ArrayList<>();
        List<Integer> nearby = linkedTo(prev);          // pictures linked to the previous one,
        nearby.addAll(groupMates(prev));                // then the rest of its groups (e.g. other food)
        List<Integer> liked = interestPictures();       // groups the child enjoys (child profile)
        List<Integer> picked = new ArrayList<>();
        // Eraka 28 Oct 2026: away from home ("Where are we?" = church, clinic, taxi…) three
        // places of the row show that place's pictures (at the start of the row) (place_words.json, in their order; the
        // place picture itself is skipped). They are not exploration: nothing is logged as such.
        List<Integer> placeIds = placeSuggestions(notThese, ErakaExplorer.EXPLORE_SLOTS);
        if (!placeIds.isEmpty()) {
            // Eraka 28 Oct 2026 (Keneke): the place's pictures come FIRST in the row, the
            // predictions after them
            List<SmartRowItem> placeItems = new ArrayList<>();
            for (int id : placeIds) {
                SmartRowItem item = toItem(id);
                if (item != null && used.add(item.pictogramId)) placeItems.add(item);
            }
            items.addAll(0, placeItems);
            service.setCurrentExplore(new int[0]);
            fillWithCoreWords(items, used, exclude, SUGGESTION_LIMIT);
            deliver(items, callback);
            return;
        }
        if (liked.isEmpty()) {
            picked.addAll(service.pickExplore(nearby, core, notThese, ErakaExplorer.EXPLORE_SLOTS));
        } else {
            // one place follows the message, the others the child's interests (21/22 Oct 2026)
            picked.addAll(service.pickExplore(nearby, core, notThese, 1));
            Set<Integer> notThese2 = new HashSet<>(notThese);
            notThese2.addAll(picked);
            int wanted = ErakaExplorer.EXPLORE_SLOTS - picked.size();
            List<Integer> fromInterests = service.pickExplore(liked, new ArrayList<>(), notThese2, wanted);
            picked.addAll(fromInterests);
            notThese2.addAll(fromInterests);
            if (picked.size() < ErakaExplorer.EXPLORE_SLOTS)          // interests used up: back to the message
                picked.addAll(service.pickExplore(nearby, core, notThese2, ErakaExplorer.EXPLORE_SLOTS - picked.size()));
        }
        for (int id : picked) {
            SmartRowItem item = toItem(id);
            if (item != null && used.add(item.pictogramId)) {
                items.add(item);
                explore.add(id);
            }
        }
        int[] exploreIds = new int[explore.size()];
        for (int i = 0; i < exploreIds.length; i++) exploreIds[i] = explore.get(i);
        service.setCurrentExplore(exploreIds);

        // if fewer new pictures were found, the remaining slots get core words (not exploration)
        fillWithCoreWords(items, used, exclude, SUGGESTION_LIMIT);

        deliver(items, callback);
    }

    private void fillWithCoreWords(List<SmartRowItem> items, Set<String> used, Set<Integer> exclude, int upTo) {
        for (String word : CORE_WORDS) {
            if (items.size() >= upTo) break;
            Integer id = coreIds().get(word);
            if (id == null || exclude.contains(id)) continue;
            SmartRowItem item = toItem(id);
            if (item != null && used.add(item.pictogramId)) items.add(item);
        }
    }

    /** The core words' picture ids, in their fixed order (also used to top up the 4 tiles). */
    public List<Integer> coreWordIds() {
        List<Integer> ids = new ArrayList<>();
        for (String word : CORE_WORDS) {
            Integer id = coreIds().get(word);
            if (id != null) ids.add(id);
        }
        return ids;
    }

    /** Pictures linked to the given picture in the vocabulary ("relacion"), in stored order. */
    private static List<Integer> linkedTo(int pictoId) {
        List<Integer> ids = new ArrayList<>();
        if (pictoId < 0) return ids;
        try {
            JSONObject picto = Json.getInstance().getPictoFromId2(pictoId);
            JSONArray rel = picto == null ? null : picto.optJSONArray("relacion");
            for (int i = 0; rel != null && i < rel.length(); i++) {
                JSONObject r = rel.optJSONObject(i);
                if (r != null && !r.isNull("id")) ids.add(r.optInt("id", -1));
            }
        } catch (Exception e) {
            // no links: exploration uses the base model and core words only
        }
        return ids;
    }

    /**
     * Pictures in the same groups as the given picture (not the "ALL" group), in group order:
     * after "apple", other food the child has not tried yet. With no previous picture, empty.
     */
    private static List<Integer> groupMates(int pictoId) {
        List<Integer> ids = new ArrayList<>();
        if (pictoId < 0) return ids;
        try {
            JSONArray groups = Json.getInstance().getmJSONArrayTodosLosGrupos();
            for (int g = 0; groups != null && g < groups.length(); g++) {
                JSONObject group = groups.optJSONObject(g);
                if (group == null || group.optInt("id", -1) == ALL_GROUP_ID) continue;
                JSONArray rel = group.optJSONArray("relacion");
                if (rel == null || rel.length() > MAX_GROUP_SIZE) continue;   // "ALL"-like groups
                boolean member = false;
                for (int i = 0; i < rel.length() && !member; i++) {
                    JSONObject r = rel.optJSONObject(i);
                    member = r != null && r.optInt("id", -1) == pictoId;
                }
                if (!member) continue;
                for (int i = 0; i < rel.length(); i++) {
                    JSONObject r = rel.optJSONObject(i);
                    if (r != null && !r.isNull("id")) ids.add(r.optInt("id", -1));
                }
            }
        } catch (Exception e) {
            // groups not loaded: exploration uses the links and core words only
        }
        return ids;
    }

    /**
     * Pictures from the groups that match the interests chosen in the child profile ("What do
     * you enjoy?"), taking one picture from each group in turn so that every interest gets a
     * chance: food, animals, food, animals... Empty when no interest was chosen.
     */
    /** Up to n pictures of the current place (not at home), not already shown or said. */
    private List<Integer> placeSuggestions(Set<Integer> notThese, int n) {
        List<Integer> out = new ArrayList<>();
        na.eraka.utils.ErakaPlaces.Place place = na.eraka.utils.ErakaPlaces.current(appContext);
        if ("HOME".equals(place.key)) return out;
        List<Integer> ids = na.eraka.utils.ErakaPlaceWords.idsFor(appContext, place.key);
        for (int i = 1; i < ids.size() && out.size() < n; i++) {   // index 0 = the place picture itself
            int id = ids.get(i);
            if (!notThese.contains(id)) out.add(id);
        }
        return out;
    }

    private List<Integer> interestPictures() {
        List<Integer> ids = new ArrayList<>();
        try {
            Set<String> interests = na.eraka.utils.ErakaChildProfile.interests(
                    android.preference.PreferenceManager.getDefaultSharedPreferences(appContext));
            if (interests.isEmpty()) return ids;
            JSONArray groups = Json.getInstance().getmJSONArrayTodosLosGrupos();
            List<List<Integer>> lists = new ArrayList<>();
            for (int g = 0; groups != null && g < groups.length(); g++) {
                JSONObject group = groups.optJSONObject(g);
                if (group == null || group.optInt("id", -1) == ALL_GROUP_ID) continue;
                JSONArray rel = group.optJSONArray("relacion");
                if (rel == null || rel.length() > MAX_GROUP_SIZE) continue;
                String interest = na.eraka.utils.ErakaChildProfile.interestOfGroup(group.optInt("id", -1),
                        JSONutils.getNombre(group, "en"), interests);
                if (interest != null) lists.add(familiarFirst(rel, interest));
            }
            for (int i = 0; i < MAX_GROUP_SIZE; i++) {          // round robin over the groups
                boolean any = false;
                for (List<Integer> rel : lists) {
                    if (i >= rel.size()) continue;
                    any = true;
                    ids.add(rel.get(i));
                }
                if (!any) break;
            }
        } catch (Exception e) {
            // no groups or settings yet: exploration works without interests
        }
        return ids;
    }

    /**
     * The group's pictures with the words young children know best first (dog, cat, cow... for
     * animals; ErakaChildProfile.FAMILIAR), then the rest in stored order. Without this the
     * interest place showed OTTAA's Spanish-alphabetical first picture (bee, chard).
     */
    private static List<Integer> familiarFirst(JSONArray rel, String interest) {
        List<String> familiar = na.eraka.utils.ErakaChildProfile.FAMILIAR.get(interest);
        List<Integer> ids = new ArrayList<>();
        final Map<Integer, Integer> rank = new HashMap<>();
        for (int i = 0; i < rel.length(); i++) {
            JSONObject r = rel.optJSONObject(i);
            if (r == null || r.isNull("id")) continue;
            int id = r.optInt("id", -1);
            ids.add(id);
            int pos = 1000 + i;                                  // not in the list: stored order
            if (familiar != null) {
                try {
                    JSONObject picto = Json.getInstance().getPictoFromId2(id);
                    String name = picto == null ? null : JSONutils.getNombre(picto, "en");
                    int k = name == null ? -1 : familiar.indexOf(name.toLowerCase(Locale.ROOT).trim());
                    if (k >= 0) pos = k;
                } catch (Exception ignored) {
                }
            }
            rank.put(id, pos);
        }
        java.util.Collections.sort(ids, (a, b) -> Integer.compare(rank.get(a), rank.get(b)));
        return ids;
    }

    private static final int ALL_GROUP_ID = 24;
    private static final int MAX_GROUP_SIZE = 200;

    /** Core word -> picture id, looked up once by English name (first picture with that name). */
    private Map<String, Integer> coreIds() {
        if (coreIdsByName != null) return coreIdsByName;
        Map<String, Integer> map = new HashMap<>();
        try {
            JSONArray all = Json.getInstance().getmJSONArrayTodosLosPictos();
            for (int i = 0; all != null && i < all.length(); i++) {
                JSONObject picto = all.optJSONObject(i);
                if (picto == null) continue;
                String name = JSONutils.getNombre(picto, "en");
                if (name == null) continue;
                String key = name.toLowerCase(Locale.ROOT).trim();
                if (CORE_WORDS.contains(key) && !map.containsKey(key)) map.put(key, JSONutils.getId(picto));
            }
        } catch (Exception e) {
            return map; // pictures not loaded yet: try again next time
        }
        if (!map.isEmpty()) coreIdsByName = map;
        return map;
    }

    private void deliver(List<SmartRowItem> items, Callback callback) {
        new Handler(Looper.getMainLooper()).post(() -> callback.onResult(items));
    }

    private static SmartRowItem toItem(int pictoId) {
        // pictogram audit: hidden and caregiver-only pictures are never suggested to the child
        if (na.eraka.utils.ErakaVocabulary.isHiddenForChild(pictoId)) return null;
        try {
            JSONObject picto = Json.getInstance().getPictoFromId2(pictoId);
            if (picto == null) return null;
            String label = JSONutils.getNombre(picto, "en");
            if (label == null || label.isEmpty()) return null;
            String imageUrl = JSONutils.getImagen(picto).optString("picto", "");
            return new SmartRowItem(String.valueOf(pictoId), label, imageUrl);
        } catch (JSONException e) {
            return null;
        }
    }

    private static int parse(String id) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
