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
 * Décor « Cuivre » (Onzième Docteur, 2010) : tout en cuivre et en orangé.
 * <ul>
 *     <li>murs de roundels de cuivre en relief, allumés en orange en diagonale, bandeaux de plaques rivetées ;</li>
 *     <li>autour de la console, un <b>sol de verre</b> au-dessus d'une fosse éclairée en <b>vert</b> ;
 *     ailleurs, grilles de cuivre ajourées ;</li>
 *     <li>quatre jambes de cuivre coiffées d'entretoises, rambardes de laiton, plafond aux lumières orangées ;</li>
 *     <li>banquettes, bibliothèques sous la galerie, porte-manteau, scanner ; console de cuivre (colonne jusqu'au plafond).</li>
 * </ul>
 */
public class CopperTheme extends InteriorTheme {

    /** Rayon du sol de verre autour de l'estrade. */
    private static final double GLASS = 11.5;

    private static final BlockState COPPER = Blocks.WAXED_CUT_COPPER.getDefaultState();
    private static final BlockState COPPER_BLOCK = Blocks.WAXED_COPPER_BLOCK.getDefaultState();
    private static final BlockState PANEL = ModBlocks.COPPER_WALL_PANEL.getDefaultState();
    private static final BlockState GRATE = ModBlocks.floorGrate("copper").getDefaultState();
    private static final BlockState GLASS_FLOOR = ModBlocks.COPPER_GLASS_FLOOR.getDefaultState();
    private static final BlockState GREEN = ModBlocks.underlight("green").getDefaultState();
    private static final BlockState ORANGE_LIGHT = Blocks.SHROOMLIGHT.getDefaultState();
    private static final BlockState DARK = Blocks.POLISHED_DEEPSLATE.getDefaultState();

    @Override
    public String id() {
        return "copper";
    }

    @Override
    public InteriorBuilder.Theme palette() {
        return InteriorBuilder.theme("copper");
    }

    @Override
    protected BlockState stairs() {
        return Blocks.WAXED_CUT_COPPER_STAIRS.getDefaultState();
    }

    @Override
    protected BlockState railing() {
        return ModBlocks.RAILING.getDefaultState();
    }

    /** Rambarde de laiton : sa barre suit l'anneau (est-ouest au nord et au sud, nord-sud ailleurs). */
    @Override
    protected void placeRailing(RoomCanvas c, int x, int y, int z) {
        Direction facing = Math.abs(z) >= Math.abs(x) ? Direction.NORTH : Direction.EAST;
        c.set(x, y, z, railing().with(FacingDecorBlock.FACING, facing));
    }

    // --- Murs ---------------------------------------------------------------------------------

    @Override
    protected BlockState wallBlock(int x, int y, int z) {
        if (y == GALLERY_Y || y == HEIGHT - 1 || y == 1) {
            return PANEL; // plinthe, bandeau de la galerie, corniche
        }
        boolean lit = Math.floorMod(x + 2 * y + z, 4) == 0;
        return ModBlocks.roundel("copper", lit).getDefaultState()
                .with(FacingDecorBlock.FACING, RoomCanvas.towardCenter(x, z));
    }

    @Override
    protected BlockState outerWall() {
        return COPPER;
    }

    // --- Sol de verre au-dessus d'une fosse verte, grilles ailleurs -------------------------------

    @Override
    protected BlockState floorBlock(int x, int z) {
        return RoomCanvas.radius(x, z) <= GLASS ? GLASS_FLOOR : GRATE;
    }

    @Override
    protected BlockState subFloorBlock(int x, int z) {
        if (RoomCanvas.radius(x, z) <= GLASS) {
            return AIR; // la fosse sous le verre
        }
        return Math.floorMod(x, 3) == 0 && Math.floorMod(z, 3) == 0 ? GREEN : DARK;
    }

    @Override
    protected BlockState pitBlock(int x, int y, int z) {
        if (y == -2 && RoomCanvas.radius(x, z) <= GLASS) {
            // Fond de la fosse : damier de lumières vertes et de cuivre, poutres en croix
            if (x == 0 || z == 0) {
                return COPPER_BLOCK;
            }
            return Math.floorMod(x + z, 2) == 0 ? GREEN : COPPER;
        }
        return DARK;
    }

    @Override
    protected BlockState ceilingBlock(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        if (r >= 7.5 && r < 9 && Math.floorMod(x + z, 2) == 0) {
            return ORANGE_LIGHT;
        }
        if (r >= 13 && Math.floorMod(x, 3) == 0 && Math.floorMod(z, 3) == 0) {
            return ORANGE_LIGHT;
        }
        return COPPER;
    }

    // --- Estrade et galerie -------------------------------------------------------------------------

    @Override
    protected BlockState daisTop(int x, int z) {
        return GRATE; // on voit le verre et la fosse verte à travers
    }

    @Override
    protected BlockState galleryFloor(int x, int z) {
        return GRATE;
    }

    @Override
    protected int ambientLevel() {
        return 12;
    }

    // --- Décor ----------------------------------------------------------------------------------

    @Override
    protected void decorate(RoomCanvas c) {
        // Quatre jambes de cuivre en diagonale, coiffées d'une entretoise tournée vers la console
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(45 + k * 90);
            int px = (int) Math.round(Math.cos(a) * 8.5);
            int pz = (int) Math.round(Math.sin(a) * 8.5);
            for (int y = 1; y < HEIGHT; y++) {
                c.set(px, y, pz, ModBlocks.COPPER_BEAM.getDefaultState());
            }
            Direction toward = RoomCanvas.towardCenter(px, pz);
            for (int y : new int[]{GALLERY_Y, HEIGHT - 1}) {
                c.set(px + toward.getOffsetX(), y, pz + toward.getOffsetZ(),
                        ModBlocks.COPPER_STRUT.getDefaultState().with(FacingDecorBlock.FACING, toward));
            }
        }
        // Banquettes face à la console, sur le verre
        for (int z = 3; z <= 6; z++) {
            seat(c, -9, 1, z, ModBlocks.LEATHER_SEAT, Direction.EAST);
            seat(c, 9, 1, z, ModBlocks.LEATHER_SEAT, Direction.WEST);
        }
        // Bibliothèques contre le mur, sous la galerie (entre les sorties et les escaliers)
        int max = (int) Math.ceil(RADIUS);
        for (int x = -max; x <= max; x++) {
            for (int z = -max; z <= max; z++) {
                double r = RoomCanvas.radius(x, z);
                double a = RoomCanvas.angle(x, z);
                boolean shelfZone = RoomCanvas.between(a, 196, 252) || RoomCanvas.between(a, 288, 344);
                if (r > RADIUS - 1 && r <= RADIUS && shelfZone) {
                    for (int y = 1; y <= 3; y++) {
                        c.set(x, y, z, Blocks.BOOKSHELF.getDefaultState());
                    }
                }
            }
        }
        // Scanner au-dessus de la sortie nord, porte-manteau près de la porte
        for (int x = -1; x <= 1; x++) {
            for (int y = 4; y <= 5; y++) {
                c.set(x, y, -16, ModBlocks.SCANNER.getDefaultState().with(FacingDecorBlock.FACING, Direction.SOUTH));
            }
        }
        c.set(4, 1, 13, ModBlocks.HAT_STAND.getDefaultState());
        // Tuyaux de cuivre qui rayonnent au plafond depuis la colonne
        for (Direction d : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
            Direction.Axis axis = d.getAxis();
            for (int i = 3; i <= 15; i++) {
                c.set(d.getOffsetX() * i, HEIGHT - 1, d.getOffsetZ() * i, ModBlocks.COPPER_PIPE.getDefaultState()
                        .with(net.minecraft.block.PillarBlock.AXIS, axis));
            }
        }
        wallLamps(c, GALLERY_Y + 4);
    }
}
