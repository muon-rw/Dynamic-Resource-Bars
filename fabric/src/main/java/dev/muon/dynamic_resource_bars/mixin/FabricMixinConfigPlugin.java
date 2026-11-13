package dev.muon.dynamic_resource_bars.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class FabricMixinConfigPlugin implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith("ManaAttributesClientMixin")) {
            return FabricLoader.getInstance().isModLoaded("manaattributes");
        }
        if (mixinClassName.endsWith("StaminaAttributesClientMixin")) {
            return FabricLoader.getInstance().isModLoaded("staminaattributes");
        }
        if (mixinClassName.endsWith("RpgManaInGameHudMixin")) {
            return FabricLoader.getInstance().isModLoaded("rpgmana");
        }
        if (mixinClassName.endsWith("FarmersDelightOverlayMixin")) {
            return FabricLoader.getInstance().isModLoaded("farmersdelight");
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
