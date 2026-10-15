package fr.doctorwho.tardis.interior;

import fr.doctorwho.block.FacingDecorBlock;
import fr.doctorwho.block.LightColumnBlock;
import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.tardis.InteriorBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EndRodBlock;
import net.minecraft.block.PillarBlock;
import net.minecraft.util.math.Direction;

import static fr.doctorwho.tardis.interior.ConsoleRoom.*;

/**
 * Décor « Bleu cathédrale » (le film de 1996, version vaisseau gothique) :
 * <ul>
 *     <li>deux grandes <b>arches gothiques</b> en poutres d'acier bleu ajourées qui se croisent au-dessus de la console ;</li>
 *     <li>murs de roundels bleus cerclés de laiton entre des poutres ajourées ;</li>
 *     <li>galerie-bibliothèque au plancher de bois, chandeliers, colonnes de lumière bleue ;</li>
 *     <li>voûte noire constellée d'étoiles ; console d'acier bleu aux arcs de laiton.</li>
 * </ul>
 */
public class CathedralTheme extends InteriorTheme {

    /** Les deux arches : chaque paire d'angles opposés forme une arche qui passe au-dessus de la console. */
    private static final double[] ARCH_FEET = {30, 210, 150, 330};
    private static final double ARCH_FOOT = 11.5;

    private static final BlockState TRUSS = ModBlocks.CATHEDRAL_TRUSS.getDefaultState();
    private static final BlockState TILES = Blocks.DEEPSLATE_TILES.getDefaultState();
    private static final BlockState SLATE = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState BLUE = ModBlocks.BLUE_HEX_LIGHT.getDefaultState();
    private static final BlockState NIGHT = Blocks.BLACK_CONCRETE.getDefaultState();
    private static final BlockState WOOD = Blocks.SPRUCE_PLANKS.getDefaultState();
    private static final BlockState COLUMN = ModBlocks.LIGHT_COLUMN.getDefaultState()
            .with(LightColumnBlock.COLOR, LightColumnBlock.Color.BLUE);

    @Override
    public String id() {
        return "cathedral";
    }

    @Override
    public InteriorBuilder.Theme palette() {
        return InteriorBuilder.theme("cathedral");
    }

    @Override
    protected BlockState stairs() {
        return Blocks.POLISHED_DEEPSLATE_STAIRS.getDefaultState();
    }

    @Override
    protected BlockState railing() {
        return Blocks.DARK_OAK_FENCE.getDefaultState();
    }

    @Override
    protected int ambientLevel() {
        return 8; // pénombre, éclairée par les bougies et le bleu
    }

    // --- Murs, sol, voûte ------------------------------------------------------------------------

    @Override
    protected BlockState wallBlock(int x, int y, int z) {
        double a = RoomCanvas.angle(x, z);
        if (Math.toRadians(Math.abs(((a + 15) % 30) - 15)) * RoomCanvas.radius(x, z) < 0.6) {
            return TRUSS; // poutres verticales ajourées
        }
        if (y > GALLERY_Y && y <= GALLERY_Y + 3) {
            return Blocks.BOOKSHELF.getDefaultState(); // la bibliothèque de la galerie
        }
        if (y == GALLERY_Y || y == HEIGHT - 1) {
            return SLATE;
        }
        boolean lit = Math.floorMod(x + z + y, 4) == 0;
        return ModBlocks.roundel("cathedral", lit).getDefaultState()
                .with(FacingDecorBlock.FACING, RoomCanvas.towardCenter(x, z));
    }

    @Override
    protected BlockState outerWall() {
        return SLATE;
    }

    @Override
    protected BlockState floorBlock(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        // Rosace : un anneau d'alvéoles bleues autour de l'estrade, rayons de dalles
        if (r >= 8.6 && r < 9.4 && Math.floorMod(x + z, 2) == 0) {
            return BLUE;
        }
        return Math.floorMod((int) Math.round(RoomCanvas.angle(x, z) / 22.5), 2) == 0 ? TILES : SLATE;
    }

