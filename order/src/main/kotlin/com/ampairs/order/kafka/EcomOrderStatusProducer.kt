package com.ampairs.order.kafka

import com.ampairs.event.domain.kafka.EcomOrderStatusEvent
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class EcomOrderStatusProducer(
    @Qualifier("ecomOrderStatusKafkaTemplate") private val ecomOrderStatusKafkaTemplate: KafkaTemplate<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val mapper = jacksonObjectMapper().findAndRegisterModules()

    fun publishStatusUpdate(event: EcomOrderStatusEvent) {
        try {
            val payload = mapper.writeValueAsString(event)
            ecomOrderStatusKafkaTemplate.send("ecom-order-status", event.workspaceId, payload)
        } catch (e: Exception) {
            log.error("Failed to publish EcomOrderStatusEvent for {}: {}", event.ecomOrderRef, e.message, e)
            throw e
        }
    }
}
