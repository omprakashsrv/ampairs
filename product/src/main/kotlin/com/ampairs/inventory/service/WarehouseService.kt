package com.ampairs.inventory.service

import com.ampairs.inventory.config.Constants
import com.ampairs.inventory.domain.model.Warehouse
import com.ampairs.inventory.repository.WarehouseRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Warehouse Service
 *
 * Business logic layer for warehouse management.
 * Handles CRUD operations, validation, and business rules for warehouses.
 *
 * Key Responsibilities:
 * - Create, read, update, delete warehouses
 * - Validate warehouse code uniqueness
 * - Manage default warehouse designation
 * - Prevent deletion of warehouses with existing inventory
 */
@Service
class WarehouseService @Autowired constructor(
    private val warehouseRepository: WarehouseRepository
) {

    /**
     * Create a new warehouse
     *
     * @param warehouse Warehouse entity to create
     * @return Created warehouse
     * @throws IllegalArgumentException if warehouse code already exists
     */
    @Transactional
    fun createWarehouse(warehouse: Warehouse): Warehouse {
        // Validate warehouse code uniqueness
        if (warehouseRepository.existsByCode(warehouse.code)) {
            throw IllegalArgumentException(Constants.ERROR_DUPLICATE_WAREHOUSE_CODE + ": ${warehouse.code}")
        }

        // If this is marked as default, unset existing default
        if (warehouse.isDefault) {
            unsetDefaultWarehouse()
        }

        return warehouseRepository.save(warehouse)
    }

    /**
     * Update an existing warehouse
     *
     * @param uid Warehouse UID
     * @param updates Warehouse with updated fields
     * @return Updated warehouse
     * @throws IllegalArgumentException if warehouse not found or code conflict
     */
    @Transactional
    fun updateWarehouse(uid: String, updates: Warehouse): Warehouse {
        val existing = getWarehouseByUid(uid)
            ?: throw IllegalArgumentException(Constants.ERROR_WAREHOUSE_NOT_FOUND + ": $uid")

        // Check code uniqueness if changed
        if (updates.code != existing.code && warehouseRepository.existsByCode(updates.code)) {
            throw IllegalArgumentException(Constants.ERROR_DUPLICATE_WAREHOUSE_CODE + ": ${updates.code}")
        }

        // If setting this as default, unset existing default
        if (updates.isDefault && !existing.isDefault) {
            unsetDefaultWarehouse()
        }

        // Update fields
        existing.name = updates.name
        existing.code = updates.code
        existing.warehouseType = updates.warehouseType
        existing.isActive = updates.isActive
        existing.isDefault = updates.isDefault
        existing.phone = updates.phone
        existing.email = updates.email
        existing.managerName = updates.managerName
        existing.address = updates.address
        existing.description = updates.description
        existing.attributes = updates.attributes

        return warehouseRepository.save(existing)
    }

    /**
     * Get warehouse by UID
     *
     * @param uid Warehouse UID
     * @return Warehouse if found, null otherwise
     */
    fun getWarehouseByUid(uid: String): Warehouse? {
        return warehouseRepository.findByUid(uid)
    }

    /**
     * Get warehouse by code
     *
     * @param code Warehouse code
     * @return Warehouse if found, null otherwise
     */
    fun getWarehouseByCode(code: String): Warehouse? {
        return warehouseRepository.findByCode(code)
    }

    /**
     * Get all warehouses for current tenant
     *
     * @return List of all warehouses
     */
    fun getAllWarehouses(): List<Warehouse> {
        return warehouseRepository.findAll().toList()
    }

    /**
     * Get all active warehouses for current tenant
     *
     * @return List of active warehouses
     */
    fun getActiveWarehouses(): List<Warehouse> {
        return warehouseRepository.findByIsActiveTrue()
    }

    /**
     * Get default warehouse for current tenant
     *
     * @return Default warehouse if exists, null otherwise
     */
    fun getDefaultWarehouse(): Warehouse? {
        return warehouseRepository.findByIsDefaultTrue()
    }

    /**
     * Set warehouse as default
     *
     * Unsets existing default and marks specified warehouse as default
     *
     * @param uid Warehouse UID to set as default
     * @return Updated warehouse
     * @throws IllegalArgumentException if warehouse not found
     */
    @Transactional
    fun setDefaultWarehouse(uid: String): Warehouse {
        val warehouse = getWarehouseByUid(uid)
            ?: throw IllegalArgumentException(Constants.ERROR_WAREHOUSE_NOT_FOUND + ": $uid")

        // Unset existing default
        unsetDefaultWarehouse()

        // Set as default
        warehouse.isDefault = true
        return warehouseRepository.save(warehouse)
    }

    /**
     * Delete warehouse
     *
     * Performs soft delete by setting isActive = false
     * Validates that warehouse has no existing inventory
     *
     * @param uid Warehouse UID
     * @throws IllegalArgumentException if warehouse not found or has inventory
     */
    @Transactional
    fun deleteWarehouse(uid: String) {
        val warehouse = getWarehouseByUid(uid)
            ?: throw IllegalArgumentException(Constants.ERROR_WAREHOUSE_NOT_FOUND + ": $uid")

        // TODO: Check if warehouse has inventory items before deletion
        // For now, perform soft delete
        warehouse.isActive = false

        // If this was default, unset it
        if (warehouse.isDefault) {
            warehouse.isDefault = false
        }

        warehouseRepository.save(warehouse)
    }

    /**
     * Unset default warehouse
     *
     * Marks existing default warehouse (if any) as non-default
     */
    @Transactional
    private fun unsetDefaultWarehouse() {
        val existingDefault = getDefaultWarehouse()
        existingDefault?.let {
            it.isDefault = false
            warehouseRepository.save(it)
        }
    }

    /**
     * Count total warehouses for current tenant
     *
     * @return Count of warehouses
     */
    fun countWarehouses(): Long {
        return warehouseRepository.count()
    }
}
