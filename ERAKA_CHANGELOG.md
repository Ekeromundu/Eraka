# Eraka app – redesign & fixes log (Sept–Oct 2026)

Project folder: `OTTAAProject - Copy - Copy` (Android, package com.stonefacesoft.ottaa). Backups of every changed file: `_backup_login_activity_2/` in the project root.
(Also saved in the claude.ai Project "Eraka AAC Mobile App" as `claude/eraka-redesign-changelog.md`.)

## Skin colour: moved to the profile page, emoji shades – 28 Oct 2026

- Keneke's test of the avatar-page version: the page looked crowded, the mixed tile was confusing, and the question should read "What colour should the pictures be?".
- The question now sits on the child-profile page (`login_activity_2.xml`, normal + hdpi), under the interests: five tiles with the waving child (`ic_hola`) in the **five emoji skin shades** of the phone keyboard, lightest first. No mixed tile, no colour names; still optional.
  - `ChildProfileActivity.setupToneChoice()` / `markTone()` (moved from `AvatarChoiceActivity`; recoloured in a background thread). Also works when the profile page is opened from the menu.
  - Removed from `login_activity_avatar.xml` (normal + hdpi) and `AvatarChoiceActivity`.
- `ErakaSkinTone`: tones `light` (ARASAAC original, default), `medium_light` #F3D2A2, `medium` #D5AB88, `medium_dark` #AF7E57, `dark` #7C533E (`CHOICES`, `rgbOf()`). Medium and dark were ARASAAC's #E3AB72 / #A65C17 before. `mixed` still works for phones that saved it.
- A colour tapped by the parent wins: `pickTone()` remembers it (`eraka_skin_tone_picked`), and a character chosen afterwards no longer changes the pictures (`setToneFromAvatar()`). With no tile tapped, the character still sets the colour.
- Strings: `tone_question` "What colour should the pictures be?", `tone_option_1`–`5` (TalkBack).
- Backup: `_backup_login_activity_2/skin_tone_name_page_20261028/`.

## Skin colour asked with example pictures – 28 Oct 2026

- Keneke's test: with no avatar chosen (or an own photo) the pictures were "mixed", so some people looked white and some dark, at random. Colour names (light/medium/dark) are not wanted.
- Avatar page (`login_activity_avatar.xml`, normal + hdpi): new question at the top, "Which pictures look most like you?", with four tiles: the ARASAAC waving child (`ic_hola`) in light, medium and dark, plus a tile with all three (mixed). No colour words; TalkBack reads "Example pictures, first choice" etc.
  - Tiles are LinearLayouts (style `ErakaToneTile`, background `bg_eraka_tone_tile` with a thick mustard border when selected), because the page's onClick treats any ImageView as an avatar.
