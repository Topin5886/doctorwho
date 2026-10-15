package fr.doctorwho.client.screen;

import fr.doctorwho.block.ConsolePartBlock;
import fr.doctorwho.block.TardisConsoleBlock;
import fr.doctorwho.item.SonicMode;
import fr.doctorwho.item.SonicScrewdriverItem;
import fr.doctorwho.tardis.ConsoleControls;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;

/**
 * Mode Lecture du tournevis sonique : tant qu'on vise la console du TARDIS avec le tournevis
 * en main, le nom du pupitre visé et sa commande s'affichent sous le viseur.
 * On sait ainsi sur quoi on appuie, sans ouvrir le panneau de vol.
 */
public final class SonicReadingHud {

    private SonicReadingHud() {
    }

    public static void register() {
        HudRenderCallback.EVENT.register(SonicReadingHud::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null || client.options.hudHidden || !holdsReadingSonic(client)) {
            return;
        }
        if (!(client.crosshairTarget instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = client.world.getBlockState(pos);
        BlockPos console = state.getBlock() instanceof TardisConsoleBlock ? pos
                : state.getBlock() instanceof ConsolePartBlock ? ConsolePartBlock.findConsole(client.world, pos) : null;
        if (console == null) {
            return;
        }
        Text text = ConsoleControls.describe(ConsoleControls.panelAt(console, hit.getPos()));
        int width = client.textRenderer.getWidth(text);
        int x = (context.getScaledWindowWidth() - width) / 2;
        int y = context.getScaledWindowHeight() / 2 + 12;
        context.fill(x - 4, y - 3, x + width + 4, y + 11, 0xA0101828);
        context.drawTextWithShadow(client.textRenderer, text, x, y, 0xFFFFFF);
    }

    private static boolean holdsReadingSonic(MinecraftClient client) {
        for (Hand hand : Hand.values()) {
            ItemStack stack = client.player.getStackInHand(hand);
            if (stack.getItem() instanceof SonicScrewdriverItem && SonicScrewdriverItem.getMode(stack) == SonicMode.LECTURE) {
                return true;
            }
        }
        return false;
    }
}
