package dev.muon.dynamic_resource_bars.client;

import dev.muon.dynamic_resource_bars.Constants;
import dev.muon.dynamic_resource_bars.util.AnimationMetadataCache;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;

public class FabricAnimationCacheReloadListener implements SimpleSynchronousResourceReloadListener {

    private static final Identifier ID = Constants.loc("animation_cache");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        AnimationMetadataCache.clear();
        Constants.LOG.info("Animation metadata cache refreshed due to resource reload");
    }
}
