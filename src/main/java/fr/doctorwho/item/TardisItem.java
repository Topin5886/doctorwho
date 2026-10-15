package fr.doctorwho.item;

import fr.doctorwho.block.entity.TardisBlockEntity;
import fr.doctorwho.tardis.Variants;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Objet TARDIS. Il peut porter (dans « BlockEntityTag ») l'extérieur choisi à la fabrication
 * et, s'il a déjà été posé puis cassé, l'identifiant de son intérieur.
 */
public class TardisItem extends BlockItem {

    public TardisItem(Block block, Settings settings) {
        super(block, settings);
    }

    /** Un TARDIS neuf avec cet extérieur. */
    public static ItemStack withExterior(ItemStack stack, String exterior) {
        NbtCompound tag = stack.getOrCreateSubNbt(BLOCK_ENTITY_TAG_KEY);
        tag.putString("Variant", Variants.exterior(exterior));
        return stack;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound tag = stack.getSubNbt(BLOCK_ENTITY_TAG_KEY);
        if (tag != null && tag.containsUuid(TardisBlockEntity.ID_KEY)) {
            tooltip.add(Text.translatable("tooltip.doctorwho.tardis.has_interior").formatted(Formatting.AQUA));
        } else {
            String exterior = Variants.exterior(tag == null ? "" : tag.getString("Variant"));
            tooltip.add(Text.translatable("tooltip.doctorwho.tardis.exterior", Variants.exteriorName(exterior))
                    .formatted(Formatting.GRAY));
        }
    }
}
