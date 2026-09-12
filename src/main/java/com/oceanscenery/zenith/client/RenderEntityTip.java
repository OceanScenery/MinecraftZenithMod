package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID,value = Dist.CLIENT)
public class RenderEntityTip {
    public static int currId=-1;
    public static int serverBackId=-1;
    public static boolean canAttack=false;

    @SubscribeEvent
    public static void onSubmitGeometry(SubmitCustomGeometryEvent event){
        Minecraft mc=Minecraft.getInstance();
        Entity selected=mc.crosshairPickEntity;

        if(selected==null){
            return;
        }
        if(selected.getId()!=serverBackId || selected.getId()!=currId || canAttack){
            return;
        }

        Vec3 cam=mc.gameRenderer.mainCamera().position();
        AABB box=selected.getBoundingBox().inflate(0.1).move(-cam.x, -cam.y, -cam.z);

        event.getSubmitNodeCollector().submitCustomGeometry(
                event.getPoseStack(),
                RenderTypes.LINES,
                (pose, vertexConsumer) -> {
                    VoxelShape shape= Shapes.create(box);
                    shape.forAllEdges((x1, y1, z1, x2, y2, z2) -> {
                        Vector3f normal = (new Vector3f((float)(x2 - x1), (float)(y2 - y1), (float)(z2 - z1))).normalize();
                        vertexConsumer.addVertex(pose, (float)(x1), (float)(y1), (float)(z1)).setColor(0xFFFF3333).setNormal(pose, normal).setLineWidth(2f);
                        vertexConsumer.addVertex(pose, (float)(x2), (float)(y2), (float)(z2)).setColor(0xFFFF3333).setNormal(pose, normal).setLineWidth(2f);
                    });
                }
        );
    }
}
