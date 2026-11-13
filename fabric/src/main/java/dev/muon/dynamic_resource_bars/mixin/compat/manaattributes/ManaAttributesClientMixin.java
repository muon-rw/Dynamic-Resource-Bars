package dev.muon.dynamic_resource_bars.mixin.compat.manaattributes;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.theredbrain.manaattributes.registry.ClientEventsRegistry", remap = false)
public class ManaAttributesClientMixin {

    @Inject(method = "lambda$initializeClientEvents$0", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cancelManaAttributesHud(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (ModConfigManager.getClient().manaBarBehavior == ManaBarBehavior.MANA_ATTRIBUTES) {
            ci.cancel();
        }
    }
}
