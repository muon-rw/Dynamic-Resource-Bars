package dev.muon.dynamic_resource_bars.config.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;

import java.util.ArrayList;
import java.util.List;

/**
 * A right-click context menu with black background and translucent selection highlights.
 * Scaled to match the size of health/stamina text for consistency.
 */
public class ContextMenu implements Renderable, GuiEventListener {
    private static final int BACKGROUND_COLOR = 0xE0000000; // Black with transparency
    private static final int HOVER_COLOR = 0x80FFFFFF; // White translucent for hover
    private static final int BORDER_COLOR = 0xFF404040; // Gray border
    private static final float SCALE_FACTOR = 0.5f; // Scale to match health text size
    private static final int PADDING = 3;
    private static final int ITEM_HEIGHT = 9;
    private static final int MIN_WIDTH = 100;
    
    private final List<ContextMenuItem> items;
    private final int x;
    private final int y;
    private int width;
    private int height;
    private int hoveredIndex = -1;
    private boolean visible = true;
    private Runnable onClose;
    
    public ContextMenu(int x, int y, List<ContextMenuItem> items) {
        this.items = new ArrayList<>(items);
        calculateDimensions();
        
        // Adjust position if menu would go off screen
        Minecraft mc = Minecraft.getInstance();
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        
        // Check right edge
        if (x + width > screenWidth) {
            x = screenWidth - width - 2;
        }
        // Check left edge
        if (x < 0) {
            x = 2;
        }
        
        // Check bottom edge
        if (y + height > screenHeight) {
            y = screenHeight - height - 2;
        }
        // Check top edge
        if (y < 0) {
            y = 2;
        }
        
        this.x = x;
        this.y = y;
    }
    
    private void calculateDimensions() {
        Font font = Minecraft.getInstance().font;
        int maxWidth = MIN_WIDTH;
        
        for (ContextMenuItem item : items) {
            if (!item.getLabel().getString().isEmpty()) {
                int labelWidth = (int)((font.width(item.getLabel()) + PADDING * 2) * SCALE_FACTOR);
                maxWidth = Math.max(maxWidth, labelWidth);
            }
        }
        
        this.width = maxWidth;
        this.height = items.size() * ITEM_HEIGHT + PADDING * 2;
    }
    
    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }
    
    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;
        
        RenderSystem.enableBlend();
        
        // Draw background
        graphics.fill(x, y, x + width, y + height, BACKGROUND_COLOR);
        
        // Draw border
        graphics.fill(x, y, x + width, y + 1, BORDER_COLOR); // Top
        graphics.fill(x, y + height - 1, x + width, y + height, BORDER_COLOR); // Bottom
        graphics.fill(x, y, x + 1, y + height, BORDER_COLOR); // Left
        graphics.fill(x + width - 1, y, x + width, y + height, BORDER_COLOR); // Right
        
        // Update hovered item
        updateHoveredIndex(mouseX, mouseY);
        
        // Draw items
        Font font = Minecraft.getInstance().font;
        int itemY = y + PADDING;
        
        graphics.pose().pushPose();
        
        for (int i = 0; i < items.size(); i++) {
            ContextMenuItem item = items.get(i);
            
            // Draw hover highlight for clickable items
            if (i == hoveredIndex && item.isClickable()) {
                graphics.fill(x + 1, itemY, x + width - 1, itemY + ITEM_HEIGHT, HOVER_COLOR);
            }
            
            // Draw text
            if (!item.getLabel().getString().isEmpty()) {
                graphics.pose().pushPose();
                graphics.pose().translate(x + PADDING, itemY + 2, 0);
                graphics.pose().scale(SCALE_FACTOR, SCALE_FACTOR, 1.0f);
                
                int color = item.isEnabled() ? item.getTextColor() : 0x808080;
                graphics.drawString(font, item.getLabel(), 0, 0, color, false);
                
                graphics.pose().popPose();
            }
            
            itemY += ITEM_HEIGHT;
        }
        
        graphics.pose().popPose();
        
        RenderSystem.disableBlend();
    }
    
    private void updateHoveredIndex(int mouseX, int mouseY) {
        hoveredIndex = -1;
        
        if (mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height) {
            int relativeY = mouseY - y - PADDING;
            int index = relativeY / ITEM_HEIGHT;
            
            if (index >= 0 && index < items.size() && items.get(index).isClickable()) {
                hoveredIndex = index;
            }
        }
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!visible) return false;
        
        int mx = (int) mouseX;
        int my = (int) mouseY;
        
        // Check if click is outside menu - close it
        if (mx < x || mx >= x + width || my < y || my >= y + height) {
            close();
            return false;
        }
        
        // Handle item click
        if (button == 0 && hoveredIndex >= 0 && hoveredIndex < items.size()) {
            ContextMenuItem item = items.get(hoveredIndex);
            if (item.isClickable()) {
                item.execute();
                // Menu stays open - it will only close when user clicks outside or presses ESC
                // Any widget rebuilds are deferred until menu closes
                return true;
            }
        }
        
        return true; // Consume the click even if nothing happened
    }
    
    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        updateHoveredIndex((int)mouseX, (int)mouseY);
    }
    
    public void close() {
        visible = false;
        if (onClose != null) {
            onClose.run();
        }
    }
    
    public int getX() {
        return x;
    }
    
    public int getY() {
        return y;
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public boolean isMouseOver(double mouseX, double mouseY) {
        return visible && mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;
    }
    
    @Override
    public void setFocused(boolean focused) {
        // Not needed for this implementation
    }
    
    @Override
    public boolean isFocused() {
        return visible;
    }
}

