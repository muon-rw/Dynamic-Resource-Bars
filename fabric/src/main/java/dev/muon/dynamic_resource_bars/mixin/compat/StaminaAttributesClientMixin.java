package dev.muon.dynamic_resource_bars.mixin.compat;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.StaminaBarBehavior;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.theredbrain.staminaattributes.registry.ClientEventsRegistry", remap = false)
public class StaminaAttributesClientMixin {

    @Inject(method = "lambda$initializeClientEvents$0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cancelStaminaAttributesHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ModConfigManager.getClient().staminaBarBehavior == StaminaBarBehavior.STAMINA_ATTRIBUTES) {
            ci.cancel();
        }
    }
}
