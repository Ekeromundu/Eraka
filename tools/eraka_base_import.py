#!/usr/bin/env python3
"""
Eraka base-model importer.

Turns the starter messages from teachers and parents into picture-ID sequences
for the app's base model (cold start).

    python tools/eraka_base_import.py                      # default paths below
    python tools/eraka_base_import.py --raw my.csv --out some/other.csv

Input  eraka_base_data/raw_messages.csv   (columns: source,time,place,message)
         source  = teacher / parent / questionnaire (free text, kept for the thesis)
         time    = morning / lunch / 14:30 / evening ... or empty
         place   = home / school / shop ... or empty
         message = the sentence as the teacher or parent wrote it
       eraka_base_data/synonyms.csv       (word,replacement)  e.g. mum,mom
       eraka_base_data/choose_id.csv      (name,id)  which picture to use when several share a name
       eraka_base_data/extra_vocab.csv    (id,name)  extra pictures, e.g. the Namibian photo set
Output app/src/main/assets/eraka_base/base_messages.csv   (read by ErakaBaseModel.java)
       eraka_base_data/import_report.md                     (what matched, what did not)

Steps for each message (tokenisation):
  1. lowercase, remove punctuation (keep apostrophes)
  2. replace synonyms (mum -> mom, toilet -> bathroom, ...)
  3. split into words
  4. match the longest picture name first ("brush my teeth" before "brush"),
     trying the singular form too (apples -> apple)
  5. drop small function words that children do not tap (the, a, to, ...)
  6. report every other word as "no picture yet" (candidates for new pictures)
"""
import argparse
import collections
import csv
import json
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VOCAB = os.path.join(ROOT, "app/src/main/assets/eraka_seed/Archivos_Paises/pictos/pictos_en.txt")
DATA = os.path.join(ROOT, "eraka_base_data")
OUT = os.path.join(ROOT, "app/src/main/assets/eraka_base/base_messages.csv")

MAX_PHRASE = 4  # longest picture name in words that we try to match
FUNCTION_WORDS = {"the", "a", "an", "to", "of", "is", "am", "are", "it", "some", "at", "on", "with",
                  "me", "my", "for",   # 22 Oct 2026: "give me phone", "looking for my shoes"
                  "may", "can", "let's", "lets", "are", "and", "in", "some"}   # "May I please...", "Can we..."

TIME_WORDS = {
    "MORNING": ["morning", "breakfast", "early", "wake", "am"],
    "MIDDAY": ["midday", "noon", "lunch"],
    "AFTERNOON": ["afternoon", "after school"],
    "EVENING": ["evening", "night", "dinner", "supper", "bedtime", "bed time"],
}
PLACE_WORDS = {
    "HOME": ["home", "house"],
    "SCHOOL": ["school", "class", "classroom"],
    "PARK": ["park", "playground"],
    "STORE": ["shop", "store", "supermarket", "mall"],
    "MARKET": ["market", "open market"],
    "TAXI": ["taxi", "car", "bus", "combi", "on the road"],
    "VILLAGE": ["village", "farm", "cattle post", "north"],
    "RESTAURANT": ["restaurant", "takeaway"],
    "HOSPITAL": ["hospital", "clinic", "doctor"],
    "CHURCH": ["church"],
}


def normalise(text):
    text = text.lower().replace("’", "'").replace("‘", "'")
    text = re.sub(r"[^a-z0-9' ]+", " ", text)
    return re.sub(r"\s+", " ", text).strip()


def singular(word):
    if len(word) <= 3 or word.endswith("ss"):
        return word
    if word.endswith("ies"):
        return word[:-3] + "y"
    for end in ("ches", "shes", "xes", "ses", "oes"):
        if word.endswith(end):
            return word[:-2]
    if word.endswith("s"):
        return word[:-1]
    return word


