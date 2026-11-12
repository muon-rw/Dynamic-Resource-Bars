package dev.muon.dynamic_resource_bars.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.muon.dynamic_resource_bars.render.CustomHudRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class, priority = 1500)
public class GuiMixin {

    @Inject(method = "extractItemHotbar", at = @At("HEAD"), remap = false)
    private void clearStaleOffhandTasks(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        CustomHudRenderer.clearDeferredOffhandTasks();
    }

    @Definition(id = "blitSprite", method = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    @Definition(id = "HOTBAR_OFFHAND_LEFT_SPRITE", field = "Lnet/minecraft/client/gui/Gui;HOTBAR_OFFHAND_LEFT_SPRITE:Lnet/minecraft/resources/Identifier;")
    @Expression("?.blitSprite(?, HOTBAR_OFFHAND_LEFT_SPRITE, ?, ?, ?, ?)")
    @WrapOperation(method = "extractItemHotbar", at = @At("MIXINEXTRAS:EXPRESSION"), remap = false)
    private void deferOffhandLeftSprite(GuiGraphicsExtractor instance, RenderPipeline pipeline, Identifier sprite,
                                        int x, int y, int width, int height, Operation<Void> original) {
        CustomHudRenderer.deferOffhandTask(instance, () -> original.call(instance, pipeline, sprite, x, y, width, height));
    }

    @Definition(id = "blitSprite", method = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V")
    @Definition(id = "HOTBAR_OFFHAND_RIGHT_SPRITE", field = "Lnet/minecraft/client/gui/Gui;HOTBAR_OFFHAND_RIGHT_SPRITE:Lnet/minecraft/resources/Identifier;")
    @Expression("?.blitSprite(?, HOTBAR_OFFHAND_RIGHT_SPRITE, ?, ?, ?, ?)")
    @WrapOperation(method = "extractItemHotbar", at = @At("MIXINEXTRAS:EXPRESSION"), remap = false)
    private void deferOffhandRightSprite(GuiGraphicsExtractor instance, RenderPipeline pipeline, Identifier sprite,
                                         int x, int y, int width, int height, Operation<Void> original) {
        CustomHudRenderer.deferOffhandTask(instance, () -> original.call(instance, pipeline, sprite, x, y, width, height));
    }

    @Definition(id = "extractSlot", method = "Lnet/minecraft/client/gui/Gui;extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V")
    @Definition(id = "offhand", local = @ com.llamalad7.mixinextras.sugar.Local(type = ItemStack.class))
    @Expression("this.extractSlot(?, ?, ?, ?, ?, offhand, ?)")
    @WrapOperation(method = "extractItemHotbar", at = @At("MIXINEXTRAS:EXPRESSION"), remap = false)
    private void deferOffhandItemSlot(Gui instance, GuiGraphicsExtractor graphics, int x, int y,
                                      DeltaTracker deltaTracker, Player player, ItemStack stack, int seed,
                                      Operation<Void> original) {
        CustomHudRenderer.deferOffhandTask(graphics, () -> original.call(instance, graphics, x, y, deltaTracker, player, stack, seed));
    }
}
