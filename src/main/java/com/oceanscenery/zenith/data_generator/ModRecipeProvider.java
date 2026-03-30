package com.oceanscenery.zenith.data_generator;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.concurrent.CompletableFuture;

public class ModRecipeProvider extends RecipeProvider {
    protected ModRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        super(registries, output);
    }

    @Override
    protected void buildRecipes() {
        shaped(
                RecipeCategory.COMBAT, ZenithItems.ZENITH.get()
        ).pattern("abc").pattern("def").pattern("ghi").define('a', Items.OAK_LOG).define('b',Items.COBBLESTONE)
                .define('c',Items.IRON_BLOCK).define('d',Items.GOLD_BLOCK).define('e', Items.NETHERITE_SWORD)
                .define('f',Items.COPPER_BLOCK).define('g',Items.DIAMOND_BLOCK).define('h',Items.NETHERITE_BLOCK)
                .define('i',Items.ENCHANTED_GOLDEN_APPLE).unlockedBy("get_netherite_sword_for_zenith",has(Items.NETHERITE_SWORD))
                .showNotification(true)
                .save(output);
    }

    public static class Runner extends RecipeProvider.Runner{
        protected Runner(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries) {
            super(packOutput, registries);
        }

        @Override
        protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
            return new ModRecipeProvider(registries, output);
        }

        @Override
        public String getName() {
            return "zenith_recipe_gen";
        }
    }
}
