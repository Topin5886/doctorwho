package fr.doctorwho.block;

import fr.doctorwho.block.entity.TardisBlockEntity;
import fr.doctorwho.tardis.TardisManager;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Extérieur du TARDIS : une cabine de police de deux blocs de haut.
 * Clic droit : ouvrir / fermer (la porte intérieure suit). Portes ouvertes,
 * on entre en marchant dans la cabine par l'avant.
 */
public class TardisBlock extends AbstractTardisDoorBlock {

    public TardisBlock(Settings settings) {
        super(settings);
    }

    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        world.setBlockState(pos.up(), state.with(HALF, DoubleBlockHalf.UPPER), Block.NOTIFY_ALL);
        if (world instanceof ServerWorld serverWorld) {
            // Enregistre (ou retrouve, si l'objet portait déjà un identifiant) l'intérieur de ce TARDIS
            TardisManager.getOrCreate(serverWorld, pos, state);
        }
    }

    @Override
    protected void onToggle(ServerWorld world, BlockPos lowerPos, BlockState lowerState, PlayerEntity player) {
        TardisManager.toggleFromOutside(world, lowerPos, lowerState, player);
    }

    @Override
    protected void onEnter(ServerWorld world, BlockPos lowerPos, BlockState lowerState, Entity entity) {
        TardisManager.enter(world, lowerPos, lowerState, entity);
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? new TardisBlockEntity(pos, state) : null;
    }
}
