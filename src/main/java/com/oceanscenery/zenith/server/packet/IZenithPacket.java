package com.oceanscenery.zenith.server.packet;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public interface IZenithPacket {
    void write(FriendlyByteBuf buf);
    void handle(Supplier<NetworkEvent.Context> context);
}