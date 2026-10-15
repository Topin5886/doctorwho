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
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
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
 * Entité Cyberman - Guerrier cybernétique impitoyable !
 * Très résistant, attaque au corps à corps, dit « Supprimer ! ».
 */
public class CybermanEntity extends HostileEntity implements GeoEntity {
    // Données suivies pour l'état du Cyberman
    private static final TrackedData<Boolean> IS_CHARGING = DataTracker.registerData(CybermanEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // Animation
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // Temps de charge pour l'attaque puissante (en ticks)
    private int chargeTime = 0;
    private static final int CHARGE_TIME = 30; // 1.5 seconde de charge
    
    public CybermanEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 6;
    }
    
    // === Géométrie GeckoLib ===
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (isCharging()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.cyberman.charge"));
            } else if (hasTarget()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.cyberman.walk"));
            } else {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.cyberman.idle"));
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
    
    public static DefaultAttributeContainer.Builder createCybermanAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 60.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 5.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 12.0D)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 0.8D);
    }
    
    // === Données suivies ===
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(IS_CHARGING, false);
    }
    
    public void setCharging(boolean charging) {
        dataTracker.set(IS_CHARGING, charging);
    }
    
    public boolean isCharging() {
        return dataTracker.get(IS_CHARGING);
    }
    
    // === IA et comportement ===
    
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new CybermanAttackGoal(this, 0.8D, false));
        this.goalSelector.add(2, new CybermanChargeGoal(this, 0.8D, 8.0F));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.6D));
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
        // Particules d'étincelles à la mort
        if (!this.world.isClient) {
            // TODO: ajouter des particules d'étincelles
        }
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isUnblockable()) {
            return super.damage(source, amount);
        }
        // Les Cybermen sont très résistants
        return super.damage(source, amount * 0.5F); // 50% de résistance
    }
    
    @Override
    public boolean interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        // Si le joueur fait un clic droit à mains nues ou avec un objet amical
        if (stack.isEmpty() || 
            stack.getItem() instanceof net.minecraft.item.Item) {
            
            // Le Cyberman répond agressivement
            if (!this.world.isClient) {
                this.world.playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        DoctorWhoMod.id("entity.cyberman.aggressive"),
                        this.getSoundCategory(),
                        1.0F,
                        1.0F
                );
            }
            
            return true;
        }
        
        return super.interact(player, hand);
    }
    
    // === Sons ===
    
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        // 0.5% de chance de dire "Supprimer !" quand il est idle
        if (!hasTarget() && this.random.nextFloat() < 0.005F) {
            return DoctorWhoMod.id("entity.cyberman.delete");
        }
        return DoctorWhoMod.id("entity.cyberman.ambient");
    }
    
    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return DoctorWhoMod.id("entity.cyberman.hurt");
    }
    
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return DoctorWhoMod.id("entity.cyberman.death");
    }
    
    // === Helper methods ===
    
    protected boolean hasTarget() {
        return this.getTarget() != null && this.getTarget().isAlive();
    }
    
    // === Objectifs personnalisés ===
    
    /**
     * Objectif d'attaque spécial pour le Cyberman : attaque puissante chargée
     */
    static class CybermanAttackGoal extends Goal {
        private final CybermanEntity cyberman;
        private final double speed;
        private final boolean longMemory;
        private int attackCooldown = 0;
        
        public CybermanAttackGoal(CybermanEntity cyberman, double speed, boolean longMemory) {
            this.cyberman = cyberman;
            this.speed = speed;
            this.longMemory = longMemory;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            return cyberman.getTarget() != null && cyberman.getTarget().isAlive();
        }
        
        @Override
        public boolean shouldContinue() {
            return cyberman.getTarget() != null && cyberman.getTarget().isAlive() && cyberman.isWithinDistance(cyberman.getTarget(), 12.0);
        }
        
        @Override
        public void start() {
            cyberman.getNavigation().stop();
            attackCooldown = 0;
        }
        
        @Override
        public void stop() {
        }
        
        @Override
        public void tick() {
            cyberman.getLookControl().lookAt(cyberman.getTarget().getX(), cyberman.getTarget().getY(), cyberman.getTarget().getZ(), 
                    (float) cyberman.getYaw(), (float) cyberman.getPitch());
            
            if (--attackCooldown <= 0) {
                Vec3d vec3d = cyberman.getTarget().getPos().subtract(cyberman.getPos());
                double distance = vec3d.horizontalLength();
                
                if (distance > 1.5) {
                    // S'approcher pour préparer l'attaque chargée
                    cyberman.setCharging(true);
                    cyberman.getNavigation().startMovingTo(cyberman.getTarget().getX(), cyberman.getTarget().getY(), cyberman.getTarget().getZ(), cyberman.speed);
                } else {
                    // À portée, attaquer
                    cyberman.setCharging(false);
                    if (cyberman.getTarget().damage(DamageSource.mob(cyberman), (float) cyberman.getAttributes().getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE))) {
                        // Effet de recul puissant
                        cyberman.getTarget().addVelocity(
                                (cyberman.getTarget().getX() - cyberman.getX()) * 0.5,
                                0.3,
                                (cyberman.getTarget().getZ() - cyberman.getZ()) * 0.5
                        );
                    }
                }
                
                attackCooldown = 40; // 2 secondes entre les attaques
            }
        }
    }
    
    /**
     * Objectif de charge de l'attaque puissante
     */
    static class CybermanChargeGoal extends Goal {
        private final CybermanEntity cyberman;
        private final double speed;
        private final float distance;
        private int chargeTicks = 0;
        
        public CybermanChargeGoal(CybermanEntity cyberman, double speed, float distance) {
            this.cyberman = cyberman;
            this.speed = speed;
            this.distance = distance;
            this.setControls(EnumSet.of(Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            return cyberman.isCharging() && cyberman.hasTarget();
        }
        
        @Override
        public boolean shouldContinue() {
            return cyberman.isCharging() && cyberman.hasTarget() && chargeTicks < CybermanEntity.CHARGE_TIME * 2;
        }
        
        @Override
        public void start() {
            chargeTicks = 0;
        }
        
        @Override
        public void stop() {
            cyberman.setCharging(false);
        }
        
        @Override
        public void tick() {
            cyberman.getNavigation().startMovingTo(cyberman.getTarget().getX(), cyberman.getTarget().getY(), cyberman.getTarget().getZ(), cyberman.speed);
            
            if (cyberman.isCharging()) {
                chargeTicks++;
                
                if (chargeTicks >= CybermanEntity.CHARGE_TIME) {
                    // Attaque puissante !
                    if (cyberman.getTarget().damage(DamageSource.mob(cyberman), 10.0F)) {
                        // Particules et son de l'impact
                        cyberman.world.playSound(
                                null, 
                                cyberman.getX(), cyberman.getY(), cyberman.getZ(),
                                DoctorWhoMod.id("entity.cyberman.strong_hit"), 
                                cyberman.getSoundCategory(), 
                                1.5F, 
                                1.0F
                        );
                        
                        // Effet de recul encore plus puissant
                        cyberman.getTarget().addVelocity(
                                (cyberman.getTarget().getX() - cyberman.getX()) * 0.8,
                                0.5,
                                (cyberman.getTarget().getZ() - cyberman.getZ()) * 0.8
                        );
                    }
                    
                    cyberman.setCharging(false);
                }
            }
        }
    }
}