package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mixin.KeyMappingAccessor;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.server.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID,value = Dist.CLIENT)
public class ClientSendPacket {
    @SubscribeEvent
    public static void onRightClickItem(ScreenEvent.MouseButtonPressed.Pre event){
        if(event.getButton()!=GLFW.GLFW_MOUSE_BUTTON_RIGHT){
            return;
        }
        Screen screen= event.getScreen();
        if(!(screen instanceof AbstractContainerScreen<?>)){
            return;
        }
        Minecraft mc=Minecraft.getInstance();

        Slot slot=((AbstractContainerScreen<?>) screen).getSlotUnderMouse();
        if (slot==null || !(slot.container instanceof Inventory)) {
            return;
        }

        if(slot.hasItem() && slot.getItem().is(ZenithItems.ZENITH.get())){
            event.setCanceled(true);
            int slot_index=slot.getSlotIndex();
            if(slot_index<0 || slot_index>=slot.container.getContainerSize()){
                return;
            }
            long window=mc.getWindow().getWindow();
            boolean ctrlDown=GLFW.glfwGetKey(window,GLFW.GLFW_KEY_LEFT_CONTROL)==GLFW.GLFW_PRESS;
            boolean shiftDown=GLFW.glfwGetKey(window,GLFW.GLFW_KEY_LEFT_SHIFT)==GLFW.GLFW_PRESS;
            if(ctrlDown){
                ZenithNetworkHandler.sendToServer(new CycleAttackModePacket(slot_index));
            }else if(shiftDown){
                ZenithNetworkHandler.sendToServer(new CycleAttackPlayerPacket(slot_index));
            }else{
                ZenithNetworkHandler.sendToServer(new CycleZenithDefaultDistancePacket(slot_index));
            }
        }
    }
    @SubscribeEvent
    public static void holdAttack(ClientTickEvent.Pre event){
        LocalPlayer player=Minecraft.getInstance().player;
        KeyMapping right=Minecraft.getInstance().options.keyUse;
        if(((KeyMappingAccessor)right).getIsDown()){
            if(player!=null && (player.getMainHandItem().is(ZenithItems.ZENITH) || player.getOffhandItem().is(ZenithItems.ZENITH))){
                ZenithNetworkHandler.sendToServer(new ZenithAttackPacket(player.getId()));
            }
        }
    }
}
