package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ModConfigScreen extends Screen {

    private final Screen parentScreen; // The screen that opened this one (e.g., mods list)
    
    // Text customization fields
    private EditBox textColorBox;
    private EditBox textOpacityBox;
    private EditBox textSizeBox;

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

        currentY += buttonHeight + 15; // Extra spacing before text settings
        
        // Text customization section
        ClientConfig config = ModConfigManager.getClient();
        
        // Text Color
        int labelWidth = 80;
        int editBoxWidth = 60;
        int totalWidth = labelWidth + editBoxWidth + 5;
        int startX = centerX - totalWidth / 2;
        
        textColorBox = new EditBox(this.font, startX + labelWidth + 5, currentY, editBoxWidth, buttonHeight, 
                Component.literal(String.format("#%06X", config.globalTextColor & 0xFFFFFF)));
        textColorBox.setValue(String.format("%06X", config.globalTextColor & 0xFFFFFF));
        textColorBox.setMaxLength(6);
        textColorBox.setResponder((text) -> {
            try {
                int color = Integer.parseInt(text, 16);
                config.globalTextColor = color | 0xFF000000; // Ensure full alpha
                textColorBox.setTextColor(0xE0E0E0);
            } catch (NumberFormatException e) {
                textColorBox.setTextColor(0xFF5555);
            }
        });
        this.addRenderableWidget(textColorBox);
        
        currentY += buttonHeight + 5;
        
        // Text Opacity
        textOpacityBox = new EditBox(this.font, startX + labelWidth + 5, currentY, editBoxWidth, buttonHeight, 
                Component.literal(String.valueOf(config.globalTextOpacity)));
        textOpacityBox.setValue(String.valueOf(config.globalTextOpacity));
        textOpacityBox.setMaxLength(3);
        textOpacityBox.setResponder((text) -> {
            try {
                int opacity = Integer.parseInt(text);
                if (opacity >= 0 && opacity <= 255) {
                    config.globalTextOpacity = opacity;
                    textOpacityBox.setTextColor(0xE0E0E0);
                } else {
                    textOpacityBox.setTextColor(0xFF5555);
                }
            } catch (NumberFormatException e) {
                textOpacityBox.setTextColor(0xFF5555);
            }
        });
        this.addRenderableWidget(textOpacityBox);
        
        currentY += buttonHeight + 5;
        
        // Text Size
        textSizeBox = new EditBox(this.font, startX + labelWidth + 5, currentY, editBoxWidth, buttonHeight, 
                Component.literal(String.format("%.1f", config.globalTextSize)));
        textSizeBox.setValue(String.format("%.1f", config.globalTextSize));
        textSizeBox.setMaxLength(4);
        textSizeBox.setResponder((text) -> {
            try {
                float size = Float.parseFloat(text);
                if (size >= 0.1f && size <= 5.0f) {
                    config.globalTextSize = size;
                    textSizeBox.setTextColor(0xE0E0E0);
                } else {
                    textSizeBox.setTextColor(0xFF5555);
                }
            } catch (NumberFormatException e) {
                textSizeBox.setTextColor(0xFF5555);
            }
        });
        this.addRenderableWidget(textSizeBox);

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
        
        // Draw labels for text customization
        if (textColorBox != null) {
            int labelX = textColorBox.getX() - 95;
            int labelY = textColorBox.getY() + (textColorBox.getHeight() - this.font.lineHeight) / 2;
            
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.config.label.text_color"), 
                    labelX, labelY, 0xFFFFFF);
            
            labelY = textOpacityBox.getY() + (textOpacityBox.getHeight() - this.font.lineHeight) / 2;
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.config.label.text_opacity"), 
                    labelX, labelY, 0xFFFFFF);
            
            labelY = textSizeBox.getY() + (textSizeBox.getHeight() - this.font.lineHeight) / 2;
            graphics.drawString(this.font, Component.translatable("gui.dynamic_resource_bars.config.label.text_size"), 
                    labelX, labelY, 0xFFFFFF);
        }
    }

    @Override
    public void onClose() {    
        // Save the configuration when closing
        ClientConfig.getInstance().save();
        
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }
} 