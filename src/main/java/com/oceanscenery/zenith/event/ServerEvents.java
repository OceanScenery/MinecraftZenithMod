package com.oceanscenery.zenith.event;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mixin_extension.IZenithLivingEntityExtension;
import com.oceanscenery.zenith.util.StaticCollections;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID)
public class ServerEvents {
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            return;
        }

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
    public static void onServerStop(ServerStoppedEvent event) {
        StaticCollections.checkedVictims.clear();
        StaticCollections.taskList.clear();
    }

    @SubscribeEvent
    public static void onEntityRemoved(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof LivingEntity living) {
            IZenithLivingEntityExtension.of(living).zenith$setDead(false);
        }
    }
}
