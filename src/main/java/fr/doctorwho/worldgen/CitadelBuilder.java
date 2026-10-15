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
 * Cités des Seigneurs du Temps sous dôme de verre, sur Gallifrey, en trois tailles :
 * <ul>
 *     <li>maison de Seigneur du Temps (~36 blocs) : petit dôme, tour-bibliothèque, crypte gardée ;</li>
 *     <li>Arcadia (~52 blocs) : dôme moyen, Capitole, 6 flèches, jardins ;</li>
 *     <li>la Citadelle (~84 blocs) : immense dôme, Capitole et sa crypte, 8 flèches, Ood et K9.</li>
 * </ul>
 * La crypte (sous la salle) est toujours gardée par un Ange Pleureur. Butin : {@code doctorwho:chests/citadel}.
 */
final class CitadelBuilder {

    /**
     * @param dome       rayon du dôme de verre
     * @param tower      rayon du Capitole (tour centrale)
     * @param spires     nombre de flèches autour
     * @param ood        nombre d'Ood
     * @param k9         un K9 se promène dans la cité
     */
    private record Params(int dome, int tower, int spires, int ood, boolean k9) {
    }

    private static Params params(StructureSize size) {
        return switch (size) {
            case SMALL -> new Params(16, 3, 4, 1, false);
            case MEDIUM -> new Params(24, 5, 8, 2, false);
            case LARGE -> new Params(38, 7, 8, 4, true);
        };
    }

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

    private final StructureCanvas c;
    private final Params p;
    private final int platform;
    private final int towerTop;
    private final int hallCeiling;
    private final int spireDistance;
    private final int crypt;

    private CitadelBuilder(StructureCanvas canvas, StructureSize size) {
        this.c = canvas;
        this.p = params(size);
        this.platform = p.dome() + 3;
        // Le toit du Capitole (cône + flèche + pointe) doit rester sous le verre
        this.towerTop = p.dome() - (p.tower() + 1) - 7;
        this.hallCeiling = Math.min(13, Math.max(6, towerTop - 4));
        this.spireDistance = (int) Math.round(p.dome() * 0.63);
        this.crypt = Math.min(5, p.tower());
    }

    static int radius(StructureSize size) {
        return params(size).dome() + 3;
    }

    static int height(StructureSize size) {
        return params(size).dome() + 6;
    }

    static void build(StructureCanvas canvas, StructureSize size) {
        new CitadelBuilder(canvas, size).build();
    }

    private void build() {
        platform();
        dome();
        capitol();
        crypt(); // après le sol de la salle : l'échelle traverse ce sol
        spires();
        gardens();
        lamps();

        // Les Ood servent la cité ; dans la Citadelle, un K9 se promène aussi
        double[][] spots = {{spireDistance - 3.5, -4.5}, {-spireDistance / 2.0, spireDistance / 2.5}, {2.5, 1.5}, {-1.5, -2.5}};
        for (int i = 0; i < p.ood(); i++) {
            c.mob(spots[i][0], 1, spots[i][1], ModEntities.OOD);
        }
        if (p.k9()) {
            c.mob(5.5, 1, -spireDistance + 2.5, ModEntities.K9);
        }
    }

    // --- Socle ---------------------------------------------------------------------------------

    private void platform() {
        int dome = p.dome();
        c.columns(-platform, -platform, platform, platform, (x, z) -> {
            if (!inDisc(x, z, platform)) {
                return;
            }
            c.foundation(x, z, -1, BASE, 48);
            c.set(x, 0, z, floor(x, z));
            c.clear(x, z, 1, inDisc(x, z, dome) ? dome + 1 : 12);
        });
    }

    private BlockState floor(int x, int z) {
        double d = Math.sqrt(x * x + z * z);
        double dome = p.dome();
        if (d > platform - 1.5) {
            return GOLDEN; // bordure du socle
        }
        if (Math.abs(x) <= 1 || Math.abs(z) <= 1) {
            return PATH; // avenues vers les quatre portes
        }
        double ring1 = dome * 0.4;
        double ring2 = dome * 0.8;
        if ((d >= ring1 && d < ring1 + 1.5) || (d >= ring2 && d < ring2 + 1.5)) {
            return GOLDEN; // boulevards circulaires
        }
        if (d > ring1 + 1.5 && d < ring2 - 1 && sector(x, z, 16) % 2 == 1) {
            return GRASS; // jardins
        }
        return WHITE;
    }

    // --- Dôme de verre --------------------------------------------------------------------------

