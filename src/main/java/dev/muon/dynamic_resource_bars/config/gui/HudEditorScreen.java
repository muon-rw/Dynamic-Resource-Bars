package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.AirBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.*;
import dev.muon.dynamic_resource_bars.compat.ManaProviderManager;
import dev.muon.dynamic_resource_bars.compat.StaminaProviderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import java.util.ArrayList;
import java.util.List;

public class HudEditorScreen extends Screen {

    private static final int HELP_TEXT_TOP_Y = 15;
    private static final int LINE_SPACING = 2;
    private static final int HANDLE_SIZE = 3; // Adjusted handle size
    private static final int HANDLE_HOVER_COLOR = 0xAAFFFF00; // Color when hovering over handle (semi-transparent yellow)

    private enum ResizeMode {
        NONE,
        WIDTH, // Resizing width (from the right handle)
        HEIGHT // Resizing height (from the bottom handle)
    }

    private ResizeMode currentResizeMode = ResizeMode.NONE;
    private int resizeStartX;
    private int resizeStartY;
    private int initialWidth;
    private int initialHeight;
    private SubElementType resizingSubElement = null; // Renamed for clarity
    private DraggableElement focusedElementForResize = null; // Store the focused element during resize

    private Screen previousScreen;
    private long lastClickTime = 0;
    private double lastClickX = 0;
    private double lastClickY = 0;
    private static final int DOUBLE_CLICK_TIME_MS = 300; 

    // Undo state for last whole element move
    private DraggableElement lastDraggedElementForUndo = null;
    private int lastDragInitialXOffset;
    private int lastDragInitialYOffset;
    private boolean canUndoLastDrag = false;

    // Undo state for last sub-element move
    private DraggableElement lastFocusedElementForSubUndo = null;
    private SubElementType lastDraggedSubElementForUndo = null;
    private int lastSubDragInitialXOffset;
    private int lastSubDragInitialYOffset;
    private boolean canUndoLastSubDrag = false;

    // Button for Non-Focus Mode (Grid)
    private Button resetButtonForAllBars;

    // Context menu
    private ContextMenu activeContextMenu = null;
    private DraggableElement contextMenuElement = null;
    private SubElementType contextMenuSubElement = null;
    private int contextMenuX = 0;
    private int contextMenuY = 0;

    public HudEditorScreen(Screen previous) {
        super(Component.translatable("gui.dynamic_resource_bars.hud_editor.title"));
        this.previousScreen = previous;
    }

    @Override
    protected void init() {
        super.init();
        if (!EditModeManager.isEditModeEnabled()) {
            EditModeManager.toggleEditMode(); 
        }
        rebuildEditorWidgets();
        this.canUndoLastDrag = false;
        this.lastDraggedElementForUndo = null;
        this.canUndoLastSubDrag = false;
        this.lastFocusedElementForSubUndo = null;
        this.lastDraggedSubElementForUndo = null;
    }

