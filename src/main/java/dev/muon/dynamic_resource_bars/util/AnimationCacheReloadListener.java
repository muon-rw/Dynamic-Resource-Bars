package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.DynamicResourceBars;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

#if FABRIC
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
#elif FORGELIKE
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
#endif

/**
 * Resource reload listener to refresh animation metadata cache when resource packs change.
 */
public class AnimationCacheReloadListener 
#if FABRIC
    implements SimpleSynchronousResourceReloadListener 
#elif FORGELIKE
    implements PreparableReloadListener
#endif
{
    #if FABRIC
    private static final ResourceLocation ID = DynamicResourceBars.loc("animation_cache");
    
    @Override
    public ResourceLocation getFabricId() {
        return ID;
    }
    
    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        AnimationMetadataCache.clear();
        DynamicResourceBars.LOGGER.info("Animation metadata cache refreshed due to resource reload");
    }
    #elif FORGELIKE
    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor, Executor gameExecutor) {
        return CompletableFuture.runAsync(() -> {
            // Nothing to prepare
        }, backgroundExecutor).thenCompose(stage::wait).thenRunAsync(() -> {
            AnimationMetadataCache.clear();
            DynamicResourceBars.LOGGER.info("Animation metadata cache refreshed due to resource reload");
        }, gameExecutor);
    }
    #endif
}

