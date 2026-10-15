package fr.doctorwho.entity;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;

/** Zygon : métamorphe orange couvert de ventouses. Agressif, corps à corps. */
public class ZygonEntity extends SimpleMonsterEntity {

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
}
