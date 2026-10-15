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
 * Entité WeepingAngel - Ange Pleureur quantique !
 * Ne bouge que quand aucun joueur ne le regarde, téléporte les joueurs qui le touchent.
 */
public class WeepingAngelEntity extends HostileEntity implements GeoEntity {
    // Données suivies pour l'état de l'Ange Pleureur
    private static final TrackedData<Boolean> IS_COVERING_EYES = DataTracker.registerData(WeepingAngelEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> LOOK_DIRECTION = DataTracker.registerData(WeepingAngelEntity.class, TrackedDataHandlerRegistry.INTEGER); // 0=nord,1=est,2=sud,3=ouest
    
    // Animation
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    
    // Temps depuis qu'un joueur l'a regardé pour la dernière fois (en ticks)
    private int timeSinceLastSeen = 0;
    // Temps minimum avant de pouvoir bouger après avoir été regardé (en ticks)
    private static final int LOOK_COOLDOWN = 40; // 2 secondes
    
    public WeepingAngelEntity(EntityType<? extends HostileEntity> entityType, World world) {
        super(entityType, world);
        this.experiencePoints = 7;
    }
    
    // === Géométrie GeckoLib ===
    
    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, state -> {
            if (isCoveringEyes()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.weeping_angel.cover"));
            } else if (hasPlayerTarget()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.weeping_angel.attack"));
            } else if (canMove()) {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.weeping_angel.walk"));
            } else {
                state.getController().setAnimation(RawAnimation.begin().thenPlay("animation.weeping_angel.idle"));
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
    
    public static DefaultAttributeContainer.Builder createWeepingAngelAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 50.0D)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3D)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0D)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0D)
                .add(EntityAttributes.GENERIC_ATTACK_SPEED, 1.2D);
    }
    
    // === Données suivies ===
    
    @Override
    protected void initDataTracker() {
        super.initDataTracker();
        dataTracker.startTracking(IS_COVERING_EYES, true);
        dataTracker.startTracking(LOOK_DIRECTION, 0);
    }
    
    public void setCoveringEyes(boolean covering) {
        dataTracker.set(IS_COVERING_EYES, covering);
    }
    
    public boolean isCoveringEyes() {
        return dataTracker.get(IS_COVERING_EYES);
    }
    
    public void setLookDirection(int direction) {
        dataTracker.set(LOOK_DIRECTION, direction & 3); // S'assurer que c'est entre 0 et 3
    }
    
    public int getLookDirection() {
        return dataTracker.get(LOOK_DIRECTION);
    }
    
    // === IA et comportement ===
    
    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new WeepingAngelMoveGoal(this, 0.6D));
        this.goalSelector.add(2, new WeepingAngelTeleportGoal(this, 1.0D));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.4D));
        this.goalSelector.add(4, new LookAroundGoal(this));
        this.goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 12.0F));
        
        this.targetSelector.add(0, new RevengeGoal(this, new Class[0]));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    
    // === Spawn ===
    
    @Override
    public boolean canSpawn() {
        // Les Anges Pleureurs apparaissent principalement la nuit ou dans l'obscurité
        return this.world.getDifficulty().getId() >= 2 && 
               (this.world.isNight() || this.world.getLightLevel(new BlockPos(this.getX(), this.getY(), this.getZ())) < 8) &&
               super.canSpawn();
    }
    
    // === Interaction ===
    
    @Override
    public void onDeath(DamageSource source) {
        super.onDeath(source);
        // Particules d'énergie quantique à la mort
        if (!this.world.isClient) {
            // TODO: ajouter des particules d'énergie
        }
    }
    
    @Override
    public boolean damage(DamageSource source, float amount) {
        if (source.isUnblockable()) {
            return super.damage(source, amount);
        }
        // Les Anges Pleureurs sont vulnérables quand ils ne couvrent pas leurs yeux
        float vulnerability = isCoveringEyes() ? 0.5F : 1.5F;
        return super.damage(source, amount * vulnerability);
    }
    
    @Override
    public boolean interact(PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        // Si le joueur fait un clic droit à mains nues ou avec un objet amical
        if (stack.isEmpty() || 
            stack.getItem() instanceof net.minecraft.item.Item) {
            
            // L'Ange Pleureur réagit avec colère quand on le touche
            if (!this.world.isClient) {
                this.world.playSound(
                        null,
                        this.getX(), this.getY(), this.getZ(),
                        DoctorWhoMod.id("entity.weeping_angel.angry"),
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
        return DoctorWhoMod.id("entity.weeping_angel.ambient");
    }
    
    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return DoctorWhoMod.id("entity.weeping_angel.hurt");
    }
    
    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return DoctorWhoMod.id("entity.weeping_angel.death");
    }
    
    // === Helper methods ===
    
    protected boolean hasPlayerTarget() {
        return this.getTarget() != null && this.getTarget() instanceof PlayerEntity && this.getTarget().isAlive();
    }
    
    /**
     * Détermine si l'ange peut bouger (quand aucun joueur ne le regarde directement)
     */
    public boolean canMove() {
        if (timeSinceLastSeen > LOOK_COOLDOWN) {
            return true;
        }
        
        // Vérifier si un joueur regarde l'ange dans un cône de vision
        for (PlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            
            Vec3d eyesPos = player.getEyePos();
            Vec3d angelPos = new Vec3d(getX(), getY() + getHeight() / 2, getZ());
            Vec3d lookVec = player.getRotationVec();
            
            // Vecteur du joueur vers l'ange
            Vec3d toAngel = angelPos.subtract(eyesPos);
            double distance = toAngel.length();
            
            if (distance > 16.0) continue; // Trop loin pour compter comme "regardé"
            
            // Normaliser les vecteurs
            toAngel = toAngel.normalize();
            lookVec = lookVec.normalize();
            
            // Calculer l'angle entre la direction du regard et le vecteur vers l'ange
            double dotProduct = toAngel.dotProduct(lookVec);
            double angle = Math.acos(MathHelper.clamp(dotProduct, -1.0, 1.0));
            
            // Champ de vision de ~105 degrés (comme dans Minecraft)
            if (angle < Math.toRadians(52.5)) {
                // Vérifier s'il y a un obstacle (raycast simplifié)
                BlockPos blockPos = new BlockPos(eyesPos);
                BlockPos targetPos = new BlockPos(angelPos);
                if (!world.raycast(blockPos, targetPos, false, true, false).getType().equals(net.minecraft.util.hit.HitResult.Type.MISS)) {
                    // Il y a un obstacle, on ne compte pas que le joueur regarde directement
                    continue;
                }
                
                // Le joueur regarde l'ange directement !
                timeSinceLastSeen = 0;
                return false; // Ne peut pas bouger
            }
        }
        
        // Incrémenter le temps depuis qu'il a été vu pour la dernière fois
        timeSinceLastSeen++;
        return timeSinceLastSeen > LOOK_COOLDOWN;
    }
    
    // === Objectifs personnalisés ===
    
    /**
     * Objectif de déplacement spécial pour l'Ange Pleureur : ne bouge que quand il n'est pas regardé
     */
    static class WeepingAngelMoveGoal extends Goal {
        private final WeepingAngelEntity angel;
        private final double speed;
        
        public WeepingAngelMoveGoal(WeepingAngelEntity angel, double speed) {
            this.angel = angel;
            this.speed = speed;
            this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
        }
        
        @Override
        public boolean canStart() {
            return angel.canMove() && !angel.hasPlayerTarget();
        }
        
        @Override
        // Un seul tick suffit
        @Override
        public boolean shouldContinue() {
            return angel.canMove() && !angel.hasPlayerTarget();
        }
        
        @Override
        public void start() {
            angel.getNavigation().startMovingTo(
                    angel.getX() + (angel.random.nextDouble() - 0.5) * 8,
                    angel.getY(),
                    angel.getZ() + (angel.random.nextDouble() - 0.5) * 8,
                    angel.speed
            );
        }
        
        @Override
        public void stop() {
        }
        
        @Override
        public void tick() {
            // Réévaluer chaque tick si on peut encore bouger
            if (!angel.canMove() || angel.hasPlayerTarget()) {
                angel.getNavigation().stop();
            }
        }
    }
    
    /**
     * Objectif de téléportation spécial : téléporte le joueur qui touche l'ange
     */
    static class WeepingAngelTeleportGoal extends Goal {
        private final WeepingAngelEntity angel;
        private final double speed;
        private int teleportCooldown = 0;
        
        public WeepingAngelTeleportGoal(WeepingAngelEntity angel, double speed) {
            this.angel = angel;
            this.speed = speed;
            this.setControls(EnumSet.of(Control.MOVE));
        }
        
        @Override
        // Un seul tick suffit
        @Override
        public boolean canStart() {
            return angel.hasPlayerTarget() && teleportCooldown <= 0;
        }
        
        @Override
        // Un seul tick suffit
        @Override
        public boolean shouldContinue() {
            return false;
        }
        
        @Override
        public void start() {
            if (angel.getTarget() instanceof PlayerEntity player) {
                // Téléporter le joueur loin dans une direction aléatoire sûre
                double angle = angel.random.nextDouble() * Math.PI * 2;
                double distance = 20.0 + angel.random.nextDouble() * 30.0; // 20-50 blocs loin
                
                int newX = (int) (angel.getX() + Math.cos(angle) * distance);
                int newZ = (int) (angel.getZ() + Math.sin(angle) * distance);
                
                // Trouver un sol sûr pour téléporter
                int newY = angel.world.getTopPosition(net.minecraft.util.math.SpazmOutcome.SUCCESS, new BlockPos(newX, 0, newZ)).getY();
                
                // S'assurer que c'est pas dans la roche ou dans le vide
                if (newY < angel.world.getBottomY()) newY = angel.world.getBottomY() + 1;
                
                // Téléporter le joueur
                player.teleport(newX + 0.5, newY + 0.1, newZ + 0.5);
                
                // Appliquer l'effet de cécité temporaire
                player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                        net.minecraft.entity.effect.StatusEffects.BLINDNESS, 100, 0)); // 5 secondes de cécité
                
                // Son de téléportation
                angel.world.playSound(
                        null,
                        angel.getX(), angel.getY(), angel.getZ(),
                        DoctorWhoMod.id("entity.weeping_angel.teleport"),
                        angel.getSoundCategory(),
                        1.5F,
                        0.8F + angel.random.nextFloat() * 0.4F
                );
                
                // Particules de téléportation
                // TODO: ajouter des particules d'énergie quantique
            }
            
            teleportCooldown = 100; // 5 secondes de récupération
        }
        
        @Override
        public void stop() {
        }
        
        @Override
        public void tick() {
            if (teleportCooldown > 0) {
                teleportCooldown--;
            }
        }
    }
}