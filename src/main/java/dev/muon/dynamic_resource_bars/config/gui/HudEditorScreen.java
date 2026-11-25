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
import net.minecraft.client.gui.components.Tooltip;

import java.util.function.Supplier;

public class HudEditorScreen extends Screen {

    private static final int HELP_TEXT_TOP_Y = 15;
    private static final int LINE_SPACING = 2;
    private static final int HANDLE_SIZE = 3;
    private static final int HANDLE_HOVER_COLOR = 0xAAFFFF00;

    private enum ResizeMode {
        NONE,
        WIDTH,
        HEIGHT
    }

    private ResizeMode currentResizeMode = ResizeMode.NONE;
    private int resizeStartX;
    private int resizeStartY;
    private int initialWidth;
    private int initialHeight;
    private SubElementType resizingSubElement = null;
    private DraggableElement focusedElementForResize = null;

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
    private Button resetPositionButton;
    private Button resetSizeButton;
    private Button cycleFillDirectionButton;
    private Button toggleAboveXpButton;

    // Buttons for Non-Focus Mode (Grid)
    private Button toggleHealthBarButton;
    private Button toggleStaminaBarButton;
    private Button cycleManaBarBehaviorButton;
    private Button cycleArmorBehaviorButton;
    private Button cycleAirBehaviorButton;
    private Button openHealthSettingsButton;
    private Button openStaminaSettingsButton;
    private Button openManaSettingsButton;
    private Button openArmorSettingsButton;
    private Button openAirSettingsButton;
    private Button resetButtonForAllBars;
    private Button cycleStaminaBarBehaviorButton;

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
        toggleAboveXpButton = null;
        cycleFillDirectionButton = null;
        toggleBackgroundButton = null; toggleForegroundButton = null; toggleFadeFullButton = null;
        cycleTextBehaviorButton = null; cycleTextAlignButton = null; cycleAnchorButton = null;
        resetPositionButton = null; resetSizeButton = null;
        toggleHealthBarButton = null; toggleStaminaBarButton = null; cycleManaBarBehaviorButton = null;
        cycleArmorBehaviorButton = null; cycleAirBehaviorButton = null;
        openHealthSettingsButton = null; openStaminaSettingsButton = null; openManaSettingsButton = null;
        openArmorSettingsButton = null; openAirSettingsButton = null;
        resetButtonForAllBars = null;

        if (focused == null) {
            int gridButtonHeight = 20;
            int rowSpacing = 5;
            int colSpacing = 5;
            int gridTopY = 40;
            int currentX, currentY;

            // Section 1: Health, Mana, Stamina (3 columns)
            int threeColButtonWidth = 100;
            int threeColContentWidth = 3 * threeColButtonWidth + 2 * colSpacing;
            int threeColStartX = (this.width - threeColContentWidth) / 2;

            currentY = gridTopY;
            currentX = threeColStartX;

            toggleHealthBarButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.health_toggle_format",
                config.enableHealthBar ? Component.translatable("gui.dynamic_resource_bars.behavior.custom_simple") : Component.translatable("gui.dynamic_resource_bars.behavior.vanilla_simple")),
                (b) -> { config.enableHealthBar = !config.enableHealthBar; rebuildEditorWidgets(); }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            addRenderableWidget(toggleHealthBarButton);
            currentX += threeColButtonWidth + colSpacing;

