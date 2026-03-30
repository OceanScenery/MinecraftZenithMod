package com.oceanscenery.zenith.server;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.data_component.Distance;
import com.oceanscenery.zenith.registry.ZenithDataComponents;
import com.oceanscenery.zenith.registry.ZenithItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CycleZenithDefaultDistancePacket(int slot) implements CustomPacketPayload {
    public static final Type<CycleZenithDefaultDistancePacket> TYPE=new Type<>(Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith_cycle_range"));
    public static final Identifier ID=Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith_cycle_range");

    public static final StreamCodec<ByteBuf,CycleZenithDefaultDistancePacket> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.INT,CycleZenithDefaultDistancePacket::slot,
            CycleZenithDefaultDistancePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public CycleZenithDefaultDistancePacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void write(FriendlyByteBuf buf){
        buf.writeInt(slot);
    }

    public Identifier getId(){
        return ID;
    }

    public static void handle(final CycleZenithDefaultDistancePacket packet, final IPayloadContext context){
        context.enqueueWork(
                ()->{
                    if(context.player() instanceof ServerPlayer player){
                        if(packet.slot<0 || packet.slot>=player.getInventory().getContainerSize()){
                            return;
                        }

                        ItemStack item=player.getInventory().getItem(packet.slot);
                        if(item.is(ZenithItems.ZENITH.get())){
                            if(item.get(ZenithDataComponents.DISTANCE)==null){
                                item.set(ZenithDataComponents.DISTANCE.get(),new Distance(20));
                            }else{
                                double preDist=item.get(ZenithDataComponents.DISTANCE.get()).dist();
                                preDist+=20;
                                if(preDist>100){
                                    preDist=20;
                                }
                                item.set(ZenithDataComponents.DISTANCE.get(),new Distance(preDist));
                            }

                            player.sendSystemMessage(
                                    Component.translatable("the_zenith_sword.packet.distance").append(":"+String.valueOf(item.get(ZenithDataComponents.DISTANCE).dist())),
                                    true
                            );
                        }
                    }
                }
        );
    }
}
