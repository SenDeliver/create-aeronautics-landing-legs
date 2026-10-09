package dev.legworks.registry;

import dev.legworks.LegworksIds;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class LegworksCreativeTabs {
    private static final DeferredRegister<CreativeModeTab> REGISTER = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LegworksIds.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = REGISTER.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.legworks"))
            .icon(() -> LegworksBlocks.SIMPLE_LEG_MOUNT.asStack())
            .displayItems((params, out) -> {
                out.accept(LegworksBlocks.SIMPLE_LEG_MOUNT);
                out.accept(LegworksBlocks.HYDRAULIC_LEG_MOUNT);
                out.accept(LegworksBlocks.LEG_CONTROLLER);
                out.accept(LegworksItems.SHORT_LIMB);
                out.accept(LegworksItems.LONG_LIMB);
                out.accept(LegworksItems.HEAVY_LIMB);
            })
            .build());

    private LegworksCreativeTabs() {
    }

    public static void register(IEventBus modBus) {
        REGISTER.register(modBus);
    }
}
