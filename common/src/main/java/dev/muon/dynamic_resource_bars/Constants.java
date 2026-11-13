package dev.muon.dynamic_resource_bars;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {

    public static final String MOD_ID = "dynamic_resource_bars";
    public static final String MOD_NAME = "Dynamic Resource Bars";
    public static final Logger LOG = LoggerFactory.getLogger(MOD_ID);

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
