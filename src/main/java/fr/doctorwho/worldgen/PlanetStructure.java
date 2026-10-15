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
 * bâtiments daleks de Skaro, cités des Seigneurs du Temps sur Gallifrey, bases lunaires (trois tailles chacun).
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

    /** Les familles de bâtiments (un constructeur chacune). */
    public enum Family { DALEK, CITADEL, MOON_BASE, VILLAGE, GOLEM_LAIR }

    /** Les structures disponibles : famille, taille, rayon et hauteur (en blocs). */
    public enum Kind implements StringIdentifiable {
        DALEK_OUTPOST("dalek_outpost", Family.DALEK, StructureSize.SMALL),
        DALEK_CITY("dalek_city", Family.DALEK, StructureSize.MEDIUM),
        DALEK_CAPITAL("dalek_capital", Family.DALEK, StructureSize.LARGE),
        TIMELORD_HOUSE("timelord_house", Family.CITADEL, StructureSize.SMALL),
        ARCADIA("arcadia", Family.CITADEL, StructureSize.MEDIUM),
        CITADEL("citadel", Family.CITADEL, StructureSize.LARGE),
        MOON_OUTPOST("moon_outpost", Family.MOON_BASE, StructureSize.SMALL),
        MOON_BASE("moon_base", Family.MOON_BASE, StructureSize.MEDIUM),
        MOON_STATION("moon_station", Family.MOON_BASE, StructureSize.LARGE),
        SKARO_VILLAGE("skaro_village", Family.VILLAGE, StructureSize.MEDIUM),
        GALLIFREY_VILLAGE("gallifrey_village", Family.VILLAGE, StructureSize.MEDIUM),
        MOON_VILLAGE("moon_village", Family.VILLAGE, StructureSize.MEDIUM),
        GOLEM_LAIR("golem_lair", Family.GOLEM_LAIR, StructureSize.MEDIUM);

        public static final Codec<Kind> CODEC = StringIdentifiable.createCodec(Kind::values);

        private final String id;
        private final Family family;
        private final StructureSize size;

        Kind(String id, Family family, StructureSize size) {
            this.id = id;
            this.family = family;
            this.size = size;
        }

        public Family family() {
            return family;
        }

        public StructureSize size() {
            return size;
        }

        public int radius() {
            return switch (family) {
                case DALEK -> DalekCityBuilder.radius(size);
                case CITADEL -> CitadelBuilder.radius(size);
                case MOON_BASE -> MoonBaseBuilder.radius(size);
                case VILLAGE -> VillageBuilder.RADIUS;
                case GOLEM_LAIR -> GolemLairBuilder.RADIUS;
            };
        }

        public int height() {
            return switch (family) {
                case DALEK -> DalekCityBuilder.height(size);
                case CITADEL -> CitadelBuilder.height(size);
                case MOON_BASE -> MoonBaseBuilder.height(size);
                case VILLAGE -> VillageBuilder.HEIGHT;
                case GOLEM_LAIR -> GolemLairBuilder.HEIGHT;
            };
        }

        /** Construit cette structure sur la toile (appelé pour chaque chunk touché). */
        public void build(StructureCanvas canvas) {
            switch (family) {
                case DALEK -> DalekCityBuilder.build(canvas, size);
                case CITADEL -> CitadelBuilder.build(canvas, size);
                case MOON_BASE -> MoonBaseBuilder.build(canvas, size);
                case VILLAGE -> VillageBuilder.build(canvas, this);
                case GOLEM_LAIR -> GolemLairBuilder.build(canvas);
            }
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
        if (kind.family() == Family.GOLEM_LAIR) {
            // Sous terre : entre 10 blocs au-dessus du fond du monde et 30 blocs sous la surface
            int low = context.world().getBottomY() + 10;
            int high = y - 30;
            if (high < low) {
                return Optional.empty();
            }
            y = low + context.random().nextInt(high - low + 1);
        }

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
