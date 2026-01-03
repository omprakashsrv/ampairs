package com.ampairs.inventory.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.inventory.domain.dto.*
import com.ampairs.inventory.service.InventorySerialService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*

/**
 * Serial Controller
 *
 * REST API endpoints for serial number tracking and lifecycle management.
 * Handles individual unit tracking from receipt to sale/disposal.
 *
 * Base path: /inventory/v1/serials
 */
@RestController
@RequestMapping("/inventory/v1/serials")
class SerialController @Autowired constructor(
    private val inventorySerialService: InventorySerialService
) {

    // ============================================================================
    // CRUD Endpoints
    // ============================================================================

    /**
     * Create a single serial number
     *
     * POST /inventory/v1/serials
     *
     * @param request Serial creation request
     * @return Created serial
     */
    @PostMapping("")
    fun createSerial(@Valid @RequestBody request: SerialRequest): ApiResponse<InventorySerialResponse> {
        val serial = inventorySerialService.createSerial(request)
        return ApiResponse.success(serial.asInventorySerialResponse())
    }

    /**
     * Create multiple serial numbers in bulk
     *
     * POST /inventory/v1/serials/bulk
     *
     * Used when receiving inventory with multiple serials
     *
     * @param request Bulk serial creation request
     * @return List of created serials
     */
    @PostMapping("/bulk")
    fun createBulkSerials(@Valid @RequestBody request: BulkSerialRequest): ApiResponse<List<InventorySerialResponse>> {
        val serials = inventorySerialService.createBulkSerials(request)
        return ApiResponse.success(serials.asInventorySerialResponses())
    }

    /**
     * Get serial by UID
     *
     * GET /inventory/v1/serials/{uid}
     *
     * @param uid Serial UID
     * @return Serial details
     */
    @GetMapping("/{uid}")
    fun getSerial(@PathVariable uid: String): ApiResponse<InventorySerialResponse> {
        val serial = inventorySerialService.getSerialByUid(uid)
        return ApiResponse.success(serial.asInventorySerialResponse())
    }

    /**
     * Track serial by serial number
     *
     * GET /inventory/v1/serials/track/{serialNumber}
     *
     * Public endpoint for tracking serial numbers (e.g., warranty lookups)
     *
     * @param serialNumber Serial number
     * @return Serial details
     */
    @GetMapping("/track/{serialNumber}")
    fun trackSerial(@PathVariable serialNumber: String): ApiResponse<InventorySerialResponse> {
        val serial = inventorySerialService.getSerialByNumber(serialNumber)
        return ApiResponse.success(serial.asInventorySerialResponse())
    }

    /**
     * Update serial information
     *
     * PUT /inventory/v1/serials/{uid}
     *
     * @param uid Serial UID
     * @param request Update request
     * @return Updated serial
     */
    @PutMapping("/{uid}")
    fun updateSerial(
        @PathVariable uid: String,
        @Valid @RequestBody request: SerialUpdateRequest
    ): ApiResponse<InventorySerialResponse> {
        val serial = inventorySerialService.updateSerial(uid, request)
        return ApiResponse.success(serial.asInventorySerialResponse())
    }

    /**
     * Delete serial (only if AVAILABLE status)
     *
     * DELETE /inventory/v1/serials/{uid}
     *
     * @param uid Serial UID
     * @return Success message
     */
    @DeleteMapping("/{uid}")
    fun deleteSerial(@PathVariable uid: String): ApiResponse<String> {
        inventorySerialService.deleteSerial(uid)
        return ApiResponse.success("Serial deleted successfully")
    }

    // ============================================================================
    // Query Endpoints
    // ============================================================================

    /**
     * Get all serials for an inventory item
     *
     * GET /inventory/v1/serials/item/{itemId}
     *
     * @param itemId Inventory item UID
     * @return List of serials
     */
    @GetMapping("/item/{itemId}")
    fun getSerialsByItem(@PathVariable itemId: String): ApiResponse<List<SerialSummaryResponse>> {
        val serials = inventorySerialService.getSerialsByItem(itemId)
        return ApiResponse.success(serials.asSerialSummaryResponses())
    }

    /**
     * Get serials for an item at a specific warehouse
     *
     * GET /inventory/v1/serials/item/{itemId}/warehouse/{warehouseId}
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of serials
     */
    @GetMapping("/item/{itemId}/warehouse/{warehouseId}")
    fun getSerialsByItemAndWarehouse(
        @PathVariable itemId: String,
        @PathVariable warehouseId: String
    ): ApiResponse<List<SerialSummaryResponse>> {
        val serials = inventorySerialService.getSerialsByItemAndWarehouse(itemId, warehouseId)
        return ApiResponse.success(serials.asSerialSummaryResponses())
    }

    /**
     * Get available serials for an item at a warehouse
     *
     * GET /inventory/v1/serials/item/{itemId}/warehouse/{warehouseId}/available
     *
     * Returns serials in FIFO order (oldest received first)
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return List of available serials
     */
    @GetMapping("/item/{itemId}/warehouse/{warehouseId}/available")
    fun getAvailableSerials(
        @PathVariable itemId: String,
        @PathVariable warehouseId: String
    ): ApiResponse<List<SerialSummaryResponse>> {
        val serials = inventorySerialService.getAvailableSerials(itemId, warehouseId)
        return ApiResponse.success(serials.asSerialSummaryResponses())
    }

    /**
     * Get serials sold to a customer
     *
     * GET /inventory/v1/serials/customer/{customerId}
     *
     * @param customerId Customer UID
     * @return List of customer's serials
     */
    @GetMapping("/customer/{customerId}")
    fun getSerialsByCustomer(@PathVariable customerId: String): ApiResponse<List<InventorySerialResponse>> {
        val serials = inventorySerialService.getSerialsByCustomer(customerId)
        return ApiResponse.success(serials.asInventorySerialResponses())
    }

    // ============================================================================
    // Lifecycle Management Endpoints
    // ============================================================================

    /**
     * Reserve serials for an order
     *
     * POST /inventory/v1/serials/reserve
     *
     * @param serialNumbers List of serial numbers to reserve
     * @return Success message
     */
    @PostMapping("/reserve")
    fun reserveSerials(@RequestBody serialNumbers: List<String>): ApiResponse<String> {
        inventorySerialService.reserveSerials(serialNumbers)
        return ApiResponse.success("${serialNumbers.size} serials reserved successfully")
    }

    /**
     * Release reserved serials
     *
     * POST /inventory/v1/serials/release
     *
     * @param serialNumbers List of serial numbers to release
     * @return Success message
     */
    @PostMapping("/release")
    fun releaseReservations(@RequestBody serialNumbers: List<String>): ApiResponse<String> {
        inventorySerialService.releaseReservations(serialNumbers)
        return ApiResponse.success("${serialNumbers.size} serial reservations released successfully")
    }

    /**
     * Mark serial as sold
     *
     * POST /inventory/v1/serials/sell
     *
     * @param request Sale request
     * @return Updated serial
     */
    @PostMapping("/sell")
    fun markSerialAsSold(@Valid @RequestBody request: SerialSaleRequest): ApiResponse<InventorySerialResponse> {
        val serial = inventorySerialService.markSerialAsSold(request)
        return ApiResponse.success(serial.asInventorySerialResponse())
    }

    /**
     * Mark serial as returned
     *
     * POST /inventory/v1/serials/return
     *
     * @param request Return request
     * @return Updated serial
     */
    @PostMapping("/return")
    fun markSerialAsReturned(@Valid @RequestBody request: SerialReturnRequest): ApiResponse<InventorySerialResponse> {
        val serial = inventorySerialService.markSerialAsReturned(request)
        return ApiResponse.success(serial.asInventorySerialResponse())
    }

    /**
     * Mark serial as damaged
     *
     * POST /inventory/v1/serials/damage
     *
     * @param serialNumber Serial number
     * @param notes Damage notes
     * @return Updated serial
     */
    @PostMapping("/damage")
    fun markSerialAsDamaged(
        @RequestParam serialNumber: String,
        @RequestParam(required = false) notes: String?
    ): ApiResponse<InventorySerialResponse> {
        val serial = inventorySerialService.markSerialAsDamaged(serialNumber, notes)
        return ApiResponse.success(serial.asInventorySerialResponse())
    }

    /**
     * Make serial available again
     *
     * POST /inventory/v1/serials/make-available
     *
     * Used for returned or repaired serials
     *
     * @param serialNumber Serial number
     * @return Updated serial
     */
    @PostMapping("/make-available")
    fun makeSerialAvailable(@RequestParam serialNumber: String): ApiResponse<InventorySerialResponse> {
        val serial = inventorySerialService.makeSerialAvailable(serialNumber)
        return ApiResponse.success(serial.asInventorySerialResponse())
    }

    // ============================================================================
    // Warranty Management Endpoints
    // ============================================================================

    /**
     * Get serials with expiring warranty
     *
     * GET /inventory/v1/serials/warranty/expiring
     *
     * @param days Days until expiry (default: 30)
     * @return List of serials with expiring warranty
     */
    @GetMapping("/warranty/expiring")
    fun getSerialsWithExpiringWarranty(
        @RequestParam(defaultValue = "30") days: Int
    ): ApiResponse<List<InventorySerialResponse>> {
        val serials = inventorySerialService.getSerialsWithExpiringWarranty(days)
        return ApiResponse.success(serials.asInventorySerialResponses())
    }

    /**
     * Get customer's serials with active warranty
     *
     * GET /inventory/v1/serials/customer/{customerId}/warranty
     *
     * @param customerId Customer UID
     * @return List of serials with active warranty
     */
    @GetMapping("/customer/{customerId}/warranty")
    fun getCustomerSerialsWithActiveWarranty(@PathVariable customerId: String): ApiResponse<List<InventorySerialResponse>> {
        val serials = inventorySerialService.getCustomerSerialsWithActiveWarranty(customerId)
        return ApiResponse.success(serials.asInventorySerialResponses())
    }

    // ============================================================================
    // Statistics Endpoints
    // ============================================================================

    /**
     * Get serial status summary for an item
     *
     * GET /inventory/v1/serials/item/{itemId}/warehouse/{warehouseId}/summary
     *
     * Returns counts by status (AVAILABLE, RESERVED, SOLD, DAMAGED, RETURNED)
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return Status summary
     */
    @GetMapping("/item/{itemId}/warehouse/{warehouseId}/summary")
    fun getSerialStatusSummary(
        @PathVariable itemId: String,
        @PathVariable warehouseId: String
    ): ApiResponse<SerialStatusSummary> {
        val summary = inventorySerialService.getSerialStatusSummary(itemId, warehouseId)
        return ApiResponse.success(summary)
    }

    /**
     * Count available serials
     *
     * GET /inventory/v1/serials/item/{itemId}/warehouse/{warehouseId}/count
     *
     * @param itemId Inventory item UID
     * @param warehouseId Warehouse UID
     * @return Count of available serials
     */
    @GetMapping("/item/{itemId}/warehouse/{warehouseId}/count")
    fun countAvailableSerials(
        @PathVariable itemId: String,
        @PathVariable warehouseId: String
    ): ApiResponse<Long> {
        val count = inventorySerialService.countAvailableSerials(itemId, warehouseId)
        return ApiResponse.success(count)
    }
}
