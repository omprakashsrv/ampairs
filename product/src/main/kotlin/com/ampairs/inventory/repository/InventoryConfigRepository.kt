package com.ampairs.inventory.repository

import com.ampairs.inventory.domain.model.InventoryConfig
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

/**
 * Inventory Configuration Repository
 *
 * Data access layer for InventoryConfig entities with multi-tenant support.
 * Only one configuration per tenant (enforced by unique constraint).
 *
 * Key Features:
 * - Automatic tenant filtering via @TenantId
 * - Single configuration per tenant
 */
@Repository
interface InventoryConfigRepository : CrudRepository<InventoryConfig, Long> {

    /**
     * Find configuration by unique identifier (UID)
     *
     * @param uid Configuration unique identifier
     * @return InventoryConfig if found, null otherwise
     */
    fun findByUid(uid: String): InventoryConfig?

    /**
     * Find configuration for current tenant
     *
     * Since there's only one configuration per tenant (enforced by unique constraint),
     * this will return the single configuration or null if not yet created.
     *
     * @return InventoryConfig if exists, null otherwise
     */
    fun findFirstByOrderByCreatedAtDesc(): InventoryConfig?

    /**
     * Check if configuration exists for current tenant
     *
     * @return true if configuration exists, false otherwise
     */
    fun existsByOwnerId(ownerId: String): Boolean
}
