package dev.muon.dynamic_resource_bars.event;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.StaminaBarBehavior;
#if FORGE
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 1.20.1 Forge Only. See CommonEvents for 1.21.1, GuiMixin for 1.20.1 Fabric
@Mod.EventBusSubscriber(modid = DynamicResourceBars.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    @SubscribeEvent
    public static void cancelVanillaBars(RenderGuiOverlayEvent.Pre event) {
        ClientConfig config = ModConfigManager.getClient();

        if (config.enableHealthBar && event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            event.setCanceled(true);
        }
        if (config.staminaBarBehavior.equals(StaminaBarBehavior.FOOD) && event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            event.setCanceled(true);
        }
        
        // Cancel mount health when stamina bar is enabled (since it renders mount health)
        if (config.mergeMountHealth && config.enableMountHealth && event.getOverlay() == VanillaGuiOverlay.MOUNT_HEALTH.type()) {
            event.setCanceled(true);
        }
        
        if (event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type()) {
            if (config.armorBarBehavior == BarRenderBehavior.CUSTOM ||
                config.armorBarBehavior == BarRenderBehavior.HIDDEN) {
                event.setCanceled(true);
            }
        }

        if (event.getOverlay() == VanillaGuiOverlay.AIR_LEVEL.type()) {
            if (config.airBarBehavior == BarRenderBehavior.CUSTOM ||
                config.airBarBehavior == BarRenderBehavior.HIDDEN) {
                event.setCanceled(true);
            }
        }
    }
}
#endif