package dev.legworks.registry;

import com.tterrag.registrate.util.entry.BlockEntityEntry;
import dev.legworks.LegworksIds;
import dev.legworks.client.LegRenderer;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import dev.legworks.client.HydraulicLegRenderer;
import dev.legworks.content.controller.LegControllerBlockEntity;
import dev.legworks.content.hydraulic.HydraulicLegMountBlockEntity;
import dev.legworks.content.simple.SimpleLegMountBlockEntity;

import static dev.legworks.Legworks.REGISTRATE;

public final class LegworksBlockEntityTypes {
    public static final BlockEntityEntry<SimpleLegMountBlockEntity> SIMPLE_LEG_MOUNT = REGISTRATE
            .blockEntity(LegworksIds.SIMPLE_LEG_MOUNT, SimpleLegMountBlockEntity::new)
            .validBlock(LegworksBlocks.SIMPLE_LEG_MOUNT)
            .renderer(() -> LegRenderer::new)
            .register();

    public static final BlockEntityEntry<HydraulicLegMountBlockEntity> HYDRAULIC_LEG_MOUNT = REGISTRATE
            .blockEntity(LegworksIds.HYDRAULIC_LEG_MOUNT, HydraulicLegMountBlockEntity::new)
            .visual(() -> SingleAxisRotatingVisual::shaft, true)
            .validBlock(LegworksBlocks.HYDRAULIC_LEG_MOUNT)
            .renderer(() -> HydraulicLegRenderer::new)
            .register();

    public static final BlockEntityEntry<LegControllerBlockEntity> LEG_CONTROLLER = REGISTRATE
            .blockEntity(LegworksIds.LEG_CONTROLLER, LegControllerBlockEntity::new)
            .validBlock(LegworksBlocks.LEG_CONTROLLER)
            .register();

    private LegworksBlockEntityTypes() {
    }

    public static void init() {
    }
}
