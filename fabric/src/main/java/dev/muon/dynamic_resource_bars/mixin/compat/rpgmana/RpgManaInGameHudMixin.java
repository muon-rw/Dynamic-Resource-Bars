package dev.muon.dynamic_resource_bars.mixin.compat.rpgmana;

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
@Mixin(targets = "com.cleannrooster.rpgmana.client.InGameHud", remap = false)
public class RpgManaInGameHudMixin {

    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private void cancelRpgManaHud(GuiGraphics drawContext, DeltaTracker tickCounter, CallbackInfo ci) {
        if (ModConfigManager.getClient().manaBarBehavior == ManaBarBehavior.RPG_MANA) {
            ci.cancel();
        }
    }
}
