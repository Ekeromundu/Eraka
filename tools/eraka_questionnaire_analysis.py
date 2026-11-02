#!/usr/bin/env python3
"""
Eraka field evaluation: analysis of the three Google Forms questionnaires.

    python tools/eraka_questionnaire_analysis.py <responses.xlsx | folder of CSVs> [--out <folder>]

Input: the responses spreadsheet "Eraka – evaluation responses", downloaded from Google Sheets with
File > Download > Microsoft Excel (.xlsx), or each tab downloaded as CSV into one folder. The script
recognises each tab by its questions, so the tab names do not matter:
    parents before  - has A1..A7 and B1..B12
    parents after   - has B13, U1..U10, the SUS, F1..F14, G1..G5, H1..H8
    teachers        - has T1..T4 and the 15 example sentences
Answers are linked by the participant code (upper case, spaces removed), so the same code must be
used on both parent questionnaires. Rows whose code starts with TEST (your own test answers) are ignored.

Results (in <input folder>/questionnaire_results unless --out is given):
    questionnaire_summary.md     everything, readable
    sus_table.tex                rows: SUS per respondent + median
    usability_table.tex          rows for Table tab:resusability (SUS, checklist before/after/change)
    checklist_table.tex          rows: communication checklist in detail (B11, B12, B13 too)
    culture_table.tex            rows: F items (parents and teachers), median and % agree
    sentences_table.tex          rows: teachers' rating of the 15 example sentences
    open_answers.md              all open answers by question, for the thematic analysis
    per_respondent.csv           SUS and checklist scores per code
"""
import argparse
import csv
import os
import re
import statistics
import sys
from collections import Counter, OrderedDict, defaultdict

ITEM_RE = re.compile(r"(?:^|\[|\s)([A-Z]{1,2}\d{1,2})\.\s")
EASE5 = {"very hard": 1, "hard": 2, "sometimes easy, sometimes hard": 3, "easy": 4, "very easy": 5}
COMPARE5 = {"much harder": 1, "a little harder": 2, "about the same": 3, "a little easier": 4, "much easier": 5}
SENT = ["Correct and natural", "Correct, but not natural", "Small error", "Wrong"]


# ----------------------------------------------------------------------------- reading

def read_tables(path):
    """-> list of (name, header, rows as dicts)."""
    tables = []
    if os.path.isdir(path):
        for f in sorted(os.listdir(path)):
            if f.lower().endswith(".csv"):
                with open(os.path.join(path, f), encoding="utf-8-sig") as fh:
                    r = list(csv.reader(fh))
                if r:
                    tables.append((f, r[0], [dict(zip(r[0], x)) for x in r[1:] if any(c.strip() for c in x)]))
    else:
        import openpyxl
        wb = openpyxl.load_workbook(path, read_only=True, data_only=True)
        for ws in wb.worksheets:
            vals = [["" if c is None else str(c) for c in row] for row in ws.iter_rows(values_only=True)]
            if vals and vals[0]:
                h = vals[0]
                tables.append((ws.title, h, [dict(zip(h, x)) for x in vals[1:] if any(c.strip() for c in x)]))
    return tables


def kind(header):
    h = " ".join(header)
    if "T1." in h and ("S15." in h or "Sentence suggested" in h):
        return "teachers"
    if "B13." in h or "U10." in h:
        return "after"
    if "A1." in h or "A6." in h:
        return "before"
    return None


def code_of(row, header, teacher=False):
    for col in header:
        c = col.lower()
        if teacher and c.startswith("teacher code"):
            return norm(row.get(col))
        if not teacher and c.startswith("participant code"):
            return norm(row.get(col))
    return ""


def norm(c):
    return re.sub(r"\s+", "", str(c or "")).upper()


def item_cols(header):
    """item code -> column, e.g. 'B3' -> 'My child… [B3. asks for ...]'. Grid columns hold the code in [ ]."""
    out = OrderedDict()
    for col in header:
        m = re.search(r"\[([A-Z]{1,2}\d{1,2})\.", col) or re.match(r"\s*([A-Z]{1,2}\d{1,2})\.", col)
        if m:
            key = m.group(1)
            # SUS items and the teachers' example sentences are both S1..; tell them apart by the grid title
            if key.startswith("S") and ("Sentence suggested" in col or "→" in col or "->" in col):
                key = "X" + key[1:]
            out.setdefault(key, col)
    return out


def num(v):
    """'4 – Often' -> 4; '5' -> 5; '' -> None."""
    m = re.match(r"\s*(\d)", str(v or ""))
    return int(m.group(1)) if m else None


