package dev.legworks.leg;

import dev.legworks.config.LegworksConfig;
import dev.legworks.limb.Limb;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.force.ForceTotal;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.mixinterface.clip_overwrite.ClipContextExtension;
import dev.ryanhcode.sable.physics.config.dimension_physics.DimensionPhysicsData;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.createmod.catnip.levelWrappers.WrappedLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The physics of one Leg: Ground probing, spring and damping, Foot grip, Walls and the client-side drawn Reach.
 * Owned by a Leg Mount block entity ({@link LegHost}); Leveling across a Ship lives in {@link LegLeveling}.
 * Spring and raycast logic adapted from Create Offroad's WheelMountBlockEntity (MIT, The Simulated Team / The Creators of Aeronautics).
 */
public final class Leg {
    static final double FOOT_SAMPLE = 0.28;
    static final int DEBOUNCE_TICKS = 6;
    static final double DEBOUNCE_JUMP = 0.3;
    static final double STEP_TOL = 0.5;
    static final double WALL_BIAS = 0.1;
    static final double FOOT_FALL_RATE = 3.0;
    static final double BUMP_DIST = 0.1;
    static final double BUMP_STIFFNESS = 10.0;
    static final double MIN_FACING = 0.25;
    static final double GRIP_DAMP = 0.8;
    static final double TICK = 0.05;
    public static final double TUCKED_REACH = 0.3;
    static final double OVERREACH_TILT = Math.toRadians(2.0);
    static final int OVERREACH_TICKS = 20;
    static final double LOAD_SMOOTH = 0.2;
    public static final String NBT_STATUS = "Status";

    public record GroundHit(double distance, Vec3 location, Vector3d normalLocal, @Nullable SubLevel subLevel) {}

    private final LegHost host;
    private final ForceTotal forceTotal = new ForceTotal();

    private UUID shipId;
    private int tickCounter;

    private GroundHit ground;
    private Vec3 anchor;
    private SubLevel anchorFrame;
    private int lostTicks;
    private boolean everGrounded;
    private boolean resumeFromGround;
    private double footExt = Double.NaN;
    private boolean clientEverGrounded;
    private List<Vector3d> walls = List.of();

    private double lastNormalForce, lastVelAxis, lastK;
    private double lastAnchorOffset;
    private boolean lastSlip;
    private int lastWallHits;

    double rest = Double.NaN;
    boolean probed;
    double lastTiltDisp = Double.NaN;
    double antiRollForce;

    private Vector3d kneeDir;
    private double clientReach = Double.NaN, clientReachPrev = Double.NaN;
    private volatile boolean onShip;
    private volatile int collisionReach16;

    private boolean statusGrounded, statusOverreach;
    private float statusLoad, statusReach = Float.NaN;
    private double loadAvg;
    private int overreachFlip;
    private boolean sentGrounded, sentOverreach;
    private float sentLoad = -1, sentReach = -1;
    private double displayReach = Double.NaN;

    public Leg(LegHost host) {
        this.host = host;
    }

    private BlockEntity be() {
        return host.asBlockEntity();
    }

    private Level level() {
        return be().getLevel();
    }

    public BlockPos pos() {
        return be().getBlockPos();
    }

    public Direction facing() {
        BlockState state = be().getBlockState();
        return state.hasProperty(BlockStateProperties.FACING) ? state.getValue(BlockStateProperties.FACING) : Direction.DOWN;
    }

    public boolean isFoot() {
        return facing() == Direction.DOWN;
    }

    @Nullable
    public Limb limb() {
        return host.limb();
    }

    public boolean participating() {
        return host.limb() != null && host.legSettings().active();
    }

    boolean isFootNow() {
        return !be().isRemoved() && participating() && isFoot();
    }

    /** Current maximum Reach: the Limb's, shortened while the Leg folds for Gear Up. */
    public double reach() {
        Limb limb = host.limb();
        if (limb == null) return 0;
        double fold = host.legSettings().fold();
        if (fold <= 0) return limb.reach();
        double tucked = Math.min(TUCKED_REACH, limb.reach());
        return tucked + (limb.reach() - tucked) * (1 - Math.min(1, fold));
    }

    double minRest() {
        Limb limb = host.limb();
        return limb == null ? 0 : Math.min(limb.minRest(), reach());
    }

    double clampRest(double r) {
        return Math.max(minRest(), Math.min(reach(), r));
    }

    double softening() {
        return Math.max(0, Math.min(1, host.legSettings().softening()));
    }

