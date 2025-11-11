package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Screen for manually editing main element position offsets
 */
public class ManualPositionScreen extends Screen {
    private final Screen parentScreen;
    private final DraggableElement element;
    
    private EditBox xOffsetBox;
    private EditBox yOffsetBox;

    public ManualPositionScreen(Screen parent, DraggableElement element) {
        super(Component.translatable("gui.dynamic_resource_bars.manual_position.title"));
        this.parentScreen = parent;
        this.element = element;
    }

    @Override
    protected void init() {
        super.init();
        
        ClientConfig config = ModConfigManager.getClient();
        
        // Get current values
        int[] offsets = getCurrentOffsets(config);
        int xOffset = offsets[0];
        int yOffset = offsets[1];
        
        int centerX = this.width / 2;
        int startY = this.height / 3;
        int boxWidth = 80;
        int boxHeight = 20;
        int spacing = 30;
        
        // X Offset
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.dynamic_resource_bars.manual.x_offset"),
            (btn) -> {}).bounds(centerX - 150, startY, 60, boxHeight).build()).active = false;
        
        xOffsetBox = new EditBox(this.font, centerX - 80, startY, boxWidth, boxHeight, Component.empty());
        xOffsetBox.setValue(String.valueOf(xOffset));
        xOffsetBox.setMaxLength(5);
        this.addRenderableWidget(xOffsetBox);
        
        // Y Offset
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.dynamic_resource_bars.manual.y_offset"),
            (btn) -> {}).bounds(centerX - 150, startY + spacing, 60, boxHeight).build()).active = false;
        
        yOffsetBox = new EditBox(this.font, centerX - 80, startY + spacing, boxWidth, boxHeight, Component.empty());
        yOffsetBox.setValue(String.valueOf(yOffset));
        yOffsetBox.setMaxLength(5);
        this.addRenderableWidget(yOffsetBox);
        
        // Apply and Cancel buttons
        int buttonY = this.height - 40;
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            (btn) -> {
                applyOffsets();
                this.onClose();
            }).bounds(centerX - 110, buttonY, 100, 20).build());
        
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.cancel"),
            (btn) -> this.onClose()).bounds(centerX + 10, buttonY, 100, 20).build());
    }

    private int[] getCurrentOffsets(ClientConfig config) {
        // Returns [xOffset, yOffset]
        return switch (element) {
            case HEALTH_BAR -> new int[]{config.healthTotalXOffset, config.healthTotalYOffset};
            case STAMINA_BAR -> new int[]{config.staminaTotalXOffset, config.staminaTotalYOffset};
            case MANA_BAR -> new int[]{config.manaTotalXOffset, config.manaTotalYOffset};
            case ARMOR_BAR -> new int[]{config.armorTotalXOffset, config.armorTotalYOffset};
            case AIR_BAR -> new int[]{config.airTotalXOffset, config.airTotalYOffset};
        };
    }

    private void applyOffsets() {
        try {
            int xOffset = Integer.parseInt(xOffsetBox.getValue());
            int yOffset = Integer.parseInt(yOffsetBox.getValue());
            
            ClientConfig config = ModConfigManager.getClient();
            switch (element) {
                case HEALTH_BAR -> {
                    config.healthTotalXOffset = xOffset;
                    config.healthTotalYOffset = yOffset;
                }
                case STAMINA_BAR -> {
                    config.staminaTotalXOffset = xOffset;
                    config.staminaTotalYOffset = yOffset;
                }
                case MANA_BAR -> {
                    config.manaTotalXOffset = xOffset;
                    config.manaTotalYOffset = yOffset;
                }
                case ARMOR_BAR -> {
                    config.armorTotalXOffset = xOffset;
                    config.armorTotalYOffset = yOffset;
                }
                case AIR_BAR -> {
                    config.airTotalXOffset = xOffset;
                    config.airTotalYOffset = yOffset;
                }
            }
        } catch (NumberFormatException e) {
            // Ignore invalid input
        }
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        #if NEWER_THAN_20_1
        this.renderBackground(graphics, mouseX, mouseY, partialTicks);
        #else
        this.renderBackground(graphics);
        #endif

        super.render(graphics, mouseX, mouseY, partialTicks);
        
        graphics.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);
        
        Component elementName = switch (element) {
            case HEALTH_BAR -> Component.translatable("gui.dynamic_resource_bars.element.health");
            case STAMINA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.stamina");
            case MANA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.mana");
            case ARMOR_BAR -> Component.translatable("gui.dynamic_resource_bars.element.armor");
            case AIR_BAR -> Component.translatable("gui.dynamic_resource_bars.element.air");
        };
        
        graphics.drawCenteredString(this.font, elementName, this.width / 2, 40, 0xAAAAAA);
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parentScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

