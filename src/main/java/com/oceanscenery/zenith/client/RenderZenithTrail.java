package com.oceanscenery.zenith.client;

import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.*;
import com.oceanscenery.zenith.TheZenithMod;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.FrameGraphSetupEvent;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID, value = Dist.CLIENT)
public final class RenderZenithTrail {
    private static final int BUFFER_SIZE = 32768;
    private static final RenderType CORE_TYPE = createType("prismatic_trail_core", BlendFunction.TRANSLUCENT, true);
    private static final RenderType HALO_TYPE = createType("prismatic_trail_halo", BlendFunction.LIGHTNING, false);
    private static final Matrix4f MODEL_VIEW = new Matrix4f();

    private static ByteBufferBuilder coreStorage, haloStorage;
    private static BufferBuilder core, halo;
    private static GpuTexture depthBeforeClouds;
    private static GpuTextureView depthView;
    private static GpuTexture capturedFrom;
    private static boolean collecting;
    private static boolean cloudsShareMainDepth;

    private RenderZenithTrail() {
    }

    @SubscribeEvent
    public static void registerPipelines(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(CORE_TYPE.pipeline());
        event.registerPipeline(HALO_TYPE.pipeline());
    }

    @SubscribeEvent
    public static void beginFrame(FrameGraphSetupEvent event) {
        discardFrame();
        MODEL_VIEW.set(RenderSystem.getModelViewMatrix());
        collecting = true;
        cloudsShareMainDepth = event.getTargetBundle().clouds == null;
    }

    static boolean isCollecting() {
        return collecting;
    }

    static VertexConsumer coreVertices() {
        if (core == null) {
            if (coreStorage == null) {
                coreStorage = new ByteBufferBuilder(BUFFER_SIZE);
            }
            core = new BufferBuilder(coreStorage, CORE_TYPE.mode(), CORE_TYPE.format());
        }
        return core;
    }

    static VertexConsumer haloVertices() {
        if (halo == null) {
            if (haloStorage == null) {
                haloStorage = new ByteBufferBuilder(BUFFER_SIZE);
            }
            halo = new BufferBuilder(haloStorage, HALO_TYPE.mode(), HALO_TYPE.format());
        }
        return halo;
    }

    @SubscribeEvent
    public static void captureDepthBeforeClouds(RenderLevelStageEvent.AfterTranslucentParticles event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!collecting || !cloudsShareMainDepth || core == null
            || minecraft.gameRenderer.getGameRenderState().optionsRenderState.cloudStatus == CloudStatus.OFF
            || ARGB.alpha(event.getLevelRenderState().cloudColor) == 0) {
            return;
        }

        GpuTexture depth = minecraft.getMainRenderTarget().getDepthTexture();
        if (depth == null) {
            return;
        }

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

    @SubscribeEvent
    public static void drawAfterLevel(RenderLevelStageEvent.AfterLevel event) {
        collecting = false;
        if (core == null && halo == null) {
            return;
        }

        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        GpuTextureView oldColor = RenderSystem.outputColorTextureOverride;
        GpuTextureView oldDepth = RenderSystem.outputDepthTextureOverride;
        Matrix4fStack modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        try {
            modelView.set(MODEL_VIEW);
            RenderSystem.outputColorTextureOverride = main.getColorTextureView();
            RenderSystem.outputDepthTextureOverride = capturedFrom != null && capturedFrom == main.getDepthTexture()
                ? depthView : main.getDepthTextureView();
            drawBatch(CORE_TYPE, coreStorage, core);
            drawBatch(HALO_TYPE, haloStorage, halo);
        } finally {
            RenderSystem.outputDepthTextureOverride = oldDepth;
            RenderSystem.outputColorTextureOverride = oldColor;
            modelView.popMatrix();
            discardFrame();
        }
    }

    private static void drawBatch(RenderType type, ByteBufferBuilder storage, BufferBuilder builder) {
        if (builder == null) {
            return;
        }
        try (MeshData mesh = builder.build()) {
            if (mesh != null) {
                if (type.sortOnUpload()) {
                    mesh.sortQuads(storage, RenderSystem.getProjectionType().vertexSorting());
                }
                type.draw(mesh);
            }
        }
    }

    private static void discardFrame() {
        if (core != null) {
            discardVertices(coreStorage);
        }
        if (halo != null) {
            discardVertices(haloStorage);
        }
        core = halo = null;
        capturedFrom = null;
        collecting = false;
    }

    private static void discardVertices(ByteBufferBuilder storage) {
        try (ByteBufferBuilder.Result ignored = storage.build()) {
        } catch (Exception ignored) {
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        discardFrame();
        releaseDepth();
        if (coreStorage != null) {
            coreStorage.close();
            coreStorage = null;
        }
        if (haloStorage != null) {
            haloStorage.close();
            haloStorage = null;
        }
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
        RenderPipeline pipeline = RenderPipeline.builder(RenderPipelines.MATRICES_PROJECTION_SNIPPET)
            .withLocation(Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID, "pipeline/" + name))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withCull(false)
            .withDepthStencilState(new DepthStencilState(CompareOp.LESS_THAN_OR_EQUAL, false))
            .withColorTargetState(new ColorTargetState(blend))
            .build();
        var setup = RenderSetup.builder(pipeline).bufferSize(BUFFER_SIZE);
        if (sort) {
            setup.sortOnUpload();
        }
        return RenderType.create(name, setup.createRenderSetup());
    }
}
