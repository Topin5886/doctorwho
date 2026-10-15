package fr.doctorwho.worldgen;

import fr.doctorwho.registry.ModEntities;
import fr.doctorwho.registry.ModBlocks;
import net.minecraft.block.BedBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.CropBlock;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Direction;

import static fr.doctorwho.worldgen.StructureCanvas.inDisc;
import static fr.doctorwho.worldgen.StructureCanvas.inSphere;
import static fr.doctorwho.worldgen.StructureCanvas.onRing;

/**
 * Village aux couleurs d'une planète : place centrale (cloche, puits ou monument), maisons de trois
 * tailles tournées vers la place (lit, bloc de métier, villageois), chemins, champs (sous serre sur la Lune),
 * lampadaires et un golem de fer pour protéger les habitants.
 */
final class VillageBuilder {

    /** Rayon occupé par un village. */
    static final int RADIUS = 30;
    static final int HEIGHT = 14;

    /**
     * Blocs d'un village.
     *
     * @param ground     sol naturel de la planète autour des maisons
     * @param greenhouse les champs sont sous une serre de verre (Lune)
     */
    private record Palette(Block wall, Block corner, Block roof, Block floor, Block path, Block ground, Block light,
                           Block glass, boolean greenhouse, Centerpiece centerpiece) {
    }

    /** Ce qui trône au milieu de la place. */
    private enum Centerpiece { WELL, SILVER_TREE, BEACON }

    private static Palette palette(PlanetStructure.Kind kind) {
        return switch (kind) {
            // Gallifrey : murs blancs, poutres d'arbre argenté, toits orange
            case GALLIFREY_VILLAGE -> new Palette(Blocks.CALCITE, ModBlocks.GALLIFREY_LOG, Blocks.ORANGE_TERRACOTTA,
                    Blocks.BIRCH_PLANKS, Blocks.SMOOTH_SANDSTONE, ModBlocks.GALLIFREY_GRASS, Blocks.SEA_LANTERN,
                    Blocks.GLASS, false, Centerpiece.SILVER_TREE);
            // Lune : colonie en modules blancs, serres de verre
            case MOON_VILLAGE -> new Palette(Blocks.WHITE_CONCRETE, Blocks.LIGHT_GRAY_CONCRETE, Blocks.SMOOTH_STONE,
                    Blocks.POLISHED_ANDESITE, Blocks.SMOOTH_STONE, ModBlocks.MOON_DUST, ModBlocks.HEX_LIGHT,
                    Blocks.GLASS, true, Centerpiece.BEACON);
            // Skaro : village de Thals en roche rouge, toits de terre cuite
            default -> new Palette(ModBlocks.SKARO_ROCK, Blocks.POLISHED_BLACKSTONE, Blocks.RED_TERRACOTTA,
                    Blocks.SMOOTH_RED_SANDSTONE, Blocks.TERRACOTTA, ModBlocks.SKARO_SAND, Blocks.OCHRE_FROGLIGHT,
                    Blocks.ORANGE_STAINED_GLASS, false, Centerpiece.WELL);
        };
    }

    private static final Block[] WORKSTATIONS = {Blocks.LECTERN, Blocks.COMPOSTER, Blocks.SMOKER, Blocks.BLAST_FURNACE,
            Blocks.CARTOGRAPHY_TABLE, Blocks.FLETCHING_TABLE, Blocks.BARREL, Blocks.LOOM, Blocks.SMITHING_TABLE,
            Blocks.STONECUTTER, Blocks.BREWING_STAND, Blocks.GRINDSTONE, Blocks.CAULDRON};
    private static final Block[] CROPS = {Blocks.WHEAT, Blocks.CARROTS, Blocks.POTATOES, Blocks.BEETROOTS};

    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    /** Une maison : centre, direction de la porte (vers la place), demi-largeur, demi-profondeur, hauteur des murs. */
    private record House(int x, int z, Direction front, int halfWidth, int halfDepth, int wallHeight) {
    }

    private final StructureCanvas c;
    private final Palette p;
    private final BlockState wall;
    private final BlockState corner;
    private final BlockState roof;
    private final BlockState floor;
    private final BlockState path;
    private final BlockState light;
    private final BlockState glass;

