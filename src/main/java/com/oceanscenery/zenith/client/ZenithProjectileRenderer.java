package com.oceanscenery.zenith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
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
    private final ItemStack[] renderItems = new ItemStack[COLORS.length];
    private final Orbit orbit = new Orbit();
    private final GlowPoint[] trailPoints = createTrailPoints();
    private final Vector3f cameraRight = new Vector3f();
    private final Vector3f cameraUp = new Vector3f();

    public ZenithProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.resolver = context.getItemModelResolver();
    }

    @Override
    public boolean shouldRender(ZenithProjectile entity, Frustum frustum, double x, double y, double z, float partialTick) {
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

        ItemStack stack = renderItems[type];
        if (stack == null) {
            CustomModelData model = new CustomModelData(List.of((float) type), List.of(), List.of(), List.of());
            stack = new ItemStack(ZenithItems.ZENITH, 1,
                DataComponentPatch.builder().set(DataComponents.CUSTOM_MODEL_DATA, model).build());
            renderItems[type] = stack;
        }
        resolver.updateForNonLiving(state.itemState, stack, ItemDisplayContext.NONE, entity);
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

            poses.pushPose();
            try {
                poses.translate(swordX, swordY, swordZ);
                poses.rotate(Quaternion.rotate(state.normal, -PosUtil.getPolar(state.progressAngle, state.distance)).toQuaternionf());
                poses.rotate(state.fixed[2]);
                poses.rotate(state.fixed[1]);
                poses.rotate(state.fixed[0]);
                poses.rotate(Q2);
                poses.rotate(Q1);
                state.itemState.submit(poses, collector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
            } finally {
                poses.popPose();
            }

            if (state.progressAngle > 0 && RenderZenithTrail.isCollecting()) {
                renderTrail(state, poses.last(), camera);
            }
        } finally {
            poses.popPose();
        }
    }

    private void renderTrail(ZenithProjectileRenderState state, PoseStack.Pose pose, CameraRenderState camera) {
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
        cameraRight.set(1, 0, 0).rotate(camera.orientation);
        cameraUp.set(0, 1, 0).rotate(camera.orientation);
        double growth = smoothStep(Math.min(1, progressAngle / 0.20));
        for (int i = 0; i <= segments; i++) {
            double age = (double) i / segments;
            sampleGlow(trailPoints[i], Math.max(0, progressAngle - span * age), age,
                widthScale, growth, time, phase, color, viewX, viewY, viewZ, pixelScale);
        }

        VertexConsumer core = RenderZenithTrail.coreVertices();
        for (int i = 1; i <= segments; i++) {
            ribbon(core, pose, trailPoints[i - 1], trailPoints[i], false, 1, true);
            ribbon(core, pose, trailPoints[i - 1], trailPoints[i], true, 0.60F, true);
        }

        VertexConsumer halo = RenderZenithTrail.haloVertices();
        int glintStride = Math.max(1, (segments + 7) / 8);
        for (int i = 1; i <= segments; i++) {
            GlowPoint current = trailPoints[i - 1], next = trailPoints[i];
            double age = (double) i / segments;
            ribbon(halo, pose, current, next, false, 1, false);
            ribbon(halo, pose, current, next, true, 0.45F, false);
            if (i % glintStride == 0 && age < 0.9) {
                double pulse = Math.pow(Math.max(0, Math.sin(time * 0.24 - age * 19 + phase * 6.28)), 6);
                glint(halo, pose, next, next.haloWidth * (0.18 + pulse * 0.40), (float) pulse);
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

    private static void ribbon(VertexConsumer vertex, PoseStack.Pose pose, GlowPoint near,
                               GlowPoint far, boolean cross, float opacity, boolean core) {
        int bandCount = core ? CORE_BANDS.length : BANDS.length;
        for (int band = 0; band < bandCount - 1; band++) {
            float bandOpacity = core ? opacity : opacity * (float) haloBandFade(near, far, cross, band);
            bandVertex(vertex, pose, far, band, cross, bandOpacity, core);
            bandVertex(vertex, pose, far, band + 1, cross, bandOpacity, core);
            bandVertex(vertex, pose, near, band + 1, cross, bandOpacity, core);
            bandVertex(vertex, pose, near, band, cross, bandOpacity, core);
        }
    }

    private static void bandVertex(VertexConsumer vertex, PoseStack.Pose pose, GlowPoint point,
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
        glowVertex(vertex, pose, point.x + ax * extent, point.y + ay * extent, point.z + az * extent,
            point.r + (1 - point.r) * white, point.g + (1 - point.g) * white,
            point.b + (1 - point.b) * white, alpha * opacity);
    }

    private void glint(VertexConsumer vertex, PoseStack.Pose pose, GlowPoint point,
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
            glowVertex(vertex, pose, point.x, point.y, point.z, 1, 1, 1, point.alpha * pulse);
            glowVertex(vertex, pose, point.x + ax * size, point.y + ay * size, point.z + az * size,
                point.r, point.g, point.b, 0);
            glowVertex(vertex, pose, point.x + bx * size, point.y + by * size, point.z + bz * size,
                point.r, point.g, point.b, 0);
            glowVertex(vertex, pose, point.x + bx * size, point.y + by * size, point.z + bz * size,
                point.r, point.g, point.b, 0);
        }
    }

    private static void glowVertex(VertexConsumer vertex, PoseStack.Pose pose,
                                   double x, double y, double z, float r, float g, float b, float alpha) {
        vertex.addVertex(pose, (float) x, (float) y, (float) z).setColor(r, g, b, alpha);
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
