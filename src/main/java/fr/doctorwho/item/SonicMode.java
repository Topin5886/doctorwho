package fr.doctorwho.item;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Modes du tournevis sonique, changés avec accroupi + clic droit. */
public enum SonicMode {
    OUVERTURE("ouverture", Formatting.AQUA),
    ACTIVATION("activation", Formatting.GOLD),
    ANALYSE("analyse", Formatting.GREEN),
    /** Lecture : indique ce que fait chaque pupitre de la console du TARDIS, sans ouvrir le panneau. */
    LECTURE("lecture", Formatting.LIGHT_PURPLE);

    private final String key;
    private final Formatting color;

    SonicMode(String key, Formatting color) {
        this.key = key;
        this.color = color;
    }

    public Text getDisplayName() {
        return Text.translatable("sonic_mode.doctorwho." + key).formatted(color);
    }

    public SonicMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static SonicMode byOrdinal(int ordinal) {
        SonicMode[] modes = values();
        return ordinal >= 0 && ordinal < modes.length ? modes[ordinal] : OUVERTURE;
    }
}
