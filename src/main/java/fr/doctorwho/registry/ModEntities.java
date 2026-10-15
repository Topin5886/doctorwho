package fr.doctorwho.registry;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.entity.AdiposeEntity;
import fr.doctorwho.entity.CybermanEntity;
import fr.doctorwho.entity.DalekEntity;
import fr.doctorwho.entity.K9Entity;
import fr.doctorwho.entity.SilenceEntity;
import fr.doctorwho.entity.SlitheenEntity;
import fr.doctorwho.entity.SontarianEntity;
import fr.doctorwho.entity.ZygonEntity;
import fr.doctorwho.entity.OodEntity;
import fr.doctorwho.entity.RedstoneGolemEntity;
import fr.doctorwho.entity.SeatEntity;
import fr.doctorwho.entity.WeepingAngelEntity;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.Item;
import fr.doctorwho.item.CreatureSpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.Difficulty;
import net.minecraft.world.Heightmap;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.List;

/**
 * Créatures de la série : types d'entités, attributs, règles d'apparition et œufs.
 * <ul>
 *     <li>Skaro : Daleks</li>
 *     <li>Lune : Cybermen</li>
 *     <li>Gallifrey : Anges Pleureurs, Ood</li>
 *     <li>Surface : un peu de tout, plus rarement (Adipose fréquents)</li>
 * </ul>
 */
public final class ModEntities {

    public static final List<Item> SPAWN_EGGS = new ArrayList<>();

