package fr.doctorwho.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Adipose : petite créature faite de graisse, inoffensive. Elle fait coucou.
 */
public class AdiposeEntity extends PathAwareEntity implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.adipose.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.adipose.walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public AdiposeEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 6.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new EscapeDangerGoal(this, 1.6));
        goalSelector.add(5, new WanderAroundFarGoal(this, 1.0));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENTITY_SLIME_SQUISH_SMALL;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_SLIME_HURT_SMALL;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_SLIME_DEATH_SMALL;
    }

    /** Petite voix aiguë. */
    @Override
    public float getSoundPitch() {
        return 1.8f + random.nextFloat() * 0.2f;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mouvement", 3,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
