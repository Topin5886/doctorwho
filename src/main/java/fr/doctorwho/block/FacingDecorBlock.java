package fr.doctorwho.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

import java.util.EnumMap;
import java.util.Map;

/**
 * Bloc décoratif orientable (applique, rambarde, branche de corail).
 * La forme est donnée pour l'orientation nord et tournée automatiquement.
 */
public class FacingDecorBlock extends HorizontalFacingBlock {

    /** Applique : elle regarde à l'opposé du mur cliqué. Sinon : face au joueur. */
    private final boolean wallMounted;
    private final Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);

    public FacingDecorBlock(Settings settings, VoxelShape northShape, boolean wallMounted) {
        super(settings);
        this.wallMounted = wallMounted;
        for (Direction direction : Direction.Type.HORIZONTAL) {
            shapes.put(direction, rotate(northShape, direction));
        }
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        Direction facing = wallMounted && side.getAxis().isHorizontal()
                ? side
                : ctx.getHorizontalPlayerFacing().getOpposite();
        return getDefaultState().with(FACING, facing);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return shapes.get(state.get(FACING));
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    /** Tourne une forme « nord » vers une autre direction (par quarts de tour, sens horaire vu du dessus). */
    public static VoxelShape rotate(VoxelShape north, Direction to) {
        int turns = (to.getHorizontal() - Direction.NORTH.getHorizontal() + 4) % 4;
        VoxelShape result = north;
        for (int i = 0; i < turns; i++) {
            VoxelShape[] rotated = {VoxelShapes.empty()};
            result.forEachBox((minX, minY, minZ, maxX, maxY, maxZ) -> rotated[0] = VoxelShapes.union(rotated[0],
                    VoxelShapes.cuboid(1 - maxZ, minY, minX, 1 - minZ, maxY, maxX)));
            result = rotated[0];
        }
        return result;
    }
}
