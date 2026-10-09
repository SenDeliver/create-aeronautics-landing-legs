package dev.legworks.leg;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Positions of Leg Mounts with a fitted Limb, per side, so the entity-collision mixin can find drawn legs
 * that reach outside Sable's normal scan box.
 */
public final class LegColliders {
    private static final Long2ObjectOpenHashMap<Set<BlockPos>> CLIENT = new Long2ObjectOpenHashMap<>();
    private static final Long2ObjectOpenHashMap<Set<BlockPos>> SERVER = new Long2ObjectOpenHashMap<>();
    private static volatile double maxReach = 1;

    private LegColliders() {
    }

    private static Long2ObjectOpenHashMap<Set<BlockPos>> map(boolean client) {
        return client ? CLIENT : SERVER;
    }

    public static synchronized void add(boolean client, BlockPos pos, double reach) {
        map(client).computeIfAbsent(ChunkPos.asLong(pos), k -> new HashSet<>()).add(pos.immutable());
        if (reach > maxReach) maxReach = reach;
    }

    public static synchronized void remove(boolean client, BlockPos pos) {
        long key = ChunkPos.asLong(pos);
        Set<BlockPos> set = map(client).get(key);
        if (set == null) return;
        set.remove(pos);
        if (set.isEmpty()) map(client).remove(key);
    }

    public static synchronized void clear(boolean client) {
        map(client).clear();
    }

    public static synchronized boolean any(boolean client) {
        return !map(client).isEmpty();
    }

    public static double maxReach() {
        return maxReach;
    }

    public static int margin() {
        return (int) Math.ceil(maxReach) + 1;
    }

    public static synchronized List<BlockPos> near(boolean client, BlockPos min, BlockPos max, int margin) {
        Long2ObjectOpenHashMap<Set<BlockPos>> m = map(client);
        if (m.isEmpty()) return List.of();
        List<BlockPos> out = new ArrayList<>();
        int x0 = min.getX() - margin, y0 = min.getY() - margin, z0 = min.getZ() - margin;
        int x1 = max.getX() + margin, y1 = max.getY() + margin, z1 = max.getZ() + margin;
        for (int cx = x0 >> 4; cx <= x1 >> 4; cx++) {
            for (int cz = z0 >> 4; cz <= z1 >> 4; cz++) {
                Set<BlockPos> set = m.get(ChunkPos.asLong(cx, cz));
                if (set == null) continue;
                for (BlockPos p : set) {
                    boolean inScan = p.getX() >= min.getX() && p.getX() <= max.getX() && p.getY() >= min.getY() && p.getY() <= max.getY() && p.getZ() >= min.getZ() && p.getZ() <= max.getZ();
                    boolean inMargin = p.getX() >= x0 && p.getX() <= x1 && p.getY() >= y0 && p.getY() <= y1 && p.getZ() >= z0 && p.getZ() <= z1;
                    if (inMargin && !inScan) out.add(p);
                }
            }
        }
        return out;
    }
}
