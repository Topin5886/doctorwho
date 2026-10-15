package fr.doctorwho.registry;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.block.entity.TardisBlockEntity;
import fr.doctorwho.block.entity.TardisConsoleBlockEntity;
import fr.doctorwho.block.entity.TardisInteriorDoorBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public final class ModBlockEntities {

    public static final BlockEntityType<TardisBlockEntity> TARDIS = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            DoctorWhoMod.id("tardis"),
            FabricBlockEntityTypeBuilder.create(TardisBlockEntity::new, ModBlocks.TARDIS).build());

    public static final BlockEntityType<TardisInteriorDoorBlockEntity> TARDIS_INTERIOR_DOOR = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            DoctorWhoMod.id("tardis_interior_door"),
            FabricBlockEntityTypeBuilder.create(TardisInteriorDoorBlockEntity::new, ModBlocks.TARDIS_INTERIOR_DOOR).build());

    public static final BlockEntityType<TardisConsoleBlockEntity> TARDIS_CONSOLE = Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            DoctorWhoMod.id("tardis_console"),
            FabricBlockEntityTypeBuilder.create(TardisConsoleBlockEntity::new, ModBlocks.TARDIS_CONSOLE).build());

    private ModBlockEntities() {
    }

    public static void register() {
    }
}
