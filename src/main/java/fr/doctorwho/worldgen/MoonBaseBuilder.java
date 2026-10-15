package fr.doctorwho.worldgen;

import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.registry.ModEntities;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.enums.BedPart;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import static fr.doctorwho.worldgen.StructureCanvas.inDisc;
import static fr.doctorwho.worldgen.StructureCanvas.inSphere;
import static fr.doctorwho.worldgen.StructureCanvas.onSphere;
import static fr.doctorwho.worldgen.StructureCanvas.sector;

/**
 * Base lunaire envahie par les Cybermen (clin d'œil à « The Moonbase », 1967) :
 * <ul>
 *     <li>un dôme central (salle de contrôle) relié par des tubes à quatre modules :
 *     laboratoire (nord), dortoir (est), salle de conversion des Cybermen (sud), entrepôt (ouest) ;</li>
 *     <li>une antenne, des panneaux solaires et une aire d'atterrissage ;</li>
 *     <li>des vitres brisées, deux cages à Cybermen et du butin ({@code doctorwho:chests/moon_base}).</li>
 * </ul>
 * Entrées : les portes extérieures des modules est et ouest (ou une vitre brisée !).
 */
final class MoonBaseBuilder {

    private static final int HUB = 9;
    private static final int MODULE = 5;
    private static final int MODULE_DISTANCE = 18;
    private static final int TUBE_START = HUB - 2;
    private static final int TUBE_END = MODULE_DISTANCE - MODULE + 2;

    /** Les quatre modules, dans l'ordre nord, est, sud, ouest. */
    private static final int[][] MODULES = {{0, -MODULE_DISTANCE}, {MODULE_DISTANCE, 0}, {0, MODULE_DISTANCE}, {-MODULE_DISTANCE, 0}};

    private static final int PAD_X = 13;
    private static final int PAD_Z = -13;
    private static final int MAST_X = 12;
    private static final int MAST_Z = 12;

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState ROCK = ModBlocks.MOON_ROCK.getDefaultState();
    private static final BlockState WHITE = Blocks.WHITE_CONCRETE.getDefaultState();
    private static final BlockState GREY = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
    private static final BlockState DARK = Blocks.GRAY_CONCRETE.getDefaultState();
    private static final BlockState FLOOR = Blocks.SMOOTH_STONE.getDefaultState();
    private static final BlockState GRATING = ModBlocks.METAL_GRATING.getDefaultState();
    private static final BlockState LIGHT = ModBlocks.HEX_LIGHT.getDefaultState();
    private static final BlockState GLASS = Blocks.GLASS.getDefaultState();

    private MoonBaseBuilder() {
    }

    static void build(StructureCanvas c) {
        ground(c);
        hub(c);
        for (int[] module : MODULES) {
            module(c, module[0], module[1]);
        }
        for (int[] module : MODULES) {
            tube(c, Integer.signum(module[0]), Integer.signum(module[1]));
        }
        laboratory(c, 0, -MODULE_DISTANCE);
        dormitory(c, MODULE_DISTANCE, 0);
        conversion(c, 0, MODULE_DISTANCE);
        storage(c, -MODULE_DISTANCE, 0);
        outside(c);
    }

    // --- Sol ----------------------------------------------------------------------------------------

    private static void ground(StructureCanvas c) {
        int r = MODULE_DISTANCE + MODULE + 1;
        c.columns(-r, -r, r, r, (x, z) -> {
            BlockState floor = floor(x, z);
            if (floor == null) {
                return;
            }
            c.foundation(x, z, -1, ROCK, 20);
            c.set(x, 0, z, floor);
            c.clear(x, z, 1, inDisc(x, z, HUB) ? HUB + 1 : MODULE + 1);
        });
    }

