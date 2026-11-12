package com.oceanscenery.zenith.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.util.PosUtil;
import com.oceanscenery.zenith.util.Quaternion;
import com.oceanscenery.zenith.util.Vector3;
import com.oceanscenery.zenith.zenith_class.entity.ZenithProjectile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL30;

public final class ZenithProjectileRenderer extends EntityRenderer<ZenithProjectile> {
    private static final ResourceLocation WHITE_TEXTURE = ResourceLocation.fromNamespaceAndPath("forge", "textures/white.png");
    private static boolean useEntityTrails;
    private static boolean entityTrailsDisabled;
    private static boolean swordRenderingDisabled;
    private static final double[] BANDS = {-2.0, -1.15, -0.42, 0, 0.42, 1.15, 2.0};
    private static final float[] BAND_ALPHA = {0, 0.08F, 0.22F, 0.28F, 0.22F, 0.08F, 0};
    private static final float[] BAND_WHITE = {0, 0, 0.02F, 0.08F, 0.02F, 0, 0};
    private static final double[] CORE_BANDS = {-0.42, -0.34, -0.10, 0.10, 0.34, 0.42};
    private static final float[] CORE_ALPHA = {0, 0.94F, 1, 1, 0.94F, 0};
    private static final float[] CORE_WHITE = {0, 0.02F, 0.14F, 0.14F, 0.02F, 0};
    private static final double MAX_WIDTH_SCALE = 3.8;
    private static final int MAX_TRAIL_SEGMENTS = 128;
    private static final double HALO_CAMERA_HIDE_RADIUS = 0.5;
    private static final double HALO_CAMERA_FULL_RADIUS = 2.0;
    private static final double HALO_RETURN_NARROW_RADIUS = 3.0;
    private static final double HALO_RETURN_FULL_RADIUS = 8.0;
    private static final Quaternionf Q1 = Quaternion.trans(
        new Vector3(1, 1, 0), new Vector3(0, 1, 0)).toQuaternionf();
    private static final Quaternionf Q2 = Quaternion.trans(
        new Vector3(0, 1, 0), new Vector3(0, 0, -1)).toQuaternionf();
    private static final int[][] COLORS = {
        {204, 255, 255}, {153, 255, 204}, {32, 32, 32}, {0, 204, 102},
        {178, 0, 0}, {255, 153, 51}, {153, 153, 255}, {255, 255, 102},
        {153, 204, 255}, {127, 0, 255}, {255, 178, 255}, {51, 51, 255},
        {153, 51, 255}, {0, 153, 0}, {255, 0, 127}, {255, 51, 153},
        {0, 255, 0}, {255, 128, 0}, {255, 255, 0}, {204, 153, 255},
        {255, 64, 0}
    };

    private final ItemRenderer itemRenderer;
    private final PoseStack swordPoses = new PoseStack();
    private final ItemStack[] renderItems = new ItemStack[COLORS.length];
    private final Orbit orbit = new Orbit();
    private final GlowPoint[] trailPoints = createTrailPoints();
    private final Vector3f cameraRight = new Vector3f();
    private final Vector3f cameraUp = new Vector3f();

