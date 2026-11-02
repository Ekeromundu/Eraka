# Eraka

Eraka (Otjiherero for "voice") is an offline Android AAC (Augmentative and Alternative Communication) application for non-verbal and minimally verbal autistic children in Namibia. It was developed for a Master of Computer Science thesis at the Namibia University of Science and Technology (NUST), *An Augmentative Communication Tool for Autistic Children in Namibia*.

Eraka adopts the open-source [OTTAA Project](https://github.com/OTTAA-Project) as its framework and enhances it. The original OTTAA README is kept in `OTTAA_Readme.md`.

## What Eraka adds to OTTAA

- **Offline use:** all communication functions, including the AI components, run on the phone after a one-time Google sign-in. Firebase is used only for sign-in and an optional backup made by the caregiver.
- **Pictogram prediction:** a per-child interpolated n-gram model (two previous pictograms, time of day, place, 14-day recency), started from a base model of Namibian parents' messages, with exploration of new pictograms (UCB) and learning from ignored suggestions (`na.eraka.erakaprediction`).
- **Sentence construction:** 24 templates realised with SimpleNLG turn the selected pictograms into a grammatical sentence that the child can accept or ignore (`ErakaSentenceBuilder`).
- **Speech input:** on-device speech recognition with Whisper (`ggml-tiny.en-q5_1`) through whisper.cpp, with a vocabulary hint and a personal voice dictionary that learns each child's pronunciation (`WhisperManager`, `VoiceDictionary`).
- **Cultural content:** skin colour of the pictograms matched to the child, Namibian avatars, local foods, places and routines (*My day*), the family's own words and voice recordings..
- **Caregiver tools:** caregiver lock, child profile, progress report, teach a word, speech check, backup and restore, and "Send usage data" for the study.

## Building

1. Open the project in Android Studio.
2. Add a `google-services.json` from your own Firebase project to `app/` (it is not included in this repository).
3. The Whisper model `ggml-tiny.en-q5_1.bin` is in `app/src/main/assets/`.
4. Build and run on a phone or tablet with Android 8 (API 26) or newer.

## Tools

`tools/` contains the Python scripts used in the evaluation: the offline replay of the prediction model (`eraka_replay_eval.py`, `eraka_predictor.py`), the usage-log summary and the questionnaire analysis.

## Acknowledgements and licence

Eraka is based on the OTTAA Project and, like OTTAA, is licensed under the GNU General Public License v3 (see `License.txt`). Pictograms are from [ARASAAC](https://arasaac.org) (CC BY-NC-SA 4.0). Avatars use the Avataaars illustrations by Pablo Stanley. 
