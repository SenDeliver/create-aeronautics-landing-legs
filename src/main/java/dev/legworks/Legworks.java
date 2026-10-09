package dev.legworks;

import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import com.simibubi.create.foundation.item.TooltipModifier;
import dev.legworks.registry.LegworksDisplaySources;
import net.createmod.catnip.lang.FontHelper;
import dev.legworks.config.LegworksConfig;
import dev.legworks.data.LegworksDatagen;
import dev.legworks.content.controller.LinkMoves;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import dev.legworks.leg.LegColliders;
import dev.legworks.leg.ShipLegs;
import dev.legworks.registry.LegworksBlockEntityTypes;
import dev.legworks.registry.LegworksBlocks;
import dev.legworks.registry.LegworksCreativeTabs;
import dev.legworks.registry.LegworksDataComponents;
import dev.legworks.registry.LegworksItems;
import dev.legworks.registry.LegworksLang;
import dev.ryanhcode.sable.neoforge.event.ForgeSablePrePhysicsTickEvent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

@Mod(LegworksIds.MOD_ID)
public class Legworks {
    // Registrate's default tab is SEARCH, which already holds every item: adding them again crashes the creative inventory.
    // LegworksCreativeTabs lists our items itself.
    public static final CreateRegistrate REGISTRATE = CreateRegistrate.create(LegworksIds.MOD_ID)
            .defaultCreativeTab((ResourceKey<CreativeModeTab>) null);

    public Legworks(IEventBus modBus, ModContainer container) {
        REGISTRATE.registerEventListeners(modBus);
        REGISTRATE.setTooltipModifierFactory(item -> new ItemDescription.Modifier(item, FontHelper.Palette.STANDARD_CREATE)
                .andThen(TooltipModifier.mapNull(KineticStats.create(item))));
        LegworksDataComponents.register(modBus);
        LegworksDisplaySources.init();
        LegworksBlocks.init();
        LegworksItems.init();
        LegworksBlockEntityTypes.init();
        LegworksCreativeTabs.register(modBus);
        LegworksLang.init();
        LegworksDatagen.register(modBus);
        container.registerConfig(ModConfig.Type.SERVER, LegworksConfig.SPEC);

        NeoForge.EVENT_BUS.addListener((ForgeSablePrePhysicsTickEvent e) -> ShipLegs.flush());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Pre e) -> LinkMoves.flush());
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post e) -> LinkMoves.flush());
        NeoForge.EVENT_BUS.addListener((ServerStoppedEvent e) -> {
            LinkMoves.clear();
            ShipLegs.clear();
            LegColliders.clear(false);
        });
    }
}