- `AvatarChoiceActivity`: `setupToneChoice()` (examples recoloured in a background thread), `markTone()`. The question is optional (Keneke: don't force it): with nothing chosen, `ErakaSkinTone.DEFAULT` is now `light` = the pictures exactly as ARASAAC draws them (was `mixed`). Choosing a character still sets the colour and now also highlights the matching tile; the family can change the tile afterwards.
- `ErakaSkinTone.isChosen()` and `example(context, name, tone)` (a picture in a given tone, whatever is chosen now).
- Strings `tone_*` in `strings_eraka_loader.xml`. The right-to-left layout has no tiles (code copes).
- Families who never chose (no saved colour) now see the ARASAAC originals instead of the random mix; a saved choice is kept.
- Backup: `_backup_login_activity_2/skin_tone_picker_20261028/`.

## Communication level removed from sign-up – 27 Oct 2026

- Keneke's decision: the level did not change anything in the app (only exported for the study), and it is a clinical question at sign-up. The researcher asks it in the parent questionnaire instead and links it by the child's study code.
- `login_activity_2.xml` (normal + hdpi): `textViewCommLevel` and `constraintCommLevel` set to `gone` (kept in the layout, so `ChildProfileActivity`'s look-ups still work).
- `ChildProfileActivity.validateGenderSelection()` now always true (the level is no longer required to continue); `setUpUserData()` keeps a level saved by an earlier version.
- Export `info.txt` still has the `comm_level` line (empty for new children); the replay tool copes with an empty value.
- Thesis update 13 `60design.tex`: child-profile paragraph and Send-usage-data sentence updated.
- Backup: `_backup_login_activity_2/comm_level_removed_20261027/`.

## Settings: Prediction and Language pages removed – 27 Oct 2026

- `preferences.xml`: the two pages get keys `eraka_screen_prediction` and `eraka_screen_language`; `SettingsActivity.hideLanguageSettings()` removes both with the new `removeSettingsPage(key)` (after `initComponents()` has looked their items up, so nothing returns null).
  - Prediction = OTTAA's calendar, GPS, crowd suggestions, age and gender options; Eraka's own model replaces them (its switch "Learn from this child" stays under Pictograms).
  - Language: Eraka is English-only (experimental mode / ChatGPT stay off).
- **Bug found:** the 19 Oct code looked the Language page up through the key "category_8", which the Facial control group under Accessibility also uses, so it found the wrong group (the Language page stayed visible and an Accessibility part could be removed instead). Now each page is found by its own key.
- Backup: `_backup_login_activity_2/settings_pages_20261027/`.

## Settings sub-pages get a back arrow – 27 Oct 2026

- In Settings, Pictograms / Prediction / Accessibility … open as full-screen dialogs (OTTAA's old `PreferenceActivity` nested `PreferenceScreen`s) with a title bar but no way back except the phone's back gesture.
- `SettingsActivity.onPreferenceTreeClick` now calls `addBackArrow(dialog)`: shows the "<-" arrow in the sub-page's title bar and closes the sub-page when tapped. A Dialog never calls the activity's `onOptionsItemSelected`, so the arrow tap (`android.R.id.home`) is caught by wrapping the dialog window's `Window.Callback` in a `java.lang.reflect.Proxy` that dismisses the dialog and passes everything else on unchanged.
- Backup: `_backup_login_activity_2/settings_back_20261027/`.

## Voice & speech: male/female labels – 27 Oct 2026

- Android's voice list says nothing about male or female, so the page showed "Voice 1, Voice 2…" and male voices could not be found. `ErakaVoiceActivity.voiceGender()` labels Google's offline voices from the code in their names (en-GB gbb/gbd/rjs male, gba/gbc/gbg female; en-US iol/iom/tpd male, sfg/iob/iog/tpc/tpf female; en-AU aub/aud male, aua/auc female); unknown codes stay unlabelled.
- If no installed voice is male, the page explains how to download one (Android Settings → Text-to-speech output → Google → Install voice data → English (United Kingdom)).
- Strings `voice_male`, `voice_female`, `voice_male_tip`. Backup: `_backup_login_activity_2/voice_gender_20261027/`.

## Cancel button on the avatar page from the menu – 27 Oct 2026

- `AvatarChoiceActivity`: opened from the menu ("Change avatar"), the top-left button (hidden before) now shows **Cancel**. Cancel - and the phone's back gesture - put back what was there when the page opened and close it: the avatar (`userAvatar`, `SelectedAvatar`), the child's own photo and the pictures' skin colour (`rememberStartState()` / `cancelChanges()`, result `RESULT_CANCELED`). Next still saves as before. During sign-up the button stays "Back" to the child profile.
- `AvatarPhotoStore.backup()` / `restore()`: a copy of the own photo in the cache, because choosing a character or a new photo replaces the file straight away.
- String `avatar_cancel`. Backup: `_backup_login_activity_2/avatar_cancel_20261027/`.

## Faster pictures with the skin-colour change – 27 Oct 2026

Why: Keneke found the app slow (pictures, home page after sign-in). Since the default became "mixed", about two-thirds of all pictures are recoloured, each at full 500×500 size on the main thread, and a gallery with many pictures pushed them out of the memory cache again.

- `ErakaSkinTone.resource()` now decides first whether a picture needs a new skin colour (`targetTone`). Only then is it decoded - at half size (`inSampleSize = 2`, `inScaled = false`, 250 px) - and recoloured: a quarter of the pixels to process and to keep in memory (the cache now holds about four times as many pictures).
- Pictures with no people are remembered **on the phone** (pref `eraka_skin_none`), so after the first time they are never decoded twice or examined again.
- Threshold for "no skin" scales with the picture size.
- Timing logged as `ErakaPerf skin_recolour <name>` (logcat), to measure on the A13.
- Backup: `_backup_login_activity_2/speed_skin_20261027/`.

## "More below" hint on the profile and avatar pages – 27 Oct 2026

Why: Keneke's test – on the child profile the skin-colour choice was below the screen edge and nothing showed that the page continued.

- New `utils/ErakaScrollHint` (+ small `ErakaScrollHintHolder`): a mustard pill "More below ⌄" at the bottom of the scrolling panel. It shows while there is more content below (`ScrollView.canScrollVertically(1)`), bobs gently, scrolls down by two-thirds of a screen when tapped, and disappears at the bottom or when everything fits.
- Layouts `login_activity_2.xml` and `login_activity_avatar.xml` (normal + hdpi): new `scrollHint` view after the ScrollView; drawables `ic_eraka_arrow_down`, `bg_eraka_scroll_hint`; string `scroll_more_below`.
- Called from `ChildProfileActivity` and `AvatarChoiceActivity` `onCreate`.
- Backups: `_backup_login_activity_2/scroll_hint_20261027/`.

## Fixes after Keneke's test of 27 Oct 2026

- **Pictures follow the avatar's skin colour.** The avatar and the pictures' skin tone were two unconnected settings (pictures defaulted to Dark), so a white avatar still showed black people. Now:
  - `ErakaSkinTone.DEFAULT` = **mixed** (no skin colour assumed until a parent chooses);
  - choosing an avatar sets the picture tone (`ErakaSkinTone.toneOfAvatar`: `_light/_medium/_dark` suffix, or the mixed-set drawing: 24, 32 light; 15, 23, 33 medium; others dark);
  - avatar screen: new chips Light / Medium / Dark / Mixed above the grid (`chipGroupAvatarTone`, normal + hdpi); they switch the 15 avatars and set the same tone as the child profile (`AvatarChoiceActivity.setUpToneChips`);
  - child profile: with "mixed" the chosen avatar is no longer moved to the mixed set's (brown) version, and no avatar is forced when none was chosen.
- **Neutral default avatar** `drawable/ic_avatar_default.xml` (grey person shape, no face/skin/gender): `SelectedAvatar`, `SplashActivity`, `ErakaReportActivity`, `AvatarChoiceActivity` (offline / no avatar / unknown name). The avatar chosen on this phone now comes before an old one stored in Firebase.
- **Sign-in children:** avatar 25 (brown girl) replaced by 32 (light-skinned girl): 3 brown + 2 light children (`ic_eraka_children.webp`, `eraka_base_data/login/gen_login_children.js`).
- **Home word / own voice everywhere a picture is said:** new `ErakaHomeWords.speakPicto()`; used in `PictogramRecyclerView` (tap and long press in a group) and `FindAllPictogramsRecyclerView`; the edit screen's speaker plays the new or saved recording, else says the home word (`ErakaHomeVoicePanel.playIfAny / homeWordOr`). Before, only the board used them.
- **Menu:** Games hidden (`activity_main_drawer.xml`, `android:visible="false"`).
- **Groups:**
  - titles "Groups" / "Pictures" (were GaleriaGrupos / GaleriaPictos);
  - group list: search added (`action_search`, hint "Search groups"; `GroupRecyclerView` shows the filtered list and maps taps back to the full list with `realPosition`); sort (3 lines), view toggle and cloud download hidden; always the grid;
  - single group: sort, view toggle and "+" hidden; only search;
  - search is case-insensitive (`BaseRecyclerView.onQueryTextSubmit`);
  - bottom bar: "Edit" slot hidden, Close shows an X (`ic_eraka_close`) — `activity_galeria_grupos2.xml` (normal, hdpi, ldrtl).
- **Cloud button removed** instead of asking for a storage permission: it downloaded photos from OTTAA's online storage, which Eraka does not use.
- **ARASAAC picture search:** Global Symbols failing or returning nothing left an empty screen. `ArasaacClient` now URL-encodes the word, falls back to ARASAAC's own API (`api.arasaac.org/v1/pictograms/en/search/…`, results converted to the Global Symbols shape) and always answers, so the dialog closes; `ArasaacSearchActivity` shows "No pictures found…" when nothing comes back.
- **Crash fix (same day, A13 logcat 16:10):** opening the avatar screen crashed ("The style on this component requires your app theme to be Theme.MaterialComponents"). `AvatarChoiceActivity` uses the app's older theme, and Material chips need a Material theme. The new skin-colour `chipGroupAvatarTone` now carries `android:theme="@style/Theme.MaterialComponents.Light.NoActionBar"` (applies to the chips only; the rest of the screen is unchanged), normal + hdpi.
- Backups: `_backup_login_activity_2/test_fixes_20261027/`.

## Menu tab on the board – 24 Oct 2026

Why: the side menu only opened with a swipe from the screen edge (OTTAA's toolbar button is hidden), so users did not know it existed.

- `app_bar_main.xml` / `app_bar_main_right.xml`: new `erakaMenuTab`, a mustard tab (30×64 dp, three-line icon) on the edge where the menu slides out, vertically centred: left edge normally, right edge in the left-handed layout ("skillHand"). New drawables `ic_eraka_menu`, `bg_eraka_menu_tab`, `bg_eraka_menu_tab_end`; string `open_menu`.
- `MainBoardActivity.setUpMenuTab()` (called from `initMenu`): tap opens the menu; the tab fades while the menu slides out; on the first three starts it nudges twice (pref `eraka_menu_tab_hints`). The swipe still works.
- Backups: `_backup_login_activity_2/menu_tab_20261024/`.

## Smarter models: home words in speech, My day in the base model, yesterday/tomorrow – 24 Oct 2026

- **Whisper / voice dictionary:** the family's home words now go first into Whisper's word hint (`MainBoardActivity.buildVocabularyHint`, new `addHintText`, `ErakaHomeWords.allWords`), and `VoiceDictionary.match` also scores each picture against its home word, so saying "meme" can find mom. A word Whisper splits ("me me", "ta te") now matches the joined label (score 7). Backup: `_backup_login_activity_2/homewords_voice_20261024/`.
- **Base model:** 14 short routine sequences in the order of the My day group, with time of day and place (e.g. "wake up toilet" morning/home, "pray sleep" evening/home), source "researcher: My day routine", added to `eraka_base_data/raw_messages.csv`. Synonyms: toilet/loo now map to the new toilet picture (5033) instead of bathroom. Re-imported: 58 messages, 154 taps, 98.8% coverage. Backup: `_backup_login_activity_2/my_day_base_20261024/`.
- **Sentence builder:** time words. "yesterday" puts the sentence in the past and "tomorrow" in the future, with the word at the end: "I went to church yesterday.", "I ate an apple yesterday.", "I saw a horse yesterday.", "I will go to school tomorrow."; with a time word, doing comes before wanting; wishes stay present ("I want milk tomorrow."); questions are not built; two time words: no sentence. Implementation: `build()` picks out the time word, `realiseS()` sets the SimpleNLG tense, `add()` appends the word (also "didn't", "won't"). 24 templates, 25 tests pass (new `yesterdayAndTomorrow`). Backup: `_backup_login_activity_2/tense_20261024/`.
- Thesis update 13 `60design.tex`: speech hint, Time template row (24 templates), base-model routine sentence.

## Remaining screens made Namibian – 24 Oct 2026

- **Sign-in:** new line "Welcome · Welkom" (English, Afrikaans) under "Welcome to Eraka" (`textLoginGreeting`, string `login_welcome_languages`), normal and hdpi layouts.
- **Child profile and avatar screens:** the pencil mascot is replaced by the five children (`ic_eraka_children`), 300×98 dp and 210×68 dp. The pencil stays on the loading popup and the About screen.
- **Games screen** and the **"add a picture" window** (camera / photo album): their ARASAAC drawings now follow the skin tone chosen in the child profile. New `ErakaSkinTone.applyTagged(View)` recolours every ImageView tagged `skin:<drawable name>`; called in `ErakaGamesActivity` and in `EditPictogramActivity.SelectorFuente()`. Tags added in `activity_eraka_games.xml` (4 game pictures) and `dialog_select_imagen.xml` (camera, album).
- Backups: `_backup_login_activity_2/screens_namibian_20261024/`.

## Namibian sign-in screen – 24 Oct 2026

- `res/drawable/bg_eraka_grass.xml` (the strip at the bottom of the splash, sign-in, child profile, avatar, about, help, favourites and loader screens): waves and bubbles replaced by a Namibian landscape - sun, dunes, savanna grass, a quiver tree (kokerboom) and a camelthorn tree, in Eraka's green and mustard. Generator: `eraka_base_data/login/gen_landscape_strip.py`.
- Sign-in screen (`login_activity_1.xml`, normal and hdpi): the pencil mascot is replaced by `drawable-nodpi/ic_eraka_children.webp`, five children in the avatar style (Avataaars), mixed skin tones, boys and girls; `translationY` 30dp → 0dp so they stand on the panel. New string `login_children_desc`. Generator: `eraka_base_data/login/gen_login_children.js`. The pencil stays on the loader, about, profile and avatar screens.
- Still open: a welcome line in Namibian languages (Keneke to supply the greetings).
- Backups: `_backup_login_activity_2/login_namibian_20261024/`. Thesis: update 13 `60design.tex`, one sentence after the avatars.

## Last routine pictures – 23 Oct 2026 (night)

- Added: church (drawing), school uniform (drawing), kapana (photo, cropped to the plate), and the **My day** group picture (ARASAAC calendar, `eraka_d_my_day`), which replaces OTTAA's clock on the next start.
- 23 of the 24 new words now have pictures; only **clinic** is still missing.
- church.jpg and uniform.jpg are 612×612, the size of iStock/Getty preview images: source to be checked before release (licence).

## Routine and family pictures added – 23 Oct 2026 (evening)

- 20 of the 24 new words now have pictures (`res/drawable-xxhdpi/`):
  - ARASAAC drawings (lossless webp, so the skin tone can be changed): grandma, aunt, uncle, cousin, baby, wake up, toilet, bath, get dressed, walk to school, homework, sweep, wash dishes, pray. Checked: the people in grandma, baby, pray, sweep and wash dishes take the dark tone.
  - Photos: braai, cattle, mahangu, radio, donkey cart, fetch water (a tap; a Namibian communal tap would be better).
  - Donkey (288): OTTAA drawing replaced by a photo (`photo` correction).
- Still missing: school uniform, church, clinic, kapana, and the "My day" group picture (OTTAA's clock is used meanwhile).
- `picture_sources.csv`: 21 rows added; Keneke to fill in ARASAAC numbers and photo sources. Crop notes: `eraka_base_data/picture_crops/crop_round3_23oct.txt`.
- homework.png has Spanish words on the worksheet (ARASAAC original) - acceptable, could be swapped later.

## Parents' photos: pear, orange, water, rice, pasta – 23 Oct 2026

- Real photos from a parent (WhatsApp) replace OTTAA's drawings through the `photo` correction: pear (475), orange (435, a naartjie), water (500, a water jug), rice (481, from a mince-and-rice plate), pasta (480, macaroni with mince). 500×500 webp in `res/drawable-xxhdpi/eraka_photo_*.webp`; crop boxes in `eraka_base_data/picture_crops/crop_parent_photos_23oct.py`; sources in `picture_sources.csv`.
- Not used: Slice bread bag, Rhodes jam, Stork margarine, Oros, Fizz Pop (brand logos); the oshifima plate and the popcorn bowl look like internet pictures (source to be confirmed).
- Synonyms: naartjie(s) → orange, macaroni/spaghetti → pasta.

## Namibian routines and extended family – 23 Oct 2026

Why: the proposal promises pictures of Namibian schedules and daily life; the supervisor found the app looked Western.

- `vocabulary_corrections.json` `add`: 24 new words (ids 5027–5050). Each appears once its picture is in the app:
  - family (People group): grandma, aunt, uncle, cousin, baby (OTTAA had grandpa but no grandma);
  - routine: wake up, toilet, bath, get dressed, school uniform, walk to school, homework, fetch water, sweep, wash dishes, pray;
  - everyday Namibia: church, clinic, braai, kapana, mahangu, radio, cattle, donkey cart.
- New `add_groups` section and `ErakaVocabulary.addGroups()`: group **My day** (id 6001), shown first in the group gallery. Members in the order of a child's day: wake up, toilet, bath, get dressed, school uniform, breakfast, walk to school, school, lunch, homework, play, fetch water, sweep, dinner, wash dishes, TV, pray, sleep. Pictures not in the app yet are left out and slot into their place later; pictures a parent adds stay after them. Uses OTTAA's "time" picture until `eraka_d_my_day` exists.
- Picture names: `eraka_d_*` = ARASAAC drawings (get the chosen skin tone, `ErakaSkinTone`), other `eraka_*` = photos (never recoloured).
- Sentence builder: clinic is a place; braai, homework, biltong, droewors take no article. **Fixed:** the biltong/droewors change and its test from 22 Oct had never reached the project. 24 tests pass, including a new `namibianRoutinesAndFamily` ("Grandma wants to go to church.", "I want to fetch water.", "I want braai.").
- `eraka_base_data/synonyms.csv`: granny/gogo/grandmother/ouma → grandma, auntie/aunty → aunt, grandfather/oupa → grandpa.
- Picture checklist for Keneke: `Documents\Eraka\eraka_base_data\new_pictures\README_round3_routines_family.txt` (15 ARASAAC drawings, 10 photos).
- Thesis: update 13 `60design.tex`, new paragraph after home words.
- Backups: `_backup_login_activity_2/routines_family_20261023/`.

## Home words and the parent's own voice – 22 Oct 2026 (late night)

Why: cultural fit. Eraka speaks English (the proposal's only output language), but children hear other words at home ("meme" for mom, "tate" for dad). Mindel & John (2022): families should bring their own language into AAC.

- New `utils/ErakaHomeWords.java`: per picture a **home word** (`files/eraka_home_words.json`, id -> word) and a **recording** (`files/eraka_voice/ID.m4a`). `spoken()` gives the home word or the label; `substitute()` puts home words into a sentence built from the English labels ("Mom, I want water." -> "Meme, I want water."; whole words, plural -s/-es, capital kept, a/an corrected); `playVoice()` plays the recording. Nothing is uploaded or exported.
- New `utils/ErakaHomeVoicePanel.java`: the editor part. Record (max 10 s, AAC .m4a; asks for the microphone if needed) / Stop, Play, Delete. Changes wait in `pending.m4a` until **Save**; **Cancel** throws them away. Hidden for groups.
- `edit_picto_visual.xml` (normal, hdpi, land): new section "Word at home (optional)" + "Your voice (optional)" under the text field. (The unused ldrtl layout is unchanged; the panel copes with it.) Strings in new `values/strings_eraka_home.xml`.
- `EditPictogramActivity`: creates the panel; `commit(id)` on save (existing picture: its id; new picture: the id of the picture just created), `discard()` on cancel / finish.
- `MainBoardActivity`:
  - tapping a picture plays the parent's recording if there is one; otherwise, if "say the picture's name" is on, the home word or label is spoken;
  - the message spoken with Play (`CargarOracion`) uses home words;
  - the suggested sentence is shown and spoken with home words (`sentenceToSay()`); the usage log still records the English sentence.
- The English label, prediction model, templates and study data are unchanged.
- Backups: `_backup_login_activity_2/home_words_20261022/`. Thesis: update 13 `60design.tex` new paragraph, `mindel2022aac` in `references_update13.bib`.

## Skin colour of the people in the pictures – 22 Oct 2026 (late night)

Why: ARASAAC draws every person with one light skin colour (#F5E5DE); 123 of a sample of 400 bundled pictures showed only white people. The supervisor found the app looked Western.

- New `utils/ErakaSkinTone.java`: when a bundled picture is shown, the light skin colour (and its blends into the black outline and the white background) is swapped for the chosen tone, the same way ARASAAC makes its own skin-tone versions. Tones: **light** (original, for white children), **medium** (#E3AB72), **dark** (#A65C17, default), **mixed** (each picture gets one of the three, fixed per picture). Results are kept in a memory cache; pictures without skin are remembered and skipped. Files on disk are not changed.
- Never recoloured: real photos (`eraka_*`, except the drawings `eraka_hurt`, `eraka_laundry`), avatars, the child's own photos.
- New views `Views/ErakaPictoView`, `ErakaGroupView`, `ErakaGameGroupView` (subclasses of the PictogramsLibrary views, overriding `findResource`); all 20 layouts now use them (69 tags).
- `Json.getIcono` / `getIconWithNullOption` and `SmartRowAdapter` load bundled pictures through `ErakaSkinTone`.
- Child profile (`login_activity_2`, normal and hdpi): new "Skin colour of the people in the pictures" choice (Light / Medium / Dark / Mixed), saved on the phone (`eraka_skin_tone`).
- **Avatars follow the tone** (Keneke's request): each of the 15 avatars now also exists as `ic_avatarNN_light`, `_medium`, `_dark` (45 new webp in `res/drawable-nodpi`, same design, only skin and, for light, hair colour change; generator `eraka_base_data/avatars/gen_avatars_tones.js`). `AvatarChoiceActivity.showAvatarsInSkinTone()` shows the set for the chosen tone (Mixed shows the mixed set `ic_avatarNN`); the tag becomes e.g. `avatar22_dark`, so `userAvatar` = `ic_avatar22_dark`. Saving the child profile moves the chosen avatar to the same avatar in the new tone (`ErakaSkinTone.avatarName`), or sets `ic_avatar35_<tone>` if none was chosen yet.
- Backups and a before/after sheet: `_backup_login_activity_2/skin_tone_20261022/`.
- Thesis: update 13 `60design.tex`, new paragraph after the avatars.

## Avatars replaced by Avataaars (boys and girls) – 22 Oct 2026 (night)
- Keneke: "why am I not seeing boys?" Recolouring can't change shapes, and in OTTAA's style the boys didn't read as boys.
- 15 new avatars made with DiceBear 9 (npm `@dicebear/core`, `@dicebear/collection`), style **Avataaars** by Pablo Stanley (free for personal and commercial use):
  - 7 boys (short cuts): 11, 13, 15, 22, 24, 31, 35;
  - 8 girls (fro, afro with band, curls, bun, bob, long curls, long straight hair): 12, 14, 21, 23, 25, 32, 33, 34;
  - skin: 24 and 32 light, 15, 23 and 33 tan, the rest brown to deep;
  - 22 and 34 wear glasses;
  - 35, the default, is a boy in a green shirt;
  - no graphic shirts (one had a bat-like logo).
- Rendered with Chromium to 480×480 transparent PNG, then webp in `res/drawable-nodpi/ic_avatarNN.webp`. The names are unchanged, so the picker, saved choices and defaults keep working.
- The old vector avatars (`res/drawable/ic_avatar*.xml`) moved to `_backup_login_activity_2/avatars_avataaars_20261022/`; two images with the same name can't coexist in one resource set.
- The About text credits Avataaars/Pablo Stanley and the photos and drawings added for Eraka.
- Lost: OTTAA's two wheelchair avatars (Avataaars has no wheelchair). Noted as a limitation in the thesis.
- Generator: `eraka_base_data/avatars/gen_avatars.js` (Node, DiceBear 9).

## Corrections after Keneke's test: avatars, voice, suggestion row – 22 Oct 2026 (evening)
- **Avatars:** the first recolouring made almost all avatars dark-skinned and the set looked all female.
  - Skin now ranges: 22 and 32 light, 31 light tan, 14 and 33 tan, the rest brown to deep.
  - Boys: 12, 14, 15, 22, 24, 34, 35.
  - 14 (a second woman with earrings) and 35 are new short-haired boys built from avatar 22 (tan with a blue shirt, and deep-skinned with a green shirt). 22 is back to its original light skin. 31 has light-tan skin and brown hair; 33 has tan skin.
- **Voice:** Google's TTS has **no** English (South Africa) voice (its English voices: AU, IN, NG, GB, US), so the earlier version never found one.
  - `UtilsTTS.preferredEnglishVoice()` tries en-ZA first, then **en-GB**. British English is non-rhotic like Namibian English, so it is closer than the American default.
  - The tip and link now point to English (United Kingdom).
  - The auto-choice flag was never set on phones without a ZA voice, so they pick the British voice at the next start.
- **Suggestion row: 3 + 3** (`ErakaExplorer.EXPLORE_SLOTS` 2 → 3).
  - With interests: 1 new-picture place follows the message and 2 come from the interests, topped up from the message if the interests run out.
  - Without interests: all 3 follow the message.
  - Places for the model's predictions: 3 (was 4).
- Backup `_backup_login_activity_2/row_voice_avatars_20261022/`.

## South African English voice by default – 22 Oct 2026
- `utils/Ttsutils/UtilsTTS`: new `southAfricanVoice(tts)` finds an installed offline voice with locale en-ZA, sorted by name so the choice is stable. In `applySavedVoice()`, when no voice has been chosen and Eraka hasn't auto-chosen before (`eraka_tts_za_auto_done`), it uses that voice and saves it as the chosen voice (`eraka_tts_voice`). It does this only once, so a caregiver who switches back to "Phone default" or another voice keeps their choice. If the voice isn't installed, the phone default stays and Eraka tries again at the next start.
- `ErakaVoiceActivity`: the South African voice is marked "(recommended)". If none is installed, the page shows a tip (Settings → Text-to-speech → Google → Install voice data → English (South Africa)) and a "Get the South African voice →" link that opens Android's voice-data installer (`ACTION_INSTALL_TTS_DATA`).
- New strings: `voice_recommended`, `voice_za_tip`, `voice_za_get`.
- Why: no Namibian English voice exists, and South African English is the closest available accent. The words and the app stay in English.
- Backup `_backup_login_activity_2/voice_za_20261022/`.

## "Where are we?": Namibian places – 22 Oct 2026
- `ErakaPlaces.PLACES`: 10 places, the most everyday first. Home, School, **Taxi / car** (TAXI 🚕), Shop, **Market** (MARKET 🍅), Church, **Village / farm** (VILLAGE 🌾), Park, Clinic, Eating out. The Zoo is removed; a saved ZOO falls back to Home. Emoji chosen to display on Android 8 too.
- New strings: `place_taxi`, `place_market`, `place_village` (`place_zoo` kept, unused).
- `dialog_eraka_places.xml`: the grid sits inside a ScrollView, so 5 rows fit on small phones and tablets in landscape.
- `tools/eraka_base_import.py` PLACE_WORDS: MARKET, TAXI and VILLAGE added ("market" moved from STORE); ZOO removed.
- The prediction model stores the place key as text, so new keys need no other change. OTTAA's `Json.calcularPosicion()` treats unknown keys as its default; it isn't used while Eraka is on.
- Backup `_backup_login_activity_2/places_namibian_20261022/`.

## Namibian avatars – 22 Oct 2026
- Before: 11 of the 15 avatars were light-skinned (mostly orange or straight hair), avatars 11 and 12 had the puzzle-piece autism symbol on the shirt, and the default avatar 35 was OTTAA's robot mascot ("OP").
- `res/drawable/ic_avatar*.xml`, by recolouring fill colours only, so shapes, faces and layout are unchanged:
  - skin → deep to light-brown tones (#5C3A2E, #7A4B35, #8D5A3B, #A8704F) in 11, 12, 21, 22, 23, 24, 31, 33, 34; 13, 14, 15 and 25 were already brown; 32 stays light-skinned;
  - orange and teal hair → black or dark brown (#1C1C1C, #2B1B14);
  - puzzle pieces removed from 11 and 12 (4 paths each).
- `ic_avatar35` (the robot) replaced by a child avatar: avatar 12's shape, deep skin, green shirt. The grid, tags and code stay the same, and the default ("ic_avatar35" in SelectedAvatar, SplashActivity and ErakaReportActivity) is now a child. Anyone who had chosen the robot sees the new avatar.
- Originals in `_backup_login_activity_2/avatars_namibian_20261022/`.

## Interests: familiar words first; OTTAA's faded tiles and tip removed – 22 Oct 2026
- Keneke's test (new account, interest Animals): the interest place showed a **bee**. Group order is OTTAA's Spanish alphabetical order (abeja = bee; for Food it would be acelga = chard).
- `ErakaChildProfile.FAMILIAR`: an early-learned word list per interest (food, animals, family, music, outdoors, school), informed by early-vocabulary inventories (MacArthur-Bates CDI / Wordbank, Frank et al. 2017) and the parents' foods. `interestOfGroup()` was added.
- `ErakaSmartRowDataSource.familiarFirst()`: each interest group is ordered so its familiar words come first (dog, cat, cow… / milk, water, juice, banana…); the rest follow in stored order. Round robin over the groups as before.
- `MainBoardActivity.formatoTransparencia`: tiles are always fully opaque. OTTAA showed "suggested" tiles 65% transparent (a child can read them as not tappable), and the first time it showed a "Suggestions… transparent pictos" tip (`explicarSugerencias`), which no longer appears.
- Common words missing from the vocabulary, as candidates for the next pictures: bread, park, playground, tree, sun, baby, grandma, elephant, monkey, zebra, dance, class.
- Fourth picture batch: bread (photo, cropped to the basket; Food + meal groups), playground (photo; Places), tree and sun (ARASAAC drawings; Nature/Places, Nature/Time). Ids 5023–5026; 25 added pictograms in total.
- Backup `_backup_login_activity_2/interests_familiar_20261022/`.

## First parent starter messages → base model; new pictograms; sentence fixes – 22 Oct 2026
- **Base model is now on.**
  - `eraka_base_data/raw_messages.csv`: 26 messages from parent P1, with children's names removed. That is 14 things the child says, plus liked foods entered as "I want …".
  - The importer writes `app/src/main/assets/eraka_base/base_messages.csv`: 64 taps, 87% coverage. The app loads it automatically (K = 30).
  - `synonyms.csv`: shoes→shoe, spaghetti→pasta, oats→porridge, weetbix→cereal, kids→friends, looking→look, coke/cool drink→cooldrink.
  - `choose_id.csv`: fish→476 (food, not the animal).
- **`tools/eraka_base_import.py`:**
  - Applies the app's vocabulary corrections: hidden and caregiver-only pictures are never used, and renamed pictures are found by their new labels (e.g. 385 "help me", 125 "sorry").
  - Includes "add" pictures once their drawing exists.
  - me/my/for added to the function words.
- **New pictograms:**
  - `vocabulary_corrections.json` gets a new section `add`, ids 5001–5009 (5004 dropped: "sorry" already exists as 125): sleep, hurt, phone, porridge, cereal, meat, noodles, muffin, each with its groups.
  - New renames: 504 poultry→chicken, 501 soda→cooldrink.
  - `ErakaVocabulary` adds these pictures to the pictogram list, to their groups and to ALL each time the vocabulary loads, so existing installs get them too. It only does so if `res/drawable*/eraka_<word>` exists, so a missing drawing never makes an empty tile.
- **New `tools/eraka_add_pictures.py`:** converts the ARASAAC PNGs in `eraka_base_data/new_pictures/` (named sleep.png …) to 500×500 white-background webp in `res/drawable-xxhdpi/eraka_<word>.webp`. The drawings are still to be downloaded (ARASAAC is blocked for Claude).
- **`ErakaSentenceBuilder`:**
  - "help me" (the renamed 385) counts as help: before, the real button was treated as a social word.
  - "sorry" → "I am sorry."
  - "hurt" is a feeling.
  - cooldrink/porridge/cereal take no article.
  - "give + phone" → "Give me the phone." Objects that are not asked for take "the"; food stays "a/an".
  - New "ache" template: headache → "I have a headache.", pain → "I am in pain." (was "I want a headache.").
  - 22 tests pass.
- Brand items from parents (KFC, Nutella, Roblox, YouTube, Jeep…) are not in the shared vocabulary. Parents add them as photos on their own child's phone.
- **Second parent (P2), same day:**
  - 17 of 20 messages added to `raw_messages.csv`. Messages 15–17 contained the family's full names and were left out; the film title in message 7 became "watch TV".
  - The base model now has 44 messages, 108 taps, 85% coverage, rising to about 97% once the new pictures are in.
  - Importer: may/can/let's/are/and/in/some added to the function words.
  - Synonyms: going→go, eating→eat, biscuit(s)→cookie, shopping→shop, call→call someone, "put on (my) clothes"→change clothes, all done→finished, laundry→do laundry.
- **Core words missing from OTTAA's vocabulary:** found while importing. more, stop, finished/all done and come were in Eraka's core-word list but never existed, so they were silently skipped; "like" is "I like" in the vocabulary.
  - Added to `add` (5010–5017), with P2's words: more, stop, finished, come, we, shop, haircut, do laundry. That makes 16 new pictures in total.
  - `ErakaSmartRowDataSource.CORE_WORDS`: "like" → "i like".
- **`ErakaSentenceBuilder`:**
  - "more" (alone → "I want more."; + food → "I want more food.")
  - "come" → "Come here." / "Mom, come here."
  - "stop + mom" → "Stop, mom!"
  - "finished" → "I am finished."
  - 23 tests pass.
- `tools/eraka_add_pictures.py`: accepts photos (JPG, middle square cropped) as well as drawings. Short file names: laundry.jpg for "do laundry".
- `eraka_base_data/new_pictures/README.txt` lists the 16 pictures; photos are best for everyday things, drawings for actions and core words.
- P2 also sent two restaurant photos (Fanta bottle, chicken and chips). They were not added: one has a logo and a child's face, and the other is better as that child's own photo.
- **Pictures from Keneke** (`Documents\Eraka\eraka_base_data\new_pictures`, 18 files), reviewed one by one:
  - **Used, cropped to 500×500 webp in `res/drawable-xxhdpi/`:**
    - new words: cereal, meat, muffin, porridge (oats), phone, oshifima (5018), biltong (5019), vetkoek (5020), chips (5021, Pexels), Namibia (5022, type 6: no sentence around a country name);
    - real photos replacing OTTAA drawings, through the new `photo` section in `vocabulary_corrections.json`: dog (342), cow (357), traffic light (687).
  - **Not used:**
    - kfc, Weet-Bix box and Kapana noodles packet: logos and packaging, and two carry a shop's "African Marketing Namibia" watermark;
    - the two WhatsApp photos: a Fanta logo and a child's face.
  - Crop settings: `eraka_base_data/picture_crops/crop_photos_22oct.py`. Sources: `eraka_base_data/picture_sources.csv`; Keneke still has to fill in who took each photo or where it came from.
- **`ErakaVocabulary`:** new `photo` correction. A drawing is replaced by a photo only while the picture is still OTTAA's original, the parent hasn't chosen their own picture (`pictoEditado`), and the photo exists in the app.
- **Synonyms:** pap→oshifima, fat cake(s)→vetkoek, (fried) chips/fries→chips.
- **Builder:** biltong takes no article; 23 tests pass.
- **Base model:** 44 messages, 90% coverage.
- **Still missing pictures:** we, sleep, hurt, noodles (a plain bowl), shop, haircut, laundry, more, finished, stop, come. Drawings are best for the actions and core words.
- **Second batch of pictures**, reviewed and placed in `res/drawable-xxhdpi/`:
  - ARASAAC drawings: come (the beckoning one), do laundry (hand loading the machine), finished (thumbs up), hurt (coloured), more, stop (the stop sign; "stop (2)" is a taxi stop), sleep, we.
  - noodles: photo cropped to the bowl.
  - orange_juice.jpg is used as a photo for **juice** (497), not for cooldrink: it clearly shows juice, and P1's child drinks "cooldrink, no juice".
  - Crop script: `picture_crops/crop_photos_22oct_b.py`.
- Base model coverage: 97.2%. Left: kfc (brand), live, and shop and haircut, which still need photos.
- Cooldrink (501) still uses OTTAA's "cocacola" drawing; a plain-glass photo would be better.
- **Third batch:**
  - ARASAAC drawings: shop (person with a trolley), haircut.
  - doctor: picture 629 "medic" renamed "doctor" and given the new drawing through `photo`; no duplicate pictogram.
  - All 21 added pictograms now have pictures. Base model coverage is 98%; only "kfc" (brand) and "live" are left.
- Backup `_backup_login_activity_2/base_model_20261022/`.

## Child profile: level and interests saved and used, nothing personal uploaded – 21 Oct 2026
- New `utils/ErakaChildProfile`: settings `eraka_comm_level` and `eraka_interests` (e.g. "food,animals"), `ageYears()`, and `groupMatches()` (interest → starter groups, by id or name):
  - food = Food 3 + meal groups
  - animals = Animals 2
  - family = People 17
  - music = Music instruments 10 / Music lesson 34
  - outdoors = Places 13, Nature 14, Transport 23
  - school = School 5
- `ChildProfileActivity`:
  - The communication level gets its own setting. It used to be saved in OTTAA's gender field (`Constants.GENERO`): nothing read it, and editing from the menu reset it to "Select level", because `selectIndicator` looked for "Masculino"/"Femenino". Old profiles are still read once from the old field.
  - The interest chips are now saved and pre-filled; before, nothing read them.
  - `setUpUserData` no longer calls `UploadUserData`, and `subirEdadUsuario` is removed, so the name, birth date, level and age group stay on the phone. The age group is still saved locally.
- `SignInActivity`: removed `subirNombreUsuario` and `subirEmail` (they copied the caregiver's Google name and e-mail into the database). `subirPago` is kept (it stores no personal data).
- `ErakaSmartRowDataSource`: when interests are set, the two "new picture" places split:
  - place 5 follows the message (links, then the previous picture's groups);
  - place 6 comes from the interest groups, taken round-robin, one picture from each group in turn;
  - if no interest picture is left, place 6 falls back to the message.
- `ErakaDataExport` info.txt adds `comm_level`, `age_years` and `interests`; no name, no birth date.
- `tools/eraka_replay_study.py`: the per-child overview shows level, age and interests from info.txt. `eraka_make_test_logs.py` writes them too.
- Backup `_backup_login_activity_2/child_profile_20261021/`.
- Note: entries from earlier test accounts already in Firebase (`usuarios/…`, `Edad/…`, `email/…`) are not deleted by this change.

## Fixes from Keneke's A13 test: sentences, suggestion row, voice window – 21 Oct 2026
- `erakanlg/ErakaSentenceBuilder`, all problems reproduced on the PC first:
  - **Requests without a person:** "give + milk and cookies" → "Give me milk and cookies." / "Can you give me…?" (was "I want to give milk and cookies."). "open + door" → "Open the door." "me" is added only for give/bring/show, and "the" is used for things acted on.
  - **Yes/no open the sentence and answer:** "yes + want" → "Yes, I want it."; "no + milk" → "No, I don't want milk."; "no + eat" → "No, I don't want to eat." Other social words still go at the end.
  - **Describing words:** colours and other non-feelings describe a thing, not the child. "yellow" → "It is yellow." (was "I am yellow."); "yellow + ball" → "I want a yellow ball." / "The ball is yellow."; "I + yellow" → "I like yellow."
  - **Categories:** `Word` now carries a category (FOOD, TOY, CLOTHING, ANIMAL, COLOR, FEELING, PLACE, OTHER). A thing on its own is asked for only if it is food, a toy or clothing. Animals and other objects are commented on: "oven" → "Look, an oven!" / "I see an oven."; "horse" → "Look, a horse!". With "want" it is still "I want an oven." Unknown category: as before.
  - **Help** (any word type): "oven + help" → "Can you help me with the oven?" / "I need help with the oven."; "help" → "Help me, please."; "mom + help" → "Mom, help me."; "help + open + door" → "Can you help me open the door?"
  - **Greetings to one person or thing:** "horse + bye" → "Bye, horse!" (was "I want a horse, bye."); "hello + mom" → "Hello, mom!"
  - **Question words** (where/what/who, "?" ignored): "where + milk" → "Where is the milk?"; "where + mom" → "Where is mom?"; "where" → "Where is it?"
  - **Plural fix:** "shoes" → "I want shoes." (was "I want shoe."); unknown plurals ("chestnuts") are kept as written.
- `ErakaSentenceBuilderTest`: 7 new tests (21 in total); all 21 pass, run on the PC with SimpleNLG 4.4.8.
- `MainBoardActivity`: `pictureCategory(id)` / `groupCategory()` build a cached map from the board's groups. Groups are recognised by id or name: Colors 6, Feelings 20, Food 3 + breakfast/lunch/snack/dinner, Toys 12, Clothing 16, Animals 2, Places 13; House/Body/Transport/Concepts/Music instruments = OTHER. The map is passed to the sentence builder.
- **Suggestion row, the two "new picture" places:**
  - `ErakaExplorer.NEW_UNTIL_USES` 3 → 1: only pictures the child has never selected. Before, pictures used once or twice came back and looked like old favourites.
  - `ErakaSmartRowDataSource` also draws candidates from the other pictures in the previous picture's groups (`groupMates`, the "ALL" group skipped).
  - `ErakaExplorerTest` updated to the new rule; 5/5 pass.
- **Voice "Did you mean…?":**
  - Tapping a picture already taught the dictionary; it now also shows "Learned: 'hello' means Hello".
  - The "Teach" button is renamed "Another picture" (`voice_dict_teach`), since it opens the full list for when the right picture isn't shown.
- Backup `_backup_login_activity_2/sentence_fixes_20261021/`.

## Timing logs for the thesis (Technical Performance) – 21 Oct 2026
- New `utils/ErakaPerf.java`: writes timing lines to logcat under the tag `ErakaPerf` (e.g. `model_build ms=84.2 taps=412`). Logging only; the app behaves exactly as before.
- Timed points:
  - `start_to_board`: process start → board ready, once per launch, in `MainBoardActivity.loadPictograms`.
  - `model_build`: retraining from the usage log, plus the number of taps, in `ErakaPredictionService.reload`.
  - `predict_probs` / `predict_rank` / `predict_row`: each model call, in `ErakaPredictionService`.
  - `tap_to_board`: the synchronous work after a tap, in `addPictogramToPhrase`; it includes OTTAA's save of the whole pictogram file.
  - `smart_row`: `refreshSmartRow`; the body moved unchanged into `refreshSmartRowNow`.
  - `whisper_load`: `WhisperBridge.createContext`. Whisper's own "Transcribe finished in …ms" line already existed.
- Cold start and memory come from Android itself (`am start -W`, `dumpsys meminfo`), so no code was added for them.
- `tools/eraka_perf_report.py` fixed after the first real run (21 Oct): reads PowerShell's UTF-16 files, Android 14's "Displayed … for user 0" lines, and counts memory once per dumpsys output.
- New `tools/eraka_perf_report.py`: reads logcat / `am start -W` / `dumpsys meminfo` output and writes `perf_report.md` and `perf_table.tex` (median and range per measure). Tested on made-up lines only.
- Backup `_backup_login_activity_2/perf_logging_20261021/`.
- Code map rebuilt (279 classes) and republished.

## Replay evaluation tools for the pilot (tools only, app unchanged) – 21 Oct 2026
- `tools/eraka_replay_eval.py`: the prototype predictor's files are now found again. The demo assets moved to `Documents\Eraka\eraka-demo-dev\`, so since the move the prototype comparison had been skipped without any message. The script now tries `ERAKA_DEMO_ASSETS`, then `~/Documents/Eraka/...`, then the older locations, and prints a note if it still can't find them. Self-test still OK. Backup `_backup_login_activity_2/eraka_replay_eval_20261021/`.
- New `tools/eraka_replay_study.py <pilot folder>`: reads the zips the parents send with "Send usage data" (or one subfolder per child). It gets the child code from `info.txt` or the zip name, and merges repeated sends by tap id so each tap counts once. Each child gets his or her own fresh models. It writes `replay_results/`:
  - `results.md`: per-child overview, pooled and after-warm-up tables, top-4 and MRR per child with medians, and live tables.
  - `replay_table.tex` / `replay_table_warm.tex`: rows for Table tab:resreplay in thesis order.
  - `replay_per_child.tex`.
  - `ranks_<code>.csv`.
  - "Prototype predictor" row = `eduardo_fixed` (the model itself; `eduardo` = as the demo app ran it).
- New `tools/eraka_make_test_logs.py <out>`: FAKE pilot data in the app's zip format for testing. Three made-up children: T01 depends on time/place and sends twice; T02 switches juice → water mid-week; T03 explores at random. About 7% of taps are undone. Test run: 570 replayed taps, double send counted once. The expected models won (T01 Eraka beat no time/place +1.5 points top-4; T02 Eraka beat no recency +3 points top-1), but the gaps are small. In a one-week pilot, the recency (half-life 14 days) and time/place (weight 0.1 each) ablations will probably show small differences.

## Release 110 (Eraka project) – 19 Oct 2026
- `app/build.gradle`: versionCode 109 → 110 (versionName "1.0-study"). First release from the Eraka project: includes the demo tablet fixes, English-only, OTTAA clean-up (no ChatGPT/translation/OTTAA mailing-list calls), offline story game, English class names, smaller speech bubble, cleaned texts. Backup `_backup_login_activity_2/release110_20261019/`.

## App texts: English everywhere, no OTTAA branding, values-en removed – 19 Oct 2026
- Finding: `res/values-en/strings.xml` (+ `strings_activity_login.xml`) overrode the default texts on English-language phones with OTTAA's older versions ("Thank you for choosing OTTAA Project", "Rate OTTAA Project", "Welcome, this is OTTAA", and the games message in Spanish: "Upps no se puede jugar en este grupo…"). Both files moved to the backup, so every phone now uses the default (Eraka) texts. `values-en/styles.xml` and `themes.xml` kept unchanged (they style English phones as tested).
- Better wording from values-en copied into the default file first: whisper_* messages ("Listening…", "Voice model is still loading…", "Heard: %1$s", …), "Signing in…", "Fitzgerald Key", "Edit pictogram"/"Edit group", and a few capitalisations.
- Spanish texts in the default file translated: `precisi_n` "Precision", `nino` "Child", `varon1` "Male", arrays Sexo (Choose gender, Male, Female, Fluid, Binary, Other), Horario, Edad, Clima, Posicion (these arrays are not shown on English screens; translated so none can surface). Non-translatable keys/constants left (str_idioma, list_sexo_valores, str_constant_*, child/varon — unused).
- 27 user-visible texts said "OTTAA" → "Eraka"; "the OTTAA logo" → "the Speak button". Kept: the About licence line (credits OTTAA), the prediction switch text (OTTAA order = comparison baseline) and 3 unused non-translatable strings.
- Backup `_backup_login_activity_2/english_strings_20261019/`.

## Stage 3c: remaining Spanish / snake_case class names renamed – 19 Oct 2026
- 50 classes: `GaleriaGruposAdapter` → `GroupGalleryAdapter`, `GaleriaPictosAdapter` → `PictogramGalleryAdapter`, `GaleriaJuegosAdapter` → `GameGalleryAdapter`, `VincularPictosAdapter` → `LinkPictogramsAdapter`, `Item_adapter` → `PreferenceItemAdapter`, `gridViewAdapter` → `ArasaacGridAdapter`, `Custom_recyclerView` → `BaseRecyclerView`, `Picto_Recycler_view` → `PictogramRecyclerView`, `Picto_Recycler_View_Sort` → `PictogramSortRecyclerView`, `Picto_Vincular_Recycler_View` → `PictogramLinkRecyclerView`, `Grupo_Recycler_View` → `GroupRecyclerView`, `Grupo_Recycler_View_Game` → `GroupGameRecyclerView`, `Grupo_Recycler_View_Sort` → `GroupSortRecyclerView`, `Favorite_Phrases_recycler_view` → `FavoritePhrasesRecyclerView`, `MostUsedPhrases_Recycler_View` → `MostUsedPhrasesRecyclerView`, `FindAllPictograms_Recycler_View` → `FindAllPictogramsRecyclerView`, `Game_Recyler_View` → `GameRecyclerView`, `viewpager_galeria_grupo` → `GroupGalleryViewPager`, `viewpager_galeria_pictos` → `PictogramGalleryViewPager`, `viewpager_game_filter_view` → `GameFilterViewPager`, `ViewPager_Game_Grupo` → `GameGroupViewPager`, `Dialog_abstract_class` → `BaseDialog`, `custom_dialog_option_item` → `OptionItemDialog`, `Yes_noDialogs` → `YesNoDialog`, `Yes_no_otheroptionDialog` → `YesNoOtherDialog`, `Devices_Version_Dialog` → `DeviceVersionDialog`, `Progress_dialog_options` → `ProgressDialogOptions`, `NewDialogsOTTAA` → `NewsDialogs`, `EstaConectada` → `ConnectionListener`, `FallanDatosDelUsuario` → `UserDataFailureListener`, `TagInterfazJson` → `TagJsonListener`, `SearchAraasacPictogram` → `ArasaacSearchListener`, `interface_search` → `SearchListener`, `interface_show_dialog_options` → `DialogOptionsListener`, `Lock_Unlocked_Pictograms` → `PictogramLockListener`, `Make_Click_At_Time` → `TimedClickListener`, `translateInterface` → `LabelReadyListener`, `CalculaPuntos` → `ScoreCalculator`, `Juego` → `GameState`, `Reloj` → `GameClock`, `Clima` → `Weather`, `Edad` → `AgeGroup`, `Horario` → `TimeOfDay`, `Posicion` → `Position`, `Sexo` → `Gender`, `HandlerComunicationClass` → `MainBoardHandler`, `ActivityUtilsEstatus` → `ActivityStatusUtils`, `InmersiveMode` → `ImmersiveMode`, `Custom_button` → `CustomButton`, `Json0Recover` → `JsonRecovery`, `myContextWrapper` → `LanguageContextWrapper`. Matching OTTAA test classes renamed too.
- Kept: enum values (NINO, JOVEN, MASCULINO… — stored in settings and starter file names), resource names (R.id/R.layout…), text strings (e.g. "Sexo", "Edad" labels in strings.xml), package folders (Dialogos, Accesibilidad, idioma, Prediction…).
- XML: only fully qualified class names changed (custom view `na.eraka.utils.Custom_button` → `CustomButton` in dialog_number_picker_selector / dialog_yes_no_cancel).
- Checks: no old name left in code, class = file name everywhere, no duplicate class names, no clash with PictogramsLibrary classes, no wildcard imports.
- 103 files changed, 57 renamed. Backup `_backup_login_activity_2/rename_3c_20261019/`.

## Stage 3b, batch 2: helper classes renamed to English – 19 Oct 2026
- `Historial` → `MessageHistory`, `CompartirArchivos` → `MessageSharer`, `BarridoPantalla` → `ScreenScanning`, `idioma/ConfigurarIdioma` → `idioma/AppLanguage`, `Translates/traducirTexto` → `Translates/PictogramLabel`, `textToSpeech` → `SpeechOutput`, `DatosDeUso` → `UsageData`, `Bitmap/GestionarBitmap` → `Bitmap/BitmapManager`, `Bitmap/bajarFotos` → `Bitmap/PhotoDownloader`, `Custom_Picto` → `PictogramView`, `AsignTags` → `TagAssigner`, `BuscarArasaac` → `ArasaacClient`, `verificarPaqueteInstalado` → `InstalledPackageCheck`, `timer_pictogram_clicker` → `PictogramClickTimer`, `FirebaseRequests/SubirArchivosFirebase` → `FirebaseUploader`, `SubirBackupFirebase` → `FirebaseBackupUploader`, `utils/UserLicence/LicenciaUsuario` → `UserLicence`. Matching OTTAA tests renamed too (`HistorialTest` → `MessageHistoryTest`, `BarridoPantallaTest` → `ScreenScanningTest`, …).
- `utils/LicenciaUsuario.java` was an unused duplicate of `utils/UserLicence/LicenciaUsuario.java` (only the latter is imported) → moved to the backup.
- Kept on purpose: menu item id `R.id.bajarFotos` and the `declare-styleable name="Custom_Picto"` (+ `R.styleable.Custom_Picto_*`) — resource names, not classes; `traducirTexto` replaced only as a type (it is also a variable name in EditPictogramActivity / FindAllPictograms_Recycler_View); text strings untouched.
- Checks: no old class name left in code; every class matches its file name; no clash with PictogramsLibrary class names.
- 92 files changed, 24 renamed. Backup `_backup_login_activity_2/rename_3b_batch2_20261019/`.

## Smaller avatar speech bubble – 19 Oct 2026
- The bubble shown when the message is spoken (`CustomToast`, layout `custom_toast.xml`) covered the bottom tiles and the button bar. Layout: bubble wraps its content instead of full width, min height 100 → 64 dp, avatar 100 → 56 dp, text max width 260 dp, 18 → 16 sp. `CustomToast`: text size capped at 18 sp (OTTAA's subtitle-size setting defaults to 25 sp); bubble placed 104 dp above the bottom (was 50 px, on top of the button bar), centred. Backup `_backup_login_activity_2/smaller_toast_20261019/`.

## Stage 3b, batch 1: screens renamed to English – 19 Oct 2026
- Classes (file + every use in Java, tests, manifest, layouts/menus `tools:context`):
  `Principal` → `MainBoardActivity`, `GaleriaGrupos2` → `GroupGalleryActivity`, `GaleriaPictos3` → `PictogramGalleryActivity`, `Edit_Picto_Visual` → `EditPictogramActivity`, `GaleriaArasaac` → `ArasaacSearchActivity`, `prefs` → `SettingsActivity`, `LoginActivity2` → `SignInActivity`, `LoginActivity2Step2` → `ChildProfileActivity`, `LoginActivity2Avatar` → `AvatarChoiceActivity`, `Activities/Groups_TellStory` → `Activities/StoryGroupPickerActivity`, `Activities/Phrases/VincularFrases` → `Activities/Phrases/LinkPhrasesActivity`.
- Method: a script replaced the names only in code and comments, never inside text strings (analytics labels such as "Principal", log tags and @AddTrace names stay, so nothing stored or reported changes). `prefs` is also an ordinary variable name (SharedPreferences prefs) in several classes, so only its class uses were replaced (class declaration, `prefs.class`, `prefs.this`, import, `HandlerComunicationClass` field/constructor, manifest). Settings keys, JSON fields and file names unchanged.
- Check afterwards: no old name left in code; every file's class matches its file name; only `res/xml/headers.xml` still names `com.stonefacesoftware.ottaa.prefs$PresentacionFragment` (already a dead OTTAA reference, unchanged) and the uncompiled `res/res/` leftovers.
- 54 files changed, 11 renamed. Backup (originals with their old names) `_backup_login_activity_2/rename_3b_batch1_20261019/`.

## Stage 3a final sweep (group 3): unused classes, layouts, firebase-functions – 19 Oct 2026
- New reachability check (same method as group 1: start from everything Android can open — manifest, XML — and follow class names in code with comments and strings removed): 280 classes, 277 reachable. Deleted the 2 newly unreachable: `utils/TalkActions/TalkAction` (base class of the deleted TalkChatGPT) and `Viewpagers/viewpager_galeria_juegos` (old games gallery). `erakanlg/ErakaNlgProbe` kept on purpose (SimpleNLG feasibility test for the thesis, run by its unit test).
- `app/build.gradle`: `firebase-functions` dependency commented out (no code uses Firebase Functions any more).
- Layouts: 53 layouts (76 files across layout / layout-hdpi / layout-land / layout-ldrtl) that no code, layout, menu or manifest refers to were moved out — old OTTAA screens and dialogs (licence, reports, location, tutorial, old principal v2, old login step 3, old games pieces, samples…). Kept `group.xml` and `pictogram.xml` although unreferenced in the app: they override the PictogramsLibrary's own layouts (the library defines colorchooser, gamegroup, group, pictogram). Checked that no view id used in Java was defined only in a moved layout.
- Backup `_backup_login_activity_2/dead_code_group3_20261019/` (same paths as in the project).

## "Tell a story" game works offline – 19 Oct 2026
- Keneke's decision: keep the game, make it offline. OTTAA sent the four chosen pictures to ChatGPT (`text-davinci-003`) through a Firebase callable function that Eraka doesn't have, so the speaker button never produced a story.
- New `erakanlg/ErakaStoryMaker` (pure Java): `make(who, what, does, where)` fills one of 3 short templates ("Once upon a time, there was a happy dog. One day, the dog wanted to play. So the dog went to the park and had lots of fun. The end!"); a/an, "home" without "the", "to eat" not doubled, empty slots get neutral words; the same four pictures always give the same story (template chosen from the words). New `ErakaStoryMakerTest`: 7/7 pass on the PC.
- `TellAStory.executeChatGpt()` (name kept for the button) now builds the story with ErakaStoryMaker, shows the eye (story text) and share buttons and reads it aloud; later taps repeat it until a picture changes. ChatGPT prompt, Remote Config prompt download and TellStoryPhrase removed.
- Deleted: `TellStoryPhrase`, `TalkChatGPT`, `HandlerChatgptGame`, `TraducirFrase` (last users of the ChatGPT/online-translation chain). The `firebase-functions` dependency is still in build.gradle (now unused; can go in a later clean-up).
- Backup `_backup_login_activity_2/story_offline_20261019/`.

## Stage 3a group 2, step B: OTTAA "experimental mode" removed (NLG, ChatGPT, online translation) – 19 Oct 2026
- OTTAA's experimental mode sent the message to its old sentence maker `NLG`, to ChatGPT (Firebase callable function, `text-davinci-003`) or to an online translator (libretranslate) before speaking or sharing. Eraka's own sentence bar (ErakaSentenceBuilder + SimpleNLG, offline) replaces it.
- `Principal`: `hablarModoExperimental()` now just calls `speak()` (name kept: several buttons call it); `shareAction()` always shares the message as built; `useWhisperWords()` → `confirmWhisperPhrase()` (no ChatGPT clean-up); removed `nlgTalkAction()`, `EjecutarNLG()`, `onTextoTraducido()` (+ `implements translateInterface`), the unused `runNlgProbeOnce()` (OTTAA-NLG comparison, 11 Oct; results kept in the changelog/thesis), fields traducirfrase / traducirFrase / processPhrase, and `historial.downloadPromt()` (read a ChatGPT prompt from Firebase at start).
- `Historial`: NLG field, ChatGPT/Whisper prompts and `talkWithtNLG`, `nlgObject`, `whisperNlgObject`, `viterbyObject`, `chatGptObject`, `downloadPromt` removed (message list functions unchanged).
- `traducirTexto`: online translation removed; `traducirIdioma()` keeps the text as typed and answers at once (the edit screens still read getTexto/getmSource/getmTarget).
- Settings: the "Language" page (language picker, Experimental mode, ChatGPT) hidden by `prefs.hideLanguageSettings()`; both switches forced off.
- Deleted: `NLG.java`, `utils/TalkActions/ProcessPhrase.java`, old OTTAA test `NLGTest`; `HistorialTest.talkWithtNLG` removed. `TellAStory`: unused ProcessPhrase import.
- Kept for now: the "Tell a story" game's ChatGPT chain (`TellStoryPhrase`, `TalkChatGPT`, `HandlerChatgptGame`, `TraducirFrase`) — it needs a Firebase function Eraka doesn't have (Spark plan), so the story step can't work; decision pending (remove the game or make it offline).
- `ErakaNlgProbe` + its test kept (thesis evidence). Backup `_backup_login_activity_2/dead_code_group2b_20261019/`.

## English only + stage 3a group 2, step A (AboutOttaa, MainJuegos, OTTAA cloud calls) – 19 Oct 2026
- **Always English (Keneke's decision):** `app/build.gradle` `resConfigs 'en'` (was 'ar,en,es,it': OTTAA's Arabic/Spanish/Italian texts are no longer packed). `ErakaApp.forceEnglish()` (in onCreate, onConfigurationChanged and before every screen): `Locale.setDefault(Locale.ENGLISH)` so all of OTTAA's `Locale.getDefault()` calls (board files, sign-up) say "en"; pref `idioma` = "en"; `ConfigurarIdioma.setLanguage("en")`. A Spanish tablet now gets the English board and screens. Backup `_backup_login_activity_2/english_only_20261019/`.
- **Privacy fix:** OTTAA posted the user's e-mail, name and phone number to OTTAA's own cloud function (`us-central1-ottaa-project.cloudfunctions.net/add2listwelcome`) on every sign-in, and to `add2list` from a news dialog. Both calls removed, `utils/CloudFunctionHTTPRequest` deleted.
- Deleted (unreachable): `MainJuegos` (old games menu; replaced by ErakaGamesActivity; manifest entry was commented out), `utils/AboutOttaa` (+ manifest entry; About Eraka replaces it), layouts `activity_main_juegos`, `app_bar_game`, `activity_about_ottaa2` (layout, layout-hdpi, layout-ldrtl). `GameControl` / `ScrollFuntionGames`: MainJuegos constructor, field and branches removed (the games keep using them). `Principal`: unused `startGames()` and AboutOttaa import removed.
- Checked: no remaining code references; no view id used in Java lost its layout. Backup (moved files + edited originals) `_backup_login_activity_2/dead_code_group2a_20261019/`.
- Next (step B): OTTAA "experimental mode" chain — NLG, ChatGPT (TalkChatGPT, HandlerChatgptGame, ProcessPhrase), translation (TraducirFrase, traducirTexto) — used by speak/share/edit; needs code changes in Principal, Edit_Picto_Visual, FindAllPictograms, TellAStory.

## Demo fixes (16–18 Oct) ported from the Downloads copy – 19 Oct 2026
All changes made in `Downloads\OTTAAProject - Copy - Copy` since this project was copied (13 Oct) are now here too, with `com.stonefacesoft.ottaa` → `na.eraka`. Each file was merged three ways (this project + the demo's "before" backup + the demo's current file) with git merge-file, so this project's own changes (namespace, deleted dead code, paywall removal) were kept; no conflicts.
- Tablets: OTTAA's old `layout-sw600dp` + `layout-ldrtl-sw600dp` moved to `_backup_login_activity_2/tablet_layouts_20261019/` (every layout still has a default in `layout/`).
- New `ErakaApp` (manifest `android:name=".ErakaApp"`): tablets (physical screen ≥ 600 dp) → landscape, set in `onActivityPreCreated` (Android 10+) / `onActivityCreated` (older); SplashActivity and Principal have no manifest orientation and get portrait on phones from `ErakaApp`.
- SplashActivity: `openNext()` opens the next screen only once; manifest `configChanges` on the splash.
- New `layout-land/activity_principal_v4.xml` + `_right.xml` (tile box fills the free height; not centred) and `layout-land/edit_picto_visual.xml` (two columns).
- Grass strip capped at 110 dp (login 1 / 2 / avatar, cropper, splash; layout + hdpi); sign-in mascot min 110 dp.
- Gallery long press → "Add to sentence" (`Picto_Recycler_view`, `viewpager_galeria_pictos`, `PopupMenuUtils.inflateWithAddToSentence`, string `eraka_add_to_sentence`).
- `Principal.updateSentenceSuggestion()` null guard.
- Sign-up on a Spanish device: `ErakaFileStore` fallback for `pictos/es/pictos_es_<gender>.txt` and `DownloadPictograms.useBundledEnglish()` safety net.
- `app/build.gradle`: versionCode 109.
- Check afterwards: java, test, res (layout, layout-hdpi, layout-land, values, menu, xml), assets, manifest and build.gradle compared with the demo (ignoring the package name and line endings): identical except this project's own stage-2/3a changes.
- Backup of every replaced file: `_backup_login_activity_2/port_from_demo_20261019/` (same paths as in the project).

## Sign-out message in English – 17 Oct 2026
- `values/strings.xml`: `expired_sesions` was Spanish ("Su sesion expiró, vuelva a iniciar sesion.") and there is no English override, so English users saw Spanish when signing out. Now "Your session has expired. Please sign in again." (Downloads demo copy and Eraka project.) Other Spanish texts remain in the default strings file (e.g. "Elija una opción", "Mañana", "Niño", "Precisión") – to translate with stage 3.

## Stage 3a group 2, step 1: paywall removed – 13 Oct 2026
- OTTAA's "licence expired" screen could no longer appear (all premium checks return true since Sept), but 9 classes still pointed to it in their "not paid" branches.
- Removed those `else { … LicenciaExpirada … }` branches in `GaleriaPictos3` (3: new pictogram, link, sort), `Grupo_Recycler_View`, `Picto_Recycler_view`, `viewpager_galeria_grupo`, `viewpager_galeria_pictos`, `viewpager_game_filter_view` (edit), `prefs.showPaymentMessage()`; `Custom_recyclerView.startExpiredLicense()` deleted; unused imports removed (also `Groups_TellStory`). The paid branch is the one that always runs, so behaviour is unchanged.
- Deleted `LicenciaExpirada.java`, `Adapters/LicenciaExpiradaAdapter.java`, layouts `activity_licencia_expirada.xml` + `item_licencia_expirada.xml` (layout/ and layout-sw600dp/) and the manifest entry. Text search: no remaining reference. (Unused strings `licencia_expirada*` left; old copies in the stray `res/res/` folder are not compiled.)
- Backup: `_backup_login_activity_2/paywall_removed_20261013/`.

## Stage 3a: unused OTTAA code removed (group 1) – 13 Oct 2026
- Method: reachability from what Android starts (launcher Splash, declared services/providers, custom views and preferences named in XML), following every class name mentioned in code (comments and string contents ignored). Classes never reached = nothing in the app can use them.
- Deleted (20 classes + 1 test, final list): interfaces `CloudFunctionResponse`, `FindPictogram`, `SingletonListener`, `cargandoOpciones`, `preparePictures`; `DialogTags`, `TutorialDialog`, `Dialog_options_level_game`, `GeneralClassWithScreenScanning`, `AccessibillityDevicesConfig`, `MemoryUtils`, `SelectedID`, `StrictModePolicyClass`, `OutOfMemoryException`, `MyFirebaseInstanceIdService`, `Login_class` (empty), `WhisperLib`, `Custom_ImageDescription` (+ `Custom_ImageDescriptionTest`), screens never opened `ActivityInformes`, `LoginActivity2Step3`, `Viewpager_tutorial` (+ their 3 manifest entries); folders `src/main/cpp` (old unbuilt whisper copy, 30 files), `src/main/gen` (old Eclipse files), empty `java/com/stonefacesoft/`.
- **Correction during the step:** `GaleriaArasaac` was first flagged and deleted, then restored: the first checker treated the text "image/*" in `Edit_Picto_Visual` as a comment start and missed `new Intent(…, GaleriaArasaac.class)` (the ARASAAC button). Its helpers `gridViewAdapter` and `DownloadArasaac` (only used by GaleriaArasaac) were also restored after the A13 build failed on them. Final check = plain text search without comment stripping: none of the 20 deleted classes is mentioned anywhere in the code (except one comment in ErakaReportActivity); manifest valid XML. Lesson: the compiler is the final judge. **A13-verified 13 Oct** (Clean + Run; board, settings, galleries, voice, speech check, Edit pictogram → ARASAAC search).
- Backup of everything deleted + old manifest: `_backup_login_activity_2/dead_code_group1_20261013/`.
- Group 2 (later, needs caller edits): `LicenciaExpirada` + adapter, `AboutOttaa`, `MainJuegos`, ChatGPT / online translation / cloud-function chain, OTTAA `NLG`.

## Renaming stage 2: Java namespace com.stonefacesoft.ottaa → na.eraka – 13 Oct 2026
- Folders moved: `app/src/{main,test,androidTest}/java/com/stonefacesoft/ottaa/` → `java/na/eraka/` (one directory move each; contents unchanged).
- Text replaced `com.stonefacesoft.ottaa` → `na.eraka` in 369 of 370 Java/Kotlin files (package, imports, fully written class names) and 57 resource XML files (custom views in layouts, menus, `xml/preferences.xml`, `values/strings.xml`: unused `fileProviderGeneral` and the notification channel id); `app/build.gradle` `namespace 'na.eraka'` (so R and BuildConfig are now `na.eraka.R` / `na.eraka.BuildConfig`); `proguard-rules.pro` keep rule.
- Unchanged on purpose: applicationId `na.eraka.app` (so the installed app and its data stay), the PictogramsLibrary package `com.stonefacesoft.pictogramslibrary` (external JitPack library), stored JSON/preference keys. Left as old leftovers (not built): `src/main/gen/` (old Eclipse files) and `src/main/cpp/whisper_jni.cpp` + `utils/WhisperLib.java`; empty folders `java/com/stonefacesoft/`.
- **A13-verified 13 Oct** (Clean + Run; board, settings, galleries, voice, About, speech check work).
- Checks: no file in java/test/androidTest/manifest mentions the old name; every file's package matches its folder; 48/48 PC tests pass with the new packages (predictor 16, explorer 5, ignore 5, speech check 7, sentences 14, NLG probe 1).
- Backup: `_backup_login_activity_2/rename_stage2_20261013/` (build.gradle, proguard-rules.pro); full pre-rename project = `Downloads\OTTAAProject - Copy - Copy`.

## Own project: copy + renaming, stage 1 – 13 Oct 2026
- Working project is now `C:\Users\KEKasaona\Desktop\My Projects\Eraka` (full copy of `Downloads\OTTAAProject - Copy - Copy`, checked file by file; the old folder stays untouched as fallback). Builds and runs on the A13.
- Stage 1: `settings.gradle` → `rootProject.name = "Eraka"` (Android Studio shows the project as Eraka). Backup `_backup_login_activity_2/rename_stage1_20261013/`.
- Found for stage 2: Java namespace `com.stonefacesoft.ottaa` used in 311 Java + 1 Kotlin files (main), tests, 57 res XML files, `proguard-rules.pro`; no reflection on own class names; `utils/WhisperLib.java` + `src/main/cpp/whisper_jni.cpp` are unused leftovers (JNI names tied to the old package; no externalNativeBuild) – Whisper runs through the `:lib` module (`com.whispercpp.whisper`) via `WhisperBridge.kt`.

## Release prep: version 108 / "1.0-study" – 13 Oct 2026
- `app/build.gradle`: versionCode 107 → 108, versionName "6.7.17" → "1.0-study". Raise versionCode by 1 for every APK sent out; always sign with `eraka_first_release`.
- **Same day: applicationId → `na.eraka.app`** (debug `na.eraka.app.debug`). New `app/google-services.json` from Firebase (project otta-project-1ee57) lists all 4 apps; SHA-1s checked: na.eraka.app = release key `B5:FA:F3:44:…:B1:3B`, na.eraka.app.debug = debug key `43:6D:3D:52:…:CA:75`. No code hard-codes the old ID (FileProvider uses `${applicationId}`, lookups use `getPackageName()`); Java namespace unchanged. Effect: Eraka installs as a new app next to OTTAA and the old debug app (fresh data; Keneke exported his test logs first with Send usage data).
- **A13 release test passed (13 Oct):** signed APK installed beside OTTAA; Google sign-in with another account works (release SHA-1 OK); Send usage data from the release build → zip with filled CSVs (logging works outside debug).
- Backup: `_backup_login_activity_2/release_20261013/`.

## ARASAAC credit in About – 13 Oct 2026
- `activity_eraka_about.xml`: two small lines under the licence line: ARASAAC attribution (`about_arasaac`: property of the Government of Aragón, created by Sergio Palao for ARASAAC, arasaac.org link, CC BY-NC-SA; notes that some English labels were corrected) and third-party parts (`about_third_party`: whisper.cpp/Whisper MIT, SimpleNLG MPL 2.0). Strings in `strings_eraka_menu.xml`.
- Backup: `_backup_login_activity_2/arasaac_credit_20261013/`.

## Study tools: "Send usage data" + "Speech check" – 13 Oct 2026
- Two new **caregiver-locked** menu items (group "general", behind CaregiverGate): **Speech check** and **Send usage data** (`activity_main_drawer.xml`, strings in `values/strings.xml`).
- **Send usage data** – new `utils/ErakaDataExport.java`: asks once for a **child code** (e.g. C01, cleaned to A–Z/0–9, saved in prefs `eraka_child_code`), then on the usage log's writer thread (so every logged tap is saved first) packs one zip `eraka_<code>_<yyyyMMdd_HHmm>.zip` in the app cache (`eraka_share/`, only the newest kept): `info.txt` (code, app version, export time, phone, Android version, current place), `tap_events.csv`, `message_events.csv`, `speech_check.csv`, `voice_eval.csv`. Files inside are named without the account id (pseudonymous). Shared through the existing FileProvider (`<package>.fileprovider`, cache-path) with `ACTION_SEND` application/zip → WhatsApp, Gmail, Bluetooth… No audio, photos or names.
- **Speech check** – new `erakaprediction/ErakaSpeechCheck.java` (pure Java): 10 fixed words (water 500, milk 495, apple 474, ball 996, dog 342, mom 628, dad 632, eat 118, drink 51, happy 171); `recognised()` = the word itself (or its plural) appears as a whole word in what Whisper heard ("what a" ≠ water); `rankOf()` = place of the picture in the voice suggestions (0 = not offered). Flow in `Principal`: phase (start / end of week) → dialog with picture + word → **Record** (Whisper with the normal vocabulary hint; VAD stops by itself, or **Stop**) → shows what was heard → next word; **Skip** logs ms = −1. Uses `VoiceDictionary.match()` read-only: nothing is taught, no audio kept. Cancel while recording = that word is not logged. Toast at the end: "x of n words recognised". Mic permission request code 102.
- Log **v5**: new table `speech_check` (ts, check_id, phase, picto_id, label, heard, recognised, rank, ms); `ErakaUsageLog.logSpeechCheck()`, `uid()`; `exportCsv()` also writes `speech_check_<uid>.csv`, is synchronized, and CSV values now drop line breaks as well as commas.
- Tests: new `ErakaSpeechCheckTest` (7) → **7/7 pass** on the PC (with predictor 16, explorer 5, ignore 5, sentences 14 = 47).
- Backup: `_backup_login_activity_2/send_data_20261013/`.

## Sentence suggestions: templates + SimpleNLG – 13 Oct 2026
- New `erakanlg/ErakaSentenceBuilder.java` (pure Java + SimpleNLG 4.4.8 from app/libs): message pictures → **roles** from OTTAA's word type `tipo` (1 person, 2 thing, 3 action, 4 describing, 5 social, 6 other) corrected by small word lists (want/need = WANT; I like / I don't like; be; places; mass nouns such as water, milk, oshifima; numbers; feelings; request verbs give/help/open…; cook = action, hungry = describing) → **templates decide the structure** (want_thing, can_have, thing_please, vocative_want, want_action, do_action, want_go_place, request, be_describe, feel_describe, like, dislike, social, "…, please") → **SimpleNLG does the grammar** (a/an, "to" + verb, am/is/wants, plurals with numbers, questions, imperatives, "play with", "don't"). Repeated pictures counted once. No sentence when it is too complex (prepositions, question words, two actions, "want" alone) → never "I want to want." Only the child's own pictures give content words.
- New `erakanlg/ErakaTemplateStats.java`: per-child accept/ignore counts; score (accepted+1)/(shown+2); stable re-order (builder order for ties).
- Screen: new `erakaSentenceBar` under the picture strip in `activity_principal_v4.xml` and `_right.xml` (text + speaker button = **accept** (says the sentence) + x = **ignore** (hidden until the message changes)); `gone` when nothing fits or the sentence equals the words; the 4-tile panel now hangs under the bar.
- `Principal`: `initSentenceSuggestions()` (replaces the SimpleNLG probe call; lexicon loaded once in a background thread; stats rebuilt from the log), `updateSentenceSuggestion()` (called from `refreshSmartRow()` after every message change), `acceptSentence()`, `ignoreSentence()`; normal Speak while a sentence is shown logs `sentence_not_used` (so acceptance is a conscious choice, not a side effect of Speak).
- Log **v4**: `message_events.template` + `sentence`; types `sentence_accepted`, `sentence_ignored`, `sentence_not_used`; `ErakaUsageLog.loadSentenceEvents()`.
- Tests: new `ErakaSentenceBuilderTest` (12 tests: want thing/action, never want-to-want, places, feelings, other people/requests, numbers + two things, social + please, like/dislike, too complex → nothing, repeats, stats re-order) → **12/12 pass** on the PC.
- Teacher review sheet: `Documents\Eraka\sentence_templates\Eraka_sentence_templates_review.xlsx` (24 examples, correct?/natural?/better wording).
- **Fixes after the A13 test (same day):** (1) order does not matter for describing words: "happy mom" → "Mom is happy.", "tired I" → "I am tired."; (2) plural-looking things take no article: "Dad, give me milk and cookies." (was "a milk and cookies"), "I want chestnuts."; (3) template learning is now per template **and kind of message** (`Candidate.key`, e.g. `want_action@ACTION+THING`, stored in message_events.template), so ignoring "I want to drink water." no longer moved "I go to school." ahead of "I want to go to school.". Tests 14/14 (`orderDoesNotMatterForDescribingWords`, `pluralThingsHaveNoArticle`, stats test extended). Previous builder kept as `ErakaSentenceBuilder_v1.java` in the backup folder.
- Backup: `_backup_login_activity_2/sentences_20261013/`.

## Step 5b: learning from ignored suggestions – 13 Oct 2026
- New `erakaprediction/ErakaIgnoreStats.java` (pure Java): per (previous picture, suggested picture) it counts, over the last 14 days, how often the picture was shown in the **exploitation slots** of the suggestion row and how often it was taken. Score multiplier m = (taken+1) / (taken+1 + 0.25·ignored): never ignored = 1, ignored 4× = 0.5, 12× = 0.25; taking it again raises it. Context-specific (ignored after "I want" says nothing about after "eat").
- `ErakaPredictionService.rankSuggestions()`: model probability × m, looking further down the model's list (3× the slots), so a picture the child keeps ignoring makes room for the next one. `onSuggestionDecision()` updates live; stats rebuilt from the log on reload (`ErakaUsageLog.loadSuggestionEvents()`: shown_smart minus explore_ids; older rows: all shown_smart).
- `ErakaSmartRowDataSource`: slots 1–4 use `rankSuggestions()`. `Principal.logTapEvent()` records the decision (smart row minus exploration slots).
- **Not applied to the 4 tiles**, on purpose: tiles keep stable positions (children learn where pictures are – motor planning). Exploration slots already learn from ignoring via UCB (5a).
- No new log column needed (uses shown_smart + explore_ids). Probabilities in the replay are unchanged; 5b is evaluated from live logs (rank_smart over time).
- Tests: new `ErakaIgnoreStatsTest` (5) → **26/26 pass** (predictor 16, explorer 5, ignore 5).
- Backup: `_backup_login_activity_2/ignored_20261013/`.

## Pictogram audit applied: hidden, caregiver-only and renamed pictures – 13 Oct 2026
- Audit file: `Documents\Eraka\pictogram_audit\Eraka_pictogram_audit.xlsx` (585 built-in pictures; script `tools/pictogram_audit/`). Keneke's decisions + follow-up answers (chubby remove; kiss, knife keep; abuse ×2 and medicines caregiver-only; bikini, bra remove; blank duplicates: Claude keeps one where the meaning is the same).
- New asset `assets/eraka_corrections/vocabulary_corrections.json`: **hide 40** (make love, boobs, drunk, beer, wine, bar, disco, rehab, crazy, chubby, ugly, in love ×2, invoice, Blockbuster, butt, bikini, bra, second rooster, 21 duplicates), **caregiver_only 7** (physical abuse, verbal abuse, aspirin, decongestant, digestive, syrup, pills), **rename 41** (e.g. cock→rooster, hook up→button up, escort→go with, shamefaced→embarrassed, Football→soccer, lean→clean, desert→dessert, seat→sit down, beat legs→kick, snicker→takkie, change diapper→change nappy; duplicates given distinct labels: canary, cold drink, a little, pretty, who is it?, help me, a drink, autumn, in front of, bread roll, a cold). All "from" labels checked against pictos_en.txt.
- New `utils/ErakaVocabulary.java`: loads the corrections once; `applyToPictos()` renames (only while the label is still OTTAA's original, so a parent's own label is never overwritten) and removes hidden + caregiver-only pictures from every picture's links (tiles); `applyToGroups()` removes hidden pictures from all gallery groups (incl. "ALL"; caregiver-only stay for adults); `isHidden()`, `isHiddenForChild()`.
- Applied in `Json.initJsonArrays()` and `setmJSONArrayTodosLosPictos/Grupos()` (restore paths) → new users, existing users and restored backups. OTTAA's files and the pictures are not edited.
- Extra guards: `ErakaSmartRowDataSource.toItem()` (suggestions + exploration + base model), `HeuristicSmartRowDataSource` (OTTAA mode), `Principal.topUpTiles()`, `VoiceDictionary.match()` (voice never offers them).
- Backup: `_backup_login_activity_2/vocab_corrections_20261013/`.

## Tiles: a picture already in the message is not shown again – 13 Oct 2026
- A13 test of step 5a: `explore_ids` filled and rotating (e.g. 385 388 → 642 51 → 34 85 → 14 171 → 272 137 …); exploration pictures tapped 3× (rank_smart 5/6) ✓.
- Bug found in the same test: water tapped in the smart row → water appeared again among the 4 tiles. Cause: `rankChildrenWithEraka()` only moved pictures already in the message to the end of the group; a group with ≤ 4 pictures (here water's links, which also contain water itself since the "water water water" spam test added an OTTAA link water → water) still showed it.
- Fix (`Principal.rankChildrenWithEraka()`): pictures already in the message are removed from the tile list (also for groups with 1 picture: the old "< 2 → return" shortcut was removed). If fewer than 4 remain, new `topUpTiles()` fills the first page with the model's next best pictures, then core words (new `ErakaSmartRowDataSource.coreWordIds()`), never duplicates or message pictures. Eraka off: unchanged (OTTAA order).
- Backup: `_backup_login_activity_2/tiles_no_repeat_20261013/`.

## Step 5a: two exploration slots in the suggestion row – 13 Oct 2026
- Exploration vs exploitation (supervisor feedback 4 Oct; Keneke chose **2 slots**). With Eraka on, the 6-slot suggestion row = **slots 1–4 exploitation** (model's best pictures, free slots core words) + **slots 5–6 exploration** (pictures the child has not used yet). OTTAA mode (switch off): unchanged, no exploration.
- New `erakaprediction/ErakaExplorer.java` (pure Java): UCB score = (picks+1)/(shows+2) + C·sqrt(ln(decisions+2)/(shows+1)), C = 1.0; never-shown pictures get the largest bonus → new pictures take turns; shown-but-ignored pictures slowly drop; taken ones keep a high score. Pictures used ≥ 3 times (`NEW_UNTIL_USES`) are not "new". Stable sort: ties keep the candidate order. `EXPLORE_SLOTS = 2` (one number to change).
- Candidates (preference order): pictures linked to the previous picture in the vocabulary (`relacion`, stored order) → base-model pictures (most common first) → core words. Never what is on the tiles, in slots 1–4 or in the message. If fewer new pictures exist, the free slots get core words (not logged as exploration).
- `ErakaPredictor`: `timesUsed()` (real taps per picture, no decay; repeats/undone not counted), `pictosByFrequency()`, `basePictosByFrequency()`.
- `ErakaPredictionService`: `pickExplore()`, `setCurrentExplore()/currentExplore()`, `onExploreDecision()`; the explorer statistics are rebuilt from the log on every reload (`ErakaUsageLog.loadExploreEvents()`).
- Log **v3**: new column `tap_events.explore_ids` (the exploration pictures on screen at each tap; added by `onUpgrade`, no data lost). `Principal.logTapEvent()` logs them and records the decision (shown → picked or not).
- Tests: new `ErakaExplorerTest` (5 tests: new child order, rotation after ignoring, taken > ignored, exclusions/well-known skipped, duplicates) + `ErakaPredictorTest` → **21/21 pass** on the PC. Android part to be checked on the A13.
- Evaluation: exploration is measured from live logs (how often an exploration picture is taken; new pictures discovered per child), not by replay.
- Backup: `_backup_login_activity_2/explore_20261013/`.

## Spamming fix: repeated taps are learned once per message – 13 Oct 2026
- Problem: a child tapping the same picture several times in one message ("water water water") made the model count it several times, and taught it "after water comes water".
- `ErakaPredictor.Tap`: new field `repeat` (new 8-argument constructor; the old 7-argument one means repeat = false, so tests and the base model are unchanged). `addTap()` ignores taps that are undone **or** repeat.
- `ErakaPredictor.findRepeats(session[], position[], picto[])` (new, pure Java): a tap is a repeat when its picture is already in the message on screen. It rebuilds the message per session from the positions, cutting it back to each tap's position first, so a picture removed with undo and tapped again is not a repeat.
- `ErakaUsageLog.loadTaps()` marks repeats with `findRepeats` (as it already marks undone taps). `ErakaPredictionService.onTap(id, prev1, prev2, repeat)` skips repeats live; `Principal.logTapEvent()` checks whether the picture is already in the message. **Every tap is still logged**, so repeat rates can be reported.
- `tools/eraka_replay_eval.py`: `mark_repeats()` (same rule); repeats are neither predicted nor learned (like undone taps; the demo's "as written" model still learns them); report shows the repeat count. `--selftest` OK.
- Tests: `ErakaPredictorTest` + `repeatedTapsAreLearnedOnce`, `findRepeatsFollowsWhatIsOnScreen` → **16/16 pass**. Replay on the 11 Oct log (`..._db3`): 0 repeats (108 taps, 29 undone, 79 evaluated) – the fix is preventive.
- Backup: `_backup_login_activity_2/spam_fix_20261013/`.

## SimpleNLG feasibility test – 11 Oct 2026
- Finding: SimpleNLG is **already in the app** — `app/libs/SimpleNLG-4.4.8.jar` (270 KB, includes its English lexicon `default-lexicon.xml`), pulled in by `fileTree(dir: 'libs')`. OTTAA uses it in `NLG.java` (by Hector, 2016) via `Historial.talkWithtNLG()`, only in OTTAA's "experimental mode", English only, and the result is then sent to an online translator. It builds the clause from each pictogram's own **word type `tipo`** (1 subject/person, 2 noun, 3 action, 4 adjective, 5 social, 6 misc — Fitzgerald-key colours) and has a "want/need/have + verb → want to …" rule. No build.gradle change and no extra APK size needed.
- New `erakanlg/ErakaNlgProbe.java` (pure Java): loads the default lexicon (timed) and builds 12 test sentences directly with the SimpleNLG API, the way a template would supply roles: I want a dog / an apple / water / to eat; Mom is happy; I am tired; I feel happy; Mom, give me meat; Dad goes to the shop; Can I have an apple?; Dad wants two apples; I ate an apple. Reports each output, expected, ms.
- New `app/src/test/.../erakanlg/ErakaNlgProbeTest.java`: runs the probe on the PC. Result (JDK 21, SimpleNLG 4.4.8): **12/12 as expected**, lexicon load 534 ms, first sentence 37 ms, then ~0.3–1.4 ms per sentence.
- `Principal.runNlgProbeOnce()`: debug builds only, once per app start, background thread; logs the probe report and runs OTTAA's existing `NLG` class on 8 picture sequences (I want water / eat / play ball, mom happy, dad give juice, want apple, go school, I tired) for comparison. Logcat tag `ErakaNLG`. No UI change.
- Backup: `_backup_login_activity_2/simplenlg_probe_20261011/`.

## Replay: Eduardo's predictor added as a comparison – 11 Oct 2026
- `tools/eraka_replay_eval.py`: new models `eduardo` (port of `NextPicturePredictor.kt` from eraka-demo-dev v0.4, as written: 0.55/0.30/0.10 + 0.25 × OTTAA prior from his `priors.json`, category boosts for place/time, last-use recency, context kept across messages, undone taps learned), `eduardo_fixed` (same formula, context from the message, undone ignored), `eduardo_noprior` (n-gram part only) and `eraka_ottaa_prior` (Eraka + the same prior as a stand-in base model, test only). Assets read from `Documents\Eraka\eraka-demo-dev\…\matcher\src\main\assets` (override with env `ERAKA_DEMO_ASSETS`).
- Practice log 7 Oct (38 taps, one adult, not a result): n-gram alone Eraka top-1 68.4 % / MRR 0.706 vs eduardo_noprior 57.9 % / 0.645; with priors eduardo_fixed 63.2 % / top-4 84.2 % / 0.722 vs eraka_ottaa_prior 63.2 % / 78.9 % / 0.705; demo as written 57.9 % / 81.6 % / 0.677. The prior raises top-4 above the 73.7 % first-use ceiling.
- Backup: `_backup_login_activity_2/eraka_replay_eval_20261011.py`.

## Step 4: replay evaluation script – 7 Oct 2026
- New `tools/eraka_predictor.py`: Python copy of `ErakaPredictor.java` (same formulas/defaults, base-model mix). `--selftest` reproduces the Java unit tests exactly (0.507/0.253; 118 with P 0.676, 10 taps used).
- New `tools/eraka_replay_eval.py <log folder> [--warmup 20]`: reads tap/message CSVs (app export `,` or Database Inspector export `;`), marks undone taps (same rule as `loadTaps`), replays taps in time order: before each real tap every model ranks the pictures (prev 2, hour, place; pictures already in the message excluded), records the rank of the chosen picture, then learns the tap (online, past only). Metrics top-1 / top-4 (tiles) / top-6 (suggestion row) / MRR, all taps and after warm-up, plus the first-use ceiling and a "live" table from the logged ranks per active model.
- Models: most_used, bigram, stupid_backoff (Brants 2007), trigram_plain, eraka_no_ctx and eraka_no_rec (ablations), eraka, eraka_base (if the base asset exists), ottaa (offline approximation: starter relation counts from pictos_en.txt + 1 per tap incl. undone, 2 × count among the previous picture's related pictures).
- Writes `eval_report.md` and `eval_ranks.csv` into the log folder.
- `--selftest` also runs a known-answer replay (4 hand-made messages incl. an undone picture; expected ranks worked out by hand: first use = 0, repeat = 1, undone neither predicted nor learned, pictures in the message never ranked) → OK.
- Added baseline `speakbright` (same day): rebuild of SpeakBright's Temporal PrefixSpan recommender (Reroma et al. 2026) — last 14 days, ±30 min of the current time of day, sessions split at gaps > 10 min, pictures following the current picture ranked by session support; unstated details (any later position, support counted once per session, min support 1, start of message = most frequent in window) documented in the class. Practice log: top-1 21.1 %, top-4 55.3 %, MRR 0.369 (its 10-min sessions span several messages, so 'what follows water' includes the next message's 'I', 'want').
- First run on Keneke's practice log (`Documents\Eraka\usage_logs\2026-10-07`, 55 taps, 38 evaluated): Eraka top-1 68.4 %, MRR 0.706 vs OTTAA approx. 34.2 %, 0.547; 26 % of taps were first-ever uses (ceiling 73.7 % for models starting from zero). Live: tapped picture was tile 1 in 62 % (eraka) vs 44 % (ottaa). Practice data only — pipeline check, not a result.

## Tie-break: group's fixed order instead of OTTAA's counts – 7 Oct 2026
- Device test showed an undone picture ("search", 4× undone) appearing 3rd after "I want": Eraka ignored it, but pictures Eraka scores equally (score 0) kept OTTAA's order, and OTTAA's `relacion` counts include undone taps.
- `Json.elegirHijos2()`: keeps `fixedOrder` (the `relacion` list as stored, before OTTAA's sort) and gives a copy to the ranker; if the ranker ranked (returns true) that list is used, otherwise OTTAA's sorted list (Eraka off / no data = comparison mode unchanged).
- `Json.ChildrenRanker.reorder()` now returns boolean. `Principal.rankChildrenWithEraka()` returns false when Eraka is off or has no data; its stable sort keeps the fixed order for ties.
- Effect: Eraka's learned pictures first (by probability), then the rest in the group's stable order; undone taps and OTTAA counting no longer influence the tiles while Eraka is on.
- **OTTAA fully out while Eraka is on (same day):** `rankChildrenWithEraka()` now returns false only when the switch is off; with no data yet every picture ties → group's fixed order. `ErakaSmartRowDataSource`: free slots (and the whole row when there is no data) are filled with **core words in a fixed order** (`CORE_WORDS`: want, more, help, stop, yes, no, i, you, go, eat, drink, play, like, finished, all done, come, look, give, happy, sad, tired, hurt; 15 exist in the pictogram set — more, stop, like, finished, all done, come, hurt are missing → candidates for new pictures), instead of the OTTAA-score heuristic. The heuristic is used only when the switch is off (comparison baseline). The `setLimit(18)` from the earlier fix is no longer needed and was removed (setLimit stays available).
- Backup: `_backup_login_activity_2/tiebreak_20261007/`.

## Step 3 device test + smart-row fill fix – 7 Oct 2026
- A13 test (fresh install, 19 → ~50 taps): Logcat "base model not found" + "model trained on 0 taps; base model off", later 19 taps ✓; no tile/message repeats in the smart row ✓; live learning: "I want" → water before eat without restart ✓; OTTAA mode (switch off): after 3× "I want water", water still not in the first suggestions; switch on → water first again ✓ (`model` column eraka/ottaa ✓); undone "search" never passed spoken pictures ✓ (it still appears after them because zero-probability pictures keep OTTAA's order, and OTTAA's own counts include undone taps).
- **Fix:** smart row sometimes showed only 4 suggestions. Cause: the heuristic fallback returned only 6 candidates, and `ErakaSmartRowDataSource` then removed the model's picks and pictures already in the message. `HeuristicSmartRowDataSource.setLimit()` (new) → Eraka source asks for 18 spare candidates; when the heuristic is used alone (Eraka off / no data) its result is cut to 6.
- Open: place showed SCHOOL in message_events but HOME in tap_events — both read `ErakaPlaces.current()` at call time, so most likely the inspector showed the first 50 tap rows (new rows on page 2); to be rechecked with ORDER BY id DESC.
- Backup: `_backup_login_activity_2/smartrow_fill_20261007/`.

## Base model for cold start (starter messages) – 7 Oct 2026
New children now start from a base model learned from Namibian teacher and parent messages instead of OTTAA's ranking.
- `ErakaPredictor`: `setBaseModel()`, `hasBaseModel()`, `baseShare()`. Final probability = (1−b)·P_child + b·P_base with **b = K/(K+n)** (n = child's taps, K = `Config.baseStrength` = 30, "the base is worth 30 taps"): new child → base only; 30 taps → half/half; 270 taps → 90 % own. `explain()` shows the base part and b. Taps with unknown time (hour −1) or place ("") are no longer counted in the time/place tables.
- New `erakaprediction/ErakaBaseModel.java` (pure Java): reads `assets/eraka_base/base_messages.csv` (`source,time,place,picto_ids,text`), turns each message into taps from START (same code path as the child's model), recency off (half-life ∞). Time MORNING/MIDDAY/AFTERNOON/EVENING → hour 8/13/17/20, empty → unknown.
- `ErakaPredictionService`: loads the base model once on the log thread and gives it to every reload; `hasData()` is now true with taps **or** a base model, so Eraka (not OTTAA) ranks from day 1. OTTAA order only remains as tie-break for pictures neither model knows, and when the Settings switch is off. Logcat `ErakaPrediction` shows "base model: N starter taps" and the share.
- New importer `tools/eraka_base_import.py` (run on the PC): `eraka_base_data/raw_messages.csv` (source,time,place,message) → lowercase, strip punctuation, synonyms (`synonyms.csv`), longest picture-name match first with singular fallback, drop function words, map time/place words → writes the asset + `eraka_base_data/import_report.md` (coverage %, words with no picture, names shared by several pictures, per-message check). `choose_id.csv` picks between duplicate names; `extra_vocab.csv` adds pictures (Namibian photo set). Made-up test data in `SAMPLE_raw_messages.csv` / `sample_output/` (10 messages, coverage 84 %; not for the app).
- No asset yet: until the real messages arrive the base model is off and behaviour is unchanged.
- Tests: 5 new (parsing, new child = base only, child's habit takes over, unknown time/place, empty file); all 14 pass off-device (JUnit 4.13.2).
- Backup: `_backup_login_activity_2/base_model_20261007/`.

## Eraka model on screen (step 3) – 6 Oct 2026
The model now drives the smart row and the order of the 4 tiles, with a Settings switch back to OTTAA's order.
- New `erakaprediction/ErakaPredictionService.java`: one per child; `reload()` trains from the usage log on the log's writer thread (after all saved taps); `onTap()` learns live; taps made during a reload are kept and re-added; one lock for all model access (tiles sort on a background thread, smart row on the main thread). Switch pref `eraka_prediction_enabled` (default on); `activeModel()` = eraka / ottaa.
- New `SmartRow/ErakaSmartRowDataSource.java` (replaces HeuristicSmartRowDataSource in Principal): Eraka on + history → model's top 6 for the current message, time and place, excluding the 4 tiles and pictures already in the message; free slots filled by the heuristic. Eraka off or new child → heuristic only.
- `Json`: new `ChildrenRanker` hook, called in `elegirHijos2()` after OTTAA's sort (exceptions keep OTTAA order).
- `Principal`: `rankChildrenWithEraka()` reorders a picture's children by model probability (stable sort, so ties keep OTTAA order; pictures already in the message go last); `currentMessageIds()`; `predictionService()`; model reloads at start and after undo; `onTap()` after each logged tap; startup now calls `refreshSmartRow()` (fix: the first smart-row load ran without the tiles excluded, so a tile could appear twice, seen in the 6 Oct log row 1).
- `ErakaUsageLog` v2: new column `tap_events.model` (eraka/ottaa), added on existing phones by `onUpgrade()` with ALTER TABLE (no data lost); `logTap(..., model)`; `runAfterWrites()`.
- Settings → Pictograms: new "Word suggestions → Learn from this child" switch (`preferences.xml`, strings in `strings_eraka_menu.xml`).
- Checked: new classes compiled against stand-ins; Principal/Json edits need the Android Studio build. Test on the A13: tap a few messages, check the smart row never repeats a tile, check `model` = eraka in Database Inspector, switch off in Settings and check `model` = ottaa.
- Backup: `_backup_login_activity_2/predictor_wiring_20261006/`.

## Eraka prediction model (step 2) – 6 Oct 2026
The model itself; not yet shown on screen (step 3).
- New package `erakaprediction` (lowercase; the existing `prediction` folder holds OTTAA's enums with package name `Prediction`, and Windows treats the two as the same folder, so a separate name avoids a clash).
- New `erakaprediction/ErakaPredictor.java` (pure Java, no Android): personalised **trigram model with Jelinek–Mercer interpolation**, time-decayed counts, time-of-day and place context.
  - P(w|h) = (1−μT−μP)·[λ3·P3(w|prev2,prev1) + λ2·P2(w|prev1) + λ1·P1(w)] + μT·P(w|time bucket) + μP·P(w|place).
  - Defaults (`Config`): λ3 0.5, λ2 0.3, λ1 0.2, μT 0.1, μP 0.1, half-life 14 days, prior weight 1. Levels without data for the current context are left out and the others rescaled (always sums to 1).
  - Recency: each tap's weight is 2^(age since first tap / half-life), equivalent to old taps halving every half-life; lets `addTap()` update live without recounting.
  - Undone taps ignored. Optional cold-start prior via `setPrior()` (fades as real taps grow). Start of message = START (-1).
  - API: `train()`, `addTap()`, `probabilities()`, `rank(query, candidates, exclude, k)`, `explain()` (score breakdown), `timeBucket()` (OTTAA boundaries).
- `ErakaUsageLog.loadTaps()`: reads all taps for the model and marks taps removed by undo (latest tap in that session at position length−1 with the removed picture).
- New unit tests `app/src/test/.../erakaprediction/ErakaPredictorTest.java` (9 tests: I-want-water example = 2/3 vs 1/3, real 6 Oct log → 118 after 643 44 with P=0.676, undone ignored, recency, place, cold start, candidates/exclusions, time buckets). All 9 pass (run off-device with a JUnit stand-in); run in Android Studio: right-click the file → Run.
- Note for step 3: exclude pictures already in the message (the real-log test shows 44 as 2nd suggestion after "643 44").
- Backup: `_backup_login_activity_2/predictor_20261006/`.

## Usage log for the Eraka prediction model (step 1) – 6 Oct 2026
Records what the child actually does so the per-child n-gram model can learn from it and be evaluated. Nothing changes on screen.
- New `utils/ErakaUsageLog.java` (built-in SQLite, one database per child: `eraka_usage_<uid>.db`, phone only, background writes).
  - `tap_events`: ts, session_id (one message), position (0 = first), picto_id, prev1_id, prev2_id (-1 = none), source (board / smart_row / voice / voice_corrected / gallery), place, hour, weekday, shown_board (4 tile ids), shown_smart, rank_board / rank_smart (position of the tapped picture among those shown, 0 = not shown).
  - `message_events`: spoken / cleared / undo with session_id, length, picto_ids, place, hour.
  - `exportCsv()` → `Android/data/<package>/files/eraka_usage/tap_events_<uid>.csv` and `message_events_<uid>.csv` (copy over USB).
- `Principal.java`: `click(opcion, source)` and `addPictogramToPhrase(opcion, source)`; `logTapEvent()` just before `historial.addPictograma()`; `logMessageEvent()` on speak, long-press erase (cleared) and short-press erase (undo); `onPause()` exports the CSVs.
- `SmartRowAdapter.getItems()`, `SmartRowView.getCurrentItems()`.
- OTTAA's `relacion`/`frec` counting is unchanged (kept as the baseline).
- Verified on the A13 (6 Oct) with Android Studio → App Inspection → Database Inspector: 12 taps in 4 messages, correct position/prev1/prev2, place, hour, ranks; message_events shows spoken and undo. OTTAA's order put the tapped picture first in 3 of 12 taps. Seen: the smart row sometimes repeats a tile or shows the previous board (refreshes too early) — fix in step 3.
- Backup: `_backup_login_activity_2/usage_log_20261006/`.

## Design system
- Palette in `res/values/colors_eraka_kids.xml`: cream bg #FFF1C2, mustard #E9A92A, leaf green #4E9F5F (+ soft/dark variants).
- Shared drawables `bg_eraka_*` (panel, field, 3D mustard button, green outline button, bubbles/wave strip `bg_eraka_grass`, green rounded header `bg_eraka_toolbar_green`). Shared page header `layout/eraka_page_header.xml`.
- Buttons use `androidx.appcompat.widget.AppCompatButton` (plain `<Button>` gets repainted blue by the MaterialComponents theme).
- Layouts exist in both `layout/` and `layout-hdpi/`; update both. Tablet `sw600dp` layouts still old.
- Pictogram tiles come from the JitPack dependency `com.github.OTTAA-Project:PictogramsLibrary:0.19.62` (source not in repo; resources cached at C:\GradleCache\...\jetified-PictogramsLibrary-0.19.62). The app overrides its resources by name: `drawable/background_shape.xml`, `layout/group.xml`, `layout/pictogram.xml` (same ids, only style changed). App tile wrapper `drawable/picto_shape.xml` restyled too.

## Screens restyled
Splash, login, child form (Step2), avatar page (+ own photo upload/camera, stored offline via `AvatarPhotoStore`), portrait avatar cropper, main board, side menu + "Welcome back, <name>!" header, loader between screens (`utils/ErakaLoader`, blurred background), group gallery + words-in-group gallery ("All" button; shared `activity_galeria_grupos2.xml`), search all pictograms (`findallpictograms.xml`), Edit pictogram (`edit_picto_visual.xml`, with Save / Cancel buttons -> `savePictogram()`), picture-source dialog (`dialog_select_imagen.xml`), ARASAAC search (`activity_galeria_pictos.xml`). All portrait. Grid columns: `Custom_recyclerView.calcularCantidadColumnas` = max(2, width/170dp).

## Side menu (current)
Where are we? (offline Places picker, `utils/ErakaPlaces`) · Mute · Favourite phrases · Games · Change avatar · Child profile* · Progress report* · Voice & speech* · Teach a word* · Backup & restore* · Settings* · Help & tips · About Eraka · Exit · Log out*
(* = behind caregiver lock, `utils/CaregiverGate`, simple sum, unlocked 3 min)
New pages live in `Activities/Eraka/` (Help, About, Report, Games, Voice, Backup, Favourites). Text in `values/strings_eraka_menu.xml`.

## Premium / subscription – fully removed (everything free)
- Checks now always "unlocked": `User.isPremium()`, `Json.consultarPago()`, `BarridoPantalla.consultarPago()/devolverpago()`, direct checks in GaleriaGrupos2, GaleriaPictos3 (adding/editing/linking pictograms in a group), Edit_Picto_Visual (location tag), AboutOttaa, prefs (`premiumLevel()`=1).
- `Principal.consultarPago()` no longer reads OTTAA's "pago" flag (it used to switch off premium AND screen scanning for unpaid users). Still creates `LicenciaUsuario` because its auth listener returns the user to login after sign-out; `LicenciaUsuario.changePremiumState` always writes premium=1.
- Paywall screen `LicenciaExpirada` is now unreachable (only in else-branches of always-true checks).

## Release APK for testers – 28 Sept 2026
- First signed release APK built (Generate Signed APK → APK, new release keystore kept outside the project): `app/release/app-release.apk`, 226 MB, build took about 16 min (whisper.cpp compiled at -O3 for 3 ABIs).
- Unused Whisper models moved out of `app/src/main/assets` to `_eraka_unused_models/` (project root, not packed): `ggml-tiny.bin` (78 MB) and `ggml-base.en-q5_1.bin` (60 MB). Only `ggml-tiny.en-q5_1.bin` (32 MB, `WhisperManager.MODEL_NAME`) stays. No code referenced the removed files. Next APK should be about 90 MB.
- **Own package name (29 Sept):** `applicationId` changed from `com.stonefacesoft.ottaa` to **`na.eraka.app`** (debug: `na.eraka.app.debug`), because the release APK clashed with the original OTTAA app installed on the phone ("package conflicts with an existing package"). Eraka and OTTAA now install side by side. `namespace` and Java packages stay `com.stonefacesoft.ottaa`; FileProvider already uses `${applicationId}`. Needs two new Android apps in Firebase project otta-project-1ee57 (`na.eraka.app`, `na.eraka.app.debug`) with their SHA-1s and a new `google-services.json`, otherwise the build fails with "No matching client found". Same database and accounts. Backup: `_backup_login_activity_2/package_rename_20260929/`.
- **Reverted 2 Oct:** applicationId set back to `com.stonefacesoft.ottaa` on request, to demo the app to the supervisor (the build failed because `google-services.json` had not yet been replaced with one listing `na.eraka.app`). Re-apply the rename after the new json is in place.
- Google sign-in in the release APK needs the **release keystore's SHA-1** added under app `na.eraka.app` in Firebase (the existing SHA-1 is the debug key). Check with `gradlew signingReport`.

## Notable fixes
- App crashed on reopen: Splash opened unregistered `PrincipalV5` → now `Principal`; PrincipalV5 deleted.
- Login stuck after a slow attempt: Firebase auth listener doesn't re-fire on retry → LoginActivity2 now restarts setup when already signed in, resets the progress counter, transitions once. Debug builds use Firebase emulators on the laptop (10.153.3.50) – slow/hung downloads usually mean laptop asleep / IP changed / emulators not running.
- Settings crash: GPS preference was removed before `initComponents()` looked it up → null. Now removed at end of onCreate (`hideOldGpsSetting`).
- Old report used OTTAA's cloud functions + premium gate → new offline `ErakaReportActivity`.
- GPS location (needed paid Google Places key) replaced by manual Places picker; GPS setting hidden.
- Settings: paywall removed, Eraka theme, portrait, green switches.
- MainJuegos (games) had its onCreate commented out → replaced by `ErakaGamesActivity` launching the existing games.
- Favourite phrases: new `ErakaFavouritesActivity` replaces OTTAA's FavoritePhrases in the menu and board. Same storage (favoritePhrases.txt).
- Chosen TTS voice persisted via pref `eraka_tts_voice` (applied in `UtilsTTS.applySavedVoice`).
- Edit pictogram: saving was only reachable via Back → now a visible Save button.
- Build fix (26 Sept): `WhisperManager.onAudioDataCaptured` failed to compile – the no-speech `catch (Throwable t)` reused the name of the transcription `Thread t`. Renamed the catch variable to `nsErr`.

- Edit / Add pictogram (2 Oct): the OTTAA "Choose a TAG" editor (time / location / calendar / age tags) is hidden on request. `Edit_Picto_Visual.hideTagEditing()` hides the Tags button column and `cardViewChooseTAGS`, and shows the Text editor by default; the tab switches now set the tag card to GONE. Layout files unchanged (ids kept). Backup: `_backup_login_activity_2/hide_tags_20261002/`.
## Open items
- targetSdk 33 → raise to 35 (needs AGP 8 upgrade + edge-to-edge opt-out) to stop Play Protect warning.
- Not yet restyled: game screens; tablet (sw600dp) layouts.
- About page contact email is in `strings_eraka_menu.xml` (`eraka_contact_email`).
- Many new files not yet committed to git.

## Personal voice dictionary (Whisper → pictograms) – 25 Sept 2026
Whisper no longer just types what it heard. It now suggests the child's own pictograms and learns that child's sounds over time. Everything works offline, and no audio is saved.
- **New `utils/VoiceDictionary.java`** does the matching and learning:
  - Text is normalised first, which removes Whisper tags like [BLANK_AUDIO].
  - The transcript is compared with the child's pictogram names. It checks, in order: exact name 8, name as a word in the sentence 6, spelling similarity ≥0.6 (5×sim), first sounds like "wa"→water (3×), and Soundex sound-alike 3.
  - Links the child has confirmed before count too: an exact match is worth 10, a close one 6×sim, scaled by count (log) and recency (2-week half-life).
  - It shows the top 4.
  - `learn()` (+1) runs when the child taps a suggestion. `teach()` (+3) runs when a caregiver teaches or corrects. `forget()` removes a link.
  - Saved per child in the app's files folder as `voice_dictionary_<uid>.json`.
- **Evaluation log** `voice_eval_<uid>.csv`, one line per attempt, with columns timestamp, transcript, shown_ids, chosen_id, rank, source, mode.
  - rank is 1–4 when the child picks a suggestion, 0 when a caregiver picks another picture, and -1 when nothing is chosen.
  - mode is child / none / words / corrected / taught / no_match.
  - Use it for top-1/top-4 accuracy and the learning curve.
- **New `utils/VoiceSuggestions.java`** holds the two screens:
  - The "Did you mean…?" dialog shows up to 4 big pictogram tiles plus None of these / Use the words / Teach.
  - A searchable picture picker is used when a caregiver teaches or corrects.
- **`Principal.java` changes:**
  - The Whisper result now goes to `handleWhisperTranscript()`. A tapped tile is added through `addPictogramToPhrase()`, so it is spoken and also feeds the prediction links.
  - "Teach" sits behind the caregiver lock.
  - When nothing matches, or "Use the words" is tapped, the old text path runs (`useWhisperWords()`: ChatGPT only if it is switched on, otherwise the Yes/No text confirm).
  - New side-menu item **Teach a word** (caregiver lock, `R.id.teach_word`). It records the child's sound, then the caregiver picks the picture it means.
- Strings added at the end of `values/strings.xml` (`voice_dict_*`, `menu_teach_word`). The menu item is in `menu/activity_main_drawer.xml`.
- The matching was unit-tested off-device with sample words:
  - "wata"→water, "Mama"→mom (sound-alike), "ba"→ball.
  - After 2 confirmations, "bah bah"/"ba ba"→ball.
- **Not done yet:**
  - The model is still `ggml-base.en` (English).
  - The child's vocabulary is not yet given to Whisper as a prompt; that needs a JNI change.
- Backups: `_backup_login_activity_2/voice_dict_20260925_1320/`.


## Whisper button stuck on "loading" – 25 Sept 2026
- `WhisperManager`:
  - Transcription now catches `Throwable`, not just `RuntimeException`. Out-of-memory or native errors used to kill the thread silently, and the button span forever.
  - Added a 90 s timeout. It gives the button back and shows a "took too long" message (`whisper_timeout`).
  - Timing is logged under the tag `whisper` ("Starting transcribe … / Transcribe finished in … ms").
  - The microphone is now released after the automatic stop on silence. Before, AudioRecord stayed open, and the next recording could fail without any message.
  - The listener is updated on every recording. The first one used to be captured forever, which mattered for Teach mode.
- Likely main cause is speed, not a bug in the matching code:
  - The whisper `:lib` module is built from the debug variant. That makes the native code unoptimised and very slow.
  - The Galaxy A13 also only has slow cores.
  - Fix: build the native code in Release (see the chat notes) or test with a release build.
- Backups: `_backup_login_activity_2/whisper_hang_*`.

## "Suggestions" tip dialog too wide – 25 Sept 2026
- `layout/dialog_new_features.xml` was OTTAA's fixed-size landscape card, which overflowed the screen in portrait. It is now an Eraka portrait card: cream dialog, the two hug images, text, and a full-width mustard OK button. The view ids are the same.
- `NewDialogsOTTAA.initDialog()` sizes this dialog to 90% of the screen width, with the height fitting the content.
- The dialog is shown once, the first time a suggested (transparent) pictogram appears (`Principal.explicarSugerencias`). It is not related to Whisper.

## Whisper still timing out after 90 s – 25 Sept 2026
- The model changed from `ggml-base.en-q5_1.bin` to **`ggml-tiny.en-q5_1.bin`**, which was already in assets. It is several times faster and good enough for single words. The name is now `WhisperManager.MODEL_NAME`.
- New "busy" guard (`isTranscribing()`, string `whisper_busy`). whisper.cpp runs one job at a time, so tapping the mic again during a slow transcription used to queue a second job behind the first, and that one timed out as well. Recording is now blocked until the current job finishes, in both Principal and Teach a word.
- The log now also shows the model name and the number of CPU cores.
- `whisper-latest/.../lib/build.gradle` has a debug buildType that forces a Release/-O3 native build. It needs a Clean Project to take effect.

## Whisper: 33 s → optimised native build + audio_ctx – 25 Sept 2026
- Measured with tiny.en on the A13: 4.5 s of audio took 33 s to transcribe.
- The Gradle debug flags never took effect. The only native build cache was from 20 July (`CMAKE_BUILD_TYPE=Debug`, ggml compiled without -O).
- Changes in `whisper-latest/examples/whisper.android/lib` (originals in `lib/_eraka_backup/`):
  - `src/main/jni/whisper/CMakeLists.txt` now always builds Release with -O3, even for a debug app build.
  - `src/main/jni/whisper/jni.c` (`fullTranscribe`):
    - `audio_ctx` is set to the recorded length (50 frames/s + 64, min 256, max 1500) instead of the full 30 s window.
    - Also sets no_timestamps, max_tokens 48 and suppress_blank, and turns off print_realtime/timestamps.
    - The log line "About to run whisper_full (audio_ctx=…, threads=…)" (tag JNI/LibWhisper) shows the values used.
  - The old `.cxx` folder was renamed to `_old_cxx_delete_me` so CMake reconfigures. The user can delete that folder.

## Whisper: variable speed (4 s vs 45 s) and repeat loops – 25 Sept 2026
- The logs show the Galaxy A13 runs **32-bit Android** (Primary ABI armeabi-v7a, `libwhisper_vfpv4.so`). The faster 64-bit fp16 library can't be used on this phone.
- Timings after the optimised build: 3.8 s ("Oh"), 45 s ("the"), 32 s ("Hello" ×19, rejected). The slow runs were whisper.cpp's temperature fallback, which re-decodes up to 5 times when the output looks repetitive.
- `lib/src/main/jni/whisper/jni.c`:
  - `temperature_inc = 0` turns the fallback off.
  - Also sets `single_segment = true`, `max_tokens = 16` and `greedy.best_of = 1`.
- `lib/.../WhisperCpuConfig.kt`: thread cap raised from 4 to 6, since the A13 has 8 equal cores.
- `WhisperManager.collapseRepeats()`: "Hello Hello Hello…" becomes "Hello" instead of the whole result being rejected. It logs "Collapsed repeats".
- Backups: `lib/_eraka_backup/` (jni_v2.c, WhisperCpuConfig.kt) and `_backup_login_activity_2/whisper_hang_*/WhisperManager_v3.java`.

## Voice: "No picture found" box replaces the text Yes/No – 25 Sept 2026
- When Whisper's text matched none of the child's pictures, the app used to show OTTAA's text-only "Is this what you said?" Yes/No box, which a child who can't read can't use.
- New `VoiceSuggestions.showNoMatch()`: an Eraka card titled "No picture found", with Heard: "…" and three choices:
  - **🎤 Try again** (big mustard button) records again straight away.
  - **✋ Teach this sound** (caregiver lock) opens the picture search. The sound is learned with `teach()`, the picture is added to the sentence, and the attempt is logged as `corrected` with rank 0.
  - **Use the words** is the old text path (`useWhisperWords`).
- The miss is still logged as `no_match`. The card is 90% of the screen width.
- `Principal.handleWhisperTranscript()` calls it. New strings: `voice_nomatch_title`, `voice_nomatch_try_again`, `voice_nomatch_teach`.
- Backups: `_backup_login_activity_2/voice_nomatch_20260925_1556/`.

## Whisper: shorter audio, vocabulary hint, busy animation – 25 Sept 2026
1. **Shorter recordings**
   - `SimpleVad.SILENCE_HANG_MS` is now 900 ms (was 1500).
   - `WhisperRecorder` cuts the silence before and after the speech, keeping 250 ms before and 300 ms after.
   - It logs "Trimmed audio X ms -> Y ms".
2. **Smaller audio window:** `lib/.../jni.c` sets `audio_ctx` to 50 frames/s + 32, with a minimum of 128 (about 2.5 s). The old minimum was 256.
3. **Vocabulary hint (Whisper initial_prompt)**
   - `lib` changes:
     - `jni.c` has a new `setInitialPrompt` JNI function (mutex-protected global) that `fullTranscribe` uses.
     - `LibWhisper.kt` gains `WhisperLib.setInitialPrompt` and `WhisperContext.setInitialPrompt`.
   - App changes:
     - `WhisperBridge.setPrompt` passes the hint through.
     - `WhisperManager.setVocabularyHint()` stores the hint, applies it before each transcription, and logs "Vocabulary hint: […]".
   - `Principal.buildVocabularyHint()` builds the hint:
     - Up to 30 of the child's picture words: the 4 on screen first, then the most-used by summed `relacion` frec.
     - Only short plain words (at most 2 words and 20 characters).
   - Thesis caveat: the hint can bias Whisper towards board words, even on noise. The child still confirms by tapping.
4. **Busy animation:** new `utils/WhisperButtonAnimator` for the Voice button.
   - **Idle** is a white button with the label "Voice".
   - **Listening** is a mustard button that pulses, labelled "Listening…".
   - **Thinking** is a green button with a spinning icon, labelled "Thinking…".
   - It replaces the "Transcribing…" toast. Strings: `whisper_state_*`.
- Backups: `_backup_login_activity_2/whisper_speed2_*`, `lib/_eraka_backup/jni_v3.c`, `LibWhisper.kt`.

## Whisper test after speed changes – 25 Sept 2026
- Measured on the A13:
  - Trimming cut audio from 3.5–4.5 s to 1.55–2.05 s.
  - **Transcription now takes 3.46–3.56 s, consistently** (was 33–45 s).
  - 6 threads, with a 30-word vocabulary hint.
- New issue: list-style loops that copy the comma-separated hint format ("grandpa, good night, good night…", "oi, la, oi, la…"). Two results were rejected as implausible, and "water ×9" was collapsed to "water".
- Fix: `WhisperManager.collapseRepeats()` now dedupes whole comma-separated phrases (or words when there are no commas), keeps their order, and applies the plausibility check afterwards. So "grandpa, good night ×5" becomes "grandpa, good night", and both are offered as pictures.
- Backup: `_backup_login_activity_2/whisper_speed2_*/WhisperManager_v4.java`.

## Voice matching v2: stricter, so "No picture found" can happen – 25 Sept 2026
- Device test (A13, about 2.7 s per word): fillers like "well", "or", "mmm", "ha", "and, uh" and "like a" always produced picture suggestions. With hundreds of pictures, the loose rules always found something similar, so "No picture found" never appeared.
- `VoiceDictionary` changes:
  - New `FILLERS` list (uh, um, mmm, oh, ha, well, and, or, the, a, okay, yeah, "like a", "thank you"…). These are ignored for name matching. Learned links still use the full text, so a caregiver can still teach "mmm" as "more".
  - `MIN_SCORE` raised from 2.0 to 3.0.
  - New relative cutoff: candidates below 60% of the best score are dropped.
  - Stricter name rules:
    - 1–2 letter sounds only match a picture exactly.
    - Spelling similarity needs 4+ letters (weight 5.5×sim).
    - A prefix needs 3+ letters and at least half the word (3.5).
    - Sound-alike needs a Soundex match plus spelling similarity ≥0.5 (2.5+sim).
- Off-device test with 208 common AAC words:
  - well / or / mmm / ha / and, uh / Oh / the / ba → **No picture found**
  - water → water only; "wata" → water first; "nana" → banana; "dada" → dad; "juice please" → juice, please
  - After teach("mmm"), "mmm" → the taught picture
- Trade-off for the thesis: 2-letter approximations such as "ba" for ball are no longer guessed. They must be taught once, and then they are learned.
- Backup: `_backup_login_activity_2/voice_strict_20260925_1737/`.

## Voice: accent mishearing, humming hallucinations, shorter answers – 25 Sept 2026
- Device test:
  - "water" (Namibian English pronunciation) was heard as "what" 3 of 4 times.
  - Humming "hmm" came back as a made-up sentence, "oh, so is it your home".
  - Every answer looped 6–9 times ("mom, mom, mom…").
- **No-speech gate:**
  - `lib/jni.c` gains a new JNI function `getNoSpeechProb` (max of `whisper_full_get_segment_no_speech_prob`), exposed through `LibWhisper.kt` `WhisperContext.getNoSpeechProb()` and `WhisperBridge.noSpeechProb()`.
  - `WhisperManager` rejects the result when the score is above 0.6 (`NO_SPEECH_THRESHOLD`, whisper.cpp's default) and logs "No-speech probability: …". Tune the threshold from the logs.
- **Shorter answers:** `jni.c` `max_tokens` changed from 16 to 8. This stops the repeat loops earlier and saves decode time.
- **Taught words go into the hint:**
  - `VoiceDictionary.learnedPictoIds()` lists this child's confirmed or taught pictures, most-used first.
  - `Principal.buildVocabularyHint()` puts up to 10 of them first, then the 4 on screen, then the most-used words.
  - So once a caregiver teaches "what" → water, Whisper is also primed to expect "water".
- The accent issue itself is by design handled by the personal dictionary: teach once, then the learned link (score ≥10) beats name matching. This is a good thesis example of why personalisation matters for Namibian speakers.
- Backups: `_backup_login_activity_2/voice_nospeech_20260925_2026/`, `lib/_eraka_backup/jni_v4.c`, `LibWhisper_v2.kt`. Needs a clean build (native change).

## Switch debug build to the LIVE Firebase + security rules – 28 Sept 2026
- Why: sign-in sometimes hung and then showed "something went wrong". The debug build pointed at local emulators on the laptop (10.153.3.50, a Namibia Wi-Fi address). It failed whenever the emulators were off, the laptop was asleep, the phone was on another network, or the laptop's IP had changed (as it has in Tenerife).
- `app/build.gradle` debug: `FIREBASE_EMULATOR_HOST` is now `""`, so the app uses the live project `otta-project-1ee57`. To go back to the emulators, put the laptop's current IPv4 there.
- **Security rules** (before, Storage was `allow read, write: if true`):
  - `storage.rules`:
    - `Archivos_Paises/**` and `Archivos_Sugerencias/**` are read-only for signed-in users.
    - `Archivos_Usuarios/<folder>/<uid>/**` is available only to that uid.
    - `Archivos_Usuarios/<folder>/<name>_<email>_<lang>.txt` is available only to that e-mail.
    - Files are limited to 10 MB. Everything else is closed.
  - New `database.rules.json`:
    - `<Node>/<uid>` is available only to that uid.
    - `version` is read-only.
    - `Fotos/<pushKey>` (OTTAA's global photo index) is open to signed-in users.
  - `firebase.json` now references both rule files. Deploy with `firebase deploy --only storage,database`.
- The live project must contain the starter files the emulator was seeded with (`firebase_data_new_user` export), otherwise new users get no pictures:
  - `Archivos_Paises/pictos/pictos_en.txt`
  - `Archivos_Paises/grupos/grupos_en.txt`
  - `Archivos_Sugerencias/pictos_<sexo>_<edad>.txt`
- Also check that the debug keystore SHA-1 is registered in the console, and set a Blaze budget alert.
- Backup: `_backup_login_activity_2/live_firebase_20260928_1755/`.

## Firebase LIVE / EMULATOR switch – 28 Sept 2026
- New `FirebaseRequests/FirebaseServerConfig.java`:
  - Stores the chosen mode (`live` or `emulator`) and the emulator host in SharedPreferences (`eraka_firebase_server`).
  - Defaults come from `gradle.properties` (`erakaFirebaseMode`, `erakaEmulatorHost`) through BuildConfig (`FIREBASE_MODE_DEFAULT`, `FIREBASE_EMULATOR_HOST`).
  - Release builds are always LIVE.
- New `FirebaseRequests/FirebaseServerDialog.java` (debug only):
  - A developer dialog with Live or Emulator plus an IP field.
  - "Save & restart" signs out and restarts the process, because Firebase can only switch server before its first use.
  - It opens from a long-press on the sign-in logo (`LoginActivity2`, `R.id.logoBadge`) or on the side-menu header (`Principal.initAvatar`).
  - The sign-in screen toasts `DEV: LIVE Firebase` or `DEV: EMULATOR <ip>`.
- `FirebaseUtils.initFirebaseAndEmulator()` uses `FirebaseServerConfig.useEmulator()` (context from `FirebaseApp`) and catches an `IllegalStateException` if Firebase was already used.
- `app/build.gradle`:
  - debug: `FIREBASE_MODE_DEFAULT` and `FIREBASE_EMULATOR_HOST` are read from `gradle.properties`.
  - release: `live` and `""`.
- Laptop helpers in the project root:
  - `firebase-emulators-start.bat` prints the IPv4 and starts the emulators, importing from and exporting to `firebase_emulator_data`.
  - `firebase-deploy-rules.bat` deploys the Storage and Database rules to live.
  - `.firebaserc` sets the default project `otta-project-1ee57`.
  - `FIREBASE_SETUP.md` is the how-to.
- Backup: `_backup_login_activity_2/firebase_switch_20260928_1810/`.

## No Cloud Storage: stays on the free Spark plan (option B) – 28 Sept 2026
- Why: on the free plan, Cloud Storage is locked ("upgrade to recover access to your existing bucket"), and Blaze needed a $30 prepayment. Keneke chose to stay free. Auth, the Realtime Database and Remote Config stay within the Spark quotas.
- New `FirebaseRequests/ErakaFileStore.java` replaces every Storage read and write:
  1. **Starter files** (`Archivos_Paises/…`, `Archivos_Sugerencias/…`) are packed in the app under `assets/eraka_seed/<same path>`. That is 22 files, about 5.7 MB: pictos/grupos en+es, 12 suggestion files, pictosDatabase_es, frases-juegos, desc-es, all from the old emulator export `firebase_data_new_user`. A language that isn't bundled falls back to en.
  2. **Family backups** (`Archivos_Usuarios/<Folder>/<name>_<email>_<lang>.txt`) are stored as text in the Realtime Database at `Backups/<uid>/<Folder>/<lang>`, covered by the per-uid rule. If a backup is missing, the board and groups fall back to the starter files, so sign-in never hangs.
  3. **Photos and avatars** are never uploaded. They fail with `PhotosStayOnPhoneException` and callers keep the local file.
  - Same shape as the Storage calls: `download(ctx, ref.getPath(), file)` returns a `Task<Long>`; `upload(ctx, ref.getPath(), stream)` returns a `Task<Uri>` pseudo-URL `eraka-db://…`, which OTTAA stores in `URL_…` as before. `upload` reads the stream immediately, because some callers close it straight after.
- Call sites changed:
  - `DownloadFile/*` (Pictograms, Groups, Phrases, FavoritePhrases, Games, PredictionsPictograms) and `DownloadJsonFromFirebase`: `getFile` → `ErakaFileStore.download`.
  - `DownloadFile.onFailure` no longer casts to `StorageException`.
  - `UploadFiles/*` (Pictos, Groups, Phrases, FavoritePhrases, Games) and `SubirBackupFirebase`: `putStream` + `getDownloadUrl` → `ErakaFileStore.upload`.
  - `DownloadPredictionsPictograms`: a failure now also calls `onPictosSugeridosBajados(true)`.
  - `LoginActivity2Step2`: the suggestion files load even offline.
- **Bugs this would have caused on the free plan, fixed:**
  - `Edit_Picto_Visual`: when online, a new **photo pictogram was only saved after a successful photo upload**. It is now always saved locally (`cargarFotosFirebase` is no longer called).
  - `DownloadPictogram` (adding an ARASAAC picture from search): the picture was only added after uploading it. It is now added immediately from the local file.
  - `SubirArchivosFirebase.subirFotosOffline()` **deleted the queued offline photos** after starting the upload. It is disabled.
  - Avatar upload (`LoginActivity2Avatar`) already kept the local photo on failure. Unchanged.
- Texts: `backup_intro` and `about_privacy_cloud` now say photos stay on the phone.
- `firebase-deploy-rules.bat` deploys only the database rules. `storage.rules` is kept for the emulators.
- Backup: `_backup_login_activity_2/no_storage_20260928_1845/`.

## Sign-in toast removed – 28 Sept 2026
- Live Firebase confirmed working on the phone (free plan, no Storage).
- `LoginActivity2`: removed the `DEV: LIVE Firebase / EMULATOR …` toast. The long-press on the logo still opens the server switch, and the dialog shows "Now using: …".

## 27 Oct 2026 — Settings sub-pages: back arrow now works
- Problem (A13 test, logcat 17:23): the "<-" arrow on settings sub-pages (Accessibility, Pictograms…) showed but did nothing.
- Cause: a sub-page is a Dialog whose title bar keeps the window callback it got when the page was built; `addBackArrow` replaced the callback afterwards, so the arrow tap never reached the proxy.
- Fix (`SettingsActivity.addBackArrow`): after the page is shown, find the arrow image (`android.R.id.home`) and set a click listener on its parent (the tappable area) that closes the page. The proxy is kept as a fallback. Logs `addBackArrow: back arrow wired`.
- Backup: `_backup_login_activity_2/settings_back_arrow_fix_20261027/`.

## 27 Oct 2026 — Speed: taps on the board
- Measured (A13 logcat 17:21): tap→board 679 ms (was 129 ms on 21 Oct), suggestion row 158–236 ms, each skin recolour 27–50 ms, 42 skipped frames after a tap; about 800 debug log lines per tap.
- `MainBoardActivity.createRelationShip`: no longer writes the whole pictures file (`guardarJson(ARCHIVO_PICTOS)`) on every tap; OTTAA's counts stay in memory and are saved in `onPause()`/`onStop()` as before.
- Removed OTTAA debug logs from the per-tap loops: `JSONutils.getPictoFromId2` ("position:", "getPictoFromId2:"), the score log, "JSONArray Failing"; `Json.elegirHijos2` ("Looping through child", "Sorting child").
- `ErakaSkinTone.recolourPixels`: remembers the result per colour (4096-slot table), so the colour maths runs once per colour instead of per pixel. Tested in the cloud against the old code: identical pixels on 50 random 250×250 images; about 3× faster pixel loop (decoding the file still costs most of the 30–50 ms).
- Backup: `_backup_login_activity_2/speed_tap_20261027/`.

## 27 Oct 2026 — Whole spoken sentences become a picture message
- Problem (Keneke's test): "I want to go home, I want to" gave four single tiles (go, want, I, wait); the child could add only one, the order was lost and "home" was missing.
- `VoiceDictionary.matchSequence()` (new): for transcripts of 3+ words, reads the words left to right, skips fillers, and per word (or 2–3-word name like "get dressed") takes: exact name or home word → learned link for exactly that word → word without -s/-es/-ing/-ed (`stems()`) → close spelling (4+ letters, same first letter, similarity ≥ 0.6). Unmatched words are left out; a picture already found is not repeated; max 6 pictures; needs ≥ 2 pictures. Hidden pictures are never offered.
- `VoiceSuggestions.showSuggestions(...)` overload: "💬 Use this message" box with the pictures in order (small tiles with › between) above the single tiles; one tap adds them all. New interface `OnSequencePicked`; string `voice_dict_use_message`.
- `MainBoardActivity.handleWhisperTranscript`: computes the sequence; the dialog shows even when only the sequence matched; on tap adds each picture via `addPictogramToPhrase` with `quietPictureAdd = true` (names not spoken one by one); logged with mode "sentence" in the voice evaluation log.
- Tested in the cloud with stub classes: "I want to go home, I want to" → [I][want][go][home]; "I want to eat bread and drink juice" → 6 pictures; "I am going swimming" → [I][go][swim]; "Uh, I want, um, more wata" → [I][want][more][water]; single words and "more more more" → no message (normal tiles).
- Unit test `VoiceDictionarySentenceTest` (stems, fillers).
- Thesis `60design.tex` (update 13): new paragraph in Personal Voice Dictionary.
- Backup: `_backup_login_activity_2/voice_sentence_20261027/`.

## 27 Oct 2026 — Groups screen: Speak button covered "Previous"
- Cause: the bottom bar has five equal slots (Close, Previous, empty centre, Next, Edit) and Speak floats over the middle of the bar. Hiding Edit with `gone` left four slots, so the middle of the bar fell between Previous and the empty slot and Speak sat on top of Previous.
- Fix: the Edit slot in `activity_galeria_grupos2.xml` (normal, hdpi, ldrtl) is now `invisible` instead of `gone`: it keeps its space but is not shown and cannot be tapped, so Speak is centred again.
- Backup: `_backup_login_activity_2/group_bar_speak_overlap_20261027/`.

## 27 Oct 2026 — Pictures screen: "+" (new picture) is back
- This morning's change hid "+" on both gallery screens. On the Groups screen "+" made a new group; on a group's Pictures screen it is the only way to create a new word, so it is restored there (`PictogramGalleryActivity.onCreateOptionsMenu`: `nuevo` visible unless the screen is choosing or sorting pictures). Sort and view toggle stay hidden.
- `menu_galeria_pictos.xml`: Spanish titles (shown on long press) now English: "New picture", "Add existing pictures to this group" (the ⊞ button).
- Backup: `_backup_login_activity_2/pictures_add_button_20261027/`.

## 27 Oct 2026 — Speed: coming back to the main board
- Measured (A13 logcat 19:21): returning from the Groups screen froze the board for 8.6 s (517 skipped frames). `MainBoardActivity` called `CargarJson()` in both `onRestart()` and `onResume()`, and each call re-read and parsed all six JSON files (about 4 s each).
- `MainBoardActivity.onRestart`: no `CargarJson()` (onResume always follows).
- `Json.initJsonArrays`: skips reading when every file (pictos, groups, phrases, suggested pictos, favourite phrases, games) has the same last-modified time and size as when it was last read or written (`rememberFileStamps`, `filesUnchangedSinceLoad`; `guardarJson` updates the stamp after writing). New `forceReloadJsonArrays()`; `refreshJsonArrays()` uses it. Logs `ErakaPerf json_load` when it does read, "files unchanged, kept in memory" when it skips.
- `Json` tag check uses `optJSONArray("tags")` instead of throwing and printing a stack trace for every picture without tags (24 per group screen); removed the per-comparison "compare 2"/"getScore" logs.
- Backup: `_backup_login_activity_2/speed_board_return_20261027/`.

## 27 Oct 2026 — Skin colour through the avatar (no colour names); animals keep their colours
- Avatar screen: Light/Medium/Dark/Mixed chips hidden (`chipGroupAvatarTone` gone, normal + hdpi). The 15 places now show 15 different children, five per skin colour, spread over the grid (`ErakaSkinTone.displayAvatar`: light 14 15 21 24 32, medium 13 22 23 33 34, dark 11 12 25 31 35). Clicking stores e.g. `ic_avatar14_light`; `toneOfAvatar` sets the pictures' tone. `AvatarChoiceActivity.showAvatars()` replaces `showAvatarsInSkinTone`/`setUpToneChips`.
- Child profile: skin-colour title and chips hidden (normal + hdpi); `ChildProfileActivity` no longer reads them or re-tones the avatar on save. Without an avatar the pictures stay mixed.
- `ErakaSkinTone.recolourPixels(px, w, h, t)` (second version): only the skin colour itself (k ≥ 0.85) is "core" skin; outline/background blends are recoloured only within 2 px of core skin (two 4-neighbour steps). The picture needs enough core pixels (same threshold). Fixes the black cat turning brown and pale beiges in objects. Java checked against the Python scan on 8 pictures (identical pixel counts).
- `NOT_PEOPLE`: all 33 drawings of the Animals group + `anana`, `lata_de_sardinas`, `lata_de_atun` are never recoloured.
- Scan of all bundled drawings (scripts and contact sheets in `eraka_base_data/skin_scan/`): 314 contain people under the new rule.
- Thesis `60design.tex` (update 13) updated.
- Backup: `_backup_login_activity_2/avatar_tone_no_labels_20261027/`.

## 27 Oct 2026 — Suggestion row: arrows show there is more
- `smart_row_view.xml`: the RecyclerView is now inside a FrameLayout with two round white arrows (`smartRowMoreLeft`, `smartRowMoreRight`, green arrow, same style as the bottom-bar buttons) at the ends.
- `SmartRowView.setUpMoreArrows()/updateMoreArrows()`: each arrow is shown only while the row can still scroll that way (`canScrollHorizontally(±1)`), updated on scroll, layout and new suggestions; a tap scrolls 70% of the row width.
- Strings `smart_row_more_right`, `smart_row_more_left` (TalkBack descriptions).
- Backup: `_backup_login_activity_2/smart_row_arrow_20261027/`.

## 28 Oct 2026 — "Teach a word" opens its own page
- Before: the menu item showed a toast ("Recording… let the child say the word, then tap the mic again") and used the board's mic; it was not clear what was happening, and after recording a picture list appeared.
- `VoiceSuggestions.showTeachWord()` (new, with interfaces `TeachUi` and `TeachController`): a page with the steps, a big picture tile ("Tap to choose a picture" → searchable picture list), a big Record / Stop / Record again button, a status line (choose a picture first → tap Record → Listening… tap Stop → Writing down what it heard… → Heard: "wa". Tap Save to link it to water / Nothing was heard) and Cancel / Save. Record is greyed out until a picture is chosen; Save until something was heard.
- `MainBoardActivity.startTeachWord()`: checks the mic (`whisperReadyFor(REQ_TEACH_WORD_MIC = 103)`, permission result handled), opens the page, records with the chosen word first in the Whisper hint, stops the recording when the page closes, and on Save calls `VoiceDictionary.teach()` + `logAttempt(..., "taught")` and shows "Learned: …". Nothing is added to the child's message.
- 15 strings `teach_*`.
- Backup: `_backup_login_activity_2/teach_word_page_20261028/`.

## 28 Oct 2026 — Parent's recording not played for new pictures (negative picture ids)
- Problem (Keneke): after recording "my voice" for a picture, tapping it said the label with the default voice.
- Cause: OTTAA numbers a new picture with `(int) System.currentTimeMillis()`. The time in ms no longer fits in an int, so new pictures get negative ids (today about -2,111,000,000). Eraka treats ids < 0 as "no picture" in several places: `ErakaHomeWords.speakPicto` (galleries) skipped the recording, and the voice dictionary, explorer, ignore statistics and smart-row links skipped these pictures too.
- `JSONutils.newId()` / `positive()`: new pictures and groups (`crearPicto`, `crearGrupo`, ARASAAC search results in `FindAllPictogramsRecyclerView`) get a positive id (time & 0x7fffffff, at least 100000).
- `ErakaIdFix.fixNegativeIds()` (new), run in `Json.readAllJsonArrays` after the groups are read: renumbers pictures with a negative id once, updates every `relacion` link in pictures and groups, moves the home word and the recording (`ErakaHomeWords.moveId`), re-sorts and saves pictos and groups. Old usage-log lines keep the old number.
- `ErakaHomeWords.speakPicto`: only -1 means "no picture".
- Backup: `_backup_login_activity_2/negative_ids_20261028/`.

## 28 Oct 2026 — Photos stay inside the picture frame
- Problem (Keneke): an uploaded photo (donkey) showed its square corners outside the coloured frame's round corners.
- Cause: in `pictogram.xml` the frame (`color_Picto`, `fizgerald_shape`: 5 dp line, 20 dp corners) is drawn over the picture (`Imagen_Picto`), whose drawing area is a plain rectangle. Drawings have white corners, so it only showed with photos.
- `ErakaPictoView.keepPictureInsideFrame()`: `Imagen_Picto` gets a `ViewOutlineProvider` with a rounded rectangle inside its padding (radius 15 dp = the frame's inner corner) and `setClipToOutline(true)`. Applies everywhere `ErakaPictoView` is used (board, galleries, edit screen, games).
- Backup: `_backup_login_activity_2/picture_frame_clip_20261028/`.

## 28 Oct 2026 — Speed: opening any page (one text-to-speech connection)
- Measured (A13 logcats 27 Oct): right after every page opened (Settings, Pictures, Profile, back to the board) the logcat shows "Long monitor contention … TextToSpeech$Connection.runAction" and "Skipped 59–87 frames" (about 1–1.5 s frozen).
- Cause: `SpeechOutput.getInstance()` (called by every screen, 26 places) created a new `UtilsTTS` = a new `TextToSpeech` connection each time and never closed the old one. In `onInit` OTTAA then called `setEngineByPackageName("com.google.android.tts")`, which connects a second time, and set language, pitch, speed and voice (`getVoices()`, slow) on the main thread.
- `SpeechOutput.prepareTextToSpeech`: keeps one `UtilsTTS`; later calls only `refresh()` it (new screen's toast; set up again only if pitch/speed/voice/language changed). A new one is made only if none exists or it was shut down.
- `UtilsTTS`: uses the application context; connects directly to Google's engine (`new TextToSpeech(ctx, onInit, "com.google.android.tts")`), no second connection; engine set-up (`preparEngineTTS`, `setUpTTSVOICE`) runs on a background thread; `applySavedVoice` skips `getVoices()` when the saved voice is already set; error toast posted to the main thread; `isLanguageAvailable(...) >= LANG_AVAILABLE`; install intent gets `FLAG_ACTIVITY_NEW_TASK`; new `refresh()`, `shutdown()`, `isShutDown()`.
- Backup: `_backup_login_activity_2/tts_reuse_20261028/`.

## 28 Oct 2026 — Groups bottom bar evenly spaced; photos inside the frame (second fix); hardware drawing on
- Groups screen bottom bar (`activity_galeria_grupos2.xml`, normal + hdpi): Speak is no longer a button floating over the middle of the bar but one of four equal slots (Close, Previous, Speak, Next) with its own "Speak" label, so all four button centres line up and are evenly spaced; the hidden Edit slot is `gone` again (no empty space on the right).
- Photos in the frame: the 27 Oct clip did nothing because `AndroidManifest.xml` had `android:hardwareAccelerated="false"` (OTTAA), and clipping to an outline needs hardware drawing. Now:
  - `pictogram.xml`: `Imagen_Picto` padding 7 dp (was 5): a square photo corner then lies under the frame's 5 dp line (corner 18.4 dp from the 20 dp curve's centre). Works with or without hardware drawing.
  - `ErakaPictoView`: outline clip radius 13 dp (20 − 7).
  - `AndroidManifest.xml`: `android:hardwareAccelerated="true"`. Every screen was drawn by the CPU before; on the A13 this should also make scrolling and opening pages faster. If any screen now draws wrongly or crashes ("Canvas: trying to draw too large bitmap"), set it back to "false" and tell Claude which screen.
- Backups: `_backup_login_activity_2/group_bar_align_20261028/`, `_backup_login_activity_2/hw_accel_frame_20261028/`.

## 28 Oct 2026 — Photos inside the frame (third fix: clip in the picture view itself)
- Keneke's test: a parent's own dog photo still covered the frame's top and left side (the bundled dog photo was fine).
- `Views/ErakaFramedImageView.java` (new, `AppCompatImageView`): `Imagen_Picto` in `pictogram.xml` is now this class. `onDraw` clips to a rounded rectangle inside the padding (radius 13 dp) before drawing, and `cropToPadding` is on, so nothing the picture library loads (Glide with its own corner/size transforms) can be drawn outside the frame's inside. Works with and without hardware drawing. The library still sees an ImageView.
- `ErakaPictoView.keepPictureInsideFrame()`: now only brings the frame (`color_Picto`) to the front; the outline clip was removed.
- Backup: `_backup_login_activity_2/picture_frame_clip3_20261028/`.

## 28 Oct 2026 — Saving a picture: loader + "saved" message
- Problem (Keneke): Save on a new picture took a while with no feedback.
- Cause: OTTAA showed a "please wait" dialog, then did all the work on the main thread (read the pictures and groups files, add the picture, write both files, home word/recording, upload) and closed the dialog at once; the dialog was never drawn, the screen just froze.
- `EditPictogramActivity`: `savePictogram()` shows `ErakaLoader` ("Saving the picture…" / "Saving the group…") and ignores a second tap; `onTextoTraducido` runs `aceptarCambios()` on a background thread; new `finishAfterSave()` dismisses the loader on the main thread, shows a toast ("Picture saved ✓", "Group saved ✓", "Changes saved ✓", "Group changes saved ✓"), sets the result and closes. On an error: loader closed + "Something went wrong. Please try again."; the no-photo case closes the loader and cancels as before.
- Strings in `strings_eraka_loader.xml`.
- Backup: `_backup_login_activity_2/save_loader_20261028/`.

## 28 Oct 2026 — Picture editor: clear messages when something is missing
- Problem (Keneke): a new picture with only a name and no image was not added to the group, with no message. Cause: `aceptarCambios()` quietly calls `cancelar()` for a new picture/group without `mCurrentPhotoPath`; the earlier check only tested for any drawable (the placeholder counts), and its toast was vague.
- `EditPictogramActivity.checkBeforeSaving()` (called first in `savePictogram()`): a new picture or group needs an image (camera, gallery or ARASAAC); every picture needs a name. If something is missing: the name box is marked ("Type a name") and focused, and a window "Almost there" lists what to add; with a missing image it has an "Add an image" button that opens the picture choices (same as tapping the picture box). Nothing is saved until both are there.
- Strings `check_*` in `strings_eraka_loader.xml`.
- Backup: `_backup_login_activity_2/save_loader_20261028/EditPictogramActivity_before_validation.java`.

## 28 Oct 2026 — Select and delete several pictures or groups at once
- New toolbar button (bin icon, "Select and delete", `select_delete` in `menu_galeria_pictos.xml`) on the Pictures screen of a group and on the Groups screen (hidden while choosing or sorting pictures).
- `utils/ErakaMultiDelete.java` (new): a window listing every picture/group with a tick box, its picture and name; tap rows to tick; "Select all / Select none"; "Delete (n)" (red, disabled at 0) asks once ("Are you sure?") before deleting.
- Pictures: `PictogramRecyclerView.removeFromGroup(ids)` removes them from this group (as the single Delete does: they stay in the app and in other groups), saves and uploads the groups. Toast "n picture(s) removed ✓".
- Groups: `GroupRecyclerView.deleteGroups(ids)` deletes the chosen groups (never "ALL", id 24, which is not listed), saves, uploads, refreshes (also clears a search). Toast "n group(s) deleted ✓". Pictures inside stay in the app.
- Strings `multi_*` (`strings_eraka_loader.xml`), colour `eraka_delete_red`.
- Backup: `_backup_login_activity_2/multi_delete_20261028/`.

## 28 Oct 2026 — Pictures screen toolbar: bin shown, ⊞ removed
- The bin ("Select and delete") never appeared: its visibility also required the picture list to exist, but the list is created after the toolbar menu. Now shown whenever the screen is not choosing or sorting pictures (Pictures and Groups); the click still checks that the list exists.
- The ⊞ "Add existing pictures to this group" button (`vincular`) is hidden on the Pictures screen (Keneke).
- Backup: `_backup_login_activity_2/multi_delete_20261028/PictogramGalleryActivity_before_menu_fix.java`.

## 28 Oct 2026 — "Where are we?" now changes the pictures: place words + "Here" group
- Before: the chosen place only counted through the child's own taps at that place (weight 0.1); a new child had none and the base model has no church/clinic/taxi data, so choosing "Church" changed almost nothing.
- `assets/eraka_places/place_words.json` (new): 11–14 starter pictures per place (HOME, SCHOOL, TAXI, STORE, MARKET, CHURCH, VILLAGE, PARK, HOSPITAL = clinic, RESTAURANT); the first is the place picture. Drafted by Claude from the interviews and existing pictures — to be checked by parents/teachers.
- `utils/ErakaPlaceWords.java` (new): loads the lists (`idsFor` skips hidden/missing pictures), `forPredictor()` (all places except home), `updateHereGroup()`.
- Prediction (`ErakaPredictor`): `setPlaceWords()`; after the child/base blend, a place with a list gets a fixed share: P = 0.7·P_model + 0.3·P(w|place), where P(w|place) blends the child's own taps at that place with the list (worth 5 taps). New config `muPlaceWords = 0.3`, `placeWordsWeight = 5`. Home is unchanged. `ErakaPredictionService.reload` sets the lists. Cloud test: a new child at church gets toilet and pray in the top 3 (none at home); with 200 home taps the effect on the 3 prediction slots is small, hence:
- Suggestion row (`ErakaSmartRowDataSource.placeSuggestions`): away from home, the 3 exploration slots show the place's pictures (list order, the place picture itself skipped, nothing already shown/in the message); exploration resumes at home.
- "Here" group (id 6100): always first in the Groups list, title "Here: Church" etc., pictures = the place's list, picture = the place picture; at home "Here: Home" with everyday words. Updated when a place is chosen (`ErakaPlaces.apply`) and when the board loads (`CargarJson`); saved only if changed. String `here_group`.
- `MainBoardActivity`: the suggestion row refreshes right after a place is chosen.
- Thesis `60design.tex` (update 13): new paragraph "Places".
- Backup: `_backup_login_activity_2/place_words_20261028/`.

## 28 Oct 2026 — Build fix: bin button also in `menu-v26`
- Build error "cannot find symbol R.id.select_delete": the toolbar menu exists twice, `menu/menu_galeria_pictos.xml` and `menu-v26/menu_galeria_pictos.xml` (the one Android 8+ uses, so also the A13). The bin was only added to the first. Now in both (same items in both files).

## 28 Oct 2026 — Sign-in: "Eraka isn't responding" (ANR) fixed
- Logcat 15:08 (first sign-in with an account whose data is downloaded): `ANR in na.eraka.app.debug (AvatarChoiceActivity) … Input dispatching timed out`, main thread blocked ~15 s; `ErakaPerf json_load ms=5691` on the main thread right after the download (`BajarJsonFirebase` case 5 → `MainBoardActivity.onDescargaCompleta` → `refreshJsonArrays()`); 213 skipped frames before it.
- `SortPictograms.quickSort` (used every time the pictures are loaded, and after `ErakaIdFix`): OTTAA's quicksort used the last element as pivot — the worst case for the already-sorted pictures file (≈600,000 JSON reads for 1,100 pictures). Now copies the range to a list and uses `Collections.sort` by id (n log n, near-linear for sorted input).
- `MainBoardActivity.onDescargaCompleta`: the reload of the downloaded files runs on a background thread while the "please wait" dialog stays up; then `initFirstPictograms()` and the dialog closes on the main thread.
- `Json.readAllJsonArrays`: new timings `json_load_pictos_read`, `json_load_pictos_corrections`, `json_load_pictos_sort`, `json_load_groups` (ErakaPerf) to see what is still slow.
- Backup: `_backup_login_activity_2/login_anr_20261028/`.

## 28 Oct 2026 — "Please wait" / yes-no windows fit the screen
- `res/layout/dialog_yes_no_cancel.xml` (OTTAA's window for "Syncing… / please wait", progress and yes/no questions, via `DialogUtils`/`ProgressDialogOptions`/`YesNoDialog`) was a fixed 400 × 150 dp, wider than the A13 screen, so it was cut off. Now `match_parent` × 170 dp.
- `DialogUtils.mostrarDialogo()`: after showing, sets the window to 92 % of the screen width (at most 420 dp), height wrap_content.
- Still fixed wide (not changed): `dialog_compartir_frases.xml` (500 dp, share phrases) and `dialog_number_picker_selector.xml` (400 dp).
- Backup: `_backup_login_activity_2/dialog_width_20261028/`.

## 28 Oct 2026 — No cloud sync at every start ("Syncing…", black screen, not responding); Eraka loader everywhere
- Cause of the remaining freeze (Keneke's screenshot "Syncing… We are synchronizing your pictograms / groups"): every time the board opened, `uploadFiles()` → `userDataExists` → `onDatosEncontrados` downloaded the whole account from the cloud (`DownloadJsonFromFirebase.syncFiles`: pictures, groups, phrases, favourites, games), compared and re-saved each file in the Firebase callbacks on the main thread, after first parsing the whole pictures file just to check it was not empty. The picture editor did the same each time it opened (`downloadData()`). It could also overwrite changes made offline with the older cloud copy.
- `MainBoardActivity.onDatosEncontrados`: no start-up sync when the phone already has its pictures file (checked by file size, not by parsing). The cloud copy is only fetched when the phone has no pictures yet; Menu → Backup & restore still restores on request. Uploads after changes are unchanged.
- `EditPictogramActivity`: `downloadData()` is no longer called when the editor opens.
- `ProgressDialogOptions` (OTTAA's orange "please wait" window, used for syncing, ARASAAC search, etc.) now shows Eraka's loader (`ErakaLoader`: mascot on a blurred background) with the window's message; falls back to the old window only without an Activity. `ErakaLoader.show(CharSequence)` / `setMessage(CharSequence)` added.
- Backup: `_backup_login_activity_2/sync_loader_20261028/`.

## 28 Oct 2026 — Board start: blank seconds reduced + timings for every start-up step
- Logcat 15:41 (Keneke: ~3 s blank): `Displayed MainBoardActivity +10s169ms`, 518 skipped frames before the first draw, then 177 (≈3 s) while the main thread did: `cargarPictosSugeridosJson` re-reading the OTTAA suggestions file on every `onResume` (0.5 s, although `initJsonArrays` had just loaded it), the first skin recolour (433 ms: loading the "no skin" list and the picture decoder), the four tiles and the suggestion row. Start-up sync now correctly skipped ("pictures are on the phone, no start-up sync").
- `Json.cargarPictosSugeridosJson`: skipped when already loaded and the files are unchanged.
- `ErakaSkinTone.warmUp()` (new), called at the start of `MainBoardActivity.initComponents`: loads the "no skin" list and recolours the first common pictures (hola, quiero, mamá, gracias) on a background thread; logs `skin_warmup`.
- `MainBoardActivity`: each start-up step timed — `board_init_layout`, `_flags`, `_backup`, `_menu`, `_selection`, `_action_buttons`, `_pictograms`, `_avatar`, `_tts`, `_scanning`, `_whisper` (new `boardStep()`), to find the remaining seconds before the first draw.
- Backup: `_backup_login_activity_2/board_start_20261028/`.

## 28 Oct 2026 — Search + loader when deleting several; place pictures first; loader for "Where are we?"
- `ErakaMultiDelete`: search box above the list (filters by name; "Select all/none" works on what is shown). New `BackgroundDelete` (work in the background, done on the main thread) with a loader "Deleting…" while the files are changed and saved. `PictogramRecyclerView.removeFromGroup` / `GroupRecyclerView.deleteGroups` now only change and save the data; the screens refresh with new `showPictures()` / `showGroups()`.
- Suggestion row away from home: the place's 3 pictures are now FIRST, the predictions after them (`items.addAll(0, placeItems)`). Note for the evaluation: at a place other than home, slots 1–3 of the logged row are place pictures.
- "Where are we?": tapping a place lights the tile, closes the window and shows the loader "Getting the pictures for Church…" (at least 0.6 s) while the "Here" group is saved in the background; then the suggestion row refreshes. `ErakaPlaces.apply()` (used at start-up) also saves the "Here" group in the background now.
- Board start: `MainBoardActivity` uses new `ErakaBoardTheme` whose window background is `bg_eraka_board_starting` (cream + Eraka logo) instead of the default, so any moment before the board is drawn shows Eraka rather than black (where Android shows the window before the first frame).
- The 17:44 logcat contained no Eraka lines (only system logs), so the remaining start-up time could not be measured.
- Strings `loader_changing_place`, `loader_deleting`, `multi_search_hint`.
- Backup: `_backup_login_activity_2/loaders_search_20261028/`.

## 28 Oct 2026 — Profile title without the waving hand
- `strings.xml` `lets_get_to_know_this_child`: "Hi there! Let's get to know you" (👋 removed, Keneke).
