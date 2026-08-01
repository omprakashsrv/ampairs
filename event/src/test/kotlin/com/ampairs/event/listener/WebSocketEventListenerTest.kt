package com.ampairs.event.listener

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.DeviceStatus
import com.ampairs.event.domain.WebSocketSession
import com.ampairs.event.domain.dto.UserStatusEvent
import com.ampairs.event.repository.WebSocketSessionRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.messaging.Message
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.stomp.StompCommand
import org.springframework.messaging.simp.stomp.StompHeaderAccessor
import org.springframework.messaging.support.MessageBuilder
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.messaging.SessionConnectEvent
import org.springframework.web.socket.messaging.SessionDisconnectEvent

class WebSocketEventListenerTest {

    private val repository: WebSocketSessionRepository = mock()
    private val messagingTemplate: SimpMessagingTemplate = mock()

    private lateinit var listener: WebSocketEventListener

    @BeforeEach
    fun setUp() {
        listener = WebSocketEventListener(repository, messagingTemplate)
        whenever(repository.save(any<WebSocketSession>())).thenAnswer { it.arguments[0] }
    }

    @AfterEach
    fun tearDown() = TenantContextHolder.clearTenantContext()

    private fun connectMessage(
        sessionId: String?,
        attrs: Map<String, Any>?,
        userAgent: String? = null,
    ): Message<ByteArray> {
        val accessor = StompHeaderAccessor.create(StompCommand.CONNECT)
        if (sessionId != null) accessor.sessionId = sessionId
        if (attrs != null) accessor.sessionAttributes = attrs.toMutableMap()
        if (userAgent != null) accessor.setNativeHeader("User-Agent", userAgent)
        return MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
    }

    @Test
    fun `connect registers a new device session and broadcasts ONLINE`() {
        whenever(repository.findByWorkspaceIdAndUserIdAndDeviceId("ws-1", "usr-1", "dev-1"))
            .thenReturn(null)

        val msg = connectMessage(
            "sess-1",
            mapOf("userId" to "usr-1", "tenantId" to "ws-1", "deviceId" to "dev-1"),
            userAgent = "Mozilla Mobile",
        )
        listener.handleWebSocketConnectEvent(SessionConnectEvent(this, msg))

        val sessionCaptor = argumentCaptor<WebSocketSession>()
        verify(repository).save(sessionCaptor.capture())
        assertEquals("ws-1", sessionCaptor.firstValue.workspaceId)
        assertEquals(DeviceStatus.ONLINE, sessionCaptor.firstValue.status)
        assertEquals("Mobile App", sessionCaptor.firstValue.deviceName)

        val statusCaptor = argumentCaptor<UserStatusEvent>()
        verify(messagingTemplate).convertAndSend(any<String>(), statusCaptor.capture())
        assertEquals(DeviceStatus.ONLINE, statusCaptor.firstValue.status)
    }

    @Test
    fun `connect reactivates an existing session`() {
        val existing = WebSocketSession().apply {
            uid = "WSS-1"; workspaceId = "ws-1"; userId = "usr-1"; deviceId = "dev-1"
            status = DeviceStatus.OFFLINE; deviceName = "Old"
        }
        whenever(repository.findByWorkspaceIdAndUserIdAndDeviceId("ws-1", "usr-1", "dev-1"))
            .thenReturn(existing)

        val msg = connectMessage("sess-2", mapOf("userId" to "usr-1", "workspaceId" to "ws-1", "deviceId" to "dev-1"))
        listener.handleWebSocketConnectEvent(SessionConnectEvent(this, msg))

        assertEquals(DeviceStatus.ONLINE, existing.status)
        verify(repository).save(existing)
    }

    @Test
    fun `connect with no session attributes is ignored`() {
        val msg = connectMessage("sess-3", attrs = null)
        listener.handleWebSocketConnectEvent(SessionConnectEvent(this, msg))

        verify(repository, never()).save(any<WebSocketSession>())
    }

    @Test
    fun `connect with missing workspace is skipped`() {
        val msg = connectMessage("sess-4", mapOf("userId" to "usr-1", "deviceId" to "dev-1"))
        listener.handleWebSocketConnectEvent(SessionConnectEvent(this, msg))

        verify(repository, never()).save(any<WebSocketSession>())
    }

    @Test
    fun `connect with missing userId is skipped`() {
        val msg = connectMessage("sess-5", mapOf("tenantId" to "ws-1", "deviceId" to "dev-1"))
        listener.handleWebSocketConnectEvent(SessionConnectEvent(this, msg))

        verify(repository, never()).save(any<WebSocketSession>())
    }

    @Test
    fun `disconnect marks the session offline and broadcasts`() {
        val existing = WebSocketSession().apply {
            uid = "WSS-9"; workspaceId = "ws-1"; userId = "usr-1"; deviceId = "dev-1"
            sessionId = "sess-9"; status = DeviceStatus.ONLINE
        }
        whenever(repository.findBySessionIdCrossTenant("sess-9")).thenReturn(existing)

        listener.handleWebSocketDisconnectEvent(disconnectEvent("sess-9"))

        assertEquals(DeviceStatus.OFFLINE, existing.status)
        verify(repository).save(existing)
        verify(messagingTemplate).convertAndSend(any<String>(), any<UserStatusEvent>())
    }

    @Test
    fun `disconnect for unknown session is a no-op`() {
        whenever(repository.findBySessionIdCrossTenant("sess-x")).thenReturn(null)

        listener.handleWebSocketDisconnectEvent(disconnectEvent("sess-x"))

        verify(repository, never()).save(any<WebSocketSession>())
        verify(messagingTemplate, never()).convertAndSend(any<String>(), any<UserStatusEvent>())
    }

    private fun disconnectEvent(sessionId: String): SessionDisconnectEvent {
        val accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT)
        accessor.sessionId = sessionId
        val msg = MessageBuilder.createMessage(ByteArray(0), accessor.messageHeaders)
        return SessionDisconnectEvent(this, msg, sessionId, CloseStatus.NORMAL)
    }
}
