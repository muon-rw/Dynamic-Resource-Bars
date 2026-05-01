package dev.muon.dynamic_resource_bars.config.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/** Yes/No confirmation dialog used before destructive resets. */
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
        int buttonY = this.height / 2 + 20;

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.yes"),
                        b -> { confirmAction.run(); onClose(); })
                .bounds(startX, buttonY, buttonWidth, buttonHeight)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.translatable("gui.no"),
                        b -> onClose())
                .bounds(startX + buttonWidth + spacing, buttonY, buttonWidth, buttonHeight)
                .build());
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        graphics.centeredText(this.font, this.title, this.width / 2, this.height / 2 - 20, 0xFFFFFFFF);
        graphics.centeredText(this.font, this.explanation, this.width / 2, this.height / 2 - 5, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.parentScreen);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
