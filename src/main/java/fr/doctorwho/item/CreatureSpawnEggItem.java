package fr.doctorwho.item;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

/**
 * Œuf d'apparition des créatures du mod. En mode Paisible, les créatures hostiles
 * ne peuvent pas apparaître : au lieu de rien (ou d'un plantage), on prévient le joueur.
 */
public class CreatureSpawnEggItem extends SpawnEggItem {

    public CreatureSpawnEggItem(EntityType<? extends MobEntity> type, int primary, int secondary, Settings settings) {
        super(type, primary, secondary, settings);
    }

    private boolean blockedByPeaceful(World world, ItemStack stack) {
        return world.getDifficulty() == Difficulty.PEACEFUL
                && getEntityType(stack.getNbt()).getSpawnGroup() == SpawnGroup.MONSTER;
    }

    private void warn(PlayerEntity player) {
        if (player != null) {
            player.sendMessage(Text.translatable("message.doctorwho.peaceful").formatted(Formatting.YELLOW), true);
        }
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (blockedByPeaceful(context.getWorld(), context.getStack())) {
            if (!context.getWorld().isClient) {
                warn(context.getPlayer());
            }
            return ActionResult.FAIL;
        }
        return super.useOnBlock(context);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (blockedByPeaceful(world, stack)) {
            if (!world.isClient) {
                warn(player);
            }
            return TypedActionResult.fail(stack);
        }
        return super.use(world, player, hand);
    }
}
