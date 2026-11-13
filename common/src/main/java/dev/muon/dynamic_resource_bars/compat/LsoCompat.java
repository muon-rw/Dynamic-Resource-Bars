package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.platform.Services;
import net.minecraft.world.entity.player.Player;

/**
 * Bridge for Legendary Survival Overhaul (NeoForge-only). Common code calls into here without
 * referencing LSO classes; the NeoForge entrypoint registers {@link Provider} when LSO is loaded.
 */
public final class LsoCompat {

    public static final String MOD_ID = "legendarysurvivaloverhaul";

    private static Provider provider;

    private LsoCompat() {}

    public interface Provider {
        float getTemperatureScale(Player player);
        float getWetnessScale(Player player);
        boolean isFrozen(Player player);
        boolean isScorched(Player player);
        boolean isThirstActive(Player player);
        float getCurrentThirst(Player player);
        float getMaxThirst(Player player);
        boolean hasThirstEffect(Player player);
        boolean hasHeatThirstEffect(Player player);
    }

    public static void setProvider(Provider p) {
        provider = p;
    }

    public static boolean isLoaded() {
        return provider != null && Services.PLATFORM.isModLoaded(MOD_ID);
    }

    public static boolean hasThirstMod() {
        return Services.PLATFORM.isModLoaded(MOD_ID);
    }

    public static float getTemperatureScale(Player player) {
        return isLoaded() ? provider.getTemperatureScale(player) : 0f;
    }

    public static float getWetnessScale(Player player) {
        return isLoaded() ? provider.getWetnessScale(player) : 0f;
    }

    public static boolean isFrozen(Player player) {
        return isLoaded() && provider.isFrozen(player);
    }

    public static boolean isScorched(Player player) {
        return isLoaded() && provider.isScorched(player);
    }

    public static boolean isThirstActive(Player player) {
        return isLoaded() && provider.isThirstActive(player);
    }

    public static float getCurrentThirst(Player player) {
        return isLoaded() ? provider.getCurrentThirst(player) : 0f;
    }

    public static float getMaxThirst(Player player) {
        return isLoaded() ? provider.getMaxThirst(player) : 0f;
    }

    public static boolean hasThirstEffect(Player player) {
        return isLoaded() && provider.hasThirstEffect(player);
    }

    public static boolean hasHeatThirstEffect(Player player) {
        return isLoaded() && provider.hasHeatThirstEffect(player);
    }
}
