package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.util.ManaProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

#if FABRIC && UPTO_20_1
import com.cleannrooster.rpgmana.api.ManaInterface;
#endif

#if FABRIC
import net.spell_engine.internals.casting.SpellCasterClient;
#endif

public class RPGManaManaProvider implements ManaProvider {
    
    @Override
    public double getCurrentMana() {
        #if FABRIC && UPTO_20_1
        Player player = Minecraft.getInstance().player;
        if (player != null && player instanceof ManaInterface manaInterface) {
            return manaInterface.getMana();
        }
        #endif
        return 0;
    }
    
    @Override
    public float getMaxMana() {
        #if FABRIC && UPTO_20_1
        Player player = Minecraft.getInstance().player;
        if (player != null && player instanceof ManaInterface manaInterface) {
            return (float) manaInterface.getMaxMana();
        }
        #endif
        return 0;
    }
    
    @Override
    public float getReservedMana() {
        // RPGMana doesn't have reserved mana concept
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