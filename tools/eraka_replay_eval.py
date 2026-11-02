#!/usr/bin/env python3
"""
Eraka step 4: chronological replay evaluation of next-picture prediction.

    python tools/eraka_replay_eval.py <folder with tap_events + message_events CSVs> [--warmup 20]
    python tools/eraka_replay_eval.py --selftest

Method (what the thesis reports):
  1. Read the child's logged taps and message events (app export ',' or Database
     Inspector export ';'). Mark taps the child undid (same rule as ErakaUsageLog.loadTaps).
  2. Walk through the taps in time order. Before each real (not undone) tap, every model
     ranks the pictures for that moment (previous two pictures, hour, place), the
     pictures already in the message are left out (as in the app), and we record the
     rank of the picture the child actually chose. Then every model learns that tap.
     So each prediction only uses the past - like the child using the app.
  3. Metrics: top-1, top-4 (the 4 tiles), top-6 (the suggestion row), MRR
     (mean reciprocal rank; 1/rank, 0 if not ranked).

Models compared (all start from zero, learn online):
  most_used       how often each picture was used (unigram counts)
  bigram          count of picture after the previous picture (ties: most used)
  stupid_backoff  trigram -> 0.4 x bigram -> 0.16 x unigram (Brants et al. 2007)
  speakbright     rebuild of SpeakBright's recommender (Reroma et al. 2026): taps of the
                  last 14 days within +/-30 min of the current time of day, sessions split
                  at gaps > 10 min, pictures that follow the current picture in a session
                  ranked by support (number of sessions with the pattern <current, next>)
  trigram_plain   Eraka formula without recency, time or place
  eraka_no_ctx    Eraka without time and place          (ablation)
  eraka_no_rec    Eraka without recency weighting       (ablation)
  eraka           full Eraka model (as in the app)
  eraka_base      Eraka + base model, if assets/eraka_base/base_messages.csv exists
  eduardo         NextPicturePredictor.kt from eraka-demo-dev v0.4 (E. Nacimiento-Garcia),
                  ported as written: weights 0.55/0.30/0.10 + 0.25 x OTTAA prior, category
                  boosts for place/time, last-use recency; context kept inside the model
                  across messages; undone taps are learned (as in the demo app)
  eduardo_fixed   the same formula, but context taken from the message (resets at each
                  message) and undone taps ignored - separates the model from the app bugs
  eduardo_noprior eduardo_fixed without the OTTAA prior - the n-gram part alone
  eraka_ottaa_prior  Eraka + the same OTTAA prior (test only: like-for-like cold start)
  ottaa           approximation of OTTAA's ranking: starter relation counts from
                  pictos_en.txt + 1 per tap (undone taps included, as in OTTAA),
                  only the previous picture's related pictures, score 2 x count
                  (no time/place tags exist in the starter vocabulary)
"""
import argparse
import csv
import glob
import json
import math
import os
import sys
from collections import defaultdict

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from eraka_predictor import Config, ErakaPredictor, START  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VOCAB = os.path.join(ROOT, "app/src/main/assets/eraka_seed/Archivos_Paises/pictos/pictos_en.txt")
BASE = os.path.join(ROOT, "app/src/main/assets/eraka_base/base_messages.csv")
# Eduardo's demo (eraka-demo-dev) assets, if the repo is next to the thesis files
_DEMO_SUB = os.path.join("eraka-demo-dev", "eraka-demo-dev-main", "android", "matcher", "src", "main", "assets")
_DEMO_TRY = [os.environ.get("ERAKA_DEMO_ASSETS", ""),
             os.path.join(os.path.expanduser("~"), "Documents", "Eraka", _DEMO_SUB),          # Windows / Mac
             os.path.join(os.path.expanduser("~"), "mnt", "Documents--Eraka", _DEMO_SUB),    # Claude's shell
             os.path.join(os.path.expanduser("~"), "mnt", "Eraka", _DEMO_SUB)]
DEMO_ASSETS = next((p for p in _DEMO_TRY if p and os.path.exists(os.path.join(p, "roles.json"))), _DEMO_TRY[1])


# ----------------------------------------------------------------------------- reading

