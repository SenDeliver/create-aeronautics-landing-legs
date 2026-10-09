package dev.legworks.content.controller;

import dev.legworks.content.hydraulic.HydraulicLegMountBlockEntity;
import dev.legworks.leg.LegHost;
import dev.legworks.registry.LegworksDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The Leg Controller item links Legs. Sneak-click a placed Controller to select it, then click Hydraulic Legs to
 * link or unlink them. Without a selection, clicked Legs are remembered and linked when this Controller is placed.
 */
public class LegControllerItem extends BlockItem {
    public LegControllerItem(Block block, Properties properties) {
        super(block, properties);
    }

    public static LinkSelection selection(ItemStack stack) {
        LinkSelection s = stack.get(LegworksDataComponents.LINK_SELECTION.get());
        return s == null ? LinkSelection.EMPTY : s;
    }

    private static void setSelection(ItemStack stack, LinkSelection s) {
        if (s.isEmpty()) stack.remove(LegworksDataComponents.LINK_SELECTION.get());
        else stack.set(LegworksDataComponents.LINK_SELECTION.get(), s);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        BlockEntity target = level.getBlockEntity(pos);
        LinkSelection sel = selection(stack);

        if (target instanceof LegControllerBlockEntity controller && player != null && player.isShiftKeyDown()) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (sel.controller().filter(pos::equals).isPresent()) {
                setSelection(stack, LinkSelection.EMPTY);
                tell(player, Component.translatable("legworks.link.deselected"), ChatFormatting.WHITE);
            } else {
                setSelection(stack, LinkSelection.of(pos));
                tell(player, Component.translatable("legworks.link.selected", controller.linkCount(), Linking.maxLegs()), ChatFormatting.GREEN);
            }
            return InteractionResult.SUCCESS;
        }

        if (target instanceof HydraulicLegMountBlockEntity) {
            if (level.isClientSide) return InteractionResult.SUCCESS;
            if (sel.controller().isPresent()) {
                Linking.Outcome out = Linking.toggle(level, sel.controller().get(), pos);
                if (out.result() == Linking.Result.NO_CONTROLLER) setSelection(stack, LinkSelection.EMPTY);
                tell(player, out.message(), out.result().ok() ? ChatFormatting.GREEN : ChatFormatting.RED);
                if (out.result().ok()) level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.7f, out.result() == Linking.Result.UNLINKED ? 0.8f : 1.2f);
                return InteractionResult.SUCCESS;
            }
            if (!sel.pending().contains(pos) && sel.pending().size() >= Linking.maxLegs()) {
                tell(player, Component.translatable("legworks.link.pending_full", Linking.maxLegs()), ChatFormatting.RED);
                return InteractionResult.SUCCESS;
            }
            LinkSelection next = sel.togglePending(pos);
            setSelection(stack, next);
            tell(player, Component.translatable("legworks.link.pending", next.pending().size()), ChatFormatting.GREEN);
            return InteractionResult.SUCCESS;
        }

        if (target instanceof LegHost) {
            if (!level.isClientSide) tell(player, Component.translatable("legworks.link.not_a_leg"), ChatFormatting.RED);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPlaceContext place = new BlockPlaceContext(ctx);
        BlockPos placedAt = place.getClickedPos();
        InteractionResult result = super.useOn(ctx);
        if (level.isClientSide || !result.consumesAction() || !(level.getBlockEntity(placedAt) instanceof LegControllerBlockEntity placed)) return result;
        if (!stack.isEmpty()) setSelection(stack, LinkSelection.EMPTY);
        if (sel.pending().isEmpty()) return result;
        int skipped = 0;
        for (BlockPos leg : sel.pending()) {
            Linking.Outcome out = Linking.toggle(level, placedAt, leg);
            if (!out.result().ok()) skipped++;
        }
        MutableComponent msg = Component.translatable("legworks.link.placed", placed.linkCount(), Linking.maxLegs());
        if (skipped > 0) msg.append(". ").append(Component.translatable("legworks.link.placed_skipped", skipped));
        tell(player, msg, skipped > 0 ? ChatFormatting.GOLD : ChatFormatting.GREEN);
        return result;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !selection(stack).isEmpty()) {
            if (!level.isClientSide) {
                setSelection(stack, LinkSelection.EMPTY);
                tell(player, Component.translatable("legworks.link.cleared"), ChatFormatting.WHITE);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return super.use(level, player, hand);
    }

    private static void tell(Player player, Component msg, ChatFormatting color) {
        if (player != null) player.displayClientMessage(msg.copy().withStyle(color), true);
    }
}
