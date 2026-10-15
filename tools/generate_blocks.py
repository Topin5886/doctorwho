"""
Générateur des blocs « classiques » du mod (modèles JSON vanilla + textures) :
décor de la salle de console (style Dalek Mod) et sols des planètes.

Les modèles JSON sont rendus avec les chunks (pas de BlockEntityRenderer) :
c'est le plus léger et le plus compatible avec Sodium / Iris.

Utilisation (depuis la racine du projet) :
    python tools/generate_blocks.py
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from generate_assets import Image, shade  # noqa: E402

RES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
ASSETS = os.path.join(RES, "assets", "doctorwho")
DATA = os.path.join(RES, "data", "doctorwho")


def save_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2)


def tex(name, img):
    img.save(os.path.join(ASSETS, "textures", "block", name + ".png"))


def noise_fill(img, color, amount, seed, x0=0, y0=0, x1=None, y1=None):
    rnd = random.Random(seed)
    for y in range(y0, y1 or img.h):
        for x in range(x0, x1 or img.w):
            img.set(x, y, shade(color, 1 + rnd.uniform(-amount, amount)))


# ---------------------------------------------------------------------------
# Textures
# ---------------------------------------------------------------------------
AMBER = (201, 138, 61)


def roundel(lit, base=AMBER, hollow=(231, 183, 106), glow=((255, 205, 130), (255, 250, 240))):
    img = Image(32, 32)
    noise_fill(img, base, 0.05, 11)
    cx = cy = 15.5
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - cx, y - cy)
            if d <= 11.5:
                if d > 10.5:
                    img.set(x, y, shade(base, 0.62))        # bord creusé
                elif lit:
                    k = 1 - d / 10.5
                    a, b = glow
                    img.set(x, y, tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3)))
                else:
                    # creux ombré en haut à gauche, éclairé en bas à droite
                    k = ((x - cx) + (y - cy)) / 22
                    img.set(x, y, shade(hollow, 0.9 + 0.2 * k))
    img.frame(0, 0, 32, 32, shade(base, 0.8), 1)
    return img


def coral_wall():
    img = Image(16, 16)
    rnd = random.Random(5)
    for y in range(16):
        for x in range(16):
            v = math.sin(x * 0.9 + math.sin(y * 0.7) * 2) + math.sin(y * 1.3 + x * 0.3)
            base = (196, 106, 58) if v > -0.4 else (150, 72, 40)
            img.set(x, y, shade(base, 1 + rnd.uniform(-0.08, 0.08)))
    return img


def grating():
    img = Image(16, 16)  # trous entièrement transparents (rendu « cutout »)
    for y in range(16):
        for x in range(16):
            if x % 4 in (0, 1) or y % 4 in (0, 1) or x in (0, 15) or y in (0, 15):
                k = 1.15 if (x % 4 == 0 or y % 4 == 0) else 0.9
                img.set(x, y, shade((70, 74, 80), k))
    return img


def hex_light():
    img = Image(16, 16)
    noise_fill(img, (40, 34, 30), 0.08, 3)
    cx, cy, r = 7.5, 7.5, 6.2
    for y in range(16):
        for x in range(16):
            # distance « hexagonale »
            dx, dy = abs(x - cx), abs(y - cy)
            d = max(dx * 0.866 + dy * 0.5, dy)
            if d <= r:
                k = 1 - d / r
                img.set(x, y, (255, int(190 + 60 * k), int(100 + 120 * k)))
    return img


def coral_pillar():
    img = Image(16, 16)
    rnd = random.Random(9)
    for y in range(16):
        for x in range(16):
            stripe = math.sin(x * 1.6 + math.sin(y * 0.5) * 1.5)
            base = (190, 98, 52) if stripe > 0 else (140, 66, 36)
            img.set(x, y, shade(base, 1 + rnd.uniform(-0.07, 0.07)))
    return img


def copper_beam():
    img = Image(16, 16)
    noise_fill(img, (184, 115, 51), 0.06, 4)
    img.rect(0, 0, 1, 16, (120, 70, 30))
    img.rect(15, 0, 16, 16, (120, 70, 30))
    for y in (2, 13):
        for x in (3, 12):
            img.set(x, y, (235, 170, 100))
    return img


def solid_tex(color, amount, seed):
    img = Image(16, 16)
    noise_fill(img, color, amount, seed)
    return img


def lamp_glass():
    img = Image(16, 16)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5) / 10.6
            img.set(x, y, (255, int(250 - 40 * d), int(220 - 90 * d)))
    return img


def moon_dust():
    img = Image(16, 16)
    noise_fill(img, (185, 185, 180), 0.07, 21)
    return img


def moon_rock():
    img = Image(16, 16)
    noise_fill(img, (138, 138, 134), 0.08, 22)
    rnd = random.Random(23)
    for _ in range(4):  # petits cratères
        cx, cy, r = rnd.randint(2, 13), rnd.randint(2, 13), rnd.choice((1.5, 2, 2.5))
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - cx, y - cy)
                if d <= r:
                    img.set(x, y, shade((138, 138, 134), 0.75 if d < r - 0.7 else 1.15))
    return img


def skaro_sand():
    img = Image(16, 16)
    noise_fill(img, (181, 100, 58), 0.09, 31)
    return img


def skaro_rock():
    img = Image(16, 16)
    noise_fill(img, (110, 58, 42), 0.08, 32)
    rnd = random.Random(33)
    x, y = rnd.randint(0, 15), 0
    while y < 16:  # fissure
        img.set(x, y, (60, 30, 22))
        x = max(0, min(15, x + rnd.choice((-1, 0, 1))))
        y += 1
    return img


GALLIFREY_RED = (178, 34, 40)


def gallifrey_grass_top():
    img = Image(16, 16)
    noise_fill(img, GALLIFREY_RED, 0.14, 41)
    return img


def gallifrey_grass_side():
    img = Image(16, 16)
    noise_fill(img, (122, 82, 53), 0.1, 42)
    rnd = random.Random(43)
    for x in range(16):
        depth = rnd.choice((2, 3, 3, 4))
        for y in range(depth):
            img.set(x, y, shade(GALLIFREY_RED, 1 + rnd.uniform(-0.12, 0.12)))
    return img


def gallifrey_log_side():
    img = Image(16, 16)
    rnd = random.Random(51)
    for x in range(16):
        tone = rnd.uniform(0.85, 1.12)
        for y in range(16):
            img.set(x, y, shade((176, 172, 160), tone * (1 + rnd.uniform(-0.05, 0.05))))
    for x in (3, 9, 13):
        for y in range(16):
            if rnd.random() < 0.8:
                img.set(x, y, (120, 112, 100))
    return img


def gallifrey_log_top():
    img = Image(16, 16)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 6.8:
                img.set(x, y, (150, 146, 134))
            else:
                img.set(x, y, shade((214, 206, 182), 0.92 if int(d) % 2 else 1.04))
    return img


def gallifrey_leaves():
    img = Image(16, 16)  # trous transparents (rendu « cutout »)
    rnd = random.Random(52)
    for y in range(16):
        for x in range(16):
            if rnd.random() < 0.78:
                k = rnd.uniform(0.8, 1.15)
                img.set(x, y, shade((206, 214, 222), k))
    return img


# ---------------------------------------------------------------------------
# Modèles
# ---------------------------------------------------------------------------
def face(t, uv=None):
    f = {"texture": t}
    if uv:
        f["uv"] = uv
    return f


def box(frm, to, t, rotation=None, uv=False):
    faces = {}
    for d in ("north", "south", "east", "west", "up", "down"):
        faces[d] = face(t)
    e = {"from": frm, "to": to, "faces": faces}
    if rotation:
        e["rotation"] = rotation
    return e


def model_cube_all(name, texture=None):
    return {"parent": "minecraft:block/cube_all", "textures": {"all": "doctorwho:block/" + (texture or name)}}


def model_elements(textures, elements, parent="minecraft:block/block"):
    return {"parent": parent, "textures": textures, "elements": elements}


BLOCKS = {}


def simple_block(name, model, facing=False, item_model=None, axis=False, blockstate=None, extra_models=None):
    """Déclare un bloc. blockstate : fichier d'états personnalisé ; extra_models : autres modèles {nom: json}."""
    BLOCKS[name] = {"model": model, "facing": facing, "item_model": item_model, "axis": axis,
                    "blockstate": blockstate, "extra_models": extra_models or {}}


