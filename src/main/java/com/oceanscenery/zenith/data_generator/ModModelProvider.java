package com.oceanscenery.zenith.data_generator;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.client.ZenithProjectileRenderer;
import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.*;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.properties.numeric.CustomModelDataProperty;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Holder;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.client.model.generators.template.ExtendedModelTemplateBuilder;

import java.util.ArrayList;
import java.util.stream.Stream;

public class ModModelProvider extends ModelProvider {
    public ModModelProvider(PackOutput output) {
        super(output, TheZenithMod.MOD_ID);
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        ArrayList<ItemModel.Unbaked> unbakeds=new ArrayList<>();
        ArrayList<RangeSelectItemModel.Entry> entry=new ArrayList<>();

        Identifier zenith_model=itemModels.createFlatItemModel(
                ZenithItems.ZENITH.get(),
                ModelTemplates.FLAT_HANDHELD_ITEM
        );

        ModelTemplate template=ExtendedModelTemplateBuilder.of(ModelTemplates.FLAT_HANDHELD_ITEM)
                .transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
                        transformVecBuilder -> transformVecBuilder.rotation(149.49f, -87.13f, 111.83f)
                                .translation(2.5f,5f,0f)
                ).transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                        transformVecBuilder -> transformVecBuilder.rotation(-147.32f, 74.07f, -143.41f)
                                .translation(0.25f, 3.5f, 1f)
                ).transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        transformVecBuilder -> transformVecBuilder.rotation(-89.87f, 87.79f, 128.89f)
                                .translation(0f, 6.75f, 0.5f)
                ).transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
                        transformVecBuilder -> transformVecBuilder.rotation(40.04f, -82.94f, -9.32f)
                                .translation(0, 6.5f, 1.75f)
                ).transform(ItemDisplayContext.GROUND,
                        transformVecBuilder -> transformVecBuilder.translation(0f,2.5f,0f)
                                .scale(0.5f,0.5f,0.5f)
                ).build();

        int i=0;
        for(String str: ZenithProjectileRenderer.SWORD_MODEL){
            if(str.equals("zenith")){
                continue;
            }
            i++;
            Identifier id=template.create(
                    Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID,"item/"+str),
                    TextureMapping.layer0(new Material(Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID, "item/"+str))),
                    itemModels.modelOutput
            );
            unbakeds.add(ItemModelUtils.plainModel(id));
            entry.add(ItemModelUtils.override(ItemModelUtils.plainModel(id),i));
        }

        ItemModel.Unbaked fm=ItemModelUtils.rangeSelect(
                new CustomModelDataProperty(0),
                ItemModelUtils.plainModel(zenith_model),
                entry
        );

        itemModels.itemModelOutput.accept(
                ZenithItems.ZENITH.get(),
                fm
        );
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return ZenithItems.ITEMS.getEntries().stream();
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return Stream.empty();
    }
}