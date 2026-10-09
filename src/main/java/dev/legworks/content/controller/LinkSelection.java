package dev.legworks.content.controller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What a held Leg Controller item is Linking: a placed Controller (sneak-click it), or Legs clicked before the
 * Controller is placed. Positions are absolute (plot coordinates on a Ship) and only valid for a short while.
 */
public record LinkSelection(Optional<BlockPos> controller, List<BlockPos> pending) {
    public static final LinkSelection EMPTY = new LinkSelection(Optional.empty(), List.of());

    public static final Codec<LinkSelection> CODEC = RecordCodecBuilder.create(i -> i.group(
            BlockPos.CODEC.optionalFieldOf("controller").forGetter(LinkSelection::controller),
            BlockPos.CODEC.listOf().optionalFieldOf("pending", List.of()).forGetter(LinkSelection::pending)
    ).apply(i, LinkSelection::new));

    public static final StreamCodec<ByteBuf, LinkSelection> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.optional(BlockPos.STREAM_CODEC), LinkSelection::controller,
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), LinkSelection::pending,
            LinkSelection::new);

    public LinkSelection {
        pending = List.copyOf(pending);
    }

    public static LinkSelection of(BlockPos controller) {
        return new LinkSelection(Optional.of(controller.immutable()), List.of());
    }

    public LinkSelection togglePending(BlockPos leg) {
        List<BlockPos> list = new ArrayList<>(pending);
        if (!list.remove(leg)) list.add(leg.immutable());
        return new LinkSelection(Optional.empty(), list);
    }

    public boolean isEmpty() {
        return controller.isEmpty() && pending.isEmpty();
    }
}
