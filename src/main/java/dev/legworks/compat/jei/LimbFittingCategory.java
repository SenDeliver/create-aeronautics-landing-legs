package dev.legworks.compat.jei;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.legworks.LegworksIds;
import dev.legworks.client.LegRenderer;
import dev.legworks.client.LimbStyle;
import dev.legworks.limb.Limb;
import dev.legworks.registry.LegworksItems;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class LimbFittingCategory implements IRecipeCategory<LimbFitting> {
    public static final RecipeType<LimbFitting> TYPE = RecipeType.create(LegworksIds.MOD_ID, "limb_fitting", LimbFitting.class);
    static final int WIDTH = 160;
    static final int HEIGHT = 90;
    static final float SCALE = 17;

    private final IDrawable icon;
    private final IDrawable slot;
    private final IDrawable plus;

    public LimbFittingCategory(IGuiHelper helper) {
        icon = helper.createDrawableItemLike(LegworksItems.SHORT_LIMB.get());
        slot = helper.getSlotDrawable();
        plus = helper.getRecipePlusSign();
    }

    @Override
    public RecipeType<LimbFitting> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("legworks.jei.fitting");
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LimbFitting recipe, IFocusGroup focuses) {
        builder.addInputSlot(80, 10).addItemStacks(recipe.limbs());
        builder.addInputSlot(114, 10).addItemStack(recipe.mount());
    }

    @Override
    public void draw(LimbFitting recipe, IRecipeSlotsView slots, GuiGraphics graphics, double mouseX, double mouseY) {
        slot.draw(graphics, 79, 9);
        slot.draw(graphics, 113, 9);
        plus.draw(graphics, 100, 13);
        drawLeg(recipe, slots, graphics);
        Font font = Minecraft.getInstance().font;
        int y = 34;
        for (var line : font.split(Component.translatable("legworks.jei.fitting.hint"), 92)) {
            graphics.drawString(font, line, 66, y, 0xFF505050, false);
            y += 10;
        }
    }

    private static void drawLeg(LimbFitting recipe, IRecipeSlotsView slots, GuiGraphics graphics) {
        ItemStack limbStack = slots.getSlotViews(RecipeIngredientRole.INPUT).get(0).getDisplayedItemStack()
                .orElseGet(() -> new ItemStack(LegworksItems.SHORT_LIMB.get()));
        Limb limb = Limb.of(limbStack);
        if (limb == null || !(recipe.mount().getItem() instanceof BlockItem mountItem)) return;
        BlockState mountState = mountItem.getBlock().defaultBlockState();
        PoseStack ps = graphics.pose();
        ps.pushPose();
        ps.translate(32, 12, 120);
        ps.scale(SCALE, -SCALE, SCALE);
        ps.mulPose(Axis.XP.rotationDegrees(20));
        ps.mulPose(Axis.YP.rotationDegrees(45));
        ps.translate(-0.5, -1, -0.5);
        Lighting.setupFor3DItems();
        Minecraft.getInstance().getBlockRenderer().renderSingleBlock(mountState, ps, graphics.bufferSource(), LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
        ps.translate(0.5, 0, 0.5);
        LegRenderer.drawLimb(ps, graphics.bufferSource(), limb, LimbStyle.of(limbStack), limb.nominalRest(), 0, LightTexture.FULL_BRIGHT);
        graphics.flush();
        ps.popPose();
    }
}