def sus_score(row, cols):
    vals = [num(row.get(cols.get(f"S{i}"))) for i in range(1, 11)]
    if any(v is None for v in vals):
        return None
    return 2.5 * sum((v - 1) if i % 2 == 0 else (5 - v) for i, v in enumerate(vals))


def med(xs):
    xs = [x for x in xs if x is not None]
    return statistics.median(xs) if xs else None


def pct(xs, test):
    xs = [x for x in xs if x is not None]
    return round(100 * sum(1 for x in xs if test(x)) / len(xs)) if xs else None


def f1(x):
    return "--" if x is None else (f"{x:.1f}" if isinstance(x, float) and x != int(x) else str(int(x)) if isinstance(x, (int, float)) else str(x))


def tex(t):
    return str(t).replace("&", r"\&").replace("%", r"\%").replace("_", r"\_").replace("#", r"\#")


def short(col):
    m = re.search(r"\[(.*)\]\s*$", col)
    return (m.group(1) if m else col).strip()


# ----------------------------------------------------------------------------- analysis

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("input")
    ap.add_argument("--out")
    a = ap.parse_args()
    tables = read_tables(a.input)
    forms = defaultdict(list)
    for name, header, rows in tables:
        k = kind(header)
        if k:
            forms[k].append((header, rows))
        else:
            print(f"skipped tab/file '{name}' (not recognised as one of the three questionnaires)")
    if not forms:
        sys.exit("no questionnaire responses recognised")
    out = a.out or os.path.join(os.path.dirname(os.path.abspath(a.input)), "questionnaire_results")
    os.makedirs(out, exist_ok=True)

    md, opens = ["# Eraka questionnaires - summary", ""], ["# Open answers (for the thematic analysis)", ""]
    data = {}
    for k, lst in forms.items():
        header = lst[0][0]
        rows = [r for _, rs in lst for r in rs]
        teacher = k == "teachers"
        keep = []
        for r in rows:
            c = code_of(r, header, teacher)
            if c.startswith("TEST"):
                continue
            r["__code"] = c or f"(no code {len(keep) + 1})"
            keep.append(r)
        # the newest answer per code wins (a parent may submit twice)
        by = OrderedDict()
        for r in keep:
            by[r["__code"]] = r
        data[k] = (header, item_cols(header), list(by.values()))
        md.append(f"- {k}: {len(by)} respondents ({', '.join(by)})" + (f"; {len(keep) - len(by)} duplicate answers dropped" if len(keep) > len(by) else ""))
    md.append("")

    per = defaultdict(dict)

    # ---- SUS
    sus_lines = ["% Rows: SUS per respondent (eraka_questionnaire_analysis.py). Columns: group & code & SUS (0-100) \\\\"]
    md += ["## System Usability Scale (0-100; 68 is the usual average)", ""]
    for k, label in (("after", "Parent"), ("teachers", "Teacher")):
        if k not in data:
            continue
        header, cols, rows = data[k]
        scores = []
        for r in rows:
            s = sus_score(r, cols)
            scores.append(s)
            per[r["__code"]][f"sus_{k}"] = s
            sus_lines.append(f"{label} & {tex(r['__code'])} & {f1(s)} \\\\")
        valid = [s for s in scores if s is not None]
        if valid:
            md.append(f"- {label}s: n = {len(valid)}, median {f1(statistics.median(valid))}, "
                      f"mean {statistics.mean(valid):.1f}, range {f1(min(valid))}-{f1(max(valid))}")
            sus_lines.append(f"\\hline {label}s, median (range) & & {f1(statistics.median(valid))} ({f1(min(valid))}--{f1(max(valid))}) \\\\")
    md.append("")
    open(os.path.join(out, "sus_table.tex"), "w", encoding="utf-8").write("\n".join(sus_lines) + "\n")

    # ---- communication checklist, before vs after
    md += ["## Communication checklist (1 Never ... 5 Always; B11 = frustration, lower is better)", ""]
    chk = ["% Rows: checklist per child, before -> after. Columns: child & B1..B10 total before & after & change"
           " & B11 before & after & B12 before & after & B13 \\\\"]
    if "before" in data and "after" in data:
        hb, cb, rb = data["before"]
        ha, ca, ra = data["after"]
        before = {r["__code"]: r for r in rb}
        after = {r["__code"]: r for r in ra}
        both = [c for c in after if c in before]
        md.append(f"Children with both questionnaires: {len(both)} ({', '.join(both)}); "
                  f"before only: {', '.join(c for c in before if c not in after) or 'none'}; "
                  f"after only: {', '.join(c for c in after if c not in before) or 'none'}")
        md.append("")
        item_changes = defaultdict(list)
        for c in both:
            b, af = before[c], after[c]
            tb = [num(b.get(cb.get(f"B{i}"))) for i in range(1, 11)]
            ta = [num(af.get(ca.get(f"B{i}"))) for i in range(1, 11)]
            sb = sum(x for x in tb if x is not None) if all(x is not None for x in tb) else None
            sa = sum(x for x in ta if x is not None) if all(x is not None for x in ta) else None
            for i in range(1, 12):
                x, y = num(b.get(cb.get(f"B{i}"))), num(af.get(ca.get(f"B{i}")))
                if x is not None and y is not None:
                    item_changes[f"B{i}"].append(y - x)
            b11 = (num(b.get(cb.get("B11"))), num(af.get(ca.get("B11"))))
            b12 = (EASE5.get(str(b.get(cb.get("B12"), "")).strip().lower()),
                   EASE5.get(str(af.get(ca.get("B12"), "")).strip().lower()))
            b13 = af.get(ca.get("B13"), "")
            per[c].update({"checklist_before": sb, "checklist_after": sa,
                           "checklist_change": (sa - sb) if sa is not None and sb is not None else None,
                           "b11_before": b11[0], "b11_after": b11[1], "b12_before": b12[0], "b12_after": b12[1],
                           "b13": b13})
            ch = (sa - sb) if sa is not None and sb is not None else None
            md.append(f"- {c}: B1-B10 total {f1(sb)} -> {f1(sa)} (change {'+' if ch and ch > 0 else ''}{f1(ch)}); "
                      f"B11 {f1(b11[0])} -> {f1(b11[1])}; B12 {f1(b12[0])} -> {f1(b12[1])}; B13 '{b13}'")
            chk.append(f"{tex(c)} & {f1(sb)} & {f1(sa)} & {('+' if ch and ch > 0 else '') + f1(ch)} & "
                       f"{f1(b11[0])} & {f1(b11[1])} & {f1(b12[0])} & {f1(b12[1])} & {tex(b13)} \\\\")
        md += ["", "Change per item (after - before), median over children:"]
        for i in range(1, 12):
            ch = item_changes.get(f"B{i}", [])
            if ch:
                md.append(f"- B{i}: median {f1(statistics.median(ch))}; better {sum(1 for x in ch if (x > 0 if i < 11 else x < 0))}, "
                          f"same {sum(1 for x in ch if x == 0)}, worse {sum(1 for x in ch if (x < 0 if i < 11 else x > 0))}")
    else:
        md.append("(needs both parent questionnaires)")
    md.append("")
    open(os.path.join(out, "checklist_table.tex"), "w", encoding="utf-8").write("\n".join(chk) + "\n")

    # rows for Table tab:resusability: family & SUS & checklist before & after & change
    us = ["% Rows for Table tab:resusability (eraka_questionnaire_analysis.py). Checklist = B1-B10 total (10-50).",
          "% Columns: family & SUS (0-100) & checklist before & checklist after & change \\\\"]
    for c, v in per.items():
        if "checklist_before" in v or "sus_after" in v:
            ch = v.get("checklist_change")
            us.append(f"{tex(c)} & {f1(v.get('sus_after'))} & {f1(v.get('checklist_before'))} & "
                      f"{f1(v.get('checklist_after'))} & {('+' if ch and ch > 0 else '') + f1(ch)} \\\\")
    open(os.path.join(out, "usability_table.tex"), "w", encoding="utf-8").write("\n".join(us) + "\n")

    # ---- culture / agreement items (parents F1-F12, G1-G4; teachers F1-F8, O1-O5)
    cul = ["% Rows: agreement items. Columns: group & item & n & median & % agree (4-5) \\\\"]
    md += ["## Agreement items (1 Strongly disagree ... 5 Strongly agree)", ""]
    for k, label, codes in (("after", "Parents", [f"F{i}" for i in range(1, 13)] + [f"G{i}" for i in range(1, 5)]),
                            ("teachers", "Teachers", [f"F{i}" for i in range(1, 9)])):
        if k not in data:
            continue
        header, cols, rows = data[k]
        for it in codes:
            col = cols.get(it)
            if not col:
                continue
            xs = [num(r.get(col)) for r in rows]
            n = len([x for x in xs if x is not None])
            if not n:
                continue
            m, p = med(xs), pct(xs, lambda x: x >= 4)
            md.append(f"- {label} {short(col)}: n = {n}, median {f1(m)}, {p}% agree")
            cul.append(f"{label} & {tex(short(col))} & {n} & {f1(m)} & {p}\\% \\\\")
    # any other grid with numeric answers (e.g. the cultural-representation importance grid added by hand)
    known = set()
    for k in data:
        known |= set(data[k][1].values())
    for k in ("before", "after", "teachers"):
        if k not in data:
            continue
        header, cols, rows = data[k]
        for col in header:
            if col in known or "[" not in col:
                continue
            xs = [num(r.get(col)) for r in rows]
            if len([x for x in xs if x is not None]) >= max(1, len(rows) // 2):
                md.append(f"- ({k}) {col}: median {f1(med(xs))}, {pct(xs, lambda x: x >= 4)}% rated 4-5")
                cul.append(f"{k.capitalize()} & {tex(short(col))} & {len([x for x in xs if x is not None])} & "
                           f"{f1(med(xs))} & {pct(xs, lambda x: x >= 4)}\\% \\\\")
    md.append("")
    open(os.path.join(out, "culture_table.tex"), "w", encoding="utf-8").write("\n".join(cul) + "\n")

    # ---- teachers' sentence ratings
    if "teachers" in data:
        header, cols, rows = data["teachers"]
        st = ["% Rows: example sentences. Columns: sentence & correct and natural & correct, not natural & small error & wrong \\\\"]
        md += ["## Teachers' rating of the example sentences", ""]
        total = Counter()
        for i in range(1, 16):
            col = cols.get(f"X{i}")
            if not col:
                continue
            c = Counter(str(r.get(col, "")).strip() for r in rows if str(r.get(col, "")).strip())
            total.update(c)
            md.append(f"- {short(col)}: " + ", ".join(f"{s}: {c.get(s, 0)}" for s in SENT))
            st.append(f"{tex(short(col))} & " + " & ".join(str(c.get(s, 0)) for s in SENT) + r" \\")
        n = sum(total.values())
        if n:
            md.append(f"- All sentences: {total.get(SENT[0], 0)} of {n} ratings correct and natural "
                      f"({100 * total.get(SENT[0], 0) / n:.0f}%), {100 * (total.get(SENT[0], 0) + total.get(SENT[1], 0)) / n:.0f}% correct")
        md.append("")
        open(os.path.join(out, "sentences_table.tex"), "w", encoding="utf-8").write("\n".join(st) + "\n")

    # ---- choice items: counts (U, A, C, D, E, T, H5-H7 ...)
    md += ["## Multiple-choice and tick-box answers (counts)", ""]
    for k in ("before", "after", "teachers"):
        if k not in data:
            continue
        header, cols, rows = data[k]
        for it, col in cols.items():
            if "[" in col:
                continue
            vals = [str(r.get(col, "")).strip() for r in rows]
            if not any(vals):
                continue
            avg_len = statistics.mean(len(v) for v in vals if v) if any(vals) else 0
            if avg_len > 60:        # open answer
                continue
            parts = Counter(p.strip() for v in vals for p in v.split(", ") if p.strip())
            md.append(f"- ({k}) {col}: " + "; ".join(f"{p} {n}" for p, n in parts.most_common()))
    md.append("")

    # ---- open answers
    for k in ("before", "after", "teachers"):
        if k not in data:
            continue
        header, cols, rows = data[k]
        for col in header:
            vals = [(r["__code"], str(r.get(col, "")).strip()) for r in rows]
            vals = [(c, v) for c, v in vals if v]
            if not vals or "[" in col:
                continue
            if statistics.mean(len(v) for _, v in vals) <= 25 and not re.search(r"\b(C5|E3|F13|F14|G5|H1|H2|H3|H4|H6|H8|O6|F9|S16|U7)\.", col):
                continue
            opens += [f"## ({k}) {col}", ""] + [f"- **{c}**: {v}" for c, v in vals] + [""]
    open(os.path.join(out, "open_answers.md"), "w", encoding="utf-8").write("\n".join(opens) + "\n")
    open(os.path.join(out, "questionnaire_summary.md"), "w", encoding="utf-8").write("\n".join(md) + "\n")

    keys = sorted({k for v in per.values() for k in v})
    with open(os.path.join(out, "per_respondent.csv"), "w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["code"] + keys)
        for c, v in per.items():
            w.writerow([c] + [v.get(k, "") for k in keys])
    print(f"results -> {out}")


if __name__ == "__main__":
    main()
