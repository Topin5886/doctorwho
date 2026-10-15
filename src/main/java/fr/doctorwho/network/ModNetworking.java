package fr.doctorwho.network;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.block.TardisConsoleBlock;
import fr.doctorwho.block.entity.TardisBlockEntity;
import fr.doctorwho.block.entity.TardisConsoleBlockEntity;
import fr.doctorwho.item.SonicScrewdriverItem;
import fr.doctorwho.registry.ModDimensions;
import fr.doctorwho.tardis.Architecture;
import fr.doctorwho.tardis.TardisData;
import fr.doctorwho.tardis.TardisFlight;
import fr.doctorwho.tardis.TardisManager;
import fr.doctorwho.tardis.TardisRegistry;
import fr.doctorwho.tardis.Variants;
import fr.doctorwho.tardis.Waypoint;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Paquets réseau du panneau de la console.
 * <ul>
 *     <li>Serveur → client : ouvrir (ou rafraîchir) le panneau avec les données du TARDIS</li>
 *     <li>Client → serveur : décoller, waypoints, apparence, construction de salles</li>
 * </ul>
 */
public final class ModNetworking {

    public static final Identifier OPEN_FLIGHT_PANEL = DoctorWhoMod.id("open_flight_panel");
    public static final Identifier FLIGHT_COMMAND = DoctorWhoMod.id("flight_command");

    public static final int ACTION_TAKE_OFF = 0;
    public static final int ACTION_SAVE_WAYPOINT = 1;
    public static final int ACTION_DELETE_WAYPOINT = 2;
    public static final int ACTION_SET_EXTERIOR = 3;
    public static final int ACTION_SET_DESKTOP = 4;
    public static final int ACTION_SET_SONIC = 5;
    public static final int ACTION_BUILD_ROOM = 6;

    /** Distance maximale (en blocs) entre le joueur et une console pour piloter. */
    private static final int CONSOLE_REACH = 6;

    private ModNetworking() {
    }

