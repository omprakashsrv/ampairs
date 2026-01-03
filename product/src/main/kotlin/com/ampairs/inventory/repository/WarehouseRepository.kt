package com.ampairs.inventory.repository

import com.ampairs.inventory.domain.model.Warehouse
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository

/**
 * Warehouse Repository
 *
 * Data access layer for Warehouse entities with multi-tenant support.
 * Provides standard CRUD operations and custom queries for warehouse management.
 *
 * Key Features:
 * - Automatic tenant filtering via @TenantId
 * - Unique warehouse code validation within tenant
 * - Default warehouse retrieval
 * - Active warehouse filtering
 */
@Repository
interface WarehouseRepository : CrudRepository<Warehouse, Long> {

    /**
     * Find warehouse by unique identifier (UID)
     *
     * @param uid Warehouse unique identifier
     * @return Warehouse if found, null otherwise
     */
    fun findByUid(uid: String): Warehouse?

    /**
     * Find warehouse by code within current tenant
     *
     * Warehouse codes are unique per tenant via unique constraint
     *
     * @param code Warehouse code
     * @return Warehouse if found, null otherwise
     */
    fun findByCode(code: String): Warehouse?

    /**
     * Find all active warehouses for current tenant
     *
     * @return List of active warehouses
     */
    fun findByIsActiveTrue(): List<Warehouse>

    /**
     * Find the default warehouse for current tenant
     *
     * Only one warehouse should be marked as default per tenant
     *
     * @return Default warehouse if exists, null otherwise
     */
    fun findByIsDefaultTrue(): Warehouse?

    /**
     * Find all warehouses (active and inactive) for current tenant
     *
     * @return List of all warehouses
     */
    override fun findAll(): MutableList<Warehouse>

    /**
     * Check if warehouse code already exists for current tenant
     *
     * @param code Warehouse code to check
     * @return true if code exists, false otherwise
     */
    fun existsByCode(code: String): Boolean

    /**
     * Count total warehouses for current tenant
     *
     * @return Count of warehouses
     */
    override fun count(): Long
}
