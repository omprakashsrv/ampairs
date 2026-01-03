package com.ampairs.inventory.domain.dto

import com.ampairs.core.domain.model.Address
import com.ampairs.inventory.domain.model.Warehouse
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

/**
 * Warehouse Request DTO
 *
 * Used for creating and updating warehouses.
 * Validation ensures data integrity before persistence.
 */
data class WarehouseRequest(

    @field:NotBlank(message = "Warehouse name is required")
    @field:Size(min = 2, max = 200, message = "Warehouse name must be between 2 and 200 characters")
    var name: String = "",

    @field:NotBlank(message = "Warehouse code is required")
    @field:Size(min = 1, max = 50, message = "Warehouse code must be between 1 and 50 characters")
    var code: String = "",

    var warehouseType: String = "WAREHOUSE",  // WAREHOUSE, STORE, GODOWN, SHOWROOM, FACTORY

    var isActive: Boolean = true,

    var isDefault: Boolean = false,

    @field:Size(max = 20, message = "Phone number must not exceed 20 characters")
    var phone: String? = null,

    @field:Size(max = 100, message = "Email must not exceed 100 characters")
    var email: String? = null,

    @field:Size(max = 100, message = "Manager name must not exceed 100 characters")
    var managerName: String? = null,

    var address: Address = Address(),

    @field:Size(max = 500, message = "Description must not exceed 500 characters")
    var description: String? = null,

    var attributes: Map<String, Any>? = null
)

/**
 * Warehouse Response DTO
 *
 * Used for API responses. Exposes only relevant fields to clients.
 * Includes timestamps for synchronization and audit purposes.
 */
data class WarehouseResponse(
    val uid: String,
    val name: String,
    val code: String,
    val warehouseType: String,
    val isActive: Boolean,
    val isDefault: Boolean,
    val phone: String?,
    val email: String?,
    val managerName: String?,
    val address: Address,
    val description: String?,
    val attributes: Map<String, Any>?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)

// Extension Functions for DTO Conversion

/**
 * Convert WarehouseRequest to Warehouse entity
 */
fun WarehouseRequest.toWarehouse(): Warehouse {
    val warehouse = Warehouse()
    warehouse.name = this.name.trim()
    warehouse.code = this.code.trim().uppercase()  // Normalize code to uppercase
    warehouse.warehouseType = this.warehouseType
    warehouse.isActive = this.isActive
    warehouse.isDefault = this.isDefault
    warehouse.phone = this.phone?.trim()
    warehouse.email = this.email?.trim()
    warehouse.managerName = this.managerName?.trim()
    warehouse.address = this.address
    warehouse.description = this.description?.trim()
    warehouse.attributes = this.attributes
    return warehouse
}

/**
 * Convert Warehouse entity to WarehouseResponse DTO
 */
fun Warehouse.asWarehouseResponse(): WarehouseResponse {
    return WarehouseResponse(
        uid = this.uid,
        name = this.name,
        code = this.code,
        warehouseType = this.warehouseType,
        isActive = this.isActive,
        isDefault = this.isDefault,
        phone = this.phone,
        email = this.email,
        managerName = this.managerName,
        address = this.address,
        description = this.description,
        attributes = this.attributes,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * Convert list of Warehouse entities to list of WarehouseResponse DTOs
 */
fun List<Warehouse>.asWarehouseResponses(): List<WarehouseResponse> {
    return this.map { it.asWarehouseResponse() }
}
