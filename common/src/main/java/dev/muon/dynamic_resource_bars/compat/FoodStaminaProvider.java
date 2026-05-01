package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.platform.Services;
import dev.muon.dynamic_resource_bars.provider.StaminaProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class FoodStaminaProvider implements StaminaProvider {

    @Override
    public float getCurrentStamina(Player player) {
        return player.getFoodData().getFoodLevel();
    }

    @Override
    public float getMaxStamina(Player player) {
        return 20.0f;
    }

    @Override
    public long getGameTime() {
        if (Minecraft.getInstance().level != null) {
            return Minecraft.getInstance().level.getGameTime();
        }
        return 0;
    }

    @Override
    public String getBarTexture(Player player, float currentValue) {
        if (Services.PLATFORM.isModLoaded(FarmersDelightCompat.MOD_ID) && FarmersDelightCompat.hasNourishment(player)) {
            return "stamina_bar_nourished";
        }
        if (player.hasEffect(MobEffects.HUNGER)) {
            return "stamina_bar_hunger";
        }
        if (currentValue <= 6.0f) {
            return "stamina_bar_critical";
        }
        return "stamina_bar";
    }

    @Override
    public boolean shouldShowOverlays() {
        return true; // Food provider should show saturation overlays
    }
}
