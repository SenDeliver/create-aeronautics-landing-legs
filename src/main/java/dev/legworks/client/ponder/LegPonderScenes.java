package dev.legworks.client.ponder;

import com.simibubi.create.foundation.ponder.CreateSceneBuilder;
import dev.legworks.content.hydraulic.HydraulicLegMountBlockEntity;
import dev.legworks.leg.LegHost;
import dev.legworks.registry.LegworksBlocks;
import dev.legworks.registry.LegworksItems;
import net.createmod.catnip.math.Pointing;
import net.createmod.ponder.api.PonderPalette;
import net.createmod.ponder.api.element.ElementLink;
import net.createmod.ponder.api.element.WorldSectionElement;
import net.createmod.ponder.api.scene.SceneBuilder;
import net.createmod.ponder.api.scene.SceneBuildingUtil;
import net.createmod.ponder.api.scene.Selection;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.function.DoubleUnaryOperator;

/**
 * Ponder scenes. Ponder cannot run Sable physics, so each scene is a schematic whose Ship section is moved by hand
 * while the drawn Reach of every Leg is set to match (Leg#setDisplayReach).
 */
public final class LegPonderScenes {
    private static final int STEP = 3;

    private LegPonderScenes() {
    }

    private static void reach(CreateSceneBuilder scene, BlockPos pos, double reach) {
        scene.world().modifyBlockEntity(pos, BlockEntity.class, be -> {
            if (!(be instanceof LegHost host)) return;
            host.leg().setDisplayReach(reach);
            double kx = pos.getX() - 3, kz = pos.getZ() - 3;
            if (host.leg().isFoot() && kx * kx + kz * kz > 0.01) host.leg().setDisplayKnee(kx, 0, kz);
        });
    }

    private static void limb(CreateSceneBuilder scene, BlockPos pos, ItemStack stack) {
        scene.world().modifyBlockEntity(pos, BlockEntity.class, be -> {
            if (!(be instanceof LegHost host)) return;
            host.limbSlot().take();
            if (!stack.isEmpty()) host.limbSlot().set(stack.copy());
            host.leg().onLimbChanged();
        });
    }

    /** Moves a section by dy over the duration while each Leg's Reach follows reachAt(progress 0..1). */
    private static void glide(CreateSceneBuilder scene, ElementLink<WorldSectionElement> ship, double dy, int ticks,
                              List<BlockPos> legs, List<DoubleUnaryOperator> reachAt) {
        if (dy != 0) scene.world().moveSection(ship, new Vec3(0, dy, 0), ticks);
        for (int t = STEP; t <= ticks; t += STEP) {
            scene.idle(STEP);
            double p = (double) t / ticks;
            for (int i = 0; i < legs.size(); i++) reach(scene, legs.get(i), reachAt.get(i).applyAsDouble(p));
        }
    }

    private static DoubleUnaryOperator lerp(double from, double to) {
        return p -> from + (to - from) * p;
    }

