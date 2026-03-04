package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.registry.ModItems;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber
public class ModLogic {
    @SubscribeEvent
    public static void blockInteract(PlayerInteractEvent.RightClickBlock event){
        if(event.getItemStack().is(ModItems.ZENITH.get())){
            event.setUseBlock(TriState.FALSE);
        }
    }
    @SubscribeEvent
    public static void entityInteract(PlayerInteractEvent.EntityInteract event){
        if(event.getItemStack().is(ModItems.ZENITH.get())){
            event.setCanceled(true);
        }
    }
}