    double strokeRate() {
        Limb limb = host.limb();
        return limb == null ? 0 : host.legSettings().strokeRate(limb);
    }

    public double rest() {
        return rest;
    }

    public void setSavedRest(double saved) {
        Limb limb = host.limb();
        rest = limb == null || Double.isNaN(saved) ? Double.NaN : limb.clampRest(saved);
    }

    double rideTarget() {
        Limb limb = host.limb();
        return limb == null ? 0 : clampRest(host.legSettings().rideTarget(limb));
    }

    private Vec3 mountFace() {
        Direction f = facing();
        return be().getBlockPos().getCenter().add(f.getStepX() * 0.5, f.getStepY() * 0.5, f.getStepZ() * 0.5);
    }

    private GroundHit castGround(SubLevel self, double reach, double footDepth, List<Vector3d> wallsOut) {
        Direction f = facing();
        Vec3 axis = new Vec3(f.getStepX(), f.getStepY(), f.getStepZ());
        Vec3 centre = mountFace();
        GroundHit best = castRay(self, centre, axis, reach);
        for (Direction.Axis a : Direction.Axis.values()) {
            if (a == f.getAxis()) continue;
            Vec3 u = new Vec3(a == Direction.Axis.X ? 1 : 0, a == Direction.Axis.Y ? 1 : 0, a == Direction.Axis.Z ? 1 : 0);
            for (int sign = 1; sign >= -1; sign -= 2) {
                Vec3 off = u.scale(sign);
                GroundHit h = castRay(self, centre.add(off.scale(FOOT_SAMPLE)), axis, reach);
                if (h == null) continue;
                if (footDepth < reach + 1 && h.distance() < footDepth - STEP_TOL) {
                    if (wallsOut != null) wallsOut.add(new Vector3d(off.x, off.y, off.z));
                    continue;
                }
                if (best == null || h.distance() < best.distance()) best = h;
            }
        }
        return best;
    }

    private GroundHit castRay(SubLevel self, Vec3 from, Vec3 axis, double reach) {
        Level level = level();
        Vec3 to = from.add(axis.scale(reach));
        ClipContext ctx = new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty());
        ((ClipContextExtension) ctx).sable$setIgnoredSubLevel(self);
        BlockHitResult hit = level.clip(ctx);
        if (hit.getType() == HitResult.Type.MISS) return null;

        Pose3dc pose = self.logicalPose();
        SubLevel other = Sable.HELPER.getContaining(level, hit.getLocation());
        Vec3 worldHit = other == null ? hit.getLocation() : other.logicalPose().transformPosition(hit.getLocation());
        Vec3 localHit = pose.transformPositionInverse(worldHit);
        Direction d = hit.getDirection();
        Vector3d n = new Vector3d(d.getStepX(), d.getStepY(), d.getStepZ());
        if (other != null) other.logicalPose().transformNormal(n);
        pose.transformNormalInverse(n);

