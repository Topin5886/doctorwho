package fr.doctorwho.entity;

import fr.doctorwho.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.AttackWithOwnerGoal;
import net.minecraft.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.ProjectileAttackGoal;
import net.minecraft.entity.ai.goal.SitGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.TrackOwnerAttackerGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.EntityView;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * K9 : chien robot. Un clic droit avec de la redstone l'apprivoise (1 chance sur 3) ;
 * il suit alors son maître, tire au laser sur les monstres, et s'assoit ou se relève
 * au clic droit à la main.
 */
public class K9Entity extends TameableEntity implements GeoEntity, RangedAttackMob {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.k9.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.k9.walk");
    private static final float LASER_DAMAGE = 4.0f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int voiceCooldown;

    public K9Entity(EntityType<? extends TameableEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_ARMOR, 6.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new SitGoal(this));
        goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, 25, 14.0f));
        goalSelector.add(3, new FollowOwnerGoal(this, 1.1, 8.0f, 2.0f, false));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.7));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(7, new LookAroundGoal(this));
        targetSelector.add(1, new TrackOwnerAttackerGoal(this));
        targetSelector.add(2, new AttackWithOwnerGoal(this));
        // K9 protège son maître : il tire sur les monstres proches (seulement s'il est apprivoisé)
        targetSelector.add(3, new ActiveTargetGoal<>(this, HostileEntity.class, 10, true, false,
                entity -> isTamed() && !(entity instanceof K9Entity)));
    }

    @Override
    public ActionResult interactMob(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (!isTamed() && stack.isOf(Items.REDSTONE)) {
            if (!getWorld().isClient) {
                if (!player.getAbilities().creativeMode) {
                    stack.decrement(1);
                }
                if (random.nextInt(3) == 0) {
                    setOwner(player);
                    navigation.stop();
                    setTarget(null);
                    setSitting(true);
                    getWorld().sendEntityStatus(this, (byte) 7);
                    say(ModSounds.K9_AFFIRM);
                    player.sendMessage(Text.translatable("message.doctorwho.k9.tamed").formatted(Formatting.AQUA), true);
                } else {
                    getWorld().sendEntityStatus(this, (byte) 6);
                }
            }
            return ActionResult.success(getWorld().isClient);
        }
        if (isTamed() && isOwner(player) && hand == Hand.MAIN_HAND) {
            if (!getWorld().isClient) {
                setSitting(!isSitting());
                say(ModSounds.K9_AFFIRM);
                player.sendMessage(Text.translatable(isSitting() ? "message.doctorwho.k9.stay" : "message.doctorwho.k9.follow"), true);
            }
            return ActionResult.success(getWorld().isClient);
        }
        return super.interactMob(player, hand);
    }

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        if (!(getWorld() instanceof ServerWorld world) || !canSee(target)) {
            return;
        }
        Vec3d from = getPos().add(0, 0.7, 0);
        Vec3d to = target.getPos().add(0, target.getHeight() * 0.55, 0);
        Vec3d step = to.subtract(from);
        int points = (int) (step.length() * 4);
        DustParticleEffect beam = new DustParticleEffect(new Vector3f(1.0f, 0.2f, 0.2f), 1.0f);
        for (int i = 0; i <= points; i++) {
            Vec3d p = from.add(step.multiply(i / (double) Math.max(points, 1)));
            world.spawnParticles(beam, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        playSound(ModSounds.DALEK_LASER, 0.7f, 1.6f);
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
    protected void mobTick() {
        super.mobTick();
        if (voiceCooldown > 0) {
            voiceCooldown--;
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_IRON_GOLEM_DEATH;
    }

    /** Exigé par l'interface Tameable (nom non mappé dans Yarn 1.20.1) : le monde de l'entité. */
    @Override
    public EntityView method_48926() {
        return getWorld();
    }

    // --- Pas de reproduction : on ne fait pas de bébés robots ------------------------------

    @Nullable
    @Override
    public PassiveEntity createChild(ServerWorld world, PassiveEntity entity) {
        return null;
    }

    @Override
    public boolean isBreedingItem(ItemStack stack) {
        return false;
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
