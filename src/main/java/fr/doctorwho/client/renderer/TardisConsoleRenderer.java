package fr.doctorwho.client.renderer;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.block.entity.TardisConsoleBlockEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Rendu de la console : modèle GeckoLib (rotor animé, rendu translucide pour la
 * colonne de verre) + tournevis sonique dressé dans son logement (os « sonic_port »).
 */
public class TardisConsoleRenderer extends GeoBlockRenderer<TardisConsoleBlockEntity> {

    public TardisConsoleRenderer() {
        super(new DefaultedBlockGeoModel<>(DoctorWhoMod.id("tardis_console")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        addRenderLayer(new BlockAndItemGeoLayer<>(this, TardisConsoleRenderer::sonicForBone, (bone, console) -> null) {
            @Override
            protected ModelTransformationMode getTransformTypeForStack(GeoBone bone, ItemStack stack, TardisConsoleBlockEntity console) {
                return ModelTransformationMode.GROUND;
            }
        });
    }

    private static ItemStack sonicForBone(GeoBone bone, TardisConsoleBlockEntity console) {
        return bone.getName().equals("sonic_port") && !console.getSonic().isEmpty() ? console.getSonic() : null;
    }

    @Override
    public RenderLayer getRenderType(TardisConsoleBlockEntity animatable, Identifier texture,
                                     VertexConsumerProvider bufferSource, float partialTick) {
        return RenderLayer.getEntityTranslucent(texture);
    }
}
