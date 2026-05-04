package dev.muon.dynamic_resource_bars.config.gui;

import dev.muon.dynamic_resource_bars.compat.ManaProviderManager;
import dev.muon.dynamic_resource_bars.compat.StaminaProviderManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import dev.muon.dynamic_resource_bars.render.AbstractBarRenderer;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Drag-and-drop HUD editor.
 *
 * <p>Modes</p>
 * <ul>
 *   <li><b>Bar mode</b> (default): click a bar to select it; drag to move the whole bar; arrow
 *       keys nudge it.</li>
 *   <li><b>Focus mode</b> (entered by clicking the already-selected bar a second time, or by
 *       picking the "Edit Sub-Elements" context-menu item): each sub-element (background,
 *       bar fill, foreground, text) is individually clickable. Click and drag a sub-element
 *       to move it within its parent bar. Right/bottom edge handles resize the background,
 *       bar fill, and foreground. Esc returns to bar mode.</li>
 * </ul>
 *
 * <p>Bars are rendered on top of the editor's dim overlay so the player can see what they
 * are editing — the editor's {@link #extractBackground(GuiGraphicsExtractor, int, int, float)} draws a flat dim instead of the
 * default blur, then bars are painted afterwards in {@link #extractRenderState}.
 */
public class HudEditorScreen extends Screen {

    private static final int OVERLAY_DIM_COLOR = 0x80000000;
    private static final int HELP_TEXT_COLOR = 0xFFFFFFFF;
    private static final int FOCUS_OUTLINE_COLOR = 0xFFFFFF00;
    private static final int FOCUS_HOVER_COLOR = 0x80FFFF00;
    private static final int RESIZE_HANDLE_COLOR = 0xFFFFAA00;
    private static final int RESIZE_HANDLE_HOT_COLOR = 0xFFFFFFFF;
    /** Handles are square; rendered exactly as drawn with no expanded hitbox. */
    private static final int HANDLE_SIZE = 2;
    private static final int NUDGE_PIXELS = 1;
    private static final int NUDGE_PIXELS_FAST = 5;


    /**
     * Sub-elements that participate in focus-mode drag/resize, in Tab cycle order.
     * ICON / ABSORPTION_TEXT are only applicable to bars that expose them via
     * {@code getCustomSubElementRect} (icons: air/armor, absorption text: health) —
     * sub-elements with an empty rect are skipped during hit-testing and Tab.
     */
    private static final SubElementType[] FOCUS_SUB_ELEMENTS = {
            SubElementType.BACKGROUND, SubElementType.BAR_MAIN,
            SubElementType.FOREGROUND, SubElementType.TEXT,
            SubElementType.ICON, SubElementType.ABSORPTION_TEXT
    };

    private static final List<Component> MAIN_HELP_TEXT = List.of(
            Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line1"),
            Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line2"),
            Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line3"),
            Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line4"));
    private static final List<Component> FOCUS_HELP_LINES = List.of(
            Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line1"),
            Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line2"),
            Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line3"));

    private final Screen previousScreen;
    private final List<BarHandle> bars = new ArrayList<>();

    // ===== Editor state =====
    private SubElementType activeSub;
    private DragKind dragKind = DragKind.NONE;
    private int dragStartMouseX, dragStartMouseY;
    private int dragStartValueA, dragStartValueB; // X+Y for move; W+H for resize

    private ContextMenu activeContextMenu;

    private DraggableElement undoElement;
    private int undoTotalX, undoTotalY;
    private boolean undoAvailable;

    private enum DragKind { NONE, MOVE_BAR, MOVE_SUB, RESIZE_W, RESIZE_H }

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
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        // Flat dim instead of the default blur so bars are still readable on top.
        graphics.fill(0, 0, this.width, this.height, OVERLAY_DIM_COLOR);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

        // Paint the bars *after* the screen background so they sit on top of the dim.
        Player player = player();
        if (player != null) {
            DeltaTracker dt = Minecraft.getInstance().getDeltaTracker();
            for (BarHandle bar : bars) bar.renderer.render(graphics, player, dt);
        }

        renderFocusModeOverlay(graphics, mouseX, mouseY);

        int y = 8;
        for (Component line : helpText()) {
            graphics.centeredText(this.font, line, this.width / 2, y, HELP_TEXT_COLOR);
            y += 10;
        }

        if (activeContextMenu != null && activeContextMenu.isVisible()) {
            activeContextMenu.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }
    }

    private List<Component> helpText() {
        if (!EditModeManager.isFocusMode()) return MAIN_HELP_TEXT;
        // Focus title carries the focused-bar name, which changes on Tab — has to be rebuilt.
        List<Component> result = new ArrayList<>(1 + FOCUS_HELP_LINES.size());
        result.add(Component.translatable("gui.dynamic_resource_bars.hud_editor.title_focused",
                elementName(EditModeManager.getFocusedElement())));
        result.addAll(FOCUS_HELP_LINES);
        return result;
    }

    /**
     * Renders focus-mode sub-element outlines, resize handles, and the hover tooltip in one
     * pass over {@link #FOCUS_SUB_ELEMENTS}. Caches each sub's rect locally so we never call
     * {@code getSubElementRect} more than once per sub per frame (it allocates a fresh
     * BarConfig snapshot under the hood).
     */
    private void renderFocusModeOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!EditModeManager.isFocusMode()) return;
        BarHandle bar = focusedBar();
        if (bar == null) return;
        Player player = player();
        if (player == null) return;

        ScreenRect[] rects = new ScreenRect[FOCUS_SUB_ELEMENTS.length];
        SubElementType handleSub = null;
        boolean handleIsWidth = false;
        SubElementType hoveredSub = null;

        for (int i = 0; i < FOCUS_SUB_ELEMENTS.length; i++) {
            SubElementType sub = FOCUS_SUB_ELEMENTS[i];
            ScreenRect r = bar.renderer.getSubElementRect(sub, player);
            rects[i] = r;
            if (r.width() <= 0 || r.height() <= 0) continue;

            // Outline (every visible sub).
            int outlineColor = sub == activeSub ? FOCUS_OUTLINE_COLOR : 0xA0FFFFFF;
            graphics.outline(r.x() - 1, r.y() - 1, r.width() + 2, r.height() + 2, outlineColor);

            // Track the topmost hovered sub for tooltip fallback. Forward iteration order
            // matches paint order; later subs (TEXT/ABSORPTION_TEXT) override earlier hits,
            // mirroring the reverse-priority of the old subAt().
            if (isInside(mouseX, mouseY, r)) hoveredSub = sub;

            if (!sub.isResizable()) continue;

            // Hover tint on resizable subs only (matches prior behavior).
            if (sub != activeSub && isInside(mouseX, mouseY, r)) {
                graphics.fill(r.x(), r.y(), r.x() + r.width(), r.y() + r.height(), FOCUS_HOVER_COLOR);
            }

            int wHandleX = r.x() + r.width() - 1;
            int wHandleY = r.y() + r.height() / 2 - HANDLE_SIZE / 2;
            boolean wHot = hitWidthHandle(mouseX, mouseY, r);
            graphics.fill(wHandleX, wHandleY, wHandleX + HANDLE_SIZE, wHandleY + HANDLE_SIZE,
                    wHot ? RESIZE_HANDLE_HOT_COLOR : RESIZE_HANDLE_COLOR);
            int hHandleX = r.x() + r.width() / 2 - HANDLE_SIZE / 2;
            int hHandleY = r.y() + r.height() - 1;
            boolean hHot = hitHeightHandle(mouseX, mouseY, r);
            graphics.fill(hHandleX, hHandleY, hHandleX + HANDLE_SIZE, hHandleY + HANDLE_SIZE,
                    hHot ? RESIZE_HANDLE_HOT_COLOR : RESIZE_HANDLE_COLOR);
            if (handleSub == null) {
                if (wHot) { handleSub = sub; handleIsWidth = true; }
                else if (hHot) { handleSub = sub; handleIsWidth = false; }
            }
        }

        // Handle tooltip wins over plain sub tooltip — handles sit on the sub rect's edge,
        // and the user wants to know which axis they're about to drag.
        if (handleSub != null) {
            String key = handleIsWidth
                    ? "gui.dynamic_resource_bars.handle.horizontal_size"
                    : "gui.dynamic_resource_bars.handle.vertical_size";
            renderTooltip(graphics, Component.translatable(key,
                    Component.translatable(handleSub.getTranslationKey())), mouseX, mouseY);
        } else if (hoveredSub != null) {
            renderTooltip(graphics, Component.translatable(hoveredSub.getTranslationKey()), mouseX, mouseY);
        }
    }

    /** Inventory-style hover tooltip rendered near the cursor. */
    private void renderTooltip(GuiGraphicsExtractor graphics, Component label, int mouseX, int mouseY) {
        int textWidth = this.font.width(label);
        int padding = 3;
        int boxWidth = textWidth + padding * 2;
        int boxHeight = this.font.lineHeight + padding * 2;
        int x = mouseX + 10;
        int y = mouseY - boxHeight - 4;
        if (x + boxWidth > this.width) x = this.width - boxWidth - 2;
        if (y < 2) y = mouseY + 14;
        graphics.fill(x, y, x + boxWidth, y + boxHeight, 0xE0000000);
        graphics.fill(x, y, x + boxWidth, y + 1, 0xFF404040);
        graphics.fill(x, y + boxHeight - 1, x + boxWidth, y + boxHeight, 0xFF404040);
        graphics.fill(x, y, x + 1, y + boxHeight, 0xFF404040);
        graphics.fill(x + boxWidth - 1, y, x + boxWidth, y + boxHeight, 0xFF404040);
        graphics.text(this.font, label, x + padding, y + padding, 0xFFFFFFFF, false);
    }

    // ===== Mouse =====

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        // Capture the menu reference locally — its onClose may null `activeContextMenu`
        // synchronously inside handleMouseClicked, which would NPE the post-call check.
        ContextMenu menu = activeContextMenu;
        if (menu != null && menu.isVisible()) {
            boolean consumed = menu.handleMouseClicked(event);
            if (!menu.isVisible()) activeContextMenu = null;
            if (consumed) return true;
        }
        if (super.mouseClicked(event, doubleClick)) return true;

        int mouseX = (int) event.x();
        int mouseY = (int) event.y();
        int button = event.button();

        if (EditModeManager.isFocusMode()) {
            BarHandle bar = focusedBar();
            if (bar == null) { exitFocus(); return true; }
            Player player = player();
            if (player == null) return true;

            if (button == 1) {
                SubElementType sub = subAt(bar, mouseX, mouseY);
                if (sub != null) {
                    openSubContextMenu(bar, sub, mouseX, mouseY);
                } else {
                    // Right-click on empty space (no sub-element under cursor): show the
                    // general bar menu, mode-aware so it offers Exit Sub-Element instead of Edit.
                    openBarContextMenu(bar, mouseX, mouseY);
                }
                return true;
            }

            for (SubElementType sub : FOCUS_SUB_ELEMENTS) {
                if (!sub.isResizable()) continue;
                ScreenRect r = bar.renderer.getSubElementRect(sub, player);
                if (hitWidthHandle(mouseX, mouseY, r)) {
                    activeSub = sub;
                    startResize(bar, sub, mouseX, mouseY, DragKind.RESIZE_W);
                    return true;
                }
                if (hitHeightHandle(mouseX, mouseY, r)) {
                    activeSub = sub;
                    startResize(bar, sub, mouseX, mouseY, DragKind.RESIZE_H);
                    return true;
                }
            }

            SubElementType clicked = subAt(bar, mouseX, mouseY);
            if (clicked != null) {
                activeSub = clicked;
                startSubMove(bar, clicked, mouseX, mouseY);
                return true;
            }

            // Click hit no sub-element and no resize handle → exit focus mode. The complex
            // rect outline is hidden in focus mode so it carries no UX meaning here either.
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
            // Explicit double-click on a bar enters focus mode (uses framework's
            // double-click detection — short window so accidental click-then-click
            // does NOT trigger focus).
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
            default -> {}
        }
        return true;
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (dragKind != DragKind.NONE && event.button() == 0) {
            if (dragKind == DragKind.MOVE_BAR) {
                BarHandle bar = focusedBar();
                if (bar != null) {
                    undoElement = bar.element;
                    undoTotalX = dragStartValueA;
                    undoTotalY = dragStartValueB;
                    undoAvailable = true;
                }
            }
            dragKind = DragKind.NONE;
            ModConfigManager.getClient().save();
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
        BarHandle bar = focusedBar();

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (EditModeManager.isFocusMode()) { exitFocus(); return true; }
            onClose();
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB) {
            // Tab cycles to the next element AND opens its context menu — saves a right-click
            // for the most common interaction (cycle-and-tweak). Re-tabbing closes the prior
            // menu (via openMenuAtElement → activeContextMenu replacement) and opens the next.
            if (EditModeManager.isFocusMode()) {
                cycleSub(shift ? -1 : 1);
                openMenuAtFocusedSub();
            } else {
                cycleBarFocus(shift ? -1 : 1);
                openMenuAtFocusedBar();
            }
            return true;
        }

        if (bar != null) {
            ClientConfig c = ModConfigManager.getClient();
            if (EditModeManager.isFocusMode() && activeSub != null) {
                // Shift+arrow resizes the active sub-element (only meaningful when it's
                // resizable — non-resizable subs route to no-op setters).
                if (shift && activeSub.isResizable()) {
                    switch (key) {
                        case GLFW.GLFW_KEY_LEFT  -> { bar.access.setSubElementWidth(c, activeSub, Math.max(1, bar.access.subElementWidth(c, activeSub) - 1)); c.save(); return true; }
                        case GLFW.GLFW_KEY_RIGHT -> { bar.access.setSubElementWidth(c, activeSub, bar.access.subElementWidth(c, activeSub) + 1); c.save(); return true; }
                        case GLFW.GLFW_KEY_UP    -> { bar.access.setSubElementHeight(c, activeSub, Math.max(1, bar.access.subElementHeight(c, activeSub) - 1)); c.save(); return true; }
                        case GLFW.GLFW_KEY_DOWN  -> { bar.access.setSubElementHeight(c, activeSub, bar.access.subElementHeight(c, activeSub) + 1); c.save(); return true; }
                    }
                }
                switch (key) {
                    case GLFW.GLFW_KEY_LEFT  -> { bar.access.setSubElementX(c, activeSub, bar.access.subElementX(c, activeSub) - NUDGE_PIXELS); c.save(); return true; }
                    case GLFW.GLFW_KEY_RIGHT -> { bar.access.setSubElementX(c, activeSub, bar.access.subElementX(c, activeSub) + NUDGE_PIXELS); c.save(); return true; }
                    case GLFW.GLFW_KEY_UP    -> { bar.access.setSubElementY(c, activeSub, bar.access.subElementY(c, activeSub) - NUDGE_PIXELS); c.save(); return true; }
                    case GLFW.GLFW_KEY_DOWN  -> { bar.access.setSubElementY(c, activeSub, bar.access.subElementY(c, activeSub) + NUDGE_PIXELS); c.save(); return true; }
                }
            } else {
                int barStep = shift ? NUDGE_PIXELS_FAST : NUDGE_PIXELS;
                switch (key) {
                    case GLFW.GLFW_KEY_LEFT  -> { bar.access.setTotalX(c, bar.access.totalX(c) - barStep); c.save(); return true; }
                    case GLFW.GLFW_KEY_RIGHT -> { bar.access.setTotalX(c, bar.access.totalX(c) + barStep); c.save(); return true; }
                    case GLFW.GLFW_KEY_UP    -> { bar.access.setTotalY(c, bar.access.totalY(c) - barStep); c.save(); return true; }
                    case GLFW.GLFW_KEY_DOWN  -> { bar.access.setTotalY(c, bar.access.totalY(c) + barStep); c.save(); return true; }
                    case GLFW.GLFW_KEY_R -> { openResetConfirm(bar); return true; }
                    case GLFW.GLFW_KEY_Z -> {
                        if (undoAvailable && undoElement == bar.element) {
                            bar.access.setTotalX(c, undoTotalX);
                            bar.access.setTotalY(c, undoTotalY);
                            c.save();
                            undoAvailable = false;
                            return true;
                        }
                    }
                }
            }
        }
        return super.keyPressed(event);
    }

    // ===== Helpers =====

    private void enterFocus() {
        activeSub = SubElementType.BAR_MAIN;
        EditModeManager.setFocusMode(true);
    }

    private void exitFocus() {
        activeSub = null;
        dragKind = DragKind.NONE;
        EditModeManager.setFocusMode(false);
    }

    private void cycleSub(int direction) {
        BarHandle bar = focusedBar();
        Player player = player();
        if (bar == null || player == null) return;
        int idx = activeSub == null ? 0 : indexOfSub(activeSub);
        // Step through the cycle until we land on a sub the focused bar actually exposes.
        for (int attempt = 0; attempt < FOCUS_SUB_ELEMENTS.length; attempt++) {
            idx = (idx + direction + FOCUS_SUB_ELEMENTS.length) % FOCUS_SUB_ELEMENTS.length;
            SubElementType candidate = FOCUS_SUB_ELEMENTS[idx];
            ScreenRect r = bar.renderer.getSubElementRect(candidate, player);
            if (r.width() > 0 && r.height() > 0) {
                activeSub = candidate;
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

    /**
     * Hit-tests both the complex rect AND every sub-element rect — the user can grab a bar by
     * any of its visible parts (background, fill, foreground, text, icon), not just the
     * potentially-disjoint complex outline. First bar that contains the cursor in any of
     * those regions wins.
     */
    private BarHandle hitTest(double mouseX, double mouseY) {
        Player p = player();
        if (p == null) return null;
        int mx = (int) mouseX;
        int my = (int) mouseY;
        for (BarHandle bar : bars) {
            if (isInside(mx, my, bar.renderer.getScreenRect(p))) return bar;
            for (SubElementType sub : SubElementType.values()) {
                ScreenRect r = bar.renderer.getSubElementRect(sub, p);
                if (isInside(mx, my, r)) return bar;
            }
        }
        return null;
    }

    private SubElementType subAt(BarHandle bar, int mouseX, int mouseY) {
        Player p = player();
        if (p == null) return null;
        // Iterate in reverse so visually-on-top sub-elements (text, foreground) win the hit-test.
        for (int i = FOCUS_SUB_ELEMENTS.length - 1; i >= 0; i--) {
            SubElementType sub = FOCUS_SUB_ELEMENTS[i];
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

    private boolean hitWidthHandle(int mouseX, int mouseY, ScreenRect r) {
        if (r == null || r.width() <= 0) return false;
        int hX = r.x() + r.width() - 1;
        int hY = r.y() + r.height() / 2 - HANDLE_SIZE / 2;
        return mouseX >= hX && mouseX < hX + HANDLE_SIZE
                && mouseY >= hY && mouseY < hY + HANDLE_SIZE;
    }

    private boolean hitHeightHandle(int mouseX, int mouseY, ScreenRect r) {
        if (r == null || r.height() <= 0) return false;
        int hX = r.x() + r.width() / 2 - HANDLE_SIZE / 2;
        int hY = r.y() + r.height() - 1;
        return mouseX >= hX && mouseX < hX + HANDLE_SIZE
                && mouseY >= hY && mouseY < hY + HANDLE_SIZE;
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
        }
        if (sub == SubElementType.ABSORPTION_TEXT) {
            items.add(absorptionTextAlignItem(bar));
            items.add(absorptionDisplayModeItem(bar));
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
        List<ContextMenuItem> items = new ArrayList<>();
        items.add(ContextMenuItem.title(Component.translatable("gui.dynamic_resource_bars.hud_editor.title_main")));
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

    /** Resets a single sub-element's geometry + sub-specific behavior to defaults. */
    private void resetSub(BarHandle bar, SubElementType sub) {
        ClientConfig defaults = ClientConfig.createDefaults();
        ClientConfig c = ModConfigManager.getClient();
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
