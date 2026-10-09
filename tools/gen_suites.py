"""Generates the RCON scenario files for every phase-1 suite. Run: python3 tools/gen_suites.py"""
import math, os

HERE = os.path.dirname(os.path.abspath(__file__))
Z = 0
GROUND_TOP = 151
REACH = {"short": 3.0, "long": 4.5, "heavy": 3.5}


def mount(x, y, z, limb, facing="down"):
    nbt = "" if limb is None else '{Limb:{id:"legworks:%s_limb",count:1}}' % limb
    return f"setblock {x} {y} {z} legworks:simple_leg_mount[facing={facing}]{nbt}"


def mount_y(limbs):
    return GROUND_TOP + math.ceil(min(REACH[l] for l in limbs))


def ship5(X, limbs, terrain=None):
    """5x5 iron hull with four corner legs (limbs: 4 names in order (0,0),(4,0),(0,4),(4,4))."""
    y = mount_y(limbs)
    out = [f"fill {X-3} 148 {Z-3} {X+7} 150 {Z+7} minecraft:stone"]
    if terrain == "hole1":
        out.append(f"fill {X-1} 150 {Z-1} {X+1} 150 {Z+1} minecraft:air")
    elif terrain == "hole2":
        out.append(f"fill {X-1} 149 {Z-1} {X+1} 150 {Z+1} minecraft:air")
    elif terrain == "step2":
        out.append(f"fill {X-3} 151 {Z+3} {X+7} 152 {Z+7} minecraft:stone")
    elif terrain == "bottomless":
        out.append(f"fill {X-1} 148 {Z-1} {X+1} 150 {Z+1} minecraft:air")
    elif terrain == "trench":
        out.append(f"fill {X-3} 150 {Z-1} {X+7} 150 {Z+1} minecraft:air")
    out.append(f"fill {X} {y+1} {Z} {X+4} {y+1} {Z+4} minecraft:iron_block")
    for (dx, dz), limb in zip(((0, 0), (4, 0), (0, 4), (4, 4)), limbs):
        out.append(mount(X + dx, y, Z + dz, limb))
    return out, f"sable assemble area {X} {y} {Z} {X+4} {y+1} {Z+4}"


def ship7(X, limbs, mat="stone", pit=False, six=False):
    """7x3 iron hull, legs at the corners (and the long-side middles when six=True). Round 4/5 geometry."""
    y = mount_y(limbs)
    out = [f"fill {X-4} 145 {Z-4} {X+10} 147 {Z+6} minecraft:stone",
           f"fill {X-4} 148 {Z-4} {X+10} 150 {Z+6} minecraft:{mat}"]
    if pit:
        out += [f"fill {X-1} 149 {Z} {X} 150 {Z} minecraft:air",
                f"fill {X+7} 150 {Z-3} {X+7} 150 {Z+5} minecraft:air",
                f"fill {X-3} 150 {Z+3} {X+5} 150 {Z+3} minecraft:air"]
    out.append(f"fill {X} {y+1} {Z} {X+6} {y+1} {Z+2} minecraft:iron_block")
    spots = [(0, 0), (6, 0), (0, 2), (6, 2)] + ([(3, 0), (3, 2)] if six else [])
    for (dx, dz), limb in zip(spots, limbs):
        out.append(mount(X + dx, y, Z + dz, limb))
    return out, f"sable assemble area {X} {y} {Z} {X+6} {y+1} {Z+2}"


def wrap(lanes, settle, tail, spacing=30, events=None, header_sleep=6):
    """lanes: list of (name, builder(X) -> (cmds, assemble)). events: list of (name, cmds(X, y)) played after settle."""
    xs = [100 + spacing * i for i in range(len(lanes))]
    out = [f"forceload add {xs[0]-10} -16 {xs[-1]+20} 16", f"#sleep {header_sleep}"]
    assembles = []
    ys = []
    for (name, build), X in zip(lanes, xs):
        cmds, asm = build(X)
        out += cmds
        assembles.append(asm)
        ys.append(int(asm.split()[4]))
    out += ["legworks trace 1"]
    for (name, _), X in zip(lanes, xs):
        out.append(f"legworks mark {name}.{X}")
    out += assembles
    out.append(f"#sleep {settle}")
    if events:
        for (name, _), X, y in zip(lanes, xs, ys):
            ev = events.get(name)
            if ev:
                out.append(f"legworks mark {name}.{X}.event")
                out += ev(X, y)
    out += [f"#sleep {tail}", "legworks mark end", "legworks trace 0", "stop"]
    return out


