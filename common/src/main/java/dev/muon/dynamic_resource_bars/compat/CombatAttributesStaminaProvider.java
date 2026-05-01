package dev.muon.dynamic_resource_bars.compat;

import dev.muon.combat_attributes.resource.PlayerResources;
import dev.muon.dynamic_resource_bars.provider.StaminaProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Stamina source backed by Combat Attributes' {@code PlayerResources}.
 *
 * <p>Loaded only when {@code combat_attributes} is on the runtime classpath — see the
 * {@code Services.PLATFORM.isModLoaded("combat_attributes")} guard in
 * {@link StaminaProviderManager#initialize()}. As long as the manager doesn't reference
 * this class statically, the JVM never resolves {@link PlayerResources} when CA is absent,
 * so the optional dependency stays safe.
 */
public class CombatAttributesStaminaProvider implements StaminaProvider {

    @Override
    public float getCurrentStamina(Player player) {
        return PlayerResources.getStamina(player);
    }

    @Override
    public float getMaxStamina(Player player) {
        return PlayerResources.getMaxStamina(player);
    }

    @Override
    public long getGameTime() {
        if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.getGameTime();
        }
        return 0;
    }
}
