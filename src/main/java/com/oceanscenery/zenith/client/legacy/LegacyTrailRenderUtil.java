package com.oceanscenery.zenith.client.legacy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oceanscenery.zenith.util.Vector3;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Vertex helpers used exclusively by the unregistered legacy trail. */
public final class LegacyTrailRenderUtil {
    private LegacyTrailRenderUtil() {
    }

    public static void createTrail(VertexConsumer vertex, Vector3[] np, Vector3[] cp, PoseStack poseStack, int alpha, int[] color) {
        Vec3 np0 = np[0].toVec3(), np1 = np[1].toVec3(), np2 = np[2].toVec3(), np3 = np[3].toVec3();
        Vec3 cp0 = cp[0].toVec3(), cp1 = cp[1].toVec3(), cp2 = cp[2].toVec3(), cp3 = cp[3].toVec3();

        addVertex(vertex, np0, np1, cp1, cp0, poseStack, alpha, color);
        addVertex(vertex, np2, np1, cp1, cp2, poseStack, alpha, color);
        addVertex(vertex, np2, np3, cp3, cp2, poseStack, alpha, color);
        addVertex(vertex, np0, np3, cp3, cp0, poseStack, alpha, color);
    }

    public static void addVertex(VertexConsumer vertex, Vec3 far_inner, Vec3 far_outer, Vec3 near_outer, Vec3 near_inner, PoseStack poseStack, int alpha, int[] color) {

        vertex.addVertex(poseStack.last().pose(), (float) far_inner.x, (float) far_inner.y, (float) far_inner.z)
            .setColor(color[0], color[1], color[2], alpha)
            .setUv(0, 0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(poseStack.last(), 0, 1, 0);
        vertex.addVertex(poseStack.last().pose(), (float) far_outer.x, (float) far_outer.y, (float) far_outer.z)
            .setColor(color[0], color[1], color[2], alpha)
            .setUv(0, 1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(poseStack.last(), 0, 1, 0);
        vertex.addVertex(poseStack.last().pose(), (float) near_outer.x, (float) near_outer.y, (float) near_outer.z)
            .setColor(color[0], color[1], color[2], alpha)
            .setUv(1, 1)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(poseStack.last(), 0, 1, 0);
        vertex.addVertex(poseStack.last().pose(), (float) near_inner.x, (float) near_inner.y, (float) near_inner.z)
            .setColor(color[0], color[1], color[2], alpha)
            .setUv(1, 0)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightTexture.FULL_BRIGHT)
            .setNormal(poseStack.last(), 0, 1, 0);
    }
}
