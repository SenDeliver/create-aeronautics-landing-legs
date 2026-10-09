package dev.legworks.registry;

import dev.legworks.LegworksIds;
import dev.legworks.content.controller.LinkSelection;
import dev.legworks.limb.Limb;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LegworksDataComponents {
    private static final DeferredRegister.DataComponents REGISTER = DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, LegworksIds.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Limb>> LIMB = REGISTER.registerComponentType(
            LegworksIds.LIMB_COMPONENT, b -> b.persistent(Limb.CODEC).networkSynchronized(Limb.STREAM_CODEC).cacheEncoding());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LinkSelection>> LINK_SELECTION = REGISTER.registerComponentType(
            LegworksIds.LINK_SELECTION_COMPONENT, b -> b.networkSynchronized(LinkSelection.STREAM_CODEC));

    private LegworksDataComponents() {
    }

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
