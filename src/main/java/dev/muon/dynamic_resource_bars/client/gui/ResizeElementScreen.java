package dev.muon.dynamic_resource_bars.client.gui;

import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
import dev.muon.dynamic_resource_bars.foundation.config.CClient;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import toni.lib.config.ConfigBase; // Import ConfigBase
import toni.lib.config.ConfigBase.ConfigInt; // Try importing inner class

import java.util.Optional;

public class ResizeElementScreen extends Screen {

    private final Screen parentScreen;
    private final DraggableElement elementToResize;

    // Edit Boxes
    private EditBox bgWidthBox;
    private EditBox bgHeightBox;
    private EditBox barWidthBox;
    private EditBox barHeightBox; // Max 6
    private EditBox overlayWidthBox;
    private EditBox overlayHeightBox;

    public ResizeElementScreen(Screen parent, DraggableElement element) {
        // I18N for title - uses format string
        super(Component.translatable("gui.dynamic_resource_bars.resize.title_format", getFriendlyElementName(element))); 
        this.parentScreen = parent;
        this.elementToResize = element;
    }

    @Override
    protected void init() {
        super.init();
        CClient config = AllConfigs.client();
        ConfigBase.ConfigInt bgWidthConf, bgHeightConf, barWidthConf, barHeightConf, overlayWidthConf, overlayHeightConf; // Use full type

        // Get the correct config fields based on the element
        switch (elementToResize) {
            case HEALTH_BAR: 
                bgWidthConf = config.healthBackgroundWidth; bgHeightConf = config.healthBackgroundHeight;
                barWidthConf = config.healthBarWidth; barHeightConf = config.healthBarHeight;
                overlayWidthConf = config.healthOverlayWidth; overlayHeightConf = config.healthOverlayHeight;
                break;
            case STAMINA_BAR:
                bgWidthConf = config.staminaBackgroundWidth; bgHeightConf = config.staminaBackgroundHeight;
                barWidthConf = config.staminaBarWidth; barHeightConf = config.staminaBarHeight;
                overlayWidthConf = config.staminaOverlayWidth; overlayHeightConf = config.staminaOverlayHeight;
                break;
            case MANA_BAR:
                bgWidthConf = config.manaBackgroundWidth; bgHeightConf = config.manaBackgroundHeight;
                barWidthConf = config.manaBarWidth; barHeightConf = config.manaBarHeight;
                overlayWidthConf = config.manaOverlayWidth; overlayHeightConf = config.manaOverlayHeight;
                break;
            default: return; // Should not happen
        }

        int boxWidth = 50;
        int boxHeight = 20;
        int labelWidth = 100; // Width allocated for labels
        int startX = (this.width / 2) - (labelWidth + boxWidth + 5) / 2; // Center the label+box pair
        int currentY = 40; // Start below title
        int rowSpacing = 5;

        // Background Width/Height
        // Use empty Component for EditBox label, we draw it manually
        bgWidthBox = createIntEditBox(startX + labelWidth + 5, currentY, boxWidth, boxHeight, bgWidthConf);
        bgHeightBox = createIntEditBox(startX + labelWidth + 5, currentY + boxHeight + rowSpacing, boxWidth, boxHeight, bgHeightConf);
        this.addRenderableWidget(bgWidthBox);
        this.addRenderableWidget(bgHeightBox);
        currentY += 2 * (boxHeight + rowSpacing);

        // Bar Width/Height
        barWidthBox = createIntEditBox(startX + labelWidth + 5, currentY, boxWidth, boxHeight, barWidthConf);
        barHeightBox = createIntEditBox(startX + labelWidth + 5, currentY + boxHeight + rowSpacing, boxWidth, boxHeight, barHeightConf, 6); // Max 6
        this.addRenderableWidget(barWidthBox);
        this.addRenderableWidget(barHeightBox);
        currentY += 2 * (boxHeight + rowSpacing);

        // Overlay Width/Height
        overlayWidthBox = createIntEditBox(startX + labelWidth + 5, currentY, boxWidth, boxHeight, overlayWidthConf);
        overlayHeightBox = createIntEditBox(startX + labelWidth + 5, currentY + boxHeight + rowSpacing, boxWidth, boxHeight, overlayHeightConf);
        this.addRenderableWidget(overlayWidthBox);
        this.addRenderableWidget(overlayHeightBox);
        // currentY += 2 * (boxHeight + rowSpacing);

        int doneButtonWidth = 100;
        // Done Button (Uses standard MC key)
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                (button) -> this.onClose())
                .bounds((this.width / 2) - (doneButtonWidth / 2), this.height - boxHeight - 20, doneButtonWidth, boxHeight)
                .build());
    }

    // Helper uses ConfigBase.ConfigInt
    private EditBox createIntEditBox(int x, int y, int width, int height, ConfigBase.ConfigInt configInt) {
        return createIntEditBox(x, y, width, height, configInt, Integer.MAX_VALUE);
    }

    // Helper uses ConfigBase.ConfigInt
    private EditBox createIntEditBox(int x, int y, int width, int height, ConfigBase.ConfigInt configInt, int maxValue) {
        // Pass empty component to EditBox constructor, we draw labels manually
        EditBox editBox = new EditBox(this.font, x, y, width, height, Component.empty()); 
        editBox.setValue(String.valueOf(configInt.get()));
        editBox.setResponder((text) -> {
            try {
                int value = Integer.parseInt(text);
                if (value >= 0 && value <= maxValue) {
                    configInt.set(value);
                    editBox.setTextColor(0xE0E0E0);
                } else {
                    editBox.setTextColor(0xFF5555);
                }
            } catch (NumberFormatException e) {
                 editBox.setTextColor(0xFF5555);
            }
        });
        return editBox;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics); 
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        // Draw labels - Use translatable keys
        if (bgWidthBox != null) { // Check if init completed
            int labelX = bgWidthBox.getX() - 105; // Position label left of the boxes
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.background_width"), labelX, bgWidthBox.getY() + (bgWidthBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.background_height"), labelX, bgHeightBox.getY() + (bgHeightBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.bar_width"), labelX, barWidthBox.getY() + (barWidthBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.bar_height"), labelX, barHeightBox.getY() + (barHeightBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.overlay_width"), labelX, overlayWidthBox.getY() + (overlayWidthBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.resize.label.overlay_height"), labelX, overlayHeightBox.getY() + (overlayHeightBox.getHeight() - this.font.lineHeight) / 2, 0xFFFFFF);
        }

        super.render(graphics, mouseX, mouseY, partialTicks); // Render EditBoxes + Done button
    }

    // Override mouseClicked to ensure focus changes correctly
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // First, let children (EditBoxes, Buttons) handle the click
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        // If click wasn't on a child, unfocus any focused EditBox
        // This logic might need refinement based on specific EditBox behavior
        this.children().stream()
            .filter(c -> c instanceof EditBox)
            .forEach(c -> ((EditBox)c).setFocused(false));
        return false; 
    }

    // Basic keyboard handling (Tab navigation, maybe Enter)
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // TODO: Implement Tab/Shift+Tab navigation between EditBoxes
        // TODO: Maybe handle Enter key to confirm/close?
        // Let focused EditBox handle typing first
        if (this.getFocused() != null && this.getFocused().keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        // Values should be saved by EditBox responders already
        this.minecraft.setScreen(this.parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false; 
    }

    // Helper to get friendly element name (similar to HudEditorScreen)
    private static String getFriendlyElementName(DraggableElement element) {
        if (element == null) return "";
        // These should match the keys used in HudEditorScreen for consistency
        return switch (element) {
            case HEALTH_BAR -> Component.translatable("gui.dynamic_resource_bars.element.health_bar").getString();
            case MANA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.mana_bar").getString();
            case STAMINA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.stamina_bar").getString();
            default -> element.name();
        };
    }
} 