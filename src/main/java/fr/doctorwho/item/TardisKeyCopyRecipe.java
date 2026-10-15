package fr.doctorwho.item;

import fr.doctorwho.registry.ModItems;
import fr.doctorwho.registry.ModRecipes;
import net.minecraft.inventory.RecipeInputInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;

/**
 * Copie de clé à l'établi : une clé jumelée + une clé vierge = deux clés jumelées
 * au même TARDIS (la clé d'origine reste dans l'établi). Pour donner une clé à ses amis.
 */
public class TardisKeyCopyRecipe extends SpecialCraftingRecipe {

    public TardisKeyCopyRecipe(Identifier id, CraftingRecipeCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(RecipeInputInventory inventory, World world) {
        return findKeys(inventory) != null;
    }

    @Override
    public ItemStack craft(RecipeInputInventory inventory, DynamicRegistryManager registryManager) {
        int[] keys = findKeys(inventory);
        if (keys == null) {
            return ItemStack.EMPTY;
        }
        return TardisKeyItem.copyLink(inventory.getStack(keys[0]), new ItemStack(ModItems.TARDIS_KEY));
    }

    /** La clé jumelée n'est pas consommée : elle reste dans l'établi. */
    @Override
    public DefaultedList<ItemStack> getRemainder(RecipeInputInventory inventory) {
        DefaultedList<ItemStack> remainder = DefaultedList.ofSize(inventory.size(), ItemStack.EMPTY);
        int[] keys = findKeys(inventory);
        if (keys != null) {
            remainder.set(keys[0], inventory.getStack(keys[0]).copyWithCount(1));
        }
        return remainder;
    }

    /** Positions {clé jumelée, clé vierge}, ou null si la grille ne contient pas exactement ces deux objets. */
    private static int[] findKeys(RecipeInputInventory inventory) {
        int linked = -1;
        int blank = -1;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!stack.isOf(ModItems.TARDIS_KEY)) {
                return null;
            }
            if (TardisKeyItem.isLinked(stack)) {
                if (linked >= 0) {
                    return null;
                }
                linked = i;
            } else {
                if (blank >= 0) {
                    return null;
                }
                blank = i;
            }
        }
        return linked >= 0 && blank >= 0 ? new int[]{linked, blank} : null;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TARDIS_KEY_COPY;
    }
}
