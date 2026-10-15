"""
Générateur des fichiers de génération du monde des planètes (data/doctorwho/worldgen et tags).

- Structures : 3 tailles par planète (Skaro, Gallifrey, Lune), un « structure_set » par planète
  qui choisit la taille au hasard (petite fréquente, grande rare) et règle l'espacement.
- Minerais du jeu dans la roche de chaque planète, par couches (ardoise des abîmes au fond).
- Grottes, grandes cavernes et canyons (carvers) ; puits de mine abandonnés (mineshafts du jeu).

Utilisation (depuis la racine du projet) :
    python tools/generate_worldgen.py

Python 3 pur, aucune dépendance.
"""
import json
import os

DATA = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "data", "doctorwho")

# Planète -> (structures (id, poids), espacement en chunks, séparation, sel)
BUILDINGS = {
    "skaro": ([("dalek_outpost", 4), ("dalek_city", 2), ("dalek_capital", 1)], 48, 16, 19631123),
    "gallifrey": ([("timelord_house", 4), ("arcadia", 2), ("citadel", 1)], 48, 16, 20050326),
    "moon": ([("moon_outpost", 4), ("moon_base", 2), ("moon_station", 1)], 48, 16, 19670211),
}


# Roche de chaque planète (là où poussent les minerais) et minerai bonus de la planète
ROCKS = {"skaro": "doctorwho:skaro_rock", "gallifrey": "minecraft:stone", "moon": "doctorwho:moon_rock"}
BONUS = {"skaro": "redstone", "gallifrey": "gold", "moon": "iron"}

# Minerai -> (taille du filon, filons par chunk, répartition, Y min, Y max, part jetée au contact de l'air)
# Le monde des planètes va de Y 0 à 256 ; la surface est vers Y 65-90.
ORES = {
    "coal": (17, 18, "uniform", 20, 140, 0.0),
    "copper": (10, 12, "trapezoid", 0, 100, 0.0),
    "iron": (9, 14, "trapezoid", 0, 90, 0.0),
    "gold": (9, 4, "trapezoid", 0, 40, 0.5),
    "lapis": (7, 2, "trapezoid", 0, 40, 0.0),
    "redstone": (8, 6, "uniform", 1, 20, 0.0),
    "diamond": (4, 3, "trapezoid", -8, 22, 0.5),
    "emerald": (3, 3, "trapezoid", 40, 180, 0.0),
}


