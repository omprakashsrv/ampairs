package com.ampairs.inventory.domain.dto

import com.ampairs.core.validation.Alphanumeric
import com.ampairs.core.validation.SafeString
import com.ampairs.core.validation.ValidPrice
import com.ampairs.inventory.domain.model.Inventory
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size

data class InventoryRequest(
    @field:SafeString(maxLength = 50, message = "ID contains invalid characters")
    @field:Size(max = 50, message = "ID cannot exceed 50 characters")
    val id: String?,

    @field:SafeString(maxLength = 50, message = "Reference ID contains invalid characters")
    @field:Size(max = 50, message = "Reference ID cannot exceed 50 characters")
    val refId: String?,

    @field:SafeString(maxLength = 50, message = "Product ID contains invalid characters")
    @field:Size(max = 50, message = "Product ID cannot exceed 50 characters")
    val productId: String?,

    @field:SafeString(maxLength = 50, message = "Product Reference ID contains invalid characters")
    @field:Size(max = 50, message = "Product Reference ID cannot exceed 50 characters")
    val productRefId: String?,

    @field:SafeString(maxLength = 500, message = "Description contains invalid characters")
    @field:Size(max = 500, message = "Description cannot exceed 500 characters")
    val description: String? = "",

    @field:Alphanumeric
    @field:Size(max = 50, message = "Code cannot exceed 50 characters")
    val code: String?,

    @field:SafeString(maxLength = 20, message = "Tax code contains invalid characters")
    @field:Size(max = 20, message = "Tax code cannot exceed 20 characters")
    val taxCode: String?,

    @field:SafeString(maxLength = 50, message = "Base unit ID contains invalid characters")
    @field:Size(max = 50, message = "Base unit ID cannot exceed 50 characters")
    val baseUnitId: String?,

    val active: Boolean? = true,

    @field:ValidPrice(
        min = 0.0,
        max = 9999999.99,
        message = "MRP must be a positive number with up to 2 decimal places"
    )
    val mrp: Double? = 0.0,

    @field:ValidPrice(min = 0.0, max = 9999999.99, message = "DP must be a positive number with up to 2 decimal places")
    val dp: Double? = 0.0,

    @field:DecimalMin(value = "0.0", message = "Stock cannot be negative")
    @field:DecimalMax(value = "999999999.99", message = "Stock value too large")
    val stock: Double? = 0.0,

    @field:ValidPrice(
        min = 0.0,
        max = 9999999.99,
        message = "Selling price must be a positive number with up to 2 decimal places"
    )
    val sellingPrice: Double? = 0.0,

    @field:ValidPrice(
        min = 0.0,
        max = 9999999.99,
        message = "Buying price must be a positive number with up to 2 decimal places"
    )
    val buyingPrice: Double? = 0.0,

    @field:Min(value = 0, message = "Last updated timestamp cannot be negative")
    val lastUpdated: Long?,

    @field:SafeString(maxLength = 30, message = "Created at contains invalid characters")
    val createdAt: String?,

    @field:SafeString(maxLength = 30, message = "Updated at contains invalid characters")
    val updatedAt: String?,
)

fun List<InventoryRequest>.asDatabaseModel(): List<Inventory> {
    return map {
        it.asDatabaseModel()
    }
}

fun InventoryRequest.asDatabaseModel(): Inventory {
    val inventory = Inventory()
    inventory.uid = this.id ?: ""
    inventory.refId = this.refId
    inventory.productId = this.productId ?: ""
    inventory.unitId = this.baseUnitId
    inventory.description = this.description ?: ""
    inventory.active = this.active ?: true
    inventory.mrp = this.mrp ?: 0.0
    inventory.dp = this.dp ?: 0.0
    inventory.stock = this.stock ?: 0.0
    inventory.sellingPrice = this.sellingPrice ?: 0.0
    inventory.buyingPrice = this.buyingPrice ?: 0.0
    return inventory
}