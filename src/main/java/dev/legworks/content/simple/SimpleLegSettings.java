package dev.legworks.content.simple;

import dev.legworks.config.LegworksConfig;
import dev.legworks.leg.LegSettings;
import dev.legworks.limb.Limb;

final class SimpleLegSettings implements LegSettings {
    static final SimpleLegSettings INSTANCE = new SimpleLegSettings();

    private SimpleLegSettings() {
    }

    @Override
    public boolean active() {
        return true;
    }

    @Override
    public double stiffnessScale() {
        return 1.0;
    }

    @Override
    public double dampingRatio() {
        return LegworksConfig.value(LegworksConfig.SIMPLE_DAMPING_RATIO);
    }

    @Override
    public double rideTarget(Limb limb) {
        return limb.nominalRest();
    }
}