def time_bucket(value):
    v = normalise(value)
    if not v:
        return ""
    m = re.match(r"^(\d{1,2})(?: (\d{2}))?$", v.replace("h", " ").strip())
    if m:  # "14:30" became "14 30" after normalise
        h = int(m.group(1))
        if 5 <= h < 12: return "MORNING"
        if 12 <= h < 15: return "MIDDAY"
        if 15 <= h < 20: return "AFTERNOON"
        return "EVENING"
    for bucket, words in TIME_WORDS.items():
        if any(re.search(r"\b%s\b" % re.escape(w), v) for w in words):  # whole words: "noon" is not "afternoon"
            return bucket
    return ""


def place_key(value):
    v = normalise(value)
    for key, words in PLACE_WORDS.items():
        if any(w in v.split() or w == v for w in words):
            return key
    return ""


def read_csv(path, columns):
    if not os.path.exists(path):
        return []
    with open(path, newline="", encoding="utf-8-sig") as f:
        rows = list(csv.DictReader(f))
    for r in rows:
        for c in columns:
            if c not in r:
                sys.exit(f"{path}: missing column '{c}'")
    return rows


CORRECTIONS = os.path.join(ROOT, "app/src/main/assets/eraka_corrections/vocabulary_corrections.json")
RES = os.path.join(ROOT, "app/src/main/res")


def has_drawing(name):
    """True when res/drawable*/<name>.png|webp|jpg|xml exists (the app can show the picture)."""
    if not name or not os.path.isdir(RES):
        return False
    for d in os.listdir(RES):
        if d.startswith("drawable"):
            for ext in (".png", ".webp", ".jpg", ".xml"):
                if os.path.exists(os.path.join(RES, d, name + ext)):
                    return True
    return False


def load_vocabulary(extra_path, choose_path):
    """Picture names -> ids, as the child sees them in Eraka: the app's vocabulary corrections
    are applied (22 Oct 2026), so hidden and caregiver-only pictures are never used, renamed
    pictures are found by their new label, and new pictures ("add") are included once their
    drawing is in the app."""
    with open(VOCAB, encoding="utf-8") as f:
        pictos = json.load(f)
    corr = {}
    if os.path.exists(CORRECTIONS):
        with open(CORRECTIONS, encoding="utf-8") as f:
            corr = json.load(f)
    blocked = {int(k) for k in corr.get("hide", {})} | {int(k) for k in corr.get("caregiver_only", {})}
    renamed = {int(k): v.get("to", "") for k, v in corr.get("rename", {}).items()}
    for k, a in corr.get("add", {}).items():
        if has_drawing(a.get("picto", "")):
            pictos.append({"id": int(k), "texto": {"en": a.get("en", "")}})
    ids_by_name = collections.OrderedDict()
    for p in pictos:
        if p["id"] in blocked:
            continue
        name = normalise(renamed.get(p["id"]) or p.get("texto", {}).get("en", ""))
        if name:
            ids_by_name.setdefault(name, []).append(p["id"])
    for r in read_csv(extra_path, ["id", "name"]):
        ids_by_name.setdefault(normalise(r["name"]), []).insert(0, int(r["id"]))  # own photos win
    chosen = {normalise(r["name"]): int(r["id"]) for r in read_csv(choose_path, ["name", "id"])}
    vocab, ambiguous = {}, {}
    for name, ids in ids_by_name.items():
        vocab[name] = chosen.get(name, ids[0])
        if len(ids) > 1 and name not in chosen:
            ambiguous[name] = ids
    return vocab, ambiguous


