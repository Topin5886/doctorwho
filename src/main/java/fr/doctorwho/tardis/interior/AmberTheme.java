package fr.doctorwho.tardis.interior;

import fr.doctorwho.block.FacingDecorBlock;
import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.tardis.InteriorBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.math.Direction;

import static fr.doctorwho.tardis.interior.ConsoleRoom.*;

/**
 * Décor « Ambre organique » (Dixième Docteur 2005 et War Doctor) : un TARDIS « qui a poussé ».
 * <ul>
 *     <li>six grandes <b>arches organiques</b> ambrées qui partent du sol et se rejoignent au-dessus de la console ;</li>
 *     <li>murs de roundels ambrés, alvéoles bleues lumineuses dans les murs et au plafond ;</li>
 *     <li>sol en grille ambrée éclairé en vert par en dessous, piliers et branches de corail ambré le long des murs ;</li>
 *     <li>console de corail à colonne verte.</li>
 * </ul>
 */
public class AmberTheme extends InteriorTheme {

    /** Angles des arches (degrés), symétriques : loin des sorties (0°, 180°, 270°) et de l'entrée (90°) dégagée. */
    private static final double[] ARCHES = {30, 150, 210, 250, 290, 330};
    /** Rayon du pied des arches et de leur sommet (près de la colonne). */
    private static final double ARCH_FOOT = 11.5;
    private static final double ARCH_TOP = 3.5;

    private static final BlockState AMBER = ModBlocks.AMBER_CORAL.getDefaultState();
    private static final BlockState GRATE = ModBlocks.floorGrate("amber").getDefaultState();
    private static final BlockState GREEN = ModBlocks.underlight("green").getDefaultState();
    private static final BlockState BLUE = ModBlocks.BLUE_HEX_LIGHT.getDefaultState();
    private static final BlockState DARK = Blocks.POLISHED_DEEPSLATE.getDefaultState();

    @Override
    public String id() {
        return "coral";
    }

    @Override
    public InteriorBuilder.Theme palette() {
        return InteriorBuilder.theme("coral");
    }

    @Override
    protected BlockState stairs() {
        return Blocks.SMOOTH_RED_SANDSTONE_STAIRS.getDefaultState();
    }

    @Override
    protected BlockState railing() {
        return ModBlocks.RAILING.getDefaultState();
    }


    @Override
    protected int ambientLevel() {
        return 10; // ambiance chaude et tamisée
    }

    // --- Murs, sol, plafond --------------------------------------------------------------------

    @Override
    protected BlockState wallBlock(int x, int y, int z) {
        if (y == GALLERY_Y || y == HEIGHT - 1) {
            return AMBER;
        }
        if (Math.floorMod(x * 3 + z * 7 + y * 5, 19) == 0) {
            return BLUE; // alvéoles bleues éparses
        }
        boolean lit = Math.floorMod(x + z + 2 * y, 5) == 0;
        return ModBlocks.roundel("amber", lit).getDefaultState()
                .with(FacingDecorBlock.FACING, RoomCanvas.towardCenter(x, z));
    }

    @Override
    protected BlockState outerWall() {
        return AMBER;
    }

    @Override
    protected BlockState floorBlock(int x, int z) {
        return GRATE;
    }

    @Override
    protected BlockState subFloorBlock(int x, int z) {
        return Math.floorMod(x, 3) == 0 && Math.floorMod(z, 3) == 0 ? GREEN : DARK;
    }

    @Override
    protected BlockState ceilingBlock(int x, int z) {
        // Nid d'alvéoles bleues (pavage hexagonal approximé) dans l'ambre
        int row = Math.floorMod(z, 4);
        int col = Math.floorMod(x + (Math.floorMod(z / 4, 2) == 0 ? 0 : 2), 4);
        return row == 0 && col == 0 ? BLUE : AMBER;
    }

    @Override
    protected BlockState daisTop(int x, int z) {
        return RoomCanvas.radius(x, z) > 5.5 ? AMBER : GRATE;
    }

    @Override
    protected BlockState galleryFloor(int x, int z) {
        return GRATE;
    }

    // --- Décor : arches organiques, piliers et branches --------------------------------------------

    @Override
    protected void decorate(RoomCanvas c) {
        for (double angle : ARCHES) {
            arch(c, Math.toRadians(angle));
        }
        int max = (int) Math.ceil(RADIUS);
        for (int x = -max; x <= max; x++) {
            for (int z = -max; z <= max; z++) {
                double r = RoomCanvas.radius(x, z);
                if (r <= RADIUS - 1 || r > RADIUS) {
                    continue;
                }
                double a = RoomCanvas.angle(x, z);
                Direction toward = RoomCanvas.towardCenter(x, z);
                // Branches ambrées qui sortent du haut des murs, tous les 30°
                if (Math.toRadians(Math.abs(((a + 15) % 30) - 15)) * r < 0.5) {
                    c.set(x, HEIGHT - 2, z, ModBlocks.AMBER_STRUT.getDefaultState().with(FacingDecorBlock.FACING, toward));
                    // Piliers ambrés au pied des murs, sous la galerie (hors sorties et escaliers)
                    boolean free = !ConsoleRoom.onStairs(x, z) && Math.abs(x) > 2 && Math.abs(z) > 2;
                    if (free) {
                        for (int y = 1; y < GALLERY_Y; y++) {
                            c.set(x, y, z, ModBlocks.AMBER_PILLAR.getDefaultState());
                        }
                    }
                }
            }
        }
        // Deux sièges face à la console, de part et d'autre de l'allée de la porte
        for (int z = 5; z <= 7; z++) {
            seat(c, 8, 1, z, ModBlocks.WOODEN_SEAT, Direction.WEST);
            seat(c, -8, 1, z, ModBlocks.WOODEN_SEAT, Direction.EAST);
        }
    }

    /**
     * Une arche : elle monte presque droite depuis le sol, puis se courbe vers la colonne
     * et rejoint le plafond. Épaisse de deux blocs en bas, d'un bloc en haut.
     */
    private void arch(RoomCanvas c, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        for (int i = 0; i <= 200; i++) {
            double t = i / 200.0;
            double r = ARCH_FOOT - (ARCH_FOOT - ARCH_TOP) * (1 - Math.cos(t * Math.PI / 2));
            int y = 1 + (int) Math.round((HEIGHT - 2) * Math.sin(t * Math.PI / 2));
            double thickness = t < 0.5 ? 0.9 : 0.45;
            for (double dr = -thickness; dr <= thickness; dr += 0.45) {
                int x = (int) Math.round((r + dr) * cos);
                int z = (int) Math.round((r + dr) * sin);
                c.set(x, y, z, AMBER);
                // Une alvéole bleue tous les quelques blocs, côté console
                if (dr < -0.4 && i % 40 == 20) {
                    c.set(x, y, z, BLUE);
                }
            }
        }
    }
}
