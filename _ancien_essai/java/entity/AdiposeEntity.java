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
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.server.world.ServerWorld;
import java.util.EnumSet;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Entité Adipose - Petite créature grassouillette et adorable !
 * Pacifique, fait coucou, très mignonne mais facilement effrayée.
 */
public class AdiposeEntity extends PassiveEntity implements GeoEntity {
    // Données suivies pour l'état de l'Adipose
    private static final TrackedData<Boolean> IS_WAVING = DataTracker.registerData(AdiposeEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    
    // Animation
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // Temps depuis la dernière interaction avec un joueur (en ticks)
    private int timeSinceLastInteraction = 0;
    // Temps avant de redevenir nerveux après une interaction positive (en ticks)
    private int nervousTime = 0;
    
    public AdiposeEntity(EntityType<? extends PassiveEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 1;
    }
    
    // === Géométrie GeckoLib ===
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (isWaving()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.adipose.wave"));
            } else if (isNervous()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.adipose.nervous"));
            } else if (hasPlayerNearby()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.adipose.curious"));
            } else {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.adipose.idle"));
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
    
    public static DefaultAttributeContainer.Builder createAdiposeAttributes() {
        return DefaultAttributeContainer.builder()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 4.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.15D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 8.0D)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 1.0D);
    }
    
