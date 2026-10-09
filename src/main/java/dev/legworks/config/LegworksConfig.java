package dev.legworks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class LegworksConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue REST_SAG;
    public static final ModConfigSpec.DoubleValue SIMPLE_DAMPING_RATIO;
    public static final ModConfigSpec.DoubleValue GRIP_FRICTION;
    public static final ModConfigSpec.DoubleValue GRIP_OMEGA;

    public static final ModConfigSpec.DoubleValue LEVEL_GAIN;
    public static final ModConfigSpec.DoubleValue LOAD_GAIN;
    public static final ModConfigSpec.DoubleValue RIDE_GAIN;
    public static final ModConfigSpec.DoubleValue OFFLOAD_GAIN;
    public static final ModConfigSpec.DoubleValue MAX_REST_RATE;
    public static final ModConfigSpec.DoubleValue CROUCH_RATE;
    public static final ModConfigSpec.DoubleValue ANTI_ROLL;
    public static final ModConfigSpec.DoubleValue ANTI_ROLL_DAMP;

    public static final ModConfigSpec.IntValue MAX_LEGS_PER_CONTROLLER;
    public static final ModConfigSpec.IntValue LINK_RANGE;
    public static final ModConfigSpec.DoubleValue HYDRAULIC_STRESS;
    public static final ModConfigSpec.DoubleValue STROKE_RPM_SECONDS;
    public static final ModConfigSpec.DoubleValue SOFTEN_RATE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();

        b.comment("Leg springs and Foot grip").push("leg");
        REST_SAG = b.comment("How far a Limb with Stiffness x1 compresses under its full share of the Ship's weight, in blocks.")
                .defineInRange("restSag", 0.5, 0.05, 2.0);
        SIMPLE_DAMPING_RATIO = b.comment("Damping ratio of Simple Legs (1 = critical).")
                .defineInRange("simpleDampingRatio", 0.8, 0.05, 3.0);
        GRIP_FRICTION = b.comment("Foot grip friction coefficient (tangential force cap = friction x normal force).")
                .defineInRange("gripFriction", 1.2, 0.0, 5.0);
        GRIP_OMEGA = b.comment("Foot grip anchor spring frequency, rad/s.")
                .defineInRange("gripOmega", 8.0, 0.0, 50.0);
        b.pop();

        b.comment("Leveling (runs once per Ship per tick)").push("leveling");
        LEVEL_GAIN = b.comment("Slow length correction: rest rate per block of tilt displacement.")
                .defineInRange("levelGain", 0.4, 0.0, 5.0);
        LOAD_GAIN = b.comment("Load term gain (evens the twist between diagonal Legs).")
                .defineInRange("loadGain", 1.0, 0.0, 5.0);
        RIDE_GAIN = b.comment("Ride height term gain.")
                .defineInRange("rideGain", 0.3, 0.0, 5.0);
        OFFLOAD_GAIN = b.comment("Moves anti-roll work into leg lengths so flat ground ends level.")
                .defineInRange("offloadGain", 1.0, 0.0, 5.0);
        MAX_REST_RATE = b.comment("Maximum rest length change, blocks per second.")
                .defineInRange("maxRestRate", 0.5, 0.05, 5.0);
        CROUCH_RATE = b.comment("Crouch speed of grounded Legs while a Foot is searching for Ground, blocks per second.")
                .defineInRange("crouchRate", 3.0, 0.0, 20.0);
        ANTI_ROLL = b.comment("Anti-roll stiffness, in units of Ship weight x 3 blocks per radian.")
                .defineInRange("antiRoll", 6.0, 0.0, 50.0);
        ANTI_ROLL_DAMP = b.comment("Anti-roll damping time constant, seconds.")
                .defineInRange("antiRollDamp", 0.04, 0.0, 1.0);
        b.pop();

        b.comment("Hydraulic Legs and Leg Controllers").push("hydraulic");
        MAX_LEGS_PER_CONTROLLER = b.comment("Most Legs one Leg Controller can Link.")
                .defineInRange("maxLegsPerController", 8, 1, 64);
        LINK_RANGE = b.comment("Longest Link between a Leg Controller and a Leg, in blocks.")
                .defineInRange("linkRange", 16, 1, 64);
        HYDRAULIC_STRESS = b.comment("Stress impact of a Hydraulic Leg Mount, SU per RPM. Applies after a restart.")
                .defineInRange("hydraulicStressImpact", 4.0, 0.0, 64.0);
        STROKE_RPM_SECONDS = b.comment("Seconds a full-Reach stroke (e.g. a Gear Up fold) takes at 1 RPM; at 16 RPM it takes this / 16.")
                .defineInRange("strokeRpmSeconds", 64.0, 1.0, 10000.0);
        SOFTEN_RATE = b.comment("How fast Softening follows power loss or a top redstone change, full range per second.")
                .defineInRange("softenRate", 2.0, 0.1, 100.0);
        b.pop();

        SPEC = b.build();
    }

    private LegworksConfig() {
    }

    /** The value, or its default where the server config is not loaded (e.g. Ponder on the title screen). */
    public static double value(ModConfigSpec.DoubleValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }

    public static int value(ModConfigSpec.IntValue v) {
        return SPEC.isLoaded() ? v.get() : v.getDefault();
    }
}
