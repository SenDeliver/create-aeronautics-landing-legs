package dev.legworks.registry;

import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import com.tterrag.registrate.util.entry.BlockEntry;
import dev.legworks.LegworksIds;
import com.simibubi.create.api.behaviour.display.DisplaySource;
import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import dev.legworks.config.LegworksConfig;
import dev.legworks.content.controller.LegControllerBlock;
import dev.legworks.content.controller.LegControllerItem;
import dev.legworks.content.hydraulic.HydraulicLegMountBlock;
import dev.legworks.content.simple.SimpleLegMountBlock;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import static dev.legworks.Legworks.REGISTRATE;

public final class LegworksBlocks {
    public static final BlockEntry<SimpleLegMountBlock> SIMPLE_LEG_MOUNT = REGISTRATE.block(LegworksIds.SIMPLE_LEG_MOUNT, SimpleLegMountBlock::new)
            .properties(p -> p.mapColor(MapColor.METAL).strength(2.0f).sound(SoundType.METAL).dynamicShape().noOcclusion())
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate((ctx, prov) -> legMountState(ctx, prov, LegworksModels.simpleMount(prov)))
            .transform(DisplaySource.displaySource(LegworksDisplaySources.MOUNT_STATUS))
            .item()
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName())))
            .build()
            .register();

    public static final BlockEntry<HydraulicLegMountBlock> HYDRAULIC_LEG_MOUNT = REGISTRATE.block(LegworksIds.HYDRAULIC_LEG_MOUNT, HydraulicLegMountBlock::new)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW).strength(2.0f).sound(SoundType.METAL).dynamicShape().noOcclusion())
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate((ctx, prov) -> hydraulicMountState(ctx, prov, ctx.getName()))
            .onRegister(b -> BlockStressValues.IMPACTS.register(b, () -> LegworksConfig.SPEC.isLoaded() ? LegworksConfig.HYDRAULIC_STRESS.get() : 4.0))
            .transform(DisplaySource.displaySource(LegworksDisplaySources.MOUNT_STATUS))
            .item()
            .model((ctx, prov) -> prov.withExistingParent(ctx.getName(), prov.modLoc("block/" + ctx.getName())))
            .build()
            .register();

    public static final BlockEntry<LegControllerBlock> LEG_CONTROLLER = REGISTRATE.block(LegworksIds.LEG_CONTROLLER, LegControllerBlock::new)
            .properties(p -> p.mapColor(MapColor.TERRACOTTA_YELLOW).strength(2.0f).sound(SoundType.METAL))
            .tag(BlockTags.MINEABLE_WITH_PICKAXE)
            .blockstate((ctx, prov) -> prov.simpleBlock(ctx.get(), LegworksModels.controller(prov)))
            .transform(DisplaySource.displaySource(LegworksDisplaySources.CONTROLLER_STATUS))
            .item(LegControllerItem::new)
            .build()
            .register();

    private LegworksBlocks() {
    }

    public static void init() {
    }

    private static int facingRotX(Direction f) {
        return switch (f) {
            case UP -> 0;
            case DOWN -> 180;
            default -> 90;
        };
    }

    private static int facingRotY(Direction f) {
        return switch (f) {
            case SOUTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0;
        };
    }

    private static <T extends Block> void legMountState(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, ModelFile model) {
        prov.getVariantBuilder(ctx.get()).forAllStates(state -> {
            Direction f = state.getValue(BlockStateProperties.FACING);
            return ConfiguredModel.builder().modelFile(model).rotationX(facingRotX(f)).rotationY(facingRotY(f)).build();
        });
    }

    /**
     * Picks the model whose shaft holes (model X or Z) land on the shaft axis after the vanilla blockstate rotation, turning the
     * settings-face texture where needed so its two label panels lie along the value boxes (side by side horizontally; along X on up/down).
     */
    private static <T extends Block> void hydraulicMountState(DataGenContext<Block, T> ctx, RegistrateBlockstateProvider prov, String name) {
        ModelFile[][] models = new ModelFile[2][2];
        prov.getVariantBuilder(ctx.get()).forAllStates(state -> {
            Direction f = state.getValue(BlockStateProperties.FACING);
            int x = facingRotX(f), y = facingRotY(f);
            Quaternionf rot = new Quaternionf().rotateYXZ((float) Math.toRadians(-y), (float) Math.toRadians(-x), 0);
            Direction.Axis shaft = ((DirectionalAxisKineticBlock) state.getBlock()).getRotationAxis(state);
            boolean holesOnX = worldAxis(rot, 1, 0, 0) == shaft;
            Direction.Axis settingsNormal = holesOnX ? worldAxis(rot, 0, 0, 1) : worldAxis(rot, 1, 0, 0);
            Direction.Axis panelAxis = holesOnX ? worldAxis(rot, 1, 0, 0) : worldAxis(rot, 0, 0, 1);
            boolean turned = settingsNormal == Direction.Axis.Y ? panelAxis != Direction.Axis.X : panelAxis == Direction.Axis.Y;
            int i = holesOnX ? 0 : 1, j = turned ? 1 : 0;
            if (models[i][j] == null) models[i][j] = LegworksModels.hydraulicMount(prov, name + (holesOnX ? "" : "_z") + (turned ? "_turned" : ""), holesOnX, turned);
            return ConfiguredModel.builder().modelFile(models[i][j]).rotationX(x).rotationY(y).build();
        });
    }

    private static Direction.Axis worldAxis(Quaternionf rot, float x, float y, float z) {
        Vector3f v = rot.transform(new Vector3f(x, y, z));
        return Direction.getNearest(v.x, v.y, v.z).getAxis();
    }
}
