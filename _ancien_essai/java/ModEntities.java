package fr.doctorwho.registry;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.entity.AdiposeEntity;
import fr.doctorwho.entity.CybermanEntity;
import fr.doctorwho.entity.DalekEntity;
import fr.doctorwho.entity.OodEntity;
import fr.doctorwho.entity.WeepingAngelEntity;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

/**
 * Enregistrement des entités du mod Doctor Who.
 */
public final class ModEntities {

    // Dalek
    public static final EntityType<DalekEntity> DALEK = register("dalek",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DalekEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 1.8f))
                    .build());

    // Variantes de Dalek (même entité, différents modèles/textures)
    public static final EntityType<DalekEntity> DALEK_BRONZE = register("dalek_bronze",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DalekEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 1.8f))
                    .build());
                    
    public static final EntityType<DalekEntity> DALEK_SILVER = register("dalek_silver",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DalekEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 1.8f))
                    .build());
                    
    public static final EntityType<DalekEntity> DALEK_RED = register("dalek_red",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DalekEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 1.8f))
                    .build());
                    
    public static final EntityType<DalekEntity> DALEK_BLUE = register("dalek_blue",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DalekEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 1.8f))
                    .build());
                    
    public static final EntityType<DalekEntity> DALEK_YELLOW = register("dalek_yellow",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DalekEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 1.8f))
                    .build());
                    
    public static final EntityType<DalekEntity> DALEK_GREEN = register("dalek_green",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, DalekEntity::new)
                    .dimensions(EntityDimensions.fixed(0.9f, 1.8f))
                    .build());

    // Cyberman
    public static final EntityType<CybermanEntity> CYBERMAN = register("cyberman",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, CybermanEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build());

    // Ange Pleureur
    public static final EntityType<WeepingAngelEntity> WEEPING_ANGEL = register("weeping_angel",
            FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, WeepingAngelEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build());

    // Ood
    public static final EntityType<OodEntity> OOD = register("ood",
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, OodEntity::new)
                    .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
                    .build());

    // Adipose
    public static final EntityType<AdiposeEntity> ADIPOSE = register("adipose",
            FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, AdiposeEntity::new)
                    .dimensions(EntityDimensions.fixed(0.5f, 0.5f))
                    .build());

    private ModEntities() {
    }

    private static <T extends net.minecraft.entity.Entity> EntityType<T> register(String name, EntityType<T> entityType) {
        return Registry.register(Registries.ENTITY_TYPE, DoctorWhoMod.id(name), entityType);
    }

    public static void register() {
        // Les enregistrements se font automatiquement lors de l'initialisation des champs statiques
    }
}