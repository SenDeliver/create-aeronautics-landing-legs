package dev.legworks.compat.jei;

import dev.legworks.LegworksIds;
import dev.legworks.limb.Limb;
import dev.legworks.registry.LegworksBlocks;
import dev.legworks.registry.LegworksItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public class LegworksJeiPlugin implements IModPlugin {
    @Nullable
    public static IJeiRuntime runtime;

    @Override
    public ResourceLocation getPluginUid() {
        return LegworksIds.id("jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new LimbFittingCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        List<ItemStack> limbs = List.of(LegworksItems.SHORT_LIMB.asStack(), LegworksItems.LONG_LIMB.asStack(), LegworksItems.HEAVY_LIMB.asStack());
        registration.addRecipes(LimbFittingCategory.TYPE, List.of(
                new LimbFitting(limbs, LegworksBlocks.SIMPLE_LEG_MOUNT.asStack()),
                new LimbFitting(limbs, LegworksBlocks.HYDRAULIC_LEG_MOUNT.asStack())));

        registration.addIngredientInfo(LegworksBlocks.SIMPLE_LEG_MOUNT.get(),
                t("simple"), t("facing"), t("limb"), t("goggles"));
        registration.addIngredientInfo(LegworksBlocks.HYDRAULIC_LEG_MOUNT.get(),
                t("hydraulic"), t("facing"), t("hydraulic_power"), t("hydraulic_settings"), t("hydraulic_softening"), t("goggles"));
        registration.addIngredientInfo(LegworksBlocks.LEG_CONTROLLER.get(),
                t("controller"), t("controller_linking"), t("controller_ride_height"), t("controller_gear"), t("goggles"));
        limbInfo(registration, LegworksItems.SHORT_LIMB.get());
        limbInfo(registration, LegworksItems.LONG_LIMB.get());
        limbInfo(registration, LegworksItems.HEAVY_LIMB.get());
    }

    private static void limbInfo(IRecipeRegistration registration, ItemLike item) {
        Limb limb = Limb.of(new ItemStack(item));
        List<Component> lines = new ArrayList<>();
        lines.add(t("limb"));
        if (limb != null) lines.add(Component.translatable("legworks.jei.info.limb_stats", num(limb.reach()), num(limb.stiffness()), num(limb.nominalRest())));
        lines.add(t("facing"));
        lines.add(t("limb_overreach"));
        registration.addIngredientInfo(item, lines.toArray(Component[]::new));
    }

    private static Component t(String key) {
        return Component.translatable("legworks.jei.info." + key);
    }

    private static String num(float v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
    }

    @Override
    public void onRuntimeUnavailable() {
        runtime = null;
    }
}
