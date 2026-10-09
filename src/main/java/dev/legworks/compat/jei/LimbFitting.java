package dev.legworks.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/** A JEI "recipe" showing that any Limb fits this Leg Mount. */
public record LimbFitting(List<ItemStack> limbs, ItemStack mount) {
}
