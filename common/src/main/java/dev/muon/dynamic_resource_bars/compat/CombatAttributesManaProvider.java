package dev.muon.dynamic_resource_bars.compat;

import dev.muon.combat_attributes.resource.PlayerResources;
import dev.muon.dynamic_resource_bars.provider.ManaProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Mana source backed by Combat Attributes' {@code PlayerResources}.
 *
 * <p>Loaded only when {@code combat_attributes} is on the runtime classpath. See the
 * {@code Services.PLATFORM.isModLoaded("combat_attributes")} guard in
 * {@link ManaProviderManager#initialize()}.
 */
public class CombatAttributesManaProvider implements ManaProvider {

    @Override
    public double getCurrentMana() {
        Player p = Minecraft.getInstance().player;
        return p == null ? 0 : PlayerResources.getMana(p);
    }

    @Override
    public float getMaxMana() {
        Player p = Minecraft.getInstance().player;
        return p == null ? 0 : PlayerResources.getMaxMana(p);
    }

    @Override
    public float getReservedMana() {
        return 0; // Combat Attributes has no reserved-mana concept.
    }

    @Override
    public long getGameTime() {
        if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.getGameTime();
        }
        return 0;
    }
}
