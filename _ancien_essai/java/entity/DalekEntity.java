package fr.doctorwho.entity;

import fr.doctorwho.DoctorWhoMod;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Entité Dalek - Exterminateur impitoyable !
 * Agressif, tire des lasers instantanés à distance, dit « Exterminer ! ».
 */
public class DalekEntity extends HostileEntity implements GeoEntity {
    // Données suivies pour l'état du Dalek
    private static final TrackedData<Boolean> IS_CHARGING = DataTracker.registerData(DalekEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> VARIANT = DataTracker.registerData(DalekEntity.class, TrackedDataHandlerRegistry.INTEGER);
    
    // Animation
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // Temps de charge du laser (en ticks)
    private int chargeTime = 0;
    private static final int CHARGE_TIME = 20; // 1 seconde de charge
    
    // Dernière fois qu'il a dit "Exterminer !"
    private int exterminateCooldown = 0;
    
    public DalekEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 5;
    }
    
    // === Géométrie GeckoLib ===
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (isCharging()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.dalek.charge"));
            } else if (hasTarget()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.dalek.walk"));
            } else {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.dalek.idle"));
            }
            return PlayState.CONTINUE;
        }));
        // Simple look controller
        controllers.add(new AnimationController<>(this, "look", 0, state -> PlayState.STOP));
    }
    
    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
    
    // === Attributs ===
    
    public static DefaultAttributeContainer.Builder createDalekAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 1.0D);
    }
    
    // === Données suivies ===
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(IS_CHARGING, false);
        dataTracker.startTracking(VARIANT, 0);
    }
    
    public void setCharging(boolean charging) {
        dataTracker.set(IS_CHARGING, charging);
    }
    
    public boolean isCharging() {
        return dataTracker.get(IS_CHARGING);
    }
    
    public void setVariant(int variant) {
        dataTracker.set(VARIANT, variant);
    }
    
    public int getVariant() {
        return dataTracker.get(VARIANT);
    }
    
    // === IA et comportement ===
    
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new DalekAttackGoal(this, 1.0D, false));
        this.goalSelector.add(2, new DalekChargeGoal(this, 1.0D, 10.0F));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        
        this.targetSelector.add(0, new RevengeGoal(this, new Class[0]));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    // === Spawn ===
    
    @Override
    public boolean canSpawn() {
        return this.world.getDifficulty().getId() >= 2 && super.canSpawn();
    }
    
    // === Interaction ===
    
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        if (!this.world.isClient) {
            this.world.playSound(
                    null, 
                    this.getX(), this.getY(), this.getZ(),
                    net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE, 
                    this.getSoundCategory(), 
                    1.0F, 
                    1.0F
            );
        }
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isUnblockable()) {
            return super.damage(source, amount);
        }
        return super.damage(source, amount * 0.7F);
    }
    
    // === Sons ===
    
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (exterminateCooldown > 0) {
            exterminateCooldown--;
            return null;
        }
        
        if (!hasTarget() && this.random.nextFloat() < 0.01F) {
            exterminateCooldown = 100;
            return DoctorWhoMod.id("entity.dalek.exterminate");
        }
        
        return DoctorWhoMod.id("entity.dalek.ambient");
    }
    
    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return DoctorWhoMod.id("entity.dalek.hurt");
    }
    
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return DoctorWhoMod.id("entity.dalek.death");
    }
    
    // === Helper methods ===
    
    protected boolean hasTarget() {
        return this.getTarget() != null && this.getTarget().isAlive();
    }
    
    // === Objectifs personnalisés ===
    
    static class DalekAttackGoal extends Goal {
        private final DalekEntity dalek;
        private final double speed;
        private final boolean longMemory;
        private int attackCooldown = 0;
        
        public DalekAttackGoal(DalekEntity dalek, double speed, boolean longMemory) {
            this.dalek = dalek;
            this.speed = speed;
            this.longMemory = longMemory;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            return dalek.getTarget() != null && dalek.getTarget().isAlive();
        }
        
        @Override
        public boolean shouldContinue() {
            return dalek.getTarget() != null && dalek.getTarget().isAlive() && dalek.isWithinDistance(dalek.getTarget(), 16.0);
        }
        
        @Override
        public void start() {
            dalek.getNavigation().stop();
            attackCooldown = 0;
        }
        
        @Override
        public void stop() {
        }
        
        @Override
        public void tick() {
            dalek.getLookControl().lookAt(dalek.getTarget().getX(), dalek.getTarget().getY(), dalek.getTarget().getZ(), 
                    (float) dalek.getYaw(), (float) dalek.getPitch());
            
            if (--attackCooldown <= 0) {
                Vec3d vec3d = dalek.getTarget().getPos().subtract(dalek.getPos());
                double distance = vec3d.horizontalLength();
                
                if (distance > 2.0) {
                    dalek.setCharging(true);
                    dalek.getNavigation().startMovingTo(dalek.getTarget().getX(), dalek.getTarget().getY(), dalek.getTarget().getZ(), dalek.speed);
                } else {
                    dalek.setCharging(false);
                    if (dalek.getTarget().damage(DamageSource.mob(dalek), (float) dalek.getAttributes().getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE))) {
                        dalek.getTarget().addVelocity(
                                (dalek.getTarget().getX() - dalek.getX()) * 0.3,
                                0.2,
                                (dalek.getTarget().getZ() - dalek.getZ()) * 0.3
                        );
                    }
                }
                
                attackCooldown = 20;
            }
        }
    }
    
    static class DalekChargeGoal extends Goal {
        private final DalekEntity dalek;
        private final double speed;
        private final float distance;
        private int chargeTicks = 0;
        
        public DalekChargeGoal(DalekEntity dalek, double speed, float distance) {
            this.dalek = dalek;
            this.speed = speed;
            this.distance = distance;
            this.setControls(EnumSet.of(Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            return dalek.isCharging() && dalek.hasTarget();
        }
        
        @Override
        public boolean shouldContinue() {
            return dalek.isCharging() && dalek.hasTarget() && chargeTicks < DalekEntity.CHARGE_TIME * 2;
        }
        
        @Override
        public void start() {
            chargeTicks = 0;
        }
        
        @Override
        public void stop() {
            dalek.setCharging(false);
        }
        
        @Override
        public void tick() {
            dalek.getNavigation().startMovingTo(dalek.getTarget().getX(), dalek.getTarget().getY(), dalek.getTarget().getZ(), dalek.speed);
            
            if (dalek.isCharging()) {
                chargeTicks++;
                
                if (chargeTicks >= DalekEntity.CHARGE_TIME) {
                    Vec3d vec3d = dalek.getTarget().getPos().subtract(dalek.getPos());
                    double distance = vec3d.horizontalLength();
                    
                    if (distance <= 16.0) {
                        if (dalek.getTarget().damage(DamageSource.mob(dalek), 8.0F)) {
                            dalek.world.playSound(
                                    null, 
                                    dalek.getX(), dalek.getY(), dalek.getZ(),
                                    DoctorWhoMod.id("entity.dalek.laser"), 
                                    dalek.getSoundCategory(), 
                                    1.0F, 
                                    1.0F + dalek.world.random.nextFloat() * 0.2F
                            );
                        }
                    }
                    
                    dalek.setCharging(false);
                }
            }
        }
    }
}