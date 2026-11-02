#!/usr/bin/env python3
"""
Eraka pilot: replay evaluation for ALL children at once (wraps eraka_replay_eval.py).

    python tools/eraka_replay_study.py <pilot folder> [--warmup 20] [--out <folder>]

<pilot folder> holds what the parents sent with "Send usage data", in any of these forms:
    eraka_C01_20261105_1830.zip            zips straight from WhatsApp / Gmail
    C01/  (CSV files or zips inside)       one subfolder per child
The child code is read from info.txt in the zip, else from the zip name (eraka_<code>_...),
else from the subfolder name. A parent may send the data more than once: every zip holds
the whole log so far, so taps are merged by their id and counted once.

Each child gets its own fresh models (the model is per child, as on the phone), and the
taps are replayed exactly as in eraka_replay_eval.py. Results:
    replay_results/results.md          everything, readable
    replay_results/replay_table.tex    rows for Table tab:resreplay (all children pooled)
    replay_results/replay_per_child.tex  top-4 per child (appendix / discussion)
    replay_results/ranks_<code>.csv    every replayed tap with each model's rank
"""
import argparse
import csv
import io
import os
import re
import statistics
import sys
import zipfile
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import eraka_replay_eval as ev  # noqa: E402

# Thesis rows (Table tab:resreplay) -> model name in eraka_replay_eval.py, in table order
THESIS_ROWS = [
    ("Most used", "most_used"),
    ("Bigram", "bigram"),
    ("Trigram without time or place", "trigram_plain"),
    ("Stupid Backoff", "stupid_backoff"),
    ("SpeakBright-style", "speakbright"),
    ("OTTAA (approximation)", "ottaa"),
    ("Prototype predictor", "eduardo_fixed"),   # the model itself; 'eduardo' = as the demo app ran it
    ("Eraka without time and place", "eraka_no_ctx"),
    ("Eraka without recency", "eraka_no_rec"),
    (r"\textbf{Eraka}", "eraka"),
    ("Eraka with base model", "eraka_base"),
]
CODE_RE = re.compile(r"eraka_([A-Za-z0-9-]+?)_\d{8}", re.I)


# ----------------------------------------------------------------------------- reading

def _rows(text):
    text = text.lstrip("﻿")
    head = text.split("\n", 1)[0]
    delim = ";" if head.count(";") > head.count(",") else ","
    return list(csv.DictReader(io.StringIO(text), delimiter=delim))


def _from_zip(path):
    files, code, info = {}, None, {}
    with zipfile.ZipFile(path) as z:
        for n in z.namelist():
            base = os.path.basename(n)
            if base == "info.txt":
                text = z.read(n).decode("utf-8", "replace")
                info = dict(l.split("=", 1) for l in text.splitlines() if "=" in l)
                code = info.get("child_code") or None
            elif base.endswith(".csv"):
                files[base] = z.read(n).decode("utf-8-sig", "replace")
    if not code:
        m = CODE_RE.search(os.path.basename(path))
        code = m.group(1) if m else None
    files["__info__"] = info
    return code, files


def collect(folder):
    """child code -> {'taps': [...], 'msgs': [...], 'sources': [...]} (raw rows, merged)."""
    kids = defaultdict(lambda: {"taps": {}, "msgs": {}, "sources": [], "info": {}})

    def add(code, files, src):
        k = kids[code]
        k["sources"].append(src)
        k["info"].update(files.pop("__info__", {}) or {})   # the newest zip read last wins
        for name, text in files.items():
            if "tap_events" in name:
                for r in _rows(text):
                    k["taps"][(r["id"], r["ts"])] = r
            elif "message_events" in name:
                for r in _rows(text):
                    k["msgs"][(r["id"], r["ts"])] = r

    for entry in sorted(os.listdir(folder)):
        p = os.path.join(folder, entry)
        if entry.lower().endswith(".zip"):
            code, files = _from_zip(p)
            add(code or os.path.splitext(entry)[0], files, entry)
        elif os.path.isdir(p) and entry != "replay_results":
            for sub in sorted(os.listdir(p)):
                q = os.path.join(p, sub)
                if sub.lower().endswith(".zip"):
                    code, files = _from_zip(q)
                    add(code or entry, files, f"{entry}/{sub}")
                elif sub.endswith(".csv"):
                    add(entry, {sub: open(q, encoding="utf-8-sig").read()}, f"{entry}/{sub}")
    return {c: k for c, k in kids.items() if k["taps"]}


