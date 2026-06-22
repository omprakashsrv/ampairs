package com.ampairs.ecom.kafka

import com.ampairs.ecom.domain.model.EcomOrder
import com.ampairs.event.domain.kafka.EcomOrderLineItemPayload
import com.ampairs.event.domain.kafka.EcomOrderPlacedEvent
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.ampairs.event.kafka.KafkaAvailableCondition
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Conditional
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

/**
 * Kafka transport for "order placed" — disabled unless a broker is reachable
 * ([KafkaAvailableCondition]). The default path is the in-process [com.ampairs.ecom.event.EcomOrderEventPublisher].
 */
@Component
@Conditional(KafkaAvailableCondition::class)
class EcomOrderKafkaProducer(
    @Qualifier("ecomOrderKafkaTemplate") private val ecomOrderKafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(EcomOrderKafkaProducer::class.java)
    private val objectMapper = ObjectMapper()
        .registerKotlinModule()
        .registerModule(JavaTimeModule())

    fun publishOrderPlaced(order: EcomOrder) {
        val event = EcomOrderPlacedEvent(
            ecomOrderRef = order.ecomOrderRef,
            workspaceId = order.workspaceId,
            storefrontId = order.storefrontId,
            customerId = order.customerId,
            customerName = order.customerName,
            customerEmail = order.customerEmail,
            customerPhone = order.customerPhone,
            deliveryAddress = buildAddress(order.deliveryAddress),
            lineItems = order.lineItems.map { item ->
                EcomOrderLineItemPayload(
                    listedProductId = item.listedProductId,
                    managementProductId = item.managementProductId,
                    productName = item.productName,
                    unitPrice = item.unitPrice,
                    quantityOrdered = item.quantityOrdered,
                    lineTotal = item.lineTotal,
                )
            },
            subtotal = order.subtotal,
            totalAmount = order.totalAmount,
            placedAt = order.placedAt,
        )
        try {
            val payload = objectMapper.writeValueAsString(event)
            ecomOrderKafkaTemplate.send("ecom-order-placed", order.workspaceId, payload)
            log.debug("Published order placed event for {}", order.ecomOrderRef)
        } catch (ex: Exception) {
            log.error("Failed to publish order placed event for {}: {}", order.ecomOrderRef, ex.message, ex)
            throw ex
        }
    }

    private fun buildAddress(deliveryAddress: Map<String, Any>): com.ampairs.core.domain.model.Address {
        return com.ampairs.core.domain.model.Address(
            street = deliveryAddress["addressLine1"] as? String ?: "",
            street2 = deliveryAddress["addressLine2"] as? String ?: "",
            city = deliveryAddress["city"] as? String ?: "",
            state = deliveryAddress["state"] as? String ?: "",
            country = deliveryAddress["country"] as? String ?: "IN",
            pincode = deliveryAddress["pinCode"] as? String ?: "",
            phone = deliveryAddress["phone"] as? String ?: "",
        )
    }
}
