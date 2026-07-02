package com.ampairs.event.domain.dto

import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.WorkspaceEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class WorkspaceEventResponseTest {

    private fun event(block: WorkspaceEvent.() -> Unit = {}): WorkspaceEvent =
        WorkspaceEvent().apply {
            uid = "EVT-1"
            eventType = EventType.CUSTOMER_UPDATED
            entityType = "customer"
            entityId = "CUS-1"
            deviceId = "dev-1"
            userId = "usr-1"
            sequenceNumber = 42
            workspaceId = "ws-1"
            block()
        }

    @Test
    fun `maps base fields and parses last_updated_at from payload`() {
        val e = event {
            payload = """{"last_updated_at":"2026-01-02T03:04:05Z","action":"updated"}"""
        }

        val res = e.asWorkspaceEventResponse()

        assertEquals("EVT-1", res.uid)
        assertEquals(EventType.CUSTOMER_UPDATED, res.eventType)
        assertEquals("customer", res.entityType)
        assertEquals("CUS-1", res.entityId)
        assertEquals(42L, res.sequenceNumber)
        assertEquals("ws-1", res.workspaceId)
        assertEquals(Instant.parse("2026-01-02T03:04:05Z"), res.lastUpdatedAt)
        assertEquals("updated", res.payload["action"])
    }

    @Test
    fun `lastUpdatedAt is null when payload empty`() {
        val res = event { payload = "" }.asWorkspaceEventResponse()

        assertNull(res.lastUpdatedAt)
        assertTrue(res.payload.isEmpty())
    }

    @Test
    fun `lastUpdatedAt is null when timestamp unparseable`() {
        val res = event { payload = """{"last_updated_at":"not-a-date"}""" }.asWorkspaceEventResponse()

        assertNull(res.lastUpdatedAt)
    }

    @Test
    fun `malformed json payload yields empty map and null timestamp`() {
        val res = event { payload = "{not valid json" }.asWorkspaceEventResponse()

        assertTrue(res.payload.isEmpty())
        assertNull(res.lastUpdatedAt)
    }

    @Test
    fun `list mapper maps each event`() {
        val list = listOf(
            event { uid = "EVT-1" },
            event { uid = "EVT-2"; sequenceNumber = 43 },
        )

        val responses = list.asWorkspaceEventResponses()

        assertEquals(2, responses.size)
        assertEquals(setOf("EVT-1", "EVT-2"), responses.map { it.uid }.toSet())
    }
}
