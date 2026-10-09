package dev.legworks.registry;

import com.simibubi.create.content.processing.sequenced.SequencedAssemblyItem;
import com.tterrag.registrate.util.entry.ItemEntry;
import dev.legworks.LegworksIds;
import dev.legworks.limb.Limb;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;

import static dev.legworks.Legworks.REGISTRATE;

public final class LegworksItems {
    public static final ItemEntry<Item> SHORT_LIMB = limb(LegworksIds.SHORT_LIMB, Limb.SHORT);
    public static final ItemEntry<Item> LONG_LIMB = limb(LegworksIds.LONG_LIMB, Limb.LONG);
    public static final ItemEntry<Item> HEAVY_LIMB = limb(LegworksIds.HEAVY_LIMB, Limb.HEAVY);

    public static final ItemEntry<SequencedAssemblyItem> INCOMPLETE_HYDRAULIC_LEG_MOUNT = REGISTRATE
            .item(LegworksIds.INCOMPLETE_HYDRAULIC_LEG_MOUNT, SequencedAssemblyItem::new)
            .model((ctx, prov) -> incompleteHydraulicModel(prov.withExistingParent(ctx.getName(), prov.mcLoc("block/block"))))
            .register();

    private LegworksItems() {
    }

    public static void init() {
    }

    private static void incompleteHydraulicModel(ItemModelBuilder b) {
        b.texture("particle", LegworksModels.create("block/brass_casing"))
                .texture("casing", LegworksModels.create("block/brass_casing"))
                .texture("gearbox", LegworksModels.create("block/brass_gearbox"))
                .texture("side", LegworksIds.id("block/hydraulic_leg_mount_side_incomplete"))
                .texture("rod", LegworksIds.id("block/hydraulic_rod"));
        LegworksModels.hydraulicHull(b.element());
        LegworksModels.hydraulicCore(b.element(), true, "#side", false)
                .face(Direction.UP).uvs(0, 0, 16, 16).texture("#casing").end();
        b.element().from(6.5f, 14, 6.5f).to(9.5f, 19, 9.5f).allFaces((d, f) -> f.texture("#rod"));
        b.element().from(5.5f, 19, 5.5f).to(10.5f, 20, 10.5f)
                .face(Direction.NORTH).texture("#rod").end()
                .face(Direction.SOUTH).texture("#rod").end()
                .face(Direction.EAST).texture("#rod").end()
                .face(Direction.WEST).texture("#rod").end()
                .face(Direction.UP).texture("#rod").end();
    }

    private static ItemEntry<Item> limb(String name, Limb limb) {
        return REGISTRATE.item(name, Item::new)
                .properties(p -> p.stacksTo(16).component(LegworksDataComponents.LIMB.get(), limb))
                .model((ctx, prov) -> prov.generated(ctx, prov.modLoc("item/" + ctx.getName())))
                .register();
    }
}
