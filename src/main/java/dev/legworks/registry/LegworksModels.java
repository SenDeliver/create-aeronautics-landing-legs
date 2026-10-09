package dev.legworks.registry;

import com.tterrag.registrate.providers.RegistrateBlockstateProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.client.model.generators.ModelFile;

/**
 * Block models. Create textures are referenced by path, never copied.
 */
final class LegworksModels {
    private LegworksModels() {
    }

    /**
     * Hydraulic Mount: shaft holes on model east/west ({@code holesOnX}) or north/south. {@code turned} turns the settings-face texture
     * by 90 degrees for the blockstates that would otherwise stand its two value-label panels on top of each other.
     */
    static ModelFile hydraulicMount(RegistrateBlockstateProvider prov, String name, boolean holesOnX, boolean turned) {
        BlockModelBuilder b = prov.models().withExistingParent("block/" + name, prov.mcLoc("block/block"))
                .texture("particle", create("block/brass_casing"))
                .texture("casing", create("block/brass_casing"))
                .texture("gearbox", create("block/brass_gearbox"))
                .texture("side", prov.modLoc("block/hydraulic_leg_mount_side"))
                .texture("socket", prov.modLoc("block/hydraulic_leg_mount_socket"));
        hydraulicHull(b.element());
        hydraulicCore(b.element(), holesOnX, "#side", turned);
        b.element().from(0, 14, 0).to(16, 16, 16)
                .face(Direction.NORTH).uvs(0, 0, 16, 2).texture("#casing").end()
                .face(Direction.SOUTH).uvs(0, 0, 16, 2).texture("#casing").end()
                .face(Direction.EAST).uvs(0, 0, 16, 2).texture("#casing").end()
                .face(Direction.WEST).uvs(0, 0, 16, 2).texture("#casing").end()
                .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#casing").end()
                .face(Direction.UP).uvs(0, 0, 16, 16).texture("#socket").cullface(Direction.UP).end()
                .end();
        return b;
    }

    static <T extends ModelBuilder<T>> void hydraulicHull(ModelBuilder<T>.ElementBuilder e) {
        e.from(0, 0, 0).to(16, 2, 16)
                .face(Direction.NORTH).uvs(0, 14, 16, 16).texture("#casing").end()
                .face(Direction.SOUTH).uvs(0, 14, 16, 16).texture("#casing").end()
                .face(Direction.EAST).uvs(0, 14, 16, 16).texture("#casing").end()
                .face(Direction.WEST).uvs(0, 14, 16, 16).texture("#casing").end()
                .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#casing").cullface(Direction.DOWN).end()
                .face(Direction.UP).uvs(0, 0, 16, 16).texture("#casing").end();
    }

