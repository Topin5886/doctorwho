package fr.doctorwho.worldgen;

import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.util.math.Direction;

import static fr.doctorwho.worldgen.StructureCanvas.inDisc;
import static fr.doctorwho.worldgen.StructureCanvas.inSphere;
import static fr.doctorwho.worldgen.StructureCanvas.onRing;
import static fr.doctorwho.worldgen.StructureCanvas.onSphere;
import static fr.doctorwho.worldgen.StructureCanvas.sector;

/**
 * La Citadelle des Seigneurs du Temps, sur Gallifrey (environ 80 blocs de large) :
 * <ul>
 *     <li>un socle blanc et un immense dôme de verre à nervures, avec quatre portes ;</li>
 *     <li>au centre, le Capitole : salle du Panoptique (sol au sceau gallifreyen, bibliothèques)
 *     et sa crypte secrète, gardée par un Ange Pleureur (trésor) ;</li>
 *     <li>huit flèches blanches et dorées, des avenues, des jardins aux arbres argentés ;</li>
 *     <li>quelques Ood au service de la Citadelle.</li>
 * </ul>
 */
final class CitadelBuilder {

    private static final int PLATFORM = 41;
    private static final int DOME = 38;
    private static final int TOWER = 7;
    private static final int TOWER_TOP = 24;
    private static final int HALL_CEILING = 13;
    private static final int SPIRE_DISTANCE = 24;

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState BASE = Blocks.CALCITE.getDefaultState();
    private static final BlockState WHITE = Blocks.SMOOTH_QUARTZ.getDefaultState();
    private static final BlockState BRICK = Blocks.QUARTZ_BRICKS.getDefaultState();
    private static final BlockState PILLAR = Blocks.QUARTZ_PILLAR.getDefaultState();
    private static final BlockState GOLDEN = Blocks.WAXED_CUT_COPPER.getDefaultState();
    private static final BlockState GOLD = Blocks.GOLD_BLOCK.getDefaultState();
    private static final BlockState ORANGE = Blocks.ORANGE_TERRACOTTA.getDefaultState();
    private static final BlockState PATH = Blocks.SMOOTH_SANDSTONE.getDefaultState();
    private static final BlockState GLASS = Blocks.GLASS.getDefaultState();
    private static final BlockState WINDOW = Blocks.ORANGE_STAINED_GLASS.getDefaultState();
    private static final BlockState LIGHT = Blocks.SEA_LANTERN.getDefaultState();
    private static final BlockState BOOKS = Blocks.BOOKSHELF.getDefaultState();
    private static final BlockState CRYPT = Blocks.POLISHED_BLACKSTONE_BRICKS.getDefaultState();
    private static final BlockState GRASS = ModBlocks.GALLIFREY_GRASS.getDefaultState();
    private static final BlockState LOG = ModBlocks.GALLIFREY_LOG.getDefaultState();
    private static final BlockState LEAVES = ModBlocks.GALLIFREY_LEAVES.getDefaultState()
            .with(LeavesBlock.PERSISTENT, true);

    private CitadelBuilder() {
    }

    static void build(StructureCanvas c) {
        platform(c);
        dome(c);
        capitol(c);
        crypt(c); // après le sol de la salle : l'échelle traverse ce sol
        spires(c);
        gardens(c);
        lamps(c);

        // Les Ood servent la Citadelle
        c.mob(3.5, 1, 2.5, ModEntities.OOD);
        c.mob(-2.5, 1, -3.5, ModEntities.OOD);
        c.mob(20.5, 1, -5.5, ModEntities.OOD);
        c.mob(-12.5, 1, 9.5, ModEntities.OOD);
    }

    // --- Socle ---------------------------------------------------------------------------------

    private static void platform(StructureCanvas c) {
        c.columns(-PLATFORM, -PLATFORM, PLATFORM, PLATFORM, (x, z) -> {
            if (!inDisc(x, z, PLATFORM)) {
                return;
            }
            c.foundation(x, z, -1, BASE, 48);
            c.set(x, 0, z, floor(x, z));
            c.clear(x, z, 1, inDisc(x, z, DOME) ? DOME + 1 : 12);
        });
    }

    private static BlockState floor(int x, int z) {
        double d = Math.sqrt(x * x + z * z);
        if (d > PLATFORM - 1.5) {
            return GOLDEN; // bordure du socle
        }
        if (Math.abs(x) <= 1 || Math.abs(z) <= 1) {
            return PATH; // avenues vers les quatre portes
        }
        if ((d >= 15 && d < 16.5) || (d >= 30 && d < 31.5)) {
            return GOLDEN; // boulevards circulaires
        }
        if (d > 17 && d < 28 && sector(x, z, 16) % 2 == 1) {
            return GRASS; // jardins
        }
        return WHITE;
    }

