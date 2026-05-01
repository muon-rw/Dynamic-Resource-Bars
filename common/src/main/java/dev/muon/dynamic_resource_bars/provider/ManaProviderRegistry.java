package dev.muon.dynamic_resource_bars.provider;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ManaProviderRegistry {

    private static final List<Supplier<ManaProvider>> registeredProviders = new ArrayList<>();
    private static ManaProvider activeProvider = null;

    public static void registerProvider(Supplier<ManaProvider> providerSupplier) {
        registeredProviders.add(providerSupplier);
        // For now, automatically activate the first registered provider
        if (activeProvider == null) {
            setActiveProvider(providerSupplier.get()); 
        }
        // TODO: Log registration?
    }

    public static void setActiveProvider(@Nullable ManaProvider provider) {
        activeProvider = provider;
    }
    
    @Nullable
    public static ManaProvider getActiveProvider() {
        if (activeProvider == null && !registeredProviders.isEmpty()) {
             setActiveProvider(registeredProviders.get(0).get());
        }
        return activeProvider;
    }

    public static boolean hasProviders() {
        return !registeredProviders.isEmpty();
    }

    public static List<Supplier<ManaProvider>> getRegisteredProviders() {
        return List.copyOf(registeredProviders); // Return immutable copy
    }

    // Called during game load/unload or config change potentially
    public static void clear() {
        registeredProviders.clear();
        activeProvider = null;
    }
} 