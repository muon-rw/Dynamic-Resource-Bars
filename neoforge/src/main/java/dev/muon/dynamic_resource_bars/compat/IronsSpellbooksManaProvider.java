package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.provider.ManaProvider;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.gui.overlays.ManaBarOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;

public class IronsSpellbooksManaProvider implements ManaProvider {

    @Override
    public double getCurrentMana() {
        return ClientMagicData.getPlayerMana();
    }

    @Override
    public float getMaxMana() {
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            AttributeInstance maxManaAttribute = player.getAttribute(AttributeRegistry.MAX_MANA);
            if (maxManaAttribute != null) {
                return (float) maxManaAttribute.getValue();
            }
        }
        return 0;
    }

    @Override
    public float getReservedMana() {
        return 0;
    }

    @Override
    public long getGameTime() {
        if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.getGameTime();
        }
        return 0;
    }

    @Override
    public boolean shouldDisplayBarOverride(Player player) {
        return ManaBarOverlay.shouldShowManaBar(player);
    }

    @Override
    public boolean hasSpecificVisibilityLogic() {
        return true;
    }
}
