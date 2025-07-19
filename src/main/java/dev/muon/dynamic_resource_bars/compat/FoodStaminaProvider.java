package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.util.StaminaProvider;
import dev.muon.dynamic_resource_bars.util.PlatformUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

#if UPTO_20_1 && FABRIC
import moriyashiine.bewitchment.api.BewitchmentAPI;
import moriyashiine.bewitchment.common.registry.BWComponents;
import moriyashiine.bewitchment.api.component.BloodComponent;
#endif

public class FoodStaminaProvider implements StaminaProvider {
    
    @Override
    public float getCurrentStamina(Player player) {
        #if UPTO_20_1 && FABRIC
        if (PlatformUtil.isModLoaded("bewitchment") && BewitchmentAPI.isVampire(player, true)) {
            return BWComponents.BLOOD_COMPONENT.get(player).getBlood();
        }
        #endif
        return player.getFoodData().getFoodLevel();
    }
    
    @Override
    public float getMaxStamina(Player player) {
        #if UPTO_20_1 && FABRIC
        if (PlatformUtil.isModLoaded("bewitchment") && BewitchmentAPI.isVampire(player, true)) {
            return BloodComponent.MAX_BLOOD;
        }
        #endif
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
        #if UPTO_20_1 && FABRIC
        if (PlatformUtil.isModLoaded("bewitchment") && BewitchmentAPI.isVampire(player, true)) {
            return "stamina_bar_blood";
        }
        #endif
        
        // Check for various effects and states
        if (PlatformUtil.isModLoaded("farmersdelight") && hasNourishmentEffect(player)) {
            return "stamina_bar_nourished";
        }
        if (player.hasEffect(net.minecraft.world.effect.MobEffects.HUNGER)) {
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
    
    private boolean hasNourishmentEffect(Player player) {
        #if UPTO_20_1
            // 1.20.1 Forge: RegistryObject<MobEffect>
            // 1.20.1 Fabric: Supplier<MobEffect>
        var nourishmentEffect = vectorwing.farmersdelight.common.registry.ModEffects.NOURISHMENT.get();
        return player.hasEffect(nourishmentEffect);
        #elif NEWER_THAN_20_1
            // 1.21.1 Fabric/NeoForge - Holder<MobEffect>
        var nourishmentEffect = vectorwing.farmersdelight.common.registry.ModEffects.NOURISHMENT;
        return player.hasEffect(nourishmentEffect);
        #else
        return false;
        #endif
    }
} 