    static <T extends ModelBuilder<T>> ModelBuilder<T>.ElementBuilder hydraulicCore(ModelBuilder<T>.ElementBuilder e, boolean holesOnX, String side, boolean turned) {
        e.from(0.95f, 2, 0.95f).to(15.05f, 14, 15.05f);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            boolean shaftFace = (d.getAxis() == Direction.Axis.X) == holesOnX;
            if (shaftFace) {
                e.face(d).uvs(1, 2, 15, 14).texture("#gearbox").end();
            } else if (turned) {
                e.face(d).uvs(2, 1, 14, 15).rotation(ModelBuilder.FaceRotation.CLOCKWISE_90).texture(side).end();
            } else {
                e.face(d).uvs(1, 2, 15, 14).texture(side).end();
            }
        }
        return e;
    }

    static ResourceLocation create(String path) {
        return ResourceLocation.fromNamespaceAndPath("create", path);
    }

    static ModelFile simpleMount(RegistrateBlockstateProvider prov) {
        BlockModelBuilder b = prov.models().withExistingParent("block/simple_leg_mount", prov.mcLoc("block/block"))
                .texture("particle", ResourceLocation.fromNamespaceAndPath("create", "block/andesite_casing"))
                .texture("casing", ResourceLocation.fromNamespaceAndPath("create", "block/andesite_casing"))
                .texture("side", prov.modLoc("block/simple_leg_mount_side"))
                .texture("socket", prov.modLoc("block/simple_leg_mount_socket"));
        b.element().from(0, 0, 0).to(16, 3, 16)
                .face(Direction.NORTH).uvs(0, 13, 16, 16).texture("#casing").end()
                .face(Direction.SOUTH).uvs(0, 13, 16, 16).texture("#casing").end()
                .face(Direction.EAST).uvs(0, 13, 16, 16).texture("#casing").end()
                .face(Direction.WEST).uvs(0, 13, 16, 16).texture("#casing").end()
                .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#casing").cullface(Direction.DOWN).end()
                .face(Direction.UP).uvs(0, 0, 16, 16).texture("#casing").end()
                .end();
        b.element().from(1, 3, 1).to(15, 13, 15)
                .face(Direction.NORTH).uvs(1, 3, 15, 13).texture("#side").end()
                .face(Direction.SOUTH).uvs(1, 3, 15, 13).texture("#side").end()
                .face(Direction.EAST).uvs(1, 3, 15, 13).texture("#side").end()
                .face(Direction.WEST).uvs(1, 3, 15, 13).texture("#side").end()
                .end();
        b.element().from(0, 13, 0).to(16, 16, 16)
                .face(Direction.NORTH).uvs(0, 0, 16, 3).texture("#casing").end()
                .face(Direction.SOUTH).uvs(0, 0, 16, 3).texture("#casing").end()
                .face(Direction.EAST).uvs(0, 0, 16, 3).texture("#casing").end()
                .face(Direction.WEST).uvs(0, 0, 16, 3).texture("#casing").end()
                .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#casing").end()
                .face(Direction.UP).uvs(0, 0, 16, 16).texture("#socket").cullface(Direction.UP).end()
                .end();
        return b;
    }

    static ModelFile controller(RegistrateBlockstateProvider prov) {
        BlockModelBuilder b = prov.models().withExistingParent("block/leg_controller", prov.mcLoc("block/block"))
                .texture("particle", create("block/brass_casing"))
                .texture("casing", create("block/brass_casing"))
                .texture("side", prov.modLoc("block/leg_controller_side"))
                .texture("top", prov.modLoc("block/leg_controller_top"));
        b.element().from(0, 0, 0).to(16, 2, 16)
                .face(Direction.NORTH).uvs(0, 14, 16, 16).texture("#casing").end()
                .face(Direction.SOUTH).uvs(0, 14, 16, 16).texture("#casing").end()
                .face(Direction.EAST).uvs(0, 14, 16, 16).texture("#casing").end()
                .face(Direction.WEST).uvs(0, 14, 16, 16).texture("#casing").end()
                .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#casing").cullface(Direction.DOWN).end()
                .face(Direction.UP).uvs(0, 0, 16, 16).texture("#casing").end()
                .end();
        b.element().from(1, 2, 1).to(15, 13, 15)
                .face(Direction.NORTH).uvs(1, 3, 15, 14).texture("#side").end()
                .face(Direction.SOUTH).uvs(1, 3, 15, 14).texture("#side").end()
                .face(Direction.EAST).uvs(1, 3, 15, 14).texture("#side").end()
                .face(Direction.WEST).uvs(1, 3, 15, 14).texture("#side").end()
                .end();
        b.element().from(0, 13, 0).to(16, 16, 16)
                .face(Direction.NORTH).uvs(0, 0, 16, 3).texture("#casing").end()
                .face(Direction.SOUTH).uvs(0, 0, 16, 3).texture("#casing").end()
                .face(Direction.EAST).uvs(0, 0, 16, 3).texture("#casing").end()
                .face(Direction.WEST).uvs(0, 0, 16, 3).texture("#casing").end()
                .face(Direction.DOWN).uvs(0, 0, 16, 16).texture("#casing").end()
                .face(Direction.UP).uvs(0, 0, 16, 16).texture("#top").cullface(Direction.UP).end()
                .end();
        return b;
    }
}
