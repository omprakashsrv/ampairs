package com.ampairs.order.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.core.security.AuthenticationHelper
import com.ampairs.event.domain.events.OrderCreatedEvent
import com.ampairs.event.domain.events.OrderStatusChangedEvent
import com.ampairs.event.domain.events.OrderUpdatedEvent
import com.ampairs.event.domain.kafka.EcomOrderStatusEvent
import com.ampairs.inventory.service.InventoryStockService
import com.ampairs.inventory.service.StockLine
import com.ampairs.inventory.service.StockMutationCommand
import com.ampairs.inventory.service.StockSourceType
import com.ampairs.invoice.service.InvoiceService
import com.ampairs.order.kafka.EcomOrderStatusProducer
import com.ampairs.order.domain.enums.OrderStatus
import com.ampairs.order.domain.dto.OrderResponse
import com.ampairs.order.domain.dto.OrderUpdateRequest
import com.ampairs.order.domain.dto.toInvoice
import com.ampairs.order.domain.dto.toInvoiceItems
import com.ampairs.order.domain.dto.toOrder
import com.ampairs.order.domain.dto.toOrderItems
import com.ampairs.order.domain.dto.toResponse
import com.ampairs.order.domain.model.Order
import com.ampairs.order.domain.model.OrderItem
import java.math.BigDecimal
import com.ampairs.order.repository.OrderItemRepository
import com.ampairs.order.repository.OrderPagingRepository
import com.ampairs.order.repository.OrderRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Instant


import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull

