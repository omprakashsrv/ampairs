package com.ampairs.event.controller

import com.ampairs.event.domain.EventType
import com.ampairs.event.domain.WorkspaceEvent
import com.ampairs.event.service.WorkspaceEventService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable

class WorkspaceEventControllerTest {

    private val service: WorkspaceEventService = mock()
    private lateinit var controller: WorkspaceEventController

    private fun event(uid: String, seq: Long = 1): WorkspaceEvent = WorkspaceEvent().apply {
        this.uid = uid
        this.eventType = EventType.CUSTOMER_UPDATED
        this.entityType = "customer"
        this.entityId = "CUS-1"
        this.sequenceNumber = seq
        this.workspaceId = "ws-1"
    }

    @BeforeEach
    fun setUp() {
        controller = WorkspaceEventController(service)
    }

    @Test
    fun `getEvents maps events since the given sequence`() {
        whenever(service.getEventsSince(eq(5L), eq(100), eq("dev-1")))
            .thenReturn(listOf(event("EVT-1", 6), event("EVT-2", 7)))

        val response = controller.getEvents(sinceSequence = 5, limit = 100, deviceId = "dev-1")

        assertTrue(response.success)
        assertEquals(2, response.data?.size)
        assertEquals(setOf("EVT-1", "EVT-2"), response.data?.map { it.uid }?.toSet())
    }

    @Test
    fun `getAllEvents returns paged content`() {
        whenever(service.getAllEvents(any<Pageable>()))
            .thenReturn(PageImpl(listOf(event("EVT-9", 9))))

        val response = controller.getAllEvents(0, 50)

        assertTrue(response.success)
        assertEquals(1, response.data?.size)
        assertEquals("EVT-9", response.data?.first()?.uid)
    }

    @Test
    fun `getEvent returns mapped event when found`() {
        whenever(service.getEventByUid("EVT-1")).thenReturn(event("EVT-1"))

        val response = controller.getEvent("EVT-1")

        assertTrue(response.success)
        assertEquals("EVT-1", response.data?.uid)
    }

    @Test
    fun `getEvent returns null data when not found`() {
        whenever(service.getEventByUid("missing")).thenReturn(null)

        val response = controller.getEvent("missing")

        assertTrue(response.success)
        assertNull(response.data)
    }
}
