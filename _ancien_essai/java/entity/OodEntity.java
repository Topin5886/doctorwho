package fr.doctorwho.entity;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.item.SonicScrewdriverItem;
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
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Entité Ood - Créature pacifique avec des tentacules facilitatrices !
 * Pacifique, parle au clic droit avec son dispositif de traduction.
 */
public class OodEntity extends PassiveEntity implements GeoEntity {
    // Données suivies pour l'état de l'Ood
    private static final TrackedData<Integer> MOOD = DataTracker.registerData(OodEntity.class, TrackedDataHandlerRegistry.INTEGER);
    // 0=normal, 1=heureux, 2=curieux, 3=possédé (rouge ood temporaire)
    
    // Animation
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // Temps depuis la dernière interaction avec un joueur (en ticks)
    private int timeSinceLastInteraction = 0;
    
    public OodEntity(EntityType<? extends PassiveEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 1;
        this.setPersistenceRequired(false); // Ne pas persister indéfiniment
    }
    
    // === Géométrie GeckoLib ===
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            switch (getMood()) {
                case 1: // Heureux
                    state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ood.happy"));
                    break;
                case 2: // Curieux
                    state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ood.curious"));
                    break;
                case 3: // Possédé
                    state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ood.possessed"));
                    break;
                default: // Normal
                    if (hasPlayerNearby()) {
                        state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ood.idle"));
                    } else {
                        state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.ood.wander"));
                    }
                    break;
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
    
    public static DefaultAttributeContainer.Builder createOodAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 0.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 16.0D)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 1.0D);
    }
    
    // === Données suivies ===
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(MOOD, 0);
    }
    
    public void setMood(int mood) {
        dataTracker.set(MOOD, mood & 3); // S'assurer que c'est entre 0 et 3
    }
    
    public int getMood() {
        return dataTracker.get(MOOD);
    }
    
    // === IA et comportement ===
    
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new OodLookAtGoal(this, 8.0F));
        this.goalSelector.add(2, new OodInteractGoal(this, 1.0D));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.6D));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 6.0F));
        
        // Pas de cible car pacifique
        this.targetSelector.add(0, new RevengeGoal(this, new Class[0]));
    }
    
    // === Spawn ===
    
    @Override
    public boolean canSpawn() {
        // Les Oods apparaissent rarement, préférentiellement en lumière douce
        return this.world.getDifficulty().getId() >= 1 && 
               this.world.getLightLevel(new net.minecraft.util.math.BlockPos(this.getX(), this.getY(), this.getZ())) > 8 &&
               super.canSpawn();
    }
    
    // === Interaction ===
    
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        // Petits sons tristounets à la mort
        if (!this.world.isClient) {
            this.world.playSound(
                    null,
                    this.getX(), this.getY(), this.getZ(),
                    DoctorWhoMod.id("entity.ood.sad"),
                    this.getSoundCategory(),
                    0.8F,
                    1.0F
            );
        }
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isUnblockable()) {
            return super.damage(source, amount);
        }
        // Les Oods sont très vulnérables
        return super.damage(source, amount * 2.0F); // Prend le double de dégâts
    }
    
    @Override
    public boolean interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        // Si le joueur fait un clic droit à mains nues ou avec un objet non-nocif
        if (stack.isEmpty() || stack.getItem() instanceof net.minecraft.item.Item) {
            // L'Ood répond gentiment
            if (!this.world.isClient) {
                // Sons amicaux
                this.world.playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        DoctorWhoMod.id("entity.ood.help"),
                        this.getSoundCategory(),
                        1.0F,
                        1.0F
                );
                
                // Changer l'humeur pour montrer qu'il est content d'avoir interagi
                setMood(1); // Heureux
                timeSinceLastInteraction = 0;
                
                // Particules amicales
                // TODO: ajouter des particules lumineuses
            }
            
            return true;
        }
        
        return super.interact(player, hand);
    }
    
    // === Sons ===
    
    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        switch (getMood()) {
            case 1: // Heureux
                return DoctorWhoMod.id("entity.ood.happy");
            case 2: // Curieux
                return DoctorWhoMod.id("entity.ood.curious");
            case 3: // Possédé
                return DoctorWhoMod.id("entity.ood.possessed");
            default: // Normal
                if (hasPlayerNearby() && timeSinceLastInteraction < 100) {
                    return DoctorWhoMod.id("entity.ood.interested");
                }
                return DoctorWhoMod.id("entity.ood.ambient");
        }
    }
    
    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return DoctorWhoMod.id("entity.ood.hurt");
    }
    
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return DoctorWhoMod.id("entity.ood.death");
    }
    
    // === Helper methods ===
    
    protected boolean hasPlayerNearby() {
        return !this.world.getPlayers().isEmpty() && 
               this.world.getPlayers().stream()
                   .filter(p -> !p.isSpectator())
                   .anyMatch(p -> p.squaredDistanceTo(this) < 25.0); // Dans un rayon de 5 blocs
    }
    
    // === Objectifs personnalisés ===
    
    /**
     * Objectif de regarder les joueurs intéressés
     */
    static class OodLookAtGoal extends Goal {
        private final OodEntity ood;
        private final float maxDistance;
        
        public OodLookAtGoal(OodEntity ood, float maxDistance) {
            this.ood = ood;
            this.maxDistance = maxDistance;
            this.setControls(EnumSet.of(Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            return ood.hasPlayerNearby();
        }
        
        @Override
        public boolean shouldContinue() {
            return ood.hasPlayerNearby() && ood.getMood() != 3; // Pas quand possédé
        }
        
        @Override
        public void start() {
        }
        
        @Override
        // Trouver le joueur le plus proche
        @Override
        public void stop() {
        }
        
        @Override
        public void tick() {
            // Trouver le joueur le plus proche
            OodEntity.EntityAccessor accessor = new OodEntity.EntityAccessor(ood);
            double closestDistance = Double.MAX_VALUE;
            PlayerEntity closestPlayer = null;
            
            for (PlayerEntity player : ood.world.getPlayers()) {
                if (player.isSpectator()) continue;
                double distance = player.squaredDistanceTo(ood);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestPlayer = player;
                }
            }
            
            if (closestPlayer != null && Math.sqrt(closestDistance) <= ood.maxDistance) {
                ood.getLookControl().lookAt(
                        closestPlayer.getX(), 
                        closestPlayer.getEyeY(), 
                        closestPlayer.getZ(),
                        10.0F, 
                        (float) ood.getLookPitch()
                );
            }
        }
    }
    
    /**
     * Objectif d'interaction spéciale : répondre gentiment quand on lui parle
     */
    static class OodInteractGoal extends Goal {
        private final OodEntity ood;
        private final double speed;
        private int interactCooldown = 0;
        
        public OodInteractGoal(OodEntity ood, double speed) {
            this.ood = ood;
            this.speed = speed;
            this.setControls(EnumSet.of(Control.MOVE));
        }
        
        @Override
        public boolean canStart() {
            return ood.hasPlayerNearby() && ood.timeSinceLastInteraction > 60 && interactCooldown <= 0;
        }
        
        @Override
        // Un seul tick suffit
        @Override
        public boolean shouldContinue() {
            return false;
        }
        
        @Override
        public void start() {
            // L'Ood devient curieux quand quelqu'un s'approche
            if (ood.getMood() == 0) { // Si normal, devenir curieux
                ood.setMood(2); // Curieux
            }
            
            ood.timeSinceLastInteraction = 0;
            interactCooldown = 200; // 10 secondes avant de pouvoir re-déclencher
        }
        
        @Override
        public void stop() {
        }
        
        @Override
        public void tick() {
            if (interactCooldown > 0) {
                interactCooldown--;
            }
        }
    }
    
    // === Wrapper pour accéder aux méthodes protégées ===
    private static class EntityAccessor extends OodEntity {
        EntityAccessor(OodEntity entity) {
            super(entity.getType(), entity.world);
            this.copyPositionAndRotation(entity);
        }
    }
}