    public ZenithProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public boolean shouldRender(@NotNull ZenithProjectile entity, @NotNull Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    protected boolean shouldShowName(@NotNull ZenithProjectile entity) {
        return false;
    }

    @Override
    public void render(ZenithProjectile entity, float entityYaw, float partialTick
        , @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isAlive() || entity.isRemoved()) {
            return;
        }
        int swordType = entity.getSwordType();
        if (swordType < 0 || swordType >= COLORS.length) {
            return;
        }
        int progress = Math.min(entity.getLocalProgress(), ZenithProjectile.STAGE_COUNT + 1);
        if (progress > ZenithProjectile.STAGE_COUNT
            || !(entity.level().getEntity(entity.getOwnerID()) instanceof LivingEntity owner)) {
            return;
        }

        var config = ZenithConfigs.ZENITH_CLIENT_CONFIG;
        double trailDegrees = config.TRAIL_ANGLE.get();
        int amount = (int) (60 * trailDegrees / 80) / 2;
        if (amount <= 0) {
            return;
        }
        double onceAngle = Math.toRadians(trailDegrees) / amount;
        double progressAngle = Mth.TWO_PI
            * Mth.lerp(partialTick, (double) progress - 1, progress) / ZenithProjectile.STAGE_COUNT;
        double distance = entity.getDistance();
        Vector3[] reference = entity.getReference();
        Quaternion[] fixed = entity.getFixedPose();
        Quaternion combined = fixed[2].multiply(fixed[1]).multiply(fixed[0]);
        Vector3 normal = new Vector3(0, 1, 0).rot(combined).normalize();
        Minecraft minecraft = Minecraft.getInstance();
        boolean cameraOwner = owner == minecraft.cameraEntity;
        orbit.set(reference, normal, distance, entity.getAngle(), cameraOwner);

        double swordX = -reference[2].x();
        double swordY = -reference[2].y();
        double swordZ = -reference[2].z();
        if (progressAngle > 0) {
            orbit.sample(progressAngle);
            swordX = orbit.x + orbit.cx;
            swordY = orbit.y + orbit.cy;
            swordZ = orbit.z + orbit.cz;
        }

        Vec3 pos = entity.getPosition(partialTick);
        Vec3 eye = owner.getEyePosition(partialTick);
        poseStack.pushPose();
        try {
            poseStack.translate(eye.x - pos.x, eye.y - pos.y, eye.z - pos.z);
            if (cameraOwner && minecraft.options.getCameraType().isFirstPerson()
                && config.RENDER_OFFSET.get()) {
                poseStack.translate(-reference[1].x(), -reference[1].y(), -reference[1].z());
            }

            if (!swordRenderingDisabled) {
                try {
                    renderSword(entity, swordType, progressAngle, distance, fixed, normal,
                        swordX, swordY, swordZ, poseStack.last(), bufferSource);
                } catch (RuntimeException | LinkageError failure) {
                    swordRenderingDisabled = true;
                    switchToEntityTrails("Zenith projectile sword rendering failed. Sword models are disabled for this client session.", failure);
                }
            }
            if (progressAngle > 0 && !entityTrailsDisabled) {
                renderTrail(entity, partialTick, progressAngle, onceAngle * amount,
                    COLORS[swordType], eye, minecraft, poseStack.last(), bufferSource);
            }
        } finally {
            poseStack.popPose();
        }
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ZenithProjectile projectile) {
        return TheZenithMod.loc("textures/entity/trail.png");
    }

