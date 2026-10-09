package dev.legworks.content.controller;

import dev.legworks.config.LegworksConfig;
import dev.legworks.content.hydraulic.HydraulicLegMountBlockEntity;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Linking Legs to Leg Controllers. Links are stored on both sides as offsets (Mount to Controller, Controller to Leg),
 * so they survive a Ship's assembly moving both blocks into plot coordinates.
 */
public final class Linking {
    public enum Result {
        LINKED("linked"), UNLINKED("unlinked"), RELINKED("relinked"),
        TOO_FAR("too_far"), OTHER_SHIP("other_ship"), FULL("full"), NOT_A_LEG("not_a_leg"), NO_CONTROLLER("no_controller");

        public final String key;

        Result(String key) {
            this.key = key;
        }

        public boolean ok() {
            return this == LINKED || this == UNLINKED || this == RELINKED;
        }
    }

    public record Outcome(Result result, int count, int max) {
        public Component message() {
            return Component.translatable("legworks.link." + result.key, count, max);
        }
    }

    private Linking() {
    }

    public static int maxLegs() {
        return LegworksConfig.value(LegworksConfig.MAX_LEGS_PER_CONTROLLER);
    }

    public static int range() {
        return LegworksConfig.value(LegworksConfig.LINK_RANGE);
    }

    @Nullable
    public static SubLevel shipAt(Level level, BlockPos pos) {
        return Sable.HELPER.getContaining(level, Vec3.atCenterOf(pos));
    }

    public static boolean sameShip(Level level, BlockPos a, BlockPos b) {
        return shipAt(level, a) == shipAt(level, b);
    }

    public static boolean inRange(BlockPos a, BlockPos b) {
        double r = range();
        return a.distSqr(b) <= r * r;
    }

    /** Toggles the Link between the Controller at {@code controllerPos} and the Leg at {@code legPos}. Server side. */
    public static Outcome toggle(Level level, BlockPos controllerPos, BlockPos legPos) {
        int max = maxLegs();
        if (!(level.getBlockEntity(controllerPos) instanceof LegControllerBlockEntity controller)) return new Outcome(Result.NO_CONTROLLER, 0, max);
        if (!(level.getBlockEntity(legPos) instanceof HydraulicLegMountBlockEntity mount)) return new Outcome(Result.NOT_A_LEG, controller.linkCount(), max);
        BlockPos toLeg = legPos.subtract(controllerPos);
        if (controller.hasLink(toLeg)) {
            controller.removeLink(toLeg);
            if (controllerPos.equals(mount.controllerPos())) mount.setControllerOffset(null);
            return new Outcome(Result.UNLINKED, controller.linkCount(), max);
        }
        if (!sameShip(level, controllerPos, legPos)) return new Outcome(Result.OTHER_SHIP, controller.linkCount(), max);
        if (!inRange(controllerPos, legPos)) return new Outcome(Result.TOO_FAR, controller.linkCount(), max);
        if (controller.linkCount() >= max) return new Outcome(Result.FULL, controller.linkCount(), max);

        Result result = Result.LINKED;
        BlockPos oldPos = mount.controllerPos();
        if (oldPos != null && level.getBlockEntity(oldPos) instanceof LegControllerBlockEntity old) {
            old.removeLink(legPos.subtract(oldPos));
            result = Result.RELINKED;
        }
        controller.addLink(toLeg);
        mount.setControllerOffset(controllerPos.subtract(legPos));
        return new Outcome(result, controller.linkCount(), max);
    }

    /** A Mount is gone: drop it from its Controller. */
    public static void onMountRemoved(Level level, BlockPos legPos, @Nullable BlockPos controllerPos) {
        if (controllerPos != null && level.getBlockEntity(controllerPos) instanceof LegControllerBlockEntity c) c.removeLink(legPos.subtract(controllerPos));
    }

    /** A Controller is gone: unlink its Legs. */
    public static void onControllerRemoved(Level level, BlockPos controllerPos, Iterable<BlockPos> links) {
        for (BlockPos off : links) {
            BlockPos legPos = controllerPos.offset(off);
            if (level.getBlockEntity(legPos) instanceof HydraulicLegMountBlockEntity m && controllerPos.equals(m.controllerPos())) m.setControllerOffset(null);
        }
    }
}
