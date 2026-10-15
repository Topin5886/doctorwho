"""
Générateur des modèles GeckoLib (.geo.json) et de leurs textures (.png) pour les entités du mod Doctor Who.

Pourquoi un script ? Pour avoir des modèles de départ propres sans Blockbench.
Ils pourront être remplacés plus tard par des modèles faits à la main dans Blockbench
(mêmes noms de fichiers, mêmes noms d'os).

Utilisation (depuis la racine du projet) :
    python tools/generate_entities.py

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
# Entités Doctor Who
# ---------------------------------------------------------------------------

# Couleurs communes
BRONZE = (184, 115, 51)
SILVER = (178, 182, 190)
BLACK = (20, 20, 20)
DARK_GRAY = (40, 40, 40)
LIGHT_GRAY = (120, 120, 120)
WHITE = (255, 255, 255)
RED = (200, 30, 30)
BLUE = (30, 30, 200)
YELLOW = (255, 255, 0)
GREEN = (30, 200, 30)


def p_metal(tex, glow, x, y, w, h, s, face, seed):
    """Peinture métallique de base"""
    noisy(tex, x, y, x + w, y + h, SILVER, 0.04, seed)


def p_bronze(tex, glow, x, y, w, h, s, face, seed):
    """Peinture bronze pour les Daleks"""
    noisy(tex, x, y, x + w, y + h, BRONZE, 0.04, seed)


def p_black(tex, glow, x, y, w, h, s, face, seed):
    """Peinture noire"""
    noisy(tex, x, y, x + w, y + h, BLACK, 0.04, seed)


def p_dark_gray(tex, glow, x, y, w, h, s, face, seed):
    """Peinture gris foncé"""
    noisy(tex, x, y, x + w, y + h, DARK_GRAY, 0.04, seed)


def p_light_gray(tex, glow, x, y, w, h, s, face, seed):
    """Peinture gris clair"""
    noisy(tex, x, y, x + w, y + h, LIGHT_GRAY, 0.04, seed)


def p_white(tex, glow, x, y, w, h, s, face, seed):
    """Peinture blanche"""
    noisy(tex, x, y, x + w, y + h, WHITE, 0.04, seed)


def p_red(tex, glow, x, y, w, h, s, face, seed):
    """Peinture rouge"""
    noisy(tex, x, y, x + w, y + h, RED, 0.04, seed)


def p_blue(tex, glow, x, y, w, h, s, face, seed):
    """Peinture bleue"""
    noisy(tex, x, y, x + w, y + h, BLUE, 0.04, seed)


def p_yellow(tex, glow, x, y, w, h, s, face, seed):
    """Peinture jaune"""
    noisy(tex, x, y, x + w, y + h, YELLOW, 0.04, seed)


def p_green(tex, glow, x, y, w, h, s, face, seed):
    """Peinture verte"""
    noisy(tex, x, y, x + w, y + h, GREEN, 0.04, seed)


def p_eye_glow(tex, glow, x, y, w, h, s, face, seed):
    """Brillance de l'œil (pour le masque de luminescence)"""
    if face == "north":  # Seulement sur la face avant
        # Cercle central pour l'œil
        cx, cy = w // 2, h // 2
        radius = min(w, h) // 3
        for yy in range(y, y + h):
            for xx in range(x, x + w):
                dx, dy = xx - (x + cx), yy - (y + cy)
                if dx * dx + dy * dy <= radius * radius:
                    intensity = 1.0 - (dx * dx + dy * dy) / (radius * radius)
                    glow.set(xx, yy, shade((255, 255, 100), intensity))


