package dev.muon.dynamic_resource_bars.event;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.render.AirBarRenderer;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
#if FORGE
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

// For 1.20.1 Forge
public class GuiOverlays {
    public static final IGuiOverlay RESOURCE_BARS = (ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) -> {
        Minecraft minecraft = gui.getMinecraft();
        if (minecraft.options.hideGui) return;
        if (!gui.shouldDrawSurvivalElements()) return;

        var player = minecraft.player;
        if (player == null) return;

        var config = ModConfigManager.getClient();

        if (config.enableHealthBar.get()) {
            HealthBarRenderer.render(graphics, player, player.getMaxHealth(), player.getHealth(), (int) player.getAbsorptionAmount(), partialTick);
            gui.leftHeight += config.healthBackgroundHeight.get() + 1;
        }
        if (config.enableStaminaBar.get()) {
            StaminaBarRenderer.render(graphics, player, partialTick);
            gui.rightHeight += config.staminaBackgroundHeight.get() + 1;
        }

        ArmorBarRenderer.render(graphics, player);
        if (config.armorBarBehavior.get() == BarRenderBehavior.CUSTOM) {
            gui.leftHeight += config.armorBackgroundHeight.get() + 1;
        }

        AirBarRenderer.render(graphics, player);
        if (config.airBarBehavior.get() == BarRenderBehavior.CUSTOM) {
            gui.rightHeight += config.airBackgroundHeight.get() + 1;
        }

    };
}
#endif