def build_blocks():
    # --- Décor de la salle de console -------------------------------------------
    tex("roundel_wall", roundel(False))
    tex("roundel_wall_lit", roundel(True))
    tex("coral_wall", coral_wall())
    tex("metal_grating", grating())
    tex("hex_light", hex_light())
    tex("coral_pillar", coral_pillar())
    tex("copper_beam", copper_beam())
    tex("bronze", solid_tex((96, 64, 38), 0.07, 6))
    tex("brass", solid_tex((156, 122, 69), 0.06, 7))
    tex("lamp_glass", lamp_glass())

    simple_block("roundel_wall", model_cube_all("roundel_wall"))
    simple_block("roundel_wall_lit", model_cube_all("roundel_wall_lit"))
    simple_block("coral_wall", model_cube_all("coral_wall"))
    simple_block("metal_grating", model_cube_all("metal_grating"))
    simple_block("hex_light", model_cube_all("hex_light"))

    coral = "#coral"
    simple_block("coral_pillar", model_elements(
        {"coral": "doctorwho:block/coral_pillar", "particle": "doctorwho:block/coral_pillar"},
        [box([5, 0, 5], [11, 16, 11], coral),
         box([4, 3, 6], [5, 7, 10], coral),
         box([11, 9, 5], [12, 13, 9], coral),
         box([6, 12, 4], [10, 15, 5], coral),
         box([7, 0, 11], [9, 16, 12], coral),
         box([6, 1, 4.5], [9, 5, 5], coral)]))

    # Branche de corail en biais : penche vers le nord (rotations par l'état de bloc)
    simple_block("coral_strut", model_elements(
        {"coral": "doctorwho:block/coral_pillar", "particle": "doctorwho:block/coral_pillar"},
        [box([6, -4, 6], [10, 20, 10], coral, {"origin": [8, 8, 8], "axis": "x", "angle": -45}),
         box([5, 0, 12], [11, 4, 16], coral),
         box([7, 9, 3], [9, 13, 5], coral, {"origin": [8, 8, 8], "axis": "x", "angle": -22.5})]),
        facing=True)

    simple_block("copper_beam", model_elements(
        {"beam": "doctorwho:block/copper_beam", "particle": "doctorwho:block/copper_beam"},
        [box([5, 0, 5], [11, 16, 11], "#beam")]))

    # Applique murale (dos contre le mur au sud quand elle regarde au nord)
    simple_block("wall_lamp", model_elements(
        {"metal": "doctorwho:block/bronze", "glass": "doctorwho:block/lamp_glass", "particle": "doctorwho:block/bronze"},
        [box([5, 3, 15], [11, 13, 16], "#metal"),
         box([7, 7, 13], [9, 9, 15], "#metal"),
         box([6, 5, 9], [10, 11, 13], "#glass"),
         box([5.5, 11, 8.5], [10.5, 12, 13.5], "#metal"),
         box([5.5, 4, 8.5], [10.5, 5, 13.5], "#metal")]),
        facing=True)

    # Rambarde en laiton : se raccorde à ses voisines comme une vitre (poteau + un bras par côté relié)
    metal = {"metal": "doctorwho:block/brass", "particle": "doctorwho:block/brass"}
    railing_side = model_elements(metal, [box([7, 14, 0], [9, 16, 8], "#metal"), box([7.5, 7, 0], [8.5, 8.5, 8], "#metal")])
    railing_state = {"multipart": [{"apply": {"model": "doctorwho:block/railing"}}] + [
        {"when": {side: "true"}, "apply": {"model": "doctorwho:block/railing_side", "y": y}}
        for side, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))]}
    simple_block("railing", model_elements(metal, [box([6.5, 0, 6.5], [9.5, 16, 9.5], "#metal"),
                                                   box([6, 14, 6], [10, 16, 10], "#metal")]),
                 blockstate=railing_state, extra_models={"railing_side": railing_side},
                 item_model=model_elements(metal, [box([0, 0, 7], [2, 16, 9], "#metal"), box([14, 0, 7], [16, 16, 9], "#metal"),
                                                   box([0, 14, 6.5], [16, 16, 9.5], "#metal"),
                                                   box([0, 7, 7.5], [16, 8.5, 8.5], "#metal")]))

    # Murs à roundels des autres décors d'intérieur
    themes = {
        "white": ((226, 226, 220), (246, 246, 240), ((235, 240, 255), (255, 255, 255))),
        "dark": ((58, 60, 66), (88, 92, 100), ((120, 190, 255), (225, 245, 255))),
        "copper": ((166, 98, 50), (205, 140, 82), ((255, 170, 90), (255, 236, 200))),
    }
    for theme, (base, hollow, glow) in themes.items():
        tex(theme + "_roundel_wall", roundel(False, base, hollow, glow))
        tex(theme + "_roundel_wall_lit", roundel(True, base, hollow, glow))
        simple_block(theme + "_roundel_wall", model_cube_all(theme + "_roundel_wall"))
        simple_block(theme + "_roundel_wall_lit", model_cube_all(theme + "_roundel_wall_lit"))

    interior_blocks()

    # --- Planètes ---------------------------------------------------------------
    tex("moon_dust", moon_dust())
    tex("moon_rock", moon_rock())
    tex("skaro_sand", skaro_sand())
    tex("skaro_rock", skaro_rock())
    tex("gallifrey_grass_top", gallifrey_grass_top())
    tex("gallifrey_grass_side", gallifrey_grass_side())
    tex("gallifrey_log", gallifrey_log_side())
    tex("gallifrey_log_top", gallifrey_log_top())
    tex("gallifrey_leaves", gallifrey_leaves())
    simple_block("gallifrey_log", {"parent": "minecraft:block/cube_column", "textures": {
        "end": "doctorwho:block/gallifrey_log_top", "side": "doctorwho:block/gallifrey_log"}}, axis=True)
    simple_block("gallifrey_leaves", model_cube_all("gallifrey_leaves"))
    for name in ("moon_dust", "moon_rock", "skaro_sand", "skaro_rock"):
        simple_block(name, model_cube_all(name))
    simple_block("gallifrey_grass", {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "top": "doctorwho:block/gallifrey_grass_top",
        "side": "doctorwho:block/gallifrey_grass_side",
        "bottom": "minecraft:block/dirt"}})

    # --- Fichiers : modèles, états, objets, butin --------------------------------
    for name, spec in BLOCKS.items():
        save_json(os.path.join(ASSETS, "models", "block", name + ".json"), spec["model"])
        for extra, model in spec["extra_models"].items():
            save_json(os.path.join(ASSETS, "models", "block", extra + ".json"), model)
        if spec["blockstate"]:
            save_json(os.path.join(ASSETS, "blockstates", name + ".json"), spec["blockstate"])
        elif spec["axis"]:
            m = "doctorwho:block/" + name
            variants = {"axis=y": {"model": m}, "axis=z": {"model": m, "x": 90},
                        "axis=x": {"model": m, "x": 90, "y": 90}}
        elif spec["facing"]:
            variants = {"facing=%s" % f: {"model": "doctorwho:block/" + name, "y": y}
                        for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}
        else:
            variants = {"": {"model": "doctorwho:block/" + name}}
        if not spec["blockstate"]:
            save_json(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": variants})
        save_json(os.path.join(ASSETS, "models", "item", name + ".json"),
                  spec["item_model"] or {"parent": "doctorwho:block/" + name})
        save_json(os.path.join(DATA, "loot_tables", "blocks", name + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": "doctorwho:" + name}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    print("OK", len(BLOCKS), "blocs :", ", ".join(BLOCKS))


# ---------------------------------------------------------------------------
# Décor des grandes salles de console (thèmes d'intérieur)
# ---------------------------------------------------------------------------
# Familles de roundels en relief : couleur du panneau, du creux éteint, et dégradé du disque allumé.
# Pour un nouveau thème : ajouter une ligne ici, puis le nom dans ModBlocks.ROUNDEL_FAMILIES.
ROUNDEL_FAMILIES = {
    "classic": ((232, 232, 226), (206, 206, 200), ((255, 226, 170), (255, 252, 236))),
    "copper": ((178, 106, 56), (140, 80, 40), ((255, 150, 60), (255, 236, 186))),
    "amber": ((204, 140, 62), (166, 104, 42), ((255, 186, 84), (255, 246, 214))),
    # 4e valeur facultative : couleur du liseré autour du creux
    "dark": ((50, 50, 56), (30, 30, 34), ((140, 196, 255), (236, 248, 255)), (196, 156, 72)),
    "cathedral": ((52, 66, 120), (34, 44, 86), ((120, 160, 255), (232, 240, 255)), (196, 156, 72)),
}
# Grilles de sol ajourées (on voit les lumières en dessous) : couleur des barreaux.
GRATE_FAMILIES = {
    "copper": (190, 116, 60),
    "amber": (150, 98, 42),
    "dark": (66, 68, 74),
}
# Lumières de sous-plancher colorées : couleur du bord et du cœur (niveau de lumière 15).
UNDERLIGHTS = {
    "green": ((40, 150, 90), (190, 255, 210)),
    "orange": ((200, 90, 20), (255, 222, 160)),
    "cyan": ((30, 170, 170), (210, 255, 250)),
}
HOLE = 12.5   # rayon du trou (sur 32 px)


def roundel_front(panel, rim=None):
    """Face avant : panneau percé d'un trou rond (transparent), avec un biseau sombre autour."""
    img = Image(32, 32)
    noise_fill(img, panel, 0.025, 21)
    c = 15.5
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - c, y - c)
            if d <= HOLE:
                img.px[(y * 32 + x) * 4 + 3] = 0
            elif d <= HOLE + 1.6:
                # biseau : ombre en haut à gauche, lumière en bas à droite
                k = 0.78 + 0.16 * ((x - c) + (y - c)) / (2 * HOLE)
                img.set(x, y, shade(rim or panel, k if rim is None else k + 0.25))
    img.frame(0, 0, 32, 32, shade(panel, 0.86), 1)
    return img


def roundel_disc(panel, hollow, glow, lit):
    """Fond du creux : disque éteint (ombré) ou allumé (dégradé lumineux)."""
    img = Image(32, 32)
    c = 15.5
    for y in range(32):
        for x in range(32):
            d = math.hypot(x - c, y - c)
            if lit:
                k = max(0.0, 1 - d / (HOLE + 1))
                a, b = glow
                img.set(x, y, tuple(int(a[i] + (b[i] - a[i]) * k) for i in range(3)))
            else:
                k = 0.86 + 0.12 * ((x - c) + (y - c)) / 32 + (0.06 if d < HOLE - 3 else 0)
                img.set(x, y, shade(hollow, k))
    return img


def roundel_model(front, disc, side):
    """Bloc plein dont la face nord porte un disque creusé de 3 px (regarde au nord par défaut)."""
    t = {"front": front, "disc": disc, "side": side, "particle": side}
    back = {"from": [0, 0, 3], "to": [16, 16, 16], "faces": {
        "north": {"texture": "#disc"},
        "south": {"texture": "#side", "cullface": "south"},
        "east": {"texture": "#side", "cullface": "east"},
        "west": {"texture": "#side", "cullface": "west"},
        "up": {"texture": "#side", "cullface": "up"},
        "down": {"texture": "#side", "cullface": "down"}}}
    plate = {"from": [0, 0, 0], "to": [16, 16, 3], "faces": {
        "north": {"texture": "#front", "cullface": "north"},
        "east": {"texture": "#side", "cullface": "east"},
        "west": {"texture": "#side", "cullface": "west"},
        "up": {"texture": "#side", "cullface": "up"},
        "down": {"texture": "#side", "cullface": "down"}}}
    return {"parent": "minecraft:block/block", "textures": t, "elements": [back, plate]}


def pilaster(panel):
    """Pilastre cannelé (côtés) : cannelures verticales."""
    img = Image(16, 16)
    noise_fill(img, panel, 0.02, 31)
    for x in range(16):
        k = {0: 0.8, 1: 0.95, 2: 1.04, 3: 0.9}[x % 4]
        for y in range(16):
            img.set(x, y, shade(panel, k * (1 + 0.02 * ((x * 7 + y * 3) % 3 - 1))))
    return img


def light_panel():
    img = Image(16, 16)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5), abs(y - 7.5)) / 7.5
            img.set(x, y, shade((255, 250, 236), 1 - 0.08 * d))
    img.frame(0, 0, 16, 16, (214, 212, 204), 1)
    return img


