package dev.muon.dynamic_resource_bars.util;

import dev.muon.dynamic_resource_bars.Constants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Pre-registers this mod's mcmeta-animated bar textures with the TextureManager.
 * GeckoLib wraps the lazy SimpleTexture fallback in {@code TextureManager.getTexture}
 * and swaps any unregistered texture carrying vanilla animation metadata for a
 * frame-sized animated texture, which breaks our full-strip UV math. A registered
 * texture never reaches that fallback; TextureManager reloads it on later pack swaps.
 */
public class AnimatedTextureRegistrar {

    private static final Set<Identifier> registered = new HashSet<>();

    public static void registerAnimatedBarTextures(ResourceManager resourceManager) {
        TextureManager textureManager = Minecraft.getInstance().getTextureManager();
        int count = 0;
        Map<Identifier, Resource> textures = resourceManager.listResources("textures/gui",
                id -> id.getNamespace().equals(Constants.MOD_ID) && id.getPath().endsWith(".png"));
        for (Map.Entry<Identifier, Resource> entry : textures.entrySet()) {
            Identifier id = entry.getKey();
            if (registered.contains(id)) continue;
            if (!hasAnimationMetadata(id, entry.getValue())) continue;
            textureManager.registerAndLoad(id, new SimpleTexture(id));
            registered.add(id);
            count++;
        }
        if (count > 0) {
            Constants.LOG.debug("Pre-registered {} animated bar textures", count);
        }
    }

    private static boolean hasAnimationMetadata(Identifier id, Resource resource) {
        try {
            return resource.metadata().getSection(AnimationMetadataSection.TYPE).isPresent();
        } catch (Exception e) {
            Constants.LOG.warn("Skipping unreadable texture metadata for {}", id, e);
            return false;
        }
    }
}
