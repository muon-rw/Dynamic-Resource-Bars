package dev.muon.dynamic_resource_bars.util;

import net.minecraft.client.Minecraft;

public class HUDPositioning {

    public enum BarPlacement {
        HEALTH,         // Health bar placement
        HUNGER,         // Stamina bar placement
        ABOVE_UTILITIES // Mana bar placement
    }

    public static Position getHealthAnchor() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return new Position(
                (screenWidth / 2) - 91, // Vanilla health bar X - this is the left edge, expecting usage of align-left
                screenHeight - 40       // Vanilla health bar Y (+1)
        );
    }

    public static Position getHungerAnchor() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return new Position(
                (screenWidth / 2) + 91, // Vanilla hunger bar X - this is the right edge, expecting usage of align-right
                screenHeight - 40       // Vanilla hunger bar Y (+1)
        );
    }

    public static Position getAboveUtilitiesAnchor() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return new Position(
                screenWidth / 2,        // Center X
                screenHeight - 65       // Above other hotbar elements -- Shifts
        );
    }
}
