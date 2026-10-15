package fr.doctorwho.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.boss.BossBar;
import net.minecraft.entity.boss.ServerBossBar;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Golem de redstone : mini-boss des cavernes, inspiré de Minecraft Dungeons.
 * <ul>
 *     <li>frappe des deux poings : dégâts autour de lui et projection en l'air ;</li>
 *     <li>onde de choc toutes les 8 s : il se redresse (1 s pour reculer !) puis frappe le sol,
 *     touchant tout ce qui est au sol dans un rayon de 7 blocs ;</li>
 *     <li>barre de vie de boss, impossible à repousser, insensible au feu et aux chutes.</li>
 * </ul>
 * Difficulté : 140 PV, armure 12. Un coup de poing toutes les 2 s (le bouclier le bloque),
 * et 2 s d'essoufflement après chaque onde de choc pour riposter.
 */
public class RedstoneGolemEntity extends HostileEntity implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.redstone_golem.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.redstone_golem.walk");
    private static final RawAnimation SLAM = RawAnimation.begin().thenPlay("animation.redstone_golem.slam");
    private static final RawAnimation SHOCKWAVE = RawAnimation.begin().thenPlay("animation.redstone_golem.shockwave");

    private static final float SLAM_DAMAGE = 8.0f;
    private static final double SLAM_RADIUS = 3.5;
    private static final float SHOCKWAVE_DAMAGE = 5.0f;
    private static final double SHOCKWAVE_RADIUS = 7.0;
    private static final int SHOCKWAVE_COOLDOWN = 160;
    private static final int SHOCKWAVE_CHARGE = 20;
    /** Après l'onde de choc, il reste essoufflé 2 s : c'est le moment de le frapper. */
    private static final int RECOVERY = 40;
    private static final DustParticleEffect RED_DUST = new DustParticleEffect(new Vector3f(1.0f, 0.15f, 0.1f), 2.0f);

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossBar bossBar = new ServerBossBar(getDisplayName(), BossBar.Color.RED, BossBar.Style.NOTCHED_10);
    private int shockwaveCooldown = SHOCKWAVE_COOLDOWN / 2;
    private int shockwaveCharge;
    private int recovery;

    public RedstoneGolemEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 80;
        this.setStepHeight(1.0f);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 140.0)
                .add(EntityAttributes.GENERIC_ARMOR, 12.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, SLAM_DAMAGE)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        // Poings lourds : un coup toutes les 2 s (au lieu d'1 s), le temps de riposter
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true) {
            @Override
            protected int getMaxCooldown() {
                return getTickCount(40);
            }
        });
        goalSelector.add(6, new WanderAroundFarGoal(this, 0.6));
        goalSelector.add(7, new LookAtEntityGoal(this, PlayerEntity.class, 12.0f));
        goalSelector.add(8, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
    }

    // --- Attaques ---------------------------------------------------------------------------

    /** Frappe des deux poings : tous ceux qui sont tout près sont touchés et projetés. */
    @Override
    public boolean tryAttack(Entity target) {
        if (!(getWorld() instanceof ServerWorld world) || shockwaveCharge > 0 || recovery > 0) {
            return false;
        }
        triggerAnim("attaque", "slam");
        Vec3d front = getPos().add(getRotationVector().multiply(1.5, 0, 1.5));
        Box area = new Box(front, front).expand(SLAM_RADIUS, 2.0, SLAM_RADIUS);
        boolean hit = false;
        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, area, this::canHit)) {
            hit |= victim.damage(getDamageSources().mobAttack(this), SLAM_DAMAGE);
            victim.addVelocity(0, 0.6, 0);
            victim.velocityModified = true;
        }
        world.spawnParticles(ParticleTypes.EXPLOSION, front.x, getY() + 0.2, front.z, 2, 0.6, 0.1, 0.6, 0.0);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.HOSTILE, 1.5f, 0.5f);
        return hit;
    }

    private boolean canHit(LivingEntity entity) {
        return entity != this && entity.isAlive() && !(entity instanceof RedstoneGolemEntity)
                && !(entity instanceof PlayerEntity player && (player.isCreative() || player.isSpectator()));
    }

    @Override
    protected void mobTick() {
        super.mobTick();
        bossBar.setPercent(getHealth() / getMaxHealth());
        if (!(getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (shockwaveCharge > 0) {
            // Il se redresse : immobile, des étincelles rouges montent de ses poings
            getNavigation().stop();
            world.spawnParticles(RED_DUST, getX(), getY() + 2.5, getZ(), 6, 1.2, 0.8, 1.2, 0.0);
            if (--shockwaveCharge == 0) {
                shockwave(world);
                recovery = RECOVERY;
            }
            return;
        }
        if (recovery > 0) {
            // Essoufflé : immobile, de la fumée sort de ses épaules
            recovery--;
            getNavigation().stop();
            if (recovery % 5 == 0) {
                world.spawnParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 3.2, getZ(), 3, 1.0, 0.2, 1.0, 0.01);
            }
            return;
        }
        LivingEntity target = getTarget();
        if (--shockwaveCooldown <= 0 && target != null && isOnGround() && squaredDistanceTo(target) < SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS * 2) {
            shockwaveCharge = SHOCKWAVE_CHARGE;
            shockwaveCooldown = SHOCKWAVE_COOLDOWN;
            triggerAnim("attaque", "shockwave");
            world.playSound(null, getBlockPos(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 2.0f, 0.5f);
        }
    }

    /** Onde de choc : touche tout ce qui est au sol autour de lui et le repousse. */
    private void shockwave(ServerWorld world) {
        Box area = getBoundingBox().expand(SHOCKWAVE_RADIUS, 1.0, SHOCKWAVE_RADIUS);
        for (LivingEntity victim : world.getEntitiesByClass(LivingEntity.class, area, this::canHit)) {
            if (!victim.isOnGround() || victim.squaredDistanceTo(this) > SHOCKWAVE_RADIUS * SHOCKWAVE_RADIUS) {
                continue; // en sautant au bon moment, on l'évite !
            }
            victim.damage(getDamageSources().mobAttack(this), SHOCKWAVE_DAMAGE);
            Vec3d away = victim.getPos().subtract(getPos()).multiply(1, 0, 1).normalize().multiply(1.2);
            victim.addVelocity(away.x, 0.45, away.z);
            victim.velocityModified = true;
        }
        // Anneau de poussière rouge qui s'étend sur le sol
        for (int i = 0; i < 48; i++) {
            double angle = i * Math.PI * 2 / 48;
            for (double r = 2; r <= SHOCKWAVE_RADIUS; r += 2.5) {
                world.spawnParticles(RED_DUST, getX() + Math.cos(angle) * r, getY() + 0.2, getZ() + Math.sin(angle) * r,
                        1, 0.0, 0.05, 0.0, 0.0);
            }
        }
        world.spawnParticles(ParticleTypes.EXPLOSION_EMITTER, getX(), getY(), getZ(), 1, 0, 0, 0, 0);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 1.4f, 0.6f);
    }

    // --- Boss -------------------------------------------------------------------------------

    @Override
    public void onStartedTrackingBy(ServerPlayerEntity player) {
        super.onStartedTrackingBy(player);
        bossBar.addPlayer(player);
    }

    @Override
    public void onStoppedTrackingBy(ServerPlayerEntity player) {
        super.onStoppedTrackingBy(player);
        bossBar.removePlayer(player);
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        if (hasCustomName()) {
            bossBar.setName(getDisplayName());
        }
    }

    @Override
    public boolean cannotDespawn() {
        return true;
    }

    @Override
    public boolean isFireImmune() {
        return true;
    }

    @Override
    public boolean canBreatheInWater() {
        return true;
    }

    @Override
    public boolean handleFallDamage(float distance, float multiplier, DamageSource source) {
        return false;
    }

    // --- Sons -------------------------------------------------------------------------------

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
        playSound(SoundEvents.ENTITY_IRON_GOLEM_STEP, 1.2f, 0.5f);
    }

    @Override
    public float getSoundPitch() {
        return 0.6f;
    }

    // --- GeckoLib ---------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mouvement", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "attaque", 0, state -> PlayState.STOP)
                .triggerableAnim("slam", SLAM)
                .triggerableAnim("shockwave", SHOCKWAVE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
