"""
Générateur des créatures (modèles GeckoLib .geo.json, textures et animations).

Même principe que generate_assets.py : des pavés + des textures dessinées par code.
Face avant des modèles = nord (-z). Unités : pixels (16 = 1 bloc), origine aux pieds.
Pour voir le résultat sans lancer le jeu :  python tools/preview.py

Utilisation (depuis la racine du projet) :
    python tools/generate_entities.py
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from generate_assets import ROOT, build, noisy, shade, write  # noqa: E402
from preview import read_png  # noqa: E402


# ---------------------------------------------------------------------------
# Finition « pixel art » (même esprit que les mods soignés comme le Dalek Mod) :
# grain en gros pixels, chaque pièce éclairée en haut et assombrie en bas,
# arêtes biseautées, reflets sur le métal, usure selon la matière.
# ---------------------------------------------------------------------------
def noisy(img, x0, y0, x1, y1, c, amount, seed):
    """Comme generate_assets.noisy, mais par paquets de 2x2 texels : grain « pixel art » plutôt que bruit fin."""
    rnd = random.Random(seed)
    x0, y0, cells = int(x0), int(y0), {}
    for y in range(y0, int(y1)):
        for x in range(x0, int(x1)):
            key = ((x - x0) // 2, (y - y0) // 2)
            if key not in cells:
                cells[key] = 1 + rnd.uniform(-amount, amount)
            img.set(x, y, shade(c, cells[key]))


def px(img, x, y):
    i = (y * img.w + x) * 4
    return tuple(img.px[i:i + 3])


def blend(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


# matière -> style de finition (None = laissé tel quel : lumières, bosses, grilles)
STYLE = {
    "hull": "metal", "bumpd": "metal", "dome": "metal", "slats": "metal", "silver": "metal", "ring": "metal",
    "metal": "metal", "armor": "metal", "buckle": "metal",
    "dark": "matte", "black": "matte", "cup": "matte", "ribs": "matte", "belt": "matte", "shoe": "matte",
    "claw": "matte", "tooth": "matte", "keypad": "matte", "wheel": "matte", "chest": "matte",
    "face": "skin", "skin": "skin", "tendril": "skin", "vent": "skin", "belly": "skin", "sucker": "skin",
    "fat": "soft", "suit": "cloth", "shirt": "cloth", "tie": "cloth", "tuft": "cloth",
    "stone": "stone", "fold": "stone", "wing": "stone",
}


def finish(tex, x, y, w, h, face, seed, style):
    if not style:
        return
    rnd = random.Random(seed * 7919 + 17)
    side = face not in ("up", "down")
    spec = rnd.uniform(0.2, 0.34)
    bevel = w >= 3 and h >= 3 and style != "soft"
    for yy in range(y, y + h):
        v = (yy - y + 0.5) / h
        for xx in range(x, x + w):
            u = (xx - x + 0.5) / w
            k = 1.08 - 0.2 * v if side else (1.06 if face == "up" else 0.8)
            if style == "metal":
                d = abs(u - spec) if side else abs((u + v) / 2 - spec)
                k += 0.24 * max(0.0, 1 - d / 0.1) + 0.08 * max(0.0, 1 - abs(u - spec - 0.42) / 0.05)
            elif style == "soft":
                k = 1.0 + (k - 1.0) * 0.5 + 0.1 * max(0.0, 1 - math.hypot(u - 0.35, v - 0.3) / 0.35)
            if bevel:
                if yy == y:
                    k *= 1.2
                elif yy == y + h - 1:
                    k *= 0.7
                if xx == x:
                    k *= 1.08
                elif xx == x + w - 1:
                    k *= 0.8
            tex.set(xx, yy, shade(px(tex, xx, yy), k))
    area = w * h
    if style == "metal":                               # fines rayures claires
        for _ in range(area // 90):
            sx, sy = rnd.randrange(x, x + w), rnd.randrange(y, y + h)
            for k in range(rnd.randint(2, 5)):
                if sx + k < x + w and sy + k // 2 < y + h:
                    tex.set(sx + k, sy + k // 2, shade(px(tex, sx + k, sy + k // 2), 1.25))
    elif style == "stone":                             # fissures + lichen
        for _ in range(area // 380):
            cx, cy = rnd.randrange(x, x + w), rnd.randrange(y, y + h)
            for _ in range(rnd.randint(4, 10)):
                tex.set(cx, cy, shade(px(tex, cx, cy), 0.74))
                cx = min(x + w - 1, max(x, cx + rnd.choice((-1, 0, 1))))
                cy = min(y + h - 1, cy + 1)
        for _ in range(area // 260):
            cx, cy = rnd.randrange(x, x + w), rnd.randrange(y, y + h)
            for dy in range(2):
                for dx in range(rnd.randint(2, 3)):
                    if cx + dx < x + w and cy + dy < y + h:
                        tex.set(cx + dx, cy + dy, blend(px(tex, cx + dx, cy + dy), (120, 128, 92), 0.5))
    elif style == "skin":                              # pores et rides
        for _ in range(area // 40):
            sx, sy = rnd.randrange(x, x + w), rnd.randrange(y, y + h)
            tex.set(sx, sy, shade(px(tex, sx, sy), 0.82))
        for _ in range(area // 160):
            sx, sy = rnd.randrange(x, x + w), rnd.randrange(y, y + h)
            for k in range(rnd.randint(3, 5)):
                if sx + k < x + w:
                    tex.set(sx + k, sy, shade(px(tex, sx + k, sy), 0.86))
    elif style == "cloth" and side and w >= 8:        # plis du tissu
        for _ in range(max(1, w // 10)):
            fx = rnd.randrange(x + 2, x + w - 2)
            if face == "north" and abs((fx - x) / w - 0.5) < 0.18:
                continue                               # pas de pli sur la chemise / cravate
            for yy in range(y + 1, y + h - 1):
                tex.set(fx, yy, shade(px(tex, fx, yy), 0.78))
                tex.set(fx + 1, yy, shade(px(tex, fx + 1, yy), 1.1))


def build_fx(name, bones, scale, atlas_w, painters, **styles):
    """build() + finition de chaque face selon sa matière (styles = remplacements de STYLE)."""
    wrapped = {}
    for key, paint in painters.items():
        def fx(tex, glw, x, y, w, h, s, face, seed, paint=paint, style=styles.get(key, STYLE.get(key))):
            paint(tex, glw, x, y, w, h, s, face, seed)
            finish(tex, x, y, w, h, face, seed, style)
        wrapped[key] = fx
    return build(name, bones, scale, atlas_w, wrapped)


# ---------------------------------------------------------------------------
# Outils
# ---------------------------------------------------------------------------
def mat(color, amount=0.06, glow=False):
    """Matière unie légèrement bruitée (éventuellement lumineuse)."""
    def paint(tex, glw, x, y, w, h, s, face, seed):
        noisy(tex, x, y, x + w, y + h, color, amount, seed)
        if glow:
            glw.rect(x, y, x + w, y + h, color)
    return paint


def box(origin, size, material, **faces):
    m = {"*": material}
    m.update(faces)
    return (tuple(origin), tuple(size), m)


def bone(name, pivot, cubes, parent=None, rotation=None):
    b = {"name": name, "pivot": list(pivot), "cubes": cubes}
    if parent:
        b["parent"] = parent
    if rotation:
        b["rotation"] = list(rotation)
    return b


def animations(name, anims):
    path = os.path.join(ROOT, "animations", "entity", name + ".animation.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    data = {"format_version": "1.8.0", "animations": {}}
    for key, (length, loop, bones) in anims.items():
        data["animations"]["animation.%s.%s" % (name, key)] = {
            "loop": loop, "animation_length": length, "bones": bones}
    with open(path, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=1)


def swing(amplitude, length, phase=0.0, axis=0):
    """Balancement en 5 images clés (aller-retour) autour d'un axe."""
    keys = {}
    for i in range(5):
        t = length * i / 4
        v = [0, 0, 0]
        v[axis] = round(amplitude * math.sin(2 * math.pi * (i / 4) + phase), 2)
        keys[str(round(t, 3))] = v
    return keys


def hold(rotation):
    return {"0.0": list(rotation)}


def walk_bones(legs=35, arms=30, length=1.0):
    return {"right_leg": {"rotation": swing(legs, length)},
            "left_leg": {"rotation": swing(-legs, length)},
            "right_arm": {"rotation": swing(-arms, length)},
            "left_arm": {"rotation": swing(arms, length)}}


