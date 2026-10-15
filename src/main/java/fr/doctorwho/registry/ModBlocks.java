package fr.doctorwho.registry;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.block.ConsolePartBlock;
import fr.doctorwho.block.DecorBlock;
import fr.doctorwho.block.FacingDecorBlock;
import fr.doctorwho.block.HandrailBlock;
import fr.doctorwho.block.LightColumnBlock;
import fr.doctorwho.block.SeatBlock;
import fr.doctorwho.block.TardisBlock;
import fr.doctorwho.block.TardisConsoleBlock;
import fr.doctorwho.block.TardisInteriorDoorBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.GlassBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.block.MapColor;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.BlockSoundGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModBlocks {

    public static final Block TARDIS = register("tardis", new TardisBlock(FabricBlockSettings.create()
            .mapColor(MapColor.BLUE)
            .strength(4.0f, 3600000.0f) // résiste à toutes les explosions
            .sounds(BlockSoundGroup.WOOD)
            .nonOpaque()
            .pistonBehavior(PistonBehavior.BLOCK)
            // La lampe sur le toit éclaire les alentours
            .luminance(state -> state.get(TardisBlock.HALF) == DoubleBlockHalf.UPPER ? 10 : 0)));

    /** Porte intérieure de la salle de console : générée, incassable, sans objet. */
    public static final Block TARDIS_INTERIOR_DOOR = register("tardis_interior_door", new TardisInteriorDoorBlock(
            FabricBlockSettings.create()
                    .mapColor(MapColor.BLUE)
                    .strength(-1.0f, 3600000.0f)
                    .sounds(BlockSoundGroup.WOOD)
                    .nonOpaque()
                    .dropsNothing()
                    .pistonBehavior(PistonBehavior.BLOCK)));

    /** Console de la salle de commande : générée avec l'intérieur, incassable. */
    public static final Block TARDIS_CONSOLE = register("tardis_console", new TardisConsoleBlock(FabricBlockSettings.create()
            .mapColor(MapColor.ORANGE)
            .strength(-1.0f, 3600000.0f)
            .sounds(BlockSoundGroup.COPPER)
            .nonOpaque()
            .dropsNothing()
            .luminance(state -> 12)
            .pistonBehavior(PistonBehavior.BLOCK)));

    /** Blocs invisibles qui donnent sa collision au modèle de la console. */
    public static final Block CONSOLE_PART = register("console_part", new ConsolePartBlock(FabricBlockSettings.create()
            .strength(-1.0f, 3600000.0f)
            .sounds(BlockSoundGroup.COPPER)
            .nonOpaque()
            .dropsNothing()
            .pistonBehavior(PistonBehavior.BLOCK)));

    // --- Décor de la salle de console (style Dalek Mod) -------------------------------
    // Ces blocs ont aussi un objet : ils apparaissent dans l'onglet créatif (liste DECORATION).

    public static final List<Block> DECORATION = new ArrayList<>();
    public static final List<Block> PLANET_BLOCKS = new ArrayList<>();

    public static final Block ROUNDEL_WALL = decor("roundel_wall", new Block(stone(MapColor.ORANGE)));
    public static final Block ROUNDEL_WALL_LIT = decor("roundel_wall_lit", new Block(stone(MapColor.ORANGE).luminance(state -> 15)));
    public static final Block CORAL_WALL = decor("coral_wall", new Block(stone(MapColor.ORANGE)));
    public static final Block METAL_GRATING = decor("metal_grating", new Block(metal().nonOpaque()));
    public static final Block HEX_LIGHT = decor("hex_light", new Block(metal().luminance(state -> 15)));
    public static final Block CORAL_PILLAR = decor("coral_pillar", new DecorBlock(stone(MapColor.ORANGE).nonOpaque(),
            Block.createCuboidShape(4, 0, 4, 12, 16, 12)));
    public static final Block CORAL_STRUT = decor("coral_strut", new FacingDecorBlock(stone(MapColor.ORANGE).nonOpaque().noCollision(),
            Block.createCuboidShape(4, 0, 2, 12, 16, 16), false));
    public static final Block COPPER_BEAM = decor("copper_beam", new DecorBlock(metal().nonOpaque(),
            Block.createCuboidShape(5, 0, 5, 11, 16, 11)));
    public static final Block WALL_LAMP = decor("wall_lamp", new FacingDecorBlock(metal().nonOpaque().noCollision().luminance(state -> 14),
            Block.createCuboidShape(5, 3, 8.5, 11, 13, 16), true));
    public static final Block RAILING = decor("railing", new FacingDecorBlock(metal().nonOpaque(),
            Block.createCuboidShape(0, 0, 6.5, 16, 16, 9.5), false));

    // Murs à roundels des autres décors d'intérieur (blanc classique, sombre, cuivre)
    public static final Block WHITE_ROUNDEL_WALL = decor("white_roundel_wall", new Block(stone(MapColor.WHITE)));
    public static final Block WHITE_ROUNDEL_WALL_LIT = decor("white_roundel_wall_lit", new Block(stone(MapColor.WHITE).luminance(state -> 15)));
    public static final Block DARK_ROUNDEL_WALL = decor("dark_roundel_wall", new Block(stone(MapColor.GRAY)));
    public static final Block DARK_ROUNDEL_WALL_LIT = decor("dark_roundel_wall_lit", new Block(stone(MapColor.GRAY).luminance(state -> 15)));
    public static final Block COPPER_ROUNDEL_WALL = decor("copper_roundel_wall", new Block(stone(MapColor.ORANGE)));
    public static final Block COPPER_ROUNDEL_WALL_LIT = decor("copper_roundel_wall_lit", new Block(stone(MapColor.ORANGE).luminance(state -> 15)));

    // --- Grandes salles de console (thèmes d'intérieur) ---------------------------------

    /**
     * Familles de roundels en relief (disque creusé, éteint ou allumé), une par thème.
     * Pour un nouveau thème : ajouter son nom ici et dans ROUNDEL_FAMILIES de tools/generate_blocks.py.
     */
    public static final List<String> ROUNDEL_FAMILIES = List.of("classic", "copper", "amber", "dark", "cathedral");
    private static final Map<String, Block> ROUNDELS = new HashMap<>();
    /** Grilles de sol ajourées, une par thème ({@code <famille>_floor_grate}). */
    public static final List<String> GRATE_FAMILIES = List.of("copper", "amber", "dark");
    private static final Map<String, Block> GRATES = new HashMap<>();
    /** Lumières de sous-plancher colorées ({@code <couleur>_underlight}, niveau 15). */
    public static final List<String> UNDERLIGHT_COLORS = List.of("green", "orange", "cyan");
    private static final Map<String, Block> UNDERLIGHTS = new HashMap<>();

    static {
        for (String family : ROUNDEL_FAMILIES) {
            ROUNDELS.put(family, decor(family + "_roundel", roundelBlock(0)));
            ROUNDELS.put(family + "_lit", decor(family + "_roundel_lit", roundelBlock(14)));
        }
        for (String family : GRATE_FAMILIES) {
            GRATES.put(family, decor(family + "_floor_grate", new Block(metal().nonOpaque())));
        }
        for (String color : UNDERLIGHT_COLORS) {
            UNDERLIGHTS.put(color, decor(color + "_underlight", new Block(metal().luminance(state -> 15))));
        }
    }

    /** Pilastre cannelé des murs du décor classique. */
    public static final Block CLASSIC_PILASTER = decor("classic_pilaster", new Block(stone(MapColor.WHITE)));
    /** Panneau lumineux (plafonds). */
    public static final Block LIGHT_PANEL = decor("light_panel", new Block(stone(MapColor.WHITE).luminance(state -> 15)));
    /** Écran du scanner (galaxie), accroché au mur. */
    public static final Block SCANNER = decor("scanner", new FacingDecorBlock(metal().nonOpaque().luminance(state -> 7),
            Block.createCuboidShape(0, 1, 13, 16, 15, 16), true));
    /** Porte-manteau (≈ 2 blocs de haut), comme dans la salle classique. */
    public static final Block HAT_STAND = decor("hat_stand", new DecorBlock(FabricBlockSettings.create()
            .mapColor(MapColor.BROWN).strength(1.0f).sounds(BlockSoundGroup.WOOD).nonOpaque(),
            Block.createCuboidShape(6, 0, 6, 10, 16, 10)));

    // Cuivre (2010)
    /** Plaque de cuivre rivetée (bandeaux des murs). */
    public static final Block COPPER_WALL_PANEL = decor("copper_wall_panel", new Block(metal().mapColor(MapColor.ORANGE)));
    /** Entretoise de cuivre en biais (même modèle que la branche de corail). */
    public static final Block COPPER_STRUT = decor("copper_strut", new FacingDecorBlock(metal().nonOpaque().noCollision(),
            Block.createCuboidShape(4, 0, 2, 12, 16, 16), false));
    /** Dalle de verre cerclée de cuivre : on voit les lumières sous le plancher. */
    public static final Block COPPER_GLASS_FLOOR = decor("copper_glass_floor", new GlassBlock(FabricBlockSettings.copyOf(Blocks.GLASS)
            .strength(2.0f, 6.0f).mapColor(MapColor.ORANGE)));

    // Ambre organique (2005, War Doctor)
    /** Matière organique ambrée (grandes arches, plafond). */
    public static final Block AMBER_CORAL = decor("amber_coral", new Block(stone(MapColor.ORANGE)));
    /** Pilier ambré (même modèle que le pilier de corail). */
    public static final Block AMBER_PILLAR = decor("amber_pillar", new DecorBlock(stone(MapColor.ORANGE).nonOpaque(),
            Block.createCuboidShape(4, 0, 4, 12, 16, 12)));
    /** Branche ambrée en biais (même modèle que la branche de corail). */
    public static final Block AMBER_STRUT = decor("amber_strut", new FacingDecorBlock(stone(MapColor.ORANGE).nonOpaque().noCollision(),
            Block.createCuboidShape(4, 0, 2, 12, 16, 16), false));
    /** Alvéole hexagonale bleue lumineuse. */
    public static final Block BLUE_HEX_LIGHT = decor("blue_hex_light", new Block(metal().luminance(state -> 14)));

    // Sombre industriel (2014) et colonnes de lumière
    /** Plaque d'acier rivetée. */
    public static final Block DARK_WALL_PANEL = decor("dark_wall_panel", new Block(metal().mapColor(MapColor.GRAY)));
    /** Rambarde de laiton, raccordée à ses voisines comme des barreaux. */
    public static final Block BRASS_HANDRAIL = decor("brass_handrail", new HandrailBlock(FabricBlockSettings.copyOf(Blocks.IRON_BARS)
            .mapColor(MapColor.GOLD)));
    /** Colonne de lumière animée (orange, cyan ou bleue), empilable. */
    public static final Block LIGHT_COLUMN = decor("light_column", new LightColumnBlock(metal().nonOpaque().luminance(state -> 15)));

    // Bleu cathédrale (1996)
    /** Poutre d'acier bleu ajourée, orientable comme une bûche (arches gothiques). */
    public static final Block CATHEDRAL_TRUSS = decor("cathedral_truss", new PillarBlock(metal().mapColor(MapColor.BLUE).nonOpaque()));
    /** Chandelier de laiton à trois bougies. */
    public static final Block CANDELABRA = decor("candelabra", new DecorBlock(FabricBlockSettings.create().mapColor(MapColor.GOLD)
            .strength(1.0f).sounds(BlockSoundGroup.METAL).nonOpaque().luminance(state -> 14),
            Block.createCuboidShape(3, 0, 5, 13, 16, 11)));

    // Turquoise tech (2012)
    /** Panneau hexagonal en relief, éteint ou allumé en cyan. */
    public static final Block HEX_WALL_PANEL = decor("hex_wall_panel", new FacingDecorBlock(stone(MapColor.WHITE).nonOpaque(),
            Block.createCuboidShape(0, 0, 0, 16, 16, 16), false));
    public static final Block HEX_WALL_PANEL_LIT = decor("hex_wall_panel_lit", new FacingDecorBlock(stone(MapColor.CYAN).nonOpaque()
            .luminance(state -> 13), Block.createCuboidShape(0, 0, 0, 16, 16, 16), false));
    /** Panneau blanc technologique. */
    public static final Block WHITE_TECH_PANEL = decor("white_tech_panel", new Block(stone(MapColor.WHITE)));

    // Mobilier (tous thèmes)
    public static final Block LEATHER_SEAT = decor("leather_seat", seat(MapColor.BROWN));
    public static final Block WHITE_SEAT = decor("white_seat", seat(MapColor.WHITE));
    public static final Block WOODEN_SEAT = decor("wooden_seat", seat(MapColor.OAK_TAN));
    /** Horloge murale au cadran animé. */
    public static final Block WALL_CLOCK = decor("wall_clock", new FacingDecorBlock(metal().nonOpaque().noCollision(),
            Block.createCuboidShape(2, 2, 15, 14, 14, 16), true));
    /** Tuyau de cuivre et câble noir, orientables comme une bûche. */
    public static final Block COPPER_PIPE = decor("copper_pipe", new PillarBlock(metal().nonOpaque()));
    public static final Block BLACK_CABLE = decor("black_cable", new PillarBlock(metal().nonOpaque().mapColor(MapColor.BLACK)));
    /** Télescope de laiton et globe terrestre (salle de 1996). */
    public static final Block TELESCOPE = decor("telescope", new FacingDecorBlock(metal().nonOpaque(),
            Block.createCuboidShape(3, 0, 1, 13, 16, 15), false));
    public static final Block GLOBE = decor("globe", new DecorBlock(metal().nonOpaque(),
            Block.createCuboidShape(3, 0, 3, 13, 14, 13)));

    /** Grille de sol d'une famille ({@link #GRATE_FAMILIES}). */
    public static Block floorGrate(String family) {
        return GRATES.getOrDefault(family, METAL_GRATING);
    }

    /** Lumière de sous-plancher d'une couleur ({@link #UNDERLIGHT_COLORS}). */
    public static Block underlight(String color) {
        return UNDERLIGHTS.getOrDefault(color, HEX_LIGHT);
    }

    /** Roundel en relief d'une famille ({@link #ROUNDEL_FAMILIES}), éteint ou allumé. */
    public static Block roundel(String family, boolean lit) {
        Block block = ROUNDELS.get(lit ? family + "_lit" : family);
        return block != null ? block : (lit ? WHITE_ROUNDEL_WALL_LIT : WHITE_ROUNDEL_WALL);
    }

    private static Block seat(MapColor color) {
        return new SeatBlock(FabricBlockSettings.create().mapColor(color).strength(1.0f).sounds(BlockSoundGroup.WOOD).nonOpaque(),
                Block.createCuboidShape(0, 0, 0, 16, 9, 16));
    }

    /** Bloc plein orientable dont une face porte le disque ; non opaque pour que la lumière éclaire le creux. */
    private static Block roundelBlock(int light) {
        return new FacingDecorBlock(stone(MapColor.WHITE).nonOpaque().luminance(state -> light),
                Block.createCuboidShape(0, 0, 0, 16, 16, 16), false);
    }

    // --- Sols des planètes -------------------------------------------------------------

    public static final Block MOON_DUST = planet("moon_dust", new Block(FabricBlockSettings.create()
            .mapColor(MapColor.LIGHT_GRAY).strength(0.5f).sounds(BlockSoundGroup.SAND)));
    public static final Block MOON_ROCK = planet("moon_rock", new Block(FabricBlockSettings.create()
            .mapColor(MapColor.GRAY).strength(1.5f, 6.0f).requiresTool().sounds(BlockSoundGroup.STONE)));
    public static final Block SKARO_SAND = planet("skaro_sand", new Block(FabricBlockSettings.create()
            .mapColor(MapColor.ORANGE).strength(0.5f).sounds(BlockSoundGroup.SAND)));
    public static final Block SKARO_ROCK = planet("skaro_rock", new Block(FabricBlockSettings.create()
            .mapColor(MapColor.TERRACOTTA_RED).strength(1.5f, 6.0f).requiresTool().sounds(BlockSoundGroup.STONE)));
    public static final Block GALLIFREY_GRASS = planet("gallifrey_grass", new Block(FabricBlockSettings.create()
            .mapColor(MapColor.RED).strength(0.6f).sounds(BlockSoundGroup.GRASS)));
    /** Arbres de Gallifrey : tronc pâle et feuillage argenté. */
    public static final Block GALLIFREY_LOG = planet("gallifrey_log", new PillarBlock(FabricBlockSettings.copyOf(Blocks.BIRCH_LOG)
            .mapColor(MapColor.LIGHT_GRAY)));
    public static final Block GALLIFREY_LEAVES = planet("gallifrey_leaves", new LeavesBlock(FabricBlockSettings.copyOf(Blocks.BIRCH_LEAVES)
            .mapColor(MapColor.WHITE_GRAY)));

    private ModBlocks() {
    }

    private static Block register(String name, Block block) {
        return Registry.register(Registries.BLOCK, DoctorWhoMod.id(name), block);
    }

    private static Block decor(String name, Block block) {
        DECORATION.add(withItem(name, block));
        return block;
    }

    private static Block planet(String name, Block block) {
        PLANET_BLOCKS.add(withItem(name, block));
        return block;
    }

    private static Block withItem(String name, Block block) {
        register(name, block);
        Registry.register(Registries.ITEM, DoctorWhoMod.id(name), new BlockItem(block, new FabricItemSettings()));
        return block;
    }

    private static FabricBlockSettings stone(MapColor color) {
        return FabricBlockSettings.create().mapColor(color).strength(2.0f, 6.0f).sounds(BlockSoundGroup.STONE);
    }

    private static FabricBlockSettings metal() {
        return FabricBlockSettings.create().mapColor(MapColor.IRON_GRAY).strength(3.0f, 6.0f).sounds(BlockSoundGroup.COPPER);
    }

    public static void register() {
        // Charge la classe : les blocs sont enregistrés par les champs statiques
    }
}
