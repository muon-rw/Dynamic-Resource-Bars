package dev.muon.dynamic_resource_bars.compat;

#if FORGELIKE
import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.client.ClientInfo;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.hollingsworth.arsnouveau.client.gui.GuiManaHUD;
#endif
import dev.muon.dynamic_resource_bars.util.ManaProvider;
import dev.muon.dynamic_resource_bars.util.PlatformUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
#if FORGE
import net.minecraftforge.common.util.LazyOptional;
#endif

public class ArsNouveauManaProvider implements ManaProvider {
    
    @Override
    public double getCurrentMana() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return 0.0;
        }

        // Totally redundant but prevents unreachable statement compile errors
        if (PlatformUtil.isModLoaded( "ars_nouveau")) {
        #if NEO
        IManaCap manaCap = CapabilityRegistry.getMana(player);
        if (manaCap != null) {
            return manaCap.getCurrentMana();
        }
        #elif FORGE
        LazyOptional<IManaCap> manaCapLazyOpt = CapabilityRegistry.getMana(player);
        return manaCapLazyOpt.map(IManaCap::getCurrentMana).orElse(0.0);
        #endif
        }
        return 0;
    }
    
    @Override
    public float getMaxMana() {
        Player player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }
        // Totally redundant but prevents unreachable statement compile errors
        if (PlatformUtil.isModLoaded( "ars_nouveau")) {
        #if NEO
        IManaCap manaCap = CapabilityRegistry.getMana(player);
        if (manaCap != null) {
            return manaCap.getMaxMana();
        }
        #elif FORGE
        LazyOptional<IManaCap> manaCapLazyOpt = CapabilityRegistry.getMana(player);
        return manaCapLazyOpt.map(IManaCap::getMaxMana).orElse(0);
        #endif
        }
        return 0;
    }
    
    @Override
    public float getReservedMana() {
        // Totally redundant but prevents unreachable statement compile errors
        if (PlatformUtil.isModLoaded( "ars_nouveau")) {
        #if FORGELIKE
        return ClientInfo.reservedOverlayMana;
        #endif
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
    public boolean shouldDisplayBarOverride(Player player) {
        #if FORGELIKE
        if (Minecraft.getInstance().player != null) { // GuiManaHUD.shouldDisplayBar() internally uses Minecraft.getInstance().player
            return GuiManaHUD.shouldDisplayBar();
        }
        #endif
        return true; // Default if not FORGELIKE or player is null
    }

    @Override
    public boolean hasSpecificVisibilityLogic() {
        return true; // Ars Nouveau has its own specific logic for when the bar should be displayed.
    }
} 