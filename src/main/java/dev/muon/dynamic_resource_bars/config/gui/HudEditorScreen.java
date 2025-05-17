package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.AirBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.*;
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
import net.minecraft.client.gui.components.Tooltip;

import java.util.function.Supplier;

public class HudEditorScreen extends Screen {

    private static final int HELP_TEXT_TOP_Y = 15;
    private static final int LINE_SPACING = 2;

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
    private Button toggleStaminaBarButton;
    private Button toggleManaBarButton;
    private Button cycleArmorBehaviorButton;
    private Button cycleAirBehaviorButton;
    private Button openHealthSettingsButton; 
    private Button openStaminaSettingsButton;
    private Button openManaSettingsButton;
    private Button openArmorSettingsButton;
    private Button openAirSettingsButton;
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
        this.canUndoLastDrag = false; 
        this.lastDraggedElementForUndo = null;
        this.canUndoLastSubDrag = false;
        this.lastFocusedElementForSubUndo = null;
        this.lastDraggedSubElementForUndo = null;
    }

    private void rebuildEditorWidgets() {
        DraggableElement currentFocused = EditModeManager.getFocusedElement(); // Get current focus *before* clearing widgets
        // Compare with a stored previous focus to detect change, or just check if it *was* something and now isn't, or vice-versa
        // For simplicity, we can tie sub-element undo state tightly to the rebuild call if focus has changed or cleared.
        // However, EditModeManager.getFocusedElement() inside clearWidgets() might be an issue if it calls something that changes focus.
        // Best to get the focused element *before* clearWidgets().

        // If focus is changing or clearing, sub-element undo context is lost
        // Let's refine this: we need to compare the current focused element with the one stored for sub-undo.
        if (currentFocused != this.lastFocusedElementForSubUndo && this.lastFocusedElementForSubUndo != null) {
            // Focus has changed away from the element we had a sub-undo for.
            this.canUndoLastSubDrag = false;
            this.lastFocusedElementForSubUndo = null;
            this.lastDraggedSubElementForUndo = null;
        } else if (currentFocused == null && this.lastFocusedElementForSubUndo != null) {
            // Focus was cleared, and we had a sub-undo for some element.
            this.canUndoLastSubDrag = false;
            this.lastFocusedElementForSubUndo = null;
            this.lastDraggedSubElementForUndo = null;
        }

        clearWidgets();
        
        toggleBackgroundButton = null; toggleForegroundButton = null; toggleFadeFullButton = null;
        cycleTextBehaviorButton = null; cycleTextAlignButton = null; cycleAnchorButton = null;
        resizeButton = null; resetPositionButton = null; resetSizeButton = null;
        
        toggleHealthBarButton = null; toggleStaminaBarButton = null; toggleManaBarButton = null;
        cycleArmorBehaviorButton = null; cycleAirBehaviorButton = null;

        openHealthSettingsButton = null; openStaminaSettingsButton = null; openManaSettingsButton = null;
        openArmorSettingsButton = null; openAirSettingsButton = null;
        resetButtonForAllBars = null;

        DraggableElement focused = EditModeManager.getFocusedElement();
        ClientConfig config = ModConfigManager.getClient();
        int fontHeight = Minecraft.getInstance().font.lineHeight;

        if (focused == null) {
            int gridButtonHeight = 20;
            int rowSpacing = 5;
            int colSpacing = 5;
            int gridTopY = 40; 
            int currentX, currentY;

            // --- Section 1: Health, Mana, Stamina (3 columns) ---
            int threeColButtonWidth = 100; // Adjusted for 3 columns
            int threeColContentWidth = 3 * threeColButtonWidth + 2 * colSpacing;
            int threeColStartX = (this.width - threeColContentWidth) / 2;

            // Row 1: H, M, S Toggles
            currentY = gridTopY;
            currentX = threeColStartX;

            toggleHealthBarButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.health_toggle_format", 
                config.enableHealthBar.get() ? Component.translatable("gui.dynamic_resource_bars.behavior.custom_simple") : Component.translatable("gui.dynamic_resource_bars.behavior.vanilla_simple")),
                (b) -> { config.enableHealthBar.set(!config.enableHealthBar.get()); rebuildEditorWidgets(); }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            addRenderableWidget(toggleHealthBarButton);
            currentX += threeColButtonWidth + colSpacing;

            boolean hasManaProvider = ManaProviderRegistry.hasProviders();
            toggleManaBarButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.mana_toggle_format", 
                config.enableManaBar.get() ? Component.translatable("gui.dynamic_resource_bars.behavior.custom_simple") : Component.translatable("gui.dynamic_resource_bars.behavior.vanilla_simple")),
                (b) -> { 
                if(b.active) { config.enableManaBar.set(!config.enableManaBar.get()); rebuildEditorWidgets(); }
            }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            toggleManaBarButton.active = hasManaProvider;
            if (!hasManaProvider) {
                toggleManaBarButton.setTooltip(Tooltip.create(Component.translatable("gui.dynamic_resource_bars.hud_editor.tooltip.no_mana_provider")));
            }
            addRenderableWidget(toggleManaBarButton);
            currentX += threeColButtonWidth + colSpacing;

            toggleStaminaBarButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.stamina_toggle_format", 
                config.enableStaminaBar.get() ? Component.translatable("gui.dynamic_resource_bars.behavior.custom_simple") : Component.translatable("gui.dynamic_resource_bars.behavior.vanilla_simple")),
                (b) -> { config.enableStaminaBar.set(!config.enableStaminaBar.get()); rebuildEditorWidgets(); }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            addRenderableWidget(toggleStaminaBarButton);

            // Row 2: H, M, S Settings
            currentY += gridButtonHeight + rowSpacing;
            currentX = threeColStartX;

            openHealthSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.health_settings"), (b) -> { EditModeManager.setFocusedElement(DraggableElement.HEALTH_BAR); rebuildEditorWidgets(); }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            openHealthSettingsButton.active = config.enableHealthBar.get();
            addRenderableWidget(openHealthSettingsButton);
            currentX += threeColButtonWidth + colSpacing;

            openManaSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.mana_settings"), (b) -> { 
                if(b.active) { EditModeManager.setFocusedElement(DraggableElement.MANA_BAR); rebuildEditorWidgets(); }
            }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            openManaSettingsButton.active = config.enableManaBar.get() && hasManaProvider;
             if (!hasManaProvider) {
                openManaSettingsButton.setTooltip(Tooltip.create(Component.translatable("gui.dynamic_resource_bars.hud_editor.tooltip.no_mana_provider")));
            }
            addRenderableWidget(openManaSettingsButton);
            currentX += threeColButtonWidth + colSpacing;

            openStaminaSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.stamina_settings"), (b) -> { EditModeManager.setFocusedElement(DraggableElement.STAMINA_BAR); rebuildEditorWidgets(); }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            openStaminaSettingsButton.active = config.enableStaminaBar.get();
            addRenderableWidget(openStaminaSettingsButton);

            // --- Section 2: Armor, Air (2 columns) ---
            currentY += gridButtonHeight + rowSpacing + 5; // Extra spacing between sections
            int twoColButtonWidth = 120; // Slightly wider for 2 columns if desired
            int twoColContentWidth = 2 * twoColButtonWidth + colSpacing;
            int twoColStartX = (this.width - twoColContentWidth) / 2;

            // Row 3: Armor, Air Cycle Behavior
            currentX = twoColStartX;
            cycleArmorBehaviorButton = Button.builder(getBarBehaviorComponent(config.armorBarBehavior.get(), "armor"), (b) -> {
                config.armorBarBehavior.set(getNextBarBehavior(config.armorBarBehavior.get()));
                rebuildEditorWidgets();
            }).bounds(currentX, currentY, twoColButtonWidth, gridButtonHeight).build();
            addRenderableWidget(cycleArmorBehaviorButton);
            currentX += twoColButtonWidth + colSpacing;

            #if !(UPTO_20_1 && FABRIC)
            cycleAirBehaviorButton = Button.builder(getBarBehaviorComponent(config.airBarBehavior.get(), "air"), (b) -> {
                config.airBarBehavior.set(getNextBarBehavior(config.airBarBehavior.get()));
                rebuildEditorWidgets();
            }).bounds(currentX, currentY, twoColButtonWidth, gridButtonHeight).build();
            addRenderableWidget(cycleAirBehaviorButton);
            #else
            // On Fabric 1.20.1, air is tied to stamina
            cycleAirBehaviorButton = Button.builder(getBarBehaviorComponent(config.airBarBehavior.get(), "air"), (b) -> {
                // No-op since air is tied to stamina
            }).bounds(currentX, currentY, twoColButtonWidth, gridButtonHeight).build();
            cycleAirBehaviorButton.active = false;
            cycleAirBehaviorButton.setTooltip(Tooltip.create(Component.translatable("gui.dynamic_resource_bars.hud_editor.tooltip.air_tied_to_stamina")));
            addRenderableWidget(cycleAirBehaviorButton);
            #endif

            // Row 4: Armor, Air Settings
            currentY += gridButtonHeight + rowSpacing;
            currentX = twoColStartX;

            openArmorSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.armor_settings"), (b) -> {
                if (b.active) { EditModeManager.setFocusedElement(DraggableElement.ARMOR_BAR); rebuildEditorWidgets(); }
            }).bounds(currentX, currentY, twoColButtonWidth, gridButtonHeight).build();
            openArmorSettingsButton.active = config.armorBarBehavior.get() == BarRenderBehavior.CUSTOM;
            addRenderableWidget(openArmorSettingsButton);
            currentX += twoColButtonWidth + colSpacing;
            
            openAirSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.air_settings"), (b) -> {
                if (b.active) { EditModeManager.setFocusedElement(DraggableElement.AIR_BAR); rebuildEditorWidgets(); }
            }).bounds(currentX, currentY, twoColButtonWidth, gridButtonHeight).build();
            #if !(UPTO_20_1 && FABRIC)
            openAirSettingsButton.active = config.airBarBehavior.get() == BarRenderBehavior.CUSTOM;
            #else
            // On Fabric 1.20.1, air settings are tied to stamina toggle
            openAirSettingsButton.active = config.enableStaminaBar.get();
            #endif
            addRenderableWidget(openAirSettingsButton);
            
            // Reset All Button - Positioned below all sections
            currentY += gridButtonHeight + rowSpacing + 10; // Extra spacing before reset all
            int resetAllButtonWidth = 150; 
            resetButtonForAllBars = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.reset_all_bars"), (b) -> {
                openConfirmScreen(
                                Component.translatable("gui.dynamic_resource_bars.confirm.reset_all.title"), 
                                Component.translatable("gui.dynamic_resource_bars.confirm.reset_all.explanation"),
                                this::resetAllDefaultsAction);
                })
                .bounds((this.width - resetAllButtonWidth) / 2, currentY, resetAllButtonWidth, gridButtonHeight) 
                .build();
            addRenderableWidget(resetButtonForAllBars);

        } else {
            // --- Focus Mode --- 
            int focusButtonWidth = 120; 
            int focusButtonHeight = 20;
            int focusColSpacing = 5;
            int focusRowSpacing = 5; 
            
            // Estimate space needed for help text (4 lines) and header (1 line)
            int textBlockHeight = (5 * (fontHeight + LINE_SPACING)) + 10; // 5 lines of text + overall padding
            int buttonsTopY = HELP_TEXT_TOP_Y + textBlockHeight; // helpTextY is defined in render, assume 15 for now.
                                                        // This will be more robust if helpTextY is a constant.
                                                        // Let's use a fixed starting Y for buttons that assumes help text is above.
            buttonsTopY = HELP_TEXT_TOP_Y + (4 * (fontHeight + LINE_SPACING)) + (fontHeight + LINE_SPACING) + 15; // helpY + 4 lines help + 1 line header + padding

            final Supplier<Boolean> bgGetter; final Supplier<Boolean> fgGetter; final Supplier<Boolean> fadeGetter;
            final Supplier<TextBehavior> textGetter; final Supplier<HorizontalAlignment> textAlignGetter;
            final Supplier<HUDPositioning.BarPlacement> anchorGetter;
            final Runnable bgToggler; final Runnable fgToggler; final Runnable fadeToggler;
            final Runnable textCycler; final Runnable textAlignCycler; final Runnable anchorCycler;
            boolean fgSupported = false;
            boolean textSupported = false;
            boolean anchorSupported = false;
            boolean fadeSupported = false;
            boolean bgSupported = false;

            switch (focused) { 
                case HEALTH_BAR:
                    bgGetter = config.enableHealthBackground::get; fgGetter = config.enableHealthForeground::get; fadeGetter = config.fadeHealthWhenFull::get;
                    textGetter = config.showHealthText::get; textAlignGetter = config.healthTextAlign::get; anchorGetter = config.healthBarAnchor::get;
                    bgToggler = () -> { config.enableHealthBackground.set(!config.enableHealthBackground.get()); rebuildEditorWidgets(); }; 
                    fgToggler = () -> { config.enableHealthForeground.set(!config.enableHealthForeground.get()); rebuildEditorWidgets(); };
                    fadeToggler = () -> { config.fadeHealthWhenFull.set(!config.fadeHealthWhenFull.get()); rebuildEditorWidgets(); };
                    textCycler = () -> { config.showHealthText.set(getNextTextBehavior(config.showHealthText.get())); rebuildEditorWidgets(); }; 
                    textAlignCycler = () -> { config.healthTextAlign.set(getNextHorizontalAlignment(config.healthTextAlign.get())); rebuildEditorWidgets(); }; 
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.healthBarAnchor.get()); config.healthBarAnchor.set(nextAnchor);
                        int bgWidth = config.healthBackgroundWidth.get(); int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.healthTotalXOffset.set(newDefaultXOffset); config.healthTotalYOffset.set(0); rebuildEditorWidgets(); 
                    };
                    fgSupported = true; textSupported = true; anchorSupported = true; fadeSupported = true; bgSupported = true;
                    break;
                case MANA_BAR:
                    bgGetter = config.enableManaBackground::get; fgGetter = config.enableManaForeground::get; fadeGetter = config.fadeManaWhenFull::get;
                    textGetter = config.showManaText::get; textAlignGetter = config.manaTextAlign::get; anchorGetter = config.manaBarAnchor::get;
                    bgToggler = () -> { config.enableManaBackground.set(!config.enableManaBackground.get()); rebuildEditorWidgets(); }; 
                    fgToggler = () -> { config.enableManaForeground.set(!config.enableManaForeground.get()); rebuildEditorWidgets(); };
                    fadeToggler = () -> { config.fadeManaWhenFull.set(!config.fadeManaWhenFull.get()); rebuildEditorWidgets(); };
                    textCycler = () -> { config.showManaText.set(getNextTextBehavior(config.showManaText.get())); rebuildEditorWidgets(); };
                    textAlignCycler = () -> { config.manaTextAlign.set(getNextHorizontalAlignment(config.manaTextAlign.get())); rebuildEditorWidgets(); };
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.manaBarAnchor.get()); config.manaBarAnchor.set(nextAnchor);
                        int bgWidth = config.manaBackgroundWidth.get(); int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.manaTotalXOffset.set(newDefaultXOffset); config.manaTotalYOffset.set(0); rebuildEditorWidgets();
                    };
                    fgSupported = true; textSupported = true; anchorSupported = true; fadeSupported = true; bgSupported = true;
                    break;
                case STAMINA_BAR:
                    bgGetter = config.enableStaminaBackground::get; fgGetter = config.enableStaminaForeground::get; fadeGetter = config.fadeStaminaWhenFull::get;
                    textGetter = config.showStaminaText::get; textAlignGetter = config.staminaTextAlign::get; anchorGetter = config.staminaBarAnchor::get;
                    bgToggler = () -> { config.enableStaminaBackground.set(!config.enableStaminaBackground.get()); rebuildEditorWidgets(); }; 
                    fgToggler = () -> { config.enableStaminaForeground.set(!config.enableStaminaForeground.get()); rebuildEditorWidgets(); };
                    fadeToggler = () -> { config.fadeStaminaWhenFull.set(!config.fadeStaminaWhenFull.get()); rebuildEditorWidgets(); };
                    textCycler = () -> { config.showStaminaText.set(getNextTextBehavior(config.showStaminaText.get())); rebuildEditorWidgets(); };
                    textAlignCycler = () -> { config.staminaTextAlign.set(getNextHorizontalAlignment(config.staminaTextAlign.get())); rebuildEditorWidgets(); };
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.staminaBarAnchor.get()); config.staminaBarAnchor.set(nextAnchor);
                        int bgWidth = config.staminaBackgroundWidth.get(); int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.staminaTotalXOffset.set(newDefaultXOffset); config.staminaTotalYOffset.set(0); rebuildEditorWidgets();
                    };
                    fgSupported = true; textSupported = true; anchorSupported = true; fadeSupported = true; bgSupported = true;
                    break;
                case ARMOR_BAR:
                    bgGetter = () -> true; // Not directly toggled, but used for consistency in lambdas
                    fgGetter = () -> false; fadeGetter = () -> false; textGetter = () -> TextBehavior.NEVER; textAlignGetter = () -> HorizontalAlignment.CENTER;
                    anchorGetter = config.armorBarAnchor::get;
                    bgToggler = () -> {}; fgToggler = () -> {}; fadeToggler = () -> {}; textCycler = () -> {}; textAlignCycler = () -> {};
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.armorBarAnchor.get()); config.armorBarAnchor.set(nextAnchor);
                        int bgWidth = config.armorBackgroundWidth.get(); int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.armorTotalXOffset.set(newDefaultXOffset); config.armorTotalYOffset.set(0); rebuildEditorWidgets();
                    };
                    anchorSupported = true; bgSupported = false; // Armor/Air don't have these toggles
                    break;
                case AIR_BAR:
                    bgGetter = () -> true;
                    fgGetter = () -> false; fadeGetter = () -> false; textGetter = () -> TextBehavior.NEVER; textAlignGetter = () -> HorizontalAlignment.CENTER;
                    anchorGetter = config.airBarAnchor::get;
                    bgToggler = () -> {}; fgToggler = () -> {}; fadeToggler = () -> {}; textCycler = () -> {}; textAlignCycler = () -> {};
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.airBarAnchor.get()); config.airBarAnchor.set(nextAnchor);
                        int bgWidth = config.airBackgroundWidth.get(); int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.airTotalXOffset.set(newDefaultXOffset); config.airTotalYOffset.set(0); rebuildEditorWidgets();
                    };
                    anchorSupported = true; bgSupported = false;
                    break;
                default: return; // Should not happen
            }

            // Button Grid Layout (Max 3 per row, centered)
            int maxButtonsPerRow = 3;
            int focusGridContentWidth = maxButtonsPerRow * focusButtonWidth + (maxButtonsPerRow - 1) * focusColSpacing;
            int focusGridStartX = (this.width - focusGridContentWidth) / 2;
            
            int currentX = focusGridStartX;
            int currentY = buttonsTopY;
            int buttonsInCurrentRow = 0;

            // Row 1: Background, Foreground, Fade
            if (bgSupported) {
                toggleBackgroundButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.toggle_background_format", 
                    bgGetter.get() ? Component.translatable("options.on") : Component.translatable("options.off")), 
                    (b) -> { bgToggler.run(); /* Button message updates on rebuild */ }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
                addRenderableWidget(toggleBackgroundButton);
                currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;
            }
            if (fgSupported) {
                if (buttonsInCurrentRow >= maxButtonsPerRow) { currentX = focusGridStartX; currentY += focusButtonHeight + focusRowSpacing; buttonsInCurrentRow = 0; }
                toggleForegroundButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.toggle_foreground_format", 
                    fgGetter.get() ? Component.translatable("options.on") : Component.translatable("options.off")), 
                    (b) -> { fgToggler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
                addRenderableWidget(toggleForegroundButton);
                currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;
            }
            if (fadeSupported) {
                if (buttonsInCurrentRow >= maxButtonsPerRow) { currentX = focusGridStartX; currentY += focusButtonHeight + focusRowSpacing; buttonsInCurrentRow = 0; }
                toggleFadeFullButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.toggle_fade_full_format", 
                    fadeGetter.get() ? Component.translatable("options.on") : Component.translatable("options.off")), 
                    (b) -> { fadeToggler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
                addRenderableWidget(toggleFadeFullButton);
                currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;
            }

            // Next Row for Text and Anchor related buttons
            if (buttonsInCurrentRow > 0 || (textSupported || anchorSupported)) { // Start new row if previous had items OR if new items exist
                 currentX = focusGridStartX; 
                 currentY += focusButtonHeight + focusRowSpacing; 
                 buttonsInCurrentRow = 0;
            }

            if (textSupported) {
                cycleTextBehaviorButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.cycle_text_behavior_format", Component.translatable("text_behavior." + textGetter.get().name().toLowerCase())), 
                    (b) -> { textCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
                addRenderableWidget(cycleTextBehaviorButton);
                currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;
            
                if (buttonsInCurrentRow >= maxButtonsPerRow) { currentX = focusGridStartX; currentY += focusButtonHeight + focusRowSpacing; buttonsInCurrentRow = 0; }
                cycleTextAlignButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.cycle_text_align_format", Component.translatable("horizontal_alignment." + textAlignGetter.get().name().toLowerCase())), 
                    (b) -> { textAlignCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
                addRenderableWidget(cycleTextAlignButton);
                currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;
            }

            if (anchorSupported) {
                if (buttonsInCurrentRow >= maxButtonsPerRow) { currentX = focusGridStartX; currentY += focusButtonHeight + focusRowSpacing; buttonsInCurrentRow = 0; }
                cycleAnchorButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.cycle_anchor_format", Component.translatable("bar_placement." + anchorGetter.get().name().toLowerCase())), 
                    (b) -> { anchorCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
                addRenderableWidget(cycleAnchorButton);
                currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;
            }

            // Next Row for Meta actions (Reset, Resize)
            if (buttonsInCurrentRow > 0) { // Start new row if previous had items
                 currentX = focusGridStartX; 
                 currentY += focusButtonHeight + focusRowSpacing; 
                 buttonsInCurrentRow = 0;
            }
            currentY += 5; // Extra small padding before meta buttons

            final DraggableElement finalFocusedElement = focused; // Used for confirm screens and resize screen

            resetPositionButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.reset_position"), (b) -> {
                openConfirmScreen(
                    Component.translatable("gui.dynamic_resource_bars.confirm.reset_position.title", getFriendlyElementName(finalFocusedElement)), 
                    Component.translatable("gui.dynamic_resource_bars.confirm.reset_position.explanation"),
                    () -> resetPositionDefaultsAction(finalFocusedElement));
            }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
            addRenderableWidget(resetPositionButton);
            currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;

            if (buttonsInCurrentRow >= maxButtonsPerRow) { currentX = focusGridStartX; currentY += focusButtonHeight + focusRowSpacing; buttonsInCurrentRow = 0; }
            resetSizeButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.reset_size"), (b) -> {
                openConfirmScreen(
                    Component.translatable("gui.dynamic_resource_bars.confirm.reset_size.title", getFriendlyElementName(finalFocusedElement)), 
                    Component.translatable("gui.dynamic_resource_bars.confirm.reset_size.explanation"),
                    () -> resetSizeDefaultsAction(finalFocusedElement));
            }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
            addRenderableWidget(resetSizeButton);
            currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;
            
            // Resize button is always available (even for Armor/Air)
            if (buttonsInCurrentRow >= maxButtonsPerRow) { currentX = focusGridStartX; currentY += focusButtonHeight + focusRowSpacing; buttonsInCurrentRow = 0; }
            resizeButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.resize_bar"), (b) -> {
                this.minecraft.setScreen(new ResizeElementScreen(this, finalFocusedElement));
            }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
            addRenderableWidget(resizeButton);
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

    private BarRenderBehavior getNextBarBehavior(BarRenderBehavior current) {
        BarRenderBehavior[] behaviors = BarRenderBehavior.values();
        int nextIndex = (current.ordinal() + 1) % behaviors.length;
        return behaviors[nextIndex];
    }

    private HUDPositioning.BarPlacement getNextBarPlacement(HUDPositioning.BarPlacement current) {
        HUDPositioning.BarPlacement[] placements = HUDPositioning.BarPlacement.values();
        int nextIndex = (current.ordinal() + 1) % placements.length;
        return placements[nextIndex];
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
            // Main help text does not have line3/4 in current en_us.json, but keeping structure for potential future additions

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
            
            // No explicit header for main mode, buttons start after help text based on their own layout logic.
            // So, update overallMaxY based on help text.
            overallMaxY = Math.max(overallMaxY, currentTextY);

        } else { // Focus Mode: Render help text and then the header
            Component helpFocusLine1 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line1");
            Component helpFocusLine2 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line2.sub_element");
            Component helpFocusLine3 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line3");
            Component helpFocusLine4 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line4");

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
                case HEALTH_BAR: barRect = HealthBarRenderer.getScreenRect(player); currentAnchor = currentConfig.healthBarAnchor.get(); break; 
                case MANA_BAR: barRect = ManaBarRenderer.getScreenRect(player); currentAnchor = currentConfig.manaBarAnchor.get(); break; 
                case STAMINA_BAR: barRect = StaminaBarRenderer.getScreenRect(player); currentAnchor = currentConfig.staminaBarAnchor.get(); break; 
                case ARMOR_BAR: barRect = ArmorBarRenderer.getScreenRect(player); currentAnchor = currentConfig.armorBarAnchor.get(); break;
                case AIR_BAR: barRect = AirBarRenderer.getScreenRect(player); currentAnchor = currentConfig.airBarAnchor.get(); break;
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

        super.render(graphics, mouseX, mouseY, partialTicks); 
    }

    @Override
    public void renderBackground(GuiGraphics graphics#if NEWER_THAN_20_1, int mouseX, int mouseY, float partialTicks#endif) {
        // Do nothing here to prevent the default background dim/dirt.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
             return true;
        }

        if (!EditModeManager.isEditModeEnabled() || button != 0) {
            return false;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null) return false;

        long currentTime = System.currentTimeMillis();
        boolean isDoubleClick = (currentTime - lastClickTime < DOUBLE_CLICK_TIME_MS) &&
                                (Math.abs(mouseX - lastClickX) < 5 && Math.abs(mouseY - lastClickY) < 5);
        boolean actionTaken = false;

        if (isDoubleClick) {
            DraggableElement clickedBarForFocus = getClickedBarComplex(mouseX, mouseY, player);
            if (clickedBarForFocus != null) {
                if (EditModeManager.getFocusedElement() == clickedBarForFocus) {
                    EditModeManager.clearFocusedElement(); 
                } else {
                    EditModeManager.setFocusedElement(clickedBarForFocus); 
                }
                rebuildEditorWidgets();
                actionTaken = true;
            }
            lastClickTime = 0; 
        }

        if (!actionTaken) {
            if (EditModeManager.getFocusedElement() != null) {
                if (EditModeManager.getFocusedElement() != DraggableElement.ARMOR_BAR && EditModeManager.getFocusedElement() != DraggableElement.AIR_BAR) {
                    SubElementType clickedSub = getClickedSubElement(EditModeManager.getFocusedElement(), mouseX, mouseY, player);
                    if (clickedSub != null && clickedSub != SubElementType.BACKGROUND) {
                        int currentSubX = 0; int currentSubY = 0;
                        ClientConfig currentConfig = ModConfigManager.getClient();
                        DraggableElement currentFocused = EditModeManager.getFocusedElement(); // Already known to be non-null here
                        switch (currentFocused) { 
                             case HEALTH_BAR: if (clickedSub == SubElementType.BAR_MAIN) { currentSubX = currentConfig.healthBarXOffset.get(); currentSubY = currentConfig.healthBarYOffset.get(); } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { currentSubX = currentConfig.healthOverlayXOffset.get(); currentSubY = currentConfig.healthOverlayYOffset.get(); } break;
                             case MANA_BAR: if (clickedSub == SubElementType.BAR_MAIN) { currentSubX = currentConfig.manaBarXOffset.get(); currentSubY = currentConfig.manaBarYOffset.get(); } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { currentSubX = currentConfig.manaOverlayXOffset.get(); currentSubY = currentConfig.manaOverlayYOffset.get(); } break;
                             case STAMINA_BAR: if (clickedSub == SubElementType.BAR_MAIN) { currentSubX = currentConfig.staminaBarXOffset.get(); currentSubY = currentConfig.staminaBarYOffset.get(); } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { currentSubX = currentConfig.staminaOverlayXOffset.get(); currentSubY = currentConfig.staminaOverlayYOffset.get(); } break;
                        }
                        EditModeManager.setDraggedSubElement(clickedSub, (int)mouseX, (int)mouseY, currentSubX, currentSubY);
                        
                        // Prepare for potential sub-element undo
                        this.lastFocusedElementForSubUndo = currentFocused;
                        this.lastDraggedSubElementForUndo = clickedSub;
                        this.lastSubDragInitialXOffset = currentSubX;
                        this.lastSubDragInitialYOffset = currentSubY;
                        this.canUndoLastSubDrag = false; // Becomes true after a confirmed drag in mouseReleased
                        this.canUndoLastDrag = false; // Sub-element action overrides whole-element undo
                        this.lastDraggedElementForUndo = null;

                        actionTaken = true;
                    } else {
                        // Clicked outside a draggable sub-element of a focused bar
                        // This might mean clicking on the background of the focused bar, or empty space.
                        // We don't want to immediately start dragging the whole bar if a sub-element was missed.
                        // If no sub-element action, let it fall through to general bar dragging IF this click wasn't on a button.
                        // Super.mouseClicked already handled buttons.
                        // actionTaken = true; // Tentatively consume, might need refinement
                    }
                } else {
                     actionTaken = true; // Armor/Air focused, no sub-elements, so click is handled (e.g. by buttons or does nothing)
                }
            }
            
            if (!actionTaken && EditModeManager.getFocusedElement() == null) { // Only try to drag whole bar if not focused or if focus click failed
                DraggableElement clickedBarForDrag = getClickedBarComplex(mouseX, mouseY, player);
                 if (clickedBarForDrag != null) {
                    int totalX = 0; int totalY = 0;
                    ClientConfig currentConfig = ModConfigManager.getClient();
                     switch (clickedBarForDrag) {
                        case HEALTH_BAR: totalX = currentConfig.healthTotalXOffset.get(); totalY = currentConfig.healthTotalYOffset.get(); break;
                        case MANA_BAR: totalX = currentConfig.manaTotalXOffset.get(); totalY = currentConfig.manaTotalYOffset.get(); break;
                        case STAMINA_BAR: totalX = currentConfig.staminaTotalXOffset.get(); totalY = currentConfig.staminaTotalYOffset.get(); break;
                        case ARMOR_BAR: totalX = currentConfig.armorTotalXOffset.get(); totalY = currentConfig.armorTotalYOffset.get(); break;
                        case AIR_BAR: totalX = currentConfig.airTotalXOffset.get(); totalY = currentConfig.airTotalYOffset.get(); break;
                    }
                    // Prepare for potential undo
                    this.lastDraggedElementForUndo = clickedBarForDrag;
                    this.lastDragInitialXOffset = totalX;
                    this.lastDragInitialYOffset = totalY;
                    this.canUndoLastDrag = false; // Becomes true only after a confirmed drag in mouseReleased

                    EditModeManager.setDraggedElement(clickedBarForDrag, (int) mouseX, (int) mouseY, totalX, totalY);
                    actionTaken = true;
                 }
            }
        }

        if (!isDoubleClick && !actionTaken) { // If not a double click and no action taken yet by dragging/focusing
            lastClickTime = currentTime;
            lastClickX = mouseX;
            lastClickY = mouseY;
        } else if (!isDoubleClick && actionTaken) { // Single click that resulted in an action (drag start)
             lastClickTime = currentTime; // Still update for next potential double click
             lastClickX = mouseX;
             lastClickY = mouseY;
        }
        // If it IS a double click, lastClickTime was reset to 0 to prevent triple click issues.

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
        ClientConfig currentConfig = ModConfigManager.getClient();

        switch (focusedBar) {
            case HEALTH_BAR:
                barMainRect = HealthBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                if (currentConfig.enableHealthForeground.get()) {
                    barFgRect = HealthBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                }
                break;
            case STAMINA_BAR:
                barMainRect = StaminaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                if (currentConfig.enableStaminaForeground.get()) {
                    barFgRect = StaminaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                }
                break;
            case MANA_BAR:
                barMainRect = ManaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                if (currentConfig.enableManaForeground.get()) {
                    barFgRect = ManaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                }
                break;
        }

        if (barFgRect != null && barFgRect.contains((int)mouseX, (int)mouseY)) return SubElementType.FOREGROUND_DETAIL;
        if (barMainRect != null && barMainRect.contains((int)mouseX, (int)mouseY)) return SubElementType.BAR_MAIN;
        return null; 
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (button != 0) return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        ClientConfig currentConfig = ModConfigManager.getClient();

        if (EditModeManager.getDraggedSubElement() != null && EditModeManager.getFocusedElement() != null &&
            EditModeManager.getFocusedElement() != DraggableElement.ARMOR_BAR && EditModeManager.getFocusedElement() != DraggableElement.AIR_BAR) {
            int deltaX = (int) (mouseX - EditModeManager.getSubElementDragStartX());
            int deltaY = (int) (mouseY - EditModeManager.getSubElementDragStartY());
            int newSubX = EditModeManager.getInitialSubElementXOffset() + deltaX;
            int newSubY = EditModeManager.getInitialSubElementYOffset() + deltaY;

            DraggableElement focused = EditModeManager.getFocusedElement();
            SubElementType sub = EditModeManager.getDraggedSubElement();

            switch (focused) {
                case HEALTH_BAR:
                    if (sub == SubElementType.BAR_MAIN) { currentConfig.healthBarXOffset.set(newSubX); currentConfig.healthBarYOffset.set(newSubY); } 
                    else if (sub == SubElementType.FOREGROUND_DETAIL) { currentConfig.healthOverlayXOffset.set(newSubX); currentConfig.healthOverlayYOffset.set(newSubY); }
                    break;
                case MANA_BAR:
                     if (sub == SubElementType.BAR_MAIN) { currentConfig.manaBarXOffset.set(newSubX); currentConfig.manaBarYOffset.set(newSubY); } 
                     else if (sub == SubElementType.FOREGROUND_DETAIL) { currentConfig.manaOverlayXOffset.set(newSubX); currentConfig.manaOverlayYOffset.set(newSubY); }
                    break;
                case STAMINA_BAR:
                     if (sub == SubElementType.BAR_MAIN) { currentConfig.staminaBarXOffset.set(newSubX); currentConfig.staminaBarYOffset.set(newSubY); } 
                     else if (sub == SubElementType.FOREGROUND_DETAIL) { currentConfig.staminaOverlayXOffset.set(newSubX); currentConfig.staminaOverlayYOffset.set(newSubY); }
                    break;
            }
            return true; 
        }
        else if (EditModeManager.getDraggedElement() != null) {
            int deltaX = (int) (mouseX - EditModeManager.getDragStartX());
            int deltaY = (int) (mouseY - EditModeManager.getDragStartY());
            int newTotalX = EditModeManager.getInitialElementXOffset() + deltaX;
            int newTotalY = EditModeManager.getInitialElementYOffset() + deltaY;
            DraggableElement dragged = EditModeManager.getDraggedElement();
            switch (dragged) {
                case HEALTH_BAR: currentConfig.healthTotalXOffset.set(newTotalX); currentConfig.healthTotalYOffset.set(newTotalY); break;
                case MANA_BAR: currentConfig.manaTotalXOffset.set(newTotalX); currentConfig.manaTotalYOffset.set(newTotalY); break;
                case STAMINA_BAR: currentConfig.staminaTotalXOffset.set(newTotalX); currentConfig.staminaTotalYOffset.set(newTotalY); break;
                case ARMOR_BAR: currentConfig.armorTotalXOffset.set(newTotalX); currentConfig.armorTotalYOffset.set(newTotalY); break;
                case AIR_BAR: currentConfig.airTotalXOffset.set(newTotalX); currentConfig.airTotalYOffset.set(newTotalY); break;
            }
            return true; 
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        boolean handled = false;
        if (EditModeManager.getDraggedSubElement() != null && button == 0) {
            DraggableElement currentFocused = EditModeManager.getFocusedElement();
            SubElementType releasedSubElement = EditModeManager.getDraggedSubElement();

            if (currentFocused == this.lastFocusedElementForSubUndo && releasedSubElement == this.lastDraggedSubElementForUndo) {
                double dragThreshold = 1.5;
                if (Math.abs(mouseX - EditModeManager.getSubElementDragStartX()) > dragThreshold ||
                    Math.abs(mouseY - EditModeManager.getSubElementDragStartY()) > dragThreshold) {
                    this.canUndoLastSubDrag = true;
                } else {
                    this.canUndoLastSubDrag = false;
                    this.lastFocusedElementForSubUndo = null;
                    this.lastDraggedSubElementForUndo = null;
                }
            } else {
                this.canUndoLastSubDrag = false;
                this.lastFocusedElementForSubUndo = null;
                this.lastDraggedSubElementForUndo = null;
            }
            EditModeManager.clearDraggedSubElement();
            handled = true;
        }
        if (EditModeManager.getDraggedElement() != null && button == 0) {
            DraggableElement releasedElement = EditModeManager.getDraggedElement();
            if (releasedElement == this.lastDraggedElementForUndo) {
                // Check if the mouse actually moved significantly from drag start
                double dragThreshold = 1.5; // Small threshold to differentiate click from drag
                if (Math.abs(mouseX - EditModeManager.getDragStartX()) > dragThreshold || 
                    Math.abs(mouseY - EditModeManager.getDragStartY()) > dragThreshold) {
                    this.canUndoLastDrag = true; // A drag occurred, so make it undoable
                } else {
                    // Not a significant drag, so don't enable undo for this "move"
                    this.canUndoLastDrag = false; 
                    this.lastDraggedElementForUndo = null; // Clear, as no actual move to undo occurred
                }
            } else {
                // If a different element was involved, or something unexpected, clear undo state
                this.canUndoLastDrag = false;
                this.lastDraggedElementForUndo = null;
            }
            EditModeManager.clearDraggedElement();
            handled = true;
        }
        return handled || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void onClose() {
        if (EditModeManager.isEditModeEnabled()) {
            EditModeManager.toggleEditMode(); 
        }
        if (EditModeManager.getFocusedElement() != null) { 
            EditModeManager.clearFocusedElement();
        }
        this.minecraft.setScreen(this.previousScreen); 
        this.canUndoLastDrag = false; 
        this.lastDraggedElementForUndo = null;
        this.canUndoLastSubDrag = false;
        this.lastFocusedElementForSubUndo = null;
        this.lastDraggedSubElementForUndo = null;
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
        // Ctrl+Z for undo
        if (Screen.hasControlDown() && keyCode == GLFW.GLFW_KEY_Z) {
            if (this.canUndoLastSubDrag && this.lastFocusedElementForSubUndo != null && this.lastDraggedSubElementForUndo != null) {
                ClientConfig config = ModConfigManager.getClient();
                // Apply undo for sub-element
                switch (this.lastFocusedElementForSubUndo) {
                    case HEALTH_BAR:
                        if (this.lastDraggedSubElementForUndo == SubElementType.BAR_MAIN) {
                            config.healthBarXOffset.set(this.lastSubDragInitialXOffset);
                            config.healthBarYOffset.set(this.lastSubDragInitialYOffset);
                        } else if (this.lastDraggedSubElementForUndo == SubElementType.FOREGROUND_DETAIL) {
                            config.healthOverlayXOffset.set(this.lastSubDragInitialXOffset);
                            config.healthOverlayYOffset.set(this.lastSubDragInitialYOffset);
                        }
                        break;
                    case MANA_BAR:
                        if (this.lastDraggedSubElementForUndo == SubElementType.BAR_MAIN) {
                            config.manaBarXOffset.set(this.lastSubDragInitialXOffset);
                            config.manaBarYOffset.set(this.lastSubDragInitialYOffset);
                        } else if (this.lastDraggedSubElementForUndo == SubElementType.FOREGROUND_DETAIL) {
                            config.manaOverlayXOffset.set(this.lastSubDragInitialXOffset);
                            config.manaOverlayYOffset.set(this.lastSubDragInitialYOffset);
                        }
                        break;
                    case STAMINA_BAR:
                        if (this.lastDraggedSubElementForUndo == SubElementType.BAR_MAIN) {
                            config.staminaBarXOffset.set(this.lastSubDragInitialXOffset);
                            config.staminaBarYOffset.set(this.lastSubDragInitialYOffset);
                        } else if (this.lastDraggedSubElementForUndo == SubElementType.FOREGROUND_DETAIL) {
                            config.staminaOverlayXOffset.set(this.lastSubDragInitialXOffset);
                            config.staminaOverlayYOffset.set(this.lastSubDragInitialYOffset);
                        }
                        break;
                }
                this.canUndoLastSubDrag = false; // Consume sub-undo
                return true;
            } else if (this.canUndoLastDrag && this.lastDraggedElementForUndo != null) {
                ClientConfig config = ModConfigManager.getClient();
                switch (this.lastDraggedElementForUndo) {
                    case HEALTH_BAR:
                        config.healthTotalXOffset.set(this.lastDragInitialXOffset);
                        config.healthTotalYOffset.set(this.lastDragInitialYOffset);
                        break;
                    case MANA_BAR:
                        config.manaTotalXOffset.set(this.lastDragInitialXOffset);
                        config.manaTotalYOffset.set(this.lastDragInitialYOffset);
                        break;
                    case STAMINA_BAR:
                        config.staminaTotalXOffset.set(this.lastDragInitialXOffset);
                        config.staminaTotalYOffset.set(this.lastDragInitialYOffset);
                        break;
                    case ARMOR_BAR:
                        config.armorTotalXOffset.set(this.lastDragInitialXOffset);
                        config.armorTotalYOffset.set(this.lastDragInitialYOffset);
                        break;
                    case AIR_BAR:
                        config.airTotalXOffset.set(this.lastDragInitialXOffset);
                        config.airTotalYOffset.set(this.lastDragInitialYOffset);
                        break;
                }
                this.canUndoLastDrag = false; // Consume the undo
                // Optional: rebuildEditorWidgets(); if needed, though render should pick it up.
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    protected void clearWidgets() {
        this.renderables.clear();
        this.children().clear();
    }

    private void resetPositionDefaultsAction(DraggableElement element) {
        ClientConfig config = ModConfigManager.getClient();
        if (element == null) return;

        HUDPositioning.BarPlacement defaultAnchor = null;
        int defaultBarX = 0, defaultBarY = 0, defaultOverlayX = 0, defaultOverlayY = 0, defaultTotalY = 0;
        int defaultBgWidthForCalc = 0; 

        switch (element) {
            case HEALTH_BAR: 
                defaultAnchor = ClientConfig.DEFAULT_HEALTH_BAR_ANCHOR;
                defaultBgWidthForCalc = ClientConfig.DEFAULT_HEALTH_BACKGROUND_WIDTH;
                defaultBarX = ClientConfig.DEFAULT_HEALTH_BAR_X_OFFSET; defaultBarY = ClientConfig.DEFAULT_HEALTH_BAR_Y_OFFSET;
                defaultOverlayX = ClientConfig.DEFAULT_HEALTH_OVERLAY_X_OFFSET; defaultOverlayY = ClientConfig.DEFAULT_HEALTH_OVERLAY_Y_OFFSET;
                defaultTotalY = ClientConfig.DEFAULT_HEALTH_TOTAL_Y_OFFSET;
                config.healthBarAnchor.set(defaultAnchor);
                config.healthBarXOffset.set(defaultBarX); config.healthBarYOffset.set(defaultBarY);
                config.healthOverlayXOffset.set(defaultOverlayX); config.healthOverlayYOffset.set(defaultOverlayY);
                config.healthTotalYOffset.set(defaultTotalY);
                break;
            case STAMINA_BAR:
                defaultAnchor = ClientConfig.DEFAULT_STAMINA_BAR_ANCHOR;
                defaultBgWidthForCalc = ClientConfig.DEFAULT_STAMINA_BACKGROUND_WIDTH;
                defaultBarX = ClientConfig.DEFAULT_STAMINA_BAR_X_OFFSET; defaultBarY = ClientConfig.DEFAULT_STAMINA_BAR_Y_OFFSET;
                defaultOverlayX = ClientConfig.DEFAULT_STAMINA_OVERLAY_X_OFFSET; defaultOverlayY = ClientConfig.DEFAULT_STAMINA_OVERLAY_Y_OFFSET;
                defaultTotalY = ClientConfig.DEFAULT_STAMINA_TOTAL_Y_OFFSET;
                config.staminaBarAnchor.set(defaultAnchor);
                config.staminaBarXOffset.set(defaultBarX); config.staminaBarYOffset.set(defaultBarY);
                config.staminaOverlayXOffset.set(defaultOverlayX); config.staminaOverlayYOffset.set(defaultOverlayY);
                config.staminaTotalYOffset.set(defaultTotalY);
                break;
            case MANA_BAR:
                defaultAnchor = ClientConfig.DEFAULT_MANA_BAR_ANCHOR;
                defaultBgWidthForCalc = ClientConfig.DEFAULT_MANA_BACKGROUND_WIDTH;
                defaultBarX = ClientConfig.DEFAULT_MANA_BAR_X_OFFSET; defaultBarY = ClientConfig.DEFAULT_MANA_BAR_Y_OFFSET;
                defaultOverlayX = ClientConfig.DEFAULT_MANA_OVERLAY_X_OFFSET; defaultOverlayY = ClientConfig.DEFAULT_MANA_OVERLAY_Y_OFFSET;
                defaultTotalY = ClientConfig.DEFAULT_MANA_TOTAL_Y_OFFSET;
                config.manaBarAnchor.set(defaultAnchor);
                config.manaBarXOffset.set(defaultBarX); config.manaBarYOffset.set(defaultBarY);
                config.manaOverlayXOffset.set(defaultOverlayX); config.manaOverlayYOffset.set(defaultOverlayY);
                config.manaTotalYOffset.set(defaultTotalY);
                break;
            case ARMOR_BAR:
                defaultAnchor = ClientConfig.DEFAULT_ARMOR_BAR_ANCHOR;
                defaultBgWidthForCalc = ClientConfig.DEFAULT_ARMOR_BACKGROUND_WIDTH;
                defaultTotalY = ClientConfig.DEFAULT_ARMOR_TOTAL_Y_OFFSET;
                config.armorBarAnchor.set(defaultAnchor);
                config.armorBarXOffset.set(ClientConfig.DEFAULT_ARMOR_BAR_X_OFFSET);
                config.armorBarYOffset.set(ClientConfig.DEFAULT_ARMOR_BAR_Y_OFFSET);
                config.armorTotalYOffset.set(defaultTotalY);
                break;
            case AIR_BAR:
                defaultAnchor = ClientConfig.DEFAULT_AIR_BAR_ANCHOR;
                defaultBgWidthForCalc = ClientConfig.DEFAULT_AIR_BACKGROUND_WIDTH;
                defaultTotalY = ClientConfig.DEFAULT_AIR_TOTAL_Y_OFFSET;
                config.airBarAnchor.set(defaultAnchor);
                config.airBarXOffset.set(ClientConfig.DEFAULT_AIR_BAR_X_OFFSET);
                config.airBarYOffset.set(ClientConfig.DEFAULT_AIR_BAR_Y_OFFSET);
                config.airTotalYOffset.set(defaultTotalY);
                break;
             default: return; 
        }

        int newDefaultTotalX = 0;
        if (defaultAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultTotalX = -defaultBgWidthForCalc / 2; }
        else if (defaultAnchor != null && defaultAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultTotalX = -defaultBgWidthForCalc; }
        
        switch(element) {
            case HEALTH_BAR: config.healthTotalXOffset.set(newDefaultTotalX); break;
            case STAMINA_BAR: config.staminaTotalXOffset.set(newDefaultTotalX); break;
            case MANA_BAR: config.manaTotalXOffset.set(newDefaultTotalX); break;
            case ARMOR_BAR: config.armorTotalXOffset.set(newDefaultTotalX); break;
            case AIR_BAR: config.airTotalXOffset.set(newDefaultTotalX); break;
        }
        rebuildEditorWidgets(); 
    }

    private void resetSizeDefaultsAction(DraggableElement element) {
        ClientConfig config = ModConfigManager.getClient();
        if (element == null) return;

        switch(element) {
            case HEALTH_BAR: 
                config.healthBackgroundWidth.set(ClientConfig.DEFAULT_HEALTH_BACKGROUND_WIDTH); config.healthBackgroundHeight.set(ClientConfig.DEFAULT_HEALTH_BACKGROUND_HEIGHT);
                config.healthBarWidth.set(ClientConfig.DEFAULT_HEALTH_BAR_WIDTH); config.healthBarHeight.set(ClientConfig.DEFAULT_HEALTH_BAR_HEIGHT);
                config.healthOverlayWidth.set(ClientConfig.DEFAULT_HEALTH_OVERLAY_WIDTH); config.healthOverlayHeight.set(ClientConfig.DEFAULT_HEALTH_OVERLAY_HEIGHT);
                break;
            case STAMINA_BAR: 
                config.staminaBackgroundWidth.set(ClientConfig.DEFAULT_STAMINA_BACKGROUND_WIDTH); config.staminaBackgroundHeight.set(ClientConfig.DEFAULT_STAMINA_BACKGROUND_HEIGHT);
                config.staminaBarWidth.set(ClientConfig.DEFAULT_STAMINA_BAR_WIDTH); config.staminaBarHeight.set(ClientConfig.DEFAULT_STAMINA_BAR_HEIGHT);
                config.staminaOverlayWidth.set(ClientConfig.DEFAULT_STAMINA_OVERLAY_WIDTH); config.staminaOverlayHeight.set(ClientConfig.DEFAULT_STAMINA_OVERLAY_HEIGHT);
                break;
            case MANA_BAR: 
                config.manaBackgroundWidth.set(ClientConfig.DEFAULT_MANA_BACKGROUND_WIDTH); config.manaBackgroundHeight.set(ClientConfig.DEFAULT_MANA_BACKGROUND_HEIGHT);
                config.manaBarWidth.set(ClientConfig.DEFAULT_MANA_BAR_WIDTH); config.manaBarHeight.set(ClientConfig.DEFAULT_MANA_BAR_HEIGHT);
                config.manaOverlayWidth.set(ClientConfig.DEFAULT_MANA_OVERLAY_WIDTH); config.manaOverlayHeight.set(ClientConfig.DEFAULT_MANA_OVERLAY_HEIGHT);
                break;
            case ARMOR_BAR:
                config.armorBackgroundWidth.set(ClientConfig.DEFAULT_ARMOR_BACKGROUND_WIDTH); config.armorBackgroundHeight.set(ClientConfig.DEFAULT_ARMOR_BACKGROUND_HEIGHT);
                config.armorBarWidth.set(ClientConfig.DEFAULT_ARMOR_BAR_WIDTH); config.armorBarHeight.set(ClientConfig.DEFAULT_ARMOR_BAR_HEIGHT);
                break;
            case AIR_BAR:
                config.airBackgroundWidth.set(ClientConfig.DEFAULT_AIR_BACKGROUND_WIDTH); config.airBackgroundHeight.set(ClientConfig.DEFAULT_AIR_BACKGROUND_HEIGHT);
                config.airBarWidth.set(ClientConfig.DEFAULT_AIR_BAR_WIDTH); config.airBarHeight.set(ClientConfig.DEFAULT_AIR_BAR_HEIGHT);
                break;
        }
        rebuildEditorWidgets(); 
    }

    private void resetAllDefaultsAction() {
        resetPositionDefaultsAction(DraggableElement.HEALTH_BAR);
        resetSizeDefaultsAction(DraggableElement.HEALTH_BAR);
        resetPositionDefaultsAction(DraggableElement.STAMINA_BAR);
        resetSizeDefaultsAction(DraggableElement.STAMINA_BAR);
        resetPositionDefaultsAction(DraggableElement.MANA_BAR);
        resetSizeDefaultsAction(DraggableElement.MANA_BAR);
        resetPositionDefaultsAction(DraggableElement.ARMOR_BAR);
        resetSizeDefaultsAction(DraggableElement.ARMOR_BAR);
        resetPositionDefaultsAction(DraggableElement.AIR_BAR);
        resetSizeDefaultsAction(DraggableElement.AIR_BAR);
        rebuildEditorWidgets(); 
    }

    private void openConfirmScreen(Component title, Component explanation, Runnable confirmAction) {
        this.minecraft.setScreen(new ConfirmResetScreen(this, title, explanation, confirmAction));
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

    private Component getBarBehaviorComponent(BarRenderBehavior behavior, String barTypeKey) {
        String behaviorKey = "bar_behavior." + behavior.name().toLowerCase();
        // e.g., "gui.dynamic_resource_bars.hud_editor.button.armor_behavior_format"
        // expects one argument: the behavior string itself (e.g. "Custom")
        return Component.translatable(
            "gui.dynamic_resource_bars.hud_editor.button." + barTypeKey + "_behavior_format", 
            Component.translatable(behaviorKey)
        );
    }
} 