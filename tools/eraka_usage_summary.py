#!/usr/bin/env python3
"""
Eraka field evaluation: how each child used the app (descriptive statistics from the usage data).

    python tools/eraka_usage_summary.py <usage folder> [--out <folder>]

<usage folder> holds what the parents sent with "Send usage data" (the same folder as for
eraka_replay_study.py): eraka_<code>_<date>.zip files, or one subfolder per child with zips or CSVs.
A parent may send the data more than once; every zip holds the whole log so far, so rows are
merged and counted once.

Files read from each zip: info.txt, tap_events.csv, message_events.csv, speech_check.csv,
voice_eval.csv (missing files are fine). Logs from the researcher's own tests must NOT be in
this folder (Section 3.7: test logs are kept separate).

Results (in <usage folder>/usage_results unless --out is given):
    usage_summary.md        everything, readable, per child and pooled
    usage_per_child.csv     one row per child (for your own checks / a spreadsheet)
    usage_table.tex         rows for the results chapter: use per child
    speech_table.tex        rows for Table tab:resspeech (speech check, start and end)
    voice_table.tex         rows: voice suggestions in everyday use
"""
import argparse
import csv
import io
import os
import re
import statistics
import sys
import zipfile
from collections import Counter, defaultdict
from datetime import datetime

CODE_RE = re.compile(r"eraka_([A-Za-z0-9-]+?)_\d{8}", re.I)
SOURCES = ["board", "smart_row", "voice", "voice_corrected", "gallery"]


# ----------------------------------------------------------------------------- reading

def _rows(text):
    text = text.lstrip("﻿")
    if not text.strip():
        return []
    head = text.split("\n", 1)[0]
    delim = ";" if head.count(";") > head.count(",") else ","
    return list(csv.DictReader(io.StringIO(text), delimiter=delim))


def _from_zip(path):
    files, info = {}, {}
    with zipfile.ZipFile(path) as z:
        for n in z.namelist():
            base = os.path.basename(n)
            if base == "info.txt":
                text = z.read(n).decode("utf-8", "replace")
                info = dict(l.split("=", 1) for l in text.splitlines() if "=" in l)
            elif base.endswith(".csv"):
                files[base] = z.read(n).decode("utf-8-sig", "replace")
    code = info.get("child_code") or None
    if not code or code == "NOCODE":
        m = CODE_RE.search(os.path.basename(path))
        code = m.group(1) if m else code
    return code, files, info


def collect(folder):
    kids = defaultdict(lambda: {"taps": {}, "msgs": {}, "speech": {}, "voice": {}, "info": {}, "sources": []})

    def add(code, files, info, src):
        k = kids[code.upper()]
        k["sources"].append(src)
        k["info"].update(info or {})
        for name, text in files.items():
            rows = _rows(text)
            if "tap_events" in name:
                for r in rows:
                    k["taps"][(r.get("id"), r.get("ts"))] = r
            elif "message_events" in name:
                for r in rows:
                    k["msgs"][(r.get("id"), r.get("ts"))] = r
            elif "speech_check" in name:
                for r in rows:
                    k["speech"][(r.get("id"), r.get("ts"))] = r
            elif "voice_eval" in name:
                for r in rows:
                    k["voice"][(r.get("timestamp"), r.get("transcript"), r.get("chosen_id"))] = r

    for entry in sorted(os.listdir(folder)):
        p = os.path.join(folder, entry)
        if entry.lower().endswith(".zip"):
            code, files, info = _from_zip(p)
            add(code or os.path.splitext(entry)[0], files, info, entry)
        elif os.path.isdir(p) and not entry.endswith("_results"):
            for sub in sorted(os.listdir(p)):
                q = os.path.join(p, sub)
                if sub.lower().endswith(".zip"):
                    code, files, info = _from_zip(q)
                    add(code or entry, files, info, f"{entry}/{sub}")
                elif sub.endswith(".csv"):
                    add(entry, {sub: open(q, encoding="utf-8-sig").read()}, {}, f"{entry}/{sub}")
    return {c: k for c, k in kids.items() if k["taps"] or k["voice"] or k["speech"]}


def _int(x, default=0):
    try:
        return int(float(x))
    except (TypeError, ValueError):
        return default


def _ids(s):
    return [int(t) for t in re.split(r"[ ,]+", (s or "").strip()) if t.lstrip("-").isdigit()]


def _day(ms):
    return datetime.fromtimestamp(_int(ms) / 1000).date()


# ----------------------------------------------------------------------------- per child