def r3():
    S = ["short"] * 4
    lanes = [(t, (lambda t: lambda X: ship5(X, S, t))(t)) for t in ("hole1", "hole2", "step2", "trench")]
    return wrap(lanes, 40, 1, spacing=20)


def r4(load_kg=15):
    S = ["short"] * 4

    def at(X, y):
        return f"{X+3.5} {y+0.5} {Z+1.5}"

    def walk(X, y):
        w = []
        for dx, dz in ((0, 0), (6, 0), (6, 2), (0, 2), (3, 1), (0, 0), (6, 2)):
            w += [f"legworks load {X+dx+0.5} {y+1} {Z+dz+0.5} {load_kg} 30", "#sleep 1.5"]
        return w

    def near(X, y, cmd):
        return f"execute positioned {at(X, y)} run {cmd}"

    tests = [
        ("idle", True, lambda X, y: []),
        ("push_small_x", True, lambda X, y: [f"legworks push {at(X, y)} 30 0 0"]),
        ("push_med_negx", True, lambda X, y: [f"legworks push {at(X, y)} -60 0 0"]),
        ("push_med_z", True, lambda X, y: [f"legworks push {at(X, y)} 0 0 40"]),
        ("push_big_negz", True, lambda X, y: [f"legworks push {at(X, y)} 0 0 -80"]),
        ("push_corner_twist", True, lambda X, y: [f"legworks push {X} {y+0.5} {Z} 30 0 -30"]),
        ("walk_load", True, walk),
        ("drop_1_5", True, lambda X, y: [near(X, y, "sable physics translation @n add 0 1.5 0")]),
        ("drop_drift", True, lambda X, y: [near(X, y, "sable physics translation @n add 0 1.5 0"), f"legworks push {X+3.5} {y+2} {Z+1.5} 50 0 25"]),
        ("walk_load_flat", False, walk),
    ]
    lanes = [(n, (lambda p: lambda X: ship7(X, S, pit=p))(pit)) for n, pit, _ in tests]
    events = {n: ev for n, _, ev in tests}
    return wrap(lanes, 15, 14, events=events)


def r5():
    S = ["short"] * 4
    lanes, events = [], {}

    def drop(h, spin=False):
        def ev(X, y):
            c = [f"execute positioned {X+3.5} {y+0.5} {Z+1.5} run sable physics translation @n add 0 {h} 0"]
            if spin:
                c.append(f"execute positioned {X+3.5} {y+0.5} {Z+1.5} run sable physics impulse @n angular 25 0 15")
            return c
        return ev

    def add(name, mat, pit, ev):
        lanes.append((name, (lambda m, p: lambda X: ship7(X, S, mat=m, pit=p))(mat, pit)))
        events[name] = ev

    for mat in ("sand", "grass_block", "stone"):
        for h in (5, 10, 20):
            add(f"drop_{mat}_{h}", mat, False, drop(h))
    for h in (5, 10, 20):
        add(f"drop_pit_{h}", "stone", True, drop(h))
    for h in (10, 20):
        add(f"drop_tilted_{h}", "stone", False, drop(h, True))
        add(f"drop_tilted_pit_{h}", "sand", True, drop(h, True))
    add("place_centre", "stone", True, lambda X, y: [f"setblock {X} 149 0 minecraft:stone"])
    add("place_slab", "stone", True, lambda X, y: [f"setblock {X} 149 0 minecraft:stone_slab[type=bottom]"])
    add("place_neighbour", "stone", True, lambda X, y: [f"setblock {X-1} 149 0 minecraft:stone"])
    return wrap(lanes, 14, 14, events=events, header_sleep=8)


def limbs():
    lanes = []
    for limb in ("long", "heavy"):
        for t in (None, "hole1", "hole2", "step2", "trench"):
            lanes.append((f"{limb}_{t or 'flat'}", (lambda l, t: lambda X: ship5(X, [l] * 4, t))(limb, t)))
    mixed = ["short", "long", "heavy", "short"]
    for t in (None, "hole1", "hole2", "step2", "trench"):
        lanes.append((f"mixed_{t or 'flat'}", (lambda t: lambda X: ship5(X, mixed, t))(t)))
    lanes.append(("mixed7_pit", lambda X: ship7(X, ["short", "long", "heavy", "long"], pit=True)))
    return wrap(lanes, 40, 1, spacing=20)