    private void renderSword(ZenithProjectile entity, int swordType, double progressAngle, double distance
        , Quaternion[] fixed, Vector3 normal
        , double x, double y, double z, PoseStack.Pose pose, MultiBufferSource buffers) {
        // ItemRenderer may throw with its own pose still on the stack.
        swordPoses.last().pose().set(pose.pose());
        swordPoses.last().normal().set(pose.normal());
        swordPoses.translate(x, y, z);
        Quaternion rotation = Quaternion.rotate(normal, -PosUtil.getPolar(progressAngle, distance));
        swordPoses.mulPose(rotation.toQuaternionf());
        swordPoses.mulPose(fixed[2].toQuaternionf());
        swordPoses.mulPose(fixed[1].toQuaternionf());
        swordPoses.mulPose(fixed[0].toQuaternionf());
        swordPoses.mulPose(Q2);
        swordPoses.mulPose(Q1);

        ItemStack stack = renderItems[swordType];
        if (stack == null) {
            stack = ZenithItems.ZENITH.get().getDefaultInstance();
            stack.getOrCreateTag().putInt("CustomModelData", swordType);
            renderItems[swordType] = stack;
        }
        BakedModel model = itemRenderer.getModel(stack, entity.level(), null, entity.getId());
        itemRenderer.render(stack, ItemDisplayContext.NONE, false, swordPoses, buffers,
            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
    }

    private void renderTrail(ZenithProjectile entity, float partialTick, double progressAngle, double totalAngle
        , int[] color, Vec3 eye, Minecraft minecraft, PoseStack.Pose pose, MultiBufferSource buffers) {
        if (!useEntityTrails) {
            try {
                submitTrail(entity, partialTick, progressAngle, totalAngle, color, eye, minecraft, pose, buffers, true);
                return;
            } catch (RuntimeException | LinkageError failure) {
                switchToEntityTrails("Zenith staged trail submission failed.", failure);
            }
        }
        try {
            submitTrail(entity, partialTick, progressAngle, totalAngle, color, eye, minecraft, pose, buffers, false);
        } catch (RuntimeException | LinkageError failure) {
            entityTrailsDisabled = true;
            TheZenithMod.LOGGER.error("Disabled Zenith projectile trails after entity rendering failed", failure);
        }
    }

    private void submitTrail(ZenithProjectile entity, float partialTick, double progressAngle, double totalAngle
        , int[] color, Vec3 eye, Minecraft minecraft, PoseStack.Pose pose, MultiBufferSource buffers, boolean staged) {
        double span = Math.min(progressAngle, totalAngle);
        int segments = Math.min(MAX_TRAIL_SEGMENTS,
            Math.max(12, (int) Math.ceil(span * Math.sqrt(Math.max(1, orbit.a)) * 4)));
        double widthScale = distanceWidthScale(entity.getDistance());
        double time = entity.tickCount + partialTick;
        double phase = (entity.getId() * 0.61803398875) % 1;
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        double viewX = eye.x - camera.x, viewY = eye.y - camera.y, viewZ = eye.z - camera.z;
        if (orbit.cameraOwner && minecraft.options.getCameraType().isFirstPerson()
            && ZenithConfigs.ZENITH_CLIENT_CONFIG.RENDER_OFFSET.get()) {
            viewX -= orbit.yx;
            viewY -= orbit.yy;
            viewZ -= orbit.yz;
        }
        double fov = Math.toRadians(minecraft.options.fov().get());
        double pixelScale = 3 * Math.tan(fov / 2) / Math.max(1, minecraft.getWindow().getHeight());
        cameraRight.set(1, 0, 0).rotate(minecraft.gameRenderer.getMainCamera().rotation());
        cameraUp.set(0, 1, 0).rotate(minecraft.gameRenderer.getMainCamera().rotation());
        double growth = smoothStep(Math.min(1, progressAngle / 0.20));
        for (int i = 0; i <= segments; i++) {
            double age = (double) i / segments;
            sampleGlow(trailPoints[i], Math.max(0, progressAngle - span * age), age,
                widthScale, growth, time, phase, color, viewX, viewY, viewZ, pixelScale);
        }

        TrailVertex core;
        TrailVertex halo;
        if (staged) {
            DeferredTrails.beginSubmission();
            core = stagedVertices(DeferredTrails.CORE_BUFFER, pose.pose());
            halo = stagedVertices(DeferredTrails.HALO_BUFFER, pose.pose());
        } else {
            core = entityVertices(buffers, pose);
            halo = core;
        }
        for (int i = 1; i <= segments; i++) {
            ribbon(core, trailPoints[i - 1], trailPoints[i], false, 1, true);
            ribbon(core, trailPoints[i - 1], trailPoints[i], true, 0.60F, true);
        }

        int glintStride = Math.max(1, (segments + 7) / 8);
        for (int i = 1; i <= segments; i++) {
            GlowPoint current = trailPoints[i - 1], next = trailPoints[i];
            double age = (double) i / segments;
            ribbon(halo, current, next, false, 1, false);
            ribbon(halo, current, next, true, 0.45F, false);
            if (i % glintStride == 0 && age < 0.9) {
                double pulse = Math.pow(Math.max(0, Math.sin(time * 0.24 - age * 19 + phase * 6.28)), 6);
                glint(halo, next, next.haloWidth * (0.18 + pulse * 0.40), (float) pulse);
            }
        }
    }

    private static GlowPoint[] createTrailPoints() {
        GlowPoint[] points = new GlowPoint[MAX_TRAIL_SEGMENTS + 1];
        for (int i = 0; i < points.length; i++) {
            points[i] = new GlowPoint();
        }
        return points;
    }

    private static double distanceWidthScale(double distance) {
        return Math.min(MAX_WIDTH_SCALE, 1 + 0.18 * Math.sqrt(Math.max(0, distance - 12)));
    }

    private static double smoothStep(double value) {
        return value * value * (3 - 2 * value);
    }

    private static double cameraFade(double distance) {
        double t = Math.max(0, Math.min(1, (distance - HALO_CAMERA_HIDE_RADIUS)
            / (HALO_CAMERA_FULL_RADIUS - HALO_CAMERA_HIDE_RADIUS)));
        return smoothStep(t);
    }

    private static double haloWidth(double width, double taper, double angle, double ownerDistance) {
        double returning = smoothStep(Math.max(0, Math.min(1, (angle - Mth.PI) / 0.45)));
        double nearby = 1 - smoothStep(Math.max(0, Math.min(1,
            (ownerDistance - HALO_RETURN_NARROW_RADIUS)
                / (HALO_RETURN_FULL_RADIUS - HALO_RETURN_NARROW_RADIUS))));
        double narrowWidth = Math.min(width, Math.max(0.5 * taper, width * 0.25));
        return width + (narrowWidth - width) * returning * nearby;
    }

    private static double segmentCameraDistance(double ax, double ay, double az,
                                                double bx, double by, double bz) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double lengthSquared = dx * dx + dy * dy + dz * dz;
        double t = lengthSquared > 1.0E-20
            ? Math.max(0, Math.min(1, -(ax * dx + ay * dy + az * dz) / lengthSquared)) : 0;
        double x = ax + t * dx, y = ay + t * dy, z = az + t * dz;
        return Math.sqrt(x * x + y * y + z * z);
    }

