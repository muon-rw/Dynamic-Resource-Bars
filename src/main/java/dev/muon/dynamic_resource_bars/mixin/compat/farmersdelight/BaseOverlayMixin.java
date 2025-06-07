package dev.muon.dynamic_resource_bars.mixin.compat.farmersdelight;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
#if NEWER_THAN_20_1 && FABRIC
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.client.gui.HUDOverlays;

@Mixin(HUDOverlays.BaseOverlay.class)
#else
    // Surely there's some better way to do this
    @Mixin(Minecraft.class)
#endif
public class BaseOverlayMixin {
    #if NEWER_THAN_20_1 && FABRIC
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void cancelComfortOverlay(Minecraft mc, Gui gui, GuiGraphics graphics, CallbackInfo ci) {
        ClientConfig config = ModConfigManager.getClient();
        if (config.enableStaminaBar) {
            ci.cancel();
        }
    }
    #endif
}
