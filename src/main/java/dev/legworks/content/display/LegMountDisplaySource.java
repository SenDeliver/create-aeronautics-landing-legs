package dev.legworks.content.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.legworks.content.LegStatusText;
import dev.legworks.leg.LegHost;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/** Leg Mount status for Display Links: Reach, load (and Overreach), one per line. */
public class LegMountDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        return context.getSourceBlockEntity() instanceof LegHost host ? LegStatusText.mountDisplay(host) : EMPTY;
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
