package com.oceanscenery.zenith.util;

import com.oceanscenery.zenith.mixin.ClientLevelAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class LevelUtil {
    public static Entity getEntity(Level level, UUID uuid) {
        if (level.isClientSide()) {
            return ((ClientLevelAccessor) level).getEntityStorage().getEntityGetter().get(uuid);
        } else {
            return ((ServerLevel) level).getEntity(uuid);
        }
    }
}
