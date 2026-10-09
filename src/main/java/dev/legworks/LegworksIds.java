package dev.legworks;

import net.minecraft.resources.ResourceLocation;

/**
 * Save-format identifiers. Worlds store these strings; renaming any of them breaks existing saves.
 * See docs/save-format.md.
 */
public final class LegworksIds {
    public static final String MOD_ID = "legworks";

    public static final String SIMPLE_LEG_MOUNT = "simple_leg_mount";
    public static final String HYDRAULIC_LEG_MOUNT = "hydraulic_leg_mount";
    public static final String LEG_CONTROLLER = "leg_controller";

    public static final String SHORT_LIMB = "short_limb";
    public static final String LONG_LIMB = "long_limb";
    public static final String HEAVY_LIMB = "heavy_limb";

    public static final String INCOMPLETE_HYDRAULIC_LEG_MOUNT = "incomplete_hydraulic_leg_mount";

    public static final String CONTROLLER_STATUS_SOURCE = "leg_controller_status";
    public static final String MOUNT_STATUS_SOURCE = "leg_mount_status";

    public static final String LIMB_COMPONENT = "limb";
    public static final String LINK_SELECTION_COMPONENT = "link_selection";

    public static final String NBT_LIMB = "Limb";
    public static final String NBT_REST = "Rest";
    public static final String NBT_SETTINGS = "Settings";
    public static final String NBT_SETTINGS_STIFFNESS = "Stiffness";
    public static final String NBT_SETTINGS_DAMPING = "Damping";
    public static final String NBT_CONTROLLER = "Controller";
    public static final String NBT_LINKS = "Links";
    public static final String NBT_RIDE_HEIGHT = "RideHeight";

    private LegworksIds() {
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