def scanner_screen():
    """Écran du scanner : une galaxie spirale turquoise sur fond sombre."""
    img = Image(32, 32)
    rnd = random.Random(8)
    for y in range(32):
        for x in range(32):
            img.set(x, y, (8 + rnd.randint(0, 6), 14 + rnd.randint(0, 6), 24 + rnd.randint(0, 8)))
    c = 15.5
    for arm in range(2):
        for s in range(160):
            a = s * 0.07 + arm * math.pi
            r = s * 0.085
            x, y = c + math.cos(a) * r, c + math.sin(a) * r * 0.7
            for dx in (0, 1):
                k = 1 - s / 200
                img.set(int(x) + dx, int(y), (int(80 * k + 40), int(230 * k + 20), int(220 * k + 30)))
    for y in range(13, 19):
        for x in range(13, 19):
            if math.hypot(x - c, y - c) < 3:
                img.set(x, y, (220, 255, 250))
    img.frame(0, 0, 32, 32, (60, 64, 70), 1)
    return img


def floor_grate(color):
    """Grille en losanges, trous transparents (rendu « cutout »)."""
    img = Image(16, 16)
    for y in range(16):
        for x in range(16):
            u, v = (x + y) % 8, (x - y) % 8
            if u in (0, 1) or v in (0, 1) or x in (0, 15) or y in (0, 15):
                k = 1.18 if (u == 0 or v == 0) else 0.86
                if x in (0, 15) or y in (0, 15):
                    k = 0.72
                img.set(x, y, shade(color, k))
    return img


