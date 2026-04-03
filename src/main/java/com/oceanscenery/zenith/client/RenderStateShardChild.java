package com.oceanscenery.zenith.client;

import net.minecraft.client.renderer.RenderStateShard;

public class RenderStateShardChild extends RenderStateShard {
    public RenderStateShardChild(String p_110161_, Runnable p_110162_, Runnable p_110163_) {
        super(p_110161_, p_110162_, p_110163_);
    }

    public static ShaderStateShard getShaderState(){
        return RenderStateShard.RENDERTYPE_ENTITY_TRANSLUCENT_EMISSIVE_SHADER;
    }

    public static TransparencyStateShard getTransparency(){
        return RenderStateShard.TRANSLUCENT_TRANSPARENCY;
    }

    public static CullStateShard getCull(){
        return RenderStateShard.NO_CULL;
    }

    public static LightmapStateShard getLightmap(){
        return RenderStateShard.NO_LIGHTMAP;
    }

    public static OverlayStateShard getOverlay(){
        return RenderStateShard.NO_OVERLAY;
    }

    public static WriteMaskStateShard getWriteMask(){
        return RenderStateShard.COLOR_WRITE;
    }
}
