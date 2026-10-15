package fr.doctorwho.client.renderer;

import fr.doctorwho.block.entity.AbstractTardisDoorBlockEntity;
import fr.doctorwho.block.entity.TardisBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/**
 * Rendu GeckoLib des portes du TARDIS : l'extérieur ({@link TardisExteriorModel},
 * texture selon la variante) et la porte intérieure (modèle « tardis_door »).
 * <p>
 * Pendant une (dé)matérialisation, la cabine est rendue translucide et clignote ;
 * le reste du temps, rendu normal (meilleur pour Sodium / Iris).
 */
public class TardisRenderer<T extends AbstractTardisDoorBlockEntity> extends GeoBlockRenderer<T> {

    public TardisRenderer(GeoModel<T> model) {
        super(model);
        addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            public void render(MatrixStack poseStack, T animatable, BakedGeoModel bakedModel, RenderLayer renderType,
                               VertexConsumerProvider bufferSource, VertexConsumer buffer, float partialTick,
                               int packedLight, int packedOverlay) {
                // Les parties lumineuses s'éteignent pendant que la cabine s'efface
                if (alpha(animatable, partialTick) >= 1.0f) {
                    super.render(poseStack, animatable, bakedModel, renderType, bufferSource, buffer, partialTick,
                            packedLight, packedOverlay);
                }
            }
        });
    }

    private static float alpha(AbstractTardisDoorBlockEntity animatable, float partialTick) {
        return animatable instanceof TardisBlockEntity exterior ? exterior.getAlpha(partialTick) : 1.0f;
    }

    @Override
    public RenderLayer getRenderType(T animatable, Identifier texture, VertexConsumerProvider bufferSource, float partialTick) {
        if (alpha(animatable, partialTick) < 1.0f) {
            return RenderLayer.getEntityTranslucent(texture);
        }
        return super.getRenderType(animatable, texture, bufferSource, partialTick);
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        return Color.ofRGBA(1.0f, 1.0f, 1.0f, alpha(animatable, partialTick));
    }
}
