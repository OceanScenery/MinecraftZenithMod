package com.oceanscenery.zenith.data_generator;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelBuilder;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output,ExistingFileHelper existingFileHelper) {
        super(output, TheZenithMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        ItemModelBuilder arkhalis=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "arkhalis"
                )
        );
        applyTransforms(arkhalis);
        ItemModelBuilder bee_keeper=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "bee_keeper"
                )
        );
        applyTransforms(bee_keeper);
        ItemModelBuilder blade_of_grass=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "blade_of_grass"
                )
        );
        applyTransforms(blade_of_grass);
        ItemModelBuilder blood_butcherer=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "blood_butcherer"
                )
        );
        applyTransforms(blood_butcherer);
        ItemModelBuilder copper_shortsword=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "copper_shortsword"
                )
        );
        applyTransforms(copper_shortsword);
        ItemModelBuilder enchanted_sword=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "enchanted_sword"
                )
        );
        applyTransforms(enchanted_sword);
        ItemModelBuilder excalibur=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "excalibur"
                )
        );
        applyTransforms(excalibur);
        ItemModelBuilder influx_waver=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "influx_waver"
                )
        );
        applyTransforms(influx_waver);
        ItemModelBuilder lights_bane=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "lights_bane"
                )
        );
        applyTransforms(lights_bane);
        ItemModelBuilder meowmere=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "meowmere"
                )
        );
        applyTransforms(meowmere);
        ItemModelBuilder muramasa=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "muramasa"
                )
        );
        applyTransforms(muramasa);
        ItemModelBuilder nights_edge=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "nights_edge"
                )
        );
        applyTransforms(nights_edge);
        ItemModelBuilder seedler=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "seedler"
                )
        );
        applyTransforms(seedler);
        ItemModelBuilder star_wrath=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "star_wrath"
                )
        );
        applyTransforms(star_wrath);
        ItemModelBuilder starfury=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "starfury"
                )
        );
        applyTransforms(starfury);
        ItemModelBuilder terra_blade=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "terra_blade"
                )
        );
        applyTransforms(terra_blade);
        ItemModelBuilder the_horsemans_blade=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "the_horsemans_blade"
                )
        );
        applyTransforms(the_horsemans_blade);
        ItemModelBuilder true_excalibur=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "true_excalibur"
                )
        );
        applyTransforms(true_excalibur);
        ItemModelBuilder true_nights_edge=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "true_nights_edge"
                )
        );
        applyTransforms(true_nights_edge);
        ItemModelBuilder volcano=handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "volcano"
                )
        );
        applyTransforms(volcano);


        handheldItem(
                ResourceLocation.fromNamespaceAndPath(
                        TheZenithMod.MOD_ID,
                        "zenith"
                )
        ).override()
                .predicate(ResourceLocation.parse("custom_model_data"), 1)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/arkhalis")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 2)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/bee_keeper")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 3)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/blade_of_grass")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 4)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/blood_butcherer")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 5)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/copper_shortsword")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 6)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/enchanted_sword")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 7)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/excalibur")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 8)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/influx_waver")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 9)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/lights_bane")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 10)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/meowmere")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 11)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/muramasa")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 12)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/nights_edge")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 13)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/seedler")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 14)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/star_wrath")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 15)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/starfury")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 16)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/terra_blade")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 17)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/the_horsemans_blade")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 18)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/true_excalibur")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 19)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/true_nights_edge")))
                .end()

                .override()
                .predicate(ResourceLocation.parse("custom_model_data"), 20)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/volcano")))
                .end();
    }

    public static void applyTransforms(ItemModelBuilder builder){
        builder.transforms().transform(ItemDisplayContext.FIRST_PERSON_LEFT_HAND).rotation(149.49f, -87.13f, 111.83f)
                .translation(2.5f, 5f, 0f).end().end()
                .transforms().transform(ItemDisplayContext.FIRST_PERSON_RIGHT_HAND).rotation(-147.32f, 74.07f, -143.41f)
                .translation(0.25f, 3.5f, 1f).end().end()
                .transforms().transform(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND).rotation(-89.87f, 87.79f, 128.89f)
                .translation(0f, 6.75f, 0.5f).end().end()
                .transforms().transform(ItemDisplayContext.THIRD_PERSON_LEFT_HAND).rotation(40.04f, -82.94f, -9.32f)
                .translation(0, 6.5f, 1.75f).end().end()
                .transforms().transform(ItemDisplayContext.GROUND).translation(0f, 2.5f, 0f).scale(0.5f,0.5f,0.5f).end();
    }
}