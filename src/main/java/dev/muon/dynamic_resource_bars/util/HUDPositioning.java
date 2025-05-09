package dev.muon.dynamic_resource_bars.util;

import net.minecraft.client.Minecraft;
import dev.muon.dynamic_resource_bars.foundation.config.ModConfigManager;

public class HUDPositioning {
    public enum BarPlacement {
        HEALTH(AnchorSide.LEFT),
        ARMOR(AnchorSide.LEFT),
        HUNGER(AnchorSide.RIGHT),
        AIR(AnchorSide.RIGHT),
        ABOVE_UTILITIES(AnchorSide.LEFT);

        private final AnchorSide side;

        BarPlacement(AnchorSide side) {
            this.side = side;
        }

        public AnchorSide getSide() {
            return side;
        }
    }

    public enum AnchorSide {
        LEFT,
        RIGHT
    }

    public static Position getPositionFromAnchor(BarPlacement anchor) {
        return switch (anchor) {
            case HEALTH -> getHealthAnchor();
            case ARMOR -> getArmorAnchor();
            case HUNGER -> getHungerAnchor();
            case AIR -> getAirAnchor();
            case ABOVE_UTILITIES -> getAboveUtilitiesAnchor();
        };
    }

    public static Position getHealthAnchor() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return new Position(
                (screenWidth / 2) - 91,
                screenHeight - 40
        );
    }

    public static Position getArmorAnchor() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int healthBarHeight = ModConfigManager.getClient().enableHealthBar.get() ? ModConfigManager.getClient().healthBackgroundHeight.get() : 0;
        return new Position(
                (screenWidth / 2) - 91,
                getHealthAnchor().y() - healthBarHeight - 1
        );
    }

    public static Position getHungerAnchor() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return new Position(
                (screenWidth / 2) + 91,
                screenHeight - 40
        );
    }

    public static Position getAirAnchor() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int staminaBarHeight = ModConfigManager.getClient().enableStaminaBar.get() ? ModConfigManager.getClient().staminaBackgroundHeight.get() : 0;
        return new Position(
                (screenWidth / 2) + 91,
                getHungerAnchor().y() - staminaBarHeight - 1
        );
    }

    public static Position getAboveUtilitiesAnchor() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return new Position(
                screenWidth / 2,
                screenHeight - 65   // TODO: reimplement shift
        );
    }
}
