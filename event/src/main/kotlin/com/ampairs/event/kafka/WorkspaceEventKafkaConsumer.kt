package com.ampairs.event.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Conditional
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
@Conditional(KafkaAvailableCondition::class)
class WorkspaceEventKafkaConsumer(
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val logger = LoggerFactory.getLogger(WorkspaceEventKafkaConsumer::class.java)
    private val objectMapper = jacksonObjectMapper().apply {
        findAndRegisterModules()
    }

    @KafkaListener(
        topics = ["\${websocket.message-broker.kafka.topic-name:workspace-events}"],
        containerFactory = "workspaceEventListenerContainerFactory"
    )
    fun consume(payload: String) {
        try {
            val envelope = objectMapper.readValue(payload, KafkaEventEnvelope::class.java)
            messagingTemplate.convertAndSend(envelope.destination, envelope.event)
            logger.debug("Forwarded Kafka event to WebSocket: destination={}", envelope.destination)
        } catch (e: Exception) {
            logger.error("Failed to process Kafka event: {}", payload.take(200), e)
        }
    }
}
