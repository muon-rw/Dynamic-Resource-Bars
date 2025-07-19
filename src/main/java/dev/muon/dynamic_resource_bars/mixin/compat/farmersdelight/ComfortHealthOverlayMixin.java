package dev.muon.dynamic_resource_bars.mixin.compat.farmersdelight;


import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import dev.muon.dynamic_resource_bars.util.StaminaBarBehavior;
#if UPTO_20_1 && FABRIC
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.client.gui.ComfortHealthOverlay;

@Mixin(value = ComfortHealthOverlay.class, remap = false)
#else
    // Surely there's some better way to do this
    @Mixin(Minecraft.class)
#endif
public class ComfortHealthOverlayMixin {
    #if UPTO_20_1 && FABRIC
    @Inject(method = "renderComfortOverlay", at = @At("HEAD"), cancellable = true)
    private static void cancelComfortOverlay(Minecraft mc, Gui gui, GuiGraphics graphics, CallbackInfo ci) {
        ClientConfig config = ModConfigManager.getClient();
        if (config.staminaBarBehavior.equals(StaminaBarBehavior.FOOD)) {
            ci.cancel();
        }
    }
    #endif
}
