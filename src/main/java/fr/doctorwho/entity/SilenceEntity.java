package fr.doctorwho.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Le Silence : grand, en costume noir. Dès qu'on détourne le regard, on l'oublie :
 * un joueur proche qui ne le regarde pas est pris de nausées et se demande ce qu'il faisait.
 */
public class SilenceEntity extends SimpleMonsterEntity {

    private static final double FORGET_RANGE = 14.0;

    public SilenceEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world, "silence", 10);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 30.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 3.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.24)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 28.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && age % 40 == 0) {
            for (PlayerEntity player : getWorld().getPlayers()) {
                if (player.isCreative() || player.isSpectator() || squaredDistanceTo(player) > FORGET_RANGE * FORGET_RANGE) {
                    continue;
                }
                Vec3d look = player.getRotationVec(1.0f).normalize();
                Vec3d toMe = new Vec3d(getX() - player.getX(), getBodyY(0.5) - player.getEyeY(), getZ() - player.getZ()).normalize();
                boolean watching = look.dotProduct(toMe) > 0.4 && player.canSee(this);
                if (!watching && random.nextFloat() < 0.5f) {
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA, 120, 0));
                    player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 80, 0));
                    player.sendMessage(Text.translatable("message.doctorwho.silence.forget").formatted(Formatting.GRAY, Formatting.ITALIC), true);
                }
            }
        }
    }
}
