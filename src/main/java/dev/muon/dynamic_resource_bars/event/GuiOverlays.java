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

// 1.20.1 Forge Only. See CommonEvents for 1.21.1, GuiMixin for 1.20.1 Fabric
public class GuiOverlays {
    public static final IGuiOverlay RESOURCE_BARS = (ForgeGui gui, GuiGraphics graphics, float partialTick, int screenWidth, int screenHeight) -> {
        Minecraft minecraft = gui.getMinecraft();
        if (minecraft.options.hideGui) return;
        if (!gui.shouldDrawSurvivalElements()) return;

        var player = minecraft.player;
        if (player == null) return;

        var config = ModConfigManager.getClient();

        if (config.enableHealthBar) {
            HealthBarRenderer.render(graphics, player, player.getMaxHealth(), player.getHealth(), (int) player.getAbsorptionAmount(), partialTick);
            gui.leftHeight += config.healthBackgroundHeight + 1;
        }
        if (config.enableStaminaBar) {
            StaminaBarRenderer.render(graphics, player, partialTick);
            gui.rightHeight += config.staminaBackgroundHeight + 1;
        }

        if (config.armorBarBehavior == BarRenderBehavior.CUSTOM) {
            ArmorBarRenderer.render(graphics, player);
            gui.leftHeight += config.armorBackgroundHeight + 1;
        }

        if (config.airBarBehavior == BarRenderBehavior.CUSTOM) {
            AirBarRenderer.render(graphics, player, partialTick);
            gui.rightHeight += config.airBackgroundHeight + 1;
        }

    };
}
#endif