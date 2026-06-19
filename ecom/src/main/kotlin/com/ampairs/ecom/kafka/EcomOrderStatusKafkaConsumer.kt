package com.ampairs.ecom.kafka

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.ecom.service.EcomOrderService
import com.ampairs.event.domain.kafka.EcomOrderStatusEvent
import com.ampairs.event.kafka.KafkaAvailableCondition
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
@Conditional(KafkaAvailableCondition::class)
class EcomOrderStatusKafkaConsumer(
    private val ecomOrderService: EcomOrderService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper().findAndRegisterModules()

    @KafkaListener(
        topics = ["ecom-order-status"],
        containerFactory = "ecomOrderStatusListenerContainerFactory",
        groupId = "ecom-order-status-consumer",
    )
    fun onEcomOrderStatus(payload: String, ack: Acknowledgment) {
        val event: EcomOrderStatusEvent = try {
            mapper.readValue(payload)
        } catch (e: Exception) {
            log.error("Failed to deserialize EcomOrderStatusEvent: {}", e.message)
            ack.acknowledge()
            return
        }

        TenantContextHolder.setCurrentTenant(event.workspaceId)
        try {
            ecomOrderService.applyStatusUpdate(event)
        } catch (e: Exception) {
            log.error("Failed to apply status update for ecomOrderRef={}: {}", event.ecomOrderRef, e.message, e)
            ack.acknowledge()
            return
        } finally {
            TenantContextHolder.clearTenantContext()
        }
        ack.acknowledge()
    }
}
