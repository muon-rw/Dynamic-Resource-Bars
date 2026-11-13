package dev.muon.dynamic_resource_bars.compat;

import com.github.theredbrain.staminaattributes.entity.StaminaUsingEntity;
import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.provider.StaminaProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.spell_engine.internals.casting.SpellCaster;

public class StaminaAttributesProvider implements StaminaProvider {

    @Override
    public float getCurrentStamina(Player player) {
        if (player instanceof StaminaUsingEntity staminaUsingEntity) {
            return staminaUsingEntity.staminaattributes$getStamina();
        }
        return 0;
    }

    @Override
    public float getMaxStamina(Player player) {
        if (player instanceof StaminaUsingEntity staminaUsingEntity) {
            return staminaUsingEntity.staminaattributes$getMaxStamina();
        }
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
    public boolean forceShowBarConditions(Player player) {
        return Services.PLATFORM.isModLoaded("spell_engine")
                && player instanceof SpellCaster.Client spellCaster
                && spellCaster.isCastingSpell();
    }

    @Override
    public String getBarTexture(Player player, float currentValue) {
        return "stamina_bar";
    }

    @Override
    public boolean shouldShowOverlays() {
        return false;
    }
}
