package dev.legworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.legworks.leg.Leg;
import dev.legworks.leg.LegHost;
import dev.legworks.limb.Limb;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import org.joml.Quaternionf;
import org.joml.Vector2d;
import org.joml.Vector3f;

import java.util.List;
import java.util.function.Function;

/**
 * Draws the fitted Limb below a Leg Mount: hip, upper segment, knee, lower segment and Foot. Nothing without a Limb.
 */
public class LegRenderer<T extends BlockEntity & LegHost> implements BlockEntityRenderer<T> {
    private static final double BEND_FOLDED = 70;
    private static final double BEND_STRAIGHT = 10;
    private static final double FOLD_PIVOT_DROP = 1 / 16.0;
    private static final double FOLD_CLEARANCE = 1 / 16.0;
    private static final double MOUNT_HALF = 0.5;
    private static final double HULL_LIMIT = 15 / 16.0;

    /**
     * Leg-space layout of a drawn Limb: hip pivot height, knee, segment angles (about Z, from straight down) and lengths,
     * Foot anchor (pad bottom centre) and Foot tilt (about Z; folded = sole facing outward).
     */
    public record Geometry(double pivotY, Vector2d knee, float upperAngle, float upperLen, float lowerAngle, float lowerLen, Vector2d foot, float footAngle) {
    }

    /** Receives every drawn box: the pose it is drawn in, its Part, and its bounds in that pose (blocks). */
    public interface BoxSink {
        void accept(PoseStack.Pose pose, LimbStyle.Part part, float x0, float x1, float y0, float y1, float z0, float z1);
    }

    public LegRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(T be, float partialTick, PoseStack ps, MultiBufferSource buffers, int light, int overlay) {
        Limb limb = be.limb();
        if (limb == null) return;
        Leg leg = be.leg();
        LimbStyle style = LimbStyle.of(be.limbSlot().stack());
        Direction f = leg.facing();
        Quaternionf toFacing = new Quaternionf().rotationTo(new Vector3f(0, -1, 0), new Vector3f(f.getStepX(), f.getStepY(), f.getStepZ()));
        double fold = be.legSettings().fold();
        ps.pushPose();
        ps.translate(0.5, 0.5, 0.5);
        ps.mulPose(toFacing);
        ps.translate(0, -0.5, 0);
        double yaw = 0;
        if (leg.kneeDir() != null) {
            Vector3f k = new Vector3f((float) leg.kneeDir().x, (float) leg.kneeDir().y, (float) leg.kneeDir().z);
            new Quaternionf(toFacing).conjugate().transform(k);
            yaw = -Math.atan2(k.z, k.x);
        }
        ps.mulPose(Axis.YP.rotation((float) foldYaw(yaw, fold)));
        drawLimb(ps, buffers, limb, style, leg.renderReach(partialTick), fold, light);
        ps.popPose();
    }

    private static double smooth(double t) {
        t = Math.max(0, Math.min(1, t));
        return t * t * (3 - 2 * t);
    }

    /** Fold progress of the knee, hip and yaw (done by half-way) and of the lower strut swing (whole fold). */
    static double kneeStage(double fold) {
        return smooth(fold * 2);
    }

    static double swingStage(double fold) {
        return smooth(fold);
    }

    /** While folding, the knee turns to the nearest Mount side so the folded Leg lies flat beside it. */
    public static double foldYaw(double yaw, double fold) {
        double snapped = Math.round(yaw / (Math.PI / 2)) * (Math.PI / 2);
        return yaw + (snapped - yaw) * kneeStage(fold);
    }

    private static double maxRadius(List<LimbStyle.Part> parts) {
        double r = 0;
        for (LimbStyle.Part p : parts) {
            double x = Math.abs(p.xOff()) / 16.0 + p.w() / 32.0;
            double y = Math.max(Math.abs(p.lenFrom()), Math.abs(p.lenTo())) / 16.0;
            r = Math.max(r, Math.hypot(x, y));
        }
        return r;
    }

    private static double halfWidth(List<LimbStyle.Part> parts) {
        return parts.stream().mapToDouble(p -> Math.abs(p.xOff()) / 16.0 + p.w() / 32.0).max().orElse(0);
    }

    private static double ankleOffset(LimbStyle style) {
        return style.foot().isEmpty() ? 0 : (style.foot().getFirst().lenFrom() + style.foot().getFirst().lenTo()) / 32.0;
    }

