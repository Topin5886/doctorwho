package fr.doctorwho.client.renderer;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.block.TardisConsoleBlock;
import fr.doctorwho.block.entity.TardisConsoleBlockEntity;
import fr.doctorwho.registry.ModBlocks;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;
import software.bernie.geckolib.renderer.layer.BlockAndItemGeoLayer;

/**
 * Rendu de la console : modèle GeckoLib selon son style (rotor animé, rendu translucide pour la
 * colonne de verre) + tournevis sonique dressé dans son logement (os « sonic_port »).
 * La colonne du rotor monte jusqu'au plafond de la salle, quelle que soit sa hauteur :
 * l'os « shaft » est étiré et l'os « mount » (fixation) remonté jusqu'au premier bloc au-dessus.
 */
public class TardisConsoleRenderer extends GeoBlockRenderer<TardisConsoleBlockEntity> {

    public TardisConsoleRenderer() {
        super(new ConsoleModel());
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

    /** Bas et haut de la colonne dans le modèle (en pixels), dessinée pour un plafond à 144 px. */
    private static final float SHAFT_BOTTOM = 65;
    private static final float SHAFT_TOP = 141;
    private static final float DESIGN_CEILING = 144;
    /** Au-delà, pas de plafond : la colonne est cachée (console posée en plein air). */
    private static final int MAX_CEILING = 24;

    @Override
    public void renderRecursively(MatrixStack poseStack, TardisConsoleBlockEntity console, GeoBone bone, RenderLayer renderType,
                                  VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender,
                                  float partialTick, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        String name = bone.getName();
        if (name.equals("shaft") || name.equals("mount")) {
            float ceiling = ceilingPixels(console);
            boolean visible = ceiling > SHAFT_BOTTOM + 6;
            bone.setHidden(!visible);
            if (visible) {
                if (name.equals("shaft")) {
                    bone.setScaleY((ceiling - (DESIGN_CEILING - SHAFT_TOP) - SHAFT_BOTTOM) / (SHAFT_TOP - SHAFT_BOTTOM));
                } else {
                    bone.setPosY(ceiling - DESIGN_CEILING);
                }
            }
        }
        super.renderRecursively(poseStack, console, bone, renderType, bufferSource, buffer, isReRender, partialTick,
                packedLight, packedOverlay, red, green, blue, alpha);
    }

    /** Hauteur du plafond au-dessus du bas de la console, en pixels (0 s'il n'y en a pas). */
    private static float ceilingPixels(TardisConsoleBlockEntity console) {
        World world = console.getWorld();
        if (world == null) {
            return 0;
        }
        BlockPos.Mutable pos = console.getPos().mutableCopy();
        for (int dy = 1; dy <= MAX_CEILING; dy++) {
            BlockState state = world.getBlockState(pos.move(0, 1, 0));
            if (!state.isAir() && !state.isOf(ModBlocks.CONSOLE_PART)) {
                return dy * 16;
            }
        }
        return 0;
    }

    /** La console dépasse largement de son bloc : on la dessine même si son bloc est hors champ. */
    @Override
    public boolean rendersOutsideBoundingBox(TardisConsoleBlockEntity console) {
        return true;
    }

    @Override
    public RenderLayer getRenderType(TardisConsoleBlockEntity animatable, Identifier texture,
                                     VertexConsumerProvider bufferSource, float partialTick) {
        return RenderLayer.getEntityTranslucent(texture);
    }

    /** Un modèle et une texture par style ; une seule animation (os « rotor » et « throttle » communs). */
    private static class ConsoleModel extends DefaultedBlockGeoModel<TardisConsoleBlockEntity> {

        ConsoleModel() {
            super(DoctorWhoMod.id("tardis_console"));
        }

        private static String style(TardisConsoleBlockEntity console) {
            return console.getCachedState().contains(TardisConsoleBlock.STYLE)
                    ? console.getCachedState().get(TardisConsoleBlock.STYLE).asString() : "dark";
        }

        @Override
        public Identifier getModelResource(TardisConsoleBlockEntity console) {
            return DoctorWhoMod.id("geo/block/console_" + style(console) + ".geo.json");
        }

        @Override
        public Identifier getTextureResource(TardisConsoleBlockEntity console) {
            return DoctorWhoMod.id("textures/block/console_" + style(console) + ".png");
        }
    }
}
