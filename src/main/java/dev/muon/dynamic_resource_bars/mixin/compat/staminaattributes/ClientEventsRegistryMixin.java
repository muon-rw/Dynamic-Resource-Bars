package dev.muon.dynamic_resource_bars.mixin.compat.staminaattributes;

import dev.muon.dynamic_resource_bars.compat.StaminaProviderManager;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.StaminaBarBehavior;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
#if FABRIC && NEWER_THAN_20_1
import net.minecraft.client.DeltaTracker;
import com.github.theredbrain.staminaattributes.registry.ClientEventsRegistry;
import net.minecraft.client.gui.GuiGraphics;

@Mixin(value = ClientEventsRegistry.class, remap = false)
#else
@Mixin(Minecraft.class)
#endif
public class ClientEventsRegistryMixin {
    #if FABRIC && NEWER_THAN_20_1
    @Inject(method = "lambda$initializeClientEvents$0", at = @At("HEAD"), cancellable = true)
    private static void cancelStaminaOverlay(GuiGraphics matrixStack, DeltaTracker delta, CallbackInfo ci) {
        var config = ModConfigManager.getClient();
        if (config.staminaBarBehavior == StaminaBarBehavior.STAMINA_ATTRIBUTES) {
            ci.cancel();
            Player player = Minecraft.getInstance().player;
            var staminaProvider = StaminaProviderManager.getProviderForBehavior(StaminaBarBehavior.STAMINA_ATTRIBUTES);
            if (player != null && staminaProvider != null && staminaProvider.getMaxStamina(player) > 0) {
                StaminaBarRenderer.render(matrixStack, player, delta);
            }
        }
    }
    #endif
} 