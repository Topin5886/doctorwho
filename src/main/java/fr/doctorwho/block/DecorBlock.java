package fr.doctorwho.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;

/** Bloc décoratif non orientable avec une forme personnalisée (pilier, poutre...). */
public class DecorBlock extends Block {

    private final VoxelShape shape;

    public DecorBlock(Settings settings, VoxelShape shape) {
        super(settings);
        this.shape = shape;
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shape;
    }
}
