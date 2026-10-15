package fr.doctorwho.worldgen;

import fr.doctorwho.DoctorWhoMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.block.entity.MobSpawnerBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.dynamic.Range;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Heightmap;
import net.minecraft.world.MobSpawnerEntry;
import net.minecraft.world.StructureWorldAccess;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * « Toile » sur laquelle on construit une structure en code.
 * <p>
 * Minecraft génère une grande structure chunk par chunk : la même méthode de construction
 * est appelée une fois par chunk touché, et chaque appel ne doit poser que les blocs de SON chunk.
 * Cette classe s'en charge : tout ce qui sort du chunk en cours est simplement ignoré.
 * <p>
 * Toutes les coordonnées sont <b>relatives au centre</b> de la structure :
 * x = 0, z = 0 au centre, y = 0 au niveau du sol de la structure.
 * Le hasard (ruines, matériaux) vient de {@link #noise} : il donne toujours le même résultat
 * pour un même bloc, quel que soit le chunk en cours, donc les morceaux se raccordent.
 */
public final class StructureCanvas {

    /** Peint un bloc à la position relative (x, y, z), ou renvoie null pour ne rien changer. */
    @FunctionalInterface
    public interface Painter {
        @Nullable
        BlockState paint(int x, int y, int z);
    }

    /** Travaille sur une colonne (x, z) entière. */
    @FunctionalInterface
    public interface ColumnPainter {
        void paint(int x, int z);
    }

    private static final BlockState AIR = Blocks.AIR.getDefaultState();

    private final StructureWorldAccess world;
    private final BlockBox chunkBox;
    private final int cx;
    private final int by;
    private final int cz;
    private final long seed;
    private final BlockPos.Mutable pos = new BlockPos.Mutable();

    public StructureCanvas(StructureWorldAccess world, BlockBox chunkBox, BlockPos center, long seed) {
        this.world = world;
        this.chunkBox = chunkBox;
        this.cx = center.getX();
        this.by = center.getY();
        this.cz = center.getZ();
        this.seed = seed;
    }

    // --- Blocs à l'unité ----------------------------------------------------------------

    public boolean contains(int x, int y, int z) {
        return chunkBox.contains(cx + x, by + y, cz + z);
    }

    public void set(int x, int y, int z, @Nullable BlockState state) {
        if (state != null && contains(x, y, z)) {
            world.setBlockState(pos.set(cx + x, by + y, cz + z), state, Block.NOTIFY_LISTENERS);
        }
    }

    public BlockState get(int x, int y, int z) {
        return world.getBlockState(pos.set(cx + x, by + y, cz + z));
    }

    /** Hauteur relative du premier bloc d'air au-dessus du terrain naturel, en (x, z). */
    public int surfaceY(int x, int z) {
        return world.getTopY(Heightmap.Type.WORLD_SURFACE_WG, cx + x, cz + z) - by;
    }

    // --- Zones ------------------------------------------------------------------------------

    /** Appelle le peintre pour chaque bloc de la boîte (bornes incluses), limitée au chunk en cours. */
    public void region(int x1, int y1, int z1, int x2, int y2, int z2, Painter painter) {
        int minX = Math.max(cx + Math.min(x1, x2), chunkBox.getMinX()) - cx;
        int maxX = Math.min(cx + Math.max(x1, x2), chunkBox.getMaxX()) - cx;
        int minY = Math.max(by + Math.min(y1, y2), chunkBox.getMinY()) - by;
        int maxY = Math.min(by + Math.max(y1, y2), chunkBox.getMaxY()) - by;
        int minZ = Math.max(cz + Math.min(z1, z2), chunkBox.getMinZ()) - cz;
        int maxZ = Math.min(cz + Math.max(z1, z2), chunkBox.getMaxZ()) - cz;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockState state = painter.paint(x, y, z);
                    if (state != null) {
                        world.setBlockState(pos.set(cx + x, by + y, cz + z), state, Block.NOTIFY_LISTENERS);
                    }
                }
            }
        }
    }

    /** Remplit toute la boîte avec un seul bloc. */
    public void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState state) {
        region(x1, y1, z1, x2, y2, z2, (x, y, z) -> state);
    }

    /** Appelle le peintre pour chaque colonne (x, z) du rectangle, limité au chunk en cours. */
    public void columns(int x1, int z1, int x2, int z2, ColumnPainter painter) {
        int minX = Math.max(cx + Math.min(x1, x2), chunkBox.getMinX()) - cx;
        int maxX = Math.min(cx + Math.max(x1, x2), chunkBox.getMaxX()) - cx;
        int minZ = Math.max(cz + Math.min(z1, z2), chunkBox.getMinZ()) - cz;
        int maxZ = Math.min(cz + Math.max(z1, z2), chunkBox.getMaxZ()) - cz;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                painter.paint(x, z);
            }
        }
    }

    /** Fondations : remplit vers le bas depuis {@code topY} tant qu'il y a du vide (air, eau, herbes...). */
    public void foundation(int x, int z, int topY, BlockState state, int maxDepth) {
        if (!contains(x, topY, z)) {
            return;
        }
        for (int y = topY; y > topY - maxDepth; y--) {
            if (!contains(x, y, z)) {
                return;
            }
            BlockState current = get(x, y, z);
            if (!current.isAir() && !current.isReplaceable() && current.getFluidState().isEmpty()) {
                return;
            }
            set(x, y, z, state);
        }
    }

    /** Vide une colonne (terrain qui dépasse dans la structure). */
    public void clear(int x, int z, int y1, int y2) {
        for (int y = y1; y <= y2; y++) {
            if (contains(x, y, z) && !get(x, y, z).isAir()) {
                set(x, y, z, AIR);
            }
        }
    }

    // --- Formes ------------------------------------------------------------------------------

    /** Vrai si (x, z) est dans un disque de rayon r (bord légèrement arrondi). */
    public static boolean inDisc(double x, double z, double r) {
        return x * x + z * z <= r * r + r * 0.8;
    }

    /** Vrai si (x, z) est sur le bord d'un disque de rayon r : un mur d'un bloc, sans trous. */
    public static boolean onRing(int x, int z, double r) {
        return inDisc(x, z, r) && (!inDisc(x + 1, z, r) || !inDisc(x - 1, z, r)
                || !inDisc(x, z + 1, r) || !inDisc(x, z - 1, r));
    }

    public static boolean inSphere(double x, double y, double z, double r) {
        return x * x + y * y + z * z <= r * r + r * 0.8;
    }

    /** Vrai si (x, y, z) est sur la coque d'une sphère de rayon r : étanche, sans trous en diagonale. */
    public static boolean onSphere(int x, int y, int z, double r) {
        return inSphere(x, y, z, r) && (!inSphere(x + 1, y, z, r) || !inSphere(x - 1, y, z, r)
                || !inSphere(x, y + 1, z, r) || !inSphere(x, y - 1, z, r)
                || !inSphere(x, y, z + 1, r) || !inSphere(x, y, z - 1, r));
    }

    /** Numéro du secteur angulaire de (x, z), de 0 à n-1 (pour alterner fenêtres, lampes...). */
    public static int sector(double x, double z, int n) {
        double angle = (Math.atan2(z, x) + Math.PI) / (2 * Math.PI);
        return Math.floorMod((int) Math.floor(angle * n), n);
    }

    // --- Hasard reproductible -------------------------------------------------------------------

    /** Nombre entre 0 et 1, toujours le même pour un même bloc de la même structure. */
    public double noise(int x, int y, int z) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 23) ^ (y * 0xC2B2AE3D27D4EB4FL);
        h = Long.rotateLeft(h, 29) ^ (z * 0x165667B19E3779F9L);
        h ^= h >>> 33;
        h *= 0xFF51AFD7ED558CCDL;
        h ^= h >>> 33;
        h *= 0xC4CEB9FE1A85EC53L;
        h ^= h >>> 33;
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Entier entre 0 et bound-1, tiré une fois pour toute la structure (taille d'une tour...). */
    public int pick(int salt, int bound) {
        return (int) (noise(salt, -4096, salt * 7) * bound);
    }

    // --- Blocs spéciaux ----------------------------------------------------------------------

    /**
     * Cage à monstres. Les règles d'apparition « personnalisées » (toute lumière acceptée)
     * permettent aux gardiens d'apparaître même en plein jour, comme sur leur planète.
     */
    public void spawner(int x, int y, int z, EntityType<?> type, int count, int maxNearby, int minDelay, int maxDelay) {
        if (!contains(x, y, z)) {
            return;
        }
        set(x, y, z, Blocks.SPAWNER.getDefaultState());
        BlockEntity blockEntity = world.getBlockEntity(pos.set(cx + x, by + y, cz + z));
        if (!(blockEntity instanceof MobSpawnerBlockEntity spawner)) {
            return;
        }
        NbtCompound entity = new NbtCompound();
        entity.putString("id", Registries.ENTITY_TYPE.getId(type).toString());
        MobSpawnerEntry entry = new MobSpawnerEntry(entity, Optional.of(
                new MobSpawnerEntry.CustomSpawnRules(new Range<>(0, 15), new Range<>(0, 15))));

        NbtCompound nbt = new NbtCompound();
        MobSpawnerEntry.CODEC.encodeStart(NbtOps.INSTANCE, entry).result()
                .ifPresent(encoded -> nbt.put("SpawnData", encoded));
        nbt.putShort("Delay", (short) 40);
        nbt.putShort("MinSpawnDelay", (short) minDelay);
        nbt.putShort("MaxSpawnDelay", (short) maxDelay);
        nbt.putShort("SpawnCount", (short) count);
        nbt.putShort("MaxNearbyEntities", (short) maxNearby);
        nbt.putShort("RequiredPlayerRange", (short) 16);
        nbt.putShort("SpawnRange", (short) 4);
        spawner.readNbt(nbt);
    }

    /** Coffre (ou tonneau) avec sa table de butin {@code doctorwho:chests/<nom>}. */
    public void container(int x, int y, int z, BlockState state, String lootTable) {
        if (!contains(x, y, z)) {
            return;
        }
        set(x, y, z, state);
        BlockPos at = new BlockPos(cx + x, by + y, cz + z);
        Identifier loot = DoctorWhoMod.id("chests/" + lootTable);
        LootableContainerBlockEntity.setLootTable(world, Random.create(seed ^ at.asLong()), at, loot);
    }

    /** Créature posée à la génération (une seule fois : seulement par le chunk qui la contient). */
    public void mob(double x, int y, double z, EntityType<? extends MobEntity> type) {
        int bx = (int) Math.floor(x);
        int bz = (int) Math.floor(z);
        if (!contains(bx, y, bz)) {
            return;
        }
        MobEntity mob = type.create(world.toServerWorld());
        if (mob == null) {
            return;
        }
        BlockPos at = new BlockPos(cx + bx, by + y, cz + bz);
        mob.refreshPositionAndAngles(cx + x, by + y, cz + z, (float) (noise(bx, y, bz) * 360), 0.0f);
        mob.initialize(world, world.getLocalDifficulty(at), SpawnReason.STRUCTURE, null, null);
        mob.setPersistent();
        world.spawnEntityAndPassengers(mob);
    }
}
