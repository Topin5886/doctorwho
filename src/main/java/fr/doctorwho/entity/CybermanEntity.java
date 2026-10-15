package fr.doctorwho.entity;

import fr.doctorwho.registry.ModSounds;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Cyberman : agressif, corps à corps, très résistant. « Vous serez améliorés. »
 */
public class CybermanEntity extends HostileEntity implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.cyberman.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.cyberman.walk");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenPlay("animation.cyberman.attack");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public CybermanEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 8;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_ARMOR, 10.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(7, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
        targetSelector.add(4, new ActiveTargetGoal<>(this, IronGolemEntity.class, true));
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            triggerAnim("attaque", "attack");
            if (random.nextFloat() < 0.35f) {
                say(ModSounds.CYBERMAN_DELETE);
            }
        }
        return hit;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return getTarget() != null ? ModSounds.CYBERMAN_DELETE : ModSounds.CYBERMAN_AMBIENT;
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 260;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_IRON_GOLEM_DEATH;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        playSound(SoundEvents.ENTITY_IRON_GOLEM_STEP, 0.4f, 1.3f);
    }

    // --- Voix : une seule réplique à la fois ----------------------------------------------

    /** Ticks avant la prochaine réplique (évite que deux phrases se chevauchent). */
    private int voiceCooldown;

    /** Dit une réplique si la créature ne parle pas déjà. */
    protected void say(SoundEvent line) {
        if (voiceCooldown <= 0 && line != null) {
            playSound(line, getSoundVolume(), getSoundPitch());
            voiceCooldown = 70;
        }
    }

    @Override
    public void playAmbientSound() {
        say(getAmbientSound());
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        if (voiceCooldown > 0) {
            voiceCooldown--;
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mouvement", 4,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attaque", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", ATTACK));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
