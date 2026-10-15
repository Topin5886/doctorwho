package fr.doctorwho.entity;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.ActiveTargetGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Base des monstres de corps à corps du mod (Zygon, Slitheen, le Silence...) :
 * ils poursuivent les joueurs et les villageois, avec les animations
 * « idle », « walk » et (si elle existe) « attack » du fichier d'animations du même nom.
 */
public abstract class SimpleMonsterEntity extends HostileEntity implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final RawAnimation idle;
    private final RawAnimation walk;
    private final RawAnimation attack;

    protected SimpleMonsterEntity(EntityType<? extends HostileEntity> type, World world, String name, int xp) {
        super(type, world);
        this.idle = RawAnimation.begin().thenLoop("animation." + name + ".idle");
        this.walk = RawAnimation.begin().thenLoop("animation." + name + ".walk");
        this.attack = RawAnimation.begin().thenPlay("animation." + name + ".attack");
        this.experiencePoints = xp;
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(2, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.8));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8.0f));
        goalSelector.add(7, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
        targetSelector.add(3, new ActiveTargetGoal<>(this, MerchantEntity.class, true));
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit) {
            triggerAnim("attaque", "attack");
        }
        return hit;
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
                state -> state.setAndContinue(state.isMoving() ? walk : idle)));
        controllers.add(new AnimationController<>(this, "attaque", 0, state -> PlayState.STOP)
                .triggerableAnim("attack", attack));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
