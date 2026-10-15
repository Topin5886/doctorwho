package fr.doctorwho.tardis;

import fr.doctorwho.block.AbstractTardisDoorBlock;
import fr.doctorwho.block.FacingDecorBlock;
import fr.doctorwho.block.TardisConsoleBlock;
import fr.doctorwho.registry.ModBlocks;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

/**
 * Construit la salle de console « corail » (Dixième Docteur), dans l'esprit du
 * Dalek Mod : salle ronde, plancher en grille éclairé par en dessous, murs à
 * roundels, piliers et branches de corail, appliques, rambarde autour de la
 * console et porte intérieure encadrée de poutres de cuivre (mur sud).
 * <p>
 * Les autres intérieurs au choix arriveront avec l'écran de sélection.
 */
public final class InteriorBuilder {

    /** Version de l'agencement : augmentée quand la salle change, pour reconstruire les anciens intérieurs. */
    public static final int VERSION = 5;

    /** Rayon intérieur de la salle (le mur est juste au-delà). */
    private static final double ROOM_RADIUS = 9.5;
    private static final double WALL_RADIUS = 10.7;
    /** Hauteur du plafond au-dessus du sol. */
    private static final int H = 10;
    /** Zone nettoyée avant de (re)construire. */
    private static final int CLEAR = 12;

