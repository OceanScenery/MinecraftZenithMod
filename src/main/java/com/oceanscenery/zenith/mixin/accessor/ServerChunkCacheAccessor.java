package com.oceanscenery.zenith.mixin.accessor;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerChunkCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ServerChunkCache.class)
public interface ServerChunkCacheAccessor {
    @Accessor(value = "chunkMap")
    ChunkMap zenith$getChunkMap();
}
