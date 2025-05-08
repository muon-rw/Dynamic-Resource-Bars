package dev.muon.dynamic_resource_bars.client.gui;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
import dev.muon.dynamic_resource_bars.foundation.config.CClient;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.*; // Includes DraggableElement, EditModeManager, ScreenRect, SubElementType, TextBehavior
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW; // For key codes
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Renderable;
import toni.lib.config.ConfigBase.ConfigInt;
import toni.lib.config.ConfigBase.ConfigEnum;
import net.minecraft.client.gui.components.Tooltip; // Import Tooltip

import java.util.function.Supplier;

public class HudEditorScreen extends Screen {

    private Screen previousScreen;
    private long lastClickTime = 0;
    private double lastClickX = 0;
    private double lastClickY = 0;
    private static final int DOUBLE_CLICK_TIME_MS = 300; 

    // Buttons for Focus Mode
    private Button toggleBackgroundButton;
    private Button toggleForegroundButton;
    private Button toggleFadeFullButton;
    private Button cycleTextBehaviorButton;
    private Button cycleTextAlignButton;
    private Button cycleAnchorButton;
    private Button resizeButton;
    private Button resetPositionButton;
    private Button resetSizeButton;

    // Buttons for Non-Focus Mode (Grid)
    private Button toggleHealthBarButton;
    private Button toggleStaminaBarButton; // Swapped order for visual balance
    private Button toggleManaBarButton;
    private Button openHealthSettingsButton; 
    private Button openStaminaSettingsButton;
    private Button openManaSettingsButton;
    private Button resetButtonForAllBars;

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
    }

    private void rebuildEditorWidgets() {
        clearWidgets();
        
        // Reset button fields
        toggleBackgroundButton = null; toggleForegroundButton = null; toggleFadeFullButton = null;
        cycleTextBehaviorButton = null; cycleTextAlignButton = null; cycleAnchorButton = null;
        resizeButton = null; resetPositionButton = null; resetSizeButton = null;
        toggleHealthBarButton = null; toggleStaminaBarButton = null; toggleManaBarButton = null;
        openHealthSettingsButton = null; openStaminaSettingsButton = null; openManaSettingsButton = null;
        resetButtonForAllBars = null;

        DraggableElement focused = EditModeManager.getFocusedElement();
        CClient config = AllConfigs.client();
        int fontHeight = Minecraft.getInstance().font.lineHeight;

        if (focused == null) {
            // --- Non-Focus Mode: 3x2 Grid + Reset All Button --- 
            int gridButtonWidth = 100;
            int gridButtonHeight = 20;
            int gridTotalWidth = 3 * gridButtonWidth + 2 * 5; // 3 buttons, 2 gaps of 5px
            int gridStartX = (this.width - gridTotalWidth) / 2; // Center the grid horizontally
            int gridTopY = 30; // Start below help text
            int rowSpacing = 5;
            int colSpacing = 5;

            // Row 1: Toggles (Health - Mana - Stamina)
            int row1Y = gridTopY;
            toggleHealthBarButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.health_toggle_format", boolToOnOff(config.enableHealthBar.get())), (b) -> { config.enableHealthBar.set(!config.enableHealthBar.get()); rebuildEditorWidgets(); }).bounds(gridStartX, row1Y, gridButtonWidth, gridButtonHeight).build();
            addRenderableWidget(toggleHealthBarButton);

            // --- Mana Toggle Button (Conditional) ---
            boolean hasManaProvider = ManaProviderRegistry.hasProviders();
            toggleManaBarButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.mana_toggle_format", boolToOnOff(config.enableManaBar.get())), (b) -> { 
                if(b.active) { config.enableManaBar.set(!config.enableManaBar.get()); rebuildEditorWidgets(); }
            }).bounds(gridStartX + gridButtonWidth + colSpacing, row1Y, gridButtonWidth, gridButtonHeight).build();
            toggleManaBarButton.active = hasManaProvider;
            if (!hasManaProvider) {
                toggleManaBarButton.setTooltip(Tooltip.create(Component.translatable("gui.dynamic_resource_bars.hud_editor.tooltip.no_mana_provider")));
            }
            addRenderableWidget(toggleManaBarButton);

            toggleStaminaBarButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.stamina_toggle_format", boolToOnOff(config.enableStaminaBar.get())), (b) -> { config.enableStaminaBar.set(!config.enableStaminaBar.get()); rebuildEditorWidgets(); }).bounds(gridStartX + 2 * (gridButtonWidth + colSpacing), row1Y, gridButtonWidth, gridButtonHeight).build();
            addRenderableWidget(toggleStaminaBarButton);

            // Row 2: Settings Buttons (Health - Mana - Stamina)
            int row2Y = row1Y + gridButtonHeight + rowSpacing;
            openHealthSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.health_settings"), (b) -> { EditModeManager.setFocusedElement(DraggableElement.HEALTH_BAR); rebuildEditorWidgets(); }).bounds(gridStartX, row2Y, gridButtonWidth, gridButtonHeight).build();
            addRenderableWidget(openHealthSettingsButton);

            // --- Mana Settings Button (Conditional) ---
            openManaSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.mana_settings"), (b) -> { 
                if(b.active) { EditModeManager.setFocusedElement(DraggableElement.MANA_BAR); rebuildEditorWidgets(); }
            }).bounds(gridStartX + gridButtonWidth + colSpacing, row2Y, gridButtonWidth, gridButtonHeight).build();
            openManaSettingsButton.active = hasManaProvider;
             if (!hasManaProvider) {
                openManaSettingsButton.setTooltip(Tooltip.create(Component.translatable("gui.dynamic_resource_bars.hud_editor.tooltip.no_mana_provider")));
            }
            addRenderableWidget(openManaSettingsButton);

            openStaminaSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.stamina_settings"), (b) -> { EditModeManager.setFocusedElement(DraggableElement.STAMINA_BAR); rebuildEditorWidgets(); }).bounds(gridStartX + 2 * (gridButtonWidth + colSpacing), row2Y, gridButtonWidth, gridButtonHeight).build();
            addRenderableWidget(openStaminaSettingsButton);

            // Reset All Button
            int resetAllY = row2Y + gridButtonHeight + rowSpacing + 10; // Add extra space
            resetButtonForAllBars = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.reset_all_bars"), (b) -> {
                openConfirmScreen(
                                Component.translatable("gui.dynamic_resource_bars.confirm.reset_all.title"), 
                                Component.translatable("gui.dynamic_resource_bars.confirm.reset_all.explanation"),
                                this::resetAllDefaultsAction);
                })
                .bounds(gridStartX, resetAllY, gridTotalWidth, gridButtonHeight) // Span full grid width
                .build();
            addRenderableWidget(resetButtonForAllBars);

        } else {
            // --- Focus Mode: 3x2 Grid + Bottom Meta Row --- 
            int focusButtonWidth = 100; // Adjusted width for 3x2 grid
            int focusButtonHeight = 20;
            int focusColSpacing = 5;
            int focusRowSpacing = 3;
            int headerTextY = 25; 
            int focusStartY = headerTextY + fontHeight + 5; // Adjusted start Y

            // Calculate horizontal start for the 3x2 grid (centered)
            int gridTotalWidth = 3 * focusButtonWidth + 2 * focusColSpacing;
            int gridStartX = (this.width - gridTotalWidth) / 2;
            
            // (Getters/Setters logic remains the same as before)
            final Supplier<Boolean> bgGetter; final Supplier<Boolean> fgGetter; final Supplier<Boolean> fadeGetter;
            final Supplier<TextBehavior> textGetter; final Supplier<HorizontalAlignment> textAlignGetter;
            final Supplier<HUDPositioning.BarPlacement> anchorGetter;
            final Runnable bgToggler; final Runnable fgToggler; final Runnable fadeToggler;
            final Runnable textCycler; final Runnable textAlignCycler; final Runnable anchorCycler;
            boolean fgSupported = true;
            switch (focused) { // ... (switch logic is unchanged, defines getters/setters/cyclers) ...
                case HEALTH_BAR:
                    bgGetter = config.enableHealthBackground::get; fgGetter = config.enableHealthForeground::get; fadeGetter = config.fadeHealthWhenFull::get;
                    textGetter = config.showHealthText::get; textAlignGetter = config.healthTextAlign::get; anchorGetter = config.healthBarAnchor::get;
                    bgToggler = () -> config.enableHealthBackground.set(!config.enableHealthBackground.get()); fgToggler = () -> config.enableHealthForeground.set(!config.enableHealthForeground.get());
                    fadeToggler = () -> config.fadeHealthWhenFull.set(!config.fadeHealthWhenFull.get());
                    textCycler = () -> { config.showHealthText.set(getNextTextBehavior(config.showHealthText.get())); rebuildEditorWidgets(); }; // Rebuild needed to update button text
                    textAlignCycler = () -> { config.healthTextAlign.set(getNextHorizontalAlignment(config.healthTextAlign.get())); rebuildEditorWidgets(); }; // Rebuild needed
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.healthBarAnchor.get()); config.healthBarAnchor.set(nextAnchor);
                        int bgWidth = config.healthBackgroundWidth.get(); int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.healthTotalXOffset.set(newDefaultXOffset); config.healthTotalYOffset.set(0); rebuildEditorWidgets(); // Rebuild needed
                    };
                    break;
                case MANA_BAR:
                    bgGetter = config.enableManaBackground::get; fgGetter = config.enableManaForeground::get; fadeGetter = config.fadeManaWhenFull::get;
                    textGetter = config.showManaText::get; textAlignGetter = config.manaTextAlign::get; anchorGetter = config.manaBarAnchor::get;
                    bgToggler = () -> config.enableManaBackground.set(!config.enableManaBackground.get()); fgToggler = () -> config.enableManaForeground.set(!config.enableManaForeground.get());
                    fadeToggler = () -> config.fadeManaWhenFull.set(!config.fadeManaWhenFull.get());
                    textCycler = () -> { config.showManaText.set(getNextTextBehavior(config.showManaText.get())); rebuildEditorWidgets(); };
                    textAlignCycler = () -> { config.manaTextAlign.set(getNextHorizontalAlignment(config.manaTextAlign.get())); rebuildEditorWidgets(); };
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.manaBarAnchor.get()); config.manaBarAnchor.set(nextAnchor);
                        int bgWidth = config.manaBackgroundWidth.get(); int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.manaTotalXOffset.set(newDefaultXOffset); config.manaTotalYOffset.set(0); rebuildEditorWidgets();
                    };
                    break;
                case STAMINA_BAR:
                    bgGetter = config.enableStaminaBackground::get; fgGetter = config.enableStaminaForeground::get; fadeGetter = config.fadeStaminaWhenFull::get;
                    textGetter = config.showStaminaText::get; textAlignGetter = config.staminaTextAlign::get; anchorGetter = config.staminaBarAnchor::get;
                    bgToggler = () -> config.enableStaminaBackground.set(!config.enableStaminaBackground.get()); fgToggler = () -> config.enableStaminaForeground.set(!config.enableStaminaForeground.get());
                    fadeToggler = () -> config.fadeStaminaWhenFull.set(!config.fadeStaminaWhenFull.get());
                    textCycler = () -> { config.showStaminaText.set(getNextTextBehavior(config.showStaminaText.get())); rebuildEditorWidgets(); };
                    textAlignCycler = () -> { config.staminaTextAlign.set(getNextHorizontalAlignment(config.staminaTextAlign.get())); rebuildEditorWidgets(); };
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.staminaBarAnchor.get()); config.staminaBarAnchor.set(nextAnchor);
                        int bgWidth = config.staminaBackgroundWidth.get(); int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.staminaTotalXOffset.set(newDefaultXOffset); config.staminaTotalYOffset.set(0); rebuildEditorWidgets();
                    };
                    break;
                default: return;
            }

            // --- Create and add focus-mode buttons: 3x2 Grid ---
            int currentX = gridStartX;
            int currentY = focusStartY;

            // Row 1
            toggleBackgroundButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.background_toggle_format", boolToOnOff(bgGetter.get())), (b) -> { bgToggler.run(); b.setMessage(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.background_toggle_format", boolToOnOff(bgGetter.get()))); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(toggleBackgroundButton); currentX += focusButtonWidth + focusColSpacing;
            if (fgSupported) { toggleForegroundButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.foreground_toggle_format", boolToOnOff(fgGetter.get())), (b) -> { fgToggler.run(); b.setMessage(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.foreground_toggle_format", boolToOnOff(fgGetter.get()))); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(toggleForegroundButton); } else { /* Optional: add a disabled placeholder or leave empty */ } currentX += focusButtonWidth + focusColSpacing;
            toggleFadeFullButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.fade_when_full_toggle_format", boolToOnOff(fadeGetter.get())), (b) -> { fadeToggler.run(); b.setMessage(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.fade_when_full_toggle_format", boolToOnOff(fadeGetter.get()))); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(toggleFadeFullButton);
            
            // Row 2
            currentX = gridStartX; // Reset X for new row
            currentY += focusButtonHeight + focusRowSpacing;
            cycleTextBehaviorButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.show_text_cycle_format", textGetter.get().name()), (b) -> { textCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(cycleTextBehaviorButton); currentX += focusButtonWidth + focusColSpacing;
            cycleTextAlignButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.text_align_cycle_format", textAlignGetter.get().name()), (b) -> { textAlignCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(cycleTextAlignButton); currentX += focusButtonWidth + focusColSpacing;
            cycleAnchorButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.anchor_cycle_format", anchorGetter.get().name()), (b) -> { anchorCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(cycleAnchorButton);

            // --- Bottom Meta Row (Reset Position, Reset Sizes, Resize...) ---
            int metaRowY = currentY + focusButtonHeight + focusRowSpacing + 10; // Add extra space before meta row
            int metaButtonWidth = focusButtonWidth; // Can be same or different
            int metaTotalWidth = 3 * metaButtonWidth + 2 * focusColSpacing;
            int metaStartX = (this.width - metaTotalWidth) / 2; // Center the meta row

            final DraggableElement finalFocusedForReset = focused; // Final for lambda
            resetPositionButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.reset_position"), (b) -> {
                openConfirmScreen(
                                Component.translatable("gui.dynamic_resource_bars.confirm.reset_position.title"), 
                                Component.translatable("gui.dynamic_resource_bars.confirm.reset_position.explanation"),
                                () -> resetPositionDefaultsAction(finalFocusedForReset));
                 }).bounds(metaStartX, metaRowY, metaButtonWidth, focusButtonHeight).build();
             addRenderableWidget(resetPositionButton);

            resetSizeButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.reset_sizes"), (b) -> {
                openConfirmScreen(
                                Component.translatable("gui.dynamic_resource_bars.confirm.reset_sizes.title"), 
                                Component.translatable("gui.dynamic_resource_bars.confirm.reset_sizes.explanation"),
                                () -> resetSizeDefaultsAction(finalFocusedForReset));
                 }).bounds(metaStartX + metaButtonWidth + focusColSpacing, metaRowY, metaButtonWidth, focusButtonHeight).build();
            addRenderableWidget(resetSizeButton);
            
            final DraggableElement finalFocusedForResize = focused; // Need final variable for lambda
            resizeButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.resize"), (b) -> {
                    this.minecraft.setScreen(new ResizeElementScreen(this, finalFocusedForResize));
                 })
                 .bounds(metaStartX + 2 * (metaButtonWidth + focusColSpacing), metaRowY, metaButtonWidth, focusButtonHeight).build();
             addRenderableWidget(resizeButton);
        }
    }

    // Helper to cycle through TextBehavior enum
    private TextBehavior getNextTextBehavior(TextBehavior current) {
        TextBehavior[] behaviors = TextBehavior.values();
        int nextOrdinal = (current.ordinal() + 1) % behaviors.length;
        return behaviors[nextOrdinal];
    }

    // Helper to cycle through HorizontalAlignment enum
    private HorizontalAlignment getNextHorizontalAlignment(HorizontalAlignment current) {
        HorizontalAlignment[] alignments = HorizontalAlignment.values();
        int nextOrdinal = (current.ordinal() + 1) % alignments.length;
        return alignments[nextOrdinal];
    }

    // Helper to cycle through HUDPositioning.BarPlacement enum
    private HUDPositioning.BarPlacement getNextBarPlacement(HUDPositioning.BarPlacement current) {
        HUDPositioning.BarPlacement[] placements = HUDPositioning.BarPlacement.values();
        int nextOrdinal = (current.ordinal() + 1) % placements.length;
        // Potentially filter out unusable anchors if needed, but for now, cycle all.
        return placements[nextOrdinal];
    }

    // Helper for button text
    private Component boolToOnOff(boolean value) {
        return value ? Component.translatable("gui.dynamic_resource_bars.common.on") : Component.translatable("gui.dynamic_resource_bars.common.off");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        DraggableElement focused = EditModeManager.getFocusedElement();
        DraggableElement dragged = EditModeManager.getDraggedElement();
        Player player = Minecraft.getInstance().player;
        Font font = this.font;
        int fontHeight = font.lineHeight;
        int helpTextY = 10;
        int headerY = 25; 
        int padding = 5;
        int backgroundColor = 0x90000000; 

        // --- Calculate Dynamic Background Box Bounds ---
        // Initialize bounds loosely
        float minX = Float.MAX_VALUE; float minY = Float.MAX_VALUE; 
        float maxX = Float.MIN_VALUE; float maxY = Float.MIN_VALUE;
        boolean foundElement = false;

        // Include Help Text bounds (centered)
        Component helpText = (focused == null) ? 
            Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main") :
            Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus");
        int helpTextWidth = font.width(helpText);
        float helpTextX1 = (this.width / 2.0f) - (helpTextWidth / 2.0f);
        float helpTextX2 = helpTextX1 + helpTextWidth;
        minX = Math.min(minX, helpTextX1); minY = Math.min(minY, helpTextY);
        maxX = Math.max(maxX, helpTextX2); maxY = Math.max(maxY, helpTextY + fontHeight);
        foundElement = true;

        // Include Header Text bounds if focused (now centered)
        Component headerText = null;
        if (focused != null) {
            // I18N for header text - uses a format string that includes the element name
            headerText = Component.translatable("gui.dynamic_resource_bars.hud_editor.header.settings_format", getFriendlyElementName(focused)); 
            int headerWidth = font.width(headerText);
            float headerTextX1 = (this.width / 2.0f) - (headerWidth / 2.0f);
            float headerTextX2 = headerTextX1 + headerWidth;
            minX = Math.min(minX, headerTextX1);
            minY = Math.min(minY, headerY); // Y position remains the same
            maxX = Math.max(maxX, headerTextX2);
            maxY = Math.max(maxY, headerY + fontHeight);
            foundElement = true;
        }

        // Include Button bounds (iterate through actual added renderables)
        for (Renderable renderable : this.renderables) { // Use this.renderables
            if (renderable instanceof AbstractWidget widget) { // Check if it's an AbstractWidget
                 minX = Math.min(minX, widget.getX());
                 minY = Math.min(minY, widget.getY());
                 maxX = Math.max(maxX, widget.getX() + widget.getWidth());
                 maxY = Math.max(maxY, widget.getY() + widget.getHeight());
                 foundElement = true;
            }
        }
        
        // --- Draw Background Box if any elements found ---
        if (foundElement) {
            // Ensure min/max are valid before drawing
            if (minX <= maxX && minY <= maxY) {
                graphics.fill((int)(minX - padding), (int)(minY - padding), 
                              (int)(maxX + padding), (int)(maxY + padding), backgroundColor);
            }
        }

        // --- Draw Text ---
        graphics.drawCenteredString(font, helpText, this.width / 2, helpTextY, 0xFFFFFF);
        if (headerText != null) {
            // Changed to draw centered
            graphics.drawCenteredString(font, headerText, this.width / 2, headerY, 0xFFFFFF);
        }

        // --- Render L-Line if dragging non-focused element ---
        if (focused == null && dragged != null && player != null) {
            ScreenRect barRect = null; HUDPositioning.BarPlacement currentAnchor = null; CClient config = AllConfigs.client();
            switch (dragged) { case HEALTH_BAR: barRect = HealthBarRenderer.getScreenRect(player); currentAnchor = config.healthBarAnchor.get(); break; case MANA_BAR: barRect = ManaBarRenderer.getScreenRect(player); currentAnchor = config.manaBarAnchor.get(); break; case STAMINA_BAR: barRect = StaminaBarRenderer.getScreenRect(player); currentAnchor = config.staminaBarAnchor.get(); break; }
            if (barRect != null && currentAnchor != null && barRect.width() > 0 && barRect.height() > 0) { Position anchorPos = HUDPositioning.getPositionFromAnchor(currentAnchor); int barCenterX = barRect.x() + barRect.width() / 2; int barCenterY = barRect.y() + barRect.height() / 2; int lineColor = 0xA0FFFFFF; graphics.hLine(anchorPos.x(), barCenterX, anchorPos.y(), lineColor); graphics.vLine(barCenterX, anchorPos.y(), barCenterY, lineColor); }
        }

        // --- Render Widgets (Buttons) ---
        super.render(graphics, mouseX, mouseY, partialTicks); 
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Widgets get first dibs
        if (super.mouseClicked(mouseX, mouseY, button)) {
             return true;
        }

        // Only handle left clicks in edit mode
        if (!EditModeManager.isEditModeEnabled() || button != 0) {
            return false;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        long currentTime = System.currentTimeMillis();
        boolean isDoubleClick = (currentTime - lastClickTime < DOUBLE_CLICK_TIME_MS) &&
                                (Math.abs(mouseX - lastClickX) < 5 && Math.abs(mouseY - lastClickY) < 5);
        boolean actionTaken = false;

        // --- Check for Double Click Action --- 
        if (isDoubleClick) {
            DraggableElement clickedBarForFocus = getClickedBarComplex(mouseX, mouseY, player);
            if (clickedBarForFocus != null) {
                if (EditModeManager.getFocusedElement() == clickedBarForFocus) {
                    EditModeManager.clearFocusedElement(); // Unfocus
                } else {
                    EditModeManager.setFocusedElement(clickedBarForFocus); // Focus
                }
                rebuildEditorWidgets();
                actionTaken = true;
            }
            // Reset timer after any double click attempt
            lastClickTime = 0; 
        }

        // --- If not a double click that took action, check for Single Click Drag Action ---
        if (!actionTaken) {
            if (EditModeManager.getFocusedElement() != null) {
                // Try dragging a sub-element
                SubElementType clickedSub = getClickedSubElement(EditModeManager.getFocusedElement(), mouseX, mouseY, player);
                if (clickedSub != null && clickedSub != SubElementType.BACKGROUND) {
                    int currentSubX = 0; int currentSubY = 0;
                    // Get current specific offsets...
                    switch (EditModeManager.getFocusedElement()) { 
                         case HEALTH_BAR: if (clickedSub == SubElementType.BAR_MAIN) { currentSubX = AllConfigs.client().healthBarXOffset.get(); currentSubY = AllConfigs.client().healthBarYOffset.get(); } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { currentSubX = AllConfigs.client().healthOverlayXOffset.get(); currentSubY = AllConfigs.client().healthOverlayYOffset.get(); } break;
                         case MANA_BAR: if (clickedSub == SubElementType.BAR_MAIN) { currentSubX = AllConfigs.client().manaBarXOffset.get(); currentSubY = AllConfigs.client().manaBarYOffset.get(); } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { currentSubX = AllConfigs.client().manaOverlayXOffset.get(); currentSubY = AllConfigs.client().manaOverlayYOffset.get(); } break;
                         case STAMINA_BAR: if (clickedSub == SubElementType.BAR_MAIN) { currentSubX = AllConfigs.client().staminaBarXOffset.get(); currentSubY = AllConfigs.client().staminaBarYOffset.get(); } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { currentSubX = AllConfigs.client().staminaOverlayXOffset.get(); currentSubY = AllConfigs.client().staminaOverlayYOffset.get(); } break;
                    }
                    EditModeManager.setDraggedSubElement(clickedSub, (int)mouseX, (int)mouseY, currentSubX, currentSubY);
                    actionTaken = true;
                } else {
                    // Click inside focused element but not on a draggable sub-part.
                    // Consume the click but don't start drag.
                    actionTaken = true; 
                }
            } else {
                // No focus, try dragging the whole bar complex
                DraggableElement clickedBarForDrag = getClickedBarComplex(mouseX, mouseY, player);
                 if (clickedBarForDrag != null) {
                    int totalX = 0; int totalY = 0;
                    // Get current total offsets...
                     switch (clickedBarForDrag) {
                        case HEALTH_BAR: totalX = AllConfigs.client().healthTotalXOffset.get(); totalY = AllConfigs.client().healthTotalYOffset.get(); break;
                        case MANA_BAR: totalX = AllConfigs.client().manaTotalXOffset.get(); totalY = AllConfigs.client().manaTotalYOffset.get(); break;
                        case STAMINA_BAR: totalX = AllConfigs.client().staminaTotalXOffset.get(); totalY = AllConfigs.client().staminaTotalYOffset.get(); break;
                    }
                    EditModeManager.setDraggedElement(clickedBarForDrag, (int) mouseX, (int) mouseY, totalX, totalY);
                    actionTaken = true;
                 }
            }
        }

        // Update last click info IF this click wasn't a double click action.
        if (!isDoubleClick) {
            lastClickTime = currentTime;
            lastClickX = mouseX;
            lastClickY = mouseY;
        }

        return actionTaken; // True if we started a drag or changed focus
    }

    private DraggableElement getClickedBarComplex(double mouseX, double mouseY, Player player) {
        // Check Health, Stamina, Mana bars
        if (HealthBarRenderer.getScreenRect(player).contains((int)mouseX, (int)mouseY)) return DraggableElement.HEALTH_BAR;
        if (StaminaBarRenderer.getScreenRect(player).contains((int)mouseX, (int)mouseY)) return DraggableElement.STAMINA_BAR;
        if (ManaBarRenderer.getScreenRect(player).contains((int)mouseX, (int)mouseY)) return DraggableElement.MANA_BAR;
        return null;
    }

    private SubElementType getClickedSubElement(DraggableElement focusedBar, double mouseX, double mouseY, Player player) {
        if (focusedBar == null) return null;

        ScreenRect barMainRect = null;
        ScreenRect barFgRect = null;
        // Background is not considered a draggable sub-element this way
        // ScreenRect barBgRect = null; 

        switch (focusedBar) {
            case HEALTH_BAR:
                // barBgRect = HealthBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                barMainRect = HealthBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                if (AllConfigs.client().enableHealthForeground.get()) {
                    barFgRect = HealthBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                }
                break;
            case STAMINA_BAR:
                // barBgRect = StaminaBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                barMainRect = StaminaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                if (AllConfigs.client().enableStaminaForeground.get()) {
                    barFgRect = StaminaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                }
                break;
            case MANA_BAR:
                // barBgRect = ManaBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                barMainRect = ManaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                if (AllConfigs.client().enableManaForeground.get()) {
                    barFgRect = ManaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                }
                break;
        }

        // Check foreground first as it might overlap main bar
        if (barFgRect != null && barFgRect.contains((int)mouseX, (int)mouseY)) return SubElementType.FOREGROUND_DETAIL;
        if (barMainRect != null && barMainRect.contains((int)mouseX, (int)mouseY)) return SubElementType.BAR_MAIN;
        // if (barBgRect != null && barBgRect.contains((int)mouseX, (int)mouseY)) return SubElementType.BACKGROUND;

        return null; // Click was not on any known sub-element of the focused bar
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);

        // --- Dragging a Sub-Element ---
        if (EditModeManager.getDraggedSubElement() != null && EditModeManager.getFocusedElement() != null) {
            int deltaX = (int) (mouseX - EditModeManager.getSubElementDragStartX());
            int deltaY = (int) (mouseY - EditModeManager.getSubElementDragStartY());
            int newSubX = EditModeManager.getInitialSubElementXOffset() + deltaX;
            int newSubY = EditModeManager.getInitialSubElementYOffset() + deltaY;

            DraggableElement focused = EditModeManager.getFocusedElement();
            SubElementType sub = EditModeManager.getDraggedSubElement();

            // Update the correct specific config based on 'focused' and 'sub'
            switch (focused) {
                case HEALTH_BAR:
                    if (sub == SubElementType.BAR_MAIN) { 
                        AllConfigs.client().healthBarXOffset.set(newSubX); 
                        AllConfigs.client().healthBarYOffset.set(newSubY); 
                    } else if (sub == SubElementType.FOREGROUND_DETAIL) { 
                        AllConfigs.client().healthOverlayXOffset.set(newSubX); 
                        AllConfigs.client().healthOverlayYOffset.set(newSubY); 
                    }
                    break;
                case MANA_BAR:
                     if (sub == SubElementType.BAR_MAIN) { 
                        AllConfigs.client().manaBarXOffset.set(newSubX); 
                        AllConfigs.client().manaBarYOffset.set(newSubY); 
                     } else if (sub == SubElementType.FOREGROUND_DETAIL) { 
                        AllConfigs.client().manaOverlayXOffset.set(newSubX); 
                        AllConfigs.client().manaOverlayYOffset.set(newSubY); 
                     }
                    break;
                case STAMINA_BAR:
                     if (sub == SubElementType.BAR_MAIN) { 
                        AllConfigs.client().staminaBarXOffset.set(newSubX); 
                        AllConfigs.client().staminaBarYOffset.set(newSubY); 
                     } else if (sub == SubElementType.FOREGROUND_DETAIL) { 
                        AllConfigs.client().staminaOverlayXOffset.set(newSubX); 
                        AllConfigs.client().staminaOverlayYOffset.set(newSubY); 
                     }
                    break;
            }
            return true; // Event handled
        }
        // --- Dragging a Whole Bar Complex ---
        else if (EditModeManager.getDraggedElement() != null) {
            int deltaX = (int) (mouseX - EditModeManager.getDragStartX());
            int deltaY = (int) (mouseY - EditModeManager.getDragStartY());
            int newTotalX = EditModeManager.getInitialElementXOffset() + deltaX;
            int newTotalY = EditModeManager.getInitialElementYOffset() + deltaY;
            DraggableElement dragged = EditModeManager.getDraggedElement();
            switch (dragged) {
                case HEALTH_BAR: AllConfigs.client().healthTotalXOffset.set(newTotalX); AllConfigs.client().healthTotalYOffset.set(newTotalY); break;
                case MANA_BAR: AllConfigs.client().manaTotalXOffset.set(newTotalX); AllConfigs.client().manaTotalYOffset.set(newTotalY); break;
                case STAMINA_BAR: AllConfigs.client().staminaTotalXOffset.set(newTotalX); AllConfigs.client().staminaTotalYOffset.set(newTotalY); break;
            }
            return true; // Event handled
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
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

    @Override
    public void onClose() {
        if (EditModeManager.isEditModeEnabled()) {
            EditModeManager.toggleEditMode(); // This will also trigger save (if implemented in EditModeManager)
        }
        if (EditModeManager.getFocusedElement() != null) { // Ensure focus is also cleared if screen closes abruptly
            EditModeManager.clearFocusedElement();
        }
        this.minecraft.setScreen(this.previousScreen); // Return to the screen that opened this, if any
    }

    @Override
    public boolean isPauseScreen() {
        return false; // Does not pause the game
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Only close the screen if no element is focused.
        // If an element is focused, Esc is handled by keyPressed to unfocus.
        return EditModeManager.getFocusedElement() == null;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (EditModeManager.getFocusedElement() != null) {
                EditModeManager.clearFocusedElement();
                rebuildEditorWidgets();
                return true; // Consume the Esc key press
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(GuiGraphics graphics) {
        // Do nothing to keep the background transparent, allowing the game world to be visible.
        // Ensure we don't draw the default Screen background.
    }

    // Add clearWidgets helper if it doesn't exist, or ensure it clears correctly
    protected void clearWidgets() {
        // Ensure all buttons are removed. Standard way is to clear renderables and children.
        // Depending on Screen implementation details, one or both might be needed.
        this.renderables.clear();
        this.children().clear(); // This usually includes renderables
        // If using NeoForge/Fabric specific widget management, adjust accordingly.
    }

    // --- Reset Logic Helpers ---

    private void resetPositionDefaultsAction(DraggableElement element) {
        CClient config = AllConfigs.client();
        if (element == null) return;

        ConfigEnum<HUDPositioning.BarPlacement> anchorConf = null;
        ConfigInt totalXConf = null, totalYConf = null, barXConf = null, barYConf = null, overlayXConf = null, overlayYConf = null;
        // ConfigInt bgWidthConf = null; // No longer needed directly here, default comes from CClient

        HUDPositioning.BarPlacement defaultAnchor = null;
        int defaultBarX = 0, defaultBarY = 0, defaultOverlayX = 0, defaultOverlayY = 0, defaultTotalY = 0;
        int defaultBgWidthForCalc = 0; // For calculating defaultTotalX

        switch (element) {
            case HEALTH_BAR: 
                anchorConf = config.healthBarAnchor; totalXConf = config.healthTotalXOffset; totalYConf = config.healthTotalYOffset;
                barXConf = config.healthBarXOffset; barYConf = config.healthBarYOffset; overlayXConf = config.healthOverlayXOffset; overlayYConf = config.healthOverlayYOffset;
                // bgWidthConf = config.healthBackgroundWidth; // Not needed for direct reset
                defaultAnchor = CClient.DEFAULT_HEALTH_BAR_ANCHOR;
                defaultBgWidthForCalc = CClient.DEFAULT_HEALTH_BACKGROUND_WIDTH;
                defaultBarX = CClient.DEFAULT_HEALTH_BAR_X_OFFSET; defaultBarY = CClient.DEFAULT_HEALTH_BAR_Y_OFFSET;
                defaultOverlayX = CClient.DEFAULT_HEALTH_OVERLAY_X_OFFSET; defaultOverlayY = CClient.DEFAULT_HEALTH_OVERLAY_Y_OFFSET;
                defaultTotalY = CClient.DEFAULT_HEALTH_TOTAL_Y_OFFSET;
                break;
            case STAMINA_BAR:
                anchorConf = config.staminaBarAnchor; totalXConf = config.staminaTotalXOffset; totalYConf = config.staminaTotalYOffset;
                barXConf = config.staminaBarXOffset; barYConf = config.staminaBarYOffset; overlayXConf = config.staminaOverlayXOffset; overlayYConf = config.staminaOverlayYOffset;
                // bgWidthConf = config.staminaBackgroundWidth;
                defaultAnchor = CClient.DEFAULT_STAMINA_BAR_ANCHOR;
                defaultBgWidthForCalc = CClient.DEFAULT_STAMINA_BACKGROUND_WIDTH;
                defaultBarX = CClient.DEFAULT_STAMINA_BAR_X_OFFSET; defaultBarY = CClient.DEFAULT_STAMINA_BAR_Y_OFFSET;
                defaultOverlayX = CClient.DEFAULT_STAMINA_OVERLAY_X_OFFSET; defaultOverlayY = CClient.DEFAULT_STAMINA_OVERLAY_Y_OFFSET;
                defaultTotalY = CClient.DEFAULT_STAMINA_TOTAL_Y_OFFSET;
                break;
            case MANA_BAR:
                anchorConf = config.manaBarAnchor; totalXConf = config.manaTotalXOffset; totalYConf = config.manaTotalYOffset;
                barXConf = config.manaBarXOffset; barYConf = config.manaBarYOffset; overlayXConf = config.manaOverlayXOffset; overlayYConf = config.manaOverlayYOffset;
                // bgWidthConf = config.manaBackgroundWidth;
                defaultAnchor = CClient.DEFAULT_MANA_BAR_ANCHOR;
                defaultBgWidthForCalc = CClient.DEFAULT_MANA_BACKGROUND_WIDTH;
                defaultBarX = CClient.DEFAULT_MANA_BAR_X_OFFSET; defaultBarY = CClient.DEFAULT_MANA_BAR_Y_OFFSET;
                defaultOverlayX = CClient.DEFAULT_MANA_OVERLAY_X_OFFSET; defaultOverlayY = CClient.DEFAULT_MANA_OVERLAY_Y_OFFSET;
                defaultTotalY = CClient.DEFAULT_MANA_TOTAL_Y_OFFSET;
                break;
        }

        if (anchorConf != null && totalXConf != null && totalYConf != null && barXConf != null && barYConf != null && overlayXConf != null && overlayYConf != null) {
            anchorConf.set(defaultAnchor);
            barXConf.set(defaultBarX);
            barYConf.set(defaultBarY);
            overlayXConf.set(defaultOverlayX);
            overlayYConf.set(defaultOverlayY);
            totalYConf.set(defaultTotalY); 

            // Calculate default TotalX based on default anchor and default size
            int newDefaultTotalX = 0;
            if (defaultAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultTotalX = -defaultBgWidthForCalc / 2; }
            else if (defaultAnchor != null && defaultAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultTotalX = -defaultBgWidthForCalc; }
            // else for LEFT side, newDefaultTotalX remains 0 which is correct.
            totalXConf.set(newDefaultTotalX);
        }
        rebuildEditorWidgets(); // Update UI
    }

    private void resetSizeDefaultsAction(DraggableElement element) {
        CClient config = AllConfigs.client();
        if (element == null) return;

        ConfigInt bgWConf=null, bgHConf=null, barWConf=null, barHConf=null, ovWConf=null, ovHConf=null;
        int defaultBgW=0, defaultBgH=0, defaultBarW=0, defaultBarH=0, defaultOvW=0, defaultOvH=0;

        switch(element) {
            case HEALTH_BAR: 
                bgWConf=config.healthBackgroundWidth; bgHConf=config.healthBackgroundHeight; barWConf=config.healthBarWidth; barHConf=config.healthBarHeight; ovWConf=config.healthOverlayWidth; ovHConf=config.healthOverlayHeight; 
                defaultBgW=CClient.DEFAULT_HEALTH_BACKGROUND_WIDTH; defaultBgH=CClient.DEFAULT_HEALTH_BACKGROUND_HEIGHT; defaultBarW=CClient.DEFAULT_HEALTH_BAR_WIDTH; defaultBarH=CClient.DEFAULT_HEALTH_BAR_HEIGHT; defaultOvW=CClient.DEFAULT_HEALTH_OVERLAY_WIDTH; defaultOvH=CClient.DEFAULT_HEALTH_OVERLAY_HEIGHT;
                break;
            case STAMINA_BAR: 
                bgWConf=config.staminaBackgroundWidth; bgHConf=config.staminaBackgroundHeight; barWConf=config.staminaBarWidth; barHConf=config.staminaBarHeight; ovWConf=config.staminaOverlayWidth; ovHConf=config.staminaOverlayHeight; 
                defaultBgW=CClient.DEFAULT_STAMINA_BACKGROUND_WIDTH; defaultBgH=CClient.DEFAULT_STAMINA_BACKGROUND_HEIGHT; defaultBarW=CClient.DEFAULT_STAMINA_BAR_WIDTH; defaultBarH=CClient.DEFAULT_STAMINA_BAR_HEIGHT; defaultOvW=CClient.DEFAULT_STAMINA_OVERLAY_WIDTH; defaultOvH=CClient.DEFAULT_STAMINA_OVERLAY_HEIGHT;
                break;
            case MANA_BAR: 
                bgWConf=config.manaBackgroundWidth; bgHConf=config.manaBackgroundHeight; barWConf=config.manaBarWidth; barHConf=config.manaBarHeight; ovWConf=config.manaOverlayWidth; ovHConf=config.manaOverlayHeight; 
                defaultBgW=CClient.DEFAULT_MANA_BACKGROUND_WIDTH; defaultBgH=CClient.DEFAULT_MANA_BACKGROUND_HEIGHT; defaultBarW=CClient.DEFAULT_MANA_BAR_WIDTH; defaultBarH=CClient.DEFAULT_MANA_BAR_HEIGHT; defaultOvW=CClient.DEFAULT_MANA_OVERLAY_WIDTH; defaultOvH=CClient.DEFAULT_MANA_OVERLAY_HEIGHT;
                break;
        }
        if(bgWConf!=null) bgWConf.set(defaultBgW); if(bgHConf!=null) bgHConf.set(defaultBgH);
        if(barWConf!=null) barWConf.set(defaultBarW); if(barHConf!=null) barHConf.set(defaultBarH);
        if(ovWConf!=null) ovWConf.set(defaultOvW); if(ovHConf!=null) ovHConf.set(defaultOvH);
        rebuildEditorWidgets(); // Update UI (though size changes aren't visible in buttons)
    }

    private void resetAllDefaultsAction() {
        resetPositionDefaultsAction(DraggableElement.HEALTH_BAR);
        resetSizeDefaultsAction(DraggableElement.HEALTH_BAR);
        resetPositionDefaultsAction(DraggableElement.STAMINA_BAR);
        resetSizeDefaultsAction(DraggableElement.STAMINA_BAR);
        resetPositionDefaultsAction(DraggableElement.MANA_BAR);
        resetSizeDefaultsAction(DraggableElement.MANA_BAR);
        // Note: This resets even if the bar is disabled via enableXYZBar config
        rebuildEditorWidgets(); // Final rebuild
    }

    private void openConfirmScreen(Component title, Component explanation, Runnable confirmAction) {
        this.minecraft.setScreen(new ConfirmResetScreen(this, title, explanation, confirmAction));
    }

    private String getFriendlyElementName(DraggableElement element) {
        if (element == null) return "";
        // These will be translated via their own keys for the header
        return switch (element) {
            case HEALTH_BAR -> Component.translatable("gui.dynamic_resource_bars.element.health_bar").getString();
            case MANA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.mana_bar").getString();
            case STAMINA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.stamina_bar").getString();
            default -> element.name();
        };
    }
} 