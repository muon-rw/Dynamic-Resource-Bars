package dev.muon.dynamic_resource_bars.mixin;

import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MouseHandler.class)
public interface MouseHandlerAccessor {

    @Accessor("xpos")
    void dynamic_resource_bars$setXpos(double xpos);

    @Accessor("ypos")
    void dynamic_resource_bars$setYpos(double ypos);
}
