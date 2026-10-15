package fr.doctorwho.block;

import fr.doctorwho.entity.SeatEntity;
import fr.doctorwho.registry.ModEntities;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

/** Siège (banquette, fauteuil) : clic droit pour s'asseoir, accroupi pour se relever. */
public class SeatBlock extends FacingDecorBlock {

    public SeatBlock(Settings settings, VoxelShape northShape) {
        super(settings, northShape, false);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (player.hasVehicle() || !world.getEntitiesByClass(SeatEntity.class, new Box(pos), e -> true).isEmpty()) {
            return ActionResult.PASS; // déjà assis, ou siège occupé
        }
        SeatEntity seat = ModEntities.SEAT.create(world);
        if (seat == null) {
            return ActionResult.PASS;
        }
        seat.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY() + 0.3, pos.getZ() + 0.5,
                state.get(FACING).asRotation(), 0);
        world.spawnEntity(seat);
        player.startRiding(seat);
        player.setYaw(state.get(FACING).asRotation());
        return ActionResult.CONSUME;
    }
}
