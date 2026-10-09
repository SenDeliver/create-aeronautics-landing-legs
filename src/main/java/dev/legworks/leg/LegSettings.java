package dev.legworks.leg;

import dev.legworks.limb.Limb;

/**
 * Per-Mount tuning the shared Leg physics reads every tick. Simple Leg Mounts use fixed values;
 * Hydraulic Leg Mounts return their value boxes, Softening, fold and their Controller's Ride Height.
 */
public interface LegSettings {
    /** Whether the Leg pushes at all (e.g. false for an unlinked or fully folded Hydraulic Leg). */
    boolean active();

    /** Mount Stiffness setting; multiplies the Limb's base Stiffness. Softening is applied separately. */
    double stiffnessScale();

    /** 0 = none, 1 = full Softening (no spring, damping only). */
    default double softening() {
        return 0;
    }

    double dampingRatio();

    /** Rest length Leveling aims for on average (Ride Height), clamped to the Leg's range by the caller. */
    double rideTarget(Limb limb);

    /** 0 = Gear Down, 1 = fully folded (Gear Up). Shrinks the Leg's maximum Reach. */
    default double fold() {
        return 0;
    }

    /** Fastest rest length change the Mount can drive, blocks per second. */
    default double strokeRate(Limb limb) {
        return Double.POSITIVE_INFINITY;
    }
}
