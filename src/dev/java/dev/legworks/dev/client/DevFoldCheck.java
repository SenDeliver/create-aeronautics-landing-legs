package dev.legworks.dev.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.legworks.LegworksIds;
import dev.legworks.client.LegRenderer;
import dev.legworks.client.LimbStyle;
import dev.legworks.leg.Leg;
import dev.legworks.limb.Limb;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Set;

/**
 * Dev-only: once per client start, checks the drawn Limb against its Mount for fold 0..1 in 0.05 steps, several Reaches
 * and knee yaws, using the renderer's own boxes: no point on any box edge (hip parts excluded) may enter the Mount
 * (|x|, |z| < 0.5 and y > -1/16) or rise above y = 15/16 (the block over the Mount). Logs FOLDCHECK ok|fail limb fold worst
 * for folds 0, 0.25, 0.5, 0.75, 1 and for every failing step (worst = deepest intrusion in blocks, <= 0 is clear).
 */
@EventBusSubscriber(modid = LegworksIds.MOD_ID, value = Dist.CLIENT)
public final class DevFoldCheck {
    private static final Logger LOG = LoggerFactory.getLogger("legworks-dev");
    private static final double FACE = -1 / 16.0;
    private static final double TOP = 15 / 16.0;
    private static final int EDGE_STEPS = 12;
    private static boolean done;

    private DevFoldCheck() {
    }

    @SubscribeEvent
    static void onTick(ClientTickEvent.Post event) {
        if (done) return;
        done = true;
        run();
    }

    public static boolean run() {
        boolean all = true;
        for (Object[] c : new Object[][]{{"short", Limb.SHORT, LimbStyle.SHORT}, {"long", Limb.LONG, LimbStyle.LONG}, {"heavy", Limb.HEAVY, LimbStyle.HEAVY}}) {
            Limb limb = (Limb) c[1];
            LimbStyle style = (LimbStyle) c[2];
            for (int step = 0; step <= 20; step++) {
                double fold = step / 20.0;
                double tucked = Math.min(Leg.TUCKED_REACH, limb.reach());
                double maxReach = tucked + (limb.reach() - tucked) * (1 - fold);
                double worst = -Double.MAX_VALUE;
                for (double reach : new double[]{Math.min(limb.minRest(), maxReach), Math.min(limb.nominalRest(), maxReach), maxReach}) {
                    for (int yawStep = 0; yawStep <= 8; yawStep++) {
                        double yaw = LegRenderer.foldYaw(Math.toRadians(yawStep * 11.25), fold);
                        worst = Math.max(worst, intrusion(limb, style, reach, fold, yaw));
                    }
                }
                boolean ok = worst <= 1e-6;
                all &= ok;
                if (step % 5 == 0 || !ok) LOG.info(String.format(Locale.ROOT, "FOLDCHECK %s %s %.2f %.4f", ok ? "ok" : "fail", c[0], fold, worst));
            }
        }
        LOG.info("FOLDCHECK {}", all ? "all ok" : "FAILED");
        return all;
    }

    private static double intrusion(Limb limb, LimbStyle style, double reach, double fold, double yaw) {
        PoseStack ps = new PoseStack();
        ps.mulPose(Axis.YP.rotation((float) yaw));
        Set<LimbStyle.Part> hip = Collections.newSetFromMap(new IdentityHashMap<>());
        hip.addAll(style.hip());
        double[] worst = {-Double.MAX_VALUE};
        LegRenderer.forEachBox(ps, limb, style, reach, fold, (pose, part, x0, x1, y0, y1, z0, z1) -> {
            if (hip.contains(part)) return;
            Matrix4f m = pose.pose();
            float[][] c = {{x0, y0, z0}, {x1, y0, z0}, {x1, y1, z0}, {x0, y1, z0}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}, {x0, y1, z1}};
            int[][] edges = {{0, 1}, {1, 2}, {2, 3}, {3, 0}, {4, 5}, {5, 6}, {6, 7}, {7, 4}, {0, 4}, {1, 5}, {2, 6}, {3, 7}};
            Vector3f v = new Vector3f();
            for (int[] e : edges) {
                for (int i = 0; i <= EDGE_STEPS; i++) {
                    float t = i / (float) EDGE_STEPS;
                    float[] a = c[e[0]], b = c[e[1]];
                    m.transformPosition(a[0] + (b[0] - a[0]) * t, a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t, v);
                    double inMount = Math.min(Math.min(0.5 - Math.abs(v.x), 0.5 - Math.abs(v.z)), v.y - FACE);
                    worst[0] = Math.max(worst[0], Math.max(inMount, v.y - TOP));
                }
            }
        });
        return worst[0];
    }
}
