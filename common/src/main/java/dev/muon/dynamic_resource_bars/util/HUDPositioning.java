package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
// BarRenderBehavior is in this same package (util), no import needed.
import net.minecraft.client.Minecraft;

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

    private static int vanillaHealthHeight = 9; // Default fallback
    private static int vanillaHungerHeight = 9; // Default fallback

    public static int getVanillaHealthHeight() { return vanillaHealthHeight; }
    public static void setVanillaHealthHeight(int v) { vanillaHealthHeight = v; }
    public static int getVanillaHungerHeight() { return vanillaHungerHeight; }
    public static void setVanillaHungerHeight(int v) { vanillaHungerHeight = v; }

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

    /** Above the health anchor on the left, offset by the rendered health-bar height. */
    public static Position getArmorAnchor() {
        int healthHeight = ModConfigManager.getClient().healthBarBehavior == BarRenderBehavior.CUSTOM
                ? ModConfigManager.getClient().healthBackgroundHeight
                : getVanillaHealthHeight();
        Position health = getHealthAnchor();
        return new Position(health.x(), health.y() - healthHeight - 1);
    }

    public static Position getHungerAnchor() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        return new Position(
                (screenWidth / 2) + 91,
                screenHeight - 40
        );
    }

    /** Above the hunger/stamina anchor on the right, offset by the rendered stamina-bar height. */
    public static Position getAirAnchor() {
        int staminaHeight = !StaminaBarBehavior.OFF.equals(ModConfigManager.getClient().staminaBarBehavior)
                ? ModConfigManager.getClient().staminaBackgroundHeight
                : getVanillaHungerHeight();
        Position hunger = getHungerAnchor();
        return new Position(hunger.x(), hunger.y() - staminaHeight - 1);
    }

    public static Position getAboveUtilitiesAnchor() {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        return new Position(
                screenWidth / 2,
                Minecraft.getInstance().getWindow().getGuiScaledHeight() - 65
        );
    }
}
