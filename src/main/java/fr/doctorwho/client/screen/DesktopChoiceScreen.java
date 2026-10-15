package fr.doctorwho.client.screen;

import fr.doctorwho.network.ModNetworking;
import fr.doctorwho.tardis.Variants;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

/**
 * Écran affiché à la première entrée dans un TARDIS neuf : on choisit le décor
 * de la salle de console parmi des cartes illustrées. Échap = ressortir de la cabine.
 */
@Environment(EnvType.CLIENT)
public class DesktopChoiceScreen extends Screen {

    private static final int TEAL = 0x2FC4E8;
    private static final int ORANGE = 0xE8913A;

    private static final int CARD_W = 94;
    private static final int CARD_H = 94;
    private static final int GAP = 6;
    private static final int MAX_COLUMNS = 3;

    /**
     * Couleurs de la petite illustration de chaque décor.
     *
     * @param books une rangée de bibliothèques au pied du mur
     */
    private record Look(int wall, int roundel, int floor, int trim, int glow, boolean books, boolean columns) {
    }

    private static final Look DEFAULT_LOOK = new Look(0x3A4250, 0x8A96A8, 0x262A30, 0x707C8C, 0x2FC4E8, false, false);
    private static final Map<String, Look> LOOKS = Map.of(
            "coral", new Look(0xB0602E, 0xF0A45C, 0x3E3A36, 0xC8783A, 0x5CF0C8, false, false),
            "copper", new Look(0x8C5428, 0xE0904A, 0xA8D4EC, 0xC87533, 0x2FC4E8, false, false),
            "white", new Look(0xD8D8D2, 0xF6F6F2, 0xC4C4BE, 0x9A9A96, 0x7FD0FF, false, false),
            "dark", new Look(0x343A44, 0x98A2B0, 0x1E2126, 0x5A6170, 0xFFB050, true, false),
            "columns", new Look(0xE2E2DC, 0xFAFAF6, 0x8A8C8E, 0xB0B0AA, 0x9FE0FF, false, true),
            "victorian", new Look(0x4A3020, 0xE8A050, 0x7A5A3A, 0x8C6A48, 0xFFC870, true, true));

    private int left;
    private int top;
    private int panelW;
    private int panelH;
    private boolean answered;

    public DesktopChoiceScreen() {
        super(Text.translatable("screen.doctorwho.desktop_choice.title"));
    }

    @Override
    protected void init() {
        List<String> desktops = Variants.DESKTOPS;
        int columns = Math.min(MAX_COLUMNS, desktops.size());
        int rows = (desktops.size() + columns - 1) / columns;
        panelW = columns * CARD_W + (columns - 1) * GAP + 24;
        panelH = 50 + rows * (CARD_H + GAP) + 12;
        left = (width - panelW) / 2;
        top = Math.max(2, (height - panelH) / 2);

        for (int i = 0; i < desktops.size(); i++) {
            String desktop = desktops.get(i);
            int x = left + 12 + (i % columns) * (CARD_W + GAP);
            int y = top + 50 + (i / columns) * (CARD_H + GAP);
            addDrawableChild(new DesktopCard(x, y, desktop, button -> choose(desktop)));
        }
    }

    private void choose(String desktop) {
        answered = true;
        send(desktop);
        super.close();
    }

    /** Échap : le joueur ressort du TARDIS sans le construire. */
    @Override
    public void close() {
        if (!answered) {
            answered = true;
            send("");
        }
        super.close();
    }

    private static void send(String desktop) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(desktop);
        ClientPlayNetworking.send(ModNetworking.CHOOSE_DESKTOP, buf);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        int right = left + panelW;
        int bottom = top + panelH;
        context.fillGradient(left, top, right, bottom, 0xF0121C2A, 0xF0080C14);
        StyledButton.drawBorder(context, left, top, right, bottom, 0xC0000000 | TEAL);
        context.fill(left + 1, top + 1, right - 1, top + 3, 0xFF000000 | ORANGE);
        FlightPanelScreen.drawSeal(context, right - 40, top + 30, 120, 0.08f);
        FlightPanelScreen.drawSeal(context, left + 20, top + 15, 18, 0.85f);

