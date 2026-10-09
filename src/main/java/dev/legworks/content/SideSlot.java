package dev.legworks.content;

import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiPredicate;

/** A value box (the face follows the looked-at side) on the block faces the predicate allows, shifted sideways by {@code x} (in 1/16 block) so two boxes share a face. */
public class SideSlot extends ValueBoxTransform.Sided {
    private final BiPredicate<BlockState, Direction> sides;
    private final double x;

    public SideSlot(BiPredicate<BlockState, Direction> sides, double x, float scale) {
        this.sides = sides;
        this.x = x;
        this.scale = scale;
    }

    @Override
    protected Vec3 getSouthLocation() {
        return new Vec3(x / 16.0, 0.5, 15.5 / 16.0);
    }

    @Override
    protected boolean isSideActive(BlockState state, Direction direction) {
        return sides.test(state, direction);
    }

    @Override
    public boolean testHit(LevelAccessor level, BlockPos pos, BlockState state, Vec3 localHit) {
        fromSide(nearestFace(localHit));
        return super.testHit(level, pos, state, localHit);
    }

    private static Direction nearestFace(Vec3 p) {
        double dx = p.x - 0.5, dy = p.y - 0.5, dz = p.z - 0.5;
        double ax = Math.abs(dx), ay = Math.abs(dy), az = Math.abs(dz);
        if (ax >= ay && ax >= az) return dx > 0 ? Direction.EAST : Direction.WEST;
        if (ay >= az) return dy > 0 ? Direction.UP : Direction.DOWN;
        return dz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    @Override
    public float getScale() {
        return scale;
    }
}
