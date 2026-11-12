package dev.muon.dynamic_resource_bars.config.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

import java.util.Locale;

final class FieldEditorUtils {

    private static final int VALID_TEXT_COLOR = 0xFFE0E0E0;
    private static final int INVALID_TEXT_COLOR = 0xFFFF5555;
    private static final int LABEL_COLOR = 0xFFE8E8E8;
    private static final int LABEL_GAP = 6;

    private FieldEditorUtils() {}

    static EditBox intBox(Font font, int x, int y, int w, int h, int initial, int min, int max) {
        EditBox box = new EditBox(font, x, y, w, h, Component.empty());
        box.setValue(String.valueOf(initial));
        box.setResponder(text -> {
            try {
                int v = Integer.parseInt(text.trim());
                box.setTextColor(v >= min && v <= max ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR);
            } catch (NumberFormatException e) {
                box.setTextColor(INVALID_TEXT_COLOR);
            }
        });
        return box;
    }

    static EditBox floatBox(Font font, int x, int y, int w, int h, float initial, float min, float max) {
        EditBox box = new EditBox(font, x, y, w, h, Component.empty());
        box.setValue(formatScale(initial));
        box.setResponder(text -> {
            try {
                float v = Float.parseFloat(text.trim());
                box.setTextColor(v >= min && v <= max ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR);
            } catch (NumberFormatException e) {
                box.setTextColor(INVALID_TEXT_COLOR);
            }
        });
        return box;
    }

    static String formatScale(float value) {
        float rounded = Math.round(value * 100f) / 100f;
        if (Math.abs(rounded * 10f - Math.round(rounded * 10f)) < 0.001f) {
            return String.format(Locale.ROOT, "%.1f", rounded);
        }
        return String.format(Locale.ROOT, "%.2f", rounded);
    }

    static int parseSafely(EditBox box, int defaultVal, int min, int max) {
        if (box == null) return defaultVal;
        try {
            int v = Integer.parseInt(box.getValue().trim());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    static float parseFloatSafely(EditBox box, float defaultVal, float min, float max) {
        if (box == null) return defaultVal;
        try {
            float v = Float.parseFloat(box.getValue().trim());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    static void drawLabel(GuiGraphicsExtractor graphics, Font font, String key, int ignoredX, EditBox box) {
        if (box == null || !box.visible) return;
        Component label = Component.translatable(key);
        int textWidth = font.width(label);
        int rightAlignedX = Math.max(4, box.getX() - LABEL_GAP - textWidth);
        int y = box.getY() + (box.getHeight() - font.lineHeight) / 2;
        graphics.text(font, label, rightAlignedX, y, LABEL_COLOR, true);
    }
}
