package dev.legworks.leg;

import dev.legworks.config.LegworksConfig;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import org.joml.Matrix3d;
import org.joml.Vector3d;

import java.util.List;

/**
 * Leveling solver, run once per Ship per tick over every downward Leg on it, whatever its Limb or Mount type.
 * Each Leg brings its own Reach, rest range, Stiffness and Ride Height target ({@link LegSettings}).
 */
public final class LegLeveling {
    static final double ANTI_ROLL_HEIGHT = 3.0;
    static final int LOST_LEG_CROUCH_TICKS = 20;
    private static final double DT = Leg.TICK;

    /** Optional hook called after each solve; used by dev tooling for tracing. */
    public static volatile LevelingObserver observer;

    public interface LevelingObserver {
        void onLeveled(ServerSubLevel ship, List<Leg> legs, long gameTime);
    }

    private LegLeveling() {
    }

    static void tick(ServerSubLevel ship, List<Leg> legs, long gameTime) {
        int n = legs.size();
        if (n == 0) return;
        for (Leg leg : legs) if (!leg.probed) return;

        Pose3dc pose = ship.logicalPose();
        Vector3d centre = new Vector3d();
        for (Leg leg : legs) centre.add(mountLocal(leg));
        centre.div(n);

        double[] tiltDisp = new double[n];
        double[] lx = new double[n], lz = new double[n];
        for (int i = 0; i < n; i++) {
            Vector3d p = mountLocal(legs.get(i)).sub(centre);
            lx[i] = p.x;
            lz[i] = p.z;
            tiltDisp[i] = pose.transformNormal(new Vector3d(p)).y - p.y;
        }

        double[] loadResidual = loadResidual(legs, lx, lz);

        int grounded = 0;
        double rideError = 0;
        double targetSum = 0;
        int firmGrounded = 0;
        for (Leg leg : legs) {
            targetSum += leg.rideTarget();
            if (leg.ground() == null) continue;
            grounded++;
            if (leg.softening() >= 1) continue;
            rideError += leg.rideTarget() - leg.rest;
            firmGrounded++;
        }
        rideError = firmGrounded > 0 ? rideError / firmGrounded : 0;
        for (Leg leg : legs) {
            if (leg.ground() == null || leg.softening() >= 1) continue;
            if (leg.rest >= leg.reach() - 0.01) rideError = Math.min(rideError, 0);
            if (leg.rest <= leg.minRest() + 0.01) rideError = Math.max(rideError, 0);
        }
        double sharedAirborneRest = targetSum / n;

        double levelGain = LegworksConfig.LEVEL_GAIN.get();
        double loadGain = LegworksConfig.LOAD_GAIN.get();
        double rideGain = LegworksConfig.RIDE_GAIN.get();
        double offloadGain = LegworksConfig.OFFLOAD_GAIN.get();
        double maxRate = LegworksConfig.MAX_REST_RATE.get();
        double crouchRate = LegworksConfig.CROUCH_RATE.get();

        boolean fullSupport = grounded == n && !(n <= 3 && ShipLegs.lostLegWithin(ship.getUniqueId(), gameTime, LOST_LEG_CROUCH_TICKS));
        for (int i = 0; i < n; i++) {
            Leg leg = legs.get(i);
            double stroke = leg.strokeRate();
            if (leg.ground() == null) {
                leg.rest = grounded > 0 ? leg.reach() : approach(leg.rest, leg.clampRest(sharedAirborneRest), stroke * DT);
                continue;
            }
            if (leg.softening() >= 1) {
                leg.rest = leg.clampRest(leg.ground().distance());
                continue;
            }
            if (!fullSupport) {
                leg.rest = Math.max(leg.minRest(), leg.rest - crouchRate * DT);
                continue;
            }
            double rate = -levelGain * tiltDisp[i] + rideGain * rideError;
            if (leg.lastK() > 0) {
                if (loadResidual != null) rate -= loadGain * loadResidual[i] / leg.lastK();
                rate += offloadGain * leg.antiRollForce / leg.lastK();
            }
            double cap = Math.min(maxRate, stroke);
            rate = Math.max(-cap, Math.min(cap, rate));
            leg.rest = leg.clampRest(leg.rest + rate * DT);
        }

        double shipMass = ship.getMassTracker().getMass();
        double gravity = DimensionPhysicsData.getGravity(ship.getLevel()).length();
        double sxx = 0, szz = 0, sxz = 0, sxd = 0, szd = 0, sxr = 0, szr = 0;
        for (int i = 0; i < n; i++) {
            Leg leg = legs.get(i);
            double rate = Double.isNaN(leg.lastTiltDisp) ? 0 : (tiltDisp[i] - leg.lastTiltDisp) / DT;
            sxx += lx[i] * lx[i];
            szz += lz[i] * lz[i];
            sxz += lx[i] * lz[i];
            sxd += lx[i] * tiltDisp[i];
            szd += lz[i] * tiltDisp[i];
            sxr += lx[i] * rate;
            szr += lz[i] * rate;
        }
        double det = sxx * szz - sxz * sxz;
        boolean active = fullSupport && n >= 3 && det > 1e-6;
        double gx = 0, gz = 0, gxr = 0, gzr = 0;
        if (active) {
            gx = (szz * sxd - sxz * szd) / det;
            gz = (sxx * szd - sxz * sxd) / det;
            gxr = (szz * sxr - sxz * szr) / det;
            gzr = (sxx * szr - sxz * sxr) / det;
        }
        double stiffness = LegworksConfig.ANTI_ROLL.get() * shipMass * gravity * ANTI_ROLL_HEIGHT;
        double damping = stiffness * LegworksConfig.ANTI_ROLL_DAMP.get();
        double mx = -(stiffness * gx + damping * gxr);
        double mz = -(stiffness * gz + damping * gzr);
        for (int i = 0; i < n; i++) {
            legs.get(i).antiRollForce = active ? mx * lx[i] / sxx + mz * lz[i] / szz : 0;
            legs.get(i).lastTiltDisp = tiltDisp[i];
        }

        LevelingObserver obs = observer;
        if (obs != null) obs.onLeveled(ship, legs, gameTime);
    }

    private static double approach(double from, double to, double step) {
        if (Double.isNaN(from) || Double.isInfinite(step)) return to;
        return from + Math.max(-step, Math.min(step, to - from));
    }

    private static Vector3d mountLocal(Leg leg) {
        BlockPos p = leg.pos();
        return new Vector3d(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
    }

    private static double[] loadResidual(List<Leg> legs, double[] lx, double[] lz) {
        int n = legs.size();
        if (n < 4) return null;
        Matrix3d ata = new Matrix3d().zero();
        Vector3d atb = new Vector3d();
        for (int i = 0; i < n; i++) {
            if (legs.get(i).ground() == null) return null;
            double[] row = {1, lx[i], lz[i]};
            double f = legs.get(i).lastNormalForce();
            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 3; c++) ata.set(c, r, ata.get(c, r) + row[r] * row[c]);
            }
            atb.add(row[0] * f, row[1] * f, row[2] * f);
        }
        if (Math.abs(ata.determinant()) < 1e-6) return null;
        Vector3d coef = ata.invert().transform(atb);
        double[] res = new double[n];
        for (int i = 0; i < n; i++) res[i] = legs.get(i).lastNormalForce() - (coef.x + coef.y * lx[i] + coef.z * lz[i]);
        return res;
    }
}
