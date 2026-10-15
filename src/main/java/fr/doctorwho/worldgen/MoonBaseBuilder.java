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

import java.util.ArrayList;
import java.util.List;

import static fr.doctorwho.worldgen.StructureCanvas.inDisc;
import static fr.doctorwho.worldgen.StructureCanvas.inSphere;
import static fr.doctorwho.worldgen.StructureCanvas.onSphere;
import static fr.doctorwho.worldgen.StructureCanvas.sector;

/**
 * Bases lunaires envahies par les Cybermen (clin d'œil à « The Moonbase », 1967), en trois tailles :
 * <ul>
 *     <li>avant-poste : dôme de contrôle + laboratoire et salle de conversion ;</li>
 *     <li>base : dôme + 4 modules (labo, dortoir, conversion, entrepôt) reliés par des tubes ;</li>
 *     <li>station : grand dôme, 4 grands modules, hangar des Sontariens et observatoire à coupole de verre.</li>
 * </ul>
 * Antenne, panneaux solaires, aire d'atterrissage, vitres brisées. Butin : {@code doctorwho:chests/moon_base}.
 */
final class MoonBaseBuilder {

    private enum Role { LAB, DORM, CONVERSION, STORAGE }

    /** Un module : position, rôle et direction vers l'extérieur (pour la porte). */
    private record Module(int x, int z, Role role, Direction outward) {
    }

    /**
     * @param hub      rayon du dôme central
     * @param module   rayon des modules
     * @param distance distance entre le centre et chaque module
     * @param roles    rôle des modules nord, est, sud, ouest (null = pas de module)
     * @param annexes  hangar sontarien + observatoire
     */
    private record Params(int hub, int module, int distance, Role[] roles, boolean annexes) {
    }

    private static Params params(StructureSize size) {
        return switch (size) {
            case SMALL -> new Params(6, 4, 12, new Role[]{Role.LAB, null, Role.CONVERSION, null}, false);
            case MEDIUM -> new Params(9, 5, 18, new Role[]{Role.LAB, Role.DORM, Role.CONVERSION, Role.STORAGE}, false);
            case LARGE -> new Params(12, 6, 22, new Role[]{Role.LAB, Role.DORM, Role.CONVERSION, Role.STORAGE}, true);
        };
    }

    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static final BlockState AIR = Blocks.AIR.getDefaultState();
    private static final BlockState ROCK = ModBlocks.MOON_ROCK.getDefaultState();
    private static final BlockState WHITE = Blocks.WHITE_CONCRETE.getDefaultState();
    private static final BlockState GREY = Blocks.LIGHT_GRAY_CONCRETE.getDefaultState();
    private static final BlockState DARK = Blocks.GRAY_CONCRETE.getDefaultState();
    private static final BlockState FLOOR = Blocks.SMOOTH_STONE.getDefaultState();
    private static final BlockState GRATING = ModBlocks.METAL_GRATING.getDefaultState();
    private static final BlockState LIGHT = ModBlocks.HEX_LIGHT.getDefaultState();
    private static final BlockState GLASS = Blocks.GLASS.getDefaultState();

    private final StructureCanvas c;
    private final StructureSize size;
    private final Params p;
    private final List<Module> modules = new ArrayList<>();
    private final int tubeStart;
    private final int tubeEnd;
    private final int padX;
    private final int padZ;
    private final int mastX;
    private final int mastZ;
    private final int panelX0;
    private final int panelX1;
    private final int panelZ0;
    private final int panelZ1;
    /** Annexes de la station : hangar (sud-ouest) et observatoire (nord-est). */
    private final int annex;

    private MoonBaseBuilder(StructureCanvas canvas, StructureSize size) {
        this.c = canvas;
        this.size = size;
        this.p = params(size);
        for (int i = 0; i < 4; i++) {
            Role role = p.roles()[i];
            if (role != null) {
                Direction side = SIDES[i];
                modules.add(new Module(side.getOffsetX() * p.distance(), side.getOffsetZ() * p.distance(), role, side));
            }
        }
        this.tubeStart = p.hub() - 2;
        this.tubeEnd = p.distance() - p.module() + 2;
        double f = p.distance() / 18.0;
        if (p.annexes()) {
            this.padX = 0;
            this.padZ = -(p.distance() + p.module() + 6);
        } else {
            this.padX = (int) Math.round(13 * f);
            this.padZ = (int) Math.round(-13 * f);
        }
        this.mastX = (int) Math.round(12 * f);
        this.mastZ = (int) Math.round(12 * f);
        this.panelX0 = (int) Math.round(-15 * f);
        this.panelX1 = (int) Math.round(-9 * f);
        this.panelZ0 = (int) Math.round(10 * f);
        this.panelZ1 = panelZ0 + 4;
        this.annex = p.distance() + 4;
    }

