package dev.muon.dynamic_resource_bars.client;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;

public final class NeoForgeCompatSuppression {

    private static final ResourceLocation IRONS_MANA = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "mana_overlay");
    private static final ResourceLocation ARS_MANA = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "mana_hud");
    private static final ResourceLocation RPGMANA_HUD = ResourceLocation.fromNamespaceAndPath("rpgmana", "mana_hud");
    private static final ResourceLocation FD_COMFORT = ResourceLocation.fromNamespaceAndPath("farmersdelight", "comfort");
    private static final ResourceLocation FD_NOURISHMENT = ResourceLocation.fromNamespaceAndPath("farmersdelight", "nourishment");

    private NeoForgeCompatSuppression() {}

    public static void install() {
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, RenderGuiLayerEvent.Pre.class, NeoForgeCompatSuppression::onPreRenderGuiLayer);
    }

    private static void onPreRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        ResourceLocation name = event.getName();
        ClientConfig config = ModConfigManager.getClient();

        // Cancel before NORMAL-priority layer replacers (e.g. OverflowingBars) run and swallow the wrapped layer.
        if (VanillaGuiLayers.PLAYER_HEALTH.equals(name) && config.healthBarBehavior != BarRenderBehavior.VANILLA) {
            event.setCanceled(true);
        } else if (VanillaGuiLayers.FOOD_LEVEL.equals(name) && StaminaBarBehavior.FOOD.equals(config.staminaBarBehavior)) {
            event.setCanceled(true);
        } else if (VanillaGuiLayers.ARMOR_LEVEL.equals(name) && config.armorBarBehavior != BarRenderBehavior.VANILLA) {
            event.setCanceled(true);
        } else if (VanillaGuiLayers.AIR_LEVEL.equals(name) && config.airBarBehavior != BarRenderBehavior.VANILLA) {
            event.setCanceled(true);
        } else if (VanillaGuiLayers.VEHICLE_HEALTH.equals(name) && config.mergeMountHealth && config.enableMountHealth) {
            event.setCanceled(true);
        } else if (IRONS_MANA.equals(name) && config.manaBarBehavior == ManaBarBehavior.IRONS_SPELLBOOKS) {
            event.setCanceled(true);
        } else if (ARS_MANA.equals(name) && config.manaBarBehavior == ManaBarBehavior.ARS_NOUVEAU) {
            event.setCanceled(true);
        } else if (RPGMANA_HUD.equals(name) && config.manaBarBehavior == ManaBarBehavior.RPG_MANA) {
            event.setCanceled(true);
        } else if (FD_COMFORT.equals(name) && config.healthBarBehavior == BarRenderBehavior.CUSTOM) {
            event.setCanceled(true);
        } else if (FD_NOURISHMENT.equals(name) && config.staminaBarBehavior == StaminaBarBehavior.FOOD) {
            event.setCanceled(true);
        }
    }
}
