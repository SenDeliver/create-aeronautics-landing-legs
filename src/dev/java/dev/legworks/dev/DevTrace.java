package dev.legworks.dev;

import dev.legworks.leg.Leg;
import dev.legworks.leg.LegLeveling;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Per-tick TRACE line per Ship: position, tilt, and per Leg rest/dist/slip-or-wall/anchor/force/embedded/depth error.
 * Format is parsed by tools/analyze.py.
 */
final class DevTrace implements LegLeveling.LevelingObserver {
    private static final Logger LOG = LoggerFactory.getLogger("legworks-dev");
    static final DevTrace INSTANCE = new DevTrace();

    static void set(boolean on) {
        LegLeveling.observer = on ? INSTANCE : null;
    }

    @Override
    public void onLeveled(ServerSubLevel ship, List<Leg> legs, long gameTime) {
        Pose3dc pose = ship.logicalPose();
        Vector3d up = pose.transformNormal(new Vector3d(0, 1, 0));
        double tiltDeg = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, up.y))));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < legs.size(); i++) {
            Leg leg = legs.get(i);
            sb.append(String.format(" L%d=%.3f/%s/%s/%.2f/%.0f/%s/%.2f", i, leg.rest(),
                    leg.ground() == null ? "none" : String.format("%.3f", leg.ground().distance()),
                    leg.lastSlip() ? "S" : leg.lastWallHits() > 0 ? "W" : "-", leg.lastAnchorOffset(), leg.lastNormalForce(),
                    leg.footEmbedded(ship) ? "E" : "-", leg.usedFootDistance() - leg.plainCentreDistance(ship)));
        }
        Vector3d pos = new Vector3d(pose.position());
        LOG.info("TRACE ship={} t={} pos={},{},{} tilt={}{}", ship.getUniqueId().toString().substring(0, 8), gameTime,
                String.format("%.3f", pos.x), String.format("%.3f", pos.y), String.format("%.3f", pos.z), String.format("%.2f", tiltDeg), sb);
    }
}
