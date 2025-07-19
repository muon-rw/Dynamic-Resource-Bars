package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import net.minecraft.resources.ResourceLocation;
import dev.muon.dynamic_resource_bars.util.StaminaBarBehavior;
#if NEO
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.api.distmarker.Dist;

@EventBusSubscriber(modid = DynamicResourceBars.ID, value = Dist.CLIENT)
#endif
public class FarmersDelightEventHandler {
#if NEO
    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Pre event) {
        ResourceLocation overlay = event.getName();
        if (overlay.getNamespace().equals("farmersdelight")) {
            if (overlay.getPath().equals("nourishment") && ModConfigManager.getClient().staminaBarBehavior.equals(StaminaBarBehavior.FOOD)) {
                event.setCanceled(true);
            } else if (overlay.getPath().equals("comfort") && ModConfigManager.getClient().enableHealthBar) {
                event.setCanceled(true);
            }
        }
    }
#endif
} 