def remove():
    lanes = [("remove4", lambda X: ship5(X, ["short"] * 4)),
             ("remove6", lambda X: ship7(X, ["short"] * 6, six=True)),
             ("remove_mixed", lambda X: ship5(X, ["short", "long", "heavy", "short"]))]

    def ev4(X, y):
        return [f"legworks limb {X+4.5} {y+0.5} {Z+4.5} none", "#sleep 8", f"legworks mark refit.{X}", f"legworks limb {X+4.5} {y+0.5} {Z+4.5} short"]

    def ev6(X, y):
        return [f"legworks limb {X+6.5} {y+0.5} {Z+0.5} none", "#sleep 8", f"legworks mark refit.{X}", f"legworks limb {X+6.5} {y+0.5} {Z+0.5} short"]

    def evm(X, y):
        return [f"legworks limb {X+4.5} {y+0.5} {Z+0.5} none", "#sleep 8", f"legworks mark refit.{X}", f"legworks limb {X+4.5} {y+0.5} {Z+0.5} long"]

    return wrap(lanes, 20, 14, events={"remove4": ev4, "remove6": ev6, "remove_mixed": evm})


def reload_a():
    lanes = [("reload_short_hole1", lambda X: ship5(X, ["short"] * 4, "hole1")),
             ("reload_mixed_hole2", lambda X: ship5(X, ["short", "long", "heavy", "short"], "hole2")),
             ("reload_long_step2", lambda X: ship5(X, ["long"] * 4, "step2"))]
    out = wrap(lanes, 30, 1, spacing=20)
    i = out.index("legworks mark end")
    return out[:i] + ["legworks mark before_save", "#sleep 1", "save-all flush", "#sleep 2"] + out[i:]


def reload_b():
    return ["legworks trace 1", "legworks mark after_reload", "#sleep 15", "legworks mark end", "legworks trace 0", "stop"]


def pitdrops():
    S = ["short"] * 4
    lanes, events = [], {}
    for h in (5, 10, 15, 18, 19, 20, 21, 22):
        lanes.append((f"drop_pit_{h}", lambda X: ship7(X, S, pit=True)))
        events[f"drop_pit_{h}"] = (lambda h: lambda X, y: [f"execute positioned {X+3.5} {y+0.5} {Z+1.5} run sable physics translation @n add 0 {h} 0"])(h)
    for h in (10, 15, 20):
        lanes.append((f"drop_tilted_pit_{h}", lambda X: ship7(X, S, mat="sand", pit=True)))
        events[f"drop_tilted_pit_{h}"] = (lambda h: lambda X, y: [f"execute positioned {X+3.5} {y+0.5} {Z+1.5} run sable physics translation @n add 0 {h} 0",
                                                                    f"execute positioned {X+3.5} {y+0.5} {Z+1.5} run sable physics impulse @n angular 25 0 15"])(h)
    return wrap(lanes, 14, 16, events=events, header_sleep=8)


def pit20():
    S = ["short"] * 4
    lanes, events = [], {}
    for i in range(10):
        h = 20 if i < 8 else 21
        name = f"drop_pit_{h}_{i}"
        lanes.append((name, lambda X: ship7(X, S, pit=True)))
        events[name] = (lambda h: lambda X, y: [f"execute positioned {X+3.5} {y+0.5} {Z+1.5} run sable physics translation @n add 0 {h} 0"])(h)
    return wrap(lanes, 14, 16, events=events, header_sleep=8)


HMOUNT = "legworks:hydraulic_leg_mount[facing=down,axis_along_first=true]"
CORNERS = ((0, 0), (4, 0), (0, 4), (4, 4))


