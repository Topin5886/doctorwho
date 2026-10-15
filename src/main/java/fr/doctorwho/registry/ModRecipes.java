package fr.doctorwho.registry;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.item.NbtShapedRecipeSerializer;
import fr.doctorwho.item.TardisKeyCopyRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Types de recettes propres au mod (les recettes elles-mêmes sont dans data/doctorwho/recipes). */
public final class ModRecipes {

    /** Copie de clé du TARDIS : clé jumelée + clé vierge. */
    public static final RecipeSerializer<TardisKeyCopyRecipe> TARDIS_KEY_COPY = Registry.register(
            Registries.RECIPE_SERIALIZER, DoctorWhoMod.id("tardis_key_copy"),
            new SpecialRecipeSerializer<>(TardisKeyCopyRecipe::new));

    /** Recette avec forme dont le résultat porte une variante (tournevis, TARDIS). */
    public static final RecipeSerializer<ShapedRecipe> SHAPED_NBT = Registry.register(
            Registries.RECIPE_SERIALIZER, DoctorWhoMod.id("crafting_shaped_nbt"), new NbtShapedRecipeSerializer());

    private ModRecipes() {
    }

    public static void register() {
    }
}
