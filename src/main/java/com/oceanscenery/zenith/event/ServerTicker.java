package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Queue;

@Mod.EventBusSubscriber(modid=TheZenithMod.MOD_ID)
public class ServerTicker {
    public static Queue<ServerTickTask> taskList=new ArrayDeque<>();
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event){
        if(event.phase == TickEvent.Phase.END){
            return;
        }

        Iterator<ServerTickTask> iter=taskList.iterator();
        while(iter.hasNext()){
            ServerTickTask task=iter.next();
            task.tick();
            if(task.isCompleted()){
                iter.remove();
            }
        }
    }
}
