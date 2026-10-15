package fr.doctorwho.client.renderer;

import fr.doctorwho.entity.DisguisedMonsterEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.VillagerResemblingModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

/**
 * Rendu d'un monstre déguisé (Zygon, Slitheen) : tant qu'il est déguisé, on dessine
 * un villageois ordinaire (modèle et textures du jeu, métier selon {@link DisguisedMonsterEntity#getLook()}),
 * puis sa vraie forme GeckoLib une fois démasqué.
 */
public class DisguisedCreatureRenderer<T extends DisguisedMonsterEntity> extends CreatureRenderer<T> {

    private static final Identifier VILLAGER = new Identifier("textures/entity/villager/villager.png");
    private static final Identifier PLAINS = new Identifier("textures/entity/villager/type/plains.png");
    /** Métiers imités : le premier (0) est un villageois sans métier. */
    private static final String[] PROFESSIONS = {"none", "farmer", "librarian", "fisherman", "shepherd", "cleric"};

    private final VillagerDisguise<T> disguise;

    public DisguisedCreatureRenderer(EntityRendererFactory.Context context, String name, float shadow) {
        super(context, name, shadow, false);
        this.disguise = new VillagerDisguise<>(context);
    }

    @Override
    public void render(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        if (entity.isDisguised()) {
            disguise.render(entity, yaw, tickDelta, matrices, consumers, light);
        } else {
            super.render(entity, yaw, tickDelta, matrices, consumers, light);
        }
    }

    /** Le « costume » : le rendu de villageois du jeu appliqué à notre créature. */
    private static class VillagerDisguise<T extends DisguisedMonsterEntity> extends MobEntityRenderer<T, VillagerResemblingModel<T>> {

        VillagerDisguise(EntityRendererFactory.Context context) {
            super(context, new VillagerResemblingModel<>(context.getPart(EntityModelLayers.VILLAGER)), 0.5f);
            addFeature(new FeatureRenderer<>(this) {
                @Override
                public void render(MatrixStack matrices, VertexConsumerProvider consumers, int light, T entity, float limbAngle,
                                   float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
                    if (entity.isInvisible()) {
                        return;
                    }
                    renderModel(getContextModel(), PLAINS, matrices, consumers, light, entity, 1.0f, 1.0f, 1.0f);
                    int look = Math.floorMod(entity.getLook(), PROFESSIONS.length);
                    if (look > 0) {
                        Identifier profession = new Identifier("textures/entity/villager/profession/" + PROFESSIONS[look] + ".png");
                        renderModel(getContextModel(), profession, matrices, consumers, light, entity, 1.0f, 1.0f, 1.0f);
                    }
                }
            });
        }

        @Override
        public Identifier getTexture(T entity) {
            return VILLAGER;
        }

        @Override
        protected void scale(T entity, MatrixStack matrices, float amount) {
            matrices.scale(0.9375f, 0.9375f, 0.9375f); // même taille qu'un vrai villageois
        }
    }
}
