package dev.muon.dynamic_resource_bars.config.gui;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/**
 * Represents a single item in a context menu.
 */
public class ContextMenuItem {
    private final Component label;
    private final Consumer<ContextMenuItem> action;
    private final boolean isTitle;
    private final int textColor;
    private boolean enabled;
    
    /**
     * Creates a regular menu item with an action
     */
    public ContextMenuItem(Component label, Consumer<ContextMenuItem> action) {
        this(label, action, false, 0xFFFFFF, true);
    }
    
    /**
     * Creates a menu item with specific color
     */
    public ContextMenuItem(Component label, Consumer<ContextMenuItem> action, int textColor) {
        this(label, action, false, textColor, true);
    }
    
    /**
     * Creates a menu item that can be disabled
     */
    public ContextMenuItem(Component label, Consumer<ContextMenuItem> action, boolean enabled) {
        this(label, action, false, 0xFFFFFF, enabled);
    }
    
    /**
     * Creates a title (non-clickable header)
     */
    public static ContextMenuItem title(Component label) {
        return new ContextMenuItem(label, null, true, 0xAAAA00, true);
    }
    
    /**
     * Creates a separator (empty space)
     */
    public static ContextMenuItem separator() {
        return new ContextMenuItem(Component.empty(), null, true, 0xFFFFFF, false);
    }
    
    private ContextMenuItem(Component label, Consumer<ContextMenuItem> action, boolean isTitle, int textColor, boolean enabled) {
        this.label = label;
        this.action = action;
        this.isTitle = isTitle;
        this.textColor = textColor;
        this.enabled = enabled;
    }
    
    public Component getLabel() {
        return label;
    }
    
    public boolean isTitle() {
        return isTitle;
    }
    
    public boolean isEnabled() {
        return enabled;
    }
    
    public int getTextColor() {
        return textColor;
    }
    
    public void execute() {
        if (action != null && enabled && !isTitle) {
            action.accept(this);
        }
    }
    
    public boolean isClickable() {
        return action != null && enabled && !isTitle;
    }
}

