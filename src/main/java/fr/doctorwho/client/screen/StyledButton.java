package fr.doctorwho.client.screen;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Bouton plat de l'interface du TARDIS : fond sombre translucide, liseré coloré,
 * surbrillance au survol. Remplace la texture « pierre » des boutons vanilla.
 */
@Environment(EnvType.CLIENT)
public class StyledButton extends ButtonWidget {

    private final int accent;
    private boolean selected;
    /** Couleur d'une pastille ronde dessinée à gauche du texte (0 = aucune). */
    private int dotColor;
    private boolean big;

    public StyledButton(int x, int y, int width, int height, Text message, int accent, PressAction onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION_SUPPLIER);
        this.accent = accent;
    }

    public StyledButton selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public StyledButton dot(int color) {
        this.dotColor = color;
        return this;
    }

    public StyledButton big() {
        this.big = true;
        return this;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        int x0 = getX();
        int y0 = getY();
        int x1 = x0 + getWidth();
        int y1 = y0 + getHeight();
        boolean hovered = isSelected();
        int accentRgb = accent & 0xFFFFFF;

        int background;
        if (!active) {
            background = 0x60202630;
        } else if (selected) {
            background = 0x70000000 | accentRgb;
        } else if (hovered) {
            background = 0x40000000 | accentRgb;
        } else {
            background = 0xA0141C28;
        }
        if (big && active) {
            // Dégradé vertical sur le gros bouton
            context.fillGradient(x0, y0, x1, y1, 0xD0000000 | accentRgb, 0xA0000000 | darker(accentRgb));
        } else {
            context.fill(x0, y0, x1, y1, background);
        }

        int border = !active ? 0x60808890 : (selected || hovered ? 0xFF000000 | accentRgb : 0x90000000 | accentRgb);
        if (big && active) {
            // Liseré qui « respire »
            float pulse = 0.55f + 0.45f * (float) Math.sin(System.currentTimeMillis() / 300.0);
            border = ((int) (255 * pulse) << 24) | 0xFFE7C0;
        }
        drawBorder(context, x0, y0, x1, y1, border);

        TextRenderer font = MinecraftClient.getInstance().textRenderer;
        int textColor = !active ? 0xFF7A8088 : 0xFFEAF6FF;
        int textX = x0 + getWidth() / 2;
        if (dotColor != 0) {
            int cx = x0 + 9;
            int cy = y0 + getHeight() / 2;
            fillCircle(context, cx, cy, 4, 0xFF000000 | dotColor);
            context.fill(cx - 2, cy - 2, cx, cy, 0x50FFFFFF); // reflet
            textX = x0 + 18 + (getWidth() - 18) / 2;
        }
        // Texte raccourci s'il ne tient pas dans le bouton
        int available = getWidth() - (dotColor != 0 ? 22 : 6);
        Text label = getMessage();
        if (font.getWidth(label) > available) {
            label = Text.literal(font.trimToWidth(label.getString(), available - font.getWidth("…")) + "…");
        }
        context.drawCenteredTextWithShadow(font, label, textX, y0 + (getHeight() - 8) / 2, textColor);
    }

    private static int darker(int rgb) {
        int r = (rgb >> 16 & 0xFF) * 3 / 5;
        int g = (rgb >> 8 & 0xFF) * 3 / 5;
        int b = (rgb & 0xFF) * 3 / 5;
        return r << 16 | g << 8 | b;
    }

    static void drawBorder(DrawContext context, int x0, int y0, int x1, int y1, int color) {
        context.fill(x0, y0, x1, y0 + 1, color);
        context.fill(x0, y1 - 1, x1, y1, color);
        context.fill(x0, y0 + 1, x0 + 1, y1 - 1, color);
        context.fill(x1 - 1, y0 + 1, x1, y1 - 1, color);
    }

    static void fillCircle(DrawContext context, int cx, int cy, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int half = (int) Math.round(Math.sqrt(radius * radius - dy * dy));
            context.fill(cx - half, cy + dy, cx + half + 1, cy + dy + 1, color);
        }
    }
}
