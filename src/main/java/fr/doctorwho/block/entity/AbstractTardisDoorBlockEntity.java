package fr.doctorwho.block.entity;

import fr.doctorwho.block.AbstractTardisDoorBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Animations des portes, communes à l'extérieur du TARDIS et à la porte intérieure. */
public abstract class AbstractTardisDoorBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation CLOSED = RawAnimation.begin().thenLoop("animation.tardis.closed");
    private static final RawAnimation DOOR_OPEN = RawAnimation.begin().thenPlayAndHold("animation.tardis.door_open");
    private static final RawAnimation DOOR_CLOSE = RawAnimation.begin().thenPlayAndHold("animation.tardis.door_close");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** Côté client uniquement : évite de jouer l'animation de fermeture au chargement du monde. */
    private boolean hasBeenOpened;

    protected AbstractTardisDoorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "portes", 0, state -> {
            if (getCachedState().get(AbstractTardisDoorBlock.OPEN)) {
                hasBeenOpened = true;
                return state.setAndContinue(DOOR_OPEN);
            }
            return state.setAndContinue(hasBeenOpened ? DOOR_CLOSE : CLOSED);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
