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
 * Décor « Classique » (années 60-70, et la salle blanche de 2017) :
 * murs blancs entièrement couverts de roundels en relief, séparés par des pilastres cannelés,
 * panneaux de roundels allumés en blanc chaud, sol blanc lisse,
 * plafond lumineux, écran du scanner au-dessus de la sortie nord, porte-manteau près de la porte.
 * Console blanche hexagonale ; pas de colonne jusqu'au plafond (le rotor reste dans la console).
 */
public class ClassicTheme extends InteriorTheme {

    /** Les murs sont découpés en 12 panneaux de 30° séparés par des pilastres. */
    private static final double PANEL = 30;

    private static final BlockState WHITE = Blocks.SMOOTH_QUARTZ.getDefaultState();
    /** Sol et plafond d'un blanc pur, comme la salle de l'image de référence. */
    private static final BlockState PURE = Blocks.WHITE_CONCRETE.getDefaultState();
    private static final BlockState GREY = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
    private static final BlockState BRICKS = Blocks.QUARTZ_BRICKS.getDefaultState();
    private static final BlockState PILASTER = ModBlocks.CLASSIC_PILASTER.getDefaultState();
    private static final BlockState PANEL_LIGHT = ModBlocks.LIGHT_PANEL.getDefaultState();

    @Override
    public String id() {
        return "white";
    }

    @Override
    public InteriorBuilder.Theme palette() {
        return InteriorBuilder.theme("white");
    }

    @Override
    protected BlockState stairs() {
        return Blocks.SMOOTH_QUARTZ_STAIRS.getDefaultState();
    }

    @Override
    protected BlockState railing() {
        return Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState();
    }

    /** Salle blanche, lumineuse partout. */
    @Override
    protected int ambientLevel() {
        return 15;
    }

    // --- Murs : panneaux de roundels entre des pilastres ---------------------------------------

    @Override
    protected BlockState wallBlock(int x, int y, int z) {
        double a = RoomCanvas.angle(x, z);
        double fromEdge = Math.min(a % PANEL, PANEL - a % PANEL);
        if (Math.toRadians(fromEdge) * RoomCanvas.radius(x, z) < 0.75) {
            return PILASTER;
        }
        if (y == GALLERY_Y || y == HEIGHT - 1) {
            return WHITE; // bandeau au niveau de la galerie, corniche sous le plafond
        }
        // Un panneau sur deux allumé, en alternance entre le bas et le haut
        int panel = (int) (a / PANEL);
        boolean lit = (panel + (y > GALLERY_Y ? 1 : 0)) % 2 == 0;
        return ModBlocks.roundel("classic", lit).getDefaultState()
                .with(FacingDecorBlock.FACING, RoomCanvas.towardCenter(x, z));
    }

    @Override
    protected BlockState outerWall() {
        return WHITE;
    }

    // --- Sol et plafond -------------------------------------------------------------------------

    @Override
    protected BlockState floorBlock(int x, int z) {
        // Sol blanc uni ; seule une bordure grise longe le pied du mur (sous la galerie)
        return RoomCanvas.radius(x, z) >= 15.6 ? GREY : PURE;
    }

    @Override
    protected BlockState subFloorBlock(int x, int z) {
        return WHITE;
    }

    @Override
    protected BlockState pitBlock(int x, int y, int z) {
        return WHITE;
    }

    @Override
    protected BlockState ceilingBlock(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        if (r <= 3.5) {
            return PANEL_LIGHT; // grand disque lumineux au-dessus de la console
        }
        if (r >= 8.5 && r < 10 && Math.floorMod(x + z, 2) == 0) {
            return PANEL_LIGHT; // couronne lumineuse
        }
        if (r >= 13 && Math.floorMod(x, 3) == 0 && Math.floorMod(z, 3) == 0) {
            return PANEL_LIGHT; // au-dessus de la galerie
        }
        return PURE;
    }

    // --- Estrade et galerie ---------------------------------------------------------------------

    @Override
    protected BlockState daisTop(int x, int z) {
        return RoomCanvas.radius(x, z) > 5.5 ? BRICKS : PURE;
    }

    @Override
    protected BlockState galleryFloor(int x, int z) {
        // Quelques panneaux lumineux dans le plancher : ils éclairent aussi le dessous de la galerie
        return Math.floorMod(x * 3 + z * 5, 11) == 0 ? PANEL_LIGHT : PURE;
    }

    // --- Décor ----------------------------------------------------------------------------------

    @Override
    protected void decorate(RoomCanvas c) {
        // Écran du scanner (3 x 2) au-dessus de la sortie nord, encastré dans le mur
        for (int x = -1; x <= 1; x++) {
            for (int y = 4; y <= 5; y++) {
                c.set(x, y, -16, ModBlocks.SCANNER.getDefaultState().with(FacingDecorBlock.FACING, Direction.SOUTH));
            }
        }
        // Porte-manteau près de la porte
        c.set(-4, 1, 13, ModBlocks.HAT_STAND.getDefaultState());
        // Deux banquettes blanches face à la console, à l'est et à l'ouest
        for (int z = -6; z <= -3; z++) {
            c.setShaped(-12, 1, z, stairs().with(StairsBlock.FACING, Direction.WEST));
            c.setShaped(12, 1, z, stairs().with(StairsBlock.FACING, Direction.EAST));
        }
        // Sur la galerie : coin lecture (bibliothèques et pupitre) côté nord
        for (int x = -3; x <= 3; x++) {
            if (Math.abs(x) >= 2) {
                c.set(x, GALLERY_Y + 1, -16, Blocks.BOOKSHELF.getDefaultState());
                c.set(x, GALLERY_Y + 2, -16, Blocks.BOOKSHELF.getDefaultState());
            }
        }
        c.set(0, GALLERY_Y + 1, -15, Blocks.LECTERN.getDefaultState());
    }

    @Override
    protected void doorFrame(RoomCanvas c, int z) {
        for (int y = 1; y <= 5; y++) {
            c.set(1, y, z, PILASTER);
            c.set(-1, y, z, PILASTER);
        }
        c.set(0, 5, z, PILASTER);
    }
}
