package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.platform.Services;
import net.minecraft.world.entity.player.Player;

/**
 * Bridge for vampire blood pools (e.g. Witchery). When active and the player is a vampire,
 * {@link FoodStaminaProvider} switches from vanilla hunger to blood.
 */
public final class VampireBloodCompat {

    public static final String WITCHERY_MOD_ID = "witchery";

    private static Provider provider;

    private VampireBloodCompat() {}

    public interface Provider {
        boolean isVampire(Player player);
        float getCurrentBlood(Player player);
        float getMaxBlood(Player player);
    }

    public static void setProvider(Provider p) {
        provider = p;
    }

    public static boolean isLoaded() {
        return provider != null && Services.PLATFORM.isModLoaded(WITCHERY_MOD_ID);
    }

    public static boolean isVampire(Player player) {
        return player != null && isLoaded() && provider.isVampire(player);
    }

    public static float getCurrentBlood(Player player) {
        return isLoaded() ? provider.getCurrentBlood(player) : 0f;
    }

    public static float getMaxBlood(Player player) {
        return isLoaded() ? provider.getMaxBlood(player) : 0f;
    }
}