    // === Données suivies ===
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(IS_WAVING, false);
    }
    
    public void setWaving(boolean waving) {
        dataTracker.set(IS_WAVING, waving);
    }
    
    public boolean isWaving() {
        return dataTracker.get(IS_WAVING);
    }
    
    public boolean isNervous() {
        return nervousTime > 0;
    }
    
    public void setNervous(boolean nervous) {
        if (nervous) {
            nervousTime = 200; // 10 secondes de nervosité
        } else {
            nervousTime = 0;
        }
    }
    
    // === IA et comportement ===
    
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new AdiposeWaveGoal(this, 1.0D));
        this.goalSelector.add(2, new AdiposeApproachGoal(this, 0.8D));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.4D));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 4.0F));
        
        // Pas de cible car pacifique et non combattant
        this.targetSelector.add(0, new RevengeGoal(this, new Class[0]));
    }
    
    // === Spawn ===
    
    @Override
    public boolean canSpawn() {
        // Les Adipose apparaissent très rarement, préférentiellement en journée
        // Utilisation de méthodes accessibles au lieu d'accéder directement au champ privé world
        World world = this.getWorld();
        if (world == null) {
            return false;
        }
        // Vérifier si c'est jour et si le niveau de lumière est suffisant
        return world.getDifficulty().getId() >= 1 && 
               world.getTimeOfDay() < 13000 && // Jour
               world.getLightLevel(new net.minecraft.util.math.BlockPos(this.getX(), this.getY(), this.getZ())) > 10.0F &&
               super.canSpawn();
    }
    
    // === Création d'entité enfant (requise pour PassiveEntity) ===
    
    @Override
    public PassiveEntity createChild(ServerWorld serverWorld, PassiveEntity passiveEntity) {
        return new AdiposeEntity((EntityType<? extends PassiveEntity>) getType(), serverWorld);
    }
    
    // === Interaction ===
    
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        // Petit "pop" sonore à la mort (comme dans la série)
        if (!this.getWorld().isClient) {
            // Utiliser un son par défaut puisque la conversion Identifier -> SoundEvent est complexe
            this.getWorld().playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_GENERIC_EXPLODE, 
                    this.getSoundCategory(),
                    0.6F,
                    1.0F + this.getWorld().random.nextFloat() * 0.4F
            );
            
            // Particules de disparition
            // TODO: ajouter des particules de matière qui se décompose
        }
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isUnblockable()) {
            return super.damage(source, amount);
        }
        // Les Adipose sont extrêmement vulnérables
        return super.damage(source, amount * 3.0F); // Prend le triple de dégâts
    }
    
    // Comme interact() est final dans MobEntity, on ne peut pas l'overrider
    
    // === Sons ===
    
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        if (isNervous()) {
            return DoctorWhoMod.id("entity.adipose.nervous");
        }
        if (hasPlayerNearby() && timeSinceLastInteraction < 100) {
            return DoctorWhoMod.id("entity.adipose.curious_sound");
        }
        return DoctorWhoMod.id("entity.adipose.ambient");
    }
    
    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return DoctorWhoMod.id("entity.adipose.hurt");
    }
    
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return DoctorWhoMod.id("entity.adipose.pop"); // Même son que la mort normale
    }
    
    // === Helper methods ===
    
    protected boolean hasPlayerNearby() {
        return !this.getWorld().getPlayers().isEmpty() && 
               this.getWorld().getPlayers().stream()
                   .filter(p -> !p.isSpectator())
                   .anyMatch((p) -> p.squaredDistanceTo(this) < 16.0); // Dans un rayon de 4 blocs
    }
    
    // === Objectifs personnalisés ===
    
    /**
     * Objectif de waving spécial : faire coucou quand content
     */
    static class AdiposeWaveGoal extends Goal {
        private final AdiposeEntity adipose;
        private double speed;
        private int waveTimer = 0;
        
        public AdiposeWaveGoal(AdiposeEntity adipose, double speed) {
            this.adipose = adipose;
            this.speed = speed;
            this.setControls(EnumSet.of(Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            return adipose.hasPlayerNearby() && 
                   adipose.timeSinceLastInteraction > 60 && 
                   adipose.timeSinceLastInteraction < 300 && // Pas trop tôt, pas trop tard
                   !adipose.isWaving() && 
                   waveTimer <= 0 &&
                   adipose.nervousTime <= 10;
        }
        
        @Override
        public boolean shouldContinue() {
            return false;
        }
        
        @Override
        public void start() {
            adipose.setWaving(true);
            waveTimer = 100; // 5 secondes avant de pouvoir re-waver
        }
        
        @Override
        public void stop() {
        }
        
        @Override
        public void tick() {
            if (waveTimer > 0) {
                waveTimer--;
            }
            
            if (adipose.isWaving()) {
                // Arrêter de waver après un court moment
                adipose.setWaving(false);
            }
        }
    }
    
    /**
     * Objectif d'approche douce : s'approcher prudemment des joueurs amicaux
     */
    static class AdiposeApproachGoal extends Goal {
        private final AdiposeEntity adipose;
        private double speed;
        private int approachCooldown = 0;
        
        public AdiposeApproachGoal(AdiposeEntity adipose, double speed) {
            this.adipose = adipose;
            this.speed = speed;
            this.setControls(EnumSet.of(Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            return adipose.hasPlayerNearby() && 
                   adipose.timeSinceLastInteraction > 200 && // Attendre un bit après interaction
                   approachCooldown <= 0 &&
                   adipose.nervousTime == 0;
        }
        
        @Override
        public boolean shouldContinue() {
            return false;
        }
        
        @Override
        public void start() {
            // S'approcher lentement du joueur le plus proche
            approachCooldown = 60; // 3 secondes avant de pouvoir re-approcher
            
            // Trouver le joueur le plus proche
            AdiposeEntity.EntityAccessor accessor = new AdiposeEntity.EntityAccessor(adipose);
            double closestDistance = Double.MAX_VALUE;
            PlayerEntity closestPlayer = null;
            
            for (PlayerEntity player : adipose.world.getPlayers()) {
                if (player.isSpectator()) continue;
                double distance = player.squaredDistanceTo(adipose);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = player;
                }
            }
            
            if (closestPlayer != null) {
                double angle = Math.atan2(
                        closestPlayer.getZ() - adipose.getZ(),
                        closestPlayer.getX() - adipose.getX()
                );
                
                // Se déplacer lentement vers le joueur
                adipose.getNavigation().startMovingTo(
                        adipose.getX() + Math.cos(angle) * 0.5,
                        adipose.getY(),
                        adipose.getZ() + Math.sin(angle) * 0.5,
                        adipose.speed * 0.5 // Moitié vitesse pour approche prudente
                );
            }
        }
        
        @Override
        public void stop() {
        }
        
        @Override
        public void tick() {
            if (approachCooldown > 0) {
                approachCooldown--;
            }
        }
    }
    
    // === Wrapper pour accéder aux méthodes protégées ===
    private static class EntityAccessor extends AdiposeEntity {
        EntityAccessor(AdiposeEntity entity) {
            super(entity.getType(), entity.world);
            this.copyPositionAndRotation(entity);
        }
    }
}