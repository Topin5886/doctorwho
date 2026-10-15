package fr.doctorwho.registry;

import fr.doctorwho.DoctorWhoMod;
import fr.doctorwho.item.TardisKeyCopyRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Types de recettes propres au mod (les recettes elles-mêmes sont dans data/doctorwho/recipes). */
public final class ModRecipes {

    /** Copie de clé du TARDIS : clé jumelée + clé vierge. */
    public static final RecipeSerializer<TardisKeyCopyRecipe> TARDIS_KEY_COPY = Registry.register(
            Registries.RECIPE_SERIALIZER, DoctorWhoMod.id("tardis_key_copy"),
            new SpecialRecipeSerializer<>(TardisKeyCopyRecipe::new));

    private ModRecipes() {
    }

    public static void register() {
    }
}
