package fr.doctorwho.tardis;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Commandes du mod (réservées aux opérateurs) :
 * <ul>
 *     <li>{@code /tardis desktop <décor>} : reconstruit la salle de console du TARDIS où l'on se trouve
 *     avec ce décor, même si c'est déjà le sien (pratique pour tester un thème).</li>
 * </ul>
 */
public final class TardisCommands {

    private TardisCommands() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                CommandManager.literal("tardis")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("desktop")
                                .then(CommandManager.argument("desktop", StringArgumentType.word())
                                        .suggests((context, builder) -> CommandSource.suggestMatching(Variants.DESKTOPS, builder))
                                        .executes(context -> desktop(context.getSource(),
                                                StringArgumentType.getString(context, "desktop")))))));
    }

    private static int desktop(ServerCommandSource source, String desktop) {
        ServerPlayerEntity player = source.getPlayer();
        if (player == null || player.getWorld().getRegistryKey() != fr.doctorwho.registry.ModDimensions.TARDIS_INTERIOR) {
            source.sendError(Text.translatable("command.doctorwho.desktop.not_inside"));
            return 0;
        }
        if (!Variants.DESKTOPS.contains(desktop)) {
            source.sendError(Text.translatable("command.doctorwho.desktop.unknown", desktop));
            return 0;
        }
        TardisData data = TardisRegistry.get(source.getServer()).getByIndex(TardisManager.indexAt(player.getBlockPos()));
        if (data == null) {
            source.sendError(Text.translatable("command.doctorwho.desktop.not_inside"));
            return 0;
        }
        TardisManager.changeDesktop(source.getServer(), data, desktop);
        source.sendFeedback(() -> Text.translatable("command.doctorwho.desktop.done", Variants.desktopName(desktop)), true);
        return 1;
    }
}
