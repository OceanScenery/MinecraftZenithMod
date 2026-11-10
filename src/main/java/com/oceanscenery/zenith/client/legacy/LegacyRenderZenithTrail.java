package com.oceanscenery.zenith.client.legacy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oceanscenery.zenith.mod_class.entity.ZenithProjectile;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.util.PosUtil;
import com.oceanscenery.zenith.util.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

/** Original trail calculation and vertex emission; no event hooks or active callers. */
public final class LegacyRenderZenithTrail {
    private LegacyRenderZenithTrail() {
    }

    public record TrailResult(Vector3 swordPos, double distance, double progressAngle) {
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
        int amount = (int) (60 * ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.get() / 80) / 2;
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

        int[] color = LegacyZenithProjectileRenderer.COLOR[swordType];
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
                LegacyTrailRenderUtil.createTrail(
                    vertex, np, cp, poseStack,
                    255 - (factorI + 1) * (240 / amount), color
                );
            }
        }
        return new TrailResult(swordPos, distance, progressAngle);
    }
}
