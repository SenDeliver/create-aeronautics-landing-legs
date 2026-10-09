package dev.legworks.content.controller;

import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.block.IBE;
import dev.legworks.registry.LegworksBlockEntityTypes;
import dev.legworks.content.LegStatusText;
import net.minecraft.ChatFormatting;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;

public class LegControllerBlock extends Block implements IBE<LegControllerBlockEntity>, IWrenchable, BlockSubLevelAssemblyListener {
    public LegControllerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (!level.isClientSide) withBlockEntityDo(level, pos, LegControllerBlockEntity::updateSignal);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof LegControllerItem) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LegControllerBlockEntity be)) return InteractionResult.PASS;
        if (!level.isClientSide) player.displayClientMessage(status(be), true);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static Component status(LegControllerBlockEntity be) {
        var msg = Component.translatable("legworks.controller.status", be.linkCount(), Linking.maxLegs(),
                LegStatusText.rideHeight(be.rideHeightSetting()),
                Component.translatable(be.isGearUp() ? "legworks.controller.gear_up" : "legworks.controller.gear_down"));
        if (be.hasConflict()) msg.append(" | ").append(Component.translatable("legworks.controller.conflict").withStyle(ChatFormatting.GOLD));
        return msg;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && !movedByPiston && level.getBlockEntity(pos) instanceof LegControllerBlockEntity be) {
            Linking.onControllerRemoved(level, pos, new ArrayList<>(be.links()));
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        LinkMoves.record(newLevel, oldPos, newPos);
    }

    @Override
    public Class<LegControllerBlockEntity> getBlockEntityClass() {
        return LegControllerBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends LegControllerBlockEntity> getBlockEntityType() {
        return LegworksBlockEntityTypes.LEG_CONTROLLER.get();
    }
}
