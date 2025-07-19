package dev.muon.dynamic_resource_bars.compat;
#if UPTO_20_1 && FABRIC
import moriyashiine.bewitchment.api.BewitchmentAPI;
import net.minecraft.world.entity.player.Player;
#endif
public class BewitchmentCompat {
    #if UPTO_20_1 && FABRIC
    public static boolean isVampire(Player player) {
        return BewitchmentAPI.isVampire(player, true);
    }
    #endif
}
