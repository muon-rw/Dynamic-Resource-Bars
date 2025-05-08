package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;

public class EditModeManager {
    private static boolean editModeEnabled = false;
    
    // For dragging the entire bar complex
    private static DraggableElement draggedElement = null;
    private static int dragStartX = 0; 
    private static int dragStartY = 0; 
    private static int initialElementXOffset = 0; // The bar's TotalXOffset 
    private static int initialElementYOffset = 0; // The bar's TotalYOffset

    // For focus mode and dragging sub-elements
    private static DraggableElement focusedElement = null; // Which bar complex is focused
    private static SubElementType draggedSubElement = null; // Which part of the focused bar is being dragged
    private static int subElementDragStartX = 0; // Mouse X when sub-element drag started
    private static int subElementDragStartY = 0; // Mouse Y when sub-element drag started
    private static int initialSubElementXOffset = 0; // The sub-element's specific X offset config (e.g., barXOffset)
    private static int initialSubElementYOffset = 0; // The sub-element's specific Y offset config (e.g., barYOffset)

    public static boolean isEditModeEnabled() {
        return editModeEnabled;
    }

    public static void toggleEditMode() {
        editModeEnabled = !editModeEnabled;
        if (!editModeEnabled) {
            clearDraggedElement(); 
            clearFocusedElement(); // Also clear focus when exiting edit mode
            // No explicit save call needed here; ModConfigSpec handles it.
        } else {
            // On entering edit mode, ensure states are clean
            clearDraggedElement();
            clearFocusedElement();
        }
        DynamicResourceBars.LOGGER.debug("Edit Mode toggled: {}", editModeEnabled ? "Enabled" : "Disabled");
    }

    // --- Whole Bar Dragging --- 
    public static DraggableElement getDraggedElement() {
        return draggedElement;
    }
    public static void setDraggedElement(DraggableElement element, int mouseX, int mouseY, int currentTotalXOffset, int currentTotalYOffset) {
        if (!editModeEnabled) return;
        // Ensure we are not in sub-element drag mode
        if (draggedSubElement != null || focusedElement != null) return; 
        draggedElement = element;
        dragStartX = mouseX;
        dragStartY = mouseY;
        initialElementXOffset = currentTotalXOffset;
        initialElementYOffset = currentTotalYOffset;
    }
    public static void clearDraggedElement() {
        draggedElement = null;
    }
    public static int getDragStartX() { return dragStartX; }
    public static int getDragStartY() { return dragStartY; }
    public static int getInitialElementXOffset() { return initialElementXOffset; }
    public static int getInitialElementYOffset() { return initialElementYOffset; }

    // --- Focus Mode & Sub-Element Dragging --- 
    public static DraggableElement getFocusedElement() {
        return focusedElement;
    }
    public static void setFocusedElement(DraggableElement element) {
        if (!editModeEnabled) return;
        focusedElement = element;
        clearDraggedElement(); // Can't drag whole bar when one is focused for sub-editing
        clearDraggedSubElement(); // Clear any previous sub-element drag
        DynamicResourceBars.LOGGER.debug("Focused element set to: {}", element);
    }
    public static void clearFocusedElement() {
        focusedElement = null;
        clearDraggedSubElement(); // If focus is cleared, sub-element drag is also cleared
        DynamicResourceBars.LOGGER.debug("Focused element cleared.");
    }

    public static SubElementType getDraggedSubElement() {
        return draggedSubElement;
    }
    public static void setDraggedSubElement(SubElementType subElement, int mouseX, int mouseY, int currentSubElementX, int currentSubElementY) {
        if (!editModeEnabled || focusedElement == null) return; // Must be in edit mode and have a focused bar
        draggedSubElement = subElement;
        subElementDragStartX = mouseX;
        subElementDragStartY = mouseY;
        initialSubElementXOffset = currentSubElementX;
        initialSubElementYOffset = currentSubElementY;
        DynamicResourceBars.LOGGER.debug("Dragging sub-element: {} of bar: {}", subElement, focusedElement);
    }
    public static void clearDraggedSubElement() {
        draggedSubElement = null;
    }
    public static int getSubElementDragStartX() { return subElementDragStartX; }
    public static int getSubElementDragStartY() { return subElementDragStartY; }
    public static int getInitialSubElementXOffset() { return initialSubElementXOffset; }
    public static int getInitialSubElementYOffset() { return initialSubElementYOffset; }
} 