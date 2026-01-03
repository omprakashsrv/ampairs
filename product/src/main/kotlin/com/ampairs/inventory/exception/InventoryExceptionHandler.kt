package com.ampairs.inventory.exception

import com.ampairs.core.domain.dto.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.context.request.WebRequest
import java.time.Instant

/**
 * Inventory Exception Handler
 *
 * Global exception handler for all inventory-related exceptions.
 * Converts exceptions to standardized API responses with appropriate HTTP status codes.
 *
 * Exception Handling Strategy:
 * - Not Found exceptions → 404 NOT FOUND
 * - Validation/Business Logic exceptions → 400 BAD REQUEST
 * - Insufficient Stock/Resources → 409 CONFLICT
 * - Duplicate entries → 409 CONFLICT
 * - General inventory exceptions → 400 BAD REQUEST
 *
 * All responses use the ApiResponse wrapper for consistency.
 */
@RestControllerAdvice
class InventoryExceptionHandler {

    private val logger = LoggerFactory.getLogger(InventoryExceptionHandler::class.java)

    // ============================================================================
    // Not Found Exceptions (404)
    // ============================================================================

    @ExceptionHandler(
        WarehouseNotFoundException::class,
        InventoryItemNotFoundException::class,
        BatchNotFoundException::class,
        SerialNumberNotFoundException::class,
        TransactionNotFoundException::class,
        LedgerNotFoundException::class
    )
    fun handleNotFoundException(
        ex: InventoryException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Resource not found: ${ex.message}")

        val errorCode = when (ex) {
            is WarehouseNotFoundException -> "WAREHOUSE_NOT_FOUND"
            is InventoryItemNotFoundException -> "INVENTORY_ITEM_NOT_FOUND"
            is BatchNotFoundException -> "BATCH_NOT_FOUND"
            is SerialNumberNotFoundException -> "SERIAL_NUMBER_NOT_FOUND"
            is TransactionNotFoundException -> "TRANSACTION_NOT_FOUND"
            is LedgerNotFoundException -> "LEDGER_NOT_FOUND"
            else -> "RESOURCE_NOT_FOUND"
        }

        val response = ApiResponse.error<Nothing>(
            code = errorCode,
            message = ex.message ?: "Resource not found"
        )

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response)
    }

    // ============================================================================
    // Conflict Exceptions (409)
    // ============================================================================

    @ExceptionHandler(
        InsufficientStockException::class,
        InsufficientBatchStockException::class,
        InsufficientSerialsException::class,
        DuplicateWarehouseCodeException::class,
        DuplicateSKUException::class,
        DuplicateBatchNumberException::class,
        DuplicateSerialNumberException::class,
        WarehouseHasInventoryException::class,
        BatchHasStockException::class
    )
    fun handleConflictException(
        ex: InventoryException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Conflict: ${ex.message}")

        val errorCode = when (ex) {
            is InsufficientStockException -> "INSUFFICIENT_STOCK"
            is InsufficientBatchStockException -> "INSUFFICIENT_BATCH_STOCK"
            is InsufficientSerialsException -> "INSUFFICIENT_SERIALS"
            is DuplicateWarehouseCodeException -> "DUPLICATE_WAREHOUSE_CODE"
            is DuplicateSKUException -> "DUPLICATE_SKU"
            is DuplicateBatchNumberException -> "DUPLICATE_BATCH_NUMBER"
            is DuplicateSerialNumberException -> "DUPLICATE_SERIAL_NUMBER"
            is WarehouseHasInventoryException -> "WAREHOUSE_HAS_INVENTORY"
            is BatchHasStockException -> "BATCH_HAS_STOCK"
            else -> "CONFLICT"
        }

        val response = ApiResponse.error<Nothing>(
            code = errorCode,
            message = ex.message ?: "Conflict occurred"
        )

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response)
    }

    // ============================================================================
    // Bad Request Exceptions (400)
    // ============================================================================

    @ExceptionHandler(
        NegativeStockNotAllowedException::class,
        InvalidSerialStatusException::class,
        SerialNumberRequiredException::class,
        InvalidTransactionException::class,
        TransactionValidationException::class,
        SameWarehouseTransferException::class,
        BatchExpiredException::class,
        InvalidConfigurationException::class,
        ConfigurationNotFoundException::class,
        LedgerGenerationException::class
    )
    fun handleBadRequestException(
        ex: InventoryException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn("Bad request: ${ex.message}")

        val errorCode = when (ex) {
            is NegativeStockNotAllowedException -> "NEGATIVE_STOCK_NOT_ALLOWED"
            is InvalidSerialStatusException -> "INVALID_SERIAL_STATUS"
            is SerialNumberRequiredException -> "SERIAL_NUMBER_REQUIRED"
            is InvalidTransactionException -> "INVALID_TRANSACTION"
            is TransactionValidationException -> "TRANSACTION_VALIDATION_FAILED"
            is SameWarehouseTransferException -> "SAME_WAREHOUSE_TRANSFER"
            is BatchExpiredException -> "BATCH_EXPIRED"
            is InvalidConfigurationException -> "INVALID_CONFIGURATION"
            is ConfigurationNotFoundException -> "CONFIGURATION_NOT_FOUND"
            is LedgerGenerationException -> "LEDGER_GENERATION_FAILED"
            else -> "BAD_REQUEST"
        }

        val response = ApiResponse.error<Nothing>(
            code = errorCode,
            message = ex.message ?: "Invalid request"
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    // ============================================================================
    // General Inventory Exception (400)
    // ============================================================================

    @ExceptionHandler(InventoryException::class)
    fun handleInventoryException(
        ex: InventoryException,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Inventory exception: ${ex.message}", ex)

        val response = ApiResponse.error<Nothing>(
            code = "INVENTORY_ERROR",
            message = ex.message ?: "An inventory error occurred"
        )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response)
    }

    // ============================================================================
    // General Exception Handler (500)
    // ============================================================================

    @ExceptionHandler(Exception::class)
    fun handleGeneralException(
        ex: Exception,
        request: WebRequest
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.error("Unexpected error in inventory module: ${ex.message}", ex)

        val response = ApiResponse.error<Nothing>(
            code = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred. Please try again or contact support."
        )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response)
    }
}
