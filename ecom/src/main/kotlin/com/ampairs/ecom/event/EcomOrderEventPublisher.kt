package com.ampairs.ecom.event

import com.ampairs.core.domain.model.Address
import com.ampairs.ecom.domain.model.EcomOrder
import com.ampairs.event.domain.kafka.EcomOrderLineItemPayload
import com.ampairs.event.domain.kafka.EcomOrderPlacedEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Publishes the "order placed" domain event in-process via Spring [ApplicationEventPublisher].
 *
 * The order module reacts (creates a management order) on a separate thread, after the checkout
 * transaction commits — no message broker required. This keeps order placement working without
 * Kafka. A Kafka bridge can later `@EventListener` this same [EcomOrderPlacedEvent] and forward it
 * to a topic, with no change to checkout.
 */
@Component
class EcomOrderEventPublisher(
    private val publisher: ApplicationEventPublisher,
) {
    fun publishOrderPlaced(order: EcomOrder, requestedCustomerId: String? = null) {
        publisher.publishEvent(order.toPlacedEvent(requestedCustomerId))
    }
}

private fun EcomOrder.toPlacedEvent(requestedCustomerId: String?): EcomOrderPlacedEvent = EcomOrderPlacedEvent(
    ecomOrderRef = ecomOrderRef,
    orderNumber = orderNumber,
    workspaceId = workspaceId,
    storefrontId = storefrontId,
    customerId = customerId,
    customerName = customerName,
    customerEmail = customerEmail,
    customerPhone = customerPhone,
    requestedCustomerId = requestedCustomerId,
    deliveryAddress = deliveryAddress.toAddress(),
    lineItems = lineItems.map {
        EcomOrderLineItemPayload(
            listedProductId = it.listedProductId,
            managementProductId = it.managementProductId,
            productName = it.productName,
            unitPrice = it.unitPrice,
            quantityOrdered = it.quantityOrdered,
            lineTotal = it.lineTotal,
        )
    },
    subtotal = subtotal,
    totalAmount = totalAmount,
    placedAt = placedAt,
)

private fun Map<String, Any>.toAddress(): Address = Address(
    street = this["addressLine1"] as? String ?: "",
    street2 = this["addressLine2"] as? String ?: "",
    city = this["city"] as? String ?: "",
    state = this["state"] as? String ?: "",
    country = this["country"] as? String ?: "IN",
    pincode = this["pinCode"] as? String ?: "",
    phone = this["phone"] as? String ?: "",
)
