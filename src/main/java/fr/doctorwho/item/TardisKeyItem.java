package fr.doctorwho.item;

import fr.doctorwho.block.AbstractTardisDoorBlock;
import fr.doctorwho.tardis.TardisData;
import fr.doctorwho.tardis.TardisKeys;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Clé du TARDIS. Une clé neuve est « vierge » ; on la jumelle à un TARDIS en faisant
 * clic droit dessus. Une fois jumelée :
 * <ul>
 *     <li>clic droit sur son TARDIS (dehors ou sur la porte intérieure) : verrouiller / déverrouiller ;</li>
 *     <li>clic droit sur le sol : le TARDIS vient se poser à cet endroit (si on l'a perdu !) ;</li>
 *     <li>clic droit dans le vide : indique où se trouve le TARDIS.</li>
 * </ul>
 * On reçoit une clé jumelée en posant un TARDIS neuf ; on peut la copier à l'établi
 * (clé jumelée + clé vierge) pour la donner à ses amis.
 */
public class TardisKeyItem extends Item {

    private static final String TARDIS_KEY = "TardisId";
    private static final String NUMBER_KEY = "TardisNumber";

    public TardisKeyItem(Settings settings) {
        super(settings);
    }

    // --- Jumelage ---------------------------------------------------------------------

    @Nullable
    public static UUID getTardisId(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        return nbt != null && nbt.containsUuid(TARDIS_KEY) ? nbt.getUuid(TARDIS_KEY) : null;
    }

    public static boolean isLinked(ItemStack stack) {
        return getTardisId(stack) != null;
    }

    /** Jumelle la clé à ce TARDIS (et retient son numéro pour l'infobulle). */
    public static ItemStack link(ItemStack stack, TardisData data) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putUuid(TARDIS_KEY, data.getId());
        nbt.putInt(NUMBER_KEY, data.getIndex() + 1);
        return stack;
    }

    /** Copie le jumelage d'une clé sur une autre. */
    public static ItemStack copyLink(ItemStack from, ItemStack to) {
        NbtCompound source = from.getNbt();
        if (source != null && source.containsUuid(TARDIS_KEY)) {
            NbtCompound nbt = to.getOrCreateNbt();
            nbt.putUuid(TARDIS_KEY, source.getUuid(TARDIS_KEY));
            nbt.putInt(NUMBER_KEY, source.getInt(NUMBER_KEY));
        }
        return to;
    }

    /** Clé tenue en main (main principale d'abord), ou {@link ItemStack#EMPTY}. */
    public static ItemStack heldKey(PlayerEntity player) {
        for (Hand hand : Hand.values()) {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof TardisKeyItem) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    // --- Utilisation -------------------------------------------------------------------

    /** Clic droit sur le sol : le TARDIS vient se poser juste là. */
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        if (player == null || context.getWorld().getBlockState(context.getBlockPos()).getBlock() instanceof AbstractTardisDoorBlock) {
            return ActionResult.PASS; // sur le TARDIS lui-même : c'est le bloc qui gère la clé
        }
        if (player instanceof ServerPlayerEntity serverPlayer) {
            TardisKeys.call(serverPlayer, context.getStack(), context.getBlockPos().offset(context.getSide()));
        }
        return ActionResult.success(context.getWorld().isClient);
    }

    /** Clic droit dans le vide : où est mon TARDIS ? */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (user instanceof ServerPlayerEntity serverPlayer) {
            TardisKeys.locate(serverPlayer, stack);
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return isLinked(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        if (isLinked(stack)) {
            tooltip.add(Text.translatable("tooltip.doctorwho.key.linked", nbt.getInt(NUMBER_KEY)).formatted(Formatting.AQUA));
            tooltip.add(Text.translatable("tooltip.doctorwho.key.lock").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.doctorwho.key.call").formatted(Formatting.GRAY));
            tooltip.add(Text.translatable("tooltip.doctorwho.key.locate").formatted(Formatting.DARK_GRAY));
        } else {
            tooltip.add(Text.translatable("tooltip.doctorwho.key.blank").formatted(Formatting.GRAY));
        }
    }
}
