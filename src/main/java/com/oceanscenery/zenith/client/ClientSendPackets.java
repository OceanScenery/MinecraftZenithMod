package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.mixin.KeyMappingAccessor;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.registry.ZenithKeyMappings;
import com.oceanscenery.zenith.server.*;
import com.oceanscenery.zenith.server.packet.ToggleZenithBlacklistPacket;
import com.oceanscenery.zenith.server.packet.ZenithPacket;
import com.oceanscenery.zenith.server.packet.ZenithUpdatePickedEntityQuestPacket;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

import static com.oceanscenery.zenith.client.RenderEntityTip.currId;

@Mod.EventBusSubscriber(modid = TheZenithMod.MOD_ID,value = Dist.CLIENT)
public class ClientSendPackets {
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

        Player player=mc.player;
        if(player==null){
            return;
        }

        if(slot.hasItem() && slot.getItem().is(ZenithItems.ZENITH.get())){
            event.setCanceled(true);
            int slot_index=slot.getSlotIndex();
            if(slot_index<0 || slot_index>=slot.container.getContainerSize()){
                return;
            }
            long window= mc.getWindow().getWindow();
            boolean ctrlDown=GLFW.glfwGetKey(window,GLFW.GLFW_KEY_LEFT_CONTROL)==GLFW.GLFW_PRESS;
            boolean shiftDown=GLFW.glfwGetKey(window,GLFW.GLFW_KEY_LEFT_SHIFT)==GLFW.GLFW_PRESS;
            if(ctrlDown){
                ZenithNetworkHandler.playToServer(new ZenithPacket(player.getId(),2,slot_index));
            }else if(shiftDown){
                ZenithNetworkHandler.playToServer(new ZenithPacket(player.getId(),3,slot_index));
            }else{
                ZenithNetworkHandler.playToServer(new ZenithPacket(player.getId(),1,slot_index));
            }
        }
    }
    @SubscribeEvent
    public static void holdAttack(TickEvent.ClientTickEvent event){
        if(event.phase == TickEvent.Phase.END){
            return;
        }

        LocalPlayer player=Minecraft.getInstance().player;
        KeyMapping right=Minecraft.getInstance().options.keyUse;
        if(((KeyMappingAccessor)right).getIsDown()){
            if(player!=null && (player.getMainHandItem().is(ZenithItems.ZENITH.get()) || player.getOffhandItem().is(ZenithItems.ZENITH.get()))){
                ZenithNetworkHandler.playToServer(new ZenithPacket(player.getId(),0,0));
            }
        }
    }

    @SubscribeEvent
    public static void toggleAttackBlacklist(TickEvent.ClientTickEvent event){
        if(event.phase == TickEvent.Phase.END){
            return;
        }

        KeyMapping blacklist=ZenithKeyMappings.TOGGLE_BLACKLIST;
        LocalPlayer player=Minecraft.getInstance().player;
        Entity selected=Minecraft.getInstance().crosshairPickEntity;
        boolean keyDown=blacklist.consumeClick();
        if(selected==null || !keyDown){
            return;
        }
        boolean shiftDown=Screen.hasShiftDown();
        if (player != null) {
            ToggleZenithBlacklistPacket packet=new ToggleZenithBlacklistPacket(player.getId(),selected.getId(),shiftDown);
            ZenithNetworkHandler.playToServer(packet);
        }
    }

    @SubscribeEvent
    public static void checkSelectedEntity(TickEvent.ClientTickEvent event){
        if(event.phase == TickEvent.Phase.END){
            return;
        }

        Entity selected=Minecraft.getInstance().crosshairPickEntity;
        Player player=Minecraft.getInstance().player;;
        if(selected==null || player==null){
            currId =-1;
            return;
        }

        int id=selected.getId();
        if(id!=currId){
            currId = id;

            ZenithUpdatePickedEntityQuestPacket packet = new ZenithUpdatePickedEntityQuestPacket(player.getId(), id);
            ZenithNetworkHandler.playToServer(packet);
        }
    }
}
