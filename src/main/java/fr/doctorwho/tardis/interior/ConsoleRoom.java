package fr.doctorwho.tardis.interior;

import net.minecraft.util.math.BlockPos;

/**
 * Plan commun à toutes les grandes salles de console (coordonnées relatives au centre, y = 0 au sol).
 *
 * <pre>
 *   y 17  ─────────────── plafond ───────────────
 *         │ mur                                 │
 *   y 7   │═══ galerie ═══╗           ╔═══ galerie ═══│   (anneau de 12,5 à 16,5 blocs du centre)
 *         │  escaliers  ╱    console   ╲            │
 *   y 1   │        ▁▁▁▁▁ estrade (r 6,5) ▁▁▁▁▁       │
 *   y 0   ═══════════════ sol ═══════════════════════
 * </pre>
 * La porte intérieure est au sud (z positif), les trois sorties vers les couloirs au nord, à l'est
 * et à l'ouest. Les deux escaliers montent vers la galerie de part et d'autre de la porte.
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

    /** Escaliers vers la galerie : bande de rayon, et arcs (degrés, du bas vers le haut). */
    public static final double STAIR_INNER = 13.5;
    public static final double STAIR_OUTER = 15.7;
    public static final double[][] STAIRS = {{112, 162}, {68, 18}};

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

    /** Vrai si (x, z) est dans un des deux escaliers (sur toute leur bande, garde-corps compris). */
    public static boolean onStairs(int x, int z) {
        double r = RoomCanvas.radius(x, z);
        if (r < GALLERY_INNER || r > RADIUS) {
            return false;
        }
        double a = RoomCanvas.angle(x, z);
        for (double[] flight : STAIRS) {
            if (RoomCanvas.between(a, flight[0], flight[1])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Hauteur de la marche d'escalier en (x, z) : de 1 (en bas) à {@link #GALLERY_Y} (palier), ou 0 hors escalier.
     */
    public static int stairHeight(int x, int z) {
        double a = RoomCanvas.angle(x, z);
        for (double[] flight : STAIRS) {
            if (RoomCanvas.between(a, flight[0], flight[1])) {
                double t = (a - flight[0]) / (flight[1] - flight[0]);
                return Math.max(1, Math.min(GALLERY_Y, 1 + (int) (t * GALLERY_Y)));
            }
        }
        return 0;
    }

    /** Sens de montée de l'escalier en (x, z) : vrai si les angles augmentent en montant. */
    public static boolean stairsIncreasing(int x, int z) {
        double a = RoomCanvas.angle(x, z);
        for (double[] flight : STAIRS) {
            if (RoomCanvas.between(a, flight[0], flight[1])) {
                return flight[1] > flight[0];
            }
        }
        return true;
    }
}
