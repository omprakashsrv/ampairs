package com.ampairs.workspace.navigation

import androidx.navigation.NavController

/**
 * Interface for modules to provide their navigation information
 * Each module implements this to register with the ModuleRegistry
 */
interface IModuleNavigationProvider {
    /**
     * Module code that matches the backend module code (e.g., "customer-management")
     */
    val moduleCode: String

    /**
     * Display name for the module
     */
    val displayName: String

    /**
     * Default route to navigate to when module is selected
     */
    val defaultRoute: Any

    /**
     * Check if this module's navigation implementation is available
     */
    fun isAvailable(): Boolean = true

    /**
     * Get the route for a specific feature within the module
     * Returns null if feature is not supported
     */
    fun getFeatureRoute(feature: String): Any? = null
}

/**
 * Central registry for all available module navigation providers
 * Handles dynamic discovery and mapping of module codes to navigation routes
 */
object ModuleRegistry {

    private val providers = mutableMapOf<String, IModuleNavigationProvider>()

    /**
     * Register a module navigation provider
     */
    fun register(provider: IModuleNavigationProvider) {
        providers[provider.moduleCode] = provider
    }

    /**
     * Get navigation provider for a module code
     */
    fun getProvider(moduleCode: String): IModuleNavigationProvider? {
        return providers[moduleCode]
    }

    /**
     * Get all registered module codes
     */
    fun getRegisteredModules(): List<String> {
        return providers.keys.toList()
    }

    /**
     * Get all available module providers (where implementation exists)
     */
    fun getAvailableProviders(): List<IModuleNavigationProvider> {
        return providers.values.filter { it.isAvailable() }
    }

    /**
     * Check if a module code has a registered navigation provider
     */
    fun hasProvider(moduleCode: String): Boolean {
        return providers.containsKey(moduleCode) && providers[moduleCode]?.isAvailable() == true
    }

    /**
     * Get default navigation route for a module
     * Returns null if module is not registered or not available
     */
    fun getDefaultRoute(moduleCode: String): Any? {
        return getProvider(moduleCode)?.takeIf { it.isAvailable() }?.defaultRoute
    }

    /**
     * Get feature route for a specific module and feature
     */
    fun getFeatureRoute(moduleCode: String, feature: String): Any? {
        return getProvider(moduleCode)?.takeIf { it.isAvailable() }?.getFeatureRoute(feature)
    }

    /**
     * Navigate to a module's default route
     * Returns true if navigation was successful, false if module not found
     */
    fun navigateToModule(navController: NavController, moduleCode: String): Boolean {
        val route = getDefaultRoute(moduleCode)
        return if (route != null) {
            navController.navigate(route)
            true
        } else {
            false
        }
    }

    /**
     * Navigate to a specific feature within a module
     * Falls back to default route if feature not found
     * Returns true if navigation was successful, false if module not found
     */
    fun navigateToFeature(navController: NavController, moduleCode: String, feature: String): Boolean {
        val provider = getProvider(moduleCode)?.takeIf { it.isAvailable() }
            ?: return false

        val route = provider.getFeatureRoute(feature) ?: provider.defaultRoute
        navController.navigate(route)
        return true
    }

    /**
     * Initialize registry with all available module providers
     * Should be called during app initialization
     */
    fun initialize() {
        // Clear any existing registrations
        providers.clear()

        // Register all available module providers
        // Note: These will be registered by the ModuleProviders.kt file
        println("ModuleRegistry: Initialized with ${providers.size} providers")
    }
}

/**
 * Result of a module navigation attempt
 */
sealed class ModuleNavigationResult {
    object Success : ModuleNavigationResult()
    data class ModuleNotFound(val moduleCode: String) : ModuleNavigationResult()
    data class ModuleNotAvailable(val moduleCode: String, val displayName: String) : ModuleNavigationResult()
}

/**
 * Enhanced module navigation with detailed results
 */
fun ModuleRegistry.navigateToModuleWithResult(
    navController: NavController,
    moduleCode: String
): ModuleNavigationResult {
    val provider = getProvider(moduleCode)

    return when {
        provider == null -> ModuleNavigationResult.ModuleNotFound(moduleCode)
        !provider.isAvailable() -> ModuleNavigationResult.ModuleNotAvailable(moduleCode, provider.displayName)
        else -> {
            navController.navigate(provider.defaultRoute)
            ModuleNavigationResult.Success
        }
    }
}