    // --- Dôme de verre --------------------------------------------------------------------------

    private static void dome(StructureCanvas c) {
        c.region(-DOME - 1, 1, -DOME - 1, DOME + 1, DOME + 1, DOME + 1, (x, y, z) -> {
            if (!onSphere(x, y, z, DOME)) {
                return null;
            }
            // Portes : ouvertures de 5 blocs sur les avenues, encadrées de doré
            int lateral = Math.min(Math.abs(x), Math.abs(z));
            if (lateral <= 2 && y <= 5) {
                return AIR;
            }
            if ((lateral == 3 && y <= 6) || (lateral <= 3 && y == 6)) {
                return GOLDEN;
            }
            if (x * x + z * z <= 9) {
                return LIGHT; // lanterne au sommet
            }
            if (y == 13 || y == 26 || rib(x, z)) {
                return BRICK;
            }
            return GLASS;
        });
    }

    /** Huit nervures qui montent du sol jusqu'au sommet du dôme. */
    private static boolean rib(int x, int z) {
        for (int k = 0; k < 8; k++) {
            double angle = Math.PI / 8 + k * Math.PI / 4;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            if (x * cos + z * sin > 0 && Math.abs(x * sin - z * cos) < 0.75) {
                return true;
            }
        }
        return false;
    }

    // --- Capitole (salle du Panoptique) --------------------------------------------------------------

    private static void capitol(StructureCanvas c) {
        // Murs : portes sur les avenues, bandeaux dorés, fenêtres orange
        c.region(-TOWER - 1, 1, -TOWER - 1, TOWER + 1, TOWER_TOP, TOWER + 1, (x, y, z) -> {
            if (!onRing(x, z, TOWER)) {
                return null;
            }
            if ((Math.abs(x) <= 1 || Math.abs(z) <= 1) && y <= 4) {
                return AIR;
            }
            if ((Math.abs(x) <= 2 || Math.abs(z) <= 2) && y == 5) {
                return GOLDEN;
            }
            if (y % 6 == 0) {
                return GOLDEN;
            }
            int step = y % 6;
            if (y > 6 && step >= 2 && step <= 4 && sector(x, z, 16) % 2 == 1) {
                return WINDOW;
            }
            return WHITE;
        });

        // Sol : le sceau gallifreyen (anneaux orange et blancs, rayons dorés)
        c.region(-TOWER, 0, -TOWER, TOWER, 0, TOWER, (x, y, z) -> {
            if (!inDisc(x, z, TOWER) || onRing(x, z, TOWER)) {
                return null;
            }
            int ring = (int) Math.sqrt(x * x + z * z);
            if (ring == 0) {
                return GOLDEN;
            }
            if (ring % 2 == 1) {
                return sector(x, z, 12) % 3 == 0 ? GOLDEN : ORANGE;
            }
            return WHITE;
        });

        // Bibliothèques le long du mur (sauf devant les portes)
        c.region(-TOWER, 1, -TOWER, TOWER, 3, TOWER, (x, y, z) -> {
            if (Math.abs(x) <= 2 || Math.abs(z) <= 2) {
                return null;
            }
            return onRing(x, z, TOWER - 1) && !onRing(x, z, TOWER) ? BOOKS : null;
        });

        // Plafond de la salle avec ses lumières
        c.region(-TOWER, HALL_CEILING, -TOWER, TOWER, HALL_CEILING, TOWER, (x, y, z) -> {
            if (!inDisc(x, z, TOWER) || onRing(x, z, TOWER)) {
                return null;
            }
            double d = Math.sqrt(x * x + z * z);
            return Math.abs(d - 4) < 0.6 && sector(x, z, 8) % 2 == 0 ? LIGHT : WHITE;
        });

        // Corniche, toit conique, flèche et pointe dorée
        c.region(-TOWER - 1, TOWER_TOP, -TOWER - 1, TOWER + 1, TOWER_TOP + 7, TOWER + 1, (x, y, z) -> {
            int layer = y - TOWER_TOP;
            if (layer == 0) {
                return onRing(x, z, TOWER + 1) ? GOLDEN : null;
            }
            return inDisc(x, z, TOWER + 1 - layer) ? BRICK : null;
        });
        c.fill(0, TOWER_TOP + 8, 0, 0, TOWER_TOP + 10, 0, PILLAR);
        c.set(0, TOWER_TOP + 11, 0, GOLD);
    }

