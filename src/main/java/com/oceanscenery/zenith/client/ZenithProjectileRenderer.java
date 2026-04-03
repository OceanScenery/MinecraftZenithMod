package com.oceanscenery.zenith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.entity.ZenithProjectile;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.tool.PosUtil;
import com.oceanscenery.zenith.tool.Quaternion;
import com.oceanscenery.zenith.tool.RenderUtil;
import com.oceanscenery.zenith.tool.Vector3;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
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
import net.neoforged.neoforge.client.model.standalone.StandaloneModelLoader;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

import java.util.List;

public class ZenithProjectileRenderer extends EntityRenderer<ZenithProjectile,ZenithProjectileRenderState> {
    protected ItemModelResolver resolver;

    public static final Quaternionf Q1 = Quaternion.trans(new Vector3(1, 1, 0), new Vector3(0, 1, 0)).toQuaternionf();
    public static final Quaternionf Q2 = Quaternion.trans(new Vector3(0, 1, 0), new Vector3(0, 0, -1)).toQuaternionf();

    public static final int[][] COLOR = new int[][]{
            {204, 255, 255},
            {153, 255, 204},
            {32, 32, 32},
            {0, 204, 102},
            {178, 0, 0},
            {255, 153, 51},
            {153, 153, 255},
            {255, 255, 102},
            {153, 204, 255},
            {127, 0, 255},
            {255, 178, 255},
            {51, 51, 255},
            {153, 51, 255},
            {0, 153, 0},
            {255, 0, 127},
            {255, 51, 153},
            {0, 255, 0},
            {255, 128, 0},
            {255, 255, 0},
            {204, 153, 255},
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

    protected ZenithProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.resolver = context.getItemModelResolver();
    }

    @Override
    public boolean shouldRender(@NotNull ZenithProjectile entity, @NotNull Frustum camera, double camX, double camY, double camZ) {
        if (entity.isRemoved() || !entity.isAlive()) {
            return false;
        }
        return true;
    }

    @Override
    public ZenithProjectileRenderState createRenderState() {
        ZenithProjectileRenderState state = new ZenithProjectileRenderState(
                0, 0, 20, 0, -1
        );
        state.itemState = new ItemStackRenderState();
        return state;
    }

    @Override
    public void extractRenderState(ZenithProjectile entity, ZenithProjectileRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.reference = entity.getReference();
        state.distance = entity.getDistance();
        state.actuallyProgress = entity.getLocalProgress();
        state.level = (net.minecraft.client.multiplayer.ClientLevel) entity.level();
        state.angle = entity.getAngle();
        state.fixed = entity.getFixedPose();
        state.ownerId = entity.getOwnerID();
        state.swordType = entity.getSwordType();
        CustomModelData data = new CustomModelData(
                List.of((float) state.swordType),
                List.of(), List.of(), List.of()
        );
        ItemStack stack = new ItemStack(ZenithItems.ZENITH, 1, DataComponentPatch.builder().set(DataComponents.CUSTOM_MODEL_DATA, data).build());

        resolver.updateForNonLiving(
                state.itemState, stack, ItemDisplayContext.NONE, entity
        );
    }

    @Override
    protected void finalizeRenderState(ZenithProjectile entity, ZenithProjectileRenderState state) {
        super.finalizeRenderState(entity, state);
    }

    @Override
    public void submit(ZenithProjectileRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, CameraRenderState camera) {
        super.submit(state, poseStack, submitNodeCollector, camera);

        double TOTAL_ANGLE = Math.toRadians(ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.getAsDouble());
        int AMOUNT = (int) (60 * ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.getAsDouble() / 80);
        double ONCE_ANGLE = TOTAL_ANGLE / AMOUNT;
        double angle = state.angle;
        double distance = state.distance;
        int progress = state.actuallyProgress;
        float partialTick = state.partialTick;
        double progress_angle = Mth.TWO_PI * Mth.lerp(partialTick, (double) progress - 1, progress) / ZenithProjectile.STAGE_COUNT;

        Quaternion[] fixed = state.fixed;
        Quaternion r1 = fixed[0];
        Quaternion r2 = fixed[1];
        Quaternion r3 = fixed[2];
        Quaternion tmp = r3.multiply(r2).multiply(r1);
        Vector3 normal = new Vector3(0, 1, 0).rot(tmp);

        poseStack.translate(-state.x, -state.y, -state.z);
        Vector3[] relative_center = state.reference;
        Vector3[] relative_world = Vector3.WORLD;

        if (state.getOwner() == null) {
            return;
        }

        state.render_pos = new Vector3(0, 0, -1).VecInNewRefer(relative_center, relative_world).add(new Vector3(state.getOwner().getEyePosition(state.partialTick)));
        boolean firstPerson = Minecraft.getInstance().getCameraEntity() == state.getOwner();
        boolean isCamera = Minecraft.getInstance().getCameraEntity() == state.getOwner();

        if (firstPerson && Minecraft.getInstance().options.getCameraType().isFirstPerson() && ZenithConfigs.ZENITH_CLIENT_CONFIG.RENDER_OFFSET.get()) {
            Vector3 cameraT = new Vector3(0, -1, 0).VecInNewRefer(
                    relative_center,
                    relative_world
            );
            poseStack.translate(cameraT.getX(), cameraT.getY(), cameraT.getZ());
        }

        SubmitNodeCollector.CustomGeometryRenderer trailRenderer = (pose, vertexConsumer) -> {
            this.renderTrail(state, pose, vertexConsumer, camera, isCamera, normal);
        };

        submitNodeCollector.submitCustomGeometry(
                poseStack,
                RenderTypes.entityTranslucent(Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID, "textures/entity/trail.png")),
                trailRenderer
        );

