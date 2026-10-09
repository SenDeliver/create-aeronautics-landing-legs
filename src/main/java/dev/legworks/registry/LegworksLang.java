package dev.legworks.registry;

import dev.legworks.LegworksIds;

import static dev.legworks.Legworks.REGISTRATE;

public final class LegworksLang {
    private LegworksLang() {
    }

    private static void tooltip(String kind, String id, String summary, String... conditionBehaviour) {
        String base = kind + "." + LegworksIds.MOD_ID + "." + id + ".tooltip";
        REGISTRATE.addRawLang(base + ".summary", summary);
        for (int i = 0; i + 1 < conditionBehaviour.length; i += 2) {
            REGISTRATE.addRawLang(base + ".condition" + (i / 2 + 1), conditionBehaviour[i]);
            REGISTRATE.addRawLang(base + ".behaviour" + (i / 2 + 1), conditionBehaviour[i + 1]);
        }
    }

    public static void init() {
        REGISTRATE.addRawLang("itemGroup.legworks", "Landing Legs");
        REGISTRATE.addRawLang("legworks.message.limb_fitted", "Fitted %s");
        REGISTRATE.addRawLang("legworks.message.limb_status", "%s fitted (sneak with an empty hand to remove)");
        REGISTRATE.addRawLang("legworks.message.no_limb", "No Limb fitted");
        REGISTRATE.addRawLang("legworks.message.not_linked", "Not linked (click it with a Leg Controller)");
        REGISTRATE.addRawLang("legworks.link.linked", "Leg linked (%s/%s)");
        REGISTRATE.addRawLang("legworks.link.unlinked", "Leg unlinked (%s/%s)");
        REGISTRATE.addRawLang("legworks.link.relinked", "Leg moved to this Controller (%s/%s)");
        REGISTRATE.addRawLang("legworks.link.too_far", "Too far from the Leg Controller");
        REGISTRATE.addRawLang("legworks.link.other_ship", "The Leg and the Leg Controller must be on the same Ship");
        REGISTRATE.addRawLang("legworks.link.full", "This Leg Controller already has %s/%s Legs");
        REGISTRATE.addRawLang("legworks.link.not_a_leg", "Only Hydraulic Legs can be linked");
        REGISTRATE.addRawLang("legworks.link.no_controller", "The selected Leg Controller is gone");
        REGISTRATE.addRawLang("legworks.link.selected", "Leg Controller selected (%s/%s Legs). Click Hydraulic Legs to link or unlink them");
        REGISTRATE.addRawLang("legworks.link.deselected", "Leg Controller deselected");
        REGISTRATE.addRawLang("legworks.link.pending", "%s Legs will be linked when you place this Leg Controller");
        REGISTRATE.addRawLang("legworks.link.pending_full", "At most %s Legs per Leg Controller");
        REGISTRATE.addRawLang("legworks.link.placed", "Leg Controller placed with %s/%s Legs linked");
        REGISTRATE.addRawLang("legworks.link.placed_skipped", "%s Legs could not be linked (too far, other Ship or gone)");
        REGISTRATE.addRawLang("legworks.link.cleared", "Link selection cleared");
        REGISTRATE.addRawLang("legworks.controller.status", "Leg Controller: %s/%s Legs, Ride Height %s, %s");
        REGISTRATE.addRawLang("legworks.controller.gear_up", "Gear Up");
        REGISTRATE.addRawLang("legworks.controller.gear_down", "Gear Down");
        REGISTRATE.addRawLang("legworks.controller.conflict", "Conflicting Ride Height");
        REGISTRATE.addRawLang("legworks.ride_height.auto", "Auto");
        REGISTRATE.addRawLang("legworks.value.ride_height", "Ride Height");
        REGISTRATE.addRawLang("legworks.value.stiffness", "Stiffness");
        REGISTRATE.addRawLang("legworks.value.damping", "Damping");
        REGISTRATE.addRawLang("legworks.value.ride_height.board", "Ride Height (blocks, 0 = Auto)");
        REGISTRATE.addRawLang("legworks.value.stiffness.board", "Stiffness (percent of the Limb's)");
        REGISTRATE.addRawLang("legworks.value.damping.board", "Damping (1.0 = no bounce)");
        REGISTRATE.addRawLang("legworks.unit.blocks", "%s blocks");
        REGISTRATE.addRawLang("legworks.unit.percent", "%s%%");

        REGISTRATE.addRawLang("legworks.goggles.simple_leg", "Simple Leg:");
        REGISTRATE.addRawLang("legworks.goggles.hydraulic_leg", "Hydraulic Leg:");
        REGISTRATE.addRawLang("legworks.goggles.leg_controller", "Leg Controller:");
        REGISTRATE.addRawLang("legworks.status.limb", "Limb: %s");
        REGISTRATE.addRawLang("legworks.status.reach", "Reach: %s / %s blocks");
        REGISTRATE.addRawLang("legworks.status.load", "Load: %s of the Ship's weight");
        REGISTRATE.addRawLang("legworks.status.searching", "Searching for Ground");
        REGISTRATE.addRawLang("legworks.status.off_ship", "Not on a Ship: inert");
        REGISTRATE.addRawLang("legworks.status.bumper", "Bumper: spring only, no Leveling");
        REGISTRATE.addRawLang("legworks.status.not_linked", "Not linked: inert");
        REGISTRATE.addRawLang("legworks.status.linked", "Linked to a Leg Controller");
        REGISTRATE.addRawLang("legworks.status.folded", "Gear Up: folded");
        REGISTRATE.addRawLang("legworks.status.overreach", "Overreach: Ground too uneven to level");
        REGISTRATE.addRawLang("legworks.status.stiffness", "Stiffness: %s (Limb x%s)");
        REGISTRATE.addRawLang("legworks.status.stiffness_fixed", "Stiffness: fixed (Limb x%s)");
        REGISTRATE.addRawLang("legworks.status.damping", "Damping: %s");
        REGISTRATE.addRawLang("legworks.status.damping_fixed", "Damping: fixed %s");
        REGISTRATE.addRawLang("legworks.status.ride_height_fixed", "Ride Height: fixed %s");
        REGISTRATE.addRawLang("legworks.status.power", "Power: %s RPM");
        REGISTRATE.addRawLang("legworks.status.no_power", "No power: fully Softened");
        REGISTRATE.addRawLang("legworks.status.softening", "Softening: %s (top redstone %s)");
        REGISTRATE.addRawLang("legworks.status.legs", "Legs: %s/%s linked");
        REGISTRATE.addRawLang("legworks.status.grounded", "On Ground: %s/%s");
        REGISTRATE.addRawLang("legworks.status.ride_height", "Ride Height: %s");
        REGISTRATE.addRawLang("legworks.status.gear", "Gear: %s");
        REGISTRATE.addRawLang("legworks.status.no_legs", "No Legs linked");
        REGISTRATE.addRawLang("legworks.display.controller", "Ride Height %s | Legs %s/%s grounded | %s");
        REGISTRATE.addRawLang("legworks.display.mount", "Reach %s/%s | Load %s");
        REGISTRATE.addRawLang("legworks.display.overreach", "Overreach");
        REGISTRATE.addRawLang("legworks.display.ride_height", "Ride %s");
        REGISTRATE.addRawLang("legworks.display.grounded", "Grounded %s/%s");
        REGISTRATE.addRawLang("legworks.display.reach", "Reach %s/%s");
        REGISTRATE.addRawLang("legworks.display.load", "Load %s");
        REGISTRATE.addRawLang("legworks.display_source.leg_controller_status", "Leg Controller Status");
        REGISTRATE.addRawLang("legworks.display_source.leg_mount_status", "Leg Status");

        tooltip("block", LegworksIds.SIMPLE_LEG_MOUNT,
                "A _Leg Mount_ for Ships. Fit a _Limb_ and it _Levels_ the Ship on its own. Needs no power and no Leg Controller.",
                "When a Limb is fitted", "The Leg _reaches down_ to the Ground and keeps the deck level, with _fixed_ Stiffness and Damping.",
                "When facing sideways or up", "It is a _Bumper_: a spring against Ground it meets. It never Levels.",
                "When R-Clicked with a Limb", "_Fits_ the Limb. _Sneak-click_ with an empty hand to take it back.");
        tooltip("block", LegworksIds.HYDRAULIC_LEG_MOUNT,
                "A powered _Leg Mount_. It Levels to the _Ride Height_ of its _Leg Controller_ and folds on _Gear Up_. Set _Stiffness_ and _Damping_ on its sides.",
                "When Rotated", "_Faster_ rotation changes the Reach _faster_. Without power it _Softens fully_ and lets the Ship settle.",
                "When Powered From Top", "_Softens_ the Leg: a stronger signal makes it _softer_.",
                "Without a Leg Controller", "The Leg is _inert_. _Link_ it by clicking it with a Leg Controller.");
        tooltip("block", LegworksIds.LEG_CONTROLLER,
                "Coordinates a _Leg Group_ of Hydraulic Legs on the _same Ship_. Needs no rotational power.",
                "When Used on Hydraulic Legs", "_Links_ them, up to _8 Legs_ within _16 blocks_. _Sneak-click_ a placed Controller first to select it.",
                "When Powered by Redstone", "_Gear Up_: the Leg Group folds against the Ship. No signal means _Gear Down_.",
                "When its value is Scrolled", "Sets the _Ride Height_, how high the Legs hold the deck. _Auto_ uses each Limb's default.");
        tooltip("item", LegworksIds.SHORT_LIMB,
                "An early _Limb_ for small Ships. _Reach 3 blocks_, _Stiffness x1_.",
                "When Used on a Leg Mount", "_Fits_ onto it and becomes the leg drawn below the Mount. Any Limb fits any Leg Mount.");
        tooltip("item", LegworksIds.LONG_LIMB,
                "A long brass _Limb_ for tall Ships and rough Ground. _Reach 4.5 blocks_, _Stiffness x1_.",
                "When Used on a Leg Mount", "_Fits_ onto it and becomes the leg drawn below the Mount. Any Limb fits any Leg Mount.");
        tooltip("item", LegworksIds.HEAVY_LIMB,
                "A sturdy _Limb_ for big, heavy Ships. _Reach 3.5 blocks_, _Stiffness x2_: it sags half as much.",
                "When Used on a Leg Mount", "_Fits_ onto it and becomes the leg drawn below the Mount. Any Limb fits any Leg Mount.");

        REGISTRATE.addRawLang("legworks.jei.fitting", "Leg Fitting");
        REGISTRATE.addRawLang("legworks.jei.fitting.hint", "R-Click the Leg Mount with the Limb");
        REGISTRATE.addRawLang("legworks.jei.info.limb", "A Limb fits any Leg Mount: right-click the Mount with it. It becomes the Leg drawn below the Mount. Sneak-click the Mount with an empty hand to take the Limb back.");
        REGISTRATE.addRawLang("legworks.jei.info.limb_stats", "The Limb decides the Leg's maximum Reach and base Stiffness. Reach: %s blocks. Stiffness: x%s. Default height: %s blocks.");
        REGISTRATE.addRawLang("legworks.jei.info.limb_overreach", "If the Ground is too uneven for the Reach, the Legs stretch to the maximum and the Ship tilts toward the low side: Overreach.");
        REGISTRATE.addRawLang("legworks.jei.info.facing", "Facing: the Limb extends the way the Mount faces. Facing down it is a Foot and helps Level the Ship. Facing sideways or up it is a Bumper: a spring against Ground it meets, never Leveling.");
        REGISTRATE.addRawLang("legworks.jei.info.simple", "Place it on a Ship's hull and right-click it with a Limb. It needs no power and no Leg Controller: it Levels the Ship on its own, with fixed Stiffness and Damping, at the Limb's default height. Off a Ship it does nothing.");
        REGISTRATE.addRawLang("legworks.jei.info.hydraulic", "Place it on a Ship's hull and right-click it with a Limb. It needs rotation through its shaft (4 SU per RPM) and a Linked Leg Controller on the same Ship; without a Link it is inert.");
        REGISTRATE.addRawLang("legworks.jei.info.hydraulic_power", "Power: faster rotation changes the Reach faster (a full Gear Up takes about 4 s at 16 RPM). Without power, or when overstressed, the Leg Softens fully and lets the Ship settle onto the Ground.");
        REGISTRATE.addRawLang("legworks.jei.info.hydraulic_settings", "Stiffness (25 to 200 percent) and Damping (0.2 to 1.5) are set on the value boxes on its sides. Stiffness is how hard the spring pushes back. Damping is how quickly it stops bouncing.");
        REGISTRATE.addRawLang("legworks.jei.info.hydraulic_softening", "Softening: a redstone signal on its top face makes the Leg softer; a stronger signal means softer.");
        REGISTRATE.addRawLang("legworks.jei.info.controller", "Coordinates the Hydraulic Legs Linked to it (a Leg Group). It needs no rotational power and must be on the same Ship as its Legs.");
        REGISTRATE.addRawLang("legworks.jei.info.controller_linking", "Linking: sneak-click the placed Leg Controller with a Leg Controller item to select it, then click each Hydraulic Leg Mount. Up to 8 Legs within 16 blocks. Click a Linked Leg again to unlink it.");
        REGISTRATE.addRawLang("legworks.jei.info.controller_ride_height", "Ride Height: scroll its value box to set how high the Legs hold the deck, in blocks. Auto uses each Limb's own default height.");
        REGISTRATE.addRawLang("legworks.jei.info.controller_gear", "Gear Up: any redstone signal folds the Leg Group against the Ship for flight. Without a signal the Legs come back down (Gear Down).");
        REGISTRATE.addRawLang("legworks.jei.info.goggles", "Engineer's Goggles show each Leg's Reach, load and settings, and warn about Overreach and Conflicting Ride Height.");

        REGISTRATE.addRawLang("config.jade.plugin_legworks.leg_status", "Landing Legs: Leg status");
    }
}
