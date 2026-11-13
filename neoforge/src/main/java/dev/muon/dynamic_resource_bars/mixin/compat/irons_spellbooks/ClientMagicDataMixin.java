package dev.muon.dynamic_resource_bars.mixin.compat.irons_spellbooks;

import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.provider.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientMagicData.class, remap = false)
public class ClientMagicDataMixin {

    @Inject(method = "setMana", at = @At("HEAD"))
    private static void notifyManaDrain(int playerMana, CallbackInfo ci) {
        if (ModConfigManager.getClient().manaBarBehavior == ManaBarBehavior.IRONS_SPELLBOOKS
                && playerMana < ClientMagicData.getPlayerMana()) {
            ManaBarRenderer.INSTANCE.onManaDrain();
        }
    }

    @Inject(method = "setClientCastState", at = @At("HEAD"))
    private static void notifyCastAttempt(String spellId, int spellLevel, int castDuration,
                                          CastSource castSource, String castingEquipmentSlot, CallbackInfo ci) {
        if (ModConfigManager.getClient().manaBarBehavior == ManaBarBehavior.IRONS_SPELLBOOKS) {
            ManaBarRenderer.INSTANCE.onManaDrainAttempt();
        }
    }
}
