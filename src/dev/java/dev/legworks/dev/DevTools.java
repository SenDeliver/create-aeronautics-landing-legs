package dev.legworks.dev;

import dev.legworks.LegworksIds;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the dev source set. Only on the classpath of dev runs, never in the release jar.
 */
@EventBusSubscriber(modid = LegworksIds.MOD_ID)
public final class DevTools {
    private DevTools() {
    }

    @SubscribeEvent
    static void onCommands(RegisterCommandsEvent event) {
        DevCommands.register(event);
        DevHydraulic.register(event.getDispatcher(), event.getBuildContext());
        DevPhase3.register(event.getDispatcher());
    }

    @SubscribeEvent
    static void onStarted(ServerStartedEvent event) {
        LoggerFactory.getLogger("legworks-dev").info("legworks dev tools active (dist={})", event.getServer().isDedicatedServer() ? "dedicated" : "integrated");
        if (Boolean.getBoolean("legworks.trace")) DevTrace.set(true);
    }

    @SubscribeEvent
    static void onServerTick(ServerTickEvent.Post event) {
        DevCommands.serverTick();
        DevHydraulic.serverTick();
    }

    @SubscribeEvent
    static void onPrePhysics(ForgeSablePrePhysicsTickEvent event) {
        DevCommands.tickLoads(event.getTimeStep());
        DevHydraulic.tickPhysics(event.getTimeStep());
    }

    @SubscribeEvent
    static void onStopped(ServerStoppedEvent event) {
        DevCommands.clear();
        DevHydraulic.clear();
    }
}
