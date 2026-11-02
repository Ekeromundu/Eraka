#!/usr/bin/env python3
"""
Makes FAKE pilot data for testing eraka_replay_study.py - never report these numbers.

    python tools/eraka_make_test_logs.py <out folder> [--seed 1] [--days 7]

Writes zips in the same format as the app's "Send usage data" (info.txt, tap_events.csv,
message_events.csv; ';' separated). Each made-up child has one habit, so we know in
advance which model SHOULD win - a check that the replay measures what it claims:

  T01 "routine"   messages depend on place and time of day (breakfast at home in the
                  morning, toilet at school ...)  -> Eraka should beat Eraka without
                  time and place. T01's parent sends the data twice (day 3 and day 7),
                  to test that taps sent twice are counted once.
  T02 "changes"   wants juice for the first half of the week, then water
                  -> Eraka should beat Eraka without recency.
  T03 "explorer"  random two-word messages from 60 pictures -> low scores for every
                  model and many first-ever uses.
About 7% of taps are a wrong picture that is then undone (like a real child).
"""
import argparse
import io
import os
import random
import zipfile
from datetime import datetime, timedelta

I, WANT, GO, PLAY, EAT, DRINK = 643, 44, 30, 22, 118, 34
P = {"breakfast": 9101, "juice": 9102, "water": 9103, "toilet": 9104, "ball": 9105, "tv": 9106,
     "sleep": 9107, "teacher": 9108, "home": 9109, "swing": 9110, "bath": 9111, "mom": 9112}

TAP_COLS = "id;ts;session_id;position;picto_id;prev1_id;prev2_id;source;place;hour;weekday;" \
           "shown_board;shown_smart;rank_board;rank_smart;model"
MSG_COLS = "id;ts;session_id;type;length;picto_ids;place;hour"


def place_for(day, hour):
    if day.weekday() < 5 and 8 <= hour < 13:
        return "SCHOOL"
    return "PARK" if hour in (15, 16) else "HOME"


def routine(rng, day, hour, place, d):
    if place == "SCHOOL":
        return rng.choice([[I, WANT, P["toilet"]], [PLAY, P["ball"]], [I, WANT, P["teacher"]]])
    if place == "PARK":
        return rng.choice([[PLAY, P["swing"]], [PLAY, P["ball"]], [GO, P["home"]]])
    if hour < 10:
        return rng.choice([[I, WANT, P["breakfast"]], [I, WANT, P["breakfast"]], [DRINK, P["juice"]]])
    if hour >= 19:
        return rng.choice([[I, WANT, P["sleep"]], [I, WANT, P["bath"]], [P["mom"]]])
    return rng.choice([[I, WANT, P["tv"]], [EAT], [I, WANT, P["tv"]]])


def changes(rng, day, hour, place, d):
    drink = P["juice"] if d < 3 else P["water"]
    return rng.choice([[I, WANT, drink], [I, WANT, drink], [DRINK, drink], [I, WANT, P["tv"]], [PLAY, P["ball"]]])


def explorer(rng, day, hour, place, d):
    words = list(range(9200, 9260))
    return [rng.choice([I, WANT, PLAY, GO]), rng.choice(words)]


CHILDREN = [("T01", routine, (3, 7)), ("T02", changes, (7,)), ("T03", explorer, (7,))]


def simulate(code, habit, rng, start, days):
    taps, msgs, sess = [], [], 0
    for d in range(days):
        day = start + timedelta(days=d)
        hours = sorted(rng.sample([7, 8, 9, 10, 11, 12, 14, 15, 16, 17, 18, 19, 20], 8))
        for hour in hours:
            for _ in range(rng.randint(1, 2)):
                sess += 1
                place = place_for(day, hour)
                ts = int((day.replace(hour=hour, minute=rng.randint(0, 50))).timestamp() * 1000)
                msg = habit(rng, day, hour, place, d)
                on = []
                for pos, pid in enumerate(msg):
                    if rng.random() < 0.07:  # wrong picture, then undo
                        wrong = rng.choice(list(P.values()))
                        ts += rng.randint(1500, 4000)
                        taps.append(dict(ts=ts, s=sess, pos=pos, pid=wrong, on=list(on), place=place, hour=hour, day=day))
                        ts += 1500
                        msgs.append(dict(ts=ts, s=sess, type="undo", ids=on + [wrong], place=place, hour=hour))
                    ts += rng.randint(1500, 4000)
                    taps.append(dict(ts=ts, s=sess, pos=pos, pid=pid, on=list(on), place=place, hour=hour, day=day))
                    on.append(pid)
                ts += 2000
                msgs.append(dict(ts=ts, s=sess, type="spoken", ids=on, place=place, hour=hour))
    return taps, msgs


def csv_text(taps, msgs, until_ts):
    t_out, m_out = [TAP_COLS], [MSG_COLS]
    for i, t in enumerate(taps, 1):
        if t["ts"] > until_ts:
            break
        p1 = t["on"][-1] if t["on"] else -1
        p2 = t["on"][-2] if len(t["on"]) > 1 else -1
        t_out.append(f"{i};{t['ts']};{t['s']};{t['pos']};{t['pid']};{p1};{p2};board;{t['place']};{t['hour']};"
                     f"{t['day'].isoweekday()};;;0;0;eraka")
    for i, m in enumerate(msgs, 1):
        if m["ts"] > until_ts:
            break
        m_out.append(f"{i};{m['ts']};{m['s']};{m['type']};{len(m['ids'])};{' '.join(map(str, m['ids']))};"
                     f"{m['place']};{m['hour']}")
    return "\n".join(t_out) + "\n", "\n".join(m_out) + "\n", len(t_out) - 1


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument("out")
    ap.add_argument("--seed", type=int, default=1)
    ap.add_argument("--days", type=int, default=7)
    a = ap.parse_args()
    os.makedirs(a.out, exist_ok=True)
    start = datetime(2026, 11, 2)
    for code, habit, sends in CHILDREN:
        rng = random.Random(f"{a.seed}-{code}")
        taps, msgs = simulate(code, habit, rng, start, a.days)
        for day_n in sends:
            until = start + timedelta(days=min(day_n, a.days))
            tap_csv, msg_csv, n = csv_text(taps, msgs, int(until.timestamp() * 1000))
            name = f"eraka_{code}_{until:%Y%m%d}_2000.zip"
            with zipfile.ZipFile(os.path.join(a.out, name), "w", zipfile.ZIP_DEFLATED) as z:
                z.writestr("info.txt", f"child_code={code}\napp_version=TEST (synthetic)\n"
                                       f"comm_level=Minimally verbal\nage_years=7\ninterests=food,animals\n")
                z.writestr("tap_events.csv", tap_csv)
                z.writestr("message_events.csv", msg_csv)
            print(f"{name}: {n} taps")
        print(f"{code}: {len(taps)} taps in total, {sum(1 for m in msgs if m['type'] == 'undo')} undone")


if __name__ == "__main__":
    main()
