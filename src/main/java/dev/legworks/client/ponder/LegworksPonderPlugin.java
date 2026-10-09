package dev.legworks.client.ponder;

import com.tterrag.registrate.util.entry.ItemProviderEntry;
import com.tterrag.registrate.util.entry.RegistryEntry;
import dev.legworks.LegworksIds;
import dev.legworks.registry.LegworksBlocks;
import dev.legworks.registry.LegworksItems;
import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.createmod.ponder.api.registration.PonderTagRegistrationHelper;
import net.createmod.ponder.foundation.PonderIndex;
import net.minecraft.resources.ResourceLocation;

import java.util.function.BiConsumer;

public class LegworksPonderPlugin implements PonderPlugin {
    public static final ResourceLocation LEGS = LegworksIds.id("legs");

    /** Datagen: Ponder scene and tag text for the lang file. */
    public static void provideLang(BiConsumer<String, String> out) {
        if (PonderIndex.streamPlugins().noneMatch(p -> p.getModId().equals(LegworksIds.MOD_ID))) PonderIndex.addPlugin(new LegworksPonderPlugin());
        PonderIndex.getLangAccess().provideLang(LegworksIds.MOD_ID, out);
    }

    @Override
    public String getModId() {
        return LegworksIds.MOD_ID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        PonderSceneRegistrationHelper<ItemProviderEntry<?, ?>> h = helper.withKeyFunction(RegistryEntry::getId);
        h.forComponents(LegworksBlocks.SIMPLE_LEG_MOUNT, LegworksItems.SHORT_LIMB, LegworksItems.LONG_LIMB, LegworksItems.HEAVY_LIMB)
                .addStoryBoard("simple_leg_fitting", LegPonderScenes::fitting, LEGS)
                .addStoryBoard("simple_leg_leveling", LegPonderScenes::leveling, LEGS)
                .addStoryBoard("simple_leg_facing", LegPonderScenes::facing, LEGS);
        h.forComponents(LegworksBlocks.HYDRAULIC_LEG_MOUNT)
                .addStoryBoard("hydraulic_leg_ship", LegPonderScenes::hydraulicPower, LEGS)
                .addStoryBoard("hydraulic_leg_ship", LegPonderScenes::hydraulicSoftening, LEGS);
        h.forComponents(LegworksBlocks.LEG_CONTROLLER)
                .addStoryBoard("hydraulic_leg_ship", LegPonderScenes::controllerLinking, LEGS)
                .addStoryBoard("hydraulic_leg_ship", LegPonderScenes::controllerRideHeight, LEGS);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<ResourceLocation> helper) {
        helper.registerTag(LEGS)
                .addToIndex()
                .item(LegworksBlocks.SIMPLE_LEG_MOUNT.get(), true, false)
                .title("Ship Legs")
                .description("Leg Mounts, Limbs and Leg Controllers that land Ships on uneven Ground and keep them level")
                .register();
        PonderTagRegistrationHelper<ItemProviderEntry<?, ?>> h = helper.withKeyFunction(RegistryEntry::getId);
        h.addToTag(LEGS)
                .add(LegworksBlocks.SIMPLE_LEG_MOUNT)
                .add(LegworksBlocks.HYDRAULIC_LEG_MOUNT)
                .add(LegworksBlocks.LEG_CONTROLLER)
                .add(LegworksItems.SHORT_LIMB)
                .add(LegworksItems.LONG_LIMB)
                .add(LegworksItems.HEAVY_LIMB);
    }
}
