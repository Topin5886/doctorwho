"""
Générateur des recettes de fabrication du mod Doctor Who (data/doctorwho/recipes).

- Tournevis soniques : un modèle par couleur de verre (l'embout) + redstone + corps (fer, cuivre ou or).
- TARDIS : un extérieur par objet posé en haut de la recette (glowstone, lanterne...).
- Clé du TARDIS, et blocs du décor de la salle de console.

Pour ajouter un modèle : ajouter une ligne dans SONICS ou EXTERIORS, puis relancer :
    python tools/generate_recipes.py

Python 3 pur, aucune dépendance.
"""
import json
import os

RECIPES = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "doctorwho", "recipes")

# Tournevis : modèle -> (verre de l'embout, corps)
SONICS = {
    "second": ("light_gray_stained_glass", "iron_ingot"),
    "third": ("gray_stained_glass", "iron_ingot"),
    "fourth": ("white_stained_glass", "iron_ingot"),
    "seventh": ("magenta_stained_glass", "iron_ingot"),
    "eighth": ("cyan_stained_glass", "iron_ingot"),
    "tenth": ("light_blue_stained_glass", "iron_ingot"),
    "eleventh": ("lime_stained_glass", "copper_ingot"),
    "twelfth": ("purple_stained_glass", "iron_ingot"),
    "thirteenth": ("orange_stained_glass", "copper_ingot"),
    "war": ("red_stained_glass", "iron_ingot"),
    "river": ("blue_stained_glass", "gold_ingot"),
    "master": ("yellow_stained_glass", "gold_ingot"),
    "fourteenth": ("light_blue_stained_glass", "copper_ingot"),
}

# TARDIS : extérieur -> objet posé en haut (la lampe du toit)
EXTERIORS = {
    "classic": "lantern",
    "fifth": "end_rod",
    "eighth": "candle",
    "fourteenth": "pearlescent_froglight",
    "tenth": "sea_lantern",
    "eleventh": "shroomlight",
    "twelfth": "glowstone",
    "thirteenth": "redstone_lamp",
    "war": "soul_lantern",
}


def item(name):
    return {"item": name if ":" in name else "minecraft:" + name}


def save(name, recipe):
    os.makedirs(RECIPES, exist_ok=True)
    with open(os.path.join(RECIPES, name + ".json"), "w", encoding="utf-8") as f:
        json.dump(recipe, f, indent=2)
        f.write("\n")


def shaped(name, category, pattern, key, result, count=1, nbt=None):
    output = item(result)
    if count > 1:
        output["count"] = count
    recipe_type = "minecraft:crafting_shaped"
    if nbt:
        output["nbt"] = nbt
        recipe_type = "doctorwho:crafting_shaped_nbt"
    save(name, {
        "type": recipe_type,
        "category": category,
        "pattern": pattern,
        "key": {symbol: item(ingredient) for symbol, ingredient in key.items()},
        "result": output,
    })


def shapeless(name, category, ingredients, result, count=1):
    output = item(result)
    if count > 1:
        output["count"] = count
    save(name, {
        "type": "minecraft:crafting_shapeless",
        "category": category,
        "ingredients": [item(i) for i in ingredients],
        "result": output,
    })


def sonics():
    # Recette de base (colorant bleu clair) : tournevis du modèle par défaut
    shaped("sonic_screwdriver", "equipment", ["D", "R", "I"],
           {"D": "light_blue_dye", "R": "redstone", "I": "iron_ingot"}, "doctorwho:sonic_screwdriver")
    for variant, (tip, body) in SONICS.items():
        shaped("sonic_screwdriver_" + variant, "equipment", ["G", "R", "B"],
               {"G": tip, "R": "redstone", "B": body}, "doctorwho:sonic_screwdriver",
               nbt='{Variant:"%s"}' % variant)


def tardis():
    for variant, top in EXTERIORS.items():
        name = "tardis" if variant == "twelfth" else "tardis_" + variant
        shaped(name, "misc", [" L ", "BEB", "BBB"],
               {"L": top, "B": "blue_concrete", "E": "ender_eye"}, "doctorwho:tardis",
               nbt='{BlockEntityTag:{Variant:"%s"}}' % variant)


def key():
    shapeless("tardis_key", "equipment", ["iron_ingot", "gold_nugget", "lapis_lazuli"], "doctorwho:tardis_key")
    save("tardis_key_copy", {"type": "doctorwho:tardis_key_copy", "category": "equipment"})