@Service
@Transactional(readOnly = true)
class OrderService(
    val orderRepository: OrderRepository,
    val orderItemRepository: OrderItemRepository,
    val orderPagingRepository: OrderPagingRepository,
    val invoiceService: InvoiceService,
    val inventoryStockService: InventoryStockService,
    val eventPublisher: ApplicationEventPublisher,
    val ecomOrderStatusProducer: EcomOrderStatusProducer,
) {

    /**
     * Spec 014 (US1): move stock for the sale based on the order's status. Gated inside the
     * inventory engine by (a) inventory-management installed and (b) the auto_deduct_on_order config;
     * idempotent per order line. Untracked products are skipped. CONFIRMED..DELIVERED apply the sale;
     * CANCELLED/REFUNDED restore it.
     */
    private fun syncStockForOrder(order: Order, orderItems: List<OrderItem>) {
        // Never move stock for a removed (soft-deleted) line.
        val activeItems = orderItems.filter { it.active }
        if (activeItems.isEmpty()) return
        val command = StockMutationCommand(
            sourceType = StockSourceType.ORDER,
            sourceId = order.uid,
            referenceNumber = order.orderNumber,
            performedBy = getUserId(),
            lines = activeItems.map {
                StockLine(
                    sourceLineUid = it.uid,
                    productId = it.productId,
                    quantity = BigDecimal.valueOf(it.quantity),
                )
            },
        )
        when (order.status) {
            OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.SHIPPED,
            OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED, OrderStatus.INVOICED ->
                inventoryStockService.applySale(command)
            OrderStatus.CANCELLED, OrderStatus.REFUNDED ->
                inventoryStockService.reverseSale(command)
            else -> Unit
        }
    }

    /**
     * Bridges a management-side status change back to the buyer-facing [EcomOrderStatus] (ecom
     * module — not depended on here, so the mapped value travels as a plain string, same as
     * [com.ampairs.order.service.OrderEcomServiceImpl.confirmEcomOrder]). Only INVOICED/SHIPPED/
     * OUT_FOR_DELIVERY/DELIVERED/CANCELLED are meaningful to the buyer; everything else (DRAFT,
     * ORDERED, CONFIRMED, PROCESSING, REFUNDED, ...) is a no-op here — CONFIRMED is instead handled
     * by [OrderEcomServiceImpl.confirmEcomOrder] itself, which is the only path that sets it.
     */
    private fun ecomStatusFor(status: OrderStatus): String? = when (status) {
        OrderStatus.INVOICED -> "PROCESSING"
        OrderStatus.SHIPPED, OrderStatus.OUT_FOR_DELIVERY -> "DISPATCHED"
        OrderStatus.DELIVERED -> "DELIVERED"
        OrderStatus.CANCELLED -> "CANCELLED"
        else -> null
    }

    /**
     * Without this, an order ingested from the storefront (has [Order.ecomOrderRef]) silently never
     * tells the buyer app about anything that happens after ingestion/confirmation — the buyer stays
     * stuck on "Reviewing your order" forever regardless of invoicing/shipping/delivery on the seller
     * side. No-ops for non-ecom orders and for statuses with no buyer-facing meaning.
     */
    private fun notifyEcomIfLinked(order: Order, oldStatus: OrderStatus?) {
        val ecomOrderRef = order.ecomOrderRef
        if (ecomOrderRef.isNullOrBlank() || oldStatus == order.status) return
        val mapped = ecomStatusFor(order.status) ?: return
        ecomOrderStatusProducer.publishStatusUpdate(
            EcomOrderStatusEvent(
                ecomOrderRef = ecomOrderRef,
                workspaceId = order.ownerId,
                newStatus = mapped,
                managementOrderRef = order.uid,
                updatedAt = Instant.now(),
            )
        )
    }

    /**
     * Helper methods for event publishing
     */
    private fun getWorkspaceId(): String = TenantContextHolder.getCurrentTenant() ?: ""

    private fun getUserId(): String {
        val auth = SecurityContextHolder.getContext().authentication
        return auth?.let { AuthenticationHelper.getCurrentUserId(it) } ?: ""
    }

    private fun getDeviceId(): String = DeviceContextHolder.getCurrentDevice() ?: ""

    @Transactional
    fun updateOrder(order: Order, orderItems: List<OrderItem>): OrderResponse {
        val existingOrder = orderRepository.findByUid(order.uid).getOrNull()
        val isNewOrder = existingOrder == null
        val oldStatus = existingOrder?.status

        order.uid = existingOrder?.uid ?: order.uid
        order.orderNumber = existingOrder?.orderNumber ?: ""
        if (order.orderNumber.isEmpty()) {
            val orderNumber = orderRepository.findMaxOrderNumber().getOrDefault("0").toIntOrNull() ?: 0
            order.orderNumber = (orderNumber + 1).toString()
        }
        // ecomOrderRef is a system-assigned, immutable-once-set linkage — no request DTO exposes it
        // for the client to send back, so it must be carried over explicitly or every update wipes
        // it (breaking both notifyEcomIfLinked and confirmEcomOrder's own findByEcomOrderRef lookup).
        order.ecomOrderRef = existingOrder?.ecomOrderRef ?: order.ecomOrderRef
        val savedOrder = orderRepository.save(order)

        orderItems.forEach { orderItem ->
            if (orderItem.uid.isNotEmpty()) {
                val existingOrderItem = orderItemRepository.findByUid(orderItem.uid).getOrNull()
                orderItem.uid = existingOrderItem?.uid ?: orderItem.uid
            }
            orderItemRepository.save(orderItem)
        }

        // Publish events
        if (isNewOrder) {
            eventPublisher.publishEvent(
                OrderCreatedEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = savedOrder.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    orderNumber = savedOrder.orderNumber,
                    customerName = savedOrder.customerName ?: "",
                    totalAmount = savedOrder.totalAmount
                )
            )
        } else {
            eventPublisher.publishEvent(
                OrderUpdatedEvent(
                    source = this,
                    workspaceId = getWorkspaceId(),
                    entityId = savedOrder.uid,
                    userId = getUserId(),
                    deviceId = getDeviceId(),
                    fieldChanges = mapOf("order" to "updated", "items" to orderItems.size)
                )
            )

            // Publish status changed event if status changed
            if (oldStatus != null && oldStatus != savedOrder.status) {
                eventPublisher.publishEvent(
                    OrderStatusChangedEvent(
                        source = this,
                        workspaceId = getWorkspaceId(),
                        entityId = savedOrder.uid,
                        userId = getUserId(),
                        deviceId = getDeviceId(),
                        orderNumber = savedOrder.orderNumber,
                        oldStatus = oldStatus.name,
                        newStatus = savedOrder.status.name
                    )
                )
            }
        }
        notifyEcomIfLinked(savedOrder, oldStatus)

        // Spec 014: apply/reverse inventory for the sale (idempotent; no-op if module/config off).
        syncStockForOrder(savedOrder, orderItems)

        return savedOrder.toResponse(orderItems)
    }

    @Transactional
    fun createInvoice(order: Order, orderItems: List<OrderItem>): OrderResponse {
        // Must look up by uid (not id — the numeric PK is never populated from the request), or
        // this always misses and both the invoiceRefId guard below and the status update never
        // fire, letting the same order be invoiced repeatedly.
        val existingOrder = orderRepository.findByUid(order.uid).getOrNull()
        val oldStatus = existingOrder?.status
        order.uid = existingOrder?.uid ?: order.uid
        order.orderNumber = existingOrder?.orderNumber ?: ""
        if (order.orderNumber.isEmpty()) {
            val orderNumber = orderRepository.findMaxOrderNumber().getOrDefault("0").toIntOrNull() ?: 0
            order.orderNumber = (orderNumber + 1).toString()
        }
        // See updateOrder's identical comment — ecomOrderRef must be carried over, not clobbered.
        order.ecomOrderRef = existingOrder?.ecomOrderRef ?: order.ecomOrderRef
        // Invoicing is terminal for the order's sale lifecycle regardless of whether this is the
        // first call (creates the invoice) or a repeat call (idempotent re-save below).
        order.status = OrderStatus.INVOICED
        val savedOrder = orderRepository.save(order)
        val savedOrderItems = orderItems.map { orderItem ->
            if (orderItem.uid.isNotEmpty()) {
                val existingOrderItem = orderItemRepository.findByUid(orderItem.uid).getOrNull()
                orderItem.uid = existingOrderItem?.uid ?: orderItem.uid
            }
            orderItemRepository.save(orderItem)
        }.toList()
        if (!existingOrder?.invoiceRefId.isNullOrEmpty()) {
            // Already invoiced — reuse the existing invoice ref instead of creating a duplicate.
            savedOrder.invoiceRefId = existingOrder.invoiceRefId
            orderRepository.save(savedOrder)
        } else {
            val updatedInvoice = invoiceService.updateInvoice(savedOrder.toInvoice(), savedOrderItems.toInvoiceItems())
            savedOrder.invoiceRefId = updatedInvoice.id
            orderRepository.save(savedOrder)
        }
        notifyEcomIfLinked(savedOrder, oldStatus)

        return savedOrder.toResponse(orderItems)
    }

    /**
     * Offline-sync bulk upsert (spec 010). Client UID is authoritative — preserved on insert and
     * matched on update. No tax/total recomputation: values are stored exactly as supplied.
     * Assigns a server orderNumber when blank. Does not publish per-row events (sync is bulk).
     */
    @Transactional
    fun bulkUpsertOrders(requests: List<OrderUpdateRequest>): List<OrderResponse> =
        requests.map { upsertOrder(it.toOrder(), it.orderItems.toOrderItems()) }

    private fun upsertOrder(order: Order, orderItems: List<OrderItem>): OrderResponse {
        val existing = if (order.uid.isNotEmpty()) orderRepository.findByUid(order.uid).getOrNull() else null
        val oldStatus = existing?.status
        if (existing != null) {
            order.id = existing.id
            order.uid = existing.uid
            order.createdAt = existing.createdAt
            if (order.orderNumber.isEmpty()) order.orderNumber = existing.orderNumber
            // See updateOrder's identical comment — ecomOrderRef must be carried over, not clobbered.
            // OrderUpdateRequest has no field for it at all, so every offline-sync push would
            // otherwise silently null it out on the very first update after ingestion.
            order.ecomOrderRef = existing.ecomOrderRef
        }
        if (order.orderNumber.isEmpty()) {
            val maxOrderNumber = orderRepository.findMaxOrderNumber().getOrDefault("0").toIntOrNull() ?: 0
            order.orderNumber = (maxOrderNumber + 1).toString()
        }
        val savedOrder = orderRepository.save(order)
        val savedItems = orderItems.map { item ->
            val existingItem = if (item.uid.isNotEmpty()) orderItemRepository.findByUid(item.uid).getOrNull() else null
            if (existingItem != null) {
                item.id = existingItem.id
                item.createdAt = existingItem.createdAt
            }
            item.orderId = savedOrder.uid
            orderItemRepository.save(item)
        }
        // No per-row *internal* activity events on this bulk path (see doc comment above), but the
        // ecom bridge is a functional link (not a log entry) — a client marking an order
        // SHIPPED/OUT_FOR_DELIVERY/DELIVERED offline and syncing it through this exact endpoint must
        // still notify the buyer, or the storefront never advances past ingestion/confirmation.
        notifyEcomIfLinked(savedOrder, oldStatus)
        // Spec 014: apply/reverse inventory for the sale on the offline-sync path too. Idempotent per
        // line, so re-pushing an order with unchanged items/quantities is a no-op (no double-count).
        syncStockForOrder(savedOrder, savedItems)
        return savedOrder.toResponse(savedItems)
    }

    /**
     * Incremental sync feed: orders updated at/after [lastSync] (ISO-8601), INCLUDING cancelled rows,
     * ordered by updatedAt ASC, paginated. Falls back to the full feed when the cursor is absent/invalid.
     */
    fun getOrdersAfterSync(lastSync: String?, pageable: Pageable): Page<Order> {
        if (lastSync.isNullOrBlank()) return orderPagingRepository.findAllBy(pageable)
        return try {
            val decoded = URLDecoder.decode(lastSync, StandardCharsets.UTF_8)
            orderPagingRepository.findByUpdatedAtGreaterThanEqual(Instant.parse(decoded), pageable)
        } catch (e: Exception) {
            orderPagingRepository.findAllBy(pageable)
        }
    }

    fun getOrders(lastUpdated: Instant?): List<Order> {
        val orders =
            orderPagingRepository.findAllByUpdatedAtGreaterThanEqual(
                lastUpdated ?: Instant.EPOCH, PageRequest.of(0, 50, Sort.by("updatedAt").ascending())
            )
        return orders
    }


}
