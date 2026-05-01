package dev.muon.dynamic_resource_bars.config.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.input.MouseButtonEvent;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3x2fStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight popup menu rendered on top of the HUD editor. Owns its own hit-test and
 * close lifecycle; the host screen forwards mouse events via {@link #handleMouseClicked}
 * and key events via {@link #handleKeyPressed}.
 */
public class ContextMenu implements Renderable, GuiEventListener {

    private static final int BACKGROUND_COLOR = 0xE0000000;
    private static final int HOVER_COLOR = 0x80FFFFFF;
    private static final int BORDER_COLOR = 0xFF404040;
    private static final float TEXT_SCALE = 0.5f;
    private static final int PADDING = 3;
    private static final int ITEM_HEIGHT = 9;
    private static final int MIN_WIDTH = 100;

    private final List<ContextMenuItem> items;
    private final int x;
    private final int y;
    private final int width;
    private final int height;
    private int hoveredIndex = -1;
    private boolean visible = true;
    private Runnable onClose;

    public ContextMenu(int x, int y, List<ContextMenuItem> items) {
        this.items = new ArrayList<>(items);

        Font font = Minecraft.getInstance().font;
        int maxWidth = MIN_WIDTH;
        for (ContextMenuItem item : items) {
            if (!item.label().getString().isEmpty()) {
                int labelWidth = (int) ((font.width(item.label()) + PADDING * 2) * TEXT_SCALE);
                maxWidth = Math.max(maxWidth, labelWidth);
            }
        }
        this.width = maxWidth;
        this.height = items.size() * ITEM_HEIGHT + PADDING * 2;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        if (x + width > screenWidth) x = screenWidth - width - 2;
        if (x < 0) x = 2;
        if (y + height > screenHeight) y = screenHeight - height - 2;
        if (y < 0) y = 2;
        this.x = x;
        this.y = y;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        graphics.fill(x, y, x + width, y + height, BACKGROUND_COLOR);
        graphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        graphics.fill(x, y, x + 1, y + height, BORDER_COLOR);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);

        updateHovered(mouseX, mouseY);

        Font font = Minecraft.getInstance().font;
        Matrix3x2fStack pose = graphics.pose();
        int itemY = y + PADDING;
        for (int i = 0; i < items.size(); i++) {
            ContextMenuItem item = items.get(i);
            if (i == hoveredIndex && item.isClickable()) {
                graphics.fill(x + 1, itemY, x + width - 1, itemY + ITEM_HEIGHT, HOVER_COLOR);
            }
            if (!item.label().getString().isEmpty()) {
                pose.pushMatrix();
                pose.translate(x + PADDING, itemY + 2);
                pose.scale(TEXT_SCALE, TEXT_SCALE);
                int color = item.isEnabled() ? item.textColor() : 0xFF808080;
                graphics.text(font, item.label(), 0, 0, color, false);
                pose.popMatrix();
            }
            itemY += ITEM_HEIGHT;
        }
    }

    private void updateHovered(int mouseX, int mouseY) {
        hoveredIndex = -1;
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            int relativeY = mouseY - y - PADDING;
            int idx = relativeY / ITEM_HEIGHT;
            if (idx >= 0 && idx < items.size() && items.get(idx).isClickable()) hoveredIndex = idx;
        }
    }

    /**
     * Forwarded from the host screen's {@code mouseClicked} hook.
     *
     * @return {@code true} when the click was consumed (caller should NOT also process the click).
     *         Always returns true while the menu is visible — clicks outside merely close it.
     */
    public boolean handleMouseClicked(MouseButtonEvent event) {
        if (!visible) return false;
        int mx = (int) event.x();
        int my = (int) event.y();
        boolean inside = mx >= x && mx < x + width && my >= y && my < y + height;
        if (!inside) {
            close();
            return false;
        }
        if (event.button() == 0 && hoveredIndex >= 0 && hoveredIndex < items.size()) {
            ContextMenuItem item = items.get(hoveredIndex);
            if (item.isClickable()) item.execute();
        }
        return true;
    }

    /** Forwarded from the host screen's {@code keyPressed}. */
    public boolean handleKeyPressed(int key) {
        if (!visible) return false;
        if (key == 256) { // GLFW_KEY_ESCAPE
            close();
            return true;
        }
        return false;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        updateHovered((int) mouseX, (int) mouseY);
    }

    public void close() {
        if (!visible) return;
        visible = false;
        if (onClose != null) onClose.run();
    }

    public boolean isVisible() { return visible; }

    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }

    @Override
    public void setFocused(boolean focused) {}

    @Override
    public boolean isFocused() {
        return visible;
    }
}
