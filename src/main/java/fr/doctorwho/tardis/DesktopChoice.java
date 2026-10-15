package fr.doctorwho.tardis;

import fr.doctorwho.block.AbstractTardisDoorBlock;
import fr.doctorwho.block.TardisBlock;
import fr.doctorwho.network.ModNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Choix du décor à la première entrée dans un TARDIS neuf.
 * <p>
 * Le joueur qui passe les portes d'un TARDIS dont l'intérieur n'existe pas encore
 * reçoit l'écran de choix ; sa réponse construit la salle de console avec ce décor
 * puis le fait entrer. S'il annule (Échap), il ressort simplement de la cabine.
 */
public final class DesktopChoice {

    /** Au-delà (en ticks), une question restée sans réponse est oubliée et peut être reposée. */
    private static final int TIMEOUT = 20 * 60;

    /** Le TARDIS devant lequel attend chaque joueur à qui on a posé la question. */
    private record Pending(RegistryKey<World> world, BlockPos pos, int askedAt) {
    }

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private DesktopChoice() {
    }

    /** Ouvre l'écran de choix (une seule fois tant que le joueur n'a pas répondu). */
    public static void ask(ServerPlayerEntity player, ServerWorld world, BlockPos lowerPos) {
        int now = world.getServer().getTicks();
        Pending pending = PENDING.get(player.getUuid());
        if (pending != null && now - pending.askedAt() < TIMEOUT) {
            return;
        }
        PENDING.put(player.getUuid(), new Pending(world.getRegistryKey(), lowerPos.toImmutable(), now));
        ModNetworking.openDesktopChoice(player);
    }

    /** Réponse du joueur : un décor de {@link Variants#DESKTOPS}, ou "" s'il a annulé. */
    public static void answer(ServerPlayerEntity player, String desktop) {
        Pending pending = PENDING.remove(player.getUuid());
        if (pending == null) {
            return;
        }
        ServerWorld world = player.getServer().getWorld(pending.world());
        if (world == null || player.getWorld() != world) {
            return;
        }
        BlockState state = world.getBlockState(pending.pos());
        if (!(state.getBlock() instanceof TardisBlock)) {
            return;
        }
        if (!Variants.DESKTOPS.contains(desktop)) {
            stepOut(player, world, pending.pos(), state);
            return;
        }
        if (player.squaredDistanceTo(Vec3d.ofBottomCenter(pending.pos())) > 16) {
            return; // le joueur s'est éloigné entre-temps
        }
        TardisManager.enterWithDesktop(player, world, pending.pos(), state, desktop);
    }

    /** Le joueur s'est déconnecté : on oublie sa question en cours. */
    public static void forget(UUID player) {
        PENDING.remove(player);
    }

    /** Ramène le joueur juste devant les portes, sans qu'il ne rentre aussitôt. */
    private static void stepOut(ServerPlayerEntity player, ServerWorld world, BlockPos lowerPos, BlockState state) {
        Direction front = state.get(AbstractTardisDoorBlock.FACING);
        Vec3d out = Vec3d.ofBottomCenter(lowerPos).add(front.getOffsetX() * 1.3, 0, front.getOffsetZ() * 1.3);
        player.teleport(world, out.x, out.y, out.z, Set.of(), front.asRotation(), player.getPitch());
        player.setPortalCooldown(40);
    }
}
