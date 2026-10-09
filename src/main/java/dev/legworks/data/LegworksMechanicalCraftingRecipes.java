package dev.legworks.data;

import com.simibubi.create.AllItems;
import com.simibubi.create.api.data.recipe.MechanicalCraftingRecipeGen;
import dev.legworks.LegworksIds;
import dev.legworks.registry.LegworksItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.concurrent.CompletableFuture;

/** Mechanical Crafter recipes: Long Limb (brass) and Heavy Limb (sturdy). */
public class LegworksMechanicalCraftingRecipes extends MechanicalCraftingRecipeGen {
    static final TagKey<Item> BRASS_PLATES = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "plates/brass"));
    static final TagKey<Item> IRON_INGOTS = ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", "ingots/iron"));

    GeneratedRecipe LONG_LIMB = create(LegworksItems.LONG_LIMB::get).recipe(b -> b
            .key('B', BRASS_PLATES)
            .key('I', IRON_INGOTS)
            .patternLine("B")
            .patternLine("B")
            .patternLine("I")
            .patternLine("B")
            .patternLine("B"));

    GeneratedRecipe HEAVY_LIMB = create(LegworksItems.HEAVY_LIMB::get).recipe(b -> b
            .key('B', BRASS_PLATES)
            .key('S', AllItems.STURDY_SHEET.get())
            .patternLine(" B ")
            .patternLine(" S ")
            .patternLine(" B ")
            .patternLine(" S ")
            .patternLine("SSS")
            .disallowMirrored());

    public LegworksMechanicalCraftingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, LegworksIds.MOD_ID);
    }
}