    private VillageBuilder(StructureCanvas canvas, PlanetStructure.Kind kind) {
        this.c = canvas;
        this.p = palette(kind);
        this.wall = p.wall().getDefaultState();
        this.corner = p.corner().getDefaultState();
        this.roof = p.roof().getDefaultState();
        this.floor = p.floor().getDefaultState();
        this.path = p.path().getDefaultState();
        this.light = p.light().getDefaultState();
        this.glass = p.glass().getDefaultState();
    }

    static void build(StructureCanvas canvas, PlanetStructure.Kind kind) {
        new VillageBuilder(canvas, kind).build();
    }

    private void build() {
        ground();
        int houses = 5 + c.pick(3, 3);
        int slots = houses + 2;
        double offset = c.pick(4, 360);
        int farm = 0;
        for (int i = 0; i < slots; i++) {
            double angle = Math.toRadians(offset + i * 360.0 / slots + (c.pick(10 + i, 15) - 7));
            // Deux emplacements (opposés) servent de champs, les autres de maisons
            if (i == 0 || i == slots / 2) {
                field(angle, farm++);
            } else {
                house(angle, i);
            }
        }
        plaza();
        c.mob(3.5, 1, -3.5, EntityType.IRON_GOLEM);
        // Un village sur deux cache un espion : un Zygon ou un Slitheen déguisé en villageois
        if (c.pick(91, 2) == 0) {
            c.mob(-3.5, 1, 3.5, c.pick(92, 2) == 0 ? ModEntities.ZYGON : ModEntities.SLITHEEN);
        }
    }

    // --- Sol, place et chemins ----------------------------------------------------------------------

    private void ground() {
        BlockState naturalGround = p.ground().getDefaultState();
        c.columns(-RADIUS, -RADIUS, RADIUS, RADIUS, (x, z) -> {
            double d = Math.sqrt(x * x + z * z);
            if (d > RADIUS + 0.5 || (d > RADIUS - 4 && c.noise(x, 1, z) < (d - (RADIUS - 4)) / 4.0)) {
                return; // bord irrégulier : on rejoint le terrain naturel
            }
            c.foundation(x, z, -1, naturalGround, 16);
            c.set(x, 0, z, naturalGround);
            c.clear(x, z, 1, HEIGHT);
        });
    }

