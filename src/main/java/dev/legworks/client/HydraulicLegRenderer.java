package dev.legworks.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.ShaftRenderer;
import dev.legworks.content.hydraulic.HydraulicLegMountBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

/** The shaft through a Hydraulic Leg Mount plus its drawn Limb. */
public class HydraulicLegRenderer extends ShaftRenderer<HydraulicLegMountBlockEntity> {
    private final LegRenderer<HydraulicLegMountBlockEntity> leg;

    public HydraulicLegRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
        leg = new LegRenderer<>(context);
    }

    @Override
    protected void renderSafe(HydraulicLegMountBlockEntity be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        leg.render(be, partialTicks, ms, buffer, light, overlay);
    }

    @Override
    public AABB getRenderBoundingBox(HydraulicLegMountBlockEntity be) {
        return leg.getRenderBoundingBox(be);
    }

    @Override
    public int getViewDistance() {
        return leg.getViewDistance();
    }
}
