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
 * Bâtiments daleks en ruine sur Skaro, en trois tailles :
 * <ul>
 *     <li>avant-poste (~40 blocs) : petit dôme, 3 tours, une cage à Daleks ;</li>
 *     <li>ville (~70 blocs) : grand dôme, 6 tours, passerelles, 3 cages ;</li>
 *     <li>capitale (~105 blocs) : immense dôme, 9 tours, et une tour-prison où des Ood attendent qu'on les libère.</li>
 * </ul>
 * Coffres : table de butin {@code doctorwho:chests/dalek_city}.
 */
final class DalekCityBuilder {

    /** Dimensions d'une taille de ville. */
    private record Params(int radius, int dome, int wallTop, int towerDistance, int towers, int towerMinHeight,
                          int towerExtraHeight, int pylonRadius, int pylons, int domeDaleks, boolean prison) {
    }

    private static Params params(StructureSize size) {
        return switch (size) {
            case SMALL -> new Params(18, 8, 4, 13, 3, 10, 6, 16, 6, 1, false);
            case MEDIUM -> new Params(34, 13, 6, 24, 6, 15, 12, 31, 12, 2, false);
            case LARGE -> new Params(52, 20, 8, 37, 9, 20, 14, 49, 18, 3, true);
        };
    }

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
    private static final BlockState BARS = Blocks.IRON_BARS.getDefaultState();

    /** Une tour : position, rayon, hauteur, effondrée ou non, et direction vers le centre. */
    private record Tower(int x, int z, int radius, int height, boolean broken, double towardX, double towardZ) {
    }

    private final StructureCanvas c;
    private final Params p;
    private final int clearHeight;
    private final int bridgeY;

    private DalekCityBuilder(StructureCanvas canvas, StructureSize size) {
        this.c = canvas;
        this.p = params(size);
        this.clearHeight = p.wallTop() + p.dome() + 6;
        this.bridgeY = p.wallTop() + 3;
    }

    /** Rayon et hauteur occupés par cette taille (pour la boîte de la structure). */
    static int radius(StructureSize size) {
        return params(size).radius() + 1;
    }

    static int height(StructureSize size) {
        Params p = params(size);
        return p.towerMinHeight() + p.towerExtraHeight() + 12;
    }

    static void build(StructureCanvas canvas, StructureSize size) {
        new DalekCityBuilder(canvas, size).build();
    }

    private void build() {
        ground();
        dome();
        Tower[] towers = towers();
        for (int i = 0; i < towers.length; i++) {
            tower(towers[i], i);
        }
        for (int i = 1; i < towers.length; i += 2) {
            bridge(towers[i], i);
        }
        pylons();
    }

    // --- Esplanade ------------------------------------------------------------------------------

    private void ground() {
        int r = p.radius();
        c.columns(-r, -r, r, r, (x, z) -> {
            double d = Math.sqrt(x * x + z * z);
            if (d > r + 0.5) {
                return;
            }
            // Au bord, le dallage disparaît peu à peu sous le désert
            if (d > r - 5 && c.noise(x, 0, z) < (d - (r - 5)) / 5.0) {
                return;
            }
            c.foundation(x, z, -1, ROCK, 24);
            c.set(x, 0, z, floor(x, z, d));
            c.clear(x, z, 1, clearHeight);
        });
    }

    private BlockState floor(int x, int z, double d) {
        // Anneau de lumières encastrées autour du dôme
        if (Math.abs(d - (p.dome() + 4)) < 0.5 && sector(x, z, 24) % 3 == 0) {
            return LIGHT;
        }
        double n = c.noise(x, 0, z);
        double edge = d / p.radius();
        if (n < 0.08 + 0.3 * edge * edge) {
            return SAND;
        }
        return n < 0.4 ? CRACKED : TILES;
    }

    /** Bronze plus ou moins oxydé : les vieux murs de la ville. */
    private BlockState metal(int x, int y, int z) {
        double n = c.noise(x, y, z);
        return n < 0.2 ? BRONZE_OLD : n < 0.35 ? BRONZE_BRIGHT : BRONZE;
    }

