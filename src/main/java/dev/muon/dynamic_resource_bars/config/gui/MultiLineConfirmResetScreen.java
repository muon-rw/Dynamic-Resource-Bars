package dev.muon.dynamic_resource_bars.config.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Confirmation screen with multi-line warning for reset operations
 */
public class MultiLineConfirmResetScreen extends Screen {

    private final Screen parentScreen;
    private final Runnable confirmAction;

    public MultiLineConfirmResetScreen(Screen parent, Component title, Runnable confirmAction) {
        super(title);
        this.parentScreen = parent;
        this.confirmAction = confirmAction;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 100;
        int buttonHeight = 20;
        int spacing = 10;
        int totalWidth = 2 * buttonWidth + spacing;
        int startX = (this.width - totalWidth) / 2;
        int buttonY = this.height / 2 + 40; // Position buttons below the text

        // Confirm Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.yes"),
                (button) -> {
                    this.confirmAction.run();
                    this.onClose();
                })
                .bounds(startX, buttonY, buttonWidth, buttonHeight)
                .build());

        // Cancel Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.no"),
                (button) -> this.onClose())
                .bounds(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        #if NEWER_THAN_20_1
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        #else
        this.renderBackground(graphics);
        #endif

        super.render(graphics, mouseX, mouseY, partialTicks);

        // Title
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 40, 0xFFFFFF);
        
        // Line 1 (normal text)
        Component line1 = Component.translatable("gui.dynamic_resource_bars.confirm.reset_element.line1");
        graphics.drawCenteredString(this.font, line1, this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        
        // Line 2 (warning - bold red)
        Component line2 = Component.translatable("gui.dynamic_resource_bars.confirm.reset_element.line2");
        graphics.drawCenteredString(this.font, line2, this.width / 2, this.height / 2 - 5, 0xFF4444);
        
        // Line 3 (warning continued - bold red)
        Component line3 = Component.translatable("gui.dynamic_resource_bars.confirm.reset_element.line3");
        graphics.drawCenteredString(this.font, line3, this.width / 2, this.height / 2 + 10, 0xFF4444);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
             this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

