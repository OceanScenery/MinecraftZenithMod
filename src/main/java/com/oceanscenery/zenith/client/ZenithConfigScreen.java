package com.oceanscenery.zenith.client;

import com.oceanscenery.zenith.registry.ZenithConfigs;
import com.oceanscenery.zenith.zenith_class.config.ZenithClientConfig;
import com.oceanscenery.zenith.zenith_class.config.ZenithConfig;
import com.oceanscenery.zenith.zenith_class.config.InvalidDamageAction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;

public final class ZenithConfigScreen extends Screen {
    private static final int TAB_Y = 30;
    private static final int TAB_WIDTH = 96;
    private static final int TAB_GAP = 8;
    private static final int CONTENT_Y = 62;
    private static final int ROW_HEIGHT = 30;
    private static final int PANEL_PADDING = 14;
    private static final int LABEL_CONTROL_GAP = 16;
    private static final int CONTROL_HEIGHT = 20;

    private final Screen parent;
    private final List<Entry> clientEntries = new ArrayList<>();
    private final List<Entry> combatEntries = new ArrayList<>();
    private final List<Entry> behaviorEntries = new ArrayList<>();
    private List<Entry> visibleEntries = clientEntries;
    private Page page = Page.CLIENT;
    private Component status = CommonComponents.EMPTY;

    public ZenithConfigScreen(Screen parent) {
        super(Component.translatable("the_zenith_sword.configuration.title"));
        this.parent = parent;
        createClientEntries();
        createServerEntries();
    }

    private void createClientEntries() {
        ZenithClientConfig config = ZenithConfigs.ZENITH_CLIENT_CONFIG;
        clientEntries.add(Entry.booleanEntry("render_offset", config.RENDER_OFFSET));
        clientEntries.add(Entry.doubleEntry("trail_angle", config.TRAIL_ANGLE, 80.0, 200.0));
        clientEntries.add(Entry.booleanEntry("3d_trail", config._3D_TRAIL));
        clientEntries.add(Entry.booleanEntry("block_interaction", config.BLOCK_INTERACTION));
    }

    private void createServerEntries() {
        if (!ZenithConfigs.CONFIG.isLoaded()) {
            return;
        }

        ZenithConfig config = ZenithConfigs.ZENITH_CONFIG;
        combatEntries.add(Entry.doubleEntry("ensured_damage_for_non_player", config.ensured_damage_for_non_player, 0.0, 1.0));
        combatEntries.add(Entry.doubleEntry("ensured_damage_for_player", config.ensured_damage_for_player, 0.0, 1.0));
        combatEntries.add(Entry.doubleEntry("default_damage", config.default_damage, 0.0, Float.MAX_VALUE));
        combatEntries.add(Entry.doubleEntry("ranged_damage_factor", config.ranged_damage_factor, 0.0, 1.0));
        combatEntries.add(Entry.doubleEntry("percentage_damage_factor", config.percentage_damage_factor, 0.0, 1.0));
        combatEntries.add(Entry.enumEntry("invalid_damage_action", config.invalid_damage_action));

        behaviorEntries.add(Entry.booleanEntry("sort_farest", config.sort_farest));
        behaviorEntries.add(Entry.booleanEntry("disable_knockback", config.disable_knockback));
        behaviorEntries.add(Entry.booleanEntry("enable_bypass_invulnerable", config.enable_bypass_invulnerable));
        behaviorEntries.add(Entry.booleanEntry("enable_attack_item", config.enable_attack_item));
        behaviorEntries.add(Entry.booleanEntry("enable_terra_damage_modifier", config.enable_terra_damage_modifier));
    }

