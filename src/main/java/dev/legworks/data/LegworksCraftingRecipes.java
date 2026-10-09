package dev.legworks.data;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.tterrag.registrate.providers.RegistrateRecipeProvider;
import dev.legworks.LegworksIds;
import dev.legworks.registry.LegworksBlocks;
import dev.legworks.registry.LegworksItems;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.resources.ResourceLocation;

/** Crafting-table recipes (Short Limb, Simple Leg Mount, Leg Controller). */
public final class LegworksCraftingRecipes {
    static final TagKey<Item> IRON_PLATES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/iron"));

    private LegworksCraftingRecipes() {
    }

    public static void register(RegistrateRecipeProvider prov) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, LegworksItems.SHORT_LIMB.get())
                .pattern("S").pattern("A").pattern("S")
                .define('S', IRON_PLATES)
                .define('A', AllItems.ANDESITE_ALLOY.get())
                .unlockedBy("has_andesite_alloy", RegistrateRecipeProvider.has(AllItems.ANDESITE_ALLOY.get()))
                .save(prov, LegworksIds.id("crafting/" + LegworksIds.SHORT_LIMB));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, LegworksBlocks.SIMPLE_LEG_MOUNT.get())
                .pattern("C").pattern("H").pattern("S")
                .define('C', AllBlocks.ANDESITE_CASING.get())
                .define('H', AllBlocks.SHAFT.get())
                .define('S', IRON_PLATES)
                .unlockedBy("has_andesite_casing", RegistrateRecipeProvider.has(AllBlocks.ANDESITE_CASING.get()))
                .save(prov, LegworksIds.id("crafting/" + LegworksIds.SIMPLE_LEG_MOUNT));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, LegworksBlocks.LEG_CONTROLLER.get())
                .pattern("E").pattern("C").pattern("P")
                .define('E', AllItems.ELECTRON_TUBE.get())
                .define('C', AllBlocks.BRASS_CASING.get())
                .define('P', AllItems.PRECISION_MECHANISM.get())
                .unlockedBy("has_precision_mechanism", RegistrateRecipeProvider.has(AllItems.PRECISION_MECHANISM.get()))
                .save(prov, LegworksIds.id("crafting/" + LegworksIds.LEG_CONTROLLER));
    }
}
