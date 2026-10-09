package dev.legworks.data;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.api.data.recipe.SequencedAssemblyRecipeGen;
import com.simibubi.create.content.fluids.transfer.FillingRecipe;
import com.simibubi.create.content.kinetics.deployer.DeployerApplicationRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import dev.legworks.LegworksIds;
import dev.legworks.registry.LegworksBlocks;
import dev.legworks.registry.LegworksItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.material.Fluids;

import java.util.concurrent.CompletableFuture;

/** Sequenced Assembly: Hydraulic Leg Mount from a Brass Casing (deploy a Mechanical Piston, Spout water, press). */
public class LegworksSequencedAssemblyRecipes extends SequencedAssemblyRecipeGen {
    GeneratedRecipe HYDRAULIC_LEG_MOUNT = create(LegworksIds.HYDRAULIC_LEG_MOUNT, b -> b
            .require(AllBlocks.BRASS_CASING.get())
            .transitionTo(LegworksItems.INCOMPLETE_HYDRAULIC_LEG_MOUNT.get())
            .addOutput(LegworksBlocks.HYDRAULIC_LEG_MOUNT.get(), 1)
            .loops(1)
            .addStep(DeployerApplicationRecipe::new, r -> r.require(AllBlocks.MECHANICAL_PISTON.get()))
            .addStep(FillingRecipe::new, r -> r.require(Fluids.WATER, 250))
            .addStep(PressingRecipe::new, r -> r));

    public LegworksSequencedAssemblyRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, LegworksIds.MOD_ID);
    }
}