def dalek_base():
    """Corps de base d'un Dalek"""
    # Dimensions approximatives d'un Dalek : largeur ~1.2 blocs, hauteur ~1.8 blocs
    # En pixels (échelle 16): largeur ~19, hauteur ~29
    body = [
        # Poitrine supérieure
        ((-6, 0, -6), (12, 10, 12), {"*": "bronze"}),
        # Poitrine inférieure avec renforts
        ((-7, 10, -7), (14, 8, 14), {"*": "bronze"}),
        # Base
        ((-8, 18, -8), (16, 2, 16), {"*": "bronze"}),
    ]
    
    # Tête/dôme
    head = [
        ((-4, -6, -4), (8, 6, 8), {"*": "bronze", "north": "eye_glow"}),
    ]
    
    # Bras manipulateurs (deux bras latéraux)
    left_arm = [
        ((-12, 4, -2), (4, 6, 4), {"*": "bronze"}),
        ((-12, 10, -2), (2, 2, 2), {"*": "bronze"}),  # Pince gauche
    ]
    
    right_arm = [
        ((8, 4, -2), (4, 6, 4), {"*": "bronze"}),
        ((10, 10, -2), (2, 2, 2), {"*": "bronze"}),  # Pince droite
    ]
    
    # Œil central (saillant)
    eye = [
        ((-1, -8, -4), (2, 2, 4), {"*": "silver", "north": "eye_glow"}),
    ]
    
    # Mid-section détails
    mid_details = [
        # Côtes
        ((-8, 4, -7), (2, 10, 2), {"*": "dark_gray"}),
        ((6, 4, -7), (2, 10, 2), {"*": "dark_gray"}),
        # Barres latérales
        ((-8, 4, 5), (16, 2, 2), {"*": "dark_gray"}),
        ((-8, 14, 5), (16, 2, 2), {"*": "dark_gray"}),
    ]
    
    bones = [
        {"name": "root", "pivot": [0, 0, 0], "cubes": body + mid_details},
        {"name": "head", "parent": "root", "pivot": [0, -2, 0], "cubes": head},
        {"name": "left_arm", "parent": "root", "pivot": [0, 0, 0], "cubes": left_arm},
        {"name": "right_arm", "parent": "root", "pivot": [0, 0, 0], "cubes": right_arm},
        {"name": "eye", "parent": "head", "pivot": [0, 0, 0], "cubes": eye},
    ]
    
    painters = {
        "bronze": p_bronze,
        "silver": p_metal,
        "dark_gray": p_dark_gray,
        "eye_glow": p_eye_glow,
    }
    
    return "dalec_base", bones, painters


def dalek_variants():
    """Générer toutes les variantes de Daleks"""
    variants = [
        ("dalek_bronze", "Bronze Dalek (2005)", BRONZE),
        ("dalek_silver", "Silver Dalek", SILVER),
        ("dalek_red", "Red Dalek", RED),
        ("dalek_blue", "Blue Dalek", BLUE),
        ("dalek_yellow", "Yellow Dalek", YELLOW),
        ("dalek_green", "Green Dalek", GREEN),
    ]
    
    results = []
    for variant_name, display_name, base_color in variants:
        # Fonction de peinture personnalisée pour cette variante
        def make_painter(color):
            def painter(tex, glow, x, y, w, h, s, face, seed):
                noisy(tex, x, y, x + w, y + h, color, 0.04, seed)
                # Détails spécifiques selon la variante
                if face == "up":  # Dessus du Dalek
                    # Rainures
                    for i in range(3):
                        tx = x + 4 + i * 4
                        tex.rect(tx, y, tx + 2, y + h, shade(color, 0.8))
            return painter
        
        # Créer les os de base
        name, bones, base_painters = dalek_base()
        
        # Mettre à jour les peintres avec la couleur spécifique
        painters = base_painters.copy()
        painters["bronze"] = make_painter(base_color)
        
        results.append((variant_name, bones, painters, display_name))
    
    return results


def dalek():
    """Générer le Dalek de base (bronze)"""
    name, bones, painters = dalek_base()
    geo, tex, glow = build(name, bones, 16, 256, painters)
    write("entity", name, geo, tex, glow)
    
    # Générer aussi les variantes
    variants = dalek_variants()
    for variant_name, bones, painters, display_name in variants:
        geo, tex, glow = build(variant_name, bones, 16, 256, painters)
        write("entity", variant_name, geo, tex, glow)


