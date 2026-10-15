package fr.doctorwho.registry;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.item.SonicScrewdriverItem;
import fr.doctorwho.tardis.Variants;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;

public final class ModItemGroups {

    /** Onglet « Doctor Who » de l'inventaire créatif. */
    public static final ItemGroup DOCTOR_WHO = Registry.register(
            Registries.ITEM_GROUP,
            DoctorWhoMod.id("main"),
            FabricItemGroup.builder()
                    .displayName(Text.translatable("itemGroup.doctorwho.main"))
                    .icon(() -> new ItemStack(ModItems.TARDIS))
                    .entries((context, entries) -> {
                        entries.add(ModItems.TARDIS);
                        entries.add(ModItems.TARDIS_KEY);
                        // Un tournevis de chaque modèle
                        for (String variant : Variants.SONICS) {
                            entries.add(SonicScrewdriverItem.withVariant(new ItemStack(ModItems.SONIC_SCREWDRIVER), variant));
                        }
                        ModEntities.SPAWN_EGGS.forEach(entries::add);
                        ModBlocks.DECORATION.forEach(entries::add);
                        ModBlocks.PLANET_BLOCKS.forEach(entries::add);
                    })
                    .build());

    private ModItemGroups() {
    }

    public static void register() {
    }
}