def tokenise(message, vocab, synonyms):
    """Returns (matched [(name, id)], unmatched words, dropped function words)."""
    text = normalise(message)
    for word, repl in synonyms:
        text = re.sub(r"\b%s\b" % re.escape(word), repl, text)
    words = text.split()
    matched, unmatched, dropped = [], [], []
    i = 0
    while i < len(words):
        hit = None
        for n in range(min(MAX_PHRASE, len(words) - i), 0, -1):
            phrase = words[i:i + n]
            for cand in (" ".join(phrase), " ".join(phrase[:-1] + [singular(phrase[-1])])):
                if cand in vocab:
                    hit = (cand, n)
                    break
            if hit:
                break
        if hit and hit[1] == 1 and words[i] in FUNCTION_WORDS:
            dropped.append(words[i])  # a picture exists ("with", "a") but children rarely tap it
            i += 1
        elif hit:
            matched.append((hit[0], vocab[hit[0]]))
            i += hit[1]
        else:
            (dropped if words[i] in FUNCTION_WORDS else unmatched).append(words[i])
            i += 1
    return matched, unmatched, dropped


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("--raw", default=os.path.join(DATA, "raw_messages.csv"))
    ap.add_argument("--out", default=OUT)
    ap.add_argument("--report", default=os.path.join(DATA, "import_report.md"))
    args = ap.parse_args()

    vocab, ambiguous = load_vocabulary(os.path.join(DATA, "extra_vocab.csv"), os.path.join(DATA, "choose_id.csv"))
    synonyms = [(normalise(r["word"]), normalise(r["replacement"]))
                for r in read_csv(os.path.join(DATA, "synonyms.csv"), ["word", "replacement"])]
    synonyms.sort(key=lambda s: -len(s[0]))  # longest first
    rows = read_csv(args.raw, ["source", "time", "place", "message"])
    if not rows:
        sys.exit(f"No messages found in {args.raw}")

    out_lines, report_rows = [], []
    missing, used_ambiguous = collections.Counter(), set()
    n_words = n_matched_words = n_skipped = 0
    for r in rows:
        matched, unmatched, dropped = tokenise(r["message"], vocab, synonyms)
        missing.update(unmatched)
        used_ambiguous.update(name for name, _ in matched if name in ambiguous)
        n_words += sum(len(name.split()) for name, _ in matched) + len(unmatched)
        n_matched_words += sum(len(name.split()) for name, _ in matched)
        t, p = time_bucket(r["time"]), place_key(r["place"])
        if not matched:
            n_skipped += 1
        else:
            text = r["message"].replace("\n", " ").strip()
            out_lines.append(f"{normalise(r['source'])},{t},{p},{' '.join(str(i) for _, i in matched)},{text}")
        report_rows.append((r["message"].strip(), " + ".join(f"{n} ({i})" for n, i in matched) or "-",
                            ", ".join(unmatched) or "", t or "?", p or "?"))

    os.makedirs(os.path.dirname(args.out), exist_ok=True)
    with open(args.out, "w", encoding="utf-8", newline="\n") as f:
        f.write("# Eraka base model: starter messages as picture IDs. Made by tools/eraka_base_import.py - do not edit by hand.\n")
        f.write("source,time,place,picto_ids,text\n")
        f.write("\n".join(out_lines) + "\n")

    cover = 100.0 * n_matched_words / n_words if n_words else 0
    with open(args.report, "w", encoding="utf-8") as f:
        f.write("# Eraka base model import report\n\n")
        f.write(f"- Messages read: {len(rows)}; kept: {len(out_lines)}; skipped (no picture matched): {n_skipped}\n")
        f.write(f"- Content-word coverage: {n_matched_words}/{n_words} = {cover:.1f}% (function words like 'the', 'to' not counted)\n")
        f.write(f"- Starter taps for the base model: {sum(len(l.split(',')[3].split()) for l in out_lines)}\n\n")
        f.write("## Words with no picture yet (most frequent first)\n\nAdd a picture (e.g. Namibian photo set + extra_vocab.csv) or a synonym (synonyms.csv).\n\n")
        f.write("| word | times |\n|---|---|\n")
        for w, c in missing.most_common():
            f.write(f"| {w} | {c} |\n")
        f.write("\n## Names shared by several pictures (first ID used; fix in choose_id.csv)\n\n| name | IDs | used |\n|---|---|---|\n")
        for name in sorted(used_ambiguous):
            f.write(f"| {name} | {' '.join(map(str, ambiguous[name]))} | {vocab[name]} |\n")
        f.write("\n## Every message\n\n| message | pictures | no picture | time | place |\n|---|---|---|---|---|\n")
        for m, pics, miss, t, p in report_rows:
            f.write(f"| {m} | {pics} | {miss} | {t} | {p} |\n")
    print(f"{len(out_lines)}/{len(rows)} messages kept, coverage {cover:.1f}%, "
          f"{len(missing)} words without a picture -> {args.report}")


if __name__ == "__main__":
    main()
