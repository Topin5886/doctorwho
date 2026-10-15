package fr.doctorwho.worldgen;

import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.util.math.Direction;

import static fr.doctorwho.worldgen.StructureCanvas.inDisc;
import static fr.doctorwho.worldgen.StructureCanvas.onRing;
import static fr.doctorwho.worldgen.StructureCanvas.onSphere;
import static fr.doctorwho.worldgen.StructureCanvas.sector;

/**
 * Ville dalek en ruine sur Skaro (environ 70 blocs de large) :
 * <ul>
 *     <li>un grand dôme de bronze (troué par endroits) et son estrade centrale gardée par une cage à Daleks ;</li>
 *     <li>six tours à dôme, dont certaines effondrées, reliées au dôme par des passerelles ;</li>
 *     <li>une esplanade dallée envahie par le sable, bordée de pylônes lumineux ;</li>
 *     <li>des coffres (table de butin {@code doctorwho:chests/dalek_city}).</li>
 * </ul>
 */
final class DalekCityBuilder {

    private static final int RADIUS = 34;
    private static final int DOME = 13;
    private static final int WALL_TOP = 6;
    private static final int TOWER_DISTANCE = 24;
    private static final int TOWERS = 6;
    private static final int CLEAR_HEIGHT = 26;
    private static final int BRIDGE_Y = 9;

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState TILES = Blocks.DEEPSLATE_TILES.getDefaultState();
    private static final BlockState CRACKED = Blocks.CRACKED_DEEPSLATE_TILES.getDefaultState();
    private static final BlockState SAND = ModBlocks.SKARO_SAND.getDefaultState();
    private static final BlockState ROCK = ModBlocks.SKARO_ROCK.getDefaultState();
    private static final BlockState BAND = Blocks.POLISHED_DEEPSLATE.getDefaultState();
    private static final BlockState DARK = Blocks.POLISHED_BLACKSTONE.getDefaultState();
    private static final BlockState RUBBLE = Blocks.COBBLED_DEEPSLATE.getDefaultState();
    private static final BlockState BRONZE = Blocks.WAXED_EXPOSED_CUT_COPPER.getDefaultState();
    private static final BlockState BRONZE_BRIGHT = Blocks.WAXED_CUT_COPPER.getDefaultState();
    private static final BlockState BRONZE_OLD = Blocks.WAXED_WEATHERED_CUT_COPPER.getDefaultState();
    private static final BlockState LIGHT = Blocks.OCHRE_FROGLIGHT.getDefaultState();
    private static final BlockState GLASS = Blocks.ORANGE_STAINED_GLASS.getDefaultState();

    /** Une tour : position, rayon, hauteur, effondrée ou non, et direction vers le centre. */
    private record Tower(int x, int z, int radius, int height, boolean broken, double towardX, double towardZ) {
    }

    private DalekCityBuilder() {
    }

    static void build(StructureCanvas c) {
        ground(c);
        dome(c);
        Tower[] towers = towers(c);
        for (int i = 0; i < towers.length; i++) {
            tower(c, towers[i], i);
        }
        for (int i = 1; i < towers.length; i += 2) {
            bridge(c, towers[i], i);
        }
        pylons(c);
    }

    // --- Esplanade ------------------------------------------------------------------------------

    private static void ground(StructureCanvas c) {
        c.columns(-RADIUS, -RADIUS, RADIUS, RADIUS, (x, z) -> {
            double d = Math.sqrt(x * x + z * z);
            if (d > RADIUS + 0.5) {
                return;
            }
            // Au bord, le dallage disparaît peu à peu sous le désert
            if (d > RADIUS - 5 && c.noise(x, 0, z) < (d - (RADIUS - 5)) / 5.0) {
                return;
            }
            c.foundation(x, z, -1, ROCK, 24);
            c.set(x, 0, z, floor(c, x, z, d));
            c.clear(x, z, 1, CLEAR_HEIGHT);
        });
    }

    private static BlockState floor(StructureCanvas c, int x, int z, double d) {
        // Anneau de lumières encastrées autour du dôme
        if (Math.abs(d - 17) < 0.5 && sector(x, z, 24) % 3 == 0) {
            return LIGHT;
        }
        double n = c.noise(x, 0, z);
        double edge = d / RADIUS;
        if (n < 0.08 + 0.3 * edge * edge) {
            return SAND;
        }
        return n < 0.4 ? CRACKED : TILES;
    }

    /** Bronze plus ou moins oxydé : les vieux murs de la ville. */
    private static BlockState metal(StructureCanvas c, int x, int y, int z) {
        double n = c.noise(x, y, z);
        return n < 0.2 ? BRONZE_OLD : n < 0.35 ? BRONZE_BRIGHT : BRONZE;
    }

