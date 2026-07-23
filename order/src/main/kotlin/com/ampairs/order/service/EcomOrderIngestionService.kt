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

        // Checkout already resolved (and blocked, if unlinked) the CRM distributor account this buyer
        // may order for — requestedCustomerId here is that resolved id, not a raw client choice.
        // Fall back to event.customerId only for events published before this field existed.
        val crmCustomerId = event.requestedCustomerId ?: event.customerId

        val order = Order().apply {
            orderType = "ECOM"
            ecomOrderRef = event.ecomOrderRef
            // Same number the buyer sees on the ecom side (assigned once at checkout) — never
            // generated independently here, so both sides always reference the same order number.
            orderNumber = event.orderNumber
            customerId = crmCustomerId
            customerName = event.customerName
            customerPhone = event.customerPhone
            status = OrderStatus.PENDING_MERCHANT_REVIEW
            // Buyer is the ecom customer (set above); seller is the implicit workspace.
            placeOfSupply = event.deliveryAddress.state ?: ""
            shippingAddress = event.deliveryAddress
            billingAddress = event.deliveryAddress
            subtotal = event.subtotal.toDouble()
            totalAmount = event.totalAmount.toDouble()
            // Populate the invoice-relevant totals so an invoice raised from this order carries the
            // real amount. Ecom orders carry no tax yet, so base == subtotal and cost == total; GST
            // on storefront orders is a follow-up.
            basePrice = event.subtotal.toDouble()
            totalCost = event.totalAmount.toDouble()
            totalTax = 0.0
            totalItems = event.lineItems.size
            totalQuantity = event.lineItems.sumOf { it.quantityOrdered.toDouble() }
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
                // Populate the displayed/invoiced line totals — the order module reads totalCost for
                // the line amount, so leaving it 0 made every ecom line show a zero total. Ecom orders
                // carry no tax yet, so base == total and tax == 0 (GST on storefront orders is a follow-up).
                totalCost = item.lineTotal.toDouble()
                basePrice = item.lineTotal.toDouble()
                totalTax = 0.0
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
