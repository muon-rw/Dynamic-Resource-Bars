package dev.muon.dynamic_resource_bars.platform.services;

import java.nio.file.Path;

public interface IPlatformHelper {

    String getPlatformName();

    boolean isModLoaded(String modId);

    boolean isDevelopmentEnvironment();

    default String getEnvironmentName() {
        return isDevelopmentEnvironment() ? "development" : "production";
    }

    /** Whether the running side is the client. Server-side calls return false. */
    boolean isClient();

    /** Loader-resolved config directory. Equivalent to FabricLoader#getConfigDir / FMLPaths#CONFIGDIR. */
    Path getConfigDir();
}