        Vector3 near = PosUtil.calCenPos(distance, progress_angle, angle).VecInNewRefer(
                relative_center,
                relative_world
        );

        Vector3 center = new Vector3(0, 0, distance / 2 - 1).VecInNewRefer(
                relative_center, relative_world
        ).add(Vector3.transToVector3(state.getOwner().getEyePosition(state.partialTick)));

        Vector3 offset = new Vector3(0, 0, -1).VecInNewRefer(
                relative_center, relative_world
        );

        Vector3 sword_pos = near.add(center).add(offset);
        poseStack.translate(sword_pos.getX(), sword_pos.getY(), sword_pos.getZ());

        Quaternion r4 = Quaternion.rotate(normal, -PosUtil.getPolar(progress_angle, distance));

        Vector3 near_inner = near.applyOffset(-0.5).add(!firstPerson ? new Vector3(0, 0, 0) : relative_center[1].multiply(-0.2).add(center).add(offset));
        Vector3 near_outer = near.applyOffset(0.5).add(!firstPerson ? new Vector3(0, 0, 0) : relative_center[1].multiply(0.2).add(center).add(offset));
        state.round = near_outer.multiply(-1).add(near_inner);

        Vector3 toCenter = new Vector3(0, 0, 1).rot(r4.multiply(tmp));
        if (state.round != null && firstPerson && !ZenithConfigs.ZENITH_CLIENT_CONFIG._3D_TRAIL.get()) {
            poseStack.mulPose(Quaternion.trans(toCenter, state.round).toQuaternionf());
        }

        poseStack.mulPose(r4.toQuaternionf());
        poseStack.mulPose(r3.toQuaternionf());
        poseStack.mulPose(r2.toQuaternionf());
        poseStack.mulPose(r1.toQuaternionf());

        poseStack.mulPose(Q2);
        poseStack.mulPose(Q1);

