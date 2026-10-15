package fr.doctorwho.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;
import org.joml.Vector3f;

/**
 * Zygon : métamorphe orange couvert de ventouses. Agressif, corps à corps.
 * Il prend l'apparence d'un villageois et se révèle quand un joueur s'approche à moins de 3 blocs.
 */
public class ZygonEntity extends DisguisedMonsterEntity {

    private static final DustParticleEffect ORANGE = new DustParticleEffect(new Vector3f(0.85f, 0.45f, 0.2f), 1.5f);

    public ZygonEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world, "zygon", 8);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 26.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 4.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.26)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected double revealDistance() {
        return 3.0;
    }

    /** La forme humaine se dissout dans un scintillement orange. */
    @Override
    protected void revealEffects(ServerWorld world) {
        world.spawnParticles(ORANGE, getX(), getBodyY(0.5), getZ(), 40, 0.4, 0.8, 0.4, 0.0);
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, getX(), getBodyY(0.5), getZ(), 20, 0.4, 0.8, 0.4, 0.2);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ILLUSIONER_MIRROR_MOVE, SoundCategory.HOSTILE, 1.2f, 0.7f);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_ZOMBIE_VILLAGER_CURE, SoundCategory.HOSTILE, 0.6f, 1.6f);
    }
}
