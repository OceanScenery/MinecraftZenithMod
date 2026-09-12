package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithKeyMappings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID,value = Dist.CLIENT)
public class RegistryKeyMapping {
    @SubscribeEvent
    public static void addKeyMapping(RegisterKeyMappingsEvent event){
        event.register(ZenithKeyMappings.TOGGLE_ATTACK_BLACKLIST);
    }
}