    public static void fitting(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("simple_leg_fitting", "Fitting a Limb to a Leg Mount");
        scene.configureBasePlate(0, 0, 5);
        scene.scaleSceneView(0.9f);
        scene.showBasePlate();
        scene.idle(10);
        BlockPos mount = util.grid().at(2, 4, 1);
        scene.world().showSection(util.select().fromTo(1, 4, 1, 3, 5, 3), Direction.DOWN);
        scene.idle(20);
        scene.overlay().showText(70).text("Leg Mounts are placed on the hull of a Ship. Without a Limb they do nothing")
                .pointAt(util.vector().blockSurface(mount, Direction.WEST)).placeNearTarget().attachKeyFrame();
        scene.idle(80);

        scene.overlay().showControls(util.vector().blockSurface(mount, Direction.NORTH), Pointing.DOWN, 40).rightClick()
                .withItem(LegworksItems.SHORT_LIMB.asStack());
        scene.idle(7);
        reach(scene, mount, 0.4);
        limb(scene, mount, LegworksItems.SHORT_LIMB.asStack());
        scene.idle(5);
        reach(scene, mount, 3.0);
        scene.idle(20);
        scene.overlay().showText(80).text("Right-click it with a Limb to fit it. The Limb becomes the Leg below the Mount")
                .pointAt(util.vector().of(2.5, 2.5, 1.5)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        scene.overlay().showControls(util.vector().blockSurface(mount, Direction.NORTH), Pointing.DOWN, 30).rightClick()
                .withItem(LegworksItems.LONG_LIMB.asStack());
        scene.idle(7);
        limb(scene, mount, LegworksItems.LONG_LIMB.asStack());
        reach(scene, mount, 3.0);
        scene.idle(30);
        scene.overlay().showControls(util.vector().blockSurface(mount, Direction.NORTH), Pointing.DOWN, 30).rightClick()
                .withItem(LegworksItems.HEAVY_LIMB.asStack());
        scene.idle(7);
        limb(scene, mount, LegworksItems.HEAVY_LIMB.asStack());
        reach(scene, mount, 3.0);
        scene.idle(10);
        scene.overlay().showText(90).text("Any Limb fits any Leg Mount. The Limb decides the Leg's maximum Reach and its Stiffness")
                .pointAt(util.vector().of(2.5, 2.0, 1.5)).placeNearTarget().attachKeyFrame();
        scene.idle(100);

        scene.overlay().showControls(util.vector().blockSurface(mount, Direction.NORTH), Pointing.DOWN, 40).rightClick().whileSneaking();
        scene.idle(7);
        limb(scene, mount, ItemStack.EMPTY);
        scene.idle(10);
        scene.overlay().showText(70).text("Sneak-click with an empty hand to take the Limb back")
                .pointAt(util.vector().blockSurface(mount, Direction.WEST)).placeNearTarget().attachKeyFrame();
        scene.idle(80);
        scene.markAsFinished();
    }

    public static void leveling(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("simple_leg_leveling", "Leveling on uneven Ground");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.8f);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(5, 1, 0, 6, 1, 6), Direction.DOWN);
        scene.idle(10);
        List<BlockPos> legs = List.of(util.grid().at(1, 3, 2), util.grid().at(1, 3, 4), util.grid().at(5, 3, 2), util.grid().at(5, 3, 4));
        ElementLink<WorldSectionElement> ship = scene.world().showIndependentSection(util.select().fromTo(1, 3, 2, 5, 4, 4), Direction.DOWN);
        scene.world().moveSection(ship, new Vec3(0, 2, 0), 0);
        for (BlockPos p : legs) reach(scene, p, 3.0);
        scene.idle(20);
        scene.overlay().showText(80).text("Simple Legs Level a Ship on their own. They need no power and no Leg Controller")
                .pointAt(util.vector().of(3, 5.0, 2.0)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        DoubleUnaryOperator low = p -> Math.min(3.0, 2.0 + 2 * (1 - p));
        DoubleUnaryOperator high = p -> 1.0 + 2 * (1 - p);
        glide(scene, ship, -2, 45, legs, List.of(low, low, high, high));
        scene.idle(10);
        scene.overlay().showText(90).text("Each Leg finds its own Reach down to the Ground, so the deck stays level")
                .pointAt(util.vector().of(5.5, 2.2, 2.5)).placeNearTarget().attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80).text("Feet grip the Ground like rubber pads, so a Ship does not slide off a slope")
                .pointAt(util.vector().of(1.5, 1.05, 2.5)).placeNearTarget();
        scene.idle(90);

