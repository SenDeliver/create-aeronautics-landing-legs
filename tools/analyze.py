"""Analyzes TRACE logs from tools/run_headless.sh and checks pass thresholds.
usage: python3 tools/analyze.py <suite> <log> [<log after reload>]
suites: r3 r4 r5 limbs remove reload. Exit code 1 if any blocking check fails."""
import collections, math, re, sys

TR = re.compile(r'TRACE ship=(\w+) t=(\d+) pos=([-\d.]+),([-\d.]+),([-\d.]+) tilt=([\d.]+)(.*)')
LEG = re.compile(r'L(\d+)=([-\d.]+)/(\S+?)/([SW-])/([\d.]+)/([-\d.]+)/([E-])/([-\d.]+)')
MARK = re.compile(r'([A-Za-z0-9_]+)\.(\d+)(\.event)?')


def parse(path):
    marks, rows, events, refits, idx = [], collections.defaultdict(list), {}, {}, 0
    named = {}
    for line in open(path, errors="replace"):
        m = re.search(r'MARK (\S+)', line)
        if m:
            tag = m.group(1)
            mm = MARK.fullmatch(tag)
            if mm and mm.group(1) == "refit":
                refits[int(mm.group(2))] = idx
            elif mm and mm.group(3):
                events[int(mm.group(2))] = idx
            elif mm:
                marks.append((mm.group(1), int(mm.group(2)), idx))
            else:
                named[tag] = idx
            continue
        m = TR.search(line)
        if m:
            idx += 1
            legs = [dict(rest=float(a[1]), dist=None if a[2] == 'none' else float(a[2]), slip=a[3] == 'S', wall=a[3] == 'W',
                         force=float(a[5]), emb=a[6] == 'E', err=float(a[7])) for a in LEG.findall(m.group(7))]
            rows[m.group(1)].append(dict(i=idx, t=int(m.group(2)), pos=(float(m.group(3)), float(m.group(4)), float(m.group(5))),
                                         tilt=float(m.group(6)), legs=legs))
    return marks, rows, events, refits, named


def ship_for(rows, X):
    return min(rows, key=lambda s: abs(rows[s][0]['pos'][0] - (X + 3)))


def metrics(r):
    if len(r) < 3:
        return None
    t0 = r[0]['t']
    settle = None
    for i in range(1, len(r)):
        if all(r[j]['tilt'] <= 1.5 and math.dist(r[j]['pos'], r[j - 1]['pos']) <= 0.005 for j in range(i, len(r))):
            settle = (r[i]['t'] - t0) / 20
            break
    flips = slips = walls = 0
    lost_at = {}
    for a, b in zip(r, r[1:]):
        for k, (la, lb) in enumerate(zip(a['legs'], b['legs'])):
            if la['dist'] is not None and lb['dist'] is not None and abs(la['dist'] - lb['dist']) > 0.4:
                flips += 1
            if la['dist'] is not None and lb['dist'] is None:
                lost_at[k] = b['t']
            if la['dist'] is None and lb['dist'] is not None and k in lost_at and b['t'] - lost_at[k] <= 10:
                flips += 1
            slips += lb['slip']
            walls += lb['wall']
    tail = r[-20:]
    return dict(
        settle=settle, maxTilt=max(x['tilt'] for x in r), endTilt=r[-1]['tilt'],
        drift=math.hypot(r[-1]['pos'][0] - r[0]['pos'][0], r[-1]['pos'][2] - r[0]['pos'][2]),
        flips=flips, slips=slips, walls=walls,
        embEnd=any(l['emb'] for x in tail for l in x['legs']),
        embTicks=sum(1 for x in r for l in x['legs'] if l['emb']),
        errEnd=max((abs(l['err']) for x in tail for l in x['legs']), default=0),
        minDist=min((l['dist'] for x in r for l in x['legs'] if l['dist'] is not None), default=float('nan')),
        legsEnd=len(r[-1]['legs']))


def fmt(v):
    if v is None:
        return "never"
    if isinstance(v, bool):
        return "YES" if v else "no"
    if isinstance(v, float):
        return f"{v:.2f}"
    return str(v)


def check(rules, m):
    """rules: list of (label, fn(m) -> bool). Returns list of failed labels."""
    return [label for label, fn in rules if not fn(m)]


def settle_le(x):
    return lambda m: m['settle'] is not None and m['settle'] <= x