    private void dome() {
        int dome = p.dome();
        int cap = Math.max(2, dome / 12);
        int band1 = dome / 3;
        int band2 = dome * 2 / 3;
        c.region(-dome - 1, 1, -dome - 1, dome + 1, dome + 1, dome + 1, (x, y, z) -> {
            if (!onSphere(x, y, z, dome)) {
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
            if (x * x + z * z <= cap * cap) {
                return LIGHT; // lanterne au sommet
            }
            if (y == band1 || y == band2 || rib(x, z)) {
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

    private void capitol() {
        int tower = p.tower();
        // Murs : portes sur les avenues, bandeaux dorés, fenêtres orange
        c.region(-tower - 1, 1, -tower - 1, tower + 1, towerTop, tower + 1, (x, y, z) -> {
            if (!onRing(x, z, tower)) {
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
        c.region(-tower, 0, -tower, tower, 0, tower, (x, y, z) -> {
            if (!inDisc(x, z, tower) || onRing(x, z, tower)) {
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
        if (tower >= 4) {
            c.region(-tower, 1, -tower, tower, 3, tower, (x, y, z) -> {
                if (Math.abs(x) <= 2 || Math.abs(z) <= 2) {
                    return null;
                }
                return onRing(x, z, tower - 1) && !onRing(x, z, tower) ? BOOKS : null;
            });
        }

        // Plafond de la salle avec ses lumières
        c.region(-tower, hallCeiling, -tower, tower, hallCeiling, tower, (x, y, z) -> {
            if (!inDisc(x, z, tower) || onRing(x, z, tower)) {
                return null;
            }
            double d = Math.sqrt(x * x + z * z);
            return Math.abs(d - tower / 2.0) < 0.6 && sector(x, z, 8) % 2 == 0 ? LIGHT : WHITE;
        });

        // Corniche, toit conique, flèche et pointe dorée
        c.region(-tower - 1, towerTop, -tower - 1, tower + 1, towerTop + tower + 1, tower + 1, (x, y, z) -> {
            int layer = y - towerTop;
            if (layer == 0) {
                return onRing(x, z, tower + 1) ? GOLDEN : null;
            }
            return inDisc(x, z, tower + 1 - layer) ? BRICK : null;
        });
        int spireBase = towerTop + tower + 2;
        c.fill(0, spireBase, 0, 0, spireBase + 2, 0, PILLAR);
        c.set(0, spireBase + 3, 0, GOLD);
    }

    /** Crypte sous la salle : on y descend par une échelle ; un Ange Pleureur garde le trésor. */
    private void crypt() {
        int r = crypt;
        c.region(-r - 1, -8, -r - 1, r + 1, -1, r + 1, (x, y, z) -> {
            if (!inDisc(x, z, r)) {
                return null;
            }
            return y == -8 || onRing(x, z, r) ? CRYPT : AIR;
        });
        // Échelle accrochée au mur nord, du fond de la crypte jusqu'au sol de la salle
        BlockState ladder = Blocks.LADDER.getDefaultState().with(LadderBlock.FACING, Direction.SOUTH);
        c.fill(0, -7, -(r - 1), 0, 0, -(r - 1), ladder);

        int k = r - 2;
        c.spawner(0, -7, k, ModEntities.WEEPING_ANGEL, 1, 2, 400, 800);
        c.container(-k, -7, 0, chest(Direction.EAST), "citadel");
        c.set(k, -7, 0, Blocks.SOUL_LANTERN.getDefaultState());
        if (r >= 4) {
            c.container(k, -7, -1, chest(Direction.WEST), "citadel");
            c.set(-k, -7, k, Blocks.SOUL_LANTERN.getDefaultState());
        }
    }

    // --- Flèches ------------------------------------------------------------------------------------

    private void spires() {
        int count = p.spires();
        for (int k = 0; k < count; k++) {
            double angle = Math.PI / count + k * 2 * Math.PI / count;
            int sx = (int) Math.round(Math.cos(angle) * spireDistance);
            int sz = (int) Math.round(Math.sin(angle) * spireDistance);
            int r = p.dome() >= 30 ? 2 + k % 2 : (p.dome() >= 20 ? 2 : 1);
            int roof = (r + 1) * 2;
            // Hauteur maximale pour que la pointe reste sous le verre
            double inner = p.dome() - 1.5;
            int room = (int) Math.floor(Math.sqrt(Math.max(0, inner * inner - (spireDistance + r) * (spireDistance + r))));
            int maxTop = room - roof - 2;
            int top = Math.max(4, Math.min(maxTop, maxTop - 5 + c.pick(70 + k, 6)));
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
            c.region(sx - r - 1, top + 1, sz - r - 1, sx + r + 1, top + roof, sz + r + 1, (x, y, z) -> {
                double radius = r + 1 - (y - top - 1) / 2.0;
                return radius > 0 && inDisc(x - sx, z - sz, radius) ? BRICK : null;
            });
            c.set(sx, top + roof + 1, sz, GOLD);

            if (k == 1 || k == count / 2 + 1) {
                c.container(sx, 1, sz, chest(Direction.getFacing(towardX, 0, towardZ)), "citadel");
            } else {
                c.set(sx, 0, sz, LIGHT);
            }
        }
    }

    // --- Jardins et lampadaires -----------------------------------------------------------------

    private void gardens() {
        double dome = p.dome();
        for (int k = 0; k < 4; k++) {
            // Décalés des flèches quand celles-ci sont sur les diagonales (4 flèches)
            double angle = Math.PI / 4 + k * Math.PI / 2 + (p.spires() == 4 ? Math.PI / 8 : 0);
            for (double ring : new double[]{dome * 0.5, dome * 0.71}) {
                if (dome < 20 && ring > dome * 0.6) {
                    continue; // trop près du verre dans un petit dôme
                }
                tree((int) Math.round(Math.cos(angle) * ring), (int) Math.round(Math.sin(angle) * ring));
            }
        }
    }

    /** Petit arbre argenté de Gallifrey sur sa pelouse rouge. */
    private void tree(int tx, int tz) {
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
    private void lamps() {
        int dome = p.dome();
        int[] distances = dome >= 30 ? new int[]{10, 20, 34} : new int[]{p.tower() + 3, dome - 4};
        for (int distance : distances) {
            for (int sign : new int[]{-1, 1}) {
                lamp(2, sign * distance);
                lamp(-2, sign * distance);
                lamp(sign * distance, 2);
                lamp(sign * distance, -2);
            }
        }
    }

    private void lamp(int x, int z) {
        c.fill(x, 1, z, x, 3, z, PILLAR);
        c.set(x, 4, z, LIGHT);
    }

    private static BlockState chest(Direction facing) {
        return Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing);
    }
}
