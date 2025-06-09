package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.util.ManaProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

#if FABRIC && NEWER_THAN_20_1
import com.github.theredbrain.manaattributes.entity.ManaUsingEntity;
#endif

#if FABRIC
import net.spell_engine.internals.casting.SpellCasterClient;
#endif

public class ManaAttributesManaProvider implements ManaProvider {
    
    @Override
    public double getCurrentMana() {
        #if FABRIC && NEWER_THAN_20_1
        Player player = Minecraft.getInstance().player;
        if (player != null && player instanceof ManaUsingEntity manaUsingEntity) {
            return manaUsingEntity.manaattributes$getMana();
        }
        #endif
        return 0;
    }
    
    @Override
    public float getMaxMana() {
        #if FABRIC && NEWER_THAN_20_1
        Player player = Minecraft.getInstance().player;
        if (player != null && player instanceof ManaUsingEntity manaUsingEntity) {
            return manaUsingEntity.manaattributes$getMaxMana();
        }
        #endif
        return 0;
    }
    
    @Override
    public float getReservedMana() {
        #if FABRIC && NEWER_THAN_20_1
        Player player = Minecraft.getInstance().player;
        if (player != null && player instanceof ManaUsingEntity manaUsingEntity) {
            // Mana Attributes uses reserved mana as a percentage
            return manaUsingEntity.manaattributes$getReservedMana() / 100f;
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
        if (player instanceof SpellCasterClient spellCaster) {
            if (spellCaster.isCastingSpell()) {
                return true; 
            }
        }
        #endif
        return false; 
    }
} 