        context.drawTextWithShadow(textRenderer, title, left + 34, top + 11, 0xFF000000 | TEAL);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.doctorwho.desktop_choice.line1"),
                left + 12, top + 25, 0xFFEAF6FF);
        context.drawTextWithShadow(textRenderer, Text.translatable("screen.doctorwho.desktop_choice.line2"),
                left + 12, top + 36, 0xFF7F94A8);
        Text hint = Text.translatable("screen.doctorwho.desktop_choice.cancel");
        context.drawTextWithShadow(textRenderer, hint, right - 8 - textRenderer.getWidth(hint), bottom - 11, 0xFF5E6E80);
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    /** Une carte cliquable : illustration de la salle, nom du décor et époque. */
    private static class DesktopCard extends ButtonWidget {

        private final String desktop;
        private final Look look;

        DesktopCard(int x, int y, String desktop, PressAction onPress) {
            super(x, y, CARD_W, CARD_H, Variants.desktopName(desktop), onPress, DEFAULT_NARRATION_SUPPLIER);
            this.desktop = desktop;
            this.look = LOOKS.getOrDefault(desktop, DEFAULT_LOOK);
        }

        @Override
        protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
            int x0 = getX();
            int y0 = getY();
            int x1 = x0 + width;
            int y1 = y0 + height;
            boolean hovered = isSelected();
            context.fill(x0, y0, x1, y1, hovered ? 0x602FC4E8 : 0xA0141C28);
            drawRoom(context, x0 + 4, y0 + 4, x1 - 4, y0 + 56, hovered);
            StyledButton.drawBorder(context, x0, y0, x1, y1, hovered ? 0xFF000000 | TEAL : 0x702FC4E8);

            TextRenderer font = MinecraftClient.getInstance().textRenderer;
            int cx = x0 + width / 2;
            context.drawCenteredTextWithShadow(font, getMessage(), cx, y0 + 60, 0xFFEAF6FF);
            if (desktop.equals(Variants.DEFAULT_DESKTOP)) {
                // Étoile : le décor par défaut (celui de David)
                context.drawTextWithShadow(font, Text.literal("★"), cx - font.getWidth(getMessage()) / 2 - 10, y0 + 60,
                        0xFF000000 | ORANGE);
            }
            Text era = Text.translatable("variant.doctorwho.desktop." + desktop + ".era");
            for (int i = 0; i < 2 && i < font.wrapLines(era, width - 6).size(); i++) {
                context.drawCenteredTextWithShadow(font, font.wrapLines(era, width - 6).get(i), cx, y0 + 71 + i * 10, 0xFF8A98A8);
            }
        }

        /** Petite salle de console : murs à roundels, sol, console et colonne lumineuse. */
        private void drawRoom(DrawContext context, int x0, int y0, int x1, int y1, boolean hovered) {
            int floorTop = y1 - 14;
            context.fillGradient(x0, y0, x1, floorTop, 0xFF000000 | look.wall(), 0xFF000000 | darker(look.wall()));
            // Deux rangées de roundels
            for (int row = 0; row < 2; row++) {
                int cy = y0 + 9 + row * 15;
                for (int cx = x0 + 8 + (row % 2) * 6; cx + 6 <= x1; cx += 13) {
                    StyledButton.fillCircle(context, cx, cy, 5, 0xFF000000 | darker(look.wall()));
                    StyledButton.fillCircle(context, cx, cy, 4, 0xFF000000 | look.roundel());
                }
            }
            if (look.books()) {
                for (int x = x0; x < x1; x += 4) {
                    int color = ((x / 4) % 3 == 0) ? 0x7A4A28 : ((x / 4) % 3 == 1) ? 0x5A3A22 : 0x8C6234;
                    context.fill(x, floorTop - 9, Math.min(x + 3, x1), floorTop, 0xFF000000 | color);
                }
            }
            if (look.columns()) {
                // Hautes colonnes le long des murs
                for (int x : new int[]{x0 + 3, x0 + 17, x1 - 22, x1 - 8}) {
                    context.fill(x, y0, x + 5, floorTop, 0xFF000000 | brighter(look.trim()));
                    context.fill(x + 4, y0, x + 5, floorTop, 0xFF000000 | darker(look.trim()));
                }
            }
            context.fill(x0, floorTop, x1, y1, 0xFF000000 | look.floor());
            context.fill(x0, floorTop, x1, floorTop + 1, 0xFF000000 | darker(look.floor()));

            // Console au centre et rotor lumineux qui « respire »
            int cx = (x0 + x1) / 2;
            float pulse = 0.6f + 0.4f * (float) Math.sin(System.currentTimeMillis() / (hovered ? 180.0 : 400.0));
            int glowAlpha = (int) (110 + 120 * pulse) << 24;
            context.fill(cx - 2, y0 + 18, cx + 3, floorTop - 6, glowAlpha | look.glow());
            context.fill(cx - 1, y0 + 16, cx + 2, floorTop - 6, 0xFF000000 | brighter(look.glow()));
            context.fill(cx - 14, floorTop - 6, cx + 15, floorTop - 2, 0xFF000000 | look.trim());
            context.fill(cx - 10, floorTop - 2, cx + 11, floorTop + 2, 0xFF000000 | darker(look.trim()));
        }

        private static int darker(int rgb) {
            return ((rgb >> 16 & 0xFF) * 3 / 5) << 16 | ((rgb >> 8 & 0xFF) * 3 / 5) << 8 | (rgb & 0xFF) * 3 / 5;
        }

        private static int brighter(int rgb) {
            int r = Math.min(255, (rgb >> 16 & 0xFF) + 80);
            int g = Math.min(255, (rgb >> 8 & 0xFF) + 80);
            int b = Math.min(255, (rgb & 0xFF) + 80);
            return r << 16 | g << 8 | b;
        }
    }
}
