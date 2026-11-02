# Eraka base model data

1. Put the teacher and parent messages in `raw_messages.csv` (one message per row):
   `source,time,place,message` e.g. `parent,morning,home,"I want water, please"`
   (quote a message that contains a comma). time and place may be empty.
2. Run from the project folder: `python tools/eraka_base_import.py`
3. Read `import_report.md`: words with no picture -> add a synonym (`synonyms.csv`)
   or a picture (`extra_vocab.csv`, e.g. the Namibian photo set); names shared by
   several pictures -> pick one in `choose_id.csv`. Run again.
4. The script writes `app/src/main/assets/eraka_base/base_messages.csv`; rebuild the app.

`SAMPLE_raw_messages.csv` and `sample_output/` are made-up test data, NOT for the app.
