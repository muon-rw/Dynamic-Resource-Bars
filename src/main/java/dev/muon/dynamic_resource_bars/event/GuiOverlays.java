package dev.muon.dynamic_resource_bars.event;

import dev.muon.dynamic_resource_bars.foundation.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
#if FORGE
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
public class GuiOverlays {
    public static final IGuiOverlay RESOURCE_BARS = (ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) -> {
        Minecraft minecraft = gui.getMinecraft();
        if (minecraft.options.hideGui) return;
        if (!gui.shouldDrawSurvivalElements()) return;

        var player = minecraft.player;
        if (player == null) return;

        if (ModConfigManager.getClient().enableHealthBar.get()) {
            HealthBarRenderer.render(graphics, player, player.getMaxHealth(), player.getHealth(), (int) player.getAbsorptionAmount(), partialTick);
            gui.leftHeight += ModConfigManager.getClient().healthBackgroundHeight.get() + 1;
        }
        if (ModConfigManager.getClient().enableStaminaBar.get()) {
            StaminaBarRenderer.render(graphics, player, partialTick);
            gui.rightHeight += ModConfigManager.getClient().staminaBackgroundHeight.get() + 1;
        }

        if (ModConfigManager.getClient().enableArmorBar.get()) {
            // ArmorBarRenderer.render(graphics, player); // Omitted as not implemented
            
            // Only adjust height if we are not completely hiding the bar
            // (i.e., if enableArmorBar is true, and hideArmorBar is false)
            if (!ModConfigManager.getClient().hideArmorBar.get()) {
                gui.leftHeight += ModConfigManager.getClient().armorBackgroundHeight.get() + 1;
            }
        }

    };
}
#endif