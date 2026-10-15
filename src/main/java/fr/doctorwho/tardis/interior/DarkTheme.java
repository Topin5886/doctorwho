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
 * Décor « Sombre industriel » (Douzième Docteur, 2014) : acier sombre, laiton et lumière orange.
 * <ul>
 *     <li>murs de plaques d'acier rivetées et de roundels sombres cerclés d'or, allumés en blanc-bleu ;</li>
 *     <li>bibliothèques tout autour de la galerie (le Douzième lisait beaucoup) ;</li>
 *     <li>un anneau lumineux orange autour de la console, une couronne de grilles éclairées en orange ;</li>
 *     <li>quatre grandes <b>colonnes de lumière</b> orange du sol au plafond ; rambardes de laiton ;</li>
 *     <li>console sombre et dorée avec son grand anneau suspendu.</li>
 * </ul>
 */
public class DarkTheme extends InteriorTheme {

    private static final BlockState PANEL = ModBlocks.DARK_WALL_PANEL.getDefaultState();
    private static final BlockState GRATE = ModBlocks.floorGrate("dark").getDefaultState();
    private static final BlockState ORANGE = ModBlocks.underlight("orange").getDefaultState();
    private static final BlockState FLOOR = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState STONE = Blocks.POLISHED_BLACKSTONE.getDefaultState();
    private static final BlockState BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
    private static final BlockState CEILING_LIGHT = Blocks.OCHRE_FROGLIGHT.getDefaultState();
    private static final BlockState COLUMN = ModBlocks.LIGHT_COLUMN.getDefaultState()
            .with(LightColumnBlock.COLOR, LightColumnBlock.Color.ORANGE);

    @Override
    public String id() {
        return "dark";
    }

    @Override
    public InteriorBuilder.Theme palette() {
        return InteriorBuilder.theme("dark");
    }

    @Override
    protected BlockState stairs() {
        return Blocks.POLISHED_BLACKSTONE_STAIRS.getDefaultState();
    }

    @Override
    protected BlockState railing() {
        return ModBlocks.BRASS_HANDRAIL.getDefaultState();
    }

    @Override
    protected int ambientLevel() {
        return 9; // ambiance sombre, éclairée par l'orange et les roundels
    }

    // --- Murs ---------------------------------------------------------------------------------

    @Override
    protected BlockState wallBlock(int x, int y, int z) {
        double a = RoomCanvas.angle(x, z);
        boolean stripe = Math.toRadians(Math.abs(((a + 15) % 30) - 15)) * RoomCanvas.radius(x, z) < 0.6;
        if (stripe || y == 1 || y == GALLERY_Y || y == HEIGHT - 1) {
            return PANEL;
        }
        if (y > GALLERY_Y && y <= GALLERY_Y + 3) {
            return Blocks.BOOKSHELF.getDefaultState(); // bibliothèques de la galerie
        }
        boolean lit = y % 2 == 1 && Math.floorMod(x + z, 2) == 0;
        return ModBlocks.roundel("dark", lit).getDefaultState()
                .with(FacingDecorBlock.FACING, RoomCanvas.towardCenter(x, z));
    }

    @Override
    protected BlockState outerWall() {
        return PANEL;
    }

    // --- Sol, plafond -------------------------------------------------------------------------

    @Override
    protected BlockState floorBlock(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        return r >= 7.5 && r <= 10.5 ? GRATE : FLOOR;
    }

    @Override
    protected BlockState subFloorBlock(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        return r >= 7.5 && r <= 10.5 && Math.floorMod(x + z, 2) == 0 ? ORANGE : STONE;
    }

    @Override
    protected BlockState ceilingBlock(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        if (r >= 7 && r < 8.2) {
            return CEILING_LIGHT; // couronne lumineuse au-dessus de l'anneau suspendu
        }
        if (r >= 11 && Math.floorMod(x, 4) == 0 && Math.floorMod(z, 4) == 0) {
            return CEILING_LIGHT;
        }
        return BRICKS;
    }

    // --- Estrade et galerie -------------------------------------------------------------------

    @Override
    protected BlockState daisTop(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        if (r > 3.6 && r <= 4.6) {
            return ORANGE; // anneau lumineux orange autour de la console
        }
        return r > 5.5 ? BRICKS : STONE;
    }

    @Override
    protected BlockState galleryFloor(int x, int z) {
        return GRATE;
    }

    // --- Décor ----------------------------------------------------------------------------------

    @Override
    protected void decorate(RoomCanvas c) {
        // Quatre colonnes de lumière orange du sol au plafond, en diagonale
        for (int k = 0; k < 4; k++) {
            double a = Math.toRadians(45 + k * 90);
            int px = (int) Math.round(Math.cos(a) * 9);
            int pz = (int) Math.round(Math.sin(a) * 9);
            for (int y = 1; y < HEIGHT; y++) {
                c.set(px, y, pz, COLUMN);
            }
        }
        // Deux fauteuils face à la console
        for (int z = 4; z <= 5; z++) {
            c.setShaped(11, 1, z, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.EAST));
            c.setShaped(-11, 1, z, Blocks.DARK_OAK_STAIRS.getDefaultState().with(StairsBlock.FACING, Direction.WEST));
        }
        // Tableau noir couvert de calculs (côté nord de la galerie) et scanner au-dessus de la sortie nord
        for (int x = -3; x <= 3; x++) {
            for (int y = GALLERY_Y + 1; y <= GALLERY_Y + 3; y++) {
                c.set(x, y, -17, Blocks.BLACK_CONCRETE.getDefaultState());
            }
        }
        for (int x = -1; x <= 1; x++) {
            for (int y = 4; y <= 5; y++) {
                c.set(x, y, -16, ModBlocks.SCANNER.getDefaultState().with(FacingDecorBlock.FACING, Direction.SOUTH));
            }
        }
        wallLamps(c, 4);
    }
}