def underlight(edge, core):
    """Lumière hexagonale encastrée, du bord coloré au cœur presque blanc."""
    img = Image(16, 16)
    noise_fill(img, shade(edge, 0.45), 0.06, 51)
    cx = cy = 7.5
    for y in range(16):
        for x in range(16):
            dx, dy = abs(x - cx), abs(y - cy)
            d = max(dx * 0.866 + dy * 0.5, dy) / 6.8
            if d <= 1:
                img.set(x, y, tuple(int(edge[i] + (core[i] - edge[i]) * (1 - d)) for i in range(3)))
    return img


def riveted_panel(color, seed):
    """Plaque de métal rivetée : joint central et rivets dans les coins."""
    img = Image(16, 16)
    noise_fill(img, color, 0.05, seed)
    img.frame(0, 0, 16, 16, shade(color, 0.7), 1)
    img.rect(0, 8, 16, 9, shade(color, 0.72))
    for rx, ry in ((2, 2), (13, 2), (2, 5), (13, 5), (2, 11), (13, 11), (2, 13), (13, 13)):
        img.set(rx, ry, shade(color, 1.35))
        img.set(rx + 1, ry + 1, shade(color, 0.6))
    return img


def framed_glass(frame, tint):
    """Dalle de verre cerclée de métal (rendu translucide)."""
    img = Image(16, 16)
    for y in range(16):
        for x in range(16):
            img.set(x, y, (*tint, 70 if (x + y) % 9 else 110))
    img.frame(0, 0, 16, 16, shade(frame, 1.0), 2)
    img.frame(0, 0, 16, 16, shade(frame, 0.75), 1)
    for x in range(2, 14):
        img.set(x, 7, (*shade(frame, 0.9), 255))
    return img


