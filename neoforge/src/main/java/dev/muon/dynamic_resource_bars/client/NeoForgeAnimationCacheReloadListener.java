package dev.muon.dynamic_resource_bars.client;

import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.util.AnimationMetadataCache;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

public class NeoForgeAnimationCacheReloadListener extends SimplePreparableReloadListener<Void> {

    @Override
    protected Void prepare(ResourceManager manager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void preparations, ResourceManager manager, ProfilerFiller profiler) {
        AnimationMetadataCache.clear();
        Constants.LOG.info("Animation metadata cache refreshed due to resource reload");
    }
}
