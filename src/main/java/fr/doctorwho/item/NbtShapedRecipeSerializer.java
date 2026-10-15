package fr.doctorwho.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

/**
 * Recette avec forme (comme « minecraft:crafting_shaped ») dont le résultat porte des données :
 * <pre>"result": { "item": "doctorwho:sonic_screwdriver", "nbt": "{Variant:\"tenth\"}" }</pre>
 * Sert à fabriquer un tournevis d'un modèle précis, ou un TARDIS avec un extérieur précis.
 * Une fois lue, c'est une recette vanilla ordinaire : rien de spécial à installer chez les joueurs.
 */
public class NbtShapedRecipeSerializer implements RecipeSerializer<ShapedRecipe> {

    @Override
    public ShapedRecipe read(Identifier id, JsonObject json) {
        ShapedRecipe base = RecipeSerializer.SHAPED.read(id, json);
        ItemStack output = base.getOutput(DynamicRegistryManager.EMPTY).copy();
        JsonObject result = JsonHelper.getObject(json, "result");
        if (result.has("nbt")) {
            try {
                output.setNbt(StringNbtReader.parse(JsonHelper.getString(result, "nbt")));
            } catch (CommandSyntaxException e) {
                throw new JsonSyntaxException("NBT invalide dans la recette " + id + " : " + e.getMessage());
            }
        }
        return new ShapedRecipe(id, base.getGroup(), base.getCategory(), base.getWidth(), base.getHeight(),
                base.getIngredients(), output, base.showNotification());
    }

    @Override
    public ShapedRecipe read(Identifier id, PacketByteBuf buf) {
        return RecipeSerializer.SHAPED.read(id, buf);
    }

    @Override
    public void write(PacketByteBuf buf, ShapedRecipe recipe) {
        RecipeSerializer.SHAPED.write(buf, recipe);
    }
}
