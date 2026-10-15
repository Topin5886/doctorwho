package fr.doctorwho.tardis;

import fr.doctorwho.block.AbstractTardisDoorBlock;
import fr.doctorwho.block.TardisBlock;
import fr.doctorwho.block.entity.TardisBlockEntity;
import fr.doctorwho.registry.ModDimensions;
import fr.doctorwho.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import fr.doctorwho.item.TardisKeyItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Logique serveur des TARDIS : relier un extérieur à son intérieur,
 * générer l'intérieur, faire entrer / sortir, synchroniser les portes.
 * Les voyages sont dans {@link TardisFlight}.
 */
public final class TardisManager {

    /** Distance entre deux intérieurs dans la dimension (en blocs, sur l'axe X). */
    public static final int SPACING = 2048;
    /** Hauteur du sol des salles de console. */
    public static final int FLOOR_Y = 64;

    private TardisManager() {
    }

    /**
     * Retrouve les données du TARDIS posé à cet endroit, ou en crée de nouvelles,
     * et met à jour la position de l'extérieur.
     */
    @Nullable
    public static TardisData getOrCreate(ServerWorld world, BlockPos lowerPos, BlockState state) {
        if (!(world.getBlockEntity(lowerPos) instanceof TardisBlockEntity blockEntity)) {
            return null;
        }
        TardisRegistry registry = TardisRegistry.get(world.getServer());
        TardisData data = blockEntity.getTardisId() == null ? null : registry.get(blockEntity.getTardisId());
        if (data == null) {
            data = registry.create();
            data.setExteriorVariant(blockEntity.getVariant()); // extérieur choisi à la fabrication
            blockEntity.setTardisId(data.getId());
        }
        if (data.isLanded()) {
            data.setExterior(world.getRegistryKey(), lowerPos, state.get(AbstractTardisDoorBlock.FACING));
            registry.markDirty();
        }
        blockEntity.setVariant(data.getExteriorVariant());
        blockEntity.setLocked(data.isLocked());
        return data;
    }

    public static BlockPos interiorCenter(TardisData data) {
        return new BlockPos(data.getIndex() * SPACING, FLOOR_Y, 0);
    }

    /** Index de la zone (donc du TARDIS) qui contient cette position de la dimension intérieure. */
    public static int indexAt(BlockPos pos) {
        return Math.floorDiv(pos.getX() + SPACING / 2, SPACING);
    }

    @Nullable
    public static ServerWorld interiorWorld(MinecraftServer server) {
        return server.getWorld(ModDimensions.TARDIS_INTERIOR);
    }

    // --- Entrer / sortir -----------------------------------------------------------------

    public static void enter(ServerWorld world, BlockPos lowerPos, BlockState state, Entity entity) {
        TardisData data = getOrCreate(world, lowerPos, state);
        ServerWorld interior = interiorWorld(world.getServer());
        if (data == null || interior == null || !data.isLanded()) {
            return;
        }
        if (!data.isInteriorGenerated()) {
            // Première visite : le joueur choisit d'abord le décor de la salle de console
            if (entity instanceof ServerPlayerEntity player) {
                DesktopChoice.ask(player, world, lowerPos);
            }
            return;
        }
        teleportInside(interior, data, entity);
    }

    /** Entrée après le choix du décor : la salle est construite avec ce décor si elle n'existe pas encore. */
    public static void enterWithDesktop(ServerPlayerEntity player, ServerWorld world, BlockPos lowerPos, BlockState state,
                                        String desktop) {
        TardisData data = getOrCreate(world, lowerPos, state);
        ServerWorld interior = interiorWorld(world.getServer());
        if (data == null || interior == null || !data.isLanded()) {
            return;
        }
        if (!data.isInteriorGenerated()) {
            data.setDesktop(desktop);
        }
        teleportInside(interior, data, player);
        player.sendMessage(Text.translatable("message.doctorwho.welcome", Variants.desktopName(data.getDesktop())), true);
    }

    private static void teleportInside(ServerWorld interior, TardisData data, Entity entity) {
        BlockPos door = ensureInterior(interior, data);
        // La porte intérieure regarde la salle (nord) : on arrive deux blocs devant, face à la console
        teleport(entity, interior, Vec3d.ofBottomCenter(door.north(2)), Direction.NORTH.asRotation());
    }

    public static void exit(ServerWorld interior, BlockPos doorLowerPos, Entity entity) {
        MinecraftServer server = interior.getServer();
        TardisData data = TardisRegistry.get(server).getByIndex(indexAt(doorLowerPos));
        if (data != null && !data.isLanded()) {
            return;
        }
        ServerWorld target = data == null ? null : server.getWorld(data.getExteriorWorld());
        if (data == null || target == null) {
            // TARDIS inconnu : retour au point d'apparition du monde
            ServerWorld overworld = server.getOverworld();
            teleport(entity, overworld, Vec3d.ofBottomCenter(overworld.getSpawnPos()), 0.0f);
            return;
        }
        Direction facing = data.getExteriorFacing();
        teleport(entity, target, Vec3d.ofBottomCenter(data.getExteriorPos().offset(facing)), facing.asRotation());
    }

    private static void teleport(Entity entity, ServerWorld destination, Vec3d pos, float yaw) {
        entity.setVelocity(Vec3d.ZERO);
        entity.teleport(destination, pos.x, pos.y, pos.z, Set.of(), yaw, entity.getPitch());
        entity.setPortalCooldown(20);
        // Son du passage entre la cabine et la salle de console
        destination.playSound(null, pos.x, pos.y, pos.z, ModSounds.TARDIS_ENTER, SoundCategory.BLOCKS, 0.8f, 1.0f);
    }