def amber_coral():
    """Matière organique ambrée : veines sombres et reflets clairs, comme une résine."""
    img = Image(16, 16)
    rnd = random.Random(71)
    for y in range(16):
        for x in range(16):
            v = math.sin(y * 0.55 + math.sin(x * 0.8) * 1.6) + 0.35 * math.sin(x * 1.7 + y * 0.3)
            base = (222, 160, 66) if v > -0.2 else (176, 108, 40)
            if v > 1.05:
                base = (246, 206, 120)
            img.set(x, y, shade(base, 1 + rnd.uniform(-0.07, 0.07)))
    for _ in range(3):  # fissures
        cx, cy = rnd.randrange(16), rnd.randrange(16)
        for _s in range(rnd.randint(3, 6)):
            img.set(cx % 16, cy % 16, (110, 62, 24))
            cx += rnd.choice((-1, 0, 1))
            cy += 1
    return img


def handrail():
    """Rambarde : main courante et lisse intermédiaire, montants ; le reste est transparent."""
    img = Image(16, 16)
    brass = (196, 156, 72)
    for x in range(16):
        for y in (1, 2):
            img.set(x, y, shade(brass, 1.2 if y == 1 else 0.9))
        img.set(x, 9, shade(brass, 1.0))
    for y in range(1, 16):
        img.set(7, y, shade(brass, 1.1))
        img.set(8, y, shade(brass, 0.85))
    return img


LIGHT_COLORS = {"orange": (255, 140, 40), "cyan": (60, 230, 255), "blue": (100, 140, 255)}
FRAMES = 8


def light_column_glow(color):
    """Texture animée (8 images) : des vagues de lumière qui montent le long des tubes."""
    img = Image(16, 16 * FRAMES)
    for f in range(FRAMES):
        for y in range(16):
            k = 0.62 + 0.38 * math.sin(((15 - y) / 16 + f / FRAMES) * 2 * math.pi)
            row = tuple(min(255, int(c * k + 255 * max(0.0, k - 0.85) * 2)) for c in color)
            for x in range(16):
                edge = 0.85 if x in (0, 15) else 1.0
                img.set(x, f * 16 + y, shade(row, edge))
    return img


def light_column_model():
    """Gabarit : huit tubes lumineux autour d'un noyau sombre, bague en bas (les segments s'empilent)."""
    elements = []
    for k in range(8):
        a = k * math.pi / 4
        cx, cz = 8 + 5 * math.cos(a), 8 + 5 * math.sin(a)
        elements.append({"from": [round(cx - 1, 2), 0, round(cz - 1, 2)], "to": [round(cx + 1, 2), 16, round(cz + 1, 2)],
                         "shade": False, "faces": {f: {"texture": "#glow"} for f in ("north", "south", "east", "west")}})
    elements.append(box([6, 0, 6], [10, 16, 10], "#metal"))
    elements.append(box([2, 0, 2], [14, 1, 14], "#metal"))
    elements.append(box([2, 15, 2], [14, 16, 14], "#metal"))
    return {"parent": "minecraft:block/block", "ambientocclusion": False,
            "textures": {"glow": "doctorwho:block/light_column_orange", "metal": "doctorwho:block/dark_wall_panel",
                         "particle": "doctorwho:block/dark_wall_panel"}, "elements": elements}


def truss_side():
    """Poutre d'acier bleu ajourée de trous ronds (transparents), comme les arches de la salle de 1996."""
    img = Image(16, 16)
    blue = (64, 86, 168)
    noise_fill(img, blue, 0.05, 91)
    for cy in (4, 11):
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - 7.5, y - cy + 0.5)
                if d < 3.1:
                    img.px[(y * 16 + x) * 4 + 3] = 0
                elif d < 4.0:
                    img.set(x, y, shade(blue, 0.7))
    img.frame(0, 0, 16, 16, shade(blue, 1.25), 1)
    for ry in (0, 15):
        for rx in (2, 5, 10, 13):
            img.set(rx, ry, (196, 156, 72))
    return img


def flame():
    img = Image(16, 16)
    for y in range(16):
        for x in range(16):
            k = 1 - y / 16
            img.set(x, y, (255, int(200 + 50 * k), int(80 + 140 * k)))
    return img


def hex_front(lit):
    """Face d'un panneau hexagonal : cyan lumineux ou blanc nacré."""
    img = Image(16, 16)
    base = (90, 240, 225) if lit else (226, 238, 236)
    for y in range(16):
        for x in range(16):
            d = max(abs(x - 7.5) * 0.866 + abs(y - 7.5) * 0.5, abs(y - 7.5)) / 7.5
            img.set(x, y, shade(base, 1.08 - 0.25 * d if lit else 1.02 - 0.1 * d))
    return img


def tech_panel():
    img = Image(16, 16)
    noise_fill(img, (228, 236, 234), 0.02, 93)
    img.frame(0, 0, 16, 16, (196, 208, 206), 1)
    img.rect(1, 7, 15, 8, (204, 216, 214))
    for x in (3, 12):
        img.set(x, 3, (60, 220, 210))
        img.set(x, 12, (60, 220, 210))
    return img


CLOCK_FRAMES = 12


def clock_face():
    """Cadran animé (12 images) : la grande aiguille fait le tour, la petite avance d'un cran par tour."""
    img = Image(16, 16 * CLOCK_FRAMES)
    c = 7.5
    for f in range(CLOCK_FRAMES):
        oy = f * 16
        for y in range(16):
            for x in range(16):
                d = math.hypot(x - c, y - c)
                if d <= 7.6:
                    img.set(x, oy + y, (236, 228, 204) if d < 6.6 else (150, 112, 60))
        for h in range(12):  # graduations
            a = h * math.pi / 6
            img.set(int(round(c + math.sin(a) * 5.6)), oy + int(round(c - math.cos(a) * 5.6)), (60, 50, 40))
        for length, angle, color in ((5.0, f * math.pi / 6, (30, 30, 30)), (3.2, (f / 12) * math.pi / 6 + math.pi * 2 / 3, (120, 30, 20))):
            for k in range(int(length * 3) + 1):
                r = k / 3
                img.set(int(round(c + math.sin(angle) * r)), oy + int(round(c - math.cos(angle) * r)), color)
        img.set(7, oy + 7, (200, 160, 60))
    return img