    /** Une commande reçue du client, déjà décodée. */
    private record Command(int action, @Nullable RegistryKey<World> world, @Nullable BlockPos pos,
                           String text, int index) {
    }

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(FLIGHT_COMMAND, (server, player, handler, buf, responseSender) -> {
            Command command = readCommand(buf);
            server.execute(() -> handleCommand(player, command));
        });
    }

    private static Command readCommand(PacketByteBuf buf) {
        int action = buf.readVarInt();
        return switch (action) {
            case ACTION_TAKE_OFF -> new Command(action, worldKey(buf), buf.readBlockPos(), "", -1);
            case ACTION_SAVE_WAYPOINT -> {
                RegistryKey<World> world = worldKey(buf);
                BlockPos pos = buf.readBlockPos();
                yield new Command(action, world, pos, buf.readString(Waypoint.MAX_NAME_LENGTH), -1);
            }
            case ACTION_DELETE_WAYPOINT -> new Command(action, null, null, "", buf.readVarInt());
            case ACTION_BUILD_ROOM -> {
                int index = buf.readVarInt();
                yield new Command(action, null, null, buf.readString(32), index);
            }
            default -> new Command(action, null, null, buf.readString(32), -1);
        };
    }

    private static RegistryKey<World> worldKey(PacketByteBuf buf) {
        return RegistryKey.of(RegistryKeys.WORLD, buf.readIdentifier());
    }

    /** Ouvre le panneau pour le joueur, avec le tournevis posé dans la console la plus proche. */
    public static void openFlightPanel(ServerPlayerEntity player, TardisData data) {
        TardisConsoleBlockEntity console = nearestConsole(player);
        String sonic = console == null || console.getSonic().isEmpty() ? "" : SonicScrewdriverItem.getVariant(console.getSonic());
        PacketByteBuf buf = PacketByteBufs.create();
        FlightPanelData.of(data, sonic).write(buf);
        ServerPlayNetworking.send(player, OPEN_FLIGHT_PANEL, buf);
    }

    @Nullable
    private static TardisConsoleBlockEntity nearestConsole(ServerPlayerEntity player) {
        BlockPos origin = player.getBlockPos();
        for (BlockPos pos : BlockPos.iterateOutwards(origin, CONSOLE_REACH, 3, CONSOLE_REACH)) {
            if (player.getWorld().getBlockState(pos).getBlock() instanceof TardisConsoleBlock
                    && player.getWorld().getBlockEntity(pos) instanceof TardisConsoleBlockEntity console) {
                return console;
            }
        }
        return null;
    }

    private static void handleCommand(ServerPlayerEntity player, Command command) {
        // Il faut être dans l'intérieur de ce TARDIS, près d'une de ses consoles
        if (player.getWorld().getRegistryKey() != ModDimensions.TARDIS_INTERIOR) {
            return;
        }
        TardisData data = TardisRegistry.get(player.getServer()).getByIndex(TardisManager.indexAt(player.getBlockPos()));
        TardisConsoleBlockEntity console = nearestConsole(player);
        if (data == null || console == null) {
            return;
        }
        var server = player.getServer();
        switch (command.action()) {
            case ACTION_TAKE_OFF -> {
                Text error = TardisFlight.takeOff(server, data, command.world(), command.pos());
                player.sendMessage(error != null ? error : Text.translatable("message.doctorwho.flight.takeoff"), true);
                return;
            }
            case ACTION_SAVE_WAYPOINT -> {
                String cleanName = command.text().isBlank() ? "?" : command.text().strip();
                if (!data.addWaypoint(new Waypoint(cleanName, command.world(), command.pos()))) {
                    player.sendMessage(Text.translatable("message.doctorwho.flight.waypoints_full", TardisData.MAX_WAYPOINTS), true);
                }
            }
            case ACTION_DELETE_WAYPOINT -> data.removeWaypoint(command.index());
            case ACTION_SET_EXTERIOR -> {
                // Circuit caméléon : la cabine change d'apparence, même posée
                data.setExteriorVariant(command.text());
                TardisBlockEntity exterior = TardisFlight.exteriorBlock(server, data);
                if (exterior != null) {
                    exterior.setVariant(data.getExteriorVariant());
                }
                player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 0.8f, 1.4f);
                player.sendMessage(Text.translatable("message.doctorwho.appearance.exterior",
                        Variants.exteriorName(data.getExteriorVariant())), true);
            }
            case ACTION_SET_DESKTOP -> {
                if (!data.isLanded()) {
                    player.sendMessage(Text.translatable("message.doctorwho.flight.busy"), true);
                    return;
                }
                TardisManager.changeDesktop(server, data, command.text());
                player.playSound(SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.BLOCKS, 0.8f, 0.8f);
                player.sendMessage(Text.translatable("message.doctorwho.appearance.desktop",
                        Variants.desktopName(data.getDesktop())), true);
            }
            case ACTION_SET_SONIC -> {
                ItemStack sonic = console.getSonic();
                if (sonic.isEmpty()) {
                    return;
                }
                console.setSonic(SonicScrewdriverItem.withVariant(sonic.copy(), command.text()));
                player.playSound(SoundEvents.BLOCK_BEACON_POWER_SELECT, SoundCategory.BLOCKS, 0.8f, 1.8f);
            }
            case ACTION_BUILD_ROOM -> {
                ServerWorld interior = (ServerWorld) player.getWorld();
                Text error = Architecture.build(interior, data, command.index(), command.text());
                if (error != null) {
                    player.sendMessage(error.copy().formatted(Formatting.RED), true);
                } else {
                    player.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.BLOCKS, 1.0f, 0.8f);
                    player.sendMessage(Text.translatable("message.doctorwho.architecture.built",
                            Text.translatable("room.doctorwho." + command.text())).formatted(Formatting.GREEN), true);
                }
            }
            default -> {
                return;
            }
        }
        TardisRegistry.get(server).markDirty();
        openFlightPanel(player, data);
    }
}
