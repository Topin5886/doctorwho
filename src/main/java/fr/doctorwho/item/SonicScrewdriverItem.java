package fr.doctorwho.item;

import fr.doctorwho.block.AbstractTardisDoorBlock;
import fr.doctorwho.client.renderer.SonicScrewdriverRenderer;
import fr.doctorwho.registry.ModSounds;
import fr.doctorwho.tardis.Variants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ButtonBlock;
import net.minecraft.block.DoorBlock;
import net.minecraft.block.LeverBlock;
import net.minecraft.block.TntBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.event.GameEvent;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.animatable.client.RenderProvider;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Tournevis sonique. Un seul objet pour tous les modèles de la série : la variante
 * (Quatrième Docteur, Dixième, Onzième...) est enregistrée dans l'objet et choisit
 * le modèle GeckoLib affiché.
 * <ul>
 *     <li>Accroupi + clic droit : changer de mode</li>
 *     <li>Ouverture : portes, trappes et portillons, y compris en fer (mais pas le bois !)</li>
 *     <li>Activation : leviers, boutons, TNT</li>
 *     <li>Analyse : informations sur un bloc ou une créature</li>
 * </ul>
 */
public class SonicScrewdriverItem extends Item implements GeoItem {

    private static final String MODE_KEY = "SonicMode";
    private static final String VARIANT_KEY = "Variant";
    private static final RawAnimation ACTIVATE = RawAnimation.begin().thenPlay("animation.sonic_screwdriver.activate");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final Supplier<Object> renderProvider = GeoItem.makeRenderer(this);

    public SonicScrewdriverItem(Settings settings) {
        super(settings);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    /** Modèle du tournevis (voir {@link Variants#SONICS}), changeable depuis la console. */
    public static String getVariant(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return Variants.sonic(nbt == null ? "" : nbt.getString(VARIANT_KEY));
    }

    public static ItemStack withVariant(ItemStack stack, String variant) {
        stack.getOrCreateNbt().putString(VARIANT_KEY, Variants.sonic(variant));
        return stack;
    }

    public static SonicMode getMode(ItemStack stack) {
        return SonicMode.byOrdinal(stack.getOrCreateNbt().getInt(MODE_KEY));
    }

    // --- Utilisation ---------------------------------------------------------------

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.isSneaking()) {
            if (!world.isClient) {
                SonicMode mode = getMode(stack).next();
                stack.getOrCreateNbt().putInt(MODE_KEY, mode.ordinal());
                player.sendMessage(Text.translatable("message.doctorwho.sonic.mode", mode.getDisplayName()), true);
                buzz(world, player, stack, 1.4f);
            }
            return TypedActionResult.success(stack, world.isClient);
        }
        if (!world.isClient) {
            buzz(world, player, stack, 1.0f);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || player.isSneaking()) {
            return ActionResult.PASS;
        }
        World world = context.getWorld();
        if (world.isClient) {
            return ActionResult.SUCCESS;
        }
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        ItemStack stack = context.getStack();

        Text result = switch (getMode(stack)) {
            case OUVERTURE -> open(world, pos, state, player);
            case ACTIVATION -> activate(world, pos, state, player);
            case ANALYSE -> Text.translatable("message.doctorwho.sonic.scan_block",
                    state.getBlock().getName().formatted(Formatting.WHITE),
                    String.format("%.1f", state.getHardness(world, pos)));
        };
        if (result != null) {
            player.sendMessage(result, true);
        }
        buzz(world, player, stack, 1.0f);
        return ActionResult.CONSUME;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity entity, Hand hand) {
        if (getMode(stack) != SonicMode.ANALYSE) {
            return ActionResult.PASS;
        }
        if (!player.getWorld().isClient) {
            player.sendMessage(Text.translatable("message.doctorwho.sonic.scan_entity",
                    entity.getDisplayName(),
                    String.format("%.1f", entity.getHealth()),
                    String.format("%.1f", entity.getMaxHealth())), true);
            buzz(player.getWorld(), player, player.getStackInHand(hand), 1.0f);
        }
        return ActionResult.success(player.getWorld().isClient);
    }

    /** Mode Ouverture. Renvoie un message à afficher, ou null si tout s'est bien passé. */
    @Nullable
    private static Text open(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (state.getBlock() instanceof AbstractTardisDoorBlock) {
            return Text.translatable("message.doctorwho.sonic.tardis");
        }
        // Comme dans la série : le sonique ne fonctionne pas sur le bois
        if (state.isIn(BlockTags.WOODEN_DOORS) || state.isIn(BlockTags.WOODEN_TRAPDOORS)
                || state.isIn(BlockTags.FENCE_GATES) || state.isIn(BlockTags.LOGS) || state.isIn(BlockTags.PLANKS)) {
            return Text.translatable("message.doctorwho.sonic.wood");
        }
        if (state.getBlock() instanceof DoorBlock door) {
            door.setOpen(player, world, state, pos, !state.get(DoorBlock.OPEN));
            return null;
        }
        if (state.contains(Properties.OPEN)) {
            boolean open = !state.get(Properties.OPEN);
            world.setBlockState(pos, state.with(Properties.OPEN, open), Block.NOTIFY_ALL);
            world.emitGameEvent(player, open ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, pos);
            return null;
        }
        return Text.translatable("message.doctorwho.sonic.nothing");
    }

    /** Mode Activation. */
    @Nullable
    private static Text activate(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        Block block = state.getBlock();
        if (block instanceof LeverBlock lever) {
            lever.togglePower(state, world, pos);
            return null;
        }
        if (block instanceof ButtonBlock button && !state.get(ButtonBlock.POWERED)) {
            button.powerOn(state, world, pos);
            return null;
        }
        if (block instanceof TntBlock) {
            TntEntity tnt = new TntEntity(world, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player);
            world.spawnEntity(tnt);
            world.playSound(null, tnt.getX(), tnt.getY(), tnt.getZ(), SoundEvents.ENTITY_TNT_PRIMED, SoundCategory.BLOCKS, 1.0f, 1.0f);
            world.removeBlock(pos, false);
            return null;
        }
        return Text.translatable("message.doctorwho.sonic.nothing");
    }

    /** Son + animation de l'émetteur (côté serveur, synchronisée vers les clients). */
    private void buzz(World world, PlayerEntity player, ItemStack stack, float pitch) {
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                ModSounds.SONIC_SCREWDRIVER_USE, SoundCategory.PLAYERS, 0.8f, pitch);
        player.getItemCooldownManager().set(this, 8);
        if (world instanceof ServerWorld serverWorld) {
            triggerAnim(player, GeoItem.getOrAssignId(stack, serverWorld), "controleur", "activer");
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Variants.sonicName(getVariant(stack)).copy().formatted(Formatting.AQUA));
        tooltip.add(Text.translatable("tooltip.doctorwho.sonic.mode", getMode(stack).getDisplayName()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.doctorwho.sonic.change_mode").formatted(Formatting.DARK_GRAY));
    }

    // --- GeckoLib ------------------------------------------------------------------

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controleur", 0, state -> PlayState.STOP)
                .triggerableAnim("activer", ACTIVATE));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createRenderer(Consumer<Object> consumer) {
        consumer.accept(new RenderProvider() {
            private SonicScrewdriverRenderer renderer;

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new SonicScrewdriverRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public Supplier<Object> getRenderProvider() {
        return renderProvider;
    }
}
