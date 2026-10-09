package dev.legworks.dev;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import dev.legworks.content.controller.LegControllerBlockEntity;
import dev.legworks.content.hydraulic.HydraulicLegMountBlockEntity;
import dev.legworks.leg.Leg;
import dev.legworks.leg.LegHost;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.companion.math.Pose3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import dev.simulated_team.simulated.util.SimAssemblyHelper;
import dev.simulated_team.simulated.util.SimMathUtils;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Dev-only phase-2 commands: /legworks shipset|rpm|hold|click|status|watch|hover|disassemble|ride.
 * Positions are world points; on a Ship they are mapped into its plot.
 */
final class DevHydraulic {
    private static final Logger LOG = LoggerFactory.getLogger("legworks-dev");
    private static final GameProfile PROFILE = new GameProfile(UUID.fromString("6c6567f7-0000-4000-8000-000000000002"), "[LegworksDev]");
    private static ItemStack held = ItemStack.EMPTY;
    private static final List<Watch> WATCHES = new ArrayList<>();
    private static final List<Hover> HOVERS = new ArrayList<>();

    record Watch(ServerLevel level, Vec3 at, String tag, int[] ticksLeft) {}

    record Hover(ServerSubLevel ship, double y0, int[] ticksLeft) {}

    private DevHydraulic() {
    }

