package dev.muon.dynamic_resource_bars.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.dynamic_resource_bars.config.ClientConfig;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.render.AirBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.util.BarRenderBehavior;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import dev.muon.dynamic_resource_bars.util.PlatformUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.resources.ResourceLocation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

#if UPTO_20_1 && FABRIC
import moriyashiine.bewitchment.api.BewitchmentAPI;
#endif

#if NEWER_THAN_20_1
    import net.minecraft.client.DeltaTracker;
#endif

@Mixin(value = Gui.class, priority = 499)
public abstract class GuiMixin {

    #if NEWER_THAN_20_1
    @Shadow
    @Final
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
        if (ModConfigManager.getClient().enableHealthBar) {
            return Minecraft.getInstance().getWindow().getGuiScaledHeight() - HUDPositioning.getHealthAnchor().y() - ModConfigManager.getClient().healthBackgroundHeight;
        }
        return originalY;
    }

    #endif

    #if UPTO_20_1 && FABRIC
    @Shadow @Final public Minecraft minecraft;
    @Shadow @Final private static ResourceLocation GUI_ICONS_LOCATION;
    @Shadow private int screenWidth;
    @Shadow private int screenHeight;
    @Shadow protected abstract Player getCameraPlayer();
    @Shadow protected abstract LivingEntity getPlayerVehicleWithHealth();
    @Shadow protected abstract int getVehicleMaxHearts(LivingEntity vehicle);
    @Shadow protected abstract int getVisibleVehicleHeartRows(int vehicleHearts);

    @Inject(
            method = "renderPlayerHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V")
    )
    private void captureHealthHeight(GuiGraphics guiGraphics, CallbackInfo ci, @Local(ordinal = 8) int r) {
        HUDPositioning.setVanillaHealthHeight(r - 1); // This value has 1 pixel padding built-in, so we subtract it
    }

    @WrapOperation(
            method = "renderPlayerHealth",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;renderHearts(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/world/entity/player/Player;IIIIFIIIZ)V")
    )
    private void replaceHearts(Gui instance, GuiGraphics guiGraphics, Player player, int x, int y, int height, int offsetHeartIndex, float maxHealth, int currentHealth, int displayHealth, int absorptionAmount, boolean renderHighlight, Operation<Void> original) {
        if (ModConfigManager.getClient().enableHealthBar) {
            float actualHealth = Minecraft.getInstance().player != null ? (Minecraft.getInstance().player).getHealth() : currentHealth;
            HealthBarRenderer.render(guiGraphics, player, maxHealth, actualHealth, absorptionAmount, this.minecraft.getFrameTime());
        } else {
            original.call(instance, guiGraphics, player, x, y, height, offsetHeartIndex, maxHealth, currentHealth, displayHealth, absorptionAmount, renderHighlight);
        }
        if (ModConfigManager.getClient().armorBarBehavior == BarRenderBehavior.CUSTOM) {
            ArmorBarRenderer.render(guiGraphics, player);
        }
    }



    @ModifyExpressionValue(
        method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getArmorValue()I")
    )
    private int hideOriginalArmor(int originalArmorValue) {
        ClientConfig config = ModConfigManager.getClient();
        BarRenderBehavior armorBehavior = config.armorBarBehavior;
        if (armorBehavior == BarRenderBehavior.CUSTOM || armorBehavior == BarRenderBehavior.HIDDEN) {
            return 0;
        }
        return originalArmorValue;
    }

    @Inject(method = "renderVehicleHealth(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(value = "HEAD"), cancellable = true)
    private void hideMountHealth(GuiGraphics guiGraphics, CallbackInfo ci) {
        ClientConfig config = ModConfigManager.getClient();
        if (config.enableStaminaBar) {
            ci.cancel();
        }
    }

    @Inject(
            method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;getVehicleMaxHearts(Lnet/minecraft/world/entity/LivingEntity;)I", shift = At.Shift.BEFORE)
    )
    private void renderStaminaBeforeFood(GuiGraphics guiGraphics, CallbackInfo ci) {
        ClientConfig config = ModConfigManager.getClient();
        if (config.enableStaminaBar) {
            Player player = this.minecraft.player;
            if (player != null) {
                StaminaBarRenderer.render(guiGraphics, player, this.minecraft.getFrameTime());
            }
        }
    }
    
    @ModifyExpressionValue(
            method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;getVehicleMaxHearts(Lnet/minecraft/world/entity/LivingEntity;)I")
    )
    private int hideFoodWhenStaminaEnabled(int vehicleHearts) {
        ClientConfig config = ModConfigManager.getClient();
        if (config.enableStaminaBar) {
            // Return non-zero to skip food rendering (since food only renders when vehicleHearts == 0)
            return 1;
        }
        return vehicleHearts;
    }
    
    @Inject(
            method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z", shift = At.Shift.BEFORE)
    )
    private void renderCustomAirBeforeVanilla(GuiGraphics guiGraphics, CallbackInfo ci) {
        ClientConfig config = ModConfigManager.getClient();
        BarRenderBehavior airBehavior = config.airBarBehavior;
        
        if (airBehavior == BarRenderBehavior.CUSTOM) {
            Player player = this.minecraft.player;
            if (player != null) {
                if (PlatformUtil.isModLoaded("bewitchment") && StaminaBarRenderer.isVampire(player)) {
                    return;
                }
                AirBarRenderer.render(guiGraphics, player, this.minecraft.getFrameTime());
            }
        }
    }
    
    @WrapOperation(
            method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isEyeInFluid(Lnet/minecraft/tags/TagKey;)Z")
    )
    private boolean hideAirWhenCustom(Player instance, TagKey tagKey, Operation<Boolean> original) {
        ClientConfig config = ModConfigManager.getClient();
        BarRenderBehavior airBehavior = config.airBarBehavior;
        
        if (airBehavior == BarRenderBehavior.CUSTOM || airBehavior == BarRenderBehavior.HIDDEN) {
            // Return false to skip vanilla air rendering (the condition needs to be false)
            return false;
        }
        return original.call(instance, tagKey);
    }

    @WrapOperation(
            method = "renderPlayerHealth(Lnet/minecraft/client/gui/GuiGraphics;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getAirSupply()I", ordinal = 0)
    )
    private int hideAirByModifyingSupply(Player instance, Operation<Integer> original) {
        ClientConfig config = ModConfigManager.getClient();
        BarRenderBehavior airBehavior = config.airBarBehavior;
        
        if (airBehavior == BarRenderBehavior.CUSTOM || airBehavior == BarRenderBehavior.HIDDEN) {
            // Return max air to make the vanilla condition (z < y) false
            Player player = this.minecraft.player;
            if (player != null) {
                return player.getMaxAirSupply();
            }
        }
        return original.call(instance);
    }

    #endif
}