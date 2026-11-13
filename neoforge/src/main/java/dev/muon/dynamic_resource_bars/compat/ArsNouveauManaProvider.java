package dev.muon.dynamic_resource_bars.compat;

import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.client.ClientInfo;
import com.hollingsworth.arsnouveau.client.gui.GuiManaHUD;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.muon.dynamic_resource_bars.provider.ManaProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public class ArsNouveauManaProvider implements ManaProvider {

    @Override
    public double getCurrentMana() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return 0.0;
        IManaCap manaCap = CapabilityRegistry.getMana(player);
        return manaCap != null ? manaCap.getCurrentMana() : 0.0;
    }

    @Override
    public float getMaxMana() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return 0;
        IManaCap manaCap = CapabilityRegistry.getMana(player);
        return manaCap != null ? manaCap.getMaxMana() : 0;
    }

    @Override
    public float getReservedMana() {
        return ClientInfo.reservedOverlayMana;
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
        return Minecraft.getInstance().player != null && GuiManaHUD.shouldDisplayBar();
    }

    @Override
    public boolean hasSpecificVisibilityLogic() {
        return true;
    }
}
