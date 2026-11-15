package dev.muon.dynamic_resource_bars.compat;

import dev.sterner.witchery.core.api.possession.Possessable;
import dev.sterner.witchery_forbidden_magic.feature.possession.PossessionComponentAttachment;
import dev.sterner.witchery_forbidden_magic.feature.possession.SoulFormPlayerAttachment;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

public final class WitcherySoulFormCompatNeoForge implements WitcherySoulFormCompat.Provider {

    @Override
    public boolean isSoulFormActive(Player player) {
        return SoulFormPlayerAttachment.getData(player).isSoulFormActive();
    }

    @Override
    public boolean shouldHideFood(Player player) {
        if (!SoulFormPlayerAttachment.getData(player).isHosted()) {
            return false;
        }
        Mob host = PossessionComponentAttachment.INSTANCE.get(player).getHost();
        return !(host instanceof Possessable possessable) || !possessable.isRegularEater();
    }
}