    static void register(com.mojang.brigadier.CommandDispatcher<CommandSourceStack> d, CommandBuildContext build) {
        d.register(Commands.literal("legworks").requires(s -> s.hasPermission(2))
                .then(Commands.literal("shipset").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("block", BlockStateArgument.block(build)).executes(DevHydraulic::shipset))))
                .then(Commands.literal("shipsetrel").then(Commands.argument("anchor", Vec3Argument.vec3()).then(Commands.argument("dx", IntegerArgumentType.integer())
                        .then(Commands.argument("dy", IntegerArgumentType.integer()).then(Commands.argument("dz", IntegerArgumentType.integer())
                                .then(Commands.argument("block", BlockStateArgument.block(build)).executes(DevHydraulic::shipsetRel)))))))
                .then(Commands.literal("rpm").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("rpm", IntegerArgumentType.integer(-256, 256)).executes(DevHydraulic::rpm))))
                .then(Commands.literal("hold").then(Commands.argument("item", StringArgumentType.greedyString()).executes(DevHydraulic::hold)))
                .then(Commands.literal("click").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("sneak", IntegerArgumentType.integer(0, 1))
                        .then(Commands.argument("face", StringArgumentType.word()).executes(DevHydraulic::click)))))
                .then(Commands.literal("clickrel").then(Commands.argument("anchor", Vec3Argument.vec3()).then(Commands.argument("dx", IntegerArgumentType.integer())
                        .then(Commands.argument("dy", IntegerArgumentType.integer()).then(Commands.argument("dz", IntegerArgumentType.integer())
                                .then(Commands.argument("sneak", IntegerArgumentType.integer(0, 1)).then(Commands.argument("face", StringArgumentType.word()).executes(DevHydraulic::clickRel))))))))
                .then(Commands.literal("statusw").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("tag", StringArgumentType.word()).executes(ctx -> {
                    statusWorld(ctx.getSource().getLevel(), Vec3Argument.getVec3(ctx, "at"), StringArgumentType.getString(ctx, "tag"));
                    return 1;
                }))))
                .then(Commands.literal("status").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("tag", StringArgumentType.word()).executes(ctx -> {
                    status(ctx.getSource().getLevel(), Vec3Argument.getVec3(ctx, "at"), StringArgumentType.getString(ctx, "tag"));
                    return 1;
                }))))
                .then(Commands.literal("watch").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("tag", StringArgumentType.word())
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1)).executes(ctx -> {
                            WATCHES.add(new Watch(ctx.getSource().getLevel(), Vec3Argument.getVec3(ctx, "at"), StringArgumentType.getString(ctx, "tag"), new int[]{IntegerArgumentType.getInteger(ctx, "ticks")}));
                            return 1;
                        })))))
                .then(Commands.literal("hover").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("ticks", IntegerArgumentType.integer(0)).executes(DevHydraulic::hover))))
                .then(Commands.literal("ride").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("tenths", IntegerArgumentType.integer(0, 60)).executes(DevHydraulic::ride))))
                .then(Commands.literal("disassemble").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("extraTurns", IntegerArgumentType.integer(0, 3)).executes(DevHydraulic::disassemble)))));
    }

    static ServerSubLevel shipAt(ServerLevel level, Vec3 world, double radius) {
        ServerSubLevel best = null;
        double bestDist = Double.MAX_VALUE;
        for (SubLevel s : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(world.x - radius, world.y - radius, world.z - radius, world.x + radius, world.y + radius, world.z + radius))) {
            if (!(s instanceof ServerSubLevel ss)) continue;
            double dd = new Vector3d(ss.logicalPose().position()).distance(world.x, world.y, world.z);
            if (dd < bestDist) {
                bestDist = dd;
                best = ss;
            }
        }
        return best;
    }

    static BlockPos plotPos(ServerLevel level, Vec3 at) {
        ServerSubLevel ship = shipAt(level, at, 8);
        return BlockPos.containing(ship == null ? at : ship.logicalPose().transformPositionInverse(at));
    }

    static void statusWorld(ServerLevel level, Vec3 at, String tag) {
        statusIn(level, null, at, tag);
    }

    private static void forEachBE(ServerLevel level, Vec3 at, Consumer<BlockEntity> out) {
        forEachBE(level, shipAt(level, at, 8), at, out);
    }

    private static void forEachBE(ServerLevel level, ServerSubLevel ship, Vec3 at, Consumer<BlockEntity> out) {
        BlockPos min, max;
        if (ship != null) {
            var box = ship.getPlot().getBoundingBox();
            min = new BlockPos(box.minX(), box.minY(), box.minZ());
            max = new BlockPos(box.maxX(), box.maxY(), box.maxZ());
        } else {
            BlockPos c = BlockPos.containing(at);
            min = c.offset(-8, -8, -8);
            max = c.offset(8, 8, 8);
        }
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            BlockEntity be = level.getBlockEntity(p);
            if (be != null) out.accept(be);
        }
    }

    private static int shipset(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        return setAt(ctx, level, plotPos(level, Vec3Argument.getVec3(ctx, "at")));
    }

    private static BlockPos relPos(CommandContext<CommandSourceStack> ctx, ServerLevel level) {
        Vec3 anchor = Vec3Argument.getVec3(ctx, "anchor");
        ServerSubLevel ship = shipAt(level, anchor, 8);
        BlockPos base;
        if (ship != null) {
            var box = ship.getPlot().getBoundingBox();
            base = new BlockPos(box.minX(), box.minY(), box.minZ());
        } else {
            base = BlockPos.containing(anchor);
        }
        return base.offset(IntegerArgumentType.getInteger(ctx, "dx"), IntegerArgumentType.getInteger(ctx, "dy"), IntegerArgumentType.getInteger(ctx, "dz"));
    }

    private static int shipsetRel(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        return setAt(ctx, level, relPos(ctx, level));
    }

    private static int setAt(CommandContext<CommandSourceStack> ctx, ServerLevel level, BlockPos pos) {
        BlockInput input = BlockStateArgument.getBlock(ctx, "block");
        boolean ok = input.place(level, pos, 3);
        if (ok) level.blockUpdated(pos, input.getState().getBlock());
        LOG.info("SHIPSET {} -> {} ok={}", pos.toShortString(), input.getState(), ok);
        return ok ? 1 : 0;
    }

    private static int rpm(CommandContext<CommandSourceStack> ctx) {
        int rpm = IntegerArgumentType.getInteger(ctx, "rpm");
        int[] n = {0};
        forEachBE(ctx.getSource().getLevel(), Vec3Argument.getVec3(ctx, "at"), be -> {
            if (be instanceof CreativeMotorBlockEntity m) {
                m.generatedSpeed.setValue(rpm);
                n[0]++;
            }
        });
        LOG.info("RPM {} on {} motors", rpm, n[0]);
        return n[0];
    }

    private static int hold(CommandContext<CommandSourceStack> ctx) {
        var item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(StringArgumentType.getString(ctx, "item")));
        held = new ItemStack(item, 64);
        LOG.info("HOLD {}", held.getItem());
        return 1;
    }

    private static int click(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        return clickAt(ctx, level, plotPos(level, Vec3Argument.getVec3(ctx, "at")));
    }

    private static int clickRel(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        return clickAt(ctx, level, relPos(ctx, level));
    }

    private static int clickAt(CommandContext<CommandSourceStack> ctx, ServerLevel level, BlockPos pos) {
        Direction face = Direction.byName(StringArgumentType.getString(ctx, "face"));
        if (face == null) face = Direction.UP;
        FakePlayer player = new FakePlayer(level, PROFILE) {
            @Override
            public void displayClientMessage(Component message, boolean actionBar) {
                LOG.info("MSG {}", message.getString());
            }
        };
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.setShiftKeyDown(IntegerArgumentType.getInteger(ctx, "sneak") == 1);
        Vec3 hitLoc = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
        var result = player.gameMode.useItemOn(player, level, held, InteractionHand.MAIN_HAND, new BlockHitResult(hitLoc, face, pos, false));
        held = player.getMainHandItem();
        LOG.info("CLICK {} sneak={} face={} state={} -> {}", pos.toShortString(), player.isShiftKeyDown(), face, level.getBlockState(pos), result);
        return 1;
    }

    static void status(ServerLevel level, Vec3 at, String tag) {
        statusIn(level, shipAt(level, at, 8), at, tag);
    }

    private static void statusIn(ServerLevel level, ServerSubLevel ship, Vec3 at, String tag) {
        long t = level.getGameTime();
        if (ship != null) {
            Pose3dc pose = ship.logicalPose();
            Vector3d up = pose.transformNormal(new Vector3d(0, 1, 0));
            double tilt = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, up.y))));
            Vector3d v = Sable.HELPER.getVelocity(level, new Vector3d(pose.rotationPoint()), new Vector3d());
            LOG.info("STATUS tag={} t={} kind=ship id={} pos={},{},{} tilt={} vy={}", tag, t, ship.getUniqueId().toString().substring(0, 8),
                    f(pose.position().x()), f(pose.position().y()), f(pose.position().z()), f(tilt), f(v.y));
        } else {
            LOG.info("STATUS tag={} t={} kind=noship", tag, t);
        }
        forEachBE(level, ship, at, be -> {
            BlockPos p = be.getBlockPos();
            if (be instanceof LegHost h && h.limb() != null) {
                Leg l = h.leg();
                LOG.info("P3 tag={} kind=leg pos={} grounded={} load={} over={} reach={} line=\"{}\"", tag, p.toShortString(), l.statusGrounded() ? 1 : 0,
                        f(l.statusLoad()), l.statusOverreach() ? 1 : 0, f(l.statusReach()), dev.legworks.content.LegStatusText.mountLine(h).getString());
            } else if (be instanceof LegControllerBlockEntity c) {
                LOG.info("P3 tag={} kind=ctrl pos={} grounded={} links={} over={} line=\"{}\"", tag, p.toShortString(), c.groundedCount(), c.linkCount(),
                        c.hasOverreach() ? 1 : 0, dev.legworks.content.LegStatusText.controllerLine(c).getString());
            }
            if (be instanceof HydraulicLegMountBlockEntity m) {
                Leg leg = m.leg();
                BlockPos c = m.controllerPos();
                LOG.info("STATUS tag={} t={} kind=hmount pos={} facing={} linked={} ctrl={} fold={} soft={} rpm={} top={} rest={} force={} grounded={} stiff={} damp={} impact={} stress={} over={}",
                        tag, t, p.toShortString(), m.facing().getName(), m.isLinked() ? 1 : 0, c == null ? "none" : c.subtract(p).toShortString().replace(" ", ""),
                        f(m.fold()), f(m.softening()), f(m.getSpeed()), m.topSignal(), f(leg.rest()), f(leg.lastNormalForce()), leg.ground() == null ? 0 : 1,
                        f(m.legSettings().stiffnessScale()), f(m.legSettings().dampingRatio()),
                        f(com.simibubi.create.api.stress.BlockStressValues.getImpact(m.getBlockState().getBlock())),
                        f(m.calculateStressApplied() * Math.abs(m.getTheoreticalSpeed())), m.isOverStressed() ? 1 : 0);
            } else if (be instanceof LegHost h) {
                Leg leg = h.leg();
                LOG.info("STATUS tag={} t={} kind=smount pos={} facing={} rest={} force={} grounded={}", tag, t, p.toShortString(), leg.facing().getName(), f(leg.rest()), f(leg.lastNormalForce()), leg.ground() == null ? 0 : 1);
            } else if (be instanceof LegControllerBlockEntity c) {
                LOG.info("STATUS tag={} t={} kind=ctrl pos={} links={} ride={} gear={} conflict={} offsets={}", tag, t, p.toShortString(), c.linkCount(),
                        f(c.rideHeight()), c.isGearUp() ? "up" : "down", c.hasConflict() ? 1 : 0,
                        c.links().stream().map(o -> o.getX() + "," + o.getY() + "," + o.getZ()).toList().toString().replace(" ", ""));
            }
        });
    }

    private static String f(double v) {
        return String.format("%.3f", v);
    }

    private static int hover(CommandContext<CommandSourceStack> ctx) {
        ServerSubLevel ship = shipAt(ctx.getSource().getLevel(), Vec3Argument.getVec3(ctx, "at"), 8);
        if (ship == null) return 0;
        int ticks = IntegerArgumentType.getInteger(ctx, "ticks");
        HOVERS.removeIf(h -> h.ship() == ship);
        if (ticks > 0) HOVERS.add(new Hover(ship, ship.logicalPose().position().y(), new int[]{ticks}));
        LOG.info("HOVER ship={} ticks={}", ship.getUniqueId().toString().substring(0, 8), ticks);
        return 1;
    }

    private static int ride(CommandContext<CommandSourceStack> ctx) {
        int tenths = IntegerArgumentType.getInteger(ctx, "tenths");
        int[] n = {0};
        forEachBE(ctx.getSource().getLevel(), Vec3Argument.getVec3(ctx, "at"), be -> {
            if (be instanceof LegControllerBlockEntity c) {
                c.setRideHeightSetting(tenths);
                n[0]++;
            }
        });
        LOG.info("RIDE {} on {} controllers", tenths, n[0]);
        return n[0];
    }

    private static int disassemble(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        ServerSubLevel ship = shipAt(level, Vec3Argument.getVec3(ctx, "at"), 8);
        if (ship == null) return 0;
        var box = ship.getPlot().getBoundingBox();
        BlockPos plotAnchor = new BlockPos((box.minX() + box.maxX()) / 2, box.minY(), (box.minZ() + box.maxZ()) / 2);
        Pose3dc pose = ship.logicalPose();
        BlockPos worldAnchor = BlockPos.containing(pose.transformPosition(Vec3.atCenterOf(plotAnchor)));
        int q = -Mth.floor(SimMathUtils.getClosestYaw(new Quaterniond(pose.orientation())) / (Math.PI / 2) + 0.5) + IntegerArgumentType.getInteger(ctx, "extraTurns");
        var rot = SimAssemblyHelper.rotationFrom90DegRots(q);
        SimAssemblyHelper.disassembleSubLevel(level, ship, plotAnchor, worldAnchor, rot, false);
        LOG.info("DISASSEMBLE ship={} anchor={} -> {} rot={}", ship.getUniqueId().toString().substring(0, 8), plotAnchor.toShortString(), worldAnchor.toShortString(), rot);
        return 1;
    }

    static void tickPhysics(double dt) {
        HOVERS.removeIf(h -> h.ship().isRemoved() || h.ticksLeft()[0] <= 0);
        for (Hover h : HOVERS) {
            RigidBodyHandle handle = RigidBodyHandle.of(h.ship());
            if (handle == null || !handle.isValid()) continue;
            Pose3dc pose = h.ship().logicalPose();
            double m = h.ship().getMassTracker().getMass();
            Vector3d com = new Vector3d(pose.rotationPoint());
            Vector3d v = Sable.HELPER.getVelocity(h.ship().getLevel(), com, new Vector3d());
            Vector3d world = new Vector3d(0, m * (-v.y + 4.0 * (h.y0() - pose.position().y())), 0);
            Vector3d local = pose.transformNormalInverse(world, new Vector3d());
            handle.applyImpulseAtPoint(com, local);
        }
    }

    static void serverTick() {
        for (Hover h : HOVERS) h.ticksLeft()[0]--;
        WATCHES.removeIf(w -> w.ticksLeft()[0] <= 0);
        for (Watch w : WATCHES) {
            status(w.level(), w.at(), w.tag());
            w.ticksLeft()[0]--;
        }
    }

    static void clear() {
        WATCHES.clear();
        HOVERS.clear();
        held = ItemStack.EMPTY;
    }
}