# ---------------------------------------------------------------------------
# Cyberman
# ---------------------------------------------------------------------------
def cyberman_base():
    """Corps de base d'un Cyberman"""
    # Corps humanoïde blindé
    body = [
        # Tête (casque)
        ((-4, 10, -4), (8, 6, 8), {"*": "silver", "north": "eye_glow"}),
        # Torse
        ((-4, 0, -2), (8, 10, 4), {"*": "silver"}),
        # Bras gauche
        ((-8, 4, -2), (4, 8, 4), {"*": "silver"}),
        ((-8, 12, -2), (2, 2, 2), {"*": "silver"}),  # Main
        # Bras droit
        ((4, 4, -2), (4, 8, 4), {"*": "silver"}),
        ((6, 12, -2), (2, 2, 2), {"*": "silver"}),  # Main
        # Jambe gauche
        ((-3, -8, -2), (4, 8, 4), {"*": "silver"}),
        ((-3, -16, -2), (2, 2, 2), {"*": "silver"}),  # Pied
        # Jambe droite
        ((-1, -8, -2), (4, 8, 4), {"*": "silver"}),
        ((1, -16, -2), (2, 2, 2), {"*": "silver"}),  # Pied
    ]
    
    # Détails
    details = [
        # Poitrine circulaire
        ((-2, 4, -1), (4, 4, 2), {"*": "blue"}),
        # Ceinture
        ((-4, -4, -3), (8, 2, 6), {"*": "dark_gray"}),
        # Genouillères
        ((-3, -8, 2), (2, 4, 2), {"*": "dark_gray"}),
        ((3, -8, 2), (2, 4, 2), {"*": "dark_gray"}),
    ]
    
    bones = [
        {"name": "root", "pivot": [0, 0, 0], "cubes": body + details},
    ]
    
    painters = {
        "silver": p_metal,
        "blue": lambda tex, glow, x, y, w, h, s, face, seed: noisy(tex, x, y, x + w, y + h, BLUE, 0.04, seed),
        "dark_gray": p_dark_gray,
        "eye_glow": p_eye_glow,
    }
    
    return "cyberman", bones, painters


def cyberman():
    """Générer le Cyberman"""
    name, bones, painters = cyberman_base()
    geo, tex, glow = build(name, bones, 16, 256, painters)
    write("entity", name, geo, tex, glow)


