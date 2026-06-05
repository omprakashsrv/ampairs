package com.ampairs.event.kafka

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Condition
import org.springframework.context.annotation.ConditionContext
import org.springframework.core.type.AnnotatedTypeMetadata
import java.net.InetSocketAddress
import java.net.Socket
import java.util.Properties

/**
 * Spring Condition that resolves true when Kafka should be used as the WebSocket message broker.
 *
 * - type=KAFKA  → always true (Kafka required; startup fails if unavailable)
 * - type=AUTO   → probes the first bootstrap server via TCP; true only if reachable within the
 *                 configured connection-timeout. Falls back to SIMPLE (returns false) otherwise.
 * - type=SIMPLE → always false (in-memory broker, no Kafka beans registered)
 */
class KafkaAvailableCondition : Condition {

    private val logger = LoggerFactory.getLogger(KafkaAvailableCondition::class.java)

    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean {
        val env = context.environment
        val type = env.getProperty("websocket.message-broker.type", "AUTO").uppercase()

        return when (type) {
            "KAFKA" -> true
            "AUTO" -> {
                val bootstrapServers = env.getProperty(
                    "websocket.message-broker.kafka.bootstrap-servers", "localhost:9092"
                )
                val timeoutMs = env.getProperty(
                    "websocket.message-broker.kafka.connection-timeout", Int::class.java, 500
                )
                val available = isKafkaReachable(bootstrapServers, timeoutMs)
                if (available) {
                    logger.info("AUTO mode: Kafka reachable at {} — using Kafka fan-out", bootstrapServers)
                } else {
                    logger.info("AUTO mode: Kafka not reachable at {} — falling back to SimpleBroker", bootstrapServers)
                }
                available
            }
            else -> false // SIMPLE or unknown
        }
    }

    private fun isKafkaReachable(bootstrapServers: String, timeoutMs: Int): Boolean {
        return bootstrapServers.split(",").any { server ->
            try {
                val trimmed = server.trim()
                val lastColon = trimmed.lastIndexOf(':')
                val host = if (lastColon > 0) trimmed.substring(0, lastColon) else trimmed
                val port = if (lastColon > 0) trimmed.substring(lastColon + 1).toIntOrNull() ?: 9092 else 9092
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                    true
                }
            } catch (e: Exception) {
                false
            }
        }
    }
}