package com.oceanscenery.zenith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.entity.ZenithProjectile;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.util.PosUtil;
import com.oceanscenery.zenith.util.Quaternion;
import com.oceanscenery.zenith.util.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public final class ZenithProjectileRenderer extends EntityRenderer<ZenithProjectile, ZenithProjectileRenderState> {
    private static final Identifier WHITE_TEXTURE = Identifier.fromNamespaceAndPath("neoforge", "textures/white.png");
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

    public static final String[] SWORD_MODEL = new String[]{
        "zenith",
        "arkhalis",
        "bee_keeper",
        "blade_of_grass",
        "blood_butcherer",
        "copper_shortsword",
        "enchanted_sword",
        "excalibur",
        "influx_waver",
        "lights_bane",
        "meowmere",
        "muramasa",
        "nights_edge",
        "seedler",
        "star_wrath",
        "starfury",
        "terra_blade",
        "the_horsemans_blade",
        "true_excalibur",
        "true_nights_edge",
        "volcano"
    };

    private final ItemModelResolver resolver;
    private final PoseStack swordPoses = new PoseStack();
    private final ItemStack[] renderItems = new ItemStack[COLORS.length];
    private final Orbit orbit = new Orbit();
    private final GlowPoint[] trailPoints = createTrailPoints(MAX_TRAIL_SEGMENTS + 1);
    private final Vector3f cameraRight = new Vector3f();
    private final Vector3f cameraUp = new Vector3f();

    public ZenithProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.resolver = context.getItemModelResolver();
    }

    @Override
    public boolean shouldRender(ZenithProjectile entity, Frustum frustum, double x, double y, double z) {
        return true;
    }

    @Override
    public ZenithProjectileRenderState createRenderState() {
        return new ZenithProjectileRenderState();
    }

    @Override
    public void extractRenderState(ZenithProjectile entity, ZenithProjectileRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.ready = false;
        int type = entity.getSwordType();
        int progress = Math.min(entity.getLocalProgress(), ZenithProjectile.STAGE_COUNT + 1);
        if (!entity.isAlive() || entity.isRemoved() || type < 0 || type >= COLORS.length
            || progress > ZenithProjectile.STAGE_COUNT
            || !(entity.level().getEntity(entity.getOwnerID()) instanceof LivingEntity owner)) {
            return;
        }

        var config = ZenithConfigs.ZENITH_CLIENT_CONFIG;
        double trailDegrees = config.TRAIL_ANGLE.getAsDouble();
        int amount = (int) (60 * trailDegrees / 80) / 2;
        if (amount <= 0) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Vector3[] reference = entity.getReference();
        Quaternion[] fixed = entity.getFixedPose();
        for (int i = 0; i < 3; i++) {
            state.reference[i] = new Vector3(reference[i]);
            state.fixed[i].set(fixed[i].toQuaternionf());
        }
        state.normal = new Vector3(0, 1, 0).rot(fixed[2].multiply(fixed[1]).multiply(fixed[0])).normalize();
        state.eye = owner.getEyePosition(partialTick);
        state.lowerTrail = owner == minecraft.getCameraEntity()
            && minecraft.options.getCameraType().isFirstPerson() && config.RENDER_OFFSET.get();
        state.swordType = type;
        state.distance = entity.getDistance();
        state.angle = entity.getAngle();
        state.progressAngle = Mth.TWO_PI * Mth.lerp(partialTick, (double) progress - 1, progress) / ZenithProjectile.STAGE_COUNT;
        state.trailAngle = Math.toRadians(trailDegrees) / amount * amount;
        state.time = entity.tickCount + partialTick;
        state.phase = (entity.getId() * 0.61803398875) % 1;
        double fov = Math.toRadians(minecraft.options.fov().get());
        state.pixelScale = 3 * Math.tan(fov / 2) / Math.max(1, minecraft.getWindow().getHeight());

        if (!swordRenderingDisabled) {
            try {
                ItemStack stack = renderItems[type];
                if (stack == null) {
                    CustomModelData model = new CustomModelData(List.of((float) type), List.of(), List.of(), List.of());
                    stack = new ItemStack(ZenithItems.ZENITH, 1,
                        DataComponentPatch.builder().set(DataComponents.CUSTOM_MODEL_DATA, model).build());
                    renderItems[type] = stack;
                }
                resolver.updateForNonLiving(state.itemState, stack, ItemDisplayContext.NONE, entity);
            } catch (RuntimeException | LinkageError failure) {
                disableSwordRendering(failure);
            }
        }
        state.ready = true;
    }

    @Override
    public void submit(ZenithProjectileRenderState state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.ready) {
            return;
        }

        Vector3[] reference = state.reference;
        Vector3 shift = reference[1];
        orbit.set(reference, state.normal, state.distance, state.angle);

        double swordX = -reference[2].x(), swordY = -reference[2].y(), swordZ = -reference[2].z();
        if (state.progressAngle > 0) {
            orbit.sample(state.progressAngle);
            swordX = orbit.x + orbit.cx;
            swordY = orbit.y + orbit.cy;
            swordZ = orbit.z + orbit.cz;
        }

        poses.pushPose();
        try {
            poses.translate(state.eye.x - state.x, state.eye.y - state.y, state.eye.z - state.z);
            if (state.lowerTrail) {
                poses.translate(-shift.x(), -shift.y(), -shift.z());
            }

            if (!swordRenderingDisabled) {
                try {
                    submitSword(state, poses.last(), collector, swordX, swordY, swordZ);
                } catch (RuntimeException | LinkageError failure) {
                    disableSwordRendering(failure);
                }
            }

            if (state.progressAngle > 0 && !entityTrailsDisabled) {
                renderTrail(state, poses, collector, camera);
            }
        } finally {
            poses.popPose();
        }
    }

    private void submitSword(ZenithProjectileRenderState state, PoseStack.Pose pose
        , SubmitNodeCollector collector, double x, double y, double z) {
        swordPoses.last().set(pose);
        swordPoses.translate(x, y, z);
        swordPoses.mulPose(Quaternion.rotate(state.normal, -PosUtil.getPolar(state.progressAngle, state.distance)).toQuaternionf());
        swordPoses.mulPose(state.fixed[2]);
        swordPoses.mulPose(state.fixed[1]);
        swordPoses.mulPose(state.fixed[0]);
        swordPoses.mulPose(Q2);
        swordPoses.mulPose(Q1);
        state.itemState.submit(swordPoses, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
    }

    private static void disableSwordRendering(Throwable failure) {
        swordRenderingDisabled = true;
        RenderZenithTrail.switchToEntityRendering(
            "Zenith projectile sword rendering failed. Sword models are disabled for this client session.", failure);
    }

    private void renderTrail(ZenithProjectileRenderState state, PoseStack poses, SubmitNodeCollector collector,
                             CameraRenderState camera) {
        if (RenderZenithTrail.isCollecting()) {
            try {
                submitTrail(state, poses, collector, camera, true);
                return;
            } catch (RuntimeException | LinkageError failure) {
                RenderZenithTrail.switchToEntityRendering("Zenith staged trail submission failed.", failure);
            }
        }
        if (RenderZenithTrail.usesEntityRendering()) {
            try {
                submitTrail(state, poses, collector, camera, false);
            } catch (RuntimeException | LinkageError failure) {
                disableEntityTrails(failure);
            }
        }
    }

    private void submitTrail(ZenithProjectileRenderState state, PoseStack poses, SubmitNodeCollector collector
        , CameraRenderState camera, boolean staged) {
        double progressAngle = state.progressAngle;
        double totalAngle = state.trailAngle;
        int[] color = COLORS[state.swordType];
        double span = Math.min(progressAngle, totalAngle);
        int segments = Math.min(MAX_TRAIL_SEGMENTS,
            Math.max(12, (int) Math.ceil(span * Math.sqrt(Math.max(1, orbit.a)) * 4)));
        double widthScale = distanceWidthScale(state.distance);
        double time = state.time;
        double phase = state.phase;
        Vec3 pos = camera.pos;
        double viewX = state.eye.x - pos.x, viewY = state.eye.y - pos.y, viewZ = state.eye.z - pos.z;
        if (state.lowerTrail) {
            viewX -= state.reference[1].x();
            viewY -= state.reference[1].y();
            viewZ -= state.reference[1].z();
        }
        double pixelScale = state.pixelScale;
        Vector3f right = staged ? cameraRight : new Vector3f();
        Vector3f up = staged ? cameraUp : new Vector3f();
        right.set(1, 0, 0).rotate(camera.orientation);
        up.set(0, 1, 0).rotate(camera.orientation);
        GlowPoint[] points = staged ? trailPoints : createTrailPoints(segments + 1);
        double growth = smoothStep(Math.min(1, progressAngle / 0.20));
        for (int i = 0; i <= segments; i++) {
            double age = (double) i / segments;
            sampleGlow(points[i], Math.max(0, progressAngle - span * age), age,
                widthScale, growth, time, phase, color, viewX, viewY, viewZ, pixelScale);
        }

        if (staged) {
            PoseStack.Pose pose = poses.last();
            TrailVertex core = stagedVertices(RenderZenithTrail.coreVertices(), pose);
            TrailVertex halo = stagedVertices(RenderZenithTrail.haloVertices(), pose);
            emitTrail(points, segments, right, up, time, phase, core, halo);
        } else {
            collector.submitCustomGeometry(poses, RenderTypes.entityTranslucentEmissive(WHITE_TEXTURE, false),
                (pose, consumer) -> renderEntityTrail(points, segments, right, up, time, phase, pose, consumer));
        }
    }

    private static void renderEntityTrail(GlowPoint[] points, int segments, Vector3f right, Vector3f up,
                                          double time, double phase, PoseStack.Pose pose, VertexConsumer consumer) {
        if (entityTrailsDisabled) {
            return;
        }
        try {
            TrailVertex vertices = entityVertices(consumer, pose);
            emitTrail(points, segments, right, up, time, phase, vertices, vertices);
        } catch (RuntimeException | LinkageError failure) {
            disableEntityTrails(failure);
        }
    }

    private static void disableEntityTrails(Throwable failure) {
        entityTrailsDisabled = true;
        TheZenithMod.LOGGER.error("Disabled Zenith projectile trails after entity rendering failed", failure);
    }

    private static void emitTrail(GlowPoint[] points, int segments, Vector3f right, Vector3f up,
                                  double time, double phase, TrailVertex core, TrailVertex halo) {
        for (int i = 1; i <= segments; i++) {
            ribbon(core, points[i - 1], points[i], false, 1, true);
            ribbon(core, points[i - 1], points[i], true, 0.60F, true);
        }

        int glintStride = Math.max(1, (segments + 7) / 8);
        for (int i = 1; i <= segments; i++) {
            GlowPoint current = points[i - 1], next = points[i];
            double age = (double) i / segments;
            ribbon(halo, current, next, false, 1, false);
            ribbon(halo, current, next, true, 0.45F, false);
            if (i % glintStride == 0 && age < 0.9) {
                double pulse = Math.pow(Math.max(0, Math.sin(time * 0.24 - age * 19 + phase * 6.28)), 6);
                glint(halo, next, right, up, next.haloWidth * (0.18 + pulse * 0.40), (float) pulse);
            }
        }
    }

    private static GlowPoint[] createTrailPoints(int count) {
        GlowPoint[] points = new GlowPoint[count];
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

    private static void glint(TrailVertex vertex, GlowPoint point, Vector3f right, Vector3f up,
                              double size, float pulse) {
        double cameraDistance = Math.sqrt(point.cameraX * point.cameraX
            + point.cameraY * point.cameraY + point.cameraZ * point.cameraZ);
        pulse *= (float) cameraFade(Math.max(0, cameraDistance - size));
        if (pulse < 0.05F || point.alpha < 0.01F) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            double ax = i == 0 ? right.x : i == 1 ? up.x : i == 2 ? -right.x : -up.x;
            double ay = i == 0 ? right.y : i == 1 ? up.y : i == 2 ? -right.y : -up.y;
            double az = i == 0 ? right.z : i == 1 ? up.z : i == 2 ? -right.z : -up.z;
            int j = (i + 1) % 4;
            double bx = j == 0 ? right.x : j == 1 ? up.x : j == 2 ? -right.x : -up.x;
            double by = j == 0 ? right.y : j == 1 ? up.y : j == 2 ? -right.y : -up.y;
            double bz = j == 0 ? right.z : j == 1 ? up.z : j == 2 ? -right.z : -up.z;
            vertex.add(point.x, point.y, point.z, 1, 1, 1, point.alpha * pulse);
            vertex.add(point.x + ax * size, point.y + ay * size, point.z + az * size,
                point.r, point.g, point.b, 0);
            vertex.add(point.x + bx * size, point.y + by * size, point.z + bz * size,
                point.r, point.g, point.b, 0);
            vertex.add(point.x + bx * size, point.y + by * size, point.z + bz * size,
                point.r, point.g, point.b, 0);
        }
    }

    private static TrailVertex stagedVertices(VertexConsumer consumer, PoseStack.Pose pose) {
        return (x, y, z, r, g, b, alpha) -> consumer.addVertex(pose, (float) x, (float) y, (float) z)
            .setColor(r, g, b, alpha);
    }

    private static TrailVertex entityVertices(VertexConsumer consumer, PoseStack.Pose pose) {
        Vector3f normal = pose.transformNormal(0, 1, 0, new Vector3f());
        return (x, y, z, r, g, b, alpha) -> consumer.addVertex(pose, (float) x, (float) y, (float) z)
            .setColor(r, g, b, alpha).setUv(0.5F, 0.5F).setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(LightCoordsUtil.FULL_BRIGHT).setNormal(normal.x, normal.y, normal.z);
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

    private static final class Orbit {
        private double a, b, ux, uy, uz, zx, zy, zz, nx, ny, nz;
        private double cx, cy, cz, x, y, z;

        private void set(Vector3[] ref, Vector3 normal, double distance, double angle) {
            a = distance / 2;
            b = Math.min(distance / 4, 10);
            double cos = Math.cos(angle), sin = Math.sin(angle);
            ux = ref[0].x() * cos + ref[1].x() * sin;
            uy = ref[0].y() * cos + ref[1].y() * sin;
            uz = ref[0].z() * cos + ref[1].z() * sin;
            zx = ref[2].x();
            zy = ref[2].y();
            zz = ref[2].z();
            nx = normal.x();
            ny = normal.y();
            nz = normal.z();
            cx = (a - 2) * zx;
            cy = (a - 2) * zy;
            cz = (a - 2) * zz;
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
