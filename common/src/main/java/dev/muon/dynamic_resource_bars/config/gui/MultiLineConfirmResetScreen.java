package dev.muon.dynamic_resource_bars.config.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/** Multi-line variant of {@link ConfirmResetScreen} for longer explanations. */
public class MultiLineConfirmResetScreen extends Screen {

    private static final int LINE_SPACING = 12;

    private final Screen parentScreen;
    private final List<Component> lines;
    private final Runnable confirmAction;

    public MultiLineConfirmResetScreen(Screen parent, Component title, List<Component> lines, Runnable confirmAction) {
        super(title);
        this.parentScreen = parent;
        this.lines = List.copyOf(lines);
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
        int buttonY = (this.height / 2) + (lines.size() * LINE_SPACING / 2) + 16;

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
        int titleY = (this.height / 2) - (lines.size() * LINE_SPACING / 2) - 24;
        graphics.centeredText(this.font, this.title, this.width / 2, titleY, 0xFFFFFFFF);
        int y = titleY + 18;
        for (Component line : lines) {
            graphics.centeredText(this.font, line, this.width / 2, y, 0xFFFFFFFF);
            y += LINE_SPACING;
        }
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
