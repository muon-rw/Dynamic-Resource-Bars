package dev.muon.dynamic_resource_bars.event;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
#if FORGE
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

// 1.20.1 Forge Only. See CommonEvents and GuiMixin
@Mod.EventBusSubscriber(modid = DynamicResourceBars.ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ForgeEvents {
    @SubscribeEvent
    public static void cancelVanillaBars(RenderGuiOverlayEvent.Pre event) {
        if (AllConfigs.client().enableHealthBar.get() && event.getOverlay() == VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            event.setCanceled(true);
        }
        if (AllConfigs.client().enableStaminaBar.get() && event.getOverlay() == VanillaGuiOverlay.FOOD_LEVEL.type()) {
            event.setCanceled(true);
        }
        if (AllConfigs.client().enableArmorBar.get() && event.getOverlay() == VanillaGuiOverlay.ARMOR_LEVEL.type()) {
            event.setCanceled(true);
        }
    }
}
#endif