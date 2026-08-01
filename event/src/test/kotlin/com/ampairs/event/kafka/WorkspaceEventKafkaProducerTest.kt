package com.ampairs.event.kafka

import com.ampairs.event.config.WebSocketConfigProperties
import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.dto.WorkspaceEventResponse
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import java.time.Instant
import java.util.concurrent.CompletableFuture

class WorkspaceEventKafkaProducerTest {

    @Suppress("UNCHECKED_CAST")
    private val kafkaTemplate: KafkaTemplate<String, String> = mock()
    private val props = WebSocketConfigProperties(
        kafka = WebSocketConfigProperties.KafkaConfig(topicName = "workspace-events"),
    )

    private lateinit var producer: WorkspaceEventKafkaProducer

    private fun response() = WorkspaceEventResponse(
        uid = "EVT-1",
        eventType = EventType.CUSTOMER_UPDATED,
        entityType = "customer",
        entityId = "CUS-1",
        payload = mapOf("k" to "v"),
        lastUpdatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        deviceId = "dev-1",
        userId = "usr-1",
        sequenceNumber = 5,
        workspaceId = "ws-1",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @BeforeEach
    fun setUp() {
        producer = WorkspaceEventKafkaProducer(kafkaTemplate, props)
    }

    @Test
    fun `publish serializes the envelope and sends keyed by workspace`() {
        whenever(kafkaTemplate.send(any<String>(), any<String>(), any<String>()))
            .thenReturn(CompletableFuture<SendResult<String, String>>())

        producer.publish("/topic/workspace.events.ws-1", response())

        verify(kafkaTemplate).send(eq("workspace-events"), eq("ws-1"), any<String>())
    }

    @Test
    fun `publish swallows kafka send failures`() {
        doThrow(RuntimeException("kafka down"))
            .whenever(kafkaTemplate).send(any<String>(), any<String>(), any<String>())

        assertDoesNotThrow { producer.publish("/topic/workspace.events.ws-1", response()) }
    }

    @Test
    fun `envelope data class carries destination and event`() {
        val env = KafkaEventEnvelope("/topic/x", response())
        assertEquals("/topic/x", env.destination)
        assertEquals("EVT-1", env.event.uid)
        assertEquals(env, env.copy())
    }
}
