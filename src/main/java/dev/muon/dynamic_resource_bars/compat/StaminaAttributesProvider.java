package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.util.StaminaProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

#if FABRIC && NEWER_THAN_20_1
import com.github.theredbrain.staminaattributes.entity.StaminaUsingEntity;
#endif

#if FABRIC
import net.spell_engine.internals.casting.SpellCasterClient;
#endif

public class StaminaAttributesProvider implements StaminaProvider {
    
    @Override
    public float getCurrentStamina(Player player) {
        #if FABRIC && NEWER_THAN_20_1
        if (player instanceof StaminaUsingEntity staminaUsingEntity) {
            return (float) staminaUsingEntity.staminaattributes$getStamina();
        }
        #endif
        return 0;
    }
    
    @Override
    public float getMaxStamina(Player player) {
        #if FABRIC && NEWER_THAN_20_1
        if (player instanceof StaminaUsingEntity staminaUsingEntity) {
            return staminaUsingEntity.staminaattributes$getMaxStamina();
        }
        #endif
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
        #if FABRIC
        // Check if casting a spell (similar to mana attributes check)
        if (player instanceof SpellCasterClient spellCaster) {
            if (spellCaster.isCastingSpell()) {
                return true;
            }
        }
        #endif
        return false;
    }
    
    @Override
    public String getBarTexture(Player player, float currentValue) {
        // StaminaAttributes uses a generic stamina texture
        return "stamina_bar";
    }
    
    @Override
    public boolean shouldShowOverlays() {
        return false; // No food overlays for stamina attributes
    }
} 