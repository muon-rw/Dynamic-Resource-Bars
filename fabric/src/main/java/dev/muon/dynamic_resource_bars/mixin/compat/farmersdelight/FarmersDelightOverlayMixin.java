package dev.muon.dynamic_resource_bars.mixin.compat.farmersdelight;

import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import vectorwing.farmersdelight.client.gui.HUDOverlays;

@Pseudo
@Mixin(value = HUDOverlays.BaseOverlay.class, remap = false)
public class FarmersDelightOverlayMixin {

    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelFarmersDelightOverlays(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        ClientConfig config = ModConfigManager.getClient();
        Object self = this;
        if (self instanceof HUDOverlays.ComfortOverlay && config.healthBarBehavior == BarRenderBehavior.CUSTOM) {
            ci.cancel();
        } else if (self instanceof HUDOverlays.NourishmentOverlay && config.staminaBarBehavior == StaminaBarBehavior.FOOD) {
            ci.cancel();
        }
    }
}
