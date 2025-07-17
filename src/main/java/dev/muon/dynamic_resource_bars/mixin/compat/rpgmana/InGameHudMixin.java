package dev.muon.dynamic_resource_bars.mixin.compat.rpgmana;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
#if FABRIC
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.ManaBarBehavior;
import dev.muon.dynamic_resource_bars.render.ManaBarRenderer;
import dev.muon.dynamic_resource_bars.compat.ManaProviderManager;
import com.cleannrooster.rpgmana.client.InGameHud;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = InGameHud.class, remap = false)
#else
@Mixin(Minecraft.class)
#endif
public class InGameHudMixin {
    #if FABRIC
    @Inject(method = "onHudRender", at = @At("HEAD"), cancellable = true)
    public void cancelManaOverlay(GuiGraphics drawContext, #if UPTO_20_1 float tickDelta#elif NEWER_THAN_20_1 DeltaTracker tickDelta#endif, CallbackInfo ci) {
        var config = ModConfigManager.getClient();
        if (config.manaBarBehavior == ManaBarBehavior.RPG_MANA) {
            Player player = Minecraft.getInstance().player;
            var manaProvider = ManaProviderManager.getProviderForBehavior(ManaBarBehavior.RPG_MANA);
            if (player != null && manaProvider != null && manaProvider.getMaxMana() > 0) {
                ManaBarRenderer.render(drawContext, tickDelta, manaProvider, player);
            }
            ci.cancel();
        }
    }
    #endif
} 