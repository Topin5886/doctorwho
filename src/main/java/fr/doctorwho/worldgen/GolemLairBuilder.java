package fr.doctorwho.worldgen;

import fr.doctorwho.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.util.math.Direction;

/**
 * Repaire du golem de redstone : une caverne souterraine en forme de dôme (24 blocs de large),
 * aux parois de pierre noire veinées de redstone, éclairée par des piliers à lampes.
 * Le golem garde deux coffres au fond. Un puits avec une échelle remonte jusqu'à la surface,
 * marqué par un petit anneau de pierre noire et des torches de redstone.
 * <p>
 * y = 0 est le sol de la caverne (sous terre : voir {@link PlanetStructure}).
 */
public final class GolemLairBuilder {

    /** Rayon intérieur et hauteur sous la voûte. */
    private static final int R = 12;
    private static final int H = 10;
    /** Épaisseur de la coque (empêche l'eau ou la lave des grottes voisines d'entrer). */
    private static final int SHELL = 2;

    public static final int RADIUS = R + SHELL + 1;
    public static final int HEIGHT = H + SHELL;

    /** Puits d'accès : l'échelle est en (SHAFT_X, ·, 0), accrochée au mur en SHAFT_X + 1. */
    private static final int SHAFT_X = R - 2;

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.getDefaultState();
    private static final BlockState POLISHED = Blocks.POLISHED_BLACKSTONE.getDefaultState();
    private static final BlockState BRICKS = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
    private static final BlockState CRACKED = Blocks.CRACKED_POLISHED_BLACKSTONE_BRICKS.getDefaultState();
    private static final BlockState CHISELED = Blocks.CHISELED_POLISHED_BLACKSTONE.getDefaultState();
    private static final BlockState BASALT = Blocks.POLISHED_BASALT.getDefaultState();
    private static final BlockState REDSTONE_BLOCK = Blocks.REDSTONE_BLOCK.getDefaultState();
    private static final BlockState REDSTONE_ORE = Blocks.REDSTONE_ORE.getDefaultState();
    private static final BlockState DEEP_REDSTONE_ORE = Blocks.DEEPSLATE_REDSTONE_ORE.getDefaultState();
    private static final BlockState LAMP = Blocks.REDSTONE_LAMP.getDefaultState().with(RedstoneLampBlock.LIT, true);

    private GolemLairBuilder() {
    }

    /** Vrai si (x, y, z) est dans le vide de la caverne (sol plat en y = 0, voûte en dôme). */
    private static boolean inside(double x, int y, double z, double r, double h) {
        if (y < 1) {
            return false;
        }
        double dy = (y - 0.5) / h;
        return (x * x + z * z) / (r * r) + dy * dy < 1.0;
    }

    public static void build(StructureCanvas c) {
        // Coque et vide de la caverne
        c.region(-RADIUS, -SHELL, -RADIUS, RADIUS, HEIGHT, RADIUS, (x, y, z) -> {
            if (inside(x, y, z, R, H)) {
                return AIR;
            }
            if (y < 1 ? StructureCanvas.inDisc(x, z, R + SHELL) : inside(x, y, z, R + SHELL, H + SHELL)) {
                return wall(c, x, y, z);
            }
            return null;
        });

        // Sol : dalles de pierre noire polie, cercle ciselé au centre, veines de redstone
        c.region(-R, 0, -R, R, 0, R, (x, y, z) -> {
            if (!StructureCanvas.inDisc(x, z, R)) {
                return null;
            }
            double d = Math.sqrt(x * x + z * z);
            if (d < 3.5) {
                return StructureCanvas.onRing(x, z, 3) ? CHISELED : POLISHED;
            }
            if (Math.abs(d - 7) < 0.5) {
                return REDSTONE_ORE; // anneau de minerai qui s'illumine quand on marche dessus
            }
            double n = c.noise(x, 0, z);
            return n < 0.12 ? CRACKED : n < 0.16 ? REDSTONE_ORE : BRICKS;
        });

        // Quatre piliers à lampes (bloc de redstone + lampe allumée) jusqu'à la voûte
        for (int i = 0; i < 4; i++) {
            double angle = Math.PI / 4 + i * Math.PI / 2;
            int px = (int) Math.round(Math.cos(angle) * 7.5);
            int pz = (int) Math.round(Math.sin(angle) * 7.5);
            for (int y = 1; inside(px, y, pz, R, H); y++) {
                BlockState state = switch (y) {
                    case 3, 6 -> REDSTONE_BLOCK;
                    case 4, 7 -> LAMP;
                    default -> BASALT;
                };
                c.set(px, y, pz, state);
            }
        }

        // Estrade au fond avec deux coffres, gardée par le golem
        c.fill(-R + 2, 1, -2, -R + 3, 1, 2, BRICKS);
        BlockState chest = Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, Direction.EAST);
        c.container(-R + 3, 2, -1, chest, "golem_lair");
        c.container(-R + 3, 2, 1, chest, "golem_lair");
        c.set(-R + 2, 2, 0, REDSTONE_BLOCK);
        c.set(-R + 2, 3, 0, LAMP);

        // Le gardien
        c.mob(0.5, 1, 0.5, ModEntities.REDSTONE_GOLEM);

        shaft(c);
    }

    /** Paroi : pierre noire, ardoise, veines de minerai et quelques blocs de redstone. */
    private static BlockState wall(StructureCanvas c, int x, int y, int z) {
        double n = c.noise(x, y, z);
        if (n < 0.08) {
            return y < 4 ? DEEP_REDSTONE_ORE : REDSTONE_ORE;
        }
        if (n < 0.10) {
            return REDSTONE_BLOCK;
        }
        if (n < 0.30) {
            return Blocks.DEEPSLATE.getDefaultState();
        }
        if (n < 0.38) {
            return Blocks.BASALT.getDefaultState();
        }
        return BLACKSTONE;
    }

    /** Puits avec échelle jusqu'à la surface, tubé de pierre noire pour rester sec. */
    private static void shaft(StructureCanvas c) {
        BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.WEST);
        c.columns(SHAFT_X - 1, -1, SHAFT_X + 1, 1, (x, z) -> {
            int top = c.surfaceY(x, z);
            if (top <= 1) {
                return;
            }
            boolean hole = x == SHAFT_X && z == 0;
            boolean back = x == SHAFT_X + 1 && z == 0;
            for (int y = 1; y < top; y++) {
                if (hole) {
                    c.set(x, y, z, ladder);
                } else if (back || !inside(x, y, z, R, H)) {
                    c.set(x, y, z, BRICKS);
                }
            }
            // Margelle à la surface : un anneau de pierre noire, torches de redstone aux coins
            if (hole) {
                c.set(x, top, z, AIR);
                c.set(x, top + 1, z, AIR);
            } else if (Math.abs(x - SHAFT_X) == 1 && Math.abs(z) == 1) {
                c.set(x, top, z, BRICKS);
                c.set(x, top + 1, z, Blocks.REDSTONE_TORCH.getDefaultState());
            } else {
                c.set(x, top, z, back ? CHISELED : POLISHED);
            }
        });
    }
}
