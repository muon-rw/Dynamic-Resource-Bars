package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Pre-registers this mod's mcmeta-animated bar textures with the TextureManager.
 * GeckoLib wraps the lazy SimpleTexture fallback in {@code TextureManager.getTexture},
 * and AzureLib checks {@code currentTexture.getClass() == SimpleTexture.class} on return;
 * registering a dedicated {@link SimpleTexture} subclass prevents both from swapping
 * our vertical strips for frame-sized animated textures.
 */
public class AnimatedTextureRegistrar {

    private static final class BarStripTexture extends SimpleTexture {
        BarStripTexture(ResourceLocation location) {
            super(location);
        }
    }

    private static final Set<ResourceLocation> registered = new HashSet<>();

    public static void registerAnimatedBarTextures(ResourceManager resourceManager) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        int count = 0;
        Map<ResourceLocation, Resource> textures = resourceManager.listResources("textures/gui",
                id -> id.getNamespace().equals(Constants.MOD_ID) && id.getPath().endsWith(".png"));
        for (Map.Entry<ResourceLocation, Resource> entry : textures.entrySet()) {
            ResourceLocation id = entry.getKey();
            if (registered.contains(id)) continue;
            if (!hasAnimationMetadata(id, entry.getValue())) continue;
            textureManager.register(id, new BarStripTexture(id));
            registered.add(id);
            count++;
        }
        if (count > 0) {
            Constants.LOG.debug("Pre-registered {} animated bar textures", count);
        }
    }

    private static boolean hasAnimationMetadata(ResourceLocation id, Resource resource) {
        try {
            return resource.metadata().getSection(AnimationMetadataSection.SERIALIZER).isPresent();
        } catch (Exception e) {
            Constants.LOG.warn("Skipping unreadable texture metadata for {}", id, e);
            return false;
        }
    }
}
