package fr.doctorwho.entity;

import fr.doctorwho.registry.ModDimensions;
import fr.doctorwho.registry.ModSounds;
import net.minecraft.entity.EntityData;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
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
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.LocalDifficulty;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

/**
 * Dalek : agressif, tire un rayon laser instantané à distance, crie « Exterminer ! ».
 * Plusieurs couleurs (bronze de 2005, argent, rouge suprême, noir, blanc impérial).
 */
public class DalekEntity extends HostileEntity implements GeoEntity, RangedAttackMob {

    public static final List<String> COLORS = List.of("bronze", "silver", "red", "black", "white");

    private static final TrackedData<Integer> COLOR = DataTracker.registerData(DalekEntity.class, TrackedDataHandlerRegistry.INTEGER);

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.dalek.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.dalek.walk");
    private static final RawAnimation SHOOT = RawAnimation.begin().thenPlay("animation.dalek.shoot");

    /** Dégâts du rayon (difficulté équilibrée). */
    private static final float LASER_DAMAGE = 6.0f;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public DalekEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 10;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_ARMOR, 8.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.6);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(2, new ProjectileAttackGoal(this, 1.0, 40, 18.0f));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 10.0f));
        goalSelector.add(7, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        // Les Daleks exterminent aussi les villageois
        targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
    }

    // --- Le rayon laser -------------------------------------------------------------------

    @Override
    public void attack(LivingEntity target, float pullProgress) {
        if (!(getWorld() instanceof ServerWorld world) || !canSee(target)) {
            return;
        }
        if (random.nextFloat() < 0.3f) {
            say(ModSounds.DALEK_EXTERMINATE);
        }
        Vec3d from = getPos().add(0, 0.72, 0);
        Vec3d to = target.getPos().add(0, target.getHeight() * 0.55, 0);
        Vec3d step = to.subtract(from);
        int points = (int) (step.length() * 4);
        DustParticleEffect beam = new DustParticleEffect(new Vector3f(0.45f, 0.85f, 1.0f), 1.0f);
        for (int i = 0; i <= points; i++) {
            Vec3d p = from.add(step.multiply(i / (double) Math.max(points, 1)));
            world.spawnParticles(beam, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        playSound(ModSounds.DALEK_LASER, 1.2f, 0.9f + random.nextFloat() * 0.2f);
        target.damage(getDamageSources().mobAttack(this), LASER_DAMAGE);
        triggerAnim("tir", "shoot");
    }

    // --- Couleur ---------------------------------------------------------------------------

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(COLOR, 0);
    }

    public String getColor() {
        int i = dataTracker.get(COLOR);
        return COLORS.get(i >= 0 && i < COLORS.size() ? i : 0);
    }

    public void setColor(String color) {
        dataTracker.set(COLOR, Math.max(0, COLORS.indexOf(color)));
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason reason,
                                 @Nullable EntityData data, @Nullable NbtCompound nbt) {
        // Bronze le plus souvent ; toutes les couleurs sur Skaro
        boolean skaro = world.toServerWorld().getRegistryKey() == ModDimensions.SKARO;
        if (skaro || random.nextFloat() < 0.3f) {
            setColor(COLORS.get(random.nextInt(COLORS.size())));
        } else {
            setColor("bronze");
        }
        return super.initialize(world, difficulty, reason, data, nbt);
    }

    @Override
    public void writeCustomDataToNbt(NbtCompound nbt) {
        super.writeCustomDataToNbt(nbt);
        nbt.putString("Color", getColor());
    }

    @Override
    public void readCustomDataFromNbt(NbtCompound nbt) {
        super.readCustomDataFromNbt(nbt);
        setColor(nbt.getString("Color"));
    }

    // --- Sons et divers --------------------------------------------------------------------

    @Override
    protected SoundEvent getAmbientSound() {
        return getTarget() != null ? ModSounds.DALEK_EXTERMINATION : ModSounds.DALEK_AMBIENT;
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_IRON_GOLEM_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_GENERIC_EXPLODE;
    }

    /** Les Daleks lévitent : pas de dégâts de chute. */
    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
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

    // --- GeckoLib --------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mouvement", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
        controllers.add(new AnimationController<>(this, "tir", 0, state -> PlayState.STOP)
                .triggerableAnim("shoot", SHOOT));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
