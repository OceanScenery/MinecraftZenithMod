package com.oceanscenery.zenith.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexSorting;
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
import java.util.function.Supplier;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID, value = Dist.CLIENT)
public final class RenderZenithTrail {
    private static final Matrix4f MODEL_VIEW = new Matrix4f();

    private static boolean useEntityRendering;
    private static StagedVertexBuffer geometry;
    private static StagedVertexBuffer.Draw core, halo;
    private static GpuTexture depthBeforeClouds;
    private static GpuTextureView depthView;
    private static GpuTexture capturedFrom;

    private RenderZenithTrail() {}

    @SubscribeEvent
    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        runStaged("Zenith trail pipeline registration failed.", () -> {
            event.registerPipeline(StagedTypes.CORE.pipeline());
            event.registerPipeline(StagedTypes.HALO.pipeline());
        });
    }

    @SubscribeEvent
    public static void captureModelView(FrameGraphSetupEvent event) {
        runStaged("Zenith staged trail frame setup failed.", () -> {
            MODEL_VIEW.set(RenderSystem.getModelViewMatrixCopy());
            capturedFrom = null;
        });
    }

    static boolean isCollecting() {
        return !useEntityRendering && RenderSystem.isRenderingLevel;
    }

    static boolean usesEntityRendering() {
        return useEntityRendering;
    }

    static VertexConsumer coreVertices() {
        if (geometry == null) {
            geometry = new StagedVertexBuffer(() -> "Zenith trail", 32768);
        }
        if (core == null) {
            core = geometry.appendDraw(StagedTypes.CORE.format(), StagedTypes.CORE.primitiveTopology(), VertexSorting.DISTANCE_TO_ORIGIN);
        }
        return geometry.getVertexBuilder(core);
    }

    static VertexConsumer haloVertices() {
        if (halo == null) {
            halo = geometry.appendDraw(StagedTypes.HALO.format(), StagedTypes.HALO.primitiveTopology());
        }
        return geometry.getVertexBuilder(halo);
    }

    public static boolean hasPendingTrails() {
        return !useEntityRendering && core != null;
    }

    public static RenderPass resumeClassicPassWithDepthSnapshot(RenderPass original, Supplier<RenderTarget> target) {
        if (!hasPendingTrails()) {
            return original;
        }
        GpuTexture depth;
        GpuTextureView colorAttachment;
        GpuTextureView depthAttachment;
        try {
            RenderTarget main = target.get();
            depth = main.getDepthTexture();
            colorAttachment = main.getColorTextureView();
            depthAttachment = main.getDepthTextureView();
        } catch (RuntimeException | LinkageError failure) {
            switchToEntityRendering("Zenith could not read the classic transparency targets.", failure);
            return original;
        }
        if (depth == null || colorAttachment == null || depthAttachment == null) {
            return original;
        }

        // The encoder cannot copy a texture while the main render pass is open.
        try {
            original.close();
            captureDepth(depth);
        } catch (RuntimeException | LinkageError failure) {
            switchToEntityRendering("Zenith pre-cloud depth capture failed.", failure);
        }
        try {
            return openClassicPass(colorAttachment, depthAttachment);
        } catch (RuntimeException | LinkageError failure) {
            switchToEntityRendering("Zenith could not resume the classic transparency pass.", failure);
            try {
                return openClassicPass(colorAttachment, depthAttachment);
            } catch (RuntimeException | LinkageError retryFailure) {
                if (retryFailure != failure) {
                    failure.addSuppressed(retryFailure);
                }
                throw failure;
            }
        }
    }

    private static void captureDepth(GpuTexture depth) {
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
        RenderSystem.getDevice().createCommandEncoder()
            .copyTextureToTexture(depth, depthBeforeClouds, 0, 0, 0, 0, 0, width, height);
        capturedFrom = depth;
    }

    private static RenderPass openClassicPass(GpuTextureView color, GpuTextureView depth) {
        RenderPass continuation = RenderSystem.getDevice().createCommandEncoder()
            .createRenderPass(() -> "Main: clouds and weather", color, Optional.empty(), depth, OptionalDouble.empty());
        try {
            RenderSystem.bindDefaultUniforms(continuation);
            return continuation;
        } catch (RuntimeException | Error failure) {
            try {
                continuation.close();
            } catch (RuntimeException | LinkageError cleanupFailure) {
                if (!cleanupFailure.equals(failure)) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw failure;
        }
    }

    @SubscribeEvent
    public static void drawAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        if (!hasPendingTrails()) {
            return;
        }
        runStaged("Zenith staged trail draw failed.", RenderZenithTrail::drawFrame);
    }

    private static void drawFrame() {
        RenderTarget main = mainTarget();
        GpuTextureView depth = capturedFrom != null && capturedFrom == main.getDepthTexture()
            ? depthView : main.getDepthTextureView();
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            modelView.set(MODEL_VIEW);
            geometry.upload();
            var coreDraw = geometry.getExecuteInfo(core);
            var haloDraw = halo == null ? null : geometry.getExecuteInfo(halo);
            if (coreDraw != null || haloDraw != null) {
                drawBatches(main, depth, coreDraw, haloDraw);
            }
        } finally {
            modelView.popMatrix();
        }
        geometry.endFrame();
        core = halo = null;
        capturedFrom = null;
    }

    private static void drawBatches(RenderTarget main, GpuTextureView depth,
                                    StagedVertexBuffer.ExecuteInfo coreDraw, StagedVertexBuffer.ExecuteInfo haloDraw) {
        var preparedCore = coreDraw == null ? null : StagedTypes.CORE.prepare();
        var preparedHalo = haloDraw == null ? null : StagedTypes.HALO.prepare();
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(
            () -> "Zenith trails", main.getColorTextureView(), Optional.empty(), depth, OptionalDouble.empty())) {
            if (preparedCore != null) {
                preparedCore.drawFromBuffer(coreDraw, pass);
            }
            if (preparedHalo != null) {
                preparedHalo.drawFromBuffer(haloDraw, pass);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        runStaged("Zenith staged trail resource release failed.", RenderZenithTrail::releaseResources);
    }

    private static void runStaged(String message, Runnable action) {
        if (useEntityRendering) {
            return;
        }
        try {
            action.run();
        } catch (RuntimeException | LinkageError failure) {
            switchToEntityRendering(message, failure);
        }
    }

    public static void switchToEntityRendering(String message, Throwable failure) {
        if (useEntityRendering) {
            TheZenithMod.LOGGER.error(message, failure);
            return;
        }
        useEntityRendering = true;
        try {
            releaseResources();
        } catch (RuntimeException | LinkageError cleanupFailure) {
            if (cleanupFailure != failure) {
                failure.addSuppressed(cleanupFailure);
            }
        }
        TheZenithMod.LOGGER.error(
            "{} Using normal entity rendering for Zenith trails for this client session.", message, failure);
    }

    private static void releaseResources() {
        core = halo = null;
        StagedVertexBuffer oldGeometry = geometry;
        geometry = null;
        try (oldGeometry) {
            releaseDepth();
        }
    }

    private static RenderTarget mainTarget() {
        return Minecraft.getInstance().gameRenderer.mainRenderTarget();
    }

    private static void releaseDepth() {
        GpuTexture oldDepth = depthBeforeClouds;
        GpuTextureView oldView = depthView;
        depthBeforeClouds = null;
        depthView = null;
        capturedFrom = null;
        try (oldDepth; oldView) {
        }
    }

    private static final class StagedTypes {
        private static final RenderType CORE = createType("prismatic_trail_core", BlendFunction.TRANSLUCENT, true);
        private static final RenderType HALO = createType("prismatic_trail_halo", BlendFunction.LIGHTNING, false);
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
