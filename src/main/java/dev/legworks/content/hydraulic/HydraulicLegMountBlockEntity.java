package dev.legworks.content.hydraulic;

import com.simibubi.create.content.kinetics.base.DirectionalAxisKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.legworks.LegworksIds;
import dev.legworks.config.LegworksConfig;
import dev.legworks.content.LegStatusText;
import dev.legworks.content.SettingScroll;
import net.createmod.catnip.lang.LangBuilder;
import dev.legworks.content.SideSlot;
import dev.legworks.content.controller.LegControllerBlockEntity;
import dev.legworks.content.controller.Linking;
import dev.legworks.leg.Leg;
import dev.legworks.leg.LegHost;
import dev.legworks.leg.LegSettings;
import dev.legworks.leg.LimbSlot;
import dev.legworks.limb.Limb;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HydraulicLegMountBlockEntity extends KineticBlockEntity implements LegHost, BlockEntitySubLevelActor, Clearable {
    public static final BehaviourType<SettingScroll> STIFFNESS = new BehaviourType<>("legworks_stiffness");
    public static final BehaviourType<SettingScroll> DAMPING = new BehaviourType<>("legworks_damping");
    public static final String NBT_FOLD = "Fold";
    public static final String NBT_SOFTENING = "Softening";
    public static final String NBT_GEAR_UP = "GearUp";
    public static final String NBT_LINKED = "Linked";
    public static final String NBT_TOP_SIGNAL = "TopSignal";
    static final int STATUS_SYNC_TICKS = 10;
    static final int UNLINK_AFTER_TICKS = 40;
    static final int LOAD_GRACE_TICKS = 10;

    private final Leg leg = new Leg(this);
    private final LimbSlot limbSlot = new LimbSlot();
    private final Settings settings = new Settings();
    private SettingScroll stiffness;
    private SettingScroll damping;

    @Nullable
    private BlockPos controllerOffset;
    @Nullable
    private LegControllerBlockEntity controller;
    private int missingTicks;
    private boolean gearUp;
    private double fold;
    private double softening;
    private int topSignal = -1;
    private int age;
    private boolean linkedSynced;
    private boolean sentLinked;
    private double sentSoftening = -1;
    private int sentTop = -1;

    public HydraulicLegMountBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
        stiffness = new SettingScroll(STIFFNESS, 10, LegworksIds.NBT_SETTINGS, LegworksIds.NBT_SETTINGS_STIFFNESS, 0.01,
                Component.translatable("legworks.value.stiffness"), Component.translatable("legworks.value.stiffness.board"),
                Component.translatable("legworks.value.stiffness"), this, new SideSlot(HydraulicLegMountBlockEntity::isSettingsSide, 4.5, 0.4f),
                v -> Component.translatable("legworks.unit.percent", v));
        stiffness.between(25, 200);
        stiffness.value = 100;
        damping = new SettingScroll(DAMPING, 11, LegworksIds.NBT_SETTINGS, LegworksIds.NBT_SETTINGS_DAMPING, 0.1,
                Component.translatable("legworks.value.damping"), Component.translatable("legworks.value.damping.board"),
                Component.translatable("legworks.value.damping"), this, new SideSlot(HydraulicLegMountBlockEntity::isSettingsSide, 11.5, 0.4f),
                v -> Component.literal(LegStatusText.num(v / 10.0)));
        damping.between(2, 15);
        damping.value = 8;
        behaviours.add(stiffness);
        behaviours.add(damping);
    }

    static boolean isSettingsSide(BlockState state, Direction side) {
        if (!(state.getBlock() instanceof DirectionalAxisKineticBlock block)) return false;
        return side.getAxis() != state.getValue(BlockStateProperties.FACING).getAxis() && side.getAxis() != block.getRotationAxis(state);
    }

    @Override
    public Leg leg() {
        return leg;
    }

    @Override
    public LimbSlot limbSlot() {
        return limbSlot;
    }

    @Override
    public LegSettings legSettings() {
        return settings;
    }

    @Override
    public void onLimbSlotChanged() {
        leg.onLimbChanged();
        notifyUpdate();
    }

    @Override
    public void clearContent() {
        limbSlot.take();
    }

    public Direction facing() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    @Nullable
    public BlockPos controllerPos() {
        return controllerOffset == null ? null : worldPosition.offset(controllerOffset);
    }

    public boolean isLinked() {
        return level != null && level.isClientSide ? linkedSynced : controller != null;
    }

    public boolean isGearUp() {
        return gearUp;
    }

    /** Client only (Ponder): fold or unfold without a Controller. */
    public void setDisplayGearUp(boolean up) {
        if (level != null && level.isClientSide) gearUp = up;
    }

    public void setControllerOffset(@Nullable BlockPos offset) {
        controllerOffset = offset == null ? null : offset.immutable();
        controller = null;
        missingTicks = 0;
        notifyUpdate();
    }

    public boolean isPowered() {
        return getSpeed() != 0;
    }

    public double fold() {
        return fold;
    }

    public double softening() {
        return softening;
    }

    public int topSignal() {
        return Math.max(0, topSignal);
    }

    public void updateTopSignal() {
        if (level == null) return;
        Direction top = facing().getOpposite();
        topSignal = level.getSignal(worldPosition.relative(top), top);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;
        age++;
        if (level.isClientSide) {
            stepFold();
            leg.clientTick();
            return;
        }
        if (topSignal < 0) updateTopSignal();
        resolveController(true);
        boolean up = controller != null && controller.isGearUp();
        if (up != gearUp) {
            gearUp = up;
            sendData();
        }
        stepFold();
        double target = isPowered() ? topSignal() / 15.0 : 1.0;
        if (age > LOAD_GRACE_TICKS) {
            double step = LegworksConfig.value(LegworksConfig.SOFTEN_RATE) / 20.0;
            softening = Math.abs(target - softening) <= step ? target : softening + Math.signum(target - softening) * step;
        }
        if (age % STATUS_SYNC_TICKS == 0) {
            boolean linkedNow = controller != null;
            boolean statusChanged = leg.pollStatusSync();
            boolean settingsChanged = Math.abs(softening - sentSoftening) > 0.04 || topSignal() != sentTop;
            if (statusChanged || settingsChanged || linkedNow != sentLinked) {
                sentLinked = linkedNow;
                sentSoftening = softening;
                sentTop = topSignal();
                sendData();
            }
        }
    }

    private void stepFold() {
        double target = gearUp ? 1 : 0;
        if (fold == target) return;
        double step = Math.abs(getSpeed()) / LegworksConfig.value(LegworksConfig.STROKE_RPM_SECONDS) / 20.0;
        fold = Math.abs(target - fold) <= step ? target : fold + Math.signum(target - fold) * step;
    }

    private void resolveController(boolean countMissing) {
        if (controllerOffset == null) {
            controller = null;
            return;
        }
        BlockPos cp = worldPosition.offset(controllerOffset);
        if (!level.isLoaded(cp)) return;
        if (level.getBlockEntity(cp) instanceof LegControllerBlockEntity c && !c.isRemoved()
                && c.hasLink(worldPosition.subtract(cp)) && Linking.sameShip(level, worldPosition, cp)) {
            controller = c;
            missingTicks = 0;
            return;
        }
        controller = null;
        if (countMissing && ++missingTicks >= UNLINK_AFTER_TICKS) setControllerOffset(null);
    }

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
        if (controller == null || controller.isRemoved()) resolveController(false);
        leg.serverTick(subLevel);
    }

    @Override
    public void sable$physicsTick(ServerSubLevel subLevel, RigidBodyHandle handle, double timeStep) {
        leg.physicsTick(subLevel, timeStep);
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        limbSlot.write(tag, registries);
        if (controllerOffset != null) tag.putIntArray(LegworksIds.NBT_CONTROLLER, new int[]{controllerOffset.getX(), controllerOffset.getY(), controllerOffset.getZ()});
        tag.putDouble(NBT_FOLD, fold);
        tag.putDouble(NBT_SOFTENING, softening);
        if (clientPacket) {
            tag.putBoolean(NBT_GEAR_UP, gearUp);
            tag.putBoolean(NBT_LINKED, controller != null);
            tag.putInt(NBT_TOP_SIGNAL, topSignal());
            leg.writeStatus(tag);
        } else {
            if (!Double.isNaN(leg.rest())) tag.putDouble(LegworksIds.NBT_REST, leg.rest());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (limbSlot.read(tag, registries)) leg.onLimbChanged();
        BlockPos off = null;
        if (tag.get(LegworksIds.NBT_CONTROLLER) instanceof IntArrayTag arr && arr.size() == 3) off = new BlockPos(arr.get(0).getAsInt(), arr.get(1).getAsInt(), arr.get(2).getAsInt());
        if (off == null || !off.equals(controllerOffset)) controller = null;
        controllerOffset = off;
        fold = Math.max(0, Math.min(1, tag.getDouble(NBT_FOLD)));
        softening = Math.max(0, Math.min(1, tag.getDouble(NBT_SOFTENING)));
        if (clientPacket) {
            gearUp = tag.getBoolean(NBT_GEAR_UP);
            linkedSynced = tag.getBoolean(NBT_LINKED);
            topSignal = tag.getInt(NBT_TOP_SIGNAL);
            leg.readStatus(tag);
        } else {
            gearUp = fold > 0.5;
            leg.setSavedRest(tag.contains(LegworksIds.NBT_REST) ? tag.getDouble(LegworksIds.NBT_REST) : Double.NaN);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        new LangBuilder(LegworksIds.MOD_ID).translate("goggles.hydraulic_leg").forGoggles(tooltip);
        for (Component line : LegStatusText.mount(this)) new LangBuilder(LegworksIds.MOD_ID).add(line).forGoggles(tooltip, 1);
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        return true;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        leg.onLoad();
    }

    @Override
    public void invalidate() {
        super.invalidate();
        leg.onInvalidate();
    }

    private final class Settings implements LegSettings {
        @Override
        public boolean active() {
            return controller != null && fold < 1;
        }

        @Override
        public double stiffnessScale() {
            return stiffness == null ? 1 : stiffness.stored();
        }

        @Override
        public double softening() {
            return softening;
        }

        @Override
        public double dampingRatio() {
            return damping == null ? 0.8 : damping.stored();
        }

        @Override
        public double rideTarget(Limb limb) {
            double h = controller == null ? 0 : controller.rideHeight();
            return h > 0 ? h : limb.nominalRest();
        }

        @Override
        public double fold() {
            return fold;
        }

        @Override
        public double strokeRate(Limb limb) {
            return limb.reach() * Math.abs(getSpeed()) / LegworksConfig.value(LegworksConfig.STROKE_RPM_SECONDS);
        }
    }
}
