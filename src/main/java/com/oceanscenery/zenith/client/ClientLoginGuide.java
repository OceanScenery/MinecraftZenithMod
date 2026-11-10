package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.TheZenithMod;
import com.oceanscenery.zenith.registry.ZenithKeyMappings;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = TheZenithMod.MOD_ID, value = Dist.CLIENT)
public final class ClientLoginGuide {
    private static final String[] LINES = {
        "message.the_zenith_sword.login.header",
        "message.the_zenith_sword.login.key.toggle_slot",
        "message.the_zenith_sword.login.key.ghost_config",
        "message.the_zenith_sword.login.key.blacklist",
        "message.the_zenith_sword.login.command.entity",
        "message.the_zenith_sword.login.command.type",
        "message.the_zenith_sword.login.command.addfrom"
    };

    private static boolean pending = false;

    private ClientLoginGuide() {
    }

    @SubscribeEvent
    public static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        pending = true;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!pending) {
            return;
        }
        pending = false;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui == null) {
            return;
        }
        for (int i = 0; i < LINES.length; i++) {
            String line = LINES[i];
            switch (i) {
                case 1 -> minecraft.gui.getChat()
                    .addMessage(Component.translatable(line, ZenithKeyMappings.TOGGLE_SLOT.getTranslatedKeyMessage()));
                case 2 -> minecraft.gui.getChat()
                    .addMessage(Component.translatable(line, ZenithKeyMappings.OPEN_GHOST_CONFIG.getTranslatedKeyMessage()));
                case 3 -> minecraft.gui.getChat()
                    .addMessage(Component.translatable(line, ZenithKeyMappings.TOGGLE_ATTACK_BLACKLIST.getTranslatedKeyMessage()));
                default -> minecraft.gui.getChat()
                    .addMessage(Component.translatable(line));
            }
        }
    }
}
