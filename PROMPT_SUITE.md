# Prompt de reprise — Mod Doctor Who (Fabric 1.20.1)

> À coller tel quel dans une nouvelle conversation avec Claude (Claude Code, dans le dossier du projet).

---

Tu es un développeur Java expert en mods Minecraft **Fabric 1.20.1**. Tu reprends le développement de mon mod **Doctor Who**, entièrement en **français** (code commenté en français, traductions fr_fr + en_us), situé dans `C:\Users\jules\Documents\mods david`.

Règles de travail :
- Travaille **en autonomie**, étape par étape, et explique-moi simplement en français ce que tu fais.
- **Compile après chaque gros changement** et corrige les erreurs avant de continuer.
- **Teste en jeu** quand c'est possible (voir « Tester »), et dis-moi honnêtement ce qui a été vérifié ou non.
- Lis d'abord le code existant (`src/main/java/fr/doctorwho`, `tools/`) avant de modifier quoi que ce soit.
- Ne réinstalle rien : tout l'environnement est déjà en place.

---

## 1. Environnement (déjà installé)

| Élément | Valeur |
|---|---|
| Minecraft | 1.20.1 |
| Fabric Loader / API | 0.17.2 / 0.92.6+1.20.1 |
| Mappings | Yarn 1.20.1+build.10 |
| Loom / Gradle | 1.10-SNAPSHOT / 8.14.3 (wrapper) |
| GeckoLib | 4.8.4 (`software.bernie.geckolib:geckolib-fabric-1.20.1`) + `com.eliotlash.mclib:mclib:20` |
| Java | **JDK 17** : `C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot` (déjà dans `gradle.properties` via `org.gradle.java.home`). Le Java 25 par défaut du PC est incompatible. |
| IDE | IntelliJ IDEA 2026.2.3 + plugin Minecraft Development |
| Outils | Python 3.12 (sans Pillow), FFmpeg 9 avec libvorbis (installé via winget), voix Windows « Microsoft Hortense » (fr-FR) et « Zira » (en-US) |