def globe_texture():
    img = Image(16, 16)
    rnd = random.Random(107)
    for y in range(16):
        for x in range(16):
            land = math.sin(x * 0.9 + 1.3) + math.sin(y * 0.7 + x * 0.3) > 0.6
            img.set(x, y, shade((92, 140, 70) if land else (54, 96, 168), 1 + rnd.uniform(-0.06, 0.06)))
    return img


def cushion(color):
    img = Image(16, 16)
    noise_fill(img, color, 0.06, 101)
    img.frame(0, 0, 16, 16, shade(color, 0.72), 1)
    for x in (4, 11):
        for y in (4, 11):
            img.set(x, y, shade(color, 0.6))
    return img


def interior_blocks():
    for family, spec in ROUNDEL_FAMILIES.items():
        panel, hollow, glow = spec[:3]
        rim = spec[3] if len(spec) > 3 else None
        tex(family + "_roundel_front", roundel_front(panel, rim))
        tex(family + "_roundel_side", solid_tex(panel, 0.02, 41))
        tex(family + "_roundel_disc", roundel_disc(panel, hollow, glow, False))
        tex(family + "_roundel_disc_lit", roundel_disc(panel, hollow, glow, True))
        for lit in (False, True):
            name = family + "_roundel" + ("_lit" if lit else "")
            simple_block(name, roundel_model("doctorwho:block/%s_roundel_front" % family,
                                             "doctorwho:block/%s_roundel_disc%s" % (family, "_lit" if lit else ""),
                                             "doctorwho:block/%s_roundel_side" % family), facing=True)

    for family, color in GRATE_FAMILIES.items():
        tex(family + "_floor_grate", floor_grate(color))
        simple_block(family + "_floor_grate", model_cube_all(family + "_floor_grate"))
    for color, (edge, core) in UNDERLIGHTS.items():
        tex(color + "_underlight", underlight(edge, core))
        simple_block(color + "_underlight", model_cube_all(color + "_underlight"))

    # --- Cuivre (2010) : plaque rivetée, entretoise (même modèle que la branche de corail), verre au sol
    tex("copper_wall_panel", riveted_panel((176, 104, 54), 61))
    simple_block("copper_wall_panel", model_cube_all("copper_wall_panel"))
    simple_block("copper_strut", {"parent": "doctorwho:block/coral_strut", "textures": {
        "coral": "doctorwho:block/copper_beam", "particle": "doctorwho:block/copper_beam"}}, facing=True)
    tex("copper_glass_floor", framed_glass((184, 112, 58), (190, 255, 220)))
    simple_block("copper_glass_floor", model_cube_all("copper_glass_floor"))

    # --- Ambre organique : matière ambrée, pilier et branche (mêmes modèles que le corail), alvéole bleue
    tex("amber_coral", amber_coral())
    simple_block("amber_coral", model_cube_all("amber_coral"))
    simple_block("amber_pillar", {"parent": "doctorwho:block/coral_pillar", "textures": {
        "coral": "doctorwho:block/amber_coral", "particle": "doctorwho:block/amber_coral"}})
    simple_block("amber_strut", {"parent": "doctorwho:block/coral_strut", "textures": {
        "coral": "doctorwho:block/amber_coral", "particle": "doctorwho:block/amber_coral"}}, facing=True)
    tex("blue_hex_light", underlight((40, 110, 210), (210, 240, 255)))
    simple_block("blue_hex_light", model_cube_all("blue_hex_light"))

    # --- Sombre industriel (2014) : plaque d'acier, rambarde de laiton (raccordée comme des barreaux)
    tex("dark_wall_panel", riveted_panel((60, 62, 68), 81))
    simple_block("dark_wall_panel", model_cube_all("dark_wall_panel"))
    tex("brass_handrail", handrail())
    tex("brass_handrail_edge", solid_tex((196, 156, 72), 0.05, 83))
    bars = "doctorwho:block/brass_handrail"
    parts = ("post_ends", "post", "cap", "cap_alt", "side", "side_alt")
    extra = {"brass_handrail_" + part: {"parent": "minecraft:block/iron_bars_" + part, "textures": {
        "bars": bars, "edge": bars + "_edge", "particle": bars + "_edge"}} for part in parts}
    with open(os.path.join(os.path.dirname(__file__), "iron_bars_blockstate.json"), encoding="utf-8") as f:
        state = json.load(f)
    text = json.dumps(state).replace("minecraft:block/iron_bars_", "doctorwho:block/brass_handrail_")
    simple_block("brass_handrail", extra["brass_handrail_post_ends"], blockstate=json.loads(text), extra_models=extra,
                 item_model={"parent": "minecraft:item/generated", "textures": {"layer0": bars}})

    # Colonne de lumière : un modèle, une texture animée par couleur (propriété « color »)
    for color, rgb in LIGHT_COLORS.items():
        tex("light_column_" + color, light_column_glow(rgb))
        save_json(os.path.join(ASSETS, "textures", "block", "light_column_%s.png.mcmeta" % color),
                  {"animation": {"frametime": 3, "interpolate": True}})
    column = light_column_model()
    variants = {}
    extra = {}
    for color in LIGHT_COLORS:
        extra["light_column_" + color] = {"parent": "doctorwho:block/light_column", "textures": {
            "glow": "doctorwho:block/light_column_" + color}}
        variants["color=" + color] = {"model": "doctorwho:block/light_column_" + color}
    simple_block("light_column", column, blockstate={"variants": variants}, extra_models=extra)

    # --- Bleu cathédrale (1996) : poutre ajourée (orientable comme une bûche), chandelier
    tex("cathedral_truss", truss_side())
    tex("cathedral_truss_end", riveted_panel((64, 86, 168), 95))
    simple_block("cathedral_truss", {"parent": "minecraft:block/cube_column", "textures": {
        "end": "doctorwho:block/cathedral_truss_end", "side": "doctorwho:block/cathedral_truss"}}, axis=True)
    tex("candle_flame", flame())
    brass_t, wax, fl = "#brass", "#wax", "#flame"
    candles = []
    for cx in (3, 7, 11):
        candles.append(box([cx, 10, 7], [cx + 2, 14, 9], wax))
        candles.append({"from": [cx + 0.5, 14, 7.5], "to": [cx + 1.5, 16, 8.5], "shade": False,
                        "faces": {f: {"texture": fl} for f in ("north", "south", "east", "west", "up")}})
    simple_block("candelabra", model_elements(
        {"brass": "doctorwho:block/brass", "wax": "minecraft:block/white_candle", "flame": "doctorwho:block/candle_flame",
         "particle": "doctorwho:block/brass"},
        [box([5, 0, 5], [11, 1, 11], brass_t), box([7.5, 1, 7.5], [8.5, 9, 8.5], brass_t),
         box([3, 9, 7.25], [13, 10, 8.75], brass_t)] + candles))

    # --- Turquoise tech (2012) : panneau hexagonal en relief (éteint / allumé), panneau blanc
    tex("hex_wall_panel_side", solid_tex((214, 226, 224), 0.02, 97))
    for lit in (False, True):
        name = "hex_wall_panel" + ("_lit" if lit else "")
        tex(name + "_front", hex_front(lit))
        t = {"side": "doctorwho:block/hex_wall_panel_side", "hex": "doctorwho:block/%s_front" % name,
             "particle": "doctorwho:block/hex_wall_panel_side"}
        back = {"from": [0, 0, 1], "to": [16, 16, 16], "faces": {
            "north": {"texture": "#side"}, "south": {"texture": "#side", "cullface": "south"},
            "east": {"texture": "#side", "cullface": "east"}, "west": {"texture": "#side", "cullface": "west"},
            "up": {"texture": "#side", "cullface": "up"}, "down": {"texture": "#side", "cullface": "down"}}}
        plates = [{"from": frm, "to": to, "shade": not lit, "faces": {
            "north": {"texture": "#hex"}, "east": {"texture": "#side"}, "west": {"texture": "#side"},
            "up": {"texture": "#side"}, "down": {"texture": "#side"}}}
            for frm, to in (([4, 1, 0], [12, 15, 1]), ([2, 4, 0], [4, 12, 1]), ([12, 4, 0], [14, 12, 1]))]
        simple_block(name, {"parent": "minecraft:block/block", "textures": t, "elements": [back] + plates}, facing=True)
    tex("white_tech_panel", tech_panel())
    simple_block("white_tech_panel", model_cube_all("white_tech_panel"))

    # --- Mobilier facultatif ------------------------------------------------------------------
    # Sièges : même modèle, coussin de couleur différente (on s'y assoit : voir SeatBlock)
    for name, color, frame in (("leather_seat", (120, 62, 34), "minecraft:block/dark_oak_planks"),
                               ("white_seat", (236, 236, 230), "minecraft:block/quartz_block_bottom"),
                               ("wooden_seat", (150, 104, 60), "minecraft:block/spruce_planks")):
        tex(name, cushion(color))
        simple_block(name, model_elements(
            {"cushion": "doctorwho:block/" + name, "frame": frame, "particle": "doctorwho:block/" + name},
            [box([1, 0, 1], [3, 6, 3], "#frame"), box([13, 0, 1], [15, 6, 3], "#frame"),
             box([1, 0, 13], [3, 6, 15], "#frame"), box([13, 0, 13], [15, 6, 15], "#frame"),
             box([0, 6, 0], [16, 9, 16], "#cushion"), box([0, 9, 13], [16, 20, 16], "#cushion"),
             box([0, 9, 1], [2, 13, 13], "#frame"), box([14, 9, 1], [16, 13, 13], "#frame")]), facing=True)

    # Horloge murale (cadran animé)
    tex("wall_clock", clock_face())
    save_json(os.path.join(ASSETS, "textures", "block", "wall_clock.png.mcmeta"), {"animation": {"frametime": 100}})
    simple_block("wall_clock", model_elements(
        {"face": "doctorwho:block/wall_clock", "frame": "doctorwho:block/brass", "particle": "doctorwho:block/brass"},
        [box([2, 2, 15], [14, 14, 16], "#frame"),
         {"from": [2.5, 2.5, 14.9], "to": [13.5, 13.5, 15], "faces": {"north": {"texture": "#face"}}}]), facing=True)

    # Tuyau de cuivre et câble noir (orientables comme une bûche)
    tex("black_cable", solid_tex((30, 30, 34), 0.08, 103))
    for name, texture in (("copper_pipe", "doctorwho:block/copper_beam"), ("black_cable", "doctorwho:block/black_cable")):
        elements = [box([5, 0, 5], [11, 16, 11], "#pipe")]
        if name == "copper_pipe":
            elements += [box([4.5, 0, 4.5], [11.5, 2, 11.5], "#pipe"), box([4.5, 14, 4.5], [11.5, 16, 11.5], "#pipe")]
        else:
            elements = [box([6, 0, 6], [10, 16, 10], "#pipe"), box([3, 0, 4], [6, 16, 7], "#pipe"), box([10, 0, 9], [13, 16, 12], "#pipe")]
        simple_block(name, {"parent": "minecraft:block/block", "textures": {"pipe": texture, "particle": texture},
                            "elements": elements}, axis=True)

    # Télescope de laiton sur trépied et globe terrestre (salle de 1996)
    simple_block("telescope", model_elements(
        {"brass": "doctorwho:block/brass", "wood": "minecraft:block/dark_oak_planks", "particle": "doctorwho:block/brass"},
        [box([7, 0, 3], [8, 10, 4], "#wood", {"origin": [8, 10, 8], "axis": "x", "angle": 22.5}),
         box([8, 0, 12], [9, 10, 13], "#wood", {"origin": [8, 10, 8], "axis": "x", "angle": -22.5}),
         box([3, 0, 7.5], [4, 10, 8.5], "#wood", {"origin": [8, 10, 8], "axis": "z", "angle": -22.5}),
         box([7, 10, 7], [9, 12, 9], "#brass"),
         box([6.5, 12, 1], [9.5, 15, 15], "#brass", {"origin": [8, 13, 8], "axis": "x", "angle": -22.5}),
         box([6, 11.5, 0], [10, 15.5, 2], "#brass", {"origin": [8, 13, 8], "axis": "x", "angle": -22.5})]), facing=True)
    tex("globe", globe_texture())
    simple_block("globe", model_elements(
        {"globe": "doctorwho:block/globe", "brass": "doctorwho:block/brass", "wood": "minecraft:block/dark_oak_planks",
         "particle": "doctorwho:block/brass"},
        [box([4, 0, 4], [12, 2, 12], "#wood"), box([7.5, 2, 7.5], [8.5, 5, 8.5], "#brass"),
         box([4, 5, 4], [12, 13, 12], "#globe"), box([7.5, 4, 3], [8.5, 14, 4], "#brass"),
         box([7.5, 13, 4], [8.5, 14, 12], "#brass")]))

    tex("classic_pilaster", pilaster((236, 236, 230)))
    tex("classic_pilaster_top", solid_tex((236, 236, 230), 0.02, 43))
    simple_block("classic_pilaster", {"parent": "minecraft:block/cube_column", "textures": {
        "end": "doctorwho:block/classic_pilaster_top", "side": "doctorwho:block/classic_pilaster"}})

    tex("light_panel", light_panel())
    simple_block("light_panel", model_cube_all("light_panel"))

    # Écran du scanner, accroché au mur (dos au sud quand il regarde au nord)
    tex("scanner_screen", scanner_screen())
    tex("scanner_frame", solid_tex((70, 72, 78), 0.05, 47))
    simple_block("scanner", model_elements(
        {"frame": "doctorwho:block/scanner_frame", "screen": "doctorwho:block/scanner_screen",
         "particle": "doctorwho:block/scanner_frame"},
        [box([0, 1, 13], [16, 15, 16], "#frame"),
         {"from": [1, 2, 12.9], "to": [15, 14, 13], "faces": {"north": {"texture": "#screen"}}}]),
        facing=True)

    # Porte-manteau (≈ 2 blocs de haut), bois sombre et laiton
    wood, brass = "#wood", "#brass"
    simple_block("hat_stand", model_elements(
        {"wood": "minecraft:block/dark_oak_planks", "brass": "doctorwho:block/brass",
         "particle": "minecraft:block/dark_oak_planks"},
        [box([3, 0, 7], [13, 1.5, 9], wood), box([7, 0, 3], [9, 1.5, 13], wood),
         box([7.25, 1.5, 7.25], [8.75, 28, 8.75], wood),
         box([7, 28, 7], [9, 30, 9], brass),
         box([4, 25, 7.5], [12, 26, 8.5], brass), box([7.5, 25, 4], [8.5, 26, 12], brass),
         box([4, 26, 7.5], [5, 27.5, 8.5], brass), box([11, 26, 7.5], [12, 27.5, 8.5], brass),
         box([7.5, 26, 4], [8.5, 27.5, 5], brass), box([7.5, 26, 11], [8.5, 27.5, 12], brass)]))