    private static double haloBandFade(GlowPoint near, GlowPoint far, boolean cross, int band) {
        double axisScale = cross ? 0.65 : 1;
        double nearWidth = near.haloWidth * axisScale, farWidth = far.haloWidth * axisScale;
        double middle = (BANDS[band] + BANDS[band + 1]) * 0.5;
        double radius = (BANDS[band + 1] - BANDS[band]) * 0.5 * Math.max(nearWidth, farWidth);
        double ax = near.cameraX + (cross ? near.vx : near.sx) * middle * nearWidth;
        double ay = near.cameraY + (cross ? near.vy : near.sy) * middle * nearWidth;
        double az = near.cameraZ + (cross ? near.vz : near.sz) * middle * nearWidth;
        double bx = far.cameraX + (cross ? far.vx : far.sx) * middle * farWidth;
        double by = far.cameraY + (cross ? far.vy : far.sy) * middle * farWidth;
        double bz = far.cameraZ + (cross ? far.vz : far.sz) * middle * farWidth;
        return cameraFade(Math.max(0, segmentCameraDistance(ax, ay, az, bx, by, bz) - radius));
    }

    private void sampleGlow(GlowPoint out, double angle, double age, double widthScale,
                            double growth, double time, double phase, int[] color,
                            double viewX, double viewY, double viewZ, double pixelScale) {
        orbit.sample(angle);
        out.x = orbit.x + orbit.cx;
        out.y = orbit.y + orbit.cy;
        out.z = orbit.z + orbit.cz;
        orbit.ribbonAxes(out, angle);
        out.cameraX = out.x + viewX;
        out.cameraY = out.y + viewY;
        out.cameraZ = out.z + viewZ;
        double cameraDistance = Math.sqrt(out.cameraX * out.cameraX
            + out.cameraY * out.cameraY + out.cameraZ * out.cameraZ);
        double pixelFloor = Math.min(1.2, cameraDistance * pixelScale);
        double taper = Math.pow(Math.max(0, 1 - age), 0.65);
        out.width = Math.min(4, Math.max(0.5 * widthScale, pixelFloor)) * taper;
        out.haloWidth = haloWidth(out.width, taper, angle,
            Math.sqrt(out.x * out.x + out.y * out.y + out.z * out.z));
        double pulse = Math.pow(0.5 + 0.5 * Math.sin(age * 18 - time * 0.35 + phase * 6.28), 8);
        double hue = phase + age * 0.32 - time * 0.004;
        int rainbow = Mth.hsvToRgb((float) (hue - Math.floor(hue)), 0.68F, 1);
        float brightness = 1 + (float) pulse * 0.06F;
        out.r = Math.min(1, (color[0] / 255F * 0.88F + ((rainbow >> 16) & 255) / 255F * 0.12F) * brightness);
        out.g = Math.min(1, (color[1] / 255F * 0.88F + ((rainbow >> 8) & 255) / 255F * 0.12F) * brightness);
        out.b = Math.min(1, (color[2] / 255F * 0.88F + (rainbow & 255) / 255F * 0.12F) * brightness);
        out.alpha = (float) (0.78 * growth * Math.pow(Math.max(0, 1 - age), 1.15)
            * (0.75 + 0.25 * pulse));
        out.coreAlpha = (float) (0.94 * growth * Math.pow(Math.max(0, 1 - age), 0.55)
            * (0.96 + 0.04 * pulse));
    }

