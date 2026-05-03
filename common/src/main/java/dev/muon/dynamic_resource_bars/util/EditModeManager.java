package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.Constants;

/**
 * Tiny static state holder for the HUD editor: whether edit mode is on, which bar is the
 * current focus, and whether we're in sub-element-editor mode within that focus. Drag and
 * keyboard-selection state lives in {@code HudEditorScreen} now — this class is the single
 * source of truth for "what should the renderer do differently while the editor is open?"
 */
public class EditModeManager {

    private static boolean editModeEnabled = false;
    private static boolean focusMode = false;
    private static DraggableElement focusedElement = null;

    public static boolean isEditModeEnabled() {
        return editModeEnabled;
    }

    public static void toggleEditMode() {
        editModeEnabled = !editModeEnabled;
        if (!editModeEnabled) {
            clearFocusedElement();
            focusMode = false;
        }
        Constants.LOG.debug("Edit Mode toggled: {}", editModeEnabled ? "Enabled" : "Disabled");
    }

    public static DraggableElement getFocusedElement() {
        return focusedElement;
    }

    public static void setFocusedElement(DraggableElement element) {
        if (!editModeEnabled) return;
        focusedElement = element;
    }

    public static void clearFocusedElement() {
        focusedElement = null;
    }

    /**
     * Sub-element editor mode. When true, the renderer suppresses the white complex-rect
     * outline on the focused bar (clutter reduction); the editor screen also uses this to
     * gate sub-element hit-testing and keyboard resize.
     */
    public static boolean isFocusMode() {
        return focusMode;
    }

    public static void setFocusMode(boolean v) {
        focusMode = v;
    }
}
