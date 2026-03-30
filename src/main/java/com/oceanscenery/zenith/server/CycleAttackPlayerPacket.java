package com.oceanscenery.zenith.server;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.registry.ZenithDataComponents;
import com.oceanscenery.zenith.registry.ZenithItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CycleAttackPlayerPacket(int slot) implements CustomPacketPayload {
    public static final Identifier ID=Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID,"attack_player_mode");
    public static final Type<CycleAttackPlayerPacket> TYPE=new Type<>(ID);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<ByteBuf,CycleAttackPlayerPacket> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.INT, CycleAttackPlayerPacket::slot,
            CycleAttackPlayerPacket::new
    );

    public static void handle(final CycleAttackPlayerPacket packet, final IPayloadContext context){
        context.enqueueWork(
                ()->{
                    if(context.player() instanceof ServerPlayer player){
                        if(packet.slot<0 || packet.slot>=player.getInventory().getContainerSize()){
                            return;
                        }

                        ItemStack item=player.getInventory().getItem(packet.slot);
                        if(item.is(ZenithItems.ZENITH.get())){
                            if(item.get(ZenithDataComponents.ATTACK_MODE)==null){
                                item.set(ZenithDataComponents.ATTACK_MODE.get(),new AttackMode(AttackMode.Mode.LIVING_ENTITY,true));
                            }else{
                                boolean flag=item.get(ZenithDataComponents.ATTACK_MODE).attackPlayer();
                                item.set(ZenithDataComponents.ATTACK_MODE,new AttackMode(item.get(ZenithDataComponents.ATTACK_MODE).getStrMode(),!flag));
                            }

                            player.sendSystemMessage(
                                    Component.translatable("the_zenith_sword.packet.attack_player_mode").append(":"+item.get(ZenithDataComponents.ATTACK_MODE).attackPlayer()),
                                    true
                            );
                        }
                    }
                }
        );
    }
}
