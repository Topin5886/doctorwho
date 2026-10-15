package fr.doctorwho.registry;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.item.SonicScrewdriverItem;
import fr.doctorwho.item.TardisKeyItem;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Rarity;

public final class ModItems {

    public static final Item TARDIS = register("tardis",
            new BlockItem(ModBlocks.TARDIS, new FabricItemSettings().maxCount(1).rarity(Rarity.EPIC)));

    /** Tournevis sonique : un seul objet, la variante (modèle) est dans ses données. */
    public static final Item SONIC_SCREWDRIVER = register("sonic_screwdriver",
            new SonicScrewdriverItem(new FabricItemSettings().maxCount(1).rarity(Rarity.RARE)));

    /** Clé du TARDIS : jumelée à un TARDIS, elle le verrouille et peut l'appeler. */
    public static final Item TARDIS_KEY = register("tardis_key",
            new TardisKeyItem(new FabricItemSettings().maxCount(1).rarity(Rarity.UNCOMMON)));

    private ModItems() {
    }

    private static Item register(String name, Item item) {
        return Registry.register(Registries.ITEM, DoctorWhoMod.id(name), item);
    }

    public static void register() {
    }
}