def rules_for(suite, name):
    end = ("endTilt<=0.5", lambda m: m['endTilt'] <= 0.5)
    noemb = ("no foot embedded at end", lambda m: not m['embEnd'])
    if suite == "r3":
        return [end, ("maxTilt<=6.5", lambda m: m['maxTilt'] <= 6.5), noemb], False
    if suite == "limbs":
        return [end, ("maxTilt<=9", lambda m: m['maxTilt'] <= 9.0), noemb], False
    if suite == "r4":
        if name.startswith("walk"):
            return [end, ("maxTilt<=8", lambda m: m['maxTilt'] <= 8.0), ("drift<=0.3", lambda m: m['drift'] <= 0.3)], False
        if name.startswith("drop"):
            return [end, ("maxTilt<=2.5", lambda m: m['maxTilt'] <= 2.5), ("settle<=4s", settle_le(4.0)), ("drift<=0.3", lambda m: m['drift'] <= 0.3)], False
        return [end, ("maxTilt<=2", lambda m: m['maxTilt'] <= 2.0), ("settle<=4s", settle_le(4.0)), ("flips==0", lambda m: m['flips'] == 0)], False
    if suite == "r5":
        if name in ("drop_tilted_20", "drop_tilted_pit_20") or name.startswith("drop_pit_2"):
            return [end, noemb], True
        if name.startswith("place"):
            return [end, noemb, ("embTicks<=2", lambda m: m['embTicks'] <= 2)], False
        if "pit" in name:
            return [("endTilt<=0.25", lambda m: m['endTilt'] <= 0.25), noemb], False
        if "tilted" in name:
            return [end, noemb], False
        return [("endTilt<=0.15", lambda m: m['endTilt'] <= 0.15), noemb], False
    return [end], False


def report(suite, path):
    marks, rows, events, refits, _ = parse(path)
    cols = ["settle", "maxTilt", "endTilt", "drift", "flips", "slips", "walls", "embEnd", "embTicks", "errEnd", "minDist", "legsEnd"]
    print(f"{'lane':22} " + " ".join(f"{c:>8}" for c in cols) + "  result")
    fails = 0
    for name, X, start in marks:
        if not rows:
            break
        s = ship_for(rows, X)
        begin = events.get(X, start)
        m = metrics([x for x in rows[s] if x['i'] >= begin])
        if m is None:
            print(f"{name:22} no data  FAIL")
            fails += 1
            continue
        rules, tracked = rules_for(suite, name)
        bad = check(rules, m)
        if suite == "remove":
            before = [x for x in rows[s] if begin <= x['i'] < refits.get(X, 10 ** 9)]
            mb = metrics(before)
            bad = []
            lim = 3.0 if name == "remove6" else 60.0
            if mb is None or mb['legsEnd'] != (5 if name == "remove6" else 3):
                bad.append("leg dropped out after removal")
            if mb is not None and mb['maxTilt'] > lim:
                bad.append(f"tilt after removal<={lim}")
            if m['legsEnd'] != (6 if name == "remove6" else 4):
                bad.append("leg back after refit")
            if m['endTilt'] > 0.5:
                bad.append("endTilt<=0.5 after refit")
            if mb is not None:
                m['maxTilt'] = mb['maxTilt']
        status = "PASS" if not bad else ("TRACKED" if tracked else "FAIL")
        if bad and not tracked:
            fails += 1
        print(f"{name:22} " + " ".join(f"{fmt(m[c]):>8}" for c in cols) + f"  {status}" + (f" ({'; '.join(bad)})" if bad else ""))
    return fails


def reload_report(before_path, after_path):
    _, rb, _, _, nb = parse(before_path)
    _, ra, _, _, _ = parse(after_path)
    fails = 0
    print(f"{'ship':10} {'restDiff':>8} {'posDiff':>8} {'tiltBefore':>10} {'maxTiltAfter':>12} {'endTiltAfter':>12} {'legs':>5}  result")
    for s in rb:
        if s not in ra:
            print(f"{s:10} missing after reload  FAIL")
            fails += 1
            continue
        last = [x for x in rb[s] if x['i'] <= nb.get('before_save', 10 ** 9)][-1]
        first = ra[s][0]
        rest_diff = max(abs(a['rest'] - b['rest']) for a, b in zip(last['legs'], first['legs']))
        pos_diff = math.dist(last['pos'], first['pos'])
        max_after = max(x['tilt'] for x in ra[s])
        end_after = ra[s][-1]['tilt']
        bad = []
        if len(last['legs']) != len(first['legs']):
            bad.append("leg count")
        if rest_diff > 0.05:
            bad.append("rest kept")
        if pos_diff > 0.1:
            bad.append("no jump")
        if max_after > 1.0 or end_after > 0.5:
            bad.append("level after reload")
        fails += bool(bad)
        print(f"{s:10} {rest_diff:8.3f} {pos_diff:8.3f} {last['tilt']:10.2f} {max_after:12.2f} {end_after:12.2f} {len(first['legs']):5d}  {'PASS' if not bad else 'FAIL (' + ', '.join(bad) + ')'}")
    return fails


if __name__ == "__main__":
    suite = sys.argv[1]
    if suite == "reload":
        f = reload_report(sys.argv[2], sys.argv[3])
    else:
        f = report(suite, sys.argv[2])
    print(f"blocking failures: {f}")
    sys.exit(1 if f else 0)