def summarise(code, k):
    taps = sorted(k["taps"].values(), key=lambda r: (_int(r.get("ts")), _int(r.get("id"))))
    msgs = sorted(k["msgs"].values(), key=lambda r: _int(r.get("ts")))
    s = {"code": code}
    info = k["info"]
    s["age"] = info.get("age_years", "")
    s["interests"] = info.get("interests", "")
    s["phone"] = info.get("phone", "")
    s["app_version"] = info.get("app_version", "")
    s["files_received"] = len(k["sources"])

    days = sorted({_day(r["ts"]) for r in taps} | {_day(r["ts"]) for r in msgs})
    s["first_day"] = str(days[0]) if days else ""
    s["last_day"] = str(days[-1]) if days else ""
    s["days_used"] = len(days)

    undone_sessions = Counter()
    for m in msgs:
        if m.get("type") == "undo":
            undone_sessions[_int(m.get("session_id"))] += 1
    s["taps"] = len(taps)
    s["undo_events"] = sum(undone_sessions.values())
    s["taps_per_day"] = round(len(taps) / len(days), 1) if days else 0
    s["different_pictograms"] = len({_int(r["picto_id"]) for r in taps})

    src = Counter((r.get("source") or "").strip() for r in taps)
    for name in SOURCES:
        s[f"share_{name}"] = round(100 * src.get(name, 0) / len(taps), 1) if taps else 0

    # suggestion row: tapped from the row; exploration: the tapped picture was an exploration slot
    smart = [r for r in taps if r.get("source") == "smart_row"]
    explore_taps = [r for r in smart if _int(r["picto_id"]) in _ids(r.get("explore_ids"))]
    shown_explore = sum(len(_ids(r.get("explore_ids"))) for r in taps)
    s["explore_selected"] = len(explore_taps)
    s["explore_shown"] = shown_explore
    s["explore_rate"] = round(100 * len(explore_taps) / shown_explore, 2) if shown_explore else ""
    # pictograms used for the first time after the first day of use (discovered during the deployment)
    seen, new_later = set(), 0
    first_day = days[0] if days else None
    for r in taps:
        pid = _int(r["picto_id"])
        if pid not in seen and first_day and _day(r["ts"]) > first_day:
            new_later += 1
        seen.add(pid)
    s["new_pictograms_after_day1"] = new_later
    s["new_from_exploration"] = len({_int(r["picto_id"]) for r in explore_taps})

    spoken = [m for m in msgs if m.get("type") == "spoken"]
    s["messages_spoken"] = len(spoken)
    lengths = [_int(m.get("length")) for m in spoken if _int(m.get("length")) > 0]
    s["mean_message_length"] = round(statistics.mean(lengths), 2) if lengths else ""
    s["multi_picture_messages"] = round(100 * sum(1 for x in lengths if x >= 2) / len(lengths), 1) if lengths else ""
    s["messages_cleared"] = sum(1 for m in msgs if m.get("type") == "cleared")

    acc = sum(1 for m in msgs if m.get("type") == "sentence_accepted")
    ign = sum(1 for m in msgs if m.get("type") == "sentence_ignored")
    nu = sum(1 for m in msgs if m.get("type") == "sentence_not_used")
    s["sentences_accepted"], s["sentences_ignored"], s["sentences_not_used"] = acc, ign, nu
    s["sentence_accept_rate"] = round(100 * acc / (acc + ign + nu), 1) if (acc + ign + nu) else ""
    s["templates_accepted"] = dict(Counter(m.get("template") for m in msgs if m.get("type") == "sentence_accepted"))

    s["place_taps"] = dict(Counter((r.get("place") or "HOME").strip() or "HOME" for r in taps))
    s["hour_taps"] = dict(Counter(_int(r.get("hour"), -1) for r in taps))

    # speech check (start / end of the deployment)
    sp = list(k["speech"].values())
    for phase in ("start", "end"):
        rows = [r for r in sp if (r.get("phase") or "").lower() == phase]
        s[f"speech_{phase}_words"] = len(rows)
        s[f"speech_{phase}_recognised"] = round(100 * sum(_int(r.get("recognised")) for r in rows) / len(rows), 1) if rows else ""
        s[f"speech_{phase}_rank1"] = round(100 * sum(1 for r in rows if _int(r.get("rank")) == 1) / len(rows), 1) if rows else ""
        s[f"speech_{phase}_top4"] = round(100 * sum(1 for r in rows if 1 <= _int(r.get("rank")) <= 4) / len(rows), 1) if rows else ""
        ms = [_int(r.get("ms")) for r in rows if _int(r.get("ms")) > 0]
        s[f"speech_{phase}_median_s"] = round(statistics.median(ms) / 1000, 2) if ms else ""

    # voice suggestions in everyday use
    vo = list(k["voice"].values())
    ranks = [_int(r.get("rank"), -1) for r in vo]
    s["voice_attempts"] = len(vo)
    s["voice_rank1"] = round(100 * ranks.count(1) / len(vo), 1) if vo else ""
    s["voice_top4"] = round(100 * sum(1 for x in ranks if 1 <= x <= 4) / len(vo), 1) if vo else ""
    s["voice_corrected"] = round(100 * ranks.count(0) / len(vo), 1) if vo else ""
    s["voice_nothing"] = round(100 * ranks.count(-1) / len(vo), 1) if vo else ""
    s["voice_sentence_mode"] = sum(1 for r in vo if (r.get("mode") or "") == "sentence")
    s["voice_learned_source"] = sum(1 for r in vo if "learn" in (r.get("source") or "").lower())
    return s