    // --- Grand dôme ------------------------------------------------------------------------------

    private static void dome(StructureCanvas c) {
        // Mur circulaire avec quatre portes, fenêtres orange et lampes
        c.region(-DOME - 1, 1, -DOME - 1, DOME + 1, WALL_TOP, DOME + 1, (x, y, z) -> {
            if (!onRing(x, z, DOME)) {
                return null;
            }
            boolean axis = Math.abs(x) <= 1 || Math.abs(z) <= 1;
            if (axis && y <= 3) {
                return AIR;
            }
            if ((Math.abs(x) <= 2 || Math.abs(z) <= 2) && y == 4) {
                return BAND; // linteau au-dessus des portes
            }
            int s = sector(x, z, 32);
            if (y == 1 || y == WALL_TOP) {
                return BAND;
            }
            if (y == 4 && s % 4 == 0) {
                return LIGHT;
            }
            if ((y == 3 || y == 4) && s % 4 == 2) {
                return GLASS;
            }
            return metal(c, x, y, z);
        });

        // Coupole : nervures sombres, trous des ruines, oculus éclairé au sommet
        c.region(-DOME - 1, WALL_TOP + 1, -DOME - 1, DOME + 1, WALL_TOP + DOME + 1, DOME + 1, (x, y, z) -> {
            int dy = y - WALL_TOP;
            if (!onSphere(x, dy, z, DOME)) {
                return null;
            }
            int flat = x * x + z * z;
            if (flat <= 4) {
                return null; // oculus ouvert sur le ciel de Skaro
            }
            if (flat <= 10) {
                return LIGHT;
            }
            if (dy > 5 && c.noise(x, y, z) < 0.16) {
                return null; // trous de la ruine
            }
            if (sector(x, z, 48) % 8 == 0) {
                return BAND;
            }
            return metal(c, x, y, z);
        });

        // Estrade centrale et sa cage à Daleks
        c.region(-3, 1, -3, 3, 2, 3, (x, y, z) -> {
            if (y == 1) {
                return inDisc(x, z, 3) ? DARK : null;
            }
            return inDisc(x, z, 2) ? BAND : null;
        });
        c.spawner(0, 3, 0, ModEntities.DALEK, 2, 3, 300, 700);

        // Lumières au sol, coffres et débris tombés de la coupole
        c.region(-8, 0, -8, 8, 1, 8, (x, y, z) -> {
            double d = Math.sqrt(x * x + z * z);
            if (y == 0) {
                return Math.abs(d - 6.5) < 0.5 && sector(x, z, 8) % 2 == 0 ? LIGHT : null;
            }
            return d > 4 && d < 11 && c.noise(x, 1, z) < 0.04 ? RUBBLE : null;
        });
        c.container(7, 1, 7, chest(Direction.WEST), "dalek_city");
        c.container(-7, 1, -7, chest(Direction.EAST), "dalek_city");
    }

    // --- Tours -----------------------------------------------------------------------------------

    private static Tower[] towers(StructureCanvas c) {
        Tower[] towers = new Tower[TOWERS];
        int offset = c.pick(1, 60);
        for (int i = 0; i < TOWERS; i++) {
            double angle = Math.toRadians(offset + i * (360.0 / TOWERS));
            int x = (int) Math.round(Math.cos(angle) * TOWER_DISTANCE);
            int z = (int) Math.round(Math.sin(angle) * TOWER_DISTANCE);
            double length = Math.sqrt(x * x + z * z);
            towers[i] = new Tower(x, z, 3 + c.pick(10 + i, 2), 15 + c.pick(20 + i, 12),
                    c.pick(30 + i, 3) == 0, -x / length, -z / length);
        }
        return towers;
    }

