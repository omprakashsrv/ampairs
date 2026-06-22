package com.ampairs.order.kafka

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.kafka.EcomOrderPlacedEvent
import com.ampairs.event.kafka.KafkaAvailableCondition
import com.ampairs.order.service.EcomOrderIngestionService
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * Kafka transport for ecom "order placed" events. Disabled unless a broker is reachable
 * ([KafkaAvailableCondition]); the in-process Spring listener (EcomOrderPlacedListener) is the
 * default path. Delegates the actual work to [EcomOrderIngestionService] so both transports share
 * one implementation.
 */
@Component
@Conditional(KafkaAvailableCondition::class)
class EcomOrderPlacedConsumer(
    private val ingestionService: EcomOrderIngestionService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper().findAndRegisterModules()

    @KafkaListener(
        topics = ["ecom-order-placed"],
        containerFactory = "ecomOrderPlacedListenerContainerFactory",
        groupId = "management-ecom-order-consumer",
    )
    fun onEcomOrderPlaced(payload: String, ack: Acknowledgment) {
        val event: EcomOrderPlacedEvent = try {
            mapper.readValue(payload)
        } catch (e: Exception) {
            log.error("Failed to deserialize EcomOrderPlacedEvent: {}", e.message)
            ack.acknowledge()
            return
        }

        TenantContextHolder.setCurrentTenant(event.workspaceId)
        try {
            ingestionService.ingest(event)
        } finally {
            TenantContextHolder.clearTenantContext()
        }
        ack.acknowledge()
    }
}
