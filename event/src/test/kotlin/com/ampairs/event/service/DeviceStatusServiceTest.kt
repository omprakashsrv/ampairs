package com.ampairs.event.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.DeviceStatus
import com.ampairs.event.domain.WebSocketSession
import com.ampairs.event.domain.dto.UserStatusEvent
import com.ampairs.event.repository.WebSocketSessionRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.messaging.simp.SimpMessagingTemplate
import java.time.Instant

class DeviceStatusServiceTest {

    private val repository: WebSocketSessionRepository = mock()
    private val messagingTemplate: SimpMessagingTemplate = mock()

    private lateinit var service: DeviceStatusService

    private fun session(block: WebSocketSession.() -> Unit = {}): WebSocketSession =
        WebSocketSession().apply {
            uid = "WSS-1"
            workspaceId = "ws-1"
            userId = "usr-1"
            deviceId = "dev-1"
            deviceName = "Pixel"
            sessionId = "sess-1"
            status = DeviceStatus.ONLINE
            lastHeartbeat = Instant.now()
            connectedAt = Instant.now()
            block()
        }

    @BeforeEach
    fun setUp() {
        service = DeviceStatusService(repository, messagingTemplate)
        whenever(repository.save(any<WebSocketSession>())).thenAnswer { it.arguments[0] }
    }

    @AfterEach
    fun tearDown() = TenantContextHolder.clearTenantContext()

    @Test
    fun `markOnline reuses an existing session`() {
        val existing = session { status = DeviceStatus.OFFLINE }
        whenever(repository.findByWorkspaceIdAndUserIdAndDeviceId("ws-1", "usr-1", "dev-1"))
            .thenReturn(existing)

        val result = service.markOnline("ws-1", "usr-1", "dev-1", "sess-new", "iPhone")

        assertEquals(DeviceStatus.ONLINE, result.status)
        assertEquals("sess-new", result.sessionId)
        assertEquals("iPhone", result.deviceName)
        verify(repository).save(existing)
    }

    @Test
    fun `markOnline creates a new session when none exists`() {
        whenever(repository.findByWorkspaceIdAndUserIdAndDeviceId("ws-1", "usr-2", "dev-2"))
            .thenReturn(null)

        val result = service.markOnline("ws-1", "usr-2", "dev-2", "sess-2")

        assertEquals("usr-2", result.userId)
        assertEquals("dev-2", result.deviceId)
        assertEquals(DeviceStatus.ONLINE, result.status)
        verify(repository).save(any<WebSocketSession>())
    }

    @Test
    fun `updateHeartbeat returns false when session not found`() {
        whenever(repository.findBySessionId("none")).thenReturn(null)

        assertFalse(service.updateHeartbeat("none"))
        verify(repository, never()).save(any<WebSocketSession>())
    }

    @Test
    fun `updateHeartbeat broadcasts when transitioning AWAY to ONLINE`() {
        val away = session { status = DeviceStatus.AWAY }
        whenever(repository.findBySessionId("sess-1")).thenReturn(away)

        val result = service.updateHeartbeat("sess-1")

        assertTrue(result)
        assertEquals(DeviceStatus.ONLINE, away.status)
        val captor = argumentCaptor<UserStatusEvent>()
        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assertEquals(DeviceStatus.ONLINE, captor.firstValue.status)
    }

    @Test
    fun `updateHeartbeat does not broadcast when already ONLINE`() {
        val online = session { status = DeviceStatus.ONLINE }
        whenever(repository.findBySessionId("sess-1")).thenReturn(online)

        assertTrue(service.updateHeartbeat("sess-1"))
        verify(messagingTemplate, never()).convertAndSend(any<String>(), any<UserStatusEvent>())
    }

    @Test
    fun `markOffline updates status and broadcasts OFFLINE`() {
        val s = session()
        whenever(repository.findBySessionId("sess-1")).thenReturn(s)

        service.markOffline("sess-1")

        assertEquals(DeviceStatus.OFFLINE, s.status)
        verify(repository).save(s)
        val captor = argumentCaptor<UserStatusEvent>()
        verify(messagingTemplate).convertAndSend(any<String>(), captor.capture())
        assertEquals(DeviceStatus.OFFLINE, captor.firstValue.status)
    }

    @Test
    fun `markOffline is a no-op when session not found`() {
        whenever(repository.findBySessionId("none")).thenReturn(null)

        service.markOffline("none")

        verify(repository, never()).save(any<WebSocketSession>())
        verify(messagingTemplate, never()).convertAndSend(any<String>(), any<UserStatusEvent>())
    }

    @Test
    fun `broadcastStatusChange sends to the workspace status topic`() {
        service.broadcastStatusChange("ws-1", "usr-1", "dev-1", DeviceStatus.AWAY, "Pixel")

        val topicCaptor = argumentCaptor<String>()
        val eventCaptor = argumentCaptor<UserStatusEvent>()
        verify(messagingTemplate).convertAndSend(topicCaptor.capture(), eventCaptor.capture())
        assertTrue(topicCaptor.firstValue.endsWith("ws-1"))
        assertEquals("usr-1", eventCaptor.firstValue.userId)
        assertEquals(DeviceStatus.AWAY, eventCaptor.firstValue.status)
    }

    @Test
    fun `getActiveSessions delegates to repository`() {
        val pageable: Pageable = PageRequest.of(0, 10)
        whenever(repository.findActiveSessions("ws-1", pageable))
            .thenReturn(PageImpl(listOf(session())))

        val result = service.getActiveSessions("ws-1", pageable)

        assertEquals(1, result.content.size)
    }

    @Test
    fun `countActiveSessions delegates to repository`() {
        whenever(repository.countActiveSessions("ws-1")).thenReturn(3L)

        assertEquals(3L, service.countActiveSessions("ws-1"))
    }

    @Test
    fun `detectStaleAndAwaySessions marks a stale session OFFLINE and broadcasts`() {
        val stale = session { lastHeartbeat = Instant.now().minusSeconds(600) } // > 2 min
        whenever(repository.findActiveSessionsWithHeartbeatBeforeCrossTenant(any<Instant>()))
            .thenReturn(listOf(stale))

        service.detectStaleAndAwaySessions()

        assertEquals(DeviceStatus.OFFLINE, stale.status)
        verify(repository).save(stale)
        verify(messagingTemplate).convertAndSend(any<String>(), any<UserStatusEvent>())
    }

    @Test
    fun `detectStaleAndAwaySessions marks an idle (not stale) session AWAY`() {
        // 45s old: idle (>30s) but not stale (<2min)
        val idle = session { lastHeartbeat = Instant.now().minusSeconds(45) }
        whenever(repository.findActiveSessionsWithHeartbeatBeforeCrossTenant(any<Instant>()))
            .thenReturn(listOf(idle))

        service.detectStaleAndAwaySessions()

        assertEquals(DeviceStatus.AWAY, idle.status)
        verify(repository).save(idle)
    }

    @Test
    fun `cleanupOldSessions logs deletions without throwing`() {
        whenever(repository.deleteOfflineSessionsBefore(any<Instant>())).thenReturn(5)

        service.cleanupOldSessions()

        verify(repository).deleteOfflineSessionsBefore(any<Instant>())
    }
}
