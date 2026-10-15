package fr.doctorwho.client.renderer;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.entity.DalekEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Rendu GeckoLib des créatures : modèle geo/entity/&lt;nom&gt;.geo.json, texture
 * textures/entity/&lt;nom&gt;.png (+ _glowmask pour les yeux et lumières),
 * animations animations/entity/&lt;nom&gt;.animation.json. L'os « head » suit le regard.
 */
public class CreatureRenderer<T extends Entity & GeoAnimatable> extends GeoEntityRenderer<T> {

    /**
     * @param glowing vrai si la créature a des parties lumineuses (texture _glowmask non vide).
     *                Attention : GeckoLib plante sur une texture lumineuse vide.
     */
    public CreatureRenderer(EntityRendererFactory.Context context, GeoModel<T> model, float shadow, boolean glowing) {
        super(context, model);
        this.shadowRadius = shadow;
        if (glowing) {
            addRenderLayer(new AutoGlowingGeoLayer<>(this));
        }
    }

    public CreatureRenderer(EntityRendererFactory.Context context, String name, float shadow, boolean glowing) {
        this(context, new DefaultedEntityGeoModel<>(DoctorWhoMod.id(name), true), shadow, glowing);
    }

    /** Dalek : un seul modèle, une texture par couleur (textures/entity/dalek/&lt;couleur&gt;.png). */
    public static class DalekModel extends DefaultedEntityGeoModel<DalekEntity> {

        public DalekModel() {
            super(DoctorWhoMod.id("dalek"), true);
        }

        @Override
        public Identifier getTextureResource(DalekEntity dalek) {
            return DoctorWhoMod.id("textures/entity/dalek/" + dalek.getColor() + ".png");
        }
    }
}
