package com.oceanscenery.zenith.zenith_class.saved_data;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mixin_extension.IZenithEntityExtension;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;

import java.util.*;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID)
public final class RemovedEntitiesData extends SavedData {
    private final Set<UUID> removed = new HashSet<>();
    private final Map<UUID, Long> blockedJoins = new HashMap<>();

    private static RemovedEntitiesData get(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
            RemovedEntitiesData::load, RemovedEntitiesData::new, "the_zenith_sword_removed_entities"
        );
    }

    public static RemovedEntitiesData load(CompoundTag tag) {
        RemovedEntitiesData data = new RemovedEntitiesData();
        long[] ids = tag.getLongArray("UUIDs");
        for (int i = 0; i + 1 < ids.length; i += 2) {
            data.removed.add(new UUID(ids[i], ids[i + 1]));
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        long[] ids = new long[removed.size() * 2];
        int i = 0;
        for (UUID id : removed) {
            ids[i++] = id.getMostSignificantBits();
            ids[i++] = id.getLeastSignificantBits();
        }
        tag.putLongArray("UUIDs", ids);
        return tag;
    }

    public static boolean record(ServerLevel level, UUID id) {
        RemovedEntitiesData data = get(level);
        if (!data.removed.add(id)) {
            return false;
        }
        data.setDirty();
        return true;
    }

    public static boolean contains(ServerLevel level, UUID id) {
        RemovedEntitiesData data = get(level);
        return data.removed.contains(id);
    }

    public static boolean contains(Level level, UUID id) {
        if (level instanceof ServerLevel serverLevel) {
            return contains(serverLevel, id);
        }
        return false;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof LivingEntity living)
            || living instanceof Player) {
            return;
        }
        if (!IZenithEntityExtension.of(living).zenith$isAllReturn()) {
            return;
        }
        event.setCanceled(true);
    }
}
