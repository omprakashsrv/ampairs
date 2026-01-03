package com.ampairs.inventory.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.inventory.domain.dto.*
import com.ampairs.inventory.service.InventoryTransactionService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

/**
 * Inventory Transaction Controller
 *
 * REST API endpoints for inventory transaction management.
 * Handles all stock movements:
 * - Stock-in (purchases, returns, opening stock)
 * - Stock-out (sales, damages, losses)
 * - Transfers (between warehouses)
 * - Adjustments (corrections)
 * - Physical counts (inventory reconciliation)
 *
 * Base path: /inventory/v1/transactions
 */
@RestController
@RequestMapping("/inventory/v1/transactions")
class TransactionController @Autowired constructor(
    private val inventoryTransactionService: InventoryTransactionService
) {

    // ============================================================================
    // Stock-In Endpoints
    // ============================================================================

    /**
     * Process stock-in transaction
     *
     * POST /inventory/v1/transactions/stock-in
     *
     * Add stock to inventory (purchases, returns, opening stock)
     *
     * @param request Stock-in request
     * @return Created transaction
     */
    @PostMapping("/stock-in")
    fun stockIn(@Valid @RequestBody request: StockInRequest): ApiResponse<InventoryTransactionResponse> {
        val transaction = inventoryTransactionService.stockIn(request)
        return ApiResponse.success(transaction.asInventoryTransactionResponse())
    }

    // ============================================================================
    // Stock-Out Endpoints
    // ============================================================================

    /**
     * Process stock-out transaction
     *
     * POST /inventory/v1/transactions/stock-out
     *
     * Remove stock from inventory (sales, damages, losses)
     *
     * @param request Stock-out request
     * @return Created transaction
     */
    @PostMapping("/stock-out")
    fun stockOut(@Valid @RequestBody request: StockOutRequest): ApiResponse<InventoryTransactionResponse> {
        val transaction = inventoryTransactionService.stockOut(request)
        return ApiResponse.success(transaction.asInventoryTransactionResponse())
    }

    // ============================================================================
    // Transfer Endpoints
    // ============================================================================

    /**
     * Process stock transfer between warehouses
     *
     * POST /inventory/v1/transactions/transfer
     *
     * Transfer stock from one warehouse to another
     * Creates two transactions: stock-out from source, stock-in to destination
     *
     * @param request Stock transfer request
     * @return List of created transactions (out, in)
     */
    @PostMapping("/transfer")
    fun transfer(@Valid @RequestBody request: StockTransferRequest): ApiResponse<List<InventoryTransactionResponse>> {
        val transactions = inventoryTransactionService.transferStock(request)
        return ApiResponse.success(transactions.asInventoryTransactionResponses())
    }

    // ============================================================================
    // Adjustment Endpoints
    // ============================================================================

    /**
     * Process stock adjustment
     *
     * POST /inventory/v1/transactions/adjustment
     *
     * Manually adjust stock levels (positive or negative)
     * Used for corrections, write-offs, found stock, etc.
     *
     * @param request Stock adjustment request
     * @return Created transaction
     */
    @PostMapping("/adjustment")
    fun adjustment(@Valid @RequestBody request: StockAdjustmentRequest): ApiResponse<InventoryTransactionResponse> {
        val transaction = inventoryTransactionService.adjustStock(request)
        return ApiResponse.success(transaction.asInventoryTransactionResponse())
    }

    // ============================================================================
    // Physical Count Endpoints
    // ============================================================================

    /**
     * Process physical count
     *
     * POST /inventory/v1/transactions/physical-count
     *
     * Reconcile physical count with system stock
     * Automatically creates adjustment transaction for the difference
     *
     * @param request Physical count request
     * @return Created transaction
     */
    @PostMapping("/physical-count")
    fun physicalCount(@Valid @RequestBody request: PhysicalCountRequest): ApiResponse<InventoryTransactionResponse> {
        val transaction = inventoryTransactionService.physicalCount(request)
        return ApiResponse.success(transaction.asInventoryTransactionResponse())
    }

    // ============================================================================
    // Query Endpoints
    // ============================================================================

    /**
     * Get transaction by UID
     *
     * GET /inventory/v1/transactions/{uid}
     *
     * @param uid Transaction UID
     * @return Transaction details
     */
    @GetMapping("/{uid}")
    fun getTransaction(@PathVariable uid: String): ApiResponse<InventoryTransactionResponse> {
        val transaction = inventoryTransactionService.getTransactionByUid(uid)
        return ApiResponse.success(transaction.asInventoryTransactionResponse())
    }

    /**
     * Get transaction by transaction number
     *
     * GET /inventory/v1/transactions/number/{transactionNumber}
     *
     * @param transactionNumber Transaction number (e.g., TXN-20250119-0001)
     * @return Transaction details
     */
    @GetMapping("/number/{transactionNumber}")
    fun getTransactionByNumber(@PathVariable transactionNumber: String): ApiResponse<InventoryTransactionResponse> {
        val transaction = inventoryTransactionService.getTransactionByNumber(transactionNumber)
        return ApiResponse.success(transaction.asInventoryTransactionResponse())
    }

    /**
     * Get transactions for an inventory item
     *
     * GET /inventory/v1/transactions/item/{itemId}
     *
     * @param itemId Inventory item UID
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Page of transactions
     */
    @GetMapping("/item/{itemId}")
    fun getTransactionsByItem(
        @PathVariable itemId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<List<InventoryTransactionResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"))
        val transactions = inventoryTransactionService.getTransactionsByItem(itemId, pageable)
        return ApiResponse.success(transactions.content.asInventoryTransactionResponses())
    }

    /**
     * Get transactions for a warehouse
     *
     * GET /inventory/v1/transactions/warehouse/{warehouseId}
     *
     * @param warehouseId Warehouse UID
     * @param page Page number (default: 0)
     * @param size Page size (default: 20)
     * @return Page of transactions
     */
    @GetMapping("/warehouse/{warehouseId}")
    fun getTransactionsByWarehouse(
        @PathVariable warehouseId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ApiResponse<List<InventoryTransactionResponse>> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"))
        val transactions = inventoryTransactionService.getTransactionsByWarehouse(warehouseId, pageable)
        return ApiResponse.success(transactions.content.asInventoryTransactionResponses())
    }

    /**
     * Get transactions by reference document
     *
     * GET /inventory/v1/transactions/reference/{referenceType}/{referenceId}
     *
     * Used to get transactions linked to orders, invoices, purchases, etc.
     *
     * @param referenceType Type of reference (ORDER, INVOICE, PURCHASE, COUNT)
     * @param referenceId Reference UID
     * @return List of transactions
     */
    @GetMapping("/reference/{referenceType}/{referenceId}")
    fun getTransactionsByReference(
        @PathVariable referenceType: String,
        @PathVariable referenceId: String
    ): ApiResponse<List<InventoryTransactionResponse>> {
        val transactions = inventoryTransactionService.getTransactionsByReference(referenceType, referenceId)
        return ApiResponse.success(transactions.asInventoryTransactionResponses())
    }
}
