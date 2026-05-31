package com.ampairs.ecom.service

import com.ampairs.core.service.ConfirmedLineItem
import com.ampairs.core.service.OrderEcomService
import com.ampairs.ecom.domain.dto.EcomOrderLineItemEditRequest
import com.ampairs.ecom.domain.enums.EcomLineItemStatus
import com.ampairs.ecom.domain.enums.EcomOrderStatus
import com.ampairs.ecom.domain.model.EcomOrder
import com.ampairs.ecom.exception.EcomOrderNotFoundException
import com.ampairs.ecom.exception.InvalidOrderStatusTransitionException
import com.ampairs.ecom.repository.EcomOrderLineItemRepository
import com.ampairs.ecom.repository.EcomOrderRepository
import com.ampairs.event.domain.kafka.EcomOrderStatusEvent
import com.ampairs.notification.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.security.access.AccessDeniedException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class EcomOrderService(
    private val orderRepository: EcomOrderRepository,
    private val lineItemRepository: EcomOrderLineItemRepository,
    private val orderEcomService: OrderEcomService,
    private val notificationService: NotificationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private val validTransitions = mapOf(
            EcomOrderStatus.CONFIRMED to setOf(EcomOrderStatus.PROCESSING, EcomOrderStatus.CANCELLED),
            EcomOrderStatus.PROCESSING to setOf(EcomOrderStatus.DISPATCHED, EcomOrderStatus.CANCELLED),
            EcomOrderStatus.DISPATCHED to setOf(EcomOrderStatus.DELIVERED),
        )
    }

    fun getCustomerOrders(customerId: String, storefrontId: String, pageable: Pageable): Page<EcomOrder> =
        orderRepository.findByCustomerIdAndStorefrontId(customerId, storefrontId, pageable)

    fun getCustomerOrder(customerId: String, ecomOrderRef: String): EcomOrder {
        val order = orderRepository.findByEcomOrderRef(ecomOrderRef)
            ?: throw EcomOrderNotFoundException("Order not found: $ecomOrderRef")
        if (order.customerId != customerId) throw AccessDeniedException("Access denied")
        return order
    }

    fun getManagementOrders(workspaceId: String, status: EcomOrderStatus?, pageable: Pageable): Page<EcomOrder> =
        if (status != null) orderRepository.findByWorkspaceIdAndStatus(workspaceId, status, pageable)
        else orderRepository.findByWorkspaceId(workspaceId, pageable)

    fun getManagementOrder(workspaceId: String, ecomOrderRef: String): EcomOrder {
        val order = orderRepository.findByEcomOrderRef(ecomOrderRef)
            ?: throw EcomOrderNotFoundException("Order not found: $ecomOrderRef")
        if (order.workspaceId != workspaceId) throw AccessDeniedException("Access denied")
        return order
    }

    @Transactional
    fun editLineItems(workspaceId: String, ecomOrderRef: String, items: List<EcomOrderLineItemEditRequest>): EcomOrder {
        val order = getManagementOrder(workspaceId, ecomOrderRef)
        if (order.status != EcomOrderStatus.PENDING_MERCHANT_REVIEW) {
            throw InvalidOrderStatusTransitionException("Line items can only be edited when order is PENDING_MERCHANT_REVIEW")
        }
        val lineItems = lineItemRepository.findByEcomOrderId(order.uid).associateBy { it.uid }
        items.forEach { edit ->
            lineItems[edit.uid]?.let {
                it.quantityConfirmed = edit.quantityConfirmed
                it.status = edit.status
                lineItemRepository.save(it)
            }
        }
        return orderRepository.findByEcomOrderRef(ecomOrderRef)!!
    }

    @Transactional
    fun confirmOrder(workspaceId: String, ecomOrderRef: String): EcomOrder {
        val order = getManagementOrder(workspaceId, ecomOrderRef)
        if (order.status != EcomOrderStatus.PENDING_MERCHANT_REVIEW) {
            throw InvalidOrderStatusTransitionException("Only PENDING_MERCHANT_REVIEW orders can be confirmed")
        }
        order.status = EcomOrderStatus.CONFIRMED
        order.merchantReviewedAt = Instant.now()
        val saved = orderRepository.save(order)

        val confirmedItems = lineItemRepository.findByEcomOrderId(order.uid).map { item ->
            ConfirmedLineItem(
                managementProductId = item.managementProductId,
                quantityConfirmed = item.quantityConfirmed ?: item.quantityOrdered,
                status = item.status.name,
            )
        }
        orderEcomService.confirmEcomOrder(ecomOrderRef, confirmedItems)
        return saved
    }

    @Transactional
    fun advanceStatus(workspaceId: String, ecomOrderRef: String, newStatus: EcomOrderStatus): EcomOrder {
        val order = getManagementOrder(workspaceId, ecomOrderRef)
        val allowed = validTransitions[order.status] ?: emptySet()
        if (newStatus !in allowed) {
            throw InvalidOrderStatusTransitionException("Cannot transition from ${order.status} to $newStatus")
        }
        order.status = newStatus
        return orderRepository.save(order)
    }

    @Transactional
    fun applyStatusUpdate(event: EcomOrderStatusEvent) {
        val order = orderRepository.findByEcomOrderRef(event.ecomOrderRef) ?: return
        val newStatus = try {
            EcomOrderStatus.valueOf(event.newStatus)
        } catch (_: IllegalArgumentException) {
            return
        }
        order.status = newStatus
        event.managementOrderRef?.let { order.managementOrderRef = it }
        if (newStatus == EcomOrderStatus.CONFIRMED) {
            order.confirmedAt = Instant.now()
        }
        event.confirmedLineItems?.let { confirmed ->
            val lineItems = lineItemRepository.findByEcomOrderId(order.uid).associateBy { it.managementProductId }
            confirmed.forEach { payload ->
                lineItems[payload.managementProductId]?.let {
                    it.quantityConfirmed = payload.quantityConfirmed
                    try { it.status = EcomLineItemStatus.valueOf(payload.status) } catch (_: IllegalArgumentException) {}
                    lineItemRepository.save(it)
                }
            }
        }
        orderRepository.save(order)

        if (newStatus in setOf(EcomOrderStatus.CONFIRMED, EcomOrderStatus.DISPATCHED, EcomOrderStatus.DELIVERED, EcomOrderStatus.CANCELLED)) {
            sendStatusNotification(order, newStatus)
        }
    }

    private fun sendStatusNotification(order: EcomOrder, status: EcomOrderStatus) {
        val message = when (status) {
            EcomOrderStatus.CONFIRMED -> "Your order ${order.ecomOrderRef} has been confirmed."
            EcomOrderStatus.DISPATCHED -> "Your order ${order.ecomOrderRef} has been dispatched."
            EcomOrderStatus.DELIVERED -> "Your order ${order.ecomOrderRef} has been delivered."
            EcomOrderStatus.CANCELLED -> "Your order ${order.ecomOrderRef} has been cancelled."
            else -> return
        }
        try {
            notificationService.queueNotificationWithTenant(
                recipient = order.customerEmail,
                message = message,
                tenantId = order.workspaceId,
            )
        } catch (e: Exception) {
            log.error("Failed to queue order status notification for {}: {}", order.ecomOrderRef, e.message)
        }
    }
}
