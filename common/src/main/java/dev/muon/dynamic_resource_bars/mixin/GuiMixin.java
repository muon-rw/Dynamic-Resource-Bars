package dev.muon.dynamic_resource_bars.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.dynamic_resource_bars.render.CustomHudRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class, priority = 1500)
public class GuiMixin {

    @Inject(method = "renderItemHotbar", at = @At("HEAD"))
    private void clearStaleOffhandTasks(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        CustomHudRenderer.clearDeferredOffhandTasks();
    }

    @Definition(id = "blitSprite", method = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V")
    @Definition(id = "HOTBAR_OFFHAND_LEFT_SPRITE", field = "Lnet/minecraft/client/gui/Gui;HOTBAR_OFFHAND_LEFT_SPRITE:Lnet/minecraft/resources/ResourceLocation;")
    @Expression("?.blitSprite(HOTBAR_OFFHAND_LEFT_SPRITE, ?, ?, ?, ?)")
    @WrapOperation(method = "renderItemHotbar", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void deferOffhandLeftSprite(GuiGraphics instance, ResourceLocation sprite,
                                        int x, int y, int width, int height, Operation<Void> original) {
        CustomHudRenderer.deferOffhandTask(instance, () -> original.call(instance, sprite, x, y, width, height));
    }

    @Definition(id = "blitSprite", method = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V")
    @Definition(id = "HOTBAR_OFFHAND_RIGHT_SPRITE", field = "Lnet/minecraft/client/gui/Gui;HOTBAR_OFFHAND_RIGHT_SPRITE:Lnet/minecraft/resources/ResourceLocation;")
    @Expression("?.blitSprite(HOTBAR_OFFHAND_RIGHT_SPRITE, ?, ?, ?, ?)")
    @WrapOperation(method = "renderItemHotbar", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void deferOffhandRightSprite(GuiGraphics instance, ResourceLocation sprite,
                                         int x, int y, int width, int height, Operation<Void> original) {
        CustomHudRenderer.deferOffhandTask(instance, () -> original.call(instance, sprite, x, y, width, height));
    }

    @Definition(id = "renderSlot", method = "Lnet/minecraft/client/gui/Gui;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V")
    @Definition(id = "offhand", local = @Local(type = ItemStack.class))
    @Expression("this.renderSlot(?, ?, ?, ?, ?, offhand, ?)")
    @WrapOperation(method = "renderItemHotbar", at = @At("MIXINEXTRAS:EXPRESSION"))
    private void deferOffhandItemSlot(Gui instance, GuiGraphics graphics, int x, int y,
                                      DeltaTracker deltaTracker, Player player, ItemStack stack, int seed,
                                      Operation<Void> original) {
        CustomHudRenderer.deferOffhandTask(graphics, () -> original.call(instance, graphics, x, y, deltaTracker, player, stack, seed));
    }
}
