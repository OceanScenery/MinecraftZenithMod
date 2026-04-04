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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

public class ZenithProjectileRenderer extends EntityRenderer<ZenithProjectile> {
    public final ItemRenderer itemRenderer;

    public static final Quaternionf Q1=Quaternion.trans(new Vector3(1,1,0),new Vector3(0,1,0)).toQuaternionf();
    public static final Quaternionf Q2=Quaternion.trans(new Vector3(0,1,0),new Vector3(0,0,-1)).toQuaternionf();

    public static final int[][] COLOR=new int[][]{
            {204,255,255},
            {153,255,204},
            {32,32,32},
            {0,204,102},
            {178,0,0},
            {255,153,51},
            {153,153,255},
            {255,255,102},
            {153,204,255},
            {127,0,255},
            {255,178,255},
            {51,51,255},
            {153,51,255},
            {0,153,0},
            {255,0,127},
            {255,51,153},
            {0,255,0},
            {255,128,0},
            {255,255,0},
            {204,153,255},
            {255,64,0}
    };

    public static final String[] SWORD_MODEL=new String[]{
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
        this.itemRenderer=context.getItemRenderer();
    }

    @Override
    public boolean shouldRender(@NotNull ZenithProjectile livingEntity, @NotNull Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    protected boolean shouldShowName(@NotNull ZenithProjectile entity) {
        return false;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull ZenithProjectile entity) {
        return ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"item/zenith_sword");
    }

    @Override
    public void render(ZenithProjectile entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        double TOTAL_ANGLE=Math.toRadians(ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.getAsDouble());
        int AMOUNT= ZenithConfigs.ZENITH_CLIENT_CONFIG._3D_TRAIL.get()?(int)(60* ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.getAsDouble()/80)/2:(int)(60* ZenithConfigs.ZENITH_CLIENT_CONFIG.TRAIL_ANGLE.getAsDouble()/80);
        double ONCE_ANGLE=TOTAL_ANGLE/AMOUNT;

        Quaternion[] fixed=entity.getFixedPose();
        Quaternion r1=fixed[0];
        Quaternion r2=fixed[1];
        Quaternion r3=fixed[2];
        Quaternion tmp=r3.multiply(r2).multiply(r1);
        Vector3 normal=new Vector3(0,1,0).rot(tmp).normalize();

        if(!entity.isAlive() || entity.isRemoved()){
            poseStack.popPose();
            return;
        }

        poseStack.pushPose();
        int sword_type=entity.getSwordType();

        ItemStack render_item=new ItemStack(ZenithItems.ZENITH,1, DataComponentPatch.builder().set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(sword_type)).build());

        int progress=Math.min(entity.getLocalProgress(),ZenithProjectile.STAGE_COUNT+1);
        if(progress>ZenithProjectile.STAGE_COUNT){
            poseStack.popPose();
            return;
        }

        int[] color=COLOR[sword_type];
        double angle=entity.getAngle();
        double distance=entity.getDistance();
        double progress_angle=Mth.TWO_PI*Mth.lerp(partialTick,(double)progress-1, progress)/ZenithProjectile.STAGE_COUNT;

