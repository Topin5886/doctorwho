package fr.doctorwho.tardis;

import fr.doctorwho.block.AbstractTardisDoorBlock;
import fr.doctorwho.block.entity.TardisBlockEntity;
import fr.doctorwho.item.TardisKeyItem;
import fr.doctorwho.registry.ModDimensions;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Ce que fait la clé du TARDIS côté serveur : jumelage, verrou, appel du TARDIS
 * et localisation. Voir {@link TardisKeyItem}.
 */
public final class TardisKeys {

    /** Délai entre deux appels du TARDIS avec la même clé (en ticks). */
    private static final int CALL_COOLDOWN = 100;

    private TardisKeys() {
    }

    @Nullable
    private static TardisData linkedTardis(MinecraftServer server, ItemStack key) {
        UUID id = TardisKeyItem.getTardisId(key);
        return id == null ? null : TardisRegistry.get(server).get(id);
    }

    /**
     * Clic droit avec une clé sur un TARDIS (extérieur ou porte intérieure).
     * Clé vierge : la jumelle (si le TARDIS n'est pas verrouillé). Clé de ce TARDIS : verrouille / déverrouille.
     */
    public static void useOnTardis(ServerPlayerEntity player, ItemStack key, TardisData data) {
        UUID id = TardisKeyItem.getTardisId(key);
        if (id == null) {
            if (data.isLocked()) {
                player.sendMessage(Text.translatable("message.doctorwho.key.locked_other").formatted(Formatting.RED), true);
                return;
            }
            TardisKeyItem.link(key, data);
            player.playSound(SoundEvents.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 1.0f, 1.2f);
            player.sendMessage(Text.translatable("message.doctorwho.key.linked", data.getIndex() + 1)
                    .formatted(Formatting.GREEN), true);
            return;
        }
        if (!id.equals(data.getId())) {
            player.sendMessage(Text.translatable("message.doctorwho.key.wrong").formatted(Formatting.RED), true);
            return;
        }
        boolean locked = !data.isLocked();
        setLocked(player.getServer(), data, locked);
        player.getServerWorld().playSound(null, player.getBlockPos(),
                locked ? SoundEvents.BLOCK_IRON_DOOR_CLOSE : SoundEvents.BLOCK_IRON_DOOR_OPEN,
                SoundCategory.PLAYERS, 0.8f, 1.7f);
        player.sendMessage(Text.translatable(locked ? "message.doctorwho.key.lock" : "message.doctorwho.key.unlock")
                .formatted(locked ? Formatting.GOLD : Formatting.GREEN), true);
    }

    /** Verrouille (portes fermées des deux côtés) ou déverrouille le TARDIS. */
    public static void setLocked(MinecraftServer server, TardisData data, boolean locked) {
        data.setLocked(locked);
        if (locked) {
            ServerWorld exterior = server.getWorld(data.getExteriorWorld());
            if (exterior != null && TardisFlight.exteriorBlock(server, data) != null) {
                AbstractTardisDoorBlock.setOpen(exterior, data.getExteriorPos(), false);
            }
            ServerWorld interior = TardisManager.interiorWorld(server);
            if (interior != null && data.isInteriorGenerated()) {
                AbstractTardisDoorBlock.setOpen(interior, InteriorBuilder.doorPos(TardisManager.interiorCenter(data)), false);
            }
        }
        TardisBlockEntity exteriorBlock = TardisFlight.exteriorBlock(server, data);
        if (exteriorBlock != null) {
            exteriorBlock.setLocked(locked);
        }
        TardisRegistry.get(server).markDirty();
    }

    /** Clic droit sur le sol : le TARDIS jumelé vient se poser à cet endroit. */
    public static void call(ServerPlayerEntity player, ItemStack key, BlockPos target) {
        MinecraftServer server = player.getServer();
        TardisData data = linkedTardis(server, key);
        if (data == null) {
            player.sendMessage(Text.translatable(TardisKeyItem.isLinked(key)
                    ? "message.doctorwho.key.lost" : "message.doctorwho.key.blank").formatted(Formatting.RED), true);
            return;
        }
        ServerWorld world = player.getServerWorld();
        if (world.getRegistryKey() == ModDimensions.TARDIS_INTERIOR) {
            player.sendMessage(Text.translatable("message.doctorwho.key.inside"), true);
            return;
        }
        if (!TardisFlight.DESTINATIONS.contains(world.getRegistryKey())) {
            player.sendMessage(Text.translatable("message.doctorwho.flight.bad_dimension").formatted(Formatting.RED), true);
            return;
        }
        if (!data.isLanded()) {
            player.sendMessage(Text.translatable("message.doctorwho.flight.busy"), true);
            return;
        }
        if (data.getExteriorWorld() == world.getRegistryKey() && data.getExteriorPos().isWithinDistance(target, 3)) {
            player.sendMessage(Text.translatable("message.doctorwho.key.already_here"), true);
            return;
        }
        // Les portes s'ouvriront face au joueur
        Direction before = data.getExteriorFacing();
        Direction facing = player.getHorizontalFacing().getOpposite();
        data.setExterior(data.getExteriorWorld(), data.getExteriorPos(), facing);
        Text error = TardisFlight.takeOff(server, data, world.getRegistryKey(), target);
        if (error != null) {
            data.setExterior(data.getExteriorWorld(), data.getExteriorPos(), before);
            player.sendMessage(error, true);
            return;
        }
        player.getItemCooldownManager().set(key.getItem(), CALL_COOLDOWN);
        player.sendMessage(Text.translatable("message.doctorwho.key.called").formatted(Formatting.AQUA), true);
    }

    /** Clic droit dans le vide : indique où se trouve le TARDIS jumelé. */
    public static void locate(ServerPlayerEntity player, ItemStack key) {
        TardisData data = linkedTardis(player.getServer(), key);
        if (data == null) {
            player.sendMessage(Text.translatable(TardisKeyItem.isLinked(key)
                    ? "message.doctorwho.key.lost" : "message.doctorwho.key.blank").formatted(Formatting.GRAY), true);
            return;
        }
        if (!data.isLanded()) {
            player.sendMessage(Text.translatable("message.doctorwho.key.where_flying"), true);
            return;
        }
        BlockPos pos = data.getExteriorPos();
        Text place = Text.translatable("doctorwho.dimension." + data.getExteriorWorld().getValue().getPath());
        player.sendMessage(Text.translatable(data.isLocked() ? "message.doctorwho.key.where_locked" : "message.doctorwho.key.where",
                place, pos.getX(), pos.getY(), pos.getZ()).formatted(Formatting.AQUA), true);
    }
}