    @Override
    protected BlockState subFloorBlock(int x, int z) {
        return SLATE;
    }

    @Override
    protected BlockState ceilingBlock(int x, int z) {
        return NIGHT;
    }

    @Override
    protected BlockState daisTop(int x, int z) {
        return RoomCanvas.radius(x, z) > 5.5 ? TILES : SLATE;
    }

    @Override
    protected BlockState galleryFloor(int x, int z) {
        return WOOD;
    }

    // --- Décor ----------------------------------------------------------------------------------

    @Override
    protected void decorate(RoomCanvas c) {
        for (double foot : ARCH_FEET) {
            halfArch(c, Math.toRadians(foot));
        }
        // Étoiles : petites baguettes lumineuses accrochées à la voûte, réparties au hasard (toujours les mêmes)
        int max = (int) RADIUS;
        BlockState star = Blocks.END_ROD.getDefaultState().with(EndRodBlock.FACING, Direction.DOWN);
        for (int x = -max; x <= max; x++) {
            for (int z = -max; z <= max; z++) {
                if (RoomCanvas.radius(x, z) < RADIUS - 1 && Math.floorMod(x * 73 + z * 151 + x * z * 7, 29) == 0
                        && c.get(x, HEIGHT - 1, z).isAir()) {
                    c.set(x, HEIGHT - 1, z, star);
                }
            }
        }
        // Chandeliers autour de l'estrade (hors des allées vers la porte et les sorties)
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + k * 45);
            c.set((int) Math.round(Math.cos(a) * 8), 1, (int) Math.round(Math.sin(a) * 8),
                    ModBlocks.CANDELABRA.getDefaultState());
        }
        // Quatre colonnes de lumière bleue, de part et d'autre de la porte et de la sortie nord
        for (double angle : new double[]{72, 108, 252, 288}) {
            int px = (int) Math.round(Math.cos(Math.toRadians(angle)) * 14.5);
            int pz = (int) Math.round(Math.sin(Math.toRadians(angle)) * 14.5);
            for (int y = 1; y < HEIGHT; y++) {
                c.set(px, y, pz, COLUMN);
            }
        }
        // Fauteuils de lecture sur la galerie
        for (int x : new int[]{-2, 2}) {
            c.setShaped(x, GALLERY_Y + 1, -15, Blocks.DARK_OAK_STAIRS.getDefaultState()
                    .with(net.minecraft.block.StairsBlock.FACING, Direction.NORTH));
        }
    }

    /**
     * Demi-arche gothique : elle monte presque droite depuis le sol, puis se courbe et rejoint en pointe
     * le sommet, au-dessus de la console, où elle retrouve la demi-arche opposée.
     * Les poutres sont tournées selon la pente (verticales en bas, couchées en haut).
     */
    private void halfArch(RoomCanvas c, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        Direction.Axis flat = Math.abs(cos) >= Math.abs(sin) ? Direction.Axis.X : Direction.Axis.Z;
        for (int i = 0; i <= 240; i++) {
            double t = i / 240.0;
            double r = ARCH_FOOT * (1 - Math.pow(t, 2.2)) + 2.0 * Math.pow(t, 2.2);
            double y = 1 + (HEIGHT - 2) * Math.sin(t * Math.PI / 2);
            // Pente locale : verticale en bas, de plus en plus couchée vers le sommet
            Direction.Axis axis = t < 0.62 ? Direction.Axis.Y : flat;
            for (double dr = -0.5; dr <= 0.5; dr += 0.5) {
                int x = (int) Math.round((r + dr) * cos);
                int z = (int) Math.round((r + dr) * sin);
                c.set(x, (int) Math.round(y), z, TRUSS.with(PillarBlock.AXIS, axis));
            }
        }
    }
}
