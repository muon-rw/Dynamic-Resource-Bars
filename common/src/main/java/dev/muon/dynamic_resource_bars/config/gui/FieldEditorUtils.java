package dev.muon.dynamic_resource_bars.config.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;

/**
 * Shared helpers for "form-style" editor screens that render a column of
 * label + integer EditBox pairs (e.g. {@link ManualSizePositionScreen},
 * {@link SubElementConfigScreen}). Centralises the validate-on-type tinting,
 * the parse-with-clamp commit step, and the right-aligned label row.
 */
final class FieldEditorUtils {

    // EditBox renders these as full ARGB — the alpha byte must be 0xFF or the text is invisible.
    private static final int VALID_TEXT_COLOR = 0xFFE0E0E0;
    private static final int INVALID_TEXT_COLOR = 0xFFFF5555;

    private FieldEditorUtils() {}

    /** Editor box that flips its text color red when the typed value is out of [{@code min}, {@code max}]. */
    static EditBox intBox(Font font, int x, int y, int w, int h, int initial, int min, int max) {
        EditBox box = new EditBox(font, x, y, w, h, Component.empty());
        box.setValue(String.valueOf(initial));
        box.setResponder(text -> {
            try {
                int v = Integer.parseInt(text);
                box.setTextColor(v >= min && v <= max ? VALID_TEXT_COLOR : INVALID_TEXT_COLOR);
            } catch (NumberFormatException e) {
                box.setTextColor(INVALID_TEXT_COLOR);
            }
        });
        return box;
    }

    /** Parses an integer from the box, clamping to [{@code min}, {@code max}]; returns {@code defaultVal} on parse failure. */
    static int parseSafely(EditBox box, int defaultVal, int min, int max) {
        try {
            int v = Integer.parseInt(box.getValue());
            return Math.max(min, Math.min(max, v));
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    /** Right-aligned label drawn vertically centred against {@code box}. */
    static void drawLabel(GuiGraphicsExtractor graphics, Font font, String key, int x, EditBox box) {
        int y = box.getY() + (box.getHeight() - font.lineHeight) / 2;
        graphics.text(font, Component.translatable(key), x, y, 0xFFFFFFFF, true);
    }
}
