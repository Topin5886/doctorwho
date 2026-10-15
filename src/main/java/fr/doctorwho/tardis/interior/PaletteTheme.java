package fr.doctorwho.tardis.interior;

import fr.doctorwho.tardis.InteriorBuilder;
import net.minecraft.block.BlockState;

/**
 * Thème construit seulement à partir d'une palette de blocs ({@link InteriorBuilder#theme}) :
 * la grande salle commune (estrade, galerie, escaliers) avec les matériaux du décor.
 * Sert aux décors qui n'ont pas encore leur propre classe détaillée.
 */
public class PaletteTheme extends InteriorTheme {

    private final String id;
    private final BlockState stairs;
    private final BlockState railing;

    public PaletteTheme(String id, BlockState stairs, BlockState railing) {
        this.id = id;
        this.stairs = stairs;
        this.railing = railing;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public InteriorBuilder.Theme palette() {
        return InteriorBuilder.theme(id);
    }

    @Override
    protected BlockState stairs() {
        return stairs;
    }

    @Override
    protected BlockState railing() {
        return railing;
    }
}
