package dev.legworks.mixin;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkBlockEntity;
import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sable's own Display Link listener re-targets a moved link at its target's old position, which is wrong when the
 * target (e.g. a Display Board) moved with it: the board freezes. After the move, links whose target moved too are
 * pointed at the target's new position.
 */
@Mixin(value = SubLevelAssemblyHelper.class, remap = false)
public class SubLevelAssemblyDisplayLinkMixin {
    private static final ThreadLocal<Deque<Map<BlockPos, BlockPos>>> LEGWORKS$TARGETS = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "moveBlocks", at = @At("HEAD"))
    private static void legworks$rememberTargets(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        Map<BlockPos, BlockPos> targets = new HashMap<>();
        for (BlockPos pos : blocks) {
            if (level.getBlockEntity(pos) instanceof DisplayLinkBlockEntity link) targets.put(pos.immutable(), link.getTargetPosition().immutable());
        }
        LEGWORKS$TARGETS.get().push(targets);
    }

    @Inject(method = "moveBlocks", at = @At("TAIL"))
    private static void legworks$fixTargets(ServerLevel level, SubLevelAssemblyHelper.AssemblyTransform transform, Iterable<BlockPos> blocks, CallbackInfo ci) {
        Deque<Map<BlockPos, BlockPos>> stack = LEGWORKS$TARGETS.get();
        if (stack.isEmpty()) return;
        Map<BlockPos, BlockPos> targets = stack.pop();
        if (targets.isEmpty()) return;
        Set<BlockPos> moved = new HashSet<>();
        for (BlockPos pos : blocks) moved.add(pos.immutable());
        ServerLevel result = transform.getLevel();
        targets.forEach((from, target) -> {
            if (!moved.contains(target)) return;
            if (result.getBlockEntity(transform.apply(from)) instanceof DisplayLinkBlockEntity link) {
                link.target(transform.apply(target));
                link.notifyUpdate();
            }
        });
    }
}
