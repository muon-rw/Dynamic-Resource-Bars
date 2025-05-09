package dev.muon.dynamic_resource_bars.mixin;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif

@Mixin(value = Gui.class, priority = 499)
public class GuiMixin {
    #if NEWER_THAN_20_1
    @Shadow @Final
    private Minecraft minecraft;

    @Unique
    private DeltaTracker medieval$currentDeltaTracker;

    @Inject(method = "render", at = @At("HEAD"))
    private void captureDeltaTracker(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        this.medieval$currentDeltaTracker = deltaTracker;
    }

    @ModifyArg(
            method = "renderArmor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"),
            index = 2
    )
    private static int shiftOnlyByBarHeight(int originalY) {
        if (ModConfigManager.getClient().enableHealthBar.get()) {
            return Minecraft.getInstance().getWindow().getGuiScaledHeight() - HUDPositioning.getHealthAnchor().y() - ModConfigManager.getClient().healthBackgroundHeight.get();
        }
        return originalY;
    }
    // TODO: Air shift by needed
    // TODO: Add logic here to hide vanilla armor if !showVanillaArmorInsteadOfCustom for 1.21.1+
    // This might involve a @ModifyVariable on armorValue or an @Inject to cancel renderArmor similar to 1.20.1.

    #endif

    #if UPTO_20_1 && FABRIC
    @Shadow
    @Final
    private Minecraft minecraft;

    @WrapOperation(
            method = "renderPlayerHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V")
    )
    private void replaceHearts(Gui instance, GuiGraphics guiGraphics, Player player, int x, int y, int height, int offsetHeartIndex, float maxHealth, int currentHealth, int displayHealth, int absorptionAmount, boolean renderHighlight, Operation<Void> original) {
        if (ModConfigManager.getClient().enableHealthBar.get()) {
            HealthBarRenderer.render(guiGraphics, player, maxHealth, currentHealth, absorptionAmount, this.minecraft.getFrameTime());
        } else {
            original.call(instance, guiGraphics, player, x, y, height, offsetHeartIndex, maxHealth, currentHealth, displayHealth, absorptionAmount, renderHighlight);
        }
    }

    @Inject(
            method = "renderPlayerHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;getPlayerVehicleWithHealth()Lnet/minecraft/world/entity/LivingEntity;"),
            cancellable = true
    )
    private void replaceFoodAndAir(GuiGraphics guiGraphics, CallbackInfo ci) {
        Player player = this.minecraft.player;
        CClient config = ModConfigManager.getClient();
        boolean customStaminaEnabled = config.enableStaminaBar.get();
        BarRenderBehavior airBehavior = config.airBarBehavior.get();

        if (customStaminaEnabled && player != null) {
            StaminaBarRenderer.render(guiGraphics, player, this.minecraft.getFrameTime());
        }
        if (player != null) { 
            AirBarRenderer.render(guiGraphics, player);
        }
        
        boolean shouldCancelForAir = (airBehavior == BarRenderBehavior.CUSTOM || airBehavior == BarRenderBehavior.HIDDEN);
        if (customStaminaEnabled || shouldCancelForAir) {
             ci.cancel(); 
        }
    }

    @ModifyExpressionValue(
        method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getArmorValue()I")
    )
    private int dynamicbars$modifyArmorValue(int originalArmorValue) {
        CClient config = ModConfigManager.getClient();
        BarRenderBehavior armorBehavior = config.armorBarBehavior.get();
        if (armorBehavior == BarRenderBehavior.CUSTOM || armorBehavior == BarRenderBehavior.HIDDEN) {
            return 0;
        }
        return originalArmorValue;
    }
    #endif

}