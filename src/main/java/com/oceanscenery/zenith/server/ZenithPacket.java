package com.oceanscenery.zenith.server;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.event.WeaponHandler;
import com.oceanscenery.zenith.registry.ZenithItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record ZenithPacket(int id,int operation,int slot){
    public ZenithPacket(FriendlyByteBuf buf){
        this(buf.readInt(),buf.readInt(),buf.readInt());
    }

    public void write(FriendlyByteBuf buf){
        buf.writeInt(id);
        buf.writeInt(operation);
        buf.writeInt(slot);
    }

    public void handle(Supplier<NetworkEvent.Context> context){
        context.get().enqueueWork(
                ()->{
                    ServerPlayer player = context.get().getSender();
                    if(player.getId()!=this.id()){
                        context.get().setPacketHandled(true);
                        return;
                    }
                    if(operation==0){
                        if(player.getMainHandItem().is(ZenithItems.ZENITH.get())){
                            ZenithItems.ZENITH.get().attack(player.getMainHandItem(),player,player.level(),InteractionHand.MAIN_HAND);
                            context.get().setPacketHandled(true);
                            return;
                        }
                        if(player.getOffhandItem().is(ZenithItems.ZENITH.get())){
                            ZenithItems.ZENITH.get().attack(player.getOffhandItem(),player,player.level(),InteractionHand.OFF_HAND);
                            context.get().setPacketHandled(true);
                            return;
                        }
                    }
                    if(operation==1){
                        ItemStack stack=player.getInventory().getItem(slot);
                        if(stack.is(ZenithItems.ZENITH.get())){
                            double dist=WeaponHandler.getDistance(stack);
                            if (stack.getTag() != null) {
                                stack.getTag().getCompound("Zenith").putDouble("distance",(int)dist%100+20);
                            }
                        }
                        context.get().setPacketHandled(true);
                        return;
                    }
                    if(operation==2){
                        ItemStack stack=player.getInventory().getItem(slot);
                        if(stack.is(ZenithItems.ZENITH.get())){
                            int mode=WeaponHandler.getAttackMode(stack);
                            if (stack.getTag() != null) {
                                stack.getTag().getCompound("Zenith").putInt("attack_mode",(mode+1)%3);
                            }
                        }
                        context.get().setPacketHandled(true);
                        return;
                    }
                    if(operation==3){
                        ItemStack stack=player.getInventory().getItem(slot);
                        if(stack.is(ZenithItems.ZENITH.get())){
                            boolean flag=WeaponHandler.attackPlayer(stack);
                            if (stack.getTag() != null) {
                                stack.getTag().getCompound("Zenith").putBoolean("attack_player",!flag);
                            }
                        }
                        context.get().setPacketHandled(true);
                        return;
                    }
                }
        );
        context.get().setPacketHandled(true);
    }
}