# ---------------------------------------------------------------------------
# Ange Pleureur
# ---------------------------------------------------------------------------
def weeping_angel_base():
    """Corps de base d'un Ange Pleureur"""
    # Statue d'ange avec ailes
    body = [
        # Pieds
        ((-2, 0, -1), (4, 4, 2), {"*": "white"}),
        ((2, 0, -1), (4, 4, 2), {"*": "white"}),
        # Jambes
        ((-2, 4, -2), (4, 6, 4), {"*": "white"}),
        ((2, 4, -2), (4, 6, 4), {"*": "white"}),
        # Taille
        ((-3, 10, -2), (6, 4, 4), {"*": "white"}),
        # Torse
        ((-4, 14, -3), (8, 6, 6), {"*": "white"}),
        # Cou
        ((-1, 20, -1), (2, 2, 2), {"*": "white"}),
        # Tête
        ((-3, 22, -3), (6, 4, 6), {"*": "white", "north": "face_details"}),
    ]
    
    # Ailes
    left_wing = [
        ((-8, 10, -4), (6, 8, 4), {"*": "white"}),
        ((-12, 18, -6), (4, 6, 6), {"*": "white"}),
        ((-14, 24, -8), (2, 4, 4), {"*": "white"}),  # Pointe
    ]
    
    right_wing = [
        ((2, 10, -4), (6, 8, 4), {"*": "white"}),
        ((8, 18, -6), (4, 6, 6), {"*": "white"}),
        ((12, 24, -8), (2, 4, 4), {"*": "white"}),  # Pointe
    ]
    
    # Bras
    left_arm = [
        ((-6, 14, -2), (4, 4, 4), {"*": "white"}),
        ((-8, 18, -4), (2, 2, 6), {"*": "white"}),  # Main
    ]
    
    right_arm = [
        ((2, 14, -2), (4, 4, 4), {"*": "white"}),
        ((6, 18, -4), (2, 2, 6), {"*": "white"}),  # Main
    ]
    
    # Détails du visage
    face_details = [
        # Yeux
        ((-2, 24, -4), (1, 1, 2), {"*": "black"}),
        ((2, 24, -4), (1, 1, 2), {"*": "black"}),
        # Bouche
        ((0, 22, -3), (2, 2, 1), {"*": "black"}),
    ]
    
    bones = [
        {"name": "root", "pivot": [0, 0, 0], "cubes": body},
        {"name": "left_wing", "parent": "root", "pivot": [0, 0, 0], "cubes": left_wing},
        {"name": "right_wing", "parent": "root", "pivot": [0, 0, 0], "cubes": right_wing},
        {"name": "left_arm", "parent": "root", "pivot": [0, 14, 0], "cubes": left_arm},
        {"name": "right_arm", "parent": "root", "pivot": [0, 14, 0], "cubes": right_arm},
        {"name": "head", "parent": "torso", "pivot": [0, 6, 0], "cubes": face_details},
    ]
    
    # Réorganiser pour une hiérarchie plus logique
    bones = [
        {"name": "root", "pivot": [0, 0, 0], "cubes": body},
        {"name": "torso", "parent": "root", "pivot": [0, 6, 0], "cubes": [
            ((-4, 8, -3), (8, 6, 6), {"*": "white"}),
            ((-3, 14, -2), (6, 4, 4), {"*": "white"}),
            ((-1, 20, -1), (2, 2, 2), {"*": "white"}),
        ]},
        {"name": "head", "parent": "torso", "pivot": [0, 4, 0], "cubes": face_details},
        {"name": "left_wing", "parent": "torso", "pivot": [-4, 12, 0], "cubes": left_wing},
        {"name": "right_wing", "parent": "torso", "pivot": [4, 12, 0], "cubes": right_wing},
        {"name": "left_arm", "parent": "torso", "pivot": [-4, 14, -2], "cubes": left_arm},
        {"name": "right_arm", "parent": "torso", "pivot": [4, 14, -2], "cubes": right_arm},
        {"name": "left_leg", "parent": "root", "pivot": [-2, 0, -1], "cubes": [
            ((-2, 0, -1), (4, 4, 2), {"*": "white"}),
            ((-2, 4, -2), (4, 6, 4), {"*": "white"}),
        ]},
        {"name": "right_leg", "parent": "root", "pivot": [2, 0, -1], "cubes": [
            ((2, 0, -1), (4, 4, 2), {"*": "white"}),
            ((2, 4, -2), (4, 6, 4), {"*": "white"}),
        ]},
    ]
    
    painters = {
        "white": lambda tex, glow, x, y, w, h, s, face, seed: noisy(tex, x, y, x + w, y + h, WHITE, 0.02, seed),
        "black": p_black,
        "face_details": lambda tex, glow, x, y, w, h, s, face, seed: (
            noisy(tex, x, y, x + w, y + h, WHITE, 0.02, seed) if face != "north" else None
        ),
    }
    
    return "weeping_angel", bones, painters


def weeping_angel():
    """Générer l'Ange Pleureur"""
    name, bones, painters = weeping_angel_base()
    geo, tex, glow = build(name, bones, 16, 256, painters)
    write("entity", name, geo, tex, glow)


