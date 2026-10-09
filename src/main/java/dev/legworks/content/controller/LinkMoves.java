package dev.legworks.content.controller;

import dev.legworks.content.hydraulic.HydraulicLegMountBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Keeps Links right when Sable moves blocks (assembly, split, merge, Simulated disassembly). Moves can rotate the
 * blocks while the stored offsets stay unchanged, so every moved Mount and Controller is recorded and, once the move
 * is done, each Link is rebuilt from the real new positions. A Link whose other end did not move with it is dropped.
 */
public final class LinkMoves {
    private record Move(ServerLevel level, BlockPos from, BlockPos to) {}

    private static final List<Move> MOVES = new ArrayList<>();

    private LinkMoves() {
    }

    public static synchronized void record(ServerLevel level, BlockPos from, BlockPos to) {
        MOVES.add(new Move(level, from.immutable(), to.immutable()));
    }

    public static synchronized void flush() {
        if (MOVES.isEmpty()) return;
        Map<ServerLevel, Map<BlockPos, BlockPos>> byLevel = new HashMap<>();
        for (Move m : MOVES) byLevel.computeIfAbsent(m.level, l -> new HashMap<>()).put(m.from, m.to);
        MOVES.clear();
        byLevel.forEach((level, moved) -> moved.forEach((from, to) -> {
            var be = level.getBlockEntity(to);
            if (be instanceof LegControllerBlockEntity c) {
                List<BlockPos> rebuilt = new ArrayList<>();
                for (BlockPos off : c.links()) {
                    BlockPos legTo = moved.get(from.offset(off));
                    if (legTo != null) rebuilt.add(legTo.subtract(to));
                }
                c.replaceLinks(rebuilt);
            } else if (be instanceof HydraulicLegMountBlockEntity m && m.controllerPos() != null) {
                BlockPos ctrlTo = moved.get(from.offset(m.controllerPos().subtract(to)));
                m.setControllerOffset(ctrlTo == null ? null : ctrlTo.subtract(to));
            }
        }));
    }

    public static synchronized void clear() {
        MOVES.clear();
    }
}
