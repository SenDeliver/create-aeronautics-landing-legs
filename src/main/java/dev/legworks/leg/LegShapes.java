package dev.legworks.leg;

import dev.ryanhcode.sable.api.physics.collider.SableCollisionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Entity collision shape of a Leg Mount: the block plus the drawn strut and Foot pad at the current Reach.
 * Sable's physics collider always gets a full block, so the Ship's rigid body never collides through the leg.
 */
public final class LegShapes {
    private static final double STRUT_HALF = 3 / 16.0;
    private static final double PAD_HALF = 5 / 16.0;
    private static final double PAD_THICK = 2.5 / 16.0;
    private static final ConcurrentHashMap<Integer, VoxelShape> CACHE = new ConcurrentHashMap<>();

    private LegShapes() {
    }

    public static VoxelShape collisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (context instanceof SableCollisionContext) return Shapes.block();
        if (!(level.getBlockEntity(pos) instanceof LegHost host) || host.limb() == null || !host.leg().isOnShip()) return Shapes.block();
        int reach16 = host.leg().collisionReach16();
        if (reach16 <= 0) return Shapes.block();
        Direction f = state.getValue(BlockStateProperties.FACING);
        return CACHE.computeIfAbsent(f.ordinal() * 1000 + reach16, k -> legShape(f, reach16 / 16.0));
    }

    private static VoxelShape legShape(Direction f, double reach) {
        VoxelShape strut = Shapes.create(legBox(f, 0, reach, STRUT_HALF));
        VoxelShape pad = Shapes.create(legBox(f, Math.max(0, reach - PAD_THICK), reach, PAD_HALF));
        return Shapes.or(Shapes.block(), strut, pad);
    }

    private static AABB legBox(Direction f, double from, double to, double half) {
        double[] min = {0.5 - half, 0.5 - half, 0.5 - half};
        double[] max = {0.5 + half, 0.5 + half, 0.5 + half};
        int axis = f.getAxis().ordinal();
        if (f.getAxisDirection() == Direction.AxisDirection.POSITIVE) {
            min[axis] = 1 + from;
            max[axis] = 1 + to;
        } else {
            min[axis] = -to;
            max[axis] = -from;
        }
        return new AABB(min[0], min[1], min[2], max[0], max[1], max[2]);
    }
}
