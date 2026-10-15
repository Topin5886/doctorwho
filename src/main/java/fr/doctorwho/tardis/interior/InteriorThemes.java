package fr.doctorwho.tardis.interior;

import fr.doctorwho.tardis.Variants;
import net.minecraft.block.Blocks;

import java.util.HashMap;
import java.util.Map;

/**
 * Registre des thèmes de salle de console, par identifiant de décor ({@code Variants.DESKTOPS}).
 * Pour ajouter un thème : créer sa classe (voir {@link ClassicTheme}) et l'enregistrer ici.
 */
public final class InteriorThemes {

    private static final Map<String, InteriorTheme> THEMES = new HashMap<>();

    static {
        register(new ClassicTheme());
        register(new CopperTheme());
        register(new AmberTheme());
        // Décors pas encore détaillés : grande salle commune avec leur palette
        register(new PaletteTheme("dark", Blocks.POLISHED_BLACKSTONE_STAIRS.getDefaultState(), Blocks.IRON_BARS.getDefaultState()));
        register(new PaletteTheme("columns", Blocks.QUARTZ_STAIRS.getDefaultState(), Blocks.WHITE_STAINED_GLASS_PANE.getDefaultState()));
        register(new PaletteTheme("victorian", Blocks.DARK_OAK_STAIRS.getDefaultState(), Blocks.DARK_OAK_FENCE.getDefaultState()));
    }

    private InteriorThemes() {
    }

    private static void register(InteriorTheme theme) {
        THEMES.put(theme.id(), theme);
    }

    /** Le thème d'un décor (le décor par défaut si l'identifiant est inconnu). */
    public static InteriorTheme get(String desktop) {
        InteriorTheme theme = THEMES.get(Variants.desktop(desktop));
        return theme != null ? theme : THEMES.get(Variants.DEFAULT_DESKTOP);
    }
}
