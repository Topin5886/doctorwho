package fr.doctorwho.registry;

import fr.doctorwho.DoctorWhoMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

/**
 * Dimensions du mod. Elles sont déclarées en JSON (data/doctorwho/dimension,
 * dimension_type et worldgen) ; ici on garde leurs clés et leurs effets spéciaux.
 */
public final class ModDimensions {

    /** Une seule dimension pour tous les intérieurs : chaque TARDIS a sa zone, espacées de 2048 blocs. */
    public static final RegistryKey<World> TARDIS_INTERIOR = key("tardis_interior");

    /** La Lune : poussière grise, ciel noir, gravité réduite. */
    public static final RegistryKey<World> MOON = key("moon");
    /** Skaro, planète natale des Daleks : désert rouge et rocailleux. */
    public static final RegistryKey<World> SKARO = key("skaro");
    /** Gallifrey, planète des Seigneurs du Temps : herbe rouge et ciel orange. */
    public static final RegistryKey<World> GALLIFREY = key("gallifrey");

    private ModDimensions() {
    }

    private static RegistryKey<World> key(String name) {
        return RegistryKey.of(RegistryKeys.WORLD, DoctorWhoMod.id(name));
    }

    public static void register() {
        // Faible gravité lunaire : on saute plus haut et on retombe doucement (sans dégâts de chute)
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getRegistryKey() != MOON || world.getTime() % 20 != 0) {
                return;
            }
            world.getPlayers().forEach(player -> {
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 60, 2, true, false, true));
                player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 60, 0, true, false, true));
            });
        });
    }
}
