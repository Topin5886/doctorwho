package fr.doctorwho.tardis.interior;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * « Toile » pour construire une salle de console : coordonnées <b>relatives au centre</b>
 * (x = est, z = sud, y = 0 au niveau du sol de la salle).
 * <p>
 * Les blocs sont posés sans prévenir leurs voisins (on construit tout d'un coup). Les blocs dont la
 * forme dépend des voisins (escaliers, vitres, barreaux) sont notés avec {@link #setShaped} et
 * recalculés à la fin par {@link #finish()}, comme le fait la commande /setblock.
 */
public final class RoomCanvas {

    private final ServerWorld world;
    private final BlockPos center;
    private final List<BlockPos> shaped = new ArrayList<>();

    public RoomCanvas(ServerWorld world, BlockPos center) {
        this.world = world;
        this.center = center;
    }

    public ServerWorld world() {
        return world;
    }

    public BlockPos center() {
        return center;
    }

    public BlockPos pos(int x, int y, int z) {
        return center.add(x, y, z);
    }

    public void set(int x, int y, int z, BlockState state) {
        world.setBlockState(pos(x, y, z), state, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
    }

    /** Comme {@link #set}, mais la forme du bloc sera raccordée à ses voisins à la fin. */
    public void setShaped(int x, int y, int z, BlockState state) {
        set(x, y, z, state);
        shaped.add(pos(x, y, z));
    }

    public BlockState get(int x, int y, int z) {
        return world.getBlockState(pos(x, y, z));
    }

    /** Remplit une boîte (bornes incluses). */
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) {
            for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) {
                for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) {
                    set(x, y, z, state);
                }
            }
        }
    }

    /** Raccorde escaliers, vitres et barreaux à leurs voisins (à appeler une fois la salle finie). */
    public void finish() {
        for (BlockPos p : shaped) {
            BlockState state = world.getBlockState(p);
            BlockState fixed = Block.postProcessState(state, world, p);
            if (fixed != state) {
                world.setBlockState(p, fixed, Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            }
        }
        shaped.clear();
    }

    // --- Géométrie ------------------------------------------------------------------------

    /** Distance horizontale au centre. */
    public static double radius(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    /** Angle en degrés de 0 à 360 : 0 = est, 90 = sud (porte), 180 = ouest, 270 = nord. */
    public static double angle(double x, double z) {
        double a = Math.toDegrees(Math.atan2(z, x));
        return a < 0 ? a + 360 : a;
    }

    /** Vrai si l'angle a est entre from et to (dans le sens croissant, en passant éventuellement par 0). */
    public static boolean between(double a, double from, double to) {
        double lo = Math.min(from, to);
        double hi = Math.max(from, to);
        return a >= lo && a <= hi;
    }

    /** Direction horizontale qui regarde vers le centre depuis (x, z). */
    public static Direction towardCenter(int x, int z) {
        return Direction.getFacing(-x, 0, -z);
    }

    /** Direction de la tangente au cercle en (x, z), dans le sens des angles croissants (ou décroissants). */
    public static Direction tangent(int x, int z, boolean increasing) {
        double a = Math.toRadians(angle(x, z));
        double dx = -Math.sin(a);
        double dz = Math.cos(a);
        return increasing ? Direction.getFacing(dx, 0, dz) : Direction.getFacing(-dx, 0, -dz);
    }
}