    /**
     * Gear Up pose: upper strut out under the Mount face, knee just past the Mount side, lower strut folded straight up
     * beside the Mount and the Foot pad standing on edge with its sole outward, all below the Mount's top.
     */
    static Geometry foldedGeometry(LimbStyle style) {
        double pivotY = -style.hipDrop() - FOLD_PIVOT_DROP;
        double kneeX = MOUNT_HALF + halfWidth(style.lower()) + FOLD_CLEARANCE;
        double kneeY = -FOLD_CLEARANCE - maxRadius(style.knee());
        double footHalf = style.foot().stream().mapToDouble(p -> Math.abs(p.xOff()) / 16.0 + p.w() / 32.0).max().orElse(0);
        double ankleY = HULL_LIMIT - footHalf - FOLD_CLEARANCE;
        double ankleX = kneeX;
        return new Geometry(pivotY, new Vector2d(kneeX, kneeY),
                (float) Math.atan2(kneeX, -(kneeY - pivotY)), (float) Math.hypot(kneeX, kneeY - pivotY),
                (float) Math.PI, (float) (ankleY - kneeY), new Vector2d(ankleX + ankleOffset(style), ankleY), (float) (Math.PI / 2));
    }

    static Geometry stanceGeometry(Limb limb, LimbStyle style, double reach) {
        reach = Math.max(0.05, reach);
        double half = Math.max(0.05, reach - style.hipDrop()) / 2.0;
        double extension = Math.min(1.0, reach / limb.reach());
        double bend = Math.toRadians(BEND_FOLDED + (BEND_STRAIGHT - BEND_FOLDED) * extension);
        Vector2d knee = new Vector2d(half * Math.tan(bend), -half - style.hipDrop());
        double pivotY = -style.hipDrop();
        double ankleY = -reach + ankleOffset(style);
        return new Geometry(pivotY, knee,
                (float) Math.atan2(knee.x, -(knee.y - pivotY)), (float) Math.hypot(knee.x, knee.y - pivotY),
                (float) Math.atan2(-knee.x, -(ankleY - knee.y)), (float) Math.hypot(knee.x, ankleY - knee.y), new Vector2d(0, -reach), 0);
    }

    public static Geometry geometry(Limb limb, LimbStyle style, double reach, double fold) {
        fold = Math.max(0, Math.min(1, fold));
        Geometry stance = stanceGeometry(limb, style, reach);
        if (fold <= 0) return stance;
        Geometry folded = foldedGeometry(style);
        double a = kneeStage(fold), b = swingStage(fold);
        double pivotY = stance.pivotY() + (folded.pivotY() - stance.pivotY()) * a;
        Vector2d knee = new Vector2d(stance.knee()).lerp(folded.knee(), a);
        double lowerAngle = stance.lowerAngle() + (folded.lowerAngle() - stance.lowerAngle()) * b;
        double lowerLen = stance.lowerLen() + (folded.lowerLen() - stance.lowerLen()) * b;
        double footAngle = folded.footAngle() * b;
        double ankleX = knee.x + lowerLen * Math.sin(lowerAngle), ankleY = knee.y - lowerLen * Math.cos(lowerAngle);
        double c = ankleOffset(style);
        Vector2d foot = new Vector2d(ankleX + c * Math.sin(footAngle), ankleY - c * Math.cos(footAngle));
        return new Geometry(pivotY, knee,
                (float) Math.atan2(knee.x, -(knee.y - pivotY)), (float) Math.hypot(knee.x, knee.y - pivotY),
                (float) lowerAngle, (float) lowerLen, foot, (float) footAngle);
    }

    /** Every box of a drawn Limb in Leg space: origin = centre of the Mount's Limb face, -Y along the Limb, knee bends toward +X. */
    public static void forEachBox(PoseStack ps, Limb limb, LimbStyle style, double reach, double fold, BoxSink sink) {
        Geometry g = geometry(limb, style, reach, fold);
        Vector2d knee = g.knee();
        float upperAngle = g.upperAngle(), lowerAngle = g.lowerAngle();

        joints(ps, sink, style.hip());

        ps.pushPose();
        ps.translate(0, g.pivotY(), 0);
        ps.mulPose(Axis.ZP.rotation(upperAngle));
        segment(ps, sink, style.upper(), g.upperLen());
        ps.popPose();

        ps.pushPose();
        ps.translate(knee.x, knee.y, 0);
        ps.pushPose();
        ps.mulPose(Axis.ZP.rotation((upperAngle + lowerAngle) / 2));
        joints(ps, sink, style.knee());
        ps.popPose();
        ps.mulPose(Axis.ZP.rotation(lowerAngle));
        segment(ps, sink, style.lower(), g.lowerLen());
        ps.popPose();

        ps.pushPose();
        ps.translate(g.foot().x, g.foot().y, 0);
        ps.mulPose(Axis.ZP.rotation(g.footAngle()));
        joints(ps, sink, style.foot());
        ps.popPose();
    }

