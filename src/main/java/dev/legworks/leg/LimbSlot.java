package dev.legworks.leg;

import dev.legworks.LegworksIds;
import dev.legworks.limb.Limb;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The one Limb fitted to a Leg Mount, saved under {@value LegworksIds#NBT_LIMB} as a full item stack.
 */
public final class LimbSlot {
    private ItemStack stack = ItemStack.EMPTY;
    private Limb limb;

    public ItemStack stack() {
        return stack;
    }

    @Nullable
    public Limb limb() {
        return limb;
    }

    public boolean isEmpty() {
        return stack.isEmpty();
    }

    public void set(ItemStack newStack) {
        stack = newStack.isEmpty() ? ItemStack.EMPTY : newStack.copyWithCount(1);
        limb = Limb.of(stack);
    }

    public ItemStack take() {
        ItemStack out = stack;
        set(ItemStack.EMPTY);
        return out;
    }

    public void write(CompoundTag tag, HolderLookup.Provider registries) {
        if (!stack.isEmpty()) tag.put(LegworksIds.NBT_LIMB, stack.save(registries));
    }

    /** @return true if the fitted Limb changed */
    public boolean read(CompoundTag tag, HolderLookup.Provider registries) {
        ItemStack loaded = tag.contains(LegworksIds.NBT_LIMB) ? ItemStack.parseOptional(registries, tag.getCompound(LegworksIds.NBT_LIMB)) : ItemStack.EMPTY;
        if (ItemStack.isSameItemSameComponents(loaded, stack)) return false;
        set(loaded);
        return true;
    }
}
