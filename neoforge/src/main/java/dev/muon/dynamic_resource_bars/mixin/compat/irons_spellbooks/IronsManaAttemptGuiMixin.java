package dev.muon.dynamic_resource_bars.mixin.compat.irons_spellbooks;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class IronsManaAttemptGuiMixin {

    @Inject(method = "setOverlayMessage", at = @At("HEAD"))
    private void notifyInsufficientManaAttempt(Component component, boolean animateColor, CallbackInfo ci) {
        if (ModConfigManager.getClient().manaBarBehavior != ManaBarBehavior.IRONS_SPELLBOOKS) {
            return;
        }
        if (component != null && component.getContents() instanceof TranslatableContents tc
                && "ui.irons_spellbooks.cast_error_mana".equals(tc.getKey())) {
            ManaBarRenderer.INSTANCE.onManaDrainAttempt();
        }
    }
}
