package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.platform.Services;
import net.minecraft.world.entity.player.Player;

public final class DungeonsDelightCompat {

    public static final String MOD_ID = "dungeonsdelight";

    private static Provider provider;

    private DungeonsDelightCompat() {}

    public interface Provider {
        int getRottenHearts(Player player);
    }

    public static void setProvider(Provider p) {
        provider = p;
    }

    public static int getRottenHearts(Player player) {
        return player != null && provider != null && Services.PLATFORM.isModLoaded(MOD_ID)
                ? provider.getRottenHearts(player)
                : 0;
    }
}