    /** Crypte sous la salle : on y descend par une échelle ; un Ange Pleureur garde le trésor. */
    private static void crypt(StructureCanvas c) {
        c.region(-6, -8, -6, 6, -1, 6, (x, y, z) -> {
            if (!inDisc(x, z, 5)) {
                return null;
            }
            return y == -8 || onRing(x, z, 5) ? CRYPT : AIR;
        });
        // Échelle accrochée au mur nord, du fond de la crypte jusqu'au sol de la salle
        BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH);
        c.fill(3, -7, -3, 3, 0, -3, ladder);

        c.spawner(0, -7, 3, ModEntities.WEEPING_ANGEL, 1, 2, 400, 800);
        c.container(-3, -7, -1, chest(Direction.EAST), "citadel");
        c.container(0, -7, -3, chest(Direction.SOUTH), "citadel");
        c.set(3, -7, 1, Blocks.SOUL_LANTERN.getDefaultState());
        c.set(-3, -7, 2, Blocks.SOUL_LANTERN.getDefaultState());
    }

    // --- Flèches ------------------------------------------------------------------------------------

    private static void spires(StructureCanvas c) {
        for (int k = 0; k < 8; k++) {
            double angle = Math.PI / 8 + k * Math.PI / 4;
            int sx = (int) Math.round(Math.cos(angle) * SPIRE_DISTANCE);
            int sz = (int) Math.round(Math.sin(angle) * SPIRE_DISTANCE);
            int r = 2 + k % 2;
            int top = 12 + c.pick(70 + k, 6);
            double towardX = -Math.cos(angle);
            double towardZ = -Math.sin(angle);

            c.region(sx - r - 1, 1, sz - r - 1, sx + r + 1, top, sz + r + 1, (x, y, z) -> {
                int lx = x - sx;
                int lz = z - sz;
                if (!onRing(lx, lz, r)) {
                    return null;
                }
                double dot = (lx * towardX + lz * towardZ) / Math.max(0.01, Math.sqrt(lx * lx + lz * lz));
                if (dot > 0.75 && y <= 3) {
                    return AIR; // porte vers le Capitole
                }
                return y % 6 == 0 ? GOLDEN : WHITE;
            });
            // Toit pointu (deux couches par bloc de rayon) et pointe dorée
            int roof = (r + 1) * 2;
            c.region(sx - r - 1, top + 1, sz - r - 1, sx + r + 1, top + roof, sz + r + 1, (x, y, z) -> {
                double radius = r + 1 - (y - top - 1) / 2.0;
                return radius > 0 && inDisc(x - sx, z - sz, radius) ? BRICK : null;
            });
            c.set(sx, top + roof + 1, sz, GOLD);

            if (k == 1 || k == 5) {
                c.container(sx, 1, sz, chest(Direction.getFacing(towardX, 0, towardZ)), "citadel");
            } else {
                c.set(sx, 0, sz, LIGHT);
            }
        }
    }

    // --- Jardins et lampadaires -----------------------------------------------------------------

    private static void gardens(StructureCanvas c) {
        for (int k = 0; k < 4; k++) {
            double angle = Math.PI / 4 + k * Math.PI / 2;
            tree(c, (int) Math.round(Math.cos(angle) * 19), (int) Math.round(Math.sin(angle) * 19));
            tree(c, (int) Math.round(Math.cos(angle) * 27), (int) Math.round(Math.sin(angle) * 27));
        }
    }

    /** Petit arbre argenté de Gallifrey sur sa pelouse rouge. */
    private static void tree(StructureCanvas c, int tx, int tz) {
        c.region(tx - 3, 0, tz - 3, tx + 3, 0, tz + 3, (x, y, z) -> inDisc(x - tx, z - tz, 2.5) ? GRASS : null);
        c.fill(tx, 1, tz, tx, 4, tz, LOG);
        c.region(tx - 3, 4, tz - 3, tx + 3, 8, tz + 3, (x, y, z) -> {
            int dx = x - tx;
            int dz = z - tz;
            if (dx == 0 && dz == 0 && y <= 4) {
                return null;
            }
            return inSphere(dx, (y - 6) * 1.3, dz, 2.4) ? LEAVES : null;
        });
    }

    /** Lampadaires de part et d'autre des avenues. */
    private static void lamps(StructureCanvas c) {
        for (int distance : new int[]{10, 20, 34}) {
            for (int sign : new int[]{-1, 1}) {
                lamp(c, 2, sign * distance);
                lamp(c, -2, sign * distance);
                lamp(c, sign * distance, 2);
                lamp(c, sign * distance, -2);
            }
        }
    }

    private static void lamp(StructureCanvas c, int x, int z) {
        c.fill(x, 1, z, x, 3, z, PILLAR);
        c.set(x, 4, z, LIGHT);
    }

    private static BlockState chest(Direction facing) {
        return Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing);
    }
}