def sphere(color, rim=0.4):
    """Face d'une demi-sphère : disque ombré avec reflet en haut à gauche."""
    def paint(tex, glw, x, y, w, h, s, face, seed):
        tex.rect(x, y, x + w, y + h, shade(color, rim))
        cx, cy, r = x + w / 2 - 0.5, y + h / 2 - 0.5, min(w, h) / 2
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                d = math.hypot(xx - cx, yy - cy) / r
                if d <= 1.0:
                    hl = math.hypot(xx - (cx - r * 0.3), yy - (cy - r * 0.3)) / r
                    k = max(0.5, min(1.35, 1.35 - 0.8 * hl))
                    if d > 0.8:
                        k *= 0.68          # contour sombre
                    if hl < 0.2:
                        k = 1.75           # point de lumière
                    tex.set(xx, yy, shade(color, k))
    return paint


def slats(color, gap=(18, 18, 22), period=4):
    """Bandes horizontales (persiennes) : le « corset » des Daleks."""
    def paint(tex, glw, x, y, w, h, s, face, seed):
        noisy(tex, x, y, x + w, y + h, color, 0.04, seed)
        if face not in ("up", "down"):
            for yy in range(y, y + h):
                if (yy - y) % period == period - 1:
                    tex.rect(x, yy, x + w, yy + 1, gap)
    return paint


def grille(color, line=(26, 28, 32)):
    """Grille fine : les anneaux du cou des Daleks."""
    def paint(tex, glw, x, y, w, h, s, face, seed):
        noisy(tex, x, y, x + w, y + h, color, 0.05, seed)
        for yy in range(y, y + h, 2):
            tex.rect(x, yy, x + w, yy + 1, line)
        for xx in range(x, x + w, 3):
            tex.rect(xx, y, xx + 1, y + h, line)
    return paint


# ---------------------------------------------------------------------------
# Dalek (un modèle, plusieurs couleurs)
# ---------------------------------------------------------------------------
DALEK_COLORS = {
    #          coque             bosses           dôme
    "bronze": ((142, 98, 48), (214, 170, 78), (150, 106, 54)),
    "silver": ((172, 176, 186), (64, 108, 192), (188, 192, 202)),
    "red":    ((166, 34, 30), (92, 92, 100), (174, 42, 38)),
    "black":  ((34, 34, 38), (196, 164, 72), (42, 42, 48)),
    "white":  ((228, 226, 216), (204, 170, 82), (234, 232, 222)),
}


def dalek_bumps(cx_list, tiers):
    cubes = []
    for (y0, y1, half) in tiers:
        yc = (y0 + y1) / 2
        for k in cx_list:
            kk = k * half / 7.0
            cubes.append(box((kk - 1.3, yc - 1.3, -half - 1.1), (2.6, 2.6, 1.1), "bumpd", north="bump"))
            cubes.append(box((kk - 1.3, yc - 1.3, half), (2.6, 2.6, 1.1), "bumpd", south="bump"))
            cubes.append(box((half, yc - 1.3, kk - 1.3), (1.1, 2.6, 2.6), "bumpd", east="bump"))
            cubes.append(box((-half - 1.1, yc - 1.3, kk - 1.3), (1.1, 2.6, 2.6), "bumpd", west="bump"))
    return cubes


def dalek():
    tiers = [(1.5, 6.0, 7.6), (6.0, 10.5, 6.9), (10.5, 15.0, 6.2)]
    root = [box((-8.4, 0, -8.4), (16.8, 1.5, 16.8), "dark")]
    for (y0, y1, h) in tiers:
        root.append(box((-h, y0, -h), (2 * h, y1 - y0, 2 * h), "hull"))
    root += dalek_bumps((-3.6, 0, 3.6), tiers)
    root.append(box((-5.7, 15.0, -5.7), (11.4, 3.2, 11.4), "slats"))          # « épaules » à persiennes
    root.append(box((-6.0, 14.6, -6.0), (12.0, 0.6, 12.0), "dark"))            # liseré sous les épaules
    # tête : anneaux du cou, dôme, lampes d'oreille
    head = [box((-4.5, 18.2, -4.5), (9.0, 1.0, 9.0), "ring"),
            box((-4.1, 19.2, -4.1), (8.2, 1.2, 8.2), "mesh"),
            box((-4.5, 20.4, -4.5), (9.0, 1.0, 9.0), "ring"),
            box((-4.1, 21.4, -4.1), (8.2, 1.2, 8.2), "mesh"),
            box((-4.7, 22.6, -4.7), (9.4, 0.9, 9.4), "ring"),
            box((-4.5, 23.5, -4.5), (9.0, 2.0, 9.0), "dome"),
            box((-4.0, 25.5, -4.0), (8.0, 1.4, 8.0), "dome"),
            box((-3.1, 26.9, -3.1), (6.2, 1.2, 6.2), "dome"),
            box((-2.0, 28.1, -2.0), (4.0, 0.9, 4.0), "dome"),
            box((-6.1, 23.8, -1.0), (1.7, 2.6, 2.0), "lamp"),
            box((4.4, 23.8, -1.0), (1.7, 2.6, 2.0), "lamp"),
            box((-4.7, 24.6, -0.4), (0.5, 1.0, 0.8), "ring"),
            box((4.2, 24.6, -0.4), (0.5, 1.0, 0.8), "ring")]
    bones = [
        bone("root", (0, 0, 0), root),
        bone("gun", (3.2, 16.6, -5.7), [box((2.2, 15.7, -6.6), (2.0, 1.8, 1.6), "dark"),
                                         box((2.8, 16.2, -13.0), (0.8, 0.8, 6.6), "silver"),
                                         box((2.3, 15.7, -14.2), (1.8, 1.8, 1.4), "dark"),
                                         box((2.0, 15.4, -15.2), (0.5, 0.5, 1.2), "silver"),
                                         box((3.9, 15.4, -15.2), (0.5, 0.5, 1.2), "silver"),
                                         box((2.0, 17.1, -15.2), (0.5, 0.5, 1.2), "silver"),
                                         box((3.9, 17.1, -15.2), (0.5, 0.5, 1.2), "silver")], "root"),
        bone("plunger", (-3.2, 16.6, -5.7), [box((-4.2, 15.7, -6.6), (2.0, 1.8, 1.6), "dark"),
                                              box((-3.6, 16.2, -11.5), (0.8, 0.8, 5.0), "silver"),
                                              box((-4.6, 15.2, -13.0), (3.0, 3.0, 1.6), "dark", north="cup")], "root"),
        bone("head", (0, 18.2, 0), head, "root"),
        bone("eyestalk", (0, 24.6, -4.5), [box((-0.5, 24.1, -9.8), (1.0, 1.0, 5.8), "dark"),
                                           box((-1.5, 23.5, -10.4), (3.0, 3.0, 0.8), "silver"),
                                           box((-1.0, 24.0, -11.2), (2.0, 2.0, 0.8), "dark", north="eye")], "head"),
    ]
    geo = None
    for color, (hull, bump, dome) in DALEK_COLORS.items():
        painters = {"hull": mat(hull, 0.05), "bump": sphere(bump), "bumpd": mat(shade(bump, 0.5), 0.05),
                    "dome": mat(dome, 0.04), "dark": mat((28, 28, 32), 0.05), "slats": slats(shade(hull, 0.92)),
                    "mesh": grille((150, 154, 160), (78, 80, 86)), "ring": mat((118, 122, 128), 0.05), "silver": mat((186, 190, 198), 0.05),
                    "lamp": mat((255, 238, 176), 0.02, True), "cup": mat((46, 46, 52), 0.05),
                    "eye": mat((90, 200, 255), 0.03, True)}
        geo, tex, glow = build_fx("dalek", bones, 4, 256, painters)
        tex.save(os.path.join(ROOT, "textures", "entity", "dalek", color + ".png"))
        glow.save(os.path.join(ROOT, "textures", "entity", "dalek", color + "_glowmask.png"))
    path = os.path.join(ROOT, "geo", "entity", "dalek.geo.json")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(geo, f, indent=1)
    animations("dalek", {
        "idle": (6, True, {"head": {"rotation": swing(14, 6, axis=1)},
                           "eyestalk": {"rotation": swing(5, 3)}}),
        "walk": (2, True, {"head": {"rotation": swing(5, 2, axis=1)},
                           "eyestalk": {"rotation": swing(4, 2)}}),
        "shoot": (0.5, False, {"gun": {"position": {"0.0": [0, 0, 0], "0.1": [0, 0, 1.5], "0.5": [0, 0, 0]}},
                               "eyestalk": {"rotation": {"0.0": [0, 0, 0], "0.15": [-8, 0, 0], "0.5": [0, 0, 0]}}}),
    })
    print("OK dalek", len(DALEK_COLORS), "couleurs")