        double dist = localHit.subtract(from).dot(axis);
        double facesLeg = -n.dot(axis.x, axis.y, axis.z);
        if (dist < 0 || dist > reach || facesLeg < MIN_FACING) return null;
        return new GroundHit(dist, hit.getLocation(), n, other);
    }

    private GroundHit debounce(GroundHit fresh) {
        GroundHit held = ground;
        boolean lost = fresh == null || (held != null && fresh.subLevel() == held.subLevel() && fresh.distance() > held.distance() + DEBOUNCE_JUMP);
        if (held != null && lost && lostTicks < DEBOUNCE_TICKS) {
            lostTicks++;
            return held;
        }
        lostTicks = 0;
        return fresh;
    }

    /** Game tick on the server, from {@code sable$tick}. */
    public void serverTick(ServerSubLevel subLevel) {
        onShip = true;
        Limb limb = host.limb();
        if (limb == null || !host.legSettings().active()) {
            deactivate();
            if (limb != null) resumeFromGround = true;
            collisionReach16 = limb == null ? 0 : (int) Math.round(reach() * 16);
            clearStatus();
            statusReach = limb == null ? Float.NaN : (float) reach();
            return;
        }
        double reach = reach();
        if (Double.isNaN(rest)) rest = rideTarget();
        else if (rest > reach) rest = reach;
        if (Double.isNaN(footExt)) footExt = reach;
        trackShip(subLevel);

        footExt = ground != null ? ground.distance() : Math.min(reach, footExt + FOOT_FALL_RATE * TICK);
        double footDepth = !everGrounded ? Double.MAX_VALUE : footExt;
        List<Vector3d> newWalls = new ArrayList<>();
        GroundHit fresh = castGround(subLevel, reach, footDepth, newWalls);
        if (isFoot() && fresh != null && everGrounded) {
            double before = ground != null ? ground.distance() : Math.min(rest, reach);
            double approach = Math.max(0, -lastVelAxis * TICK);
            double appeared = (before - fresh.distance()) - approach;
            if (appeared > STEP_TOL) rest = Math.max(minRest(), rest - appeared);
        }
        walls = newWalls;
        ground = debounce(fresh);
        if (ground != null) everGrounded = true;
        if (resumeFromGround) {
            if (ground != null) rest = clampRest(ground.distance());
            resumeFromGround = false;
        }
        collisionReach16 = (int) Math.round((ground == null ? reach : ground.distance()) * 16);
        if (ground == null || ground.subLevel() != anchorFrame) {
            anchor = null;
            anchorFrame = ground == null ? null : ground.subLevel();
        }
        probed = true;
        updateStatus(subLevel);
        if (++tickCounter % 20 == 0) be().setChanged();
    }

    private void updateStatus(ServerSubLevel subLevel) {
        statusGrounded = ground != null;
        statusReach = (float) usedFootDistance();
        double weight = subLevel.getMassTracker().getMass() * DimensionPhysicsData.getGravity(level()).length();
        loadAvg += (lastNormalForce - loadAvg) * LOAD_SMOOTH;
        statusLoad = weight > 0 ? (float) (loadAvg / weight) : 0;
        Vector3d up = subLevel.logicalPose().transformNormal(new Vector3d(0, 1, 0), new Vector3d());
        double tilt = Math.acos(Math.max(-1, Math.min(1, up.y)));
        boolean candidate = isFoot() && tilt > OVERREACH_TILT
                && (ground != null ? rest >= reach() - 0.02 : ShipLegs.groundedCount(shipId) > 0);
        if (candidate == statusOverreach) overreachFlip = 0;
        else if (++overreachFlip >= OVERREACH_TICKS) {
            statusOverreach = candidate;
            overreachFlip = 0;
        }
    }

    private void clearStatus() {
        statusGrounded = false;
        statusOverreach = false;
        statusLoad = 0;
        loadAvg = 0;
        overreachFlip = 0;
    }

    /** Grounded right now (server), or as last synced (client). */
    public boolean statusGrounded() {
        return statusGrounded;
    }

    /** Share of the Ship's weight this Leg carries, smoothed (0.25 = a quarter). */
    public float statusLoad() {
        return statusLoad;
    }

    /** At maximum Reach (or unable to find Ground) while the Ship stays tilted, debounced by one second. */
    public boolean statusOverreach() {
        return statusOverreach;
    }

    /** Current Reach from the server: the Foot's Ground distance, or the maximum Reach. NaN without a Limb. */
    public float statusReach() {
        if (level() != null && level().isClientSide && !onShip && host.limb() != null) return (float) reach();
        return statusReach;
    }

    /** Server: whether the synced status changed enough to send it to clients again. */
    public boolean pollStatusSync() {
        float reachNow = Float.isNaN(statusReach) ? -1 : statusReach;
        boolean changed = sentGrounded != statusGrounded || sentOverreach != statusOverreach
                || Math.abs(sentLoad - statusLoad) > 0.02f || Math.abs(sentReach - reachNow) > 0.05f;
        if (!changed) return false;
        sentGrounded = statusGrounded;
        sentOverreach = statusOverreach;
        sentLoad = statusLoad;
        sentReach = reachNow;
        return true;
    }

    public void writeStatus(CompoundTag tag) {
        CompoundTag s = new CompoundTag();
        s.putBoolean("Grounded", statusGrounded);
        s.putBoolean("Overreach", statusOverreach);
        s.putFloat("Load", statusLoad);
        if (!Float.isNaN(statusReach)) s.putFloat("Reach", statusReach);
        tag.put(NBT_STATUS, s);
    }

    public void readStatus(CompoundTag tag) {
        CompoundTag s = tag.getCompound(NBT_STATUS);
        statusGrounded = s.getBoolean("Grounded");
        statusOverreach = s.getBoolean("Overreach");
        statusLoad = s.getFloat("Load");
        statusReach = s.contains("Reach") ? s.getFloat("Reach") : Float.NaN;
    }

    /** Client only (Ponder): the direction the knee bends toward, in block coordinates. */
    public void setDisplayKnee(double x, double y, double z) {
        kneeDir = new Vector3d(x, y, z).normalize();
    }

    /** Client only: draw the Limb at this Reach instead of probing for Ground (Ponder scenes). NaN = normal. */
    public void setDisplayReach(double reach) {
        displayReach = reach;
    }

    public GroundHit ground() {
        return ground;
    }

    public double lastNormalForce() {
        return lastNormalForce;
    }

    public boolean lastSlip() {
        return lastSlip;
    }

    public int lastWallHits() {
        return lastWallHits;
    }

    public double lastAnchorOffset() {
        return lastAnchorOffset;
    }

    double lastK() {
        return lastK;
    }

    public double usedFootDistance() {
        return ground != null ? ground.distance() : Double.isNaN(footExt) ? reach() : footExt;
    }

    public double plainCentreDistance(SubLevel self) {
        Direction f = facing();
        GroundHit h = castRay(self, mountFace(), new Vec3(f.getStepX(), f.getStepY(), f.getStepZ()), reach());
        return h == null ? reach() : h.distance();
    }

    public boolean footEmbedded(SubLevel self) {
        Direction f = facing();
        double d = Math.max(0, usedFootDistance() - 0.1);
        Vec3 plot = mountFace().add(f.getStepX() * d, f.getStepY() * d, f.getStepZ() * d);
        Vec3 world = self.logicalPose().transformPosition(plot);
        BlockPos bp = BlockPos.containing(world);
        var shape = level().getBlockState(bp).getCollisionShape(level(), bp);
        if (shape.isEmpty()) return false;
        for (AABB box : shape.toAabbs()) if (box.move(bp).contains(world)) return true;
        return false;
    }

    /** Physics sub-step on the server, from {@code sable$physicsTick}. */
    public void physicsTick(ServerSubLevel subLevel, double timeStep) {
        Limb limb = host.limb();
        boolean active = limb != null && host.legSettings().active() && shipId != null;
        Level level = level();
        if (active && isFoot() && ShipLegs.claimLeveling(shipId, level.getGameTime())) {
            LegLeveling.tick(subLevel, ShipLegs.feet(shipId), level.getGameTime());
        }
        lastNormalForce = 0;
        lastSlip = false;
        lastWallHits = 0;
        if (!active) return;
        GroundHit g = ground;

        Direction f = facing();
        Vector3d axisLocal = new Vector3d(-f.getStepX(), -f.getStepY(), -f.getStepZ());
        Pose3dc pose = subLevel.logicalPose();
        Vector3d axisWorld = pose.transformNormal(axisLocal, new Vector3d());
        double reach = g == null ? reach() : g.distance();
        Vec3 face = mountFace();
        Vector3d footPlot = new Vector3d(face.x - axisLocal.x * reach, face.y - axisLocal.y * reach, face.z - axisLocal.z * reach);
        Vector3d vFoot = Sable.HELPER.getVelocity(level, footPlot, new Vector3d());
        MassData mass = subLevel.getMassTracker();
        boolean foot = isFoot();
        int feet = Math.max(1, ShipLegs.footCount(shipId));
        if (foot) {
            for (Vector3d u : walls) {
                Vector3d uWorld = pose.transformNormal(u, new Vector3d());
                double into = vFoot.dot(uWorld) + WALL_BIAS;
                if (into <= 0) continue;
                double mLat = Math.min(1.0 / mass.getInverseNormalMass(footPlot, u), mass.getMass()) / feet;
                forceTotal.applyImpulseAtPoint(subLevel, footPlot, new Vector3d(u).mul(-into * mLat));
                ShipLegs.queue(this);
                lastWallHits++;
            }
            if (lastWallHits > 0) anchor = null;
        }

        if (g == null) {
            lastVelAxis = vFoot.dot(axisWorld);
            return;
        }

        double shipMass = mass.getMass();
        double gravity = DimensionPhysicsData.getGravity(level).length();
        double share = shipMass / feet;
        LegSettings settings = host.legSettings();
        double kFirm = share * gravity / LegworksConfig.REST_SAG.get() * limb.stiffness() * settings.stiffnessScale();
        double firmness = 1 - softening();
        double k = kFirm * firmness;
        double c = 2.0 * settings.dampingRatio() * Math.sqrt(kFirm * share);

        Vector3d vGround = new Vector3d();
        if (g.subLevel() != null) Sable.HELPER.getVelocity(level, new Vector3d(g.location().x, g.location().y, g.location().z), vGround);
        Vector3d vRel = new Vector3d(vFoot).sub(vGround);
        double velAxis = vRel.dot(axisWorld);

        double compression = rest - g.distance();
        double mEff = 1.0 / mass.getInverseNormalMass(footPlot, axisLocal);
        double springImpulse = k * compression * timeStep;
        if (g.distance() < BUMP_DIST) springImpulse += BUMP_STIFFNESS * kFirm * (BUMP_DIST - g.distance()) * timeStep;
        double dampImpulse = -c * velAxis * timeStep;
        double maxDamp = Math.min(mEff, share) * Math.abs(velAxis) / Math.max(1, ShipLegs.groundedCount(shipId));
        dampImpulse = Math.max(-maxDamp, Math.min(maxDamp, dampImpulse));
        double normalImpulse = Math.max(0.0, springImpulse + dampImpulse + (foot ? antiRollForce * firmness * timeStep : 0));

        Vector3d pushDir = pose.transformNormal(g.normalLocal(), new Vector3d()).normalize();
        Vector3d worldImpulse = pushDir.mul(normalImpulse);

        if (foot && normalImpulse > 0) {
            worldImpulse.add(gripImpulse(pose, mass, footPlot, vRel, g, normalImpulse, feet, timeStep));
        } else {
            anchor = null;
            lastAnchorOffset = 0;
        }

        lastNormalForce = normalImpulse / timeStep;
        lastVelAxis = velAxis;
        lastK = kFirm;

        if (worldImpulse.lengthSquared() > 0) {
            Vector3d localImpulse = pose.transformNormalInverse(worldImpulse, new Vector3d());
            forceTotal.applyImpulseAtPoint(subLevel, footPlot, localImpulse);
            ShipLegs.queue(this);

            if (g.subLevel() instanceof ServerSubLevel other && !other.isRemoved()) {
                RigidBodyHandle otherHandle = RigidBodyHandle.of(other);
                if (otherHandle != null && otherHandle.isValid()) {
                    Vector3d otherLocal = other.logicalPose().transformNormalInverse(new Vector3d(worldImpulse).negate(), new Vector3d());
                    otherHandle.applyImpulseAtPoint(new Vector3d(g.location().x, g.location().y, g.location().z), otherLocal);
                }
            }
        }
    }

    private Vector3d gripImpulse(Pose3dc pose, MassData mass, Vector3d footPlot, Vector3d vRel,
                                 GroundHit g, double normalImpulse, int feet, double timeStep) {
        Vector3d nWorld = pose.transformNormal(g.normalLocal(), new Vector3d()).normalize();
        Vector3d vT = new Vector3d(vRel).fma(-vRel.dot(nWorld), nWorld);

        Vector3d footWorld = pose.transformPosition(footPlot, new Vector3d());
        if (anchor == null) {
            anchor = toGroundFrame(footWorld, g.subLevel());
            anchorFrame = g.subLevel();
        }
        Vector3d anchorWorld = fromGroundFrame(anchor, g.subLevel());
        Vector3d offset = new Vector3d(footWorld).sub(anchorWorld);
        offset.fma(-offset.dot(nWorld), nWorld);

        Vector3d dir = vT.lengthSquared() > 1e-12 ? new Vector3d(vT).normalize() : (offset.lengthSquared() > 1e-12 ? new Vector3d(offset).normalize() : new Vector3d(1, 0, 0));
        Vector3d dirLocal = pose.transformNormalInverse(dir, new Vector3d());
        double mT = Math.min(1.0 / mass.getInverseNormalMass(footPlot, dirLocal), mass.getMass()) / feet;
        double omega = LegworksConfig.GRIP_OMEGA.get();
        double kT = mT * omega * omega;

        Vector3d impulse = new Vector3d(vT).mul(-GRIP_DAMP * mT).fma(-kT * timeStep, offset);
        double cap = LegworksConfig.GRIP_FRICTION.get() * normalImpulse;
        double mag = impulse.length();
        lastAnchorOffset = offset.length();
        if (mag > cap) {
            impulse.mul(cap / mag);
            anchor = toGroundFrame(footWorld, g.subLevel());
            lastSlip = true;
        }
        return impulse;
    }

    private static Vec3 toGroundFrame(Vector3d world, SubLevel frame) {
        Vec3 w = new Vec3(world.x, world.y, world.z);
        return frame == null ? w : frame.logicalPose().transformPositionInverse(w);
    }

    private static Vector3d fromGroundFrame(Vec3 p, SubLevel frame) {
        Vec3 w = frame == null ? p : frame.logicalPose().transformPosition(p);
        return new Vector3d(w.x, w.y, w.z);
    }

    void flushForces() {
        if (be().isRemoved()) return;
        if (!(Sable.HELPER.getContaining(be()) instanceof ServerSubLevel sub)) return;
        RigidBodyHandle handle = RigidBodyHandle.of(sub);
        if (handle == null || !handle.isValid()) return;
        handle.applyForcesAndReset(forceTotal);
    }

    /** Client tick: re-cast the Ground ray for the drawn Reach. */
    public void clientTick() {
        Limb limb = host.limb();
        SubLevel self = level() instanceof WrappedLevel ? null : Sable.HELPER.getContaining(be());
        onShip = self != null;
        if (limb == null) {
            clientReach = clientReachPrev = Double.NaN;
            collisionReach16 = 0;
            return;
        }
        double reach = reach();
        if (Double.isNaN(clientReach)) clientReach = reach;
        clientReachPrev = clientReach;
        double target = reach;
        if (!Double.isNaN(displayReach)) {
            target = Math.max(0.05, Math.min(reach, displayReach));
        } else if (self != null) {
            GroundHit g = castGround(self, reach, clientEverGrounded ? clientReach : Double.MAX_VALUE, null);
            if (g != null) {
                clientEverGrounded = true;
                target = g.distance();
            }
            updateKneeDir(self);
        }
        clientReach += (target - clientReach) * 0.5;
        collisionReach16 = (int) Math.round(target * 16);
    }

    public boolean isOnShip() {
        return onShip;
    }

    public int collisionReach16() {
        return collisionReach16;
    }

    private void updateKneeDir(SubLevel self) {
        var box = self.getPlot().getBoundingBox();
        BlockPos p = be().getBlockPos();
        Vector3d out = new Vector3d(p.getX() + 0.5 - (box.minX() + box.maxX() + 1) / 2.0,
                p.getY() + 0.5 - (box.minY() + box.maxY() + 1) / 2.0,
                p.getZ() + 0.5 - (box.minZ() + box.maxZ() + 1) / 2.0);
        Direction f = facing();
        Vector3d axis = new Vector3d(f.getStepX(), f.getStepY(), f.getStepZ());
        out.fma(-out.dot(axis), axis);
        if (out.lengthSquared() > 1e-4) kneeDir = out.normalize();
    }

    @Nullable
    public Vector3d kneeDir() {
        return kneeDir;
    }

    public double renderReach(float partialTick) {
        if (Double.isNaN(clientReach)) return reach();
        return clientReachPrev + (clientReach - clientReachPrev) * partialTick;
    }

    private void trackShip(ServerSubLevel subLevel) {
        UUID id = subLevel.getUniqueId();
        if (!id.equals(shipId)) {
            untrack();
            shipId = id;
        }
        if (isFoot()) ShipLegs.track(id, this);
        else ShipLegs.untrack(id, this);
    }

    private void untrack() {
        if (shipId != null) ShipLegs.untrack(shipId, this);
        shipId = null;
    }

    private void deactivate() {
        untrack();
        ShipLegs.dequeue(this);
        ground = null;
        anchor = null;
        anchorFrame = null;
        walls = List.of();
        antiRollForce = 0;
        lastTiltDisp = Double.NaN;
        probed = false;
    }

    /** Called on both sides when the fitted Limb changes (fit, removal, or a synced update). */
    public void onLimbChanged() {
        noteLost();
        deactivate();
        rest = Double.NaN;
        footExt = Double.NaN;
        everGrounded = false;
        lostTicks = 0;
        clientEverGrounded = false;
        clientReach = clientReachPrev = Double.NaN;
        refreshCollider();
    }

    public void onLoad() {
        refreshCollider();
    }

    /** A grounded Foot leaving its Ship (Limb removed or Mount broken) makes the other Legs Crouch briefly. */
    public void noteLost() {
        if (shipId != null && ground != null && isFoot() && level() != null) ShipLegs.noteLegLost(shipId, level().getGameTime());
    }

    public void onInvalidate() {
        deactivate();
        Level level = level();
        if (level != null && !(level instanceof WrappedLevel)) LegColliders.remove(level.isClientSide, pos());
    }

    private void refreshCollider() {
        Level level = level();
        if (level == null || level instanceof WrappedLevel) return;
        Limb limb = host.limb();
        if (limb == null) LegColliders.remove(level.isClientSide, pos());
        else LegColliders.add(level.isClientSide, pos(), limb.reach());
    }
}
