package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.platform.Services;
import net.minecraft.world.entity.player.Player;

public final class WitcherySoulFormCompat {

    public static final String MOD_ID = "witchery_forbidden_magic";

    private static Provider provider;

    private WitcherySoulFormCompat() {}

    public interface Provider {
        boolean isSoulFormActive(Player player);
        boolean shouldHideFood(Player player);
    }

    public static void setProvider(Provider p) {
        provider = p;
    }

    public static boolean isSoulFormActive(Player player) {
        return player != null
                && provider != null
                && Services.PLATFORM.isModLoaded(MOD_ID)
                && provider.isSoulFormActive(player);
    }

    public static boolean shouldHideFood(Player player) {
        return player != null
                && provider != null
                && Services.PLATFORM.isModLoaded(MOD_ID)
                && provider.shouldHideFood(player);
    }
}
