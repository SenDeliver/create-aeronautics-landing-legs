package dev.legworks.content;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BehaviourType;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour.ValueSettings;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntFunction;

/**
 * A Create value box holding an int step that is saved as a double under Legworks' own NBT key
 * ({@code group.key}, or top-level {@code key}), so several boxes can live on one block entity.
 */
public class SettingScroll extends ScrollValueBehaviour {
    private final BehaviourType<?> type;
    private final int netId;
    @Nullable
    private final String group;
    private final String key;
    private final double unit;
    private final IntFunction<Component> display;
    private final Component boardTitle;
    private final Component row;
    private int minValue;

    public SettingScroll(BehaviourType<?> type, int netId, @Nullable String group, String key, double unit, Component label,
                         Component boardTitle, Component row, SmartBlockEntity be, ValueBoxTransform slot, IntFunction<Component> display) {
        super(label, be, slot);
        this.boardTitle = boardTitle;
        this.row = row;
        this.type = type;
        this.netId = netId;
        this.group = group;
        this.key = key;
        this.unit = unit;
        this.display = display;
        withFormatter(v -> display.apply(v).getString());
    }

    public double stored() {
        return value * unit;
    }

    @Override
    public BehaviourType<?> getType() {
        return type;
    }

    @Override
    public int netId() {
        return netId;
    }

    @Override
    public String getClipboardKey() {
        return key;
    }

    @Override
    public void write(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        CompoundTag target = nbt;
        if (group != null) {
            target = nbt.getCompound(group);
            nbt.put(group, target);
        }
        target.putDouble(key, stored());
    }

    @Override
    public void read(CompoundTag nbt, HolderLookup.Provider registries, boolean clientPacket) {
        CompoundTag source = group == null ? nbt : nbt.getCompound(group);
        if (!source.contains(key)) return;
        value = Math.max(min(), Math.min(max, (int) Math.round(source.getDouble(key) / unit)));
    }

    private int min() {
        return minValue;
    }

    public int max() {
        return max;
    }

    @Override
    public ScrollValueBehaviour between(int min, int max) {
        minValue = min;
        return super.between(min, max);
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        int span = max - min();
        return new ValueSettingsBoard(boardTitle, span, Math.max(1, span / 7), ImmutableList.of(row),
                new ValueSettingsFormatter(v -> display.apply(v.value() + min()).copy()));
    }

    @Override
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, value - min());
    }

    @Override
    public void setValueSettings(Player player, ValueSettings settings, boolean ctrlDown) {
        if (!settings.equals(getValueSettings())) playFeedbackSound(this);
        setValue(settings.value() + min());
    }
}
