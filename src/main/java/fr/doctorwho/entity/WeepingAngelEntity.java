package fr.doctorwho.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Set;

/**
 * Ange Pleureur : une statue qui ne bouge QUE si personne ne la regarde (« verrou
 * quantique »). Quand elle touche un joueur, elle le renvoie loin, « dans le passé ».
 * Ne clignez pas des yeux.
 */
public class WeepingAngelEntity extends HostileEntity implements GeoEntity {

    private static final TrackedData<Boolean> ANGRY = DataTracker.registerData(WeepingAngelEntity.class, TrackedDataHandlerRegistry.BOOLEAN);

    private static final RawAnimation COVERING = RawAnimation.begin().thenLoop("animation.weeping_angel.covering");
    private static final RawAnimation ATTACK = RawAnimation.begin().thenLoop("animation.weeping_angel.attack");

    /** Distance à laquelle un joueur peut « verrouiller » un Ange en le regardant. */
    private static final double SIGHT_RANGE = 64.0;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private boolean observed;

    public WeepingAngelEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 15;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 50.0)
                .add(EntityAttributes.GENERIC_ARMOR, 14.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.45)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 40.0)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.add(6, new WanderAroundFarGoal(this, 0.5));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, false));
    }

    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(ANGRY, false);
    }

    // --- Verrou quantique --------------------------------------------------------------------

    @Override
    public void tick() {
        if (!getWorld().isClient) {
            observed = isObserved();
            // Regardé : l'IA s'arrête net (pas de mouvement, pas de rotation)
            setAiDisabled(observed);
            if (observed) {
                setVelocity(0, Math.min(getVelocity().y, 0), 0);
            }
            boolean angry = getTarget() != null && squaredDistanceTo(getTarget()) < 49;
            if (angry != dataTracker.get(ANGRY)) {
                dataTracker.set(ANGRY, angry);
            }
        }
        super.tick();
    }

    /** Un joueur regarde-t-il l'Ange (champ de vision + ligne de vue) ? */
    private boolean isObserved() {
        for (PlayerEntity player : getWorld().getPlayers()) {
            if (player.isSpectator() || !player.isAlive() || player.squaredDistanceTo(this) > SIGHT_RANGE * SIGHT_RANGE
                    || player.hasStatusEffect(StatusEffects.BLINDNESS) || player.hasStatusEffect(StatusEffects.DARKNESS)) {
                continue;
            }
            Vec3d look = player.getRotationVec(1.0f).normalize();
            Vec3d toAngel = new Vec3d(getX() - player.getX(), getBodyY(0.5) - player.getEyeY(), getZ() - player.getZ()).normalize();
            if (look.dotProduct(toAngel) > 0.55 && player.canSee(this)) {
                return true;
            }
        }
        return false;
    }

    // --- Attaque : renvoyé dans le passé ----------------------------------------------------

    @Override
    public boolean tryAttack(Entity target) {
        if (observed) {
            return false;
        }
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof ServerPlayerEntity player && getWorld() instanceof ServerWorld world) {
            double angle = random.nextDouble() * Math.PI * 2;
            int distance = 300 + random.nextInt(500);
            int x = (int) (player.getX() + Math.cos(angle) * distance);
            int z = (int) (player.getZ() + Math.sin(angle) * distance);
            int y = world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, x, z);
            player.teleport(world, x + 0.5, y, z + 0.5, Set.of(), player.getYaw(), player.getPitch());
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 60, 0));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 140, 0));
            player.sendMessage(Text.translatable("message.doctorwho.angel.sent_back").formatted(Formatting.GRAY, Formatting.ITALIC), false);
            world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT, SoundCategory.HOSTILE, 1.0f, 0.5f);
        }
        return hit;
    }

    /** De la pierre : les coups portent mal. */
    @Override
    public boolean damage(DamageSource source, float amount) {
        return super.damage(source, amount * 0.6f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLOCK_STONE_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLOCK_STONE_BREAK;
    }

    @Override
    public boolean handleFallDamage(float fallDistance, float damageMultiplier, DamageSource source) {
        return false;
    }

    // --- GeckoLib ---------------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Pose figée : mains sur les yeux, ou bras tendus quand une proie est proche
        controllers.add(new AnimationController<>(this, "pose", 0,
                state -> state.setAndContinue(dataTracker.get(ANGRY) ? ATTACK : COVERING)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
