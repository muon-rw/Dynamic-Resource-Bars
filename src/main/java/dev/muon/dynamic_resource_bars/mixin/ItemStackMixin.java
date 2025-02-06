package dev.muon.dynamic_resource_bars.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

@Mixin(ItemStack.class)
public class ItemStackMixin {
    @ModifyReturnValue(method = "getTooltipLines", at = @At("RETURN"))
    #if NEWER_THAN_20_1
    private List<Component> onGetTooltip(List<Component> original, @Local(argsOnly = true) Player player, @Local(argsOnly = true) TooltipFlag isAdvanced) {
    #else
    private List<Component> onGetTooltip(List<Component> original, Player player, TooltipFlag isAdvanced) {
    #endif
        if (!(player instanceof LocalPlayer)) {
            return original;
        }

        ItemStack stack = (ItemStack) (Object) this;
        if (ArmorBarRenderer.isArmorRelevantItem(stack)) {
            ArmorBarRenderer.triggerTextDisplay();
        } else {
            ArmorBarRenderer.stopTextDisplay();
        }

        return original;
    }
}