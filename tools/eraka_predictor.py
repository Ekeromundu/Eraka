"""
Python copy of ErakaPredictor.java (same formulas, same defaults), used by the
replay evaluation. Kept line-for-line close to the Java so results match the app;
eraka_replay_eval.py --selftest checks it against the Java unit-test numbers.

P(w|h) = (1-muT-muP) * [l3*P3 + l2*P2 + l1*P1] + muT*P(w|time) + muP*P(w|place)
mixed with an optional base model: (1-b)*P_child + b*P_base, b = K/(K+n).
"""
import math

START = -1


def time_bucket(hour):
    if 5 <= hour < 12: return "MORNING"
    if 12 <= hour < 15: return "MIDDAY"
    if 15 <= hour < 20: return "AFTERNOON"
    return "EVENING"


class Config:
    def __init__(self, **kw):
        self.l3, self.l2, self.l1 = 0.5, 0.3, 0.2
        self.mu_time, self.mu_place = 0.1, 0.1
        self.half_life_days = 14.0
        self.base_strength = 30.0
        for k, v in kw.items():
            if not hasattr(self, k):
                raise KeyError(k)
            setattr(self, k, v)


class Counts:
    __slots__ = ("by", "total")

    def __init__(self):
        self.by, self.total = {}, 0.0

    def add(self, pid, w):
        self.by[pid] = self.by.get(pid, 0.0) + w
        self.total += w

    def p(self, pid):
        return self.by.get(pid, 0.0) / self.total if self.total > 0 else 0.0


def _has(c):
    return c is not None and c.total > 0


class ErakaPredictor:
    def __init__(self, cfg=None):
        self.cfg = cfg or Config()
        self.tri, self.bi, self.uni = {}, {}, Counts()
        self.by_time, self.by_place = {}, {}
        self.ref_ts = None
        self.taps_used = 0
        self.base = None

    # learning
    def add_tap(self, pid, prev1, prev2, ts, hour, place, undone=False):
        if undone:
            return
        if self.ref_ts is None:
            self.ref_ts = ts
        hl_ms = self.cfg.half_life_days * 24 * 3600 * 1000
        w = 1.0 if math.isinf(hl_ms) else 2.0 ** ((ts - self.ref_ts) / hl_ms)
        self.tri.setdefault((prev2, prev1), Counts()).add(pid, w)
        self.bi.setdefault(prev1, Counts()).add(pid, w)
        self.uni.add(pid, w)
        if hour >= 0:
            self.by_time.setdefault(time_bucket(hour), Counts()).add(pid, w)
        if place:
            self.by_place.setdefault(place, Counts()).add(pid, w)
        self.taps_used += 1

    def has_base(self):
        return self.base is not None and self.base.taps_used > 0

    def base_share(self):
        if not self.has_base():
            return 0.0
        if self.taps_used == 0:
            return 1.0
        k = self.cfg.base_strength
        return k / (k + self.taps_used)

    # predicting
    def _weights(self, prev1, prev2, hour, place):
        c = self.cfg
        l3 = c.l3 if _has(self.tri.get((prev2, prev1))) else 0
        l2 = c.l2 if _has(self.bi.get(prev1)) else 0
        l1 = c.l1 if self.uni.total > 0 else 0
        s = l3 + l2 + l1
        mt = c.mu_time if _has(self.by_time.get(time_bucket(hour))) else 0
        mp = c.mu_place if _has(self.by_place.get(place)) else 0
        seq = 1.0 - mt - mp if s > 0 else 0
        if s <= 0:
            ctx = mt + mp
            if ctx > 0:
                mt, mp = mt / ctx, mp / ctx
        return ((seq * l3 / s, seq * l2 / s, seq * l1 / s) if s > 0 else (0, 0, 0)) + (mt, mp)

    def own_probabilities(self, prev1, prev2, hour, place):
        if self.uni.total <= 0:
            return {}
        w3, w2, w1, wt, wp = self._weights(prev1, prev2, hour, place)
        tri, bi = self.tri.get((prev2, prev1)), self.bi.get(prev1)
        tm, pl = self.by_time.get(time_bucket(hour)), self.by_place.get(place)
        out = {}
        for pid in self.uni.by:
            p = (w3 * (tri.p(pid) if tri else 0) + w2 * (bi.p(pid) if bi else 0) + w1 * self.uni.p(pid)
                 + wt * (tm.p(pid) if tm else 0) + wp * (pl.p(pid) if pl else 0))
            if p > 0:
                out[pid] = p
        return out

    def probabilities(self, prev1, prev2, hour, place):
        b = self.base_share()
        if b <= 0:
            return self.own_probabilities(prev1, prev2, hour, place)
        fb = self.base.probabilities(prev1, prev2, hour, place)
        if b >= 1:
            return fb
        own = self.own_probabilities(prev1, prev2, hour, place)
        if not own:
            return fb
        if not fb:
            return own
        out = {k: (1 - b) * v for k, v in own.items()}
        for k, v in fb.items():
            out[k] = out.get(k, 0.0) + b * v
        return out
