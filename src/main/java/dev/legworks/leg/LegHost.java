package dev.legworks.leg;

import dev.legworks.limb.Limb;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A block entity that owns a {@link Leg} and a {@link LimbSlot}. Implemented by every Leg Mount type.
 */
public interface LegHost {
    Leg leg();

    LimbSlot limbSlot();

    LegSettings legSettings();

    /** Server side: the fitted Limb changed; persist and sync it. */
    void onLimbSlotChanged();

    @Nullable
    default Limb limb() {
        return limbSlot().limb();
    }

    default BlockEntity asBlockEntity() {
        return (BlockEntity) this;
    }
}
