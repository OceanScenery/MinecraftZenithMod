package com.oceanscenery.zenith.event;

import com.oceanscenery.agent.ZenithAgentInstaller;
import com.oceanscenery.zenith.TheZenithMod;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Setup {
    @SubscribeEvent(receiveCanceled = true, priority = EventPriority.LOWEST)
    public static void onSetup(FMLLoadCompleteEvent event) {
        event.enqueueWork(ZenithAgentInstaller::install);
    }
}
