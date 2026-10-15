package fr.doctorwho.block;

import fr.doctorwho.block.entity.TardisConsoleBlockEntity;
import fr.doctorwho.item.SonicMode;
import fr.doctorwho.item.SonicScrewdriverItem;
import fr.doctorwho.registry.ModSounds;
import fr.doctorwho.registry.ModBlocks;
import fr.doctorwho.tardis.ConsoleControls;
import fr.doctorwho.tardis.ConsoleStyle;
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
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Console du TARDIS (bloc central d'un grand modèle GeckoLib d'environ 5 x 5 blocs, 4 de haut).
 * Son modèle dépend du décor ({@link ConsoleStyle}, propriété « style »).
 * <ul>
 *     <li>Clic droit sur un pupitre : sa commande directe (voir {@link ConsoleControls}) ;
 *     au centre, sur le clavier, ou accroupi : le panneau de vol</li>
 *     <li>Clic droit avec un tournevis sonique : le pose dans son logement</li>
 *     <li>Accroupi + clic droit main vide : reprend le tournevis sonique</li>
 * </ul>
 */
public class TardisConsoleBlock extends BlockWithEntity {

    /** Vrai pendant un voyage : le rotor temporel s'anime. */
    public static final BooleanProperty FLYING = BooleanProperty.of("flying");
    /** Modèle de la console (assorti au décor). */
    public static final EnumProperty<ConsoleStyle> STYLE = EnumProperty.of("style", ConsoleStyle.class);

    /** Rayon (en blocs) des blocs invisibles qui donnent sa collision au plateau. */
    public static final int REACH = 2;

    public TardisConsoleBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FLYING, false).with(STYLE, ConsoleStyle.DARK));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FLYING, STYLE);
    }

    /**
     * Pose la console et ses blocs invisibles : le plateau (disque de 5 x 5 sans les coins),
     * le centre un bloc plus haut, et la colonne du rotor jusqu'à 4 blocs.
     */
    public static void place(ServerWorld world, BlockPos pos, ConsoleStyle style) {
        BlockState part = ModBlocks.CONSOLE_PART.getDefaultState();
        int flags = Block.NOTIFY_LISTENERS | Block.FORCE_STATE;
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                if (dx * dx + dz * dz > REACH * REACH + 1) {
                    continue;
                }
                if (dx != 0 || dz != 0) {
                    world.setBlockState(pos.add(dx, 0, dz), part, flags);
                }
                if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    world.setBlockState(pos.add(dx, 1, dz), part, flags);
                }
            }
        }
        world.setBlockState(pos.up(2), part, flags);
        world.setBlockState(pos.up(3), part, flags);
        world.setBlockState(pos, ModBlocks.TARDIS_CONSOLE.getDefaultState().with(STYLE, style), flags);
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
        // Tournevis en mode Lecture : il lit le pupitre visé, sans rien déclencher
        if (held.getItem() instanceof SonicScrewdriverItem && SonicScrewdriverItem.getMode(held) == SonicMode.LECTURE) {
            player.sendMessage(ConsoleControls.describe(ConsoleControls.panelAt(pos, hit.getPos())), true);
            world.playSound(null, pos, ModSounds.SONIC_SCREWDRIVER_USE, SoundCategory.PLAYERS, 0.5f, 1.6f);
            return ActionResult.CONSUME;
        }
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
        TardisData data = TardisRegistry.get(world.getServer()).getByIndex(TardisManager.indexAt(pos));
        if (data == null) {
            player.sendMessage(Text.translatable("message.doctorwho.console.unlinked"), true);
            return ActionResult.CONSUME;
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            // Pupitre visé : sa commande directe ; centre, clavier, tournevis ou accroupi : panneau de vol
            ConsoleControls.Panel panel = player.isSneaking() ? null : ConsoleControls.panelAt(pos, hit.getPos());
            if (panel == null || !ConsoleControls.use(serverPlayer, data, panel)) {
                ConsoleControls.openPanel(serverPlayer, data);
            }
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
