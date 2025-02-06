package dev.muon.dynamic_resource_bars.event;

import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
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

        if (AllConfigs.client().enableHealthBar.get()) {
            HealthBarRenderer.render(graphics, player, player.getMaxHealth(), player.getHealth(), (int) player.getAbsorptionAmount(), partialTick);
            gui.leftHeight += AllConfigs.client().healthBackgroundHeight.get() + 1;
        }
        if (AllConfigs.client().enableStaminaBar.get()) {
            StaminaBarRenderer.render(graphics, player, partialTick);
            gui.rightHeight += AllConfigs.client().staminaBackgroundHeight.get() + 1;
        }

        if (AllConfigs.client().enableArmorBar.get()) {
            ArmorBarRenderer.render(graphics, player);
            gui.leftHeight += AllConfigs.client().armorBackgroundHeight.get() + 1;
        }

    };
}
#endif