        state.itemState.submit(
                poseStack,
                submitNodeCollector,
                LightCoordsUtil.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0
        );
    }

    public void renderTrail(ZenithProjectileRenderState state, PoseStack.Pose pose, VertexConsumer vertex, CameraRenderState camera, boolean firstPerson, Vector3 normal) {
        double distance = state.distance;
        Vector3[] relative_center = state.reference;
        Vector3[] relative_world = Vector3.WORLD;
        double TOTAL_ANGLE = Math.toRadians(ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.getAsDouble());
        int AMOUNT = ZenithConfigs.ZENITH_CLIENT_CONFIG._3D_TRAIL.get()?(int) (60 * ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.getAsDouble() / 80)/2:(int) (60 * ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.getAsDouble() / 80);
        double ONCE_ANGLE = TOTAL_ANGLE / AMOUNT;
        float partialTick = state.partialTick;
        int progress = state.actuallyProgress;
        double progress_angle = Mth.TWO_PI * Mth.lerp(partialTick, (double) progress - 1, progress) / ZenithProjectile.STAGE_COUNT;
        double angle = state.angle;
        int[] color = COLOR[state.swordType];

        Vector3 center = new Vector3(0, 0, distance / 2 - 1).VecInNewRefer(
                relative_center, relative_world
        ).add(Vector3.transToVector3(state.getOwner().getEyePosition(state.partialTick)));

        Vector3 offset = new Vector3(0, 0, -1).VecInNewRefer(
                relative_center, relative_world
        );
        if (!ZenithConfigs.ZENITH_CLIENT_CONFIG._3D_TRAIL.get()) {
            Vector3 last_inner = null, last_outer = null;
            Vector3 near_inner, near_outer, far_inner, far_outer;

            for (int i = 0; i < AMOUNT && (i + 1) * ONCE_ANGLE < progress_angle; i++) {

                int factorI = i + AMOUNT - Math.min(AMOUNT, (int) (progress_angle / ONCE_ANGLE));
                double current_angle = progress_angle - i * ONCE_ANGLE;
                double next_angle = progress_angle - (i + 1) * ONCE_ANGLE;
                if (last_inner == null) {
                    Vector3 near = PosUtil.calCenPos(distance, current_angle, angle).VecInNewRefer(
                            relative_center,
                            relative_world
                    );
                    state.render_pos = near.add(center).add(offset);
                    near_inner = near.applyOffset(-0.5 + factorI * (0.4 / AMOUNT)).add(!firstPerson ? new Vector3(0, 0, 0) : relative_center[1].multiply(-0.1 + (0.08 / AMOUNT) * factorI)).add(center).add(offset);
                    near_outer = near.applyOffset(0.5 - factorI * (0.4 / AMOUNT)).add(!firstPerson ? new Vector3(0, 0, 0) : relative_center[1].multiply(0.1 - (0.08 / AMOUNT) * factorI)).add(center).add(offset);
                    state.round = near_outer.multiply(-1).add(near_inner);
                } else {
                    near_inner = last_inner;
                    near_outer = last_outer;
                }
                Vector3 far = PosUtil.calCenPos(distance, next_angle, angle).VecInNewRefer(
                        relative_center,
                        relative_world
                );

                far_inner = far.applyOffset(-0.5 + factorI * (0.4 / AMOUNT)).add(!firstPerson ? new Vector3(0, 0, 0) : relative_center[1].multiply(-0.1 + factorI * (0.08 / AMOUNT))).add(center).add(offset);
                far_outer = far.applyOffset(0.5 - factorI * (0.4 / AMOUNT)).add(!firstPerson ? new Vector3(0, 0, 0) : relative_center[1].multiply(0.1 - factorI * (0.08 / AMOUNT))).add(center).add(offset);
                last_inner = far_inner;
                last_outer = far_outer;

                Vector3 norm=far_inner.subtract(far_outer).cross(far_inner.subtract(near_inner)).normalize();

                vertex.addVertex(pose, far_inner.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(0, 0)
                        .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0], color[1], color[2], 255 - (factorI + 1) * (240 / AMOUNT)).setNormal(pose, (float) norm.getX(), (float) norm.getY(), (float) norm.getZ());
                vertex.addVertex(pose, far_outer.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(0, 1)
                        .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0], color[1], color[2], 255 - (factorI + 1) * (240 / AMOUNT)).setNormal(pose, (float) norm.getX(), (float) norm.getY(), (float) norm.getZ());
                vertex.addVertex(pose, near_outer.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(1, 1)
                        .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0], color[1], color[2], 255 - factorI * (240 / AMOUNT)).setNormal(pose, (float) norm.getX(), (float) norm.getY(), (float) norm.getZ());
                vertex.addVertex(pose, near_inner.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(1, 0)
                        .setLight(LightCoordsUtil.FULL_BRIGHT).setColor(color[0], color[1], color[2], 255 - factorI * (240 / AMOUNT)).setNormal(pose, (float) norm.getX(), (float) norm.getY(), (float) norm.getZ());
            }
        } else {
            Vector3[] cp = new Vector3[4], np = new Vector3[4];
            Vector3[] save = null;
            for (int i = 0; i < AMOUNT && (i + 1) * ONCE_ANGLE < progress_angle; i++) {
                int factorI = i + AMOUNT - Math.min(AMOUNT, (int) (progress_angle / ONCE_ANGLE));
                double current_angle = progress_angle - i * ONCE_ANGLE;
                double next_angle = progress_angle - (i + 1) * ONCE_ANGLE;
                if (save == null) {
                    Vector3 near = PosUtil.calCenPos(distance, current_angle, angle).VecInNewRefer(
                            relative_center,
                            relative_world
                    );
                    cp[0] = near.applyOffset(-0.5 + factorI * (0.4 / AMOUNT)).add(center).add(offset).add(normal.multiply(0.025));
                    cp[1] = cp[0].subtract(normal.multiply(0.05));
                    cp[2] = near.applyOffset(0.5 - factorI * (0.4 / AMOUNT)).add(center).add(offset).subtract(normal.multiply(0.025));
                    cp[3] = cp[2].add(normal.multiply(0.05));
                    save = new Vector3[4];
                } else {
                    System.arraycopy(save, 0, cp, 0, 4);
                }
                Vector3 far = PosUtil.calCenPos(distance, next_angle, angle).VecInNewRefer(
                        relative_center,
                        relative_world
                );

                np[0] = far.applyOffset(-0.5 + factorI * (0.4 / AMOUNT)).add(center).add(offset).add(normal.multiply(0.025));
                np[1] = np[0].subtract(normal.multiply(0.05));
                np[2] = far.applyOffset(0.5 - factorI * (0.4 / AMOUNT)).add(center).add(offset).subtract(normal.multiply(0.025));
                np[3] = np[2].add(normal.multiply(0.05));

                System.arraycopy(np, 0, save, 0, 4);

                RenderUtil.createTrail(vertex, np, cp, pose, 255 - (factorI + 1) * (240 / AMOUNT), color);
            }
        }
    }
}