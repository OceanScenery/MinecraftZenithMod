package com.oceanscenery.zenith.data_gen;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithDamageTypes;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageEffects;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DeathMessageType;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Set;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID,bus = Mod.EventBusSubscriber.Bus.MOD)
public class DamageTypeGen {
    @SubscribeEvent
    public static void registerDamageTypes(GatherDataEvent event) {
        event.getGenerator().addProvider(
                false,
                new DatapackBuiltinEntriesProvider(
                        event.getGenerator().getPackOutput(),
                        event.getLookupProvider(),
                        new RegistrySetBuilder().add(
                                Registries.DAMAGE_TYPE, bootstrap->{
                                    bootstrap.register(
                                            ZenithDamageTypes.ZENITH,
                                            new DamageType(
                                                    "player",
                                                    DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                                                    0.1f,
                                                    DamageEffects.HURT,
                                                    DeathMessageType.DEFAULT)
                                    );
                                    bootstrap.register(
                                            ZenithDamageTypes.ZENITH_KNOCKBACK,
                                            new DamageType(
                                                    "player",
                                                    DamageScaling.WHEN_CAUSED_BY_LIVING_NON_PLAYER,
                                                    0.1f,
                                                    DamageEffects.HURT,
                                                    DeathMessageType.DEFAULT
                                            )
                                    );
                                }
                        ),
                        Set.of(TheZenithMod.MOD_ID)
                )
        );
    }
}
