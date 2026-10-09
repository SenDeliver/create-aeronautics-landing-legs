package dev.legworks.compat.jade;

import dev.legworks.LegworksIds;
import dev.legworks.content.LegStatusText;
import dev.legworks.content.controller.LegControllerBlock;
import dev.legworks.content.controller.LegControllerBlockEntity;
import dev.legworks.content.hydraulic.HydraulicLegMountBlock;
import dev.legworks.content.simple.SimpleLegMountBlock;
import dev.legworks.leg.LegHost;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

/** Jade: the same Leg status as the Engineer's Goggles, read from the synced block entity. */
@WailaPlugin(LegworksIds.MOD_ID)
public class LegworksJadePlugin implements IWailaPlugin {
    static final ResourceLocation UID = LegworksIds.id("leg_status");

    @Override
    public void register(IWailaCommonRegistration registration) {
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Provider.INSTANCE, SimpleLegMountBlock.class);
        registration.registerBlockComponent(Provider.INSTANCE, HydraulicLegMountBlock.class);
        registration.registerBlockComponent(Provider.INSTANCE, LegControllerBlock.class);
    }

    enum Provider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            List<? extends Component> lines;
            if (accessor.getBlockEntity() instanceof LegControllerBlockEntity c) lines = LegStatusText.controller(c);
            else if (accessor.getBlockEntity() instanceof LegHost host) lines = LegStatusText.mount(host);
            else return;
            for (Component line : lines) tooltip.add(line);
        }

        @Override
        public ResourceLocation getUid() {
            return UID;
        }
    }
}
