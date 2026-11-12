package dev.muon.dynamic_resource_bars.harness;

import dev.muon.dynamic_resource_bars.config.gui.HudEditorScreen;
import dev.muon.dynamic_resource_bars.config.gui.ManualSizePositionScreen;
import dev.muon.dynamic_resource_bars.config.gui.ModConfigScreen;
import dev.muon.dynamic_resource_bars.config.gui.SubElementConfigScreen;
import dev.muon.dynamic_resource_bars.config.gui.ToggleLayersScreen;
import dev.muon.dynamic_resource_bars.util.DraggableElement;
import dev.muon.dynamic_resource_bars.util.SubElementType;
import net.minecraft.client.gui.screens.Screen;

import java.util.Locale;

final class HarnessScreens {

    static final String USAGE = "hud | hud_editor | config | toggle_layers:<ELEMENT> | manual_size_position:<ELEMENT> | sub_element:<ELEMENT>:<SUB>";

    private HarnessScreens() {}

    /** Returns null for the bare HUD. */
    static Screen create(String spec) {
        String[] parts = spec.split(":");
        return switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "hud" -> null;
            case "hud_editor" -> new HudEditorScreen(null);
            case "config" -> new ModConfigScreen(null);
            case "toggle_layers" -> new ToggleLayersScreen(null, element(parts, spec));
            case "manual_size_position" -> new ManualSizePositionScreen(null, element(parts, spec));
            case "sub_element" -> {
                if (parts.length < 3) throw new IllegalArgumentException("sub_element needs <ELEMENT>:<SUB>, got '" + spec + "'");
                yield new SubElementConfigScreen(null, element(parts, spec), SubElementType.valueOf(parts[2].toUpperCase(Locale.ROOT)));
            }
            default -> throw new IllegalArgumentException("Unknown screen '" + spec + "'. Expected " + USAGE);
        };
    }

    static boolean showsHud(String spec) {
        String kind = spec.split(":")[0].toLowerCase(Locale.ROOT);
        return kind.equals("hud") || kind.equals("hud_editor");
    }

    private static DraggableElement element(String[] parts, String spec) {
        if (parts.length < 2) throw new IllegalArgumentException("Screen '" + spec + "' needs an element, like toggle_layers:HEALTH_BAR");
        return DraggableElement.valueOf(parts[1].toUpperCase(Locale.ROOT));
    }
}
