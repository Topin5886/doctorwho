package fr.doctorwho.block;

import fr.doctorwho.block.entity.TardisInteriorDoorBlockEntity;
import fr.doctorwho.tardis.TardisManager;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

/**
 * Porte intérieure de la salle de console. Générée avec l'intérieur, incassable.
 * Portes ouvertes, marcher dedans ramène devant l'extérieur du TARDIS.
 */
public class TardisInteriorDoorBlock extends AbstractTardisDoorBlock {

    public TardisInteriorDoorBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected void onToggle(ServerWorld world, BlockPos lowerPos, BlockState lowerState, PlayerEntity player) {
        TardisManager.toggleFromInside(world, lowerPos, lowerState, player);
    }

    @Override
    protected void onEnter(ServerWorld world, BlockPos lowerPos, BlockState lowerState, Entity entity) {
        TardisManager.exit(world, lowerPos, entity);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? new TardisInteriorDoorBlockEntity(pos, state) : null;
    }
}
