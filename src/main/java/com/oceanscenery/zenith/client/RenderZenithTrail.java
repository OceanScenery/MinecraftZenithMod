package com.oceanscenery.zenith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.util.PosUtil;
import com.oceanscenery.zenith.util.RenderUtil;
import com.oceanscenery.zenith.util.Vector3;
import com.oceanscenery.zenith.zenith_class.entity.ZenithProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID, value = Dist.CLIENT)
public final class RenderZenithTrail {
    private static final ResourceLocation TRAIL_TEXTURE =
        ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "textures/entity/trail.png");

    private static final RenderType TRAIL_TYPE = RenderType.entityTranslucentEmissive(TRAIL_TEXTURE);

    private RenderZenithTrail() {
    }

    public record TrailResult(Vector3 swordPos, @Nullable Vector3 round,
        boolean firstPerson, double distance, double progressAngle) {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        float partialTick = event.getPartialTick();
        Vec3 camera = event.getCamera().getPosition();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        PoseStack modelView = RenderSystem.getModelViewStack();
        modelView.pushPose();
        modelView.setIdentity();
        modelView.mulPose(Axis.XP.rotationDegrees(event.getCamera().getXRot()));
        modelView.mulPose(Axis.YP.rotationDegrees(event.getCamera().getYRot() + 180.0F));
        RenderSystem.applyModelViewMatrix();
        try {
            PoseStack poseStack = new PoseStack();
            poseStack.translate(-camera.x, -camera.y, -camera.z);

            boolean any = false;
            for (Entity candidate : minecraft.level.entitiesForRendering()) {
                if (!(candidate instanceof ZenithProjectile projectile)) {
                    continue;
                }
                poseStack.pushPose();
                VertexConsumer vertex = bufferSource.getBuffer(TRAIL_TYPE);
                TrailResult result = compute(projectile, partialTick, poseStack, vertex, normalOf(projectile));
                poseStack.popPose();
                if (result != null) {
                    any = true;
                }
            }
            if (any) {
                bufferSource.endBatch(TRAIL_TYPE);
            }
        } finally {
            modelView.popPose();
            RenderSystem.applyModelViewMatrix();
        }
    }

    public static Vector3 normalOf(ZenithProjectile entity) {
        com.oceanscenery.zenith.util.Quaternion[] fixed = entity.getFixedPose();
        return new Vector3(0, 1, 0).rot(fixed[2].multiply(fixed[1]).multiply(fixed[0])).normalize();
    }

    @Nullable
    public static TrailResult compute(ZenithProjectile entity, float partialTick, PoseStack poseStack, @Nullable VertexConsumer vertex, Vector3 normal) {
        if (!entity.isAlive() || entity.isRemoved()) {
            return null;
        }

        double totalAngle = Math.toRadians(ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.get());
        boolean threeDimensional = ZenithConfigs.ZENITH_CLIENT_CONFIG._3D_TRAIL.get();
        int amount = threeDimensional
            ? (int) (60 * ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.get() / 80) / 2
            : (int) (60 * ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.get() / 80);
        if (amount <= 0) {
            return null;
        }
        double onceAngle = totalAngle / amount;

        int swordType = entity.getSwordType();
        int progress = Math.min(entity.getLocalProgress(), ZenithProjectile.STAGE_COUNT + 1);
        if (progress > ZenithProjectile.STAGE_COUNT) {
            return null;
        }
        if (!(entity.level().getEntity(entity.getOwnerID()) instanceof LivingEntity owner)) {
            return null;
        }

        int[] color = ZenithProjectileRenderer.COLOR[swordType];
        double angle = entity.getAngle();
        double distance = entity.getDistance();
        double progressAngle = Mth.TWO_PI
            * Mth.lerp(partialTick, (double) progress - 1, progress) / ZenithProjectile.STAGE_COUNT;

        Vector3[] relativeCenter = entity.getReference();
        Vector3[] relativeWorld = Vector3.WORLD;
        Vector3 swordPos = new Vector3(0, 0, -1)
            .VecInNewRefer(relativeCenter, relativeWorld)
            .add(Vector3.of(owner.getEyePosition(partialTick)));
        boolean firstPerson = owner == Minecraft.getInstance().cameraEntity;

        if (firstPerson
            && Minecraft.getInstance().options.getCameraType().isFirstPerson()
            && ZenithConfigs.ZENITH_CLIENT_CONFIG.RENDER_OFFSET.get()) {
            Vector3 cameraOffset = new Vector3(0, -1, 0).VecInNewRefer(relativeCenter, relativeWorld);
            poseStack.translate(cameraOffset.x(), cameraOffset.y(), cameraOffset.z());
        }

        Vector3 center = new Vector3(0, 0, distance / 2 - 1)
            .VecInNewRefer(relativeCenter, relativeWorld)
            .add(Vector3.of(owner.getEyePosition(partialTick)));
        Vector3 offset = new Vector3(0, 0, -1).VecInNewRefer(relativeCenter, relativeWorld);
        Vector3 round = null;

        if (!threeDimensional) {
            Vector3 lastInner = null;
            Vector3 lastOuter = null;
            Vector3 nearInner;
            Vector3 nearOuter;
            Vector3 farInner;
            Vector3 farOuter;
            for (int i = 0; i < amount && i * onceAngle < progressAngle; i++) {
                int factorI = i + amount - Math.min(amount, (int) (progressAngle / onceAngle));
                double currentAngle = progressAngle - i * onceAngle;
                double nextAngle = Math.max(progressAngle - (i + 1) * onceAngle, 0);
                if (lastInner == null) {
                    Vector3 near = PosUtil.calCenPos(distance, currentAngle, angle)
                        .VecInNewRefer(relativeCenter, relativeWorld);
                    swordPos = near.add(center).add(offset);
                    nearInner = near.applyOffset(-0.5 + factorI * (0.4 / amount))
                        .add(firstPerson
                            ? relativeCenter[1].multiply(-0.1 + (0.08 / amount) * factorI)
                            : new Vector3(0, 0, 0))
                        .add(center)
                        .add(offset);
                    nearOuter = near.applyOffset(0.5 - factorI * (0.4 / amount))
                        .add(firstPerson
                            ? relativeCenter[1].multiply(0.1 - (0.08 / amount) * factorI)
                            : new Vector3(0, 0, 0))
                        .add(center)
                        .add(offset);
                    round = nearOuter.multiply(-1).add(nearInner);
                } else {
                    nearInner = lastInner;
                    nearOuter = lastOuter;
                }
                Vector3 far = PosUtil.calCenPos(distance, nextAngle, angle)
                    .VecInNewRefer(relativeCenter, relativeWorld);

                farInner = far.applyOffset(-0.5 + factorI * (0.4 / amount))
                    .add(firstPerson
                        ? relativeCenter[1].multiply(-0.1 + factorI * (0.08 / amount))
                        : new Vector3(0, 0, 0))
                    .add(center)
                    .add(offset);
                farOuter = far.applyOffset(0.5 - factorI * (0.4 / amount))
                    .add(firstPerson
                        ? relativeCenter[1].multiply(0.1 - factorI * (0.08 / amount))
                        : new Vector3(0, 0, 0))
                    .add(center)
                    .add(offset);
                lastInner = farInner;
                lastOuter = farOuter;

                if (vertex != null) {
                    int alpha = 255 - (factorI + 1) * (240 / amount);
                    vertex.vertex(poseStack.last().pose(), (float) farInner.x(), (float) farInner.y(), (float) farInner.z())
                        .color(color[0], color[1], color[2], alpha).uv(0, 0)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(LightTexture.FULL_BRIGHT).normal(poseStack.last().normal(), 0, 1, 0)
                        .endVertex();
                    vertex.vertex(poseStack.last().pose(), (float) farOuter.x(), (float) farOuter.y(), (float) farOuter.z())
                        .color(color[0], color[1], color[2], alpha).uv(0, 1)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(LightTexture.FULL_BRIGHT).normal(poseStack.last().normal(), 0, 1, 0)
                        .endVertex();
                    vertex.vertex(poseStack.last().pose(), (float) nearOuter.x(), (float) nearOuter.y(), (float) nearOuter.z())
                        .color(color[0], color[1], color[2], alpha).uv(1, 1)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(LightTexture.FULL_BRIGHT).normal(poseStack.last().normal(), 0, 1, 0)
                        .endVertex();
                    vertex.vertex(poseStack.last().pose(), (float) nearInner.x(), (float) nearInner.y(), (float) nearInner.z())
                        .color(color[0], color[1], color[2], alpha).uv(1, 0)
                        .overlayCoords(OverlayTexture.NO_OVERLAY)
                        .uv2(LightTexture.FULL_BRIGHT).normal(poseStack.last().normal(), 0, 1, 0)
                        .endVertex();
                }
            }
        } else {
            Vector3[] cp = new Vector3[4];
            Vector3[] np = new Vector3[4];
            Vector3[] save = null;
            for (int i = 0; i < amount && i * onceAngle < progressAngle; i++) {
                int factorI = i + amount - Math.min(amount, (int) (progressAngle / onceAngle));
                double currentAngle = progressAngle - i * onceAngle;
                double nextAngle = Math.max(progressAngle - (i + 1) * onceAngle, 0);
                if (save == null) {
                    Vector3 near = PosUtil.calCenPos(distance, currentAngle, angle)
                        .VecInNewRefer(relativeCenter, relativeWorld);
                    swordPos = near.add(center).add(offset);
                    cp[0] = near.applyOffset(-0.5 + factorI * (0.4 / amount))
                        .add(center).add(offset).add(normal.multiply(0.025));
                    cp[1] = cp[0].subtract(normal.multiply(0.03));
                    cp[2] = near.applyOffset(0.5 - factorI * (0.4 / amount))
                        .add(center).add(offset).subtract(normal.multiply(0.025));
                    cp[3] = cp[2].add(normal.multiply(0.03));
                    round = null;
                    save = new Vector3[4];
                } else {
                    System.arraycopy(save, 0, cp, 0, 4);
                }
                Vector3 far = PosUtil.calCenPos(distance, nextAngle, angle)
                    .VecInNewRefer(relativeCenter, relativeWorld);

                np[0] = far.applyOffset(-0.5 + factorI * (0.4 / amount))
                    .add(center).add(offset).add(normal.multiply(0.025));
                np[1] = np[0].subtract(normal.multiply(0.03));
                np[2] = far.applyOffset(0.5 - factorI * (0.4 / amount))
                    .add(center).add(offset).subtract(normal.multiply(0.025));
                np[3] = np[2].add(normal.multiply(0.03));

                System.arraycopy(np, 0, save, 0, 4);

                if (vertex != null) {
                    RenderUtil.createTrail(
                        vertex, np, cp, poseStack,
                        255 - (factorI + 1) * (240 / amount), color
                    );
                }
            }
        }
        return new TrailResult(swordPos, round, firstPerson, distance, progressAngle);
    }
}
