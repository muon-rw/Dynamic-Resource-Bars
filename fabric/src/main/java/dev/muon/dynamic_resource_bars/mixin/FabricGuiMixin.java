package dev.muon.dynamic_resource_bars.mixin;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.dynamic_resource_bars.client.HudBarOrchestrator;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class, priority = 1500)
public abstract class FabricGuiMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @WrapOperation(
            method = "renderHotbarAndDecorations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderItemHotbar(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/DeltaTracker;)V")
    )
    private void wrapRenderItemHotbar(Gui instance, GuiGraphics guiGraphics, DeltaTracker deltaTracker, Operation<Void> original) {
        if (!HudBarOrchestrator.renderHotbar(guiGraphics, deltaTracker)) {
            original.call(instance, guiGraphics, deltaTracker);
        }
    }

    @WrapOperation(
            method = "renderHotbarAndDecorations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderJumpMeter(Lnet/minecraft/world/entity/PlayerRideableJumping;Lnet/minecraft/client/gui/GuiGraphics;I)V")
    )
    private void wrapRenderJumpMeter(Gui instance, PlayerRideableJumping jumpable, GuiGraphics guiGraphics, int x, Operation<Void> original) {
        if (!HudBarOrchestrator.renderInfoBar(guiGraphics, this.minecraft.getTimer())) {
            original.call(instance, jumpable, guiGraphics, x);
        }
    }

    @WrapOperation(
            method = "renderHotbarAndDecorations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderExperienceBar(Lnet/minecraft/client/gui/GuiGraphics;I)V")
    )
    private void wrapRenderExperienceBar(Gui instance, GuiGraphics guiGraphics, int x, Operation<Void> original) {
        if (!HudBarOrchestrator.renderInfoBar(guiGraphics, this.minecraft.getTimer())) {
            original.call(instance, guiGraphics, x);
        }
    }

    @WrapOperation(
            method = "renderHotbarAndDecorations",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderVehicleHealth(Lnet/minecraft/client/gui/GuiGraphics;)V")
    )
    private void wrapRenderVehicleHealth(Gui instance, GuiGraphics guiGraphics, Operation<Void> original) {
        if (!HudBarOrchestrator.suppressVehicleHealth()) {
            original.call(instance, guiGraphics);
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void onRenderExperienceLevel(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        if (HudBarOrchestrator.renderExperienceLevel(guiGraphics, deltaTracker)) {
            HudBarOrchestrator.renderAllText(guiGraphics, deltaTracker);
            ci.cancel();
        }
    }

    @Inject(method = "renderExperienceLevel", at = @At("TAIL"))
    private void onRenderExperienceLevelTail(GuiGraphics guiGraphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HudBarOrchestrator.renderAllText(guiGraphics, deltaTracker);
    }

    @WrapOperation(
            method = "renderPlayerHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderArmor(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIII)V")
    )
    private void wrapRenderArmor(GuiGraphics guiGraphics, Player player, int y, int heartRows, int height, int x, Operation<Void> original) {
        if (!HudBarOrchestrator.renderArmor(guiGraphics, this.minecraft.getTimer())) {
            original.call(guiGraphics, player, y, heartRows, height, x);
        }
    }

    @WrapOperation(
            method = "renderPlayerHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V")
    )
    private void wrapRenderHearts(Gui instance, GuiGraphics guiGraphics, Player player, int x, int y, int height, int offsetHeartIndex, float maxHealth, int currentHealth, int displayHealth, int absorptionAmount, boolean renderHighlight, Operation<Void> original) {
        if (!HudBarOrchestrator.renderHealth(guiGraphics, this.minecraft.getTimer())) {
            original.call(instance, guiGraphics, player, x, y, height, offsetHeartIndex, maxHealth, currentHealth, displayHealth, absorptionAmount, renderHighlight);
        }
        HudBarOrchestrator.renderMana(guiGraphics, this.minecraft.getTimer());
        HudBarOrchestrator.renderStaminaForStaminaAttributes(guiGraphics, this.minecraft.getTimer());
    }

    @WrapOperation(
            method = "renderPlayerHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderFood(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;II)V")
    )
    private void wrapRenderFood(Gui instance, GuiGraphics guiGraphics, Player player, int y, int x, Operation<Void> original) {
        if (!HudBarOrchestrator.renderFood(guiGraphics, this.minecraft.getTimer())) {
            original.call(instance, guiGraphics, player, y, x);
        }
    }

    @Inject(method = "renderPlayerHealth", at = @At(value = "CONSTANT", args = "stringValue=air"))
    private void onRenderAir(GuiGraphics guiGraphics, CallbackInfo ci) {
        HudBarOrchestrator.renderAir(guiGraphics, this.minecraft.getTimer());
    }

    @WrapWithCondition(
            method = "renderPlayerHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V")
    )
    private boolean shouldRenderVanillaAirBubble(GuiGraphics instance, ResourceLocation sprite, int x, int y, int width, int height) {
        return ModConfigManager.getClient().airBarBehavior == BarRenderBehavior.VANILLA;
    }
}
