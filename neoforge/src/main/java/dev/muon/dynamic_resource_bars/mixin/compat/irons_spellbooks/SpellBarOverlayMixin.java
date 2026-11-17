package dev.muon.dynamic_resource_bars.mixin.compat.irons_spellbooks;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalIntRef;
import dev.muon.dynamic_resource_bars.compat.IronsSpellBarPlacement;
import dev.muon.dynamic_resource_bars.util.Position;
import dev.muon.dynamic_resource_bars.util.ScreenRect;
import io.redspace.ironsspellbooks.gui.overlays.SpellBarOverlay;
import io.redspace.ironsspellbooks.player.ClientRenderCache;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SpellBarOverlay.class, remap = false)
public class SpellBarOverlayMixin {

    @Unique
    private static final int SLOT_SIZE = 22;

    @Inject(method = "render", at = @At("HEAD"))
    private void forgetLastSlotsRect(GuiGraphics guiHelper, DeltaTracker deltaTracker, CallbackInfo ci) {
        IronsSpellBarPlacement.forgetLastSlotsRect();
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/gui/overlays/SpellBarOverlay;prepTranslucency()V"))
    private void moveClearOfResourceBars(GuiGraphics guiHelper, DeltaTracker deltaTracker, CallbackInfo ci,
                                         @Local(name = "centerX") LocalIntRef centerX,
                                         @Local(name = "centerY") LocalIntRef centerY) {
        if (ClientRenderCache.relativeSpellBarSlotLocations.isEmpty()) return;
        int left = Integer.MAX_VALUE, top = Integer.MAX_VALUE, right = Integer.MIN_VALUE, bottom = Integer.MIN_VALUE;
        for (Vec2 slot : ClientRenderCache.relativeSpellBarSlotLocations) {
            left = Math.min(left, (int) slot.x);
            top = Math.min(top, (int) slot.y);
            right = Math.max(right, (int) slot.x + SLOT_SIZE);
            bottom = Math.max(bottom, (int) slot.y + SLOT_SIZE);
        }
        ScreenRect slots = new ScreenRect(centerX.get() + left, centerY.get() + top, right - left, bottom - top);
        Position offset = IronsSpellBarPlacement.offsetClearOfBars(slots, Minecraft.getInstance().player,
                guiHelper.guiWidth(), guiHelper.guiHeight());
        centerX.set(centerX.get() + offset.x());
        centerY.set(centerY.get() + offset.y());
    }
}
