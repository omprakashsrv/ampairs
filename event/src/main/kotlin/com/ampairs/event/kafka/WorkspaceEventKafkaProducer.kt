package com.ampairs.event.kafka

import com.ampairs.event.config.WebSocketConfigProperties
import com.ampairs.event.domain.dto.WorkspaceEventResponse
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Conditional
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

data class KafkaEventEnvelope(val destination: String, val event: WorkspaceEventResponse)

@Component
@Conditional(KafkaAvailableCondition::class)
class WorkspaceEventKafkaProducer(
    @Qualifier("workspaceEventKafkaTemplate") private val kafkaTemplate: KafkaTemplate<String, String>,
    private val props: WebSocketConfigProperties
) {
    private val logger = LoggerFactory.getLogger(WorkspaceEventKafkaProducer::class.java)
    private val objectMapper = jacksonObjectMapper().apply {
        findAndRegisterModules()
    }

    fun publish(destination: String, event: WorkspaceEventResponse) {
        try {
            val envelope = KafkaEventEnvelope(destination, event)
            val payload = objectMapper.writeValueAsString(envelope)
            kafkaTemplate.send(props.kafka.topicName, event.workspaceId, payload)
            logger.debug("Published event to Kafka: topic={}, workspace={}, type={}", props.kafka.topicName, event.workspaceId, event.eventType)
        } catch (e: Exception) {
            logger.error("Failed to publish event to Kafka: workspace={}, type={}", event.workspaceId, event.eventType, e)
        }
    }
}
