package fr.doctorwho.tardis;

import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.tardis.interior.ConsoleRoom;
import fr.doctorwho.tardis.interior.InteriorThemes;
import fr.doctorwho.tardis.interior.RoomCanvas;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Salle de console du TARDIS : palettes de blocs de chaque décor (utilisées aussi par les couloirs
 * et les salles ajoutées) et point d'entrée de la construction.
 * <p>
 * La grande salle elle-même (estrade, galerie, escaliers, décor) est construite par les thèmes du
 * paquet {@code tardis.interior} : voir {@link InteriorThemes} et {@link fr.doctorwho.tardis.interior.InteriorTheme}.
 */
public final class InteriorBuilder {

    /** Version de l'agencement : augmentée quand la salle change, pour reconstruire les anciens intérieurs. */
    public static final int VERSION = 6;

    /**
     * Palette d'un décor de salle de console.
     *
     * @param lowerWall rangée basse des murs (ex. bibliothèques), ou null pour des roundels
     * @param strut     branche posée en haut des piliers, ou null
     * @param column    hautes colonnes le long des murs (décor 1963), ou null
     */
    public record Theme(Block wall, Block wallLit, Block base, Block floor, Block platform, Block subFloor,
                        Block pillar, @Nullable Block strut, @Nullable Block lowerWall, Block ceiling,
                        @Nullable Block column) {
    }

    /** Décors au choix (voir {@link Variants#DESKTOPS}). */
    public static Theme theme(String desktop) {
        return switch (Variants.desktop(desktop)) {
            // Onzième Docteur (2010) : cuivre et plancher de verre éclairé par en dessous
            case "copper" -> new Theme(ModBlocks.COPPER_ROUNDEL_WALL, ModBlocks.COPPER_ROUNDEL_WALL_LIT,
                    Blocks.WAXED_COPPER_BLOCK, Blocks.GLASS, Blocks.WAXED_COPPER_BLOCK, Blocks.WAXED_CUT_COPPER,
                    ModBlocks.COPPER_BEAM, null, null, Blocks.WAXED_CUT_COPPER, null);
            // Classique (années 60-70) : tout blanc, roundels lumineux
            case "white" -> new Theme(ModBlocks.WHITE_ROUNDEL_WALL, ModBlocks.WHITE_ROUNDEL_WALL_LIT,
                    Blocks.SMOOTH_QUARTZ, Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_BRICKS, Blocks.POLISHED_DEEPSLATE,
                    Blocks.QUARTZ_PILLAR, null, null, Blocks.SMOOTH_QUARTZ, null);
            // Douzième Docteur (2014) : sombre, bibliothèques au pied des murs
            case "dark" -> new Theme(ModBlocks.DARK_ROUNDEL_WALL, ModBlocks.DARK_ROUNDEL_WALL_LIT,
                    Blocks.POLISHED_BLACKSTONE_BRICKS, ModBlocks.METAL_GRATING, Blocks.POLISHED_BLACKSTONE,
                    Blocks.POLISHED_BLACKSTONE, Blocks.POLISHED_BASALT, null, Blocks.BOOKSHELF,
                    Blocks.POLISHED_BLACKSTONE_BRICKS, null);
            // Premier Docteur (1963) : blanc, sol gris, hautes colonnes tout autour
            case "columns" -> new Theme(ModBlocks.WHITE_ROUNDEL_WALL, ModBlocks.WHITE_ROUNDEL_WALL_LIT,
                    Blocks.SMOOTH_QUARTZ, Blocks.POLISHED_ANDESITE, Blocks.QUARTZ_BRICKS, Blocks.POLISHED_ANDESITE,
                    Blocks.QUARTZ_PILLAR, null, null, Blocks.SMOOTH_QUARTZ, Blocks.QUARTZ_PILLAR);
            // Huitième Docteur (1996) : boiseries sombres, bibliothèques, lumières chaudes
            case "victorian" -> new Theme(Blocks.DARK_OAK_PLANKS, Blocks.SHROOMLIGHT,
                    Blocks.STRIPPED_DARK_OAK_WOOD, Blocks.SPRUCE_PLANKS, Blocks.POLISHED_GRANITE, Blocks.DARK_OAK_PLANKS,
                    Blocks.DARK_OAK_LOG, null, Blocks.BOOKSHELF, Blocks.DARK_OAK_PLANKS, Blocks.DARK_OAK_LOG);
            // Dixième Docteur (2005) : corail
            default -> new Theme(ModBlocks.ROUNDEL_WALL, ModBlocks.ROUNDEL_WALL_LIT, ModBlocks.CORAL_WALL,
                    ModBlocks.METAL_GRATING, Blocks.WAXED_CUT_COPPER, Blocks.POLISHED_DEEPSLATE,
                    ModBlocks.CORAL_PILLAR, ModBlocks.CORAL_STRUT, null, ModBlocks.CORAL_WALL, null);
        };
    }

    private InteriorBuilder() {
    }

    /** Position de la moitié basse de la porte intérieure (dans le mur sud). */
    public static BlockPos doorPos(BlockPos center) {
        return ConsoleRoom.doorPos(center);
    }

    /** Position de la console (au centre, sur l'estrade). */
    public static BlockPos consolePos(BlockPos center) {
        return ConsoleRoom.consolePos(center);
    }

    /** (Re)construit entièrement la salle de console avec le décor demandé (voir {@link InteriorThemes}). */
    public static void build(ServerWorld world, BlockPos center, String desktop) {
        InteriorThemes.get(desktop).build(new RoomCanvas(world, center));
    }
}
