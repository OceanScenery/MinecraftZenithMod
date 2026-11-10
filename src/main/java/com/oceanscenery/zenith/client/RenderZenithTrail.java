package com.oceanscenery.zenith.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.renderpearl.api.commands.CommandEncoder;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.BlendFunction;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.StagedVertexBuffer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.FrameGraphSetupEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Optional;
import java.util.OptionalDouble;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID, value = Dist.CLIENT)
public final class RenderZenithTrail {
    private static final RenderType CORE_TYPE = createType("prismatic_trail_core", BlendFunction.TRANSLUCENT, true);
    private static final RenderType HALO_TYPE = createType("prismatic_trail_halo", BlendFunction.LIGHTNING, false);
    private static final Matrix4f MODEL_VIEW = new Matrix4f();

    private static StagedVertexBuffer geometry;
    private static StagedVertexBuffer.Draw core, halo;
    private static GpuTexture depthBeforeClouds;
    private static GpuTextureView depthView;
    private static GpuTexture capturedFrom;

    private RenderZenithTrail() {}

    @SubscribeEvent
    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(CORE_TYPE.pipeline());
        event.registerPipeline(HALO_TYPE.pipeline());
    }

    @SubscribeEvent
    public static void captureModelView(FrameGraphSetupEvent event) {
        MODEL_VIEW.set(RenderSystem.getModelViewMatrixCopy());
        capturedFrom = null;
    }

    static boolean isCollecting() {
        return RenderSystem.isRenderingLevel;
    }

    static VertexConsumer coreVertices() {
        if (geometry == null) {
            geometry = new StagedVertexBuffer(() -> "Zenith trail", 32768);
        }
        if (core == null) {
            core = geometry.appendDraw(CORE_TYPE.format(), CORE_TYPE.primitiveTopology(), VertexSorting.DISTANCE_TO_ORIGIN);
        }
        return geometry.getVertexBuilder(core);
    }

    static VertexConsumer haloVertices() {
        if (halo == null) {
            halo = geometry.appendDraw(HALO_TYPE.format(), HALO_TYPE.primitiveTopology());
        }
        return geometry.getVertexBuilder(halo);
    }

    public static boolean hasPendingTrails() {
        return core != null;
    }

    public static RenderPass resumeClassicPassWithDepthSnapshot() {
        RenderTarget main = mainTarget();
        GpuTexture depth = main.getDepthTexture();
        int width = depth.getWidth(0), height = depth.getHeight(0);
        if (depthBeforeClouds == null || depthBeforeClouds.getWidth(0) != width
            || depthBeforeClouds.getHeight(0) != height || depthBeforeClouds.getFormat() != depth.getFormat()) {
            releaseDepth();
            var device = RenderSystem.getDevice();
            depthBeforeClouds = device.createTexture(() -> "Zenith trail pre-cloud depth",
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_RENDER_ATTACHMENT,
                depth.getFormat(), width, height, 1, 1);
            depthView = device.createTextureView(depthBeforeClouds);
        }
        CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
        encoder.copyTextureToTexture(depth, depthBeforeClouds, 0, 0, 0, 0, 0, width, height);
        capturedFrom = depth;
        RenderPass continuation = encoder.createRenderPass(() -> "Main: clouds and weather",
            main.getColorTextureView(), Optional.empty(), main.getDepthTextureView(), OptionalDouble.empty());
        try {
            RenderSystem.bindDefaultUniforms(continuation);
            return continuation;
        } catch (RuntimeException | Error failure) {
            continuation.close();
            throw failure;
        }
    }

    @SubscribeEvent
    public static void drawAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        if (!hasPendingTrails()) {
            return;
        }

        RenderTarget main = mainTarget();
        GpuTextureView depth = capturedFrom != null && capturedFrom == main.getDepthTexture()
            ? depthView : main.getDepthTextureView();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            modelView.set(MODEL_VIEW);
            geometry.upload();
            var coreDraw = geometry.getExecuteInfo(core);
            var haloDraw = geometry.getExecuteInfo(halo);
            if (coreDraw == null && haloDraw == null) {
                return;
            }
            var preparedCore = CORE_TYPE.prepare();
            var preparedHalo = HALO_TYPE.prepare();
            try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                () -> "Zenith trails", main.getColorTextureView(), Optional.empty(), depth, OptionalDouble.empty())) {
                if (coreDraw != null) {
                    preparedCore.drawFromBuffer(coreDraw, pass);
                }
                if (haloDraw != null) {
                    preparedHalo.drawFromBuffer(haloDraw, pass);
                }
            }
        } finally {
            modelView.popMatrix();
            core = halo = null;
            capturedFrom = null;
            geometry.endFrame();
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        core = halo = null;
        capturedFrom = null;
        releaseDepth();
        if (geometry != null) {
            geometry.close();
            geometry = null;
        }
    }

    private static RenderTarget mainTarget() {
        return Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }

    private static void releaseDepth() {
        if (depthView != null) {
            depthView.close();
            depthView = null;
        }
        if (depthBeforeClouds != null) {
            depthBeforeClouds.close();
            depthBeforeClouds = null;
        }
    }

    private static RenderType createType(String name, BlendFunction blend, boolean sort) {
        RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID, "pipeline/" + name))
            .withColorTargetState(new ColorTargetState(blend))
            .build();
        var setup = RenderSetup.builder(pipeline);
        if (sort) {
            setup.sortOnUpload();
        }
        return RenderType.create(name, setup.createRenderSetup());
    }
}
