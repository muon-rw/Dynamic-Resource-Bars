package dev.muon.dynamic_resource_bars.compat;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import dev.muon.dynamic_resource_bars.compat.ManaProviderManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

#if FORGE
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
@Mod.EventBusSubscriber(modid = DynamicResourceBars.ID, value = Dist.CLIENT)
#endif
#if NEO
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.api.distmarker.Dist;

@EventBusSubscriber(modid = DynamicResourceBars.ID, value = Dist.CLIENT) #endif
public class ArsNouveauEventHandler {
    #if FORGELIKE
    @SubscribeEvent
    public static void onRender(#if NEO RenderGuiLayerEvent.Pre event #elif FORGE RenderGuiOverlayEvent.Pre event#endif ) {
        var config = ModConfigManager.getClient();
        if (config.manaBarBehavior == ManaBarBehavior.ARS_NOUVEAU) {
            boolean cancelEvent = false;
            #if NEO && NEWER_THAN_20_1
            if (event.getName().getNamespace().equals("ars_nouveau") && 
                event.getName().getPath().equals("mana_hud")) {
                cancelEvent = true;
            }
            #elif FORGE && UPTO_20_1
            if (event.getOverlay().id() != null &&
                event.getOverlay().id().getNamespace().equals("ars_nouveau") &&
                event.getOverlay().id().getPath().equals("mana_hud")) {
                cancelEvent = true;
            }
            #endif

            if (cancelEvent) {
                event.setCanceled(true);
                Player player = Minecraft.getInstance().player;
                var manaProvider = ManaProviderManager.getProviderForBehavior(ManaBarBehavior.ARS_NOUVEAU);
                if (player != null && manaProvider != null && manaProvider.getMaxMana() > 0) {
                    ManaBarRenderer.render(event.getGuiGraphics(), event.getPartialTick(), manaProvider, player);
                }
            }
        }
    }
    #endif
}