    // --- Grand dôme ------------------------------------------------------------------------------

    private void dome() {
        int dome = p.dome();
        int wallTop = p.wallTop();
        // Mur circulaire avec quatre portes, fenêtres orange et lampes
        c.region(-dome - 1, 1, -dome - 1, dome + 1, wallTop, dome + 1, (x, y, z) -> {
            if (!onRing(x, z, dome)) {
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
            if (y == 1 || y == wallTop) {
                return BAND;
            }
            if (y == 4 && s % 4 == 0) {
                return LIGHT;
            }
            if ((y == 3 || y == 4) && s % 4 == 2) {
                return GLASS;
            }
            return metal(x, y, z);
        });

        // Coupole : nervures sombres, trous des ruines, oculus éclairé au sommet
        int oculus = Math.max(1, dome / 6);
        c.region(-dome - 1, wallTop + 1, -dome - 1, dome + 1, wallTop + dome + 1, dome + 1, (x, y, z) -> {
            int dy = y - wallTop;
            if (!onSphere(x, dy, z, dome)) {
                return null;
            }
            int flat = x * x + z * z;
            if (flat <= oculus * oculus) {
                return null; // oculus ouvert sur le ciel de Skaro
            }
            if (flat <= (oculus + 1) * (oculus + 1) + 1) {
                return LIGHT;
            }
            if (dy > 5 && c.noise(x, y, z) < 0.16) {
                return null; // trous de la ruine
            }
            if (sector(x, z, 48) % 8 == 0) {
                return BAND;
            }
            return metal(x, y, z);
        });

        // Estrade centrale et sa cage à Daleks
        int dais = Math.max(2, dome / 4);
        c.region(-dais, 1, -dais, dais, 2, dais, (x, y, z) -> {
            if (y == 1) {
                return inDisc(x, z, dais) ? DARK : null;
            }
            return inDisc(x, z, dais - 1) ? BAND : null;
        });
        c.spawner(0, 3, 0, ModEntities.DALEK, p.domeDaleks(), p.domeDaleks() + 1, 300, 700);

        // Lumières au sol, coffres et débris tombés de la coupole
        int ring = dome / 2;
        c.region(-dome, 0, -dome, dome, 1, dome, (x, y, z) -> {
            double d = Math.sqrt(x * x + z * z);
            if (y == 0) {
                return Math.abs(d - ring) < 0.5 && sector(x, z, 8) % 2 == 0 ? LIGHT : null;
            }
            return d > dais + 1 && d < dome - 2 && c.noise(x, 1, z) < 0.04 ? RUBBLE : null;
        });
        int k = Math.max(3, dome / 2);
        c.container(k, 1, k, chest(Direction.WEST), "dalek_city");
        c.container(-k, 1, -k, chest(Direction.EAST), "dalek_city");
    }

    // --- Tours -----------------------------------------------------------------------------------

    private Tower[] towers() {
        Tower[] towers = new Tower[p.towers()];
        int offset = c.pick(1, 60);
        for (int i = 0; i < towers.length; i++) {
            double angle = Math.toRadians(offset + i * (360.0 / towers.length));
            int x = (int) Math.round(Math.cos(angle) * p.towerDistance());
            int z = (int) Math.round(Math.sin(angle) * p.towerDistance());
            double length = Math.sqrt(x * x + z * z);
            boolean bigCity = p.towers() > 6;
            int radius = 3 + c.pick(10 + i, bigCity ? 3 : 2);
            towers[i] = new Tower(x, z, radius, p.towerMinHeight() + c.pick(20 + i, p.towerExtraHeight()),
                    c.pick(30 + i, 3) == 0, -x / length, -z / length);
        }
        return towers;
    }

    private void tower(Tower t, int index) {
        int r = t.radius();
        int top = t.height();
        // Dans la capitale, une tour sert de prison : des Ood enfermés derrière des barreaux
        boolean prison = p.prison() && index == 2;

        c.region(t.x() - r - 1, 1, t.z() - r - 1, t.x() + r + 1, top, t.z() + r + 1, (x, y, z) -> {
            int lx = x - t.x();
            int lz = z - t.z();
            if (!onRing(lx, lz, r)) {
                // Intérieur : on vide aussi ce qui dépasse de la zone déjà dégagée
                return y > clearHeight && inDisc(lx, lz, r) ? AIR : null;
            }
            if (!prison && t.broken() && y > top - 6 && c.noise(x, y, z) < (y - (top - 6)) / 6.0) {
                return AIR; // sommet effondré
            }
            double dot = (lx * t.towardX() + lz * t.towardZ()) / Math.max(0.01, Math.sqrt(lx * lx + lz * lz));
            if (dot > 0.8 && y <= 3) {
                return prison ? BARS : AIR; // porte tournée vers le dôme (barreaux pour la prison)
            }
            if (y % 5 == 0) {
                return BAND;
            }
            if (y % 5 == 3 && sector(lx, lz, 12) % 3 == 0) {
                return prison ? BARS : GLASS;
            }
            return metal(x, y, z);
        });

        if (t.broken() && !prison) {
            // Débris au pied de la tour effondrée
            c.region(t.x() - r - 5, 1, t.z() - r - 5, t.x() + r + 5, 1, t.z() + r + 5, (x, y, z) -> {
                int lx = x - t.x();
                int lz = z - t.z();
                double d = Math.sqrt(lx * lx + lz * lz);
                if (d <= r + 0.8 || d >= r + 5 || c.noise(x, 7, z) >= 0.15) {
                    return null;
                }
                return c.noise(x, 8, z) < 0.5 ? RUBBLE : metal(x, y, z);
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
                return sector(lx, lz, 16) % 4 == 0 ? BAND : metal(x, y, z);
            });
            c.fill(t.x(), top + cap + 1, t.z(), t.x(), top + cap + 3, t.z(), BAND);
            c.set(t.x(), top + cap + 4, t.z(), LIGHT);
        }

        if (prison) {
            // Les prisonniers (pacifiques) et un garde dalek à l'intérieur
            c.set(t.x(), 0, t.z(), LIGHT);
            c.mob(t.x() + 0.5, 1, t.z() + 1.5, ModEntities.OOD);
            c.mob(t.x() - 0.5, 1, t.z() - 0.5, ModEntities.OOD);
            c.spawner(t.x() + 1, 1, t.z() - 1, ModEntities.DALEK, 1, 2, 400, 800);
            return;
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
    private void bridge(Tower t, int index) {
        double length = Math.sqrt(t.x() * t.x() + t.z() * t.z());
        double dx = t.x() / length;
        double dz = t.z() / length;
        double start = p.dome() - 1;
        double span = length - start;
        boolean collapsed = c.pick(40 + index, 3) == 0;
        int y0 = bridgeY;

        int minX = (int) Math.floor(Math.min(dx * start, t.x())) - 3;
        int maxX = (int) Math.ceil(Math.max(dx * start, t.x())) + 3;
        int minZ = (int) Math.floor(Math.min(dz * start, t.z())) - 3;
        int maxZ = (int) Math.ceil(Math.max(dz * start, t.z())) + 3;
        c.region(minX, y0, minZ, maxX, y0 + 1, maxZ, (x, y, z) -> {
            double along = x * dx + z * dz - start;
            if (along < 0 || along > span) {
                return null;
            }
            double side = Math.abs(x * dz - z * dx);
            if (side > 2.2) {
                return null;
            }
            if (inDisc(x - t.x(), z - t.z(), t.radius())) {
                return null; // ne pas entrer dans la tour
            }
            if (collapsed && Math.abs(along - span / 2) < 2) {
                return null;
            }
            if (y == y0) {
                return DARK;
            }
            return side > 1.5 ? BAND : null;
        });
    }

    /** Pylônes autour de l'esplanade, certains brisés et éteints. */
    private void pylons() {
        int offset = c.pick(2, 30);
        for (int k = 0; k < p.pylons(); k++) {
            double angle = Math.toRadians(offset + k * (360.0 / p.pylons()));
            int x = (int) Math.round(Math.cos(angle) * p.pylonRadius());
            int z = (int) Math.round(Math.sin(angle) * p.pylonRadius());
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