def decoration():
    b = "building"
    # Murs à roundels : 8 blocs de base autour d'une boule d'argile (le rond)
    for wall, base in (("roundel_wall", "orange_terracotta"), ("white_roundel_wall", "white_concrete"),
                       ("dark_roundel_wall", "gray_concrete"), ("copper_roundel_wall", "copper_ingot")):
        shaped(wall, b, ["XXX", "XCX", "XXX"], {"X": base, "C": "clay_ball"}, "doctorwho:" + wall, 8)
        # Version lumineuse : 4 murs autour d'une glowstone
        shaped(wall + "_lit", b, [" W ", "WGW", " W "], {"W": "doctorwho:" + wall, "G": "glowstone"},
               "doctorwho:" + wall + "_lit", 4)
    shaped("coral_wall", b, ["OR", "RO"], {"O": "orange_terracotta", "R": "red_terracotta"}, "doctorwho:coral_wall", 4)
    shaped("coral_pillar", b, ["C", "C", "C"], {"C": "doctorwho:coral_wall"}, "doctorwho:coral_pillar", 3)
    shaped("coral_strut", b, ["C  ", " C ", "  C"], {"C": "doctorwho:coral_wall"}, "doctorwho:coral_strut", 3)
    shaped("metal_grating", b, ["II", "II"], {"I": "iron_bars"}, "doctorwho:metal_grating", 4)
    shaped("hex_light", b, [" I ", "IGI", " I "], {"I": "iron_ingot", "G": "glowstone"}, "doctorwho:hex_light", 4)
    shaped("copper_beam", b, ["C", "C"], {"C": "copper_ingot"}, "doctorwho:copper_beam", 2)
    shaped("wall_lamp", b, ["C", "G"], {"C": "copper_ingot", "G": "glowstone_dust"}, "doctorwho:wall_lamp", 2)
    shaped("railing", b, ["CCC", "CCC"], {"C": "copper_ingot"}, "doctorwho:railing", 8)


# Grandes salles de console : roundels en relief (8 blocs de base autour d'une boule d'argile)
ROUNDEL_BASES = {"classic": "smooth_quartz", "copper": "cut_copper"}
# Lumières de sous-plancher : lanterne aquatique + colorant
UNDERLIGHT_DYES = {"green": "lime_dye"}
# Grilles de sol : lingot du thème en diagonale avec des barreaux de fer
GRATE_INGOTS = {"copper": "copper_ingot"}


def interior_decoration():
    b = "building"
    for family, base in ROUNDEL_BASES.items():
        name = family + "_roundel"
        shaped(name, b, ["XXX", "XCX", "XXX"], {"X": base, "C": "clay_ball"}, "doctorwho:" + name, 8)
        shaped(name + "_lit", b, [" W ", "WGW", " W "], {"W": "doctorwho:" + name, "G": "glowstone"},
               "doctorwho:" + name + "_lit", 4)
    for color, dye in UNDERLIGHT_DYES.items():
        shapeless(color + "_underlight", b, ["sea_lantern", dye], "doctorwho:" + color + "_underlight")
    for family, ingot in GRATE_INGOTS.items():
        shaped(family + "_floor_grate", b, ["IB", "BI"], {"I": ingot, "B": "iron_bars"},
               "doctorwho:" + family + "_floor_grate", 4)
    # Classique
    shaped("classic_pilaster", b, ["Q", "Q"], {"Q": "quartz_pillar"}, "doctorwho:classic_pilaster", 2)
    shaped("light_panel", b, [" W ", "WGW", " W "], {"W": "white_concrete", "G": "glowstone"}, "doctorwho:light_panel", 4)
    shaped("scanner", b, ["III", "IGI", "IRI"], {"I": "iron_ingot", "G": "black_stained_glass_pane", "R": "redstone"},
           "doctorwho:scanner")
    shaped("hat_stand", b, ["N", "S", "P"], {"N": "gold_nugget", "S": "stick", "P": "dark_oak_slab"}, "doctorwho:hat_stand")
    # Cuivre
    shaped("copper_wall_panel", b, ["CN", "NC"], {"C": "cut_copper", "N": "iron_nugget"}, "doctorwho:copper_wall_panel", 2)
    shaped("copper_strut", b, ["C  ", " C ", "  C"], {"C": "cut_copper"}, "doctorwho:copper_strut", 3)
    shaped("copper_glass_floor", b, [" C ", "CGC", " C "], {"C": "copper_ingot", "G": "glass"},
           "doctorwho:copper_glass_floor", 2)


if __name__ == "__main__":
    sonics()
    tardis()
    key()
    decoration()
    interior_decoration()
    print("OK recettes :", len(os.listdir(RECIPES)))