# ----------------------------------------------------------------------------- output

def fmt(x, suffix=""):
    return "--" if x in ("", None) else f"{x}{suffix}"


def tex_escape(t):
    return str(t).replace("_", r"\_").replace("%", r"\%").replace("&", r"\&")


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder")
    ap.add_argument("--out")
    a = ap.parse_args()
    kids = collect(a.folder)
    if not kids:
        sys.exit(f"no usage data found in {a.folder}")
    out = a.out or os.path.join(a.folder, "usage_results")
    os.makedirs(out, exist_ok=True)
    rows = [summarise(c, kids[c]) for c in sorted(kids)]

    flat_keys = [k for k in rows[0] if not isinstance(rows[0][k], dict)]
    with open(os.path.join(out, "usage_per_child.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.DictWriter(f, fieldnames=flat_keys, extrasaction="ignore")
        w.writeheader()
        w.writerows(rows)

    # pooled numbers
    def pooled(key):
        vals = [r[key] for r in rows if r[key] not in ("", None)]
        return (statistics.median(vals), min(vals), max(vals)) if vals else None

    md = ["# Eraka usage data - summary", "",
          f"Children: {len(rows)} ({', '.join(r['code'] for r in rows)})", ""]
    for r in rows:
        md += [f"## {r['code']}", ""]
        for k, v in r.items():
            if k != "code":
                md.append(f"- {k}: {v}")
        md.append("")
    md += ["## Pooled (median, min-max over children)", ""]
    for key in ["days_used", "taps", "taps_per_day", "different_pictograms", "messages_spoken",
                "mean_message_length", "multi_picture_messages", "sentence_accept_rate", "explore_rate",
                "new_pictograms_after_day1", "voice_attempts", "voice_rank1", "voice_top4"]:
        p = pooled(key)
        md.append(f"- {key}: " + (f"median {p[0]}, range {p[1]}-{p[2]}" if p else "no data"))
    tot_acc = sum(r["sentences_accepted"] for r in rows)
    tot_all = tot_acc + sum(r["sentences_ignored"] + r["sentences_not_used"] for r in rows)
    md.append(f"- sentence suggestions accepted, all children: {tot_acc} of {tot_all}"
              + (f" ({100 * tot_acc / tot_all:.1f}%)" if tot_all else ""))
    open(os.path.join(out, "usage_summary.md"), "w", encoding="utf-8").write("\n".join(md) + "\n")

    # LaTeX: use per child
    t = ["% Rows for the results chapter: use of Eraka per child (eraka_usage_summary.py).",
         "% Columns: child & days & taps & different pictograms & messages spoken & mean length"
         " & from suggestion row (%) & sentences accepted (%) \\\\"]
    for r in rows:
        t.append(" & ".join([tex_escape(r["code"]), str(r["days_used"]), str(r["taps"]), str(r["different_pictograms"]),
                             str(r["messages_spoken"]), fmt(r["mean_message_length"]), fmt(r["share_smart_row"]),
                             fmt(r["sentence_accept_rate"])]) + r" \\")
    open(os.path.join(out, "usage_table.tex"), "w", encoding="utf-8").write("\n".join(t) + "\n")

    # LaTeX: speech check, rows for Table tab:resspeech
    t = ["% Rows for Table tab:resspeech (eraka_usage_summary.py).",
         "% Columns: child & recognised (start) & correct picture first (start) & recognised (end) & correct picture first (end) \\\\"]
    for r in rows:
        t.append(" & ".join([tex_escape(r["code"]), fmt(r["speech_start_recognised"], "%"), fmt(r["speech_start_rank1"], "%"),
                             fmt(r["speech_end_recognised"], "%"), fmt(r["speech_end_rank1"], "%")]).replace("%", r"\%") + r" \\")
    open(os.path.join(out, "speech_table.tex"), "w", encoding="utf-8").write("\n".join(t) + "\n")

    # LaTeX: voice suggestions in everyday use
    t = ["% Rows: voice suggestions in everyday use (eraka_usage_summary.py).",
         "% Columns: child & attempts & right picture first & right picture in top 4 & corrected by caregiver & nothing chosen & whole sentences \\\\"]
    for r in rows:
        t.append(" & ".join([tex_escape(r["code"]), str(r["voice_attempts"]), fmt(r["voice_rank1"], "%"), fmt(r["voice_top4"], "%"),
                             fmt(r["voice_corrected"], "%"), fmt(r["voice_nothing"], "%"), str(r["voice_sentence_mode"])])
                 .replace("%", r"\%") + r" \\")
    open(os.path.join(out, "voice_table.tex"), "w", encoding="utf-8").write("\n".join(t) + "\n")
    print(f"{len(rows)} children -> {out}")


if __name__ == "__main__":
    main()
