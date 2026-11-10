package com.oceanscenery.zenith.client.gui;

import com.oceanscenery.zenith.mod_class.attachment.ZenithSlot;
import com.oceanscenery.zenith.mod_class.data_component.AttackMode;
import com.oceanscenery.zenith.registry.ZenithItems;
import com.oceanscenery.zenith.server.ZenithNetworkHandler;
import com.oceanscenery.zenith.server.packet.SetZenithGhostConfigPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class GhostConfigScreen extends Screen {
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int HINT_COLOR = 0xFFA0A0A0;

    private ZenithSlot slot;
    private Button distanceButton;
    private Button modeButton;

    public GhostConfigScreen(ZenithSlot slot) {
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
            (int) this.slot.distance()
        );
    }

    private Component modeLabel() {
        return Component.translatable(
            "screen.the_zenith_sword.ghost_config.mode",
            Component.translatable("screen.the_zenith_sword.ghost_config.mode." + this.slot.mode().ordinal())
        );
    }

    private void cycleDistance() {
        this.apply((int) this.slot.distance() % 100 + 20, this.slot.mode());
    }

    private void cycleMode() {
        this.apply(this.slot.distance(), this.slot.mode().next());
    }

    private void apply(double distance, AttackMode mode) {
        this.slot = new ZenithSlot(
            this.slot.valid(),
            this.slot.open(),
            distance, mode
        );

        if (Minecraft.getInstance().player != null) {
            ZenithNetworkHandler.sendToServer(new SetZenithGhostConfigPacket(
                Minecraft.getInstance().player.getId(), distance, mode
            ));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        int centerX = this.width / 2;
        int titleY = this.height / 2 - 70;

        graphics.centeredText(this.font, this.title, centerX, titleY, TEXT_COLOR);
        graphics.item(new ItemStack(ZenithItems.ZENITH.get()), centerX - 8, titleY + 18);
        graphics.centeredText(
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

        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
