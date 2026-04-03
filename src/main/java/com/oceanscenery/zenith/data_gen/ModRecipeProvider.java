package com.oceanscenery.zenith.data_gen;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {
    public ModRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> recipe) {
        ShapedRecipeBuilder.shaped(
                        RecipeCategory.COMBAT, ZenithItems.ZENITH.get()
                ).pattern("abc").pattern("def").pattern("ghi").define('a', Items.OAK_LOG).define('b',Items.COBBLESTONE)
                .define('c',Items.IRON_BLOCK).define('d',Items.GOLD_BLOCK).define('e', Items.NETHERITE_SWORD)
                .define('f',Items.COPPER_BLOCK).define('g',Items.DIAMOND_BLOCK).define('h',Items.NETHERITE_BLOCK)
                .define('i',Items.ENCHANTED_GOLDEN_APPLE).unlockedBy("get_netherite_sword_for_zenith",has(Items.NETHERITE_SWORD))
                .showNotification(true)
                .save(recipe,ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith"));
    }
}
