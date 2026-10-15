package fr.doctorwho.entity;

import fr.doctorwho.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/** Sontarien : guerrier clone en armure, tire au fusil laser à distance. « Sontar-ha ! » */
public class SontarianEntity extends HostileEntity implements GeoEntity, RangedAttackMob {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.sontarian.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.sontarian.walk");
    private static final float LASER_DAMAGE = 4.0f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int voiceCooldown;

    public SontarianEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 10;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.23)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 26.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, 30, 16.0f));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(7, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        if (!(getWorld() instanceof ServerWorld world) || !canSee(target)) {
            return;
        }
        say(ModSounds.SONTARIAN_HA);
        Vec3d from = getPos().add(0, 1.4, 0);
        Vec3d to = target.getPos().add(0, target.getHeight() * 0.55, 0);
        Vec3d step = to.subtract(from);
        int points = (int) (step.length() * 4);
        DustParticleEffect beam = new DustParticleEffect(new Vector3f(1.0f, 0.55f, 0.2f), 1.0f);
        for (int i = 0; i <= points; i++) {
            Vec3d p = from.add(step.multiply(i / (double) Math.max(points, 1)));
            world.spawnParticles(beam, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        playSound(ModSounds.DALEK_LASER, 0.9f, 1.3f);
        target.damage(getDamageSources().mobAttack(this), LASER_DAMAGE);
    }

    // --- Voix : une seule réplique à la fois ----------------------------------------------

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
    protected SoundEvent getAmbientSound() {
        return ModSounds.SONTARIAN_HA;
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 300;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_PLAYER_DEATH;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mouvement", 4,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
