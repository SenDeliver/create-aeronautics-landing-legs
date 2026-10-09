package dev.legworks.dev.client;

import com.simibubi.create.foundation.item.ItemDescription;
import com.simibubi.create.foundation.item.KineticStats;
import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.lang.FontHelper;
import net.createmod.ponder.foundation.ui.PonderUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.BlockItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Dev-only client screens for screenshots: Ponder scenes, Create-style item tooltips (with and without Shift) and 4x item icons. */
final class DevScreens {
    private static final Logger LOG = LoggerFactory.getLogger("legworks-script");

    private DevScreens() {
    }

    static ItemStack stack(String id) {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(id)));
    }

    static void ponder(String id, int scene) {
        PonderUI ui = PonderUI.of(stack(id));
        ScreenOpener.open(ui);
        try {
            Method scroll = PonderUI.class.getDeclaredMethod("scroll", boolean.class);
            scroll.setAccessible(true);
            for (int i = 0; i < scene; i++) scroll.invoke(ui, true);
        } catch (ReflectiveOperationException e) {
            LOG.error("script ponder scroll", e);
        }
        LOG.info("script ponder {} scene {}", id, scene);
    }

    static void ponderSeek(int ticks) {
        if (Minecraft.getInstance().screen instanceof PonderUI ui) {
            ui.seekToTime(ticks);
            LOG.info("script ponder seek {} scene={}", ticks, ui.getActiveScene().getId());
        }
    }

    static void valueBoard(Minecraft mc) {
        if (!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)) return;
        if (!(mc.level.getBlockEntity(hit.getBlockPos()) instanceof com.simibubi.create.foundation.blockEntity.SmartBlockEntity be)) return;
        for (var b : be.getAllBehaviours()) {
            if (!(b instanceof com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBehaviour v) || !v.testHit(hit.getLocation())) continue;
            var board = v.createBoard(mc.player, hit);
            mc.setScreen(new com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsScreen(hit.getBlockPos(), board, v.getValueSettings(), x -> {}, v.netId()));
            LOG.info("script board {} {}", hit.getBlockPos().toShortString(), board.title().getString());
            return;
        }
        LOG.info("script board: no value box hit at {}", hit.getLocation());
    }

    static void tooltips(List<String> args) {
        boolean shift = !args.get(0).equals("noshift");
        Minecraft.getInstance().setScreen(new TooltipScreen(args.subList(1, args.size()).stream().map(DevScreens::stack).toList(), shift));
    }

    static void icons(List<String> ids) {
        Minecraft.getInstance().setScreen(new IconScreen(ids.stream().map(DevScreens::stack).toList()));
    }

    static final class IconScreen extends Screen {
        private final List<ItemStack> stacks;

        IconScreen(List<ItemStack> stacks) {
            super(Component.literal("icons"));
            this.stacks = stacks;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, width, height, 0xFFC6C6C6);
            int k = Math.max(1, Math.min(4, (width - 16) / (stacks.size() * 20)));
            int size = 16 * k, gap = 4 * k;
            int x0 = (width - stacks.size() * (size + gap) + gap) / 2, y0 = (height - size) / 2;
            for (int i = 0; i < stacks.size(); i++) {
                g.pose().pushPose();
                g.pose().translate(x0 + i * (size + gap), y0, 0);
                g.pose().scale(k, k, 1);
                g.renderItem(stacks.get(i), 0, 0);
                g.pose().popPose();
            }
        }

        @Override
        public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    static final class TooltipScreen extends Screen {
        private final List<ItemStack> stacks;
        private final boolean shift;

        TooltipScreen(List<ItemStack> stacks, boolean shift) {
            super(Component.literal("tooltips"));
            this.stacks = stacks;
            this.shift = shift;
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);
            int perRow = Math.max(1, (stacks.size() + 1) / 2);
            int x = 8, y = 8, colW = width / perRow;
            for (int i = 0; i < stacks.size(); i++) {
                ItemStack s = stacks.get(i);
                List<Component> lines = new ArrayList<>();
                lines.add(s.getHoverName());
                Item item = s.getItem();
                ItemDescription d = ItemDescription.create(item, FontHelper.Palette.STANDARD_CREATE);
                lines.addAll(shift ? d.linesOnShift() : d.lines());
                if (item instanceof BlockItem) lines.addAll(KineticStats.getKineticStats(((BlockItem) item).getBlock(), minecraft.player));
                int col = i % perRow, row = i / perRow;
                g.renderComponentTooltip(font, lines, x + col * colW, y + 24 + row * (height / 2));
            }
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }
}
