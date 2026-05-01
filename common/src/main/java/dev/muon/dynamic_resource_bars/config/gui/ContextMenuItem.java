package dev.muon.dynamic_resource_bars.config.gui;

import net.minecraft.network.chat.Component;

import java.util.function.Consumer;

/** A single row in a {@link ContextMenu}. */
public class ContextMenuItem {

    private final Component label;
    private final Consumer<ContextMenuItem> action;
    private final boolean isTitle;
    private final int textColor;
    private final boolean enabled;

    public ContextMenuItem(Component label, Consumer<ContextMenuItem> action) {
        this(label, action, false, 0xFFFFFFFF, true);
    }

    public ContextMenuItem(Component label, Consumer<ContextMenuItem> action, int textColor) {
        this(label, action, false, textColor, true);
    }

    public ContextMenuItem(Component label, Consumer<ContextMenuItem> action, boolean enabled) {
        this(label, action, false, 0xFFFFFFFF, enabled);
    }

    /** Non-clickable header row, rendered with an accent color. */
    public static ContextMenuItem title(Component label) {
        return new ContextMenuItem(label, null, true, 0xFFAAAA00, true);
    }

    /** Empty spacer row. */
    public static ContextMenuItem separator() {
        return new ContextMenuItem(Component.empty(), null, true, 0xFFFFFFFF, false);
    }

    private ContextMenuItem(Component label, Consumer<ContextMenuItem> action, boolean isTitle, int textColor, boolean enabled) {
        this.label = label;
        this.action = action;
        this.isTitle = isTitle;
        this.textColor = textColor;
        this.enabled = enabled;
    }

    public Component label() { return label; }
    public boolean isTitle() { return isTitle; }
    public boolean isEnabled() { return enabled; }
    public int textColor() { return textColor; }
    public boolean isClickable() { return action != null && enabled && !isTitle; }

    public void execute() {
        if (isClickable()) action.accept(this);
    }
}
