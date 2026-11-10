package com.oceanscenery.zenith.data_generator;

import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

public class ModRecipeProvider extends RecipeProvider {
    protected ModRecipeProvider(BootstrapContext<Recipe<?>> recipeOutput, BootstrapContext<Advancement> advancementOutput) {
        super(recipeOutput, advancementOutput);
    }

    @Override
    protected void buildRecipes() {
        shaped(
            RecipeCategory.COMBAT, ZenithItems.ZENITH.get()
        ).pattern("abc").pattern("def").pattern("ghi").define('a', Items.OAK_LOG).define('b', Items.COBBLESTONE)
            .define('c', Items.IRON_BLOCK).define('d', Items.GOLD_BLOCK).define('e', Items.NETHERITE_SWORD)
            .define('f', Items.RAW_COPPER_BLOCK.asItem()).define('g', Items.DIAMOND_BLOCK).define('h', Items.NETHERITE_BLOCK)
            .define('i', Items.ENCHANTED_GOLDEN_APPLE).unlockedBy("get_netherite_sword_for_zenith", has(Items.NETHERITE_SWORD))
            .showNotification(true)
            .save(output);

        shaped(
            RecipeCategory.COMBAT, ZenithItems.ZENITH_UPGRADE
        ).pattern("aba").pattern("cde").pattern("afa")
            .define('a', Items.GOLD_BLOCK)
            .define('b', Items.NETHER_STAR)
            .define('c', Items.NETHERITE_INGOT)
            .define('d', ZenithItems.ZENITH.get())
            .define('e', Items.DIAMOND)
            .define('f', Items.ECHO_SHARD)
            .unlockedBy("get_zenith_for_zenith_upgrade", has(ZenithItems.ZENITH))
            .showNotification(true)
            .save(output);
    }
}
