package com.ampairs.core.multitenancy

import org.slf4j.LoggerFactory
import org.springframework.security.core.context.SecurityContextHolder

/**
 * Thread-safe device context holder for tracking device-specific operations
 * Used for multi-device event synchronization
 */
object DeviceContextHolder {

    private val logger = LoggerFactory.getLogger(DeviceContextHolder::class.java)

    // ThreadLocal for device context
    private val deviceThreadLocal: ThreadLocal<String?> = InheritableThreadLocal()

    const val DEVICE_ATTRIBUTE = "deviceId"

    /**
     * Get current device identifier
     * First tries Spring Security context, then falls back to ThreadLocal
     */
    fun getCurrentDevice(): String? {
        return try {
            // Try SecurityContext first (preferred approach)
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication?.details is Map<*, *>) {
                val details = authentication.details as Map<*, *>
                val deviceId = details[DEVICE_ATTRIBUTE] as? String
                if (!deviceId.isNullOrBlank()) {
                    return deviceId
                }
            }

            // Fallback to ThreadLocal
            deviceThreadLocal.get()
        } catch (e: Exception) {
            logger.warn("Error getting current device, falling back to ThreadLocal", e)
            deviceThreadLocal.get()
        }
    }

    /**
     * Set current device identifier
     * Sets both SecurityContext and ThreadLocal for maximum compatibility
     */
    fun setCurrentDevice(deviceId: String?) {
        try {
            // Set in ThreadLocal
            if (deviceId != null) {
                deviceThreadLocal.set(deviceId)
                logger.debug("Device set in ThreadLocal: {}", deviceId)
            } else {
                deviceThreadLocal.remove()
                logger.debug("Device cleared from ThreadLocal")
            }

            // Try to set in SecurityContext if available
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication != null) {
                setDeviceInSecurityContext(deviceId)
            }

        } catch (e: Exception) {
            logger.warn("Error setting device context", e)
        }
    }

    /**
     * Clear device context
     */
    fun clearDeviceContext() {
        deviceThreadLocal.remove()
        clearDeviceFromSecurityContext()
        logger.debug("Device context cleared")
    }

    /**
     * Execute code block with specific device context
     */
    fun <T> withDevice(deviceId: String, block: () -> T): T {
        val originalDevice = getCurrentDevice()
        return try {
            setCurrentDevice(deviceId)
            block()
        } finally {
            if (originalDevice != null) {
                setCurrentDevice(originalDevice)
            } else {
                clearDeviceContext()
            }
        }
    }

    /**
     * Get current device or return default value
     */
    fun getCurrentDeviceOrDefault(default: String = "unknown"): String {
        return getCurrentDevice() ?: default
    }

    /**
     * Check if device is currently set
     */
    fun hasDevice(): Boolean {
        return getCurrentDevice() != null
    }

    // Private helper methods

    private fun setDeviceInSecurityContext(deviceId: String?) {
        try {
            val authentication = SecurityContextHolder.getContext().authentication
            if (authentication != null) {
                // Create or update details map with device information
                val details = when (val existingDetails = authentication.details) {
                    is MutableMap<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        existingDetails as MutableMap<String, Any?>
                    }

                    is Map<*, *> -> {
                        @Suppress("UNCHECKED_CAST")
                        (existingDetails as Map<String, Any?>).toMutableMap()
                    }

                    else -> mutableMapOf<String, Any?>()
                }

                if (deviceId != null) {
                    details[DEVICE_ATTRIBUTE] = deviceId
                    logger.debug("Device set in SecurityContext: {}", deviceId)
                } else {
                    details.remove(DEVICE_ATTRIBUTE)
                    logger.debug("Device cleared from SecurityContext")
                }
            }
        } catch (e: Exception) {
            logger.debug("Could not set device in SecurityContext (this is normal for some contexts)", e)
        }
    }

    private fun clearDeviceFromSecurityContext() {
        setDeviceInSecurityContext(null)
    }
}
