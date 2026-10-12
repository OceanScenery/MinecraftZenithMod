package com.oceanscenery.zenith.util;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;

public class RenderUtil {
    public static void createTrail(VertexConsumer vertex, Vector3[] np, Vector3[] cp, PoseStack.Pose pose, int alpha, int[] color) {
        Vec3 np0 = np[0].toVec3(), np1 = np[1].toVec3(), np2 = np[2].toVec3(), np3 = np[3].toVec3();
        Vec3 cp0 = cp[0].toVec3(), cp1 = cp[1].toVec3(), cp2 = cp[2].toVec3(), cp3 = cp[3].toVec3();

        addVertex(vertex, np0, np1, cp1, cp0, pose, alpha, color);
        addVertex(vertex, np1, np2, cp2, cp1, pose, alpha, color);
        addVertex(vertex, np2, np3, cp3, cp2, pose, alpha, color);
        addVertex(vertex, np3, np0, cp0, cp3, pose, alpha, color);
    }

    public static void addVertex(VertexConsumer vertex, Vec3 farInner, Vec3 farOuter, Vec3 nearOuter, Vec3 nearInner, PoseStack.Pose pose, int alpha, int[] color) {
        Vector3 normal = new Vector3(farInner.subtract(farOuter).cross(farOuter.subtract(nearInner)).normalize());
        vertex.addVertex(pose, farInner.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(0, 0)
            .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0], color[1], color[2], alpha).setNormal(pose, (float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
        vertex.addVertex(pose, farOuter.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(0, 1)
            .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0], color[1], color[2], alpha).setNormal(pose, (float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
        vertex.addVertex(pose, nearOuter.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(1, 1)
            .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0], color[1], color[2], alpha).setNormal(pose, (float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
        vertex.addVertex(pose, nearInner.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(1, 0)
            .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0], color[1], color[2], alpha).setNormal(pose, (float) normal.getX(), (float) normal.getY(), (float) normal.getZ());
    }
}