# ---------------------------------------------------------------------------
# Ood
# ---------------------------------------------------------------------------
def ood_base():
    """Corps de base d'un Ood"""
    # Humanoïde avec tentacules faciaux
    body = [
        # Pieds
        ((-1, 0, -1), (2, 4, 2), {"*": "gray"}),
        ((1, 0, -1), (2, 4, 2), {"*": "gray"}),
        # Jambes
        ((-1, 4, -2), (2, 8, 4), {"*": "gray"}),
        ((1, 4, -2), (2, 8, 4), {"*": "gray"}),
        # Taille
        ((-2, 12, -2), (4, 4, 4), {"*": "gray"}),
        # Torse
        ((-3, 16, -3), (6, 6, 6), {"*": "gray"}),
        # Cou
        ((-1, 22, -1), (2, 2, 2), {"*": "gray"}),
        # Tête
        ((-3, 24, -3), (6, 4, 6), {"*": "gray", "north": "tendril_base"}),
    ]
    
    # Tentacules faciaux (4 tentacules)
    tendril1 = [  # Supérieur gauche
        ((-4, 24, -2), (2, 4, 2), {"*": "gray"}),
        ((-6, 28, -2), (2, 2, 2), {"*": "gray"}),
    ]
    
    tendril2 = [  # Supérieur droit
        ((4, 24, -2), (2, 4, 2), {"*": "gray"}),
        ((6, 28, -2), (2, 2, 2), {"*": "gray"}),
    ]
    
    tendril3 = [  # Inférieur gauche
        ((-2, 24, -4), (2, 4, 2), {"*": "gray"}),
        ((-2, 28, -6), (2, 2, 2), {"*": "gray"}),
    ]
    
    tendril4 = [  # Inférieur droit
        ((2, 24, -4), (2, 4, 2), {"*": "gray"}),
        ((2, 28, -6), (2, 2, 2), {"*": "gray"}),
    ]
    
    # Dispositif de traduction (boule dans la main)
    translator = [
        ((4, 18, -2), (2, 2, 2), {"*": "yellow_glow"}),
    ]
    
    bones = [
        {"name": "root", "pivot": [0, 0, 0], "cubes": body},
        {"name": "tendril1", "parent": "head", "pivot": [0, 0, 0], "cubes": tendril1},
        {"name": "tendril2", "parent": "head", "pivot": [0, 0, 0], "cubes": tendril2},
        {"name": "tendril3", "parent": "head", "pivot": [0, 0, 0], "cubes": tendril3},
        {"name": "tendril4", "parent": "head", "pivot": [0, 0, 0], "cubes": tendril4},
        {"name": "translator", "parent": "right_hand", "pivot": [0, 0, 0], "cubes": translator},
    ]
    
    # Réorganiser avec mains
    bones = [
        {"name": "root", "pivot": [0, 0, 0], "cubes": [
            # Pieds
            ((-1, 0, -1), (2, 4, 2), {"*": "gray"}),
            ((1, 0, -1), (2, 4, 2), {"*": "gray"}),
            # Jambes
            ((-1, 4, -2), (2, 8, 4), {"*": "gray"}),
            ((1, 4, -2), (2, 8, 4), {"*": "gray"}),
            # Taille
            ((-2, 12, -2), (4, 4, 4), {"*": "gray"}),
            # Torse
            ((-3, 16, -3), (6, 6, 6), {"*": "gray"}),
        ]},
        {"name": "torso", "parent": "root", "pivot": [0, 10, 0], "cubes": [
            ((-1, 0, -1), (2, 2, 2), {"*": "gray"}),
            ((-1, 4, -1), (2, 4, 2), {"*": "gray"}),
            ((-2, 8, -2), (4, 4, 4), {"*": "gray"}),
        ]},
        {"name": "head", "parent": "torso", "pivot": [0, 4, 0], "cubes": [
            ((-3, 0, -3), (6, 4, 6), {"*": "gray", "north": "tendril_base"}),
        ]},
        {"name": "left_shoulder", "parent": "torso", "pivot": [-3, 4, -2], "cubes": []},
        {"name": "right_shoulder", "parent": "torso", "pivot": [3, 4, -2], "cubes": []},
        {"name": "left_arm", "parent": "left_shoulder", "pivot": [0, 0, 0], "cubes": [
            ((0, 0, -2), (2, 6, 4), {"*": "gray"}),
        ]},
        {"name": "right_arm", "parent": "right_shoulder", "pivot": [0, 0, 0], "cubes": [
            ((0, 0, -2), (2, 6, 4), {"*": "gray"}),
            ((2, 6, 0), (2, 2, 2), {"*": "yellow_glow"}),
        ]},
        {"name": "left_hand", "parent": "left_arm", "pivot": [0, 6, 0], "cubes": [
            ((-1, 0, -1), (2, 2, 2), {"*": "gray"}),
        ]},
        {"name": "right_hand", "parent": "right_arm", "pivot": [0, 6, 0], "cubes": [
            ((-1, 0, -1), (2, 2, 2), {"*": "gray"}),
        ]},
        {"name": "tendril1", "parent": "head", "pivot": [-3, 2, -2], "cubes": tendril1},
        {"name": "tendril2", "parent": "head", "pivot": [3, 2, -2], "cubes": tendril2},
        {"name": "tendril3", "parent": "head", "pivot": [-2, 2, -4], "cubes": tendril3},
        {"name": "tendril4", "parent": "head", "pivot": [2, 2, -4], "cubes": tendril4},
        {"name": "translator", "parent": "right_hand", "pivot": [0, 0, 0], "cubes": translator},
    ]
    
    painters = {
        "gray": lambda tex, glow, x, y, w, h, s, face, seed: noisy(tex, x, y, x + w, y + h, LIGHT_GRAY, 0.03, seed),
        "yellow_glow": lambda tex, glow, x, y, w, h, s, face, seed: (
            noisy(tex, x, y, x + w, y + h, YELLOW, 0.04, seed) if face != "north" else 
            glow.rect(x, y, x + w, y + h, shade((255, 255, 100), 0.8))
        ),
        "tendril_base": p_eye_glow,  # Réutiliser pour la brillance des bases de tentacules
    }
    
    return "ood", bones, painters


