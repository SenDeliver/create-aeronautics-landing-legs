from PIL import Image
import math, os

ROOT = os.path.join(os.path.dirname(__file__), "..")
OUT = os.path.join(ROOT, "src/main/resources/assets/legworks/textures/block")
ITEM_OUT = os.path.join(ROOT, "src/main/resources/assets/legworks/textures/item")
SHOTS = os.path.join(os.path.dirname(__file__), "shots")
os.makedirs(OUT, exist_ok=True)
os.makedirs(ITEM_OUT, exist_ok=True)


def h(s):
    s = s.lstrip("#")
    return (int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16), 255)


CLEAR = (0, 0, 0, 0)


def img(fn):
    im = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            im.putpixel((x, y), fn(x, y))
    return im


def save(name, fn):
    img(fn).save(os.path.join(OUT, name + ".png"))
    print("wrote", name)


# --- group1 ---

def ramp(x, cols):
    """cols: colours for texel columns 4..11 around the centre (face UVs are centred on u=8)."""
    return cols[max(0, min(len(cols) - 1, x - 4))]


def simple_leg_mount_side(x, y):
    if x == 1:
        return h("#9aa49d")
    if x == 14:
        return h("#505351")
    if 3 <= y <= 12:
        if y in (3, 12) and 4 <= x <= 11:
            return h("#82613a")
        if 3 <= x <= 12:
            if y in (4, 6, 8, 10) and 5 <= x <= 10:
                return h("#d8d8d8")
            if y in (5, 7, 9, 11) and x in (5, 10):
                return h("#585858")
            if x == 7:
                return h("#a9afa1")
            if x == 8:
                return h("#6d7d73")
            return h("#2b3635")
    return h("#553a1f")