    public static final EntityType<DalekEntity> DALEK = register("dalek",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DalekEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0f, 1.75f)).trackRangeBlocks(80).build(),
            0xA0703A, 0xD2A848);
    public static final EntityType<CybermanEntity> CYBERMAN = register("cyberman",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CybermanEntity::new)
                    .dimensions(EntityDimensions.fixed(0.65f, 2.15f)).build(),
            0xB4B8C0, 0x3C3E46);
    public static final EntityType<WeepingAngelEntity> WEEPING_ANGEL = register("weeping_angel",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, WeepingAngelEntity::new)
                    .dimensions(EntityDimensions.fixed(0.7f, 2.1f)).build(),
            0x969692, 0x5E5E5A);
    public static final EntityType<OodEntity> OOD = register("ood",
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, OodEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 2.2f)).build(),
            0xC4C4C8, 0x1A1A1E);
    public static final EntityType<AdiposeEntity> ADIPOSE = register("adipose",
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, AdiposeEntity::new)
                    .dimensions(EntityDimensions.fixed(0.45f, 0.7f)).build(),
            0xF0F0EC, 0xD8A0A8);

    public static final EntityType<K9Entity> K9 = register("k9",
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, K9Entity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 0.95f)).build(),
            0x9EA4AA, 0xFF463C);
    public static final EntityType<SontarianEntity> SONTARIAN = register("sontarian",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, SontarianEntity::new)
                    .dimensions(EntityDimensions.fixed(0.75f, 1.8f)).build(),
            0x444C62, 0x70644F);
    public static final EntityType<SilenceEntity> SILENCE = register("silence",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, SilenceEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 2.8f)).build(),
            0x16161A, 0x9CA498);
    public static final EntityType<ZygonEntity> ZYGON = register("zygon",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ZygonEntity::new)
                    .dimensions(EntityDimensions.fixed(0.7f, 2.2f)).build(),
            0xCE7034, 0x964822);
    public static final EntityType<SlitheenEntity> SLITHEEN = register("slitheen",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, SlitheenEntity::new)
                    .dimensions(EntityDimensions.fixed(1.0f, 2.5f)).build(),
            0x5C8046, 0xE6BE32);
    /** Mini-boss des cavernes : n'apparait que dans son repaire (structure golem_lair). */
    public static final EntityType<RedstoneGolemEntity> REDSTONE_GOLEM = register("redstone_golem",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, RedstoneGolemEntity::new)
                    .dimensions(EntityDimensions.fixed(2.4f, 3.6f)).fireImmune().trackRangeBlocks(80).build(),
            0x3A3438, 0xE82C1E);

    /** Entité invisible des sièges (pas d'œuf). */
    public static final EntityType<SeatEntity> SEAT = Registry.register(Registries.ENTITY_TYPE, DoctorWhoMod.id("seat"),
            FabricEntityTypeBuilder.<SeatEntity>create(SpawnGroup.MISC, SeatEntity::new)
                    .dimensions(EntityDimensions.fixed(0.01f, 0.01f)).disableSummon().build());

    private ModEntities() {
    }

    private static <T extends MobEntity> EntityType<T> register(String name, EntityType<T> type, int eggColor, int eggSpots) {
        Registry.register(Registries.ENTITY_TYPE, DoctorWhoMod.id(name), type);
        Item egg = new CreatureSpawnEggItem(type, eggColor, eggSpots, new FabricItemSettings());
        SPAWN_EGGS.add(Registry.register(Registries.ITEM, DoctorWhoMod.id(name + "_spawn_egg"), egg));
        return type;
    }

    private static RegistryKey<Biome> biome(String name) {
        return RegistryKey.of(RegistryKeys.BIOME, DoctorWhoMod.id(name));
    }

    public static void register() {
        FabricDefaultAttributeRegistry.register(DALEK, DalekEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(CYBERMAN, CybermanEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(WEEPING_ANGEL, WeepingAngelEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(OOD, OodEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ADIPOSE, AdiposeEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(K9, K9Entity.createAttributes());
        FabricDefaultAttributeRegistry.register(SONTARIAN, SontarianEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SILENCE, SilenceEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(ZYGON, ZygonEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(SLITHEEN, SlitheenEntity.createAttributes());
        FabricDefaultAttributeRegistry.register(REDSTONE_GOLEM, RedstoneGolemEntity.createAttributes());

        // Sur leur planète, les monstres apparaissent même en plein jour ; ailleurs, seulement dans le noir
        SpawnRestriction.register(DALEK, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> monster(type, world, reason, pos, random, ModDimensions.SKARO));
        SpawnRestriction.register(CYBERMAN, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> monster(type, world, reason, pos, random, ModDimensions.MOON));
        SpawnRestriction.register(WEEPING_ANGEL, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> monster(type, world, reason, pos, random, ModDimensions.GALLIFREY));
        SpawnRestriction.register(SONTARIAN, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> monster(type, world, reason, pos, random, ModDimensions.MOON));
        SpawnRestriction.register(SILENCE, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> monster(type, world, reason, pos, random, null));
        SpawnRestriction.register(ZYGON, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> monster(type, world, reason, pos, random, null));
        SpawnRestriction.register(SLITHEEN, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                (type, world, reason, pos, random) -> monster(type, world, reason, pos, random, null));
        SpawnRestriction.register(OOD, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                ModEntities::peaceful);
        SpawnRestriction.register(ADIPOSE, SpawnRestriction.Location.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
                ModEntities::peaceful);

        // Planètes
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(biome("skaro")), SpawnGroup.MONSTER, DALEK, 60, 1, 3);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(biome("moon")), SpawnGroup.MONSTER, CYBERMAN, 40, 1, 3);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(biome("gallifrey")), SpawnGroup.MONSTER, WEEPING_ANGEL, 10, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(biome("gallifrey")), SpawnGroup.CREATURE, OOD, 20, 1, 3);
        BiomeModifications.addSpawn(BiomeSelectors.includeByKey(biome("moon")), SpawnGroup.MONSTER, SONTARIAN, 25, 1, 2);
        // K9 n'apparait pas naturellement : on le trouve avec son œuf, puis on l'apprivoise à la redstone
        // Surface (plus rare)
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, SONTARIAN, 3, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, SILENCE, 3, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, ZYGON, 3, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, SLITHEEN, 3, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, DALEK, 3, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, CYBERMAN, 4, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.MONSTER, WEEPING_ANGEL, 4, 1, 1);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.CREATURE, OOD, 2, 1, 2);
        BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), SpawnGroup.CREATURE, ADIPOSE, 6, 2, 4);
    }

    private static boolean monster(EntityType<? extends HostileEntity> type, ServerWorldAccess world, SpawnReason reason,
                                   BlockPos pos, Random random, RegistryKey<World> homePlanet) {
        if (world.getDifficulty() == Difficulty.PEACEFUL) {
            return false;
        }
        if (world.toServerWorld().getRegistryKey() == homePlanet) {
            return MobEntity.canMobSpawn(type, world, reason, pos, random);
        }
        return HostileEntity.canSpawnInDark(type, world, reason, pos, random);
    }

    private static boolean peaceful(EntityType<? extends MobEntity> type, ServerWorldAccess world, SpawnReason reason,
                                    BlockPos pos, Random random) {
        return MobEntity.canMobSpawn(type, world, reason, pos, random) && world.getBaseLightLevel(pos, 0) > 8;
    }
}
