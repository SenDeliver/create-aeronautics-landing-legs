package dev.legworks.data;

import com.tterrag.registrate.providers.ProviderType;
import dev.legworks.client.ponder.LegworksPonderPlugin;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.data.event.GatherDataEvent;

import static dev.legworks.Legworks.REGISTRATE;

public final class LegworksDatagen {
    private LegworksDatagen() {
    }

    public static void register(IEventBus modBus) {
        REGISTRATE.addDataGenerator(ProviderType.RECIPE, LegworksCraftingRecipes::register);
        REGISTRATE.addDataGenerator(ProviderType.LANG, prov -> LegworksPonderPlugin.provideLang(prov::add));
        modBus.addListener(LegworksDatagen::gather);
    }

    private static void gather(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        gen.addProvider(event.includeServer(), new LegworksMechanicalCraftingRecipes(out, event.getLookupProvider()));
        gen.addProvider(event.includeServer(), new LegworksSequencedAssemblyRecipes(out, event.getLookupProvider()));
    }
}
