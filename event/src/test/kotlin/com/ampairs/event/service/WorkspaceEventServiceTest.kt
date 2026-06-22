package com.ampairs.event.service

import com.ampairs.core.multitenancy.DeviceContextHolder
import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.WorkspaceEvent
import com.ampairs.event.repository.WorkspaceEventRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable

class WorkspaceEventServiceTest {

    private val eventRepository: WorkspaceEventRepository = mock()

    private lateinit var service: WorkspaceEventService

    private fun event(uid: String, seq: Long = 1): WorkspaceEvent =
        WorkspaceEvent().apply {
            this.uid = uid
            this.sequenceNumber = seq
        }

    @BeforeEach
    fun setUp() {
        service = WorkspaceEventService(eventRepository)
    }

    @AfterEach
    fun tearDown() {
        TenantContextHolder.clearTenantContext()
        DeviceContextHolder.clearDeviceContext()
    }

    @Test
    fun `getEventsSince throws when no workspace context`() {
        TenantContextHolder.clearTenantContext()

        assertThrows(IllegalStateException::class.java) {
            service.getEventsSince(0, 50)
        }
    }

    @Test
    fun `getEventsSince uses explicit excludeDeviceId when provided`() {
        TenantContextHolder.setCurrentTenant("ws-1")
        val content = listOf(event("EVT-1", 5), event("EVT-2", 6))
        whenever(
            eventRepository.findEventsSinceSequence(eq("ws-1"), eq(4L), eq("dev-A"), any<Pageable>())
        ).thenReturn(PageImpl(content))

        val result = service.getEventsSince(sinceSequence = 4, limit = 50, excludeDeviceId = "dev-A")

        assertEquals(2, result.size)
        verify(eventRepository).findEventsSinceSequence(eq("ws-1"), eq(4L), eq("dev-A"), any<Pageable>())
    }

    @Test
    fun `getEventsSince falls back to device context when excludeDeviceId is null`() {
        TenantContextHolder.setCurrentTenant("ws-1")
        DeviceContextHolder.setCurrentDevice("dev-ctx")
        whenever(
            eventRepository.findEventsSinceSequence(eq("ws-1"), eq(0L), eq("dev-ctx"), any<Pageable>())
        ).thenReturn(PageImpl(emptyList()))

        val result = service.getEventsSince(0, 25)

        assertEquals(0, result.size)
        verify(eventRepository).findEventsSinceSequence(eq("ws-1"), eq(0L), eq("dev-ctx"), any<Pageable>())
    }

    @Test
    fun `getEventsSince uses unknown device when neither provided nor in context`() {
        TenantContextHolder.setCurrentTenant("ws-1")
        DeviceContextHolder.clearDeviceContext()
        whenever(
            eventRepository.findEventsSinceSequence(eq("ws-1"), eq(0L), eq("unknown"), any<Pageable>())
        ).thenReturn(PageImpl(emptyList()))

        service.getEventsSince(0, 10)

        verify(eventRepository).findEventsSinceSequence(eq("ws-1"), eq(0L), eq("unknown"), any<Pageable>())
    }

    @Test
    fun `getAllEvents throws when no workspace context`() {
        TenantContextHolder.clearTenantContext()

        assertThrows(IllegalStateException::class.java) {
            service.getAllEvents(PageRequest.of(0, 10))
        }
    }

    @Test
    fun `getAllEvents delegates to repository for current workspace`() {
        TenantContextHolder.setCurrentTenant("ws-2")
        val page: Page<WorkspaceEvent> = PageImpl(listOf(event("EVT-9", 9)))
        whenever(
            eventRepository.findByWorkspaceIdOrderBySequenceNumberAsc(eq("ws-2"), any<Pageable>())
        ).thenReturn(page)

        val result = service.getAllEvents(PageRequest.of(0, 10))

        assertEquals(1, result.content.size)
        verify(eventRepository).findByWorkspaceIdOrderBySequenceNumberAsc(eq("ws-2"), any<Pageable>())
    }

    @Test
    fun `getEventByUid returns the matching event`() {
        val evt = event("EVT-7")
        whenever(eventRepository.findByUid("EVT-7")).thenReturn(evt)

        assertSame(evt, service.getEventByUid("EVT-7"))
    }

    @Test
    fun `getEventByUid returns null when not found`() {
        whenever(eventRepository.findByUid("missing")).thenReturn(null)

        assertNull(service.getEventByUid("missing"))
    }
}
