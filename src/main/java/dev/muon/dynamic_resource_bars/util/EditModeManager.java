package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;

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

    // For keyboard navigation
    private static DraggableElement keyboardSelectedElement = null; // Selected element for keyboard control in non-focus mode
    private static SubElementType keyboardSelectedSubElement = null; // Selected sub-element for keyboard control in focus mode

    // TODO: we can probably use lombok for most of these helpers
    public static boolean isEditModeEnabled() {
        return editModeEnabled;
    }

    public static void toggleEditMode() {
        editModeEnabled = !editModeEnabled;
        if (!editModeEnabled) {
            clearDraggedElement(); 
            clearFocusedElement(); // Also clear focus when exiting edit mode
            clearKeyboardSelectedElement();
            clearKeyboardSelectedSubElement();
            // No explicit save call needed here; ModConfigSpec handles it.
        } else {
            // On entering edit mode, ensure states are clean
            clearDraggedElement();
            clearFocusedElement();
            clearKeyboardSelectedElement();
            clearKeyboardSelectedSubElement();
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
        clearKeyboardSelectedElement(); // Clear element-level keyboard selection when entering focus mode
        clearKeyboardSelectedSubElement(); // Clear any previous sub-element selection
        DynamicResourceBars.LOGGER.debug("Focused element set to: {}", element);
    }
    public static void clearFocusedElement() {
        focusedElement = null;
        clearDraggedSubElement(); // If focus is cleared, sub-element drag is also cleared
        clearKeyboardSelectedSubElement(); // Clear sub-element keyboard selection when leaving focus mode
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

    // --- Keyboard Selection ---
    public static DraggableElement getKeyboardSelectedElement() {
        return keyboardSelectedElement;
    }
    
    public static void setKeyboardSelectedElement(DraggableElement element) {
        if (!editModeEnabled) return;
        keyboardSelectedElement = element;
        DynamicResourceBars.LOGGER.debug("Keyboard selected element: {}", element);
    }
    
    public static void clearKeyboardSelectedElement() {
        keyboardSelectedElement = null;
    }
    
    public static SubElementType getKeyboardSelectedSubElement() {
        return keyboardSelectedSubElement;
    }
    
    public static void setKeyboardSelectedSubElement(SubElementType subElement) {
        if (!editModeEnabled || focusedElement == null) return;
        keyboardSelectedSubElement = subElement;
        DynamicResourceBars.LOGGER.debug("Keyboard selected sub-element: {} of bar: {}", subElement, focusedElement);
    }
    
    public static void clearKeyboardSelectedSubElement() {
        keyboardSelectedSubElement = null;
    }
} 