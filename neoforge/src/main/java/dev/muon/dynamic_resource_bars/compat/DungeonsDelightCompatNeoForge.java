package dev.muon.dynamic_resource_bars.compat;

import net.minecraft.world.entity.player.Player;
import net.yirmiri.dungeonsdelight.common.util.misc.RottenHeartManager;
import net.yirmiri.dungeonsdelight.core.registry.DDEffects;

public final class DungeonsDelightCompatNeoForge implements DungeonsDelightCompat.Provider {

    @Override
    public int getRottenHearts(Player player) {
        if (!player.hasEffect(DDEffects.ROTGUT)) {
            return 0;
        }
        return RottenHeartManager.get(player).getRottenHearts();
    }
}