def simple_leg_mount_socket(x, y):
    if x in (0, 15) or y in (0, 15):
        return h("#505351")
    if y == 14 or x == 14:
        return h("#60635f")
    if y == 1 or x == 1:
        return h("#a8b3ab")
    if (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        return h("#c9caba")
    if (x - 1, y - 1) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        return h("#505351")
    r = math.hypot(x - 7.5, y - 7.5)
    if r < 2.5:
        return h("#000000")
    if r < 3.5:
        return h("#a8b3ab") if (x - 7.5) + (y - 7.5) < 0 else h("#828784")
    if r < 4.3:
        return h("#505351")
    if y in (5, 10):
        return h("#5a4424")
    return h("#70522e")


def limb_short_rod(x, y):
    c = ramp(x, ["#c9caba", "#a9afa1", "#a9afa1", "#829789", "#829789", "#4a5451", "#4a5451", "#374141"])
    if y % 8 == 7:
        return h("#374141") if x >= 9 else h("#5e6963")
    return h(c)


def limb_short_spring(x, y):
    row = y % 4
    if row == 3:
        return h("#505351")
    if row == 0:
        return h(ramp(x, ["#cdcdcd", "#b8b8b8", "#a5a5a5", "#a5a5a5", "#a5a5a5", "#969696", "#868686", "#707070"]))
    if row == 1:
        return h(ramp(x, ["#a5a5a5", "#969696", "#868686", "#868686", "#868686", "#7a7a7a", "#707070", "#606060"]))
    return h(ramp(x, ["#868686", "#7a7a7a", "#707070", "#707070", "#707070", "#606060", "#5a5a5a", "#505351"]))


def pin_joint(x, y, ring, hole, frame):
    r = math.hypot(x - 7.5, y - 7.5)
    if r < 2:
        return h(hole)
    if r < 3:
        return h(ring)
    d = max(abs(x - 7.5), abs(y - 7.5))
    if d > 7:
        return h(frame[0])
    if d > 6:
        return h(frame[1]) if x - 7.5 + y - 7.5 < 0 else h(frame[4])
    if x < 6 and y < 6 or (x + y) < 11:
        return h(frame[2])
    return h(frame[3])


def limb_short_joint(x, y):
    return pin_joint(x, y, "#c9caba", "#2b3635", ["#505351", "#a8b3ab", "#9aa49d", "#828784", "#60635f"])


def limb_short_foot(x, y):
    if y == 0:
        return h("#6d7d73")
    if y % 3 == 0:
        return h("#2b3635")
    return h("#4a5451") if y % 3 == 1 else h("#374141")


def limb_long_plate(x, y):
    if y == 15:
        return h("#76462d")
    if x == 8 and y in (3, 11):
        return h("#fff9c7")
    if y == 0:
        return h("#ffeb94") if x <= 8 else h("#d3a155")
    return h(ramp(x, ["#ffeb94", "#fbcc68", "#fbcc68", "#fbcc68", "#d3a155", "#995a3d", "#995a3d", "#76462d"]))


def limb_long_cog(x, y):
    dx, dy = x - 7.5, y - 7.5
    r = math.hypot(dx, dy)
    a = math.degrees(math.atan2(dy, dx)) % 360
    tooth = 5.5 < r <= 7.3 and min(abs(((a - 22.5) % 45) - 22.5), 45) < 9
    if r > 6 and not tooth:
        return CLEAR
    if tooth and r > 6:
        return h("#d3a155") if dx + dy < 0 else h("#995a3d")
    if r < 2:
        return h("#602f24")
    if r > 5:
        return h("#ffeb94") if dx + dy < -2 else (h("#b57849") if dx + dy > 2 else h("#d3a155"))
    if r > 4.2:
        return h("#995a3d")
    if abs(dx) < 1 or abs(dy) < 1:
        return h("#b57849")
    return h("#fbcc68") if dx + dy < 0 else h("#d3a155")


def limb_long_sleeve(x, y):
    if y % 6 == 0:
        return h("#d3a155") if x < 9 else h("#b57849")
    if y % 6 == 5:
        return h("#602f24") if x >= 9 else h("#76462d")
    return h(ramp(x, ["#b57849", "#b57849", "#995a3d", "#995a3d", "#995a3d", "#76462d", "#76462d", "#602f24"]))


def limb_long_rod(x, y):
    if y % 16 == 15:
        return h("#a8a8a8") if x < 9 else h("#727272")
    return h(ramp(x, ["#a8a8a8", "#d8d8d8", "#d8d8d8", "#ffffff", "#d8d8d8", "#a8a8a8", "#727272", "#585858"]))


def limb_long_joint(x, y):
    return pin_joint(x, y, "#fff9c7", "#301e0e", ["#724731", "#ffeb8c", "#f7cb6c", "#cea05a", "#9e6947"])


def limb_long_foot(x, y):
    if y == 0:
        return h("#d3a155")
    if y == 1:
        return h("#b57849")
    if y == 2:
        return h("#76462d")
    if y % 4 == 3:
        return h("#2b2b2e")
    return h("#3f3e42") if (x + y) % 5 else h("#494848")


def limb_heavy_strut(x, y):
    if y % 8 == 3 and x in (6, 9):
        return h("#989797")
    if y % 8 == 4 and x in (6, 9):
        return h("#2b2b2e")
    return h(ramp(x, ["#989797", "#757171", "#757171", "#676161", "#595858", "#494848", "#3f3e42", "#353438"]))


def limb_heavy_armor(x, y):
    if x <= 4:
        return h("#8e8da1") if x == 4 else h("#706e82")
    if x >= 11:
        return h("#191b24") if x == 11 else h("#0b0d15")
    if x in (5, 10) and y in (2, 13):
        return h("#706e82")
    if (x + y) % 4 == 0:
        return h("#434251")
    return h("#525161") if x < 8 else h("#383846")


def limb_heavy_joint(x, y):
    return pin_joint(x, y, "#525161", "#03050c", ["#0b0d15", "#8e8da1", "#706e82", "#434251", "#2a2a37"])


def limb_heavy_foot(x, y):
    if x % 4 in (1, 2) and y % 4 in (1, 2):
        return h("#706e82") if x % 4 == 1 and y % 4 == 1 else h("#525161")
    if y % 4 == 3 or x % 4 == 3:
        return h("#191b24")
    return h("#2a2a37")


for name, fn in (("simple_leg_mount_side", simple_leg_mount_side), ("simple_leg_mount_socket", simple_leg_mount_socket),
                 ("limb_short_rod", limb_short_rod), ("limb_short_spring", limb_short_spring),
                 ("limb_short_joint", limb_short_joint), ("limb_short_foot", limb_short_foot),
                 ("limb_long_plate", limb_long_plate), ("limb_long_cog", limb_long_cog), ("limb_long_sleeve", limb_long_sleeve),
                 ("limb_long_rod", limb_long_rod), ("limb_long_joint", limb_long_joint), ("limb_long_foot", limb_long_foot),
                 ("limb_heavy_strut", limb_heavy_strut), ("limb_heavy_armor", limb_heavy_armor),
                 ("limb_heavy_joint", limb_heavy_joint), ("limb_heavy_foot", limb_heavy_foot)):
    save(name, fn)


class Icon:
    def __init__(self):
        self.mat = [[None] * 16 for _ in range(16)]

    def line(self, a, b, mat):
        n = max(abs(b[0] - a[0]), abs(b[1] - a[1]))
        for i in range(n + 1):
            self.put(round(a[0] + (b[0] - a[0]) * i / n), round(a[1] + (b[1] - a[1]) * i / n), mat)

    def put(self, x, y, mat):
        if 0 <= x < 16 and 0 <= y < 16:
            self.mat[y][x] = mat

    def clear(self, x, y):
        if 0 <= x < 16 and 0 <= y < 16:
            self.mat[y][x] = None

    def seg(self, a, b, w, mat, t0=0.0, t1=1.0, offset=0.0):
        ax, ay = a
        bx, by = b
        dx, dy = bx - ax, by - ay
        ln = math.hypot(dx, dy)
        nx, ny = -dy / ln, dx / ln
        ax, ay = ax + nx * offset, ay + ny * offset
        for y in range(16):
            for x in range(16):
                px, py = x - ax, y - ay
                t = (px * dx + py * dy) / (ln * ln)
                if t < t0 - 0.5 / ln or t > t1 + 0.5 / ln:
                    continue
                t = max(t0, min(t1, t))
                dist = math.hypot(px - dx * t, py - dy * t)
                if dist < w / 2:
                    self.put(x, y, mat)

    def rect(self, x0, y0, x1, y1, mat):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, mat)

    def filled(self, x, y):
        return 0 <= x < 16 and 0 <= y < 16 and self.mat[y][x] is not None

    def render(self, outline):
        im = Image.new("RGBA", (16, 16), CLEAR)
        for y in range(16):
            for x in range(16):
                m = self.mat[y][x]
                if m is None:
                    continue
                if isinstance(m, str):
                    im.putpixel((x, y), h(m))
                    continue
                light = not self.filled(x, y - 1) or not self.filled(x - 1, y)
                dark = not self.filled(x, y + 1) or not self.filled(x + 1, y)
                im.putpixel((x, y), h(m[1] if light else m[2] if dark else m[0]))
        for y in range(16):
            for x in range(16):
                if self.mat[y][x] is None and any(self.filled(x + a, y + b) for a, b in ((1, 0), (-1, 0), (0, 1), (0, -1))):
                    im.putpixel((x, y), h(outline))
        return im