    /** Sol de chaque zone de la base (null = terrain lunaire laissé tel quel). */
    @Nullable
    private static BlockState floor(int x, int z) {
        double d = Math.sqrt(x * x + z * z);
        if (inDisc(x, z, HUB)) {
            return Math.abs(d - 6.5) < 0.5 && sector(x, z, 16) % 2 == 0 ? LIGHT : FLOOR;
        }
        for (int[] module : MODULES) {
            if (inDisc(x - module[0], z - module[1], MODULE)) {
                return module[1] == MODULE_DISTANCE ? DARK : FLOOR;
            }
        }
        // Tubes : passerelle grillagée, bords blancs
        int along = Math.max(Math.abs(x), Math.abs(z));
        int lateral = Math.min(Math.abs(x), Math.abs(z));
        if (lateral <= 2 && along >= TUBE_START && along <= TUBE_END) {
            return lateral <= 1 ? GRATING : WHITE;
        }
        // Aire d'atterrissage
        int px = x - PAD_X;
        int pz = z - PAD_Z;
        if (inDisc(px, pz, 4.5)) {
            double pd = Math.sqrt(px * px + pz * pz);
            return pd < 0.5 || (Math.abs(pd - 3.5) < 0.5 && sector(px, pz, 8) % 2 == 0) ? LIGHT : GREY;
        }
        // Panneaux solaires et pied de l'antenne
        if (x >= -15 && x <= -9 && z >= 10 && z <= 14) {
            return GREY;
        }
        if (Math.abs(x - MAST_X) <= 1 && Math.abs(z - MAST_Z) <= 1) {
            return GREY;
        }
        return null;
    }

    // --- Dômes ----------------------------------------------------------------------------------------