# ---------------------------------------------------------------------------
# Cyberman (version Cybus, 2006)
# ---------------------------------------------------------------------------
CY_SILVER = (178, 182, 190)


def cyber_face(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, CY_SILVER, 0.04, seed)
    if face != "north":
        return
    black = (14, 14, 18)
    for sign, ex in ((-1, x + w // 4 - 2), (1, x + 3 * w // 4 - 3)):
        # yeux en forme de goutte (la pointe vers le centre du visage)
        tex.rect(ex, y + h // 4, ex + 6, y + h // 4 + 6, black)
        tex.rect(ex + (3 if sign < 0 else 0), y + h // 4 + 6, ex + (6 if sign < 0 else 3), y + h // 4 + 9, black)
        tex.rect(ex + 1, y + h // 4 - 1, ex + 5, y + h // 4, shade(CY_SILVER, 0.6))
    # bouche : grille verticale sombre
    mx0, my0 = x + w // 2 - 5, y + h * 2 // 3
    tex.rect(mx0, my0, mx0 + 10, my0 + 7, black)
    for lx in range(mx0 + 2, mx0 + 10, 3):
        tex.rect(lx, my0, lx + 1, my0 + 7, shade(CY_SILVER, 0.85))
    tex.rect(x + w // 2 - 1, y + h // 2 - 1, x + w // 2 + 1, y + h // 2 + 3, shade(CY_SILVER, 0.7))  # nez


def cyber_chest(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, (52, 54, 62), 0.05, seed)
    if face == "north":
        tex.frame(x + 1, y + 1, x + w - 1, y + h - 1, CY_SILVER, 1)
        for row in range(3):
            yy = y + 4 + row * 5
            for col in range(3):
                cx = x + w // 2 - 6 + col * 6
                tex.rect(cx, yy, cx + 3, yy + 2, (96, 200, 255))
                glw.rect(cx, yy, cx + 3, yy + 2, (96, 200, 255))


def ribbed(color, line=(16, 16, 20), period=3):
    def paint(tex, glw, x, y, w, h, s, face, seed):
        noisy(tex, x, y, x + w, y + h, color, 0.05, seed)
        if face not in ("up", "down"):
            for yy in range(y, y + h):
                if (yy - y) % period == 0:
                    tex.rect(x, yy, x + w, yy + 1, line)
    return paint


def cyberman():
    legs = []
    for side in (-1, 1):
        cx = side * 2.6
        legs.append(bone("right_leg" if side < 0 else "left_leg", (cx, 12.2, 0), [
            box((cx - 2.5, 0, -3.4), (5, 2.0, 6.4), "metal"),
            box((cx - 2.3, 2.0, -2.2), (4.6, 5.6, 4.6), "metal"),
            box((cx - 2.5, 7.6, -2.4), (5.0, 1.4, 5.0), "black"),
            box((cx - 2.4, 9.0, -2.3), (4.8, 4.0, 4.6), "metal")]))
    body = [box((-4.7, 12.4, -2.7), (9.4, 2.4, 5.4), "metal"),
            box((-3.6, 14.8, -2.2), (7.2, 2.6, 4.4), "ribs"),
            box((-4.8, 17.4, -2.9), (9.6, 7.0, 5.8), "metal"),
            box((-3.7, 18.2, -3.5), (7.4, 5.4, 0.7), "chest"),
            box((-3.0, 24.4, -2.0), (6.0, 1.0, 4.0), "black")]
    head = [box((-1.5, 25.4, -1.5), (3.0, 1.2, 3.0), "black"),
            box((-3.6, 26.4, -3.8), (7.2, 6.8, 7.6), "metal", north="face"),
            box((-3.2, 33.2, -3.4), (6.4, 1.3, 6.8), "metal"),
            box((-4.7, 27.4, -1.4), (1.1, 5.2, 2.8), "black"), box((3.6, 27.4, -1.4), (1.1, 5.2, 2.8), "black"),
            box((-4.2, 31.4, -0.8), (0.6, 2.6, 1.6), "black"), box((3.6, 31.4, -0.8), (0.6, 2.6, 1.6), "black"),
            box((-1.6, 28.0, 3.8), (3.2, 4.8, 1.2), "black")]
    arms = []
    for side in (-1, 1):
        sx = side * 6.4
        arms.append(bone("right_arm" if side < 0 else "left_arm", (sx, 23.2, 0), [
            box((sx - 1.9, 22.2, -2.2), (3.8, 2.4, 4.4), "metal"),
            box((sx - 1.7, 17.4, -2.0), (3.4, 4.8, 4.0), "metal"),
            box((sx - 1.9, 16.2, -2.2), (3.8, 1.2, 4.4), "black"),
            box((sx - 1.7, 11.6, -2.0), (3.4, 4.6, 4.0), "metal"),
            box((sx - 1.8, 8.8, -2.1), (3.6, 2.8, 4.2), "black")], "body"))
    bones = [bone("body", (0, 12.4, 0), body)] + legs + [bone("head", (0, 25.4, 0), head, "body")] + arms
    painters = {"metal": mat(CY_SILVER, 0.04), "black": mat((24, 24, 30), 0.05), "face": cyber_face,
                "chest": cyber_chest, "ribs": ribbed((60, 62, 70))}
    write("entity", "cyberman", *build_fx("cyberman", bones, 4, 256, painters, face="metal"))
    animations("cyberman", {
        "idle": (4, True, {"right_arm": {"rotation": swing(3, 4)}, "left_arm": {"rotation": swing(-3, 4)}}),
        "walk": (1.2, True, walk_bones(28, 22, 1.2)),
        "attack": (0.5, False, {"right_arm": {"rotation": {"0.0": [0, 0, 0], "0.2": [-110, 0, 0], "0.5": [0, 0, 0]}}}),
    })


# ---------------------------------------------------------------------------
# Ood
# ---------------------------------------------------------------------------
OOD_SKIN = (204, 204, 208)


def ood_face(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, OOD_SKIN, 0.05, seed)
    if face == "north":
        for ex in (x + w // 4 - 3, x + 3 * w // 4 - 3):
            tex.rect(ex, y + h // 3, ex + 6, y + h // 3 + 7, (12, 12, 16))
            tex.rect(ex + 1, y + h // 3 + 1, ex + 3, y + h // 3 + 3, (190, 190, 200))
        tex.rect(x + w // 2 - 1, y + h * 2 // 3 - 2, x + w // 2 + 1, y + h * 2 // 3, shade(OOD_SKIN, 0.75))


def ood_suit(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, (38, 38, 44), 0.05, seed)
    if face == "north" and w >= 24:
        tex.rect(x + w // 2 - 3, y, x + w // 2 + 3, y + h // 3, (232, 232, 228))      # col de chemise
        tex.rect(x + w // 2 - 1, y + 1, x + w // 2 + 1, y + h - 2, (150, 40, 40))     # cravate
        tex.rect(x + w // 2 - 4, y + h // 2, x + w // 2 - 3, y + h // 2 + 1, (200, 200, 200))


def ood():
    legs = [bone("right_leg", (-2.1, 12, 0), [box((-4.1, 0, -2.6), (4.0, 12, 5.2), "suit")]),
            bone("left_leg", (2.1, 12, 0), [box((0.1, 0, -2.6), (4.0, 12, 5.2), "suit")])]
    body = [box((-4.3, 12, -2.8), (8.6, 12.4, 5.6), "suit")]
    head = [box((-4.2, 24.2, -3.8), (8.4, 5.0, 7.6), "skin", north="face"),
            box((-3.6, 29.2, -3.4), (7.2, 3.6, 6.8), "skin"),
            box((-2.6, 32.8, -2.6), (5.2, 2.0, 5.2), "skin"),
            box((-4.6, 25.4, -1.2), (0.6, 2.2, 2.4), "skin"), box((4.0, 25.4, -1.2), (0.6, 2.2, 2.4), "skin")]
    tendrils = []
    for row, tz in enumerate((-4.6, -6.0)):
        for col, tx in enumerate((-2.6, 0.0, 2.6)):
            h = 12 + ((col + row) % 2) * 2.5
            tendrils.append(box((tx - 0.7, 24.6 - h, tz - 0.7), (1.4, h, 1.4), "tendril"))
    arms = []
    for side in (-1, 1):
        sx = side * 6.0
        cubes = [box((sx - 1.9, 12.0, -2.0), (3.8, 11.0, 4.0), "suit"),
                 box((sx - 1.7, 9.2, -1.8), (3.4, 3.0, 3.6), "skin")]
        if side > 0:
            cubes += [box((sx - 1.6, 8.0, -6.0), (3.2, 3.2, 3.2), "orb"),      # sphère de traduction
                      box((sx - 0.5, 9.2, -3.2), (1.0, 1.0, 3.0), "tendril")]
        arms.append(bone("right_arm" if side < 0 else "left_arm", (sx, 22.4, 0), cubes, "body"))
    bones = [bone("body", (0, 12, 0), body)] + legs + [bone("head", (0, 24, 0), head, "body"),
                                                       bone("tendrils", (0, 24.6, -4.0), tendrils, "head")] + arms
    painters = {"suit": ood_suit, "skin": mat(OOD_SKIN, 0.05), "face": ood_face,
                "tendril": mat((176, 150, 138), 0.1), "orb": mat((255, 200, 120), 0.04, True)}
    write("entity", "ood", *build_fx("ood", bones, 4, 256, painters))
    left = {"left_arm": {"rotation": hold((-55, 0, 0))}}
    walk = walk_bones(25, 20, 1.4)
    walk.update(left)
    walk["tendrils"] = {"rotation": swing(14, 1.4)}
    idle = {"tendrils": {"rotation": swing(10, 3)}}
    idle.update(left)
    animations("ood", {"idle": (3, True, idle), "walk": (1.4, True, walk)})


# ---------------------------------------------------------------------------
# Ange Pleureur
# ---------------------------------------------------------------------------
STONE = (154, 154, 148)


def stone(color=STONE, amount=0.08):
    """Pierre : bruit + veines de pluie, plus sombre vers le bas."""
    def paint(tex, glw, x, y, w, h, s, face, seed):
        rnd = random.Random(seed)
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                k = 1 + rnd.uniform(-amount, amount) - 0.18 * (yy - y) / max(h, 1)
                tex.set(xx, yy, shade(color, k))
        for _ in range(w // 20):
            vx = rnd.randint(x, x + w - 1)
            for yy in range(y, y + h):
                if rnd.random() < 0.5:
                    tex.set(vx, yy, shade(color, 0.84))
    return paint


def feathers(tex, glw, x, y, w, h, s, face, seed):
    stone(shade(STONE, 0.95))(tex, glw, x, y, w, h, s, face, seed)
    if face in ("up", "down"):
        return
    for yy in range(y, y + h):
        for xx in range(x, x + w):
            if (yy - y + (xx - x) // 2) % 5 == 0:
                tex.set(xx, yy, shade(px(tex, xx, yy), 0.7))      # barbes des plumes


def angel_face(tex, glw, x, y, w, h, s, face, seed):
    stone()(tex, glw, x, y, w, h, s, face, seed)
    if face == "north":
        dark = shade(STONE, 0.45)
        for ex in (x + w // 4 - 2, x + 3 * w // 4 - 2):
            tex.rect(ex, y + h // 3, ex + 5, y + h // 3 + 1, dark)           # yeux clos
            tex.rect(ex + 2, y + h // 3 + 1, ex + 3, y + h // 3 + 8, shade(STONE, 0.65))   # larme
        tex.rect(x + w // 2 - 2, y + h * 3 // 4, x + w // 2 + 2, y + h * 3 // 4 + 1, dark)
        tex.rect(x + w // 2 - 1, y + h // 2, x + w // 2 + 1, y + h // 2 + 3, shade(STONE, 0.78))


def weeping_angel():
    robe = [box((-5.6, 0, -4.6), (11.2, 4.0, 9.2), "stone"),
            box((-5.1, 4.0, -4.1), (10.2, 5.0, 8.2), "stone"),
            box((-4.6, 9.0, -3.6), (9.2, 5.0, 7.2), "stone"),
            box((-4.1, 14.0, -3.2), (8.2, 4.0, 6.4), "stone"),
            box((-3.6, 18.0, -2.6), (7.2, 5.4, 5.2), "stone"),
            box((-4.6, 22.4, -2.6), (9.2, 2.2, 5.2), "stone"),            # épaules
            # plis du drapé
            box((-2.4, 0.2, -4.9), (1.0, 3.6, 0.4), "fold"), box((1.4, 0.2, -4.9), (1.0, 3.6, 0.4), "fold"),
            box((-0.6, 4.2, -4.4), (1.2, 4.6, 0.4), "fold"), box((-3.2, 4.2, -4.4), (0.9, 4.6, 0.4), "fold"),
            box((2.3, 4.2, -4.4), (0.9, 4.6, 0.4), "fold"), box((-1.6, 9.2, -3.9), (1.0, 4.6, 0.4), "fold"),
            box((0.8, 9.2, -3.9), (1.0, 4.6, 0.4), "fold"), box((-0.5, 14.2, -3.5), (1.0, 3.6, 0.4), "fold"),
            box((-3.7, 17.5, -2.8), (7.4, 0.8, 0.3), "fold"),             # ceinture
            box((-4.6, 8.0, 2.6), (9.2, 16.0, 1.6), "stone")]             # drapé dans le dos
    # ailes repliées dans le dos : plumes dressées, de plus en plus grandes vers l'extérieur
    wings = []
    for side in (-1, 1):
        for i, (ox, hh) in enumerate(((1.2, 13), (2.8, 17), (4.4, 21), (6.0, 24), (7.6, 22), (9.2, 18), (10.4, 13))):
            x0 = ox if side > 0 else -ox - 1.5
            wings.append(box((x0, 24.6 - hh + 4, 2.7 + (i % 2) * 0.5), (1.5, hh, 1.1 + (i % 2) * 0.4), "wing"))
    head = [box((-1.4, 24.6, -1.4), (2.8, 1.6, 2.8), "stone"),
            box((-3.0, 26.0, -3.0), (6.0, 6.2, 6.0), "stone", north="face"),
            box((-4.0, 31.2, -3.8), (8.0, 2.8, 8.2), "stone"),            # capuche
            box((-4.2, 26.0, 2.0), (8.4, 7.2, 2.6), "stone"),
            box((-4.4, 27.4, -2.4), (0.8, 5.2, 5.4), "stone"), box((3.6, 27.4, -2.4), (0.8, 5.2, 5.4), "stone"),
            box((-3.8, 26.2, -1.2), (0.8, 5.2, 3.8), "stone"), box((3.0, 26.2, -1.2), (0.8, 5.2, 3.8), "stone")]
    arms = []
    for side in (-1, 1):
        sx = side * 4.6
        name = "right" if side < 0 else "left"
        arms.append(bone(name + "_arm", (sx, 23.6, 0), [box((sx - 1.3, 17.6, -1.5), (2.6, 6.4, 3.0), "stone")], "body"))
        arms.append(bone(name + "_forearm", (sx, 17.6, 0),
                         [box((sx - 1.2, 11.4, -1.4), (2.4, 6.4, 2.8), "stone"),
                          box((sx - 1.3, 9.2, -1.5), (2.6, 2.4, 3.0), "stone")], name + "_arm"))
    bones = [bone("body", (0, 0, 0), robe + wings), bone("head", (0, 24.6, 0), head, "body")] + arms
    painters = {"stone": stone(), "fold": stone(shade(STONE, 0.8)), "face": angel_face, "wing": feathers}
    write("entity", "weeping_angel", *build_fx("weeping_angel", bones, 4, 256, painters, face="stone"))
    animations("weeping_angel", {
        # La pose célèbre : mains sur les yeux, tête baissée
        "covering": (1, True, {"right_arm": {"rotation": hold((-70, 22, 0))},
                               "right_forearm": {"rotation": hold((-123, 0, 0))},
                               "left_arm": {"rotation": hold((-70, -22, 0))},
                               "left_forearm": {"rotation": hold((-123, 0, 0))},
                               "head": {"rotation": hold((14, 0, 0))}}),
        # Bras tendus pour attraper sa proie, tête relevée
        "attack": (1, True, {"right_arm": {"rotation": hold((-85, 8, 0))},
                             "right_forearm": {"rotation": hold((-8, 0, 0))},
                             "left_arm": {"rotation": hold((-85, -8, 0))},
                             "left_forearm": {"rotation": hold((-8, 0, 0))},
                             "head": {"rotation": hold((-8, 0, 0))}}),
    })


# ---------------------------------------------------------------------------
# Adipose
# ---------------------------------------------------------------------------
ADI = (238, 236, 226)


def adipose_face(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, ADI, 0.03, seed)
    if face == "north":
        for ex in (x + w // 4 - 2, x + 3 * w // 4 - 3):
            tex.rect(ex, y + h // 4, ex + 5, y + h // 4 + 7, (18, 16, 20))
            tex.rect(ex + 1, y + h // 4 + 1, ex + 3, y + h // 4 + 3, (240, 240, 240))
        tex.rect(x + w // 2 - 3, y + h * 2 // 3, x + w // 2 + 3, y + h * 2 // 3 + 1, (176, 96, 110))
        tex.rect(x + w // 2 - 2, y + h * 2 // 3 + 1, x + w // 2 + 2, y + h * 2 // 3 + 2, (176, 96, 110))
        for cx in (x + 2, x + w - 5):
            tex.rect(cx, y + h // 2 + 2, cx + 3, y + h // 2 + 4, (240, 190, 190))    # joues rosées


def adipose():
    body = [box((-3.2, 1.2, -3.0), (6.4, 5.0, 6.0), "fat"),                # ventre rond
            box((-3.5, 2.5, -2.8), (7.0, 3.0, 5.6), "fat"),
            box((-2.8, 6.0, -2.8), (5.6, 2.6, 5.6), "fat"),
            box((-2.4, 8.4, -2.4), (4.8, 1.0, 4.8), "fat"),
            box((-1.6, 5.8, -3.3), (3.2, 1.6, 0.6), "fat"),               # menton
            box((-0.3, 9.4, -0.3), (0.6, 1.4, 0.6), "tuft"), box((-1.2, 9.2, 0.1), (0.5, 1.2, 0.5), "tuft"),
            box((0.7, 9.2, -0.4), (0.5, 1.2, 0.5), "tuft"),               # petite mèche de cheveux
            box((-2.6, 5.3, -3.0), (5.2, 3.6, 0.5), "face")]
    bones = [bone("body", (0, 0, 0), body),
             bone("right_arm", (-3.4, 5, 0), [box((-4.6, 2.2, -0.8), (1.4, 3.2, 1.6), "fat")], "body"),
             bone("left_arm", (3.4, 5, 0), [box((3.2, 2.2, -0.8), (1.4, 3.2, 1.6), "fat")], "body"),
             bone("right_leg", (-1.4, 1.2, 0), [box((-2.4, 0, -1.6), (2.0, 1.2, 3.0), "fat")]),
             bone("left_leg", (1.4, 1.2, 0), [box((0.4, 0, -1.6), (2.0, 1.2, 3.0), "fat")])]
    painters = {"fat": mat(ADI, 0.03), "tuft": mat((226, 214, 150), 0.05), "face": adipose_face}
    write("entity", "adipose", *build_fx("adipose", bones, 8, 256, painters))
    animations("adipose", {
        "idle": (1.2, True, {"left_arm": {"rotation": {"0.0": [0, 0, -130], "0.3": [0, 0, -160],
                                                        "0.6": [0, 0, -130], "0.9": [0, 0, -160],
                                                        "1.2": [0, 0, -130]}}}),
        "walk": (0.5, True, {"body": {"position": {"0.0": [0, 0, 0], "0.25": [0, 1, 0], "0.5": [0, 0, 0]}},
                             "right_leg": {"rotation": swing(30, 0.5)},
                             "left_leg": {"rotation": swing(-30, 0.5)}}),
    })


# ---------------------------------------------------------------------------
# K9
# ---------------------------------------------------------------------------
K9_SILVER = (166, 170, 176)


def k9_side(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, K9_SILVER, 0.04, seed)
    if face in ("east", "west"):
        for ly in range(y + 3, y + h - 3, 3):
            tex.rect(x + 3, ly, x + w - 3, ly + 1, (60, 62, 68))              # fentes d'aération
    if face == "north":
        tex.frame(x + 1, y + 1, x + w - 1, y + h - 1, (90, 92, 98), 1)


def k9_wheel(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, (30, 30, 34), 0.05, seed)
    if face in ("east", "west"):
        cx, cy = x + w // 2, y + h // 2
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                if math.hypot(xx - cx, yy - cy) <= min(w, h) / 3:
                    tex.set(xx, yy, (136, 140, 146))


def k9_keypad(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, (44, 46, 52), 0.05, seed)
    if face == "up":
        cols = ((255, 90, 80), (90, 220, 120), (250, 210, 80), (110, 170, 250))
        for r in range(2):
            for c in range(4):
                tex.rect(x + 2 + c * 3, y + 2 + r * 3, x + 4 + c * 3, y + 4 + r * 3, cols[(r + c) % 4])


def k9():
    body = [box((-3.2, 3.2, -6.4), (6.4, 5.8, 12.8), "metal"),
            box((-2.6, 9.0, -5.4), (5.2, 0.8, 8.6), "keypad"),
            box((-3.6, 3.0, -5.8), (0.6, 2.4, 11.6), "dark"), box((3.0, 3.0, -5.8), (0.6, 2.4, 11.6), "dark"),
            box((-2.8, 2.4, -6.0), (5.6, 1.0, 12.0), "dark")]
    head = [box((-2.8, 5.0, -11.6), (5.6, 4.2, 5.6), "metal"),
            box((-2.5, 7.4, -12.1), (5.0, 1.2, 0.6), "visor"),                 # visière rouge
            box((-1.0, 5.4, -13.6), (2.0, 1.6, 2.2), "dark"),                   # museau-canon
            box((-0.4, 5.8, -14.0), (0.8, 0.8, 0.6), "visor")]
    ears = [box((-4.6, 8.6, -9.2), (0.8, 5.6, 2.8), "metal"), box((3.8, 8.6, -9.2), (0.8, 5.6, 2.8), "metal"),
            box((-4.7, 9.8, -8.8), (0.2, 2.4, 2.0), "dark"), box((4.5, 9.8, -8.8), (0.2, 2.4, 2.0), "dark")]
    bones = [
        bone("body", (0, 4, 0), body),
        bone("head", (0, 7, -6), head, "body"),
        bone("ears", (0, 9, -8), ears, "head"),
        bone("antenna", (0, 8, 6), [box((-0.25, 8.0, 5.8), (0.5, 0.5, 7.0), "dark"), box((-0.5, 7.7, 12.4), (1.0, 1.0, 1.0), "visor")], "body"),
        bone("leg_fl", (4, 2, -4), [box((2.8, 0, -5.4), (2.4, 3.6, 3.2), "wheel")]),
        bone("leg_fr", (-4, 2, -4), [box((-5.2, 0, -5.4), (2.4, 3.6, 3.2), "wheel")]),
        bone("leg_bl", (4, 2, 4), [box((2.8, 0, 2.4), (2.4, 3.6, 3.2), "wheel")]),
        bone("leg_br", (-4, 2, 4), [box((-5.2, 0, 2.4), (2.4, 3.6, 3.2), "wheel")]),
    ]
    painters = {"metal": k9_side, "dark": mat((46, 48, 54), 0.06), "keypad": k9_keypad,
                "visor": mat((255, 70, 60), 0.02, True), "wheel": k9_wheel}
    write("entity", "k9", *build_fx("k9", bones, 8, 256, painters, metal="metal"))
    animations("k9", {
        "idle": (3, True, {"ears": {"rotation": swing(8, 3, axis=2)}, "antenna": {"rotation": swing(6, 3, axis=1)}}),
        "walk": (0.8, True, {"leg_fl": {"rotation": swing(25, 0.8)}, "leg_br": {"rotation": swing(25, 0.8)},
                             "leg_fr": {"rotation": swing(-25, 0.8)}, "leg_bl": {"rotation": swing(-25, 0.8)},
                             "ears": {"rotation": swing(10, 0.8, axis=2)}}),
    })


# ---------------------------------------------------------------------------
# Sontarien
# ---------------------------------------------------------------------------
SON_SKIN = (118, 104, 86)


def sontarian_face(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, SON_SKIN, 0.09, seed)
    if face == "north":
        dark = (16, 14, 12)
        for ex in (x + w // 3 - 2, x + 2 * w // 3 - 1):
            tex.rect(ex, y + h // 3, ex + 4, y + h // 3 + 3, dark)
            tex.rect(ex, y + h // 3 - 2, ex + 4, y + h // 3 - 1, shade(SON_SKIN, 0.55))      # arcade sourcilière
        tex.rect(x + w // 2 - 1, y + h // 2, x + w // 2 + 1, y + h // 2 + 4, shade(SON_SKIN, 0.6))
        tex.rect(x + w // 4, y + h * 3 // 4, x + 3 * w // 4, y + h * 3 // 4 + 2, (62, 38, 32))
    for yy in range(y + 2, y + h, 5):
        tex.rect(x, yy, x + 2, yy + 1, shade(SON_SKIN, 0.7))


def sontarian_chest(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, (62, 70, 90), 0.06, seed)
    if face == "north":
        tex.frame(x + 2, y + 2, x + w - 2, y + h - 2, (150, 156, 170), 1)
        tex.rect(x + w // 2 - 1, y + 4, x + w // 2 + 1, y + h - 4, (150, 156, 170))
        tex.rect(x + 4, y + h // 2, x + w - 4, y + h // 2 + 1, (150, 156, 170))


def sontarian():
    legs = []
    for side in (-1, 1):
        cx = side * 2.8
        legs.append(bone("right_leg" if side < 0 else "left_leg", (cx, 8.4, 0), [
            box((cx - 2.8, 0, -3.2), (5.6, 3.5, 6.4), "dark"),
            box((cx - 2.6, 3.5, -2.8), (5.2, 5.0, 5.6), "armor")]))
    body = [box((-5.0, 8.2, -3.2), (10.0, 2.4, 6.4), "belt"),
            box((-1.3, 8.2, -3.6), (2.6, 2.4, 0.5), "buckle"),
            box((-5.4, 10.4, -3.4), (10.8, 8.0, 6.8), "armor"),
            box((-4.6, 14.2, -3.9), (9.2, 3.6, 0.6), "chest")]
    head = [box((-4.6, 17.0, -4.0), (9.2, 1.4, 8.0), "armor"),             # col d'armure
            box((-5.8, 18.0, -4.8), (11.6, 8.4, 9.6), "skin", north="face"),
            box((-5.3, 26.4, -4.3), (10.6, 1.6, 8.6), "skin"),
            box((-4.2, 28.0, -3.4), (8.4, 1.0, 6.8), "skin"),
            box((-1.6, 25.6, 4.8), (3.2, 2.6, 1.2), "vent"),                  # évent « probic » dans la nuque
            box((-1.0, 26.0, 5.9), (2.0, 1.6, 0.3), "dark")]
    arms = []
    for side in (-1, 1):
        sx = side * 7.8
        cubes = [box((sx - 2.4, 17.6, -3.2), (4.8, 2.4, 6.4), "armor"),        # épaulière
                 box((sx - 2.2, 8.4, -2.4), (4.4, 9.2, 4.8), "armor"),
                 box((sx - 2.3, 5.4, -2.5), (4.6, 3.2, 5.0), "dark")]
        if side < 0:  # le fusil laser tenu à la hanche
            cubes += [box((sx - 1.3, 4.6, -11.4), (2.6, 3.0, 12.0), "dark"),
                      box((sx - 0.6, 5.3, -14.6), (1.2, 1.2, 3.4), "silver"),
                      box((sx - 1.0, 7.5, -8.4), (2.0, 1.0, 2.2), "silver")]
        arms.append(bone("right_arm" if side < 0 else "left_arm", (sx, 17.4, 0), cubes, "body"))
    bones = [bone("body", (0, 8.2, 0), body)] + legs + [bone("head", (0, 17.6, 0), head, "body")] + arms
    painters = {"armor": mat((64, 72, 94), 0.06), "chest": sontarian_chest, "skin": mat(SON_SKIN, 0.09),
                "face": sontarian_face, "dark": mat((28, 30, 36), 0.05), "belt": mat((36, 36, 42), 0.05),
                "buckle": mat((196, 168, 80), 0.05), "silver": mat((176, 180, 188), 0.05),
                "vent": mat((70, 60, 50), 0.08)}
    write("entity", "sontarian", *build_fx("sontarian", bones, 4, 256, painters))
    animations("sontarian", {
        "idle": (4, True, {"right_arm": {"rotation": swing(1, 4)}}),
        "walk": (1.0, True, walk_bones(26, 14, 1.0)),
        "attack": (0.5, False, {"right_arm": {"rotation": {"0.0": [0, 0, 0], "0.2": [-60, 0, 0], "0.5": [0, 0, 0]}}}),
    })


# ---------------------------------------------------------------------------
# Le Silence
# ---------------------------------------------------------------------------
SIL_SKIN = (158, 168, 156)


def silence_face(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, SIL_SKIN, 0.05, seed)
    if face == "north":
        black = (8, 8, 10)
        for ex in (x + w // 4 - 3, x + 3 * w // 4 - 4):
            tex.rect(ex, y + h // 5, ex + 7, y + h // 5 + 12, black)           # grandes orbites noires
            tex.rect(ex + 1, y + h // 5 - 1, ex + 6, y + h // 5, black)
            tex.rect(ex + 1, y + h // 5 + 12, ex + 6, y + h // 5 + 13, black)
        tex.rect(x + w // 2 - 1, y + h // 2 + 2, x + w // 2, y + h // 2 + 6, shade(SIL_SKIN, 0.5))   # narines
        tex.rect(x + w // 2 + 1, y + h // 2 + 2, x + w // 2 + 2, y + h // 2 + 6, shade(SIL_SKIN, 0.5))
        mx = x + w // 2 - 3                                                    # bouche ouverte (« le cri »)
        tex.rect(mx, y + h * 2 // 3, mx + 6, y + h * 2 // 3 + 9, black)
        tex.rect(mx + 1, y + h * 2 // 3 + 9, mx + 5, y + h * 2 // 3 + 11, black)
    for yy in range(y + 3, y + h, 6):
        tex.rect(x, yy, x + w, yy + 1, shade(SIL_SKIN, 0.9))


def silence():
    legs = []
    for side in (-1, 1):
        cx = side * 1.8
        legs.append(bone("right_leg" if side < 0 else "left_leg", (cx, 17, 0), [
            box((cx - 1.6, 0, -2.6), (3.2, 1.6, 4.6), "shoe"),
            box((cx - 1.4, 1.6, -1.4), (2.8, 15.4, 2.8), "suit")]))
    body = [box((-3.2, 17, -1.9), (6.4, 6.4, 3.8), "suit"),
            box((-3.4, 23.4, -2.1), (6.8, 6.4, 4.2), "suit"),
            box((-1.0, 22.6, -2.4), (2.0, 7.0, 0.5), "shirt"),
            box((-0.5, 22.0, -2.7), (1.0, 6.0, 0.4), "tie")]
    head = [box((-1.2, 29.6, -1.2), (2.4, 1.8, 2.4), "skin"),
            box((-2.9, 31.4, -3.0), (5.8, 2.6, 6.0), "skin"),
            box((-3.6, 34.0, -3.7), (7.2, 7.0, 7.4), "skin", north="face"),
            box((-2.9, 41.0, -3.0), (5.8, 2.6, 6.0), "skin"),
            box((-1.8, 43.6, -1.8), (3.6, 1.2, 3.6), "skin")]
    arms = []
    for side in (-1, 1):
        sx = side * 4.7
        arms.append(bone("right_arm" if side < 0 else "left_arm", (sx, 29.0, 0), [
            box((sx - 1.35, 14.0, -1.35), (2.7, 15.4, 2.7), "suit"),
            box((sx - 1.5, 10.6, -1.5), (3.0, 3.6, 3.0), "skin"),
            box((sx - 1.2, 8.2, -1.0), (0.8, 2.6, 0.8), "skin"), box((sx - 0.1, 8.0, -1.0), (0.8, 2.8, 0.8), "skin"),
            box((sx + 0.9, 8.4, -1.0), (0.8, 2.4, 0.8), "skin")], "body"))
    bones = [bone("body", (0, 17, 0), body)] + legs + [bone("head", (0, 29.6, 0), head, "body")] + arms
    painters = {"suit": mat((20, 20, 24), 0.05), "shirt": mat((214, 214, 208), 0.03), "tie": mat((12, 12, 14), 0.03),
                "shoe": mat((10, 10, 12), 0.03), "skin": mat(SIL_SKIN, 0.05), "face": silence_face}
    write("entity", "silence", *build_fx("silence", bones, 4, 256, painters))
    animations("silence", {
        "idle": (4, True, {"head": {"rotation": swing(5, 4, axis=2)}}),
        "walk": (1.6, True, walk_bones(20, 12, 1.6)),
    })


# ---------------------------------------------------------------------------
# Zygon
# ---------------------------------------------------------------------------
ZYG = (208, 114, 54)


def suckers(color=ZYG):
    def paint(tex, glw, x, y, w, h, s, face, seed):
        noisy(tex, x, y, x + w, y + h, color, 0.08, seed)
        rnd = random.Random(seed)
        for _ in range(max(1, w * h // 50)):          # ventouses : anneau sombre, centre clair
            cx, cy = rnd.randint(x + 2, max(x + 2, x + w - 3)), rnd.randint(y + 2, max(y + 2, y + h - 3))
            tex.rect(cx - 1, cy - 1, cx + 2, cy + 2, shade(color, 0.66))
            tex.set(cx, cy, shade(color, 1.22))
    return paint


def zygon_face(tex, glw, x, y, w, h, s, face, seed):
    suckers()(tex, glw, x, y, w, h, s, face, seed)
    if face == "north":
        for ex in (x + w // 4 - 3, x + 3 * w // 4 - 3):
            tex.rect(ex, y + h // 3, ex + 6, y + h // 3 + 6, (240, 210, 96))
            tex.rect(ex + 2, y + h // 3, ex + 4, y + h // 3 + 6, (24, 16, 10))     # pupille fendue
        for nx in (x + w // 2 - 3, x + w // 2 + 1):
            tex.rect(nx, y + h // 2 + 2, nx + 2, y + h // 2 + 4, (110, 50, 26))
        tex.rect(x + w // 4, y + h * 3 // 4, x + 3 * w // 4, y + h * 3 // 4 + 1, (96, 44, 24))


def zygon():
    legs = []
    for side in (-1, 1):
        cx = side * 2.8
        legs.append(bone("right_leg" if side < 0 else "left_leg", (cx, 13, 0),
                         [box((cx - 2.4, 0, -2.8), (4.8, 13, 5.6), "skin"),
                          box((cx - 2.6, 0, -4.0), (5.2, 1.6, 2.0), "dark")]))
    body = [box((-5.2, 13, -3.2), (10.4, 12.4, 6.4), "skin")]
    for r in range(3):
        for c in range(3):
            body.append(box((-3.6 + c * 3.4, 14.4 + r * 3.4, -3.8), (2.0, 2.0, 0.7), "sucker"))
    head = [box((-4.6, 25.2, -4.4), (9.2, 7.0, 8.8), "skin", north="face"),
            box((-4.0, 32.0, -4.0), (8.0, 1.6, 8.0), "skin"),
            box((-1.0, 33.4, -4.4), (2.0, 2.0, 9.0), "skin"),                    # crête
            box((-5.0, 27.0, -1.0), (0.8, 3.0, 2.4), "skin"), box((4.2, 27.0, -1.0), (0.8, 3.0, 2.4), "skin")]
    arms = []
    for side in (-1, 1):
        sx = side * 7.4
        arms.append(bone("right_arm" if side < 0 else "left_arm", (sx, 24.0, 0), [
            box((sx - 2.4, 21.4, -3.0), (4.8, 3.6, 6.0), "skin"),
            box((sx - 2.2, 11.0, -2.6), (4.4, 11.0, 5.2), "skin"),
            box((sx - 2.4, 8.0, -2.8), (4.8, 3.2, 5.6), "dark"),
            box((sx - 2.2, 5.6, -2.0), (0.9, 2.6, 0.9), "dark"), box((sx - 0.5, 5.4, -2.0), (0.9, 2.8, 0.9), "dark"),
            box((sx + 1.2, 5.6, -2.0), (0.9, 2.6, 0.9), "dark")], "body"))
    bones = [bone("body", (0, 13, 0), body)] + legs + [bone("head", (0, 25.2, 0), head, "body")] + arms
    painters = {"skin": suckers(), "face": zygon_face, "dark": mat((120, 58, 30), 0.06),
                "sucker": mat((246, 168, 96), 0.06)}
    write("entity", "zygon", *build_fx("zygon", bones, 4, 256, painters))
    animations("zygon", {
        "idle": (3, True, {"head": {"rotation": swing(8, 3, axis=1)}}),
        "walk": (1.0, True, walk_bones(30, 25, 1.0)),
        "attack": (0.5, False, {"right_arm": {"rotation": {"0.0": [0, 0, 0], "0.2": [-100, 0, 0], "0.5": [0, 0, 0]}},
                                "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.2": [-100, 0, 0], "0.5": [0, 0, 0]}}}),
    })


# ---------------------------------------------------------------------------
# Slitheen
# ---------------------------------------------------------------------------
SLI = (88, 126, 66)


def slitheen_skin(tex, glw, x, y, w, h, s, face, seed):
    noisy(tex, x, y, x + w, y + h, SLI, 0.09, seed)
    rnd = random.Random(seed)
    for _ in range(max(1, w * h // 60)):          # veines sombres
        vx, vy = rnd.randint(x, x + w - 1), rnd.randint(y, y + h - 1)
        for k in range(rnd.randint(3, 7)):
            tex.set(min(x + w - 1, vx + (k % 2)), min(y + h - 1, vy + k), shade(SLI, 0.62))


def slitheen_face(tex, glw, x, y, w, h, s, face, seed):
    slitheen_skin(tex, glw, x, y, w, h, s, face, seed)
    if face == "north":
        tex.rect(x + w // 2 - 1, y, x + w // 2 + 1, y + h // 3, (44, 66, 36))    # fermeture éclair du front
        for zy in range(y, y + h // 3, 2):
            tex.rect(x + w // 2 - 2, zy, x + w // 2 + 2, zy + 1, (190, 190, 180))
        for ex in (x + w // 4 - 2, x + 3 * w // 4 - 3):
            tex.rect(ex, y + h // 2 - 3, ex + 5, y + h // 2 + 2, (236, 196, 52))
            tex.rect(ex + 1, y + h // 2 - 2, ex + 4, y + h // 2 + 1, (22, 14, 8))
            tex.rect(ex - 1, y + h // 2 - 5, ex + 6, y + h // 2 - 3, shade(SLI, 0.55))     # arcade sourcilière
        tex.rect(x + w // 2 - 1, y + h * 2 // 3 - 4, x + w // 2 + 1, y + h * 2 // 3 - 1, shade(SLI, 0.55))
        tex.rect(x + 2, y + h * 3 // 4, x + w - 2, y + h * 3 // 4 + 3, (56, 28, 30))


def slitheen():
    legs = []
    for side in (-1, 1):
        cx = side * 3.8
        legs.append(bone("right_leg" if side < 0 else "left_leg", (cx, 14, 0), [
            box((cx - 2.9, 0, -3.4), (5.8, 14, 6.8), "skin"),
            box((cx - 3.1, 0, -5.4), (6.2, 2.0, 3.0), "dark")]))
    body = [box((-7.0, 14, -4.6), (14.0, 16.0, 9.2), "skin"),
            box((-5.4, 14.6, -5.8), (10.8, 9.0, 1.4), "belly"),
            box((-8.0, 27.0, -4.6), (16.0, 3.4, 9.2), "skin")]
    head = [box((-5.6, 30.4, -5.6), (11.2, 9.0, 11.2), "skin", north="face"),
            box((-6.0, 37.0, -6.2), (12.0, 2.4, 1.6), "dark"),                   # arcade sourcilière
            box((-5.2, 29.0, -7.4), (10.4, 3.0, 2.2), "skin"),                   # mâchoire
            box((-0.7, 32.6, -7.0), (1.4, 2.6, 1.4), "skin")]                    # museau
    for i in range(6):
        head.append(box((-4.4 + i * 1.8, 31.8, -7.7), (0.9, 1.5, 0.5), "tooth"))
    arms = []
    for side in (-1, 1):
        sx = side * 10.6
        arms.append(bone("right_arm" if side < 0 else "left_arm", (sx, 28.2, 0), [
            box((sx - 3.0, 12.4, -3.2), (6.0, 15.8, 6.4), "skin"),
            box((sx - 3.2, 9.0, -3.4), (6.4, 3.6, 6.8), "dark"),
            box((sx - 3.0, 5.4, -3.0), (1.3, 3.8, 1.3), "claw"), box((sx - 0.6, 5.0, -3.0), (1.3, 4.2, 1.3), "claw"),
            box((sx + 1.7, 5.4, -3.0), (1.3, 3.8, 1.3), "claw")], "body"))
    bones = [bone("body", (0, 14, 0), body)] + legs + [bone("head", (0, 30, 0), head, "body")] + arms
    painters = {"skin": slitheen_skin, "face": slitheen_face, "dark": mat((46, 70, 38), 0.08),
                "belly": mat((128, 160, 92), 0.08), "tooth": mat((236, 232, 218), 0.04), "claw": mat((38, 36, 30), 0.05)}
    write("entity", "slitheen", *build_fx("slitheen", bones, 4, 256, painters))
    animations("slitheen", {
        "idle": (4, True, {"right_arm": {"rotation": swing(3, 4)}, "left_arm": {"rotation": swing(-3, 4)}}),
        "walk": (1.4, True, walk_bones(24, 24, 1.4)),
        "attack": (0.6, False, {"right_arm": {"rotation": {"0.0": [0, 0, 0], "0.25": [-110, 0, 0], "0.6": [0, 0, 0]}},
                                "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.25": [-110, 0, 0], "0.6": [0, 0, 0]}}}),
    })


# ---------------------------------------------------------------------------
# Golem de redstone (mini-boss, inspiré de Minecraft Dungeons) : pierre noire
# parcourue de veines de redstone lumineuses, poitrine au cœur rouge, bras énormes
# ---------------------------------------------------------------------------
GOLEM_ROCK = (58, 52, 58)
GOLEM_RED = (232, 44, 30)


def golem_rock(tex, glw, x, y, w, h, s, face, seed):
    """Pierre sombre fissurée : les fissures sont des veines de redstone qui brillent."""
    rnd = random.Random(seed)
    noisy(tex, x, y, x + w, y + h, GOLEM_ROCK, 0.1, seed)
    for _ in range(max(1, w * h // 90)):
        cx, cy = rnd.randint(x, x + w - 1), rnd.randint(y, y + h - 1)
        for _step in range(rnd.randint(3, 9)):
            if x <= cx < x + w and y <= cy < y + h:
                tex.set(cx, cy, GOLEM_RED)
                glw.set(cx, cy, GOLEM_RED)
            cx += rnd.choice((-1, 0, 1))
            cy += rnd.choice((0, 1, 1))


def golem_face(tex, glw, x, y, w, h, s, face, seed):
    golem_rock(tex, glw, x, y, w, h, s, face, seed)
    if face == "north":
        for ex in (x + w // 4 - 2, x + 3 * w // 4 - 3):
            tex.rect(ex, y + h // 3, ex + 6, y + h // 3 + 4, (255, 90, 60))
            glw.rect(ex, y + h // 3, ex + 6, y + h // 3 + 4, (255, 90, 60))
        tex.rect(x + w // 4, y + h * 3 // 4, x + 3 * w // 4, y + h * 3 // 4 + 2, (20, 16, 18))


def golem_core(tex, glw, x, y, w, h, s, face, seed):
    tex.rect(x, y, x + w, y + h, GOLEM_RED)
    glw.rect(x, y, x + w, y + h, GOLEM_RED)
    tex.rect(x + w // 4, y + h // 4, x + 3 * w // 4, y + 3 * h // 4, (255, 170, 120))
    glw.rect(x + w // 4, y + h // 4, x + 3 * w // 4, y + 3 * h // 4, (255, 170, 120))


def redstone_golem():
    legs = []
    for side in (-1, 1):
        lx = side * 7
        legs.append(bone("right_leg" if side < 0 else "left_leg", (lx, 18, 0), [
            box((lx - 5, 0, -6), (10, 4, 11), "rock"),
            box((lx - 4, 4, -4), (8, 14, 8), "rock")]))
    body = [box((-11, 18, -7), (22, 9, 14), "rock"),                 # bassin
            box((-15, 27, -10), (30, 19, 19), "rock"),               # torse voûté
            box((-17, 42, -8), (34, 6, 14), "rock"),                 # épaules
            box((-4.5, 32, -10.6), (9, 9, 0.6), "core")]             # cœur de redstone
    for i, (cx, cy, cz, h) in enumerate(((-9, 46, 2, 7), (-3, 47, 4, 10), (4, 47, 3, 8), (10, 46, 1, 6), (0, 44, 8, 6))):
        body.append(box((cx - 1.5, cy, cz - 1.5), (3, h, 3), "crystal"))   # cristaux sur le dos
    head = [box((-5, 41, -16), (10, 8, 8), "rock", north="face")]
    arms = []
    for side in (-1, 1):
        ax = side * 21
        arms.append(bone("right_arm" if side < 0 else "left_arm", (ax, 44, -1), [
            box((ax - 5, 30, -6), (10, 16, 10), "rock"),             # bras
            box((ax - 4, 26, -5), (8, 4, 8), "crystal"),             # coude lumineux
            box((ax - 6.5, 4, -7.5), (13, 22, 13), "rock")], "body"))  # avant-bras et poing
    bones = [bone("body", (0, 18, 0), body)] + legs + [bone("head", (0, 42, -8), head, "body")] + arms
    painters = {"rock": golem_rock, "face": golem_face, "core": golem_core,
                "crystal": mat(GOLEM_RED, 0.12, glow=True)}
    write("entity", "redstone_golem", *build_fx("redstone_golem", bones, 2, 512, painters, rock="stone", face="stone"))
    animations("redstone_golem", {
        "idle": (3, True, {"body": {"position": {"0.0": [0, 0, 0], "1.5": [0, -0.6, 0], "3.0": [0, 0, 0]}},
                           "right_arm": {"rotation": swing(3, 3)}, "left_arm": {"rotation": swing(-3, 3)}}),
        "walk": (1.6, True, {"right_leg": {"rotation": swing(22, 1.6)}, "left_leg": {"rotation": swing(-22, 1.6)},
                             "right_arm": {"rotation": swing(-16, 1.6)}, "left_arm": {"rotation": swing(16, 1.6)},
                             "body": {"rotation": swing(4, 1.6, axis=2)}}),
        # Frappe : les deux poings montent puis s'abattent
        "slam": (1.0, False, {
            "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.4": [-150, 0, 0], "0.6": [15, 0, 0], "1.0": [0, 0, 0]}},
            "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.4": [-150, 0, 0], "0.6": [15, 0, 0], "1.0": [0, 0, 0]}},
            "body": {"rotation": {"0.0": [0, 0, 0], "0.4": [-10, 0, 0], "0.6": [12, 0, 0], "1.0": [0, 0, 0]}}}),
        # Onde de choc : il se redresse, bras écartés, puis frappe le sol
        "shockwave": (1.5, False, {
            "right_arm": {"rotation": {"0.0": [0, 0, 0], "0.8": [-170, 0, 25], "1.0": [20, 0, 0], "1.5": [0, 0, 0]}},
            "left_arm": {"rotation": {"0.0": [0, 0, 0], "0.8": [-170, 0, -25], "1.0": [20, 0, 0], "1.5": [0, 0, 0]}},
            "body": {"rotation": {"0.0": [0, 0, 0], "0.8": [-18, 0, 0], "1.0": [15, 0, 0], "1.5": [0, 0, 0]}}}),
    })


# ---------------------------------------------------------------------------
def prune_empty_glowmasks():
    """GeckoLib plante sur une texture lumineuse sans aucun pixel : on supprime celles qui sont vides."""
    base = os.path.join(ROOT, "textures", "entity")
    for dirpath, _, files in os.walk(base):
        for f in files:
            if f.endswith("_glowmask.png"):
                path = os.path.join(dirpath, f)
                w, h, rows = read_png(path)
                if not any(rows[y][x * 4 + 3] for y in range(h) for x in range(w)):
                    os.remove(path)
                    print("   (glowmask vide supprime :", f + ")")


if __name__ == "__main__":
    random.seed(1963)
    dalek()
    cyberman()
    ood()
    weeping_angel()
    adipose()
    k9()
    sontarian()
    silence()
    zygon()
    slitheen()
    redstone_golem()
    prune_empty_glowmasks()
