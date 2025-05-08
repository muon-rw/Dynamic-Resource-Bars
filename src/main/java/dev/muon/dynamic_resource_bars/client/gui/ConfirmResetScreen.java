package dev.muon.dynamic_resource_bars.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ConfirmResetScreen extends Screen {

    private final Screen parentScreen;
    private final Component explanation;
    private final Runnable confirmAction;

    public ConfirmResetScreen(Screen parent, Component title, Component explanation, Runnable confirmAction) {
        super(title);
        this.parentScreen = parent;
        this.explanation = explanation;
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
        int buttonY = this.height / 2 + 20; // Position buttons below the text

        // Confirm Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.yes"), // Use standard "Yes" translation key
                (button) -> {
                    this.confirmAction.run();
                    this.onClose();
                })
                .bounds(startX, buttonY, buttonWidth, buttonHeight)
                .build());

        // Cancel Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.no"), // Use standard "No" translation key
                (button) -> this.onClose())
                .bounds(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Dark background
        this.renderBackground(graphics);

        // Title and Explanation (centered)
        graphics.drawCenteredString(this.font, this.title, this.width / 2, this.height / 2 - 20, 0xFFFFFF);
        graphics.drawCenteredString(this.font, this.explanation, this.width / 2, this.height / 2 - 5, 0xFFFFFF);

        // Render widgets (buttons)
        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
             this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true; // Allow closing with Esc
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Doesn't pause
    }
} 