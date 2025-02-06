package dev.muon.dynamic_resource_bars.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.muon.dynamic_resource_bars.foundation.config.AllConfigs;
import dev.muon.dynamic_resource_bars.render.AirBarRenderer;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import dev.muon.dynamic_resource_bars.render.HealthBarRenderer;
import dev.muon.dynamic_resource_bars.render.StaminaBarRenderer;
import dev.muon.dynamic_resource_bars.util.HUDPositioning;
import fuzs.puzzleslib.api.event.v1.data.MutableInt;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

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

    // Using PuzzlesLib instead here, see CommonEvents
    /*
    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void renderCustomHealth(GuiGraphics graphics, Player player, int originalX, int originalY, int height, int offsetHeartIndex, float maxHealth, int health, int displayHealth, int absorptionAmount, boolean renderHighlight, CallbackInfo ci) {
        if (minecraft.options.hideGui) return;
        HealthBarRenderer.render(graphics, player, player.getMaxHealth(), player.getHealth(), absorptionAmount, medieval$currentDeltaTracker);
        ci.cancel();
    }


    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void renderCustomFood(GuiGraphics graphics, Player player, int originalY, int originalX, CallbackInfo ci) {
        if (minecraft.options.hideGui) return;
        StaminaBarRenderer.render(graphics, player, medieval$currentDeltaTracker);
        ci.cancel();
    }
     */

    @ModifyArg(
            method = "renderArmor",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V"),
            index = 2
    )
    private static int shiftOnlyByBarHeight(int originalY) {
        if (AllConfigs.client().enableHealthBar.get()) {
            return Minecraft.getInstance().getWindow().getGuiScaledHeight() - HUDPositioning.getHealthAnchor().y() - AllConfigs.client().healthBackgroundHeight.get();
        }
        return originalY;
    }
    // TODO: Air shift by needed


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
        if (AllConfigs.client().enableHealthBar.get()) {
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
        if (!AllConfigs.client().enableStaminaBar.get()) {
            return;
        }
        Player player = this.minecraft.player;
        if (player != null) {
            int partialTickFood = Math.round(this.minecraft.getFrameTime());
            StaminaBarRenderer.render(guiGraphics, player, partialTickFood);
            AirBarRenderer.render(guiGraphics, player);
        }
        ci.cancel();
    }

    @ModifyVariable(method = "renderPlayerHealth", at = @At("STORE"), ordinal = 11, slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getArmorValue()I"), to = @At(value = "FIELD", target = "Lnet/minecraft/world/effect/MobEffects;REGENERATION:Lnet/minecraft/world/effect/MobEffect;")))
    private int replaceArmor(int armorValue, GuiGraphics guiGraphics) {
        Player player = this.minecraft.player;
        if (AllConfigs.client().enableArmorBar.get()) {
            armorValue = 0;
            ArmorBarRenderer.render(guiGraphics, player);
        }
        return armorValue;
    }
    #endif

    @Inject(method = "Lnet/minecraft/client/gui/Gui;tick()V", at = @At("TAIL"))
    private void checkSelectedItem(CallbackInfo ci) {
        Player player = this.minecraft.player;
        if (player != null) {
            ItemStack selectedStack = player.getInventory().getSelected();
            if (ArmorBarRenderer.isArmorRelevantItem(selectedStack)) {
                ArmorBarRenderer.triggerTextDisplay();
            } else {
                ArmorBarRenderer.stopTextDisplay();
            }
        }
    }
}