    private static void ribbon(TrailVertex vertex, GlowPoint near,
                               GlowPoint far, boolean cross, float opacity, boolean core) {
        int bandCount = core ? CORE_BANDS.length : BANDS.length;
        for (int band = 0; band < bandCount - 1; band++) {
            float bandOpacity = core ? opacity : opacity * (float) haloBandFade(near, far, cross, band);
            bandVertex(vertex, far, band, cross, bandOpacity, core);
            bandVertex(vertex, far, band + 1, cross, bandOpacity, core);
            bandVertex(vertex, near, band + 1, cross, bandOpacity, core);
            bandVertex(vertex, near, band, cross, bandOpacity, core);
        }
    }

    private static void bandVertex(TrailVertex vertex, GlowPoint point,
                                   int band, boolean cross, float opacity, boolean core) {
        double extent = (core ? CORE_BANDS[band] * point.width : BANDS[band] * point.haloWidth)
            * (cross ? 0.65 : 1);
        double ax = cross ? point.vx : point.sx;
        double ay = cross ? point.vy : point.sy;
        double az = cross ? point.vz : point.sz;
        float white = core ? CORE_WHITE[band] : BAND_WHITE[band];
        float alpha = core ? point.coreAlpha * CORE_ALPHA[band] : point.alpha * BAND_ALPHA[band];
        if (!core) {
            double x = point.cameraX + ax * extent;
            double y = point.cameraY + ay * extent;
            double z = point.cameraZ + az * extent;
            alpha *= (float) cameraFade(Math.sqrt(x * x + y * y + z * z));
        }
        vertex.add(point.x + ax * extent, point.y + ay * extent, point.z + az * extent,
            point.r + (1 - point.r) * white, point.g + (1 - point.g) * white,
            point.b + (1 - point.b) * white, alpha * opacity);
    }

