package fr.doctorwho.registry;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.block.ConsolePartBlock;
import fr.doctorwho.block.DecorBlock;
import fr.doctorwho.block.FacingDecorBlock;
import fr.doctorwho.block.TardisBlock;
import fr.doctorwho.block.TardisConsoleBlock;
import fr.doctorwho.block.TardisInteriorDoorBlock;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
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
    public static final List<String> ROUNDEL_FAMILIES = List.of("classic");
    private static final Map<String, Block> ROUNDELS = new HashMap<>();

    static {
        for (String family : ROUNDEL_FAMILIES) {
            ROUNDELS.put(family, decor(family + "_roundel", roundelBlock(0)));
            ROUNDELS.put(family + "_lit", decor(family + "_roundel_lit", roundelBlock(14)));
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

    /** Roundel en relief d'une famille ({@link #ROUNDEL_FAMILIES}), éteint ou allumé. */
    public static Block roundel(String family, boolean lit) {
        Block block = ROUNDELS.get(lit ? family + "_lit" : family);
        return block != null ? block : (lit ? WHITE_ROUNDEL_WALL_LIT : WHITE_ROUNDEL_WALL);
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