ALLOY = ("#829789", "#a9afa1", "#4a5451")
ALLOY_DARK = ("#4a5451", "#5e6963", "#374141")
IRON = ("#a8a8a8", "#d8d8d8", "#727272")
BRASS = ("#d3a155", "#fbcc68", "#995a3d")
BRASS_DARK = ("#995a3d", "#b57849", "#76462d")
STURDY = ("#434251", "#525161", "#2a2a37")
STURDY_DARK = ("#2a2a37", "#383846", "#191b24")


def short_icon():
    ic = Icon()
    hip, kn, ft = (4, 1), (10, 7), (5, 13)
    ic.rect(3, 1, 5, 1, ALLOY_DARK)
    ic.seg(hip, kn, 2.2, ALLOY)
    for y in range(16):
        for x in range(16):
            px, py = x - hip[0], y - hip[1]
            dx, dy = kn[0] - hip[0], kn[1] - hip[1]
            t = (px * dx + py * dy) / (dx * dx + dy * dy)
            if 0.28 <= t <= 0.72 and abs(px * dy - py * dx) / math.hypot(dx, dy) < 2.0:
                ic.put(x, y, "#d8d8d8" if y % 2 == 0 else "#585858")
    ic.seg(kn, (ft[0], ft[1] - 1), 2.2, ALLOY)
    ic.rect(9, 6, 11, 8, "#c9caba")
    ic.put(10, 7, "#2b3635")
    ic.put(11, 8, "#a9afa1")
    ic.rect(3, 13, 7, 13, "#4a5451")
    ic.rect(3, 14, 7, 14, "#374141")
    return ic.render("#2b3635")


