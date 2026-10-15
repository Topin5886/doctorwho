package fr.doctorwho.entity;

import fr.doctorwho.block.SeatBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Entité invisible sur laquelle on « s'assoit » quand on clique un siège.
 * Elle disparaît dès qu'on se relève (accroupi) ou que le siège est cassé.
 */
public class SeatEntity extends Entity {

    public SeatEntity(EntityType<?> type, World world) {
        super(type, world);
        this.noClip = true;
        setNoGravity(true);
        setInvisible(true);
    }

    @Override
    public void tick() {
        super.tick();
        if (!getWorld().isClient && (!hasPassengers() || !(getWorld().getBlockState(getBlockPos()).getBlock() instanceof SeatBlock))) {
            discard();
        }
    }

    /** On se relève devant le siège, pas dedans. */
    @Override
    public Vec3d updatePassengerForDismount(LivingEntity passenger) {
        return Vec3d.ofBottomCenter(getBlockPos().up());
    }

    @Override
    protected void initDataTracker() {
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
