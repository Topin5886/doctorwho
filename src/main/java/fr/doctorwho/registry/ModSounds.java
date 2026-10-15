package fr.doctorwho.registry;

import fr.doctorwho.DoctorWhoMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

/**
 * Sons du mod. Les fichiers sont déclarés dans assets/doctorwho/sounds.json :
 * pour l'instant ils pointent vers des sons vanilla modifiés, il suffira d'y mettre
 * de vrais .ogg plus tard sans toucher au code.
 */
public final class ModSounds {

    public static final SoundEvent SONIC_SCREWDRIVER_USE = register("item.sonic_screwdriver.use");
    public static final SoundEvent TARDIS_TAKEOFF = register("block.tardis.takeoff");
    public static final SoundEvent TARDIS_LANDING = register("block.tardis.landing");
    public static final SoundEvent TARDIS_LANDED = register("block.tardis.landed");

    // Créatures (voix générées par tools/generate_sounds.py)
    public static final SoundEvent DALEK_EXTERMINATE = register("entity.dalek.exterminate");
    public static final SoundEvent DALEK_EXTERMINATION = register("entity.dalek.extermination");
    public static final SoundEvent DALEK_AMBIENT = register("entity.dalek.ambient");
    public static final SoundEvent DALEK_LASER = register("entity.dalek.laser");
    public static final SoundEvent CYBERMAN_DELETE = register("entity.cyberman.delete");
    public static final SoundEvent CYBERMAN_AMBIENT = register("entity.cyberman.ambient");
    public static final SoundEvent OOD_HELP = register("entity.ood.help");
    public static final SoundEvent OOD_AMBIENT = register("entity.ood.ambient");
    public static final SoundEvent SONTARIAN_HA = register("entity.sontarian.ha");
    public static final SoundEvent K9_AFFIRM = register("entity.k9.affirm");

    private ModSounds() {
    }

    private static SoundEvent register(String name) {
        Identifier id = DoctorWhoMod.id(name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }

    public static void register() {
    }
}
