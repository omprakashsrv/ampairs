package com.ampairs.order.service

import com.ampairs.event.domain.kafka.EcomOrderPlacedEvent
import com.ampairs.event.domain.kafka.EcomOrderStatusEvent
import com.ampairs.order.domain.enums.OrderStatus
import com.ampairs.order.domain.model.Order
import com.ampairs.order.domain.model.OrderItem
import com.ampairs.order.kafka.EcomOrderStatusProducer
import com.ampairs.order.repository.OrderItemRepository
import com.ampairs.order.repository.OrderRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

/**
 * Creates a management [Order] (+ items) from an ecom "order placed" event, then publishes the
 * resulting status back to the ecom module. Transport-agnostic: invoked by the in-process Spring
 * listener today, and re-usable by a Kafka consumer if a broker is enabled later. Idempotent on
 * [EcomOrderPlacedEvent.ecomOrderRef].
 */
@Service
class EcomOrderIngestionService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val ecomOrderStatusProducer: EcomOrderStatusProducer,
) {
    private val log = LoggerFactory.getLogger(EcomOrderIngestionService::class.java)

    @Transactional
    fun ingest(event: EcomOrderPlacedEvent) {
        if (orderRepository.findByEcomOrderRef(event.ecomOrderRef) != null) {
            log.info("Skipping duplicate ecom order: {}", event.ecomOrderRef)
            return
        }

        val order = Order().apply {
            orderType = "ECOM"
            ecomOrderRef = event.ecomOrderRef
            customerId = event.customerId
            customerName = event.customerName
            customerPhone = event.customerPhone
            status = OrderStatus.PENDING_MERCHANT_REVIEW
            // Buyer is the ecom customer (set above); seller is the implicit workspace.
            placeOfSupply = event.deliveryAddress.state ?: ""
            shippingAddress = event.deliveryAddress
            billingAddress = event.deliveryAddress
            subtotal = event.subtotal.toDouble()
            totalAmount = event.totalAmount.toDouble()
            orderDate = event.placedAt
            notes = null
        }
        val savedOrder = orderRepository.save(order)

        event.lineItems.forEachIndexed { index, item ->
            val orderItem = OrderItem().apply {
                orderId = savedOrder.uid
                productId = item.managementProductId
                description = item.productName
                quantity = item.quantityOrdered.toDouble()
                unitPrice = item.unitPrice.toDouble()
                sellingPrice = item.unitPrice.toDouble()
                productPrice = item.unitPrice.toDouble()
                lineTotal = item.lineTotal.toDouble()
                this.index = index
            }
            orderItemRepository.save(orderItem)
        }

        ecomOrderStatusProducer.publishStatusUpdate(
            EcomOrderStatusEvent(
                ecomOrderRef = event.ecomOrderRef,
                workspaceId = event.workspaceId,
                newStatus = OrderStatus.PENDING_MERCHANT_REVIEW.name,
                managementOrderRef = savedOrder.uid,
                updatedAt = Instant.now(),
            )
        )
        log.info("Created management order {} for ecom order {}", savedOrder.uid, event.ecomOrderRef)
    }
}
