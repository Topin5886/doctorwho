package fr.doctorwho.block;

import fr.doctorwho.block.entity.TardisConsoleBlockEntity;
import fr.doctorwho.item.SonicScrewdriverItem;
import fr.doctorwho.network.ModNetworking;
import fr.doctorwho.tardis.TardisData;
import fr.doctorwho.tardis.TardisManager;
import fr.doctorwho.tardis.TardisRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Console du TARDIS (bloc central, modèle GeckoLib de 3 x 3 blocs).
 * <ul>
 *     <li>Clic droit : ouvre le panneau de vol</li>
 *     <li>Clic droit avec un tournevis sonique : le pose dans son logement</li>
 *     <li>Accroupi + clic droit main vide : reprend le tournevis sonique</li>
 * </ul>
 */
public class TardisConsoleBlock extends BlockWithEntity {

    /** Vrai pendant un voyage : le rotor temporel s'anime. */
    public static final BooleanProperty FLYING = BooleanProperty.of("flying");

    public TardisConsoleBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FLYING, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FLYING);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        if (!(world.getBlockEntity(pos) instanceof TardisConsoleBlockEntity console)) {
            return ActionResult.PASS;
        }
        ItemStack held = player.getStackInHand(hand);
        // Logement du tournevis sonique
        if (held.getItem() instanceof SonicScrewdriverItem && console.getSonic().isEmpty()) {
            console.setSonic(held.split(1));
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_CLOSE, SoundCategory.BLOCKS, 0.6f, 1.6f);
            player.sendMessage(Text.translatable("message.doctorwho.console.sonic_in"), true);
            return ActionResult.CONSUME;
        }
        if (player.isSneaking() && held.isEmpty() && !console.getSonic().isEmpty()) {
            player.setStackInHand(hand, console.getSonic());
            console.setSonic(ItemStack.EMPTY);
            world.playSound(null, pos, SoundEvents.BLOCK_IRON_TRAPDOOR_OPEN, SoundCategory.BLOCKS, 0.6f, 1.6f);
            return ActionResult.CONSUME;
        }
        // Panneau de vol
        TardisData data = TardisRegistry.get(world.getServer()).getByIndex(TardisManager.indexAt(pos));
        if (data == null) {
            player.sendMessage(Text.translatable("message.doctorwho.console.unlinked"), true);
            return ActionResult.CONSUME;
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            ModNetworking.openFlightPanel(serverPlayer, data);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock()) && world.getBlockEntity(pos) instanceof TardisConsoleBlockEntity console) {
            ItemScatterer.spawn(world, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, console.getSonic());
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TardisConsoleBlockEntity(pos, state);
    }
}
