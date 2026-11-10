package com.oceanscenery.zenith.client.legacy;

import com.mojang.blaze3d.vertex.PoseStack;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.util.PosUtil;
import com.oceanscenery.zenith.util.Quaternion;
import com.oceanscenery.zenith.util.Vector3;
import com.oceanscenery.zenith.zenith_class.entity.ZenithProjectile;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Quaternionf;

/** Original sword rendering, kept for reference; not registered or instantiated by the mod. */
public class LegacyZenithProjectileRenderer extends EntityRenderer<ZenithProjectile> {
    public final ItemRenderer itemRenderer;

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

    protected LegacyZenithProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
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
        return ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID, "item/zenith_sword");
    }

    @Override
    public void render(ZenithProjectile entity, float entityYaw, float partialTick, @NotNull PoseStack poseStack, @NotNull MultiBufferSource bufferSource, int packedLight) {
        if (!entity.isAlive() || entity.isRemoved()) {
            return;
        }
        int sword_type = entity.getSwordType();

        ItemStack render_item = ZenithItems.ZENITH.get().getDefaultInstance();
        CompoundTag tag = render_item.getOrCreateTag();
        tag.putInt("CustomModelData", sword_type);
        render_item.setTag(tag);

        Quaternion[] fixed = entity.getFixedPose();
        Quaternion r1 = fixed[0];
        Quaternion r2 = fixed[1];
        Quaternion r3 = fixed[2];
        Vector3 normal = LegacyRenderZenithTrail.normalOf(entity);

        Vec3 pos = entity.getPosition(partialTick);
        poseStack.pushPose();
        poseStack.translate(-pos.x, -pos.y, -pos.z);

        LegacyRenderZenithTrail.TrailResult trail = LegacyRenderZenithTrail.compute(entity, partialTick, poseStack, null, normal);
        if (trail == null) {
            poseStack.popPose();
            return;
        }

        poseStack.translate(trail.swordPos().x(), trail.swordPos().y(), trail.swordPos().z());

        Quaternion r4 = Quaternion.rotate(normal, -PosUtil.getPolar(trail.progressAngle(), trail.distance()));

        poseStack.mulPose(r4.toQuaternionf());
        poseStack.mulPose(r3.toQuaternionf());
        poseStack.mulPose(r2.toQuaternionf());
        poseStack.mulPose(r1.toQuaternionf());

        poseStack.mulPose(Q2);
        poseStack.mulPose(Q1);

        BakedModel model = this.itemRenderer.getModel(render_item, entity.level(), null, entity.getId());

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