def ship5h(X, legs, terrain=None, ride=None, rpm=16, settings=None, controllers=None, links=True, levers=None, limb="short"):
    """5x5 hull, corner legs: 'h' = Hydraulic (Linked to the centre Controller), 'hn' = Hydraulic unlinked, 's' = Simple.
    controllers: optional list of (dx, dz, ride, [corner indices]) replacing the single centre Controller.
    levers: optional analog lever State per corner (placed on the hull above the Mount, i.e. its top)."""
    y = GROUND_TOP + math.ceil(REACH[limb])
    out, _ = ship5(X, [limb] * 4, terrain)
    out = [c for c in out if "leg_mount" not in c]
    if controllers is None:
        controllers = [(2, 2, ride, [i for i, l in enumerate(legs) if l == "h"])] if links else []
    owner = {}
    for dx, dz, r, idx in controllers:
        for i in idx:
            owner[i] = (dx, dz)
    for i, ((dx, dz), kind) in enumerate(zip(CORNERS, legs)):
        mx, mz = X + dx, Z + dz
        if kind == "s":
            out.append(mount(mx, y, mz, limb))
            continue
        nbt = ['Limb:{id:"legworks:%s_limb",count:1}' % limb]
        if i in owner:
            cdx, cdz = owner[i]
            nbt.append(f"Controller:[I;{cdx - dx},2,{cdz - dz}]")
        if settings:
            nbt.append("Settings:{%s}" % settings)
        out.append(f"setblock {mx} {y} {mz} {HMOUNT}{{{','.join(nbt)}}}")
        motor_x, facing = (mx + 1, "west") if dx == 0 else (mx - 1, "east")
        out.append(f"setblock {motor_x} {y} {mz} create:creative_motor[facing={facing}]{{ScrollValue:{rpm}}}")
        if levers and levers[i] is not None:
            out.append(f"setblock {mx} {y+2} {mz} create:analog_lever[face=floor,facing=north]{{State:{levers[i]}}}")
    for dx, dz, r, idx in controllers:
        ls = ",".join(f"[I;{CORNERS[i][0] - dx},-2,{CORNERS[i][1] - dz}]" for i in idx)
        rh = f",RideHeight:{r}d" if r is not None else ""
        out.append(f"setblock {X+dx} {y+2} {Z+dz} legworks:leg_controller{{Links:[{ls}]{rh}}}")
    return out, f"sable assemble area {X} {y} {Z} {X+4} {y+2} {Z+4}"


def centre(X, y):
    return f"{X+2.5} {y+1.5} {Z+2.5}"


def hw(X, y, tag, ticks):
    return f"legworks watch {centre(X, y)} {tag} {ticks}"


def drop(X, y, h):
    return f"execute positioned {centre(X, y)} run sable physics translation @n add 0 {h} 0"


def p2_phys():
    H = ["h"] * 4
    lanes, ev = [], {}

    def add(name, build, e=None):
        lanes.append((name, build))
        if e:
            ev[name] = e

    add("h_flat", lambda X: ship5h(X, H))
    add("h_hole1", lambda X: ship5h(X, H, "hole1"))
    add("h_step2", lambda X: ship5h(X, H, "step2"))
    add("h_nolink", lambda X: ship5h(X, ["hn"] * 4, links=False))
    add("h_stiff25", lambda X: ship5h(X, H, settings="Stiffness:0.25d"), lambda X, y: [drop(X, y, 3)])
    add("h_stiff200", lambda X: ship5h(X, H, settings="Stiffness:2.0d"), lambda X, y: [drop(X, y, 3)])
    add("h_damp02", lambda X: ship5h(X, H, settings="Damping:0.2d"), lambda X, y: [drop(X, y, 5)])
    add("h_damp15", lambda X: ship5h(X, H, settings="Damping:1.5d"), lambda X, y: [drop(X, y, 5)])
    add("h_mixed", lambda X: ship5h(X, ["s", "h", "h", "s"], ride=2.6))
    add("h_mixed_hole1", lambda X: ship5h(X, ["h", "s", "s", "h"], "hole1", ride=2.6))
    add("h_two_ctrl", lambda X: ship5h(X, H, controllers=[(1, 2, 1.5, [0, 2]), (3, 2, 2.5, [1, 3])]))
    add("h_one_leg", lambda X: ship5h(X, ["h", "hn", "hn", "hn"]))
    add("h_ride_change", lambda X: ship5h(X, H), lambda X, y: [f"legworks ride {centre(X, y)} 28"])
    out = wrap(lanes, 25, 20, spacing=20, events=ev)
    i = out.index("legworks mark end")
    xs = [100 + 20 * k for k in range(len(lanes))]
    return out[:i] + [f"legworks status {centre(X, 154)} {n}" for (n, _), X in zip(lanes, xs)] + out[i:]