def read_rows(path):
    with open(path, encoding="utf-8-sig", newline="") as f:
        head = f.readline()
        f.seek(0)
        delim = ";" if head.count(";") > head.count(",") else ","
        return list(csv.DictReader(f, delimiter=delim))


def load_log(folder):
    taps_f = sorted(glob.glob(os.path.join(folder, "*tap_events*.csv")))
    msgs_f = sorted(glob.glob(os.path.join(folder, "*message_events*.csv")))
    if not taps_f:
        sys.exit(f"no *tap_events*.csv in {folder}")
    taps = [r for f in taps_f for r in read_rows(f)]
    msgs = [r for f in msgs_f for r in read_rows(f)]
    for r in taps:
        for k in ("id", "ts", "session_id", "position", "picto_id", "prev1_id", "prev2_id", "hour"):
            r[k] = int(float(r[k]))
        r["place"] = (r.get("place") or "").strip()
    for r in msgs:
        r["ts"], r["session_id"], r["length"] = int(float(r["ts"])), int(float(r["session_id"])), int(float(r["length"]))
    taps.sort(key=lambda r: (r["ts"], r["id"]))
    mark_undone(taps, msgs)
    mark_repeats(taps)
    return taps, msgs


def mark_repeats(taps):
    """Same rule as ErakaPredictor.findRepeats(): a tap is a repeat when its picture is
    already in the message on screen (after cutting the message back to the tap's
    position, which removes what an undo took away). Repeats are logged but not learned."""
    on_screen = defaultdict(list)
    for r in taps:
        msg = on_screen[r["session_id"]]
        del msg[r["position"]:]
        r["repeat"] = r["picto_id"] in msg
        msg.append(r["picto_id"])


def mark_undone(taps, msgs):
    """Same rule as ErakaUsageLog.loadTaps(): an undo event lists the message before the
    last picture was removed; the removed tap is the latest one in that session at
    position length-1 with that picture, made before the undo."""
    undone = set()
    for m in msgs:
        if m["type"] != "undo":
            continue
        ids = (m.get("picto_ids") or "").split()
        if m["length"] < 1 or not ids:
            continue
        removed, pos = int(ids[-1]), m["length"] - 1
        best = None
        for r in taps:
            if (r["session_id"] == m["session_id"] and r["position"] == pos and r["picto_id"] == removed
                    and r["ts"] <= m["ts"] and (best is None or r["ts"] > best["ts"])):
                best = r
        if best:
            undone.add(best["id"])
    for r in taps:
        r["undone"] = r["id"] in undone


# ----------------------------------------------------------------------------- models

class Model:
    name = "?"

    def scores(self, ctx):  # pid -> score (higher = better); only pictures with score > 0
        raise NotImplementedError

    def learn(self, tap):
        raise NotImplementedError

    def learns_undone(self):
        return False


class ErakaModel(Model):
    def __init__(self, name, base=None, **cfg):
        self.name, self.m = name, ErakaPredictor(Config(**cfg))
        self.m.base = base

    def scores(self, c):
        return self.m.probabilities(c["prev1_id"], c["prev2_id"], c["hour"], c["place"])

    def learn(self, t):
        self.m.add_tap(t["picto_id"], t["prev1_id"], t["prev2_id"], t["ts"], t["hour"], t["place"])


class MostUsed(Model):
    name = "most_used"

    def __init__(self):
        self.uni = defaultdict(float)

    def scores(self, c):
        return dict(self.uni)

    def learn(self, t):
        self.uni[t["picto_id"]] += 1


class Bigram(MostUsed):
    name = "bigram"

    def __init__(self):
        super().__init__()
        self.bi = defaultdict(lambda: defaultdict(float))

    def scores(self, c):
        big = self.bi.get(c["prev1_id"], {})
        return {k: big.get(k, 0) * 1e6 + u for k, u in self.uni.items()}  # bigram first, ties by use

    def learn(self, t):
        super().learn(t)
        self.bi[t["prev1_id"]][t["picto_id"]] += 1


