package dev.muon.dynamic_resource_bars.mixin.compat.witchery;

import dev.muon.dynamic_resource_bars.compat.WitcheryCompatNeoForge;
import dev.sterner.witchery.feature.ability.AbilityHudRenderer;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbilityHudRenderer.class, remap = false)
public class AbilityHudRendererMixin {

    @Inject(method = "renderHud", at = @At("HEAD"), cancellable = true)
    private static void skipUnhoistedRender(GuiGraphics guiGraphics, CallbackInfo ci) {
        if (!WitcheryCompatNeoForge.isHoistedRenderActive()) {
            ci.cancel();
        }
    }
}
