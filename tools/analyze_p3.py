"""Phase-3 checks. usage: python3 tools/analyze_p3.py status <log>   (exit 1 on failure)"""
import re, sys


def p3_lines(text):
    rows = []
    for m in re.finditer(r'P3 tag=(\S+) kind=(\S+) pos=([^g]+?) (grounded=.*)', text):
        d = dict(kv.split("=", 1) for kv in re.findall(r'(\w+=\S+)', m.group(4)))
        d["tag"], d["kind"] = m.group(1), m.group(2)
        rows.append(d)
    return rows


def status(log):
    text = open(log, errors="replace").read()
    rows = p3_lines(text)
    results = []

    def check(name, ok, info=""):
        results.append((name, ok, info))

    recipes = re.findall(r'RECIPE (PASS|FAIL) (\S+)', text)
    check("recipes load and match (6)", len(recipes) == 6 and all(r[0] == "PASS" for r in recipes), " ".join(f"{n}:{r}" for r, n in recipes))
    check("sequenced assembly on a real belt line", "ASSEMBLY PASS" in text, (re.findall(r'ASSEMBLY \w+ items=\[.*?\]', text) or ["none"])[-1])
    bad = [l for l in text.splitlines() if ("Exception" in l or "/ERROR]" in l) and "No key layers" not in l and "Error loading class" not in l]
    check("no errors or exceptions in the log", not bad, bad[0][:160] if bad else "")

    def legs(tag):
        return [r for r in rows if r["tag"] == tag and r["kind"] == "leg"]

    def ctrl(tag):
        return [r for r in rows if r["tag"] == tag and r["kind"] == "ctrl"]

    fl = legs("p3_flat")
    check("flat: 4 Legs grounded, no Overreach", len(fl) == 4 and all(r["grounded"] == "1" and r["over"] == "0" for r in fl))
    check("flat: load 20-30% each", all(0.2 <= float(r["load"]) <= 0.3 for r in fl), " ".join(r["load"] for r in fl))
    c = ctrl("p3_flat")
    check("flat: Controller 4/4 grounded, no Overreach", bool(c) and c[0]["grounded"] == "4" and c[0]["over"] == "0")
    bl = legs("p3_bottomless")
    check("bottomless hole: the Leg over the hole shows Overreach", any(r["over"] == "1" and r["grounded"] == "0" for r in bl))
    c = ctrl("p3_bottomless")
    check("bottomless hole: Controller shows Overreach, 3/4 grounded", bool(c) and c[0]["over"] == "1" and c[0]["grounded"] == "3")
    check("bottomless hole (Simple Legs): Overreach", any(r["over"] == "1" for r in legs("p3_bottomless_simple")))
    h1 = legs("p3_hole1")
    check("1-deep hole (levels fine): no Overreach", len(h1) == 4 and all(r["over"] == "0" for r in h1))
    import os
    rcon = open(log + ".rcon", errors="replace").read() if os.path.exists(log + ".rcon") else ""
    ctrl_board = re.search(r'99, 152, 62 has the following block data: (.*)', rcon)
    mount_board = re.search(r'109, 152, 62 has the following block data: (.*)', rcon)
    check("Display Link legworks:leg_controller_status writes a board", bool(ctrl_board) and "legworks.display.ride_height" in ctrl_board.group(1))
    check("Display Link legworks:leg_mount_status writes a board", bool(mount_board) and "legworks.display.reach" in mount_board.group(1))

    fails = 0
    for name, ok, info in results:
        print(f"{'PASS' if ok else 'FAIL'}  {name}  {info}")
        fails += not ok
    return fails


if __name__ == "__main__":
    mode, log = sys.argv[1], sys.argv[2]
    sys.exit(1 if status(log) else 0)
