package com.ampairs.core.multitenancy

/**
 * Interface for entities or objects that are tenant-aware
 * This allows components to provide their tenant identifier
 */
interface TenantAware {
    /**
     * Get the tenant identifier for this object
     * @return tenant ID or null if not applicable
     */
    fun getTenantId(): String?
}