package fr.doctorwho.client.renderer;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.block.entity.TardisBlockEntity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;

/**
 * Modèle de l'extérieur du TARDIS : géométrie et animations communes,
 * texture selon la variante choisie avec le circuit caméléon.
 */
public class TardisExteriorModel extends GeoModel<TardisBlockEntity> {

    private static final Identifier MODEL = DoctorWhoMod.id("geo/block/tardis.geo.json");
    private static final Identifier ANIMATIONS = DoctorWhoMod.id("animations/block/tardis.animation.json");

    @Override
    public Identifier getModelResource(TardisBlockEntity tardis) {
        return MODEL;
    }

    @Override
    public Identifier getTextureResource(TardisBlockEntity tardis) {
        return DoctorWhoMod.id("textures/block/tardis/" + tardis.getVariant() + ".png");
    }

    @Override
    public Identifier getAnimationResource(TardisBlockEntity tardis) {
        return ANIMATIONS;
    }
}
