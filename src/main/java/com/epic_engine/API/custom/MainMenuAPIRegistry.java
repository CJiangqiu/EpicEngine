package com.epic_engine.API.custom;

import com.epic_engine.EpicEngineMod;
import com.epic_engine.config.EpicEngineCustomConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registry for external main menu component providers.
 * This class manages the registration and retrieval of custom components from other mods.
 */
public class MainMenuAPIRegistry {

    private static final List<IMainMenuComponentProvider> providers = new ArrayList<>();

    /**
     * Register a component provider for main menu integration.
     * This should be called during mod initialization, preferably in FMLCommonSetupEvent.
     *
     * @param provider The component provider to register
     */
    public static void registerComponentProvider(IMainMenuComponentProvider provider) {
        if (provider == null || provider.getModId() == null || provider.getModId().isEmpty()) {
            EpicEngineMod.LOGGER.warn("[EPIC ENGINE]: Invalid component provider registration - provider or mod ID is null/empty");
            return;
        }

        // Check if a provider for this mod is already registered
        boolean alreadyRegistered = providers.stream()
                .anyMatch(p -> p.getModId().equals(provider.getModId()));

        if (alreadyRegistered) {
            EpicEngineMod.LOGGER.warn("[EPIC ENGINE]: Component provider for mod '{}' is already registered, ignoring duplicate registration",
                    provider.getModId());
            return;
        }

        providers.add(provider);
        EpicEngineMod.LOGGER.info("[EPIC ENGINE]: Successfully registered main menu component provider for mod: {}",
                provider.getModId());
    }

    /**
     * Get all enabled component providers.
     * This method filters providers based on configuration settings and display conditions.
     *
     * @return List of enabled component providers
     */
    public static List<IMainMenuComponentProvider> getEnabledProviders() {
        // Check if external mod components are enabled in configuration
        if (!EpicEngineCustomConfig.EXTERNAL_MOD_COMPONENTS_ENABLED.get()) {
            return Collections.emptyList();
        }

        return providers.stream()
                .filter(provider -> {
                    try {
                        return provider.shouldDisplay();
                    } catch (Exception e) {
                        EpicEngineMod.LOGGER.error("[EPIC ENGINE]: Error checking display condition for mod '{}', excluding from display",
                                provider.getModId(), e);
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Get all registered component providers (regardless of enabled state).
     * This is mainly for debugging and administrative purposes.
     *
     * @return List of all registered providers
     */
    public static List<IMainMenuComponentProvider> getAllProviders() {
        return new ArrayList<>(providers);
    }

    /**
     * Get the number of registered component providers.
     *
     * @return Number of registered providers
     */
    public static int getRegisteredProviderCount() {
        return providers.size();
    }

    /**
     * Check if any component providers are registered.
     *
     * @return true if at least one provider is registered, false otherwise
     */
    public static boolean hasRegisteredProviders() {
        return !providers.isEmpty();
    }

    /**
     * Clear all registered providers.
     * This is mainly used for testing purposes and should not be called during normal operation.
     */
    public static void clearAllProviders() {
        providers.clear();
        EpicEngineMod.LOGGER.info("[EPIC ENGINE]: Cleared all registered component providers");
    }
}