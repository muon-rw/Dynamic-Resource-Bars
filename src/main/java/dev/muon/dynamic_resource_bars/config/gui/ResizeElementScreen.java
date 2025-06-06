package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

#if (!NEWER_THAN_20_1)
    import dev.muon.dynamic_resource_bars.util.ScreenRect;
#endif

public class ResizeElementScreen extends Screen {

    private final Screen parentScreen;
    private final DraggableElement elementToResize;

    private EditBox bgWidthBox;
    private EditBox bgHeightBox;
    private EditBox bgXOffsetBox;
    private EditBox bgYOffsetBox;
    private EditBox barWidthBox;
    private EditBox barHeightBox;
    private EditBox overlayWidthBox;
    private EditBox overlayHeightBox;

    public ResizeElementScreen(Screen parent, DraggableElement element) {
        super(Component.translatable("gui.dynamic_resource_bars.resize.title_format", getFriendlyElementName(element)));
        this.parentScreen = parent;
        this.elementToResize = element;
    }

    @Override
    protected void init() {
        super.init();
        ClientConfig config = ModConfigManager.getClient();
        int bgWidthConf, bgHeightConf, bgXOffsetConf, bgYOffsetConf, barWidthConf, barHeightConf, overlayWidthConf, overlayHeightConf;

        switch (elementToResize) {
            case HEALTH_BAR:
                bgWidthConf = config.healthBackgroundWidth;
                bgHeightConf = config.healthBackgroundHeight;
                bgXOffsetConf = config.healthBackgroundXOffset;
                bgYOffsetConf = config.healthBackgroundYOffset;
                barWidthConf = config.healthBarWidth;
                barHeightConf = config.healthBarHeight;
                overlayWidthConf = config.healthOverlayWidth;
                overlayHeightConf = config.healthOverlayHeight;
                break;
            case STAMINA_BAR:
                bgWidthConf = config.staminaBackgroundWidth;
                bgHeightConf = config.staminaBackgroundHeight;
                bgXOffsetConf = config.staminaBackgroundXOffset;
                bgYOffsetConf = config.staminaBackgroundYOffset;
                barWidthConf = config.staminaBarWidth;
                barHeightConf = config.staminaBarHeight;
                overlayWidthConf = config.staminaOverlayWidth;
                overlayHeightConf = config.staminaOverlayHeight;
                break;
            case MANA_BAR:
                bgWidthConf = config.manaBackgroundWidth;
                bgHeightConf = config.manaBackgroundHeight;
                bgXOffsetConf = config.manaBackgroundXOffset;
                bgYOffsetConf = config.manaBackgroundYOffset;
                barWidthConf = config.manaBarWidth;
                barHeightConf = config.manaBarHeight;
                overlayWidthConf = config.manaOverlayWidth;
                overlayHeightConf = config.manaOverlayHeight;
                break;
            default:
                if (this.minecraft != null) this.minecraft.setScreen(parentScreen);
                return;
        }

        int boxWidth = 50;
        int boxHeight = 20;
        int labelWidth = 100;
        // Recalculate startX to center the entire block of label + edit box
        int componentBlockWidth = labelWidth + 5 + boxWidth;
        int startX = (this.width / 2) - componentBlockWidth / 2;
        int editBoxX = startX + labelWidth + 5;
        int currentY = 40;
        int rowSpacing = 5;

        bgWidthBox = createIntEditBox(editBoxX, currentY, boxWidth, boxHeight, bgWidthConf);
        bgHeightBox = createIntEditBox(editBoxX, currentY + boxHeight + rowSpacing, boxWidth, boxHeight, bgHeightConf);
        bgXOffsetBox = createIntEditBox(editBoxX, currentY + 2 * (boxHeight + rowSpacing), boxWidth, boxHeight, bgXOffsetConf, Integer.MIN_VALUE, Integer.MAX_VALUE);
        bgYOffsetBox = createIntEditBox(editBoxX, currentY + 3 * (boxHeight + rowSpacing), boxWidth, boxHeight, bgYOffsetConf, Integer.MIN_VALUE, Integer.MAX_VALUE);
        this.addRenderableWidget(bgWidthBox);
        this.addRenderableWidget(bgHeightBox);
        this.addRenderableWidget(bgXOffsetBox);
        this.addRenderableWidget(bgYOffsetBox);
        currentY += 4 * (boxHeight + rowSpacing) + rowSpacing; // Add extra spacing between groups

        barWidthBox = createIntEditBox(editBoxX, currentY, boxWidth, boxHeight, barWidthConf, 0, 256);
        barHeightBox = createIntEditBox(editBoxX, currentY + boxHeight + rowSpacing, boxWidth, boxHeight, barHeightConf, 0, 32);
        this.addRenderableWidget(barWidthBox);
        this.addRenderableWidget(barHeightBox);
        currentY += 2 * (boxHeight + rowSpacing) + rowSpacing;

        overlayWidthBox = createIntEditBox(editBoxX, currentY, boxWidth, boxHeight, overlayWidthConf, 0, 256);
        overlayHeightBox = createIntEditBox(editBoxX, currentY + boxHeight + rowSpacing, boxWidth, boxHeight, overlayHeightConf, 0, 256);
        this.addRenderableWidget(overlayWidthBox);
        this.addRenderableWidget(overlayHeightBox);

        int doneButtonWidth = 100;
        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.done"),
                        (button) -> this.onClose())
                .bounds((this.width / 2) - (doneButtonWidth / 2), this.height - boxHeight - 20, doneButtonWidth, boxHeight)
                .build());
    }

    private EditBox createIntEditBox(int x, int y, int width, int height, int configIntValue) {
        // Default min value for sizes should be 1 to ensure minimum size for rendering
        return createIntEditBox(x, y, width, height, configIntValue, 1, Integer.MAX_VALUE);
    }

    private EditBox createIntEditBox(int x, int y, int width, int height, int configIntValue, int minValue, int maxValue) {
        EditBox editBox = new EditBox(this.font, x, y, width, height, Component.empty());
        editBox.setValue(String.valueOf(configIntValue));
        editBox.setResponder((text) -> {
            try {
                int value = Integer.parseInt(text);
                if (value >= minValue && value <= maxValue) { // Check against min and max
                   //configIntValue.set(value);
                    editBox.setTextColor(0xE0E0E0); // Default color
                } else {
                    editBox.setTextColor(0xFF5555); // Red for out of bounds
                }
            } catch (NumberFormatException e) {
                editBox.setTextColor(0xFF5555); // Red for invalid number
            }
        });
        return editBox;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        #if NEWER_THAN_20_1
            this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        #else
            this.renderBackground(graphics);
        #endif
        
        super.render(graphics, mouseX, mouseY, partialTicks);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        if (bgWidthBox != null) { 
            int labelX = bgWidthBox.getX() - 5 - 100; 
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.background_width"), labelX, bgWidthBox.getY() + (bgWidthBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.background_height"), labelX, bgHeightBox.getY() + (bgHeightBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.background_x_offset"), labelX, bgXOffsetBox.getY() + (bgXOffsetBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.background_y_offset"), labelX, bgYOffsetBox.getY() + (bgYOffsetBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.bar_width"), labelX, barWidthBox.getY() + (barWidthBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.bar_height"), labelX, barHeightBox.getY() + (barHeightBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.overlay_width"), labelX, overlayWidthBox.getY() + (overlayWidthBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.overlay_height"), labelX, overlayHeightBox.getY() + (overlayHeightBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        boolean unfocusedAny = false;
        for (net.minecraft.client.gui.components.events.GuiEventListener listener : this.children()) {
            if (listener instanceof EditBox) {
                EditBox box = (EditBox) listener;
                #if NEWER_THAN_20_1 // For 1.21.1+ which has containsPoint
                    ScreenRectangle vanillaRect = box.getRectangle();
                    if (box.isFocused() && !vanillaRect.containsPoint((int)mouseX, (int)mouseY)) {
                        box.setFocused(false);
                        unfocusedAny = true;
                    }
                #else // For 1.20.1 (Fabric or Forge)
                    // Vanilla ScreenRectangle exists in 1.20.1 but lacks containsPoint.
                    // We use its getters to construct our custom ScreenRect for the contains check.
                    ScreenRectangle vanillaRect = box.getRectangle();
                    ScreenRect customRect = 
                        new ScreenRect(vanillaRect.left(), vanillaRect.top(), 
                                       vanillaRect.width(), vanillaRect.height());
                    if (box.isFocused() && !customRect.contains((int)mouseX, (int)mouseY)) {
                        box.setFocused(false);
                        unfocusedAny = true;
                    }
                #endif
            }
        }
        return unfocusedAny; 
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Prioritize focused element for key presses (e.g., typing in EditBox)
        if (this.getFocused() != null && this.getFocused().keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        // Save all values to config before closing
        ClientConfig config = ModConfigManager.getClient();
        
        try {
            switch (elementToResize) {
                case HEALTH_BAR:
                    config.healthBackgroundWidth = parseIntSafely(bgWidthBox.getValue(), config.healthBackgroundWidth, 1, Integer.MAX_VALUE);
                    config.healthBackgroundHeight = parseIntSafely(bgHeightBox.getValue(), config.healthBackgroundHeight, 1, Integer.MAX_VALUE);
                    config.healthBarWidth = parseIntSafely(barWidthBox.getValue(), config.healthBarWidth, 1, 256);
                    config.healthBarHeight = parseIntSafely(barHeightBox.getValue(), config.healthBarHeight, 1, 32);
                    config.healthOverlayWidth = parseIntSafely(overlayWidthBox.getValue(), config.healthOverlayWidth, 1, 256);
                    config.healthOverlayHeight = parseIntSafely(overlayHeightBox.getValue(), config.healthOverlayHeight, 1, 256);
                    config.healthBackgroundXOffset = parseIntSafely(bgXOffsetBox.getValue(), config.healthBackgroundXOffset, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    config.healthBackgroundYOffset = parseIntSafely(bgYOffsetBox.getValue(), config.healthBackgroundYOffset, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    break;
                case STAMINA_BAR:
                    config.staminaBackgroundWidth = parseIntSafely(bgWidthBox.getValue(), config.staminaBackgroundWidth, 1, Integer.MAX_VALUE);
                    config.staminaBackgroundHeight = parseIntSafely(bgHeightBox.getValue(), config.staminaBackgroundHeight, 1, Integer.MAX_VALUE);
                    config.staminaBarWidth = parseIntSafely(barWidthBox.getValue(), config.staminaBarWidth, 1, 256);
                    config.staminaBarHeight = parseIntSafely(barHeightBox.getValue(), config.staminaBarHeight, 1, 32);
                    config.staminaOverlayWidth = parseIntSafely(overlayWidthBox.getValue(), config.staminaOverlayWidth, 1, 256);
                    config.staminaOverlayHeight = parseIntSafely(overlayHeightBox.getValue(), config.staminaOverlayHeight, 1, 256);
                    config.staminaBackgroundXOffset = parseIntSafely(bgXOffsetBox.getValue(), config.staminaBackgroundXOffset, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    config.staminaBackgroundYOffset = parseIntSafely(bgYOffsetBox.getValue(), config.staminaBackgroundYOffset, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    break;
                case MANA_BAR:
                    config.manaBackgroundWidth = parseIntSafely(bgWidthBox.getValue(), config.manaBackgroundWidth, 1, Integer.MAX_VALUE);
                    config.manaBackgroundHeight = parseIntSafely(bgHeightBox.getValue(), config.manaBackgroundHeight, 1, Integer.MAX_VALUE);
                    config.manaBarWidth = parseIntSafely(barWidthBox.getValue(), config.manaBarWidth, 1, 256);
                    config.manaBarHeight = parseIntSafely(barHeightBox.getValue(), config.manaBarHeight, 1, 32);
                    config.manaOverlayWidth = parseIntSafely(overlayWidthBox.getValue(), config.manaOverlayWidth, 1, 256);
                    config.manaOverlayHeight = parseIntSafely(overlayHeightBox.getValue(), config.manaOverlayHeight, 1, 256);
                    config.manaBackgroundXOffset = parseIntSafely(bgXOffsetBox.getValue(), config.manaBackgroundXOffset, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    config.manaBackgroundYOffset = parseIntSafely(bgYOffsetBox.getValue(), config.manaBackgroundYOffset, Integer.MIN_VALUE, Integer.MAX_VALUE);
                    break;
            }
            
            // Save config to disk
            config.save();
        } catch (Exception e) {
            // If there's any error parsing, just close without saving
        }
        
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
    
    private int parseIntSafely(String value, int defaultValue, int minValue, int maxValue) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < minValue) return minValue;
            if (parsed > maxValue) return maxValue;
            return parsed;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String getFriendlyElementName(DraggableElement element) {
        if (element == null) return "";
        return switch (element) {
            case HEALTH_BAR -> Component.translatable("gui.dynamic_resource_bars.element.health_bar").getString();
            case MANA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.mana_bar").getString();
            case STAMINA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.stamina_bar").getString();
            default -> element.name();
        };
    }
} 