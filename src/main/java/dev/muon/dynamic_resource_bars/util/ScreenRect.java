package dev.muon.dynamic_resource_bars.util;

public record ScreenRect(int x, int y, int width, int height) {
    public boolean contains(int px, int py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }
} 