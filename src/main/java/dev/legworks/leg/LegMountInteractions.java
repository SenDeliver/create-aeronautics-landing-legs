package dev.legworks.leg;

import dev.legworks.limb.Limb;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Fitting and removing Limbs, shared by every Leg Mount block.
 */
public final class LegMountInteractions {
    private LegMountInteractions() {
    }

    public static ItemInteractionResult useItemOn(ItemStack held, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!Limb.isLimb(held)) return held.isEmpty() ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!(level.getBlockEntity(pos) instanceof LegHost host)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        LimbSlot slot = host.limbSlot();
        if (ItemStack.isSameItemSameComponents(slot.stack(), held)) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;

        ItemStack old = slot.take();
        slot.set(held);
        if (!player.getAbilities().instabuild) held.shrink(1);
        if (!old.isEmpty()) giveOrDrop(player, old);
        host.onLimbSlotChanged();
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON.value(), SoundSource.BLOCKS, 0.8f, 1.0f);
        player.displayClientMessage(Component.translatable("legworks.message.limb_fitted", slot.stack().getHoverName()), true);
        return ItemInteractionResult.SUCCESS;
    }

    public static InteractionResult useWithoutItem(Level level, BlockPos pos, Player player) {
        if (!(level.getBlockEntity(pos) instanceof LegHost host)) return InteractionResult.PASS;
        LimbSlot slot = host.limbSlot();
        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                player.displayClientMessage(slot.isEmpty()
                        ? Component.translatable("legworks.message.no_limb")
                        : Component.translatable("legworks.message.limb_status", slot.stack().getHoverName()), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (slot.isEmpty()) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack removed = slot.take();
        giveOrDrop(player, removed);
        host.onLimbSlotChanged();
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_GENERIC.value(), SoundSource.BLOCKS, 0.8f, 0.8f);
        return InteractionResult.SUCCESS;
    }

    /** The Mount is broken or replaced by another block: drop its Limb. */
    public static void onMountRemoved(BlockEntity be) {
        if (!(be instanceof LegHost host) || be.getLevel() == null) return;
        host.leg().noteLost();
        ItemStack stack = host.limbSlot().take();
        if (stack.isEmpty()) return;
        BlockPos p = be.getBlockPos();
        Containers.dropItemStack(be.getLevel(), p.getX(), p.getY(), p.getZ(), stack);
    }

    private static void giveOrDrop(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }
}