        Selection hole = util.select().fromTo(0, 0, 1, 2, 0, 5);
        scene.world().hideSection(hole, Direction.DOWN);
        scene.idle(15);
        scene.world().configureCenterOfRotation(ship, util.vector().of(5.5, 3.0, 3.5));
        scene.world().rotateSection(ship, 0, 0, 9, 30);
        glide(scene, ship, 0, 30, legs, List.of(lerp(2.0, 3.0), lerp(2.0, 3.0), lerp(1.0, 1.0), lerp(1.0, 1.0)));
        scene.idle(10);
        scene.overlay().showText(100).colored(PonderPalette.RED)
                .text("If the Ground is too uneven for the Limbs' Reach, the Legs stretch to their maximum and the Ship tilts toward the low side: Overreach")
                .pointAt(util.vector().of(1.5, 2.0, 2.5)).placeNearTarget().attachKeyFrame();
        scene.idle(110);
        scene.overlay().showText(80).text("Longer Limbs, or a Ride Height closer to the middle of the Reach, give the Legs more room")
                .pointAt(util.vector().of(3, 4.5, 2.0)).placeNearTarget();
        scene.idle(90);
        scene.markAsFinished();
    }

    public static void facing(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("simple_leg_facing", "Feet and Bumpers");
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.8f);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().showSection(util.select().fromTo(6, 1, 1, 6, 5, 5), Direction.DOWN);
        BlockPos foot = util.grid().at(2, 3, 2), bumper = util.grid().at(4, 4, 3);
        reach(scene, foot, 2.0);
        reach(scene, bumper, 1.0);
        ElementLink<WorldSectionElement> ship = scene.world().showIndependentSection(util.select().fromTo(1, 3, 2, 4, 4, 4), Direction.DOWN);
        scene.idle(25);
        scene.overlay().showText(80).text("A Leg Mount facing down is a Foot. Feet carry the Ship and take part in Leveling")
                .pointAt(util.vector().of(2.5, 1.2, 2.5)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        scene.overlay().showText(90).text("Facing sideways or up it is a Bumper: a spring that pushes back from Ground it meets. Bumpers never Level")
                .pointAt(util.vector().of(5.5, 4.5, 3.5)).placeNearTarget().attachKeyFrame();
        scene.idle(100);

        scene.world().moveSection(ship, new Vec3(0.4, 0, 0), 12);
        glide(scene, ship, 0, 12, List.of(bumper), List.of(lerp(1.0, 0.6)));
        scene.world().moveSection(ship, new Vec3(-0.4, 0, 0), 18);
        glide(scene, ship, 0, 18, List.of(bumper), List.of(lerp(0.6, 1.0)));
        scene.idle(10);
        scene.overlay().showText(80).text("Mounts face the side you place them on. Sneak while placing to flip them, or turn them with a Wrench")
                .pointAt(util.vector().blockSurface(bumper, Direction.UP)).placeNearTarget().attachKeyFrame();
        scene.idle(90);
        scene.markAsFinished();
    }

    private static final int[][] SHIP_LEGS = {{1, 3, 2}, {1, 3, 4}, {5, 3, 2}, {5, 3, 4}};

    private static List<BlockPos> shipLegs(SceneBuildingUtil util) {
        return java.util.Arrays.stream(SHIP_LEGS).map(a -> util.grid().at(a[0], a[1], a[2])).toList();
    }

    private static Selection kinetics(SceneBuildingUtil util) {
        return util.select().fromTo(1, 3, 2, 5, 3, 2).add(util.select().fromTo(1, 3, 4, 5, 3, 4)).substract(util.select().column(3, 2)).substract(util.select().column(3, 4));
    }

    private static ElementLink<WorldSectionElement> showShip(CreateSceneBuilder scene, SceneBuildingUtil util, float rpm, double reach) {
        scene.configureBasePlate(0, 0, 7);
        scene.scaleSceneView(0.75f);
        scene.showBasePlate();
        scene.idle(5);
        scene.world().setKineticSpeed(kinetics(util), rpm);
        for (BlockPos p : shipLegs(util)) reach(scene, p, reach);
        ElementLink<WorldSectionElement> ship = scene.world().showIndependentSection(
                util.select().fromTo(1, 3, 2, 5, 4, 4).add(util.select().position(3, 5, 3)), Direction.DOWN);
        scene.idle(20);
        return ship;
    }

    private static List<DoubleUnaryOperator> all(DoubleUnaryOperator f) {
        return List.of(f, f, f, f);
    }

    public static void hydraulicPower(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("hydraulic_leg_power", "Powering Hydraulic Legs");
        List<BlockPos> legs = shipLegs(util);
        ElementLink<WorldSectionElement> ship = showShip(scene, util, 0, 0.8);
        scene.world().moveSection(ship, new Vec3(0, -1.2, 0), 0);
        scene.idle(10);
        BlockPos motor = util.grid().at(2, 3, 2);
        scene.overlay().showText(80).text("Hydraulic Legs need rotational power. Without it they are fully Softened and the Ship rests on the Ground")
                .pointAt(util.vector().blockSurface(motor, Direction.NORTH).add(0, -1.2, 0)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        scene.world().setKineticSpeed(kinetics(util), 16);
        scene.effects().rotationSpeedIndicator(motor);
        glide(scene, ship, 1.2, 60, legs, all(lerp(0.8, 2.0)));
        scene.overlay().showText(90).text("Power them through the shaft (4 SU per RPM). Faster rotation changes the Reach faster")
                .pointAt(util.vector().blockSurface(motor, Direction.NORTH)).placeNearTarget().attachKeyFrame();
        scene.idle(100);

        scene.world().setKineticSpeed(kinetics(util), 0);
        glide(scene, ship, -1.2, 20, legs, all(lerp(2.0, 0.8)));
        scene.overlay().showText(80).colored(PonderPalette.RED).text("If power is lost or the network is overstressed, the Legs Soften fully and the Ship settles")
                .pointAt(util.vector().of(3, 3.0, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(90);
        scene.world().setKineticSpeed(kinetics(util), 16);
        glide(scene, ship, 1.2, 60, legs, all(lerp(0.8, 2.0)));

        BlockPos ctrl = util.grid().at(3, 5, 3);
        scene.overlay().showText(80).text("They also need a Linked Leg Controller on the same Ship. Unlinked, they stay inert")
                .pointAt(util.vector().blockSurface(ctrl, Direction.UP)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        BlockPos front = legs.get(0);
        Vec3 face = util.vector().blockSurface(front, Direction.NORTH);
        scene.overlay().showScrollInput(face.add(-0.22, 0, 0), Direction.NORTH, 70);
        scene.overlay().showText(80).text("Stiffness (25 to 200 percent): how hard the Leg's spring pushes back. Higher Stiffness sags less")
                .pointAt(face.add(-0.22, 0, 0)).placeNearTarget().attachKeyFrame();
        scene.idle(90);
        scene.overlay().showScrollInput(face.add(0.22, 0, 0), Direction.NORTH, 70);
        scene.overlay().showText(80).text("Damping (0.2 to 1.5): how quickly a bounce dies down. Low Damping rocks, high Damping settles fast")
                .pointAt(face.add(0.22, 0, 0)).placeNearTarget();
        double[] bounce = {-0.5, 0.8, -0.55, 0.35, -0.1};
        double h = 0;
        for (double b : bounce) {
            double from = 2.0 + h;
            h += b;
            glide(scene, ship, b, 8, legs, all(lerp(from, 2.0 + h)));
        }
        scene.idle(40);
        scene.markAsFinished();
    }

    public static void hydraulicSoftening(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("hydraulic_leg_softening", "Softening with Redstone");
        List<BlockPos> legs = shipLegs(util);
        ElementLink<WorldSectionElement> ship = showShip(scene, util, 16, 2.0);
        Selection levers = util.select().position(1, 5, 2).add(util.select().position(5, 5, 2))
                .add(util.select().position(1, 5, 4)).add(util.select().position(5, 5, 4));
        scene.world().showSectionAndMerge(levers, Direction.DOWN, ship);
        scene.idle(20);
        scene.overlay().showText(90).text("The top of a Hydraulic Leg Mount is the face opposite its Limb. A redstone signal there Softens the Leg")
                .pointAt(util.vector().topOf(1, 5, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(100);

        scene.world().toggleRedstonePower(levers);
        scene.effects().indicateRedstone(util.grid().at(1, 5, 2));
        glide(scene, ship, -1.1, 30, legs, all(lerp(2.0, 0.9)));
        scene.overlay().showText(90).text("Softened Legs push back less, so the Ship sinks toward the Ground. A stronger signal makes them softer")
                .pointAt(util.vector().of(1.5, 1.8, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(80).text("Use an Analog Lever for partial Softening, e.g. to kneel a parked Ship for loading")
                .pointAt(util.vector().topOf(5, 5, 2).add(0, -1.1, 0)).placeNearTarget();
        scene.idle(90);

        scene.world().toggleRedstonePower(levers);
        glide(scene, ship, 1.1, 40, legs, all(lerp(0.9, 2.0)));
        scene.overlay().showText(70).text("Remove the signal and the Legs lift the Ship back to its Ride Height")
                .pointAt(util.vector().of(3, 5.5, 3.5)).placeNearTarget();
        scene.idle(80);
        scene.markAsFinished();
    }

    public static void controllerLinking(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("leg_controller_linking", "Linking Legs to a Leg Controller");
        List<BlockPos> legs = shipLegs(util);
        showShip(scene, util, 16, 2.0);
        BlockPos ctrl = util.grid().at(3, 5, 3);
        scene.overlay().showText(80).text("A Leg Controller coordinates the Hydraulic Legs on its Ship. It needs no rotational power")
                .pointAt(util.vector().blockSurface(ctrl, Direction.WEST)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        ItemStack item = LegworksBlocks.LEG_CONTROLLER.asStack();
        scene.overlay().showControls(util.vector().topOf(ctrl), Pointing.DOWN, 40).rightClick().whileSneaking().withItem(item);
        scene.idle(7);
        scene.overlay().showOutline(PonderPalette.BLUE, "controller", util.select().position(ctrl), 330);
        scene.overlay().showText(80).text("Sneak-click the placed Leg Controller with a Leg Controller item to select it")
                .pointAt(util.vector().blockSurface(ctrl, Direction.WEST)).placeNearTarget().attachKeyFrame();
        scene.idle(90);

        for (int i = 0; i < legs.size(); i++) {
            BlockPos p = legs.get(i);
            scene.overlay().showControls(util.vector().blockSurface(p, p.getZ() < 3 ? Direction.NORTH : Direction.SOUTH), Pointing.DOWN, 20)
                    .rightClick().withItem(item);
            scene.idle(7);
            scene.overlay().showOutline(PonderPalette.GREEN, "leg" + i, util.select().position(p), 230 - i * 27);
            if (i == 0) {
                scene.overlay().showText(80).text("Then click each Hydraulic Leg to Link it. Click a Linked Leg again to unlink it")
                        .pointAt(util.vector().blockSurface(p, Direction.WEST)).placeNearTarget().attachKeyFrame();
            }
            scene.idle(20);
        }
        scene.idle(40);
        scene.overlay().showText(90).text("Up to 8 Legs within 16 blocks, all on the same Ship. A Leg belongs to one Controller at a time")
                .pointAt(util.vector().of(3, 5.5, 3.5)).placeNearTarget().attachKeyFrame();
        scene.idle(100);
        scene.overlay().showText(90).text("You can also click the Legs first and then place the Controller: they are Linked as it is placed. Linking works before the Ship is assembled")
                .pointAt(util.vector().blockSurface(ctrl, Direction.UP)).placeNearTarget();
        scene.idle(100);
        scene.markAsFinished();
    }

    public static void controllerRideHeight(SceneBuilder builder, SceneBuildingUtil util) {
        CreateSceneBuilder scene = new CreateSceneBuilder(builder);
        scene.title("leg_controller_ride_height", "Ride Height and Gear Up");
        List<BlockPos> legs = shipLegs(util);
        ElementLink<WorldSectionElement> ship = showShip(scene, util, 32, 2.0);
        scene.scaleSceneView(0.65f);
        scene.setSceneOffsetY(-1.0f);
        BlockPos ctrl = util.grid().at(3, 5, 3);
        scene.overlay().showScrollInput(util.vector().topOf(ctrl), Direction.UP, 60);
        scene.overlay().showText(80).text("Scroll the Leg Controller's value to set the Ride Height: how high its Legs hold the deck")
                .pointAt(util.vector().topOf(ctrl)).placeNearTarget().attachKeyFrame();
        scene.idle(30);
        glide(scene, ship, 0.8, 40, legs, all(lerp(2.0, 2.8)));
        scene.idle(30);
        scene.overlay().showText(90).text("Auto, the default, holds each Leg at its Limb's own height. Several Controllers on one Ship settle in between")
                .pointAt(util.vector().topOf(ctrl).add(0, 0.8, 0)).placeNearTarget();
        scene.idle(100);

        glide(scene, ship, 1.0, 30, legs, all(p -> Math.min(3.0, 2.8 + 1.0 * p)));
        scene.idle(10);
        BlockPos lever = util.grid().at(4, 5, 3);
        scene.world().showSectionAndMerge(util.select().position(lever), Direction.DOWN, ship);
        scene.idle(15);
        scene.world().toggleRedstonePower(util.select().position(lever));
        scene.effects().indicateRedstone(lever);
        for (BlockPos p : legs) {
            scene.world().modifyBlockEntity(p, HydraulicLegMountBlockEntity.class, be -> be.setDisplayGearUp(true));
        }
        scene.overlay().showText(90).text("A redstone signal on the Leg Controller folds its Legs against the Ship for flight: Gear Up")
                .pointAt(util.vector().blockSurface(lever, Direction.UP).add(0, 1.8, 0)).placeNearTarget().attachKeyFrame();
        scene.idle(100);
        scene.world().toggleRedstonePower(util.select().position(lever));
        for (BlockPos p : legs) {
            scene.world().modifyBlockEntity(p, HydraulicLegMountBlockEntity.class, be -> be.setDisplayGearUp(false));
        }
        scene.overlay().showText(80).text("Without a signal the Legs unfold again, ready for Landing: Gear Down. Higher RPM folds them faster")
                .pointAt(util.vector().of(1.5, 3.5, 2)).placeNearTarget().attachKeyFrame();
        scene.idle(60);
        glide(scene, ship, -1.0, 40, legs, all(p -> Math.min(3.0, 2.8 + 1.0 * (1 - p))));
        scene.idle(30);
        scene.markAsFinished();
    }
}
