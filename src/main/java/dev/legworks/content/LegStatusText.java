package dev.legworks.content;

import dev.legworks.content.controller.LegControllerBlockEntity;
import dev.legworks.content.controller.Linking;
import dev.legworks.content.hydraulic.HydraulicLegMountBlockEntity;
import dev.legworks.leg.Leg;
import dev.legworks.leg.LegHost;
import dev.legworks.limb.Limb;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.List;

/**
 * Status lines for Leg Mounts and Leg Controllers, shared by the Engineer's Goggles, Jade and Display Links
 * so they always say the same thing. Works on both sides (client values are the synced ones).
 */
public final class LegStatusText {
    private static final ChatFormatting LABEL = ChatFormatting.GRAY;
    private static final ChatFormatting VALUE = ChatFormatting.AQUA;
    private static final ChatFormatting GOOD = ChatFormatting.GREEN;
    private static final ChatFormatting WARN = ChatFormatting.GOLD;
    private static final ChatFormatting BAD = ChatFormatting.RED;

    private LegStatusText() {
    }

    public static MutableComponent blocks(double v) {
        return Component.translatable("legworks.unit.blocks", num(v));
    }

    public static String num(double v) {
        return String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    public static MutableComponent percent(double fraction) {
        return Component.translatable("legworks.unit.percent", Math.round(fraction * 100));
    }

    public static MutableComponent rideHeight(int tenths) {
        return tenths <= 0 ? Component.translatable("legworks.ride_height.auto") : blocks(tenths / 10.0);
    }

    public static MutableComponent gear(boolean up) {
        return Component.translatable(up ? "legworks.controller.gear_up" : "legworks.controller.gear_down");
    }

    private static MutableComponent line(String key, Object... args) {
        Object[] styled = new Object[args.length];
        for (int i = 0; i < args.length; i++) {
            styled[i] = args[i] instanceof Component c ? c.copy().withStyle(VALUE) : Component.literal(String.valueOf(args[i])).withStyle(VALUE);
        }
        return Component.translatable(key, styled).withStyle(LABEL);
    }

    private static MutableComponent flag(String key, ChatFormatting color) {
        return Component.translatable(key).withStyle(color);
    }

    public static List<MutableComponent> mount(LegHost host) {
        List<MutableComponent> out = new ArrayList<>();
        Limb limb = host.limb();
        if (limb == null) {
            out.add(flag("legworks.message.no_limb", WARN));
            return out;
        }
        Leg leg = host.leg();
        HydraulicLegMountBlockEntity hydraulic = host instanceof HydraulicLegMountBlockEntity h ? h : null;
        out.add(line("legworks.status.limb", host.limbSlot().stack().getHoverName()));
        float reach = leg.statusReach();
        out.add(line("legworks.status.reach", num(Float.isNaN(reach) ? limb.reach() : reach), num(limb.reach())));

        if (!leg.isFoot()) out.add(flag("legworks.status.bumper", LABEL));
        if (!leg.isOnShip()) {
            out.add(flag("legworks.status.off_ship", WARN));
        } else if (hydraulic != null && !hydraulic.isLinked()) {
            out.add(flag("legworks.status.not_linked", BAD));
        } else if (hydraulic != null && hydraulic.isGearUp()) {
            out.add(flag("legworks.status.folded", LABEL));
        } else {
            out.add(leg.statusGrounded() ? line("legworks.status.load", percent(leg.statusLoad())) : flag("legworks.status.searching", LABEL));
        }
        if (leg.statusOverreach()) out.add(flag("legworks.status.overreach", WARN));

        if (hydraulic != null) {
            out.add(line("legworks.status.stiffness", percent(host.legSettings().stiffnessScale()), trim(limb.stiffness())));
            out.add(line("legworks.status.damping", num(host.legSettings().dampingRatio())));
            if (!hydraulic.isPowered()) {
                out.add(flag("legworks.status.no_power", BAD));
            } else {
                out.add(line("legworks.status.power", Math.round(Math.abs(hydraulic.getSpeed()))));
                if (hydraulic.softening() > 0.005 || hydraulic.topSignal() > 0) {
                    out.add(line("legworks.status.softening", percent(hydraulic.softening()), hydraulic.topSignal()));
                }
            }
            if (hydraulic.isLinked()) out.add(flag("legworks.status.linked", GOOD));
        } else {
            out.add(line("legworks.status.stiffness_fixed", trim(limb.stiffness())));
            out.add(line("legworks.status.damping_fixed", num(host.legSettings().dampingRatio())));
            out.add(line("legworks.status.ride_height_fixed", blocks(limb.nominalRest())));
        }
        return out;
    }

    public static List<MutableComponent> controller(LegControllerBlockEntity be) {
        List<MutableComponent> out = new ArrayList<>();
        out.add(line("legworks.status.legs", be.linkCount(), Linking.maxLegs()));
        if (be.linkCount() > 0) out.add(line("legworks.status.grounded", be.groundedCount(), be.linkCount()));
        out.add(line("legworks.status.ride_height", rideHeight(be.rideHeightSetting())));
        out.add(line("legworks.status.gear", gear(be.isGearUp())));
        if (be.hasConflict()) out.add(flag("legworks.controller.conflict", WARN));
        if (be.hasOverreach()) out.add(flag("legworks.status.overreach", WARN));
        if (be.linkCount() == 0) out.add(flag("legworks.status.no_legs", WARN));
        return out;
    }

    /** Display Link lines: Ride Height, grounded Legs, Gear, then Overreach if any. */
    public static List<MutableComponent> controllerDisplay(LegControllerBlockEntity be) {
        List<MutableComponent> out = new ArrayList<>();
        out.add(Component.translatable("legworks.display.ride_height", rideHeight(be.rideHeightSetting())));
        out.add(Component.translatable("legworks.display.grounded", be.groundedCount(), be.linkCount()));
        out.add(gear(be.isGearUp()));
        if (be.hasOverreach()) out.add(Component.translatable("legworks.display.overreach"));
        return out;
    }

    /** Display Link lines: Reach, load, then Overreach if any. */
    public static List<MutableComponent> mountDisplay(LegHost host) {
        List<MutableComponent> out = new ArrayList<>();
        Limb limb = host.limb();
        if (limb == null) {
            out.add(Component.translatable("legworks.message.no_limb"));
            return out;
        }
        Leg leg = host.leg();
        float reach = leg.statusReach();
        out.add(Component.translatable("legworks.display.reach", num(Float.isNaN(reach) ? limb.reach() : reach), num(limb.reach())));
        out.add(Component.translatable("legworks.display.load", percent(leg.statusGrounded() ? leg.statusLoad() : 0)));
        if (leg.statusOverreach()) out.add(Component.translatable("legworks.display.overreach"));
        return out;
    }

    /** One line (logs and chat): Ride Height, grounded Legs, Gear. */
    public static MutableComponent controllerLine(LegControllerBlockEntity be) {
        MutableComponent c = Component.translatable("legworks.display.controller", rideHeight(be.rideHeightSetting()),
                be.groundedCount(), be.linkCount(), gear(be.isGearUp()));
        if (be.hasOverreach()) c.append(" | ").append(Component.translatable("legworks.display.overreach"));
        return c;
    }

    /** One line (logs and chat): Reach and load. */
    public static MutableComponent mountLine(LegHost host) {
        Limb limb = host.limb();
        if (limb == null) return Component.translatable("legworks.message.no_limb");
        Leg leg = host.leg();
        float reach = leg.statusReach();
        MutableComponent c = Component.translatable("legworks.display.mount", num(Float.isNaN(reach) ? limb.reach() : reach),
                num(limb.reach()), percent(leg.statusGrounded() ? leg.statusLoad() : 0));
        if (leg.statusOverreach()) c.append(" | ").append(Component.translatable("legworks.display.overreach"));
        return c;
    }

    private static String trim(float v) {
        return v == Math.rint(v) ? String.valueOf((int) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
