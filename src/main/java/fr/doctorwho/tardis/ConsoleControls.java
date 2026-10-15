package fr.doctorwho.tardis;

import fr.doctorwho.block.AbstractTardisDoorBlock;
import fr.doctorwho.network.ModNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * Commandes directes de la console : un clic droit sur un pupitre déclenche sa commande.
 * <pre>
 *            nord
 *        5 frein   0 manette
 *   ouest              est        (vu de dessus ; le pupitre 0 regarde le nord)
 *        4 écran   1 boutons
 *        3 sonic   2 clavier
 *             sud (porte)
 * </pre>
 * <ul>
 *     <li>0 · manette des gaz : <b>retour rapide</b> (revient là où était le TARDIS avant son dernier voyage) ;</li>
 *     <li>1 · boutons : ouvrir / fermer les portes ;</li>
 *     <li>2 · clavier et 3 · logement du tournevis : panneau de vol ;</li>
 *     <li>4 · écran : le scanner annonce où l'on est ;</li>
 *     <li>5 · frein et cadrans : <b>saut aléatoire</b> (500 à 2000 blocs, même dimension).</li>
 * </ul>
 * Le centre (la colonne) et un clic accroupi ouvrent toujours le panneau de vol complet.
 */
public final class ConsoleControls {

    /** Pupitres (dans l'ordre des os panel_0 à panel_5 du modèle). */
    public enum Panel { THROTTLE, DOORS, KEYBOARD, SONIC, SCANNER, RANDOM }

    private ConsoleControls() {
    }

    /**
     * Quel pupitre est visé, d'après le point cliqué (relatif au centre du bloc de la console).
     * Renvoie null au centre (la colonne du rotor).
     */
    public static Panel panelAt(BlockPos console, Vec3d hit) {
        double dx = hit.x - (console.getX() + 0.5);
        double dz = hit.z - (console.getZ() + 0.5);
        if (dx * dx + dz * dz < 1.0) {
            return null;
        }
        // Angle depuis le nord, dans le sens des aiguilles d'une montre vu de dessus (nord 0, est 90)
        double angle = Math.toDegrees(Math.atan2(dx, -dz));
        int index = Math.floorMod((int) Math.round(angle / 60.0), 6);
        return Panel.values()[index];
    }

    /**
     * Ce que lit le tournevis en mode Lecture : nom du pupitre et sa commande
     * (ou la colonne du rotor si {@code panel} est null).
     */
    public static Text describe(@Nullable Panel panel) {
        String key = panel == null ? "rotor" : panel.name().toLowerCase(java.util.Locale.ROOT);
        return Text.translatable("console.doctorwho.panel." + key).formatted(Formatting.LIGHT_PURPLE)
                .append(Text.literal(" — ").formatted(Formatting.DARK_GRAY))
                .append(Text.translatable("console.doctorwho.panel." + key + ".action").formatted(Formatting.WHITE));
    }

    /** Exécute la commande du pupitre. Renvoie faux s'il faut plutôt ouvrir le panneau de vol. */
    public static boolean use(ServerPlayerEntity player, TardisData data, Panel panel) {
        ServerWorld world = player.getServerWorld();
        switch (panel) {
            case THROTTLE -> {
                click(player, SoundEvents.BLOCK_LEVER_CLICK, 0.6f);
                if (data.getPreviousWorld() == null) {
                    player.sendMessage(Text.translatable("message.doctorwho.console.no_return").formatted(Formatting.GRAY), true);
                    return true;
                }
                takeOff(player, data, data.getPreviousWorld(), data.getPreviousPos(), "message.doctorwho.console.fast_return");
                return true;
            }
            case DOORS -> {
                click(player, SoundEvents.BLOCK_STONE_BUTTON_CLICK_ON, 1.2f);
                BlockPos door = InteriorBuilder.doorPos(TardisManager.interiorCenter(data));
                BlockState state = world.getBlockState(door);
                if (state.getBlock() instanceof AbstractTardisDoorBlock) {
                    TardisManager.toggleFromInside(world, door, state, player);
                }
                return true;
            }
            case SCANNER -> {
                click(player, SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 1.6f);
                BlockPos pos = data.getExteriorPos();
                Text where = Text.translatable("doctorwho.dimension." + data.getExteriorWorld().getValue().getPath());
                player.sendMessage(Text.translatable("message.doctorwho.console.scanner", where,
                        pos.getX(), pos.getY(), pos.getZ()).formatted(Formatting.AQUA), true);
                return true;
            }
            case RANDOM -> {
                click(player, SoundEvents.BLOCK_LEVER_CLICK, 0.8f);
                Random random = world.getRandom();
                double angle = random.nextDouble() * Math.PI * 2;
                int distance = 500 + random.nextInt(1500);
                BlockPos from = data.getExteriorPos();
                BlockPos target = new BlockPos(from.getX() + (int) (Math.cos(angle) * distance), 80,
                        from.getZ() + (int) (Math.sin(angle) * distance));
                takeOff(player, data, data.getExteriorWorld(), target, "message.doctorwho.console.random");
                return true;
            }
            default -> {
                return false; // clavier, logement du tournevis : panneau de vol
            }
        }
    }

    /** Ouvre le panneau de vol complet. */
    public static void openPanel(ServerPlayerEntity player, TardisData data) {
        ModNetworking.openFlightPanel(player, data);
    }

    private static void takeOff(ServerPlayerEntity player, TardisData data,
                                net.minecraft.registry.RegistryKey<net.minecraft.world.World> world, BlockPos target, String message) {
        Text error = TardisFlight.takeOff(player.getServer(), data, world, target);
        player.sendMessage(error != null ? error : Text.translatable(message).formatted(Formatting.GOLD), true);
    }

    private static void click(ServerPlayerEntity player, SoundEvent sound, float pitch) {
        player.getServerWorld().playSound(null, player.getBlockPos(), sound, SoundCategory.BLOCKS, 0.7f, pitch);
    }
}
