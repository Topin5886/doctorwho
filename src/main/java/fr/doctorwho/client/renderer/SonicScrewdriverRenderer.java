package fr.doctorwho.client.renderer;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.item.SonicScrewdriverItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Rendu 3D du tournevis sonique. Le modèle et la texture dépendent de la variante
 * enregistrée dans l'objet : geo/item/sonic/&lt;variante&gt;.geo.json et
 * textures/item/sonic/&lt;variante&gt;.png. L'animation (os « emitter ») est commune.
 */
public class SonicScrewdriverRenderer extends GeoItemRenderer<SonicScrewdriverItem> {

    private static final Identifier ANIMATIONS = DoctorWhoMod.id("animations/item/sonic_screwdriver.animation.json");

    public SonicScrewdriverRenderer() {
        super(new SonicModel());
        ((SonicModel) getGeoModel()).renderer = this;
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    private static class SonicModel extends GeoModel<SonicScrewdriverItem> {

        private SonicScrewdriverRenderer renderer;

        private String variant() {
            ItemStack stack = renderer == null ? null : renderer.getCurrentItemStack();
            return stack == null ? "tenth" : SonicScrewdriverItem.getVariant(stack);
        }

        @Override
        public Identifier getModelResource(SonicScrewdriverItem item) {
            return DoctorWhoMod.id("geo/item/sonic/" + variant() + ".geo.json");
        }

        @Override
        public Identifier getTextureResource(SonicScrewdriverItem item) {
            return DoctorWhoMod.id("textures/item/sonic/" + variant() + ".png");
        }

        @Override
        public Identifier getAnimationResource(SonicScrewdriverItem item) {
            return ANIMATIONS;
        }
    }
}
