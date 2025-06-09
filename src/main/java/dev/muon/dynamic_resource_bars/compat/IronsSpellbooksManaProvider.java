package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.util.ManaProvider;
import dev.muon.dynamic_resource_bars.util.PlatformUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
#if FORGELIKE
import io.redspace.ironsspellbooks.player.ClientMagicData;
import static io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA;
import io.redspace.ironsspellbooks.gui.overlays.ManaBarOverlay;
#endif
#if NEO
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
#endif

public class IronsSpellbooksManaProvider implements ManaProvider {

    @Override
    public double getCurrentMana() {
        // Totally redundant but prevents unreachable statement compile errors
        if (PlatformUtil.isModLoaded( "irons_spellbooks")) {
        #if FORGELIKE
        return ClientMagicData.getPlayerMana();
        #endif
        }
        return 0;
    }
    
    @Override
    public float getMaxMana() {
        #if FORGELIKE
        Player player = Minecraft.getInstance().player;
        if (player != null) {
            var maxManaAttribute = player.getAttribute(MAX_MANA#if FORGE .get()#endif);
            if (maxManaAttribute != null) {
                return (float) maxManaAttribute.getValue();
            }
        }
        #endif
        return 0;
    }
    
    @Override
    public float getReservedMana() {
        // Iron's Spellbooks doesn't have reserved mana concept
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
        #if FORGELIKE
        if (Minecraft.getInstance().player != null) {
            return ManaBarOverlay.shouldShowManaBar(player);
        }
        #endif
        return true;
    }

    @Override
    public boolean hasSpecificVisibilityLogic() {
        return true;
    }
} 