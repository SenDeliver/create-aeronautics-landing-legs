"""Phase-2 checks (Hydraulic Legs, Leg Controller, Linking). Reads TRACE rows (via analyze.py) and STATUS lines.
usage: python3 tools/analyze_p2.py <p2_phys|p2_power|p2_link> <log> [<log after reload>]
Prints one row per check with the measured numbers and PASS/FAIL; exit code 1 on any FAIL."""
import collections, re, statistics, sys

import analyze

ST = re.compile(r'STATUS tag=(\S+) t=(\d+) kind=(\w+)(.*)')
KV = re.compile(r'(\w+)=(\S+)')


def statuses(path):
    out = collections.defaultdict(lambda: collections.defaultdict(list))
    for line in open(path, errors="replace"):
        m = ST.search(line)
        if not m:
            continue
        d = dict(KV.findall(m.group(4)))
        d['kind'] = m.group(3)
        out[m.group(1)][int(m.group(2))].append(d)
    return out


def num(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return float('nan')


def ship(rows):
    for d in rows:
        if d['kind'] == 'ship':
            return d
    return None


def kinds(rows, k):
    return [d for d in rows if d['kind'] == k]


def lines_of(path, pat):
    return [l for l in open(path, errors="replace") if pat in l]


RESULTS = []


def check(name, ok, detail):
    RESULTS.append((name, ok, detail))


def trace_lanes(path):
    marks, rows, events, _, named = analyze.parse(path)
    lanes = {}
    for name, X, start in marks:
        if name in ("restore", "gearup_air", "refit"):
            continue
        s = analyze.ship_for(rows, X) if rows else None
        if s and abs(rows[s][0]['pos'][0] - (X + 2.5)) > 4:
            s = None
        begin = events.get(X, start)
        r = [x for x in rows[s] if x['i'] >= begin] if s else []
        lanes[name] = (X, r)
    return lanes


def mean_rest(rows, kind='hmount'):
    rs = [num(d['rest']) for d in rows if d['kind'] in (kind if isinstance(kind, tuple) else (kind,))]
    return statistics.mean(rs) if rs else float('nan')


def final(st, tag):
    t = max(st[tag]) if st.get(tag) else None
    return st[tag][t] if t is not None else []


def p2_phys(path):
    lanes = trace_lanes(path)
    st = statuses(path)
    for name in ("h_flat", "h_hole1", "h_step2"):
        m = analyze.metrics(lanes[name][1])
        f = final(st, name)
        mr = mean_rest(f)
        ok = m and m['endTilt'] <= 0.5 and m['maxTilt'] <= 6.5 and not m['embEnd'] and m['settle'] is not None and m['settle'] <= 6 and abs(mr - 2.0) <= 0.15
        check(name, ok, f"settle={analyze.fmt(m['settle'])}s maxTilt={m['maxTilt']:.2f} endTilt={m['endTilt']:.2f} meanRest={mr:.2f} (target 2.0)")
    f = final(st, "h_nolink")
    flat = ship(final(st, "h_flat"))
    nl = ship(f)
    forces = [num(d['force']) for d in kinds(f, 'hmount')]
    linked = [d['linked'] for d in kinds(f, 'hmount')]
    dy = num(flat['pos'].split(',')[1]) - num(nl['pos'].split(',')[1])
    check("h_nolink (inert)", len(lanes['h_nolink'][1]) == 0 and max(forces) == 0 and set(linked) == {'0'} and dy >= 1.5,
          f"traceRows={len(lanes['h_nolink'][1])} maxForce={max(forces):.0f}N linked={linked} heightBelowLinked={dy:.2f}")
    for name, lim in (("h_stiff25", 8), ("h_stiff200", 8), ("h_damp02", 10), ("h_damp15", 8)):
        m = analyze.metrics(lanes[name][1])
        f = final(st, name)
        y = num(ship(f)['pos'].split(',')[1])
        ok = m and m['endTilt'] <= 0.5 and not m['embEnd'] and m['settle'] is not None and m['settle'] <= lim
        check(name, ok, f"after drop: settle={analyze.fmt(m['settle'])}s (<= {lim}) maxTilt={m['maxTilt']:.2f} endTilt={m['endTilt']:.2f} shipY={y:.2f} minDist={m['minDist']:.2f}")
    for name, target in (("h_mixed", 2.3), ("h_mixed_hole1", 2.3)):
        m = analyze.metrics(lanes[name][1])
        f = final(st, name)
        h, s = mean_rest(f, 'hmount'), mean_rest(f, 'smount')
        allr = statistics.mean([num(d['rest']) for d in f if d['kind'] in ('hmount', 'smount')])
        check(name, m['endTilt'] <= 0.5 and not m['embEnd'] and abs(allr - target) <= 0.2,
              f"endTilt={m['endTilt']:.2f} meanRest hydraulic={h:.2f} simple={s:.2f} all={allr:.2f} (targets 2.6 / 2.0, one Ship height -> ~{target})")
    m = analyze.metrics(lanes["h_two_ctrl"][1])
    f = final(st, "h_two_ctrl")
    conf = [d['conflict'] for d in kinds(f, 'ctrl')]
    mr = mean_rest(f)
    check("h_two_ctrl", m['endTilt'] <= 0.5 and conf == ['1', '1'] and abs(mr - 2.0) <= 0.15,
          f"endTilt={m['endTilt']:.2f} meanRest={mr:.2f} (Ride Heights 1.5 / 2.5) conflict flags={conf}")
    m = analyze.metrics(lanes["h_one_leg"][1])
    check("h_one_leg (1 of 4 Linked)", m is not None and m['maxTilt'] <= 30, f"maxTilt={m['maxTilt'] if m else float('nan'):.2f} endTilt={m['endTilt'] if m else float('nan'):.2f} (no flip)")
    m = analyze.metrics(lanes["h_ride_change"][1])
    f = final(st, "h_ride_change")
    mr = mean_rest(f)
    dy = num(ship(f)['pos'].split(',')[1]) - num(flat['pos'].split(',')[1])
    check("h_ride_change", abs(mr - 2.8) <= 0.1 and m['endTilt'] <= 0.5 and 0.6 <= dy <= 1.0,
          f"Ride Height 2.8: meanRest={mr:.2f} raised={dy:.2f} endTilt={m['endTilt']:.2f}")


def series(st, tag):
    out = []
    for t in sorted(st.get(tag, {})):
        rows = st[tag][t]
        s = ship(rows)
        hm = kinds(rows, 'hmount')
        if not s or not hm:
            continue
        out.append(dict(t=t, y=num(s['pos'].split(',')[1]), vy=num(s['vy']), tilt=num(s['tilt']),
                        fold=statistics.mean(num(d['fold']) for d in hm), soft=statistics.mean(num(d['soft']) for d in hm),
                        force=max(num(d['force']) for d in hm)))
    return out


def first_t(sr, pred):
    for x in sr:
        if pred(x):
            return x['t']
    return None


def p2_power(path):
    st = statuses(path)
    sr = series(st, "cut")
    y0 = sr[0]['y']
    t0 = sr[0]['t']
    bottom = [x for x in sr if x['t'] - t0 <= 160]
    ymin = min(x['y'] for x in bottom)
    vmin = min(x['vy'] for x in bottom)
    tsoft = first_t(sr, lambda x: x['soft'] >= 0.99)
    end = ship(final(st, "cut"))
    yend = num(end['pos'].split(',')[1])
    check("power cut (full Softening)", tsoft is not None and (tsoft - t0) <= 12 and y0 - ymin >= 1.5 and vmin >= -3.0 and max(x['tilt'] for x in bottom) <= 3.0,
          f"soft=1 after {(tsoft - t0) / 20 if tsoft else float('nan'):.2f}s drop={y0 - ymin:.2f} peakDescent={-vmin:.2f} b/s (<=3) maxTilt={max(x['tilt'] for x in bottom):.2f}")
    check("power back", abs(yend - y0) <= 0.2 and num(end['tilt']) <= 0.5, f"endY-startY={yend - y0:+.2f} endTilt={num(end['tilt']):.2f}")

    f = final(st, "rpm16")
    hm = kinds(f, 'hmount')
    check("stress impact 4 SU/RPM", hm and all(num(d['impact']) == 4.0 and abs(num(d['stress']) - 64) < 0.01 for d in hm),
          f"impact={[d['impact'] for d in hm][:1]} SU/RPM, stress per Mount at 16 RPM={[d['stress'] for d in hm][:1]} SU (expect 64)")
    ys = []
    for lv in (0, 5, 10, 15):
        f = final(st, f"soft{lv}")
        soft = statistics.mean(num(d['soft']) for d in kinds(f, 'hmount'))
        top = [d['top'] for d in kinds(f, 'hmount')]
        y = num(ship(f)['pos'].split(',')[1])
        ys.append(y)
        check(f"top redstone {lv}", abs(soft - lv / 15) <= 0.02 and set(top) == {str(lv)} and num(ship(f)['tilt']) <= 0.5,
              f"signal={top[0]} softening={soft:.2f} (expect {lv / 15:.2f}) shipY={y:.2f} tilt={num(ship(f)['tilt']):.2f}")
    check("Softening lowers the Ship monotonically", all(a > b for a, b in zip(ys, ys[1:])), "shipY " + " > ".join(f"{y:.2f}" for y in ys))

    sr = series(st, "soft_dyn")
    t0, y0 = sr[0]['t'], sr[0]['y']
    tsoft = first_t(sr, lambda x: x['soft'] >= 0.99)
    ymin = min(x['y'] for x in sr)
    last = sr[-1]
    check("top redstone placed/removed on a Ship", tsoft is not None and y0 - ymin >= 1.5 and last['soft'] <= 0.01,
          f"soft=1 after {(tsoft - t0) / 20 if tsoft else float('nan'):.2f}s drop={y0 - ymin:.2f} softAtEnd={last['soft']:.2f} riseBack={last['y'] - ymin:.2f}")

    sr = series(st, "gear_ground")
    t0, y0 = sr[0]['t'], sr[0]['y']
    tup = first_t(sr, lambda x: x['fold'] >= 0.999)
    folded = [x for x in sr if x['fold'] >= 0.999][3:]
    tstart = first_t([x for x in sr if tup and x['t'] > tup], lambda x: x['fold'] < 0.999)
    tdown = first_t([x for x in sr if tstart and x['t'] >= tstart], lambda x: x['fold'] <= 0.001)
    end = sr[-1]
    check("Gear Up on the ground", tup is not None and abs((tup - t0) / 20 - 4.0) <= 0.3 and folded and max(x['force'] for x in folded) == 0 and y0 - min(x['y'] for x in folded) >= 1.5,
          f"fold 0->1 in {(tup - t0) / 20 if tup else float('nan'):.2f}s (16 RPM, expect 4.0) forceWhileFolded={max(x['force'] for x in folded) if folded else float('nan'):.0f}N lowered={y0 - min(x['y'] for x in folded) if folded else 0:.2f}")
    check("Gear Down on the ground", tdown is not None and abs((tdown - tstart + 1) / 20 - 4.0) <= 0.3 and abs(end['y'] - y0) <= 0.2 and end['tilt'] <= 0.5,
          f"unfold in {(tdown - tstart + 1) / 20 if tdown else float('nan'):.2f}s (expect 4.0) endY-startY={end['y'] - y0:+.2f} endTilt={end['tilt']:.2f}")

    sr = series(st, "gear_flight")
    base = series(st, "cut")[0]['y']
    tup = first_t(sr, lambda x: x['fold'] >= 0.999)
    after = [x for x in sr if tup and x['t'] > tup]
    tdown = first_t(after, lambda x: x['fold'] <= 0.001)
    f = final(st, "gear_flight")
    yend = num(ship(f)['pos'].split(',')[1])
    air = [x for x in sr if tup and tup <= x['t'] < (tdown or 10 ** 9)]
    check("Gear Up in flight, Gear Down, land", tup is not None and tdown is not None and min(x['y'] for x in air) > base + 3 and abs(yend - base) <= 0.2 and num(ship(f)['tilt']) <= 0.5,
          f"folded in air at {(tup - sr[0]['t']) / 20 if tup else float('nan'):.2f}s, minY while folded={min(x['y'] for x in air) - base if air else float('nan'):+.2f} vs ground stance, landed endY={yend - base:+.2f} endTilt={num(ship(f)['tilt']):.2f}")

    sr = series(st, "gear_land_up")
    f = final(st, "gear_land_up")
    yend = num(ship(f)['pos'].split(',')[1])
    forces = [num(d['force']) for d in kinds(f, 'hmount')]
    folds = [num(d['fold']) for d in kinds(f, 'hmount')]
    check("Landing while Gear Up gives no support", max(forces) == 0 and min(folds) >= 0.999 and base - yend >= 1.5,
          f"force={max(forces):.0f}N fold={min(folds):.2f} restsBelowStance={base - yend:.2f}")

    for r in (16, 64, 256):
        sr = series(st, f"rpm{r}")
        t0 = sr[0]['t']
        tup = first_t(sr, lambda x: x['fold'] >= 0.999)
        exp = 64 / r
        got = (tup - t0) / 20 if tup else float('nan')
        check(f"stroke speed {r} RPM", tup is not None and abs(got - exp) <= 0.15, f"full fold in {got:.2f}s (expect {exp:.2f}s)")


def p2_link(path, after=None):
    msgs = [l.split("MSG ", 1)[1].strip() for l in lines_of(path, "MSG ")]
    st = statuses(path)
    for tag, pred, what in link_checks(st, msgs):
        check(tag, pred, what)
    if after:
        st2 = statuses(after)
        for tag, pred, what in reload_checks(st2):
            check(tag, pred, what)


def ctrl_links(rows):
    return [int(d['links']) for d in kinds(rows, 'ctrl')]


def linked(rows):
    return [d['linked'] for d in kinds(rows, 'hmount')]


def link_checks(st, msgs):
    out = []
    joined = " | ".join(msgs)

    def has(s):
        return any(s in m for m in msgs)

    tilts = [num(ship(r)['tilt']) for t, r in sorted(st.get("one_link", {}).items()) if ship(r)]
    out.append(("first Leg linked alone: no flip", bool(tilts) and max(tilts) <= 30, f"maxTilt={max(tilts) if tilts else float('nan'):.2f} endTilt={tilts[-1] if tilts else float('nan'):.2f}"))
    f = final(st, "click_ship")
    out.append(("link by click on a Ship", ctrl_links(f) == [4] and set(linked(f)) == {'1'}, f"controller links={ctrl_links(f)} mounts linked={linked(f)}"))
    f = final(st, "limits")
    out.append(("limit 8 Legs", has("already has 8/8") and ctrl_links(f) == [8], f"links={ctrl_links(f)} msg='already has 8/8' seen={has('already has 8/8')}"))
    out.append(("limit 16 blocks", has("Too far"), f"msg 'Too far' seen={has('Too far')}"))
    out.append(("wrong Ship rejected", joined.count("same Ship") >= 2, f"'same Ship' messages={joined.count('same Ship')} (Ship B leg + world leg)"))
    out.append(("unlink by click", has("Leg unlinked"), f"msg 'Leg unlinked' seen={has('Leg unlinked')}"))
    out.append(("Simple Leg not linkable", has("Only Hydraulic"), f"seen={has('Only Hydraulic')}"))
    for stage in ("pend_world", "pend_assembled", "pend_flown"):
        f = final(st, stage)
        out.append((f"pending links survive: {stage}", ctrl_links(f) == [4] and set(linked(f)) == {'1'}, f"links={ctrl_links(f)} mounts linked={linked(f)}"))
    f = final(st, "pend_flown")
    s = ship(f)
    out.append(("Linked Ship stands after flight", s and num(s['tilt']) <= 0.5 and abs(mean_rest(f) - 2.0) <= 0.15, f"tilt={num(s['tilt']) if s else float('nan'):.2f} meanRest={mean_rest(f):.2f}"))
    f = final(st, "partial")
    out.append(("Leg on another Ship is unlinked", set(linked(f)) == {'0'} and all(d['ctrl'] == 'none' for d in kinds(f, 'hmount')),
                f"mounts linked={linked(f)} ctrl={[d['ctrl'] for d in kinds(f, 'hmount')]}"))
    f = final(st, "partial_ctrl")
    out.append(("Controller left behind drops its Links", ctrl_links(f) == [0], f"links={ctrl_links(f)}"))
    for tag in ("disasm0", "disasm1"):
        f = final(st, tag)
        offs = [d['offsets'] for d in kinds(f, 'ctrl')]
        out.append((f"Links survive disassembly ({tag[-1]} extra quarter turns)", ctrl_links(f) == [4] and set(linked(f)) == {'1'} and ship(f) is None,
                    f"onShip={ship(f) is not None} links={ctrl_links(f)} mounts linked={linked(f)} offsets={offs}"))
    f = final(st, "wrench")
    facings = [d['facing'] for d in kinds(f, 'hmount')]
    out.append(("wrench rotates the Mount", facings not in ([], ['down']), f"facing after two wrench clicks={facings}"))
    f = final(st, "wrench_simple")
    facings = [d['facing'] for d in kinds(f, 'smount')]
    out.append(("wrench rotates a Simple Mount", facings not in ([], ['down']), f"facing after a wrench click={facings}"))
    f = final(st, "wrench_removed")
    out.append(("sneak-wrench picks the Mount up", not kinds(f, 'hmount'), f"mounts left={len(kinds(f, 'hmount'))}"))
    return out


def reload_checks(st):
    out = []
    f = final(st, "pend_reloaded")
    s = ship(f)
    out.append(("Links survive reload", ctrl_links(f) == [4] and set(linked(f)) == {'1'}, f"links={ctrl_links(f)} mounts linked={linked(f)}"))
    out.append(("Stands level after reload", s is not None and num(s['tilt']) <= 0.5 and abs(mean_rest(f) - 2.0) <= 0.15,
                f"tilt={num(s['tilt']) if s else float('nan'):.2f} meanRest={mean_rest(f):.2f}"))
    return out


if __name__ == "__main__":
    suite, log = sys.argv[1], sys.argv[2]
    {"p2_phys": p2_phys, "p2_power": p2_power}.get(suite, lambda p: p2_link(p, sys.argv[3] if len(sys.argv) > 3 else None))(log)
    fails = 0
    for name, ok, detail in RESULTS:
        print(f"{name:44} {'PASS' if ok else 'FAIL'}  {detail}")
        fails += not ok
    print(f"blocking failures: {fails}")
    sys.exit(1 if fails else 0)
