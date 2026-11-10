package com.oceanscenery.zenith.event;

import com.oceanscenery.agent.ZenithAgentInstaller;
import com.oceanscenery.zenith.TheZenithMod;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID)
public class Setup {
    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
    public static void onSetup(FMLLoadCompleteEvent event) {
        event.enqueueWork(ZenithAgentInstaller::install);
    }
}
