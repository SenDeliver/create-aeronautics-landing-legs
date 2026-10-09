package dev.legworks.content.hydraulic;

import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.legworks.content.controller.Linking;
import dev.legworks.leg.LegMountInteractions;
import dev.legworks.leg.LegShapes;
import dev.legworks.registry.LegworksBlockEntityTypes;
import dev.ryanhcode.sable.api.block.BlockSubLevelAssemblyListener;
import dev.ryanhcode.sable.api.block.BlockSubLevelCollisionShape;
import dev.legworks.content.controller.LinkMoves;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Hydraulic Leg Mount: a Leg Mount driven by rotation through a shaft across its sides. The Limb extends toward FACING;
 * the face opposite FACING is its top, where a redstone signal Softens the Leg.
 */
public class HydraulicLegMountBlock extends DirectionalAxisKineticBlock implements IBE<HydraulicLegMountBlockEntity>, BlockSubLevelCollisionShape, BlockSubLevelAssemblyListener {
    public HydraulicLegMountBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.DOWN));
    }

    @Override
    protected Direction getFacingForPlacement(BlockPlaceContext context) {
        Direction facing = context.getClickedFace();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) facing = facing.getOpposite();
        return facing;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        return LegMountInteractions.useItemOn(stack, level, pos, player, hand);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        InteractionResult result = LegMountInteractions.useWithoutItem(level, pos, player);
        if (!level.isClientSide && !player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof HydraulicLegMountBlockEntity m
                && m.limb() != null && m.controllerPos() == null) {
            player.displayClientMessage(Component.translatable("legworks.message.not_linked"), true);
        }
        return result;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        super.neighborChanged(state, level, pos, block, fromPos, moving);
        if (!level.isClientSide) withBlockEntityDo(level, pos, HydraulicLegMountBlockEntity::updateTopSignal);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && !movedByPiston) {
            var be = level.getBlockEntity(pos);
            LegMountInteractions.onMountRemoved(be);
            if (be instanceof HydraulicLegMountBlockEntity m) Linking.onMountRemoved(level, pos, m.controllerPos());
        }
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    public void afterMove(ServerLevel oldLevel, ServerLevel newLevel, BlockState state, BlockPos oldPos, BlockPos newPos) {
        LinkMoves.record(newLevel, oldPos, newPos);
    }

    @Override
    public VoxelShape getSubLevelCollisionShape(BlockGetter level, BlockState state) {
        return Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return LegShapes.collisionShape(state, level, pos, context);
    }

    @Override
    public Class<HydraulicLegMountBlockEntity> getBlockEntityClass() {
        return HydraulicLegMountBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends HydraulicLegMountBlockEntity> getBlockEntityType() {
        return LegworksBlockEntityTypes.HYDRAULIC_LEG_MOUNT.get();
    }
}