def p2_power():
    H = ["h"] * 4
    lanes, ev = [], {}

    def add(name, build, e=None):
        lanes.append((name, build))
        if e:
            ev[name] = e

    def rs(X, y, block):
        return f"legworks shipsetrel {centre(X, y)} 3 2 2 {block}"

    add("cut", lambda X: ship5h(X, H), lambda X, y: [hw(X, y, "cut", 200), f"legworks rpm {centre(X, y)} 0", "#sleep 8", f"legworks mark restore.{X}", f"legworks rpm {centre(X, y)} 16"])
    for lv in (0, 5, 10, 15):
        add(f"soft{lv}", (lambda lv: lambda X: ship5h(X, H, levers=[lv] * 4))(lv))
    add("soft_dyn", lambda X: ship5h(X, H), lambda X, y: [f"legworks shipsetrel {centre(X, y)} {dx} 2 {dz} minecraft:lever[face=floor,facing=north]" for dx, dz in CORNERS]
                                                          + ["#sleep 1", "legworks hold minecraft:air", hw(X, y, "soft_dyn", 200)] + [f"legworks clickrel {centre(X, y)} {dx} 2 {dz} 0 up" for dx, dz in CORNERS]
                                                          + ["#sleep 8", f"legworks mark restore.{X}"] + [f"legworks clickrel {centre(X, y)} {dx} 2 {dz} 0 up" for dx, dz in CORNERS])
    add("gear_ground", lambda X: ship5h(X, H), lambda X, y: [hw(X, y, "gear_ground", 400), rs(X, y, "minecraft:redstone_block"), "#sleep 8", f"legworks mark restore.{X}", rs(X, y, "minecraft:air")])
    add("gear_flight", lambda X: ship5h(X, H), lambda X, y: [drop(X, y, 5), "#sleep 0.3", f"legworks hover {centre(X, y+5)} 400", hw(X, y + 5, "gear_flight", 440), "#sleep 1",
                                                             rs(X, y + 5, "minecraft:redstone_block"), "#sleep 6", f"legworks mark gearup_air.{X}", rs(X, y + 5, "minecraft:air"), "#sleep 6",
                                                             f"legworks mark restore.{X}", f"legworks hover {centre(X, y+5)} 0"])
    add("gear_land_up", lambda X: ship5h(X, H), lambda X, y: [drop(X, y, 5), "#sleep 0.3", f"legworks hover {centre(X, y+5)} 200", "#sleep 1", rs(X, y + 5, "minecraft:redstone_block"),
                                                              "#sleep 6", f"legworks mark restore.{X}", f"legworks hover {centre(X, y+5)} 0", hw(X, y, "gear_land_up", 200)])
    for r in (16, 64, 256):
        add(f"rpm{r}", (lambda r: lambda X: ship5h(X, H, rpm=r))(r), (lambda r: lambda X, y: [hw(X, y, f"rpm{r}", 120), rs(X, y, "minecraft:redstone_block")])(r))
    out = wrap(lanes, 20, 12, spacing=20, events=ev)
    i = out.index("legworks mark end")
    xs = [100 + 20 * k for k in range(len(lanes))]
    return out[:i] + [f"legworks status {centre(X, 154)} {n}" for (n, _), X in zip(lanes, xs)] + out[i:]


