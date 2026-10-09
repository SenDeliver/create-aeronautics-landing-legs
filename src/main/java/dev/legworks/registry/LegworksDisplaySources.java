package dev.legworks.registry;

import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.legworks.LegworksIds;
import dev.legworks.content.display.LegControllerDisplaySource;
import dev.legworks.content.display.LegMountDisplaySource;

import static dev.legworks.Legworks.REGISTRATE;

public final class LegworksDisplaySources {
    public static final RegistryEntry<DisplaySource, LegControllerDisplaySource> CONTROLLER_STATUS =
            REGISTRATE.displaySource(LegworksIds.CONTROLLER_STATUS_SOURCE, LegControllerDisplaySource::new).register();
    public static final RegistryEntry<DisplaySource, LegMountDisplaySource> MOUNT_STATUS =
            REGISTRATE.displaySource(LegworksIds.MOUNT_STATUS_SOURCE, LegMountDisplaySource::new).register();

    private LegworksDisplaySources() {
    }

    public static void init() {
    }
}
