# Eraka field evaluation: from the data to the results chapter

Three scripts turn what comes back after 8 November into numbers and LaTeX table rows.
Run them from the project folder (`My Projects\Eraka`) in a terminal with Python 3.
The questionnaire script also needs `pip install openpyxl` (once).

## 1. Collect the data

- **Usage data:** save every zip the parents send (WhatsApp → download / Gmail → attachment) into
  `Documents\Eraka\usage_logs\field_2026\`. Keep the names as they are (`eraka_C01_20261108_1930.zip`).
  If a parent sends twice, keep both: rows are merged and counted once.
  Do NOT put your own test zips in this folder.
- **Questionnaires:** open the sheet "Eraka – evaluation responses" in Google Sheets →
  File → Download → Microsoft Excel (.xlsx). Save it as
  `Documents\Eraka\testing\eraka_responses.xlsx`. Your own test answers: give them a code that starts
  with TEST and they are ignored.

## 2. Run the scripts

```
python tools\eraka_usage_summary.py "C:\Users\KEKasaona\Documents\Eraka\usage_logs\field_2026"
python tools\eraka_replay_study.py  "C:\Users\KEKasaona\Documents\Eraka\usage_logs\field_2026" --warmup 20
python tools\eraka_questionnaire_analysis.py "C:\Users\KEKasaona\Documents\Eraka\testing\eraka_responses.xlsx"
```

## 3. What you get and where it goes in the thesis

| File | Goes into |
|---|---|
| `usage_results\usage_table.tex` | new table: use per child (days, taps, pictograms, messages, length, suggestion row, sentences accepted) |
| `usage_results\speech_table.tex` | rows of Table `tab:resspeech` |
| `usage_results\voice_table.tex` | voice suggestions in everyday use (Speech input paragraph) |
| `usage_results\usage_summary.md` | exploration, new pictograms, places, sentence acceptance - numbers for the text |
| `replay_results\replay_table.tex`, `replay_table_warm.tex` | rows of Table `tab:resreplay` (all taps / after the warm-up) |
| `replay_results\results.md` | the replay in words, per child |
| `questionnaire_results\usability_table.tex` | rows of Table `tab:resusability` (SUS, checklist before/after/change) |
| `questionnaire_results\sus_table.tex`, `checklist_table.tex` | detail for the text or an appendix |
| `questionnaire_results\culture_table.tex` | cultural fit: parents' and teachers' agreement items, and the importance grid from the first questionnaire |
| `questionnaire_results\sentences_table.tex` | teachers' rating of the 15 example sentences |
| `questionnaire_results\open_answers.md` | all open answers by question - code these for the thematic analysis |

Easiest: send me the three result folders (or the zips and the .xlsx) and I write the results chapter from them.

## Notes

- The codes in the thesis tables (C1, F1 ...) can simply become the codes the parents used (C01 ...).
- SUS: odd items answer − 1, even items 5 − answer, sum × 2.5 (0–100). A person who skipped an item gets no score.
- Checklist: B1–B10 summed (10–50, higher = communicates more often). B11 (frustration) is reported
  separately because lower is better. B12 = how easy it is to understand the child (1–5); B13 = parents'
  own comparison after use.
- Exploration rate = exploration pictures tapped ÷ exploration pictures shown.
- Sentence acceptance = accepted ÷ (accepted + ignored + spoken without using the sentence).
- Tested with made-up data only (`eraka_make_test_logs.py` and a fake responses sheet) - never report those numbers.
