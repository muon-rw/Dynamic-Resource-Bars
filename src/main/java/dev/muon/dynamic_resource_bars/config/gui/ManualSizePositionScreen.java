package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

/**
 * Screen for manually editing sub-element size and position values
 */
public class ManualSizePositionScreen extends Screen {
    private final Screen parentScreen;
    private final DraggableElement element;
    private final SubElementType subElement;
    
    private EditBox xOffsetBox;
    private EditBox yOffsetBox;
    private EditBox widthBox;
    private EditBox heightBox;

    public ManualSizePositionScreen(Screen parent, DraggableElement element, SubElementType subElement) {
        super(Component.translatable("gui.dynamic_resource_bars.manual_size_position.title"));
        this.parentScreen = parent;
        this.element = element;
        this.subElement = subElement;
    }

    @Override
    protected void init() {
        super.init();
        
        ClientConfig config = ModConfigManager.getClient();
        
        // Get current values
        int[] values = getCurrentValues(config);
        int xOffset = values[0];
        int yOffset = values[1];
        int width = values[2];
        int height = values[3];
        
        int centerX = this.width / 2;
        int startY = this.height / 4;
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
        
        // Width (only for resizable sub-elements)
        if (hasWidth(subElement)) {
            this.addRenderableWidget(Button.builder(
                Component.translatable("gui.dynamic_resource_bars.manual.width"),
                (btn) -> {}).bounds(centerX - 150, startY + spacing * 2, 60, boxHeight).build()).active = false;
            
            widthBox = new EditBox(this.font, centerX - 80, startY + spacing * 2, boxWidth, boxHeight, Component.empty());
            widthBox.setValue(String.valueOf(width));
            widthBox.setMaxLength(5);
            this.addRenderableWidget(widthBox);
        }
        
        // Height (only for resizable sub-elements)
        if (hasHeight(subElement)) {
            int heightY = hasWidth(subElement) ? startY + spacing * 3 : startY + spacing * 2;
            this.addRenderableWidget(Button.builder(
                Component.translatable("gui.dynamic_resource_bars.manual.height"),
                (btn) -> {}).bounds(centerX - 150, heightY, 60, boxHeight).build()).active = false;
            
            heightBox = new EditBox(this.font, centerX - 80, heightY, boxWidth, boxHeight, Component.empty());
            heightBox.setValue(String.valueOf(height));
            heightBox.setMaxLength(5);
            this.addRenderableWidget(heightBox);
        }
        
        // Apply and Cancel buttons
        int buttonY = this.height - 40;
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            (btn) -> {
                applyValues();
                this.onClose();
            }).bounds(centerX - 110, buttonY, 100, 20).build());
        
        this.addRenderableWidget(Button.builder(
            Component.translatable("gui.cancel"),
            (btn) -> this.onClose()).bounds(centerX + 10, buttonY, 100, 20).build());
    }

    private boolean hasWidth(SubElementType subElement) {
        return subElement == SubElementType.BACKGROUND || 
               subElement == SubElementType.BAR_MAIN || 
               subElement == SubElementType.FOREGROUND;
    }
    
    private boolean hasHeight(SubElementType subElement) {
        return subElement == SubElementType.BACKGROUND || 
               subElement == SubElementType.BAR_MAIN || 
               subElement == SubElementType.FOREGROUND;
    }

    private int[] getCurrentValues(ClientConfig config) {
        // Returns [xOffset, yOffset, width, height]
        return switch (element) {
            case HEALTH_BAR -> switch (subElement) {
                case BACKGROUND -> new int[]{config.healthBackgroundXOffset, config.healthBackgroundYOffset, config.healthBackgroundWidth, config.healthBackgroundHeight};
                case BAR_MAIN -> new int[]{config.healthBarXOffset, config.healthBarYOffset, config.healthBarWidth, config.healthBarHeight};
                case FOREGROUND -> new int[]{config.healthOverlayXOffset, config.healthOverlayYOffset, config.healthOverlayWidth, config.healthOverlayHeight};
                case TEXT -> new int[]{config.healthTextXOffset, config.healthTextYOffset, 0, 0};
                case ABSORPTION_TEXT -> new int[]{config.healthAbsorptionTextXOffset, config.healthAbsorptionTextYOffset, 0, 0};
                default -> new int[]{0, 0, 0, 0};
            };
            case STAMINA_BAR -> switch (subElement) {
                case BACKGROUND -> new int[]{config.staminaBackgroundXOffset, config.staminaBackgroundYOffset, config.staminaBackgroundWidth, config.staminaBackgroundHeight};
                case BAR_MAIN -> new int[]{config.staminaBarXOffset, config.staminaBarYOffset, config.staminaBarWidth, config.staminaBarHeight};
                case FOREGROUND -> new int[]{config.staminaOverlayXOffset, config.staminaOverlayYOffset, config.staminaOverlayWidth, config.staminaOverlayHeight};
                case TEXT -> new int[]{config.staminaTextXOffset, config.staminaTextYOffset, 0, 0};
                default -> new int[]{0, 0, 0, 0};
            };
            case MANA_BAR -> switch (subElement) {
                case BACKGROUND -> new int[]{config.manaBackgroundXOffset, config.manaBackgroundYOffset, config.manaBackgroundWidth, config.manaBackgroundHeight};
                case BAR_MAIN -> new int[]{config.manaBarXOffset, config.manaBarYOffset, config.manaBarWidth, config.manaBarHeight};
                case FOREGROUND -> new int[]{config.manaOverlayXOffset, config.manaOverlayYOffset, config.manaOverlayWidth, config.manaOverlayHeight};
                case TEXT -> new int[]{config.manaTextXOffset, config.manaTextYOffset, 0, 0};
                default -> new int[]{0, 0, 0, 0};
            };
            case ARMOR_BAR -> switch (subElement) {
                case BACKGROUND -> new int[]{config.armorBackgroundXOffset, config.armorBackgroundYOffset, config.armorBackgroundWidth, config.armorBackgroundHeight};
                case BAR_MAIN -> new int[]{config.armorBarXOffset, config.armorBarYOffset, config.armorBarWidth, config.armorBarHeight};
                case TEXT -> new int[]{config.armorTextXOffset, config.armorTextYOffset, 0, 0};
                case ICON -> new int[]{config.armorIconXOffset, config.armorIconYOffset, 0, 0};
                default -> new int[]{0, 0, 0, 0};
            };
            case AIR_BAR -> switch (subElement) {
                case BACKGROUND -> new int[]{config.airBackgroundXOffset, config.airBackgroundYOffset, config.airBackgroundWidth, config.airBackgroundHeight};
                case BAR_MAIN -> new int[]{config.airBarXOffset, config.airBarYOffset, config.airBarWidth, config.airBarHeight};
                case TEXT -> new int[]{config.airTextXOffset, config.airTextYOffset, 0, 0};
                case ICON -> new int[]{config.airIconXOffset, config.airIconYOffset, 0, 0};
                default -> new int[]{0, 0, 0, 0};
            };
        };
    }

    private void applyValues() {
        try {
            int xOffset = Integer.parseInt(xOffsetBox.getValue());
            int yOffset = Integer.parseInt(yOffsetBox.getValue());
            int width = hasWidth(subElement) && widthBox != null ? Integer.parseInt(widthBox.getValue()) : 0;
            int height = hasHeight(subElement) && heightBox != null ? Integer.parseInt(heightBox.getValue()) : 0;
            
            ClientConfig config = ModConfigManager.getClient();
            
            switch (element) {
                case HEALTH_BAR:
                    switch (subElement) {
                        case BACKGROUND:
                            config.healthBackgroundXOffset = xOffset;
                            config.healthBackgroundYOffset = yOffset;
                            config.healthBackgroundWidth = width;
                            config.healthBackgroundHeight = height;
                            break;
                        case BAR_MAIN:
                            config.healthBarXOffset = xOffset;
                            config.healthBarYOffset = yOffset;
                            config.healthBarWidth = width;
                            config.healthBarHeight = height;
                            break;
                        case FOREGROUND:
                            config.healthOverlayXOffset = xOffset;
                            config.healthOverlayYOffset = yOffset;
                            config.healthOverlayWidth = width;
                            config.healthOverlayHeight = height;
                            break;
                        case TEXT:
                            config.healthTextXOffset = xOffset;
                            config.healthTextYOffset = yOffset;
                            break;
                        case ABSORPTION_TEXT:
                            config.healthAbsorptionTextXOffset = xOffset;
                            config.healthAbsorptionTextYOffset = yOffset;
                            break;
                    }
                    break;
                case STAMINA_BAR:
                    switch (subElement) {
                        case BACKGROUND:
                            config.staminaBackgroundXOffset = xOffset;
                            config.staminaBackgroundYOffset = yOffset;
                            config.staminaBackgroundWidth = width;
                            config.staminaBackgroundHeight = height;
                            break;
                        case BAR_MAIN:
                            config.staminaBarXOffset = xOffset;
                            config.staminaBarYOffset = yOffset;
                            config.staminaBarWidth = width;
                            config.staminaBarHeight = height;
                            break;
                        case FOREGROUND:
                            config.staminaOverlayXOffset = xOffset;
                            config.staminaOverlayYOffset = yOffset;
                            config.staminaOverlayWidth = width;
                            config.staminaOverlayHeight = height;
                            break;
                        case TEXT:
                            config.staminaTextXOffset = xOffset;
                            config.staminaTextYOffset = yOffset;
                            break;
                    }
                    break;
                case MANA_BAR:
                    switch (subElement) {
                        case BACKGROUND:
                            config.manaBackgroundXOffset = xOffset;
                            config.manaBackgroundYOffset = yOffset;
                            config.manaBackgroundWidth = width;
                            config.manaBackgroundHeight = height;
                            break;
                        case BAR_MAIN:
                            config.manaBarXOffset = xOffset;
                            config.manaBarYOffset = yOffset;
                            config.manaBarWidth = width;
                            config.manaBarHeight = height;
                            break;
                        case FOREGROUND:
                            config.manaOverlayXOffset = xOffset;
                            config.manaOverlayYOffset = yOffset;
                            config.manaOverlayWidth = width;
                            config.manaOverlayHeight = height;
                            break;
                        case TEXT:
                            config.manaTextXOffset = xOffset;
                            config.manaTextYOffset = yOffset;
                            break;
                    }
                    break;
                case ARMOR_BAR:
                    switch (subElement) {
                        case BACKGROUND:
                            config.armorBackgroundXOffset = xOffset;
                            config.armorBackgroundYOffset = yOffset;
                            config.armorBackgroundWidth = width;
                            config.armorBackgroundHeight = height;
                            break;
                        case BAR_MAIN:
                            config.armorBarXOffset = xOffset;
                            config.armorBarYOffset = yOffset;
                            config.armorBarWidth = width;
                            config.armorBarHeight = height;
                            break;
                        case TEXT:
                            config.armorTextXOffset = xOffset;
                            config.armorTextYOffset = yOffset;
                            break;
                        case ICON:
                            config.armorIconXOffset = xOffset;
                            config.armorIconYOffset = yOffset;
                            break;
                    }
                    break;
                case AIR_BAR:
                    switch (subElement) {
                        case BACKGROUND:
                            config.airBackgroundXOffset = xOffset;
                            config.airBackgroundYOffset = yOffset;
                            config.airBackgroundWidth = width;
                            config.airBackgroundHeight = height;
                            break;
                        case BAR_MAIN:
                            config.airBarXOffset = xOffset;
                            config.airBarYOffset = yOffset;
                            config.airBarWidth = width;
                            config.airBarHeight = height;
                            break;
                        case TEXT:
                            config.airTextXOffset = xOffset;
                            config.airTextYOffset = yOffset;
                            break;
                        case ICON:
                            config.airIconXOffset = xOffset;
                            config.airIconYOffset = yOffset;
                            break;
                    }
                    break;
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
        
        // Show element and sub-element names
        Component elementName = switch (element) {
            case HEALTH_BAR -> Component.translatable("gui.dynamic_resource_bars.element.health");
            case STAMINA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.stamina");
            case MANA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.mana");
            case ARMOR_BAR -> Component.translatable("gui.dynamic_resource_bars.element.armor");
            case AIR_BAR -> Component.translatable("gui.dynamic_resource_bars.element.air");
        };
        
        Component subElementName = switch (subElement) {
            case BACKGROUND -> Component.translatable("gui.dynamic_resource_bars.subelement.background");
            case BAR_MAIN -> Component.translatable("gui.dynamic_resource_bars.subelement.bar_main");
            case FOREGROUND -> Component.translatable("gui.dynamic_resource_bars.subelement.foreground");
            case TEXT -> Component.translatable("gui.dynamic_resource_bars.subelement.text");
            case ICON -> Component.translatable("gui.dynamic_resource_bars.subelement.icon");
            case ABSORPTION_TEXT -> Component.translatable("gui.dynamic_resource_bars.subelement.absorption_text");
        };
        
        graphics.drawCenteredString(this.font, 
            Component.literal("").append(elementName).append(" - ").append(subElementName), 
            this.width / 2, 40, 0xAAAAAA);
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