    /** Dôme central : salle de contrôle avec ses pupitres. */
    private static void hub(StructureCanvas c) {
        c.region(-HUB - 1, 1, -HUB - 1, HUB + 1, HUB + 1, HUB + 1, (x, y, z) -> {
            if (!onSphere(x, y, z, HUB)) {
                return null;
            }
            if (x * x + z * z <= 4) {
                return GLASS; // verrière au sommet
            }
            if ((y == 2 || y == 3) && sector(x, z, 12) % 2 == 0) {
                return window(c, x, y, z);
            }
            return y == 1 ? GREY : WHITE;
        });
        // Colonne centrale et quatre pupitres
        c.fill(0, 1, 0, 0, 2, 0, GREY);
        c.set(0, 3, 0, LIGHT);
        for (Direction facing : Direction.Type.HORIZONTAL) {
            c.set(facing.getOffsetX() * 2, 1, facing.getOffsetZ() * 2,
                    Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, facing));
        }
    }

    private static void module(StructureCanvas c, int mx, int mz) {
        c.region(mx - MODULE - 1, 1, mz - MODULE - 1, mx + MODULE + 1, MODULE + 1, mz + MODULE + 1, (x, y, z) -> {
            int lx = x - mx;
            int lz = z - mz;
            if (!onSphere(lx, y, lz, MODULE)) {
                return null;
            }
            if (lx == 0 && lz == 0) {
                return LIGHT; // plafonnier
            }
            if (y == 2 && sector(lx, lz, 8) % 2 == 0) {
                return window(c, x, y, z);
            }
            return y == 1 ? GREY : WHITE;
        });
    }

    /** Vitre... sauf quand les Cybermen l'ont brisée. */
    private static BlockState window(StructureCanvas c, int x, int y, int z) {
        return c.noise(x, y, z) < 0.2 ? AIR : GLASS;
    }

    /** Tube de liaison (5 × 5) entre le dôme central et un module ; il perce les deux coques. */
    private static void tube(StructureCanvas c, int dx, int dz) {
        int mx = dx * MODULE_DISTANCE;
        int mz = dz * MODULE_DISTANCE;
        int x1 = dx != 0 ? dx * TUBE_START : -2;
        int x2 = dx != 0 ? dx * TUBE_END : 2;
        int z1 = dz != 0 ? dz * TUBE_START : -2;
        int z2 = dz != 0 ? dz * TUBE_END : 2;
        c.region(x1, 1, z1, x2, 4, z2, (x, y, z) -> {
            int along = Math.abs(x * dx + z * dz);
            int lateral = Math.abs(x * dz - z * dx);
            if (lateral <= 1 && y <= 3) {
                return AIR; // passage
            }
            if (inSphere(x, y, z, HUB) || inSphere(x - mx, y, z - mz, MODULE)) {
                return null; // déjà dans un dôme
            }
            if (y == 4 && lateral == 0 && along % 4 == 0) {
                return LIGHT;
            }
            if (lateral == 2 && y == 2 && along % 3 == 0) {
                return GLASS;
            }
            return WHITE;
        });
    }

    // --- Modules ----------------------------------------------------------------------------------------

    private static void laboratory(StructureCanvas c, int mx, int mz) {
        c.container(mx - 3, 1, mz, chest(Direction.EAST), "moon_base");
        c.set(mx + 3, 1, mz, Blocks.BREWING_STAND.getDefaultState());
        c.set(mx, 1, mz - 3, Blocks.CRAFTING_TABLE.getDefaultState());
        c.set(mx + 2, 1, mz - 2, Blocks.CAULDRON.getDefaultState());
    }

    private static void dormitory(StructureCanvas c, int mx, int mz) {
        bed(c, mx + 2, mz - 2, Direction.EAST);
        bed(c, mx + 2, mz + 2, Direction.EAST);
        c.container(mx - 1, 1, mz + 3, chest(Direction.NORTH), "moon_base");
        door(c, mx + MODULE, mz);
    }

    /** Salle de conversion : les Cybermen y apparaissent. */
    private static void conversion(StructureCanvas c, int mx, int mz) {
        c.spawner(mx, 1, mz + 1, ModEntities.CYBERMAN, 2, 3, 300, 700);
        c.fill(mx - 2, 1, mz + 2, mx - 2, 3, mz + 2, Blocks.IRON_BARS.getDefaultState());
        c.fill(mx + 2, 1, mz + 2, mx + 2, 3, mz + 2, Blocks.IRON_BARS.getDefaultState());
        c.set(mx, 4, mz + 1, Blocks.CHAIN.getDefaultState());
    }

    private static void storage(StructureCanvas c, int mx, int mz) {
        BlockState barrel = Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.UP);
        c.container(mx - 3, 1, mz - 1, barrel, "moon_base");
        c.container(mx - 3, 1, mz + 1, barrel, "moon_base");
        c.set(mx - 3, 1, mz, barrel);
        c.spawner(mx + 1, 1, mz + 2, ModEntities.CYBERMAN, 1, 2, 400, 800);
        door(c, mx - MODULE, mz);
    }

    private static void bed(StructureCanvas c, int x, int z, Direction facing) {
        BlockState bed = Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, facing);
        c.set(x, 1, z, bed.with(BedBlock.PART, BedPart.FOOT));
        c.set(x + facing.getOffsetX(), 1, z + facing.getOffsetZ(), bed.with(BedBlock.PART, BedPart.HEAD));
    }

    /** Porte extérieure (sas ouvert) dans la coque d'un module. */
    private static void door(StructureCanvas c, int x, int z) {
        c.fill(x, 1, z, x, 2, z, AIR);
    }

    // --- Extérieur ---------------------------------------------------------------------------------------

    private static void outside(StructureCanvas c) {
        // Antenne
        c.fill(MAST_X, 1, MAST_Z, MAST_X, 2, MAST_Z, GREY);
        c.fill(MAST_X, 3, MAST_Z, MAST_X, 9, MAST_Z, Blocks.CHAIN.getDefaultState());
        c.set(MAST_X, 10, MAST_Z, Blocks.LIGHTNING_ROD.getDefaultState());
        c.set(MAST_X, 0, MAST_Z, LIGHT);

        // Panneaux solaires
        c.region(-15, 1, 10, -9, 1, 14, (x, y, z) -> z == 12 ? null : Blocks.DAYLIGHT_DETECTOR.getDefaultState());
    }

    private static BlockState chest(Direction facing) {
        return Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing);
    }
}
