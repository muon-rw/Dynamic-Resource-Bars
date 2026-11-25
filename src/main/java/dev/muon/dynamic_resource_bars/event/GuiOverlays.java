package dev.muon.dynamic_resource_bars.event;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.render.AirBarRenderer;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.EditModeManager;
import dev.muon.dynamic_resource_bars.util.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.util.StaminaBarBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

public class GuiOverlays {

    public static final IGuiOverlay RESOURCE_BARS = (gui, graphics, partialTick, screenWidth, screenHeight) -> {
        renderBars(gui, graphics, partialTick, false);
    };

    public static final IGuiOverlay RESOURCE_BARS_ABOVE_XP = (gui, graphics, partialTick, screenWidth, screenHeight) -> {
        renderBars(gui, graphics, partialTick, true);
    };

    private static void renderBars(ForgeGui gui, GuiGraphics graphics, float partialTick, boolean aboveXpPass) {
        Minecraft minecraft = gui.getMinecraft();
        if (minecraft.options.hideGui) return;
        if (!gui.shouldDrawSurvivalElements() && !EditModeManager.isEditModeEnabled()) return;

        LocalPlayer player = minecraft.player;
        if (player == null) return;

        ClientConfig config = ModConfigManager.getClient();

        if (config.enableHealthBar) {
            if (config.healthAboveXpBar == aboveXpPass) {
                HealthBarRenderer.render(graphics, player, player.getMaxHealth(), player.getHealth(),
                        (int) player.getAbsorptionAmount(), partialTick);
            }
            if (!aboveXpPass) {
                gui.leftHeight += config.healthBackgroundHeight;
            }
        }

        if (config.staminaBarBehavior != StaminaBarBehavior.OFF) {
            if (config.staminaAboveXpBar == aboveXpPass) {
                StaminaBarRenderer.render(graphics, player, partialTick);
            }
            if (!aboveXpPass) {
                gui.rightHeight += config.staminaBackgroundHeight;
            }
        }

        if (config.armorBarBehavior == BarRenderBehavior.CUSTOM) {
            if (config.armorAboveXpBar == aboveXpPass) {
                ArmorBarRenderer.render(graphics, player);
            }
            if (!aboveXpPass) {
                gui.leftHeight += config.armorBackgroundHeight;
            }
        }

        if (config.airBarBehavior == BarRenderBehavior.CUSTOM) {
            if (config.airAboveXpBar == aboveXpPass) {
                AirBarRenderer.render(graphics, player, partialTick);
            }
            if (!aboveXpPass) {
                gui.rightHeight += config.airBackgroundHeight;
            }
        }

        if (config.manaBarBehavior != ManaBarBehavior.OFF && config.manaAboveXpBar == aboveXpPass) {
            ManaBarRenderer.renderFromOverlay(graphics, partialTick, player);
        }
    }
}