    /**
     * Génère la salle de console à la première visite, ou met à jour un intérieur
     * construit par une ancienne version du mod. Renvoie la position de la porte intérieure.
     */
    private static BlockPos ensureInterior(ServerWorld interior, TardisData data) {
        BlockPos center = interiorCenter(data);
        BlockPos door = InteriorBuilder.doorPos(center);
        if (!data.isInteriorGenerated()) {
            InteriorBuilder.build(interior, center, data.getDesktop());
            // On ne peut entrer que portes ouvertes : la porte intérieure l'est aussi
            AbstractTardisDoorBlock.setOpen(interior, door, true);
            data.setInteriorGenerated(true);
            data.setInteriorVersion(InteriorBuilder.VERSION);
            TardisRegistry.get(interior.getServer()).markDirty();
        } else if (data.getInteriorVersion() < InteriorBuilder.VERSION) {
            InteriorBuilder.build(interior, center, data.getDesktop());
            Architecture.reopenConsoleExits(interior, data);
            AbstractTardisDoorBlock.setOpen(interior, door, true);
            data.setInteriorVersion(InteriorBuilder.VERSION);
            TardisRegistry.get(interior.getServer()).markDirty();
        }
        return door;
    }

    /**
     * Change le décor de la salle de console : la salle est reconstruite (les couloirs
     * et salles ajoutés sont conservés) et l'équipage est replacé devant la porte.
     */
    public static void changeDesktop(MinecraftServer server, TardisData data, String desktop) {
        ServerWorld interior = interiorWorld(server);
        if (interior == null || !data.isInteriorGenerated()) {
            data.setDesktop(desktop);
            return;
        }
        data.setDesktop(desktop);
        BlockPos center = interiorCenter(data);
        BlockPos door = InteriorBuilder.doorPos(center);
        InteriorBuilder.build(interior, center, data.getDesktop());
        Architecture.reopenConsoleExits(interior, data);
        data.setInteriorVersion(InteriorBuilder.VERSION);
        ServerWorld exterior = server.getWorld(data.getExteriorWorld());
        boolean open = exterior != null && exterior.getBlockState(data.getExteriorPos()).getBlock() instanceof TardisBlock
                && exterior.getBlockState(data.getExteriorPos()).get(AbstractTardisDoorBlock.OPEN);
        AbstractTardisDoorBlock.setOpen(interior, door, open);
        Vec3d spawn = Vec3d.ofBottomCenter(door.north(2));
        for (var player : interior.getPlayers()) {
            if (Math.abs(player.getX() - center.getX()) <= 12 && Math.abs(player.getZ() - center.getZ()) <= 12) {
                player.teleport(interior, spawn.x, spawn.y, spawn.z, Set.of(), Direction.NORTH.asRotation(), player.getPitch());
            }
        }
        TardisRegistry.get(server).markDirty();
    }

    // --- Portes synchronisées -----------------------------------------------------------

    public static void toggleFromOutside(ServerWorld world, BlockPos lowerPos, BlockState state, PlayerEntity player) {
        TardisData data = getOrCreate(world, lowerPos, state);
        if (data != null && !data.isLanded()) {
            return;
        }
        ItemStack key = TardisKeyItem.heldKey(player);
        if (data != null && !key.isEmpty() && player instanceof ServerPlayerEntity serverPlayer) {
            TardisKeys.useOnTardis(serverPlayer, key, data);
            return;
        }
        if (data != null && data.isLocked()) {
            world.playSound(null, lowerPos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.6f, 1.6f);
            player.sendMessage(Text.translatable("message.doctorwho.key.locked_door").formatted(Formatting.GOLD), true);
            return;
        }
        boolean open = !state.get(AbstractTardisDoorBlock.OPEN);
        AbstractTardisDoorBlock.setOpen(world, lowerPos, open);
        ServerWorld interior = interiorWorld(world.getServer());
        if (data != null && interior != null && data.isInteriorGenerated()) {
            AbstractTardisDoorBlock.setOpen(interior, InteriorBuilder.doorPos(interiorCenter(data)), open);
        }
    }

    public static void toggleFromInside(ServerWorld interior, BlockPos lowerPos, BlockState state, PlayerEntity player) {
        TardisData data = TardisRegistry.get(interior.getServer()).getByIndex(indexAt(lowerPos));
        if (data != null && !data.isLanded()) {
            player.sendMessage(Text.translatable("message.doctorwho.flight.doors_locked"), true);
            return;
        }
        ItemStack key = TardisKeyItem.heldKey(player);
        if (data != null && !key.isEmpty() && player instanceof ServerPlayerEntity serverPlayer) {
            TardisKeys.useOnTardis(serverPlayer, key, data);
            return;
        }
        boolean open = !state.get(AbstractTardisDoorBlock.OPEN);
        if (open && data != null && data.isLocked()) {
            // De l'intérieur, on peut toujours sortir : ouvrir déverrouille
            TardisKeys.setLocked(interior.getServer(), data, false);
            player.sendMessage(Text.translatable("message.doctorwho.key.unlocked_inside"), true);
        }
        AbstractTardisDoorBlock.setOpen(interior, lowerPos, open);
        ServerWorld exterior = data == null ? null : interior.getServer().getWorld(data.getExteriorWorld());
        if (exterior != null && exterior.getBlockState(data.getExteriorPos()).getBlock() instanceof TardisBlock) {
            AbstractTardisDoorBlock.setOpen(exterior, data.getExteriorPos(), open);
        }
    }
}
