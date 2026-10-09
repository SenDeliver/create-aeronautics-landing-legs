package dev.legworks.mixin;

import com.google.common.collect.Iterables;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.legworks.leg.LegColliders;
import dev.ryanhcode.sable.ActiveSableCompanion;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.sublevel.entity_collision.SubLevelEntityCollision", remap = false)
public class SubLevelEntityCollisionMixin {
    @WrapOperation(method = "collide", at = @At(value = "INVOKE", target = "Lnet/minecraft/core/BlockPos;betweenClosed(Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)Ljava/lang/Iterable;"))
    private static Iterable<BlockPos> legworks$addLegMounts(BlockPos min, BlockPos max, Operation<Iterable<BlockPos>> original, @Local(argsOnly = true) Entity entity) {
        BlockPos lo = min.immutable();
        BlockPos hi = max.immutable();
        Iterable<BlockPos> scan = original.call(min, max);
        List<BlockPos> legs = LegColliders.near(entity.level().isClientSide, lo, hi, LegColliders.margin());
        return legs.isEmpty() ? scan : Iterables.concat(scan, legs);
    }

    @WrapOperation(method = "collide", at = @At(value = "INVOKE", target = "Ldev/ryanhcode/sable/ActiveSableCompanion;getAllIntersecting(Lnet/minecraft/world/level/Level;Ldev/ryanhcode/sable/companion/math/BoundingBox3dc;)Ljava/lang/Iterable;"))
    private static Iterable<SubLevel> legworks$widenForLegs(ActiveSableCompanion helper, Level level, BoundingBox3dc bounds, Operation<Iterable<SubLevel>> original) {
        if (!LegColliders.any(level.isClientSide)) return original.call(helper, level, bounds);
        return original.call(helper, level, new BoundingBox3d(bounds).expand(LegColliders.maxReach()));
    }
}
