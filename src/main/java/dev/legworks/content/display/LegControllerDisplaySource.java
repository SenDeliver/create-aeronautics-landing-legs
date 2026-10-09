package dev.legworks.content.display;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import dev.legworks.content.LegStatusText;
import dev.legworks.content.controller.LegControllerBlockEntity;
import net.minecraft.network.chat.MutableComponent;

import java.util.List;

/** Leg Controller status for Display Links, one fact per line: Ride Height, grounded Legs, Gear (and Overreach). */
public class LegControllerDisplaySource extends DisplaySource {
    @Override
    public List<MutableComponent> provideText(DisplayLinkContext context, DisplayTargetStats stats) {
        return context.getSourceBlockEntity() instanceof LegControllerBlockEntity be ? LegStatusText.controllerDisplay(be) : EMPTY;
    }

    @Override
    public int getPassiveRefreshTicks() {
        return 20;
    }
}