    private static void tower(StructureCanvas c, Tower t, int index) {
        int r = t.radius();
        int top = t.height();

        c.region(t.x() - r - 1, 1, t.z() - r - 1, t.x() + r + 1, top, t.z() + r + 1, (x, y, z) -> {
            int lx = x - t.x();
            int lz = z - t.z();
            if (!onRing(lx, lz, r)) {
                // Intérieur : on vide aussi ce qui dépasse de la zone déjà dégagée
                return y > CLEAR_HEIGHT && inDisc(lx, lz, r) ? AIR : null;
            }
            if (t.broken() && y > top - 6 && c.noise(x, y, z) < (y - (top - 6)) / 6.0) {
                return AIR; // sommet effondré
            }
            double dot = (lx * t.towardX() + lz * t.towardZ()) / Math.max(0.01, Math.sqrt(lx * lx + lz * lz));
            if (dot > 0.8 && y <= 3) {
                return AIR; // porte tournée vers le dôme
            }
            if (y % 5 == 0) {
                return BAND;
            }
            if (y % 5 == 3 && sector(lx, lz, 12) % 3 == 0) {
                return GLASS;
            }
            return metal(c, x, y, z);
        });

        if (t.broken()) {
            // Débris au pied de la tour effondrée
            c.region(t.x() - r - 5, 1, t.z() - r - 5, t.x() + r + 5, 1, t.z() + r + 5, (x, y, z) -> {
                int lx = x - t.x();
                int lz = z - t.z();
                double d = Math.sqrt(lx * lx + lz * lz);
                if (d <= r + 0.8 || d >= r + 5 || c.noise(x, 7, z) >= 0.15) {
                    return null;
                }
                return c.noise(x, 8, z) < 0.5 ? RUBBLE : metal(c, x, y, z);
            });
        } else {
            // Corniche, petit dôme et flèche lumineuse
            int cap = r + 1;
            c.region(t.x() - cap - 1, top, t.z() - cap - 1, t.x() + cap + 1, top + cap + 1, t.z() + cap + 1, (x, y, z) -> {
                int lx = x - t.x();
                int lz = z - t.z();
                int dy = y - top;
                if (dy == 0) {
                    return inDisc(lx, lz, cap) && !inDisc(lx, lz, r - 1) ? BAND : null;
                }
                if (!onSphere(lx, dy, lz, cap)) {
                    return null;
                }
                return sector(lx, lz, 16) % 4 == 0 ? BAND : metal(c, x, y, z);
            });
            c.fill(t.x(), top + cap + 1, t.z(), t.x(), top + cap + 3, t.z(), BAND);
            c.set(t.x(), top + cap + 4, t.z(), LIGHT);
        }

        // Contenu : cage à Daleks, coffre ou simple lumière
        switch (index % 3) {
            case 0 -> c.spawner(t.x(), 1, t.z(), ModEntities.DALEK, 1, 2, 400, 800);
            case 1 -> c.container(t.x(), 1, t.z(),
                    chest(Direction.getFacing(t.towardX(), 0, t.towardZ())), "dalek_city");
            default -> c.set(t.x(), 0, t.z(), LIGHT);
        }
    }

    // --- Passerelles et pylônes -------------------------------------------------------------------

    /** Passerelle avec parapet entre le dôme et une tour ; certaines sont effondrées au milieu. */
    private static void bridge(StructureCanvas c, Tower t, int index) {
        double length = Math.sqrt(t.x() * t.x() + t.z() * t.z());
        double dx = t.x() / length;
        double dz = t.z() / length;
        double start = DOME - 1;
        double span = length - start;
        boolean collapsed = c.pick(40 + index, 3) == 0;

        int minX = (int) Math.floor(Math.min(dx * start, t.x())) - 3;
        int maxX = (int) Math.ceil(Math.max(dx * start, t.x())) + 3;
        int minZ = (int) Math.floor(Math.min(dz * start, t.z())) - 3;
        int maxZ = (int) Math.ceil(Math.max(dz * start, t.z())) + 3;
        c.region(minX, BRIDGE_Y, minZ, maxX, BRIDGE_Y + 1, maxZ, (x, y, z) -> {
            double along = x * dx + z * dz - start;
            if (along < 0 || along > span) {
                return null;
            }
            double side = Math.abs(x * dz - z * dx);
            if (side > 2.2) {
                return null;
            }
            int lx = x - t.x();
            int lz = z - t.z();
            if (inDisc(lx, lz, t.radius())) {
                return null; // ne pas entrer dans la tour
            }
            if (collapsed && Math.abs(along - span / 2) < 2) {
                return null;
            }
            if (y == BRIDGE_Y) {
                return DARK;
            }
            return side > 1.5 ? BAND : null;
        });
    }

    /** Pylônes autour de l'esplanade, certains brisés et éteints. */
    private static void pylons(StructureCanvas c) {
        int offset = c.pick(2, 30);
        for (int k = 0; k < 12; k++) {
            double angle = Math.toRadians(offset + k * 30);
            int x = (int) Math.round(Math.cos(angle) * 31);
            int z = (int) Math.round(Math.sin(angle) * 31);
            int height = 3 + c.pick(50 + k, 4);
            boolean broken = c.pick(60 + k, 4) == 0;
            if (broken) {
                height = 1 + c.pick(70 + k, 2);
            }
            c.foundation(x, z, 0, ROCK, 12);
            c.fill(x, 1, z, x, height, z, BAND);
            if (!broken) {
                c.set(x, height + 1, z, LIGHT);
            }
        }
    }

    private static BlockState chest(Direction facing) {
        return Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing);
    }
}
