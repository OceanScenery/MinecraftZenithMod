package com.oceanscenery.zenith.server;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.registry.ZenithDataComponents;
import com.oceanscenery.zenith.registry.ZenithItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CycleAttackModePacket(int slot) implements CustomPacketPayload {
    public static final Type<CycleAttackModePacket> TYPE=new Type<>(ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith_cycle_attack_mode"));
    public static final ResourceLocation ID=ResourceLocation.fromNamespaceAndPath(TheZenithMod.MOD_ID,"zenith_cycle_attack_mode");

    public static final StreamCodec<ByteBuf,CycleAttackModePacket> STREAM_CODEC=StreamCodec.composite(
            ByteBufCodecs.INT,CycleAttackModePacket::slot,
            CycleAttackModePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public CycleAttackModePacket(FriendlyByteBuf buf) {
        this(buf.readInt());
    }

    public void write(FriendlyByteBuf buf){
        buf.writeInt(slot);
    }

    public ResourceLocation getId(){
        return ID;
    }

    public static void handle(final CycleAttackModePacket packet, final IPayloadContext context){
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
                                int id=item.get(ZenithDataComponents.ATTACK_MODE.get()).getMode().ordinal();
                                id=(id+1)%3;
                                item.set(ZenithDataComponents.ATTACK_MODE.get(),new AttackMode(AttackMode.Mode.values()[id],item.get(ZenithDataComponents.ATTACK_MODE).attackPlayer()));
                            }

                            player.displayClientMessage(
                                    Component.translatable("the_zenith_sword.packet.attack_mode").append(":"+item.get(ZenithDataComponents.ATTACK_MODE).getStrMode()),
                                    true
                            );
                        }
                    }
                }
        );
    }
}
