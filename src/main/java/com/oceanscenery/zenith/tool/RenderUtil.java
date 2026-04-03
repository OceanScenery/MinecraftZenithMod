package com.oceanscenery.zenith.tool;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

public class RenderUtil {
    public static void createTrail(VertexConsumer vertex, Vector3[] np, Vector3[] cp, PoseStack.Pose pose, int alpha, int[] color){
        Vec3 np0=np[0].toVec3(),np1=np[1].toVec3(),np2=np[2].toVec3(),np3=np[3].toVec3();
        Vec3 cp0=cp[0].toVec3(),cp1=cp[1].toVec3(),cp2=cp[2].toVec3(),cp3=cp[3].toVec3();

        addVertex(vertex,np0,np1,cp1,cp0,pose,alpha,color);
        addVertex(vertex,np2,np1,cp1,cp2,pose,alpha,color);
        addVertex(vertex,np2,np3,cp3,cp2,pose,alpha,color);
        addVertex(vertex,np0,np3,cp3,cp0,pose,alpha,color);
    }

    public static void addVertex(VertexConsumer vertex, Vec3 far_inner, Vec3 far_outer, Vec3 near_outer, Vec3 near_inner, PoseStack.Pose pose, int alpha, int[] color){
        Vector3 normal=new Vector3(far_inner.subtract(far_outer).cross(far_outer.subtract(near_inner)).normalize());
        vertex.addVertex(pose,far_inner.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(0,0)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0],color[1],color[2],alpha).setNormal(pose,(float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
        vertex.addVertex(pose,far_outer.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(0,1)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0],color[1],color[2],alpha).setNormal(pose,(float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
        vertex.addVertex(pose,near_outer.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(1,1)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0],color[1],color[2],alpha).setNormal(pose,(float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
        vertex.addVertex(pose,near_inner.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(1,0)
                .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0],color[1],color[2],alpha).setNormal(pose,(float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
    }
}
