package com.oceanscenery.zenith.server;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.item.ZenithItem;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record ZenithAttackPacket(int id) implements CustomPacketPayload{
    public static final Identifier ID=Identifier.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith_attack");
    public static final CustomPacketPayload.Type<ZenithAttackPacket> TYPE=new CustomPacketPayload.Type<>(ID);

    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static final StreamCodec<ByteBuf,ZenithAttackPacket> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.INT, ZenithAttackPacket::id,
            ZenithAttackPacket::new
    );

    public static void handle(final ZenithAttackPacket packet, final IPayloadContext context){
        context.enqueueWork(
                ()->{
                    if(context.player() instanceof ServerPlayer player && player.level() instanceof ServerLevel){
                        if(player.getId()!=packet.id){
                            return;
                        }
                        ItemStack m_item=player.getMainHandItem();
                        ItemStack o_item=player.getOffhandItem();
                        if(m_item.getItem() instanceof ZenithItem zenithItem){
                            zenithItem.attack(m_item,player,player.level(),InteractionHand.MAIN_HAND);
                            return;
                        }
                        if(o_item.getItem() instanceof ZenithItem zenithItem){
                            zenithItem.attack(o_item,player,player.level(),InteractionHand.OFF_HAND);
                            return;
                        }
                    }
                }
        );
    }
}
