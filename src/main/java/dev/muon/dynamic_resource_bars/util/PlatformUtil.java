package dev.muon.dynamic_resource_bars.util;

#if FABRIC
import net.fabricmc.loader.api.FabricLoader;
#endif

#if FORGE
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
#endif

#if NEOFORGE
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModInfo;
#endif

public class PlatformUtil {
    public static boolean isModLoaded(String modId) {
        #if FABRIC
            return FabricLoader.getInstance().isModLoaded(modId);
        #endif
        #if FORGELIKE
            if (ModList.get() == null) {
                return LoadingModList.get().getMods().stream().map(ModInfo::getModId).anyMatch(modId::equals);
            }
            return ModList.get().isLoaded(modId);
        #endif
    }
}
