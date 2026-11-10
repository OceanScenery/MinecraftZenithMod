package com.oceanscenery.zenith.mixin.accessor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.entity.EntityTickList;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;
import java.util.Set;

@Mixin(ServerLevel.class)
public interface ServerLevelAccessor {
    @Accessor("entityManager")
    PersistentEntitySectionManager<Entity> zenith$getEntityManager();

    @Accessor("entityTickList")
    EntityTickList zenith$getEntityTickList();

    @Accessor("players")
    List<ServerPlayer> zenith$getPlayers();

    @Accessor("navigatingMobs")
    Set<Mob> zenith$getNavigatingMobs();

    @Accessor("dragonParts")
    Int2ObjectMap<PartEntity<?>> zenith$getDragonParts();
}
