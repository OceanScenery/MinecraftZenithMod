package com.oceanscenery.zenith.client.gui;

import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import com.oceanscenery.zenith.server.packet.SetZenithGhostConfigPacket;
import com.oceanscenery.zenith.zenith_class.capabilities.cap_interface.IZenithSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

@OnlyIn(Dist.CLIENT)
public class GhostConfigScreen extends Screen {
    private static final int TEXT_COLOR = 0xFFFFFF;
    private static final int HINT_COLOR = 0xA0A0A0;

    private final IZenithSlot slot;
    private Button distanceButton;
    private Button modeButton;

    public GhostConfigScreen(IZenithSlot slot) {
        super(Component.translatable("screen.the_zenith_sword.ghost_config.title"));
        this.slot = slot;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        this.distanceButton = this.addRenderableWidget(
            Button.builder(this.distanceLabel(), button -> this.cycleDistance())
                .bounds(centerX - 100, centerY - 10, 200, 20)
                .build()
        );
        this.modeButton = this.addRenderableWidget(
            Button.builder(this.modeLabel(), button -> this.cycleMode())
                .bounds(centerX - 100, centerY + 20, 200, 20)
                .build()
        );
    }

    private Component distanceLabel() {
        return Component.translatable(
            "screen.the_zenith_sword.ghost_config.distance",
            (int) this.slot.getDistance()
        );
    }

    private Component modeLabel() {
        return Component.translatable(
            "screen.the_zenith_sword.ghost_config.mode",
            Component.translatable("screen.the_zenith_sword.ghost_config.mode." + this.slot.getMode())
        );
    }

    private void cycleDistance() {
        this.apply((int) this.slot.getDistance() % 100 + 20, this.slot.getMode());
    }

    private void cycleMode() {
        this.apply(this.slot.getDistance(), (this.slot.getMode() + 1) % 3);
    }

    private void apply(double distance, int mode) {
        this.slot.setDistance(distance);
        this.slot.setMode(mode);

        if (Minecraft.getInstance().player != null) {
            ZenithNetworkHandler.playToServer(new SetZenithGhostConfigPacket(
                Minecraft.getInstance().player.getId(), distance, mode
            ));
        }
    }

    @Override
    public void render(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);

        int centerX = this.width / 2;
        int titleY = this.height / 2 - 70;

        guiGraphics.drawCenteredString(this.font, this.title, centerX, titleY, TEXT_COLOR);
        guiGraphics.renderItem(new ItemStack(ZenithItems.ZENITH.get()), centerX - 8, titleY + 18);
        guiGraphics.drawCenteredString(
            this.font,
            Component.translatable("screen.the_zenith_sword.ghost_config.hint"),
            centerX,
            this.height / 2 + 50,
            HINT_COLOR
        );

        if (this.distanceButton != null) {
            this.distanceButton.setMessage(this.distanceLabel());
        }
        if (this.modeButton != null) {
            this.modeButton.setMessage(this.modeLabel());
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
