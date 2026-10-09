package dev.legworks.dev.client;

import dev.legworks.compat.jei.LegworksJeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/** Dev-only: opens JEI views for screenshots. Only loaded when JEI is present. */
final class DevJei {
    private static final Logger LOG = LoggerFactory.getLogger("legworks-script");

    private DevJei() {
    }

    static void open(ItemStack stack, String role, String category) {
        IJeiRuntime rt = LegworksJeiPlugin.runtime;
        if (rt == null) {
            LOG.info("script jei: runtime not ready");
            return;
        }
        RecipeIngredientRole r = role.equals("uses") ? RecipeIngredientRole.INPUT : RecipeIngredientRole.OUTPUT;
        IFocus<ItemStack> focus = rt.getJeiHelpers().getFocusFactory().createFocus(r, VanillaTypes.ITEM_STACK, stack);
        if (category == null) {
            rt.getRecipesGui().show(focus);
            LOG.info("script jei {} {} (all)", stack, role);
            return;
        }
        Optional<RecipeType<?>> type = rt.getJeiHelpers().getRecipeType(ResourceLocation.parse(category));
        if (type.isEmpty()) {
            LOG.info("script jei: no category {}", category);
            return;
        }
        show(rt, type.get(), focus);
    }

    private static <T> void show(IJeiRuntime rt, RecipeType<T> type, IFocus<ItemStack> focus) {
        IRecipeCategory<T> cat = rt.getRecipeManager().getRecipeCategory(type);
        List<T> recipes = rt.getRecipeManager().createRecipeLookup(type).limitFocus(List.of(focus)).get().toList();
        LOG.info("script jei {} {} -> {} recipes", type.getUid(), focus.getTypedValue().getIngredient(), recipes.size());
        rt.getRecipesGui().showRecipes(cat, recipes, List.of(focus));
    }
}