    @Override
    protected void init() {
        visibleEntries = switch (page) {
            case CLIENT -> clientEntries;
            case COMBAT -> combatEntries;
            case BEHAVIOR -> behaviorEntries;
        };

        int tabsWidth = TAB_WIDTH * 3 + TAB_GAP * 2;
        int tabX = width / 2 - tabsWidth / 2;
        addTab("the_zenith_sword.configuration.native.tab.client", Page.CLIENT, tabX, TAB_WIDTH);
        addTab("the_zenith_sword.configuration.native.tab.combat", Page.COMBAT, tabX + TAB_WIDTH + TAB_GAP, TAB_WIDTH);
        addTab("the_zenith_sword.configuration.native.tab.behavior", Page.BEHAVIOR,
                tabX + (TAB_WIDTH + TAB_GAP) * 2, TAB_WIDTH);

        if (!visibleEntries.isEmpty()) {
            layoutEntries();
        }

        int footerWidth = 256;
        int footerX = width / 2 - footerWidth / 2;
        int footerY = height - 27;
        addRenderableWidget(Button.builder(Component.translatable("controls.reset"), button -> resetVisibleEntries())
                .bounds(footerX, footerY, 90, CONTROL_HEIGHT)
                .build())
                .active = !visibleEntries.isEmpty();
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> onClose())
                .bounds(footerX + 98, footerY, 75, CONTROL_HEIGHT)
                .build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> saveAndClose())
                .bounds(footerX + 181, footerY, 75, CONTROL_HEIGHT)
                .build());
    }

    private void addTab(String translationKey, Page targetPage, int x, int width) {
        Button button = Button.builder(
                        Component.translatable(translationKey),
                        pressed -> switchPage(targetPage)
                )
                .bounds(x, TAB_Y, width, CONTROL_HEIGHT)
                .build();
        button.active = page != targetPage;
        addRenderableWidget(button);
    }

    private void layoutEntries() {
        int panelWidth = Math.min(430, width - 20);
        int panelX = width / 2 - panelWidth / 2;
        int contentWidth = panelWidth - PANEL_PADDING * 2;
        int contentX = panelX + PANEL_PADDING;
        int controlWidth = Math.min(190, contentWidth / 2);
        int labelWidth = contentWidth - controlWidth - LABEL_CONTROL_GAP;
        int controlX = contentX + labelWidth + LABEL_CONTROL_GAP;

        for (int i = 0; i < visibleEntries.size(); i++) {
            int y = CONTENT_Y + i * ROW_HEIGHT;
            visibleEntries.get(i).createWidgets(
                    this,
                    contentX,
                    y + 6,
                    labelWidth,
                    controlX,
                    y,
                    controlWidth
            );
        }
    }

    private void switchPage(Page nextPage) {
        if (page == nextPage) {
            return;
        }
        page = nextPage;
        status = CommonComponents.EMPTY;
        rebuildWidgets();
    }

    private void resetVisibleEntries() {
        for (Entry entry : visibleEntries) {
            entry.reset();
        }
        status = CommonComponents.EMPTY;
    }

    private void saveAndClose() {
        for (Entry entry : clientEntries) {
            if (!entry.validate()) {
                status = Component.translatable("the_zenith_sword.configuration.native.invalid_value", entry.label);
                return;
            }
        }
        for (Entry entry : combatEntries) {
            if (!entry.validate()) {
                status = Component.translatable("the_zenith_sword.configuration.native.invalid_value", entry.label);
                return;
            }
        }
        for (Entry entry : behaviorEntries) {
            if (!entry.validate()) {
                status = Component.translatable("the_zenith_sword.configuration.native.invalid_value", entry.label);
                return;
            }
        }

        for (Entry entry : clientEntries) {
            entry.apply();
        }
        for (Entry entry : combatEntries) {
            entry.apply();
        }
        for (Entry entry : behaviorEntries) {
            entry.apply();
        }

        ZenithConfigs.CLIENT_CONFIG.save();
        if (!combatEntries.isEmpty() || !behaviorEntries.isEmpty()) {
            ZenithConfigs.CONFIG.save();
        }
        onClose();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int panelWidth = Math.min(430, width - 20);
        int panelX = width / 2 - panelWidth / 2;
        int panelBottom = height - 33;
        graphics.fill(panelX, TAB_Y + 26, panelX + panelWidth, panelBottom, 0x90000000);
        graphics.renderOutline(panelX, TAB_Y + 26, panelWidth, panelBottom - TAB_Y - 26, 0xFF666666);

        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 12, 0xFFFFFF);

        if (visibleEntries.isEmpty() && page != Page.CLIENT) {
            graphics.drawCenteredString(
                    font,
                    Component.translatable("the_zenith_sword.configuration.native.server_unavailable"),
                    width / 2,
                    height / 2,
                    0xFFAA00
            );
        } else {
            for (Entry entry : visibleEntries) {
                entry.renderLabel(graphics, font);
            }
        }

        if (status != CommonComponents.EMPTY) {
            graphics.drawCenteredString(font, status, width / 2, TAB_Y + 21, 0xFF5555);
        }
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    private enum Page {
        CLIENT,
        COMBAT,
        BEHAVIOR
    }

    private static final class Entry {
        private final Component label;
        private final Component tooltip;
        private final ForgeConfigSpec.BooleanValue booleanValue;
        private final ForgeConfigSpec.DoubleValue doubleValue;
        private final ForgeConfigSpec.EnumValue<InvalidDamageAction> enumValue;
        private final double minimum;
        private final double maximum;
        private boolean pendingBoolean;
        private InvalidDamageAction pendingEnum;
        private String numberText;
        private double pendingNumber;
        private int labelX;
        private int labelY;
        private int displayWidth;
        private Button toggle;
        private EditBox input;

        private Entry(Component label, Component tooltip,
                      ForgeConfigSpec.BooleanValue booleanValue,
                      ForgeConfigSpec.DoubleValue doubleValue,
                      ForgeConfigSpec.EnumValue<InvalidDamageAction> enumValue,
                      double minimum, double maximum) {
            this.label = label;
            this.tooltip = tooltip;
            this.booleanValue = booleanValue;
            this.doubleValue = doubleValue;
            this.enumValue = enumValue;
            this.minimum = minimum;
            this.maximum = maximum;
        }

        private static Entry booleanEntry(String key, ForgeConfigSpec.BooleanValue value) {
            Entry entry = new Entry(
                    label(key),
                    tooltip(key),
                    value,
                    null,
                    null,
                    0.0,
                    0.0
            );
            entry.pendingBoolean = value.get();
            return entry;
        }

        private static Entry doubleEntry(String key, ForgeConfigSpec.DoubleValue value,
                                         double minimum, double maximum) {
            Entry entry = new Entry(
                    label(key),
                    tooltip(key),
                    null,
                    value,
                    null,
                    minimum,
                    maximum
            );
            entry.pendingNumber = value.get();
            entry.numberText = format(entry.pendingNumber);
            return entry;
        }

        private static Entry enumEntry(
                String key,
                ForgeConfigSpec.EnumValue<InvalidDamageAction> value
        ) {
            Entry entry = new Entry(
                    label(key),
                    tooltip(key),
                    null,
                    null,
                    value,
                    0.0,
                    0.0
            );
            entry.pendingEnum = value.get();
            return entry;
        }

        private void createWidgets(ZenithConfigScreen screen,
                                   int labelX, int labelY, int labelWidth,
                                   int controlX, int controlY, int controlWidth) {
            this.labelX = labelX;
            this.labelY = labelY;
            displayWidth = labelWidth;

            if (enumValue != null) {
                toggle = Button.builder(enumMessage(pendingEnum), button -> {
                            InvalidDamageAction[] values = InvalidDamageAction.values();
                            pendingEnum = values[(pendingEnum.ordinal() + 1) % values.length];
                            button.setMessage(enumMessage(pendingEnum));
                        })
                        .bounds(controlX, controlY, controlWidth, CONTROL_HEIGHT)
                        .build();
                if (tooltip != null) {
                    toggle.setTooltip(Tooltip.create(tooltip));
                }
                screen.addRenderableWidget(toggle);
            } else if (booleanValue != null) {
                toggle = Button.builder(CommonComponents.optionStatus(pendingBoolean), button -> {
                            pendingBoolean = !pendingBoolean;
                            button.setMessage(CommonComponents.optionStatus(pendingBoolean));
                        })
                        .bounds(controlX, controlY, controlWidth, CONTROL_HEIGHT)
                        .build();
                if (tooltip != null) {
                    toggle.setTooltip(Tooltip.create(tooltip));
                }
                screen.addRenderableWidget(toggle);
            } else {
                input = new EditBox(screen.font, controlX, controlY, controlWidth, CONTROL_HEIGHT, label);
                input.setMaxLength(64);
                input.setValue(numberText);
                input.setResponder(value -> numberText = value);
                input.setFilter(value -> value.isEmpty()
                        || value.matches("-?\\d*(\\.\\d*)?([eE][-+]?\\d*)?"));
                if (tooltip != null) {
                    input.setTooltip(Tooltip.create(tooltip));
                }
                screen.addRenderableWidget(input);
            }
        }

        private void renderLabel(GuiGraphics graphics, Font font) {
            String clipped = font.plainSubstrByWidth(label.getString(), displayWidth);
            graphics.drawString(font, clipped, labelX, labelY, 0xE0E0E0, false);
        }

        private boolean validate() {
            if (booleanValue != null || enumValue != null) {
                return true;
            }

            String value = numberText == null ? "" : numberText.trim();
            if (value.isEmpty()) {
                return false;
            }

            try {
                pendingNumber = Double.parseDouble(value);
            } catch (NumberFormatException exception) {
                return false;
            }
            return Double.isFinite(pendingNumber)
                    && pendingNumber >= minimum
                    && pendingNumber <= maximum;
        }

        private void apply() {
            if (enumValue != null) {
                enumValue.set(pendingEnum);
            } else if (booleanValue != null) {
                booleanValue.set(pendingBoolean);
            } else {
                doubleValue.set(pendingNumber);
            }
        }

        private void reset() {
            if (enumValue != null) {
                pendingEnum = enumValue.getDefault();
                if (toggle != null) {
                    toggle.setMessage(enumMessage(pendingEnum));
                }
            } else if (booleanValue != null) {
                pendingBoolean = booleanValue.getDefault();
                if (toggle != null) {
                    toggle.setMessage(CommonComponents.optionStatus(pendingBoolean));
                }
            } else {
                pendingNumber = doubleValue.getDefault();
                numberText = format(pendingNumber);
                if (input != null) {
                    input.setValue(numberText);
                }
            }
        }

        private static Component label(String key) {
            return Component.translatable("the_zenith_sword.configuration." + key);
        }

        private static Component tooltip(String key) {
            return Component.translatable("the_zenith_sword.configuration." + key + ".tooltip");
        }

        private static Component enumMessage(InvalidDamageAction value) {
            return Component.translatable(
                    "the_zenith_sword.configuration.invalid_damage_action.value."
                            + value.name()
            );
        }

        private static String format(double value) {
            if (value == Math.rint(value) && Math.abs(value) < 1.0E15) {
                return Long.toString((long) value);
            }
            return Double.toString(value);
        }
    }
}
