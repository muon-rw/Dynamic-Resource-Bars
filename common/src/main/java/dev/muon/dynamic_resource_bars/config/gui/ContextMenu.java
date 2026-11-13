package dev.muon.dynamic_resource_bars.config.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class ContextMenu implements Renderable, GuiEventListener {

    private static final int BACKGROUND_COLOR = 0xF0101216;
    private static final int HOVER_COLOR = 0xFF2B4C7E;
    private static final int BORDER_COLOR = 0xFF586578;
    private static final int SEPARATOR_COLOR = 0xFF343B46;
    private static final float TEXT_SCALE = 1.0f;
    private static final int PADDING = 4;
    private static final int ITEM_HEIGHT = 13;
    private static final int SEPARATOR_HEIGHT = 5;
    private static final int MIN_WIDTH = 130;

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
        int totalHeight = PADDING * 2;
        for (ContextMenuItem item : items) {
            if (!item.label().getString().isEmpty()) {
                int labelWidth = (int) Math.ceil(font.width(item.label()) * TEXT_SCALE) + PADDING * 2 + 4;
                maxWidth = Math.max(maxWidth, labelWidth);
            }
            totalHeight += rowHeight(item);
        }
        this.width = maxWidth;
        this.height = totalHeight;

        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        if (x + width > screenWidth - 4) x = screenWidth - width - 4;
        if (x < 4) x = 4;
        if (y + height > screenHeight - 4) y = screenHeight - height - 4;
        if (y < 4) y = 4;
        this.x = x;
        this.y = y;
    }

    private static int rowHeight(ContextMenuItem item) {
        return item.label().getString().isEmpty() && item.isTitle() ? SEPARATOR_HEIGHT : ITEM_HEIGHT;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        graphics.fill(x, y, x + width, y + height, BACKGROUND_COLOR);
        graphics.fill(x, y, x + width, y + 1, BORDER_COLOR);
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR);
        graphics.fill(x, y, x + 1, y + height, BORDER_COLOR);
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR);

        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            updateHovered(mouseX, mouseY);
        }

        Font font = Minecraft.getInstance().font;
        PoseStack pose = graphics.pose();
        int itemY = y + PADDING;
        for (int i = 0; i < items.size(); i++) {
            ContextMenuItem item = items.get(i);
            int rHeight = rowHeight(item);
            if (item.label().getString().isEmpty() && item.isTitle()) {
                int lineY = itemY + rHeight / 2;
                graphics.fill(x + 4, lineY, x + width - 4, lineY + 1, SEPARATOR_COLOR);
            } else {
                if (i == hoveredIndex && item.isClickable()) {
                    graphics.fill(x + 2, itemY, x + width - 2, itemY + rHeight, HOVER_COLOR);
                }
                pose.pushPose();
                int textOffsetY = Math.max(1, (int) ((rHeight - font.lineHeight * TEXT_SCALE) / 2f));
                pose.translate(x + PADDING + 2, itemY + textOffsetY, 0);
                pose.scale(TEXT_SCALE, TEXT_SCALE, 1.0f);
                int color = item.isEnabled() ? item.textColor() : 0xFF808080;
                graphics.drawString(font, item.label(), 0, 0, color, true);
                pose.popPose();
                if (item.isTitle()) {
                    graphics.fill(x + 3, itemY + rHeight - 1, x + width - 3, itemY + rHeight, SEPARATOR_COLOR);
                }
            }
            itemY += rHeight;
        }
    }

    private void updateHovered(int mouseX, int mouseY) {
        hoveredIndex = -1;
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            int scanY = y + PADDING;
            for (int i = 0; i < items.size(); i++) {
                ContextMenuItem item = items.get(i);
                int rHeight = rowHeight(item);
                if (mouseY >= scanY && mouseY < scanY + rHeight) {
                    if (item.isClickable()) hoveredIndex = i;
                    break;
                }
                scanY += rHeight;
            }
        }
    }

    public boolean handleMouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        int mx = (int) mouseX;
        int my = (int) mouseY;
        boolean inside = mx >= x && mx < x + width && my >= y && my < y + height;
        if (!inside) {
            close();
            return false;
        }
        updateHovered(mx, my);
        if (button == 0 && hoveredIndex >= 0 && hoveredIndex < items.size()) {
            ContextMenuItem item = items.get(hoveredIndex);
            if (item.isClickable()) {
                item.execute();
                close();
            }
        }
        return true;
    }

    public boolean handleKeyPressed(int key) {
        if (!visible) return false;
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_DOWN) {
            stepHover(1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_UP) {
            stepHover(-1);
            return true;
        }
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_SPACE) {
            if (hoveredIndex >= 0 && hoveredIndex < items.size()) {
                ContextMenuItem item = items.get(hoveredIndex);
                if (item.isClickable()) {
                    item.execute();
                    close();
                    return true;
                }
            }
        }
        return false;
    }

    private void stepHover(int dir) {
        if (items.isEmpty()) return;
        int idx = hoveredIndex < 0 ? (dir > 0 ? -1 : items.size()) : hoveredIndex;
        for (int attempt = 0; attempt < items.size(); attempt++) {
            idx = (idx + dir + items.size()) % items.size();
            if (items.get(idx).isClickable()) {
                hoveredIndex = idx;
                return;
            }
        }
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
