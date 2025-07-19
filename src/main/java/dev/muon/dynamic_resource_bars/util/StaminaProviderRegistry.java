package dev.muon.dynamic_resource_bars.util;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class StaminaProviderRegistry {

    // Use Supplier to potentially defer provider instantiation if needed
    private static final List<Supplier<StaminaProvider>> registeredProviders = new ArrayList<>();
    private static StaminaProvider activeProvider = null;

    public static void registerProvider(Supplier<StaminaProvider> providerSupplier) {
        registeredProviders.add(providerSupplier);
        // For now, automatically activate the first registered provider
        if (activeProvider == null) {
            setActiveProvider(providerSupplier.get()); 
        }
    }

    public static void setActiveProvider(@Nullable StaminaProvider provider) {
        activeProvider = provider;
    }
    
    @Nullable
    public static StaminaProvider getActiveProvider() {
         // Ensure the active provider is instantiated if it hasn't been already
        if (activeProvider == null && !registeredProviders.isEmpty()) {
            // Activate the first one by default if none is active
             setActiveProvider(registeredProviders.get(0).get());
        }
        return activeProvider;
    }

    public static boolean hasProviders() {
        return !registeredProviders.isEmpty();
    }

    public static List<Supplier<StaminaProvider>> getRegisteredProviders() {
        return List.copyOf(registeredProviders); // Return immutable copy
    }

    // Called during game load/unload or config change potentially
    public static void clear() {
        registeredProviders.clear();
        activeProvider = null;
    }
} 