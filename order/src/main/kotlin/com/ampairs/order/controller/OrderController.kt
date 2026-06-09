package com.ampairs.order.controller

import com.ampairs.core.domain.dto.ApiResponse
import com.ampairs.core.domain.dto.PageResponse
import com.ampairs.order.domain.dto.*
import com.ampairs.order.service.OrderService
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/order/v1/orders")
class OrderController(
    private val orderService: OrderService,
) {

    @PostMapping("/create-invoice")
    fun createInvoice(@RequestBody @Valid orderUpdateRequest: OrderUpdateRequest): ApiResponse<OrderResponse> {
        val order = orderUpdateRequest.toOrder()
        val orderItems = orderUpdateRequest.orderItems.toOrderItems()
        val result = orderService.createInvoice(order, orderItems)
        return ApiResponse.success(result)
    }

    /**
     * Offline-sync bulk upsert (spec 010). Client UID-keyed; preserves taxInfos/totals/priceMode/
     * overallDiscountMode as supplied (no server recompute). Assigns orderNumber when blank.
     */
    @PostMapping("/sync")
    fun syncOrders(@RequestBody @Valid requests: List<OrderUpdateRequest>): ApiResponse<List<OrderResponse>> {
        return ApiResponse.success(orderService.bulkUpsertOrders(requests))
    }

    /**
     * Incremental sync feed: orders updated at/after last_sync (ISO-8601), INCLUDING cancelled rows,
     * ordered by updatedAt ASC, paginated.
     */
    @GetMapping("/sync")
    fun getOrdersSync(
        @RequestParam("last_sync", required = false) lastSync: String?,
        @RequestParam("page", defaultValue = "0") page: Int,
        @RequestParam("size", defaultValue = "100") size: Int,
        @RequestParam("sort_by", defaultValue = "updatedAt") sortBy: String,
        @RequestParam("sort_dir", defaultValue = "ASC") sortDir: String
    ): ApiResponse<PageResponse<OrderResponse>> {
        val direction = Sort.Direction.fromString(sortDir)
        val pageable = PageRequest.of(page, size, Sort.by(direction, sortBy))
        val ordersPage = orderService.getOrdersAfterSync(lastSync, pageable)
        return ApiResponse.success(PageResponse.from(ordersPage) { it.toResponse(it.orderItems) })
    }

}