        VertexConsumer vertex= bufferSource.getBuffer(RenderType.entityTranslucent(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"textures/entity/trail.png")));

        if(!(entity.level().getEntity(entity.getOwnerID()) instanceof LivingEntity owner)){
            poseStack.popPose();
            return;
        }

        Vec3 pos=entity.getPosition(partialTick);

        poseStack.translate(-pos.x, -pos.y, -pos.z);
        Vector3[] relative_center=entity.getReference();
        Vector3[] relative_world=Vector3.WORLD;
        Vector3 sword_pos = new Vector3(0,0,-1).VecInNewRefer(relative_center,relative_world).add(Vector3.transToVector3(owner.getEyePosition(partialTick)));
        boolean firstPerson=owner==Minecraft.getInstance().cameraEntity;

        if(firstPerson && Minecraft.getInstance().options.getCameraType().isFirstPerson() && ZenithConfigs.ZENITH_CLIENT_CONFIG.RENDER_OFFSET.get()){
            Vector3 cameraT=new Vector3(0,-1,0).VecInNewRefer(
                    relative_center,
                    relative_world
            );
            poseStack.translate(cameraT.getX(),cameraT.getY(),cameraT.getZ());
        }

        Vector3 center=new Vector3(0,0,distance/2-1).VecInNewRefer(
            relative_center,relative_world
        ).add(Vector3.transToVector3(owner.getEyePosition(partialTick)));

        Vector3 offset=new Vector3(0,0,-1).VecInNewRefer(
                relative_center,relative_world
        );
        Vector3 round=null;
        if(!ZenithConfigs.ZENITH_CLIENT_CONFIG._3D_TRAIL.get()){
            Vector3 last_inner = null, last_outer = null;
            Vector3 near_inner, near_outer, far_inner, far_outer;
            for (int i = 0; i < AMOUNT && i * ONCE_ANGLE < progress_angle; i++) {

                int factorI = i + AMOUNT - Math.min(AMOUNT, (int) (progress_angle / ONCE_ANGLE));
                double current_angle = progress_angle - i * ONCE_ANGLE;
                double next_angle = Math.max(progress_angle - (i + 1) * ONCE_ANGLE,0);
                if (last_inner == null) {
                    Vector3 near = PosUtil.calCenPos(distance, current_angle, angle).VecInNewRefer(
                            relative_center,
                            relative_world
                    );
                    sword_pos = near.add(center).add(offset);
                    near_inner = near.applyOffset(-0.5 + factorI * (0.4 / AMOUNT)).add(!firstPerson ? new Vector3(0, 0, 0) : relative_center[1].multiply(-0.1 + (0.08 / AMOUNT) * factorI)).add(center).add(offset);
                    near_outer = near.applyOffset(0.5 - factorI * (0.4 / AMOUNT)).add(!firstPerson ? new Vector3(0, 0, 0) : relative_center[1].multiply(0.1 - (0.08 / AMOUNT) * factorI)).add(center).add(offset);
                    round = near_outer.multiply(-1).add(near_inner);
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

                vertex.addVertex(poseStack.last(), far_inner.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(0, 0)
                        .setLight(LightTexture.FULL_BRIGHT).setColor(color[0], color[1], color[2], 255 - (factorI + 1) * (240 / AMOUNT)).setNormal(poseStack.last(), 0, 1, 0);
                vertex.addVertex(poseStack.last(), far_outer.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(0, 1)
                        .setLight(LightTexture.FULL_BRIGHT).setColor(color[0], color[1], color[2], 255 - (factorI + 1) * (240 / AMOUNT)).setNormal(poseStack.last(), 0, 1, 0);
                vertex.addVertex(poseStack.last(), near_outer.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(1, 1)
                        .setLight(LightTexture.FULL_BRIGHT).setColor(color[0], color[1], color[2], 255 - factorI * (240 / AMOUNT)).setNormal(poseStack.last(), 0, 1, 0);
                vertex.addVertex(poseStack.last(), near_inner.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(1, 0)
                        .setLight(LightTexture.FULL_BRIGHT).setColor(color[0], color[1], color[2], 255 - factorI * (240 / AMOUNT)).setNormal(poseStack.last(), 0, 1, 0);
            }
        }else {
            Vector3[] cp= new Vector3[4],np= new Vector3[4];
            Vector3[] save=null;
            for (int i = 0; i < AMOUNT && i * ONCE_ANGLE < progress_angle; i++) {
                int factorI = i + AMOUNT - Math.min(AMOUNT, (int) (progress_angle / ONCE_ANGLE));
                double current_angle = progress_angle - i * ONCE_ANGLE;
                double next_angle = Math.max(progress_angle - (i + 1) * ONCE_ANGLE,0);
                if (save == null) {
                    Vector3 near = PosUtil.calCenPos(distance, current_angle, angle).VecInNewRefer(
                            relative_center,
                            relative_world
                    );
                    sword_pos = near.add(center).add(offset);
                    cp[0] = near.applyOffset(-0.5 + factorI * (0.4 / AMOUNT)).add(center).add(offset).add(normal.multiply(0.025));
                    cp[1] = cp[0].subtract(normal.multiply(0.05));
                    cp[2] = near.applyOffset(0.5 - factorI * (0.4 / AMOUNT)).add(center).add(offset).subtract(normal.multiply(0.025));
                    cp[3] = cp[2].add(normal.multiply(0.05));
                    round=null;
                    save=new Vector3[4];
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

                System.arraycopy(np,0,save,0,4);

                RenderUtil.createTrail(vertex,np,cp,poseStack,255 - (factorI + 1) * (240 / AMOUNT),color);
            }
        }
        poseStack.translate(sword_pos.getX(),sword_pos.getY(),sword_pos.getZ());

        Quaternion r4=Quaternion.rotate(normal,-PosUtil.getPolar(progress_angle,distance));

        Vector3 toCenter=new Vector3(0,0,1).rot(r4.multiply(tmp));
        if(round!=null && firstPerson){
            poseStack.mulPose(Quaternion.trans(toCenter, round).toQuaternionf());
        }

        poseStack.mulPose(r4.toQuaternionf());
        poseStack.mulPose(r3.toQuaternionf());
        poseStack.mulPose(r2.toQuaternionf());
        poseStack.mulPose(r1.toQuaternionf());

        poseStack.mulPose(Q2);
        poseStack.mulPose(Q1);

        BakedModel model=this.itemRenderer.getModel(render_item,entity.level(),null,entity.getId());

        itemRenderer.render(
                render_item,
                ItemDisplayContext.NONE,
                false,
                poseStack,
                bufferSource,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                model
        );


        poseStack.popPose();
    }
}
