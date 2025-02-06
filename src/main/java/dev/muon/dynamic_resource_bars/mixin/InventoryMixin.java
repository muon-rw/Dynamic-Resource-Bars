package dev.muon.dynamic_resource_bars.mixin;

import dev.muon.dynamic_resource_bars.render.ArmorBarRenderer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Inventory.class)
public class InventoryMixin {
    @Shadow @Final public Player player;

    @Inject(method = "setPickedItem", at = @At("TAIL"))
    private void onHotbarSelect(ItemStack stack, CallbackInfo ci) {
        if (!(player instanceof LocalPlayer)) return;
        if (ArmorBarRenderer.isArmorRelevantItem(stack)) {
            ArmorBarRenderer.triggerTextDisplay();
        } else {
            ArmorBarRenderer.stopTextDisplay();
        }
    }
}