def ood():
    """Générer l'Ood"""
    name, bones, painters = ood_base()
    geo, tex, glow = build(name, bones, 16, 256, painters)
    write("entity", name, geo, tex, glow)


# ---------------------------------------------------------------------------
# Adipose
# ---------------------------------------------------------------------------
def adipose_base():
    """Corps de base d'un Adipose"""
    # Petite créature ronde et dodue
    body = [
        # Corps principal
        ((-3, 0, -3), (6, 8, 6), {"*": "white"}),
        # Antennes
        ((-2, 8, -2), (1, 2, 1), {"*": "white"}),
        ((2, 8, -2), (1, 2, 1), {"*": "white"}),
        # Yeux
        ((-1, 4, -2), (1, 1, 2), {"*": "black", "north": "eye_glow"}),
        ((1, 4, -2), (1, 1, 2), {"*": "black", "north": "eye_glow"}),
        # Bouche
        ((0, 2, -1), (2, 2, 1), {"*": "red"}),
    ]
    
    # Pieds/pattes
    left_foot = [
        ((-2, 0, -4), (2, 2, 2), {"*": "white"}),
    ]
    
    right_foot = [
        ((0, 0, -4), (2, 2, 2), {"*": "white"}),
    ]
    
    bones = [
        {"name": "root", "pivot": [0, 0, 0], "cubes": body},
        {"name": "left_foot", "parent": "root", "pivot": [0, 0, 0], "cubes": left_foot},
        {"name": "right_foot", "parent": "root", "pivot": [0, 0, 0], "cubes": right_foot},
    ]
    
    painters = {
        "white": lambda tex, glow, x, y, w, h, s, face, seed: noisy(tex, x, y, x + w, y + h, WHITE, 0.02, seed),
        "black": p_black,
        "red": lambda tex, glow, x, y, w, h, s, face, seed: noisy(tex, x, y, x + w, y + h, RED, 0.04, seed),
        "eye_glow": p_eye_glow,
    }
    
    return "adipose", bones, painters


def adipose():
    """Générer l'Adipose"""
    name, bones, painters = adipose_base()
    geo, tex, glow = build(name, bones, 16, 256, painters)
    write("entity", name, geo, tex, glow)


# ---------------------------------------------------------------------------
# Fonction principale
# ---------------------------------------------------------------------------
if __name__ == "__main__":
    print("Génération des entités Doctor Who...")
    
    # Générer le Dalek et ses variantes
    dalek()
    
    # Générer les autres entités
    cyberman()
    weeping_angel()
    ood()
    adipose()
    
    print("Génération terminée !")