package fr.doctorwho.tardis.interior;

import fr.doctorwho.block.FacingDecorBlock;
import fr.doctorwho.block.LightColumnBlock;
import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.tardis.InteriorBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.StairsBlock;
import net.minecraft.util.math.Direction;

import static fr.doctorwho.tardis.interior.ConsoleRoom.*;

/**
 * Décor « Turquoise tech » (Onzième Docteur, 2012) : clair, net et technologique.
 * <ul>
 *     <li>murs de panneaux hexagonaux en relief, une bande allumée en cyan à chaque niveau ;</li>
 *     <li>sol de panneaux blancs, grand anneau lumineux cyan autour de la console (et sous la galerie) ;</li>
 *     <li>quatre colonnes de lumière cyan, garde-corps de verre, plafond blanc à couronne cyan ;</li>
 *     <li>console blanche et turquoise sur son socle lumineux.</li>
 * </ul>
 */
public class TurquoiseTheme extends InteriorTheme {

    private static final BlockState PANEL = ModBlocks.WHITE_TECH_PANEL.getDefaultState();
    private static final BlockState CYAN = ModBlocks.underlight("cyan").getDefaultState();
    private static final BlockState QUARTZ = Blocks.SMOOTH_QUARTZ.getDefaultState();
    private static final BlockState COLUMN = ModBlocks.LIGHT_COLUMN.getDefaultState()
            .with(LightColumnBlock.COLOR, LightColumnBlock.Color.CYAN);

    @Override
    public String id() {
        return "turquoise";
    }

    @Override
    public InteriorBuilder.Theme palette() {
        return InteriorBuilder.theme("turquoise");
    }

    @Override
    protected BlockState stairs() {
        return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState();
    }

    @Override
    protected BlockState railing() {
        return Blocks.LIGHT_BLUE_STAINED_GLASS_PANE.getDefaultState();
    }

    @Override
    protected int ambientLevel() {
        return 13; // salle claire
    }

    // --- Murs : panneaux hexagonaux ---------------------------------------------------------------

    @Override
    protected BlockState wallBlock(int x, int y, int z) {
        if (y == 1 || y == GALLERY_Y || y == HEIGHT - 1) {
            return PANEL;
        }
        boolean lit = y == 4 || y == GALLERY_Y + 4;
        BlockState hex = (lit ? ModBlocks.HEX_WALL_PANEL_LIT : ModBlocks.HEX_WALL_PANEL).getDefaultState();
        return hex.with(FacingDecorBlock.FACING, RoomCanvas.towardCenter(x, z));
    }

    @Override
    protected BlockState outerWall() {
        return QUARTZ;
    }

    // --- Sol, plafond -----------------------------------------------------------------------------

    @Override
    protected BlockState floorBlock(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        if ((r >= 8.4 && r < 9.3) || (r >= 15.8)) {
            return CYAN; // anneau lumineux autour de l'estrade, et le long du mur
        }
        return PANEL;
    }

    @Override
    protected BlockState subFloorBlock(int x, int z) {
        return QUARTZ;
    }

    @Override
    protected BlockState ceilingBlock(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        if ((r >= 6 && r < 7) || (r >= 12 && r < 12.9)) {
            return CYAN;
        }
        return PANEL;
    }

    @Override
    protected BlockState daisTop(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        return r > 3.8 && r <= 4.7 ? CYAN : QUARTZ;
    }

    @Override
    protected BlockState galleryFloor(int x, int z) {
        return PANEL;
    }

    // --- Décor ----------------------------------------------------------------------------------

    @Override
    protected void decorate(RoomCanvas c) {
        // Quatre colonnes de lumière cyan en diagonale (hors des allées de la porte et des sorties)
        for (double angle : new double[]{45, 135, 225, 315}) {
            double a = Math.toRadians(angle);
            int px = (int) Math.round(Math.cos(a) * 10.5);
            int pz = (int) Math.round(Math.sin(a) * 10.5);
            for (int y = 1; y < HEIGHT; y++) {
                c.set(px, y, pz, COLUMN);
            }
        }
        // Banquettes blanches, scanner au-dessus de la sortie nord
        for (int z = -6; z <= -3; z++) {
            c.setShaped(-12, 1, z, stairs().with(StairsBlock.FACING, Direction.WEST));
            c.setShaped(12, 1, z, stairs().with(StairsBlock.FACING, Direction.EAST));
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 4; y <= 5; y++) {
                c.set(x, y, -16, ModBlocks.SCANNER.getDefaultState().with(FacingDecorBlock.FACING, Direction.SOUTH));
            }
        }
    }

    @Override
    protected void doorFrame(RoomCanvas c, int z) {
        for (int y = 1; y <= 4; y++) {
            c.set(1, y, z, COLUMN);
            c.set(-1, y, z, COLUMN);
        }
    }
}
