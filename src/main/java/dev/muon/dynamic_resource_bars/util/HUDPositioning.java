package dev.muon.dynamic_resource_bars.util;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;

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
        
        public BarPlacement getNext() {
            BarPlacement[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public enum AnchorSide {
        LEFT,
        RIGHT
    }

    @Setter
    @Getter
    private static int vanillaHealthHeight = 9; // Default fallback

    @Setter
    @Getter
    private static int vanillaHungerHeight = 9; // Default fallback

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
        int healthHeight = ModConfigManager.getClient().enableHealthBar ?
            ModConfigManager.getClient().healthBackgroundHeight :
                getVanillaHealthHeight();
        return new Position(
                (screenWidth / 2) - 91,
                getHealthAnchor().y() - healthHeight - 1
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
        int staminaHeight = !(ModConfigManager.getClient().staminaBarBehavior.equals(StaminaBarBehavior.OFF)) ?
            ModConfigManager.getClient().staminaBackgroundHeight : 
            getVanillaHungerHeight();
        return new Position(
                (screenWidth / 2) + 91,
                getHungerAnchor().y() - staminaHeight - 1
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
