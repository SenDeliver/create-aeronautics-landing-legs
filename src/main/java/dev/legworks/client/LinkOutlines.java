package dev.legworks.client;

import dev.legworks.content.controller.LegControllerBlockEntity;
import dev.legworks.content.controller.LegControllerItem;
import dev.legworks.content.controller.LinkSelection;
import net.createmod.catnip.outliner.Outliner;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * While a Leg Controller item is held: outlines the selected (or looked-at) Controller and its Linked Legs, or the
 * Legs picked for a Controller not placed yet. Positions are plot coordinates on a Ship; Sable draws them on the Ship.
 */
final class LinkOutlines {
    private static final int CONTROLLER = 0x7FC8FF;
    private static final int LINKED = 0x9EF173;
    private static final int PENDING = 0xFFCB74;

    private LinkOutlines() {
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof LegControllerItem)) return;
        LinkSelection sel = LegControllerItem.selection(held);
        BlockPos ctrl = sel.controller().orElse(null);
        if (ctrl == null && mc.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                && mc.level.getBlockEntity(hit.getBlockPos()) instanceof LegControllerBlockEntity) ctrl = hit.getBlockPos();
        if (ctrl != null && mc.level.getBlockEntity(ctrl) instanceof LegControllerBlockEntity c) {
            show("controller", ctrl, CONTROLLER);
            for (BlockPos off : c.links()) show("link", ctrl.offset(off), LINKED);
        }
        for (BlockPos p : sel.pending()) show("pending", p, PENDING);
    }

    private static void show(String kind, BlockPos pos, int color) {
        Outliner.getInstance().showAABB("legworks_" + kind + pos.asLong(), new AABB(pos).deflate(1 / 32.0))
                .colored(color).lineWidth(1 / 16f);
    }
}
