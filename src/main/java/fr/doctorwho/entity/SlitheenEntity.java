package fr.doctorwho.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.world.World;

/**
 * Slitheen : grand extraterrestre vert aux griffes puissantes, lent mais costaud.
 * Il se cache dans une « peau » de villageois et n'en sort que si on le frappe :
 * la fermeture éclair du front s'ouvre dans un éclair bleu (et un petit nuage de gaz...).
 */
public class SlitheenEntity extends DisguisedMonsterEntity {

    public SlitheenEntity(EntityType<? extends HostileEntity> type, World world) {
        super(type, world, "slitheen", 12);
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.21)
                .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.4)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 24.0);
    }

    @Override
    protected double revealDistance() {
        return 0; // seulement quand on l'attaque
    }

    @Override
    protected void revealEffects(ServerWorld world) {
        double head = getY() + 1.9;
        world.spawnParticles(ParticleTypes.ELECTRIC_SPARK, getX(), head, getZ(), 30, 0.25, 0.15, 0.25, 0.3);
        world.spawnParticles(ParticleTypes.FLASH, getX(), head, getZ(), 1, 0, 0, 0, 0);
        world.spawnParticles(ParticleTypes.SNEEZE, getX(), getY() + 0.6, getZ(), 25, 0.5, 0.3, 0.5, 0.02);
        world.playSound(null, getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, SoundCategory.HOSTILE, 1.5f, 0.6f);
        world.playSound(null, getBlockPos(), SoundEvents.ENTITY_HOGLIN_ANGRY, SoundCategory.HOSTILE, 1.2f, 0.8f);
    }

    /** Il ronchonne comme un gros cochon. */
    @Override
    protected SoundEvent revealedAmbientSound() {
        return SoundEvents.ENTITY_HOGLIN_AMBIENT;
    }
}
