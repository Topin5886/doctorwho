"""
Générateur des modèles GeckoLib (.geo.json) et de leurs textures (.png) du mod Doctor Who.

Pourquoi un script ? Pour avoir des modèles de départ propres sans Blockbench.
Ils pourront être remplacés plus tard par des modèles faits à la main dans Blockbench
(mêmes noms de fichiers, mêmes noms d'os : root, left_door, right_door, lamp, emitter).

Utilisation (depuis la racine du projet) :
    python tools/generate_assets.py

Python 3 pur, aucune dépendance.
"""
import json
import math
import os
import random
import struct
import zlib

ROOT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "doctorwho")


# ---------------------------------------------------------------------------
# Image RGBA minimale + écriture PNG
# ---------------------------------------------------------------------------
class Image:
    def __init__(self, w, h):
        self.w, self.h = w, h
        self.px = bytearray(w * h * 4)

    def set(self, x, y, c):
        if 0 <= x < self.w and 0 <= y < self.h:
            i = (y * self.w + x) * 4
            self.px[i:i + 4] = bytes(c if len(c) == 4 else (*c, 255))

    def rect(self, x0, y0, x1, y1, c):
        for y in range(int(y0), int(y1)):
            for x in range(int(x0), int(x1)):
                self.set(x, y, c)

    def frame(self, x0, y0, x1, y1, c, t=1):
        self.rect(x0, y0, x1, y0 + t, c)
        self.rect(x0, y1 - t, x1, y1, c)
        self.rect(x0, y0, x0 + t, y1, c)
        self.rect(x1 - t, y0, x1, y1, c)

    def save(self, path):
        raw = b"".join(b"\x00" + bytes(self.px[y * self.w * 4:(y + 1) * self.w * 4]) for y in range(self.h))

        def chunk(tag, data):
            return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

        os.makedirs(os.path.dirname(path), exist_ok=True)
        with open(path, "wb") as f:
            f.write(b"\x89PNG\r\n\x1a\n")
            f.write(chunk(b"IHDR", struct.pack(">IIBBBBB", self.w, self.h, 8, 6, 0, 0, 0)))
            f.write(chunk(b"IDAT", zlib.compress(raw, 9)))
            f.write(chunk(b"IEND", b""))


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c[:3])


def noisy(img, x0, y0, x1, y1, c, amount, seed):
    rnd = random.Random(seed)
    for y in range(int(y0), int(y1)):
        for x in range(int(x0), int(x1)):
            img.set(x, y, shade(c, 1 + rnd.uniform(-amount, amount)))


# Police 3x5 pour les inscriptions
FONT = {
    "P": ["111", "101", "111", "100", "100"], "O": ["111", "101", "101", "101", "111"],
    "L": ["100", "100", "100", "100", "111"], "I": ["111", "010", "010", "010", "111"],
    "C": ["111", "100", "100", "100", "111"], "E": ["111", "100", "110", "100", "111"],
    "B": ["110", "101", "110", "101", "110"], "X": ["101", "101", "010", "101", "101"],
    " ": ["000"] * 5,
}


def text(img, x, y, s, c):
    for ch in s:
        for row, bits in enumerate(FONT[ch]):
            for col, b in enumerate(bits):
                if b == "1":
                    img.set(x + col, y + row, c)
        x += 4


# ---------------------------------------------------------------------------
# Construction d'un modèle : cubes + UV par face, atlas rempli automatiquement
# ---------------------------------------------------------------------------
FACES = ("north", "south", "east", "west", "up", "down")


def face_dims(size, face):
    w, h, d = size
    return {"north": (w, h), "south": (w, h), "east": (d, h), "west": (d, h), "up": (w, d), "down": (w, d)}[face]


