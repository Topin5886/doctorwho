package fr.doctorwho.tardis;

import net.minecraft.text.Text;

import java.util.List;

/**
 * Toutes les apparences au choix depuis la console : extérieurs du TARDIS
 * (circuit caméléon), décors de la salle de console et tournevis soniques.
 * Chaque nom correspond à des fichiers de ressources :
 * <ul>
 *     <li>extérieur : textures/block/tardis/&lt;nom&gt;.png</li>
 *     <li>tournevis : geo/item/sonic/&lt;nom&gt;.geo.json et textures/item/sonic/&lt;nom&gt;.png</li>
 *     <li>décor : palette de blocs dans {@link InteriorBuilder}</li>
 * </ul>
 */
public final class Variants {

    /** 1963, années 80, 1996, 2005, 2010, 2014, 2018, 2023 et celui du War Doctor. */
    public static final List<String> EXTERIORS = List.of("classic", "fifth", "eighth", "tenth", "eleventh", "twelfth",
            "thirteenth", "fourteenth", "war");
    public static final String DEFAULT_EXTERIOR = "twelfth";

    /** Corail (2005), cuivre (2010), blanc classique, sombre (2014), colonnes (1963), victorien (1996). */
    public static final List<String> DESKTOPS = List.of("coral", "copper", "white", "dark", "columns", "victorian");
    public static final String DEFAULT_DESKTOP = "dark";

    public static final List<String> SONICS = List.of("second", "third", "fourth", "seventh", "eighth", "tenth", "eleventh",
            "twelfth", "thirteenth", "fourteenth", "war", "river", "master");
    public static final String DEFAULT_SONIC = "twelfth";

    private Variants() {
    }

    public static String exterior(String name) {
        return EXTERIORS.contains(name) ? name : DEFAULT_EXTERIOR;
    }

    public static String desktop(String name) {
        return DESKTOPS.contains(name) ? name : DEFAULT_DESKTOP;
    }

    public static String sonic(String name) {
        return SONICS.contains(name) ? name : DEFAULT_SONIC;
    }

    public static Text exteriorName(String name) {
        return Text.translatable("variant.doctorwho.exterior." + name);
    }

    public static Text desktopName(String name) {
        return Text.translatable("variant.doctorwho.desktop." + name);
    }

    public static Text sonicName(String name) {
        return Text.translatable("variant.doctorwho.sonic." + name);
    }
}