            boolean hasManaProvider = ManaProviderManager.hasAnyManaMods();
            cycleManaBarBehaviorButton = Button.builder(getManaBarBehaviorComponent(config.manaBarBehavior),
                (b) -> {
                if (b.active) {
                    config.manaBarBehavior = getNextAvailableManaBarBehavior(config.manaBarBehavior);
                    ManaProviderManager.updateActiveProvider();
                    rebuildEditorWidgets();
                }
            }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            cycleManaBarBehaviorButton.active = hasManaProvider;
            if (!hasManaProvider) {
                cycleManaBarBehaviorButton.setTooltip(Tooltip.create(Component.translatable("gui.dynamic_resource_bars.hud_editor.tooltip.no_mana_provider")));
            }
            addRenderableWidget(cycleManaBarBehaviorButton);
            currentX += threeColButtonWidth + colSpacing;

            cycleStaminaBarBehaviorButton = Button.builder(getStaminaBarBehaviorComponent(config.staminaBarBehavior),
                (b) -> {
                    config.staminaBarBehavior = getNextAvailableStaminaBarBehavior(config.staminaBarBehavior);
                    StaminaProviderManager.updateActiveProvider();
                    rebuildEditorWidgets();
                }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            addRenderableWidget(cycleStaminaBarBehaviorButton);

            // Row 2: H, M, S Settings
            currentY += gridButtonHeight + rowSpacing;
            currentX = threeColStartX;

            openHealthSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.health_settings"), (b) -> { EditModeManager.setFocusedElement(DraggableElement.HEALTH_BAR); rebuildEditorWidgets(); }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            openHealthSettingsButton.active = config.enableHealthBar;
            addRenderableWidget(openHealthSettingsButton);
            currentX += threeColButtonWidth + colSpacing;

            openManaSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.mana_settings"), (b) -> {
                if (b.active) { EditModeManager.setFocusedElement(DraggableElement.MANA_BAR); rebuildEditorWidgets(); }
            }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            openManaSettingsButton.active = config.manaBarBehavior != ManaBarBehavior.OFF && hasManaProvider;
            if (!hasManaProvider) {
                openManaSettingsButton.setTooltip(Tooltip.create(Component.translatable("gui.dynamic_resource_bars.hud_editor.tooltip.no_mana_provider")));
            }
            addRenderableWidget(openManaSettingsButton);
            currentX += threeColButtonWidth + colSpacing;

            openStaminaSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.stamina_settings"), (b) -> { EditModeManager.setFocusedElement(DraggableElement.STAMINA_BAR); rebuildEditorWidgets(); }).bounds(currentX, currentY, threeColButtonWidth, gridButtonHeight).build();
            openStaminaSettingsButton.active = config.staminaBarBehavior != StaminaBarBehavior.OFF;
            addRenderableWidget(openStaminaSettingsButton);

            // Section 2: Armor, Air (2 columns)
            currentY += gridButtonHeight + rowSpacing + 5;
            int twoColButtonWidth = 120;
            int twoColContentWidth = 2 * twoColButtonWidth + colSpacing;
            int twoColStartX = (this.width - twoColContentWidth) / 2;
            currentX = twoColStartX;

            cycleArmorBehaviorButton = Button.builder(getBarBehaviorComponent(config.armorBarBehavior, "armor"), (b) -> {
                config.armorBarBehavior = getNextBarBehavior(config.armorBarBehavior);
                rebuildEditorWidgets();
            }).bounds(currentX, currentY, twoColButtonWidth, gridButtonHeight).build();
            addRenderableWidget(cycleArmorBehaviorButton);
            currentX += twoColButtonWidth + colSpacing;

            cycleAirBehaviorButton = Button.builder(getBarBehaviorComponent(config.airBarBehavior, "air"), (b) -> {
                config.airBarBehavior = getNextBarBehavior(config.airBarBehavior);
                rebuildEditorWidgets();
            }).bounds(currentX, currentY, twoColButtonWidth, gridButtonHeight).build();
            addRenderableWidget(cycleAirBehaviorButton);

            currentY += gridButtonHeight + rowSpacing;
            currentX = twoColStartX;

            openArmorSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.armor_settings"), (b) -> {
                if (b.active) { EditModeManager.setFocusedElement(DraggableElement.ARMOR_BAR); rebuildEditorWidgets(); }
            }).bounds(currentX, currentY, twoColButtonWidth, gridButtonHeight).build();
            openArmorSettingsButton.active = config.armorBarBehavior == BarRenderBehavior.CUSTOM;
            addRenderableWidget(openArmorSettingsButton);
            currentX += twoColButtonWidth + colSpacing;

            openAirSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.air_settings"), (b) -> {
                if (b.active) { EditModeManager.setFocusedElement(DraggableElement.AIR_BAR); rebuildEditorWidgets(); }
            }).bounds(currentX, currentY, twoColButtonWidth, gridButtonHeight).build();
            openAirSettingsButton.active = config.airBarBehavior == BarRenderBehavior.CUSTOM;
            addRenderableWidget(openAirSettingsButton);

            // Reset All Button
            currentY += gridButtonHeight + rowSpacing + 10;
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
            // Focus Mode
            int focusButtonWidth = 120;
            int focusButtonHeight = 20;
            int focusColSpacing = 5;
            int focusRowSpacing = 5;

            int buttonsTopY = HELP_TEXT_TOP_Y + (4 * (fontHeight + LINE_SPACING)) + (fontHeight + LINE_SPACING) + 15;

            final Supplier<Boolean> bgGetter;
            final Supplier<Boolean> fgGetter;
            final Supplier<Boolean> fadeGetter;
            final Runnable bgToggler; final Runnable fgToggler; final Runnable fadeToggler;
            final Runnable textCycler; final Runnable textAlignCycler; final Runnable anchorCycler;
            final Runnable fillDirectionCycler;
            final Supplier<Boolean> aboveXpGetter;
            final Runnable aboveXpToggler;
            boolean fgSupported = false;
            boolean textSupported = false;
            boolean anchorSupported = false;
            boolean fadeSupported = false;
            boolean bgSupported = false;
            boolean fillDirectionSupported = false;

            switch (focused) {
                case HEALTH_BAR:
                    bgGetter = () -> config.enableHealthBackground;
                    fgGetter = () -> config.enableHealthForeground;
                    fadeGetter = () -> config.fadeHealthWhenFull;
                    bgToggler = () -> { config.enableHealthBackground = !config.enableHealthBackground; rebuildEditorWidgets(); };
                    fgToggler = () -> { config.enableHealthForeground = !config.enableHealthForeground; rebuildEditorWidgets(); };
                    fadeToggler = () -> { config.fadeHealthWhenFull = !config.fadeHealthWhenFull; rebuildEditorWidgets(); };
                    textCycler = () -> { config.showHealthText = getNextTextBehavior(config.showHealthText); rebuildEditorWidgets(); };
                    textAlignCycler = () -> { config.healthTextAlign = getNextHorizontalAlignment(config.healthTextAlign); rebuildEditorWidgets(); };
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.healthBarAnchor); config.healthBarAnchor = nextAnchor;
                        int bgWidth = config.healthBackgroundWidth; int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.healthTotalXOffset = newDefaultXOffset; config.healthTotalYOffset = 0; rebuildEditorWidgets();
                    };
                    fillDirectionCycler = () -> { config.healthFillDirection = getNextFillDirection(config.healthFillDirection); rebuildEditorWidgets(); };
                    aboveXpGetter = () -> config.healthAboveXpBar;
                    aboveXpToggler = () -> { config.healthAboveXpBar = !config.healthAboveXpBar; rebuildEditorWidgets(); };
                    fgSupported = true; textSupported = true; anchorSupported = true; fadeSupported = true; bgSupported = true;
                    fillDirectionSupported = true;
                    break;
                case MANA_BAR:
                    bgGetter = () -> config.enableManaBackground;
                    fgGetter = () -> config.enableManaForeground;
                    fadeGetter = () -> config.fadeManaWhenFull;
                    bgToggler = () -> { config.enableManaBackground = !config.enableManaBackground; rebuildEditorWidgets(); };
                    fgToggler = () -> { config.enableManaForeground = !config.enableManaForeground; rebuildEditorWidgets(); };
                    fadeToggler = () -> { config.fadeManaWhenFull = !config.fadeManaWhenFull; rebuildEditorWidgets(); };
                    textCycler = () -> { config.showManaText = getNextTextBehavior(config.showManaText); rebuildEditorWidgets(); };
                    textAlignCycler = () -> { config.manaTextAlign = getNextHorizontalAlignment(config.manaTextAlign); rebuildEditorWidgets(); };
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.manaBarAnchor); config.manaBarAnchor = nextAnchor;
                        int bgWidth = config.manaBackgroundWidth; int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.manaTotalXOffset = newDefaultXOffset; config.manaTotalYOffset = 0; rebuildEditorWidgets();
                    };
                    fillDirectionCycler = () -> { config.manaFillDirection = getNextFillDirection(config.manaFillDirection); rebuildEditorWidgets(); };
                    aboveXpGetter = () -> config.manaAboveXpBar;
                    aboveXpToggler = () -> { config.manaAboveXpBar = !config.manaAboveXpBar; rebuildEditorWidgets(); };
                    fgSupported = true; textSupported = true; anchorSupported = true; fadeSupported = true; bgSupported = true;
                    fillDirectionSupported = true;
                    break;
                case STAMINA_BAR:
                    bgGetter = () -> config.enableStaminaBackground;
                    fgGetter = () -> config.enableStaminaForeground;
                    fadeGetter = () -> config.fadeStaminaWhenFull;
                    bgToggler = () -> { config.enableStaminaBackground = !config.enableStaminaBackground; rebuildEditorWidgets(); };
                    fgToggler = () -> { config.enableStaminaForeground = !config.enableStaminaForeground; rebuildEditorWidgets(); };
                    fadeToggler = () -> { config.fadeStaminaWhenFull = !config.fadeStaminaWhenFull; rebuildEditorWidgets(); };
                    textCycler = () -> { config.showStaminaText = getNextTextBehavior(config.showStaminaText); rebuildEditorWidgets(); };
                    textAlignCycler = () -> { config.staminaTextAlign = getNextHorizontalAlignment(config.staminaTextAlign); rebuildEditorWidgets(); };
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.staminaBarAnchor); config.staminaBarAnchor = nextAnchor;
                        int bgWidth = config.staminaBackgroundWidth; int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.staminaTotalXOffset = newDefaultXOffset; config.staminaTotalYOffset = 0; rebuildEditorWidgets();
                    };
                    fillDirectionCycler = () -> { config.staminaFillDirection = getNextFillDirection(config.staminaFillDirection); rebuildEditorWidgets(); };
                    aboveXpGetter = () -> config.staminaAboveXpBar;
                    aboveXpToggler = () -> { config.staminaAboveXpBar = !config.staminaAboveXpBar; rebuildEditorWidgets(); };
                    fgSupported = true; textSupported = true; anchorSupported = true; fadeSupported = true; bgSupported = true;
                    fillDirectionSupported = true;
                    break;
                case ARMOR_BAR:
                    bgGetter = () -> true;
                    fgGetter = () -> false;
                    fadeGetter = () -> false;
                    bgToggler = () -> {}; fgToggler = () -> {}; fadeToggler = () -> {};
                    textCycler = () -> { config.showArmorText = getNextTextBehavior(config.showArmorText); rebuildEditorWidgets(); };
                    textAlignCycler = () -> { config.armorTextAlign = getNextHorizontalAlignment(config.armorTextAlign); rebuildEditorWidgets(); };
                    fillDirectionCycler = () -> {};
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.armorBarAnchor); config.armorBarAnchor = nextAnchor;
                        int bgWidth = config.armorBackgroundWidth;
                        int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; }
                        else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.armorTotalXOffset = newDefaultXOffset; config.armorTotalYOffset = 0;
                        rebuildEditorWidgets();
                    };
                    aboveXpGetter = () -> config.armorAboveXpBar;
                    aboveXpToggler = () -> { config.armorAboveXpBar = !config.armorAboveXpBar; rebuildEditorWidgets(); };
                    fgSupported = true; textSupported = true; anchorSupported = true; fadeSupported = true; bgSupported = true;
                    fillDirectionSupported = true;
                    break;
                case AIR_BAR:
                    bgGetter = () -> true;
                    fgGetter = () -> false;
                    fadeGetter = () -> config.fadeAirWhenFull;
                    bgToggler = () -> {};
                    fgToggler = () -> {};
                    fadeToggler = () -> { config.fadeAirWhenFull = !config.fadeAirWhenFull; rebuildEditorWidgets(); };
                    textCycler = () -> { config.showAirText = getNextTextBehavior(config.showAirText); rebuildEditorWidgets(); };
                    textAlignCycler = () -> { config.airTextAlign = getNextHorizontalAlignment(config.airTextAlign); rebuildEditorWidgets(); };
                    fillDirectionCycler = () -> { config.airFillDirection = getNextFillDirection(config.airFillDirection); rebuildEditorWidgets(); };
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.airBarAnchor); config.airBarAnchor = nextAnchor;
                        int bgWidth = config.airBackgroundWidth; int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.airTotalXOffset = newDefaultXOffset; config.airTotalYOffset = 0; rebuildEditorWidgets();
                    };
                    aboveXpGetter = () -> config.airAboveXpBar;
                    aboveXpToggler = () -> { config.airAboveXpBar = !config.airAboveXpBar; rebuildEditorWidgets(); };
                    anchorSupported = true; bgSupported = false; textSupported = true; fadeSupported = true; fillDirectionSupported = true;
                    break;
                default: return;
            }

            int maxButtonsPerRow = 3;
            int focusGridContentWidth = maxButtonsPerRow * focusButtonWidth + (maxButtonsPerRow - 1) * focusColSpacing;
            int focusGridStartX = (this.width - focusGridContentWidth) / 2;
            int currentX = focusGridStartX;
            int currentY = buttonsTopY;
            int buttonsInCurrentRow = 0;

            if (bgSupported) {
                toggleBackgroundButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.toggle_background_format",
                    bgGetter.get() ? Component.translatable("options.on") : Component.translatable("options.off")),
                    (b) -> { bgToggler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
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

            if (buttonsInCurrentRow > 0 || (textSupported || anchorSupported)) {
                 currentX = focusGridStartX;
                 currentY += focusButtonHeight + focusRowSpacing;
                 buttonsInCurrentRow = 0;
            }

            if (textSupported) {
                TextBehavior currentTextBehavior = TextBehavior.NEVER;
                HorizontalAlignment currentTextAlign = HorizontalAlignment.CENTER;
                if (focused == DraggableElement.HEALTH_BAR) { currentTextBehavior = config.showHealthText; currentTextAlign = config.healthTextAlign; }
                else if (focused == DraggableElement.MANA_BAR) { currentTextBehavior = config.showManaText; currentTextAlign = config.manaTextAlign; }
                else if (focused == DraggableElement.STAMINA_BAR) { currentTextBehavior = config.showStaminaText; currentTextAlign = config.staminaTextAlign; }
                else if (focused == DraggableElement.ARMOR_BAR) { currentTextBehavior = config.showArmorText; currentTextAlign = config.armorTextAlign; }
                else if (focused == DraggableElement.AIR_BAR) { currentTextBehavior = config.showAirText; currentTextAlign = config.airTextAlign; }

                cycleTextBehaviorButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.cycle_text_behavior_format", Component.translatable("text_behavior." + currentTextBehavior.name().toLowerCase())),
                    (b) -> { textCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
                addRenderableWidget(cycleTextBehaviorButton);
                currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;

                if (buttonsInCurrentRow >= maxButtonsPerRow) { currentX = focusGridStartX; currentY += focusButtonHeight + focusRowSpacing; buttonsInCurrentRow = 0; }
                cycleTextAlignButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.cycle_text_align_format", Component.translatable("horizontal_alignment." + currentTextAlign.name().toLowerCase())),
                    (b) -> { textAlignCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
                addRenderableWidget(cycleTextAlignButton);
                currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;
            }

            if (anchorSupported) {
                HUDPositioning.BarPlacement currentAnchor = HUDPositioning.BarPlacement.HEALTH;
                FillDirection currentFillDirection = FillDirection.HORIZONTAL;
                if (focused == DraggableElement.HEALTH_BAR) { currentAnchor = config.healthBarAnchor; currentFillDirection = config.healthFillDirection; }
                else if (focused == DraggableElement.MANA_BAR) { currentAnchor = config.manaBarAnchor; currentFillDirection = config.manaFillDirection; }
                else if (focused == DraggableElement.STAMINA_BAR) { currentAnchor = config.staminaBarAnchor; currentFillDirection = config.staminaFillDirection; }
                else if (focused == DraggableElement.ARMOR_BAR) { currentAnchor = config.armorBarAnchor; }
                else if (focused == DraggableElement.AIR_BAR) { currentAnchor = config.airBarAnchor; currentFillDirection = config.airFillDirection; }

                if (buttonsInCurrentRow >= maxButtonsPerRow) { currentX = focusGridStartX; currentY += focusButtonHeight + focusRowSpacing; buttonsInCurrentRow = 0; }
                cycleAnchorButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.cycle_anchor_format", Component.translatable("bar_placement." + currentAnchor.name().toLowerCase())),
                    (b) -> { anchorCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
                addRenderableWidget(cycleAnchorButton);
                currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;

                if (fillDirectionSupported) {
                    if (buttonsInCurrentRow >= maxButtonsPerRow) { currentX = focusGridStartX; currentY += focusButtonHeight + focusRowSpacing; buttonsInCurrentRow = 0; }
                    cycleFillDirectionButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.cycle_fill_direction_format", Component.translatable("fill_direction." + currentFillDirection.name().toLowerCase())),
                        (b) -> { fillDirectionCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build();
                    addRenderableWidget(cycleFillDirectionButton);
                    currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;
                }
            }

            if (buttonsInCurrentRow >= maxButtonsPerRow) { currentX = focusGridStartX; currentY += focusButtonHeight + focusRowSpacing; buttonsInCurrentRow = 0; }
            toggleAboveXpButton = Button.builder(
                    Component.literal("Above XP: ").append(aboveXpGetter.get() ? Component.translatable("options.on") : Component.translatable("options.off")),
                    (b) -> { aboveXpToggler.run(); })
                    .bounds(currentX, currentY, focusButtonWidth, focusButtonHeight)
                    .build();
            addRenderableWidget(toggleAboveXpButton);
            currentX += focusButtonWidth + focusColSpacing; buttonsInCurrentRow++;

            if (buttonsInCurrentRow > 0) {
                 currentX = focusGridStartX;
                 currentY += focusButtonHeight + focusRowSpacing;
                 buttonsInCurrentRow = 0;
            }
            currentY += 5;

            final DraggableElement finalFocusedElement = focused;

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

    private FillDirection getNextFillDirection(FillDirection current) {
        FillDirection[] directions = FillDirection.values();
        int nextIndex = (current.ordinal() + 1) % directions.length;
        return directions[nextIndex];
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

        for (Renderable renderable : this.renderables) {
            if (renderable instanceof AbstractWidget widget) {
                 minWidgetX = Math.min(minWidgetX, widget.getX());
                 minWidgetY = Math.min(minWidgetY, widget.getY());
                 maxWidgetX = Math.max(maxWidgetX, widget.getX() + widget.getWidth());
                 maxWidgetY = Math.max(maxWidgetY, widget.getY() + widget.getHeight());
                 hasWidgets = true;
            }
        }

        float overallMinX = minWidgetX, overallMinY = HELP_TEXT_TOP_Y;
        float overallMaxX = maxWidgetX, overallMaxY = minWidgetY;
        if (!hasWidgets) {
            overallMinX = (this.width / 2.0f) - 100;
            overallMaxX = (this.width / 2.0f) + 100;
            overallMaxY = HELP_TEXT_TOP_Y + 50;
        } else {
            overallMaxY = maxWidgetY;
        }

        int currentTextY = HELP_TEXT_TOP_Y;

        if (focused == null) {
            Component helpLine1 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line1");
            Component helpLine2 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line2");

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

            overallMaxY = Math.max(overallMaxY, currentTextY);

        } else {
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

            currentTextY += 5;
            Component headerText = Component.translatable("gui.dynamic_resource_bars.hud_editor.title_focused", getFriendlyElementName(focused));
            graphics.drawCenteredString(font, headerText, this.width / 2, currentTextY, 0xFFFFFF);
            textWidth = font.width(headerText);
            overallMinX = Math.min(overallMinX, (this.width - textWidth) / 2f);
            overallMaxX = Math.max(overallMaxX, (this.width + textWidth) / 2f);
            currentTextY += fontHeight + LINE_SPACING;

            overallMaxY = Math.max(overallMaxY, currentTextY);
        }

        if (overallMinX <= overallMaxX && overallMinY <= overallMaxY) {
             graphics.fill((int)(overallMinX - padding), (int)(overallMinY - padding),
                           (int)(overallMaxX + padding), (int)(overallMaxY + padding), backgroundColor);
        }

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

        if (focused != null && player != null) {
            drawResizeHandles(graphics, player, focused, mouseX, mouseY);

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
                    graphics.renderOutline(textRect.x() - 1, textRect.y() - 1,
                                         textRect.width() + 2, textRect.height() + 2,
                                         0x60FFFFFF);
                }
            }

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
                    int iconOutlineColor = focused == DraggableElement.ARMOR_BAR ? 0xA0C0C0C0 : 0xA0ADD8E6;
                    graphics.renderOutline(iconRect.x() - 1, iconRect.y() - 1,
                                         iconRect.width() + 2, iconRect.height() + 2,
                                         iconOutlineColor);
                }
            }
        }

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    @Override
    public void renderBackground(GuiGraphics graphics #if NEWER_THAN_20_1, int mouseX, int mouseY, float partialTicks#endif) {
        // Prevent default dim background
    }

    private void drawResizeHandles(GuiGraphics graphics, Player player, DraggableElement focused, int mouseX, int mouseY) {
        ClientConfig config = ModConfigManager.getClient();

        ScreenRect bgRect = null;
        switch (focused) {
            case HEALTH_BAR:
                if (config.enableHealthBackground) {
                    bgRect = HealthBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                    drawHandlesForRect(graphics, bgRect, focused, SubElementType.BACKGROUND, mouseX, mouseY);
                }
                ScreenRect barRect = HealthBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                drawHandlesForRect(graphics, barRect, focused, SubElementType.BAR_MAIN, mouseX, mouseY);
                if (config.enableHealthForeground) {
                    ScreenRect fgRect = HealthBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    drawHandlesForRect(graphics, fgRect, focused, SubElementType.FOREGROUND_DETAIL, mouseX, mouseY);
                }
                break;

            case MANA_BAR:
                if (config.enableManaBackground) {
                    bgRect = ManaBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                    drawHandlesForRect(graphics, bgRect, focused, SubElementType.BACKGROUND, mouseX, mouseY);
                }
                ScreenRect manaBarRect = ManaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                drawHandlesForRect(graphics, manaBarRect, focused, SubElementType.BAR_MAIN, mouseX, mouseY);
                if (config.enableManaForeground) {
                    ScreenRect fgRect = ManaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    drawHandlesForRect(graphics, fgRect, focused, SubElementType.FOREGROUND_DETAIL, mouseX, mouseY);
                }
                break;

            case STAMINA_BAR:
                if (config.enableStaminaBackground) {
                    bgRect = StaminaBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                    drawHandlesForRect(graphics, bgRect, focused, SubElementType.BACKGROUND, mouseX, mouseY);
                }
                ScreenRect staminaBarRect = StaminaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                drawHandlesForRect(graphics, staminaBarRect, focused, SubElementType.BAR_MAIN, mouseX, mouseY);
                if (config.enableStaminaForeground) {
                    ScreenRect fgRect = StaminaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                    drawHandlesForRect(graphics, fgRect, focused, SubElementType.FOREGROUND_DETAIL, mouseX, mouseY);
                }
                break;

            case ARMOR_BAR:
                bgRect = ArmorBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                drawHandlesForRect(graphics, bgRect, focused, SubElementType.BACKGROUND, mouseX, mouseY);
                ScreenRect armorBarRect = ArmorBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                drawHandlesForRect(graphics, armorBarRect, focused, SubElementType.BAR_MAIN, mouseX, mouseY);
                break;

            case AIR_BAR:
                bgRect = AirBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                drawHandlesForRect(graphics, bgRect, focused, SubElementType.BACKGROUND, mouseX, mouseY);
                ScreenRect airBarRect = AirBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                drawHandlesForRect(graphics, airBarRect, focused, SubElementType.BAR_MAIN, mouseX, mouseY);
                break;
        }
    }

    private void drawHandlesForRect(GuiGraphics graphics, ScreenRect rect, DraggableElement focusedElement, SubElementType elementType, int mouseX, int mouseY) {
        if (rect == null || rect.width() <= 0 || rect.height() <= 0) return;

        int handleColor = getOutlineColorForSubElement(focusedElement, elementType);

        int centerY = rect.y() + rect.height() / 2;
        int centerX = rect.x() + rect.width() / 2;

        ScreenRect rightHandle = new ScreenRect(rect.x() + rect.width() - HANDLE_SIZE / 2, centerY - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
        ScreenRect bottomHandle = new ScreenRect(centerX - HANDLE_SIZE / 2, rect.y() + rect.height() - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);

        boolean isRightHover = rightHandle.contains(mouseX, mouseY);
        boolean isBottomHover = bottomHandle.contains(mouseX, mouseY);

        int rightActualColor = isRightHover ? HANDLE_HOVER_COLOR : handleColor;
        int bottomActualColor = isBottomHover ? HANDLE_HOVER_COLOR : handleColor;

        graphics.fill(rightHandle.x(), rightHandle.y(), rightHandle.x() + rightHandle.width(), rightHandle.y() + rightHandle.height(), rightActualColor);
        graphics.fill(bottomHandle.x(), bottomHandle.y(), bottomHandle.x() + bottomHandle.width(), bottomHandle.y() + bottomHandle.height(), bottomActualColor);
    }

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

        int centerY = rect.y() + rect.height() / 2;
        int centerX = rect.x() + rect.width() / 2;

        ScreenRect rightHandle = new ScreenRect(rect.x() + rect.width() - HANDLE_SIZE / 2, centerY - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);
        ScreenRect bottomHandle = new ScreenRect(centerX - HANDLE_SIZE / 2, rect.y() + rect.height() - HANDLE_SIZE / 2, HANDLE_SIZE, HANDLE_SIZE);

        if (rightHandle.contains(mouseX, mouseY)) {
            return new ResizeData(element, subElement, ResizeMode.WIDTH, rect.width(), rect.height());
        } else if (bottomHandle.contains(mouseX, mouseY)) {
            return new ResizeData(element, subElement, ResizeMode.HEIGHT, rect.width(), rect.height());
        }

        return null;
    }

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

        DraggableElement currentFocusedElement = EditModeManager.getFocusedElement();
        ClientConfig config = ModConfigManager.getClient();

        // 1. Resize Handle Interaction
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
                    case ARMOR_BAR:
                        if (subType == SubElementType.BACKGROUND) canTestThisSubElement = true;
                        if (subType == SubElementType.BAR_MAIN) canTestThisSubElement = true;
                        break;
                    case AIR_BAR:
                        if (subType == SubElementType.BACKGROUND) canTestThisSubElement = true;
                        if (subType == SubElementType.BAR_MAIN) canTestThisSubElement = true;
                        break;
                }

                if (canTestThisSubElement) {
                    ResizeData resizeData = getResizeHandleAtPosition(currentFocusedElement, subType, (int)mouseX, (int)mouseY);
                    if (resizeData != null) {
                        startResizeOperation(resizeData, (int)mouseX, (int)mouseY);
                        actionTaken = true;
                        break;
                    }
                }
            }
            if (actionTaken) {
                lastClickTime = 0;
                return true;
            }
        }

        // 2. Double Click for Focus/Defocus
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
            if (actionTaken) return true;
        }

        // 3. Sub-Element Drag Interaction
        if (!actionTaken && currentFocusedElement != null && this.currentResizeMode == ResizeMode.NONE) {
            SubElementType clickedSub = getClickedSubElement(currentFocusedElement, mouseX, mouseY, player);
            if (clickedSub != null) {
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
                this.lastFocusedElementForSubUndo = currentFocusedElement;
                this.lastDraggedSubElementForUndo = clickedSub;
                this.lastSubDragInitialXOffset = currentSubX;
                this.lastSubDragInitialYOffset = currentSubY;
                this.canUndoLastSubDrag = false;
                this.canUndoLastDrag = false;
                this.lastDraggedElementForUndo = null;
                actionTaken = true;
            }
            if (actionTaken) {
                lastClickTime = currentTime; lastClickX = mouseX; lastClickY = mouseY;
                return true;
            }
        }

        // 4. Whole Element Drag Interaction
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
                actionTaken = true;
            }
        }

        if (!isDoubleClick && !actionTaken) {
            lastClickTime = currentTime;
            lastClickX = mouseX;
            lastClickY = mouseY;
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
                if (currentConfig.enableHealthForeground) {
                    barFgRect = HealthBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                }
                if (currentConfig.enableHealthBackground) {
                    barBgRect = HealthBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                }
                barTextRect = HealthBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                barAbsorptionTextRect = HealthBarRenderer.getSubElementRect(SubElementType.ABSORPTION_TEXT, player);
                break;
            case STAMINA_BAR:
                barMainRect = StaminaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                if (currentConfig.enableStaminaForeground) {
                    barFgRect = StaminaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                }
                if (currentConfig.enableStaminaBackground) {
                    barBgRect = StaminaBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                }
                barTextRect = StaminaBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                break;
            case MANA_BAR:
                barMainRect = ManaBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                if (currentConfig.enableManaForeground) {
                    barFgRect = ManaBarRenderer.getSubElementRect(SubElementType.FOREGROUND_DETAIL, player);
                }
                if (currentConfig.enableManaBackground) {
                    barBgRect = ManaBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                }
                barTextRect = ManaBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                break;
            case ARMOR_BAR:
                barMainRect = ArmorBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                barBgRect = ArmorBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                barTextRect = ArmorBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                if (currentConfig.enableArmorIcon) {
                    barIconRect = ArmorBarRenderer.getSubElementRect(SubElementType.ICON, player);
                }
                break;
            case AIR_BAR:
                barMainRect = AirBarRenderer.getSubElementRect(SubElementType.BAR_MAIN, player);
                barBgRect = AirBarRenderer.getSubElementRect(SubElementType.BACKGROUND, player);
                barTextRect = AirBarRenderer.getSubElementRect(SubElementType.TEXT, player);
                if (currentConfig.enableAirIcon) {
                    barIconRect = AirBarRenderer.getSubElementRect(SubElementType.ICON, player);
                }
                break;
        }

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
            handleResize((int)mouseX, (int)mouseY);
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
                    break;
            }
            return true;
        } else if (EditModeManager.getDraggedElement() != null) {
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

        if (this.currentResizeMode != ResizeMode.NONE && button == 0) {
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
            case ARMOR_BAR:
                if (resizeData.subElement == SubElementType.BACKGROUND) { this.initialWidth = config.armorBackgroundWidth; this.initialHeight = config.armorBackgroundHeight; }
                else if (resizeData.subElement == SubElementType.BAR_MAIN) { this.initialWidth = config.armorBarWidth; this.initialHeight = config.armorBarHeight; }
                break;
            case AIR_BAR:
                if (resizeData.subElement == SubElementType.BACKGROUND) { this.initialWidth = config.airBackgroundWidth; this.initialHeight = config.airBackgroundHeight; }
                else if (resizeData.subElement == SubElementType.BAR_MAIN) { this.initialWidth = config.airBarWidth; this.initialHeight = config.airBarHeight; }
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
            case ARMOR_BAR: applyArmorResizing(config, deltaX, deltaY); break;
            case AIR_BAR: applyAirResizing(config, deltaX, deltaY); break;
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
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.healthBarHeight = Math.max(1, Math.min(256, this.initialHeight + deltaY)); }
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
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.manaBarHeight = Math.max(1, Math.min(256, this.initialHeight + deltaY)); }
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
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.staminaBarHeight = Math.max(1, Math.min(256, this.initialHeight + deltaY)); }
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
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.armorBarHeight = Math.max(1, Math.min(256, this.initialHeight + deltaY)); }
                break;
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
                else if (this.currentResizeMode == ResizeMode.HEIGHT) { config.airBarHeight = Math.max(1, Math.min(256, this.initialHeight + deltaY)); }
                break;
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
        ClientConfig.getInstance().save();
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
        switch (element) {
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
        switch (element) {
            case HEALTH_BAR:
                config.healthBackgroundWidth = ClientConfig.DEFAULT_HEALTH_BACKGROUND_WIDTH; config.healthBackgroundHeight = ClientConfig.DEFAULT_HEALTH_BACKGROUND_HEIGHT;
                config.healthBarWidth = ClientConfig.DEFAULT_HEALTH_BAR_WIDTH; config.healthBarHeight = ClientConfig.DEFAULT_HEALTH_BAR_HEIGHT;
                config.healthOverlayWidth = ClientConfig.DEFAULT_HEALTH_OVERLAY_WIDTH; config.healthOverlayHeight = ClientConfig.DEFAULT_HEALTH_OVERLAY_HEIGHT;
                break;
            case STAMINA_BAR:
                config.staminaBackgroundWidth = ClientConfig.DEFAULT_STAMINA_BACKGROUND_WIDTH; config.staminaBackgroundHeight = ClientConfig.DEFAULT_STAMINA_BACKGROUND_HEIGHT;
                config.staminaBarWidth = ClientConfig.DEFAULT_STAMINA_BAR_WIDTH;
                config.staminaBarHeight = ClientConfig.DEFAULT_STAMINA_BAR_HEIGHT;
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
        switch (element) {
            case HEALTH_BAR:
                config.enableHealthBar = ClientConfig.DEFAULT_ENABLE_HEALTH_BAR;
                config.enableHealthBackground = ClientConfig.DEFAULT_ENABLE_HEALTH_BACKGROUND;
                config.enableHealthForeground = ClientConfig.DEFAULT_ENABLE_HEALTH_FOREGROUND;
                config.fadeHealthWhenFull = ClientConfig.DEFAULT_FADE_HEALTH_WHEN_FULL;
                config.showHealthText = ClientConfig.DEFAULT_SHOW_HEALTH_TEXT;
                config.healthTextAlign = ClientConfig.DEFAULT_HEALTH_TEXT_ALIGN;
                config.healthFillDirection = ClientConfig.DEFAULT_HEALTH_FILL_DIRECTION;
                config.healthAboveXpBar = ClientConfig.DEFAULT_HEALTH_ABOVE_XP_BAR;
                break;
            case STAMINA_BAR:
                config.staminaBarBehavior = ClientConfig.DEFAULT_STAMINA_BAR_BEHAVIOR;
                config.enableStaminaBackground = ClientConfig.DEFAULT_ENABLE_STAMINA_BACKGROUND;
                config.enableStaminaForeground = ClientConfig.DEFAULT_ENABLE_STAMINA_FOREGROUND;
                config.fadeStaminaWhenFull = ClientConfig.DEFAULT_FADE_STAMINA_WHEN_FULL;
                config.showStaminaText = ClientConfig.DEFAULT_SHOW_STAMINA_TEXT;
                config.staminaTextAlign = ClientConfig.DEFAULT_STAMINA_TEXT_ALIGN;
                config.staminaFillDirection = ClientConfig.DEFAULT_STAMINA_FILL_DIRECTION;
                config.staminaAboveXpBar = ClientConfig.DEFAULT_STAMINA_ABOVE_XP_BAR;
                break;
            case MANA_BAR:
                config.manaBarBehavior = ClientConfig.DEFAULT_MANA_BAR_BEHAVIOR;
                config.enableManaBackground = ClientConfig.DEFAULT_ENABLE_MANA_BACKGROUND;
                config.enableManaForeground = ClientConfig.DEFAULT_ENABLE_MANA_FOREGROUND;
                config.fadeManaWhenFull = ClientConfig.DEFAULT_FADE_MANA_WHEN_FULL;
                config.showManaText = ClientConfig.DEFAULT_SHOW_MANA_TEXT;
                config.manaTextAlign = ClientConfig.DEFAULT_MANA_TEXT_ALIGN;
                config.manaFillDirection = ClientConfig.DEFAULT_MANA_FILL_DIRECTION;
                config.manaAboveXpBar = ClientConfig.DEFAULT_MANA_ABOVE_XP_BAR;
                break;
            case ARMOR_BAR:
                config.armorBarBehavior = ClientConfig.DEFAULT_ARMOR_BAR_BEHAVIOR;
                config.enableArmorIcon = ClientConfig.DEFAULT_ENABLE_ARMOR_ICON;
                config.showArmorText = ClientConfig.DEFAULT_SHOW_ARMOR_TEXT;
                config.armorTextAlign = ClientConfig.DEFAULT_ARMOR_TEXT_ALIGN;
                config.armorAboveXpBar = ClientConfig.DEFAULT_ARMOR_ABOVE_XP_BAR;
                break;
            case AIR_BAR:
                config.airBarBehavior = ClientConfig.DEFAULT_AIR_BAR_BEHAVIOR;
                config.enableAirIcon = ClientConfig.DEFAULT_ENABLE_AIR_ICON;
                config.showAirText = ClientConfig.DEFAULT_SHOW_AIR_TEXT;
                config.airTextAlign = ClientConfig.DEFAULT_AIR_TEXT_ALIGN;
                config.fadeAirWhenFull = ClientConfig.DEFAULT_FADE_AIR_WHEN_FULL;
                config.airFillDirection = ClientConfig.DEFAULT_AIR_FILL_DIRECTION;
                config.airAboveXpBar = ClientConfig.DEFAULT_AIR_ABOVE_XP_BAR;
                break;
        }
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
        return Component.translatable(
                "gui.dynamic_resource_bars.hud_editor.button." + barTypeKey + "_behavior_format",
                Component.translatable(behaviorKey)
        );
    }

    private int getOutlineColorForSubElement(DraggableElement focusedElement, SubElementType subElementType) {
        if (focusedElement == null || subElementType == null) {
            return 0xFFFFFFFF;
        }
        switch (subElementType) {
            case BACKGROUND:
                return 0xA0FFFF00;
            case BAR_MAIN:
                switch (focusedElement) {
                    case HEALTH_BAR: return 0xA000FF00;
                    case MANA_BAR: return 0xA000FFFF;
                    case STAMINA_BAR: return 0xA0FFA500;
                    case ARMOR_BAR: return 0xA0C0C0C0;
                    case AIR_BAR: return 0xA0ADD8E6;
                    default: return 0xFFFFFFFF;
                }
            case FOREGROUND_DETAIL:
                return 0xA0FF00FF;
            default:
                return 0xFFFFFFFF;
        }
    }

    private ManaBarBehavior getNextAvailableManaBarBehavior(ManaBarBehavior current) {
        ManaBarBehavior next = current;
        int attempts = 0;
        do {
            next = next.getNext();
            attempts++;
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

    private Component getManaBarBehaviorComponent(ManaBarBehavior behavior) {
        Component behaviorText = switch (behavior) {
            case OFF -> Component.translatable("gui.dynamic_resource_bars.behavior.off");
            case IRONS_SPELLBOOKS -> Component.translatable("gui.dynamic_resource_bars.mana_behavior.irons_spellbooks");
            case ARS_NOUVEAU -> Component.translatable("gui.dynamic_resource_bars.mana_behavior.ars_nouveau");
            case RPG_MANA -> Component.translatable("gui.dynamic_resource_bars.mana_behavior.rpg_mana");
            case MANA_ATTRIBUTES -> Component.translatable("gui.dynamic_resource_bars.mana_behavior.mana_attributes");
        };
        return Component.translatable("gui.dynamic_resource_bars.mana_format", behaviorText);
    }

    private Component getStaminaBarBehaviorComponent(StaminaBarBehavior behavior) {
        Component behaviorText = switch (behavior) {
            case OFF -> Component.translatable("gui.dynamic_resource_bars.behavior.off");
            case FOOD -> Component.translatable("gui.dynamic_resource_bars.stamina_bar_behavior.food");
            case STAMINA_ATTRIBUTES -> Component.translatable("gui.dynamic_resource_bars.stamina_bar_behavior.stamina_attributes");
        };
        return Component.translatable("gui.dynamic_resource_bars.stamina_format", behaviorText);
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
            case OFF, FOOD -> true;
            case STAMINA_ATTRIBUTES -> StaminaProviderManager.isModLoaded("staminaattributes");
        };
    }
}