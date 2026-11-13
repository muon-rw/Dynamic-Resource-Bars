package dev.muon.dynamic_resource_bars.mixin;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NeoForgeMixinConfigPlugin implements IMixinConfigPlugin {

    private static final Logger LOGGER = LogManager.getLogger("DynamicResourceBars-Mixin");
    private static final Set<String> EXCLUDED_DIRS = Set.of("client", "accessor");

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.contains(".compat.")) {
            return true;
        }
        String[] parts = mixinClassName.split("\\.");
        List<String> requiredMods = new ArrayList<>();
        for (int i = 0; i < parts.length; i++) {
            if (!"compat".equals(parts[i])) continue;
            for (int j = i + 1; j < parts.length - 1; j++) {
                String modId = parts[j];
                if (!EXCLUDED_DIRS.contains(modId)) {
                    requiredMods.add(modId);
                }
            }
            break;
        }
        for (String modId : requiredMods) {
            if (!isModLoaded(modId)) {
                LOGGER.info("Disabling mixin {} because required mod '{}' is not loaded",
                        getSimpleMixinName(mixinClassName), modId);
                return false;
            }
        }
        if (!requiredMods.isEmpty()) {
            LOGGER.info("Enabling mixin {} - all required mods {} are loaded",
                    getSimpleMixinName(mixinClassName), requiredMods);
        }
        return true;
    }

    private static String getSimpleMixinName(String mixinClassName) {
        String[] parts = mixinClassName.split("\\.");
        return parts[parts.length - 1];
    }

    private static boolean isModLoaded(String modId) {
        if (ModList.get() == null) {
            return LoadingModList.get().getMods().stream().map(ModInfo::getModId).anyMatch(modId::equals);
        }
        return ModList.get().isLoaded(modId);
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
