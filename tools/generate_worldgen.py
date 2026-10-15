"""
Générateur des fichiers de génération du monde des planètes (data/doctorwho/worldgen et tags).

- Structures : 3 tailles par planète (Skaro, Gallifrey, Lune), un « structure_set » par planète
  qui choisit la taille au hasard (petite fréquente, grande rare) et règle l'espacement.

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


def buildings():
    for planet, (structures, spacing, separation, salt) in BUILDINGS.items():
        save(f"tags/worldgen/biome/is_{planet}.json", {"values": [f"doctorwho:{planet}"]})
        for sid, _ in structures:
            structure(sid, f"#doctorwho:is_{planet}")
            remove(f"tags/worldgen/biome/has_structure/{sid}.json")
            remove(f"worldgen/structure_set/{sid}.json")
        structure_set(f"{planet}_buildings", structures, spacing, separation, salt)


if __name__ == "__main__":
    buildings()
    print("OK worldgen")
