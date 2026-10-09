package dev.legworks.dev;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.belt.transport.TransportedItemStack;
import dev.legworks.LegworksIds;
import dev.legworks.registry.LegworksBlocks;
import dev.legworks.registry.LegworksItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Phase-3 dev commands: recipe checks (crafting table, Mechanical Crafter), a real Sequenced Assembly line
 * (belt + Deployer + Spout + Press), and schematic export for Ponder scenes.
 */
final class DevPhase3 {
    private static final Logger LOG = LoggerFactory.getLogger("legworks-dev");

    private DevPhase3() {
    }

    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("legworks").requires(s -> s.hasPermission(2))
                .then(Commands.literal("recipes").executes(DevPhase3::recipes))
                .then(Commands.literal("assemblyline").then(Commands.argument("at", BlockPosArgument.blockPos()).executes(DevPhase3::assemblyLine)))
                .then(Commands.literal("power").then(Commands.argument("at", BlockPosArgument.blockPos()).then(Commands.argument("rpm", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-256, 256)).executes(ctx -> {
                    power(ctx.getSource().getLevel(), BlockPosArgument.getLoadedBlockPos(ctx, "at"), com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "rpm"));
                    return 1;
                }))))
                .then(Commands.literal("displayship").then(Commands.argument("at", net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3()).executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    var ship = DevHydraulic.shipAt(level, net.minecraft.commands.arguments.coordinates.Vec3Argument.getVec3(ctx, "at"), 8);
                    if (ship == null) return 0;
                    var box = ship.getPlot().getBoundingBox();
                    for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                        BlockEntity be = level.getBlockEntity(p);
                        if (be == null) continue;
                        String id = String.valueOf(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()));
                        if (id.contains("display") || id.contains("flap"))
                            LOG.info("DISPLAYSHIP {} {} {}", p.toShortString(), id, be.saveWithoutMetadata(level.registryAccess()));
                    }
                    return 1;
                })))
                .then(Commands.literal("shipnbt").then(Commands.argument("at", net.minecraft.commands.arguments.coordinates.Vec3Argument.vec3())
                        .then(Commands.argument("rel", BlockPosArgument.blockPos()).then(Commands.argument("nbt", net.minecraft.commands.arguments.CompoundTagArgument.compoundTag()).executes(ctx -> {
                    ServerLevel level = ctx.getSource().getLevel();
                    var ship = DevHydraulic.shipAt(level, net.minecraft.commands.arguments.coordinates.Vec3Argument.getVec3(ctx, "at"), 8);
                    if (ship == null) return 0;
                    var box = ship.getPlot().getBoundingBox();
                    var rel = ctx.getArgument("rel", net.minecraft.commands.arguments.coordinates.Coordinates.class).getBlockPos(ctx.getSource().withPosition(net.minecraft.world.phys.Vec3.ZERO));
                    BlockPos p = new BlockPos(box.minX(), box.minY(), box.minZ()).offset(rel);
                    BlockEntity be = level.getBlockEntity(p);
                    if (be == null) return 0;
                    CompoundTag t = be.saveWithoutMetadata(level.registryAccess());
                    t.merge(net.minecraft.commands.arguments.CompoundTagArgument.getCompoundTag(ctx, "nbt"));
                    be.loadWithComponents(t, level.registryAccess());
                    be.setChanged();
                    level.sendBlockUpdated(p, be.getBlockState(), be.getBlockState(), 3);
                    LOG.info("SHIPNBT {} -> {}", p.toShortString(), t);
                    return 1;
                })))))
                .then(Commands.literal("displaycheck").then(Commands.argument("at", BlockPosArgument.blockPos()).executes(ctx -> {
                    BlockPos p = BlockPosArgument.getLoadedBlockPos(ctx, "at");
                    ServerLevel level = ctx.getSource().getLevel();
                    LOG.info("DISPLAY sources at {}: {} target: {}", p.toShortString(),
                            com.simibubi.create.api.behaviour.display.DisplaySource.getAll(level, p),
                            com.simibubi.create.api.behaviour.display.DisplayTarget.get(level, p.south(2)));
                    return 1;
                })))
                .then(Commands.literal("assemblycheck").then(Commands.argument("at", BlockPosArgument.blockPos()).executes(DevPhase3::assemblyCheck)))
                .then(Commands.literal("schematic").then(Commands.argument("name", StringArgumentType.word())
                        .then(Commands.argument("from", BlockPosArgument.blockPos()).then(Commands.argument("to", BlockPosArgument.blockPos())
                                .executes(DevPhase3::schematic))))));
    }

    private static ItemStack[] grid(ItemLike[][] rows, int w) {
        ItemStack[] out = new ItemStack[rows.length * w];
        for (int r = 0; r < rows.length; r++) {
            for (int c = 0; c < w; c++) {
                ItemLike i = c < rows[r].length ? rows[r][c] : null;
                out[r * w + c] = i == null ? ItemStack.EMPTY : new ItemStack(i);
            }
        }
        return out;
    }

    private static void check(ServerLevel level, String name, RecipeType<?> type, ItemLike[][] rows, int w, ItemLike expected) {
        CraftingInput input = CraftingInput.of(w, rows.length, List.of(grid(rows, w)));
        RecipeHolder<?> found = null;
        for (RecipeHolder<?> h : level.getRecipeManager().getRecipes()) {
            if (h.value().getType() != type) continue;
            boolean match = h.value() instanceof ShapedRecipe shaped ? shaped.pattern.matches(input)
                    : ((net.minecraft.world.item.crafting.Recipe<CraftingInput>) h.value()).matches(input, level);
            if (match) {
                found = h;
                break;
            }
        }
        ItemStack result = found == null ? ItemStack.EMPTY : found.value().getResultItem(level.registryAccess());
        boolean ok = result.is(expected.asItem());
        LOG.info("RECIPE {} {} type={} id={} result={}", ok ? "PASS" : "FAIL", name, BuiltInRegistries.RECIPE_TYPE.getKey(type),
                found == null ? "none" : found.id().toString(), BuiltInRegistries.ITEM.getKey(result.getItem()));
    }

    private static int recipes(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        ItemLike sheet = AllItems.IRON_SHEET.get(), alloy = AllItems.ANDESITE_ALLOY.get(), brass = AllItems.BRASS_SHEET.get();
        ItemLike sturdy = AllItems.STURDY_SHEET.get();
        check(level, "short_limb", RecipeType.CRAFTING, new ItemLike[][]{{sheet}, {alloy}, {sheet}}, 1, LegworksItems.SHORT_LIMB.get());
        check(level, "simple_leg_mount", RecipeType.CRAFTING, new ItemLike[][]{{AllBlocks.ANDESITE_CASING.get()}, {AllBlocks.SHAFT.get()}, {sheet}}, 1, LegworksBlocks.SIMPLE_LEG_MOUNT.get());
        check(level, "leg_controller", RecipeType.CRAFTING, new ItemLike[][]{{AllItems.ELECTRON_TUBE.get()}, {AllBlocks.BRASS_CASING.get()}, {AllItems.PRECISION_MECHANISM.get()}}, 1, LegworksBlocks.LEG_CONTROLLER.get());
        check(level, "long_limb", AllRecipeTypes.MECHANICAL_CRAFTING.getType(), new ItemLike[][]{{brass}, {brass}, {net.minecraft.world.item.Items.IRON_INGOT}, {brass}, {brass}}, 1, LegworksItems.LONG_LIMB.get());
        check(level, "heavy_limb", AllRecipeTypes.MECHANICAL_CRAFTING.getType(), new ItemLike[][]{{null, brass}, {null, sturdy}, {null, brass}, {null, sturdy}, {sturdy, sturdy, sturdy}}, 3, LegworksItems.HEAVY_LIMB.get());
        boolean seq = level.getRecipeManager().byKey(LegworksIds.id("sequenced_assembly/" + LegworksIds.HYDRAULIC_LEG_MOUNT)).isPresent();
        LOG.info("RECIPE {} hydraulic_leg_mount_sequence present={}", seq ? "PASS" : "FAIL", seq);
        long ours = level.getRecipeManager().getRecipes().stream().filter(h -> h.id().getNamespace().equals(LegworksIds.MOD_ID)).count();
        LOG.info("RECIPE count legworks={}", ours);
        return 1;
    }

    private static void power(ServerLevel level, BlockPos pos, int rpm) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof IRotate rot)) return;
        Direction.Axis axis = rot.getRotationAxis(state);
        for (Direction dir : Direction.values()) {
            if (dir.getAxis() != axis || !rot.hasShaftTowards(level, pos, state, dir)) continue;
            BlockPos m = pos.relative(dir);
            if (!level.getBlockState(m).isAir()) continue;
            level.setBlock(m, AllBlocks.CREATIVE_MOTOR.getDefaultState().setValue(BlockStateProperties.FACING, dir.getOpposite()), 3);
            BlockEntity be = level.getBlockEntity(m);
            if (be != null) {
                CompoundTag t = be.saveWithoutMetadata(level.registryAccess());
                t.putInt("ScrollValue", rpm);
                be.loadWithComponents(t, level.registryAccess());
                be.setChanged();
            }
            LOG.info("ASSEMBLY motor {} -> {} axis={}", m.toShortString(), pos.toShortString(), axis);
            return;
        }
        LOG.info("ASSEMBLY no motor spot for {}", pos.toShortString());
    }

    private static int assemblyLine(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos o = BlockPosArgument.getLoadedBlockPos(ctx, "at");
        BlockPos start = o, end = o.east(9);
        for (int x = -1; x <= 11; x++) for (int y = 0; y <= 3; y++) for (int z = -2; z <= 2; z++) level.setBlock(o.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        BeltConnectorItem.createBelts(level, start, end);
        level.setBlock(end.east(), AllBlocks.DEPOT.getDefaultState(), 3);
        power(level, start, -32);

        BlockPos deployer = o.offset(2, 2, 0), spout = o.offset(5, 2, 0), press = o.offset(8, 2, 0);
        level.setBlock(deployer, AllBlocks.DEPLOYER.getDefaultState().setValue(BlockStateProperties.FACING, Direction.DOWN), 3);
        BlockEntity dbe = level.getBlockEntity(deployer);
        if (dbe != null) {
            CompoundTag t = dbe.saveWithoutMetadata(level.registryAccess());
            CompoundTag held = (CompoundTag) new ItemStack(AllBlocks.MECHANICAL_PISTON.get(), 64).save(level.registryAccess());
            held.putByte("Slot", (byte) 0);
            net.minecraft.nbt.ListTag inv = new net.minecraft.nbt.ListTag();
            inv.add(held);
            t.put("Inventory", inv);
            dbe.loadWithComponents(t, level.registryAccess());
            dbe.setChanged();
        }
        power(level, deployer, 32);
        level.setBlock(spout, AllBlocks.SPOUT.getDefaultState(), 3);
        IFluidHandler tank = level.getCapability(Capabilities.FluidHandler.BLOCK, spout, Direction.UP);
        int filled = tank == null ? 0 : tank.fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE);
        level.setBlock(press, AllBlocks.MECHANICAL_PRESS.getDefaultState(), 3);
        power(level, press, 32);

        ItemEntity item = new ItemEntity(level, start.getX() + 0.5, start.getY() + 1.0, start.getZ() + 0.5, new ItemStack(AllBlocks.BRASS_CASING.get()));
        item.setDeltaMovement(0, 0, 0);
        level.addFreshEntity(item);
        LOG.info("ASSEMBLY built at {} spoutWater={}", o.toShortString(), filled);
        return 1;
    }

    private static int assemblyCheck(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos o = BlockPosArgument.getLoadedBlockPos(ctx, "at");
        List<String> found = new ArrayList<>();
        for (int x = -1; x <= 11; x++) {
            BlockPos p = o.offset(x, 0, 0);
            IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, p, Direction.UP);
            if (h == null) continue;
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack s = h.getStackInSlot(i);
                if (!s.isEmpty()) found.add(x + ":" + BuiltInRegistries.ITEM.getKey(s.getItem()));
            }
        }
        for (ItemEntity e : level.getEntitiesOfClass(ItemEntity.class, new AABB(o).inflate(12, 4, 4)))
            found.add("entity@" + e.blockPosition().toShortString().replace(" ", "") + ":" + BuiltInRegistries.ITEM.getKey(e.getItem().getItem()));
        if (level.getBlockEntity(o) instanceof BeltBlockEntity belt && belt.getControllerBE() != null && belt.getControllerBE().getInventory() != null) {
            for (TransportedItemStack t : belt.getControllerBE().getInventory().getTransportedItems())
                found.add(String.format(java.util.Locale.ROOT, "belt@%.1f:%s", t.beltPosition, BuiltInRegistries.ITEM.getKey(t.stack.getItem())));
        }
        boolean ok = found.stream().anyMatch(s -> s.endsWith(LegworksIds.MOD_ID + ":" + LegworksIds.HYDRAULIC_LEG_MOUNT));
        LOG.info("ASSEMBLY {} items={}", ok ? "PASS" : "WAIT", found);
        return 1;
    }

    private static int schematic(CommandContext<CommandSourceStack> ctx) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerLevel level = ctx.getSource().getLevel();
        String name = StringArgumentType.getString(ctx, "name");
        BlockPos a = BlockPosArgument.getLoadedBlockPos(ctx, "from"), b = BlockPosArgument.getLoadedBlockPos(ctx, "to");
        BlockPos min = new BlockPos(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()));
        BlockPos max = new BlockPos(Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
        StructureTemplate t = new StructureTemplate();
        t.fillFromWorld(level, min, max.subtract(min).offset(1, 1, 1), false, Blocks.AIR);
        CompoundTag tag = t.save(new CompoundTag());
        String dir = System.getProperty("legworks.ponderOut");
        try {
            Path out = Path.of(dir == null ? "ponder-export" : dir).resolve(name + ".nbt");
            Files.createDirectories(out.getParent());
            NbtIo.writeCompressed(tag, out);
            LOG.info("SCHEMATIC {} -> {}", name, out.toAbsolutePath());
        } catch (Exception e) {
            LOG.error("SCHEMATIC {} failed", name, e);
        }
        return 1;
    }
}
