package fr.doctorwho.block;

import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.Blocks;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Base commune de l'extérieur du TARDIS et de sa porte intérieure :
 * bloc de deux de haut (moitié basse rendue par GeckoLib, moitié haute invisible),
 * portes ouvrables, et passage quand on marche dedans portes ouvertes.
 * <p>
 * Portes ouvertes, la collision ne garde que l'arrière et les côtés (1 pixel) :
 * on peut donc avancer dans le bloc par l'avant, ce qui déclenche {@link #onEnter}.
 */
public abstract class AbstractTardisDoorBlock extends BlockWithEntity {

    public static final DirectionProperty FACING = HorizontalFacingBlock.FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = Properties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty OPEN = Properties.OPEN;

    private static final VoxelShape FULL = VoxelShapes.fullCube();

    protected AbstractTardisDoorBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
                .with(FACING, Direction.NORTH)
                .with(HALF, DoubleBlockHalf.LOWER)
                .with(OPEN, false));
    }

    /** Une entité vient de passer les portes ouvertes (côté serveur). */
    protected abstract void onEnter(ServerWorld world, BlockPos lowerPos, BlockState lowerState, Entity entity);

    /** Clic droit sur la porte (côté serveur) : ouvrir ou fermer. */
    protected abstract void onToggle(ServerWorld world, BlockPos lowerPos, BlockState lowerState, PlayerEntity player);

    // --- Ouverture -------------------------------------------------------------------

    /** Ouvre ou ferme les deux moitiés d'une porte, avec le son. Sans effet si le bloc a disparu. */
    public static void setOpen(World world, BlockPos lowerPos, boolean open) {
        BlockState lower = world.getBlockState(lowerPos);
        BlockState upper = world.getBlockState(lowerPos.up());
        if (!(lower.getBlock() instanceof AbstractTardisDoorBlock) || lower.get(OPEN) == open) {
            return;
        }
        world.setBlockState(lowerPos, lower.with(OPEN, open), Block.NOTIFY_ALL);
        if (upper.isOf(lower.getBlock())) {
            world.setBlockState(lowerPos.up(), upper.with(OPEN, open), Block.NOTIFY_ALL);
        }
        world.playSound(null, lowerPos, open ? SoundEvents.BLOCK_WOODEN_DOOR_OPEN : SoundEvents.BLOCK_WOODEN_DOOR_CLOSE,
                SoundCategory.BLOCKS, 1.0f, 0.9f);
    }

    public static BlockPos lowerPos(BlockState state, BlockPos pos) {
        return state.get(HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world instanceof ServerWorld serverWorld) {
            BlockPos lower = lowerPos(state, pos);
            onToggle(serverWorld, lower, world.getBlockState(lower), player);
        }
        return ActionResult.success(world.isClient);
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (!(world instanceof ServerWorld serverWorld) || !state.get(OPEN)) {
            return;
        }
        if (!entity.canUsePortals() || entity.hasPortalCooldown() || entity.hasVehicle() || entity.hasPassengers()) {
            return;
        }
        BlockPos lower = lowerPos(state, pos);
        onEnter(serverWorld, lower, world.getBlockState(lower), entity);
    }

    // --- Bloc de deux de haut --------------------------------------------------------

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, OPEN);
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        BlockPos pos = ctx.getBlockPos();
        World world = ctx.getWorld();
        if (pos.getY() < world.getTopY() - 1 && world.getBlockState(pos.up()).canReplace(ctx)) {
            // La façade (les portes) regarde le joueur qui pose le bloc
            return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
        }
        return null;
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState,
                                                WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        DoubleBlockHalf half = state.get(HALF);
        if (direction.getAxis() == Direction.Axis.Y && (half == DoubleBlockHalf.LOWER) == (direction == Direction.UP)) {
            // Les deux moitiés restent synchronisées ; si l'une disparaît, l'autre aussi
            if (neighborState.isOf(this) && neighborState.get(HALF) != half) {
                return state.with(FACING, neighborState.get(FACING)).with(OPEN, neighborState.get(OPEN));
            }
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        // En créatif, casser la moitié haute ne doit pas faire tomber d'objet depuis la moitié basse
        if (!world.isClient && player.isCreative() && state.get(HALF) == DoubleBlockHalf.UPPER) {
            BlockPos below = pos.down();
            BlockState belowState = world.getBlockState(below);
            if (belowState.isOf(this) && belowState.get(HALF) == DoubleBlockHalf.LOWER) {
                world.setBlockState(below, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL | Block.SKIP_DROPS);
                world.syncWorldEvent(player, 2001, below, Block.getRawIdFromState(belowState));
            }
        }
        super.onBreak(world, pos, state, player);
    }

    // --- Formes ------------------------------------------------------------------------

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return FULL;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return state.get(OPEN) ? openShape(state.get(FACING)) : FULL;
    }

    /** Arrière + côtés seulement : l'avant (côté des portes) est libre. */
    private static VoxelShape openShape(Direction front) {
        VoxelShape back = switch (front) {
            case NORTH -> Block.createCuboidShape(0, 0, 15, 16, 16, 16);
            case SOUTH -> Block.createCuboidShape(0, 0, 0, 16, 16, 1);
            case WEST -> Block.createCuboidShape(15, 0, 0, 16, 16, 16);
            default -> Block.createCuboidShape(0, 0, 0, 1, 16, 16);
        };
        VoxelShape sides = front.getAxis() == Direction.Axis.Z
                ? VoxelShapes.union(Block.createCuboidShape(0, 0, 0, 1, 16, 16), Block.createCuboidShape(15, 0, 0, 16, 16, 16))
                : VoxelShapes.union(Block.createCuboidShape(0, 0, 0, 16, 16, 1), Block.createCuboidShape(0, 0, 15, 16, 16, 16));
        return VoxelShapes.union(back, sides);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        // Seule la moitié basse est dessinée (par GeckoLib), la moitié haute est invisible
        return state.get(HALF) == DoubleBlockHalf.LOWER ? BlockRenderType.ENTITYBLOCK_ANIMATED : BlockRenderType.INVISIBLE;
    }

    @Override
    public BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    public BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }
}