# ---------------------------------------------------------------------------
# Interface : sceau gallifreyen (blanc + transparence, teinté en jeu)
# ---------------------------------------------------------------------------
def gallifreyan_seal():
    size = 256
    img = Image(size, size)
    c = size / 2 - 0.5

    def plot(x, y, a):
        if 0 <= x < size and 0 <= y < size:
            i = (y * size + x) * 4
            img.px[i:i + 4] = bytes((255, 255, 255, max(img.px[i + 3], int(255 * a))))

    def ring(cx, cy, r, t):
        for y in range(int(cy - r - t - 1), int(cy + r + t + 2)):
            for x in range(int(cx - r - t - 1), int(cx + r + t + 2)):
                d = abs(math.hypot(x - cx, y - cy) - r)
                if d <= t / 2 + 0.7:
                    plot(x, y, min(1.0, t / 2 + 0.7 - d))

    def disc(cx, cy, r):
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                d = math.hypot(x - cx, y - cy)
                if d <= r + 0.7:
                    plot(x, y, min(1.0, r + 0.7 - d))

    def line(a, r0, r1, t=1.2):
        steps = int((r1 - r0) * 2)
        for s in range(steps + 1):
            r = r0 + (r1 - r0) * s / steps
            disc(c + math.cos(a) * r, c + math.sin(a) * r, t / 2)

    ring(c, c, 124, 3)
    ring(c, c, 114, 1.4)
    ring(c, c, 58, 2.2)
    ring(c, c, 40, 1.2)
    rnd = random.Random(1963)
    for k in range(6):
        a = k * math.pi / 3 + 0.3
        sx, sy = c + math.cos(a) * 86, c + math.sin(a) * 86
        ring(sx, sy, 15, 1.6)
        if k % 2 == 0:
            ring(sx, sy, 7, 1.2)
        for _ in range(rnd.randint(1, 3)):
            b = rnd.uniform(0, math.tau)
            disc(sx + math.cos(b) * 10, sy + math.sin(b) * 10, 2.2)
        line(a + math.pi / 6, 58, 114)
    for _ in range(14):
        b = rnd.uniform(0, math.tau)
        disc(c + math.cos(b) * 119, c + math.sin(b) * 119, 2.6)
    for _ in range(5):
        b = rnd.uniform(0, math.tau)
        disc(c + math.cos(b) * 49, c + math.sin(b) * 49, 3)
    img.save(os.path.join(ASSETS, "textures", "gui", "gallifreyan_seal.png"))
    print("OK sceau gallifreyen")


if __name__ == "__main__":
    build_blocks()
    gallifreyan_seal()
