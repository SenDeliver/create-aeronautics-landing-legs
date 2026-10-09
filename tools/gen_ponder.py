"""Builds the Ponder scene schematics in a dedicated-server world and exports them with /legworks schematic.
Run: python3 tools/gen_ponder.py, then
tools/run_headless.sh tools/scenario-ponder_export.txt tools/logs/ponder-export.log -Plegworks.ponderOut=$PWD/src/main/resources/assets/legworks/ponder"""
import os

HERE = os.path.dirname(os.path.abspath(__file__))
Y = 150
Z0 = 300
LIMB = '{id:"legworks:short_limb",count:1}'


def base(X, size):
    out = [f"fill {X-1} {Y-1} {Z0-1} {X+size} {Y+8} {Z0+size} minecraft:air", f"fill {X} {Y-1} {Z0} {X+size-1} {Y-1} {Z0+size-1} minecraft:barrier"]
    for x in range(size):
        for z in range(size):
            b = "minecraft:white_concrete" if (x + z) % 2 == 0 else "minecraft:snow_block"
            out.append(f"setblock {X+x} {Y} {Z0+z} {b}")
    return out


def at(X, x, y, z, block):
    return f"setblock {X+x} {Y+y} {Z0+z} {block}"


def fill(X, a, b, block):
    return f"fill {X+a[0]} {Y+a[1]} {Z0+a[2]} {X+b[0]} {Y+b[1]} {Z0+b[2]} {block}"


def export(name, X, size, height):
    return f"legworks schematic {name} {X} {Y} {Z0} {X+size-1} {Y+height} {Z0+size-1}"


def fitting(X):
    out = base(X, 5)
    out.append(fill(X, (1, 5, 1), (3, 5, 3), "minecraft:spruce_planks"))
    out.append(at(X, 2, 4, 1, "legworks:simple_leg_mount[facing=down]"))
    return out + ["#sleep 1", export("simple_leg_fitting", X, 5, 6)]


def leveling(X):
    out = base(X, 7)
    out.append(fill(X, (5, 1, 0), (6, 1, 6), "minecraft:stone"))
    out.append(fill(X, (1, 4, 2), (5, 4, 4), "minecraft:spruce_planks"))
    for x, z in ((1, 2), (5, 2), (1, 4), (5, 4)):
        out.append(at(X, x, 3, z, "legworks:simple_leg_mount[facing=down]{Limb:%s}" % LIMB))
    return out + ["#sleep 1", export("simple_leg_leveling", X, 7, 6)]


def facing(X):
    out = base(X, 7)
    out.append(fill(X, (6, 1, 1), (6, 5, 5), "minecraft:stone_bricks"))
    out.append(fill(X, (1, 4, 2), (3, 4, 4), "minecraft:spruce_planks"))
    out.append(at(X, 2, 3, 2, "legworks:simple_leg_mount[facing=down]{Limb:%s}" % LIMB))
    out.append(at(X, 4, 4, 3, "legworks:simple_leg_mount[facing=east]{Limb:%s}" % LIMB))
    return out + ["#sleep 1", export("simple_leg_facing", X, 7, 7)]


def ship(X):
    out = base(X, 7)
    out.append(fill(X, (1, 4, 2), (5, 4, 4), "minecraft:spruce_planks"))
    cx, cy, cz = 3, 5, 3
    mounts = ((1, 2), (5, 2), (1, 4), (5, 4))
    links = ",".join(f"[I;{x-cx},{3-cy},{z-cz}]" for x, z in mounts)
    for x, z in mounts:
        out.append(at(X, x, 3, z, "legworks:hydraulic_leg_mount[facing=down,axis_along_first=true]{Limb:%s,Controller:[I;%d,%d,%d]}" % (LIMB, cx - x, cy - 3, cz - z)))
        mx, f = (x + 1, "west") if x == 1 else (x - 1, "east")
        out.append(at(X, mx, 3, z, "create:creative_motor[facing=%s]{ScrollValue:16}" % f))
        out.append(at(X, x, 5, z, "minecraft:lever[face=floor,facing=north]"))
    out.append(at(X, cx, cy, cz, "legworks:leg_controller{Links:[%s]}" % links))
    out.append(at(X, cx + 1, cy, cz, "minecraft:lever[face=floor,facing=east]"))
    return out + ["#sleep 2", export("hydraulic_leg_ship", X, 7, 6)]


def scenario():
    out = ["forceload add 290 290 380 310", "#sleep 5"]
    for fn, X in ((fitting, 300), (leveling, 320), (facing, 340), (ship, 360)):
        out += fn(X)
    return out + ["#sleep 1", "stop"]


if __name__ == "__main__":
    with open(os.path.join(HERE, "scenario-ponder_export.txt"), "w") as f:
        f.write("\n".join(scenario()) + "\n")
    print("wrote ponder_export")
