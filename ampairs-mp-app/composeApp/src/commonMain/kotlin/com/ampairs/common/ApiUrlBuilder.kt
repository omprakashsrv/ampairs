package com.ampairs.common

import com.ampairs.common.config.ConfigurationManager

/**
 * Utility class for building API URLs with the configured base URL
 */
object ApiUrlBuilder {

    /**
     * Build complete API URL for authentication endpoints
     */
    fun authUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/$cleanPath"
    }

    /**
     * Build complete API URL for versioned endpoints (v1)
     */
    fun apiUrl(path: String): String {
        return ConfigurationManager.getApiUrl(path)
    }

    /**
     * Build complete API URL for workspace endpoints
     */
    fun workspaceUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/workspace/$cleanPath"
    }

    /**
     * Build complete API URL for user endpoints
     */
    fun userUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/user/$cleanPath"
    }

    /**
     * Build complete API URL for customer endpoints
     */
    fun customerUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/customer/$cleanPath"
    }

    /**
     * Build complete API URL for product endpoints
     */
    fun productUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/product/$cleanPath"
    }

    /**
     * Build complete API URL for order endpoints
     */
    fun orderUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/order/$cleanPath"
    }

    /**
     * Build complete API URL for invoice endpoints
     */
    fun invoiceUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/invoice/$cleanPath"
    }

    /**
     * Build complete API URL for inventory endpoints
     */
    fun inventoryUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.apiBaseUrl}/inventory/$cleanPath"
    }

    /**
     * Build WebSocket URL
     */
    fun wsUrl(path: String): String {
        val cleanPath = path.removePrefix("/")
        return "${ConfigurationManager.current.wsBaseUrl}/$cleanPath"
    }
}