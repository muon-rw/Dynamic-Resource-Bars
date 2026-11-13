package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.function.IntConsumer;

public class ModConfigScreen extends Screen {

    private static final int VALID_TEXT_COLOR = 0xFFE0E0E0;
    private static final int INVALID_TEXT_COLOR = 0xFFFF5555;

    private final Screen parentScreen;

    private EditBox textColorBox;
    private EditBox textOpacityBox;
    private EditBox textSizeBox;
    private EditBox fadeHoldBox;
    private EditBox leftHudOffsetBox;
    private EditBox rightHudOffsetBox;

    public ModConfigScreen(Screen parent) {
        super(Component.literal(Constants.MOD_NAME + " Configuration"));
        this.parentScreen = parent;
    }

    @Override
    protected void init() {
        super.init();
        ClientConfig config = ModConfigManager.getClient();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int rowHeight = 18;
        int rowGap = 4;
        int centerX = this.width / 2;

        int totalContentHeight = buttonHeight + 4 + buttonHeight + 10 + 6 * (rowHeight + rowGap);
        int topLimit = 32;
        int bottomLimit = this.height - 36;
        int currentY = Math.max(topLimit, topLimit + (bottomLimit - topLimit - totalContentHeight) / 2);

        Minecraft mc = Minecraft.getInstance();
        boolean canEditHUD = mc.level != null;

        Button editHud = Button.builder(
                        Component.translatable("gui.dynamic_resource_bars.config.button.open_hud_editor"),
                        b -> { if (b.active) Minecraft.getInstance().setScreen(new HudEditorScreen(this)); })
                .bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
                .build();
        editHud.active = canEditHUD;
        if (!canEditHUD) {
            editHud.setTooltip(Tooltip.create(
                    Component.translatable("gui.dynamic_resource_bars.config.tooltip.hud_editor_disabled")));
        }
        this.addRenderableWidget(editHud);

        currentY += buttonHeight + 4;

        Button customHudBtn = Button.builder(
                        customHudLabel(config.enableCustomHud),
                        b -> {
                            config.enableCustomHud = !config.enableCustomHud;
                            config.save();
                            b.setMessage(customHudLabel(config.enableCustomHud));
                        })
                .bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
                .build();
        this.addRenderableWidget(customHudBtn);

        currentY += buttonHeight + 10;

        int editBoxWidth = 64;
        int editX = centerX + 12;

        textColorBox = new EditBox(this.font, editX, currentY, editBoxWidth, rowHeight, Component.empty());
        textColorBox.setMaxLength(6);
        textColorBox.setValue(String.format("%06X", config.globalTextColor & 0xFFFFFF));
        textColorBox.setTextColor(VALID_TEXT_COLOR);
        textColorBox.setResponder(text -> {
            try {
                int color = Integer.parseInt(text.trim(), 16) & 0xFFFFFF;
                applyGlobalTextColor(config, color);
                textColorBox.setTextColor(VALID_TEXT_COLOR);
            } catch (NumberFormatException e) {
                textColorBox.setTextColor(INVALID_TEXT_COLOR);
            }
        });
        this.addRenderableWidget(textColorBox);
        currentY += rowHeight + rowGap;

        textOpacityBox = new EditBox(this.font, editX, currentY, editBoxWidth, rowHeight, Component.empty());
        textOpacityBox.setMaxLength(3);
        textOpacityBox.setValue(String.valueOf(config.globalTextOpacity));
        textOpacityBox.setTextColor(VALID_TEXT_COLOR);
        textOpacityBox.setResponder(text -> {
            try {
                int opacity = Integer.parseInt(text.trim());
                if (opacity >= 0 && opacity <= 255) {
                    applyGlobalTextOpacity(config, opacity);
                    textOpacityBox.setTextColor(VALID_TEXT_COLOR);
                } else {
                    textOpacityBox.setTextColor(INVALID_TEXT_COLOR);
                }
            } catch (NumberFormatException e) {
                textOpacityBox.setTextColor(INVALID_TEXT_COLOR);
            }
        });
        this.addRenderableWidget(textOpacityBox);
        currentY += rowHeight + rowGap;

        textSizeBox = new EditBox(this.font, editX, currentY, editBoxWidth, rowHeight, Component.empty());
        textSizeBox.setMaxLength(5);
        textSizeBox.setValue(FieldEditorUtils.formatScale(config.globalTextSize));
        textSizeBox.setTextColor(VALID_TEXT_COLOR);
        textSizeBox.setResponder(text -> {
            try {
                float size = Float.parseFloat(text.trim());
                if (size >= 0.1f && size <= 5.0f) {
                    config.globalTextSize = size;
                    textSizeBox.setTextColor(VALID_TEXT_COLOR);
                } else {
                    textSizeBox.setTextColor(INVALID_TEXT_COLOR);
                }
            } catch (NumberFormatException e) {
                textSizeBox.setTextColor(INVALID_TEXT_COLOR);
            }
        });
        this.addRenderableWidget(textSizeBox);
        currentY += rowHeight + rowGap;

        fadeHoldBox = createIntEditBox(editX, currentY, editBoxWidth, rowHeight, 5,
                config.fadeHoldDuration, 0, 60000,
                "gui.dynamic_resource_bars.config.tooltip.fade_hold_duration",
                ms -> config.fadeHoldDuration = ms);
        this.addRenderableWidget(fadeHoldBox);
        currentY += rowHeight + rowGap;

        leftHudOffsetBox = createIntEditBox(editX, currentY, editBoxWidth, rowHeight, 4,
                config.leftHudLayerOffset, -200, 200,
                "gui.dynamic_resource_bars.config.tooltip.left_hud_offset",
                offset -> config.leftHudLayerOffset = offset);
        this.addRenderableWidget(leftHudOffsetBox);
        currentY += rowHeight + rowGap;

        rightHudOffsetBox = createIntEditBox(editX, currentY, editBoxWidth, rowHeight, 4,
                config.rightHudLayerOffset, -200, 200,
                "gui.dynamic_resource_bars.config.tooltip.right_hud_offset",
                offset -> config.rightHudLayerOffset = offset);
        this.addRenderableWidget(rightHudOffsetBox);

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        b -> onClose())
                .bounds(centerX - buttonWidth / 2, this.height - buttonHeight - 10, buttonWidth, buttonHeight)
                .build());
    }

    private EditBox createIntEditBox(int x, int y, int width, int height, int maxLength,
                                   int initialValue, int min, int max,
                                   String tooltipKey, IntConsumer onValid) {
        EditBox box = new EditBox(this.font, x, y, width, height, Component.empty());
        box.setMaxLength(maxLength);
        box.setValue(String.valueOf(initialValue));
        box.setTextColor(VALID_TEXT_COLOR);
        box.setTooltip(Tooltip.create(Component.translatable(tooltipKey)));
        box.setResponder(text -> {
            try {
                int parsed = Integer.parseInt(text.trim());
                if (parsed >= min && parsed <= max) {
                    onValid.accept(parsed);
                    box.setTextColor(VALID_TEXT_COLOR);
                } else {
                    box.setTextColor(INVALID_TEXT_COLOR);
                }
            } catch (NumberFormatException e) {
                box.setTextColor(INVALID_TEXT_COLOR);
            }
        });
        return box;
    }

    private static void applyGlobalTextColor(ClientConfig config, int color) {
        config.globalTextColor = color;
        config.healthTextColor = color;
        config.staminaTextColor = color;
        config.manaTextColor = color;
        config.armorTextColor = color;
        config.airTextColor = color;
    }

    private static void applyGlobalTextOpacity(ClientConfig config, int opacity) {
        config.globalTextOpacity = opacity;
        config.healthTextOpacity = opacity;
        config.staminaTextOpacity = opacity;
        config.manaTextOpacity = opacity;
        config.armorTextOpacity = opacity;
        config.airTextOpacity = opacity;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        if (textColorBox == null) return;
        FieldEditorUtils.drawLabel(graphics, this.font, "gui.dynamic_resource_bars.config.label.text_color", 0, textColorBox);
        FieldEditorUtils.drawLabel(graphics, this.font, "gui.dynamic_resource_bars.config.label.text_opacity", 0, textOpacityBox);
        FieldEditorUtils.drawLabel(graphics, this.font, "gui.dynamic_resource_bars.config.label.text_size", 0, textSizeBox);
        FieldEditorUtils.drawLabel(graphics, this.font, "gui.dynamic_resource_bars.config.label.fade_hold_duration", 0, fadeHoldBox);
        FieldEditorUtils.drawLabel(graphics, this.font, "gui.dynamic_resource_bars.config.label.left_hud_offset", 0, leftHudOffsetBox);
        FieldEditorUtils.drawLabel(graphics, this.font, "gui.dynamic_resource_bars.config.label.right_hud_offset", 0, rightHudOffsetBox);

        int swatchX = textColorBox.getX() + textColorBox.getWidth() + 4;
        int swatchY = textColorBox.getY() + 2;
        int swatchSize = textColorBox.getHeight() - 4;
        int previewColor = (ModConfigManager.getClient().globalTextColor & 0xFFFFFF) | 0xFF000000;
        graphics.fill(swatchX - 1, swatchY - 1, swatchX + swatchSize + 1, swatchY + swatchSize + 1, 0xFF808080);
        graphics.fill(swatchX, swatchY, swatchX + swatchSize, swatchY + swatchSize, previewColor);
    }

    private static Component customHudLabel(boolean enabled) {
        return Component.translatable(enabled
                ? "gui.dynamic_resource_bars.config.custom_hud.enabled"
                : "gui.dynamic_resource_bars.config.custom_hud.disabled");
    }

    @Override
    public void onClose() {
        ModConfigManager.getClient().save();
        if (this.minecraft != null) this.minecraft.setScreen(this.parentScreen);
    }
}