def save(path, obj):
    full = os.path.join(DATA, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    with open(full, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)
        f.write("\n")


def remove(path):
    full = os.path.join(DATA, path)
    if os.path.exists(full):
        os.remove(full)


def structure(sid, planet_tag, step="surface_structures", adaptation="beard_box"):
    save(f"worldgen/structure/{sid}.json", {
        "type": "doctorwho:planet_structure",
        "kind": sid,
        "biomes": planet_tag,
        "step": step,
        "terrain_adaptation": adaptation,
        "spawn_overrides": {},
    })


def structure_set(name, structures, spacing, separation, salt):
    save(f"worldgen/structure_set/{name}.json", {
        "structures": [{"structure": f"doctorwho:{sid}", "weight": weight} for sid, weight in structures],
        "placement": {"type": "minecraft:random_spread", "spacing": spacing, "separation": separation, "salt": salt},
    })


# Villages : un par planète, plus fréquents que les grands bâtiments
VILLAGES = {"skaro": 34, "gallifrey": 34, "moon": 34}


def villages():
    for i, (planet, spacing) in enumerate(VILLAGES.items()):
        sid = f"{planet}_village"
        structure(sid, f"#doctorwho:is_{planet}")
        structure_set(f"{planet}_villages", [(sid, 1)], spacing, 12, 31415926 + i)


def buildings():
    for planet, (structures, spacing, separation, salt) in BUILDINGS.items():
        save(f"tags/worldgen/biome/is_{planet}.json", {"values": [f"doctorwho:{planet}"]})
        for sid, _ in structures:
            structure(sid, f"#doctorwho:is_{planet}")
            remove(f"tags/worldgen/biome/has_structure/{sid}.json")
            remove(f"worldgen/structure_set/{sid}.json")
        structure_set(f"{planet}_buildings", structures, spacing, separation, salt)


def ores():
    for planet, rock in ROCKS.items():
        placed = []
        for ore, (size, count, shape, low, high, discard) in ORES.items():
            if ore == BONUS[planet]:
                count = count * 3 // 2 + 2
            rock_target = ({"predicate_type": "minecraft:tag_match", "tag": "minecraft:stone_ore_replaceables"}
                           if rock == "minecraft:stone" else {"predicate_type": "minecraft:block_match", "block": rock})
            deep = "deepslate_" + ore + "_ore"
            name = f"ore_{ore}_{planet}"
            save(f"worldgen/configured_feature/{name}.json", {
                "type": "minecraft:ore",
                "config": {
                    "size": size,
                    "discard_chance_on_air_exposure": discard,
                    "targets": [
                        {"target": rock_target, "state": {"Name": f"minecraft:{ore}_ore"}},
                        {"target": {"predicate_type": "minecraft:tag_match", "tag": "minecraft:deepslate_ore_replaceables"},
                         "state": {"Name": f"minecraft:{deep}"}},
                    ],
                },
            })
            save(f"worldgen/placed_feature/{name}.json", {
                "feature": f"doctorwho:{name}",
                "placement": [
                    {"type": "minecraft:count", "count": count},
                    {"type": "minecraft:in_square"},
                    {"type": "minecraft:height_range", "height": {
                        "type": f"minecraft:{shape}",
                        "min_inclusive": {"absolute": low},
                        "max_inclusive": {"absolute": high},
                    }},
                    {"type": "minecraft:biome"},
                ],
            })
            placed.append(f"doctorwho:{name}")
        set_biome_step(planet, 6, placed)  # étape 6 = underground_ores


def set_biome_step(planet, step, features):
    """Remplace la liste d'une étape de génération (index) dans le biome de la planète."""
    path = os.path.join(DATA, "worldgen", "biome", planet + ".json")
    with open(path, encoding="utf-8") as f:
        biome = json.load(f)
    steps = biome.get("features", [])
    while len(steps) < 11:
        steps.append([])
    steps[step] = features
    biome["features"] = steps
    save(f"worldgen/biome/{planet}.json", biome)


def deepslate_layer():
    """Couche d'ardoise des abîmes au fond de chaque planète (transition entre Y 8 et Y 20)."""
    rule = {
        "type": "minecraft:condition",
        "if_true": {"type": "minecraft:vertical_gradient", "random_name": "doctorwho:deepslate",
                    "true_at_and_below": {"absolute": 8}, "false_at_and_above": {"absolute": 20}},
        "then_run": {"type": "minecraft:block", "result_state": {"Name": "minecraft:deepslate", "Properties": {"axis": "y"}}},
    }
    for planet in ROCKS:
        path = os.path.join(DATA, "worldgen", "noise_settings", planet + ".json")
        with open(path, encoding="utf-8") as f:
            settings = json.load(f)
        sequence = settings["surface_rule"]["sequence"]
        sequence[:] = [r for r in sequence if "doctorwho:deepslate" not in json.dumps(r)]
        sequence.insert(1, rule)  # juste après la bedrock
        save(f"worldgen/noise_settings/{planet}.json", settings)


def carver(name, carver_type, config):
    config = dict(config, replaceable="#doctorwho:carver_replaceables")
    save(f"worldgen/configured_carver/{name}.json", {"type": carver_type, "config": config})


def caves():
    """Grottes et canyons creusés dans la roche des planètes (même réglages que la Surface)."""
    save("tags/blocks/carver_replaceables.json", {"values": [
        "#minecraft:overworld_carver_replaceables", "doctorwho:skaro_rock", "doctorwho:skaro_sand",
        "doctorwho:moon_rock", "doctorwho:moon_dust", "doctorwho:gallifrey_grass"]})
    # Nombres aléatoires « flottants » : en 1.20.1, les bornes sont dans un objet « value »
    uniform = lambda lo, hi: {"type": "minecraft:uniform", "value": {"min_inclusive": lo, "max_exclusive": hi}}
    for planet in ROCKS:
        lava = {"above_bottom": 2 if planet == "moon" else 8}
        carver(f"cave_{planet}", "minecraft:cave", {
            "probability": 0.15, "lava_level": lava, "yScale": 0.5,
            "y": {"type": "minecraft:uniform", "min_inclusive": {"above_bottom": 8}, "max_inclusive": {"absolute": 180}},
            "horizontal_radius_multiplier": uniform(0.7, 1.4),
            "vertical_radius_multiplier": uniform(0.8, 1.3),
            "floor_level": uniform(-1.0, -0.4),
        })
        carver(f"cave_large_{planet}", "minecraft:cave", {
            "probability": 0.07, "lava_level": lava, "yScale": 0.5,
            "y": {"type": "minecraft:uniform", "min_inclusive": {"above_bottom": 8}, "max_inclusive": {"absolute": 47}},
            "horizontal_radius_multiplier": uniform(0.7, 1.4),
            "vertical_radius_multiplier": uniform(0.8, 1.3),
            "floor_level": uniform(-1.0, -0.4),
        })
        carver(f"canyon_{planet}", "minecraft:canyon", {
            "probability": 0.01, "lava_level": lava, "yScale": 3.0,
            "y": {"type": "minecraft:uniform", "min_inclusive": {"absolute": 10}, "max_inclusive": {"absolute": 67}},
            "vertical_rotation": uniform(-0.125, 0.125),
            "shape": {
                "distance_factor": uniform(0.75, 1.0),
                "thickness": {"type": "minecraft:trapezoid", "value": {"min": 0.0, "max": 6.0, "plateau": 2.0}},
                "width_smoothness": 3,
                "horizontal_radius_factor": uniform(0.75, 1.0),
                "vertical_radius_default_factor": 1.0,
                "vertical_radius_center_factor": 0.0,
            },
        })
        path = os.path.join(DATA, "worldgen", "biome", planet + ".json")
        with open(path, encoding="utf-8") as f:
            biome = json.load(f)
        biome["carvers"] = {"air": [f"doctorwho:cave_{planet}", f"doctorwho:cave_large_{planet}", f"doctorwho:canyon_{planet}"]}
        save(f"worldgen/biome/{planet}.json", biome)

        # Niveau de la mer à 63 (les planètes n'ont pas d'eau) : il sert à placer les puits de mine sous la surface
        path = os.path.join(DATA, "worldgen", "noise_settings", planet + ".json")
        with open(path, encoding="utf-8") as f:
            settings = json.load(f)
        settings["sea_level"] = 63
        save(f"worldgen/noise_settings/{planet}.json", settings)

    # Puits de mine abandonnés : ceux du jeu (même fréquence que dans la Surface)
    save("tags/worldgen/biome/has_mineshaft.json", {"values": [f"doctorwho:{planet}" for planet in ROCKS]})
    save("worldgen/structure/mineshaft.json", {
        "type": "minecraft:mineshaft", "biomes": "#doctorwho:has_mineshaft", "mineshaft_type": "normal",
        "step": "underground_structures", "spawn_overrides": {}})
    save("worldgen/structure_set/mineshafts.json", {
        "structures": [{"structure": "doctorwho:mineshaft", "weight": 1}],
        "placement": {"type": "minecraft:random_spread", "spacing": 1, "separation": 0, "salt": 0,
                      "frequency": 0.004, "frequency_reduction_method": "legacy_type_3"}})


if __name__ == "__main__":
    buildings()
    villages()
    ores()
    deepslate_layer()
    caves()
    print("OK worldgen")
