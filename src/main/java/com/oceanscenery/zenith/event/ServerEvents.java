package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mixin_extension.IZenithLivingEntityExtension;
import com.oceanscenery.zenith.util.StaticCollections;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Iterator;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID)
public class ServerEvents {
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Pre event) {
        Iterator<ServerTickTask> iter = StaticCollections.taskList.iterator();
        while (iter.hasNext()) {
            ServerTickTask task = iter.next();
            task.tick();
            if (task.isCompleted()) {
                iter.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onStop(ServerStoppedEvent event) {
        StaticCollections.taskList.clear();
        StaticCollections.checkedVictims.clear();
    }

    @SubscribeEvent
    public static void onEntityRemoved(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            IZenithLivingEntityExtension.of(living).zenith$setDead(false);
        }
    }
}
