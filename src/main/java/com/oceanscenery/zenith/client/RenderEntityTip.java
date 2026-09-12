package com.oceanscenery.zenith.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.item.ZenithItem;
import com.oceanscenery.zenith.registry.ZenithItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID,value = Dist.CLIENT)
public class RenderEntityTip {
    public static int currId=-1;
    public static int serverBackId=-1;
    public static boolean canAttack=false;

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event){
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_ENTITIES){
            return;
        }

        Minecraft mc=Minecraft.getInstance();
        Entity selected=mc.crosshairPickEntity;

        Player player=mc.player;
        if(player==null){
            return;
        }
        if(!player.getMainHandItem().is(ZenithItems.ZENITH) && !player.getOffhandItem().is(ZenithItems.ZENITH)){
            return;
        }

        if(selected==null || selected.getId()!=RenderEntityTip.serverBackId || selected.getId()!=RenderEntityTip.currId || RenderEntityTip.canAttack){
            return;
        }

        VertexConsumer vertexConsumer=mc.renderBuffers().bufferSource().getBuffer(RenderType.LINES);

        Vec3 cam=mc.gameRenderer.getMainCamera().getPosition();
        LevelRenderer.renderLineBox(event.getPoseStack(),vertexConsumer,selected.getBoundingBox().inflate(0.1).move(cam.scale(-1)),1f,0.2f,0.2f,0.9f);

        mc.renderBuffers().bufferSource().endBatch();
    }
}
