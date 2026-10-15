package fr.doctorwho.mixin.client;

import net.minecraft.server.integrated.IntegratedServerLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Supprime l'écran « réglages expérimentaux » (« Je sais ce que je fais ! »).
 * <p>
 * Minecraft l'affiche dès qu'un monde contient des dimensions ajoutées par un mod
 * (nos planètes et l'intérieur du TARDIS). Ce n'est qu'un avertissement : on le saute
 * à la création du monde comme à son chargement.
 */
@Mixin(IntegratedServerLoader.class)
public abstract class IntegratedServerLoaderMixin {

    /** Chargement d'un monde existant : pas de proposition de sauvegarde « expérimentale ». */
    @ModifyVariable(method = "start(Lnet/minecraft/client/gui/screen/Screen;Ljava/lang/String;ZZ)V",
            at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private boolean doctorwho$skipBackupPrompt(boolean canShowBackupPrompt) {
        return false;
    }

    /** Création d'un nouveau monde : pas de confirmation « expérimentale ». */
    @ModifyVariable(method = "tryLoad", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private static boolean doctorwho$bypassWarnings(boolean bypassWarnings) {
        return true;
    }
}
