package dev.legworks.dev.client;

import dev.legworks.LegworksIds;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenu;
import com.simibubi.create.content.contraptions.wrench.RadialWrenchMenuSubmitPacket;
import dev.ryanhcode.sable.Sable;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import dev.ryanhcode.sable.sublevel.SubLevel;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import net.minecraft.client.Screenshot;
import net.minecraft.server.MinecraftServer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Dev-only client script: -Plegworks.script=file, run once a world is joined (integrated or dedicated).
 * Lines: wait N | await name (until file run/signal/name exists) | cmd /x | shot name | hud on|off | quit. On a dedicated server, cmd needs op.
 * Player input: aimrel ax ay az dx dy dz face [ox oy oz] (look at a block given relative to the nearest Ship's plot corner, or to the anchor block)
 * | backend (log the Flywheel backend) | icons ids... (4x item icons on light grey) | click (real right-click on the looked-at block) | sneak on|off | radial (open Create's wrench menu) | radialset facing (submit it).
 */
@EventBusSubscriber(modid = LegworksIds.MOD_ID, value = Dist.CLIENT)
public final class DevScript {
    private static final Logger LOG = LoggerFactory.getLogger("legworks-script");
    private static Deque<String> lines;
    private static int waitTicks;
    private static BlockPos radialPos;

    private static void aim(Minecraft mc, String[] a) {
        Vec3 anchor = new Vec3(Double.parseDouble(a[0]), Double.parseDouble(a[1]), Double.parseDouble(a[2]));
        SubLevel ship = null;
        double best = Double.MAX_VALUE;
        for (SubLevel s : Sable.HELPER.getAllIntersecting(mc.level, new BoundingBox3d(anchor.x - 8, anchor.y - 8, anchor.z - 8, anchor.x + 8, anchor.y + 8, anchor.z + 8))) {
            double d = new Vector3d(s.logicalPose().position()).distance(anchor.x, anchor.y, anchor.z);
            if (d < best) {
                best = d;
                ship = s;
            }
        }
        BlockPos base;
        if (ship != null) {
            var box = ship.getPlot().getBoundingBox();
            base = new BlockPos(box.minX(), box.minY(), box.minZ());
        } else {
            base = BlockPos.containing(anchor);
        }
        BlockPos target = base.offset(Integer.parseInt(a[3]), Integer.parseInt(a[4]), Integer.parseInt(a[5]));
        Direction face = Direction.byName(a[6]);
        Vec3 local = Vec3.atCenterOf(target).add(face.getStepX() * 0.45, face.getStepY() * 0.45, face.getStepZ() * 0.45);
        if (a.length >= 10) local = local.add(Double.parseDouble(a[7]), Double.parseDouble(a[8]), Double.parseDouble(a[9]));
        Vec3 world = ship == null ? local : ship.logicalPose().transformPosition(local);
        Vec3 dir = world.subtract(mc.player.getEyePosition());
        float yaw = (float) Math.toDegrees(Math.atan2(-dir.x, dir.z));
        float pitch = (float) Math.toDegrees(-Math.atan2(dir.y, Math.hypot(dir.x, dir.z)));
        mc.player.setYRot(yaw);
        mc.player.setXRot(pitch);
        mc.player.yRotO = yaw;
        mc.player.xRotO = pitch;
        LOG.info("script aim {} -> {} yaw={} pitch={}", target.toShortString(), world, yaw, pitch);
    }

    private DevScript() {
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        String file = System.getProperty("legworks.script");
        if (file == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (lines == null) {
            try {
                lines = new ArrayDeque<>(Files.readAllLines(Path.of(file)));
            } catch (IOException e) {
                LOG.error("cannot read script {}", file, e);
                lines = new ArrayDeque<>();
            }
            waitTicks = 40;
            LOG.info("script start ({})", mc.getSingleplayerServer() != null ? "integrated" : "remote server");
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        while (!lines.isEmpty()) {
            String line = lines.poll().trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            LOG.info("script: {}", line);
            if (line.startsWith("await ")) {
                Path signal = mc.gameDirectory.toPath().resolve("signal").resolve(line.substring(6).trim());
                if (!Files.exists(signal)) {
                    lines.addFirst(line);
                    waitTicks = 5;
                    return;
                }
                waitTicks = 30;
                return;
            } else if (line.startsWith("wait ")) {
                waitTicks = Integer.parseInt(line.substring(5).trim());
                return;
            } else if (line.startsWith("cmd ")) {
                String command = line.substring(4).trim();
                MinecraftServer server = mc.getSingleplayerServer();
                if (server != null) {
                    server.execute(() -> server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command));
                } else {
                    mc.player.connection.sendCommand(command.startsWith("/") ? command.substring(1) : command);
                }
            } else if (line.startsWith("shot ")) {
                String name = line.substring(5).trim() + ".png";
                Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), msg -> LOG.info("screenshot {}: {}", name, msg.getString()));
                waitTicks = 2;
                return;
            } else if (line.equals("hud off")) {
                mc.options.hideGui = true;
            } else if (line.equals("hud on")) {
                mc.options.hideGui = false;
            } else if (line.startsWith("aimrel ")) {
                aim(mc, line.substring(7).trim().split("\\s+"));
                waitTicks = 3;
                return;
            } else if (line.equals("click")) {
                if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                    LOG.info("script click {} {} -> {}", hit.getBlockPos().toShortString(), hit.getDirection(),
                            mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit));
                    mc.player.swing(InteractionHand.MAIN_HAND);
                } else {
                    LOG.info("script click: nothing targeted ({})", mc.hitResult);
                }
                waitTicks = 4;
                return;
            } else if (line.startsWith("sneak ")) {
                mc.options.keyShift.setDown(line.endsWith("on"));
                waitTicks = 4;
                return;
            } else if (line.equals("radial")) {
                if (mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK) {
                    radialPos = hit.getBlockPos();
                    var menu = RadialWrenchMenu.tryCreateFor(mc.level.getBlockState(radialPos), radialPos, mc.level);
                    LOG.info("script radial {} -> {}", radialPos.toShortString(), menu.isPresent() ? "menu" : "none");
                    menu.ifPresent(mc::setScreen);
                }
                waitTicks = 10;
                return;
            } else if (line.startsWith("radialset ")) {
                Direction d = Direction.byName(line.substring(10).trim());
                if (radialPos != null && d != null) {
                    BlockState st = mc.level.getBlockState(radialPos);
                    CatnipServices.NETWORK.sendToServer(new RadialWrenchMenuSubmitPacket(radialPos, st.setValue(BlockStateProperties.FACING, d)));
                    LOG.info("script radialset {} {} (was {})", radialPos.toShortString(), d, st);
                }
                mc.setScreen(null);
                waitTicks = 10;
                return;
            } else if (line.startsWith("ponder ")) {
                String[] a = line.substring(7).trim().split("\\s+");
                DevScreens.ponder(a[0], a.length > 1 ? Integer.parseInt(a[1]) : 0);
                waitTicks = 10;
                return;
            } else if (line.startsWith("ponderseek ")) {
                DevScreens.ponderSeek(Integer.parseInt(line.substring(11).trim()));
                waitTicks = 4;
                return;
            } else if (line.startsWith("icons ")) {
                DevScreens.icons(java.util.List.of(line.substring(6).trim().split("\\s+")));
                waitTicks = 5;
                return;
            } else if (line.startsWith("tooltips ")) {
                DevScreens.tooltips(java.util.List.of(line.substring(9).trim().split("\\s+")));
                waitTicks = 5;
                return;
            } else if (line.startsWith("jei ")) {
                String[] a = line.substring(4).trim().split("\\s+");
                if (net.neoforged.fml.ModList.get().isLoaded("jei")) DevJei.open(DevScreens.stack(a[0]), a[1], a.length > 2 ? a[2] : null);
                else LOG.info("script jei: JEI not loaded");
                waitTicks = 10;
                return;
            } else if (line.startsWith("mouse ")) {
                String[] a = line.substring(6).trim().split("\\s+");
                double scale = mc.getWindow().getGuiScale();
                double x = Double.parseDouble(a[0]) * scale, y = Double.parseDouble(a[1]) * scale;
                org.lwjgl.glfw.GLFW.glfwSetCursorPos(mc.getWindow().getWindow(), x, y);
                try {
                    for (String f : new String[]{"xpos", "ypos"}) {
                        var field = net.minecraft.client.MouseHandler.class.getDeclaredField(f);
                        field.setAccessible(true);
                        field.setDouble(mc.mouseHandler, f.equals("xpos") ? x : y);
                    }
                } catch (ReflectiveOperationException e) {
                    LOG.info("script mouse: {}", e.toString());
                }
                waitTicks = 2;
                return;
            } else if (line.equals("board")) {
                DevScreens.valueBoard(mc);
                waitTicks = 6;
                return;
            } else if (line.equals("close")) {
                mc.setScreen(null);
                waitTicks = 4;
                return;
            } else if (line.startsWith("key ")) {
                com.mojang.blaze3d.platform.InputConstants.Key k = com.mojang.blaze3d.platform.InputConstants.getKey(line.substring(4).trim());
                net.minecraft.client.KeyMapping.click(k);
                waitTicks = 4;
                return;
            } else if (line.equals("backend")) {
                LOG.info("script backend {} visualization={}", dev.engine_room.flywheel.api.backend.BackendManager.currentBackend(),
                        dev.engine_room.flywheel.api.visualization.VisualizationManager.supportsVisualization(mc.level));
            } else if (line.equals("quit")) {
                mc.stop();
                return;
            }
        }
    }
}
