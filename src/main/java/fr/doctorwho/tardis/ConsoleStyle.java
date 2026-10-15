package fr.doctorwho.tardis;

import net.minecraft.util.StringIdentifiable;

/**
 * Modèle de console, assorti à chaque décor (voir {@link Variants#DESKTOPS}).
 * Fichiers : geo/block/console_&lt;id&gt;.geo.json et textures/block/console_&lt;id&gt;.png,
 * générés par {@code tools/generate_assets.py} ({@code consoles()}).
 */
public enum ConsoleStyle implements StringIdentifiable {
    /** 2005 : corail, colonne verte, six branches de corail. */
    CORAL("coral"),
    /** 2010 : cuivre et verre, cage de cuivre au-dessus. */
    COPPER("copper"),
    /** Années 70 : blanche, à roundels. */
    WHITE("white"),
    /** 2014 : sombre et dorée, grand anneau suspendu. */
    DARK("dark"),
    /** 1963 : blanche et grise, colonne d'instruments. */
    COLUMNS("columns"),
    /** 1996 : bois et laiton, dais au-dessus. */
    VICTORIAN("victorian"),
    /** 1996 (le film) : acier bleu et laiton, arcs gothiques au-dessus. */
    CATHEDRAL("cathedral"),
    /** 2012 : blanche et turquoise, socle lumineux cyan. */
    TURQUOISE("turquoise");

    private final String id;

    ConsoleStyle(String id) {
        this.id = id;
    }

    @Override
    public String asString() {
        return id;
    }

    /** La console assortie à un décor. */
    public static ConsoleStyle forDesktop(String desktop) {
        String id = Variants.desktop(desktop);
        for (ConsoleStyle style : values()) {
            if (style.id.equals(id)) {
                return style;
            }
        }
        return DARK;
    }
}