def p2_link_a():
    y = 154
    out = ["forceload add 90 -16 320 40", "#sleep 6", "legworks trace 1"]
    # A: click-link on an assembled Ship (Controller placed, nothing linked)
    X = 100
    cmds, asm = ship5h(X, ["hn"] * 4, controllers=[(2, 2, None, [])])
    out += cmds + [f"legworks mark click_ship.{X}", asm, "#sleep 3", "legworks hold legworks:leg_controller",
                   f"legworks clickrel {X+2} {y} {Z+2} 2 2 2 1 up"]
    out += [f"legworks clickrel {X+2} {y} {Z+2} 0 0 0 0 down", f"legworks watch {X+2.5} {y} {Z+2.5} one_link 100", "#sleep 6"]
    out += [f"legworks clickrel {X+2} {y} {Z+2} {dx} 0 {dz} 0 down" for dx, dz in CORNERS[1:]]
    out += ["#sleep 8", f"legworks status {X+2.5} {y} {Z+2.5} click_ship"]
    # B: limits, other Ship, world Leg, Simple Leg
    X = 130
    out += [f"fill {X-3} 148 {Z-3} {X+21} 150 {Z+18} minecraft:stone",
            f"fill {X} {y+1} {Z} {X+18} {y+1} {Z+2} minecraft:iron_block"]
    out += [f"setblock {X+i} {y} {Z+1} {HMOUNT}" for i in list(range(9)) + [18]]
    out += [f"setblock {X+10} {y} {Z+1} legworks:simple_leg_mount[facing=down]", f"setblock {X} {y+2} {Z+1} legworks:leg_controller",
            f"fill {X+5} {y+1} {Z+5} {X+7} {y+1} {Z+7} minecraft:iron_block", f"setblock {X+6} {y} {Z+6} {HMOUNT}",
            f"setblock {X+9} 151 {Z+16} {HMOUNT}",
            f"legworks mark limits.{X}", f"sable assemble area {X} {y} {Z} {X+18} {y+2} {Z+2}", f"sable assemble area {X+5} {y} {Z+5} {X+7} {y+1} {Z+7}", "#sleep 3",
            "legworks hold legworks:leg_controller", f"legworks clickrel {X+9} {y} {Z+1} 0 2 1 1 up"]
    out += [f"legworks clickrel {X+9} {y} {Z+1} {i} 0 1 0 down" for i in range(9)]
    out += [f"legworks clickrel {X+9} {y} {Z+1} 0 0 1 0 down", f"legworks clickrel {X+9} {y} {Z+1} 18 0 1 0 down",
            f"legworks clickrel {X+6} {y} {Z+6} 1 0 1 0 down", f"legworks click {X+9.5} 151.5 {Z+16.5} 0 down",
            f"legworks clickrel {X+9} {y} {Z+1} 10 0 1 0 down", f"legworks clickrel {X+9} {y} {Z+1} 0 0 1 0 down",
            "#sleep 1", f"legworks status {X+9} {y} {Z+1} limits"]
    # C: pending links in the world, place the Controller, assemble, fly (reload in part b)
    X = 180
    cmds, asm = ship5h(X, ["hn"] * 4, controllers=[])
    out += cmds + [f"legworks mark pend.{X}", "legworks hold legworks:leg_controller"]
    out += [f"legworks click {X+dx+0.5} {y+0.5} {Z+dz+0.5} 0 down" for dx, dz in CORNERS]
    out += [f"legworks click {X+2.5} {y+1.5} {Z+2.5} 0 up", "#sleep 1", f"legworks statusw {X+2.5} {y} {Z+2.5} pend_world", asm, "#sleep 8",
            f"legworks status {X+2.5} {y} {Z+2.5} pend_assembled", drop(X, y, 4), f"legworks push {centre(X, y)} 30 0 20", "#sleep 12",
            f"legworks status {X+2.5} {y} {Z+2.5} pend_flown"]
    # D: pre-linked, assemble without the Controller
    X = 210
    cmds, asm = ship5h(X, ["h"] * 4)
    out += cmds + [f"legworks mark partial.{X}", f"sable assemble area {X} {y} {Z} {X+4} {y+1} {Z+4}", "#sleep 5",
                   f"legworks status {X+2.5} {y} {Z+2.5} partial", f"legworks statusw {X+2.5} {y+2.5} {Z+2.5} partial_ctrl"]
    # E: disassembly (Simulated), with and without an extra quarter turn
    for k, X in enumerate((240, 270)):
        cmds, asm = ship5h(X, ["h"] * 4)
        out += cmds + [f"legworks mark disasm{k}.{X}", asm, "#sleep 8", f"legworks disassemble {centre(X, y)} {k}", "#sleep 3",
                       f"legworks status {X+2.5} {y} {Z+2.5} disasm{k}"]
    # F: Create wrench on a Hydraulic Mount (world): rotate twice, then sneak-wrench to pick it up
    X = 300
    out += [f"fill {X-3} 148 {Z-3} {X+7} 150 {Z+7} minecraft:stone", f"setblock {X} 152 {Z} {HMOUNT}{{Limb:{{id:\"legworks:short_limb\",count:1}}}}",
            "legworks hold create:wrench", f"legworks click {X+0.5} 152.5 {Z+0.5} 0 north", f"legworks click {X+0.5} 152.5 {Z+0.5} 0 east",
            f"legworks statusw {X+0.5} 152 {Z+0.5} wrench", f"setblock {X+4} 152 {Z} legworks:simple_leg_mount[facing=down]",
            f"legworks click {X+4.5} 152.5 {Z+0.5} 0 north", f"legworks statusw {X+4.5} 152 {Z+0.5} wrench_simple", f"legworks click {X+0.5} 152.5 {Z+0.5} 1 up", "#sleep 1", f"legworks statusw {X+0.5} 152 {Z+0.5} wrench_removed"]
    out += ["#sleep 2", "save-all flush", "#sleep 2", "legworks mark end", "legworks trace 0", "stop"]
    return out


