package dev.legworks.dev;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.legworks.leg.LegHost;
import dev.legworks.registry.LegworksItems;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.joml.Vector3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Dev-only test commands: /legworks mark|trace|push|load|limb.
 */
final class DevCommands {
    private static final Logger LOG = LoggerFactory.getLogger("legworks-dev");
    private static final List<Load> LOADS = new ArrayList<>();

    record Load(ServerSubLevel ship, Vec3 plotPoint, double mass, int[] ticksLeft) {}

    private DevCommands() {
    }

    static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("legworks").requires(s -> s.hasPermission(2))
                .then(Commands.literal("mark").then(Commands.argument("name", StringArgumentType.word()).executes(ctx -> {
                    LOG.info("MARK {}", StringArgumentType.getString(ctx, "name"));
                    return 1;
                })))
                .then(Commands.literal("trace").then(Commands.argument("on", IntegerArgumentType.integer(0, 1)).executes(ctx -> {
                    DevTrace.set(IntegerArgumentType.getInteger(ctx, "on") == 1);
                    return 1;
                })))
                .then(Commands.literal("push").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("impulse", Vec3Argument.vec3(false)).executes(DevCommands::push))))
                .then(Commands.literal("load").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("kg", DoubleArgumentType.doubleArg(0))
                        .then(Commands.argument("ticks", IntegerArgumentType.integer(1)).executes(DevCommands::load)))))
                .then(Commands.literal("limb").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("type", StringArgumentType.word())
                        .suggests((c, b) -> b.suggest("none").suggest("short").suggest("long").suggest("heavy").buildFuture())
                        .executes(DevCommands::limb))))
                .then(Commands.literal("use").then(Commands.argument("at", Vec3Argument.vec3()).then(Commands.argument("type", StringArgumentType.word())
                        .then(Commands.argument("sneak", IntegerArgumentType.integer(0, 1)).executes(DevCommands::use))))));
    }

    private static ServerSubLevel shipAt(ServerLevel level, Vec3 world, double radius) {
        ServerSubLevel best = null;
        double bestDist = Double.MAX_VALUE;
        for (SubLevel s : Sable.HELPER.getAllIntersecting(level, new BoundingBox3d(world.x - radius, world.y - radius, world.z - radius, world.x + radius, world.y + radius, world.z + radius))) {
            if (!(s instanceof ServerSubLevel ss)) continue;
            double d = new Vector3d(ss.logicalPose().position()).distance(world.x, world.y, world.z);
            if (d < bestDist) {
                bestDist = d;
                best = ss;
            }
        }
        return best;
    }

    private static int push(CommandContext<CommandSourceStack> ctx) {
        Vec3 at = Vec3Argument.getVec3(ctx, "at");
        Vec3 impulse = Vec3Argument.getVec3(ctx, "impulse");
        ServerSubLevel ship = shipAt(ctx.getSource().getLevel(), at, 3);
        if (ship == null) return 0;
        Vec3 plot = ship.logicalPose().transformPositionInverse(at);
        Vec3 local = ship.logicalPose().transformNormalInverse(impulse);
        RigidBodyHandle.of(ship).applyImpulseAtPoint(new Vector3d(plot.x, plot.y, plot.z), new Vector3d(local.x, local.y, local.z));
        LOG.info("PUSH ship={} at={} impulse={}", ship.getUniqueId().toString().substring(0, 8), at, impulse);
        return 1;
    }

    private static int load(CommandContext<CommandSourceStack> ctx) {
        Vec3 at = Vec3Argument.getVec3(ctx, "at");
        ServerSubLevel ship = shipAt(ctx.getSource().getLevel(), at, 3);
        if (ship == null) return 0;
        LOADS.add(new Load(ship, ship.logicalPose().transformPositionInverse(at), DoubleArgumentType.getDouble(ctx, "kg"),
                new int[]{IntegerArgumentType.getInteger(ctx, "ticks")}));
        LOG.info("LOAD ship={} at={} kg={}", ship.getUniqueId().toString().substring(0, 8), at, DoubleArgumentType.getDouble(ctx, "kg"));
        return 1;
    }

    private static int limb(CommandContext<CommandSourceStack> ctx) {
        Vec3 at = Vec3Argument.getVec3(ctx, "at");
        String type = StringArgumentType.getString(ctx, "type");
        ItemStack stack = switch (type) {
            case "short" -> LegworksItems.SHORT_LIMB.asStack();
            case "long" -> LegworksItems.LONG_LIMB.asStack();
            case "heavy" -> LegworksItems.HEAVY_LIMB.asStack();
            default -> ItemStack.EMPTY;
        };
        ServerLevel level = ctx.getSource().getLevel();
        ServerSubLevel ship = shipAt(level, at, 8);
        Vec3 local = ship == null ? at : ship.logicalPose().transformPositionInverse(at);
        LegHost host = nearestMount(level, local);
        if (host == null) {
            LOG.info("LIMB no mount near {}", at);
            return 0;
        }
        host.limbSlot().set(stack);
        host.onLimbSlotChanged();
        LOG.info("LIMB {} at {} -> {}", type, host.asBlockEntity().getBlockPos().toShortString(), stack.isEmpty() ? "none" : stack.getItem());
        return 1;
    }

    private static int use(CommandContext<CommandSourceStack> ctx) {
        try {
            return useUnchecked(ctx);
        } catch (RuntimeException e) {
            LOG.error("USE failed", e);
            return 0;
        }
    }

    private static int useUnchecked(CommandContext<CommandSourceStack> ctx) {
        Vec3 at = Vec3Argument.getVec3(ctx, "at");
        String type = StringArgumentType.getString(ctx, "type");
        ItemStack held = switch (type) {
            case "short" -> LegworksItems.SHORT_LIMB.asStack();
            case "long" -> LegworksItems.LONG_LIMB.asStack();
            case "heavy" -> LegworksItems.HEAVY_LIMB.asStack();
            default -> ItemStack.EMPTY;
        };
        ServerLevel level = ctx.getSource().getLevel();
        ServerSubLevel ship = shipAt(level, at, 8);
        Vec3 local = ship == null ? at : ship.logicalPose().transformPositionInverse(at);
        LegHost host = nearestMount(level, local);
        if (host == null) {
            LOG.info("USE no mount near {}", at);
            return 0;
        }
        BlockPos pos = host.asBlockEntity().getBlockPos();
        FakePlayer player = FakePlayerFactory.getMinecraft(level);
        player.getInventory().clearContent();
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        player.setShiftKeyDown(IntegerArgumentType.getInteger(ctx, "sneak") == 1);
        BlockState state = level.getBlockState(pos);
        BlockHitResult hit = new BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.DOWN, pos, false);
        String result = held.isEmpty()
                ? state.useWithoutItem(level, player, hit).toString()
                : state.useItemOn(held, level, player, InteractionHand.MAIN_HAND, hit).toString();
        StringBuilder inv = new StringBuilder();
        for (ItemStack s : player.getInventory().items) if (!s.isEmpty()) inv.append(s.getCount()).append("x").append(s.getItem()).append(' ');
        LOG.info("USE {} sneak={} at {} -> {} | fitted={} | hand={} | inventory={}", type, player.isShiftKeyDown(), pos.toShortString(), result,
                host.limbSlot().isEmpty() ? "none" : host.limbSlot().stack().getItem(), player.getMainHandItem(), inv);
        player.setShiftKeyDown(false);
        player.getInventory().clearContent();
        return 1;
    }

    private static LegHost nearestMount(ServerLevel level, Vec3 p) {
        LegHost best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos c = BlockPos.containing(p);
        for (BlockPos bp : BlockPos.betweenClosed(c.offset(-3, -3, -3), c.offset(3, 3, 3))) {
            if (!(level.getBlockEntity(bp) instanceof LegHost host)) continue;
            double d = bp.getCenter().distanceToSqr(p);
            if (d < bestDist) {
                bestDist = d;
                best = host;
            }
        }
        return best;
    }

    static void tickLoads(double dt) {
        LOADS.removeIf(l -> l.ship().isRemoved() || l.ticksLeft()[0] <= 0);
        for (Load l : LOADS) {
            RigidBodyHandle h = RigidBodyHandle.of(l.ship());
            if (h == null || !h.isValid()) continue;
            Vec3 local = l.ship().logicalPose().transformNormalInverse(new Vec3(0, -l.mass() * 11.0 * dt, 0));
            h.applyImpulseAtPoint(new Vector3d(l.plotPoint().x, l.plotPoint().y, l.plotPoint().z), new Vector3d(local.x, local.y, local.z));
        }
    }

    static void serverTick() {
        for (Load l : LOADS) l.ticksLeft()[0]--;
    }

    static void clear() {
        LOADS.clear();
        DevTrace.set(false);
    }
}
