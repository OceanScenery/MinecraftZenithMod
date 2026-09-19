package com.oceanscenery.zenith.data_generator;

import com.oceanscenery.zenith.registry.ZenithDamageType;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber
public class DataGen {
    @SubscribeEvent
    public static void generate(GatherDataEvent.Client event) {
        event.createWorldRegistryObjects(
            new RegistrySetBuilder().add(
                Registries.DAMAGE_TYPE,
                bootstrapContext -> {
                    bootstrapContext.register(
                        ZenithDamageType.ZENITH,
                        new DamageType(
                            "player",
                            DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                            0.1f,
                            DamageEffects.HURT,
                            DeathMessageType.DEFAULT)
                    );
                    bootstrapContext.register(
                        ZenithDamageType.ZENITH_KNOCKBACK,
                        new DamageType(
                            "player",
                            DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                            0.1f,
                            DamageEffects.HURT,
                            DeathMessageType.DEFAULT
                        )
                    );
                }
            )
        );

        event.createReloadableRegistryObjects(
            new RegistrySetBuilder().add(
                RecipeProvider.asBootstrap(ModRecipeProvider::new)
            )
        );

        event.createProvider(ModModelProvider::new);
        event.createProvider((output -> new ModLanguageProvider(output, "zh_cn")));
        event.createProvider((output -> new ModLanguageProvider(output, "en_us")));
        event.createProvider(ModBlockTagProvider::new);
        event.createProvider(ModItemTagProvider::new);
        event.createProvider(ModDamageTypeTagProvider::new);
    }
}
