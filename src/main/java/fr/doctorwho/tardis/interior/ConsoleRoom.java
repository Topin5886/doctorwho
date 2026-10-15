package fr.doctorwho.tardis.interior;

import net.minecraft.util.math.BlockPos;

/**
 * Plan commun à toutes les grandes salles de console (coordonnées relatives au centre, y = 0 au sol).
 *
 * <pre>
 *   y 17  ─────────────── plafond ───────────────
 *         │ mur                                 │
 *   y 7   │═══ galerie ═══╗           ╔═══ galerie ═══│   (anneau de 12,5 à 16,5 blocs du centre)
 *         │  escaliers droits      console            │
 *   y 1   │        ▁▁▁▁▁ estrade (r 6,5) ▁▁▁▁▁       │
 *   y 0   ═══════════════ sol ═══════════════════════
 * </pre>
 * La porte intérieure est au sud (z positif), les trois sorties vers les couloirs au nord, à l'est
 * et à l'ouest. Deux escaliers droits montent vers la galerie de part et d'autre de la porte.
 */
public final class ConsoleRoom {

    /** Rayon intérieur de la salle (le mur commence juste au-delà). */
    public static final double RADIUS = 16.5;
    /** Rayon extérieur du mur (deux blocs d'épaisseur). */
    public static final double WALL = 18.6;
    /** Hauteur du plafond (bloc) au-dessus du sol. */
    public static final int HEIGHT = 17;
    /** Plus bas niveau construit (fosses sous le sol). */
    public static final int BOTTOM = -4;

    /** Estrade de la console : rayon du dessus, puis une marche tout autour. */
    public static final double DAIS = 6.5;
    /** Hauteur du plancher de la galerie, et son bord intérieur. */
    public static final int GALLERY_Y = 7;
    public static final double GALLERY_INNER = 12.5;

    /**
     * Escaliers vers la galerie : deux volées <b>droites</b> de part et d'autre de la porte, le long du mur sud.
     * Celle de l'est monte vers l'est (x de 2 à 7, palier en 8), celle de l'ouest vers l'ouest.
     * Rangée {@link #STAIR_RAIL_Z} : limon et garde-corps côté salle ; rangées suivantes : les marches.
     */
    public static final int STAIR_START = 2;
    public static final int STAIR_LANDING = 8;
    public static final int STAIR_RAIL_Z = 12;
    public static final int STAIR_FIRST_Z = 13;
    public static final int STAIR_LAST_Z = 15;

    /** Distance au centre du premier bloc de mur percé par une sortie (nord, est, ouest) et par la porte (sud). */
    public static final int EXIT_DISTANCE = 17;

    private ConsoleRoom() {
    }

    /** Moitié basse de la porte intérieure, dans le mur sud. */
    public static BlockPos doorPos(BlockPos center) {
        return center.add(0, 1, EXIT_DISTANCE);
    }

    /** Bloc de la console, posée sur l'estrade. */
    public static BlockPos consolePos(BlockPos center) {
        return center.up(2);
    }

    /** Vrai si (x, z) est dans le vide de la salle. */
    public static boolean inside(int x, int z) {
        return RoomCanvas.radius(x, z) <= RADIUS;
    }

    /** Vrai si (x, z) est dans la couche intérieure du mur (celle qu'on voit depuis la salle). */
    public static boolean innerWall(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        return r > RADIUS && r <= RADIUS + 1.3;
    }

    /** Vrai si (x, z) fait partie d'un des deux escaliers (limon, marches et massif côté mur compris). */
    public static boolean onStairs(int x, int z) {
        int ax = Math.abs(x);
        return ax >= STAIR_START && ax <= STAIR_LANDING && z >= STAIR_RAIL_Z && inside(x, z);
    }

    /** Hauteur de la marche en (x, z) : de 1 (en bas) à {@link #GALLERY_Y} (palier), ou 0 hors escalier. */
    public static int stairHeight(int x, int z) {
        return onStairs(x, z) ? Math.abs(x) - STAIR_START + 1 : 0;
    }
}
