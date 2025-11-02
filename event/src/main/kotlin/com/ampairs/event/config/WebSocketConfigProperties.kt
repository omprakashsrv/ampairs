package com.ampairs.event.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for WebSocket and message broker setup.
 *
 * **Message Broker Options**:
 * 1. **SIMPLE** - In-memory broker (single instance, development)
 * 2. **RABBITMQ** - External RabbitMQ broker (multi-instance, production)
 * 3. **AUTO** - Auto-detect: try RabbitMQ, fallback to SIMPLE if unavailable
 *
 * **Use Cases**:
 * - Development/Single Instance: Use SIMPLE (no external dependencies)
 * - Production/Multi Instance: Use RABBITMQ (distributed messaging)
 * - Flexible Deployment: Use AUTO (automatic fallback)
 *
 * @property type Message broker type (SIMPLE, RABBITMQ, AUTO)
 * @property rabbitmq RabbitMQ configuration (only used when type=RABBITMQ or AUTO)
 */
@Configuration
@ConfigurationProperties(prefix = "websocket.message-broker")
data class WebSocketConfigProperties(
    /**
     * Message broker type.
     *
     * - SIMPLE: In-memory broker (no external dependencies)
     * - RABBITMQ: External RabbitMQ STOMP relay
     * - AUTO: Try RabbitMQ, fallback to SIMPLE if unavailable (default)
     */
    var type: MessageBrokerType = MessageBrokerType.AUTO,

    /**
     * STOMP protocol heartbeat interval in milliseconds.
     *
     * SimpleBroker: Set to 0 to disable (recommended - use application heartbeats instead)
     * RabbitMQ: Can use non-zero values (e.g., 10000ms) - properly supported
     *
     * Default: 0 (disabled)
     */
    var heartbeatInterval: Long = 0,

    /**
     * RabbitMQ STOMP relay configuration.
     * Only used when type=RABBITMQ or type=AUTO.
     */
    var rabbitmq: RabbitMQConfig = RabbitMQConfig()
) {
    enum class MessageBrokerType {
        /** In-memory SimpleBroker (single instance only) */
        SIMPLE,

        /** External RabbitMQ broker (supports multi-instance) */
        RABBITMQ,

        /** Auto-detect: try RabbitMQ, fallback to SIMPLE */
        AUTO
    }

    /**
     * RabbitMQ STOMP relay configuration.
     */
    data class RabbitMQConfig(
        var host: String = "localhost",
        var port: Int = 61613,  // STOMP port
        var username: String = "guest",
        var password: String = "guest",
        var virtualHost: String = "/",
        var connectionTimeout: Int = 500  // milliseconds for availability probe
    )
}
