package dev.muon.dynamic_resource_bars.mixin;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.muon.dynamic_resource_bars.config.ModConfigManager;
import dev.muon.dynamic_resource_bars.render.CustomHudRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Gui.class, priority = 1500)
public abstract class GuiMixin {

    @Shadow
    protected abstract void renderSlot(GuiGraphics guiGraphics, int x, int y, DeltaTracker deltaTracker, Player player, ItemStack stack, int seed);

    @Inject(method = "renderItemHotbar", at = @At("HEAD"))
    private void clearStaleOffhandTasks(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        CustomHudRenderer.clearDeferredOffhandTasks();
    }

    // We defer direct draw calls rather than retaining a MixinExtras Operation, because
    // MixinExtras disposes any LocalRef held by inner @WrapOperation handlers (such as
    // Amendments' hotbar animation) as soon as renderItemHotbar returns.
    @Definition(id = "blitSprite", method = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V")
    @Definition(id = "HOTBAR_OFFHAND_LEFT_SPRITE", field = "Lnet/minecraft/client/gui/Gui;HOTBAR_OFFHAND_LEFT_SPRITE:Lnet/minecraft/resources/ResourceLocation;")
    @Expression("?.blitSprite(HOTBAR_OFFHAND_LEFT_SPRITE, ?, ?, ?, ?)")
    @WrapWithCondition(method = "renderItemHotbar", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean deferOffhandLeftSprite(GuiGraphics instance, ResourceLocation sprite,
                                           int x, int y, int width, int height) {
        if (!ModConfigManager.getClient().enableMod) return true;
        CustomHudRenderer.deferOffhandTask(instance, () -> {
            RenderSystem.enableBlend();
            instance.blitSprite(sprite, x, y, width, height);
            RenderSystem.disableBlend();
        });
        return false;
    }

    @Definition(id = "blitSprite", method = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V")
    @Definition(id = "HOTBAR_OFFHAND_RIGHT_SPRITE", field = "Lnet/minecraft/client/gui/Gui;HOTBAR_OFFHAND_RIGHT_SPRITE:Lnet/minecraft/resources/ResourceLocation;")
    @Expression("?.blitSprite(HOTBAR_OFFHAND_RIGHT_SPRITE, ?, ?, ?, ?)")
    @WrapWithCondition(method = "renderItemHotbar", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean deferOffhandRightSprite(GuiGraphics instance, ResourceLocation sprite,
                                            int x, int y, int width, int height) {
        if (!ModConfigManager.getClient().enableMod) return true;
        CustomHudRenderer.deferOffhandTask(instance, () -> {
            RenderSystem.enableBlend();
            instance.blitSprite(sprite, x, y, width, height);
            RenderSystem.disableBlend();
        });
        return false;
    }

    @Definition(id = "renderSlot", method = "Lnet/minecraft/client/gui/Gui;renderSlot(Lnet/minecraft/client/gui/GuiGraphics;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V")
    @Definition(id = "offhand", local = @Local(type = ItemStack.class))
    @Expression("this.renderSlot(?, ?, ?, ?, ?, offhand, ?)")
    @WrapWithCondition(method = "renderItemHotbar", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean deferOffhandItemSlot(Gui instance, GuiGraphics graphics, int x, int y,
                                         DeltaTracker deltaTracker, Player player, ItemStack stack, int seed) {
        if (!ModConfigManager.getClient().enableMod) return true;
        CustomHudRenderer.deferOffhandTask(graphics, () -> this.renderSlot(graphics, x, y, deltaTracker, player, stack, seed));
        return false;
    }
}
