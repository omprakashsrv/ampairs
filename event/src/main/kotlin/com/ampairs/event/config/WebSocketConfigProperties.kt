package com.ampairs.event.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "websocket.message-broker")
data class WebSocketConfigProperties(
    var type: MessageBrokerType = MessageBrokerType.AUTO,
    var heartbeatInterval: Long = 0,
    var kafka: KafkaConfig = KafkaConfig()
) {
    enum class MessageBrokerType {
        /** In-memory SimpleBroker — single instance, no external dependencies */
        SIMPLE,

        /** Kafka-backed fan-out — multi-instance, Kafka required */
        KAFKA,

        /** Auto-detect: probe Kafka bootstrap server, fall back to SIMPLE if unavailable */
        AUTO
    }

    data class KafkaConfig(
        var bootstrapServers: String = "localhost:9092",
        var topicName: String = "workspace-events",
        var connectionTimeout: Int = 500
    )
}