def build(name, bones, scale, atlas_w, painters):
    """bones: liste de dicts {name, parent, pivot, cubes:[(origin, size, {face: material})]}"""
    # 1) placement des faces dans l'atlas (rangées successives)
    placements, x, y, row_h = [], 0, 0, 0
    for bone in bones:
        for cube in bone["cubes"]:
            for face in FACES:
                fw, fh = (round(v * scale) for v in face_dims(cube[1], face))
                fw, fh = max(fw, 1), max(fh, 1)
                if x + fw > atlas_w:
                    x, y, row_h = 0, y + row_h, 0
                placements.append((cube, face, x, y, fw, fh))
                x += fw
                row_h = max(row_h, fh)
    atlas_h = ((y + row_h + 15) // 16) * 16

    tex, glow = Image(atlas_w, atlas_h), Image(atlas_w, atlas_h)
    uv = {}
    for i, (cube, face, fx, fy, fw, fh) in enumerate(placements):
        material = cube[2].get(face, cube[2].get("*"))
        painters[material](tex, glow, fx, fy, fw, fh, scale, face, i)
        uv.setdefault(id(cube), {})[face] = {"uv": [fx, fy], "uv_size": [fw, fh]}

    geo_bones = []
    for bone in bones:
        cubes = []
        for c in bone["cubes"]:
            cube = {"origin": list(c[0]), "size": list(c[1]), "uv": uv[id(c)]}
            if len(c) > 3:  # rotation propre au cube : {"rotation": [x, y, z], "pivot": [x, y, z]}
                cube.update(c[3])
            cubes.append(cube)
        b = {"name": bone["name"], "pivot": bone["pivot"], "cubes": cubes}
        if bone.get("parent"):
            b["parent"] = bone["parent"]
        if bone.get("rotation"):
            b["rotation"] = bone["rotation"]
        geo_bones.append(b)

    geo = {"format_version": "1.12.0", "minecraft:geometry": [{
        "description": {"identifier": "geometry." + name, "texture_width": atlas_w, "texture_height": atlas_h,
                        "visible_bounds_width": 3, "visible_bounds_height": 4, "visible_bounds_offset": [0, 1.5, 0]},
        "bones": geo_bones}]}
    return geo, tex, glow


def write(kind, name, geo, tex, glow):
    path = os.path.join(ROOT, "geo", kind, name + ".geo.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(geo, f, indent=1)
    tex.save(os.path.join(ROOT, "textures", kind, name + ".png"))
    glow.save(os.path.join(ROOT, "textures", kind, name + "_glowmask.png"))
    print("OK", kind, name, tex.w, "x", tex.h)


# ---------------------------------------------------------------------------
# TARDIS (cabine de police, 1 bloc de large, ~2,4 blocs de haut)
# Face avant du modèle = nord (-z). Toutes les dimensions en pixels de bloc.
# ---------------------------------------------------------------------------
BLUE = (28, 62, 120)
BLUE_DARK = (18, 44, 92)
INTERIOR = (201, 138, 61)
ROUNDEL = (243, 208, 138)
GLASS = (226, 236, 246)
WEAR = 0.04      # usure (bruit) de la peinture
BADGE = False    # écusson « St John Ambulance » sur la porte (2010)


def p_blue(tex, glow, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, BLUE, WEAR, seed)


def p_dark(tex, glow, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, BLUE_DARK, WEAR, seed)


def p_post(tex, glow, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, BLUE_DARK, WEAR + 0.01, seed)
    tex.rect(x + w // 2, y, x + w // 2 + 1, y + h, shade(BLUE, 1.15))


def door_half(tex, glow, x, y, w, h, s, plaque):
    """Un vantail : cadre, fenêtre 3x2 carreaux, deux panneaux moulurés."""
    tex.frame(x, y, x + w, y + h, BLUE_DARK, 2)
    # fenêtre
    wx0, wy0, wx1, wy1 = x + s, y + 2 * s, x + w - s, y + 7 * s
    tex.rect(wx0, wy0, wx1, wy1, BLUE_DARK)
    pw, ph = (wx1 - wx0 - 4) // 2, (wy1 - wy0 - 6) // 3
    for r in range(3):
        for c in range(2):
            px0 = wx0 + 1 + c * (pw + 2)
            py0 = wy0 + 1 + r * (ph + 2)
            tex.rect(px0, py0, px0 + pw, py0 + ph, GLASS)
            glow.rect(px0, py0, px0 + pw, py0 + ph, (255, 244, 214))
    # panneaux du bas
    for (a, b) in ((8.5, 15), (16, h / s - 1.5)):
        tex.frame(x + s, y + a * s, x + w - s, y + b * s, BLUE_DARK, 2)
        tex.frame(x + s + 2, y + a * s + 2, x + w - s - 2, y + b * s - 2, shade(BLUE, 1.12), 1)
    if plaque:
        px0, py0, px1, py1 = x + s + 1, y + 9.5 * s, x + w - s - 1, y + 13 * s
        tex.rect(px0, py0, px1, py1, (236, 236, 228))
        for ly in range(int(py0) + 2, int(py1) - 1, 2):
            tex.rect(px0 + 2, ly, px1 - 2, ly + 1, (40, 40, 40))


def p_door_l(tex, glow, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, BLUE, WEAR, seed)
    door_half(tex, glow, x, y, w, h, s, plaque=True)


def p_door_r(tex, glow, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, BLUE, WEAR, seed)
    door_half(tex, glow, x, y, w, h, s, plaque=False)
    if BADGE and face == "north":
        bx, by = x + w // 2 - 5, y + int(10 * s)
        tex.rect(bx, by, bx + 10, by + 10, (235, 235, 230))
        tex.rect(bx + 4, by + 2, bx + 6, by + 8, (20, 20, 20))
        tex.rect(bx + 2, by + 4, bx + 8, by + 6, (20, 20, 20))


def p_panel(tex, glow, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, BLUE, WEAR, seed)
    door_half(tex, glow, x, y, w // 2, h, s, plaque=False)
    door_half(tex, glow, x + w // 2, y, w // 2, h, s, plaque=False)


def p_interior(tex, glow, x, y, w, h, s, face, seed):
    """Intérieur lumineux avec les célèbres « roundels »."""
    tex.rect(x, y, x + w, y + h, INTERIOR)
    glow.rect(x, y, x + w, y + h, INTERIOR)
    step, rad = 6 * s, 2 * s
    for cy in range(y + step // 2, y + h, step):
        for cx in range(x + step // 2, x + w, step):
            for yy in range(cy - rad, cy + rad + 1):
                for xx in range(cx - rad, cx + rad + 1):
                    if x <= xx < x + w and y <= yy < y + h and math.hypot(xx - cx, yy - cy) <= rad:
                        tex.set(xx, yy, ROUNDEL)
                        glow.set(xx, yy, ROUNDEL)


def p_floor(tex, glow, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, (70, 64, 58), 0.08, seed)
    for gy in range(y, y + h, 3):
        tex.rect(x, gy, x + w, gy + 1, (45, 40, 36))


def p_sign(tex, glow, x, y, w, h, s, face, seed):
    tex.rect(x, y, x + w, y + h, (14, 14, 18))
    label = "POLICE BOX"
    tw = len(label) * 4 - 1
    tx, ty = x + (w - tw) // 2, y + (h - 5) // 2
    text(tex, tx, ty, label, (240, 240, 240))
    text(glow, tx, ty, label, (240, 240, 240))


def p_glass(tex, glow, x, y, w, h, s, face, seed):
    tex.rect(x, y, x + w, y + h, (235, 245, 255))
    glow.rect(x, y, x + w, y + h, (235, 245, 255))
    tex.rect(x, y + h // 2, x + w, y + h // 2 + 1, (180, 200, 220))


def p_black(tex, glow, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, (20, 26, 40), 0.05, seed)


def tardis():
    root = []
    # socle
    root.append(((-9, 0, -9), (18, 2, 18), {"*": "dark"}))
    # plancher intérieur
    root.append(((-6, 2, -6), (12, 0.1, 12), {"*": "floor"}))
    # montants d'angle
    for px in (-8, 6):
        for pz in (-8, 6):
            root.append(((px, 2, pz), (2, 28, 2), {"*": "post"}))
    # parois : côtés et arrière (face intérieure = intérieur lumineux)
    # Attention : GeckoLib inverse l'axe X par rapport à Blockbench, donc côté -X la face
    # extérieure est « east » et côté +X c'est « west »
    root.append(((-7, 2, -6), (1, 24, 12), {"*": "blue", "east": "panel", "west": "interior"}))
    root.append(((6, 2, -6), (1, 24, 12), {"*": "blue", "west": "panel", "east": "interior"}))
    root.append(((-6, 2, 6), (12, 24, 1), {"*": "blue", "south": "panel", "north": "interior"}))
    # enseignes « POLICE BOX » sur les 4 côtés
    root.append(((-6, 26, -8), (12, 4, 2), {"*": "dark", "north": "sign", "south": "interior"}))
    root.append(((-6, 26, 6), (12, 4, 2), {"*": "dark", "south": "sign", "north": "interior"}))
    root.append(((-8, 26, -6), (2, 4, 12), {"*": "dark", "east": "sign", "west": "interior"}))
    root.append(((6, 26, -6), (2, 4, 12), {"*": "dark", "west": "sign", "east": "interior"}))
    # toit en gradins
    root.append(((-8, 30, -8), (16, 2, 16), {"*": "dark", "down": "interior"}))
    root.append(((-6.5, 32, -6.5), (13, 1, 13), {"*": "dark"}))
    root.append(((-5, 33, -5), (10, 1, 10), {"*": "blue"}))

    lamp = [
        ((-2, 34, -2), (4, 1, 4), {"*": "black"}),
        ((-1, 35, -1), (2, 3, 2), {"*": "glass", "up": "black", "down": "black"}),
        ((-1.5, 38, -1.5), (3, 1, 3), {"*": "black"}),
    ]
    left = [((-6, 2, -7), (6, 24, 1), {"*": "blue", "north": "door_l", "south": "interior"})]
    right = [((0, 2, -7), (6, 24, 1), {"*": "blue", "north": "door_r", "south": "interior"})]

    bones = [
        {"name": "root", "pivot": [0, 0, 0], "cubes": root},
        {"name": "left_door", "parent": "root", "pivot": [-6, 2, -6.5], "cubes": left},
        {"name": "right_door", "parent": "root", "pivot": [6, 2, -6.5], "cubes": right},
        {"name": "lamp", "parent": "root", "pivot": [0, 34, 0], "cubes": lamp},
    ]
    painters = {"blue": p_blue, "dark": p_dark, "post": p_post, "panel": p_panel, "door_l": p_door_l,
                "door_r": p_door_r, "interior": p_interior, "floor": p_floor, "sign": p_sign,
                "glass": p_glass, "black": p_black}
    geo, _, _ = build("tardis", bones, 4, 512, painters)
    path = os.path.join(ROOT, "geo", "block", "tardis.geo.json")
    with open(path, "w", encoding="utf-8") as f:
        json.dump(geo, f, indent=1)
    # Une texture par variante d'extérieur (même modèle, même disposition UV)
    global BLUE, BLUE_DARK, GLASS, WEAR, BADGE
    variants = {
        "classic": ((44, 76, 138), (30, 56, 108), (236, 240, 244), 0.04, False),
        "tenth": ((28, 62, 120), (18, 44, 92), (226, 236, 246), 0.04, False),
        "eleventh": ((24, 76, 156), (16, 52, 112), (232, 240, 250), 0.03, True),
        "twelfth": ((20, 40, 84), (12, 28, 60), (220, 230, 240), 0.04, False),
        "thirteenth": ((30, 72, 104), (20, 50, 76), (214, 236, 248), 0.04, False),
        "war": ((46, 62, 88), (32, 44, 66), (200, 205, 200), 0.13, False),
        # Années 80 (Cinquième Docteur) : bleu plus clair et plus vif
        "fifth": ((52, 92, 160), (36, 66, 124), (240, 244, 248), 0.03, False),
        # 1996 (Huitième Docteur, le téléfilm) : bleu nuit verni
        "eighth": ((22, 48, 104), (14, 32, 76), (238, 228, 200), 0.02, False),
        # 2023 (Quatorzième Docteur) : bleu profond, peinture neuve
        "fourteenth": ((26, 56, 128), (16, 38, 92), (230, 240, 252), 0.01, False),
    }
    for variant, (blue, dark, glass, wear, badge) in variants.items():
        BLUE, BLUE_DARK, GLASS, WEAR, BADGE = blue, dark, glass, wear, badge
        _, tex, glow = build("tardis", bones, 4, 512, painters)
        tex.save(os.path.join(ROOT, "textures", "block", "tardis", variant + ".png"))
        glow.save(os.path.join(ROOT, "textures", "block", "tardis", variant + "_glowmask.png"))
        print("OK exterieur", variant)
    BLUE, BLUE_DARK, GLASS, WEAR, BADGE = variants["tenth"][0], variants["tenth"][1], variants["tenth"][2], 0.04, False


def tardis_door():
    """Porte intérieure : encadrement + deux vantaux, posée dans le mur de la salle de console.
    Face avant (côté salle) = nord (-z)."""
    frame = [
        ((-8, 0, -1), (2, 30, 2), {"*": "post"}),
        ((6, 0, -1), (2, 30, 2), {"*": "post"}),
        ((-6, 26, -1), (12, 4, 2), {"*": "dark", "north": "sign"}),
        ((-8, 30, -1), (16, 2, 2), {"*": "dark"}),
    ]
    left = [((-6, 0, -0.5), (6, 26, 1), {"*": "blue", "north": "door_l", "south": "door_r"})]
    right = [((0, 0, -0.5), (6, 26, 1), {"*": "blue", "north": "door_r", "south": "door_r"})]
    bones = [
        {"name": "root", "pivot": [0, 0, 0], "cubes": frame},
        {"name": "left_door", "parent": "root", "pivot": [-6, 0, 0], "cubes": left},
        {"name": "right_door", "parent": "root", "pivot": [6, 0, 0], "cubes": right},
    ]
    painters = {"blue": p_blue, "dark": p_dark, "post": p_post, "door_l": p_door_l, "door_r": p_door_r,
                "sign": p_sign}
    write("block", "tardis_door", *build("tardis_door", bones, 4, 256, painters))


# ---------------------------------------------------------------------------
# Console hexagonale avec rotor temporel (centre de la salle de console)
# ---------------------------------------------------------------------------
def hexagon(side, depth_y, y0, material, extra=None):
    """Prisme hexagonal = 3 pavés identiques tournés de 0, 60 et 120 degrés."""
    apothem = side * 0.8660254
    cubes = []
    for angle in (0, 60, 120):
        cube = ((-side / 2, y0, -apothem), (side, depth_y, apothem * 2), {"*": material},
                {"rotation": [0, angle, 0], "pivot": [0, 0, 0]})
        cubes.append(cube)
    return cubes


# ---------------------------------------------------------------------------
# Tournevis sonique du Dixième Docteur (vertical, centré sur l'origine)
# ---------------------------------------------------------------------------
SILVER = (178, 182, 190)


def s_silver(tex, glow, x, y, w, h, s, face, seed):
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            k = 0.85 + 0.3 * math.sin((xx - x) / max(w, 1) * math.pi)
            tex.set(xx, yy, shade(SILVER, k))


def s_grip(tex, glow, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, (30, 30, 34), 0.1, seed)
    if face not in ("up", "down"):
        for gy in range(y + 2, y + h, 4):
            tex.rect(x, gy, x + w, gy + 1, (70, 70, 76))


def s_ringed(tex, glow, x, y, w, h, s, face, seed):
    s_silver(tex, glow, x, y, w, h, s, face, seed)
    if face not in ("up", "down"):
        for gy in range(y + 3, y + h, 5):
            tex.rect(x, gy, x + w, gy + 1, (110, 112, 120))


def s_red(tex, glow, x, y, w, h, s, face, seed):
    tex.rect(x, y, x + w, y + h, (190, 30, 30))


def s_emitter(tex, glow, x, y, w, h, s, face, seed):
    tex.rect(x, y, x + w, y + h, (90, 200, 255))
    glow.rect(x, y, x + w, y + h, (120, 220, 255))
    tex.rect(x + w // 3, y + h // 3, x + 2 * w // 3, y + 2 * h // 3, (220, 248, 255))
    glow.rect(x + w // 3, y + h // 3, x + 2 * w // 3, y + 2 * h // 3, (220, 248, 255))


SONIC_MATERIALS = {
    "silver": ((178, 182, 190), "metal"), "ringed": ((178, 182, 190), "ringed"),
    "grip": ((30, 30, 34), "grip"), "red": ((190, 30, 30), "flat"),
    "gunmetal": ((70, 74, 82), "metal"), "copper": ((184, 115, 51), "metal"),
    "bronze": ((150, 96, 54), "metal"), "bronze_dark": ((96, 60, 36), "ringed"),
    "dark": ((40, 42, 50), "metal"), "black": ((22, 22, 26), "metal"),
    "brass": ((181, 145, 72), "metal"), "wood": ((112, 72, 40), "wood"),
    "gold": ((214, 172, 70), "metal"),
    "glow_blue": ((90, 200, 255), "glow"), "glow_green": ((90, 255, 120), "glow"),
    "glow_violet": ((170, 120, 255), "glow"), "glow_orange": ((255, 150, 60), "glow"),
    "glow_red": ((255, 60, 60), "glow"), "glow_white": ((235, 245, 255), "glow"),
}


def sonic_painter(color, kind):
    def paint(tex, glow, x, y, w, h, s, face, seed):
        side = face not in ("up", "down")
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                k = 0.85 + 0.3 * math.sin((xx - x) / max(w, 1) * math.pi)
                c = shade(color, k if kind in ("metal", "ringed") else 1.0)
                if kind == "wood":
                    c = shade(color, 0.9 + 0.2 * math.sin((xx - x) * 1.7 + yy * 0.2))
                tex.set(xx, yy, c)
                if kind == "glow":
                    glow.set(xx, yy, color)
        if kind == "ringed" and side:
            for gy in range(y + 3, y + h, 5):
                tex.rect(x, gy, x + w, gy + 1, shade(color, 0.6))
        if kind == "grip" and side:
            for gy in range(y + 2, y + h, 4):
                tex.rect(x, gy, x + w, gy + 1, shade(color, 2.2))
        if kind == "glow":
            tex.rect(x + w // 3, y + h // 3, x + 2 * w // 3 + 1, y + 2 * h // 3 + 1, (240, 250, 255))
    return paint


def seg(y, h, w, mat):
    """Tronçon carré centré sur l'axe vertical."""
    return ((-w / 2, y, -w / 2), (w, h, w), {"*": mat})


def sonic_variant(name, body, emitter, extra=()):
    emitter_y = emitter[0][0][1]
    bones = [{"name": "root", "pivot": [0, 0, 0], "cubes": list(body) + list(extra)},
             {"name": "emitter", "parent": "root", "pivot": [0, emitter_y, 0], "cubes": list(emitter)}]
    used = {c[2]["*"] for b in bones for c in b["cubes"]}
    painters = {m: sonic_painter(*SONIC_MATERIALS[m]) for m in used}
    geo, tex, glow = build("sonic_" + name, bones, 8, 128, painters)
    path = os.path.join(ROOT, "geo", "item", "sonic", name + ".geo.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(geo, f, indent=1)
    tex.save(os.path.join(ROOT, "textures", "item", "sonic", name + ".png"))
    glow.save(os.path.join(ROOT, "textures", "item", "sonic", name + "_glowmask.png"))
    print("OK sonique", name)


def sonics():
    """Tous les tournevis : même principe (os « emitter » animé), silhouettes et couleurs différentes."""
    sonic_variant("fourth",
                  [seg(-6.5, 0.75, 1.75, "silver"), seg(-5.75, 7.5, 1.25, "ringed"),
                   seg(-1, 0.75, 1.75, "black"), seg(1.75, 0.75, 1.75, "silver")],
                  [seg(2.5, 1, 1, "glow_white")])
    sonic_variant("tenth",
                  [seg(-6.5, 0.75, 2, "silver"), seg(-5.75, 5, 1.5, "grip"), seg(-0.75, 1, 2, "silver"),
                   ((-0.25, -0.5, -1.25), (0.5, 0.5, 0.25), {"*": "red"}),
                   seg(0.25, 2.5, 1.25, "ringed"), seg(2.75, 1.5, 2.5, "silver")],
                  [seg(4.25, 1.25, 1.5, "glow_blue")])
    claws = [((x, 3.25, z), (0.5, 2.25, 0.5), {"*": "copper"})
             for x, z in ((-1.25, -1.25), (0.75, -1.25), (-1.25, 0.75), (0.75, 0.75))]
    sonic_variant("eleventh",
                  [seg(-6.5, 5, 1.75, "gunmetal"), seg(-1.5, 0.75, 2, "copper"), seg(-0.75, 2.5, 1.5, "gunmetal"),
                   seg(1.75, 1.5, 2.25, "copper")],
                  [seg(3.25, 1.25, 1.25, "glow_green")], claws)
    sonic_variant("twelfth",
                  [seg(-6.5, 6, 1.5, "dark"), seg(-0.5, 0.5, 1.75, "silver"), seg(0, 2.5, 1.25, "silver"),
                   seg(2.5, 1, 2, "dark")],
                  [seg(3.5, 1, 1.25, "glow_violet")])
    sonic_variant("thirteenth",
                  [seg(-6.5, 5.5, 1.75, "bronze"), seg(-1, 0.75, 2, "copper"), seg(-0.25, 2.75, 1.25, "bronze_dark")],
                  [seg(2.5, 1.5, 1.5, "glow_orange")])
    sonic_variant("war",
                  [seg(-6.5, 5, 1.75, "wood"), seg(-1.5, 0.75, 2, "brass"), seg(-0.75, 2.75, 1.25, "brass"),
                   seg(2, 1.25, 2, "brass")],
                  [seg(3.25, 1, 1.25, "glow_red")])
    sonic_variant("river",
                  [seg(-6.5, 7, 1.5, "black"), seg(0.5, 0.75, 1.75, "silver"), seg(1.25, 1.75, 1.75, "silver")],
                  [seg(3, 1, 1.25, "glow_blue")])
    # Deuxième Docteur : simple lampe-stylo argentée
    sonic_variant("second",
                  [seg(-6.5, 8, 1.0, "silver"), seg(1.5, 0.5, 1.25, "black")],
                  [seg(2, 0.75, 0.75, "glow_white")])
    # Troisième Docteur : manche noir, corps annelé, grosse tête ronde
    sonic_variant("third",
                  [seg(-6.5, 3.5, 1.5, "black"), seg(-3, 4.5, 1.25, "ringed"), seg(1.5, 1.25, 2.25, "silver")],
                  [seg(2.75, 0.75, 1.25, "glow_white")])
    # Septième Docteur : corps sombre, bagues argentées
    sonic_variant("seventh",
                  [seg(-6.5, 1, 1.75, "silver"), seg(-5.5, 5, 1.5, "gunmetal"), seg(-0.5, 0.75, 1.75, "silver"),
                   seg(0.25, 2, 1.25, "ringed")],
                  [seg(2.25, 1, 1.25, "glow_violet")])
    # Huitième Docteur (1996) : poignée noire striée, embout bleu
    sonic_variant("eighth",
                  [seg(-6.5, 4.5, 1.75, "grip"), seg(-2, 1, 2, "silver"), seg(-1, 3, 1.25, "ringed"),
                   seg(2, 0.75, 2, "silver")],
                  [seg(2.75, 1, 1.5, "glow_blue")])
    # Quatorzième Docteur (2023) : gros corps sombre, collerette cuivrée
    sonic_variant("fourteenth",
                  [seg(-6.5, 5.5, 2, "dark"), seg(-1, 1, 2.25, "copper"), seg(0, 2.25, 1.5, "gunmetal"),
                   seg(2.25, 1, 2.5, "dark")],
                  [seg(3.25, 1.25, 1.5, "glow_blue")])
    sonic_variant("master",
                  [seg(-7, 8.5, 1.25, "gold"), seg(-4, 0.5, 1.5, "black"), seg(-2, 0.5, 1.5, "black"),
                   seg(1.5, 2, 1.75, "black")],
                  [seg(3.5, 1.5, 0.75, "glow_red")])


# ---------------------------------------------------------------------------
# Icône 16x16 du TARDIS pour l'inventaire
# ---------------------------------------------------------------------------
def tardis_icon():
    img = Image(16, 16)
    img.rect(7, 0, 9, 1, (235, 245, 255))
    img.rect(5, 1, 11, 2, BLUE_DARK)
    img.rect(4, 2, 12, 3, (14, 14, 18))
    img.rect(5, 2, 11, 3, (200, 200, 200))
    img.rect(4, 3, 12, 15, BLUE)
    img.rect(4, 3, 5, 15, BLUE_DARK)
    img.rect(11, 3, 12, 15, BLUE_DARK)
    img.rect(8, 3, 8 + 1, 15, BLUE_DARK)
    for wx in (5, 9):
        img.rect(wx, 4, wx + 2, 6, GLASS)
        img.frame(wx, 7, wx + 2, 10, shade(BLUE, 1.2))
        img.frame(wx, 11, wx + 2, 14, shade(BLUE, 1.2))
    img.rect(3, 15, 13, 16, BLUE_DARK)
    img.save(os.path.join(ROOT, "textures", "item", "tardis.png"))
    print("OK icone tardis")


# ---------------------------------------------------------------------------
# Icône 16x16 de la clé du TARDIS (clé Yale argentée + porte-clés TARDIS)
# ---------------------------------------------------------------------------
def tardis_key_icon():
    img = Image(16, 16)
    steel = (196, 202, 212)
    light = (236, 240, 246)
    dark = (96, 102, 116)
    # Tête ronde de la clé (anneau)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 4.5, y - 4.5)
            if 1.6 < d <= 3.6:
                img.set(x, y, dark if d > 3.0 else (light if x + y < 9 else steel))
    # Tige et dents
    img.rect(8, 4, 15, 6, steel)
    img.rect(8, 4, 15, 5, light)
    img.rect(15, 4, 16, 6, dark)
    img.rect(8, 6, 15, 7, dark)
    for tx in (10, 12, 14):
        img.set(tx, 6, steel)
        img.set(tx, 7, dark)
    # Anneau du porte-clés et petit TARDIS
    img.set(3, 8, dark)
    img.set(3, 9, dark)
    img.rect(2, 10, 5, 11, (235, 245, 255))
    img.rect(1, 11, 6, 16, BLUE)
    img.rect(1, 11, 2, 16, BLUE_DARK)
    img.rect(5, 11, 6, 16, BLUE_DARK)
    img.rect(2, 12, 5, 13, GLASS)
    img.save(os.path.join(ROOT, "textures", "item", "tardis_key.png"))
    print("OK icone cle")


# ---------------------------------------------------------------------------
# Consoles du TARDIS : une par décor, grandes (≈ 4,6 blocs de large, 4 de haut)
# Repère : origine = centre du bas du bloc de la console, front = nord (-z).
# Chaque console garde les os « rotor » (animé), « throttle » (manette des gaz)
# et « sonic_port » (logement du tournevis), utilisés par le code Java.
# ---------------------------------------------------------------------------
TILT = 11             # inclinaison des pupitres (bord extérieur plus bas)
DESK_SIDE = 37        # côté de l'hexagone du plateau (en pixels de bloc)
# Bandes des pupitres : (distance extérieure, distance intérieure, hauteur du dessous)
STRIPS = [(31, 24, 17.5), (24, 17, 18.9), (17, 10, 20.3)]
STRIP_T = 1.5
# Colonne jusqu'au plafond, dessinée pour un plafond à 9 blocs (144 px) : du chapeau (65) à 141,
# puis collerette de 3 px sous le plafond. Le rendu Java l'adapte à la vraie hauteur.
SHAFT_BOTTOM = 65
SHAFT_TOP = 141


def k_metal(color, amount=0.06, frame=1.2):
    def paint(tex, glow, x, y, w, h, s, face, seed):
        noisy(tex, x, y, x + w, y + h, color, amount, seed)
        if w > 4 and h > 4:
            tex.frame(x, y, x + w, y + h, shade(color, frame), 1)
    return paint


def k_ribbed(color, step=6):
    def paint(tex, glow, x, y, w, h, s, face, seed):
        noisy(tex, x, y, x + w, y + h, color, 0.08, seed)
        if face not in ("up", "down"):
            for gy in range(y + step // 2, y + h, step):
                tex.rect(x, gy, x + w, gy + 1, shade(color, 0.65))
    return paint


def k_roundels(color, hole):
    """Base à roundels (cercles creux), comme les murs du TARDIS."""
    def paint(tex, glow, x, y, w, h, s, face, seed):
        noisy(tex, x, y, x + w, y + h, color, 0.04, seed)
        if face in ("up", "down"):
            return
        r = 3
        for cy in range(y + 5, y + h - 3, 9):
            for cx in range(x + 5, x + w - 3, 9):
                for yy in range(cy - r, cy + r + 1):
                    for xx in range(cx - r, cx + r + 1):
                        if (xx - cx) ** 2 + (yy - cy) ** 2 <= r * r + 1:
                            tex.set(xx, yy, hole)
    return paint


def k_coral(color):
    def paint(tex, glow, x, y, w, h, s, face, seed):
        rnd = random.Random(seed)
        noisy(tex, x, y, x + w, y + h, color, 0.12, seed)
        for _ in range(w * h // 18):
            cx, cy = rnd.randrange(x, x + w), rnd.randrange(y, y + h)
            tex.set(cx, cy, shade(color, 0.7))
            tex.set(cx + 1, cy, shade(color, 1.25))
    return paint


def k_wood(color):
    def paint(tex, glow, x, y, w, h, s, face, seed):
        rnd = random.Random(seed)
        for yy in range(y, y + h):
            k = 0.9 + 0.12 * math.sin((yy - y) * 0.7 + rnd.random())
            tex.rect(x, yy, x + w, yy + 1, shade(color, k))
        for _ in range(max(1, w * h // 60)):
            gx = rnd.randrange(x, x + w)
            tex.rect(gx, y, gx + 1, y + h, shade(color, 0.8))
    return paint


def k_desk(color, inset):
    """Dessus de pupitre : cadre, rainures et (selon le style) un panneau encastré."""
    def paint(tex, glow, x, y, w, h, s, face, seed):
        noisy(tex, x, y, x + w, y + h, color, 0.05, seed)
        if face == "up" and w > 8 and h > 6:
            tex.frame(x, y, x + w, y + h, shade(color, 0.6), 2)
            if inset:
                tex.rect(x + 4, y + 3, x + w - 4, y + h - 3, inset)
    return paint


def k_glass(color, alpha=40):
    def paint(tex, glow, x, y, w, h, s, face, seed):
        if face in ("up", "down"):
            return  # on voit le rotor à travers
        tex.rect(x, y, x + w, y + h, (*color, alpha))
        tex.frame(x, y, x + w, y + h, (*shade(color, 1.2), 130), 1)
        for gx in range(x + 3, x + w - 1, 7):
            tex.rect(gx, y, gx + 1, y + h, (255, 255, 255, 60))
    return paint


def k_rotor(color, color2=None):
    def paint(tex, glow, x, y, w, h, s, face, seed):
        for yy in range(y, y + h):
            k = 0.7 + 0.3 * math.sin((yy - y) * 0.8)
            c = color if color2 is None or (yy - y) % 4 < 2 else color2
            tex.rect(x, yy, x + w, yy + 1, shade(c, k))
            glow.rect(x, yy, x + w, yy + 1, shade(c, k))
    return paint


def k_light(color):
    def paint(tex, glow, x, y, w, h, s, face, seed):
        tex.rect(x, y, x + w, y + h, color)
        glow.rect(x, y, x + w, y + h, color)
    return paint


def k_monitor(color):
    def paint(tex, glow, x, y, w, h, s, face, seed):
        tex.rect(x, y, x + w, y + h, (10, 14, 18))
        if face in ("up", "north", "south"):
            for gy in range(y + 2, y + h - 1, 3):
                x1 = x + w - 2 - (gy * 7 % 6)
                tex.rect(x + 2, gy, x1, gy + 1, color)
                glow.rect(x + 2, gy, x1, gy + 1, color)
    return paint


def k_dial(rim):
    def paint(tex, glow, x, y, w, h, s, face, seed):
        tex.rect(x, y, x + w, y + h, rim)
        if face == "up" and w > 3 and h > 3:
            tex.rect(x + 1, y + 1, x + w - 1, y + h - 1, (238, 232, 214))
            tex.rect(x + w // 2, y + 1, x + w // 2 + 1, y + h // 2 + 1, (30, 30, 30))
    return paint


# Styles : couleurs des matières + éléments propres à chaque console
CONSOLE_STYLES = {
    # Dixième Docteur (2005) : corail, colonne verte, six branches de corail
    "coral": {"base": k_coral((176, 104, 66)), "trim": k_metal((120, 92, 60)), "rim": k_coral((150, 96, 60)),
              "desk": k_desk((128, 116, 78), (96, 84, 56)), "glass": k_glass((150, 235, 170)),
              "rotor": k_rotor((90, 240, 140)), "strut": k_coral((196, 120, 70)), "accent": k_metal((90, 160, 110))},
    # Onzième Docteur (2010) : cuivre, verre, rotor orange et bleu, cage de cuivre
    "copper": {"base": k_ribbed((150, 88, 44)), "trim": k_metal((200, 150, 70)), "rim": k_metal((184, 115, 51)),
               "desk": k_desk((168, 104, 54), (120, 72, 40)), "glass": k_glass((220, 240, 255)),
               "rotor": k_rotor((255, 150, 60), (90, 170, 255)), "strut": k_metal((184, 115, 51)),
               "accent": k_metal((70, 110, 150))},
    # Classique (années 70) : blanc, roundels, colonne argentée
    "white": {"base": k_roundels((228, 228, 222), (170, 170, 168)), "trim": k_metal((190, 192, 196)),
              "rim": k_metal((236, 236, 232)), "desk": k_desk((214, 214, 210), (60, 62, 70)),
              "glass": k_glass((230, 240, 255), 30), "rotor": k_rotor((255, 140, 90), (255, 214, 170)),
              "strut": k_metal((200, 200, 205)), "accent": k_metal((60, 62, 70))},
    # Douzième Docteur (2014) : sombre et doré, colonne bleue, grand anneau au-dessus
    "dark": {"base": k_roundels((52, 50, 56), (22, 22, 26)), "trim": k_metal((196, 156, 72)),
             "rim": k_metal((40, 38, 44)), "desk": k_desk((62, 58, 64), (36, 34, 40)),
             "glass": k_glass((170, 210, 255)), "rotor": k_rotor((110, 200, 255)),
             "strut": k_metal((196, 156, 72)), "accent": k_metal((150, 110, 60))},
    # Premier Docteur (1963) : blanc et gris, colonne d'instruments argentés
    "columns": {"base": k_ribbed((220, 222, 218), 4), "trim": k_metal((120, 124, 130)),
                "rim": k_metal((200, 202, 200)), "desk": k_desk((190, 192, 190), (40, 40, 44)),
                "glass": k_glass((240, 245, 255), 30), "rotor": k_rotor((235, 240, 250), (120, 130, 150)),
                "strut": k_metal((120, 124, 130)), "accent": k_metal((40, 40, 44))},
    # Huitième Docteur (1996) : bois sombre et laiton, lumière ambrée
    "victorian": {"base": k_wood((78, 50, 30)), "trim": k_metal((190, 150, 70)), "rim": k_wood((96, 62, 36)),
                  "desk": k_desk((110, 74, 42), (70, 46, 26)), "glass": k_glass((255, 210, 140), 50),
                  "rotor": k_rotor((255, 180, 80)), "strut": k_wood((84, 54, 32)), "accent": k_metal((190, 150, 70))},
}
CONTROL_PAINTERS = {"red": k_light((210, 50, 40)), "green": k_light((70, 210, 90)), "yellow": k_light((240, 205, 70)),
                    "blue": k_light((80, 160, 240)), "white": k_light((240, 240, 230)), "key": k_metal((214, 208, 196)),
                    "hole": k_metal((14, 12, 10)), "silver": s_silver, "monitor": k_monitor((90, 230, 120)),
                    "dial": k_dial((150, 112, 60))}


def strip_cube(j, w, mat, faces=None):
    d_out, d_in, y0 = STRIPS[j]
    m = {"*": "trim", "up": mat}
    m.update(faces or {})
    return ((-w / 2, y0, -d_out), (w, STRIP_T, d_out - d_in), m,
            {"rotation": [TILT, 0, 0], "pivot": [0, y0, -(d_out + d_in) / 2]})


def on_strip(j, x, z, w, h, d, mat):
    """Commande posée sur la bande j : x depuis le centre, z depuis le bord extérieur de la bande."""
    d_out, d_in, y0 = STRIPS[j]
    return ((x, y0 + STRIP_T, -d_out + z), (w, h, d), {"*": mat},
            {"rotation": [TILT, 0, 0], "pivot": [0, y0, -(d_out + d_in) / 2]})


def console_bones(style):
    s = DESK_SIDE
    root = (hexagon(20, 2, 0, "trim") + hexagon(13, 12, 2, "base") + hexagon(27, 3, 13, "base")
            + hexagon(s, 2, 16, "rim"))
    if style == "victorian":
        root += hexagon(15, 2, 7, "trim")           # bague de laiton sur le pied
    if style == "coral":
        root += hexagon(10, 6, 2, "accent")
    bones = [{"name": "root", "pivot": [0, 0, 0], "cubes": root}]
    colors = ("red", "green", "yellow", "blue")
    keys = "white" if style in ("white", "columns") else "key"
    layouts = {
        # 0 : manettes + rangée de boutons (la manette des gaz est un os à part)
        0: [on_strip(0, -9, 1.5, 1.5, 4, 1.5, "silver"), on_strip(0, -9.5, 1, 2.5, 1.2, 2.5, "red"),
            on_strip(0, 7.5, 1.5, 1.5, 4, 1.5, "silver"), on_strip(0, 7, 1, 2.5, 1.2, 2.5, "green")]
           + [on_strip(0, -4 + i * 2.5, 4, 1.5, 0.8, 1.5, colors[i]) for i in range(4)],
        # 1 : grille de boutons + deux cadrans
        1: [on_strip(0, -10 + c * 3, 1.5 + r * 2.6, 2, 0.8, 2, colors[(r + c) % 4]) for r in range(2) for c in range(5)]
           + [on_strip(1, -7, 1.5, 4, 0.6, 4, "dial"), on_strip(1, 3, 1.5, 4, 0.6, 4, "dial")],
        # 2 : clavier (machine à écrire)
        2: [on_strip(0, -10 + i * 2.2, 1 + j * 1.9, 1.6, 0.8, 1.5, keys) for i in range(10) for j in range(3)]
           + [on_strip(1, -5, 2, 10, 0.6, 1.6, keys)],
        # 3 : logement du tournevis + interrupteurs
        3: [on_strip(1, -2.5, 1.5, 5, 1, 1, "trim"), on_strip(1, -2.5, 5, 5, 1, 1, "trim"),
            on_strip(1, -2.5, 2.5, 1, 1, 2.5, "trim"), on_strip(1, 1.5, 2.5, 1, 1, 2.5, "trim"),
            on_strip(1, -1.5, 2.5, 3, 0.3, 2.5, "hole")]
           + [on_strip(0, -9 + i * 4.5, 2, 1.2, 2.5, 1.2, "silver") for i in range(5)],
        # 4 : grand écran
        4: [on_strip(0, -9, 1, 18, 0.6, 5, "monitor"), on_strip(1, -5, 2, 10, 0.8, 1.2, "yellow")],
        # 5 : frein à main + jauges
        5: [on_strip(0, -5, 2.5, 10, 1, 1.5, "silver"), on_strip(0, -6, 2, 2, 2.5, 2, "trim"),
            on_strip(0, 4, 2, 2, 2.5, 2, "trim"), on_strip(1, -8, 1.5, 4, 0.6, 4, "dial"),
            on_strip(1, 4, 1.5, 4, 0.6, 4, "dial")],
    }
    for k in range(6):
        cubes = [strip_cube(0, 26.5, "desk"), strip_cube(1, 18.5, "desk"), strip_cube(2, 10.5, "desk")]
        cubes += layouts[k]
        # Petites lumières sur la bande intérieure
        cubes += [on_strip(2, -3.5 + i * 2.5, 2.5, 1.2, 0.6, 1.2, colors[(i + k) % 4]) for i in range(3)]
        bones.append({"name": "panel_%d" % k, "parent": "root", "pivot": [0, 0, 0],
                      "rotation": [0, k * 60, 0], "cubes": cubes})
        # Arête entre deux pupitres (vers le coin de l'hexagone)
        bones.append({"name": "edge_%d" % k, "parent": "root", "pivot": [0, 0, 0], "rotation": [0, k * 60 + 30, 0],
                      "cubes": [((-1, 18, -33), (2, 2.5, 23), {"*": "trim"},
                                 {"rotation": [TILT, 0, 0], "pivot": [0, 18, -21.5]})]})
    d1_out, d1_in, y1 = STRIPS[1]
    mid1 = (d1_out + d1_in) / 2
    # Logement du tournevis (pupitre 3) et manette des gaz (pupitre 0)
    bones.append({"name": "sonic_port", "parent": "panel_3", "pivot": [0, y1 + STRIP_T + 0.3, -(d1_out - 3.75)],
                  "rotation": [TILT, 0, 0], "cubes": []})
    bones.append({"name": "throttle", "parent": "panel_0", "pivot": [0, y1 + STRIP_T, -mid1],
                  "cubes": [((-0.75, y1 + STRIP_T, -mid1 - 0.75), (1.5, 7, 1.5), {"*": "silver"}),
                            ((-2.5, y1 + STRIP_T + 6.5, -mid1 - 1.5), (5, 1.8, 3), {"*": "red"})]})
    # Rotor (animé) dans sa colonne de verre
    rotor = [((-2.5, 25 + i * 11, -2.5), (5, 7, 5), {"*": "rotor"}) for i in range(3)]
    if style in ("columns", "white"):
        rotor += [((-4, 28 + i * 11, -4), (8, 1, 8), {"*": "trim"}) for i in range(3)]   # disques d'instruments
    bones.append({"name": "rotor", "parent": "root", "pivot": [0, 25, 0], "cubes": rotor})
    housing = (hexagon(7, 40, 22, "glass") + hexagon(8.5, 2, 21, "trim") + hexagon(8.5, 1.5, 41, "trim")
               + hexagon(9.5, 3, 62, "trim"))
    bones.append({"name": "housing", "parent": "root", "pivot": [0, 0, 0], "cubes": housing})
    bones += console_extras(style)
    if style != "white":
        # Colonne qui monte jusqu'au plafond (étirée par le rendu selon la hauteur de la salle)
        # et collerette de fixation au plafond (déplacée par le rendu). La console classique n'en a pas.
        bones.append({"name": "shaft", "parent": "root", "pivot": [0, SHAFT_BOTTOM, 0],
                      "cubes": hexagon(6, SHAFT_TOP - SHAFT_BOTTOM, SHAFT_BOTTOM, "glass")
                      + [((-1, SHAFT_BOTTOM, -1), (2, SHAFT_TOP - SHAFT_BOTTOM, 2), {"*": "rotor"})]})
        bones.append({"name": "mount", "parent": "root", "pivot": [0, SHAFT_TOP, 0],
                      "cubes": hexagon(12, 3, SHAFT_TOP, "trim") + hexagon(8.5, 4, SHAFT_TOP - 4, "trim")})
    return bones


def console_extras(style):
    """Éléments qui donnent son caractère à chaque console."""
    extra = []
    if style == "coral":
        # Six branches de corail qui montent des coins du plateau vers la colonne
        for k in range(6):
            extra.append({"name": "strut_%d" % k, "parent": "root", "pivot": [0, 0, 0], "rotation": [0, k * 60 + 30, 0],
                          "cubes": [((-2, 17, -37), (4, 58, 4), {"*": "strut"}, {"rotation": [-22, 0, 0], "pivot": [0, 17, -35]}),
                                    ((-1.5, 30, -32), (3, 3, 3), {"*": "accent"}, {"rotation": [-22, 0, 0], "pivot": [0, 17, -35]})]})
    elif style == "copper":
        # Cage de cuivre : six montants aux coins et un anneau hexagonal au-dessus, reliés à la colonne
        for k in range(6):
            extra.append({"name": "cage_%d" % k, "parent": "root", "pivot": [0, 0, 0], "rotation": [0, k * 60 + 30, 0],
                          "cubes": [((-1, 18, -36), (2, 50, 2), {"*": "strut"}),
                                    ((-1, 66, -36), (2, 2, 27), {"*": "strut"})]})
            extra.append({"name": "ring_%d" % k, "parent": "root", "pivot": [0, 0, 0], "rotation": [0, k * 60, 0],
                          "cubes": [((-18.5, 66, -32), (37, 2, 2), {"*": "trim"})]})
    elif style == "dark":
        # Grand anneau doré suspendu au-dessus de la console, porté par trois bras
        for k in range(6):
            extra.append({"name": "ring_%d" % k, "parent": "root", "pivot": [0, 0, 0], "rotation": [0, k * 60, 0],
                          "cubes": [((-15, 70, -26.5), (30, 3, 3), {"*": "trim"}),
                                    ((-2, 69.5, -27), (4, 4, 0.5), {"*": "rotor"})]})
        for k in range(3):
            extra.append({"name": "arm_%d" % k, "parent": "root", "pivot": [0, 0, 0], "rotation": [0, k * 120 + 30, 0],
                          "cubes": [((-1, 64, -29), (2, 2, 20), {"*": "strut"}, {"rotation": [-14, 0, 0], "pivot": [0, 65, -9]})]})
        # Écran sur pied, au-dessus du pupitre 4
        extra.append({"name": "screen", "parent": "root", "pivot": [0, 0, 0], "rotation": [0, 240, 0],
                      "cubes": [((-0.75, 22, -15), (1.5, 12, 1.5), {"*": "strut"}),
                                ((-6, 32, -16), (12, 8, 1), {"*": "trim", "north": "monitor"})]})
    elif style == "white":
        # Six piliers blancs sous le plateau (pied « champignon »)
        for k in range(6):
            extra.append({"name": "leg_%d" % k, "parent": "root", "pivot": [0, 0, 0], "rotation": [0, k * 60 + 30, 0],
                          "cubes": [((-1.5, 2, -24), (3, 14, 3), {"*": "trim"})]})
    elif style == "columns":
        # Colonne centrale plus haute, ceinte d'anneaux, et pupitres soutenus par des montants sombres
        for k in range(6):
            extra.append({"name": "leg_%d" % k, "parent": "root", "pivot": [0, 0, 0], "rotation": [0, k * 60 + 30, 0],
                          "cubes": [((-1, 2, -26), (2, 14, 2), {"*": "accent"})]})
        extra.append({"name": "crown", "parent": "root", "pivot": [0, 0, 0],
                      "cubes": hexagon(11, 1.5, 52, "trim") + hexagon(6, 6, 65, "trim")})
    elif style == "victorian":
        # Pieds de bois sculptés et dais de laiton au-dessus
        for k in range(6):
            extra.append({"name": "leg_%d" % k, "parent": "root", "pivot": [0, 0, 0], "rotation": [0, k * 60 + 30, 0],
                          "cubes": [((-2, 2, -25), (4, 14, 4), {"*": "strut"}),
                                    ((-1, 65, -26), (2, 2, 17), {"*": "trim"})]})
            extra.append({"name": "canopy_%d" % k, "parent": "root", "pivot": [0, 0, 0], "rotation": [0, k * 60, 0],
                          "cubes": [((-13.5, 64.5, -24.5), (27, 3, 3), {"*": "strut"}),
                                    ((-1.5, 62, -24), (3, 3, 2), {"*": "rotor"})]})
    return extra


def consoles():
    for style, palette in CONSOLE_STYLES.items():
        painters = dict(CONTROL_PAINTERS)
        painters.update(palette)
        geo, tex, glow = build("console_" + style, console_bones(style), 2, 512, painters)
        desc = geo["minecraft:geometry"][0]["description"]
        desc.update({"visible_bounds_width": 6, "visible_bounds_height": 6, "visible_bounds_offset": [0, 2.5, 0]})
        write("block", "console_" + style, geo, tex, glow)


if __name__ == "__main__":
    tardis()
    tardis_door()
    consoles()
    sonics()
    tardis_icon()
    tardis_key_icon()
