package dev.legworks.content.controller;

import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import dev.legworks.LegworksIds;
import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import dev.legworks.content.LegStatusText;
import dev.legworks.content.SettingScroll;
import net.createmod.catnip.lang.LangBuilder;
import dev.legworks.content.SideSlot;
import dev.legworks.content.hydraulic.HydraulicLegMountBlockEntity;
import dev.legworks.limb.Limb;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Leg Controller: holds the Leg Group's Links, its Ride Height (value box) and Gear Up (any redstone signal).
 */
public class LegControllerBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    public static final BehaviourType<SettingScroll> RIDE_HEIGHT = new BehaviourType<>("legworks_ride_height");
    public static final String NBT_CONFLICT = "Conflict";
    public static final String NBT_GEAR_UP = "GearUp";
    public static final String NBT_GROUNDED = "Grounded";
    public static final String NBT_OVERREACH = "Overreach";
    public static final String NBT_RIDE_HEIGHT_MAX = "RideHeightMax";
    /** Ride Height scroll maximum while no Linked Leg has a Limb. */
    public static final int RIDE_HEIGHT_MAX_TENTHS = 60;
    static final int RIDE_HEIGHT_LIMIT_TENTHS = Math.round(Limb.MAX_REACH * 10);
    static final int PRUNE_AFTER_CHECKS = 4;

    private static final Set<LegControllerBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private final List<BlockPos> links = new ArrayList<>();
    private final Map<BlockPos, Integer> missing = new HashMap<>();
    private SettingScroll rideHeight;
    private boolean gearUp;
    private boolean signalKnown;
    private boolean conflict;
    private int grounded;
    private boolean overreach;

    public LegControllerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        setLazyTickRate(10);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        rideHeight = new SettingScroll(RIDE_HEIGHT, 10, null, LegworksIds.NBT_RIDE_HEIGHT, 0.1,
                Component.translatable("legworks.value.ride_height"), Component.translatable("legworks.value.ride_height.board"),
                Component.translatable("legworks.value.ride_height"), this, new SideSlot((s, d) -> d == Direction.UP, 8, 0.5f),
                LegStatusText::rideHeight);
        rideHeight.between(0, RIDE_HEIGHT_LIMIT_TENTHS);
        rideHeight.value = 0;
        behaviours.add(rideHeight);
    }

    /** Ride Height in blocks; 0 = Auto (each Leg's Limb default). */
    public double rideHeight() {
        return rideHeight == null ? 0 : rideHeight.stored();
    }

    public int rideHeightSetting() {
        return rideHeight == null ? 0 : rideHeight.getValue();
    }

    public void setRideHeightSetting(int tenths) {
        if (rideHeight != null) rideHeight.setValue(tenths);
    }

    public boolean isGearUp() {
        return gearUp;
    }

    public boolean hasConflict() {
        return conflict;
    }

    /** Linked Legs whose Foot is on Ground (server: last check, client: synced). */
    public int groundedCount() {
        return grounded;
    }

    /** Any Linked Leg is in Overreach. */
    public boolean hasOverreach() {
        return overreach;
    }

    public List<BlockPos> links() {
        return Collections.unmodifiableList(links);
    }

    public int linkCount() {
        return links.size();
    }

    public boolean hasLink(BlockPos offset) {
        return links.contains(offset);
    }

    void addLink(BlockPos offset) {
        if (links.contains(offset)) return;
        links.add(offset.immutable());
        notifyUpdate();
    }

    void replaceLinks(List<BlockPos> offsets) {
        if (links.equals(offsets)) return;
        links.clear();
        links.addAll(offsets);
        missing.clear();
        notifyUpdate();
    }

    void removeLink(BlockPos offset) {
        missing.remove(offset);
        if (links.remove(offset)) notifyUpdate();
    }

    public void updateSignal() {
        if (level == null) return;
        boolean up = level.hasNeighborSignal(worldPosition);
        signalKnown = true;
        if (up != gearUp) {
            gearUp = up;
            notifyUpdate();
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        if (level != null && !level.isClientSide) synchronized (LOADED) {
            LOADED.add(this);
        }
    }

    @Override
    public void invalidate() {
        super.invalidate();
        synchronized (LOADED) {
            LOADED.remove(this);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level != null && !level.isClientSide && !signalKnown) updateSignal();
    }

    @Override
    public void lazyTick() {
        super.lazyTick();
        if (level == null || level.isClientSide) return;
        pruneLinks();
        boolean c = computeConflict();
        int g = 0;
        boolean o = false;
        for (BlockPos off : links) {
            if (level.getBlockEntity(worldPosition.offset(off)) instanceof HydraulicLegMountBlockEntity m && worldPosition.equals(m.controllerPos())) {
                if (m.leg().statusGrounded()) g++;
                if (m.leg().statusOverreach()) o = true;
            }
        }
        boolean capChanged = updateRideHeightCap();
        if (capChanged || c != conflict || g != grounded || o != overreach) {
            conflict = c;
            grounded = g;
            overreach = o;
            sendData();
        }
    }

    /** Caps the Ride Height scroll to the longest Linked Limb Reach and clamps the setting to it. */
    private boolean updateRideHeightCap() {
        int cap = rideHeightCap();
        if (cap < 0) {
            if (rideHeight.max() != RIDE_HEIGHT_LIMIT_TENTHS) return false;
            rideHeight.between(0, Math.max(RIDE_HEIGHT_MAX_TENTHS, rideHeight.getValue()));
            return true;
        }
        if (cap == rideHeight.max()) return false;
        rideHeight.between(0, cap);
        if (rideHeight.getValue() > cap) rideHeight.setValue(cap);
        return true;
    }

    /** Longest Limb Reach among the Linked Legs in tenths, or -1 while a Linked Leg is not loaded or not resolved. */
    private int rideHeightCap() {
        float longest = 0;
        for (BlockPos off : links) {
            BlockPos legPos = worldPosition.offset(off);
            if (!level.isLoaded(legPos)) return -1;
            if (!(level.getBlockEntity(legPos) instanceof HydraulicLegMountBlockEntity m) || !worldPosition.equals(m.controllerPos())) return -1;
            Limb limb = m.limb();
            if (limb != null) longest = Math.max(longest, limb.reach());
        }
        if (longest <= 0) return RIDE_HEIGHT_MAX_TENTHS;
        return Math.min(RIDE_HEIGHT_LIMIT_TENTHS, (int) Math.floor(longest * 10 + 1e-4));
    }

    private void pruneLinks() {
        for (BlockPos off : new ArrayList<>(links)) {
            BlockPos legPos = worldPosition.offset(off);
            if (!level.isLoaded(legPos)) continue;
            boolean ok = level.getBlockEntity(legPos) instanceof HydraulicLegMountBlockEntity m && worldPosition.equals(m.controllerPos());
            if (ok) {
                missing.remove(off);
            } else if (missing.merge(off, 1, Integer::sum) >= PRUNE_AFTER_CHECKS) {
                removeLink(off);
            }
        }
    }

    private boolean computeConflict() {
        if (links.isEmpty()) return false;
        Object ship = Linking.shipAt(level, worldPosition);
        synchronized (LOADED) {
            for (LegControllerBlockEntity other : LOADED) {
                if (other == this || other.isRemoved() || other.level != level || other.links.isEmpty()) continue;
                if (!Objects.equals(Linking.shipAt(level, other.worldPosition), ship)) continue;
                if (other.rideHeightSetting() != rideHeightSetting()) return true;
            }
        }
        return false;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        super.write(tag, registries, clientPacket);
        ListTag list = new ListTag();
        for (BlockPos p : links) list.add(new IntArrayTag(new int[]{p.getX(), p.getY(), p.getZ()}));
        tag.put(LegworksIds.NBT_LINKS, list);
        if (clientPacket) {
            tag.putBoolean(NBT_CONFLICT, conflict);
            tag.putBoolean(NBT_GEAR_UP, gearUp);
            tag.putInt(NBT_GROUNDED, grounded);
            tag.putBoolean(NBT_OVERREACH, overreach);
            tag.putInt(NBT_RIDE_HEIGHT_MAX, rideHeight.max());
        }
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        if (clientPacket && rideHeight != null && tag.contains(NBT_RIDE_HEIGHT_MAX)) rideHeight.between(0, tag.getInt(NBT_RIDE_HEIGHT_MAX));
        super.read(tag, registries, clientPacket);
        links.clear();
        for (Tag t : tag.getList(LegworksIds.NBT_LINKS, Tag.TAG_INT_ARRAY)) {
            int[] a = ((IntArrayTag) t).getAsIntArray();
            if (a.length == 3) links.add(new BlockPos(a[0], a[1], a[2]));
        }
        if (clientPacket) {
            conflict = tag.getBoolean(NBT_CONFLICT);
            gearUp = tag.getBoolean(NBT_GEAR_UP);
            grounded = tag.getInt(NBT_GROUNDED);
            overreach = tag.getBoolean(NBT_OVERREACH);
        }
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        new LangBuilder(LegworksIds.MOD_ID).translate("goggles.leg_controller").forGoggles(tooltip);
        for (Component line : LegStatusText.controller(this)) new LangBuilder(LegworksIds.MOD_ID).add(line).forGoggles(tooltip, 1);
        return true;
    }
}