    static int radius(StructureSize size) {
        Params p = params(size);
        return p.annexes() ? 40 : p.distance() + p.module() + 2;
    }

    static int height(StructureSize size) {
        return params(size).hub() + 4;
    }

    static void build(StructureCanvas canvas, StructureSize size) {
        new MoonBaseBuilder(canvas, size).build();
    }

    private void build() {
        ground();
        hub();
        for (Module m : modules) {
            module(m.x(), m.z(), p.module());
        }
        for (Module m : modules) {
            tube(m);
        }
        for (Module m : modules) {
            switch (m.role()) {
                case LAB -> laboratory(m);
                case DORM -> dormitory(m);
                case CONVERSION -> conversion(m);
                case STORAGE -> storage(m);
            }
        }
        if (p.annexes()) {
            hangar(-annex, annex);
            observatory(annex, -annex);
        }
        outside();
    }

    // --- Sol ----------------------------------------------------------------------------------------

    private void ground() {
        int r = radius(size);
        c.columns(-r, -r, r, r, (x, z) -> {
            BlockState floor = floor(x, z);
            if (floor == null) {
                return;
            }
            c.foundation(x, z, -1, ROCK, 20);
            c.set(x, 0, z, floor);
            c.clear(x, z, 1, p.hub() + 1);
        });
    }

    /** Sol de chaque zone de la base (null = terrain lunaire laissé tel quel). */
    @Nullable
    private BlockState floor(int x, int z) {
        double d = Math.sqrt(x * x + z * z);
        if (inDisc(x, z, p.hub())) {
            return Math.abs(d - (p.hub() - 2.5)) < 0.5 && sector(x, z, 16) % 2 == 0 ? LIGHT : FLOOR;
        }
        for (Module m : modules) {
            if (inDisc(x - m.x(), z - m.z(), p.module())) {
                return m.role() == Role.CONVERSION ? DARK : FLOOR;
            }
        }
        if (p.annexes() && (inDisc(x + annex, z - annex, 8) || inDisc(x - annex, z + annex, 6))) {
            return inDisc(x + annex, z - annex, 8) ? DARK : FLOOR;
        }
        // Tubes : passerelle grillagée, bords blancs
        for (Module m : modules) {
            int along = Math.abs(x * m.outward().getOffsetX() + z * m.outward().getOffsetZ());
            int lateral = Math.abs(x * m.outward().getOffsetZ() - z * m.outward().getOffsetX());
            boolean sameSide = x * m.outward().getOffsetX() + z * m.outward().getOffsetZ() > 0;
            if (sameSide && lateral <= 2 && along >= tubeStart && along <= tubeEnd) {
                return lateral <= 1 ? GRATING : WHITE;
            }
        }
        // Aire d'atterrissage
        int px = x - padX;
        int pz = z - padZ;
        if (inDisc(px, pz, 4.5)) {
            double pd = Math.sqrt(px * px + pz * pz);
            return pd < 0.5 || (Math.abs(pd - 3.5) < 0.5 && sector(px, pz, 8) % 2 == 0) ? LIGHT : GREY;
        }
        // Panneaux solaires et pied de l'antenne
        if (x >= panelX0 && x <= panelX1 && z >= panelZ0 && z <= panelZ1) {
            return GREY;
        }
        if (Math.abs(x - mastX) <= 1 && Math.abs(z - mastZ) <= 1) {
            return GREY;
        }
        return null;
    }

    // --- Dômes ----------------------------------------------------------------------------------------