class StupidBackoff(Bigram):
    name = "stupid_backoff"

    def __init__(self, alpha=0.4):
        super().__init__()
        self.alpha = alpha
        self.tri = defaultdict(lambda: defaultdict(float))

    def scores(self, c):
        tri = self.tri.get((c["prev2_id"], c["prev1_id"]), {})
        big = self.bi.get(c["prev1_id"], {})
        tt, bt, ut = sum(tri.values()), sum(big.values()), sum(self.uni.values())
        out = {}
        for k, u in self.uni.items():
            if tri.get(k):
                out[k] = tri[k] / tt
            elif big.get(k):
                out[k] = self.alpha * big[k] / bt
            else:
                out[k] = self.alpha * self.alpha * u / ut
        return out

    def learn(self, t):
        super().learn(t)
        self.tri[(t["prev2_id"], t["prev1_id"])][t["picto_id"]] += 1


class SpeakBright(Model):
    """Reconstruction of SpeakBright's Temporal PrefixSpan recommender (Reroma et al., 2026).

    From the paper: sessions = taps with < 10 min gaps; patterns mined over the last
    14 days; temporal filter +/-30 min of the current time; suggestions = suffixes that
    follow the current item in frequent sequential patterns, by frequency.
    Not stated in the paper, so chosen here (report in the thesis): the current item is
    the last picture in the message; a pattern <current, x> counts once per session
    (sequential-pattern support) and x may come anywhere later in that session;
    minimum support 1 (small data); ties and the start of a message use how many
    sessions in the window contain x.
    """
    name = "speakbright"
    DAY_MS = 24 * 3600 * 1000

    def __init__(self, days=14, window_min=30, gap_min=10):
        self.days, self.window, self.gap_ms = days, window_min, gap_min * 60 * 1000
        self.past = []  # (ts, minute of day, picto)

    @staticmethod
    def minute_of_day(t):
        return t["hour"] * 60 + (t["ts"] // 60000) % 60  # local hour from the log + minutes

    def scores(self, c):
        now, mod = c["ts"], self.minute_of_day(c)
        recent = [(ts, p) for ts, m, p in self.past
                  if now - ts <= self.days * self.DAY_MS
                  and min(abs(m - mod), 1440 - abs(m - mod)) <= self.window]
        sessions, last = [], None
        for ts, p in recent:  # already in time order
            if last is None or ts - last > self.gap_ms:
                sessions.append([])
            sessions[-1].append(p)
            last = ts
        single, follow = defaultdict(int), defaultdict(int)
        cur = c["prev1_id"]
        for seq in sessions:
            for p in set(seq):
                single[p] += 1
            if cur != START and cur in seq:
                after = set(seq[seq.index(cur) + 1:])
                for p in after:
                    follow[p] += 1
        return {p: follow.get(p, 0) * 1e6 + n for p, n in single.items()}

    def learn(self, t):
        self.past.append((t["ts"], self.minute_of_day(t), t["picto_id"]))


class EduardoPredictor(Model):
    """Port of eraka.learn.NextPicturePredictor (eraka-demo-dev v0.4)."""

    def __init__(self, name="eduardo", faithful=True, assets=DEMO_ASSETS, use_prior=True):
        self.name, self.faithful = name, faithful
        self.w2, self.w1, self.w0, self.wp = 0.55, 0.30, 0.10, (0.25 if use_prior else 0.0)
        self.c2, self.c1, self.c0 = defaultdict(lambda: defaultdict(int)), defaultdict(lambda: defaultdict(int)), defaultdict(int)
        self.last_used = {}
        self.prev1 = self.prev2 = None
        self.groups, self.priors = {}, {}
        roles = os.path.join(assets, "roles.json")
        if os.path.exists(roles):
            self.groups = {int(k): v.get("group", "") for k, v in json.load(open(roles, encoding="utf-8")).items()}
            for k, v in json.load(open(os.path.join(assets, "priors.json"), encoding="utf-8")).items():
                self.priors[int(k)] = {int(a): float(b) for a, b in v}
        self.universe = sorted(self.groups)

    def learns_undone(self):
        return self.faithful

    @staticmethod
    def place(p):
        p = (p or "").lower()
        return "outdoors" if p in ("park", "zoo") else p

    def _boost(self, pid, place, hour):
        g, b = self.groups.get(pid, ""), 1.0
        if place == "home" and g in ("House", "Food"): b *= 1.4
        elif place == "school" and g in ("School", "Social interactions"): b *= 1.4
        elif place == "outdoors" and g in ("Places", "Nature", "Transport"): b *= 1.3
        if 7 <= hour <= 9 and g == "Food": b *= 1.3
        elif 11 <= hour <= 14 and g == "Food": b *= 1.5
        elif 18 <= hour <= 21 and g == "House": b *= 1.3
        return b

    def scores(self, c):
        if self.faithful:
            a, b = self.prev1, self.prev2
        else:
            a = None if c["prev1_id"] == START else c["prev1_id"]
            b = None if c["prev2_id"] == START else c["prev2_id"]
        n2 = self.c2.get((b, a)) if a is not None and b is not None else None
        n1 = self.c1.get(a) if a is not None else None
        t2, t1, t0 = (sum(n2.values()) if n2 else 0), (sum(n1.values()) if n1 else 0), sum(self.c0.values())
        pri = self.priors.get(a, {}) if a is not None else {}
        place, hour, now = self.place(c["place"]), c["hour"], c["ts"]
        out = {}
        for pid in set(self.universe) | set(self.c0):
            base = (self.w2 * (n2.get(pid, 0) / t2 if t2 else 0) + self.w1 * (n1.get(pid, 0) / t1 if t1 else 0)
                    + self.w0 * (self.c0.get(pid, 0) / t0 if t0 else 0) + self.wp * pri.get(pid, 0))
            if base <= 0:
                continue
            last = self.last_used.get(pid)
            rec = 1.0 if last is None else 1.0 + 0.6 / (1.0 + max(0, now - last) / 3_600_000 / 6.0)
            out[pid] = base * self._boost(pid, place, hour) * rec
        return out

    def learn(self, t):
        pid = t["picto_id"]
        if self.faithful:
            a, b = self.prev1, self.prev2
        else:
            a = None if t["prev1_id"] == START else t["prev1_id"]
            b = None if t["prev2_id"] == START else t["prev2_id"]
        if a is not None and b is not None:
            self.c2[(b, a)][pid] += 1
        if a is not None:
            self.c1[a][pid] += 1
        self.c0[pid] += 1
        self.last_used[pid] = t["ts"]
        self.prev2, self.prev1 = a, pid


class ErakaWithPrior(ErakaModel):
    """Eraka mixed with Eduardo's OTTAA co-occurrence prior as a stand-in base model:
    (1-b) P_eraka + b P_prior(w | prev1), b = K/(K+n). Test only: shows how much of the
    demo's cold-start gain comes from the prior (the app will use the Namibian base model)."""

    def __init__(self, name, priors, k=30.0):
        super().__init__(name)
        self.priors, self.k = priors, k

    def scores(self, c):
        own = super().scores(c)
        pri = self.priors.get(c["prev1_id"], {}) if c["prev1_id"] != START else {}
        tot = sum(pri.values())
        if not tot:
            return own
        b = self.k / (self.k + self.m.taps_used)
        out = {k: (1 - b) * v for k, v in own.items()}
        for k, v in pri.items():
            out[k] = out.get(k, 0.0) + b * v / tot
        return out


class OttaaApprox(Model):
    name = "ottaa"

    def __init__(self, vocab_path=VOCAB):
        self.follow = defaultdict(dict)  # parent -> {child: frec}, insertion order = stored order
        try:
            for p in json.load(open(vocab_path, encoding="utf-8")):
                for r in p.get("relacion", []):
                    self.follow[p["id"]][r["id"]] = float(r.get("frec", 0))
        except OSError:
            pass

    def learns_undone(self):
        return True  # OTTAA counts every tap

    @staticmethod
    def parent(prev1):
        return 0 if prev1 == START else prev1  # picto 0 holds the start-of-message list

    def scores(self, c):
        rel = self.follow.get(self.parent(c["prev1_id"]), {})
        n = len(rel)
        # 2 x count; tiny bonus keeps the stored order for ties
        return {k: 2 * f + (n - i) * 1e-9 + 1e-12 for i, (k, f) in enumerate(rel.items())}

    def learn(self, t):
        rel = self.follow[self.parent(t["prev1_id"])]
        rel[t["picto_id"]] = rel.get(t["picto_id"], 0.0) + 1


def load_base():
    if not os.path.exists(BASE):
        return None
    base = ErakaPredictor(Config(half_life_days=math.inf))
    for line in open(BASE, encoding="utf-8"):
        line = line.strip()
        if not line or line.startswith("#") or line.startswith("source,"):
            continue
        f = line.split(",", 4)
        hour = {"MORNING": 8, "MIDDAY": 13, "AFTERNOON": 17, "EVENING": 20}.get(f[1].strip().upper(), -1)
        p1 = p2 = START
        for x in f[3].split():
            base.add_tap(int(x), p1, p2, 0, hour, f[2].strip().upper())
            p2, p1 = p1, int(x)
    return base if base.taps_used else None


def all_models():
    ms = [MostUsed(), Bigram(), StupidBackoff(), SpeakBright(),
          ErakaModel("trigram_plain", half_life_days=math.inf, mu_time=0, mu_place=0),
          ErakaModel("eraka_no_ctx", mu_time=0, mu_place=0),
          ErakaModel("eraka_no_rec", half_life_days=math.inf),
          ErakaModel("eraka")]
    base = load_base()
    if base:
        ms.append(ErakaModel("eraka_base", base=base))
    if os.path.exists(os.path.join(DEMO_ASSETS, "roles.json")):
        ms += [EduardoPredictor("eduardo", True), EduardoPredictor("eduardo_fixed", False),
               EduardoPredictor("eduardo_noprior", False, use_prior=False)]
        ms.append(ErakaWithPrior("eraka_ottaa_prior", EduardoPredictor().priors))
    else:
        print(f"note: prototype predictor skipped - demo assets not found (set ERAKA_DEMO_ASSETS); tried {DEMO_ASSETS}",
              file=sys.stderr)
    ms.append(OttaaApprox())
    return ms


# ----------------------------------------------------------------------------- replay

def rank_of(scores, target, exclude):
    s = scores.get(target, 0)
    if s <= 0 or target in exclude:
        return 0
    better = sum(1 for k, v in scores.items() if k not in exclude and (v > s or (v == s and k < target)))
    return better + 1


def replay(taps, models):
    rows = []
    message = defaultdict(list)  # session -> pictures currently in the message
    for t in taps:
        sess = message[t["session_id"]]
        del sess[t["position"]:]  # after an undo the message is shorter again
        skip = t["undone"] or t.get("repeat", False)
        if not skip:
            exclude = set(sess)
            row = {"id": t["id"], "session": t["session_id"], "position": t["position"], "picto": t["picto_id"],
                   "model_live": t.get("model", ""), "rank_board_live": t.get("rank_board", ""),
                   "rank_smart_live": t.get("rank_smart", "")}
            for m in models:
                row[m.name] = rank_of(m.scores(t), t["picto_id"], exclude)
            rows.append(row)
        for m in models:
            if not skip or m.learns_undone():
                m.learn(t)
        sess.append(t["picto_id"])
    return rows


def summarise(rows, names, skip=0):
    rows = rows[skip:]
    out = {}
    for n in names:
        r = [x[n] for x in rows]
        k = len(r) or 1
        out[n] = {"n": len(r),
                  "top1": sum(1 for v in r if v == 1) / k,
                  "top4": sum(1 for v in r if 1 <= v <= 4) / k,
                  "top6": sum(1 for v in r if 1 <= v <= 6) / k,
                  "mrr": sum(1 / v for v in r if v >= 1) / k}
    return out


def table(summary, title):
    lines = [f"### {title}", "", "| model | taps | top-1 | top-4 | top-6 | MRR |", "|---|---|---|---|---|---|"]
    for n, s in summary.items():
        lines.append(f"| {n} | {s['n']} | {s['top1']:.1%} | {s['top4']:.1%} | {s['top6']:.1%} | {s['mrr']:.3f} |")
    return "\n".join(lines) + "\n"


def live_table(taps):
    """What the child actually saw: rank of the tapped picture on the 4 tiles, per active model."""
    lines = ["### Live (logged in the app, not replayed)", "",
             "| active model | taps | tapped picture was tile 1 | in the 4 tiles | in the suggestion row |",
             "|---|---|---|---|---|"]
    for model in sorted({t.get("model", "") for t in taps}):
        ts = [t for t in taps if t.get("model", "") == model and not t["undone"]]
        if not ts:
            continue
        k = len(ts)
        rb = [int(float(t.get("rank_board") or 0)) for t in ts]
        rs = [int(float(t.get("rank_smart") or 0)) for t in ts]
        lines.append(f"| {model or '?'} | {k} | {sum(1 for v in rb if v == 1) / k:.1%} | "
                     f"{sum(1 for v in rb if 1 <= v <= 4) / k:.1%} | {sum(1 for v in rs if v >= 1) / k:.1%} |")
    return "\n".join(lines) + "\n"


# ----------------------------------------------------------------------------- self-test

def selftest():
    """Must reproduce the Java unit tests (ErakaPredictorTest) exactly."""
    now, ok = 1791303768000, True
    m = ErakaPredictor()
    for i, (a, b, c) in enumerate([(1, 2, 3), (1, 2, 4), (1, 2, 3)]):
        ts = now + i * 60000
        for j, (p, p1, p2) in enumerate([(a, START, START), (b, a, START), (c, b, a)]):
            m.add_tap(p, p1, p2, ts + j * 1000, 17, "HOME")
    pr = m.probabilities(2, 1, 17, "HOME")
    print(f"water {pr[3]:.3f} (Java 0.507), juice {pr[4]:.3f} (Java 0.253)")
    ok &= round(pr[3], 3) == 0.507 and round(pr[4], 3) == 0.253
    m = ErakaPredictor()
    for k, msg in enumerate([[643, 44, 118, 482], [628, 14, 427], [377, 378]]):
        ts = now + [0, 20000, 35000][k]
        for i, p in enumerate(msg):
            m.add_tap(p, msg[i - 1] if i >= 1 else START, msg[i - 2] if i >= 2 else START, ts + i * 1000, 17, "HOME")
    m.add_tap(44, START, START, now + 45000, 17, "HOME")
    m.add_tap(30, 44, START, now + 48000, 17, "HOME", undone=True)
    m.add_tap(662, 30, 44, now + 50000, 17, "HOME", undone=True)
    pr = m.probabilities(44, 643, 17, "HOME")
    top = max(pr, key=pr.get)
    print(f"real log: top {top} P {pr[top]:.3f} (Java 118, 0.676), taps used {m.taps_used} (Java 10)")
    ok &= top == 118 and round(pr[top], 3) == 0.676 and m.taps_used == 10
    ok &= replay_known_answers()
    print("SELFTEST", "OK" if ok else "FAILED")
    return ok


def replay_known_answers():
    """A tiny made-up log whose correct ranks can be worked out by hand.

    msg 1: I want water   msg 2: I want water   msg 3: I want eat
    msg 4: I want giraffe -> giraffe undone, then water
    Expected (eraka): msg 2 water = rank 1 (seen once before after 'I want');
    msg 3 eat = 0 (never seen: first use, nobody can predict it);
    msg 4 water = rank 1 (2x water vs 1x eat); the undone giraffe is neither
    predicted nor learned; 'I' and 'want' are never ranked while already in the message.
    """
    import tempfile
    I, WANT, WATER, EAT, GIRAFFE = 643, 44, 500, 118, 999
    t0, taps, msgs, tid = 1791394000000, [], [], 0

    def tap(sess, pos, pid, p1, p2, ts):
        nonlocal tid
        tid += 1
        taps.append(dict(id=tid, ts=ts, session_id=sess, position=pos, picto_id=pid, prev1_id=p1, prev2_id=p2,
                         source="board", place="HOME", hour=18, weekday=4, shown_board="", shown_smart="",
                         rank_board=0, rank_smart=0, model="eraka"))

    for sess, last in [(1, WATER), (2, WATER), (3, EAT)]:
        ts = t0 + sess * 60000
        tap(sess, 0, I, -1, -1, ts); tap(sess, 1, WANT, I, -1, ts + 1000); tap(sess, 2, last, WANT, I, ts + 2000)
        msgs.append(dict(id=sess, ts=ts + 3000, session_id=sess, type="spoken", length=3,
                         picto_ids=f"{I} {WANT} {last}", place="HOME", hour=18))
    ts = t0 + 4 * 60000
    tap(4, 0, I, -1, -1, ts); tap(4, 1, WANT, I, -1, ts + 1000); tap(4, 2, GIRAFFE, WANT, I, ts + 2000)
    msgs.append(dict(id=4, ts=ts + 3000, session_id=4, type="undo", length=3, picto_ids=f"{I} {WANT} {GIRAFFE}",
                     place="HOME", hour=18))
    tap(4, 2, WATER, WANT, I, ts + 4000)

    d = tempfile.mkdtemp()
    for name, rows in (("x_tap_events.csv", taps), ("x_message_events.csv", msgs)):
        with open(os.path.join(d, name), "w", newline="", encoding="utf-8") as f:
            w = csv.DictWriter(f, fieldnames=list(rows[0].keys()), delimiter=";")
            w.writeheader()
            w.writerows(rows)
    log, _ = load_log(d)
    rows = replay(log, [ErakaModel("eraka"), MostUsed()])
    got = [(r["session"], r["picto"], r["eraka"]) for r in rows if r["position"] == 2]
    want = [(1, WATER, 0), (2, WATER, 1), (3, EAT, 0), (4, WATER, 1)]
    good = got == want and len(rows) == 12 and not any(r["picto"] == GIRAFFE for r in rows)
    print(f"known-answer replay: {got} -> {'OK' if good else 'expected ' + str(want)}")
    return good


# ----------------------------------------------------------------------------- main

def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("folder", nargs="?")
    ap.add_argument("--warmup", type=int, default=20, help="also report results after the first N taps")
    ap.add_argument("--selftest", action="store_true")
    a = ap.parse_args()
    if a.selftest or not a.folder:
        sys.exit(0 if selftest() else 1)

    taps, msgs = load_log(a.folder)
    models = all_models()
    rows = replay(taps, models)
    names = [m.name for m in models]
    n_undone = sum(t["undone"] for t in taps)
    n_repeat = sum(t.get("repeat", False) and not t["undone"] for t in taps)
    seen, novel = set(), 0
    for t in taps:
        if t["undone"] or t.get("repeat", False):
            continue
        novel += t["picto_id"] not in seen
        seen.add(t["picto_id"])
    report = [f"# Eraka replay evaluation – {os.path.basename(os.path.abspath(a.folder))}", "",
              f"- Taps logged: {len(taps)}; undone (not predicted, not learned): {n_undone}; "
              f"repeats of a picture already in the message (not predicted, not learned): {n_repeat}; evaluated: {len(rows)}",
              f"- Messages: {len(msgs)} events; sessions: {len({t['session_id'] for t in taps})}",
              f"- First-ever use of a picture: {novel} of {len(rows)} taps ({novel / max(1, len(rows)):.1%}). "
              f"A model that starts from zero cannot predict these, so its ceiling is {1 - novel / max(1, len(rows)):.1%} "
              f"(the base model is meant to raise it)",
              f"- Base model: {'yes' if any(m.name == 'eraka_base' for m in models) else 'no (assets/eraka_base/base_messages.csv not found)'}",
              "", table(summarise(rows, names), "All taps")]
    if len(rows) > a.warmup:
        report.append(table(summarise(rows, names, a.warmup), f"After the first {a.warmup} taps (model has warmed up)"))
    report.append(live_table(taps))
    report.append("Notes: ranks exclude pictures already in the message; ties broken by picture id "
                  "(OTTAA: stored order). 'ottaa' is an offline approximation of OTTAA's score formula.\n")
    text = "\n".join(report)
    print(text)
    with open(os.path.join(a.folder, "eval_report.md"), "w", encoding="utf-8") as f:
        f.write(text)
    with open(os.path.join(a.folder, "eval_ranks.csv"), "w", encoding="utf-8", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader()
        w.writerows(rows)
    print(f"-> {os.path.join(a.folder, 'eval_report.md')} and eval_ranks.csv")


if __name__ == "__main__":
    main()
