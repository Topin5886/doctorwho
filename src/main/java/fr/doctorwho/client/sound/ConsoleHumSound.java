package fr.doctorwho.client.sound;

import fr.doctorwho.registry.ModDimensions;
import fr.doctorwho.registry.ModSounds;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;

/**
 * Bourdonnement de la salle de console : joué en boucle, sans atténuation,
 * tant que le joueur est à l'intérieur d'un TARDIS. Il s'arrête dès qu'il en sort.
 */
@Environment(EnvType.CLIENT)
public class ConsoleHumSound extends MovingSoundInstance {

    private final ClientPlayerEntity player;

    public ConsoleHumSound(ClientPlayerEntity player) {
        super(ModSounds.TARDIS_HUM, SoundCategory.AMBIENT, SoundInstance.createRandom());
        this.player = player;
        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 0.35f;
        this.relative = true;
        this.attenuationType = AttenuationType.NONE;
    }

    /** Vrai si ce joueur est dans un TARDIS (là où le bourdonnement doit s'entendre). */
    public static boolean shouldPlay(ClientPlayerEntity player) {
        return player != null && !player.isRemoved() && player.getWorld().getRegistryKey() == ModDimensions.TARDIS_INTERIOR;
    }

    @Override
    public void tick() {
        if (!shouldPlay(player)) {
            setDone();
        }
    }
}