    /** Dôme central : salle de contrôle avec ses pupitres. */
    private void hub() {
        int hub = p.hub();
        c.region(-hub - 1, 1, -hub - 1, hub + 1, hub + 1, hub + 1, (x, y, z) -> {
            if (!onSphere(x, y, z, hub)) {
                return null;
            }
            if (x * x + z * z <= 4) {
                return GLASS; // verrière au sommet
            }
            if ((y == 2 || y == 3) && sector(x, z, 12) % 2 == 0) {
                return window(x, y, z);
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

    private void module(int mx, int mz, int radius) {
        c.region(mx - radius - 1, 1, mz - radius - 1, mx + radius + 1, radius + 1, mz + radius + 1, (x, y, z) -> {
            int lx = x - mx;
            int lz = z - mz;
            if (!onSphere(lx, y, lz, radius)) {
                return null;
            }
            if (lx == 0 && lz == 0) {
                return LIGHT; // plafonnier
            }
            if (y == 2 && sector(lx, lz, 8) % 2 == 0) {
                return window(x, y, z);
            }
            return y == 1 ? GREY : WHITE;
        });
    }

    /** Vitre... sauf quand les Cybermen l'ont brisée. */
    private BlockState window(int x, int y, int z) {
        return c.noise(x, y, z) < 0.2 ? AIR : GLASS;
    }

    /** Tube de liaison (5 × 5) entre le dôme central et un module ; il perce les deux coques. */
    private void tube(Module m) {
        int dx = m.outward().getOffsetX();
        int dz = m.outward().getOffsetZ();
        int x1 = dx != 0 ? dx * tubeStart : -2;
        int x2 = dx != 0 ? dx * tubeEnd : 2;
        int z1 = dz != 0 ? dz * tubeStart : -2;
        int z2 = dz != 0 ? dz * tubeEnd : 2;
        c.region(x1, 1, z1, x2, 4, z2, (x, y, z) -> {
            int along = Math.abs(x * dx + z * dz);
            int lateral = Math.abs(x * dz - z * dx);
            if (lateral <= 1 && y <= 3) {
                return AIR; // passage
            }
            if (inSphere(x, y, z, p.hub()) || inSphere(x - m.x(), y, z - m.z(), p.module())) {
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

    private void laboratory(Module m) {
        int k = p.module() - 2;
        c.container(m.x() - k, 1, m.z(), chest(Direction.EAST), "moon_base");
        c.set(m.x() + k, 1, m.z(), Blocks.BREWING_STAND.getDefaultState());
        c.set(m.x() - 1, 1, m.z() + 1, Blocks.CRAFTING_TABLE.getDefaultState());
        c.set(m.x() + 1, 1, m.z() + 1, Blocks.CAULDRON.getDefaultState());
        if (p.roles()[1] == null) {
            door(m); // l'avant-poste n'a pas de dortoir : on entre par le labo
        }
    }

    private void dormitory(Module m) {
        int k = p.module() - 3;
        bed(m.x() + k, m.z() - 2, Direction.EAST);
        bed(m.x() + k, m.z() + 2, Direction.EAST);
        if (p.module() >= 6) {
            bed(m.x() - k - 1, m.z() - 2, Direction.WEST);
        }
        c.container(m.x() - 1, 1, m.z() + p.module() - 2, chest(Direction.NORTH), "moon_base");
        door(m);
    }

    /** Salle de conversion : les Cybermen y apparaissent. */
    private void conversion(Module m) {
        int count = p.module() >= 6 ? 3 : 2;
        c.spawner(m.x(), 1, m.z() + 1, ModEntities.CYBERMAN, count, count + 1, 300, 700);
        c.fill(m.x() - 2, 1, m.z() + 2, m.x() - 2, 3, m.z() + 2, Blocks.IRON_BARS.getDefaultState());
        c.fill(m.x() + 2, 1, m.z() + 2, m.x() + 2, 3, m.z() + 2, Blocks.IRON_BARS.getDefaultState());
        c.set(m.x(), 4, m.z() + 1, Blocks.CHAIN.getDefaultState());
    }

    private void storage(Module m) {
        int k = p.module() - 2;
        BlockState barrel = Blocks.BARREL.getDefaultState().with(BarrelBlock.FACING, Direction.UP);
        c.container(m.x() - k, 1, m.z() - 1, barrel, "moon_base");
        c.container(m.x() - k, 1, m.z() + 1, barrel, "moon_base");
        c.set(m.x() - k, 1, m.z(), barrel);
        c.spawner(m.x() + 1, 1, m.z() + 2, ModEntities.CYBERMAN, 1, 2, 400, 800);
        door(m);
    }

    private void bed(int x, int z, Direction facing) {
        BlockState bed = Blocks.WHITE_BED.getDefaultState().with(BedBlock.FACING, facing);
        c.set(x, 1, z, bed.with(BedBlock.PART, BedPart.FOOT));
        c.set(x + facing.getOffsetX(), 1, z + facing.getOffsetZ(), bed.with(BedBlock.PART, BedPart.HEAD));
    }

    /** Porte extérieure (sas ouvert) dans la coque d'un module, côté extérieur. */
    private void door(Module m) {
        int x = m.x() + m.outward().getOffsetX() * p.module();
        int z = m.z() + m.outward().getOffsetZ() * p.module();
        c.fill(x, 1, z, x, 2, z, AIR);
    }

    // --- Annexes de la station -----------------------------------------------------------------------

    /** Hangar des Sontariens : grand dôme sombre, vaisseau de reconnaissance et garnison. */
    private void hangar(int hx, int hz) {
        int r = 8;
        c.region(hx - r - 1, 1, hz - r - 1, hx + r + 1, r + 1, hz + r + 1, (x, y, z) -> {
            int lx = x - hx;
            int lz = z - hz;
            if (inSphere(lx, y, lz, r - 0.6) && !onSphere(lx, y, lz, r)) {
                return AIR;
            }
            if (!onSphere(lx, y, lz, r)) {
                return null;
            }
            // Grande porte tournée vers le dôme central (nord-est)
            if (lx >= 0 && lz <= 0 && Math.abs(lx + lz) <= 2 && y <= 4) {
                return AIR;
            }
            return y == 1 ? DARK : GREY;
        });
        // Vaisseau sontarien : une soucoupe en cuivre posée au centre
        c.region(hx - 3, 1, hz - 3, hx + 3, 2, hz + 3, (x, y, z) -> {
            int lx = x - hx;
            int lz = z - hz;
            if (y == 1) {
                return inDisc(lx, lz, 3) ? Blocks.WAXED_EXPOSED_CUT_COPPER.getDefaultState() : null;
            }
            return inDisc(lx, lz, 1.5) ? Blocks.ORANGE_STAINED_GLASS.getDefaultState() : null;
        });
        c.spawner(hx - 5, 1, hz + 2, ModEntities.SONTARIAN, 2, 3, 300, 700);
        c.container(hx + 2, 1, hz + 5, chest(Direction.NORTH), "moon_base");
        c.set(hx, r - 1, hz, LIGHT);
    }

    /** Observatoire : coupole de verre, télescope (longue-vue en coffre) et pupitre. */
    private void observatory(int ox, int oz) {
        int r = 6;
        c.region(ox - r - 1, 1, oz - r - 1, ox + r + 1, r + 1, oz + r + 1, (x, y, z) -> {
            int lx = x - ox;
            int lz = z - oz;
            if (inSphere(lx, y, lz, r - 0.6) && !onSphere(lx, y, lz, r)) {
                return AIR;
            }
            if (!onSphere(lx, y, lz, r)) {
                return null;
            }
            if (lx <= 0 && lz >= 0 && Math.abs(lx + lz) <= 1 && y <= 2) {
                return AIR; // porte vers le dôme central
            }
            return y <= 1 ? WHITE : GLASS;
        });
        c.fill(ox, 1, oz, ox, 2, oz, GREY);
        c.set(ox, 3, oz, Blocks.LIGHTNING_ROD.getDefaultState());
        c.container(ox + 2, 1, oz - 2, chest(Direction.WEST), "moon_base");
        c.set(ox - 2, 1, oz - 2, Blocks.LECTERN.getDefaultState().with(LecternBlock.FACING, Direction.SOUTH));
    }

    // --- Extérieur ---------------------------------------------------------------------------------------

    private void outside() {
        // Antenne
        c.fill(mastX, 1, mastZ, mastX, 2, mastZ, GREY);
        c.fill(mastX, 3, mastZ, mastX, 9, mastZ, Blocks.CHAIN.getDefaultState());
        c.set(mastX, 10, mastZ, Blocks.LIGHTNING_ROD.getDefaultState());
        c.set(mastX, 0, mastZ, LIGHT);

        // Panneaux solaires (deux rangées)
        int gap = (panelZ0 + panelZ1) / 2;
        c.region(panelX0, 1, panelZ0, panelX1, 1, panelZ1, (x, y, z) -> z == gap ? null : Blocks.DAYLIGHT_DETECTOR.getDefaultState());
    }

    private static BlockState chest(Direction facing) {
        return Blocks.CHEST.getDefaultState().with(ChestBlock.FACING, facing);
    }
}
