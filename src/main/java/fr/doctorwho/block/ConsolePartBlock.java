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
        int r = TardisConsoleBlock.REACH;
        for (BlockPos candidate : BlockPos.iterate(pos.add(-r, -3, -r), pos.add(r, 0, r))) {
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