def long_icon():
    ic = Icon()
    hip, kn, ft = (3, 0), (12, 7), (4, 15)
    ic.rect(2, 0, 4, 0, BRASS_DARK)
    for i in range(kn[0] - hip[0] + 1):
        x = hip[0] + i
        y = hip[1] + round(i * (kn[1] - hip[1]) / (kn[0] - hip[0]))
        ic.put(x, y - 1, "#fbcc68")
        ic.put(x, y + 1, "#d3a155")
    ic.seg(kn, ft, 3.2, BRASS_DARK, 0.0, 0.5)
    ic.line(((kn[0] + ft[0]) // 2, (kn[1] + ft[1]) // 2), (ft[0] + 1, ft[1] - 2), "#d8d8d8")
    for y in range(16):
        for x in range(16):
            r = math.hypot(x - kn[0], y - kn[1])
            if r < 2.6:
                ic.put(x, y, "#fbcc68" if x + y < kn[0] + kn[1] else "#d3a155")
    for x, y in ((kn[0], kn[1] - 3), (kn[0], kn[1] + 3), (kn[0] - 3, kn[1]), (kn[0] + 3, kn[1])):
        ic.put(x, y, "#b57849")
    ic.put(kn[0], kn[1], "#602f24")
    ic.put(kn[0] - 1, kn[1] - 1, "#ffeb94")
    ic.rect(2, 14, 5, 14, "#b57849")
    ic.rect(2, 15, 5, 15, "#3f3e42")
    return ic.render("#602f24")


def heavy_icon():
    ic = Icon()
    hip, kn, ft = (4, 1), (10, 7), (6, 13)
    ic.rect(2, 1, 6, 1, STURDY_DARK)
    ic.seg(hip, kn, 4.0, STURDY)
    ic.seg(hip, kn, 1.0, "#706e82", 0.15, 0.85, 1.4)
    ic.seg(kn, (ft[0], ft[1] - 1), 3.0, STURDY_DARK)
    ic.rect(8, 5, 12, 9, "#383846")
    ic.rect(8, 5, 12, 5, "#706e82")
    ic.rect(8, 5, 8, 9, "#525161")
    ic.rect(12, 6, 12, 9, "#191b24")
    ic.rect(9, 9, 12, 9, "#191b24")
    ic.put(10, 7, "#8e8da1")
    ic.rect(2, 13, 10, 13, "#525161")
    ic.rect(2, 14, 10, 14, "#2a2a37")
    for x in (3, 6, 9):
        ic.clear(x, 14)
    return ic.render("#03050c")


icons = (("short_limb", short_icon()), ("long_limb", long_icon()), ("heavy_limb", heavy_icon()))
for name, im in icons:
    im.save(os.path.join(ITEM_OUT, name + ".png"))
    print("wrote", name)

os.makedirs(SHOTS, exist_ok=True)
sheet = Image.new("RGBA", (16 * 8 * 3 + 32 * 4, 16 * 8 + 64), h("#c6c6c6"))
for i, (name, im) in enumerate(icons):
    sheet.alpha_composite(im.resize((128, 128), Image.NEAREST), (32 + i * (128 + 32), 32))
sheet.save(os.path.join(SHOTS, "p4_icons_limbs_preview.png"))
print("wrote p4_icons_limbs_preview")


# --- group2 ---

def hydraulic_side(x, y, complete=True):
    if 7 <= x <= 8 and 3 <= y <= 12:
        if not complete:
            return h("#000000")
        if y in (3, 9):
            return h("#cea05a")
        if y <= 9:
            return h("#d8d8d8") if x == 7 else h("#727272")
        return h("#ffffff") if x == 7 else h("#a8a8a8")
    if complete and y == 2 and 3 <= x <= 7:
        return h("#d3a155") if x % 2 else h("#b57849")
    if not complete and ((y == 2 and 3 <= x <= 5) or (x == 6 and y in (3, 4))):
        return h("#d3a155") if (x + y) % 2 else h("#b57849")
    if x == 1:
        return h("#f7cb6c")
    if x == 14:
        return h("#724731")
    if 5 <= y <= 10 and (2 <= x <= 6 or 9 <= x <= 13):
        if y in (5, 10) or x in (2, 6, 9, 13):
            return h("#9e6947")
        return h("#301e0e")
    return h("#3a2411")


def hydraulic_leg_mount_socket(x, y):
    if x in (0, 15) or y in (0, 15):
        return h("#724731")
    if x == 1 or y == 1:
        return h("#ffeb8c")
    if x == 14 or y == 14:
        return h("#9e6947")
    if (x, y) in ((3, 3), (12, 3), (3, 12), (12, 12)):
        return h("#ffeb94")
    r = math.hypot(x - 7.5, y - 7.5)
    if r < 1.8:
        return h("#ffffff") if (x, y) == (7, 6) else h("#d8d8d8")
    if r < 2.8:
        return h("#000000")
    if r < 3.8:
        return h("#f7cb6c") if (x - 7.5) + (y - 7.5) < 0 else h("#cea05a")
    if r < 4.5:
        return h("#724731")
    return h("#442b15")


def hydraulic_rod(x, y):
    if y % 8 == 7:
        return h("#a8a8a8")
    d = int(abs(x - 7.5) - 0.5)
    return h(("#727272", "#a8a8a8", "#ffffff", "#d8d8d8", "#585858")[d % 5])


for name, fn in (("hydraulic_leg_mount_side", hydraulic_side),
                 ("hydraulic_leg_mount_side_incomplete", lambda x, y: hydraulic_side(x, y, False)),
                 ("hydraulic_leg_mount_socket", hydraulic_leg_mount_socket), ("hydraulic_rod", hydraulic_rod)):
    save(name, fn)


# --- group3 ---

def leg_controller_side(x, y):
    if x == 1:
        return h("#f7cb6c")
    if x == 14:
        return h("#724731")
    if y in (4, 5):
        for lx in (3, 6, 9, 12):
            if lx <= x <= lx + 1:
                return h("#5ad8e6") if (x, y) == (lx, 4) else h("#1b3a44")
    if 8 <= y <= 10 and 3 <= x <= 12:
        if y == 8:
            return h("#ffeb8c")
        if y == 10:
            return h("#9e6947")
        if x in (4, 8, 11):
            return h("#602f24")
        return h("#cea05a")
    if y == 12:
        return h("#301e0e")
    return h("#3a2411")


def leg_controller_top(x, y):
    if x in (0, 15) or y in (0, 15):
        return h("#724731")
    if x == 1 or y == 1:
        return h("#f7cb6c")
    if x == 14 or y == 14:
        return h("#9e6947")
    if 5 <= x <= 10 and 5 <= y <= 10:
        if 7 <= x <= 8 and 7 <= y <= 8:
            return h("#f7cb6c")
        if x == 5 or y == 5:
            return h("#724731")
        if x == 10 or y == 10:
            return h("#cea05a")
        return h("#1b1b1b")
    if x in (3, 4, 11, 12) and y in (3, 4, 11, 12):
        return h("#5ad8e6")
    if 3 <= x <= 12 and 3 <= y <= 12 and (x in (3, 12) or y in (3, 12)):
        return h("#9e6947")
    return h("#301e0e")


for name, fn in (("leg_controller_side", leg_controller_side), ("leg_controller_top", leg_controller_top)):
    save(name, fn)
