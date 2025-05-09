package dev.muon.dynamic_resource_bars.client.gui;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.foundation.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.foundation.config.CClient;
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
    private Button toggleStaminaBarButton;
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
        
        toggleBackgroundButton = null; toggleForegroundButton = null; toggleFadeFullButton = null;
        cycleTextBehaviorButton = null; cycleTextAlignButton = null; cycleAnchorButton = null;
        resizeButton = null; resetPositionButton = null; resetSizeButton = null;
        toggleHealthBarButton = null; toggleStaminaBarButton = null; toggleManaBarButton = null;
        openHealthSettingsButton = null; openStaminaSettingsButton = null; openManaSettingsButton = null;
        resetButtonForAllBars = null;

        DraggableElement focused = EditModeManager.getFocusedElement();
        CClient config = ModConfigManager.getClient();
        int fontHeight = Minecraft.getInstance().font.lineHeight;

        if (focused == null) {
            int gridButtonWidth = 100;
            int gridButtonHeight = 20;
            int gridTotalWidth = 3 * gridButtonWidth + 2 * 5; 
            int gridStartX = (this.width - gridTotalWidth) / 2; 
            int gridTopY = 45; 
            int rowSpacing = 5;
            int colSpacing = 5;

            int row1Y = gridTopY;
            toggleHealthBarButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.health_toggle_format", boolToOnOff(config.enableHealthBar.get())), (b) -> { config.enableHealthBar.set(!config.enableHealthBar.get()); rebuildEditorWidgets(); }).bounds(gridStartX, row1Y, gridButtonWidth, gridButtonHeight).build();
            addRenderableWidget(toggleHealthBarButton);

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

            int row2Y = row1Y + gridButtonHeight + rowSpacing;
            openHealthSettingsButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.health_settings"), (b) -> { EditModeManager.setFocusedElement(DraggableElement.HEALTH_BAR); rebuildEditorWidgets(); }).bounds(gridStartX, row2Y, gridButtonWidth, gridButtonHeight).build();
            addRenderableWidget(openHealthSettingsButton);

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

            int resetAllY = row2Y + gridButtonHeight + rowSpacing + 10; 
            resetButtonForAllBars = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.reset_all_bars"), (b) -> {
                openConfirmScreen(
                                Component.translatable("gui.dynamic_resource_bars.confirm.reset_all.title"), 
                                Component.translatable("gui.dynamic_resource_bars.confirm.reset_all.explanation"),
                                this::resetAllDefaultsAction);
                })
                .bounds(gridStartX, resetAllY, gridTotalWidth, gridButtonHeight) 
                .build();
            addRenderableWidget(resetButtonForAllBars);

        } else {
            int focusButtonWidth = 100; 
            int focusButtonHeight = 20;
            int focusColSpacing = 5;
            int focusRowSpacing = 3;
            int headerTextY = 15;
            int focusStartY = headerTextY + fontHeight + 10;

            int gridTotalWidth = 3 * focusButtonWidth + 2 * focusColSpacing;
            int gridStartX = (this.width - gridTotalWidth) / 2;
            
            final Supplier<Boolean> bgGetter; final Supplier<Boolean> fgGetter; final Supplier<Boolean> fadeGetter;
            final Supplier<TextBehavior> textGetter; final Supplier<HorizontalAlignment> textAlignGetter;
            final Supplier<HUDPositioning.BarPlacement> anchorGetter;
            final Runnable bgToggler; final Runnable fgToggler; final Runnable fadeToggler;
            final Runnable textCycler; final Runnable textAlignCycler; final Runnable anchorCycler;
            boolean fgSupported = true;
            switch (focused) { 
                case HEALTH_BAR:
                    bgGetter = config.enableHealthBackground::get; fgGetter = config.enableHealthForeground::get; fadeGetter = config.fadeHealthWhenFull::get;
                    textGetter = config.showHealthText::get; textAlignGetter = config.healthTextAlign::get; anchorGetter = config.healthBarAnchor::get;
                    bgToggler = () -> config.enableHealthBackground.set(!config.enableHealthBackground.get()); fgToggler = () -> config.enableHealthForeground.set(!config.enableHealthForeground.get());
                    fadeToggler = () -> config.fadeHealthWhenFull.set(!config.fadeHealthWhenFull.get());
                    textCycler = () -> { config.showHealthText.set(getNextTextBehavior(config.showHealthText.get())); rebuildEditorWidgets(); }; 
                    textAlignCycler = () -> { config.healthTextAlign.set(getNextHorizontalAlignment(config.healthTextAlign.get())); rebuildEditorWidgets(); }; 
                    anchorCycler = () -> {
                        HUDPositioning.BarPlacement nextAnchor = getNextBarPlacement(config.healthBarAnchor.get()); config.healthBarAnchor.set(nextAnchor);
                        int bgWidth = config.healthBackgroundWidth.get(); int newDefaultXOffset = 0;
                        if (nextAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultXOffset = -bgWidth / 2; } else if (nextAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultXOffset = -bgWidth; }
                        config.healthTotalXOffset.set(newDefaultXOffset); config.healthTotalYOffset.set(0); rebuildEditorWidgets(); 
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

            int currentX = gridStartX;
            int currentY = focusStartY;

            toggleBackgroundButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.background_toggle_format", boolToOnOff(bgGetter.get())), (b) -> { bgToggler.run(); b.setMessage(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.background_toggle_format", boolToOnOff(bgGetter.get()))); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(toggleBackgroundButton); currentX += focusButtonWidth + focusColSpacing;
            if (fgSupported) { toggleForegroundButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.foreground_toggle_format", boolToOnOff(fgGetter.get())), (b) -> { fgToggler.run(); b.setMessage(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.foreground_toggle_format", boolToOnOff(fgGetter.get()))); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(toggleForegroundButton); } else {  } currentX += focusButtonWidth + focusColSpacing;
            toggleFadeFullButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.fade_when_full_toggle_format", boolToOnOff(fadeGetter.get())), (b) -> { fadeToggler.run(); b.setMessage(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.fade_when_full_toggle_format", boolToOnOff(fadeGetter.get()))); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(toggleFadeFullButton);
            
            currentX = gridStartX; 
            currentY += focusButtonHeight + focusRowSpacing;
            cycleTextBehaviorButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.show_text_cycle_format", textGetter.get().name()), (b) -> { textCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(cycleTextBehaviorButton); currentX += focusButtonWidth + focusColSpacing;
            cycleTextAlignButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.text_align_cycle_format", textAlignGetter.get().name()), (b) -> { textAlignCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(cycleTextAlignButton); currentX += focusButtonWidth + focusColSpacing;
            cycleAnchorButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.anchor_cycle_format", anchorGetter.get().name()), (b) -> { anchorCycler.run(); }).bounds(currentX, currentY, focusButtonWidth, focusButtonHeight).build(); addRenderableWidget(cycleAnchorButton);

            int metaRowY = currentY + focusButtonHeight + focusRowSpacing + 10; 
            int metaButtonWidth = focusButtonWidth; 
            int metaTotalWidth = 3 * metaButtonWidth + 2 * focusColSpacing;
            int metaStartX = (this.width - metaTotalWidth) / 2; 

            final DraggableElement finalFocusedForReset = focused; 
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
            
            final DraggableElement finalFocusedForResize = focused; 
            resizeButton = Button.builder(Component.translatable("gui.dynamic_resource_bars.hud_editor.button.resize"), (b) -> {
                    this.minecraft.setScreen(new ResizeElementScreen(this, finalFocusedForResize));
                 })
                 .bounds(metaStartX + 2 * (metaButtonWidth + focusColSpacing), metaRowY, metaButtonWidth, focusButtonHeight).build();
             addRenderableWidget(resizeButton);
        }
    }

    private TextBehavior getNextTextBehavior(TextBehavior current) {
        TextBehavior[] behaviors = TextBehavior.values();
        int nextOrdinal = (current.ordinal() + 1) % behaviors.length;
        return behaviors[nextOrdinal];
    }

    private HorizontalAlignment getNextHorizontalAlignment(HorizontalAlignment current) {
        HorizontalAlignment[] alignments = HorizontalAlignment.values();
        int nextOrdinal = (current.ordinal() + 1) % alignments.length;
        return alignments[nextOrdinal];
    }

    private HUDPositioning.BarPlacement getNextBarPlacement(HUDPositioning.BarPlacement current) {
        HUDPositioning.BarPlacement[] placements = HUDPositioning.BarPlacement.values();
        int nextOrdinal = (current.ordinal() + 1) % placements.length;
        return placements[nextOrdinal];
    }

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
        int lineSpacing = 2;
        int headerY = (focused == null) ? helpTextY + fontHeight * 2 + lineSpacing + 5 : 15;
        int padding = 5;
        int backgroundColor = 0x90000000; 

        float minX = Float.MAX_VALUE; float minY = Float.MAX_VALUE; 
        float maxX = Float.MIN_VALUE; float maxY = Float.MIN_VALUE;
        boolean foundElement = false;

        Component helpTextLine1 = null, helpTextLine2 = null, headerText = null;

        if (focused == null) {
            helpTextLine1 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line1");
            helpTextLine2 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line2");
            int helpTextWidth1 = font.width(helpTextLine1);
            int helpTextWidth2 = font.width(helpTextLine2);
            float helpTextX1_L1 = (this.width / 2.0f) - (helpTextWidth1 / 2.0f);
            float helpTextX2_L1 = helpTextX1_L1 + helpTextWidth1;
            float helpTextX1_L2 = (this.width / 2.0f) - (helpTextWidth2 / 2.0f);
            float helpTextX2_L2 = helpTextX1_L2 + helpTextWidth2;

            minX = Math.min(minX, Math.min(helpTextX1_L1, helpTextX1_L2));
            minY = Math.min(minY, helpTextY);
            maxX = Math.max(maxX, Math.max(helpTextX2_L1, helpTextX2_L2));
            maxY = Math.max(maxY, helpTextY + fontHeight * 2 + lineSpacing);
            foundElement = true;
        } else {
            helpTextLine1 = Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus");
            int helpTextWidth = font.width(helpTextLine1);
            float helpTextX1 = (this.width / 2.0f) - (helpTextWidth / 2.0f);
            float helpTextX2 = helpTextX1 + helpTextWidth;
            minX = Math.min(minX, helpTextX1); minY = Math.min(minY, helpTextY);
            maxX = Math.max(maxX, helpTextX2); maxY = Math.max(maxY, helpTextY + fontHeight);
            foundElement = true;

            headerText = Component.translatable("gui.dynamic_resource_bars.hud_editor.header.settings_format", getFriendlyElementName(focused)); 
            int headerWidth = font.width(headerText);
            float headerTextX1 = (this.width / 2.0f) - (headerWidth / 2.0f);
            float headerTextX2 = headerTextX1 + headerWidth;
            headerY = helpTextY + fontHeight + lineSpacing + 5;
            minX = Math.min(minX, headerTextX1);
            minY = Math.min(minY, headerY);
            maxX = Math.max(maxX, headerTextX2);
            maxY = Math.max(maxY, headerY + fontHeight);
        }

        for (Renderable renderable : this.renderables) { 
            if (renderable instanceof AbstractWidget widget) { 
                 minX = Math.min(minX, widget.getX());
                 minY = Math.min(minY, widget.getY());
                 maxX = Math.max(maxX, widget.getX() + widget.getWidth());
                 maxY = Math.max(maxY, widget.getY() + widget.getHeight());
                 foundElement = true;
            }
        }
        
        if (foundElement) {
            if (minX <= maxX && minY <= maxY) {
                graphics.fill((int)(minX - padding), (int)(minY - padding), 
                              (int)(maxX + padding), (int)(maxY + padding), backgroundColor);
            }
        }

        if (helpTextLine1 != null) {
            graphics.drawCenteredString(font, helpTextLine1, this.width / 2, helpTextY, 0xFFFFFF);
            if (helpTextLine2 != null) {
                graphics.drawCenteredString(font, helpTextLine2, this.width / 2, helpTextY + fontHeight + lineSpacing, 0xFFFFFF);
            }
        }
        
        if (headerText != null) {
            graphics.drawCenteredString(font, headerText, this.width / 2, headerY, 0xFFFFFF);
        }

        if (focused == null && dragged != null && player != null) {
            ScreenRect barRect = null; HUDPositioning.BarPlacement currentAnchor = null; 
            CClient config = ModConfigManager.getClient();
            switch (dragged) { 
                case HEALTH_BAR: barRect = HealthBarRenderer.getScreenRect(player); currentAnchor = config.healthBarAnchor.get(); break; 
                case MANA_BAR: barRect = ManaBarRenderer.getScreenRect(player); currentAnchor = config.manaBarAnchor.get(); break; 
                case STAMINA_BAR: barRect = StaminaBarRenderer.getScreenRect(player); currentAnchor = config.staminaBarAnchor.get(); break; 
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
                SubElementType clickedSub = getClickedSubElement(EditModeManager.getFocusedElement(), mouseX, mouseY, player);
                if (clickedSub != null && clickedSub != SubElementType.BACKGROUND) {
                    int currentSubX = 0; int currentSubY = 0;
                    CClient currentConfig = ModConfigManager.getClient();
                    switch (EditModeManager.getFocusedElement()) { 
                         case HEALTH_BAR: if (clickedSub == SubElementType.BAR_MAIN) { currentSubX = currentConfig.healthBarXOffset.get(); currentSubY = currentConfig.healthBarYOffset.get(); } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { currentSubX = currentConfig.healthOverlayXOffset.get(); currentSubY = currentConfig.healthOverlayYOffset.get(); } break;
                         case MANA_BAR: if (clickedSub == SubElementType.BAR_MAIN) { currentSubX = currentConfig.manaBarXOffset.get(); currentSubY = currentConfig.manaBarYOffset.get(); } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { currentSubX = currentConfig.manaOverlayXOffset.get(); currentSubY = currentConfig.manaOverlayYOffset.get(); } break;
                         case STAMINA_BAR: if (clickedSub == SubElementType.BAR_MAIN) { currentSubX = currentConfig.staminaBarXOffset.get(); currentSubY = currentConfig.staminaBarYOffset.get(); } else if (clickedSub == SubElementType.FOREGROUND_DETAIL) { currentSubX = currentConfig.staminaOverlayXOffset.get(); currentSubY = currentConfig.staminaOverlayYOffset.get(); } break;
                    }
                    EditModeManager.setDraggedSubElement(clickedSub, (int)mouseX, (int)mouseY, currentSubX, currentSubY);
                    actionTaken = true;
                } else {
                    actionTaken = true; 
                }
            } else {
                DraggableElement clickedBarForDrag = getClickedBarComplex(mouseX, mouseY, player);
                 if (clickedBarForDrag != null) {
                    int totalX = 0; int totalY = 0;
                    CClient currentConfig = ModConfigManager.getClient();
                     switch (clickedBarForDrag) {
                        case HEALTH_BAR: totalX = currentConfig.healthTotalXOffset.get(); totalY = currentConfig.healthTotalYOffset.get(); break;
                        case MANA_BAR: totalX = currentConfig.manaTotalXOffset.get(); totalY = currentConfig.manaTotalYOffset.get(); break;
                        case STAMINA_BAR: totalX = currentConfig.staminaTotalXOffset.get(); totalY = currentConfig.staminaTotalYOffset.get(); break;
                    }
                    EditModeManager.setDraggedElement(clickedBarForDrag, (int) mouseX, (int) mouseY, totalX, totalY);
                    actionTaken = true;
                 }
            }
        }

        if (!isDoubleClick) {
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
        return null;
    }

    private SubElementType getClickedSubElement(DraggableElement focusedBar, double mouseX, double mouseY, Player player) {
        if (focusedBar == null) return null;

        ScreenRect barMainRect = null;
        ScreenRect barFgRect = null;
        CClient currentConfig = ModConfigManager.getClient();

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
        CClient currentConfig = ModConfigManager.getClient();

        if (EditModeManager.getDraggedSubElement() != null && EditModeManager.getFocusedElement() != null) {
            int deltaX = (int) (mouseX - EditModeManager.getSubElementDragStartX());
            int deltaY = (int) (mouseY - EditModeManager.getSubElementDragStartY());
            int newSubX = EditModeManager.getInitialSubElementXOffset() + deltaX;
            int newSubY = EditModeManager.getInitialSubElementYOffset() + deltaY;

            DraggableElement focused = EditModeManager.getFocusedElement();
            SubElementType sub = EditModeManager.getDraggedSubElement();

            switch (focused) {
                case HEALTH_BAR:
                    if (sub == SubElementType.BAR_MAIN) { 
                        currentConfig.healthBarXOffset.set(newSubX); 
                        currentConfig.healthBarYOffset.set(newSubY); 
                    } else if (sub == SubElementType.FOREGROUND_DETAIL) { 
                        currentConfig.healthOverlayXOffset.set(newSubX); 
                        currentConfig.healthOverlayYOffset.set(newSubY); 
                    }
                    break;
                case MANA_BAR:
                     if (sub == SubElementType.BAR_MAIN) { 
                        currentConfig.manaBarXOffset.set(newSubX); 
                        currentConfig.manaBarYOffset.set(newSubY); 
                     } else if (sub == SubElementType.FOREGROUND_DETAIL) { 
                        currentConfig.manaOverlayXOffset.set(newSubX); 
                        currentConfig.manaOverlayYOffset.set(newSubY); 
                     }
                    break;
                case STAMINA_BAR:
                     if (sub == SubElementType.BAR_MAIN) { 
                        currentConfig.staminaBarXOffset.set(newSubX); 
                        currentConfig.staminaBarYOffset.set(newSubY); 
                     } else if (sub == SubElementType.FOREGROUND_DETAIL) { 
                        currentConfig.staminaOverlayXOffset.set(newSubX); 
                        currentConfig.staminaOverlayYOffset.set(newSubY); 
                     }
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
            }
            return true; 
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
            EditModeManager.toggleEditMode(); 
        }
        if (EditModeManager.getFocusedElement() != null) { 
            EditModeManager.clearFocusedElement();
        }
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
        CClient config = ModConfigManager.getClient();
        if (element == null) return;

        HUDPositioning.BarPlacement defaultAnchor = null;
        int defaultBarX = 0, defaultBarY = 0, defaultOverlayX = 0, defaultOverlayY = 0, defaultTotalY = 0;
        int defaultBgWidthForCalc = 0; 

        switch (element) {
            case HEALTH_BAR: 
                defaultAnchor = CClient.DEFAULT_HEALTH_BAR_ANCHOR;
                defaultBgWidthForCalc = CClient.DEFAULT_HEALTH_BACKGROUND_WIDTH;
                defaultBarX = CClient.DEFAULT_HEALTH_BAR_X_OFFSET; defaultBarY = CClient.DEFAULT_HEALTH_BAR_Y_OFFSET;
                defaultOverlayX = CClient.DEFAULT_HEALTH_OVERLAY_X_OFFSET; defaultOverlayY = CClient.DEFAULT_HEALTH_OVERLAY_Y_OFFSET;
                defaultTotalY = CClient.DEFAULT_HEALTH_TOTAL_Y_OFFSET;
                config.healthBarAnchor.set(defaultAnchor);
                config.healthBarXOffset.set(defaultBarX); config.healthBarYOffset.set(defaultBarY);
                config.healthOverlayXOffset.set(defaultOverlayX); config.healthOverlayYOffset.set(defaultOverlayY);
                config.healthTotalYOffset.set(defaultTotalY);
                break;
            case STAMINA_BAR:
                defaultAnchor = CClient.DEFAULT_STAMINA_BAR_ANCHOR;
                defaultBgWidthForCalc = CClient.DEFAULT_STAMINA_BACKGROUND_WIDTH;
                defaultBarX = CClient.DEFAULT_STAMINA_BAR_X_OFFSET; defaultBarY = CClient.DEFAULT_STAMINA_BAR_Y_OFFSET;
                defaultOverlayX = CClient.DEFAULT_STAMINA_OVERLAY_X_OFFSET; defaultOverlayY = CClient.DEFAULT_STAMINA_OVERLAY_Y_OFFSET;
                defaultTotalY = CClient.DEFAULT_STAMINA_TOTAL_Y_OFFSET;
                config.staminaBarAnchor.set(defaultAnchor);
                config.staminaBarXOffset.set(defaultBarX); config.staminaBarYOffset.set(defaultBarY);
                config.staminaOverlayXOffset.set(defaultOverlayX); config.staminaOverlayYOffset.set(defaultOverlayY);
                config.staminaTotalYOffset.set(defaultTotalY);
                break;
            case MANA_BAR:
                defaultAnchor = CClient.DEFAULT_MANA_BAR_ANCHOR;
                defaultBgWidthForCalc = CClient.DEFAULT_MANA_BACKGROUND_WIDTH;
                defaultBarX = CClient.DEFAULT_MANA_BAR_X_OFFSET; defaultBarY = CClient.DEFAULT_MANA_BAR_Y_OFFSET;
                defaultOverlayX = CClient.DEFAULT_MANA_OVERLAY_X_OFFSET; defaultOverlayY = CClient.DEFAULT_MANA_OVERLAY_Y_OFFSET;
                defaultTotalY = CClient.DEFAULT_MANA_TOTAL_Y_OFFSET;
                config.manaBarAnchor.set(defaultAnchor);
                config.manaBarXOffset.set(defaultBarX); config.manaBarYOffset.set(defaultBarY);
                config.manaOverlayXOffset.set(defaultOverlayX); config.manaOverlayYOffset.set(defaultOverlayY);
                config.manaTotalYOffset.set(defaultTotalY);
                break;
             default: return; // Should not happen
        }

        int newDefaultTotalX = 0;
        if (defaultAnchor == HUDPositioning.BarPlacement.ABOVE_UTILITIES) { newDefaultTotalX = -defaultBgWidthForCalc / 2; }
        else if (defaultAnchor != null && defaultAnchor.getSide() == HUDPositioning.AnchorSide.RIGHT) { newDefaultTotalX = -defaultBgWidthForCalc; }
        
        // Set the calculated default TotalX
        switch(element) {
            case HEALTH_BAR: config.healthTotalXOffset.set(newDefaultTotalX); break;
            case STAMINA_BAR: config.staminaTotalXOffset.set(newDefaultTotalX); break;
            case MANA_BAR: config.manaTotalXOffset.set(newDefaultTotalX); break;
        }
        rebuildEditorWidgets(); 
    }

    private void resetSizeDefaultsAction(DraggableElement element) {
        CClient config = ModConfigManager.getClient();
        if (element == null) return;

        switch(element) {
            case HEALTH_BAR: 
                config.healthBackgroundWidth.set(CClient.DEFAULT_HEALTH_BACKGROUND_WIDTH); config.healthBackgroundHeight.set(CClient.DEFAULT_HEALTH_BACKGROUND_HEIGHT); 
                config.healthBarWidth.set(CClient.DEFAULT_HEALTH_BAR_WIDTH); config.healthBarHeight.set(CClient.DEFAULT_HEALTH_BAR_HEIGHT); 
                config.healthOverlayWidth.set(CClient.DEFAULT_HEALTH_OVERLAY_WIDTH); config.healthOverlayHeight.set(CClient.DEFAULT_HEALTH_OVERLAY_HEIGHT);
                break;
            case STAMINA_BAR: 
                config.staminaBackgroundWidth.set(CClient.DEFAULT_STAMINA_BACKGROUND_WIDTH); config.staminaBackgroundHeight.set(CClient.DEFAULT_STAMINA_BACKGROUND_HEIGHT); 
                config.staminaBarWidth.set(CClient.DEFAULT_STAMINA_BAR_WIDTH); config.staminaBarHeight.set(CClient.DEFAULT_STAMINA_BAR_HEIGHT); 
                config.staminaOverlayWidth.set(CClient.DEFAULT_STAMINA_OVERLAY_WIDTH); config.staminaOverlayHeight.set(CClient.DEFAULT_STAMINA_OVERLAY_HEIGHT);
                break;
            case MANA_BAR: 
                config.manaBackgroundWidth.set(CClient.DEFAULT_MANA_BACKGROUND_WIDTH); config.manaBackgroundHeight.set(CClient.DEFAULT_MANA_BACKGROUND_HEIGHT); 
                config.manaBarWidth.set(CClient.DEFAULT_MANA_BAR_WIDTH); config.manaBarHeight.set(CClient.DEFAULT_MANA_BAR_HEIGHT); 
                config.manaOverlayWidth.set(CClient.DEFAULT_MANA_OVERLAY_WIDTH); config.manaOverlayHeight.set(CClient.DEFAULT_MANA_OVERLAY_HEIGHT);
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
        rebuildEditorWidgets(); 
    }

    private void openConfirmScreen(Component title, Component explanation, Runnable confirmAction) {
        this.minecraft.setScreen(new ConfirmResetScreen(this, title, explanation, confirmAction));
    }

    private String getFriendlyElementName(DraggableElement element) {
        if (element == null) return "";
        return switch (element) {
            case HEALTH_BAR -> Component.translatable("gui.dynamic_resource_bars.element.health_bar").getString();
            case MANA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.mana_bar").getString();
            case STAMINA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.stamina_bar").getString();
            default -> element.name();
        };
    }
} 