    private void rebuildEditorWidgets() {
        DraggableElement focused = EditModeManager.getFocusedElement();
        ClientConfig config = ModConfigManager.getClient(); 
        int fontHeight = Minecraft.getInstance().font.lineHeight;

        clearWidgets();
        resetButtonForAllBars = null;

        if (focused == null) {
            // Declarations for non-focus mode variables
            int gridButtonHeight = 20;
            int rowSpacing = 5;
            int colSpacing = 5;
            int gridTopY = 40; 
            int currentX, currentY; // Declare once

            // --- Section 1: Health, Mana, Stamina (3 columns) ---
            int threeColButtonWidth = 100; // Declare once
            int threeColContentWidth = 3 * threeColButtonWidth + 2 * colSpacing; // Declare once
            int threeColStartX = (this.width - threeColContentWidth) / 2; // Declare once

            // Reset All Button - Positioned below help text with moderate spacing
            currentY = gridTopY + (2 * (fontHeight + LINE_SPACING)) + 10; // After 4 help lines plus modest spacing
            int resetAllButtonWidth = 150; 
            resetButtonForAllBars = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.reset_all_bars"), (b) -> {
                openMultiLineConfirmScreen(
                                Component.translatable("gui.dynamic_resource_bars.confirm.reset_all.title"), 
                                this::resetAllDefaultsAction);
                })
                .bounds((this.width - resetAllButtonWidth) / 2, currentY, resetAllButtonWidth, gridButtonHeight) 
                .build();
            addRenderableWidget(resetButtonForAllBars);

        } else {
            // Focus Mode - Add "Reset All" button for the focused element
            int focusButtonWidth = 120;
            int focusButtonHeight = 20;
            
            // Estimate space needed for help text (4 lines) and header (1 line)
            int buttonsTopY = HELP_TEXT_TOP_Y + (4 * (fontHeight + LINE_SPACING)) + (fontHeight + LINE_SPACING) + 15; // helpY + 4 lines help + 1 line header + padding
            int currentY = buttonsTopY + 5; // Small padding

            final DraggableElement finalFocusedElement = focused; // Used for confirm screens and resize screen
            int currentX = (this.width - focusButtonWidth) / 2; // Center the button

            // Reset All button for focused element
            Button resetAllButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.reset_element"), (b) -> {
                Component title = Component.translatable("gui.dynamic_resource_bars.confirm.reset_element.title", getFriendlyElementName(finalFocusedElement));
                openMultiLineConfirmScreen(title, () -> {
                    resetElementToDefaults(finalFocusedElement, config);
                    rebuildEditorWidgets();
                });
            }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
            addRenderableWidget(resetAllButton);
        }
    }

    private TextBehavior getNextTextBehavior(TextBehavior current) {
        TextBehavior[] behaviors = TextBehavior.values();
        int nextIndex = (current.ordinal() + 1) % behaviors.length;
        return behaviors[nextIndex];
    }
    
    private HorizontalAlignment getNextHorizontalAlignment(HorizontalAlignment current) {
        HorizontalAlignment[] alignments = HorizontalAlignment.values();
        int nextIndex = (current.ordinal() + 1) % alignments.length;
        return alignments[nextIndex];
    }


    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        DraggableElement focused = EditModeManager.getFocusedElement();
        DraggableElement dragged = EditModeManager.getDraggedElement();
        Player player = Minecraft.getInstance().player;
        Font font = this.font;
        int fontHeight = font.lineHeight;
        int padding = 5;
        int backgroundColor = 0x90000000;

        float minWidgetX = Float.MAX_VALUE, minWidgetY = Float.MAX_VALUE;
        float maxWidgetX = Float.MIN_VALUE, maxWidgetY = Float.MIN_VALUE;
        boolean hasWidgets = false;

        // Calculate bounds of actual widgets first for background panel
        for (Renderable renderable : this.renderables) {
            if (renderable instanceof AbstractWidget widget) {
                 minWidgetX = Math.min(minWidgetX, widget.getX());
                 minWidgetY = Math.min(minWidgetY, widget.getY());
                 maxWidgetX = Math.max(maxWidgetX, widget.getX() + widget.getWidth());
                 maxWidgetY = Math.max(maxWidgetY, widget.getY() + widget.getHeight());
                 hasWidgets = true;
            }
        }

        // Now handle text rendering and adjust overall bounds for the background panel
        float overallMinX = minWidgetX, overallMinY = HELP_TEXT_TOP_Y; // Start with help text Y
        float overallMaxX = maxWidgetX, overallMaxY = minWidgetY; // Start with top-most widget Y for maxY initially
        if (!hasWidgets) { // If no widgets, use a default small panel around text
            overallMinX = (this.width / 2.0f) - 100;
            overallMaxX = (this.width / 2.0f) + 100;
            overallMaxY = HELP_TEXT_TOP_Y + 50; // Arbitrary height if no widgets
        } else {
            overallMaxY = maxWidgetY; // If widgets exist, panel goes at least to their bottom
        }

        int currentTextY = HELP_TEXT_TOP_Y;

        if (focused == null) {
            Component helpLine1 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line1");
            Component helpLine2 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line2");
            Component helpLine3 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line3");
            Component helpLine4 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line4");

            graphics.drawCenteredString(font, helpLine1, this.width / 2, currentTextY, 0xFFFFFF);
            float textWidth = font.width(helpLine1);
            overallMinX = Math.min(overallMinX, (this.width - textWidth) / 2f);
            overallMaxX = Math.max(overallMaxX, (this.width + textWidth) / 2f);
            currentTextY += fontHeight + LINE_SPACING;

            graphics.drawCenteredString(font, helpLine2, this.width / 2, currentTextY, 0xFFFFFF);
            textWidth = font.width(helpLine2);
            overallMinX = Math.min(overallMinX, (this.width - textWidth) / 2f);
            overallMaxX = Math.max(overallMaxX, (this.width + textWidth) / 2f);
            currentTextY += fontHeight + LINE_SPACING;

            graphics.drawCenteredString(font, helpLine3, this.width / 2, currentTextY, 0xAAFFAA);
            textWidth = font.width(helpLine3);
            overallMinX = Math.min(overallMinX, (this.width - textWidth) / 2f);
            overallMaxX = Math.max(overallMaxX, (this.width + textWidth) / 2f);
            currentTextY += fontHeight + LINE_SPACING;
            
            graphics.drawCenteredString(font, helpLine4, this.width / 2, currentTextY, 0x55FF55);
            textWidth = font.width(helpLine4);
            overallMinX = Math.min(overallMinX, (this.width - textWidth) / 2f);
            overallMaxX = Math.max(overallMaxX, (this.width + textWidth) / 2f);
            currentTextY += fontHeight + LINE_SPACING;

            // No explicit header for main mode, buttons start after help text based on their own layout logic.
            // So, update overallMaxY based on help text.
            overallMaxY = Math.max(overallMaxY, currentTextY);

        } else { // Focus Mode: Render help text and then the header
            Component helpFocusLine1 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line1");
            Component helpFocusLine2 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line2.sub_element");
            Component helpFocusLine3 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line3");
            Component helpFocusLine4 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line4");
            Component keyboardHelpLine = Component.literal("Tab: Select Layer | Arrows: Move | Shift+Arrows: Resize");

            graphics.drawCenteredString(font, helpFocusLine1, this.width / 2, currentTextY, 0xFFFFFF);
            float textWidth = font.width(helpFocusLine1);
            overallMinX = Math.min(overallMinX, (this.width - textWidth) / 2f);
            overallMaxX = Math.max(overallMaxX, (this.width + textWidth) / 2f);
            currentTextY += fontHeight + LINE_SPACING;

            graphics.drawCenteredString(font, helpFocusLine2, this.width / 2, currentTextY, 0xFFFFFF);
            textWidth = font.width(helpFocusLine2);
            overallMinX = Math.min(overallMinX, (this.width - textWidth) / 2f);
            overallMaxX = Math.max(overallMaxX, (this.width + textWidth) / 2f);
            currentTextY += fontHeight + LINE_SPACING;

            graphics.drawCenteredString(font, helpFocusLine3, this.width / 2, currentTextY, 0xFFFFFF);
            textWidth = font.width(helpFocusLine3);
            overallMinX = Math.min(overallMinX, (this.width - textWidth) / 2f);
            overallMaxX = Math.max(overallMaxX, (this.width + textWidth) / 2f);
            currentTextY += fontHeight + LINE_SPACING;

            graphics.drawCenteredString(font, helpFocusLine4, this.width / 2, currentTextY, 0xFFFFFF);
            textWidth = font.width(helpFocusLine4);
            overallMinX = Math.min(overallMinX, (this.width - textWidth) / 2f);
            overallMaxX = Math.max(overallMaxX, (this.width + textWidth) / 2f);
            currentTextY += fontHeight + LINE_SPACING;
            
            graphics.drawCenteredString(font, keyboardHelpLine, this.width / 2, currentTextY, 0x55FF55);
            textWidth = font.width(keyboardHelpLine);
            overallMinX = Math.min(overallMinX, (this.width - textWidth) / 2f);
            overallMaxX = Math.max(overallMaxX, (this.width + textWidth) / 2f);
            currentTextY += fontHeight + LINE_SPACING;

            currentTextY += 5; // Spacing before header
            Component headerText = Component.translatable("gui.dynamic_resource_bars.hud_editor.title_focused", getFriendlyElementName(focused));
            graphics.drawCenteredString(font, headerText, this.width / 2, currentTextY, 0xFFFFFF);
            textWidth = font.width(headerText);
            overallMinX = Math.min(overallMinX, (this.width - textWidth) / 2f);
            overallMaxX = Math.max(overallMaxX, (this.width + textWidth) / 2f);
            currentTextY += fontHeight + LINE_SPACING;

            // Ensure buttonsTopY in init is consistent with this rendered text height
            // This currentTextY is the Y *after* all text. Widgets should start at/after this.
            // The buttonsTopY in rebuildEditorWidgets should roughly match this final currentTextY or be slightly below.
            // For safety, ensure overallMaxY includes the bottom of the header.
            overallMaxY = Math.max(overallMaxY, currentTextY);
        }

        // Draw the background panel using the calculated overall bounds
        if (overallMinX <= overallMaxX && overallMinY <= overallMaxY) {
             graphics.fill((int)(overallMinX - padding), (int)(overallMinY - padding),
                           (int)(overallMaxX + padding), (int)(overallMaxY + padding), backgroundColor);
        }

        // The actual widgets (buttons) are rendered by super.render()
        // Text has already been drawn directly.

        // Dragging line visualization
        if (focused == null && dragged != null && player != null) {
            ScreenRect barRect = null; HUDPositioning.BarPlacement currentAnchor = null;
            ClientConfig currentConfig = ModConfigManager.getClient();
            switch (dragged) {
                case HEALTH_BAR: barRect = HealthBarRenderer.getScreenRect(player); currentAnchor = currentConfig.healthBarAnchor; break;
                case MANA_BAR: barRect = ManaBarRenderer.getScreenRect(player); currentAnchor = currentConfig.manaBarAnchor; break;
                case STAMINA_BAR: barRect = StaminaBarRenderer.getScreenRect(player); currentAnchor = currentConfig.staminaBarAnchor; break;
                case ARMOR_BAR: barRect = ArmorBarRenderer.getScreenRect(player); currentAnchor = currentConfig.armorBarAnchor; break;
                case AIR_BAR: barRect = AirBarRenderer.getScreenRect(player); currentAnchor = currentConfig.airBarAnchor; break;
            }
            if (barRect != null && currentAnchor != null && barRect.width() > 0 && barRect.height() > 0) {
                Position anchorPos = HUDPositioning.getPositionFromAnchor(currentAnchor);
                int barCenterX = barRect.x() + barRect.width() / 2;
                int barCenterY = barRect.y() + barRect.height() / 2;
                int lineColor = 0xA0FFFFFF;
                graphics.hLine(anchorPos.x(), barCenterX, anchorPos.y(), lineColor);
                graphics.vLine(barCenterX, anchorPos.y(), barCenterY, lineColor);
            }
        }

        // Draw resize handles when focused on a bar
        if (focused != null && player != null) {
            // For each of the sub-elements, draw resize handles
            drawResizeHandles(graphics, player, focused, mouseX, mouseY);
            
            // Draw text outline if text is supported for this element
            ClientConfig config = ModConfigManager.getClient();
            boolean shouldDrawTextOutline = false;
            switch (focused) {
                case HEALTH_BAR:
                case MANA_BAR:
                case STAMINA_BAR:
                case ARMOR_BAR:
                case AIR_BAR:
                    shouldDrawTextOutline = true;
                    break;
            }
            
            if (shouldDrawTextOutline) {
                ScreenRect textRect = null;
                switch (focused) {
                    case HEALTH_BAR:
                        textRect = HealthBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                        break;
                    case MANA_BAR:
                        textRect = ManaBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                        break;
                    case STAMINA_BAR:
                        textRect = StaminaBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                        break;
                    case ARMOR_BAR:
                        textRect = ArmorBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                        break;
                    case AIR_BAR:
                        textRect = AirBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                        break;
                }
                
                if (textRect != null && textRect.width() > 0 && textRect.height() > 0) {
                    // Draw a thin 1px outline around text area
                    graphics.renderOutline(textRect.x() - 1, textRect.y() - 1, 
                                         textRect.width() + 2, textRect.height() + 2, 
                                         0x60FFFFFF); // Semi-transparent white outline
                }
            }
            
            // Draw icon outline if supported for this element
            boolean shouldDrawIconOutline = false;
            switch (focused) {
                case ARMOR_BAR:
                case AIR_BAR:
                    shouldDrawIconOutline = true;
                    break;
            }
            
            if (shouldDrawIconOutline) {
                ScreenRect iconRect = null;
                switch (focused) {
                    case ARMOR_BAR:
                        if (config.enableArmorIcon) {
                            iconRect = ArmorBarRenderer.getSubElementRect(SubElementType.ICON, player);
                        }
                        break;
                    case AIR_BAR:
                        if (config.enableAirIcon) {
                            iconRect = AirBarRenderer.getSubElementRect(SubElementType.ICON, player);
                        }
                        break;
                }
                
                if (iconRect != null && iconRect.width() > 0 && iconRect.height() > 0) {
                    // Draw colored outline based on element type
                    int iconOutlineColor = focused == DraggableElement.ARMOR_BAR ? 0xA0C0C0C0 : 0xA0ADD8E6;
                    graphics.renderOutline(iconRect.x() - 1, iconRect.y() - 1, 
                                         iconRect.width() + 2, iconRect.height() + 2, 
                                         iconOutlineColor);
                }
            }
        }
        
        // Draw keyboard selection indicators
        if (player != null) {
            DraggableElement keyboardSelected = EditModeManager.getKeyboardSelectedElement();
            SubElementType keyboardSelectedSub = EditModeManager.getKeyboardSelectedSubElement();
            
            // Pulsing color for keyboard selection
            float pulse = (System.currentTimeMillis() % 1000) / 1000.0f;
            int alpha = (int)(128 + 64 * Math.sin(pulse * Math.PI * 2));
            int keyboardSelectionColor = (alpha << 24) | 0x00FF00; // Green with pulsing alpha
            
            if (focused == null && keyboardSelected != null) {
                // Non-focus mode: highlight the keyboard-selected element
                ScreenRect elementRect = null;
                switch (keyboardSelected) {
                    case HEALTH_BAR: elementRect = HealthBarRenderer.getScreenRect(player); break;
                    case MANA_BAR: elementRect = ManaBarRenderer.getScreenRect(player); break;
                    case STAMINA_BAR: elementRect = StaminaBarRenderer.getScreenRect(player); break;
                    case ARMOR_BAR: elementRect = ArmorBarRenderer.getScreenRect(player); break;
                    case AIR_BAR: elementRect = AirBarRenderer.getScreenRect(player); break;
                }
                
                if (elementRect != null && elementRect.width() > 0 && elementRect.height() > 0) {
                    // Draw thick pulsing outline
                    graphics.renderOutline(elementRect.x() - 2, elementRect.y() - 2,
                                         elementRect.width() + 4, elementRect.height() + 4,
                                         keyboardSelectionColor);
                }
            } else if (focused != null && keyboardSelectedSub != null) {
                // Focus mode: highlight the keyboard-selected sub-element
                ScreenRect subElementRect = null;
                switch (focused) {
                    case HEALTH_BAR:
                        subElementRect = HealthBarRenderer.getSubElementRect(keyboardSelectedSub, player);
                        break;
                    case MANA_BAR:
                        subElementRect = ManaBarRenderer.getSubElementRect(keyboardSelectedSub, player);
                        break;
                    case STAMINA_BAR:
                        subElementRect = StaminaBarRenderer.getSubElementRect(keyboardSelectedSub, player);
                        break;
                    case ARMOR_BAR:
                        subElementRect = ArmorBarRenderer.getSubElementRect(keyboardSelectedSub, player);
                        break;
                    case AIR_BAR:
                        subElementRect = AirBarRenderer.getSubElementRect(keyboardSelectedSub, player);
                        break;
                }
                
                if (subElementRect != null && subElementRect.width() > 0 && subElementRect.height() > 0) {
                    // Draw thick pulsing outline
                    graphics.renderOutline(subElementRect.x() - 2, subElementRect.y() - 2,
                                         subElementRect.width() + 4, subElementRect.height() + 4,
                                         keyboardSelectionColor);
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
        
        // Render context menu on top of everything
        if (activeContextMenu != null && activeContextMenu.isVisible()) {
            activeContextMenu.render(graphics, mouseX, mouseY, partialTicks);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics #if NEWER_THAN_20_1, int mouseX, int mouseY, float partialTicks#endif) {
        // Do nothing here to prevent the default background dim/dirt.
    }
    /**
     * Draws resize handles on the appropriate elements when in focus mode
     */
    private void drawResizeHandles(GuiGraphics graphics, Player player, DraggableElement focused, int mouseX, int mouseY) {
        ClientConfig config = ModConfigManager.getClient();


        // Draw resize handles for background
        ScreenRect bgRect = null;
        switch (focused) {
            case HEALTH_BAR:
                if (config.enableHealthBackground) {
                    bgRect = HealthBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                    drawHandlesForRect(graphics, bgRect, focused, SubElementType.BACKGROUND, mouseX, mouseY); // Pass focused
                }

                // Draw resize handles for main bar
                ScreenRect barRect = HealthBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                drawHandlesForRect(graphics, barRect, focused, SubElementType.BAR_MAIN, mouseX, mouseY); // Pass focused

                // Draw resize handles for overlay if enabled
                if (config.enableHealthForeground) {
                    ScreenRect fgRect = HealthBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    drawHandlesForRect(graphics, fgRect, focused, SubElementType.FOREGROUND_DETAIL, mouseX, mouseY); // Pass focused
                }
                break;

            case MANA_BAR:
                if (config.enableManaBackground) {
                    bgRect = ManaBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                    drawHandlesForRect(graphics, bgRect, focused, SubElementType.BACKGROUND, mouseX, mouseY); // Pass focused
                }

                // Draw resize handles for main bar
                ScreenRect manaBarRect = ManaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                drawHandlesForRect(graphics, manaBarRect, focused, SubElementType.BAR_MAIN, mouseX, mouseY); // Pass focused

                // Draw resize handles for overlay if enabled
                if (config.enableManaForeground) {
                    ScreenRect fgRect = ManaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    drawHandlesForRect(graphics, fgRect, focused, SubElementType.FOREGROUND_DETAIL, mouseX, mouseY); // Pass focused
                }
                break;

            case STAMINA_BAR:
                if (config.enableStaminaBackground) {
                    bgRect = StaminaBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                    drawHandlesForRect(graphics, bgRect, focused, SubElementType.BACKGROUND, mouseX, mouseY); // Pass focused
                }

                // Draw resize handles for main bar
                ScreenRect staminaBarRect = StaminaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                drawHandlesForRect(graphics, staminaBarRect, focused, SubElementType.BAR_MAIN, mouseX, mouseY); // Pass focused

                // Draw resize handles for overlay if enabled
                if (config.enableStaminaForeground) {
                    ScreenRect fgRect = StaminaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    drawHandlesForRect(graphics, fgRect, focused, SubElementType.FOREGROUND_DETAIL, mouseX, mouseY); // Pass focused
                }
                break;
            case ARMOR_BAR:
                // Background for Armor Bar is not optional, always draw handles if focused
                bgRect = ArmorBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                drawHandlesForRect(graphics, bgRect, focused, SubElementType.BACKGROUND, mouseX, mouseY);
                // Main Bar for Armor Bar
                ScreenRect armorBarRect = ArmorBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                drawHandlesForRect(graphics, armorBarRect, focused, SubElementType.BAR_MAIN, mouseX, mouseY);
                break;
            case AIR_BAR:
                // Background for Air Bar is not optional, always draw handles if focused
                bgRect = AirBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                drawHandlesForRect(graphics, bgRect, focused, SubElementType.BACKGROUND, mouseX, mouseY);
                // Main Bar for Air Bar
                ScreenRect airBarRect = AirBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                drawHandlesForRect(graphics, airBarRect, focused, SubElementType.BAR_MAIN, mouseX, mouseY);
                break;
        }
    }

    /**
     * Draws the four resize handles (top, right, bottom, left) for a given rectangle
     */
    private void drawHandlesForRect(GuiGraphics graphics, ScreenRect rect, DraggableElement focusedElement, SubElementType elementType, int mouseX, int mouseY) {
        if (rect == null || rect.width() <= 0 || rect.height() <= 0) return;

        int handleColor = getOutlineColorForSubElement(focusedElement, elementType);

        // Calculate handle positions
        int centerY = rect.y() + rect.height() / 2;
        int centerX = rect.x() + rect.width() / 2; // Added for bottom handle centering

        // Right Handle (for width)
        ScreenRect rightHandle = new ScreenRect(rect.x() + rect.width() - HANDLE_SIZE / 2, centerY - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
        // Bottom Handle (for height)
        ScreenRect bottomHandle = new ScreenRect(centerX - HANDLE_SIZE / 2, rect.y() + rect.height() - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);

        // Check if mouse is over any handle
        boolean isRightHover = rightHandle.contains(mouseX, mouseY);
        boolean isBottomHover = bottomHandle.contains(mouseX, mouseY);

        // Display proper cursor (conceptual, as we can't directly change cursors easily in vanilla MC GUI)
        if (isRightHover && this.currentResizeMode == ResizeMode.NONE) {
            // Would set resize cursor (horizontal) here
        } else if (isBottomHover && this.currentResizeMode == ResizeMode.NONE) {
            // Would set resize cursor (vertical) here
        }

        // Draw the handles with appropriate colors (highlighted if hovered)
        int rightActualColor = isRightHover ? HANDLE_HOVER_COLOR : handleColor;
        int bottomActualColor = isBottomHover ? HANDLE_HOVER_COLOR : handleColor;

        graphics.fill(rightHandle.x(), rightHandle.y(), rightHandle.x() + rightHandle.width(), rightHandle.y() + rightHandle.height(), rightActualColor);
        graphics.fill(bottomHandle.x(), bottomHandle.y(), bottomHandle.x() + bottomHandle.width(), bottomHandle.y() + bottomHandle.height(), bottomActualColor);
    }

    /**
     * Gets the resize handle that the mouse is hovering over
     */
    private ResizeData getResizeHandleAtPosition(DraggableElement element, SubElementType subElement, int mouseX, int mouseY) {
        Player player = Minecraft.getInstance().player;
        if (player == null || element == null || subElement == null) return null;

        ScreenRect rect = null;
        switch (element) {
            case HEALTH_BAR:
                rect = HealthBarRenderer.getSubElementRect(subElement, player);
                break;
            case MANA_BAR:
                rect = ManaBarRenderer.getSubElementRect(subElement, player);
                break;
            case STAMINA_BAR:
                rect = StaminaBarRenderer.getSubElementRect(subElement, player);
                break;
            case ARMOR_BAR:
                rect = ArmorBarRenderer.getSubElementRect(subElement, player);
                break;
            case AIR_BAR:
                rect = AirBarRenderer.getSubElementRect(subElement, player);
                break;
        }

        if (rect == null || rect.width() <= 0 || rect.height() <= 0) return null;

        // Calculate handle positions
        int centerY = rect.y() + rect.height() / 2;
        int centerX = rect.x() + rect.width() / 2; // Added for bottom handle centering

        // Right Handle (for width)
        ScreenRect rightHandle = new ScreenRect(rect.x() + rect.width() - HANDLE_SIZE / 2, centerY - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
        // Bottom Handle (for height)
        ScreenRect bottomHandle = new ScreenRect(centerX - HANDLE_SIZE / 2, rect.y() + rect.height() - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);

        // Check if mouse is over any handle
        if (rightHandle.contains(mouseX, mouseY)) {
            return new ResizeData(element, subElement, ResizeMode.WIDTH, rect.width(), rect.height());
        } else if (bottomHandle.contains(mouseX, mouseY)) {
            return new ResizeData(element, subElement, ResizeMode.HEIGHT, rect.width(), rect.height());
        }

        return null;
    }

    /**
     * Helper class to store resize operation data
     */
    private static class ResizeData {
        final DraggableElement element;
        final SubElementType subElement;
        final ResizeMode mode;
        final int initialWidth;
        final int initialHeight;

        ResizeData(DraggableElement element, SubElementType subElement, ResizeMode mode, int initialWidth, int initialHeight) {
            this.element = element;
            this.subElement = subElement;
            this.mode = mode;
            this.initialWidth = initialWidth;
            this.initialHeight = initialHeight;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Handle context menu clicks first
        if (activeContextMenu != null) {
            if (activeContextMenu.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            // Click outside menu or menu handled it - it will close itself
            activeContextMenu = null;
            return false;
        }

        if (super.mouseClicked(mouseX, mouseY, button)) {
             return true; // Click handled by a widget (button)
        }

        if (!EditModeManager.isEditModeEnabled()) {
            return false;
        }

        // Handle right-clicks for context menu
        if (button == 1) { // Right click
            Player player = Minecraft.getInstance().player;
            if (player == null) return false;

            DraggableElement focusedElement = EditModeManager.getFocusedElement();
            
            // If in focus mode, check for sub-element right-click first
            if (focusedElement != null) {
                SubElementType clickedSub = getClickedSubElement(focusedElement, mouseX, mouseY, player);
                if (clickedSub != null) {
                    contextMenuElement = focusedElement;
                    contextMenuSubElement = clickedSub;
                    contextMenuX = (int)mouseX;
                    contextMenuY = (int)mouseY;
                    activeContextMenu = createSubElementContextMenu(contextMenuX, contextMenuY, contextMenuElement, contextMenuSubElement);
                    if (activeContextMenu != null) {
                        activeContextMenu.setOnClose(() -> {
                            activeContextMenu = null;
                            contextMenuElement = null;
                            contextMenuSubElement = null;
                        });
                        return true;
                    }
                }
            }
            
            // Check for main element right-click
            DraggableElement clickedElement = getClickedBarComplex(mouseX, mouseY, player);
            if (clickedElement != null) {
                contextMenuElement = clickedElement;
                contextMenuSubElement = null;
                contextMenuX = (int)mouseX;
                contextMenuY = (int)mouseY;
                activeContextMenu = createMainElementContextMenu(contextMenuX, contextMenuY, contextMenuElement);
                if (activeContextMenu != null) {
                    activeContextMenu.setOnClose(() -> {
                        activeContextMenu = null;
                        contextMenuElement = null;
                        contextMenuSubElement = null;
                    });
                    return true;
                }
            }
            
            return true; // Consume right-click even if no menu shown
        }

        if (button != 0) {
            return false; // Only handle left clicks beyond this point
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        long currentTime = System.currentTimeMillis();
        boolean isDoubleClick = (currentTime - lastClickTime < DOUBLE_CLICK_TIME_MS) &&
                                (Math.abs(mouseX - lastClickX) < 5 && Math.abs(mouseY - lastClickY) < 5);
        boolean actionTaken = false;

        DraggableElement currentFocusedElement = EditModeManager.getFocusedElement();
        ClientConfig config = ModConfigManager.getClient(); // Get instance for modifications

        // --- 1. Double Click for Focus/Defocus (Highest priority, works in any mode) ---
        if (isDoubleClick) {
            DraggableElement clickedBarForFocus = getClickedBarComplex(mouseX, mouseY, player);
            if (clickedBarForFocus != null) {
                // If currently focused on this element, exit focus mode
                if (currentFocusedElement != null && currentFocusedElement == clickedBarForFocus) {
                    EditModeManager.clearFocusedElement();
                } else {
                    // Otherwise, enter focus mode on the clicked element
                    EditModeManager.setFocusedElement(clickedBarForFocus);
                }
                rebuildEditorWidgets();
            }
            // Always reset timer and consume double-click to prevent single-click drag from triggering
            lastClickTime = 0;
            return true;
        }

        // --- 2. Resize Handle Interaction (when focused) ---
        if (currentFocusedElement != null) {
            SubElementType[] subElementTypesToTest = {SubElementType.BACKGROUND, SubElementType.BAR_MAIN, SubElementType.FOREGROUND_DETAIL};

            for (SubElementType subType : subElementTypesToTest) {
                boolean canTestThisSubElement = false;
                switch (currentFocusedElement) {
                    case HEALTH_BAR:
                        if (subType == SubElementType.BACKGROUND && config.enableHealthBackground) canTestThisSubElement = true;
                        if (subType == SubElementType.BAR_MAIN) canTestThisSubElement = true;
                        if (subType == SubElementType.FOREGROUND_DETAIL && config.enableHealthForeground) canTestThisSubElement = true;
                        break;
                    case MANA_BAR:
                        if (subType == SubElementType.BACKGROUND && config.enableManaBackground) canTestThisSubElement = true;
                        if (subType == SubElementType.BAR_MAIN) canTestThisSubElement = true;
                        if (subType == SubElementType.FOREGROUND_DETAIL && config.enableManaForeground) canTestThisSubElement = true;
                        break;
                    case STAMINA_BAR:
                        if (subType == SubElementType.BACKGROUND && config.enableStaminaBackground) canTestThisSubElement = true;
                        if (subType == SubElementType.BAR_MAIN) canTestThisSubElement = true;
                        if (subType == SubElementType.FOREGROUND_DETAIL && config.enableStaminaForeground) canTestThisSubElement = true;
                        break;
                    case ARMOR_BAR: // Added for Armor Bar
                        if (subType == SubElementType.BACKGROUND) canTestThisSubElement = true; // Background is always enabled conceptually
                        if (subType == SubElementType.BAR_MAIN) canTestThisSubElement = true;
                        // No FOREGROUND_DETAIL for Armor Bar
                        break;
                    case AIR_BAR: // Added for Air Bar
                        if (subType == SubElementType.BACKGROUND) canTestThisSubElement = true; // Background is always enabled conceptually
                        if (subType == SubElementType.BAR_MAIN) canTestThisSubElement = true;
                        // No FOREGROUND_DETAIL for Air Bar
                        break;
                }

                if (canTestThisSubElement) {
                    ResizeData resizeData = getResizeHandleAtPosition(currentFocusedElement, subType, (int)mouseX, (int)mouseY);
                    if (resizeData != null) {
                        startResizeOperation(resizeData, (int)mouseX, (int)mouseY); // startResizeOperation will use ModConfigManager.getClient()
                        // Auto-select the sub-element being resized
                        EditModeManager.setKeyboardSelectedSubElement(subType);
                        actionTaken = true;
                        break; // Found a handle, stop checking others
                    }
                }
            }
            if (actionTaken) {
                // Reset double-click tracking if a resize started
                lastClickTime = 0;
                return true;
            }
        }

        // --- 3. Sub-Element Drag Interaction (If focused and not resizing) ---
        if (!actionTaken && currentFocusedElement != null && this.currentResizeMode == ResizeMode.NONE) {
            SubElementType clickedSub = getClickedSubElement(currentFocusedElement, mouseX, mouseY, player);
            if (clickedSub != null) { // Allow dragging all sub-elements
                int currentSubX = 0; int currentSubY = 0;
                switch (currentFocusedElement) {
                     case HEALTH_BAR: 
                         if (clickedSub == SubElementType.BAR_MAIN) { 
                             currentSubX = config.healthBarXOffset; currentSubY = config.healthBarYOffset; 
                         } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { 
                             currentSubX = config.healthOverlayXOffset; currentSubY = config.healthOverlayYOffset; 
                         } else if (clickedSub == SubElementType.BACKGROUND) {
                             currentSubX = config.healthBackgroundXOffset; currentSubY = config.healthBackgroundYOffset;
                         } else if (clickedSub == SubElementType.TEXT) {
                             currentSubX = config.healthTextXOffset; currentSubY = config.healthTextYOffset;
                         } else if (clickedSub == SubElementType.ABSORPTION_TEXT) {
                             currentSubX = config.healthAbsorptionTextXOffset; currentSubY = config.healthAbsorptionTextYOffset;
                         }
                         break;
                     case MANA_BAR: 
                         if (clickedSub == SubElementType.BAR_MAIN) { 
                             currentSubX = config.manaBarXOffset; currentSubY = config.manaBarYOffset; 
                         } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { 
                             currentSubX = config.manaOverlayXOffset; currentSubY = config.manaOverlayYOffset; 
                         } else if (clickedSub == SubElementType.BACKGROUND) {
                             currentSubX = config.manaBackgroundXOffset; currentSubY = config.manaBackgroundYOffset;
                         } else if (clickedSub == SubElementType.TEXT) {
                             currentSubX = config.manaTextXOffset; currentSubY = config.manaTextYOffset;
                         }
                         break;
                     case STAMINA_BAR: 
                         if (clickedSub == SubElementType.BAR_MAIN) { 
                             currentSubX = config.staminaBarXOffset; currentSubY = config.staminaBarYOffset; 
                         } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { 
                             currentSubX = config.staminaOverlayXOffset; currentSubY = config.staminaOverlayYOffset; 
                         } else if (clickedSub == SubElementType.BACKGROUND) {
                             currentSubX = config.staminaBackgroundXOffset; currentSubY = config.staminaBackgroundYOffset;
                         } else if (clickedSub == SubElementType.TEXT) {
                             currentSubX = config.staminaTextXOffset; currentSubY = config.staminaTextYOffset;
                         }
                         break;
                     case ARMOR_BAR:
                         if (clickedSub == SubElementType.BAR_MAIN) { 
                             currentSubX = config.armorBarXOffset; currentSubY = config.armorBarYOffset; 
                         } else if (clickedSub == SubElementType.BACKGROUND) {
                             currentSubX = config.armorBackgroundXOffset; currentSubY = config.armorBackgroundYOffset;
                         } else if (clickedSub == SubElementType.TEXT) {
                             currentSubX = config.armorTextXOffset; currentSubY = config.armorTextYOffset;
                         } else if (clickedSub == SubElementType.ICON) {
                             currentSubX = config.armorIconXOffset; currentSubY = config.armorIconYOffset;
                         }
                         break;
                     case AIR_BAR:
                         if (clickedSub == SubElementType.BAR_MAIN) { 
                             currentSubX = config.airBarXOffset; currentSubY = config.airBarYOffset; 
                         } else if (clickedSub == SubElementType.BACKGROUND) {
                             currentSubX = config.airBackgroundXOffset; currentSubY = config.airBackgroundYOffset;
                         } else if (clickedSub == SubElementType.TEXT) {
                             currentSubX = config.airTextXOffset; currentSubY = config.airTextYOffset;
                         } else if (clickedSub == SubElementType.ICON) {
                             currentSubX = config.airIconXOffset; currentSubY = config.airIconYOffset;
                         }
                         break;
                }
                EditModeManager.setDraggedSubElement(clickedSub, (int)mouseX, (int)mouseY, currentSubX, currentSubY);
                // Auto-select the sub-element being dragged
                EditModeManager.setKeyboardSelectedSubElement(clickedSub);
                this.lastFocusedElementForSubUndo = currentFocusedElement;
                this.lastDraggedSubElementForUndo = clickedSub;
                this.lastSubDragInitialXOffset = currentSubX;
                this.lastSubDragInitialYOffset = currentSubY;
                this.canUndoLastSubDrag = false;
                this.canUndoLastDrag = false;
                this.lastDraggedElementForUndo = null;
                actionTaken = true;
            }
            if(actionTaken) {
                lastClickTime = currentTime; lastClickX = mouseX; lastClickY = mouseY; // Update for next potential double click
                return true;
            }
        }

        // --- 4. Whole Element Drag Interaction (If not focused or no sub-element interaction) ---
        if (!actionTaken && EditModeManager.getFocusedElement() == null && this.currentResizeMode == ResizeMode.NONE) {
            DraggableElement clickedBarForDrag = getClickedBarComplex(mouseX, mouseY, player);
             if (clickedBarForDrag != null) {
                int totalX = 0; int totalY = 0;
                 switch (clickedBarForDrag) {
                    case HEALTH_BAR: totalX = config.healthTotalXOffset; totalY = config.healthTotalYOffset; break;
                    case MANA_BAR: totalX = config.manaTotalXOffset; totalY = config.manaTotalYOffset; break;
                    case STAMINA_BAR: totalX = config.staminaTotalXOffset; totalY = config.staminaTotalYOffset; break;
                    case ARMOR_BAR: totalX = config.armorTotalXOffset; totalY = config.armorTotalYOffset; break;
                    case AIR_BAR: totalX = config.airTotalXOffset; totalY = config.airTotalYOffset; break;
                }
                this.lastDraggedElementForUndo = clickedBarForDrag;
                this.lastDragInitialXOffset = totalX;
                this.lastDragInitialYOffset = totalY;
                this.canUndoLastDrag = false;
                EditModeManager.setDraggedElement(clickedBarForDrag, (int) mouseX, (int) mouseY, totalX, totalY);
                // Auto-select the element being dragged
                EditModeManager.setKeyboardSelectedElement(clickedBarForDrag);
                actionTaken = true;
             }
        }

        // Update last click info if no double-click or action was taken that resets it
        if (!isDoubleClick && !actionTaken) {
            lastClickTime = currentTime;
            lastClickX = mouseX;
            lastClickY = mouseY;
        } else if (actionTaken && !isDoubleClick && this.currentResizeMode == ResizeMode.NONE && EditModeManager.getDraggedSubElement() == null && EditModeManager.getDraggedElement() == null) {
            // If a single click action occurred that wasn't a drag start (e.g. focus from double click logic falling through due to timing)
            // For now, this case shouldn't happen if double click focus is handled correctly.
            // We primarily update lastClick for drag starts or for the first click of a potential double click.
        }

        return actionTaken;
    }

    private DraggableElement getClickedBarComplex(double mouseX, double mouseY, Player player) {
        if (HealthBarRenderer.getScreenRect(player).contains((int)mouseX, (int)mouseY)) return DraggableElement.HEALTH_BAR;
        if (StaminaBarRenderer.getScreenRect(player).contains((int)mouseX, (int)mouseY)) return DraggableElement.STAMINA_BAR;
        if (ManaBarRenderer.getScreenRect(player).contains((int)mouseX, (int)mouseY)) return DraggableElement.MANA_BAR;
        if (ArmorBarRenderer.getScreenRect(player).contains((int)mouseX, (int)mouseY)) return DraggableElement.ARMOR_BAR;
        if (AirBarRenderer.getScreenRect(player).contains((int)mouseX, (int)mouseY)) return DraggableElement.AIR_BAR;
        return null;
    }

    private SubElementType getClickedSubElement(DraggableElement focusedBar, double mouseX, double mouseY, Player player) {
        if (focusedBar == null) return null;

        ScreenRect barMainRect = null;
        ScreenRect barFgRect = null;
        ScreenRect barBgRect = null;
        ScreenRect barTextRect = null;
        ScreenRect barIconRect = null;
        ScreenRect barAbsorptionTextRect = null;
        ClientConfig currentConfig = ModConfigManager.getClient();

        switch (focusedBar) {
            case HEALTH_BAR:
                barMainRect = HealthBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                // Always get rects even if disabled, so they can be right-clicked to re-enable
                barFgRect = HealthBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                barBgRect = HealthBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                barTextRect = HealthBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                barAbsorptionTextRect = HealthBarRenderer.getSubElementRect(SubElementType.ABSORPTION_TEXT, player);
                break;
            case STAMINA_BAR:
                barMainRect = StaminaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                // Always get rects even if disabled, so they can be right-clicked to re-enable
                barFgRect = StaminaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                barBgRect = StaminaBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                barTextRect = StaminaBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                break;
            case MANA_BAR:
                barMainRect = ManaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                // Always get rects even if disabled, so they can be right-clicked to re-enable
                barFgRect = ManaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                barBgRect = ManaBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                barTextRect = ManaBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                break;
            case ARMOR_BAR:
                barMainRect = ArmorBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                barBgRect = ArmorBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                barTextRect = ArmorBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                // Always get rect even if disabled, so it can be right-clicked to re-enable
                barIconRect = ArmorBarRenderer.getSubElementRect(SubElementType.ICON, player);
                break;
            case AIR_BAR:
                barMainRect = AirBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                barBgRect = AirBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                barTextRect = AirBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                // Always get rect even if disabled, so it can be right-clicked to re-enable
                barIconRect = AirBarRenderer.getSubElementRect(SubElementType.ICON, player);
                break;
        }

        // Check in order: absorption text, icon, text, foreground, main bar, then background (so clicking overlapping elements prioritizes the topmost)
        if (barAbsorptionTextRect != null && barAbsorptionTextRect.contains((int)mouseX, (int)mouseY)) return SubElementType.ABSORPTION_TEXT;
        if (barIconRect != null && barIconRect.contains((int)mouseX, (int)mouseY)) return SubElementType.ICON;
        if (barTextRect != null && barTextRect.contains((int)mouseX, (int)mouseY)) return SubElementType.TEXT;
        if (barFgRect != null && barFgRect.contains((int)mouseX, (int)mouseY)) return SubElementType.FOREGROUND_DETAIL;
        if (barMainRect != null && barMainRect.contains((int)mouseX, (int)mouseY)) return SubElementType.BAR_MAIN;
        if (barBgRect != null && barBgRect.contains((int)mouseX, (int)mouseY)) return SubElementType.BACKGROUND;
        return null; 
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        ClientConfig config = ModConfigManager.getClient();

        if (this.currentResizeMode != ResizeMode.NONE && this.resizingSubElement != null && EditModeManager.getFocusedElement() != null) {
            handleResize((int)mouseX, (int)mouseY); // Modifies config fields directly
            return true;
        }
        
        if (EditModeManager.getDraggedSubElement() != null && EditModeManager.getFocusedElement() != null) {
            final int finalNewSubX = EditModeManager.getInitialSubElementXOffset() + (int)(mouseX - EditModeManager.getSubElementDragStartX());
            final int finalNewSubY = EditModeManager.getInitialSubElementYOffset() + (int)(mouseY - EditModeManager.getSubElementDragStartY());
            DraggableElement focused = EditModeManager.getFocusedElement();
            SubElementType sub = EditModeManager.getDraggedSubElement();
            switch (focused) {
                case HEALTH_BAR:
                    if (sub == SubElementType.BAR_MAIN) { 
                        config.healthBarXOffset = finalNewSubX; config.healthBarYOffset = finalNewSubY; 
                    } else if (sub == SubElementType.FOREGROUND_DETAIL) { 
                        config.healthOverlayXOffset = finalNewSubX; config.healthOverlayYOffset = finalNewSubY; 
                    } else if (sub == SubElementType.BACKGROUND) {
                        config.healthBackgroundXOffset = finalNewSubX; config.healthBackgroundYOffset = finalNewSubY;
                    } else if (sub == SubElementType.TEXT) {
                        config.healthTextXOffset = finalNewSubX; config.healthTextYOffset = finalNewSubY;
                    } else if (sub == SubElementType.ABSORPTION_TEXT) {
                        config.healthAbsorptionTextXOffset = finalNewSubX; config.healthAbsorptionTextYOffset = finalNewSubY;
                    }
                    break;
                case MANA_BAR:
                     if (sub == SubElementType.BAR_MAIN) { 
                         config.manaBarXOffset = finalNewSubX; config.manaBarYOffset = finalNewSubY; 
                     } else if (sub == SubElementType.FOREGROUND_DETAIL) { 
                         config.manaOverlayXOffset = finalNewSubX; config.manaOverlayYOffset = finalNewSubY; 
                     } else if (sub == SubElementType.BACKGROUND) {
                         config.manaBackgroundXOffset = finalNewSubX; config.manaBackgroundYOffset = finalNewSubY;
                     } else if (sub == SubElementType.TEXT) {
                         config.manaTextXOffset = finalNewSubX; config.manaTextYOffset = finalNewSubY;
                     }
                    break;
                case STAMINA_BAR:
                     if (sub == SubElementType.BAR_MAIN) { 
                         config.staminaBarXOffset = finalNewSubX; config.staminaBarYOffset = finalNewSubY; 
                     } else if (sub == SubElementType.FOREGROUND_DETAIL) { 
                         config.staminaOverlayXOffset = finalNewSubX; config.staminaOverlayYOffset = finalNewSubY; 
                     } else if (sub == SubElementType.BACKGROUND) {
                         config.staminaBackgroundXOffset = finalNewSubX; config.staminaBackgroundYOffset = finalNewSubY;
                     } else if (sub == SubElementType.TEXT) {
                         config.staminaTextXOffset = finalNewSubX; config.staminaTextYOffset = finalNewSubY;
                     }
                    break;
                case ARMOR_BAR:
                     if (sub == SubElementType.BAR_MAIN) { 
                         config.armorBarXOffset = finalNewSubX; config.armorBarYOffset = finalNewSubY; 
                     } else if (sub == SubElementType.TEXT) {
                         config.armorTextXOffset = finalNewSubX; config.armorTextYOffset = finalNewSubY;
                     } else if (sub == SubElementType.BACKGROUND) {
                         config.armorBackgroundXOffset = finalNewSubX; config.armorBackgroundYOffset = finalNewSubY;
                     } else if (sub == SubElementType.ICON) {
                         config.armorIconXOffset = finalNewSubX; config.armorIconYOffset = finalNewSubY;
                     }
                    break;
                case AIR_BAR:
                     if (sub == SubElementType.BAR_MAIN) { 
                         config.airBarXOffset = finalNewSubX; config.airBarYOffset = finalNewSubY; 
                     } else if (sub == SubElementType.TEXT) {
                         config.airTextXOffset = finalNewSubX; config.airTextYOffset = finalNewSubY;
                     } else if (sub == SubElementType.ICON) {
                         config.airIconXOffset = finalNewSubX; config.airIconYOffset = finalNewSubY;
                     } else if (sub == SubElementType.BACKGROUND) {
                         config.airBackgroundXOffset = finalNewSubX; config.airBackgroundYOffset = finalNewSubY;
                     }
                     // Background is not offset for air
                    break;
            }
            return true; 
        }
        else if (EditModeManager.getDraggedElement() != null) {
            final int finalNewTotalX = EditModeManager.getInitialElementXOffset() + (int)(mouseX - EditModeManager.getDragStartX());
            final int finalNewTotalY = EditModeManager.getInitialElementYOffset() + (int)(mouseY - EditModeManager.getDragStartY());
            DraggableElement dragged = EditModeManager.getDraggedElement();
            switch (dragged) {
                case HEALTH_BAR: config.healthTotalXOffset = finalNewTotalX; config.healthTotalYOffset = finalNewTotalY; break;
                case MANA_BAR: config.manaTotalXOffset = finalNewTotalX; config.manaTotalYOffset = finalNewTotalY; break;
                case STAMINA_BAR: config.staminaTotalXOffset = finalNewTotalX; config.staminaTotalYOffset = finalNewTotalY; break;
                case ARMOR_BAR: config.armorTotalXOffset = finalNewTotalX; config.armorTotalYOffset = finalNewTotalY; break;
                case AIR_BAR: config.airTotalXOffset = finalNewTotalX; config.airTotalYOffset = finalNewTotalY; break;
            }
            return true; 
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        // No explicit save here, all changes are to in-memory ClientConfig POJO.
        // Final save happens in onClose().

        if (this.currentResizeMode != ResizeMode.NONE && button == 0) {
            // Optionally, could do a final precise calculation here if intermediate steps were lossy
            // but current logic in handleResize should be fine.
            finishResize(); 
            handled = true;
        }

        if (EditModeManager.getDraggedSubElement() != null && button == 0) {
            EditModeManager.clearDraggedSubElement();
            handled = true;
        }

        if (EditModeManager.getDraggedElement() != null && button == 0) {
            EditModeManager.clearDraggedElement();
            handled = true;
        }
        return handled || super.mouseReleased(mouseX, mouseY, button);
    }

    private void startResizeOperation(ResizeData resizeData, int mouseX, int mouseY) {
        this.currentResizeMode = resizeData.mode;
        this.focusedElementForResize = resizeData.element;
        this.resizingSubElement = resizeData.subElement;
        this.resizeStartX = mouseX;
        this.resizeStartY = mouseY;
        // Initialize initialWidth/Height from the current state of the in-memory config POJO
        ClientConfig config = ModConfigManager.getClient();
        switch (resizeData.element) {
            case HEALTH_BAR:
                if (resizeData.subElement == SubElementType.BACKGROUND) { this.initialWidth = config.healthBackgroundWidth; this.initialHeight = config.healthBackgroundHeight; }
                else if (resizeData.subElement == SubElementType.BAR_MAIN) { this.initialWidth = config.healthBarWidth; this.initialHeight = config.healthBarHeight; }
                else if (resizeData.subElement == SubElementType.FOREGROUND_DETAIL) { this.initialWidth = config.healthOverlayWidth; this.initialHeight = config.healthOverlayHeight; }
                break;
            case MANA_BAR:
                if (resizeData.subElement == SubElementType.BACKGROUND) { this.initialWidth = config.manaBackgroundWidth; this.initialHeight = config.manaBackgroundHeight; }
                else if (resizeData.subElement == SubElementType.BAR_MAIN) { this.initialWidth = config.manaBarWidth; this.initialHeight = config.manaBarHeight; }
                else if (resizeData.subElement == SubElementType.FOREGROUND_DETAIL) { this.initialWidth = config.manaOverlayWidth; this.initialHeight = config.manaOverlayHeight; }
                break;
            case STAMINA_BAR:
                if (resizeData.subElement == SubElementType.BACKGROUND) { this.initialWidth = config.staminaBackgroundWidth; this.initialHeight = config.staminaBackgroundHeight; }
                else if (resizeData.subElement == SubElementType.BAR_MAIN) { this.initialWidth = config.staminaBarWidth; this.initialHeight = config.staminaBarHeight; }
                else if (resizeData.subElement == SubElementType.FOREGROUND_DETAIL) { this.initialWidth = config.staminaOverlayWidth; this.initialHeight = config.staminaOverlayHeight; }
                break;
            case ARMOR_BAR: // Added for Armor Bar
                if (resizeData.subElement == SubElementType.BACKGROUND) { this.initialWidth = config.armorBackgroundWidth; this.initialHeight = config.armorBackgroundHeight; }
                else if (resizeData.subElement == SubElementType.BAR_MAIN) { this.initialWidth = config.armorBarWidth; this.initialHeight = config.armorBarHeight; }
                // No FOREGROUND_DETAIL for Armor Bar
                break;
            case AIR_BAR: // Added for Air Bar
                if (resizeData.subElement == SubElementType.BACKGROUND) { this.initialWidth = config.airBackgroundWidth; this.initialHeight = config.airBackgroundHeight; }
                else if (resizeData.subElement == SubElementType.BAR_MAIN) { this.initialWidth = config.airBarWidth; this.initialHeight = config.airBarHeight; }
                // No FOREGROUND_DETAIL for Air Bar
                break;
        }
    }

    private void handleResize(int mouseX, int mouseY) {
        if (this.currentResizeMode == ResizeMode.NONE || this.resizingSubElement == null || this.focusedElementForResize == null) return;
        
        int deltaX = mouseX - this.resizeStartX;
        int deltaY = mouseY - this.resizeStartY;
        ClientConfig config = ModConfigManager.getClient();
        
        switch (this.focusedElementForResize) {
            case HEALTH_BAR: applyHealthResizing(config, deltaX, deltaY); break;
            case MANA_BAR: applyManaResizing(config, deltaX, deltaY); break;
            case STAMINA_BAR: applyStaminaResizing(config, deltaX, deltaY); break;
            case ARMOR_BAR: applyArmorResizing(config, deltaX, deltaY); break; // Added for Armor Bar
            case AIR_BAR: applyAirResizing(config, deltaX, deltaY); break;       // Added for Air Bar
        }
    }

    private void applyHealthResizing(ClientConfig config, int deltaX, int deltaY) {
        switch (this.resizingSubElement) {
            case BACKGROUND:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.healthBackgroundWidth = Math.max(10, this.initialWidth + deltaX); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.healthBackgroundHeight = Math.max(4, this.initialHeight + deltaY); }
                break;
            case BAR_MAIN:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.healthBarWidth = Math.max(4, Math.min(256, this.initialWidth + deltaX)); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.healthBarHeight = Math.max(1, Math.min(32, this.initialHeight + deltaY)); }
                break;
            case FOREGROUND_DETAIL:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.healthOverlayWidth = Math.max(10, Math.min(256, this.initialWidth + deltaX)); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.healthOverlayHeight = Math.max(4, Math.min(256, this.initialHeight + deltaY)); }
                break;
        }
    }
    
    private void applyManaResizing(ClientConfig config, int deltaX, int deltaY) {
        switch (this.resizingSubElement) {
            case BACKGROUND:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.manaBackgroundWidth = Math.max(10, this.initialWidth + deltaX); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.manaBackgroundHeight = Math.max(4, this.initialHeight + deltaY); }
                break;
            case BAR_MAIN:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.manaBarWidth = Math.max(4, Math.min(256, this.initialWidth + deltaX)); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.manaBarHeight = Math.max(1, Math.min(32, this.initialHeight + deltaY)); }
                break;
            case FOREGROUND_DETAIL:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.manaOverlayWidth = Math.max(10, Math.min(256, this.initialWidth + deltaX)); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.manaOverlayHeight = Math.max(4, Math.min(256, this.initialHeight + deltaY)); }
                break;
        }
    }
    
    private void applyStaminaResizing(ClientConfig config, int deltaX, int deltaY) {
        switch (this.resizingSubElement) {
            case BACKGROUND:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.staminaBackgroundWidth = Math.max(10, this.initialWidth + deltaX); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.staminaBackgroundHeight = Math.max(4, this.initialHeight + deltaY); }
                break;
            case BAR_MAIN:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.staminaBarWidth = Math.max(4, Math.min(256, this.initialWidth + deltaX)); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.staminaBarHeight = Math.max(1, Math.min(32, this.initialHeight + deltaY)); }
                break;
            case FOREGROUND_DETAIL:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.staminaOverlayWidth = Math.max(10, Math.min(256, this.initialWidth + deltaX)); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.staminaOverlayHeight = Math.max(4, Math.min(256, this.initialHeight + deltaY)); }
                break;
        }
    }

    private void applyArmorResizing(ClientConfig config, int deltaX, int deltaY) {
        switch (this.resizingSubElement) {
            case BACKGROUND:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.armorBackgroundWidth = Math.max(10, this.initialWidth + deltaX); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.armorBackgroundHeight = Math.max(4, this.initialHeight + deltaY); }
                break;
            case BAR_MAIN:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.armorBarWidth = Math.max(4, Math.min(256, this.initialWidth + deltaX)); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.armorBarHeight = Math.max(1, Math.min(32, this.initialHeight + deltaY)); }
                break;
            // No FOREGROUND_DETAIL for Armor Bar
        }
    }

    private void applyAirResizing(ClientConfig config, int deltaX, int deltaY) {
        switch (this.resizingSubElement) {
            case BACKGROUND:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.airBackgroundWidth = Math.max(10, this.initialWidth + deltaX); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.airBackgroundHeight = Math.max(4, this.initialHeight + deltaY); }
                break;
            case BAR_MAIN:
                if (this.currentResizeMode == ResizeMode.WIDTH) { config.airBarWidth = Math.max(4, Math.min(256, this.initialWidth + deltaX)); }
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.airBarHeight = Math.max(1, Math.min(32, this.initialHeight + deltaY)); }
                break;
            // No FOREGROUND_DETAIL for Air Bar
        }
    }

    private void finishResize() {
        if (this.currentResizeMode != ResizeMode.NONE) {
            this.currentResizeMode = ResizeMode.NONE;
            this.focusedElementForResize = null; 
            this.resizingSubElement = null;
        }
    }

    @Override
    public void onClose() {
        if (EditModeManager.isEditModeEnabled()) {
            EditModeManager.toggleEditMode(); 
        }
        if (EditModeManager.getFocusedElement() != null) { 
            EditModeManager.clearFocusedElement();
        }
        ClientConfig.getInstance().save(); // Save config on close
        this.minecraft.setScreen(this.previousScreen); 
    }

    @Override
    public boolean isPauseScreen() {
        return false; 
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return EditModeManager.getFocusedElement() == null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (EditModeManager.getFocusedElement() != null) {
                EditModeManager.clearFocusedElement();
                rebuildEditorWidgets();
                return true; 
            }
        }
        
        // Tab key - cycle through elements or sub-elements
        if (keyCode == GLFW.GLFW_KEY_TAB) {
            handleTabNavigation();
            return true;
        }
        
        // Arrow keys - move or resize
        boolean isShiftPressed = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN || 
            keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT) {
            if (isShiftPressed) {
                handleArrowResize(keyCode);
            } else {
                handleArrowMove(keyCode);
            }
            return true;
        }
        
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void handleTabNavigation() {
        ClientConfig config = ModConfigManager.getClient();
        DraggableElement focused = EditModeManager.getFocusedElement();
        
        if (focused == null) {
            // Non-focus mode: cycle through elements
            DraggableElement current = EditModeManager.getKeyboardSelectedElement();
            DraggableElement[] elements = DraggableElement.values();
            
            if (current == null) {
                // Start with first element
                EditModeManager.setKeyboardSelectedElement(elements[0]);
            } else {
                // Find next available element
                int currentIndex = current.ordinal();
                int nextIndex = (currentIndex + 1) % elements.length;
                EditModeManager.setKeyboardSelectedElement(elements[nextIndex]);
            }
        } else {
            // Focus mode: cycle through sub-elements for the focused bar
            SubElementType current = EditModeManager.getKeyboardSelectedSubElement();
            SubElementType[] availableSubElements = getAvailableSubElements(focused, config);
            
            if (availableSubElements.length == 0) return;
            
            if (current == null) {
                // Start with first available sub-element
                EditModeManager.setKeyboardSelectedSubElement(availableSubElements[0]);
            } else {
                // Find next available sub-element
                int currentIndex = -1;
                for (int i = 0; i < availableSubElements.length; i++) {
                    if (availableSubElements[i] == current) {
                        currentIndex = i;
                        break;
                    }
                }
                int nextIndex = (currentIndex + 1) % availableSubElements.length;
                EditModeManager.setKeyboardSelectedSubElement(availableSubElements[nextIndex]);
            }
        }
    }
    
    private SubElementType[] getAvailableSubElements(DraggableElement element, ClientConfig config) {
        java.util.List<SubElementType> available = new java.util.ArrayList<>();
        
        // Always include all sub-elements, even if disabled, so they can be cycled to and re-enabled
        switch (element) {
            case HEALTH_BAR:
                available.add(SubElementType.BACKGROUND);
                available.add(SubElementType.BAR_MAIN);
                available.add(SubElementType.FOREGROUND_DETAIL);
                available.add(SubElementType.TEXT);
                available.add(SubElementType.ABSORPTION_TEXT);
                break;
            case MANA_BAR:
                available.add(SubElementType.BACKGROUND);
                available.add(SubElementType.BAR_MAIN);
                available.add(SubElementType.FOREGROUND_DETAIL);
                available.add(SubElementType.TEXT);
                break;
            case STAMINA_BAR:
                available.add(SubElementType.BACKGROUND);
                available.add(SubElementType.BAR_MAIN);
                available.add(SubElementType.FOREGROUND_DETAIL);
                available.add(SubElementType.TEXT);
                break;
            case ARMOR_BAR:
                available.add(SubElementType.BACKGROUND);
                available.add(SubElementType.BAR_MAIN);
                available.add(SubElementType.TEXT);
                available.add(SubElementType.ICON);
                break;
            case AIR_BAR:
                available.add(SubElementType.BACKGROUND);
                available.add(SubElementType.BAR_MAIN);
                available.add(SubElementType.TEXT);
                available.add(SubElementType.ICON);
                break;
        }
        
        return available.toArray(new SubElementType[0]);
    }
    
    private void handleArrowMove(int keyCode) {
        ClientConfig config = ModConfigManager.getClient();
        DraggableElement focused = EditModeManager.getFocusedElement();
        
        int dx = 0, dy = 0;
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP: dy = -1; break;
            case GLFW.GLFW_KEY_DOWN: dy = 1; break;
            case GLFW.GLFW_KEY_LEFT: dx = -1; break;
            case GLFW.GLFW_KEY_RIGHT: dx = 1; break;
        }
        
        if (focused == null) {
            // Non-focus mode: move the keyboard-selected element's total offset
            DraggableElement selected = EditModeManager.getKeyboardSelectedElement();
            if (selected == null) return;
            
            switch (selected) {
                case HEALTH_BAR:
                    config.healthTotalXOffset += dx;
                    config.healthTotalYOffset += dy;
                    break;
                case MANA_BAR:
                    config.manaTotalXOffset += dx;
                    config.manaTotalYOffset += dy;
                    break;
                case STAMINA_BAR:
                    config.staminaTotalXOffset += dx;
                    config.staminaTotalYOffset += dy;
                    break;
                case ARMOR_BAR:
                    config.armorTotalXOffset += dx;
                    config.armorTotalYOffset += dy;
                    break;
                case AIR_BAR:
                    config.airTotalXOffset += dx;
                    config.airTotalYOffset += dy;
                    break;
            }
        } else {
            // Focus mode: move the keyboard-selected sub-element
            SubElementType subSelected = EditModeManager.getKeyboardSelectedSubElement();
            if (subSelected == null) return;
            
            switch (focused) {
                case HEALTH_BAR:
                    moveHealthSubElement(config, subSelected, dx, dy);
                    break;
                case MANA_BAR:
                    moveManaSubElement(config, subSelected, dx, dy);
                    break;
                case STAMINA_BAR:
                    moveStaminaSubElement(config, subSelected, dx, dy);
                    break;
                case ARMOR_BAR:
                    moveArmorSubElement(config, subSelected, dx, dy);
                    break;
                case AIR_BAR:
                    moveAirSubElement(config, subSelected, dx, dy);
                    break;
            }
        }
    }
    
    private void handleArrowResize(int keyCode) {
        ClientConfig config = ModConfigManager.getClient();
        DraggableElement focused = EditModeManager.getFocusedElement();
        
        int dw = 0, dh = 0;
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP: dh = -1; break;
            case GLFW.GLFW_KEY_DOWN: dh = 1; break;
            case GLFW.GLFW_KEY_LEFT: dw = -1; break;
            case GLFW.GLFW_KEY_RIGHT: dw = 1; break;
        }
        
        if (focused == null) {
            // Non-focus mode: resize the keyboard-selected element's background
            DraggableElement selected = EditModeManager.getKeyboardSelectedElement();
            if (selected == null) return;
            
            switch (selected) {
                case HEALTH_BAR:
                    config.healthBackgroundWidth = Math.max(10, config.healthBackgroundWidth + dw);
                    config.healthBackgroundHeight = Math.max(4, config.healthBackgroundHeight + dh);
                    break;
                case MANA_BAR:
                    config.manaBackgroundWidth = Math.max(10, config.manaBackgroundWidth + dw);
                    config.manaBackgroundHeight = Math.max(4, config.manaBackgroundHeight + dh);
                    break;
                case STAMINA_BAR:
                    config.staminaBackgroundWidth = Math.max(10, config.staminaBackgroundWidth + dw);
                    config.staminaBackgroundHeight = Math.max(4, config.staminaBackgroundHeight + dh);
                    break;
                case ARMOR_BAR:
                    config.armorBackgroundWidth = Math.max(10, config.armorBackgroundWidth + dw);
                    config.armorBackgroundHeight = Math.max(4, config.armorBackgroundHeight + dh);
                    break;
                case AIR_BAR:
                    config.airBackgroundWidth = Math.max(10, config.airBackgroundWidth + dw);
                    config.airBackgroundHeight = Math.max(4, config.airBackgroundHeight + dh);
                    break;
            }
        } else {
            // Focus mode: resize the keyboard-selected sub-element
            SubElementType subSelected = EditModeManager.getKeyboardSelectedSubElement();
            if (subSelected == null) return;
            
            switch (focused) {
                case HEALTH_BAR:
                    resizeHealthSubElement(config, subSelected, dw, dh);
                    break;
                case MANA_BAR:
                    resizeManaSubElement(config, subSelected, dw, dh);
                    break;
                case STAMINA_BAR:
                    resizeStaminaSubElement(config, subSelected, dw, dh);
                    break;
                case ARMOR_BAR:
                    resizeArmorSubElement(config, subSelected, dw, dh);
                    break;
                case AIR_BAR:
                    resizeAirSubElement(config, subSelected, dw, dh);
                    break;
            }
        }
    }
    
    // Helper methods for moving sub-elements
    private void moveHealthSubElement(ClientConfig config, SubElementType subType, int dx, int dy) {
        switch (subType) {
            case BACKGROUND:
                config.healthBackgroundXOffset += dx;
                config.healthBackgroundYOffset += dy;
                break;
            case BAR_MAIN:
                config.healthBarXOffset += dx;
                config.healthBarYOffset += dy;
                break;
            case FOREGROUND_DETAIL:
                config.healthOverlayXOffset += dx;
                config.healthOverlayYOffset += dy;
                break;
            case TEXT:
                config.healthTextXOffset += dx;
                config.healthTextYOffset += dy;
                break;
            case ABSORPTION_TEXT:
                config.healthAbsorptionTextXOffset += dx;
                config.healthAbsorptionTextYOffset += dy;
                break;
        }
    }
    
    private void moveManaSubElement(ClientConfig config, SubElementType subType, int dx, int dy) {
        switch (subType) {
            case BACKGROUND:
                config.manaBackgroundXOffset += dx;
                config.manaBackgroundYOffset += dy;
                break;
            case BAR_MAIN:
                config.manaBarXOffset += dx;
                config.manaBarYOffset += dy;
                break;
            case FOREGROUND_DETAIL:
                config.manaOverlayXOffset += dx;
                config.manaOverlayYOffset += dy;
                break;
            case TEXT:
                config.manaTextXOffset += dx;
                config.manaTextYOffset += dy;
                break;
        }
    }
    
    private void moveStaminaSubElement(ClientConfig config, SubElementType subType, int dx, int dy) {
        switch (subType) {
            case BACKGROUND:
                config.staminaBackgroundXOffset += dx;
                config.staminaBackgroundYOffset += dy;
                break;
            case BAR_MAIN:
                config.staminaBarXOffset += dx;
                config.staminaBarYOffset += dy;
                break;
            case FOREGROUND_DETAIL:
                config.staminaOverlayXOffset += dx;
                config.staminaOverlayYOffset += dy;
                break;
            case TEXT:
                config.staminaTextXOffset += dx;
                config.staminaTextYOffset += dy;
                break;
        }
    }
    
    private void moveArmorSubElement(ClientConfig config, SubElementType subType, int dx, int dy) {
        switch (subType) {
            case BACKGROUND:
                config.armorBackgroundXOffset += dx;
                config.armorBackgroundYOffset += dy;
                break;
            case BAR_MAIN:
                config.armorBarXOffset += dx;
                config.armorBarYOffset += dy;
                break;
            case TEXT:
                config.armorTextXOffset += dx;
                config.armorTextYOffset += dy;
                break;
            case ICON:
                config.armorIconXOffset += dx;
                config.armorIconYOffset += dy;
                break;
        }
    }
    
    private void moveAirSubElement(ClientConfig config, SubElementType subType, int dx, int dy) {
        switch (subType) {
            case BACKGROUND:
                config.airBackgroundXOffset += dx;
                config.airBackgroundYOffset += dy;
                break;
            case BAR_MAIN:
                config.airBarXOffset += dx;
                config.airBarYOffset += dy;
                break;
            case TEXT:
                config.airTextXOffset += dx;
                config.airTextYOffset += dy;
                break;
            case ICON:
                config.airIconXOffset += dx;
                config.airIconYOffset += dy;
                break;
        }
    }
    
    // Helper methods for resizing sub-elements
    private void resizeHealthSubElement(ClientConfig config, SubElementType subType, int dw, int dh) {
        switch (subType) {
            case BACKGROUND:
                config.healthBackgroundWidth = Math.max(10, config.healthBackgroundWidth + dw);
                config.healthBackgroundHeight = Math.max(4, config.healthBackgroundHeight + dh);
                break;
            case BAR_MAIN:
                config.healthBarWidth = Math.max(4, Math.min(256, config.healthBarWidth + dw));
                config.healthBarHeight = Math.max(1, Math.min(32, config.healthBarHeight + dh));
                break;
            case FOREGROUND_DETAIL:
                config.healthOverlayWidth = Math.max(10, Math.min(256, config.healthOverlayWidth + dw));
                config.healthOverlayHeight = Math.max(4, Math.min(256, config.healthOverlayHeight + dh));
                break;
        }
    }
    
    private void resizeManaSubElement(ClientConfig config, SubElementType subType, int dw, int dh) {
        switch (subType) {
            case BACKGROUND:
                config.manaBackgroundWidth = Math.max(10, config.manaBackgroundWidth + dw);
                config.manaBackgroundHeight = Math.max(4, config.manaBackgroundHeight + dh);
                break;
            case BAR_MAIN:
                config.manaBarWidth = Math.max(4, Math.min(256, config.manaBarWidth + dw));
                config.manaBarHeight = Math.max(1, Math.min(32, config.manaBarHeight + dh));
                break;
            case FOREGROUND_DETAIL:
                config.manaOverlayWidth = Math.max(10, Math.min(256, config.manaOverlayWidth + dw));
                config.manaOverlayHeight = Math.max(4, Math.min(256, config.manaOverlayHeight + dh));
                break;
        }
    }
    
    private void resizeStaminaSubElement(ClientConfig config, SubElementType subType, int dw, int dh) {
        switch (subType) {
            case BACKGROUND:
                config.staminaBackgroundWidth = Math.max(10, config.staminaBackgroundWidth + dw);
                config.staminaBackgroundHeight = Math.max(4, config.staminaBackgroundHeight + dh);
                break;
            case BAR_MAIN:
                config.staminaBarWidth = Math.max(4, Math.min(256, config.staminaBarWidth + dw));
                config.staminaBarHeight = Math.max(1, Math.min(32, config.staminaBarHeight + dh));
                break;
            case FOREGROUND_DETAIL:
                config.staminaOverlayWidth = Math.max(10, Math.min(256, config.staminaOverlayWidth + dw));
                config.staminaOverlayHeight = Math.max(4, Math.min(256, config.staminaOverlayHeight + dh));
                break;
        }
    }
    
    private void resizeArmorSubElement(ClientConfig config, SubElementType subType, int dw, int dh) {
        switch (subType) {
            case BACKGROUND:
                config.armorBackgroundWidth = Math.max(10, config.armorBackgroundWidth + dw);
                config.armorBackgroundHeight = Math.max(4, config.armorBackgroundHeight + dh);
                break;
            case BAR_MAIN:
                config.armorBarWidth = Math.max(4, Math.min(256, config.armorBarWidth + dw));
                config.armorBarHeight = Math.max(1, Math.min(32, config.armorBarHeight + dh));
                break;
        }
    }
    
    private void resizeAirSubElement(ClientConfig config, SubElementType subType, int dw, int dh) {
        switch (subType) {
            case BACKGROUND:
                config.airBackgroundWidth = Math.max(10, config.airBackgroundWidth + dw);
                config.airBackgroundHeight = Math.max(4, config.airBackgroundHeight + dh);
                break;
            case BAR_MAIN:
                config.airBarWidth = Math.max(4, Math.min(256, config.airBarWidth + dw));
                config.airBarHeight = Math.max(1, Math.min(32, config.airBarHeight + dh));
                break;
        }
    }

    protected void clearWidgets() {
        this.renderables.clear();
        this.children().clear();
    }

    private void resetPositionDefaultsAction(DraggableElement element) {
        ClientConfig config = ModConfigManager.getClient();
        if (element == null) return;
        HUDPositioning.BarPlacement defaultAnchor = null;
        int defaultTotalY = 0;
        int defaultBgWidthForCalc = 0; 

        switch (element) {
            case HEALTH_BAR: 
                defaultAnchor = ClientConfig.DEFAULT_HEALTH_BAR_ANCHOR;
                defaultBgWidthForCalc = ClientConfig.DEFAULT_HEALTH_BACKGROUND_WIDTH;
                config.healthBarAnchor = defaultAnchor;
                config.healthBarXOffset = ClientConfig.DEFAULT_HEALTH_BAR_X_OFFSET;
                config.healthBarYOffset = ClientConfig.DEFAULT_HEALTH_BAR_Y_OFFSET;
                config.healthOverlayXOffset = ClientConfig.DEFAULT_HEALTH_OVERLAY_X_OFFSET;
                config.healthOverlayYOffset = ClientConfig.DEFAULT_HEALTH_OVERLAY_Y_OFFSET;
                config.healthBackgroundXOffset = ClientConfig.DEFAULT_HEALTH_BACKGROUND_X_OFFSET;
                config.healthBackgroundYOffset = ClientConfig.DEFAULT_HEALTH_BACKGROUND_Y_OFFSET;
                config.healthTotalYOffset = ClientConfig.DEFAULT_HEALTH_TOTAL_Y_OFFSET;
                config.healthTextXOffset = ClientConfig.DEFAULT_HEALTH_TEXT_X_OFFSET;
                config.healthTextYOffset = ClientConfig.DEFAULT_HEALTH_TEXT_Y_OFFSET;
                config.healthAbsorptionTextXOffset = ClientConfig.DEFAULT_HEALTH_ABSORPTION_TEXT_X_OFFSET;
                config.healthAbsorptionTextYOffset = ClientConfig.DEFAULT_HEALTH_ABSORPTION_TEXT_Y_OFFSET;
                break;
            case STAMINA_BAR:
                defaultAnchor = ClientConfig.DEFAULT_STAMINA_BAR_ANCHOR;
                defaultBgWidthForCalc = ClientConfig.DEFAULT_STAMINA_BACKGROUND_WIDTH;
                config.staminaBarAnchor = defaultAnchor;
                config.staminaBarXOffset = ClientConfig.DEFAULT_STAMINA_BAR_X_OFFSET;
                config.staminaBarYOffset = ClientConfig.DEFAULT_STAMINA_BAR_Y_OFFSET;
                config.staminaOverlayXOffset = ClientConfig.DEFAULT_STAMINA_OVERLAY_X_OFFSET;
                config.staminaOverlayYOffset = ClientConfig.DEFAULT_STAMINA_OVERLAY_Y_OFFSET;
                config.staminaBackgroundXOffset = ClientConfig.DEFAULT_STAMINA_BACKGROUND_X_OFFSET;
                config.staminaBackgroundYOffset = ClientConfig.DEFAULT_STAMINA_BACKGROUND_Y_OFFSET;
                config.staminaTotalYOffset = ClientConfig.DEFAULT_STAMINA_TOTAL_Y_OFFSET;
                config.staminaTextXOffset = ClientConfig.DEFAULT_STAMINA_TEXT_X_OFFSET;
                config.staminaTextYOffset = ClientConfig.DEFAULT_STAMINA_TEXT_Y_OFFSET;
                break;
            case MANA_BAR:
                defaultAnchor = ClientConfig.DEFAULT_MANA_BAR_ANCHOR;
                defaultBgWidthForCalc = ClientConfig.DEFAULT_MANA_BACKGROUND_WIDTH;
                config.manaBarAnchor = defaultAnchor;
                config.manaBarXOffset = ClientConfig.DEFAULT_MANA_BAR_X_OFFSET;
                config.manaBarYOffset = ClientConfig.DEFAULT_MANA_BAR_Y_OFFSET;
                config.manaOverlayXOffset = ClientConfig.DEFAULT_MANA_OVERLAY_X_OFFSET;
                config.manaOverlayYOffset = ClientConfig.DEFAULT_MANA_OVERLAY_Y_OFFSET;
                config.manaBackgroundXOffset = ClientConfig.DEFAULT_MANA_BACKGROUND_X_OFFSET;
                config.manaBackgroundYOffset = ClientConfig.DEFAULT_MANA_BACKGROUND_Y_OFFSET;
                config.manaTotalYOffset = ClientConfig.DEFAULT_MANA_TOTAL_Y_OFFSET;
                config.manaTextXOffset = ClientConfig.DEFAULT_MANA_TEXT_X_OFFSET;
                config.manaTextYOffset = ClientConfig.DEFAULT_MANA_TEXT_Y_OFFSET;
                break;
            case ARMOR_BAR:
                defaultAnchor = ClientConfig.DEFAULT_ARMOR_BAR_ANCHOR;
                defaultBgWidthForCalc = ClientConfig.DEFAULT_ARMOR_BACKGROUND_WIDTH;
                config.armorBarAnchor = defaultAnchor;
                config.armorBarXOffset = ClientConfig.DEFAULT_ARMOR_BAR_X_OFFSET;
                config.armorBarYOffset = ClientConfig.DEFAULT_ARMOR_BAR_Y_OFFSET;
                config.armorTotalYOffset = ClientConfig.DEFAULT_ARMOR_TOTAL_Y_OFFSET;
                config.armorTextXOffset = ClientConfig.DEFAULT_ARMOR_TEXT_X_OFFSET;
                config.armorTextYOffset = ClientConfig.DEFAULT_ARMOR_TEXT_Y_OFFSET;
                config.armorIconXOffset = ClientConfig.DEFAULT_ARMOR_ICON_X_OFFSET;
                config.armorIconYOffset = ClientConfig.DEFAULT_ARMOR_ICON_Y_OFFSET;
                config.armorBackgroundXOffset = ClientConfig.DEFAULT_ARMOR_BACKGROUND_X_OFFSET;
                config.armorBackgroundYOffset = ClientConfig.DEFAULT_ARMOR_BACKGROUND_Y_OFFSET;
                break;
            case AIR_BAR:
                defaultAnchor = ClientConfig.DEFAULT_AIR_BAR_ANCHOR;
                defaultBgWidthForCalc = ClientConfig.DEFAULT_AIR_BACKGROUND_WIDTH;
                config.airBarAnchor = defaultAnchor;
                config.airBarXOffset = ClientConfig.DEFAULT_AIR_BAR_X_OFFSET;
                config.airBarYOffset = ClientConfig.DEFAULT_AIR_BAR_Y_OFFSET;
                config.airTotalYOffset = ClientConfig.DEFAULT_AIR_TOTAL_Y_OFFSET;
                config.airTextXOffset = ClientConfig.DEFAULT_AIR_TEXT_X_OFFSET;
                config.airTextYOffset = ClientConfig.DEFAULT_AIR_TEXT_Y_OFFSET;
                config.airIconXOffset = ClientConfig.DEFAULT_AIR_ICON_X_OFFSET;
                config.airIconYOffset = ClientConfig.DEFAULT_AIR_ICON_Y_OFFSET;
                config.airBackgroundXOffset = ClientConfig.DEFAULT_AIR_BACKGROUND_X_OFFSET;
                config.airBackgroundYOffset = ClientConfig.DEFAULT_AIR_BACKGROUND_Y_OFFSET;
                break;
             default: return; 
        }
        int newDefaultTotalX = 0;
        if (defaultAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultTotalX = -defaultBgWidthForCalc / 2; }
        else if (defaultAnchor != null && defaultAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultTotalX = -defaultBgWidthForCalc; }
        switch(element) {
            case HEALTH_BAR: config.healthTotalXOffset = newDefaultTotalX; break;
            case STAMINA_BAR: config.staminaTotalXOffset = newDefaultTotalX; break;
            case MANA_BAR: config.manaTotalXOffset = newDefaultTotalX; break;
            case ARMOR_BAR: config.armorTotalXOffset = newDefaultTotalX; break;
            case AIR_BAR: config.airTotalXOffset = newDefaultTotalX; break;
        }
        rebuildEditorWidgets(); 
    }

    private void resetSizeDefaultsAction(DraggableElement element) {
        ClientConfig config = ModConfigManager.getClient();
        if (element == null) return;
        switch(element) {
            case HEALTH_BAR: 
                config.healthBackgroundWidth = ClientConfig.DEFAULT_HEALTH_BACKGROUND_WIDTH; config.healthBackgroundHeight = ClientConfig.DEFAULT_HEALTH_BACKGROUND_HEIGHT;
                config.healthBarWidth = ClientConfig.DEFAULT_HEALTH_BAR_WIDTH; config.healthBarHeight = ClientConfig.DEFAULT_HEALTH_BAR_HEIGHT;
                config.healthOverlayWidth = ClientConfig.DEFAULT_HEALTH_OVERLAY_WIDTH; config.healthOverlayHeight = ClientConfig.DEFAULT_HEALTH_OVERLAY_HEIGHT;
                break;
            case STAMINA_BAR: 
                config.staminaBackgroundWidth = ClientConfig.DEFAULT_STAMINA_BACKGROUND_WIDTH; config.staminaBackgroundHeight = ClientConfig.DEFAULT_STAMINA_BACKGROUND_HEIGHT;
                config.staminaBarWidth = ClientConfig.DEFAULT_STAMINA_BAR_WIDTH; config.staminaBarHeight = ClientConfig.DEFAULT_STAMINA_BAR_HEIGHT;
                config.staminaOverlayWidth = ClientConfig.DEFAULT_STAMINA_OVERLAY_WIDTH; config.staminaOverlayHeight = ClientConfig.DEFAULT_STAMINA_OVERLAY_HEIGHT;
                break;
            case MANA_BAR: 
                config.manaBackgroundWidth = ClientConfig.DEFAULT_MANA_BACKGROUND_WIDTH; config.manaBackgroundHeight = ClientConfig.DEFAULT_MANA_BACKGROUND_HEIGHT;
                config.manaBarWidth = ClientConfig.DEFAULT_MANA_BAR_WIDTH; config.manaBarHeight = ClientConfig.DEFAULT_MANA_BAR_HEIGHT;
                config.manaOverlayWidth = ClientConfig.DEFAULT_MANA_OVERLAY_WIDTH; config.manaOverlayHeight = ClientConfig.DEFAULT_MANA_OVERLAY_HEIGHT;
                break;
            case ARMOR_BAR:
                config.armorBackgroundWidth = ClientConfig.DEFAULT_ARMOR_BACKGROUND_WIDTH; config.armorBackgroundHeight = ClientConfig.DEFAULT_ARMOR_BACKGROUND_HEIGHT;
                config.armorBarWidth = ClientConfig.DEFAULT_ARMOR_BAR_WIDTH; config.armorBarHeight = ClientConfig.DEFAULT_ARMOR_BAR_HEIGHT;
                break;
            case AIR_BAR:
                config.airBackgroundWidth = ClientConfig.DEFAULT_AIR_BACKGROUND_WIDTH; config.airBackgroundHeight = ClientConfig.DEFAULT_AIR_BACKGROUND_HEIGHT;
                config.airBarWidth = ClientConfig.DEFAULT_AIR_BAR_WIDTH; config.airBarHeight = ClientConfig.DEFAULT_AIR_BAR_HEIGHT;
                break;
        }
        rebuildEditorWidgets(); 
    }

    private void resetAllDefaultsAction() {
        resetPositionDefaultsAction(DraggableElement.HEALTH_BAR);
        resetSizeDefaultsAction(DraggableElement.HEALTH_BAR);
        resetVisualDefaultsAction(DraggableElement.HEALTH_BAR);
        resetPositionDefaultsAction(DraggableElement.STAMINA_BAR);
        resetSizeDefaultsAction(DraggableElement.STAMINA_BAR);
        resetVisualDefaultsAction(DraggableElement.STAMINA_BAR);
        resetPositionDefaultsAction(DraggableElement.MANA_BAR);
        resetSizeDefaultsAction(DraggableElement.MANA_BAR);
        resetVisualDefaultsAction(DraggableElement.MANA_BAR);
        resetPositionDefaultsAction(DraggableElement.ARMOR_BAR);
        resetSizeDefaultsAction(DraggableElement.ARMOR_BAR);
        resetVisualDefaultsAction(DraggableElement.ARMOR_BAR);
        resetPositionDefaultsAction(DraggableElement.AIR_BAR);
        resetSizeDefaultsAction(DraggableElement.AIR_BAR);
        resetVisualDefaultsAction(DraggableElement.AIR_BAR);
        rebuildEditorWidgets(); 
    }

    private void resetVisualDefaultsAction(DraggableElement element) {
        ClientConfig config = ModConfigManager.getClient();
        if (element == null) return;
        switch(element) {
            case HEALTH_BAR:
                config.enableHealthBar = ClientConfig.DEFAULT_ENABLE_HEALTH_BAR;
                config.enableHealthBackground = ClientConfig.DEFAULT_ENABLE_HEALTH_BACKGROUND;
                config.enableHealthForeground = ClientConfig.DEFAULT_ENABLE_HEALTH_FOREGROUND;
                config.fadeHealthWhenFull = ClientConfig.DEFAULT_FADE_HEALTH_WHEN_FULL;
                config.showHealthText = ClientConfig.DEFAULT_SHOW_HEALTH_TEXT;
                config.healthTextAlign = ClientConfig.DEFAULT_HEALTH_TEXT_ALIGN;
                config.healthFillDirection = ClientConfig.DEFAULT_HEALTH_FILL_DIRECTION;
                break;
            case STAMINA_BAR:
                config.staminaBarBehavior = ClientConfig.DEFAULT_STAMINA_BAR_BEHAVIOR;
                config.enableStaminaBackground = ClientConfig.DEFAULT_ENABLE_STAMINA_BACKGROUND;
                config.enableStaminaForeground = ClientConfig.DEFAULT_ENABLE_STAMINA_FOREGROUND;
                config.fadeStaminaWhenFull = ClientConfig.DEFAULT_FADE_STAMINA_WHEN_FULL;
                config.showStaminaText = ClientConfig.DEFAULT_SHOW_STAMINA_TEXT;
                config.staminaTextAlign = ClientConfig.DEFAULT_STAMINA_TEXT_ALIGN;
                config.staminaFillDirection = ClientConfig.DEFAULT_STAMINA_FILL_DIRECTION;
                break;
            case MANA_BAR:
                config.manaBarBehavior = ClientConfig.DEFAULT_MANA_BAR_BEHAVIOR;
                config.enableManaBackground = ClientConfig.DEFAULT_ENABLE_MANA_BACKGROUND;
                config.enableManaForeground = ClientConfig.DEFAULT_ENABLE_MANA_FOREGROUND;
                config.fadeManaWhenFull = ClientConfig.DEFAULT_FADE_MANA_WHEN_FULL;
                config.showManaText = ClientConfig.DEFAULT_SHOW_MANA_TEXT;
                config.manaTextAlign = ClientConfig.DEFAULT_MANA_TEXT_ALIGN;
                config.manaFillDirection = ClientConfig.DEFAULT_MANA_FILL_DIRECTION;
                break;
            case ARMOR_BAR:
                config.armorBarBehavior = ClientConfig.DEFAULT_ARMOR_BAR_BEHAVIOR;
                config.enableArmorIcon = ClientConfig.DEFAULT_ENABLE_ARMOR_ICON;
                config.showArmorText = ClientConfig.DEFAULT_SHOW_ARMOR_TEXT;
                config.armorTextAlign = ClientConfig.DEFAULT_ARMOR_TEXT_ALIGN;
                break;
            case AIR_BAR:
                config.airBarBehavior = ClientConfig.DEFAULT_AIR_BAR_BEHAVIOR;
                config.enableAirIcon = ClientConfig.DEFAULT_ENABLE_AIR_ICON;
                config.showAirText = ClientConfig.DEFAULT_SHOW_AIR_TEXT;
                config.airTextAlign = ClientConfig.DEFAULT_AIR_TEXT_ALIGN;
                break;
        }
    }

    private void openMultiLineConfirmScreen(Component title, Runnable confirmAction) {
        this.minecraft.setScreen(new MultiLineConfirmResetScreen(this, title, confirmAction));
    }

    private Component getFriendlyElementName(DraggableElement element) {
        if (element == null) return Component.literal("None");
        return switch (element) {
            case HEALTH_BAR -> Component.translatable("gui.dynamic_resource_bars.element.health");
            case MANA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.mana");
            case STAMINA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.stamina");
            case ARMOR_BAR -> Component.translatable("gui.dynamic_resource_bars.element.armor");
            case AIR_BAR -> Component.translatable("gui.dynamic_resource_bars.element.air");
            default -> Component.literal("Unknown Element");
        };
    }


    // Method to get outline color for sub-elements
    private int getOutlineColorForSubElement(DraggableElement focusedElement, SubElementType subElementType) {
        if (focusedElement == null || subElementType == null) {
            return 0xFFFFFFFF; // Default white if something is wrong
        }
        switch (subElementType) {
            case BACKGROUND:
                return 0xA0FFFF00; // Yellowish for background focus
            case BAR_MAIN:
                switch (focusedElement) {
                    case HEALTH_BAR: return 0xA000FF00; // Green
                    case MANA_BAR: return 0xA000FFFF;   // Cyan
                    case STAMINA_BAR: return 0xA0FFA500; // Orange
                    case ARMOR_BAR: return 0xA0C0C0C0; // Light Gray for Armor Bar
                    case AIR_BAR: return 0xA0ADD8E6;   // Light Blue for Air Bar
                    default: return 0xFFFFFFFF;
                }
            case FOREGROUND_DETAIL:
                return 0xA0FF00FF; // Magenta
            default: // Should not happen with current handle setup
                return 0xFFFFFFFF;
        }
    }

    private ManaBarBehavior getNextAvailableManaBarBehavior(ManaBarBehavior current) {
        ManaBarBehavior next = current;
        int attempts = 0;
        do {
            next = next.getNext();
            attempts++;
            // Prevent infinite loop if no mods are loaded
            if (attempts > ManaBarBehavior.values().length) {
                return ManaBarBehavior.OFF;
            }
        } while (next != ManaBarBehavior.OFF && !isManaModAvailable(next));
        
        return next;
    }
    
    private boolean isManaModAvailable(ManaBarBehavior behavior) {
        switch (behavior) {
            case OFF:
                return true;
            case IRONS_SPELLBOOKS:
                return PlatformUtil.isModLoaded("irons_spellbooks");
            case ARS_NOUVEAU:
                return PlatformUtil.isModLoaded("ars_nouveau");
            case RPG_MANA:
                return PlatformUtil.isModLoaded("rpgmana");
            case MANA_ATTRIBUTES:
                return PlatformUtil.isModLoaded("manaattributes");
            default:
                return false;
        }
    }

    
    private StaminaBarBehavior getNextAvailableStaminaBarBehavior(StaminaBarBehavior current) {
        StaminaBarBehavior next = current.getNext();
        int attempts = 0;
        while (!isStaminaProviderAvailable(next) && attempts < StaminaBarBehavior.values().length) {
            next = next.getNext();
            attempts++;
        }
        return next;
    }
    
    private boolean isStaminaProviderAvailable(StaminaBarBehavior behavior) {
        return switch (behavior) {
            case OFF, FOOD -> true; // Always available
            case STAMINA_ATTRIBUTES -> StaminaProviderManager.isModLoaded("staminaattributes");
        };
    }
    
    /**
     * Creates a context menu for sub-elements (layers like background, bar, foreground, text, etc.)
     */
    private ContextMenu createSubElementContextMenu(int mouseX, int mouseY, DraggableElement element, SubElementType subElement) {
        List<ContextMenuItem> items = new ArrayList<>();
        ClientConfig config = ModConfigManager.getClient();
        
        // Title: Layer Name
        Component layerName = getSubElementName(subElement);
        items.add(ContextMenuItem.title(layerName));
        
        // Hide/Show toggle (only for toggleable sub-elements)
        if (canToggleSubElement(element, subElement)) {
            boolean isVisible = isSubElementVisible(element, subElement, config);
            Component visibilityLabel = Component.translatable(
                isVisible ? "gui.dynamic_resource_bars.context.hide_layer" : "gui.dynamic_resource_bars.context.show_layer"
            );
            Component statusText = isVisible ? 
                Component.translatable("gui.dynamic_resource_bars.context.status.shown").withStyle(style -> style.withColor(0x00FF00)) :
                Component.translatable("gui.dynamic_resource_bars.context.status.hidden").withStyle(style -> style.withColor(0xFF0000));
            
            items.add(new ContextMenuItem(
                Component.literal("").append(visibilityLabel).append(" (").append(statusText).append(")"),
                (item) -> {
                    toggleSubElementVisibility(element, subElement, config);
                    reopenContextMenuAfterRebuild();
                }
            ));
        }
        
        // Text settings (if this is a TEXT sub-element)
        if (subElement == SubElementType.TEXT) {
            addTextSettingsForSubElement(items, element, config);
        }
        
        // Manually set Size/Position
        items.add(new ContextMenuItem(
            Component.translatable("gui.dynamic_resource_bars.context.set_size_position"),
            (item) -> openManualSizePositionDialog(element, subElement)
        ));
        
        // Reset to default
        items.add(new ContextMenuItem(
            Component.translatable("gui.dynamic_resource_bars.context.reset_layer"),
            (item) -> {
                resetSubElementToDefaults(element, subElement, config);
                reopenContextMenuAfterRebuild();
            }
        ));
        
        return new ContextMenu(mouseX, mouseY, items);
    }
    
    /**
     * Creates a context menu for main elements (health_bar, stamina_bar, etc.)
     */
    private ContextMenu createMainElementContextMenu(int mouseX, int mouseY, DraggableElement element) {
        List<ContextMenuItem> items = new ArrayList<>();
        ClientConfig config = ModConfigManager.getClient();
        
        // Title: Group Name
        Component groupName = getFriendlyElementName(element);
        items.add(ContextMenuItem.title(groupName));
        
        // Set source/toggle behavior
        Component sourceLabel = Component.translatable("gui.dynamic_resource_bars.context.source_behavior");
        Component currentSource = getCurrentSourceBehavior(element, config);
        boolean canChangeSource = true;
        
        // Disable for mana if no mana provider mods are loaded
        if (element == DraggableElement.MANA_BAR && !ManaProviderManager.hasAnyManaMods()) {
            canChangeSource = false;
        }
        
        items.add(new ContextMenuItem(
            Component.literal("").append(sourceLabel).append(": ").append(currentSource),
                (item) -> {
                cycleSourceBehavior(element, config);
                reopenContextMenuAfterRebuild();
            },
            canChangeSource
        ));
        
        // Element-specific options (fade, text, fill direction)
        addElementSpecificOptions(items, element, config);
        
        // Anchor point
        Component anchorLabel = Component.translatable("gui.dynamic_resource_bars.context.anchor_point");
        Component currentAnchor = getAnchorName(element, config);
        items.add(new ContextMenuItem(
            Component.literal("").append(anchorLabel).append(": ").append(currentAnchor),
            (item) -> {
                cycleAnchorPoint(element, config);
                reopenContextMenuAfterRebuild();
            }
        ));
        
        // Manually set position offsets
        items.add(new ContextMenuItem(
            Component.translatable("gui.dynamic_resource_bars.context.set_position"),
            (item) -> openManualPositionDialog(element)
        ));
        
        // Reset to default (with confirmation)
        items.add(new ContextMenuItem(
            Component.translatable("gui.dynamic_resource_bars.context.reset_element"),
            (item) -> {
                Component title = Component.translatable("gui.dynamic_resource_bars.confirm.reset_element.title", groupName);
                openMultiLineConfirmScreen(title, () -> {
                    resetElementToDefaults(element, config);
                    rebuildEditorWidgets();
                });
            }
        ));
        
        // Open Settings (focus element) - at the bottom
        boolean canOpenSettings = isElementEnabled(element, config);
        items.add(new ContextMenuItem(
            Component.translatable("gui.dynamic_resource_bars.context.open_settings"),
            (item) -> {
                // Clear context menu tracking since we're entering a different mode
                contextMenuElement = null;
                contextMenuSubElement = null;
                EditModeManager.setFocusedElement(element);
                rebuildEditorWidgets();
            },
            canOpenSettings
        ));
        
        return new ContextMenu(mouseX, mouseY, items);
    }
    
    private void addElementSpecificOptions(List<ContextMenuItem> items, DraggableElement element, ClientConfig config) {
        // Fade when full
        if (element == DraggableElement.HEALTH_BAR || element == DraggableElement.STAMINA_BAR || element == DraggableElement.MANA_BAR) {
            boolean fadeEnabled = switch (element) {
                case HEALTH_BAR -> config.fadeHealthWhenFull;
                case STAMINA_BAR -> config.fadeStaminaWhenFull;
                case MANA_BAR -> config.fadeManaWhenFull;
                default -> false;
            };
            
            items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.fade_when_full", 
                    fadeEnabled ? Component.translatable("options.on") : Component.translatable("options.off")),
                (item) -> {
                    switch (element) {
                        case HEALTH_BAR: config.fadeHealthWhenFull = !config.fadeHealthWhenFull; break;
                        case STAMINA_BAR: config.fadeStaminaWhenFull = !config.fadeStaminaWhenFull; break;
                        case MANA_BAR: config.fadeManaWhenFull = !config.fadeManaWhenFull; break;
                    }
                    reopenContextMenuAfterRebuild();
                }
            ));
        }
        
        // Text behavior
        if (element == DraggableElement.HEALTH_BAR || element == DraggableElement.STAMINA_BAR || element == DraggableElement.MANA_BAR) {
            TextBehavior textBehavior = switch (element) {
                case HEALTH_BAR -> config.showHealthText;
                case STAMINA_BAR -> config.showStaminaText;
                case MANA_BAR -> config.showManaText;
                default -> TextBehavior.NEVER;
            };
            
            items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.text_behavior", 
                    Component.translatable("text_behavior." + textBehavior.name().toLowerCase())),
                (item) -> {
                    TextBehavior nextBehavior = getNextTextBehavior(textBehavior);
                    switch (element) {
                        case HEALTH_BAR: config.showHealthText = nextBehavior; break;
                        case STAMINA_BAR: config.showStaminaText = nextBehavior; break;
                        case MANA_BAR: config.showManaText = nextBehavior; break;
                    }
                    reopenContextMenuAfterRebuild();
                }
            ));
            
            // Text alignment
            HorizontalAlignment textAlign = switch (element) {
                case HEALTH_BAR -> config.healthTextAlign;
                case STAMINA_BAR -> config.staminaTextAlign;
                case MANA_BAR -> config.manaTextAlign;
                default -> HorizontalAlignment.CENTER;
            };
            
            items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.text_align", 
                    Component.translatable("horizontal_alignment." + textAlign.name().toLowerCase())),
                (item) -> {
                    HorizontalAlignment nextAlign = getNextHorizontalAlignment(textAlign);
                    switch (element) {
                        case HEALTH_BAR: config.healthTextAlign = nextAlign; break;
                        case STAMINA_BAR: config.staminaTextAlign = nextAlign; break;
                        case MANA_BAR: config.manaTextAlign = nextAlign; break;
                    }
                    reopenContextMenuAfterRebuild();
                }
            ));
        }
        
        // Fill direction
        if (element == DraggableElement.HEALTH_BAR || element == DraggableElement.STAMINA_BAR || element == DraggableElement.MANA_BAR) {
            FillDirection fillDirection = switch (element) {
                case HEALTH_BAR -> config.healthFillDirection;
                case STAMINA_BAR -> config.staminaFillDirection;
                case MANA_BAR -> config.manaFillDirection;
                default -> FillDirection.HORIZONTAL;
            };
            
            items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.fill_direction", 
                    Component.translatable("fill_direction." + fillDirection.name().toLowerCase())),
                (item) -> {
                    FillDirection nextDirection = fillDirection == FillDirection.HORIZONTAL ? FillDirection.VERTICAL : FillDirection.HORIZONTAL;
                    switch (element) {
                        case HEALTH_BAR: config.healthFillDirection = nextDirection; break;
                        case STAMINA_BAR: config.staminaFillDirection = nextDirection; break;
                        case MANA_BAR: config.manaFillDirection = nextDirection; break;
                    }
                    reopenContextMenuAfterRebuild();
                }
            ));
        }
    }
    
    private void addTextSettingsForSubElement(List<ContextMenuItem> items, DraggableElement element, ClientConfig config) {
        // Only for elements that support text
        if (element == DraggableElement.HEALTH_BAR || element == DraggableElement.STAMINA_BAR || element == DraggableElement.MANA_BAR) {
            TextBehavior textBehavior = switch (element) {
                case HEALTH_BAR -> config.showHealthText;
                case STAMINA_BAR -> config.showStaminaText;
                case MANA_BAR -> config.showManaText;
                default -> TextBehavior.NEVER;
            };
            
            items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.text_behavior", 
                    Component.translatable("text_behavior." + textBehavior.name().toLowerCase())),
                (item) -> {
                    TextBehavior nextBehavior = getNextTextBehavior(textBehavior);
                    switch (element) {
                        case HEALTH_BAR: config.showHealthText = nextBehavior; break;
                        case STAMINA_BAR: config.showStaminaText = nextBehavior; break;
                        case MANA_BAR: config.showManaText = nextBehavior; break;
                    }
                    reopenContextMenuAfterRebuild();
                }
            ));
            
            // Text alignment
            HorizontalAlignment textAlign = switch (element) {
                case HEALTH_BAR -> config.healthTextAlign;
                case STAMINA_BAR -> config.staminaTextAlign;
                case MANA_BAR -> config.manaTextAlign;
                default -> HorizontalAlignment.CENTER;
            };
            
            items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.text_align", 
                    Component.translatable("horizontal_alignment." + textAlign.name().toLowerCase())),
                (item) -> {
                    HorizontalAlignment nextAlign = getNextHorizontalAlignment(textAlign);
                    switch (element) {
                        case HEALTH_BAR: config.healthTextAlign = nextAlign; break;
                        case STAMINA_BAR: config.staminaTextAlign = nextAlign; break;
                        case MANA_BAR: config.manaTextAlign = nextAlign; break;
                    }
                    reopenContextMenuAfterRebuild();
                }
            ));
        }
    }
    
    private void reopenContextMenuAfterRebuild() {
        rebuildEditorWidgets();
        
        // Recreate the context menu at the same position
        if (contextMenuElement != null) {
            if (contextMenuSubElement != null) {
                activeContextMenu = createSubElementContextMenu(contextMenuX, contextMenuY, contextMenuElement, contextMenuSubElement);
            } else {
                activeContextMenu = createMainElementContextMenu(contextMenuX, contextMenuY, contextMenuElement);
            }
            
            if (activeContextMenu != null) {
                activeContextMenu.setOnClose(() -> {
                    activeContextMenu = null;
                    contextMenuElement = null;
                    contextMenuSubElement = null;
                });
            }
        }
    }
    
    private boolean isElementEnabled(DraggableElement element, ClientConfig config) {
        return switch (element) {
            case HEALTH_BAR -> config.enableHealthBar;
            case STAMINA_BAR -> config.staminaBarBehavior != StaminaBarBehavior.OFF;
            case MANA_BAR -> config.manaBarBehavior != ManaBarBehavior.OFF;
            case ARMOR_BAR -> config.armorBarBehavior == BarRenderBehavior.CUSTOM;
            case AIR_BAR -> config.airBarBehavior == BarRenderBehavior.CUSTOM;
        };
    }
    
    private Component getSubElementName(SubElementType subElement) {
        return switch (subElement) {
            case BACKGROUND -> Component.translatable("gui.dynamic_resource_bars.subelement.background");
            case BAR_MAIN -> Component.translatable("gui.dynamic_resource_bars.subelement.bar_main");
            case FOREGROUND_DETAIL -> Component.translatable("gui.dynamic_resource_bars.subelement.foreground");
            case TEXT -> Component.translatable("gui.dynamic_resource_bars.subelement.text");
            case ICON -> Component.translatable("gui.dynamic_resource_bars.subelement.icon");
            case ABSORPTION_TEXT -> Component.translatable("gui.dynamic_resource_bars.subelement.absorption_text");
        };
    }
    
    private boolean canToggleSubElement(DraggableElement element, SubElementType subElement) {
        // BAR_MAIN and TEXT can't be toggled off (they're core)
        // ABSORPTION_TEXT can't be toggled (it's shown based on game state)
        return subElement != SubElementType.BAR_MAIN && 
               subElement != SubElementType.TEXT &&
               subElement != SubElementType.ABSORPTION_TEXT;
    }
    
    private boolean isSubElementVisible(DraggableElement element, SubElementType subElement, ClientConfig config) {
        return switch (element) {
            case HEALTH_BAR -> switch (subElement) {
                case BACKGROUND -> config.enableHealthBackground;
                case FOREGROUND_DETAIL -> config.enableHealthForeground;
                default -> true;
            };
            case STAMINA_BAR -> switch (subElement) {
                case BACKGROUND -> config.enableStaminaBackground;
                case FOREGROUND_DETAIL -> config.enableStaminaForeground;
                default -> true;
            };
            case MANA_BAR -> switch (subElement) {
                case BACKGROUND -> config.enableManaBackground;
                case FOREGROUND_DETAIL -> config.enableManaForeground;
                default -> true;
            };
            case ARMOR_BAR -> switch (subElement) {
                case ICON -> config.enableArmorIcon;
                default -> true;
            };
            case AIR_BAR -> switch (subElement) {
                case ICON -> config.enableAirIcon;
                default -> true;
            };
        };
    }
    
    private void toggleSubElementVisibility(DraggableElement element, SubElementType subElement, ClientConfig config) {
        switch (element) {
            case HEALTH_BAR:
                if (subElement == SubElementType.BACKGROUND) config.enableHealthBackground = !config.enableHealthBackground;
                else if (subElement == SubElementType.FOREGROUND_DETAIL) config.enableHealthForeground = !config.enableHealthForeground;
                break;
            case STAMINA_BAR:
                if (subElement == SubElementType.BACKGROUND) config.enableStaminaBackground = !config.enableStaminaBackground;
                else if (subElement == SubElementType.FOREGROUND_DETAIL) config.enableStaminaForeground = !config.enableStaminaForeground;
                break;
            case MANA_BAR:
                if (subElement == SubElementType.BACKGROUND) config.enableManaBackground = !config.enableManaBackground;
                else if (subElement == SubElementType.FOREGROUND_DETAIL) config.enableManaForeground = !config.enableManaForeground;
                break;
            case ARMOR_BAR:
                if (subElement == SubElementType.ICON) config.enableArmorIcon = !config.enableArmorIcon;
                break;
            case AIR_BAR:
                if (subElement == SubElementType.ICON) config.enableAirIcon = !config.enableAirIcon;
                break;
        }
    }
    
    private Component getCurrentSourceBehavior(DraggableElement element, ClientConfig config) {
        return switch (element) {
            case HEALTH_BAR -> config.enableHealthBar ? 
                Component.translatable("gui.dynamic_resource_bars.behavior.custom_simple") : 
                Component.translatable("gui.dynamic_resource_bars.behavior.vanilla_simple");
            case STAMINA_BAR -> Component.translatable(config.staminaBarBehavior.getTranslationKey());
            case MANA_BAR -> Component.translatable(config.manaBarBehavior.getTranslationKey());
            case ARMOR_BAR -> Component.translatable("bar_behavior." + config.armorBarBehavior.toString().toLowerCase());
            case AIR_BAR -> Component.translatable("bar_behavior." + config.airBarBehavior.toString().toLowerCase());
        };
    }
    
    private void cycleSourceBehavior(DraggableElement element, ClientConfig config) {
        switch (element) {
            case HEALTH_BAR: 
                config.enableHealthBar = !config.enableHealthBar;
                break;
            case STAMINA_BAR: 
                config.staminaBarBehavior = config.staminaBarBehavior.getNext();
                break;
            case MANA_BAR: 
                config.manaBarBehavior = getNextAvailableManaBarBehavior(config.manaBarBehavior);
                ManaProviderManager.updateActiveProvider();
                break;
            case ARMOR_BAR: 
                config.armorBarBehavior = switch (config.armorBarBehavior) {
                    case VANILLA -> BarRenderBehavior.CUSTOM;
                    case CUSTOM -> BarRenderBehavior.HIDDEN;
                    case HIDDEN -> BarRenderBehavior.VANILLA;
                };
                break;
            case AIR_BAR: 
                config.airBarBehavior = switch (config.airBarBehavior) {
                    case VANILLA -> BarRenderBehavior.CUSTOM;
                    case CUSTOM -> BarRenderBehavior.HIDDEN;
                    case HIDDEN -> BarRenderBehavior.VANILLA;
                };
                break;
        }
    }
    
    private Component getAnchorName(DraggableElement element, ClientConfig config) {
        HUDPositioning.BarPlacement anchor = switch (element) {
            case HEALTH_BAR -> config.healthBarAnchor;
            case STAMINA_BAR -> config.staminaBarAnchor;
            case MANA_BAR -> config.manaBarAnchor;
            case ARMOR_BAR -> config.armorBarAnchor;
            case AIR_BAR -> config.airBarAnchor;
        };
        return Component.translatable("gui.dynamic_resource_bars.anchor." + anchor.name().toLowerCase());
    }
    
    private void cycleAnchorPoint(DraggableElement element, ClientConfig config) {
        switch (element) {
            case HEALTH_BAR: 
                config.healthBarAnchor = config.healthBarAnchor.getNext();
                break;
            case STAMINA_BAR: 
                config.staminaBarAnchor = config.staminaBarAnchor.getNext();
                break;
            case MANA_BAR: 
                config.manaBarAnchor = config.manaBarAnchor.getNext();
                break;
            case ARMOR_BAR: 
                config.armorBarAnchor = config.armorBarAnchor.getNext();
                break;
            case AIR_BAR: 
                config.airBarAnchor = config.airBarAnchor.getNext();
                break;
        }
    }
    
    private void openManualSizePositionDialog(DraggableElement element, SubElementType subElement) {
        this.minecraft.setScreen(new ManualSizePositionScreen(this, element, subElement));
    }
    
    private void openManualPositionDialog(DraggableElement element) {
        this.minecraft.setScreen(new ManualPositionScreen(this, element));
    }
    
    private void resetSubElementToDefaults(DraggableElement element, SubElementType subElement, ClientConfig config) {
        switch (element) {
            case HEALTH_BAR: 
                resetHealthSubElement(subElement, config);
                break;
            case STAMINA_BAR: 
                resetStaminaSubElement(subElement, config);
                break;
            case MANA_BAR: 
                resetManaSubElement(subElement, config);
                break;
            case ARMOR_BAR: 
                resetArmorSubElement(subElement, config);
                break;
            case AIR_BAR: 
                resetAirSubElement(subElement, config);
                break;
        }
    }
    
    private void resetHealthSubElement(SubElementType subElement, ClientConfig config) {
        switch (subElement) {
            case BACKGROUND -> {
                config.healthBackgroundWidth = ClientConfig.DEFAULT_HEALTH_BACKGROUND_WIDTH;
                config.healthBackgroundHeight = ClientConfig.DEFAULT_HEALTH_BACKGROUND_HEIGHT;
                config.healthBackgroundXOffset = ClientConfig.DEFAULT_HEALTH_BACKGROUND_X_OFFSET;
                config.healthBackgroundYOffset = ClientConfig.DEFAULT_HEALTH_BACKGROUND_Y_OFFSET;
            }
            case BAR_MAIN -> {
                config.healthBarWidth = ClientConfig.DEFAULT_HEALTH_BAR_WIDTH;
                config.healthBarHeight = ClientConfig.DEFAULT_HEALTH_BAR_HEIGHT;
                config.healthBarXOffset = ClientConfig.DEFAULT_HEALTH_BAR_X_OFFSET;
                config.healthBarYOffset = ClientConfig.DEFAULT_HEALTH_BAR_Y_OFFSET;
            }
            case FOREGROUND_DETAIL -> {
                config.healthOverlayWidth = ClientConfig.DEFAULT_HEALTH_OVERLAY_WIDTH;
                config.healthOverlayHeight = ClientConfig.DEFAULT_HEALTH_OVERLAY_HEIGHT;
                config.healthOverlayXOffset = ClientConfig.DEFAULT_HEALTH_OVERLAY_X_OFFSET;
                config.healthOverlayYOffset = ClientConfig.DEFAULT_HEALTH_OVERLAY_Y_OFFSET;
            }
            case TEXT -> {
                config.healthTextXOffset = ClientConfig.DEFAULT_HEALTH_TEXT_X_OFFSET;
                config.healthTextYOffset = ClientConfig.DEFAULT_HEALTH_TEXT_Y_OFFSET;
            }
            case ABSORPTION_TEXT -> {
                config.healthAbsorptionTextXOffset = ClientConfig.DEFAULT_HEALTH_ABSORPTION_TEXT_X_OFFSET;
                config.healthAbsorptionTextYOffset = ClientConfig.DEFAULT_HEALTH_ABSORPTION_TEXT_Y_OFFSET;
            }
        }
    }
    
    private void resetStaminaSubElement(SubElementType subElement, ClientConfig config) {
        switch (subElement) {
            case BACKGROUND -> {
                config.staminaBackgroundWidth = ClientConfig.DEFAULT_STAMINA_BACKGROUND_WIDTH;
                config.staminaBackgroundHeight = ClientConfig.DEFAULT_STAMINA_BACKGROUND_HEIGHT;
                config.staminaBackgroundXOffset = ClientConfig.DEFAULT_STAMINA_BACKGROUND_X_OFFSET;
                config.staminaBackgroundYOffset = ClientConfig.DEFAULT_STAMINA_BACKGROUND_Y_OFFSET;
            }
            case BAR_MAIN -> {
                config.staminaBarWidth = ClientConfig.DEFAULT_STAMINA_BAR_WIDTH;
                config.staminaBarHeight = ClientConfig.DEFAULT_STAMINA_BAR_HEIGHT;
                config.staminaBarXOffset = ClientConfig.DEFAULT_STAMINA_BAR_X_OFFSET;
                config.staminaBarYOffset = ClientConfig.DEFAULT_STAMINA_BAR_Y_OFFSET;
            }
            case FOREGROUND_DETAIL -> {
                config.staminaOverlayWidth = ClientConfig.DEFAULT_STAMINA_OVERLAY_WIDTH;
                config.staminaOverlayHeight = ClientConfig.DEFAULT_STAMINA_OVERLAY_HEIGHT;
                config.staminaOverlayXOffset = ClientConfig.DEFAULT_STAMINA_OVERLAY_X_OFFSET;
                config.staminaOverlayYOffset = ClientConfig.DEFAULT_STAMINA_OVERLAY_Y_OFFSET;
            }
            case TEXT -> {
                config.staminaTextXOffset = ClientConfig.DEFAULT_STAMINA_TEXT_X_OFFSET;
                config.staminaTextYOffset = ClientConfig.DEFAULT_STAMINA_TEXT_Y_OFFSET;
            }
        }
    }
    
    private void resetManaSubElement(SubElementType subElement, ClientConfig config) {
        switch (subElement) {
            case BACKGROUND -> {
                config.manaBackgroundWidth = ClientConfig.DEFAULT_MANA_BACKGROUND_WIDTH;
                config.manaBackgroundHeight = ClientConfig.DEFAULT_MANA_BACKGROUND_HEIGHT;
                config.manaBackgroundXOffset = ClientConfig.DEFAULT_MANA_BACKGROUND_X_OFFSET;
                config.manaBackgroundYOffset = ClientConfig.DEFAULT_MANA_BACKGROUND_Y_OFFSET;
            }
            case BAR_MAIN -> {
                config.manaBarWidth = ClientConfig.DEFAULT_MANA_BAR_WIDTH;
                config.manaBarHeight = ClientConfig.DEFAULT_MANA_BAR_HEIGHT;
                config.manaBarXOffset = ClientConfig.DEFAULT_MANA_BAR_X_OFFSET;
                config.manaBarYOffset = ClientConfig.DEFAULT_MANA_BAR_Y_OFFSET;
            }
            case FOREGROUND_DETAIL -> {
                config.manaOverlayWidth = ClientConfig.DEFAULT_MANA_OVERLAY_WIDTH;
                config.manaOverlayHeight = ClientConfig.DEFAULT_MANA_OVERLAY_HEIGHT;
                config.manaOverlayXOffset = ClientConfig.DEFAULT_MANA_OVERLAY_X_OFFSET;
                config.manaOverlayYOffset = ClientConfig.DEFAULT_MANA_OVERLAY_Y_OFFSET;
            }
            case TEXT -> {
                config.manaTextXOffset = ClientConfig.DEFAULT_MANA_TEXT_X_OFFSET;
                config.manaTextYOffset = ClientConfig.DEFAULT_MANA_TEXT_Y_OFFSET;
            }
        }
    }
    
    private void resetArmorSubElement(SubElementType subElement, ClientConfig config) {
        switch (subElement) {
            case BACKGROUND -> {
                config.armorBackgroundWidth = ClientConfig.DEFAULT_ARMOR_BACKGROUND_WIDTH;
                config.armorBackgroundHeight = ClientConfig.DEFAULT_ARMOR_BACKGROUND_HEIGHT;
                config.armorBackgroundXOffset = ClientConfig.DEFAULT_ARMOR_BACKGROUND_X_OFFSET;
                config.armorBackgroundYOffset = ClientConfig.DEFAULT_ARMOR_BACKGROUND_Y_OFFSET;
            }
            case BAR_MAIN -> {
                config.armorBarWidth = ClientConfig.DEFAULT_ARMOR_BAR_WIDTH;
                config.armorBarHeight = ClientConfig.DEFAULT_ARMOR_BAR_HEIGHT;
                config.armorBarXOffset = ClientConfig.DEFAULT_ARMOR_BAR_X_OFFSET;
                config.armorBarYOffset = ClientConfig.DEFAULT_ARMOR_BAR_Y_OFFSET;
            }
            case TEXT -> {
                config.armorTextXOffset = ClientConfig.DEFAULT_ARMOR_TEXT_X_OFFSET;
                config.armorTextYOffset = ClientConfig.DEFAULT_ARMOR_TEXT_Y_OFFSET;
            }
            case ICON -> {
                config.armorIconXOffset = ClientConfig.DEFAULT_ARMOR_ICON_X_OFFSET;
                config.armorIconYOffset = ClientConfig.DEFAULT_ARMOR_ICON_Y_OFFSET;
            }
        }
    }
    
    private void resetAirSubElement(SubElementType subElement, ClientConfig config) {
        switch (subElement) {
            case BACKGROUND -> {
                config.airBackgroundWidth = ClientConfig.DEFAULT_AIR_BACKGROUND_WIDTH;
                config.airBackgroundHeight = ClientConfig.DEFAULT_AIR_BACKGROUND_HEIGHT;
                config.airBackgroundXOffset = ClientConfig.DEFAULT_AIR_BACKGROUND_X_OFFSET;
                config.airBackgroundYOffset = ClientConfig.DEFAULT_AIR_BACKGROUND_Y_OFFSET;
            }
            case BAR_MAIN -> {
                config.airBarWidth = ClientConfig.DEFAULT_AIR_BAR_WIDTH;
                config.airBarHeight = ClientConfig.DEFAULT_AIR_BAR_HEIGHT;
                config.airBarXOffset = ClientConfig.DEFAULT_AIR_BAR_X_OFFSET;
                config.airBarYOffset = ClientConfig.DEFAULT_AIR_BAR_Y_OFFSET;
            }
            case TEXT -> {
                config.airTextXOffset = ClientConfig.DEFAULT_AIR_TEXT_X_OFFSET;
                config.airTextYOffset = ClientConfig.DEFAULT_AIR_TEXT_Y_OFFSET;
            }
            case ICON -> {
                config.airIconXOffset = ClientConfig.DEFAULT_AIR_ICON_X_OFFSET;
                config.airIconYOffset = ClientConfig.DEFAULT_AIR_ICON_Y_OFFSET;
            }
        }
    }
    
    private void resetElementToDefaults(DraggableElement element, ClientConfig config) {
        switch (element) {
            case HEALTH_BAR: 
                resetHealthDefaults(config);
                break;
            case STAMINA_BAR: 
                resetStaminaDefaults(config);
                break;
            case MANA_BAR: 
                resetManaDefaults(config);
                break;
            case ARMOR_BAR: 
                resetArmorDefaults(config);
                break;
            case AIR_BAR: 
                resetAirDefaults(config);
                break;
        }
    }
    
    private void resetHealthDefaults(ClientConfig config) {
        resetPositionDefaultsAction(DraggableElement.HEALTH_BAR);
        resetSizeDefaultsAction(DraggableElement.HEALTH_BAR);
        resetVisualDefaultsAction(DraggableElement.HEALTH_BAR);
    }
    
    private void resetStaminaDefaults(ClientConfig config) {
        resetPositionDefaultsAction(DraggableElement.STAMINA_BAR);
        resetSizeDefaultsAction(DraggableElement.STAMINA_BAR);
        resetVisualDefaultsAction(DraggableElement.STAMINA_BAR);
    }
    
    private void resetManaDefaults(ClientConfig config) {
        resetPositionDefaultsAction(DraggableElement.MANA_BAR);
        resetSizeDefaultsAction(DraggableElement.MANA_BAR);
        resetVisualDefaultsAction(DraggableElement.MANA_BAR);
    }
    
    private void resetArmorDefaults(ClientConfig config) {
        resetPositionDefaultsAction(DraggableElement.ARMOR_BAR);
        resetSizeDefaultsAction(DraggableElement.ARMOR_BAR);
        resetVisualDefaultsAction(DraggableElement.ARMOR_BAR);
    }
    
    private void resetAirDefaults(ClientConfig config) {
        resetPositionDefaultsAction(DraggableElement.AIR_BAR);
        resetSizeDefaultsAction(DraggableElement.AIR_BAR);
        resetVisualDefaultsAction(DraggableElement.AIR_BAR);
    }
}