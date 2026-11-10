package com.oceanscenery.zenith.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.client.gui.GhostConfigScreen;
import com.oceanscenery.zenith.mixin.accessor.KeyMappingAccessor;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.registry.ZenithKeyMappings;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import com.oceanscenery.zenith.server.packet.*;
import com.oceanscenery.zenith.util.AttachmentUtil;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import static com.oceanscenery.zenith.client.RenderEntityTip.currId;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID, value = Dist.CLIENT)
public class ClientSendPacket {
    @SubscribeEvent
    public static void onRightClickItem(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != InputConstants.MOUSE_BUTTON_RIGHT) {
            return;
        }
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen<?>)) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();

        Slot slot = ((AbstractContainerScreen<?>) screen).getHoveredSlot();
        if (slot == null || !(slot.container instanceof Inventory)) {
            return;
        }

        if (slot.hasItem() && slot.getItem().is(ZenithItems.ZENITH.get())) {
            event.setCanceled(true);
            int slot_index = slot.getSlotIndex();
            if (slot_index < 0 || slot_index >= slot.container.getContainerSize()) {
                return;
            }
            boolean ctrlDown = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL);
            if (ctrlDown) {
                ZenithNetworkHandler.sendToServer(new CycleAttackModePacket(slot_index));
            } else {
                ZenithNetworkHandler.sendToServer(new CycleZenithDefaultDistancePacket(slot_index));
            }
        }
    }

    @SubscribeEvent
    public static void holdAttack(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        KeyMapping right = Minecraft.getInstance().options.keyUse;
        if (((KeyMappingAccessor) right).getIsDown()) {
            if (player != null && (AttachmentUtil.getSlot(player).open()
                || player.getMainHandItem().is(ZenithItems.ZENITH) || player.getOffhandItem().is(ZenithItems.ZENITH))) {
                ZenithNetworkHandler.sendToServer(new ZenithAttackPacket(player.getId()));
            }
        }
    }

    @SubscribeEvent
    public static void toggleAttackBlacklist(ClientTickEvent.Pre event) {
        KeyMapping blacklist = ZenithKeyMappings.TOGGLE_ATTACK_BLACKLIST;
        LocalPlayer player = Minecraft.getInstance().player;
        Entity selected = Minecraft.getInstance().crosshairPickEntity;
        boolean keyDown = blacklist.consumeClick();
        if (selected == null || !keyDown) {
            return;
        }
        boolean shiftDown = Minecraft.getInstance().hasShiftDown();
        if (player != null) {
            ToggleBlacklistPacket packet = new ToggleBlacklistPacket(player.getId(), selected.getId(), shiftDown);
            ZenithNetworkHandler.sendToServer(packet);
        }
    }

    @SubscribeEvent
    public static void toggleSlotOpenState(ClientTickEvent.Pre event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (ZenithKeyMappings.TOGGLE_SLOT.consumeClick() && player != null) {
            ZenithNetworkHandler.sendToServer(
                new ToggleZenithSlotOpenStatePacket(
                    !AttachmentUtil.getSlot(player).open(),
                    player.getId()
                )
            );
        }
    }

    @SubscribeEvent
    public static void openGhostConfigScreen(ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (ZenithKeyMappings.OPEN_GHOST_CONFIG.consumeClick() && minecraft.player != null) {
            minecraft.setScreenAndShow(
                new GhostConfigScreen(AttachmentUtil.getSlot(minecraft.player))
            );
        }
    }

    @SubscribeEvent
    public static void checkSelectedEntity(ClientTickEvent.Pre event) {
        Entity selected = Minecraft.getInstance().crosshairPickEntity;
        Player player = Minecraft.getInstance().player;
        if (selected == null || player == null) {
            currId = -1;
            return;
        }

        int id = selected.getId();
        if (id != currId) {
            currId = id;

            ZenithUpdatePickedEntityQuestPacket packet = new ZenithUpdatePickedEntityQuestPacket(player.getId(), id);
            ZenithNetworkHandler.sendToServer(packet);
        }
    }
}
