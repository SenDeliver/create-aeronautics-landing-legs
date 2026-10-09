package dev.legworks.content.simple;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import dev.legworks.LegworksIds;
import dev.legworks.content.LegStatusText;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.network.chat.Component;
import dev.legworks.leg.Leg;
import dev.legworks.leg.LegHost;
import dev.legworks.leg.LegSettings;
import dev.legworks.leg.LimbSlot;
import dev.ryanhcode.sable.api.block.BlockEntitySubLevelActor;
import dev.ryanhcode.sable.api.physics.handle.RigidBodyHandle;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class SimpleLegMountBlockEntity extends SmartBlockEntity implements LegHost, BlockEntitySubLevelActor, Clearable, IHaveGoggleInformation {
    static final int STATUS_SYNC_TICKS = 10;
    private final Leg leg = new Leg(this);
    private final LimbSlot limbSlot = new LimbSlot();
    private int age;

    public SimpleLegMountBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
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
        return SimpleLegSettings.INSTANCE;
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

    @Override
    public void tick() {
        super.tick();
        if (level == null) return;
        if (level.isClientSide) {
            leg.clientTick();
        } else if (++age % STATUS_SYNC_TICKS == 0 && leg.pollStatusSync()) {
            sendData();
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        new LangBuilder(LegworksIds.MOD_ID).translate("goggles.simple_leg").forGoggles(tooltip);
        for (Component line : LegStatusText.mount(this)) new LangBuilder(LegworksIds.MOD_ID).add(line).forGoggles(tooltip, 1);
        return true;
    }

    @Override
    public void sable$tick(ServerSubLevel subLevel) {
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
        if (clientPacket) leg.writeStatus(tag);
        else if (!Double.isNaN(leg.rest())) tag.putDouble(LegworksIds.NBT_REST, leg.rest());
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.read(tag, registries, clientPacket);
        if (limbSlot.read(tag, registries)) leg.onLimbChanged();
        if (clientPacket) leg.readStatus(tag);
        else leg.setSavedRest(tag.contains(LegworksIds.NBT_REST) ? tag.getDouble(LegworksIds.NBT_REST) : Double.NaN);
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
}
