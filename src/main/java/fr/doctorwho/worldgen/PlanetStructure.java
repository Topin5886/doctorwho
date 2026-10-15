package fr.doctorwho.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Arrays;
import java.util.Optional;

/**
 * Grande structure d'une planète, construite en code (pas de fichier .nbt) :
 * la ville dalek de Skaro, la Citadelle de Gallifrey ou la base lunaire.
 * <p>
 * Le type est choisi dans le JSON ({@code data/doctorwho/worldgen/structure/*.json}, champ « kind »),
 * la fréquence dans {@code worldgen/structure_set}. On la retrouve avec
 * {@code /locate structure doctorwho:citadel} (ou dalek_city, moon_base).
 */
public class PlanetStructure extends Structure {

    public static final Codec<PlanetStructure> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            configCodecBuilder(instance),
            Kind.CODEC.fieldOf("kind").forGetter(structure -> structure.kind)
    ).apply(instance, PlanetStructure::new));

    /** Les structures disponibles, avec leur rayon et leur hauteur (en blocs). */
    public enum Kind implements StringIdentifiable {
        DALEK_CITY("dalek_city", 35, 40),
        CITADEL("citadel", 41, 44),
        MOON_BASE("moon_base", 24, 14);

        public static final Codec<Kind> CODEC = StringIdentifiable.createCodec(Kind::values);

        private final String id;
        private final int radius;
        private final int height;

        Kind(String id, int radius, int height) {
            this.id = id;
            this.radius = radius;
            this.height = height;
        }

        public int radius() {
            return radius;
        }

        public int height() {
            return height;
        }

        @Override
        public String asString() {
            return id;
        }

        public static Kind byId(String id) {
            return Arrays.stream(values()).filter(kind -> kind.id.equals(id)).findFirst().orElse(DALEK_CITY);
        }
    }

    private final Kind kind;

    public PlanetStructure(Config config, Kind kind) {
        super(config);
        this.kind = kind;
    }

    @Override
    protected Optional<StructurePosition> getStructurePosition(Context context) {
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getCenterX();
        int z = chunk.getCenterZ();
        int y = groundLevel(context, x, z, kind.radius() / 2);

        // Trop bas ou trop haut pour tenir dans le monde : pas de structure ici
        if (y < context.world().getBottomY() + 12 || y + kind.height() >= context.world().getTopY() - 2) {
            return Optional.empty();
        }
        BlockPos center = new BlockPos(x, y, z);
        long seed = context.random().nextLong();
        return Optional.of(new StructurePosition(center,
                collector -> collector.addPiece(new PlanetStructurePiece(kind, center, seed))));
    }

    /** Hauteur du sol : la médiane de 5 points (centre + 4 autour) pour ignorer un pic ou un trou isolé. */
    private static int groundLevel(Context context, int x, int z, int spread) {
        int[] heights = {
                height(context, x, z),
                height(context, x + spread, z),
                height(context, x - spread, z),
                height(context, x, z + spread),
                height(context, x, z - spread)
        };
        Arrays.sort(heights);
        return heights[2];
    }

    private static int height(Context context, int x, int z) {
        return context.chunkGenerator().getHeightInGround(x, z, Heightmap.Type.WORLD_SURFACE_WG,
                context.world(), context.noiseConfig());
    }

    @Override
    public StructureType<?> getType() {
        return ModStructures.PLANET_STRUCTURE;
    }
}
