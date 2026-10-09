package dev.legworks.client;

import dev.legworks.LegworksIds;
import dev.legworks.client.ponder.LegworksPonderPlugin;
import net.createmod.ponder.foundation.PonderIndex;
import dev.legworks.leg.LegColliders;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = LegworksIds.MOD_ID, dist = Dist.CLIENT)
public class LegworksClient {
    public LegworksClient(IEventBus modBus) {
        PonderIndex.addPlugin(new LegworksPonderPlugin());
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut e) -> LegColliders.clear(true));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post e) -> LinkOutlines.tick());
    }
}
