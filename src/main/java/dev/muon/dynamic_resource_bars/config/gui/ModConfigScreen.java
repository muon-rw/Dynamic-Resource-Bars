package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModConfigScreen extends Screen {

    private final Screen parentScreen; // The screen that opened this one (e.g., mods list)

    public ModConfigScreen(Screen parent) {
        super(Component.literal(DynamicResourceBars.MODNAME + " Configuration"));
        this.parentScreen = parent;
    }

    @Override
    protected void init() {
        super.init();

        int buttonWidth = 200;
        int buttonHeight = 20;
        int centerX = this.width / 2;
        int currentY = this.height / 4;

        Minecraft mc = Minecraft.getInstance();
        boolean canEditHUD = mc.level != null;

        Button editHudButton = Button.builder(
                Component.translatable("gui.dynamic_resource_bars.config.button.open_hud_editor"),
                (button) -> {
                    if (button.active) {
                        this.minecraft.setScreen(new HudEditorScreen(this));
                    }
                })
                .bounds(centerX - buttonWidth / 2, currentY, buttonWidth, buttonHeight)
                .build();
        
        editHudButton.active = canEditHUD; 

        if (!canEditHUD) {
             editHudButton.setTooltip(Tooltip.create(Component.translatable("gui.dynamic_resource_bars.config.tooltip.hud_editor_disabled")));
        }

        this.addRenderableWidget(editHudButton);

        currentY += buttonHeight + 5;

        // Done Button
        this.addRenderableWidget(Button.builder(
                Component.translatable("gui.done"),
                (button) -> this.onClose())
                .bounds(centerX - buttonWidth / 2, this.height - buttonHeight - 20, buttonWidth, buttonHeight)
                .build());
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
    }

    @Override
    public void onClose() {    if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
} 