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
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.BarVisibility;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.EditModeManager;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
    private static final int HANDLE_THICKNESS = 2;
    private static final int HANDLE_LENGTH = 6;
    private static final int NUDGE_PIXELS = 1;
    private static final int NUDGE_PIXELS_FAST = 5;

    private static final int KEY_ESCAPE = 256;
    private static final int KEY_TAB = 258;
    private static final int KEY_LEFT = 263;
    private static final int KEY_RIGHT = 262;
    private static final int KEY_UP = 265;
    private static final int KEY_DOWN = 264;
    private static final int KEY_R = 82;
    private static final int KEY_Z = 90;
    private static final int MOD_SHIFT = 1;

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

    /**
     * Sub-elements that have width/height fields and therefore drag-resize handles.
     * Icons are square so both handles bind to {@code iconSize} (see
     * {@link BarFieldAccess#setSubElementWidth}).
     */
    private static final SubElementType[] RESIZABLE_SUB_ELEMENTS = {
            SubElementType.BACKGROUND, SubElementType.BAR_MAIN, SubElementType.FOREGROUND,
            SubElementType.ICON
    };

    private final Screen previousScreen;
    private final List<BarHandle> bars = new ArrayList<>();

    // ===== Editor state =====
    private boolean focusMode;
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
        if (focusMode) {
            DraggableElement focused = EditModeManager.getFocusedElement();
            return List.of(
                    Component.translatable("gui.dynamic_resource_bars.hud_editor.title_focused", elementName(focused)),
                    Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line1"),
                    Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line2"),
                    Component.translatable("gui.dynamic_resource_bars.hud_editor.help.focus.line3"));
        }
        return List.of(
                Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line1"),
                Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line2"),
                Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line3"),
                Component.translatable("gui.dynamic_resource_bars.hud_editor.help.main.line4"));
    }

    /** Renders focus-mode sub-element outlines and resize handles. */
    private void renderFocusModeOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        if (!focusMode) return;
        BarHandle bar = focusedBar();
        if (bar == null) return;
        Player player = player();
        if (player == null) return;

        for (SubElementType sub : FOCUS_SUB_ELEMENTS) {
            ScreenRect r = bar.renderer.getSubElementRect(sub, player);
            if (r.width() <= 0 || r.height() <= 0) continue;
            int outlineColor = sub == activeSub ? FOCUS_OUTLINE_COLOR : 0xA0FFFFFF;
            graphics.outline(r.x() - 1, r.y() - 1, r.width() + 2, r.height() + 2, outlineColor);
        }

        for (SubElementType sub : RESIZABLE_SUB_ELEMENTS) {
            ScreenRect r = bar.renderer.getSubElementRect(sub, player);
            if (r.width() <= 0 || r.height() <= 0) continue;
            boolean hover = isInside(mouseX, mouseY, r);
            if (hover && sub != activeSub) {
                graphics.fill(r.x(), r.y(), r.x() + r.width(), r.y() + r.height(), FOCUS_HOVER_COLOR);
            }
            int rhX = r.x() + r.width() - 1;
            int rhY = r.y() + r.height() / 2 - HANDLE_LENGTH / 2;
            int hot = hitWidthHandle(mouseX, mouseY, r) ? RESIZE_HANDLE_HOT_COLOR : RESIZE_HANDLE_COLOR;
            graphics.fill(rhX, rhY, rhX + HANDLE_THICKNESS, rhY + HANDLE_LENGTH, hot);
            int bhX = r.x() + r.width() / 2 - HANDLE_LENGTH / 2;
            int bhY = r.y() + r.height() - 1;
            hot = hitHeightHandle(mouseX, mouseY, r) ? RESIZE_HANDLE_HOT_COLOR : RESIZE_HANDLE_COLOR;
            graphics.fill(bhX, bhY, bhX + HANDLE_LENGTH, bhY + HANDLE_THICKNESS, hot);
        }
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

        if (focusMode) {
            BarHandle bar = focusedBar();
            if (bar == null) { exitFocus(); return true; }
            Player player = player();
            if (player == null) return true;

            if (button == 1) {
                SubElementType sub = subAt(bar, mouseX, mouseY);
                openSubContextMenu(bar, sub != null ? sub : activeSub, mouseX, mouseY);
                return true;
            }

            for (SubElementType sub : RESIZABLE_SUB_ELEMENTS) {
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

            ScreenRect complex = bar.renderer.getScreenRect(player);
            if (button == 0 && !isInside(mouseX, mouseY, complex)) {
                exitFocus();
                return true;
            }
            return true;
        }

        BarHandle hit = hitTest(mouseX, mouseY);
        if (button == 1) {
            if (hit != null) openBarContextMenu(hit, mouseX, mouseY);
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
        boolean shift = (event.modifiers() & MOD_SHIFT) != 0;
        int step = shift ? NUDGE_PIXELS_FAST : NUDGE_PIXELS;
        BarHandle bar = focusedBar();

        if (key == KEY_ESCAPE) {
            if (focusMode) { exitFocus(); return true; }
            onClose();
            return true;
        }
        if (key == KEY_TAB) {
            if (focusMode) cycleSub(shift ? -1 : 1);
            else cycleBarFocus(shift ? -1 : 1);
            return true;
        }

        if (bar != null) {
            ClientConfig c = ModConfigManager.getClient();
            if (focusMode && activeSub != null) {
                switch (key) {
                    case KEY_LEFT  -> { bar.access.setSubElementX(c, activeSub, bar.access.subElementX(c, activeSub) - step); c.save(); return true; }
                    case KEY_RIGHT -> { bar.access.setSubElementX(c, activeSub, bar.access.subElementX(c, activeSub) + step); c.save(); return true; }
                    case KEY_UP    -> { bar.access.setSubElementY(c, activeSub, bar.access.subElementY(c, activeSub) - step); c.save(); return true; }
                    case KEY_DOWN  -> { bar.access.setSubElementY(c, activeSub, bar.access.subElementY(c, activeSub) + step); c.save(); return true; }
                }
            } else {
                switch (key) {
                    case KEY_LEFT  -> { bar.access.setTotalX(c, bar.access.totalX(c) - step); c.save(); return true; }
                    case KEY_RIGHT -> { bar.access.setTotalX(c, bar.access.totalX(c) + step); c.save(); return true; }
                    case KEY_UP    -> { bar.access.setTotalY(c, bar.access.totalY(c) - step); c.save(); return true; }
                    case KEY_DOWN  -> { bar.access.setTotalY(c, bar.access.totalY(c) + step); c.save(); return true; }
                    case KEY_R     -> { openResetConfirm(bar); return true; }
                    case KEY_Z     -> {
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
        focusMode = true;
        activeSub = SubElementType.BAR_MAIN;
    }

    private void exitFocus() {
        focusMode = false;
        activeSub = null;
        dragKind = DragKind.NONE;
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

    private BarHandle hitTest(double mouseX, double mouseY) {
        Player p = player();
        if (p == null) return null;
        for (BarHandle bar : bars) {
            ScreenRect rect = bar.renderer.getScreenRect(p);
            if (isInside((int) mouseX, (int) mouseY, rect)) return bar;
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
        int hY = r.y() + r.height() / 2 - HANDLE_LENGTH / 2;
        return mouseX >= hX - 1 && mouseX <= hX + HANDLE_THICKNESS + 1
                && mouseY >= hY - 1 && mouseY <= hY + HANDLE_LENGTH + 1;
    }

    private boolean hitHeightHandle(int mouseX, int mouseY, ScreenRect r) {
        if (r == null || r.height() <= 0) return false;
        int hX = r.x() + r.width() / 2 - HANDLE_LENGTH / 2;
        int hY = r.y() + r.height() - 1;
        return mouseX >= hX - 1 && mouseX <= hX + HANDLE_LENGTH + 1
                && mouseY >= hY - 1 && mouseY <= hY + HANDLE_THICKNESS + 1;
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
        if (e == null) return Component.empty();
        return switch (e) {
            case HEALTH_BAR -> Component.translatable("gui.dynamic_resource_bars.element.health");
            case MANA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.mana");
            case STAMINA_BAR -> Component.translatable("gui.dynamic_resource_bars.element.stamina");
            case ARMOR_BAR -> Component.translatable("gui.dynamic_resource_bars.element.armor");
            case AIR_BAR -> Component.translatable("gui.dynamic_resource_bars.element.air");
        };
    }

    private void openBarContextMenu(BarHandle bar, int mouseX, int mouseY) {
        EditModeManager.setFocusedElement(bar.element);
        List<ContextMenuItem> items = new ArrayList<>();
        items.add(ContextMenuItem.title(elementName(bar.element)));
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.edit_sub_elements"),
                it -> enterFocus()));
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.set_size_position"),
                it -> Minecraft.getInstance().setScreen(new ManualSizePositionScreen(this, bar.element))));
        ContextMenuItem behaviorItem = behaviorCycleItem(bar);
        if (behaviorItem != null) {
            items.add(behaviorItem);
        }
        items.add(barVisibilityItem(bar));
        items.add(textVisibilityItem(bar));
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.reset_element"),
                it -> openResetConfirm(bar),
                0xFFFF8080));

        ContextMenu menu = new ContextMenu(mouseX, mouseY, items);
        menu.setOnClose(() -> activeContextMenu = null);
        activeContextMenu = menu;
    }

    /** "Bar Visibility: %s" — opens an enum-select for {@link BarVisibility}. */
    private ContextMenuItem barVisibilityItem(BarHandle bar) {
        ClientConfig c = ModConfigManager.getClient();
        return new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.bar_visibility",
                        Component.translatable(bar.access.visibility(c).getTranslationKey())),
                it -> openEnumSelect(
                        Component.translatable("gui.dynamic_resource_bars.enum_select.bar_visibility"),
                        Arrays.asList(BarVisibility.values()),
                        v -> Component.translatable(v.getTranslationKey()),
                        () -> bar.access.visibility(ModConfigManager.getClient()),
                        v -> {
                            ClientConfig cfg = ModConfigManager.getClient();
                            bar.access.setVisibility(cfg, v);
                            cfg.save();
                        }));
    }

    /** "Text Visibility: %s" — opens an enum-select for {@link TextBehavior}. */
    private ContextMenuItem textVisibilityItem(BarHandle bar) {
        ClientConfig c = ModConfigManager.getClient();
        return new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.text_visibility",
                        Component.translatable(bar.access.textBehavior(c).getTranslationKey())),
                it -> openEnumSelect(
                        Component.translatable("gui.dynamic_resource_bars.enum_select.text_visibility"),
                        Arrays.asList(TextBehavior.values()),
                        v -> Component.translatable(v.getTranslationKey()),
                        () -> bar.access.textBehavior(ModConfigManager.getClient()),
                        v -> {
                            ClientConfig cfg = ModConfigManager.getClient();
                            bar.access.setTextBehavior(cfg, v);
                            cfg.save();
                        }));
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

    private void openSubContextMenu(BarHandle bar, SubElementType sub, int mouseX, int mouseY) {
        List<ContextMenuItem> items = new ArrayList<>();
        items.add(ContextMenuItem.title(Component.translatable(
                "gui.dynamic_resource_bars.hud_editor.title_focused", elementName(bar.element))));
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.exit_sub_elements"),
                it -> exitFocus()));
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.set_size_position"),
                it -> Minecraft.getInstance().setScreen(new ManualSizePositionScreen(this, bar.element))));
        items.add(barVisibilityItem(bar));
        items.add(textVisibilityItem(bar));
        items.add(new ContextMenuItem(
                Component.translatable("gui.dynamic_resource_bars.context.reset_element"),
                it -> openResetConfirm(bar),
                0xFFFF8080));

        ContextMenu menu = new ContextMenu(mouseX, mouseY, items);
        menu.setOnClose(() -> activeContextMenu = null);
        activeContextMenu = menu;
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
        bar.access.setBgWidth(c, bar.access.bgWidth(defaults));
        bar.access.setBgHeight(c, bar.access.bgHeight(defaults));
        bar.access.setBgX(c, bar.access.bgX(defaults));
        bar.access.setBgY(c, bar.access.bgY(defaults));
        bar.access.setBarWidth(c, bar.access.barWidth(defaults));
        bar.access.setBarHeight(c, bar.access.barHeight(defaults));
        bar.access.setBarX(c, bar.access.barX(defaults));
        bar.access.setBarY(c, bar.access.barY(defaults));
        bar.access.setOverlayWidth(c, bar.access.overlayWidth(defaults));
        bar.access.setOverlayHeight(c, bar.access.overlayHeight(defaults));
        bar.access.setOverlayX(c, bar.access.overlayX(defaults));
        bar.access.setOverlayY(c, bar.access.overlayY(defaults));
        bar.access.setTextX(c, bar.access.textX(defaults));
        bar.access.setTextY(c, bar.access.textY(defaults));
        bar.access.setIconX(c, bar.access.iconX(defaults));
        bar.access.setIconY(c, bar.access.iconY(defaults));
        bar.access.setIconSize(c, bar.access.iconSize(defaults));
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
