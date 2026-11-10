package com.oceanscenery.zenith.zenith_class.saved_data;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.event.CapabilitiesHandler;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ZenithSlotData extends SavedData {
    public static final String NAME = "zenith_slots";
    public static final String NBT_KEY = "ZenithSlot";

    private final Set<UUID> valid = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ZenithSlotData() {
    }

    public Set<UUID> validPlayers() {
        return Collections.unmodifiableSet(this.valid);
    }

    public static boolean playerValid(Player player) {
        return fromCurrentServer().filter(
            d -> d.validPlayers().contains(player.getUUID())
        ).isPresent();
    }

    public void remove(Player player) {
        this.valid.remove(player.getUUID());
        this.setDirty();
    }

    public void add(Player player) {
        this.valid.add(player.getUUID());
        this.setDirty();
    }

    public void setValid(Player player, boolean valid) {
        if (valid) {
            this.add(player);
        } else {
            this.remove(player);
        }
    }

    public void mergeExist(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        boolean[] changed = {false};
        if (this.valid.contains(player.getUUID())) {
            Optional.ofNullable(CapabilitiesHandler.getSlot(player)).ifPresent(
                origin -> {
                    if (!origin.valid()) {
                        origin.setValid(true);
                        changed[0] = true;
                    }
                }
            );
        } else {
            Optional.ofNullable(CapabilitiesHandler.getSlot(player))
                .filter(IZenithSlot::valid).ifPresent(
                    s -> this.add(player)
                );
        }
        if (changed[0]) {
            ZenithNetworkHandler.syncZenithSlot(serverPlayer);
        }
    }

    public static ZenithSlotData fromServer(MinecraftServer server) {
        return Optional.ofNullable(server)
            .map(s -> s.overworld().getDataStorage().computeIfAbsent(ZenithSlotData::load, ZenithSlotData::new, NAME))
            .orElse(null);
    }

    public static Optional<ZenithSlotData> fromCurrentServer() {
        return Optional.ofNullable(ServerLifecycleHooks.getCurrentServer())
            .map(s -> s.overworld().getDataStorage().computeIfAbsent(ZenithSlotData::load, ZenithSlotData::new, NAME));
    }

    public static ZenithSlotData load(CompoundTag tag) {
        ZenithSlotData data = new ZenithSlotData();
        Tag mustList = tag.get(NBT_KEY);
        if (!(mustList instanceof ListTag listTag)) {
            return data;
        }
        for (var entry : listTag) {
            if (entry instanceof StringTag stringTag) {
                try {
                    data.valid.add(UUID.fromString(stringTag.getAsString()));
                } catch (IllegalArgumentException e) {
                    TheZenithMod.LOGGER.warn("Found invalid uuid {} in storage.", stringTag.getAsString(), e);
                }
            }
        }
        return data;
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag uuidList = new ListTag();
        for (UUID uuid : this.valid) {
            uuidList.add(StringTag.valueOf(uuid.toString()));
        }
        tag.put(NBT_KEY, uuidList);
        return tag;
    }
}
