package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.compat.ManaProviderManager;
import dev.muon.dynamic_resource_bars.compat.StaminaProviderManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import dev.muon.dynamic_resource_bars.render.AbstractBarRenderer;
import dev.muon.dynamic_resource_bars.render.CustomHudRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.AbsorptionDisplayMode;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.BarVisibility;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.EditModeManager;
import dev.muon.dynamic_resource_bars.util.FillDirection;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.HorizontalAlignment;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import dev.muon.dynamic_resource_bars.util.TextBehavior;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class HudEditorScreen extends Screen {

    private static final int OVERLAY_DIM_COLOR = 0x70000000;
    private static final int PANEL_BG_COLOR = 0xE8101318;
    private static final int PANEL_BORDER_COLOR = 0xFF3E4856;
    private static final int CHIP_BG_COLOR = 0xFF1E242E;
    private static final int CHIP_HOVER_COLOR = 0xFF2C3848;
    private static final int CHIP_ACTIVE_BG_COLOR = 0xFF284B7A;
    private static final int CHIP_ACTIVE_BORDER_COLOR = 0xFF6CA8FF;
    private static final int CHIP_BORDER_COLOR = 0xFF3A4250;
    private static final int FOCUS_OUTLINE_COLOR = 0xFFFFD84D;
    private static final int FOCUS_INACTIVE_OUTLINE_COLOR = 0x70FFFFFF;
    private static final int FOCUS_HOVER_COLOR = 0x38FFD84D;
    private static final int RESIZE_HANDLE_COLOR = 0xFFFFAA00;
    private static final int RESIZE_HANDLE_HOT_COLOR = 0xFFFFFFFF;
    private static final int SCALE_HANDLE_COLOR = 0xFF55DDFF;
    private static final int HANDLE_DRAW_SIZE = 4;
    private static final int HANDLE_HIT_RADIUS = 4;
    private static final int NUDGE_PIXELS = 1;
    private static final int NUDGE_PIXELS_FAST = 5;
    private static final float SCALE_STEP = 0.05f;
    private static final int MAX_UNDO_STEPS = 40;

    private static final SubElementType[] FOCUS_SUB_ELEMENTS = {
            SubElementType.BACKGROUND, SubElementType.BAR_MAIN,
            SubElementType.FOREGROUND, SubElementType.TEXT,
            SubElementType.ICON, SubElementType.ABSORPTION_TEXT
    };

    private final Screen previousScreen;
    private final List<BarHandle> bars = new ArrayList<>();
    private final List<ChipButton> chipButtons = new ArrayList<>();
    private final Deque<Runnable> undoStack = new ArrayDeque<>();

    private SubElementType activeSub;
    private DragKind dragKind = DragKind.NONE;
    private int dragStartMouseX, dragStartMouseY;
    private int dragStartValueA, dragStartValueB;
    private float dragStartScale;

    private ContextMenu activeContextMenu;

    private enum DragKind { NONE, MOVE_BAR, MOVE_SUB, RESIZE_W, RESIZE_H, RESIZE_CORNER, RESIZE_TEXT_SCALE }

    private record ChipButton(int x, int y, int width, int height, Component label,
                              boolean selected, boolean accent, Runnable action, Component tooltip) {
        boolean contains(int mx, int my) {
            return mx >= x && mx < x + width && my >= y && my < y + height;
        }
    }

    public HudEditorScreen(Screen previous) {
        super(Component.translatable("gui.dynamic_resource_bars.hud_editor.title_main"));
        this.previousScreen = previous;
    }

    @Override
    protected void init() {
        super.init();
        bars.clear();
        bars.add(new BarHandle(DraggableElement.HEALTH_BAR, HealthBarRenderer.INSTANCE, BarFieldAccess.HEALTH));
        bars.add(new BarHandle(DraggableElement.MANA_BAR, ManaBarRenderer.INSTANCE, BarFieldAccess.MANA));
        bars.add(new BarHandle(DraggableElement.STAMINA_BAR, StaminaBarRenderer.INSTANCE, BarFieldAccess.STAMINA));
        bars.add(new BarHandle(DraggableElement.ARMOR_BAR,
                dev.muon.dynamic_resource_bars.render.ArmorBarRenderer.INSTANCE, BarFieldAccess.ARMOR));
        bars.add(new BarHandle(DraggableElement.AIR_BAR,
                dev.muon.dynamic_resource_bars.render.AirBarRenderer.INSTANCE, BarFieldAccess.AIR));
        if (!EditModeManager.isEditModeEnabled()) EditModeManager.toggleEditMode();
        if (EditModeManager.getFocusedElement() == null && !bars.isEmpty()) {
            EditModeManager.setFocusedElement(DraggableElement.HEALTH_BAR);
        }
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.fill(0, 0, this.width, this.height, OVERLAY_DIM_COLOR);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

        Player player = player();
        if (player != null) {
            DeltaTracker dt = Minecraft.getInstance().getDeltaTracker();
            if (CustomHudRenderer.shouldReplaceHotbar()) {
                CustomHudRenderer.renderHotbar(graphics, dt);
            }
            if (CustomHudRenderer.shouldReplaceInfoBar()) {
                CustomHudRenderer.renderInfoBar(graphics, dt);
            }
            if (CustomHudRenderer.shouldReplaceExperienceLevel()) {
                CustomHudRenderer.renderExperienceLevel(graphics, dt);
            }
            for (BarHandle bar : bars) bar.renderer.render(graphics, player, dt);
        }

        rebuildAndRenderToolbar(graphics, mouseX, mouseY);
        renderFocusModeOverlay(graphics, mouseX, mouseY);

        if (activeContextMenu != null && activeContextMenu.isVisible()) {
            activeContextMenu.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }
    }

    private void rebuildAndRenderToolbar(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        chipButtons.clear();
        BarHandle focused = focusedBar();
        ClientConfig c = ModConfigManager.getClient();
        boolean inFocus = EditModeManager.isFocusMode() && focused != null;

        int panelW = Math.min(this.width - 12, 412);
        int panelX = (this.width - panelW) / 2;
        int panelY = 4;
        int panelH = 54;

        graphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, PANEL_BG_COLOR);
        graphics.outline(panelX, panelY, panelW, panelH, PANEL_BORDER_COLOR);

        // Row 1: Mode / Selector chips (y = panelY + 4, height = 14)
        int row1Y = panelY + 4;
        int chipH = 14;
        if (!inFocus) {
            int x = panelX + 6;
            for (BarHandle bar : bars) {
                Component label = shortBarLabel(bar.element);
                int w = this.font.width(label) + 10;
                boolean sel = focused != null && focused.element == bar.element;
                DraggableElement target = bar.element;
                chipButtons.add(new ChipButton(x, row1Y, w, chipH, label, sel, false, () -> {
                    closeActiveContextMenu();
                    EditModeManager.setFocusedElement(target);
                }, elementName(bar.element)));
                x += w + 3;
            }
        } else {
            int x = panelX + 6;
            Component backLabel = Component.translatable("gui.dynamic_resource_bars.hud_editor.chip.back_bars");
            int backW = this.font.width(backLabel) + 8;
            chipButtons.add(new ChipButton(x, row1Y, backW, chipH, backLabel, false, true, () -> {
                closeActiveContextMenu();
                exitFocus();
            }, Component.translatable("gui.dynamic_resource_bars.context.exit_sub_elements")));
            x += backW + 5;

            Player p = player();
            for (SubElementType sub : FOCUS_SUB_ELEMENTS) {
                if (!barExposesSub(focused, sub, p)) continue;
                Component label = shortSubLabel(sub);
                int w = this.font.width(label) + 8;
                boolean sel = activeSub == sub;
                SubElementType targetSub = sub;
                chipButtons.add(new ChipButton(x, row1Y, w, chipH, label, sel, false, () -> {
                    closeActiveContextMenu();
                    setActiveSub(targetSub);
                }, Component.translatable(sub.getTranslationKey())));
                x += w + 3;
            }
        }

        // Row 2: Live readout + Quick actions (y = panelY + 21, height = 14)
        int row2Y = panelY + 21;
        String readout = buildInspectorReadout(focused, c, inFocus);
        graphics.text(this.font, Component.literal(readout), panelX + 6, row2Y + 3, 0xFFE6EDF5, true);

        int rightX = panelX + panelW - 6;
        Component doneLabel = Component.translatable("gui.done");
        int doneW = this.font.width(doneLabel) + 10;
        rightX -= doneW;
        chipButtons.add(new ChipButton(rightX, row2Y, doneW, chipH, doneLabel, false, true, this::onClose, null));

        if (!undoStack.isEmpty()) {
            Component undoLabel = Component.translatable("gui.dynamic_resource_bars.hud_editor.chip.undo");
            int undoW = this.font.width(undoLabel) + 8;
            rightX -= undoW + 3;
            chipButtons.add(new ChipButton(rightX, row2Y, undoW, chipH, undoLabel, false, false, this::performUndo, null));
        }

        if (focused != null) {
            Component optionsLabel = Component.translatable("gui.dynamic_resource_bars.hud_editor.chip.options");
            int optW = this.font.width(optionsLabel) + 8;
            rightX -= optW + 3;
            int menuAnchorX = rightX;
            int menuAnchorY = row2Y + chipH + 2;
            chipButtons.add(new ChipButton(rightX, row2Y, optW, chipH, optionsLabel, false, false, () -> {
                if (inFocus && activeSub != null) {
                    openSubContextMenu(focused, activeSub, menuAnchorX, menuAnchorY);
                } else {
                    openBarContextMenu(focused, menuAnchorX, menuAnchorY);
                }
            }, null));

            Component valuesLabel = Component.translatable("gui.dynamic_resource_bars.hud_editor.chip.values");
            int valW = this.font.width(valuesLabel) + 8;
            rightX -= valW + 3;
            chipButtons.add(new ChipButton(rightX, row2Y, valW, chipH, valuesLabel, false, false, () -> {
                closeActiveContextMenu();
                if (inFocus && activeSub != null) {
                    Minecraft.getInstance().setScreen(new SubElementConfigScreen(this, focused.element, activeSub));
                } else {
                    Minecraft.getInstance().setScreen(new ManualSizePositionScreen(this, focused.element));
                }
            }, null));

            if (!inFocus) {
                Component layersLabel = Component.translatable("gui.dynamic_resource_bars.hud_editor.chip.edit_layers");
                int layW = this.font.width(layersLabel) + 8;
                rightX -= layW + 3;
                chipButtons.add(new ChipButton(rightX, row2Y, layW, chipH, layersLabel, false, true, () -> {
                    closeActiveContextMenu();
                    enterFocus();
                }, null));
            } else if (activeSub != null && focused.access.hasTextScale(activeSub)) {
                Component plusLabel = Component.literal("A+");
                int plusW = this.font.width(plusLabel) + 6;
                rightX -= plusW + 2;
                chipButtons.add(new ChipButton(rightX, row2Y, plusW, chipH, plusLabel, false, false,
                        () -> adjustActiveTextScale(focused, SCALE_STEP),
                        Component.translatable("gui.dynamic_resource_bars.hud_editor.tooltip.scale_up")));

                Component minusLabel = Component.literal("A-");
                int minusW = this.font.width(minusLabel) + 6;
                rightX -= minusW + 2;
                chipButtons.add(new ChipButton(rightX, row2Y, minusW, chipH, minusLabel, false, false,
                        () -> adjustActiveTextScale(focused, -SCALE_STEP),
                        Component.translatable("gui.dynamic_resource_bars.hud_editor.tooltip.scale_down")));
            }
        }

        // Render all chips
        ChipButton hoveredChip = null;
        for (ChipButton chip : chipButtons) {
            boolean hot = (activeContextMenu == null || !activeContextMenu.isVisible()) && chip.contains(mouseX, mouseY);
            if (hot) hoveredChip = chip;
            int bg = chip.selected ? CHIP_ACTIVE_BG_COLOR : (hot ? CHIP_HOVER_COLOR : CHIP_BG_COLOR);
            int border = chip.selected ? CHIP_ACTIVE_BORDER_COLOR : (chip.accent ? 0xFF4F74A8 : CHIP_BORDER_COLOR);
            graphics.fill(chip.x, chip.y, chip.x + chip.width, chip.y + chip.height, bg);
            graphics.outline(chip.x, chip.y, chip.width, chip.height, border);
            int textX = chip.x + (chip.width - this.font.width(chip.label)) / 2;
            int textY = chip.y + (chip.height - this.font.lineHeight) / 2 + 1;
            int textColor = chip.selected ? 0xFFFFFFFF : (chip.accent ? 0xFFD8E8FF : 0xFFD0D6E0);
            graphics.text(this.font, chip.label, textX, textY, textColor, false);
        }

        // Row 3: Concise accurate shortcut hint
        int row3Y = panelY + 39;
        Component hint = inFocus
                ? ((activeSub != null && focused.access.hasTextScale(activeSub))
                    ? Component.translatable("gui.dynamic_resource_bars.hud_editor.hint.focus_text")
                    : Component.translatable("gui.dynamic_resource_bars.hud_editor.hint.focus"))
                : Component.translatable("gui.dynamic_resource_bars.hud_editor.hint.bar");
        int hintW = this.font.width(hint);
        int maxHintW = panelW - 12;
        var pose = graphics.pose();
        pose.pushMatrix();
        if (hintW > maxHintW) {
            float scale = maxHintW / (float) hintW;
            pose.translate(this.width / 2f, row3Y + 2);
            pose.scale(scale, scale);
            graphics.text(this.font, hint, -hintW / 2, 0, 0xFFA8B4C4, false);
        } else {
            graphics.centeredText(this.font, hint, this.width / 2, row3Y, 0xFFA8B4C4);
        }
        pose.popMatrix();

        if (hoveredChip != null && hoveredChip.tooltip != null) {
            renderTooltip(graphics, hoveredChip.tooltip, mouseX, mouseY);
        }
    }

    private String buildInspectorReadout(BarHandle focused, ClientConfig c, boolean inFocus) {
        if (focused == null) {
            return Component.translatable("gui.dynamic_resource_bars.hud_editor.no_selection").getString();
        }
        if (!inFocus) {
            return String.format("%s  [X: %d, Y: %d]",
                    shortBarLabel(focused.element).getString(),
                    focused.access.totalX(c),
                    focused.access.totalY(c));
        }
        SubElementType sub = activeSub != null ? activeSub : SubElementType.BAR_MAIN;
        int x = focused.access.subElementX(c, sub);
        int y = focused.access.subElementY(c, sub);
        int w = focused.access.subElementWidth(c, sub);
        int h = focused.access.subElementHeight(c, sub);
        if (focused.access.hasTextScale(sub)) {
            float scale = focused.access.subElementTextScale(c, sub);
            return String.format("%s > %s  [X:%d Y:%d  %dx%d  %sx]",
                    shortBarLabel(focused.element).getString(),
                    shortSubLabel(sub).getString(),
                    x, y, w, h, FieldEditorUtils.formatScale(scale));
        }
        return String.format("%s > %s  [X: %d, Y: %d, %dx%d]",
                shortBarLabel(focused.element).getString(),
                shortSubLabel(sub).getString(),
                x, y, w, h);
    }

    private static Component shortBarLabel(DraggableElement e) {
        return switch (e) {
            case HEALTH_BAR -> Component.translatable("gui.dynamic_resource_bars.hud_editor.bar_short.health");
            case MANA_BAR -> Component.translatable("gui.dynamic_resource_bars.hud_editor.bar_short.mana");
            case STAMINA_BAR -> Component.translatable("gui.dynamic_resource_bars.hud_editor.bar_short.stamina");
            case ARMOR_BAR -> Component.translatable("gui.dynamic_resource_bars.hud_editor.bar_short.armor");
            case AIR_BAR -> Component.translatable("gui.dynamic_resource_bars.hud_editor.bar_short.air");
        };
    }

    private static Component shortSubLabel(SubElementType sub) {
        return switch (sub) {
            case BACKGROUND -> Component.translatable("gui.dynamic_resource_bars.hud_editor.sub_short.background");
            case BAR_MAIN -> Component.translatable("gui.dynamic_resource_bars.hud_editor.sub_short.bar_main");
            case FOREGROUND -> Component.translatable("gui.dynamic_resource_bars.hud_editor.sub_short.foreground");
            case TEXT -> Component.translatable("gui.dynamic_resource_bars.hud_editor.sub_short.text");
            case ICON -> Component.translatable("gui.dynamic_resource_bars.hud_editor.sub_short.icon");
            case ABSORPTION_TEXT -> Component.translatable("gui.dynamic_resource_bars.hud_editor.sub_short.absorption_text");
        };
    }

    private static boolean barExposesSub(BarHandle bar, SubElementType sub, Player player) {
        if (bar == null || player == null) return false;
        if (sub == SubElementType.ICON && !bar.access.hasIcon()) return false;
        if (sub == SubElementType.ABSORPTION_TEXT && !bar.access.hasAbsorptionText()) return false;
        ScreenRect r = bar.renderer.getSubElementRect(sub, player);
        return r.width() > 0 && r.height() > 0;
    }

    private void renderFocusModeOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!EditModeManager.isFocusMode()) return;
        BarHandle bar = focusedBar();
        if (bar == null) return;
        Player player = player();
        if (player == null) return;

        SubElementType hoveredSub = null;
        for (SubElementType sub : FOCUS_SUB_ELEMENTS) {
            if (!barExposesSub(bar, sub, player)) continue;
            ScreenRect r = bar.renderer.getSubElementRect(sub, player);
            if (sub != activeSub) {
                graphics.outline(r.x() - 1, r.y() - 1, r.width() + 2, r.height() + 2, FOCUS_INACTIVE_OUTLINE_COLOR);
            }
            if (isInside(mouseX, mouseY, r)) hoveredSub = sub;
        }

        if (activeSub != null && barExposesSub(bar, activeSub, player)) {
            ScreenRect r = bar.renderer.getSubElementRect(activeSub, player);
            if (isInside(mouseX, mouseY, r) && dragKind == DragKind.NONE) {
                graphics.fill(r.x(), r.y(), r.x() + r.width(), r.y() + r.height(), FOCUS_HOVER_COLOR);
            }
            graphics.outline(r.x() - 1, r.y() - 1, r.width() + 2, r.height() + 2, FOCUS_OUTLINE_COLOR);

            if (activeSub.isResizable()) {
                boolean hasScale = bar.access.hasTextScale(activeSub);
                boolean wHot = hitWidthHandle(mouseX, mouseY, r);
                boolean hHot = hitHeightHandle(mouseX, mouseY, r);
                boolean cHot = hitCornerHandle(mouseX, mouseY, r);

                drawHandle(graphics, widthHandleCenterX(r), widthHandleCenterY(r),
                        wHot ? RESIZE_HANDLE_HOT_COLOR : RESIZE_HANDLE_COLOR);
                drawHandle(graphics, heightHandleCenterX(r), heightHandleCenterY(r),
                        hHot ? RESIZE_HANDLE_HOT_COLOR : RESIZE_HANDLE_COLOR);
                drawHandle(graphics, cornerHandleCenterX(r), cornerHandleCenterY(r),
                        cHot ? RESIZE_HANDLE_HOT_COLOR : (hasScale ? SCALE_HANDLE_COLOR : RESIZE_HANDLE_COLOR));

                if (activeContextMenu == null || !activeContextMenu.isVisible()) {
                    if (cHot) {
                        String key = hasScale
                                ? "gui.dynamic_resource_bars.handle.text_scale"
                                : "gui.dynamic_resource_bars.handle.corner_size";
                        renderTooltip(graphics, Component.translatable(key,
                                Component.translatable(activeSub.getTranslationKey())), mouseX, mouseY);
                        return;
                    } else if (wHot) {
                        renderTooltip(graphics, Component.translatable("gui.dynamic_resource_bars.handle.horizontal_size",
                                Component.translatable(activeSub.getTranslationKey())), mouseX, mouseY);
                        return;
                    } else if (hHot) {
                        renderTooltip(graphics, Component.translatable("gui.dynamic_resource_bars.handle.vertical_size",
                                Component.translatable(activeSub.getTranslationKey())), mouseX, mouseY);
                        return;
                    }
                }
            }
        }

        if ((activeContextMenu == null || !activeContextMenu.isVisible()) && hoveredSub != null) {
            SubElementType target = (activeSub != null && isInside(mouseX, mouseY, bar.renderer.getSubElementRect(activeSub, player)))
                    ? activeSub
                    : hoveredSub;
            renderTooltip(graphics, Component.translatable(target.getTranslationKey()), mouseX, mouseY);
        }
    }

    private static void drawHandle(GuiGraphicsExtractor graphics, int cx, int cy, int fillColor) {
        int half = HANDLE_DRAW_SIZE / 2;
        graphics.fill(cx - half - 1, cy - half - 1, cx + half + 1, cy + half + 1, 0xFF000000);
        graphics.fill(cx - half, cy - half, cx + half, cy + half, fillColor);
    }

    private void renderTooltip(GuiGraphicsExtractor graphics, Component label, int mouseX, int mouseY) {
        int textWidth = this.font.width(label);
        int padding = 3;
        int boxWidth = textWidth + padding * 2;
        int boxHeight = this.font.lineHeight + padding * 2;
        int x = mouseX + 8;
        int y = mouseY - boxHeight - 4;
        if (x + boxWidth > this.width - 2) x = this.width - boxWidth - 2;
        if (y < 60) y = mouseY + 12;
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xF0101216);
        graphics.outline(x, y, boxWidth, boxHeight, 0xFF586578);
        graphics.text(this.font, label, x + padding, y + padding, 0xFFFFFFFF, false);
    }

    // ===== Mouse & Scroll =====

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && EditModeManager.isFocusMode()) {
            BarHandle bar = focusedBar();
            if (bar != null && activeSub != null && bar.access.hasTextScale(activeSub)) {
                float delta = scrollY > 0 ? SCALE_STEP : -SCALE_STEP;
                adjustActiveTextScale(bar, delta);
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        ContextMenu menu = activeContextMenu;
        if (menu != null && menu.isVisible()) {
            boolean consumed = menu.handleMouseClicked(event);
            if (!menu.isVisible()) activeContextMenu = null;
            if (consumed) return true;
        }

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int button = event.button();

        if (button == 0) {
            for (ChipButton chip : chipButtons) {
                if (chip.contains(mouseX, mouseY)) {
                    chip.action.run();
                    return true;
                }
            }
        }

        if (super.mouseClicked(event, doubleClick)) return true;

        if (EditModeManager.isFocusMode()) {
            BarHandle bar = focusedBar();
            if (bar == null) { exitFocus(); return true; }
            Player player = player();
            if (player == null) return true;

            if (button == 1) {
                SubElementType sub = subAt(bar, mouseX, mouseY);
                if (sub != null) {
                    setActiveSub(sub);
                    openSubContextMenu(bar, sub, mouseX, mouseY);
                } else {
                    openBarContextMenu(bar, mouseX, mouseY);
                }
                return true;
            }

            // Priority 1: handles on the currently active sub-element
            if (activeSub != null && activeSub.isResizable() && barExposesSub(bar, activeSub, player)) {
                ScreenRect activeRect = bar.renderer.getSubElementRect(activeSub, player);
                if (hitCornerHandle(mouseX, mouseY, activeRect)) {
                    if (bar.access.hasTextScale(activeSub)) {
                        startTextScaleDrag(bar, activeSub, mouseX, mouseY);
                    } else {
                        startResize(bar, activeSub, mouseX, mouseY, DragKind.RESIZE_CORNER);
                    }
                    return true;
                }
                if (hitWidthHandle(mouseX, mouseY, activeRect)) {
                    startResize(bar, activeSub, mouseX, mouseY, DragKind.RESIZE_W);
                    return true;
                }
                if (hitHeightHandle(mouseX, mouseY, activeRect)) {
                    startResize(bar, activeSub, mouseX, mouseY, DragKind.RESIZE_H);
                    return true;
                }
                if (isInside(mouseX, mouseY, activeRect)) {
                    startSubMove(bar, activeSub, mouseX, mouseY);
                    return true;
                }
            }

            // Priority 2: clicking another sub-element selects and moves it
            SubElementType clicked = subAt(bar, mouseX, mouseY);
            if (clicked != null) {
                setActiveSub(clicked);
                startSubMove(bar, clicked, mouseX, mouseY);
                return true;
            }

            if (button == 0) {
                exitFocus();
                return true;
            }
            return true;
        }

        BarHandle hit = hitTest(mouseX, mouseY);
        if (button == 1) {
            if (hit != null) openBarContextMenu(hit, mouseX, mouseY);
            else openGlobalContextMenu(mouseX, mouseY);
            return true;
        }
        if (button == 0) {
            if (hit == null) {
                EditModeManager.clearFocusedElement();
                return true;
            }
            if (doubleClick && EditModeManager.getFocusedElement() == hit.element) {
                enterFocus();
                return true;
            }
            EditModeManager.setFocusedElement(hit.element);
            startBarMove(hit, mouseX, mouseY);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dx, double dy) {
        if (dragKind == DragKind.NONE) return super.mouseDragged(event, dx, dy);
        BarHandle bar = focusedBar();
        if (bar == null) return false;
        ClientConfig c = ModConfigManager.getClient();
        int dxInt = (int) (event.x() - dragStartMouseX);
        int dyInt = (int) (event.y() - dragStartMouseY);
        switch (dragKind) {
            case MOVE_BAR -> {
                bar.access.setTotalX(c, dragStartValueA + dxInt);
                bar.access.setTotalY(c, dragStartValueB + dyInt);
            }
            case MOVE_SUB -> {
                if (activeSub == null) break;
                bar.access.setSubElementX(c, activeSub, dragStartValueA + dxInt);
                bar.access.setSubElementY(c, activeSub, dragStartValueB + dyInt);
            }
            case RESIZE_W -> {
                if (activeSub == null) break;
                bar.access.setSubElementWidth(c, activeSub, Math.max(1, dragStartValueA + dxInt));
            }
            case RESIZE_H -> {
                if (activeSub == null) break;
                bar.access.setSubElementHeight(c, activeSub, Math.max(1, dragStartValueB + dyInt));
            }
            case RESIZE_CORNER -> {
                if (activeSub == null) break;
                bar.access.setSubElementWidth(c, activeSub, Math.max(1, dragStartValueA + dxInt));
                bar.access.setSubElementHeight(c, activeSub, Math.max(1, dragStartValueB + dyInt));
            }
            case RESIZE_TEXT_SCALE -> {
                if (activeSub == null) break;
                int deltaPx = Math.abs(dxInt) >= Math.abs(dyInt) ? dxInt : dyInt;
                float newScale = dragStartScale + (deltaPx * 0.02f);
                bar.access.setSubElementTextScale(c, activeSub, newScale);
            }
            default -> {}
        }
        return true;
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (dragKind != DragKind.NONE && event.button() == 0) {
            BarHandle bar = focusedBar();
            ClientConfig c = ModConfigManager.getClient();
            if (bar != null) {
                DraggableElement el = bar.element;
                SubElementType sub = activeSub;
                int startA = dragStartValueA;
                int startB = dragStartValueB;
                float startS = dragStartScale;
                switch (dragKind) {
                    case MOVE_BAR -> {
                        if (bar.access.totalX(c) != startA || bar.access.totalY(c) != startB) {
                            pushUndo(() -> {
                                BarFieldAccess acc = BarFieldAccess.forElement(el);
                                ClientConfig cfg = ModConfigManager.getClient();
                                acc.setTotalX(cfg, startA);
                                acc.setTotalY(cfg, startB);
                                cfg.save();
                            });
                        }
                    }
                    case MOVE_SUB -> {
                        if (sub != null && (bar.access.subElementX(c, sub) != startA || bar.access.subElementY(c, sub) != startB)) {
                            pushUndo(() -> {
                                BarFieldAccess acc = BarFieldAccess.forElement(el);
                                ClientConfig cfg = ModConfigManager.getClient();
                                acc.setSubElementX(cfg, sub, startA);
                                acc.setSubElementY(cfg, sub, startB);
                                cfg.save();
                            });
                        }
                    }
                    case RESIZE_W, RESIZE_H, RESIZE_CORNER -> {
                        if (sub != null && (bar.access.subElementWidth(c, sub) != startA || bar.access.subElementHeight(c, sub) != startB)) {
                            pushUndo(() -> {
                                BarFieldAccess acc = BarFieldAccess.forElement(el);
                                ClientConfig cfg = ModConfigManager.getClient();
                                acc.setSubElementWidth(cfg, sub, startA);
                                acc.setSubElementHeight(cfg, sub, startB);
                                cfg.save();
                            });
                        }
                    }
                    case RESIZE_TEXT_SCALE -> {
                        if (sub != null && Math.abs(bar.access.subElementTextScale(c, sub) - startS) > 0.001f) {
                            pushUndo(() -> {
                                BarFieldAccess acc = BarFieldAccess.forElement(el);
                                ClientConfig cfg = ModConfigManager.getClient();
                                acc.setSubElementTextScale(cfg, sub, startS);
                                cfg.save();
                            });
                        }
                    }
                    default -> {}
                }
            }
            dragKind = DragKind.NONE;
            c.save();
            return true;
        }
        return super.mouseReleased(event);
    }

    // ===== Keyboard =====

    @Override
    public boolean keyPressed(@NotNull KeyEvent event) {
        ContextMenu menu = activeContextMenu;
        if (menu != null && menu.isVisible()) {
            if (menu.handleKeyPressed(event.key())) {
                if (!menu.isVisible()) activeContextMenu = null;
                return true;
            }
        }

        int key = event.key();
        boolean shift = (event.modifiers() & GLFW.GLFW_MOD_SHIFT) != 0;
        boolean ctrl = (event.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
        BarHandle bar = focusedBar();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (EditModeManager.isFocusMode()) { exitFocus(); return true; }
            onClose();
            return true;
        }

        if (key == GLFW.GLFW_KEY_Z) {
            performUndo();
            return true;
        }

        if (key == GLFW.GLFW_KEY_TAB) {
            closeActiveContextMenu();
            if (EditModeManager.isFocusMode()) {
                cycleSub(shift ? -1 : 1);
            } else {
                cycleBarFocus(shift ? -1 : 1);
            }
            return true;
        }

        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            if (bar != null) {
                if (!EditModeManager.isFocusMode()) {
                    enterFocus();
                } else {
                    openMenuAtFocusedSub();
                }
                return true;
            }
        }

        if (key == GLFW.GLFW_KEY_M || key == GLFW.GLFW_KEY_MENU) {
            if (bar != null) {
                if (EditModeManager.isFocusMode() && activeSub != null) {
                    openMenuAtFocusedSub();
                } else {
                    openMenuAtFocusedBar();
                }
                return true;
            }
        }

        if (bar != null) {
            ClientConfig c = ModConfigManager.getClient();
            if (EditModeManager.isFocusMode() && activeSub != null) {
                if ((key == GLFW.GLFW_KEY_EQUAL || key == GLFW.GLFW_KEY_KP_ADD) && bar.access.hasTextScale(activeSub)) {
                    adjustActiveTextScale(bar, SCALE_STEP);
                    return true;
                }
                if ((key == GLFW.GLFW_KEY_MINUS || key == GLFW.GLFW_KEY_KP_SUBTRACT) && bar.access.hasTextScale(activeSub)) {
                    adjustActiveTextScale(bar, -SCALE_STEP);
                    return true;
                }
                if (shift && activeSub.isResizable()) {
                    DraggableElement el = bar.element;
                    SubElementType sub = activeSub;
                    if (bar.access.hasTextScale(activeSub) && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)) {
                        adjustActiveTextScale(bar, key == GLFW.GLFW_KEY_UP ? SCALE_STEP : -SCALE_STEP);
                        return true;
                    }
                    int oldW = bar.access.subElementWidth(c, sub);
                    int oldH = bar.access.subElementHeight(c, sub);
                    switch (key) {
                        case GLFW.GLFW_KEY_LEFT -> {
                            pushUndo(() -> { bar.access.setSubElementWidth(ModConfigManager.getClient(), sub, oldW); ModConfigManager.getClient().save(); });
                            bar.access.setSubElementWidth(c, sub, Math.max(1, oldW - 1));
                            c.save();
                            return true;
                        }
                        case GLFW.GLFW_KEY_RIGHT -> {
                            pushUndo(() -> { bar.access.setSubElementWidth(ModConfigManager.getClient(), sub, oldW); ModConfigManager.getClient().save(); });
                            bar.access.setSubElementWidth(c, sub, oldW + 1);
                            c.save();
                            return true;
                        }
                        case GLFW.GLFW_KEY_UP -> {
                            pushUndo(() -> { bar.access.setSubElementHeight(ModConfigManager.getClient(), sub, oldH); ModConfigManager.getClient().save(); });
                            bar.access.setSubElementHeight(c, sub, Math.max(1, oldH - 1));
                            c.save();
                            return true;
                        }
                        case GLFW.GLFW_KEY_DOWN -> {
                            pushUndo(() -> { bar.access.setSubElementHeight(ModConfigManager.getClient(), sub, oldH); ModConfigManager.getClient().save(); });
                            bar.access.setSubElementHeight(c, sub, oldH + 1);
                            c.save();
                            return true;
                        }
                    }
                }
                int step = ctrl ? NUDGE_PIXELS_FAST : NUDGE_PIXELS;
                SubElementType sub = activeSub;
                int oldX = bar.access.subElementX(c, sub);
                int oldY = bar.access.subElementY(c, sub);
                switch (key) {
                    case GLFW.GLFW_KEY_LEFT -> {
                        pushUndo(() -> { bar.access.setSubElementX(ModConfigManager.getClient(), sub, oldX); ModConfigManager.getClient().save(); });
                        bar.access.setSubElementX(c, sub, oldX - step);
                        c.save();
                        return true;
                    }
                    case GLFW.GLFW_KEY_RIGHT -> {
                        pushUndo(() -> { bar.access.setSubElementX(ModConfigManager.getClient(), sub, oldX); ModConfigManager.getClient().save(); });
                        bar.access.setSubElementX(c, sub, oldX + step);
                        c.save();
                        return true;
                    }
                    case GLFW.GLFW_KEY_UP -> {
                        pushUndo(() -> { bar.access.setSubElementY(ModConfigManager.getClient(), sub, oldY); ModConfigManager.getClient().save(); });
                        bar.access.setSubElementY(c, sub, oldY - step);
                        c.save();
                        return true;
                    }
                    case GLFW.GLFW_KEY_DOWN -> {
                        pushUndo(() -> { bar.access.setSubElementY(ModConfigManager.getClient(), sub, oldY); ModConfigManager.getClient().save(); });
                        bar.access.setSubElementY(c, sub, oldY + step);
                        c.save();
                        return true;
                    }
                    case GLFW.GLFW_KEY_R -> { resetSub(bar, sub); return true; }
                }
            } else {
                int barStep = shift ? NUDGE_PIXELS_FAST : NUDGE_PIXELS;
                int oldX = bar.access.totalX(c);
                int oldY = bar.access.totalY(c);
                switch (key) {
                    case GLFW.GLFW_KEY_LEFT -> {
                        pushUndo(() -> { bar.access.setTotalX(ModConfigManager.getClient(), oldX); ModConfigManager.getClient().save(); });
                        bar.access.setTotalX(c, oldX - barStep);
                        c.save();
                        return true;
                    }
                    case GLFW.GLFW_KEY_RIGHT -> {
                        pushUndo(() -> { bar.access.setTotalX(ModConfigManager.getClient(), oldX); ModConfigManager.getClient().save(); });
                        bar.access.setTotalX(c, oldX + barStep);
                        c.save();
                        return true;
                    }
                    case GLFW.GLFW_KEY_UP -> {
                        pushUndo(() -> { bar.access.setTotalY(ModConfigManager.getClient(), oldY); ModConfigManager.getClient().save(); });
                        bar.access.setTotalY(c, oldY - barStep);
                        c.save();
                        return true;
                    }
                    case GLFW.GLFW_KEY_DOWN -> {
                        pushUndo(() -> { bar.access.setTotalY(ModConfigManager.getClient(), oldY); ModConfigManager.getClient().save(); });
                        bar.access.setTotalY(c, oldY + barStep);
                        c.save();
                        return true;
                    }
                    case GLFW.GLFW_KEY_R -> { openResetConfirm(bar); return true; }
                }
            }
        }
        return super.keyPressed(event);
    }

    // ===== Helpers =====

    private void setActiveSub(SubElementType sub) {
        activeSub = sub;
        EditModeManager.setFocusedSubElement(sub);
    }

    private void enterFocus() {
        setActiveSub(SubElementType.BAR_MAIN);
        EditModeManager.setFocusMode(true);
    }

    private void exitFocus() {
        setActiveSub(null);
        dragKind = DragKind.NONE;
        EditModeManager.setFocusMode(false);
    }

    private void adjustActiveTextScale(BarHandle bar, float delta) {
        if (bar == null || activeSub == null || !bar.access.hasTextScale(activeSub)) return;
        ClientConfig c = ModConfigManager.getClient();
        SubElementType sub = activeSub;
        float prev = bar.access.subElementTextScale(c, sub);
        float next = Math.max(0.1f, Math.min(5.0f, Math.round((prev + delta) * 20f) / 20f));
        if (Math.abs(next - prev) > 0.001f) {
            pushUndo(() -> {
                bar.access.setSubElementTextScale(ModConfigManager.getClient(), sub, prev);
                ModConfigManager.getClient().save();
            });
            bar.access.setSubElementTextScale(c, sub, next);
            c.save();
        }
    }

    private void pushUndo(Runnable action) {
        if (undoStack.size() >= MAX_UNDO_STEPS) {
            undoStack.removeLast();
        }
        undoStack.push(action);
    }

    private void performUndo() {
        if (!undoStack.isEmpty()) {
            undoStack.pop().run();
        }
    }

    private void cycleSub(int direction) {
        BarHandle bar = focusedBar();
        Player player = player();
        if (bar == null || player == null) return;
        int idx = activeSub == null ? 0 : indexOfSub(activeSub);
        for (int attempt = 0; attempt < FOCUS_SUB_ELEMENTS.length; attempt++) {
            idx = (idx + direction + FOCUS_SUB_ELEMENTS.length) % FOCUS_SUB_ELEMENTS.length;
            SubElementType candidate = FOCUS_SUB_ELEMENTS[idx];
            if (barExposesSub(bar, candidate, player)) {
                setActiveSub(candidate);
                return;
            }
        }
    }

    private int indexOfSub(SubElementType sub) {
        for (int i = 0; i < FOCUS_SUB_ELEMENTS.length; i++) {
            if (FOCUS_SUB_ELEMENTS[i] == sub) return i;
        }
        return 0;
    }

    private void cycleBarFocus(int direction) {
        if (bars.isEmpty()) return;
        DraggableElement current = EditModeManager.getFocusedElement();
        int idx = -1;
        for (int i = 0; i < bars.size(); i++) {
            if (bars.get(i).element == current) { idx = i; break; }
        }
        idx = (idx + direction + bars.size()) % bars.size();
        EditModeManager.setFocusedElement(bars.get(idx).element);
    }

    private BarHandle focusedBar() {
        DraggableElement focused = EditModeManager.getFocusedElement();
        if (focused == null) return null;
        for (BarHandle bar : bars) if (bar.element == focused) return bar;
        return null;
    }

    private Player player() {
        return Minecraft.getInstance().player;
    }

    private BarHandle hitTest(double mouseX, double mouseY) {
        Player p = player();
        if (p == null) return null;
        int mx = (int) mouseX;
        int my = (int) mouseY;
        for (BarHandle bar : bars) {
            if (isInside(mx, my, bar.renderer.getScreenRect(p))) return bar;
            for (SubElementType sub : SubElementType.values()) {
                if (!barExposesSub(bar, sub, p)) continue;
                ScreenRect r = bar.renderer.getSubElementRect(sub, p);
                if (isInside(mx, my, r)) return bar;
            }
        }
        return null;
    }

    private SubElementType subAt(BarHandle bar, int mouseX, int mouseY) {
        Player p = player();
        if (p == null) return null;
        if (activeSub != null && barExposesSub(bar, activeSub, p)) {
            ScreenRect activeRect = bar.renderer.getSubElementRect(activeSub, p);
            if (isInside(mouseX, mouseY, activeRect)) return activeSub;
        }
        for (int i = FOCUS_SUB_ELEMENTS.length - 1; i >= 0; i--) {
            SubElementType sub = FOCUS_SUB_ELEMENTS[i];
            if (!barExposesSub(bar, sub, p)) continue;
            ScreenRect r = bar.renderer.getSubElementRect(sub, p);
            if (isInside(mouseX, mouseY, r)) return sub;
        }
        return null;
    }

    private static boolean isInside(int mouseX, int mouseY, ScreenRect rect) {
        if (rect == null || rect.width() <= 0 || rect.height() <= 0) return false;
        return mouseX >= rect.x() && mouseX < rect.x() + rect.width()
                && mouseY >= rect.y() && mouseY < rect.y() + rect.height();
    }

    private static int widthHandleCenterX(ScreenRect r) { return r.x() + r.width(); }
    private static int widthHandleCenterY(ScreenRect r) { return r.y() + r.height() / 2; }
    private static int heightHandleCenterX(ScreenRect r) { return r.x() + r.width() / 2; }
    private static int heightHandleCenterY(ScreenRect r) { return r.y() + r.height(); }
    private static int cornerHandleCenterX(ScreenRect r) { return r.x() + r.width(); }
    private static int cornerHandleCenterY(ScreenRect r) { return r.y() + r.height(); }

    private boolean hitWidthHandle(int mouseX, int mouseY, ScreenRect r) {
        if (r == null || r.width() <= 0) return false;
        return Math.abs(mouseX - widthHandleCenterX(r)) <= HANDLE_HIT_RADIUS
                && Math.abs(mouseY - widthHandleCenterY(r)) <= HANDLE_HIT_RADIUS;
    }

    private boolean hitHeightHandle(int mouseX, int mouseY, ScreenRect r) {
        if (r == null || r.height() <= 0) return false;
        return Math.abs(mouseX - heightHandleCenterX(r)) <= HANDLE_HIT_RADIUS
                && Math.abs(mouseY - heightHandleCenterY(r)) <= HANDLE_HIT_RADIUS;
    }

    private boolean hitCornerHandle(int mouseX, int mouseY, ScreenRect r) {
        if (r == null || r.width() <= 0 || r.height() <= 0) return false;
        return Math.abs(mouseX - cornerHandleCenterX(r)) <= HANDLE_HIT_RADIUS
                && Math.abs(mouseY - cornerHandleCenterY(r)) <= HANDLE_HIT_RADIUS;
    }

    private void startBarMove(BarHandle bar, int mouseX, int mouseY) {
        ClientConfig c = ModConfigManager.getClient();
        dragKind = DragKind.MOVE_BAR;
        dragStartMouseX = mouseX;
        dragStartMouseY = mouseY;
        dragStartValueA = bar.access.totalX(c);
        dragStartValueB = bar.access.totalY(c);
    }

    private void startSubMove(BarHandle bar, SubElementType sub, int mouseX, int mouseY) {
        ClientConfig c = ModConfigManager.getClient();
        dragKind = DragKind.MOVE_SUB;
        dragStartMouseX = mouseX;
        dragStartMouseY = mouseY;
        dragStartValueA = bar.access.subElementX(c, sub);
        dragStartValueB = bar.access.subElementY(c, sub);
    }

    private void startResize(BarHandle bar, SubElementType sub, int mouseX, int mouseY, DragKind kind) {
        ClientConfig c = ModConfigManager.getClient();
        dragKind = kind;
        dragStartMouseX = mouseX;
        dragStartMouseY = mouseY;
        dragStartValueA = bar.access.subElementWidth(c, sub);
        dragStartValueB = bar.access.subElementHeight(c, sub);
    }

    private void startTextScaleDrag(BarHandle bar, SubElementType sub, int mouseX, int mouseY) {
        ClientConfig c = ModConfigManager.getClient();
        dragKind = DragKind.RESIZE_TEXT_SCALE;
        dragStartMouseX = mouseX;
        dragStartMouseY = mouseY;
        dragStartScale = bar.access.subElementTextScale(c, sub);
    }

    private static Component elementName(DraggableElement e) {
        return e == null ? Component.empty() : Component.translatable(e.getTranslationKey());
    }

    /**
     * General per-bar context menu — used in bar mode (right-click bar) and in focus mode
     * when the user right-clicks empty space (no sub under cursor). Mode-aware: in focus mode
     * the "Edit Sub-Elements" entry is replaced by "Exit Sub-Element Editor". Sub-specific
     * tweaks (toggle this layer, text-align for TEXT, reset this layer) live in the sub menu.
     */
    private void openBarContextMenu(BarHandle bar, int mouseX, int mouseY) {
        closeActiveContextMenu();
        EditModeManager.setFocusedElement(bar.element);
        List<ContextMenuItem> items = new ArrayList<>();
        items.add(ContextMenuItem.title(elementName(bar.element)));
        if (EditModeManager.isFocusMode()) {
            items.add(new ContextMenuItem(
                    Component.translatable("gui.dynamic_resource_bars.context.exit_sub_elements"),
                    it -> exitFocus()));
        } else {
            items.add(new ContextMenuItem(
                    Component.translatable("gui.dynamic_resource_bars.context.edit_sub_elements"),
                    it -> enterFocus()));
        }
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.set_size_position"),
                it -> Minecraft.getInstance().setScreen(new ManualSizePositionScreen(this, bar.element))));
        ContextMenuItem behaviorItem = behaviorCycleItem(bar);
        if (behaviorItem != null) {
            items.add(behaviorItem);
        }
        items.add(barVisibilityItem(bar));
        items.add(textVisibilityItem(bar));
        items.add(anchorPointItem(bar));
        items.add(fillDirectionItem(bar));
        if (bar.element == DraggableElement.HEALTH_BAR) {
            items.add(absorptionDisplayModeItem(bar));
        }
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.toggle_layers"),
                it -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(new ToggleLayersScreen(this, bar.element));
                    }
                }));
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.reset_element"),
                it -> openResetConfirm(bar),
                0xFFFF8080));

        ContextMenu menu = new ContextMenu(mouseX, mouseY, items);
        menu.setOnClose(() -> activeContextMenu = null);
        activeContextMenu = menu;
    }

    /**
     * Generic factory for a "label: %s" context-menu item that opens an {@link EnumSelectScreen}
     * over {@code values}. Each per-bar enum item (anchor, text align, fill direction, bar
     * visibility, text visibility) is one call to this helper — no per-enum boilerplate.
     */
    private <T extends Enum<T>> ContextMenuItem enumItem(
            BarHandle bar, String labelKey, String selectTitleKey, T[] values,
            Function<T, String> translationKey,
            Function<ClientConfig, T> getter, BiConsumer<ClientConfig, T> setter) {
        ClientConfig c = ModConfigManager.getClient();
        return new ContextMenuItem(
                Component.translatable(labelKey, Component.translatable(translationKey.apply(getter.apply(c)))),
                it -> openEnumSelect(
                        Component.translatable(selectTitleKey),
                        Arrays.asList(values),
                        v -> Component.translatable(translationKey.apply(v)),
                        () -> getter.apply(ModConfigManager.getClient()),
                        v -> {
                            ClientConfig cfg = ModConfigManager.getClient();
                            setter.accept(cfg, v);
                            cfg.save();
                        }));
    }

    /** "Anchor Point: %s" — opens an enum-select for {@link HUDPositioning.BarPlacement}. */
    private ContextMenuItem anchorPointItem(BarHandle bar) {
        return enumItem(bar,
                "gui.dynamic_resource_bars.context.anchor_point_value",
                "gui.dynamic_resource_bars.enum_select.anchor_point",
                HUDPositioning.BarPlacement.values(),
                HUDPositioning.BarPlacement::getTranslationKey,
                bar.access::anchor, bar.access::setAnchor);
    }

    /** "Text Align: %s" — opens an enum-select for {@link HorizontalAlignment}. */
    private ContextMenuItem textAlignItem(BarHandle bar) {
        return enumItem(bar,
                "gui.dynamic_resource_bars.context.text_align",
                "gui.dynamic_resource_bars.enum_select.text_align",
                HorizontalAlignment.values(),
                HorizontalAlignment::getTranslationKey,
                bar.access::textAlign, bar.access::setTextAlign);
    }

    /** "Text Align: %s" but bound to absorption-text alignment — only meaningful on the health bar. */
    private ContextMenuItem absorptionTextAlignItem(BarHandle bar) {
        return enumItem(bar,
                "gui.dynamic_resource_bars.context.text_align",
                "gui.dynamic_resource_bars.enum_select.text_align",
                HorizontalAlignment.values(),
                HorizontalAlignment::getTranslationKey,
                bar.access::absorptionTextAlign, bar.access::setAbsorptionTextAlign);
    }

    /** "Fill Direction: %s" — opens an enum-select for {@link FillDirection}. */
    private ContextMenuItem fillDirectionItem(BarHandle bar) {
        return enumItem(bar,
                "gui.dynamic_resource_bars.context.fill_direction",
                "gui.dynamic_resource_bars.enum_select.fill_direction",
                FillDirection.values(),
                FillDirection::getTranslationKey,
                bar.access::fillDirection, bar.access::setFillDirection);
    }

    /** "Bar Visibility: %s" — opens an enum-select for {@link BarVisibility}. */
    private ContextMenuItem barVisibilityItem(BarHandle bar) {
        return enumItem(bar,
                "gui.dynamic_resource_bars.context.bar_visibility",
                "gui.dynamic_resource_bars.enum_select.bar_visibility",
                BarVisibility.values(),
                BarVisibility::getTranslationKey,
                bar.access::visibility, bar.access::setVisibility);
    }

    /** "Absorption: %s" — health-only enum-select for OVERLAY vs SQUEEZE absorption rendering. */
    private ContextMenuItem absorptionDisplayModeItem(BarHandle bar) {
        return enumItem(bar,
                "gui.dynamic_resource_bars.context.absorption_display_mode",
                "gui.dynamic_resource_bars.enum_select.absorption_display_mode",
                AbsorptionDisplayMode.values(),
                AbsorptionDisplayMode::getTranslationKey,
                bar.access::absorptionDisplayMode, bar.access::setAbsorptionDisplayMode);
    }

    /** "Text Visibility: %s" — opens an enum-select for {@link TextBehavior}. */
    private ContextMenuItem textVisibilityItem(BarHandle bar) {
        return enumItem(bar,
                "gui.dynamic_resource_bars.context.text_visibility",
                "gui.dynamic_resource_bars.enum_select.text_visibility",
                TextBehavior.values(),
                TextBehavior::getTranslationKey,
                bar.access::textBehavior, bar.access::setTextBehavior);
    }

    /** Toggle for the active sub-element's visibility — only added when {@link SubElementType#isToggleable()}. */
    private ContextMenuItem toggleLayerItem(BarHandle bar, SubElementType sub) {
        ClientConfig c = ModConfigManager.getClient();
        boolean shown = bar.access.enableSubElement(c, sub);
        Component label = shown
                ? Component.translatable("gui.dynamic_resource_bars.context.hide_layer")
                : Component.translatable("gui.dynamic_resource_bars.context.show_layer");
        return new ContextMenuItem(label, it -> {
            ClientConfig cfg = ModConfigManager.getClient();
            bar.access.setEnableSubElement(cfg, sub, !bar.access.enableSubElement(cfg, sub));
            cfg.save();
        });
    }

    /**
     * Builds the source/behavior context entry for the enum-driven bars. The label shows the
     * current value inline; clicking opens an {@link EnumSelectScreen} so the user picks the
     * value directly instead of cycling through it (the context menu closes on click, which
     * made cycling tedious). Returns {@code null} for bars with no such option (health) or
     * when only one value is selectable on the current classpath (e.g. mana with no provider mod).
     */
    private ContextMenuItem behaviorCycleItem(BarHandle bar) {
        ClientConfig c = ModConfigManager.getClient();
        return switch (bar.element) {
            case STAMINA_BAR -> {
                List<StaminaBarBehavior> opts = new ArrayList<>();
                for (StaminaBarBehavior v : StaminaBarBehavior.values()) {
                    if (StaminaProviderManager.isModLoaded(v)) opts.add(v);
                }
                if (opts.size() < 2) yield null;
                yield new ContextMenuItem(
                        Component.translatable("gui.dynamic_resource_bars.context.bar_source",
                                Component.translatable(c.staminaBarBehavior.getTranslationKey())),
                        it -> openEnumSelect(
                                Component.translatable("gui.dynamic_resource_bars.enum_select.stamina_source"),
                                opts,
                                v -> Component.translatable(v.getTranslationKey()),
                                () -> ModConfigManager.getClient().staminaBarBehavior,
                                v -> {
                                    ClientConfig cfg = ModConfigManager.getClient();
                                    cfg.staminaBarBehavior = v;
                                    StaminaProviderManager.updateActiveProvider();
                                    cfg.save();
                                }));
            }
            case MANA_BAR -> {
                List<ManaBarBehavior> opts = new ArrayList<>();
                for (ManaBarBehavior v : ManaBarBehavior.values()) {
                    if (ManaProviderManager.isModLoaded(v)) opts.add(v);
                }
                if (opts.size() < 2) yield null;
                yield new ContextMenuItem(
                        Component.translatable("gui.dynamic_resource_bars.context.bar_source",
                                Component.translatable(c.manaBarBehavior.getTranslationKey())),
                        it -> openEnumSelect(
                                Component.translatable("gui.dynamic_resource_bars.enum_select.mana_source"),
                                opts,
                                v -> Component.translatable(v.getTranslationKey()),
                                () -> ModConfigManager.getClient().manaBarBehavior,
                                v -> {
                                    ClientConfig cfg = ModConfigManager.getClient();
                                    cfg.manaBarBehavior = v;
                                    ManaProviderManager.updateActiveProvider();
                                    cfg.save();
                                }));
            }
            case ARMOR_BAR -> new ContextMenuItem(
                    Component.translatable("gui.dynamic_resource_bars.context.bar_behavior",
                            Component.translatable(c.armorBarBehavior.getTranslationKey())),
                    it -> openEnumSelect(
                            Component.translatable("gui.dynamic_resource_bars.enum_select.armor_behavior"),
                            Arrays.asList(BarRenderBehavior.values()),
                            v -> Component.translatable(v.getTranslationKey()),
                            () -> ModConfigManager.getClient().armorBarBehavior,
                            v -> {
                                ClientConfig cfg = ModConfigManager.getClient();
                                cfg.armorBarBehavior = v;
                                cfg.save();
                            }));
            case AIR_BAR -> new ContextMenuItem(
                    Component.translatable("gui.dynamic_resource_bars.context.bar_behavior",
                            Component.translatable(c.airBarBehavior.getTranslationKey())),
                    it -> openEnumSelect(
                            Component.translatable("gui.dynamic_resource_bars.enum_select.air_behavior"),
                            Arrays.asList(BarRenderBehavior.values()),
                            v -> Component.translatable(v.getTranslationKey()),
                            () -> ModConfigManager.getClient().airBarBehavior,
                            v -> {
                                ClientConfig cfg = ModConfigManager.getClient();
                                cfg.airBarBehavior = v;
                                cfg.save();
                            }));
            case HEALTH_BAR -> new ContextMenuItem(
                    Component.translatable("gui.dynamic_resource_bars.context.bar_behavior",
                            Component.translatable(c.healthBarBehavior.getTranslationKey())),
                    it -> openEnumSelect(
                            Component.translatable("gui.dynamic_resource_bars.enum_select.health_behavior"),
                            Arrays.asList(BarRenderBehavior.values()),
                            v -> Component.translatable(v.getTranslationKey()),
                            () -> ModConfigManager.getClient().healthBarBehavior,
                            v -> {
                                ClientConfig cfg = ModConfigManager.getClient();
                                cfg.healthBarBehavior = v;
                                cfg.save();
                            }));
        };
    }

    private <T> void openEnumSelect(Component title, List<T> options, Function<T, Component> labelFn,
                                    Supplier<T> getter, Consumer<T> setter) {
        if (this.minecraft == null) return;
        if (activeContextMenu != null) {
            activeContextMenu.close();
            activeContextMenu = null;
        }
        this.minecraft.setScreen(new EnumSelectScreen<>(this, title, options, labelFn, getter, setter));
    }

    /**
     * Sub-element-specific context menu — only options that apply to the targeted sub-element.
     * Header reads "Element — Sub Element". General per-bar options (anchor, visibility, etc.)
     * live in the bar context menu, surfaced on right-clicking empty space within the bar.
     */
    private void openSubContextMenu(BarHandle bar, SubElementType sub, int mouseX, int mouseY) {
        if (sub == null) return;
        closeActiveContextMenu();
        List<ContextMenuItem> items = new ArrayList<>();
        items.add(ContextMenuItem.title(Component.translatable(
                "gui.dynamic_resource_bars.context.sub_title",
                elementName(bar.element),
                Component.translatable(sub.getTranslationKey()))));
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.configure_sub_element"),
                it -> Minecraft.getInstance().setScreen(new SubElementConfigScreen(this, bar.element, sub))));
        if (sub.isToggleable()) {
            items.add(toggleLayerItem(bar, sub));
        }
        if (sub == SubElementType.TEXT) {
            items.add(textVisibilityItem(bar));
            items.add(textAlignItem(bar));
            items.add(textScaleUpItem(bar, sub));
            items.add(textScaleDownItem(bar, sub));
        }
        if (sub == SubElementType.ABSORPTION_TEXT) {
            items.add(absorptionTextAlignItem(bar));
            items.add(absorptionDisplayModeItem(bar));
            items.add(textScaleUpItem(bar, sub));
            items.add(textScaleDownItem(bar, sub));
        }
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.reset_sub_element"),
                it -> resetSub(bar, sub),
                0xFFFF8080));

        ContextMenu menu = new ContextMenu(mouseX, mouseY, items);
        menu.setOnClose(() -> activeContextMenu = null);
        activeContextMenu = menu;
    }

    /**
     * Global context menu — surfaced on right-click in bar mode when the cursor isn't on any
     * bar. Currently a one-item menu offering "Reset All Bars to Default".
     */
    private void openGlobalContextMenu(int mouseX, int mouseY) {
        closeActiveContextMenu();
        ClientConfig c = ModConfigManager.getClient();
        List<ContextMenuItem> items = new ArrayList<>();
        items.add(ContextMenuItem.title(Component.translatable("gui.dynamic_resource_bars.hud_editor.title_main")));
        items.add(new ContextMenuItem(
                Component.translatable(c.enableCustomHud
                        ? "gui.dynamic_resource_bars.config.custom_hud.enabled"
                        : "gui.dynamic_resource_bars.config.custom_hud.disabled"),
                it -> {
                    ClientConfig cfg = ModConfigManager.getClient();
                    cfg.enableCustomHud = !cfg.enableCustomHud;
                    cfg.save();
                }));
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.hud_editor.button.reset_all_bars"),
                it -> openResetAllConfirm(),
                0xFFFF8080));
        ContextMenu menu = new ContextMenu(mouseX, mouseY, items);
        menu.setOnClose(() -> activeContextMenu = null);
        activeContextMenu = menu;
    }

    private void openResetAllConfirm() {
        if (this.minecraft == null) return;
        this.minecraft.setScreen(new ConfirmResetScreen(
                this,
                Component.translatable("gui.dynamic_resource_bars.confirm.reset_all.title"),
                Component.translatable("gui.dynamic_resource_bars.confirm.reset_all.line1"),
                this::resetAllBars));
    }

    private void resetAllBars() {
        for (BarHandle bar : bars) resetBar(bar);
    }

    /**
     * Closes any visible context menu. Call before assigning a fresh menu so the prior one's
     * {@code onClose} callback fires (which nulls {@code activeContextMenu}) — without this,
     * Tab→menu would silently overwrite the live menu and leak any tear-down hooks.
     */
    private void closeActiveContextMenu() {
        if (activeContextMenu != null) {
            activeContextMenu.close();
            activeContextMenu = null;
        }
    }

    private ContextMenuItem textScaleUpItem(BarHandle bar, SubElementType sub) {
        ClientConfig c = ModConfigManager.getClient();
        String scaleStr = FieldEditorUtils.formatScale(bar.access.subElementTextScale(c, sub));
        return new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.text_scale_up", scaleStr),
                it -> {
                    setActiveSub(sub);
                    adjustActiveTextScale(bar, SCALE_STEP);
                });
    }

    private ContextMenuItem textScaleDownItem(BarHandle bar, SubElementType sub) {
        return new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.text_scale_down"),
                it -> {
                    setActiveSub(sub);
                    adjustActiveTextScale(bar, -SCALE_STEP);
                });
    }

    /** Resets a single sub-element's geometry + sub-specific behavior to defaults. */
    private void resetSub(BarHandle bar, SubElementType sub) {
        ClientConfig defaults = ClientConfig.createDefaults();
        ClientConfig c = ModConfigManager.getClient();
        int oldX = bar.access.subElementX(c, sub);
        int oldY = bar.access.subElementY(c, sub);
        int oldW = bar.access.subElementWidth(c, sub);
        int oldH = bar.access.subElementHeight(c, sub);
        float oldScale = bar.access.subElementTextScale(c, sub);
        boolean oldEnabled = bar.access.enableSubElement(c, sub);
        pushUndo(() -> {
            ClientConfig cfg = ModConfigManager.getClient();
            bar.access.setSubElementX(cfg, sub, oldX);
            bar.access.setSubElementY(cfg, sub, oldY);
            if (sub.isResizable()) {
                bar.access.setSubElementWidth(cfg, sub, oldW);
                bar.access.setSubElementHeight(cfg, sub, oldH);
            }
            if (bar.access.hasTextScale(sub)) {
                bar.access.setSubElementTextScale(cfg, sub, oldScale);
            }
            if (sub.isToggleable()) {
                bar.access.setEnableSubElement(cfg, sub, oldEnabled);
            }
            cfg.save();
        });
        bar.access.resetSubElement(c, defaults, sub);
        c.save();
    }

    /** Tab-anchor: opens the bar context menu off the top-right of the focused bar's complex rect. */
    private void openMenuAtFocusedBar() {
        BarHandle bar = focusedBar();
        Player p = player();
        if (bar == null || p == null) return;
        ScreenRect r = bar.renderer.getScreenRect(p);
        openBarContextMenu(bar, r.x() + r.width(), r.y());
    }

    /** Tab-anchor: opens the sub-element context menu off the top-right of the active sub's rect. */
    private void openMenuAtFocusedSub() {
        BarHandle bar = focusedBar();
        Player p = player();
        if (bar == null || p == null || activeSub == null) return;
        ScreenRect r = bar.renderer.getSubElementRect(activeSub, p);
        if (r.width() <= 0 || r.height() <= 0) return;
        openSubContextMenu(bar, activeSub, r.x() + r.width(), r.y());
    }

    private void openResetConfirm(BarHandle bar) {
        if (this.minecraft == null) return;
        this.minecraft.setScreen(new ConfirmResetScreen(
                this,
                Component.translatable("gui.dynamic_resource_bars.confirm.reset_element.title", elementName(bar.element)),
                Component.translatable("gui.dynamic_resource_bars.confirm.reset_element.line1"),
                () -> resetBar(bar)));
    }

    private void resetBar(BarHandle bar) {
        ClientConfig defaults = ClientConfig.createDefaults();
        ClientConfig c = ModConfigManager.getClient();
        for (SubElementType sub : SubElementType.values()) {
            bar.access.resetSubElement(c, defaults, sub);
        }
        bar.access.setTotalX(c, bar.access.totalX(defaults));
        bar.access.setTotalY(c, bar.access.totalY(defaults));
        bar.access.resetBehaviorFields(c, defaults);
        c.save();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(this.previousScreen);
        // Cleanup happens in removed(), which fires for any screen replacement — including
        // dying mid-edit, which substitutes a DeathScreen and would otherwise leave edit-mode
        // outlines and the absorption "+8" placeholder visible until the editor is reopened.
    }

    @Override
    public void removed() {
        EditModeManager.clearFocusedElement();
        EditModeManager.setFocusMode(false);
        if (EditModeManager.isEditModeEnabled()) EditModeManager.toggleEditMode();
        ModConfigManager.getClient().save();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /** Pairs a bar's editor identity with its renderer (for screen-rect hit-testing) and its config-field accessor. */
    private record BarHandle(DraggableElement element, AbstractBarRenderer renderer, BarFieldAccess access) {}
}