**Compiler** (Gradle n'est pas dans le PATH) :
```powershell
$g = Get-ChildItem "$env:USERPROFILE\.gradle\wrapper\dists\gradle-8.14.3-bin" -Recurse -Filter gradle.bat | Select-Object -First 1
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot"
& $g.FullName build --console=plain
```
`build` copie **automatiquement** le `.jar` dans mon profil CurseForge **« david mod »** (`C:\Users\jules\curseforge\minecraft\Instances\Fabulously Optimized\mods`, propriété `curseforge_mods_dir`). Minecraft doit être fermé pendant la copie. Ce profil = modpack Fabulously Optimized 5.4.1 (Sodium, Iris…), Fabric Loader 0.19.5, Fabric API 0.92.12, GeckoLib, + 3 shaders (Complementary, BSL, MakeUp).

**Tester** :
- `runClient "-PquickPlay=New World"` ouvre directement le monde de test (écran « réglages expérimentaux » → cliquer « Je sais ce que je fais ! »). Le client de test est en français.
- Si la fenêtre Minecraft reste réduite : la restaurer avec `ShowWindow`/`SetForegroundWindow` (user32) sur `MainWindowHandle` du processus java « Minecraft* ».
- Avec l'outil de contrôle du PC, **chaque clic souris fait tourner la caméra** : pour tester, mettre temporairement `key_key.use:key.keyboard.r` dans `run/options.txt` (touche R = clic droit), puis remettre `key.mouse.right` à la fin. Utiliser les commandes `/tp`, `/execute in doctorwho:moon run tp …`, `/item replace entity @s weapon.mainhand with …`.
- Clavier AZERTY : avancer = Z.
- Mon écran a un effet de transparence violette (Windhawk) : ce n'est pas un bug du jeu. Une fenêtre Windhawk peut passer au premier plan et bloquer l'outil de contrôle.

---

## 2. Récapitulatif — ce qui est FAIT (et testé en jeu)

### Architecture du code (`fr.doctorwho`)
- `DoctorWhoMod` (commun) / `client.DoctorWhoClient` (client).
- `registry/` : `ModBlocks` (listes `DECORATION`, `PLANET_BLOCKS` avec objets auto), `ModItems`, `ModBlockEntities`, `ModSounds`, `ModItemGroups` (onglet créatif « Doctor Who »), `ModDimensions` (clés + gravité lunaire).
- `block/` : `AbstractTardisDoorBlock` (bloc 2 de haut, portes, passage en marchant), `TardisBlock`, `TardisInteriorDoorBlock`, `TardisConsoleBlock` (+ `ConsolePartBlock` invisibles), `DecorBlock`, `FacingDecorBlock`.
- `block/entity/` : `AbstractTardisDoorBlockEntity` (animations portes), `TardisBlockEntity` (id, variante, clignotement), `TardisInteriorDoorBlockEntity`, `TardisConsoleBlockEntity` (rotor, tournevis posé).
- `tardis/` : `TardisData` + `TardisRegistry` (PersistentState « doctorwho_tardis »), `TardisManager` (entrer/sortir, portes synchronisées, décor), `TardisFlight` (voyages), `InteriorBuilder` (salle de console, **VERSION 4**, 4 décors via `Theme`), `Architecture` (couloirs + salles), `Variants`, `Waypoint`, `RoomExit`, `FlightPhase`.
- `network/` : `ModNetworking` (paquets panneau ↔ serveur, choix du décor), `FlightPanelData`.
- `tardis/DesktopChoice` (choix du décor à la 1re entrée), `tardis/TardisKeys` (clé : jumelage, verrou, rappel, localisation).
- `item/` : `TardisKeyItem`, `TardisKeyCopyRecipe` (copie à l'établi), `TardisItem` (extérieur dans `BlockEntityTag.Variant`), `NbtShapedRecipeSerializer` (type `doctorwho:crafting_shaped_nbt` : résultat avec NBT). `registry/ModRecipes`.
- `mixin/client/IntegratedServerLoaderMixin` (+ `doctorwho.mixins.json`) : plus d'écran « réglages expérimentaux ».
- `client/sound/ConsoleHumSound` : bourdonnement en boucle dans l'intérieur ; `client/screen/DesktopChoiceScreen`.
- `client/` : renderers GeckoLib (`TardisRenderer`, `TardisExteriorModel`, `TardisConsoleRenderer`, `SonicScrewdriverRenderer`), écrans `FlightPanelScreen` + `StyledButton`.
- `worldgen/` : structures des planètes. `ModStructures` (type `doctorwho:planet_structure` + pièce `planet_piece`), `PlanetStructure` (choix de la hauteur, champ JSON `kind`), `PlanetStructurePiece` (sauvegarde centre + graine), `StructureCanvas` (outils de construction limités au chunk en cours : disques, anneaux, sphères étanches, fondations, cages, coffres, créatures), et un constructeur par famille : `DalekCityBuilder`, `CitadelBuilder`, `MoonBaseBuilder` (3 tailles chacun, `StructureSize`), `VillageBuilder` (villages des 3 planètes), `GolemLairBuilder` (repaire souterrain du golem). `tools/generate_worldgen.py` génère tous les JSON de génération (structures, fréquences, minerais, grottes, puits de mine).
- `tools/generate_assets.py` (modèles/textures GeckoLib : TARDIS ×9, porte, console, 13 soniques, icônes TARDIS et clé) et `tools/generate_blocks.py` (blocs JSON, sceau gallifreyen). `tools/generate_recipes.py` (toutes les recettes), `tools/generate_tardis_sounds.py` (sons du TARDIS et du tournevis, synthétisés). **Les relancer après modification** : `python -I tools/generate_assets.py`.

### Fonctionnalités
- **TARDIS** : cabine 2 blocs de haut, portes animées (clic droit), lampe lumineuse, **9 extérieurs** (1963, 1980, 1996, 2005, 2010 avec écusson, 2014, 2018, 2023, War Doctor) changeables par circuit caméléon. Cassé puis reposé, il garde son intérieur.
- **Intérieur** (dimension `doctorwho:tardis_interior`, une zone par TARDIS espacée de 2048 blocs) : on entre/sort en marchant dans les portes ouvertes ; porte intérieure synchronisée ; salle ronde style Dalek Mod ; **6 décors** (Corail, Cuivre, Classique blanc, Sombre avec bibliothèques, Colonnes 1963, Victorien 1996), **choisis à la première entrée** (écran de cartes illustrées ; Échap = ressortir).
- **Consoles** (≈ 4,6 blocs de large) : **une par décor** (`ConsoleStyle`, propriété de bloc `style`) — corail (6 branches de corail), cuivre (cage de cuivre), classique blanche, sombre 2014 (grand anneau doré + écran sur pied), 1963 (colonne d'instruments), victorienne (bois, dais de laiton). Modèles `geo/block/console_<style>.geo.json` générés par `consoles()` dans `tools/generate_assets.py` ; aperçu : `tools/preview.py` (gère maintenant la rotation des cubes). Rotor animé, 6 pupitres, manette des gaz (os `throttle`), **logement du tournevis** (os `sonic_port`).
- **Colonne jusqu'au plafond** (toutes sauf la classique, à la demande de David) : os `shaft` étiré et os `mount` remonté par `TardisConsoleRenderer.renderRecursively` jusqu'au premier bloc au-dessus de la console (salle principale 9 blocs, salle de console secondaire 5 ; cachée en plein air). Collision : blocs invisibles `console_part` posés par `TardisConsoleBlock.place` (disque 5 x 5, centre plus haut, colonne). `InteriorBuilder.VERSION = 5` : les anciennes salles sont reconstruites à la prochaine entrée (rambarde éloignée à 6 blocs).
- **Panneau de console** (style sombre/holographique, sceau gallifreyen animé), 3 onglets :
  - **VOL** : 6 destinations, X/Y/Z, « Ici », aléatoire, 6 waypoints, bouton Décoller ;
  - **APPARENCE** : extérieur, décor (bouton « Reconstruire »), modèle du tournevis posé ;
  - **ARCHITECTURE** : sorties libres + 8 types de salles (chambre, cuisine, bibliothèque, console secondaire pilotable, piscine, serre, atelier, croisement = 3 sorties), max 16, vérification de place.
- **Voyage** : portes fermées, dématérialisation clignotante (7 s), vortex (5 s), rematérialisation (7 s) sur sol sûr (plateforme si vide), messages de progression pour l'équipage, sons (provisoires, vanilla).
- **13 tournevis soniques** (Deuxième, Troisième, Quatrième, Septième, Huitième, Dixième, Onzième, Douzième, Treizième, Quatorzième, War Doctor, River Song, laser du Maître), 3 modes (Ouverture — sauf le bois !, Activation, Analyse), sons, animation.
- **Planètes** : Lune (ciel noir, gravité réduite), Skaro (désert rouge), Gallifrey (herbe rouge, ciel orange, **arbres argentés**).
- **Blocs décoratifs** (onglet créatif) : murs à roundels (4 couleurs, éteints/lumineux), corail, grille, dalle hexagonale, piliers/branches de corail, poutre, applique, rambarde ; blocs des planètes.
- **Clé du TARDIS** : un TARDIS neuf est livré avec sa clé ; clé vierge (fer + pépite d'or + lapis) → clic droit sur un TARDIS non verrouillé pour la jumeler. Clé jumelée : clic droit sur le TARDIS = verrouiller/déverrouiller (portes fermées, incassable en survie, ouvrir de l'intérieur déverrouille) ; **clic droit sur le sol = le TARDIS vient s'y poser** ; clic droit dans le vide = où est le TARDIS. Copie : clé jumelée + clé vierge à l'établi.
- **Recettes** (41) : tournevis par modèle (verre de couleur + redstone + fer/cuivre/or), TARDIS par extérieur (béton bleu + œil de l'Ender + lampe du toit : glowstone = 2014, lanterne = 1963, lanterne aquatique = 2005, champilampe = 2010, lampe à redstone = 2018, lanterne des âmes = War, barre de l'End = 1980, bougie = 1996, perlesciente = 2023), clé, 15 blocs décoratifs.
- **Sons** : décollage et atterrissage = **sons fournis par David** (extraits de la série : usage privé entre amis, ne pas publier ; décollage = 12 premières s, atterrissage = 8 dernières s, −10 dB, mono). Synthétisés : passage dans la salle (`enter`), choc, bourdonnement de la salle (boucle), tournevis. `pack_sons_perso/` = pack de ressources vide pour remplacer les autres sons. **Jamais écoutés par Claude** (pas de carte son dans le cloud) : à valider à l'oreille.
- **Plus d'écran « réglages expérimentaux »** (création et chargement de monde).
- **Structures des planètes** (étape B + grosse mise à jour) — `/locate structure doctorwho:<id>` puis les coordonnées dans le panneau de vol. **3 tailles par planète** (petite fréquente, grande rare), un seul `structure_set` par planète (`<planète>_buildings`, espacement 48 chunks, poids 4/2/1) :
  - Skaro : `dalek_outpost` (dôme r18, 3 tours), `dalek_city` (r34, 6 tours), `dalek_capital` (r52, 9 tours, **prison** avec 2 Ood captifs).
  - Gallifrey : `timelord_house`, `arcadia`, `citadel` (dôme de verre, Capitole, crypte à Ange Pleureur ; K9 dans la grande).
  - Lune : `moon_outpost` (2 modules), `moon_base` (4 modules), `moon_station` (+ hangar sontarien avec soucoupe et observatoire).
  - **Villages** (`skaro_village`, `gallifrey_village`, `moon_village`, espacement 34) : 5-7 maisons aux matériaux de la planète, champs (sous serre sur la Lune), place avec cloche, 1 golem de fer, villageois ; **un village sur deux cache un espion** (Zygon ou Slitheen déguisé).
  - **Repaire du golem de redstone** (`golem_lair`, sous terre sur les 3 planètes **et dans la Surface**, espacement 40) : caverne en dôme de pierre noire veinée de redstone, piliers à lampes, 2 coffres, puits à échelle jusqu'à la surface (margelle + torches de redstone).
  - Butin : `data/doctorwho/loot_tables/chests/*.json` et `entities/redstone_golem.json`.
- **Sous-sol des planètes** : minerais du jeu (charbon, cuivre, fer, or, lapis, redstone, diamant, émeraude) par couches, + minerai bonus par planète (Skaro redstone, Gallifrey or, Lune fer) ; couche d'ardoise des abîmes sous Y 8-20 (minerais « deepslate ») ; grottes, grandes cavernes, canyons ; **puits de mine abandonnés** (ceux du jeu).
- **Gallifrey en plein jour permanent** (`fixed_time: 6000` dans `dimension_type/gallifrey.json`).

### Pièges déjà rencontrés (à connaître)
- GeckoLib **inverse l'axe X** : sur un cube, la face extérieure côté −X est « east » et côté +X « west ». Le front des modèles = nord (−Z).
- Animations GeckoLib : utiliser des images clés simples (`"0.4": [0, -55, 0]`) ; le format `{"post": …, "lerp_mode": "catmullrom"}` n'animait pas.
- Yarn 1.20.1 : `WallMountLocation` (pas `BlockFace`), pas de `TntBlock.primeTnt(world,pos,player)` (créer un `TntEntity`), `Blocks.GRASS` (pas `SHORT_GRASS`).
- Surcharger `render(...)` d'un `GeoBlockRenderer` crée un conflit de génériques : utiliser `renderFinal` ou un `BlockAndItemGeoLayer`.
- `/setblock` aligne les deux moitiés d'une porte (postProcessState) : pour poser un TARDIS ouvert, poser d'abord la moitié basse.
- Les sons vanilla s'appellent par leur fichier (`minecraft:portal/trigger`, `minecraft:block/beacon/power1`).
- L'avertissement « réglages expérimentaux » est supprimé par le mixin client (`IntegratedServerLoaderMixin`).
- Recette avec NBT : utiliser `"type": "doctorwho:crafting_shaped_nbt"` et `"result": {"item": …, "nbt": "{Variant:\"tenth\"}"}` (générées par `tools/generate_recipes.py`).
- `Map.of` est limité à 10 paires : au-delà, `Map.ofEntries`.
- Test en jeu possible **dans le cloud** : Xvfb + rendu logiciel (`DISPLAY=:99 LIBGL_ALWAYS_SOFTWARE=1 gradle runClient`), captures `import -window root`, clavier/souris `xdotool` (pas de son). Ne pas tuer le client avec un `pkill -f` dont le motif apparaît dans la commande elle-même (motif `devlaunch[i]njector`).
- Grandes constructions : une `Feature` ne peut écrire qu'à ±1 chunk autour du sien (les blocs plus loin sont perdus). Pour un bâtiment de plus de ~30 blocs, utiliser une **Structure** (comme `worldgen/`) : sa pièce est rappelée pour chaque chunk touché et ne pose que les blocs de ce chunk (`StructureCanvas` s'en charge). Le hasard doit dépendre de la position du bloc (`canvas.noise(x,y,z)`), jamais du `Random` du chunk, sinon les morceaux ne se raccordent pas.
- `terrain_adaptation: "beard_box"` aplanit le terrain jusqu'au bas de la boîte de la pièce : la boîte commence donc au niveau du sol (y du centre), pas sous les fondations.
- Cages à monstres : sans `custom_spawn_rules`, un monstre ne sort pas d'une cage en plein jour. `StructureCanvas.spawner` en ajoute (plage de lumière 0-15) ; dans le NBT elles s'affichent `custom_spawn_rules: {}` (valeurs par défaut omises) : c'est normal.
- Dans Yarn 1.20.1 : `net.minecraft.world.MobSpawnerEntry` (pas `block.spawner`), `LootableContainerBlockEntity.setLootTable(world, random, pos, id)`.

### Créatures (étape A — FAITE, compilée et déployée ; **jamais vue en jeu par Claude**, David les teste lui-même dans CurseForge)
- 10 créatures : Dalek (5 couleurs : bronze, argent, rouge, noir, blanc), Cyberman (Cybus 2006), Ange Pleureur, Ood, Adipose, K9 (apprivoisable à la redstone, 1 chance sur 3), Sontarien, Silence, Zygon, Slitheen. Entités dans `entity/` (`SimpleMonsterEntity` = base), renderer `client/renderer/CreatureRenderer`, types/œufs/apparitions dans `registry/ModEntities`, voix dans `ModSounds` + `tools/generate_sounds.py`.
- Modèles, textures et animations générés par **`tools/generate_entities.py`** (cubes + textures procédurales, passe de finition « pixel art » : reflets métal, biseaux, fissures sur la pierre…). Aperçu 3D sans lancer le jeu : `python -I tools/preview.py dalek/bronze cyberman weeping_angel@covering …` → `tools/preview.png`.
- Les noms d'os et d'animations utilisés par le Java ne doivent pas changer (dalek idle/walk/shoot ; cyberman, zygon, slitheen, sontarien idle/walk/attack ; ood, silence, adipose, k9 idle/walk ; weeping_angel covering/attack).
- Mode Peaceful : message « aucun monstre ne peut apparaître » au lieu d'un crash. Voix : cooldown de 70 ticks pour éviter les répliques doublées.
- Limite connue : formes en cubes ; pour un rendu type Dalek Mod il faudrait remodeler à la main dans Blockbench (mêmes noms de fichiers, aucun changement de code).
- **Déguisement** (`DisguisedMonsterEntity`, rendu `DisguisedCreatureRenderer`) : Zygons et Slitheens apparaissent en villageois (6 métiers, nom « Villageois », refusent de commercer, ni golems ni monstres ne les ciblent). Le Zygon se révèle quand un joueur (survie) approche à 3 blocs ; le Slitheen seulement quand on le frappe (éclair + gaz vert). Déguisés : 2 fois sur 3 dans la nature, toujours avec l'œuf ou dans un village ; `/summon` = vraie forme (ou `{Disguised:1b,DisguiseLook:0-5}`).
- **Golem de redstone** (mini-boss, `RedstoneGolemEntity`) : 140 PV, armure 12, barre de boss, coup de poing de zone toutes les 2 s (le bouclier le bloque), onde de choc toutes les 8 s (1 s de préparation : sauter pour l'éviter) suivie de 2 s d'essoufflement. Testé : en armure de fer, un joueur immobile tient ~14 s.

### Pièges GeckoLib liés aux créatures
- Une texture `_glowmask.png` **vide** fait planter le jeu : `generate_entities.py` supprime les masques vides (`prune_empty_glowmasks`) et le renderer n'ajoute la couche lumineuse (`AutoGlowingGeoLayer`) que pour dalek, cyberman, ood, k9.
- `TameableEntity` (K9) doit implémenter `public EntityView method_48926(){ return getWorld(); }` ; `RangedAttackMob.attack(LivingEntity, float)` (pas `shootAt`).
- Si le jar est verrouillé au `build` (« deployCurseForge FAILED »), c'est que Minecraft est ouvert : le fermer et relancer.
- Éditer les fichiers : en bash les apostrophes cassent les heredocs ; écrire un petit `.py` de patch (lancé avec `python -I`) ou utiliser l'outil d'édition.

### Pièges de la génération du monde
- Nombres aléatoires « flottants » des carvers en 1.20.1 : `{"type":"minecraft:uniform","value":{"min_inclusive":…,"max_exclusive":…}}` (sinon « Not a JSON object: null »).
- Les puits de mine se placent sous le niveau de la mer : `sea_level` des planètes mis à 63.
- `execute in <dimension>` ne limite pas `@e` à la dimension : ajouter `positioned … distance=..N` pour compter les entités d'une planète.
- Pour qu'aucun mob ne cible une créature (golem de fer et espions), surcharger `canTakeDamage()` (utilisé par `TargetPredicate`, pas par les dégâts).
- Rendu logiciel du cloud : les couches lumineuses GeckoLib (`_glowmask`) s'affichent **noires** (cristaux du golem, yeux du Cyberman) ; à vérifier sur un vrai PC.

### Points connus à améliorer
- Le cœur lumineux du rotor reste peu visible dans la colonne de verre.
- Sons de vol et de tournevis = sons vanilla détournés (à remplacer par de vrais .ogg).
- Il fait parfois sombre sur Skaro (cycle jour/nuit normal) ; Gallifrey est maintenant en plein jour permanent.
- Idée : un bouton « Atterrir près de la structure » dans le panneau de vol (via `ServerWorld.locateStructure`) pour éviter de passer par `/locate`.

---

## 3. Mes choix (à respecter)

- **Docteur par défaut : le Douzième (2014).** Changer les valeurs par défaut dans `Variants` : `DEFAULT_EXTERIOR = "twelfth"`, `DEFAULT_DESKTOP = "dark"`, `DEFAULT_SONIC = "twelfth"` (les TARDIS déjà existants gardent leurs réglages).
- **Difficulté : équilibrée.** Les créatures agressives sont dangereuses mais battables avec une armure en fer/diamant (ordre d'idée : Dalek ~40 PV, laser ~6 dégâts toutes les 2 s ; Cyberman ~30 PV, ~5 dégâts ; Ange ~50 PV). Éviter les morts instantanées.
- **Créatures voulues** : Dalek, Cyberman, Ange Pleureur, Ood, Adipose **+ Sontarien, le Silence, K9, Zygon, Slitheen**.
- **Usage : juste pour moi et mes amis** (pas de publication). Pas besoin de page de présentation ; garder l'auteur « David ».

---

## 4. À FAIRE (dans cet ordre)

### Étape A — Créatures de la série (GeckoLib) — ✅ FAITE (voir « Créatures » plus haut ; ce qui suit est la spec d'origine, gardée pour référence)
Créer `tools/generate_entities.py` (même principe que `generate_assets.py` : cubes + textures procédurales), puis pour chaque créature : entité, attributs, IA, animations (idle/marche/attaque), renderer GeckoLib (+ couche lumineuse), règles d'apparition, **œuf d'apparition** dans l'onglet créatif, traductions FR/EN.
1. **Dalek** (bronze, 2005) — agressif, tir **laser instantané** à distance (raycast + particules + dégâts), cri « Exterminer ! ». Skaro (fréquent), Surface (rare).
2. **Cyberman** — agressif, corps à corps, très résistant, « Supprimer ! ». Lune + Surface (rare).
3. **Ange Pleureur** — **ne bouge que si aucun joueur ne le regarde** (champ de vision + ligne de vue), pose « mains sur les yeux » / « attaque » ; son contact **téléporte le joueur loin** (« renvoyé dans le passé ») + cécité. Surface la nuit + Gallifrey.
4. **Ood** — pacifique, sphère de traduction lumineuse à la main, parle au clic droit (« Puis-je vous aider ? »). Gallifrey + Surface (rare).
5. **Adipose** — pacifique, petit, fait coucou. Surface.
- Sur les planètes, autoriser l'apparition **même de jour** (prédicat de `SpawnRestriction` personnalisé) ; en Surface, utiliser `BiomeModifications.addSpawn`.
- **Voix** : générer les répliques avec System.Speech (voix Hortense), effet robotique (modulation en anneau ~30 Hz pour les Daleks, métallique pour les Cybermen), convertir en `.ogg` avec FFmpeg, déclarer dans `sounds.json`.
6. **Sontarien** — guerrier clone trapu en armure bleue, agressif, fusil laser + corps à corps, crie « Sontar-ha ! ». Lune + Surface (rare).
7. **Le Silence** — grand, costume noir, tête bulbeuse ; quand le joueur **détourne le regard**, il l'oublie (effet : nausée + effacement de la cible), éclairs d'énergie à courte portée. Surface la nuit (rare).
8. **K9** — chien robot **apprivoisable** (comme un loup, avec de la redstone ?), suit le joueur, tire au laser sur les monstres, « Affirmatif, maître ». Œuf + recette possible.
9. **Zygon** — orange, couvert de ventouses, agressif ; peut **prendre l'apparence** d'un villageois (se déguise, se révèle quand on approche). Surface (rare).
10. **Slitheen** — grand extraterrestre vert ; se cache sous une « peau humaine » (apparence de villageois), se révèle quand attaqué, griffes. Surface (rare).
- Ordre conseillé : Dalek → Cyberman → Ange Pleureur → Ood → Adipose → K9 → Sontarien → Silence → Zygon → Slitheen.

### Étape B — Structures des planètes — ✅ FAITE (voir « Structures des planètes » plus haut)
- Vérifiée sur un serveur de test sans écran (dans le cloud) : les 3 structures se génèrent sans erreur, `/locate` les trouve (et pas dans l'Overworld), cages et coffres bien réglés, Ood présents, tables de butin valides. Rendus 3D lus directement dans les fichiers du monde → `apercu_structures.png`.
- **Pas encore vue en jeu par David** : à tester (aller sur chaque planète, `/locate structure doctorwho:dalek_city` / `citadel` / `moon_base`, combattre les gardiens, ouvrir les coffres, descendre dans la crypte de la Citadelle).
- Les mondes déjà créés n'ont les structures que dans les chunks **pas encore explorés** de chaque planète.

### Étape C — Finitions TARDIS — ✅ FAITE (voir « Fonctionnalités » plus haut)
- C1 choix du décor à la 1re entrée, C2 clé (jumelage, verrou, rappel au sol, localisation, copie), C3 plus d'avertissement expérimental, C4 sons synthétisés, C5 variantes (+3 extérieurs, +2 décors, +5 tournevis), C6 recettes (demande de David).
- Tout testé dans le vrai client (écran virtuel dans le cloud), **sauf les sons** (pas de carte son).

### Grosse mise à jour des planètes (demande de David) — ✅ FAITE
- Moins de bâtiments + 3 tailles avec d'autres mobs, villages, minerais par couches, grottes et puits de mine, golem de redstone et son repaire. Vérifié sur serveur de test (rendus, comptages) et dans le client virtuel (repaire, combat, barre de boss).

### Étape C bis — Restes
- ✅ Déguisement des Zygons et Slitheens en villageois (testé dans le client virtuel : apparence, approche, coup, espions de village).
- Retours de David après ses tests en jeu des créatures (plantages, designs ratés) : à traiter en priorité.
- Optionnel : appliquer aux blocs et au TARDIS la même passe de finition de textures que les créatures (`finish()` dans `generate_entities.py`).

### Étape D — Compatibilité et partage entre amis
- Vérifier avec Sodium/Iris + shaders dans le profil « david mod ».
- Tester à plusieurs (serveur ou LAN) : TARDIS partagé, panneau, voyages.
- Petite icône du mod (`assets/doctorwho/icon.png`) et passage en version 0.2.0. Pas de publication publique.

Prochaine étape : l'**étape D**, en demandant d'abord à David ses retours sur la grosse mise à jour des planètes (golem trop fort/faible ? fréquence des bâtiments ? espions) : à traiter en priorité.
