package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.platform.Services;
import net.minecraft.world.entity.player.Player;

public final class WitcheryAbilityCompat {

    private static Provider provider;

    private WitcheryAbilityCompat() {}

    public interface Provider {
        boolean hasSelectedAbility(Player player);
    }

    public static void setProvider(Provider p) {
        provider = p;
    }

    public static boolean hasSelectedAbility(Player player) {
        return player != null
                && provider != null
                && Services.PLATFORM.isModLoaded(VampireBloodCompat.WITCHERY_MOD_ID)
                && provider.hasSelectedAbility(player);
    }
}