    private void glint(TrailVertex vertex, GlowPoint point,
                       double size, float pulse) {
        double cameraDistance = Math.sqrt(point.cameraX * point.cameraX
            + point.cameraY * point.cameraY + point.cameraZ * point.cameraZ);
        pulse *= (float) cameraFade(Math.max(0, cameraDistance - size));
        if (pulse < 0.05F || point.alpha < 0.01F) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            double ax = i == 0 ? cameraRight.x : i == 1 ? cameraUp.x : i == 2 ? -cameraRight.x : -cameraUp.x;
            double ay = i == 0 ? cameraRight.y : i == 1 ? cameraUp.y : i == 2 ? -cameraRight.y : -cameraUp.y;
            double az = i == 0 ? cameraRight.z : i == 1 ? cameraUp.z : i == 2 ? -cameraRight.z : -cameraUp.z;
            int j = (i + 1) % 4;
            double bx = j == 0 ? cameraRight.x : j == 1 ? cameraUp.x : j == 2 ? -cameraRight.x : -cameraUp.x;
            double by = j == 0 ? cameraRight.y : j == 1 ? cameraUp.y : j == 2 ? -cameraRight.y : -cameraUp.y;
            double bz = j == 0 ? cameraRight.z : j == 1 ? cameraUp.z : j == 2 ? -cameraRight.z : -cameraUp.z;
            vertex.add(point.x, point.y, point.z, 1, 1, 1, point.alpha * pulse);
            vertex.add(point.x + ax * size, point.y + ay * size, point.z + az * size,
                point.r, point.g, point.b, 0);
            vertex.add(point.x + bx * size, point.y + by * size, point.z + bz * size,
                point.r, point.g, point.b, 0);
            vertex.add(point.x + bx * size, point.y + by * size, point.z + bz * size,
                point.r, point.g, point.b, 0);
        }
    }

    private static TrailVertex stagedVertices(VertexConsumer consumer, Matrix4f pose) {
        return (x, y, z, r, g, b, alpha) -> consumer.vertex(pose, (float) x, (float) y, (float) z)
            .color(r, g, b, alpha).endVertex();
    }

    private static TrailVertex entityVertices(MultiBufferSource buffers, PoseStack.Pose pose) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE_TEXTURE, false));
        return (x, y, z, r, g, b, alpha) -> consumer.vertex(pose.pose(), (float) x, (float) y, (float) z)
            .color(r, g, b, alpha).uv(0.5F, 0.5F).overlayCoords(OverlayTexture.NO_OVERLAY)
            .uv2(LightTexture.FULL_BRIGHT).normal(pose.normal(), 0, 1, 0).endVertex();
    }

    @FunctionalInterface
    private interface TrailVertex {
        void add(double x, double y, double z, float r, float g, float b, float alpha);
    }

    private static final class GlowPoint {
        private double x, y, z, sx, sy, sz, vx, vy, vz, width, haloWidth;
        private double cameraX, cameraY, cameraZ;
        private float r, g, b, alpha, coreAlpha;
    }

    static void drawTrailsAfterLevel() {
        if (!useEntityTrails) {
            try {
                DeferredTrails.draw();
            } catch (RuntimeException | LinkageError failure) {
                switchToEntityTrails("Zenith staged trail draw failed.", failure);
            }
        }
    }

    static void discardPendingTrails() {
        if (!useEntityTrails) {
            try {
                DeferredTrails.discard();
            } catch (RuntimeException | LinkageError failure) {
                switchToEntityTrails("Zenith staged trail cleanup failed.", failure);
            }
        }
    }

    private static void switchToEntityTrails(String message, Throwable failure) {
        if (useEntityTrails) {
            TheZenithMod.LOGGER.error(message, failure);
            return;
        }
        useEntityTrails = true;
        try {
            DeferredTrails.discard();
        } catch (RuntimeException | LinkageError cleanupFailure) {
            if (cleanupFailure != failure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
        TheZenithMod.LOGGER.error(
            "{} Using normal entity rendering for Zenith trails for this client session.", message, failure);
    }

    private static final class DeferredTrails {
        private static final RenderType CORE_TYPE = GlowType.createCore();
        private static final RenderType TRAIL_TYPE = GlowType.createTrail();
        private static final BufferBuilder CORE_BUFFER = new BufferBuilder(32768);
        private static final BufferBuilder HALO_BUFFER = new BufferBuilder(32768);
        private static final Matrix4f modelViewAtSubmission = new Matrix4f();
        private static boolean pending;

        private static void beginSubmission() {
            if (!pending) {
                modelViewAtSubmission.set(RenderSystem.getModelViewStack().last().pose());
                CORE_BUFFER.begin(CORE_TYPE.mode(), CORE_TYPE.format());
                HALO_BUFFER.begin(TRAIL_TYPE.mode(), TRAIL_TYPE.format());
                pending = true;
            }
        }

        private static void draw() {
            if (!pending) {
                return;
            }
            PoseStack modelView = RenderSystem.getModelViewStack();
            int framebuffer = GlStateManager.getBoundFramebuffer();
            int mainFramebuffer = Minecraft.getInstance().getMainRenderTarget().frameBufferId;
            modelView.pushPose();
            try {
                modelView.setIdentity();
                modelView.mulPoseMatrix(modelViewAtSubmission);
                RenderSystem.applyModelViewMatrix();
                if (framebuffer != mainFramebuffer) {
                    GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, mainFramebuffer);
                }
                CORE_BUFFER.setQuadSorting(RenderSystem.getVertexSorting());
                drawBatch(CORE_TYPE, CORE_BUFFER);
                drawBatch(TRAIL_TYPE, HALO_BUFFER);
                pending = false;
            } finally {
                modelView.popPose();
                try {
                    RenderSystem.applyModelViewMatrix();
                } finally {
                    if (framebuffer != mainFramebuffer) {
                        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
                    }
                }
            }
        }

        private static void drawBatch(RenderType type, BufferBuilder buffer) {
            try {
                type.setupRenderState();
                BufferUploader.drawWithShader(buffer.end());
            } finally {
                type.clearRenderState();
            }
        }

        private static void discard() {
            pending = false;
            try {
                discard(CORE_BUFFER);
            } finally {
                discard(HALO_BUFFER);
            }
        }

        private static void discard(BufferBuilder buffer) {
            try {
                if (buffer.building()) {
                    BufferBuilder.RenderedBuffer batch = buffer.endOrDiscardIfEmpty();
                    if (batch != null) {
                        batch.release();
                    }
                }
            } finally {
                buffer.discard();
            }
        }
    }

    private static final class GlowType extends RenderType {
        private GlowType() {
            super("zenith_glow_states", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS,
                256, false, false, () -> {
                }, () -> {
                });
        }

        private static RenderType createCore() {
            return create(TheZenithMod.MOD_ID + ":prismatic_trail_core", DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS, 32768, false, true,
                CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(TRANSLUCENT_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));
        }

        private static RenderType createTrail() {
            return create(TheZenithMod.MOD_ID + ":prismatic_trail", DefaultVertexFormat.POSITION_COLOR,
                VertexFormat.Mode.QUADS, 32768, false, false,
                CompositeState.builder()
                    .setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY)
                    .setCullState(NO_CULL)
                    .setDepthTestState(LEQUAL_DEPTH_TEST)
                    .setWriteMaskState(COLOR_WRITE)
                    .createCompositeState(false));
        }
    }

    private static final class Orbit {
        private double a, b, ux, uy, uz, zx, zy, zz, yx, yy, yz, nx, ny, nz;
        private double cx, cy, cz, x, y, z;
        private boolean cameraOwner;

        private void set(Vector3[] ref, Vector3 normal, double distance, double angle, boolean cameraOwner) {
            a = distance / 2;
            b = Math.min(distance / 4, 10);
            double cos = Math.cos(angle), sin = Math.sin(angle);
            ux = ref[0].x() * cos + ref[1].x() * sin;
            uy = ref[0].y() * cos + ref[1].y() * sin;
            uz = ref[0].z() * cos + ref[1].z() * sin;
            zx = ref[2].x();
            zy = ref[2].y();
            zz = ref[2].z();
            yx = ref[1].x();
            yy = ref[1].y();
            yz = ref[1].z();
            nx = normal.x();
            ny = normal.y();
            nz = normal.z();
            cx = (a - 2) * zx;
            cy = (a - 2) * zy;
            cz = (a - 2) * zz;
            this.cameraOwner = cameraOwner;
        }

        private void sample(double progressAngle) {
            double theta = Mth.PI - progressAngle;
            double lateral = b * Math.sin(theta), forward = a * Math.cos(theta);
            x = lateral * ux + forward * zx;
            y = lateral * uy + forward * zy;
            z = lateral * uz + forward * zz;
        }

        private void ribbonAxes(GlowPoint out, double progressAngle) {
            double theta = Mth.PI - progressAngle;
            double lateral = -b * Math.cos(theta), forward = a * Math.sin(theta);
            double tx = lateral * ux + forward * zx;
            double ty = lateral * uy + forward * zy;
            double tz = lateral * uz + forward * zz;
            double tangentLength = Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (tangentLength < 1.0E-12) {
                tx = zx;
                ty = zy;
                tz = zz;
                tangentLength = Math.max(1.0E-12, Math.sqrt(tx * tx + ty * ty + tz * tz));
            }
            tx /= tangentLength;
            ty /= tangentLength;
            tz /= tangentLength;
            double sx = ny * tz - nz * ty;
            double sy = nz * tx - nx * tz;
            double sz = nx * ty - ny * tx;
            double inverse = 1 / Math.max(1.0E-12, Math.sqrt(sx * sx + sy * sy + sz * sz));
            out.sx = sx * inverse;
            out.sy = sy * inverse;
            out.sz = sz * inverse;
            out.vx = ty * out.sz - tz * out.sy;
            out.vy = tz * out.sx - tx * out.sz;
            out.vz = tx * out.sy - ty * out.sx;
        }
    }
}