    private void plaza() {
        c.region(-6, 0, -6, 6, 0, 6, (x, y, z) -> inDisc(x, z, 5.5) ? path : null);
        switch (p.centerpiece()) {
            case WELL -> {
                c.region(-2, 0, -2, 2, 1, 2, (x, y, z) -> {
                    if (!inDisc(x, z, 2)) {
                        return null;
                    }
                    if (onRing(x, z, 2)) {
                        return corner;
                    }
                    return y == 0 ? Blocks.WATER.getDefaultState() : AIR;
                });
            }
            case SILVER_TREE -> {
                c.fill(0, 1, 0, 0, 4, 0, ModBlocks.GALLIFREY_LOG.getDefaultState());
                BlockState leaves = ModBlocks.GALLIFREY_LEAVES.getDefaultState().with(LeavesBlock.PERSISTENT, true);
                c.region(-3, 4, -3, 3, 8, 3, (x, y, z) -> {
                    if (x == 0 && z == 0 && y <= 4) {
                        return null;
                    }
                    return inSphere(x, (y - 6) * 1.3, z, 2.4) ? leaves : null;
                });
            }
            case BEACON -> {
                c.fill(0, 1, 0, 0, 2, 0, corner);
                c.set(0, 3, 0, light);
                c.fill(0, 4, 0, 0, 6, 0, Blocks.CHAIN.getDefaultState());
                c.set(0, 7, 0, Blocks.LIGHTNING_ROD.getDefaultState());
            }
        }
        c.set(0, 1, 3, Blocks.BELL.getDefaultState());
        // Lampadaires aux quatre coins de la place
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) {
                c.fill(sx * 4, 1, sz * 4, sx * 4, 2, sz * 4, corner);
                c.set(sx * 4, 3, sz * 4, light);
            }
        }
    }

    /** Chemin de 2 blocs de large, de la place jusqu'à (x, z). */
    private void pathTo(double tx, double tz) {
        double length = Math.sqrt(tx * tx + tz * tz);
        int minX = (int) Math.floor(Math.min(0, tx)) - 2;
        int maxX = (int) Math.ceil(Math.max(0, tx)) + 2;
        int minZ = (int) Math.floor(Math.min(0, tz)) - 2;
        int maxZ = (int) Math.ceil(Math.max(0, tz)) + 2;
        double dx = tx / length;
        double dz = tz / length;
        c.region(minX, 0, minZ, maxX, 0, maxZ, (x, y, z) -> {
            double along = x * dx + z * dz;
            double side = Math.abs(x * dz - z * dx);
            return along >= 4 && along <= length && side <= 1.0 ? path : null;
        });
    }

    // --- Maisons ------------------------------------------------------------------------------------

    private void house(double angle, int index) {
        int size = c.pick(20 + index, 3); // 0 petite, 1 moyenne, 2 grande
        int halfWidth = 2 + size;
        int halfDepth = 2 + (size + 1) / 2;
        int wallHeight = size == 2 ? 5 : 4;
        double distance = 15 + size * 2 + c.pick(30 + index, 3);
        int hx = (int) Math.round(Math.cos(angle) * distance);
        int hz = (int) Math.round(Math.sin(angle) * distance);
        Direction front = Direction.getFacing(-hx, 0, -hz);
        House h = new House(hx, hz, front, halfWidth, halfDepth, wallHeight);

        double doorX = hx + front.getOffsetX() * (halfDepth + 1);
        double doorZ = hz + front.getOffsetZ() * (halfDepth + 1);
        pathTo(doorX, doorZ);

        // Fondations, sol et murs (fenêtres au milieu des murs, porte face à la place)
        local(h, -halfWidth, halfWidth, -halfDepth, halfDepth, -1, wallHeight, (u, v, y, x, z) -> {
            if (y == -1) {
                c.foundation(x, z, -1, corner, 12);
                return null;
            }
            if (y == 0) {
                return floor;
            }
            boolean sideU = Math.abs(u) == halfWidth;
            boolean sideV = Math.abs(v) == halfDepth;
            if (!sideU && !sideV) {
                return AIR;
            }
            if (sideU && sideV) {
                return corner;
            }
            if (v == halfDepth && u == 0 && y <= 2) {
                return AIR; // porte
            }
            if (y == 2 && ((sideV && Math.abs(u) == halfWidth / 2 + 1) || (sideU && v == 0))) {
                return glass;
            }
            return y == wallHeight ? corner : wall;
        });
        // Toit à quatre pentes avec débord
        int layers = Math.min(halfWidth, halfDepth) + 2;
        for (int k = 0; k < layers; k++) {
            int ru = halfWidth + 1 - k;
            int rv = halfDepth + 1 - k;
            if (ru < 0 || rv < 0) {
                break;
            }
            int y = wallHeight + 1 + k;
            boolean last = ru <= 1 || rv <= 1;
            local(h, -ru, ru, -rv, rv, y, y, (u, v, yy, x, z) ->
                    last || Math.abs(u) == ru || Math.abs(v) == rv ? roof : null);
            if (last) {
                break;
            }
        }
        furnish(h, index);
    }

    /** Lit, bloc de métier, lumière, coffre (grandes maisons) et un villageois. */
    private void furnish(House h, int index) {
        int back = -h.halfDepth() + 1;
        Direction toBack = h.front().getOpposite();
        // Lit le long du mur du fond, la tête contre le mur
        placeLocal(h, -h.halfWidth() + 1, back + 1, 1, Blocks.WHITE_BED.getDefaultState()
                .with(BedBlock.FACING, toBack).with(BedBlock.PART, BedPart.FOOT));
        placeLocal(h, -h.halfWidth() + 1, back, 1, Blocks.WHITE_BED.getDefaultState()
                .with(BedBlock.FACING, toBack).with(BedBlock.PART, BedPart.HEAD));
        placeLocal(h, h.halfWidth() - 1, back, 1, WORKSTATIONS[(index + c.pick(5, 13)) % WORKSTATIONS.length].getDefaultState());
        placeLocal(h, h.halfWidth() - 1, h.halfDepth() - 1, 1, Blocks.LANTERN.getDefaultState());
        placeLocal(h, 0, 0, h.wallHeight(), light);
        if (h.halfWidth() >= 4) {
            int[] at = toWorld(h, 0, back);
            c.container(at[0], 1, at[1], Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, h.front()), "village");
        }
        c.mob(h.x() + 0.5, 1, h.z() + 0.5, EntityType.VILLAGER);
    }

    // --- Champs -------------------------------------------------------------------------------------

    /** Champ de 9 × 7 avec un canal d'eau au milieu ; sur la Lune, il est sous une serre de verre. */
    private void field(double angle, int index) {
        double distance = 19;
        int fx = (int) Math.round(Math.cos(angle) * distance);
        int fz = (int) Math.round(Math.sin(angle) * distance);
        Direction front = Direction.getFacing(-fx, 0, -fz);
        House f = new House(fx, fz, front, 4, 3, 3);
        BlockState crop = CROPS[(index + c.pick(6, 4)) % CROPS.length].getDefaultState();
        BlockState border = corner;
        pathTo(fx + front.getOffsetX() * 5, fz + front.getOffsetZ() * 5);

        local(f, -5, 5, -4, 4, -1, 4, (u, v, y, x, z) -> {
            boolean edge = Math.abs(u) == 5 || Math.abs(v) == 4;
            if (y == -1) {
                c.foundation(x, z, -1, Blocks.DIRT.getDefaultState(), 12);
                return null;
            }
            if (y == 0) {
                if (edge) {
                    return border;
                }
                return v == 0 ? Blocks.WATER.getDefaultState() : Blocks.FARMLAND.getDefaultState();
            }
            if (y == 1 && !edge && v != 0) {
                int age = Math.min(7, c.pick(40 + u * 7 + v, 8));
                return crop.getBlock() instanceof CropBlock cropBlock ? cropBlock.withAge(Math.min(age, cropBlock.getMaxAge())) : crop;
            }
            if (p.greenhouse() && edge) {
                if (v == 4 && Math.abs(u) <= 1 && y <= 2) {
                    return AIR; // entrée de la serre
                }
                return y == 4 ? glass : (Math.abs(u) == 5 && Math.abs(v) == 4 ? border : glass);
            }
            if (p.greenhouse() && y == 4) {
                return glass;
            }
            return null;
        });
        if (p.greenhouse()) {
            placeLocal(f, 0, 0, 4, light);
        }
    }

    // --- Repère local d'une maison --------------------------------------------------------------------

    /** Peintre en coordonnées locales : u = côté, v = profondeur (v positif = façade), et coordonnées monde. */
    @FunctionalInterface
    private interface LocalPainter {
        BlockState paint(int u, int v, int y, int x, int z);
    }

    private void local(House h, int u0, int u1, int v0, int v1, int y0, int y1, LocalPainter painter) {
        int fx = h.front().getOffsetX();
        int fz = h.front().getOffsetZ();
        int reach = Math.max(Math.max(Math.abs(u0), Math.abs(u1)), Math.max(Math.abs(v0), Math.abs(v1)));
        c.region(h.x() - reach, y0, h.z() - reach, h.x() + reach, y1, h.z() + reach, (x, y, z) -> {
            int dx = x - h.x();
            int dz = z - h.z();
            int v = dx * fx + dz * fz;
            int u = dx * -fz + dz * fx;
            if (u < u0 || u > u1 || v < v0 || v > v1) {
                return null;
            }
            return painter.paint(u, v, y, x, z);
        });
    }

    private int[] toWorld(House h, int u, int v) {
        int fx = h.front().getOffsetX();
        int fz = h.front().getOffsetZ();
        return new int[]{h.x() + v * fx - u * fz, h.z() + v * fz + u * fx};
    }

    private void placeLocal(House h, int u, int v, int y, BlockState state) {
        int[] at = toWorld(h, u, v);
        c.set(at[0], y, at[1], state);
    }
}
