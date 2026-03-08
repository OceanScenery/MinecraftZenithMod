package com.oceanscenery.zenith.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.entity.ZenithProjectile;
import com.oceanscenery.zenith.registry.ModConfigs;
import com.oceanscenery.zenith.registry.ModItems;
import com.oceanscenery.zenith.tool.PosUtil;
import com.oceanscenery.zenith.tool.Quaternion;
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.phys.Vec3;

public class ZenithProjectileRenderer extends EntityRenderer<ZenithProjectile> {
    public final ItemRenderer itemRenderer;
    public static final double TOTAL_ANGLE=Math.toRadians(80);
    public static final int AMOUNT=60;
    public static final double ONCE_ANGLE=TOTAL_ANGLE/AMOUNT;

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
    public boolean shouldRender(ZenithProjectile livingEntity, Frustum camera, double camX, double camY, double camZ) {
        return true;
    }

    @Override
    protected boolean shouldShowName(ZenithProjectile entity) {
        return false;
    }

    @Override
    public ResourceLocation getTextureLocation(ZenithProjectile entity) {
        return ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"item/zenith_sword");
    }

    @Override
    public void render(ZenithProjectile entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if(!entity.isAlive() || entity.isRemoved()){
            return;
        }

        poseStack.pushPose();
        int sword_type=entity.getSwordType();

        ItemStack render_item=new ItemStack(ModItems.ZENITH,1, DataComponentPatch.builder().set(DataComponents.CUSTOM_MODEL_DATA,new CustomModelData(sword_type)).build());

        Vector3 vec=entity.getFacingVector();
        Vector3 vec_last=entity.getLastVector();
        Vector3 rotFactor=vec_last.multiply(1-partialTick).add(vec.multiply(partialTick));
        int progress= Math.min(entity.getLocalProgress(),ZenithProjectile.STAGE_COUNT);

        int[] color=COLOR[sword_type];
        double angle=entity.getAngle();
        double distance=entity.getDistance();
        double progress_angle=Mth.TWO_PI*Mth.lerp(partialTick,(double)progress-1,(double)progress)/ZenithProjectile.STAGE_COUNT;

        VertexConsumer vertex= bufferSource.getBuffer(RenderType.entityTranslucentEmissive(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"textures/entity/trail.png")));

        Player owner=(Player)entity.level().getEntity(entity.getOwnerID());

        Vec3 pos=entity.getPosition(partialTick);

        if(owner!=null){
            poseStack.translate(-pos.x,-pos.y,-pos.z);

            PosUtil.Rotation rot=entity.getIniRot();
            Vector3[] relative_center=Vector3.getReferFromAngle(rot.getPitch(),rot.getYaw());
            Vector3[] relative_world=new Vector3[]{new Vector3(1,0,0),new Vector3(0,1,0),new Vector3(0,0,1)};
            Vector3 sword_pos = new Vector3(0,0,0);

            if(Minecraft.getInstance().options.getCameraType().isFirstPerson() && owner == Minecraft.getInstance().cameraEntity && ModConfigs.ZENITH_CLIENT_CONFIG.RENDER_OFFSET.get()){
                Vector3 cameraT=new Vector3(0,1,0).VecInNewRefer(
                        relative_center,
                        relative_world
                );
                poseStack.translate(cameraT.getX(),cameraT.getY(),cameraT.getZ());
            }

            Vector3 center=new Vector3(0,0,distance/2).VecInNewRefer(
                relative_center,relative_world
            ).add(Vector3.transToVector3(owner.getEyePosition(partialTick)));

            Vector3 last_inner=null,last_outer=null;
            Vector3 near_inner,near_outer,far_inner,far_outer;
            for(int i=0;i<AMOUNT && (i+1)*ONCE_ANGLE<progress_angle;i++){

                int factorI=i+AMOUNT-Math.min(AMOUNT,(int)(progress_angle/ONCE_ANGLE));
                double current_angle=progress_angle-i*ONCE_ANGLE;
                double next_angle=progress_angle-(i+1)*ONCE_ANGLE;
                if(last_inner == null){
                    Vector3 near=PosUtil.calCenPos(distance,current_angle,angle).add(new Vector3(0,0,-1)).VecInNewRefer(
                            relative_center,
                            relative_world
                    );
                    sword_pos=near.add(center);
                    near_inner=near.applyOffset(-0.5+factorI*(0.4/AMOUNT)).add(relative_center[1].multiply(-0.2+(0.16/AMOUNT)*factorI)).add(center);
                    near_outer=near.applyOffset(0.5-factorI*(0.4/AMOUNT)).add(relative_center[1].multiply(0.2-(0.16/AMOUNT)*factorI)).add(center);
                }else{
                    near_inner=last_inner;
                    near_outer=last_outer;
                }
                Vector3 far=PosUtil.calCenPos(distance,next_angle,angle).add(new Vector3(0,0,-1)).VecInNewRefer(
                        relative_center,
                        relative_world
                );

                far_inner=far.applyOffset(-0.5+factorI*(0.4/AMOUNT)).add(relative_center[1].multiply(-0.2+factorI*(0.16/AMOUNT))).add(center);
                far_outer=far.applyOffset(0.5-factorI*(0.4/AMOUNT)).add(relative_center[1].multiply(0.2-factorI*(0.16/AMOUNT))).add(center);
                last_inner=far_inner;
                last_outer=far_outer;

                vertex.addVertex(poseStack.last(),far_inner.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(0,0)
                        .setLight(LightTexture.FULL_BRIGHT).setColor(color[0],color[1],color[2],255-(factorI+1)*(240/AMOUNT)).setNormal(poseStack.last(),0,1,0);
                vertex.addVertex(poseStack.last(),far_outer.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(0,1)
                        .setLight(LightTexture.FULL_BRIGHT).setColor(color[0],color[1],color[2],255-(factorI+1)*(240/AMOUNT)).setNormal(poseStack.last(),0,1,0);
                vertex.addVertex(poseStack.last(),near_outer.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(1,1)
                        .setLight(LightTexture.FULL_BRIGHT).setColor(color[0],color[1],color[2],255-factorI*(240/AMOUNT)).setNormal(poseStack.last(),0,1,0);
                vertex.addVertex(poseStack.last(),near_inner.toVector3f()).setOverlay(OverlayTexture.NO_OVERLAY).setUv(1,0)
                        .setLight(LightTexture.FULL_BRIGHT).setColor(color[0],color[1],color[2],255-factorI*(240/AMOUNT)).setNormal(poseStack.last(),0,1,0);


            }
            poseStack.translate(sword_pos.getX(),sword_pos.getY(),sword_pos.getZ());

            poseStack.mulPose(Quaternion.trans(new Vector3(0,0,1),rotFactor).toQuaternionf());
            poseStack.mulPose(Quaternion.trans(new Vector3(0,0,1),new Vector3(0,1,0)).toQuaternionf());
            poseStack.mulPose(Quaternion.trans(new Vector3(1,1,0),new Vector3(0,1,0)).toQuaternionf());

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
        }


        poseStack.popPose();
    }
}
