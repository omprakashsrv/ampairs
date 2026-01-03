package com.ampairs.inventory.exception

/**
 * Base exception class for all inventory-related exceptions
 */
open class InventoryException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)

// ============================================================================
// Warehouse Exceptions
// ============================================================================

/**
 * Thrown when a warehouse is not found
 */
class WarehouseNotFoundException(
    warehouseId: String,
    cause: Throwable? = null
) : InventoryException("Warehouse not found: $warehouseId", cause)

/**
 * Thrown when attempting to create a warehouse with duplicate code
 */
class DuplicateWarehouseCodeException(
    code: String,
    cause: Throwable? = null
) : InventoryException("Warehouse code already exists: $code", cause)

/**
 * Thrown when attempting to delete a warehouse that has inventory
 */
class WarehouseHasInventoryException(
    warehouseId: String,
    itemCount: Long,
    cause: Throwable? = null
) : InventoryException(
    "Cannot delete warehouse $warehouseId: contains $itemCount inventory items. " +
    "Please move or remove all inventory before deleting.",
    cause
)

// ============================================================================
// Inventory Item Exceptions
// ============================================================================

/**
 * Thrown when an inventory item is not found
 */
class InventoryItemNotFoundException(
    itemId: String,
    cause: Throwable? = null
) : InventoryException("Inventory item not found: $itemId", cause)

/**
 * Thrown when insufficient stock is available for an operation
 */
class InsufficientStockException(
    itemId: String,
    itemName: String?,
    available: String,
    requested: String,
    cause: Throwable? = null
) : InventoryException(
    "Insufficient stock for item ${itemName ?: itemId}. " +
    "Available: $available, Requested: $requested",
    cause
)

/**
 * Thrown when negative stock is not allowed but would result from an operation
 */
class NegativeStockNotAllowedException(
    itemId: String,
    resultingStock: String,
    cause: Throwable? = null
) : InventoryException(
    "Operation would result in negative stock for item $itemId: $resultingStock. " +
    "Negative stock is not allowed in current configuration.",
    cause
)

/**
 * Thrown when attempting to create duplicate SKU
 */
class DuplicateSKUException(
    sku: String,
    cause: Throwable? = null
) : InventoryException("SKU already exists: $sku", cause)

// ============================================================================
// Batch Exceptions
// ============================================================================

/**
 * Thrown when a batch is not found
 */
class BatchNotFoundException(
    batchId: String,
    cause: Throwable? = null
) : InventoryException("Batch not found: $batchId", cause)

/**
 * Thrown when attempting to create a batch with duplicate batch number
 */
class DuplicateBatchNumberException(
    batchNumber: String,
    itemId: String,
    warehouseId: String,
    cause: Throwable? = null
) : InventoryException(
    "Batch number $batchNumber already exists for item $itemId in warehouse $warehouseId",
    cause
)

/**
 * Thrown when insufficient batch stock is available
 */
class InsufficientBatchStockException(
    batchId: String,
    batchNumber: String,
    available: String,
    requested: String,
    cause: Throwable? = null
) : InventoryException(
    "Insufficient stock in batch $batchNumber. " +
    "Available: $available, Requested: $requested",
    cause
)

/**
 * Thrown when attempting to delete a batch with remaining stock
 */
class BatchHasStockException(
    batchId: String,
    availableStock: String,
    reservedStock: String,
    cause: Throwable? = null
) : InventoryException(
    "Cannot delete batch $batchId with stock. " +
    "Available: $availableStock, Reserved: $reservedStock",
    cause
)

/**
 * Thrown when attempting to use an expired batch
 */
class BatchExpiredException(
    batchNumber: String,
    expiryDate: String,
    cause: Throwable? = null
) : InventoryException(
    "Batch $batchNumber has expired on $expiryDate",
    cause
)

// ============================================================================
// Serial Number Exceptions
// ============================================================================

/**
 * Thrown when a serial number is not found
 */
class SerialNumberNotFoundException(
    serialNumber: String,
    cause: Throwable? = null
) : InventoryException("Serial number not found: $serialNumber", cause)

/**
 * Thrown when attempting to create a duplicate serial number
 */
class DuplicateSerialNumberException(
    serialNumber: String,
    cause: Throwable? = null
) : InventoryException("Serial number already exists: $serialNumber", cause)

/**
 * Thrown when attempting an invalid serial status transition
 */
class InvalidSerialStatusException(
    serialNumber: String,
    currentStatus: String,
    attemptedOperation: String,
    cause: Throwable? = null
) : InventoryException(
    "Cannot $attemptedOperation serial $serialNumber with status $currentStatus",
    cause
)

/**
 * Thrown when serial number is required but not provided
 */
class SerialNumberRequiredException(
    itemId: String,
    itemName: String?,
    cause: Throwable? = null
) : InventoryException(
    "Serial number is required for item ${itemName ?: itemId} (serial tracking enabled)",
    cause
)

/**
 * Thrown when insufficient available serials exist
 */
class InsufficientSerialsException(
    itemId: String,
    warehouseId: String,
    available: Int,
    requested: Int,
    cause: Throwable? = null
) : InventoryException(
    "Insufficient available serials. Available: $available, Requested: $requested",
    cause
)

// ============================================================================
// Transaction Exceptions
// ============================================================================

/**
 * Thrown when a transaction is not found
 */
class TransactionNotFoundException(
    transactionId: String,
    cause: Throwable? = null
) : InventoryException("Transaction not found: $transactionId", cause)

/**
 * Thrown when attempting an invalid transaction
 */
class InvalidTransactionException(
    message: String,
    cause: Throwable? = null
) : InventoryException(message, cause)

/**
 * Thrown when transaction validation fails
 */
class TransactionValidationException(
    message: String,
    cause: Throwable? = null
) : InventoryException("Transaction validation failed: $message", cause)

/**
 * Thrown when attempting to transfer to the same warehouse
 */
class SameWarehouseTransferException(
    warehouseId: String,
    cause: Throwable? = null
) : InventoryException(
    "Cannot transfer to the same warehouse: $warehouseId. " +
    "Source and destination warehouses must be different.",
    cause
)

// ============================================================================
// Ledger Exceptions
// ============================================================================

/**
 * Thrown when a ledger entry is not found
 */
class LedgerNotFoundException(
    ledgerId: String,
    cause: Throwable? = null
) : InventoryException("Ledger entry not found: $ledgerId", cause)

/**
 * Thrown when ledger generation fails
 */
class LedgerGenerationException(
    date: String,
    message: String,
    cause: Throwable? = null
) : InventoryException(
    "Failed to generate ledger for $date: $message",
    cause
)

// ============================================================================
// Configuration Exceptions
// ============================================================================

/**
 * Thrown when inventory configuration is not found
 */
class ConfigurationNotFoundException(
    message: String = "Inventory configuration not found for tenant",
    cause: Throwable? = null
) : InventoryException(message, cause)

/**
 * Thrown when an invalid configuration value is provided
 */
class InvalidConfigurationException(
    setting: String,
    value: String,
    reason: String,
    cause: Throwable? = null
) : InventoryException(
    "Invalid configuration for $setting = '$value': $reason",
    cause
)