def prepare(raw_taps, raw_msgs):
    """Same conversions and marking as eraka_replay_eval.load_log."""
    taps, msgs = [dict(r) for r in raw_taps], [dict(r) for r in raw_msgs]
    for r in taps:
        for k in ("id", "ts", "session_id", "position", "picto_id", "prev1_id", "prev2_id", "hour"):
            r[k] = int(float(r[k]))
        r["place"] = (r.get("place") or "").strip()
    for r in msgs:
        r["ts"], r["session_id"], r["length"] = int(float(r["ts"])), int(float(r["session_id"])), int(float(r["length"]))
    taps.sort(key=lambda r: (r["ts"], r["id"]))
    ev.mark_undone(taps, msgs)
    ev.mark_repeats(taps)
    return taps, msgs


# ----------------------------------------------------------------------------- per child

def run_child(code, kid, warmup):
    taps, msgs = prepare(kid["taps"].values(), kid["msgs"].values())
    models = ev.all_models()
    rows = ev.replay(taps, models)
    names = [m.name for m in models]
    seen, novel = set(), 0
    for r in rows:
        novel += r["picto"] not in seen
        seen.add(r["picto"])
    days = len({t["ts"] // 86400000 for t in taps})
    return {"code": code, "taps": taps, "msgs": msgs, "rows": rows, "names": names,
            "novel": novel, "days": days, "sources": kid["sources"], "info": kid["info"],
            "all": ev.summarise(rows, names), "warm": ev.summarise(rows, names, warmup)}


def pooled_warm(results, names, warmup):
    return ev.summarise([r for res in results for r in res["rows"][warmup:]], names)


# ----------------------------------------------------------------------------- output

def pct(x):
    return f"{100 * x:.1f}"


def tex_table(summary, n_children, n_taps, note):
    out = [f"% Rows for Table tab:resreplay - {note}",
           f"% {n_children} children, {n_taps} replayed taps (pooled). Generated by tools/eraka_replay_study.py",
           r"% Paste between \hline after the header row and the closing \hline.",
           r"% Top-1/4/6 in per cent; MRR 0-1. '--' = model not available (e.g. no base model yet)."]
    for label, name in THESIS_ROWS:
        s = summary.get(name)
        if s is None:
            out.append(f"{label} & -- & -- & -- & -- \\\\")
        else:
            out.append(f"{label} & {pct(s['top1'])} & {pct(s['top4'])} & {pct(s['top6'])} & {s['mrr']:.3f} \\\\")
    return "\n".join(out) + "\n"


def tex_per_child(results):
    codes = [r["code"] for r in results]
    head = "Model & " + " & ".join(codes) + r" & Median \\ \hline"
    out = ["% Top-4 accuracy (%) per child, all replayed taps. Generated by tools/eraka_replay_study.py",
           r"\begin{tabular}{l" + "c" * (len(codes) + 1) + "}", r"\hline", head]
    for label, name in THESIS_ROWS:
        vals = [r["all"][name]["top4"] if name in r["all"] else None for r in results]
        have = [v for v in vals if v is not None]
        cells = [pct(v) if v is not None else "--" for v in vals]
        med = pct(statistics.median(have)) if have else "--"
        out.append(f"{label} & " + " & ".join(cells) + f" & {med} \\\\")
    out += [r"\hline", r"\end{tabular}"]
    return "\n".join(out) + "\n"


def md_overview(results):
    lines = ["| child | level | age | interests | days with taps | taps logged | undone | replayed | first-ever use | ceiling (top-k) | sources |",
             "|---|---|---|---|---|---|---|---|---|---|---|"]
    for r in results:
        n = len(r["rows"]) or 1
        inf = r.get("info", {})
        lines.append(f"| {r['code']} | {inf.get('comm_level', '')} | {inf.get('age_years', '')} | "
                     f"{inf.get('interests', '').replace(',', ', ')} | {r['days']} | {len(r['taps'])} | {sum(t['undone'] for t in r['taps'])} | "
                     f"{len(r['rows'])} | {r['novel']} ({r['novel'] / n:.1%}) | {1 - r['novel'] / n:.1%} | "
                     f"{', '.join(r['sources'])} |")
    return "\n".join(lines) + "\n"


def md_per_child(results, metric):
    names = results[0]["names"]
    lines = ["| model | " + " | ".join(r["code"] for r in results) + " | median |",
             "|---" * (len(results) + 2) + "|"]
    for n in names:
        vals = [r["all"][n][metric] for r in results if n in r["all"]]
        fmt = (lambda v: f"{v:.3f}") if metric == "mrr" else (lambda v: f"{v:.1%}")
        cells = [fmt(r["all"][n][metric]) if n in r["all"] else "--" for r in results]
        lines.append(f"| {n} | " + " | ".join(cells) + f" | {fmt(statistics.median(vals)) if vals else '--'} |")
    return "\n".join(lines) + "\n"


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder")
    ap.add_argument("--warmup", type=int, default=20, help="also report results after each child's first N taps")
    ap.add_argument("--out", help="output folder (default <folder>/replay_results)")
    a = ap.parse_args()

    kids = collect(a.folder)
    if not kids:
        sys.exit(f"no usage data found in {a.folder} (expected eraka_<code>_*.zip files or one folder per child)")
    results = [run_child(c, kids[c], a.warmup) for c in sorted(kids)]
    names = results[0]["names"]
    allp = ev.summarise([r for res in results for r in res["rows"]], names)
    warmp = pooled_warm(results, names, a.warmup)
    n_taps = sum(len(r["rows"]) for r in results)
    novel = sum(r["novel"] for r in results)

    out = a.out or os.path.join(a.folder, "replay_results")
    os.makedirs(out, exist_ok=True)
    rep = [f"# Eraka replay evaluation - {len(results)} children", "",
           f"Replayed taps: {n_taps}; first-ever use of a picture: {novel} ({novel / max(1, n_taps):.1%}) - no model "
           f"starting from zero can predict these. Base model: "
           f"{'yes' if 'eraka_base' in names else 'no (assets/eraka_base/base_messages.csv not found)'}.", "",
           "## Children", "", md_overview(results),
           ev.table(allp, "All children pooled - all taps (goes into Table tab:resreplay)"),
           ev.table(warmp, f"All children pooled - after each child's first {a.warmup} taps"),
           "## Per child: top-4 (the four tiles)", "", md_per_child(results, "top4"),
           "## Per child: MRR", "", md_per_child(results, "mrr")]
    for r in results:
        rep.append(ev.table(r["all"], f"{r['code']} - all taps"))
        rep.append(ev.live_table(r["taps"]))
    rep.append("Notes: each child has his or her own models, trained only on that child's earlier taps. 'Pooled' "
               "counts every replayed tap once, so children who used the app more weigh more; the per-child "
               "tables and medians show whether one child dominates. 'Prototype predictor' in the thesis table is "
               "eduardo_fixed (the prototype's model with context reset per message and undone taps ignored); "
               "'eduardo' is the prototype as its demo app ran it.\n")
    text = "\n".join(rep)
    open(os.path.join(out, "results.md"), "w", encoding="utf-8").write(text)
    open(os.path.join(out, "replay_table.tex"), "w", encoding="utf-8").write(
        tex_table(allp, len(results), n_taps, "all taps"))
    open(os.path.join(out, "replay_table_warm.tex"), "w", encoding="utf-8").write(
        tex_table(warmp, len(results), sum(max(0, len(r["rows"]) - a.warmup) for r in results),
                  f"after each child's first {a.warmup} taps"))
    open(os.path.join(out, "replay_per_child.tex"), "w", encoding="utf-8").write(tex_per_child(results))
    for r in results:
        if r["rows"]:
            with open(os.path.join(out, f"ranks_{r['code']}.csv"), "w", encoding="utf-8", newline="") as f:
                w = csv.DictWriter(f, fieldnames=list(r["rows"][0].keys()))
                w.writeheader()
                w.writerows(r["rows"])
    print(text)
    print(f"-> {out}")


if __name__ == "__main__":
    main()