def p2_link_b():
    X, y = 180, 154
    return ["legworks trace 1", "#sleep 8", f"legworks status {X+2.5} {y} {Z+2.5} pend_reloaded", "legworks mark end", "legworks trace 0", "stop"]


def p2_show():
    out = ["forceload add 90 -16 180 16", "#sleep 6"]
    for X, legs, ctrl in ((100, ["hn"] * 4, [(2, 2, None, [])]), (120, ["h"] * 4, None), (140, ["h"] * 4, None)):
        cmds, asm = ship5h(X, legs, controllers=ctrl)
        out += cmds + [asm]
    out += ["setblock 102 151 -3 minecraft:stone", "setblock 122 151 -3 minecraft:stone"]
    out += ["fill 157 148 -3 167 150 7 minecraft:stone", f"setblock 160 151 0 {HMOUNT}", "op Dev", "#sleep 1"]
    return out


def p3_status():
    H = ["h"] * 4
    lanes = [("p3_flat", lambda X: ship5h(X, H)),
             ("p3_bottomless", lambda X: ship5h(X, H, "bottomless")),
             ("p3_bottomless_simple", lambda X: ship5h(X, ["s"] * 4, "bottomless")),
             ("p3_hole1", lambda X: ship5h(X, H, "hole1"))]
    out = wrap(lanes, 25, 4, spacing=20)
    disp = ["setblock 100 152 60 legworks:leg_controller",
            'setblock 100 152 61 create:display_link[facing=south]{TargetOffset:[I;0,0,1],TargetLine:0,Source:{Id:"legworks:leg_controller_status"}}',
            "setblock 110 152 60 legworks:simple_leg_mount[facing=down]{Limb:{id:\"legworks:short_limb\",count:1}}",
            'setblock 110 152 61 create:display_link[facing=south]{TargetOffset:[I;0,0,1],TargetLine:0,Source:{Id:"legworks:leg_mount_status"}}']
    for bx in (99, 109):
        disp += [f"setblock {x} 152 62 create:display_board[facing=south]" for x in (bx, bx + 1, bx + 2)]
        disp += [f"setblock {bx + 3} 152 62 create:cogwheel[axis=z]", f"setblock {bx + 3} 152 61 create:creative_motor[facing=south]{{ScrollValue:32}}"]
    out[2:2] = ["forceload add 90 30 120 70", "legworks recipes", "legworks assemblyline 100 160 40"] + disp
    i = out.index("legworks mark end")
    xs = [100 + 20 * k for k in range(len(lanes))]
    return out[:i] + [f"legworks status {centre(X, 154)} {n}" for (n, _), X in zip(lanes, xs)] + ["legworks assemblycheck 100 160 40", "data get block 99 152 62", "data get block 109 152 62"] + out[i:]


if __name__ == "__main__":
    for name, fn in (("r3", r3), ("r4", r4), ("r5", r5), ("limbs", limbs), ("remove", remove), ("reload_a", reload_a), ("reload_b", reload_b), ("pitdrops", pitdrops), ("pit20", pit20), ("p2_phys", p2_phys), ("p2_power", p2_power), ("p2_link_a", p2_link_a), ("p2_link_b", p2_link_b), ("p2-show", p2_show), ("p3_status", p3_status)):
        with open(os.path.join(HERE, f"scenario-{name}.txt"), "w") as f:
            f.write("\n".join(fn()) + "\n")
        print("wrote", name)
    for name in ("p2_phys", "p2_power", "p2_link_a"):
        src = open(os.path.join(HERE, f"scenario-{name}.txt")).read().split("\n")
        xs = [int(l.split(".")[-1]) for l in src if l.startswith("legworks mark ") and l.split(".")[-1].isdigit()]
        mid = (min(xs) + max(xs)) // 2 if xs else 150
        out = [l for l in src if l.strip() != "stop"]
        out[2:2] = ["gamemode spectator Dev", f"tp Dev {mid} 175 -25 0 30"]
        with open(os.path.join(HERE, f"scenario-{name}-client.txt"), "w") as f:
            f.write("\n".join(out) + "\n#signal done\n")
        print("wrote", name + "-client")
