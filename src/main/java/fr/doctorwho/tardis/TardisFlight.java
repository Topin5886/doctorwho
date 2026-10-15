package fr.doctorwho.tardis;

import fr.doctorwho.block.AbstractTardisDoorBlock;
import fr.doctorwho.block.TardisBlock;
import fr.doctorwho.block.TardisConsoleBlock;
import fr.doctorwho.block.entity.TardisBlockEntity;
import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.registry.ModDimensions;
import fr.doctorwho.registry.ModSounds;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Voyages du TARDIS. Chaque tick serveur fait avancer les TARDIS en vol :
 * dématérialisation (la cabine s'efface), vortex, puis rematérialisation à destination.
 */
public final class TardisFlight {

    public static final int DEMAT_TICKS = 140;
    public static final int VORTEX_TICKS = 100;
    public static final int REMAT_TICKS = 140;

    /** Dimensions accessibles depuis le panneau de vol. */
    public static final List<RegistryKey<World>> DESTINATIONS = List.of(World.OVERWORLD, World.NETHER, World.END,
            ModDimensions.MOON, ModDimensions.SKARO, ModDimensions.GALLIFREY);

    private static final int WORLD_LIMIT = 29_999_000;

    private TardisFlight() {
    }

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(TardisFlight::tick);
    }

    /** Lance un voyage. Renvoie un message d'erreur, ou null si le TARDIS décolle. */
    @Nullable
    public static Text takeOff(MinecraftServer server, TardisData data, RegistryKey<World> world, BlockPos target) {
        if (!data.isLanded()) {
            return Text.translatable("message.doctorwho.flight.busy");
        }
        if (!DESTINATIONS.contains(world) || server.getWorld(world) == null) {
            return Text.translatable("message.doctorwho.flight.bad_dimension");
        }
        BlockPos clamped = new BlockPos(
                MathHelper.clamp(target.getX(), -WORLD_LIMIT, WORLD_LIMIT),
                target.getY(),
                MathHelper.clamp(target.getZ(), -WORLD_LIMIT, WORLD_LIMIT));
        data.setDestination(world, clamped);
        data.setPhase(FlightPhase.DEMATERIALISING);

        // Portes fermées des deux côtés, rotor en marche
        ServerWorld exterior = server.getWorld(data.getExteriorWorld());
        TardisBlockEntity exteriorBlock = exteriorBlock(server, data);
        if (exterior != null) {
            AbstractTardisDoorBlock.setOpen(exterior, data.getExteriorPos(), false);
            playSound(exterior, data.getExteriorPos(), ModSounds.TARDIS_TAKEOFF);
        }
        if (exteriorBlock != null) {
            exteriorBlock.setMaterialisation(FlightPhase.DEMATERIALISING);
        }
        ServerWorld interior = TardisManager.interiorWorld(server);
        if (interior != null) {
            BlockPos center = TardisManager.interiorCenter(data);
            AbstractTardisDoorBlock.setOpen(interior, InteriorBuilder.doorPos(center), false);
            setConsoleFlying(interior, center, true);
            playSound(interior, center.up(), ModSounds.TARDIS_TAKEOFF);
        }
        TardisRegistry.get(server).markDirty();
        return null;
    }

    private static void tick(MinecraftServer server) {
        TardisRegistry registry = TardisRegistry.get(server);
        boolean changed = false;
        for (TardisData data : registry.all()) {
            if (data.isLanded()) {
                continue;
            }
            changed = true;
            int ticks = data.tickPhase();
            if (ticks % 10 == 0) {
                notifyCrew(server, data, progressMessage(data, ticks));
            }
            switch (data.getPhase()) {
                case DEMATERIALISING -> {
                    if (ticks >= DEMAT_TICKS) {
                        removeExterior(server, data);
                        data.setPhase(FlightPhase.IN_FLIGHT);
                    }
                }
                case IN_FLIGHT -> {
                    if (ticks >= VORTEX_TICKS) {
                        land(server, data);
                        data.setPhase(FlightPhase.MATERIALISING);
                    }
                }
                case MATERIALISING -> {
                    if (ticks >= REMAT_TICKS) {
                        finishLanding(server, data);
                        data.setPhase(FlightPhase.LANDED);
                    }
                }
                default -> {
                }
            }
        }
        if (changed) {
            registry.markDirty();
        }
    }

    // --- Étapes --------------------------------------------------------------------------

    private static void removeExterior(MinecraftServer server, TardisData data) {
        ServerWorld world = server.getWorld(data.getExteriorWorld());
        if (world != null && exteriorBlock(server, data) != null) {
            // La moitié haute disparaît d'elle-même avec la moitié basse ; pas d'objet lâché
            world.setBlockState(data.getExteriorPos(), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
        }
    }

    private static void land(MinecraftServer server, TardisData data) {
        ServerWorld world = server.getWorld(data.getDestinationWorld());
        if (world == null) {
            world = server.getOverworld();
        }
        Direction facing = data.getExteriorFacing();
        BlockPos pos = findLanding(world, data.getDestinationPos());
        BlockState lower = ModBlocks.TARDIS.getDefaultState()
                .with(AbstractTardisDoorBlock.FACING, facing)
                .with(AbstractTardisDoorBlock.HALF, DoubleBlockHalf.LOWER);
        world.setBlockState(pos.up(), lower.with(AbstractTardisDoorBlock.HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
        world.setBlockState(pos, lower, Block.NOTIFY_ALL);
        data.setExterior(world.getRegistryKey(), pos, facing);
        if (world.getBlockEntity(pos) instanceof TardisBlockEntity blockEntity) {
            blockEntity.setTardisId(data.getId());
            blockEntity.setVariant(data.getExteriorVariant());
            blockEntity.setLocked(data.isLocked());
            blockEntity.setMaterialisation(FlightPhase.MATERIALISING);
        }
        playSound(world, pos, ModSounds.TARDIS_LANDING);
    }

    private static void finishLanding(MinecraftServer server, TardisData data) {
        TardisBlockEntity exterior = exteriorBlock(server, data);
        if (exterior != null) {
            exterior.setMaterialisation(FlightPhase.LANDED);
        }
        ServerWorld interior = TardisManager.interiorWorld(server);
        if (interior != null) {
            BlockPos center = TardisManager.interiorCenter(data);
            setConsoleFlying(interior, center, false);
            playSound(interior, center.up(), ModSounds.TARDIS_LANDED);
        }
        BlockPos pos = data.getExteriorPos();
        notifyCrew(server, data, Text.translatable("message.doctorwho.flight.landed",
                dimensionName(data.getExteriorWorld()), pos.getX(), pos.getY(), pos.getZ()).formatted(Formatting.GREEN));
    }

    // --- Messages à l'équipage -----------------------------------------------------------

    /** Texte affiché au-dessus de la barre d'action pendant le vol, avec la progression. */
    private static Text progressMessage(TardisData data, int ticks) {
        int total = DEMAT_TICKS + VORTEX_TICKS + REMAT_TICKS;
        int elapsed = switch (data.getPhase()) {
            case DEMATERIALISING -> ticks;
            case IN_FLIGHT -> DEMAT_TICKS + ticks;
            case MATERIALISING -> DEMAT_TICKS + VORTEX_TICKS + ticks;
            default -> total;
        };
        int percent = Math.min(100, elapsed * 100 / total);
        String key = "message.doctorwho.flight.progress." + data.getPhase().name().toLowerCase();
        return Text.translatable(key, dimensionName(data.getDestinationWorld()), percent).formatted(Formatting.GOLD);
    }

    /** Envoie un message (barre d'action) aux joueurs présents dans la salle de console de ce TARDIS. */
    private static void notifyCrew(MinecraftServer server, TardisData data, Text message) {
        ServerWorld interior = TardisManager.interiorWorld(server);
        if (interior == null) {
            return;
        }
        for (ServerPlayerEntity player : interior.getPlayers()) {
            if (TardisManager.indexAt(player.getBlockPos()) == data.getIndex()) {
                player.sendMessage(message, true);
            }
        }
    }

    private static Text dimensionName(RegistryKey<World> world) {
        return Text.translatable("doctorwho.dimension." + world.getValue().getPath());
    }

    // --- Outils --------------------------------------------------------------------------

    /**
     * Trouve où se poser : deux blocs d'air au-dessus d'un sol solide, en cherchant
     * autour de la hauteur demandée. S'il n'y a aucun sol (vide de l'End...), on
     * pose une petite plateforme pour ne pas lâcher le Docteur dans le vide.
     */
    static BlockPos findLanding(ServerWorld world, BlockPos target) {
        int minY = world.getBottomY() + 1;
        int maxY = Math.min(world.getTopY(), world.getBottomY() + world.getLogicalHeight()) - 3;
        int startY = MathHelper.clamp(target.getY(), minY, maxY);
        int x = target.getX();
        int z = target.getZ();
        world.getChunk(x >> 4, z >> 4);
        for (int d = 0; d <= maxY - minY; d++) {
            for (int y : new int[]{startY - d, startY + d}) {
                BlockPos pos = new BlockPos(x, y, z);
                if (y >= minY && y <= maxY && canLand(world, pos)) {
                    return pos;
                }
            }
        }
        int top = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
        BlockPos pos = new BlockPos(x, top > minY ? Math.min(top, maxY) : startY, z);
        for (BlockPos floor : BlockPos.iterate(pos.add(-1, -1, -1), pos.add(1, -1, 1))) {
            world.setBlockState(floor, Blocks.SMOOTH_STONE.getDefaultState(), Block.NOTIFY_ALL);
        }
        return pos;
    }

    private static boolean canLand(ServerWorld world, BlockPos pos) {
        BlockState here = world.getBlockState(pos);
        BlockState above = world.getBlockState(pos.up());
        BlockPos below = pos.down();
        return here.isReplaceable() && here.getFluidState().isEmpty()
                && above.isReplaceable() && above.getFluidState().isEmpty()
                && world.getBlockState(below).isSideSolidFullSquare(world, below, Direction.UP);
    }

    @Nullable
    public static TardisBlockEntity exteriorBlock(MinecraftServer server, TardisData data) {
        ServerWorld world = server.getWorld(data.getExteriorWorld());
        if (world == null || !(world.getBlockState(data.getExteriorPos()).getBlock() instanceof TardisBlock)) {
            return null;
        }
        if (world.getBlockEntity(data.getExteriorPos()) instanceof TardisBlockEntity blockEntity) {
            UUID id = blockEntity.getTardisId();
            return id == null || id.equals(data.getId()) ? blockEntity : null;
        }
        return null;
    }

    private static void setConsoleFlying(ServerWorld interior, BlockPos center, boolean flying) {
        BlockPos consolePos = InteriorBuilder.consolePos(center);
        BlockState state = interior.getBlockState(consolePos);
        if (state.getBlock() instanceof TardisConsoleBlock) {
            interior.setBlockState(consolePos, state.with(TardisConsoleBlock.FLYING, flying), Block.NOTIFY_ALL);
        }
    }

    private static void playSound(ServerWorld world, BlockPos pos, SoundEvent sound) {
        world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1.5f, 1.0f);
    }
}