    private static final BlockState BEYOND_DOOR = Blocks.SEA_LANTERN.getDefaultState();
    private static final BlockState AIR = Blocks.AIR.getDefaultState();

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
        return center.add(0, 1, 10);
    }

    /** Position de la console (centre de la salle, posée sur le sol). */
    public static BlockPos consolePos(BlockPos center) {
        return center.up();
    }

    /** (Re)construit entièrement la salle de console avec le décor demandé. */
    public static void build(ServerWorld world, BlockPos center, String desktop) {
        Theme theme = theme(desktop);
        BlockState grating = theme.floor().getDefaultState();
        BlockState hexLight = ModBlocks.HEX_LIGHT.getDefaultState();
        BlockState coral = theme.base().getDefaultState();
        BlockState ceiling = theme.ceiling().getDefaultState();
        BlockState roundel = theme.wall().getDefaultState();
        BlockState roundelLit = theme.wallLit().getDefaultState();
        BlockState subFloor = theme.subFloor().getDefaultState();
        BlockState platform = theme.platform().getDefaultState();

        for (int dx = -CLEAR; dx <= CLEAR; dx++) {
            for (int dz = -CLEAR; dz <= CLEAR; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                for (int dy = -1; dy <= H + 1; dy++) {
                    BlockState state = AIR;
                    if (r <= ROOM_RADIUS) {
                        if (dy == -1) {
                            // Sous le plancher : lumières hexagonales qui éclairent à travers la grille
                            state = Math.floorMod(dx, 3) == 0 && Math.floorMod(dz, 3) == 0 ? hexLight : subFloor;
                        } else if (dy == 0) {
                            state = r <= 3.5 ? platform : grating;
                        } else if (dy == H) {
                            state = Math.floorMod(dx, 4) == 0 && Math.floorMod(dz, 4) == 0 && r < 8 ? hexLight : ceiling;
                        }
                    } else if (r <= WALL_RADIUS && dy >= -1 && dy <= H) {
                        if (dy <= 0 || dy == H) {
                            state = coral;
                        } else if (dy == 1 && theme.lowerWall() != null) {
                            state = theme.lowerWall().getDefaultState();
                        } else {
                            boolean lit = (dy == 2 || dy == 6) && (Math.abs(dx) + Math.abs(dz)) % 3 == 0;
                            state = lit ? roundelLit : roundel;
                        }
                    }
                    set(world, center, dx, dy, dz, state);
                }
            }
        }

        placePillars(world, center, theme);
        placeColumns(world, center, theme);
        placeLamps(world, center);
        placeRailing(world, center);
        TardisConsoleBlock.place(world, consolePos(center), ConsoleStyle.forDesktop(desktop));
        placeDoor(world, center);
    }

    /** Quatre piliers (de corail pour le décor 2005), chacun couronné d'une branche qui penche vers la console. */
    private static void placePillars(ServerWorld world, BlockPos center, Theme theme) {
        BlockState pillar = theme.pillar().getDefaultState();
        // En diagonale, pour laisser libres la porte (sud) et les trois sorties (nord, est, ouest)
        for (int k = 0; k < 4; k++) {
            double angle = Math.toRadians(45 + k * 90);
            int px = (int) Math.round(Math.cos(angle) * 7);
            int pz = (int) Math.round(Math.sin(angle) * 7);
            for (int dy = 1; dy < H; dy++) {
                set(world, center, px, dy, pz, pillar);
            }
            if (theme.strut() != null) {
                Direction towardCenter = Direction.getFacing(-px, 0, -pz);
                BlockPos strut = center.add(px, H - 1, pz).offset(towardCenter);
                set(world, strut, theme.strut().getDefaultState().with(FacingDecorBlock.FACING, towardCenter));
            }
        }
    }

    /** Hautes colonnes le long des murs, par paires de part et d'autre des diagonales (décors 1963 et 1996). */
    private static void placeColumns(ServerWorld world, BlockPos center, Theme theme) {
        if (theme.column() == null) {
            return;
        }
        BlockState column = theme.column().getDefaultState();
        for (int k = 0; k < 8; k++) {
            double angle = Math.toRadians(30 + (k / 2) * 90 + (k % 2) * 30);
            int cx = (int) Math.round(Math.cos(angle) * 9.2);
            int cz = (int) Math.round(Math.sin(angle) * 9.2);
            for (int dy = 1; dy < H; dy++) {
                set(world, center, cx, dy, cz, column);
            }
        }
    }

    /** Appliques au mur, de part et d'autre de la porte et des sorties. */
    private static void placeLamps(ServerWorld world, BlockPos center) {
        for (int k = 0; k < 8; k++) {
            double angle = Math.toRadians(22.5 + k * 45);
            int lx = (int) Math.round(Math.cos(angle) * 9);
            int lz = (int) Math.round(Math.sin(angle) * 9);
            Direction towardCenter = Direction.getFacing(-lx, 0, -lz);
            set(world, center, lx, 4, lz, ModBlocks.WALL_LAMP.getDefaultState().with(FacingDecorBlock.FACING, towardCenter));
        }
    }

    /** Rambarde en anneau autour de la console, avec quatre passages. */
    private static void placeRailing(ServerWorld world, BlockPos center) {
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                double r = Math.sqrt(dx * dx + dz * dz);
                if (r < 5.6 || r > 6.4 || Math.abs(dx) <= 1 || Math.abs(dz) <= 1) {
                    continue;
                }
                // La barre suit l'anneau : est-ouest au nord et au sud, nord-sud à l'est et à l'ouest
                Direction facing = Math.abs(dz) >= Math.abs(dx) ? Direction.NORTH : Direction.EAST;
                set(world, center, dx, 1, dz, ModBlocks.RAILING.getDefaultState().with(FacingDecorBlock.FACING, facing));
            }
        }
    }

    /**
     * Porte intérieure dans le mur sud, encadrée de poutres de cuivre, avec une
     * niche lumineuse derrière (les portes s'y ouvrent sans traverser le mur).
     */
    private static void placeDoor(ServerWorld world, BlockPos center) {
        BlockPos door = doorPos(center);
        BlockState wall = ModBlocks.ROUNDEL_WALL.getDefaultState();
        BlockState beam = ModBlocks.COPPER_BEAM.getDefaultState();
        for (int dy = 0; dy <= 1; dy++) {
            set(world, door.south().up(dy), AIR);
            set(world, door.south().east().up(dy), wall);
            set(world, door.south().west().up(dy), wall);
            set(world, door.south(2).up(dy), BEYOND_DOOR);
        }
        set(world, door.south().down(), ModBlocks.METAL_GRATING.getDefaultState());
        set(world, door.south().up(2), wall);
        for (int dy = 0; dy < H - 1; dy++) {
            set(world, door.north().east().up(dy), beam);
            set(world, door.north().west().up(dy), beam);
        }
        BlockState doorState = ModBlocks.TARDIS_INTERIOR_DOOR.getDefaultState()
                .with(AbstractTardisDoorBlock.FACING, Direction.NORTH);
        set(world, door, doorState.with(AbstractTardisDoorBlock.HALF, DoubleBlockHalf.LOWER));
        set(world, door.up(), doorState.with(AbstractTardisDoorBlock.HALF, DoubleBlockHalf.UPPER));
    }

    private static void set(ServerWorld world, BlockPos center, int dx, int dy, int dz, BlockState state) {
        set(world, center.add(dx, dy, dz), state);
    }

    private static void set(ServerWorld world, BlockPos pos, BlockState state) {
        // Pas de mises à jour de voisins pendant la construction (blocs posés avant leur support, etc.)
        world.setBlockState(pos, state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
    }
}