    public static void drawLimb(PoseStack ps, MultiBufferSource buffers, Limb limb, LimbStyle style, double reach, double fold, int light) {
        Function<ResourceLocation, TextureAtlasSprite> atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        VertexConsumer vc = buffers.getBuffer(RenderType.cutout());
        forEachBox(ps, limb, style, reach, fold, (pose, p, x0, x1, y0, y1, z0, z1) ->
                box(pose, vc, atlas.apply(p.tex()), light, x0, x1, y0, y1, z0, z1, p.tileV(), p.decal()));
    }

    private static void segment(PoseStack ps, BoxSink sink, List<LimbStyle.Part> parts, float len) {
        for (LimbStyle.Part p : parts) {
            float x = p.xOff() / 16f, z = p.zOff() / 16f;
            sink.accept(ps.last(), p, x - p.w() / 32f, x + p.w() / 32f, -p.lenTo() * len, -p.lenFrom() * len, z - p.d() / 32f, z + p.d() / 32f);
        }
    }

    private static void joints(PoseStack ps, BoxSink sink, List<LimbStyle.Part> parts) {
        for (LimbStyle.Part p : parts) {
            float x = p.xOff() / 16f, z = p.zOff() / 16f;
            sink.accept(ps.last(), p, x - p.w() / 32f, x + p.w() / 32f, p.lenTo() / 16f, p.lenFrom() / 16f, z - p.d() / 32f, z + p.d() / 32f);
        }
    }

    /**
     * An axis-aligned box at 1 texel = 1/16 block. U is centred on the texture; tileV repeats V every block from the top,
     * otherwise V is centred too. A decal box draws only its two Z faces, each with the whole texture (a thin disc).
     */
    private static void box(PoseStack.Pose p, VertexConsumer vc, TextureAtlasSprite s, int light,
                            float x0, float x1, float y0, float y1, float z0, float z1, boolean tileV, boolean decal) {
        float w = (x1 - x0) * 16, d = (z1 - z0) * 16;
        if (decal) {
            quad(vc, p, s, light, 0, 0, -1, 0, 16, 0, 16, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0);
            quad(vc, p, s, light, 0, 0, 1, 0, 16, 0, 16, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1);
            return;
        }
        quad(vc, p, s, light, 0, 1, 0, 8 - w / 2, 8 + w / 2, 8 - d / 2, 8 + d / 2, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1);
        quad(vc, p, s, light, 0, -1, 0, 8 - w / 2, 8 + w / 2, 8 - d / 2, 8 + d / 2, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0);
        float top = y1;
        while (top > y0 + 1e-4f) {
            float bottom = tileV ? Math.max(y0, top - 1) : y0;
            float h = (top - bottom) * 16;
            float v0 = tileV ? 0 : 8 - h / 2, v1 = tileV ? h : 8 + h / 2;
            quad(vc, p, s, light, 0, 0, -1, 8 - w / 2, 8 + w / 2, v0, v1, x1, top, z0, x0, top, z0, x0, bottom, z0, x1, bottom, z0);
            quad(vc, p, s, light, 0, 0, 1, 8 - w / 2, 8 + w / 2, v0, v1, x0, top, z1, x1, top, z1, x1, bottom, z1, x0, bottom, z1);
            quad(vc, p, s, light, -1, 0, 0, 8 - d / 2, 8 + d / 2, v0, v1, x0, top, z0, x0, top, z1, x0, bottom, z1, x0, bottom, z0);
            quad(vc, p, s, light, 1, 0, 0, 8 - d / 2, 8 + d / 2, v0, v1, x1, top, z1, x1, top, z0, x1, bottom, z0, x1, bottom, z1);
            top = bottom;
        }
    }

    private static void quad(VertexConsumer vc, PoseStack.Pose p, TextureAtlasSprite s, int light, float nx, float ny, float nz,
                             float uFrom, float uTo, float vFrom, float vTo,
                             float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
        float u0 = s.getU(uFrom / 16f), u1 = s.getU(uTo / 16f);
        float v0 = s.getV(vFrom / 16f), v1 = s.getV(vTo / 16f);
        vertex(vc, p, ax, ay, az, u0, v0, light, nx, ny, nz);
        vertex(vc, p, dx, dy, dz, u0, v1, light, nx, ny, nz);
        vertex(vc, p, cx, cy, cz, u1, v1, light, nx, ny, nz);
        vertex(vc, p, bx, by, bz, u1, v0, light, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, float x, float y, float z, float u, float v, int light, float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor(0xFFFFFFFF).setUv(u, v).setLight(light).setNormal(p, nx, ny, nz);
    }

    @Override
    public AABB getRenderBoundingBox(T be) {
        Limb limb = be.limb();
        return new AABB(be.getBlockPos()).inflate((limb == null ? 0 : limb.reach()) + 0.5);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }
}
