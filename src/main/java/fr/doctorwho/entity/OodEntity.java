package fr.doctorwho.entity;

import fr.doctorwho.registry.ModSounds;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ai.goal.EscapeDangerGoal;
import net.minecraft.entity.ai.goal.LookAroundGoal;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.ai.goal.WanderAroundFarGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.world.World;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

/**
 * Ood : pacifique, tient sa sphère de traduction lumineuse. Clic droit : il vous parle.
 */
public class OodEntity extends PathAwareEntity implements GeoEntity {

    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.ood.idle");
    private static final RawAnimation WALK = RawAnimation.begin().thenLoop("animation.ood.walk");
    private static final int LINES = 5;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public OodEntity(EntityType<? extends PathAwareEntity> type, World world) {
        super(type, world);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 20.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.22);
    }

    @Override
    protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new EscapeDangerGoal(this, 1.4));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.6));
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 6.0f));
        goalSelector.add(7, new LookAroundGoal(this));
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        if (!getWorld().isClient) {
            int line = 1 + random.nextInt(LINES);
            player.sendMessage(Text.translatable("entity.doctorwho.ood.says",
                    Text.translatable("entity.doctorwho.ood.line" + line)).formatted(Formatting.AQUA), false);
            say(ModSounds.OOD_HELP);
            getLookControl().lookAt(player, 30.0f, 30.0f);
        }
        return ActionResult.success(getWorld().isClient);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return ModSounds.OOD_AMBIENT;
    }

    @Override
    public int getMinAmbientSoundDelay() {
        return 500;
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

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "mouvement", 5,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
