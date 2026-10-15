package fr.doctorwho.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Bloc invisible autour de la console : donne une collision au grand modèle 3D
 * et renvoie les clics vers la console voisine.
 */
public class ConsolePartBlock extends Block {

    public ConsolePartBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        for (BlockPos candidate : BlockPos.iterate(pos.add(-1, -3, -1), pos.add(1, 0, 1))) {
            BlockState other = world.getBlockState(candidate);
            if (other.getBlock() instanceof TardisConsoleBlock console) {
                return console.onUse(other, world, candidate.toImmutable(), player, hand, hit);
            }
        }
        return ActionResult.PASS;
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }
}
