package com.ampairs.event.controller

import com.ampairs.event.service.DeviceStatusService
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.messaging.simp.SimpMessageHeaderAccessor

class HeartbeatControllerTest {

    private val service: DeviceStatusService = mock()
    private lateinit var controller: HeartbeatController

    @BeforeEach
    fun setUp() {
        controller = HeartbeatController(service)
    }

    private fun accessor(sessionId: String?, attrs: Map<String, Any>? = mutableMapOf()): SimpMessageHeaderAccessor {
        val a = SimpMessageHeaderAccessor.create()
        if (sessionId != null) a.sessionId = sessionId
        if (attrs != null) a.sessionAttributes = attrs.toMutableMap()
        return a
    }

    @Test
    fun `heartbeat with blank session id is ignored`() {
        controller.handleHeartbeat(accessor(sessionId = null))

        verify(service, never()).updateHeartbeat(any())
    }

    @Test
    fun `heartbeat updates session using tenant and device attributes`() {
        whenever(service.updateHeartbeat("sess-1")).thenReturn(true)

        controller.handleHeartbeat(
            accessor("sess-1", mapOf("tenantId" to "ws-1", "deviceId" to "dev-1"))
        )

        verify(service).updateHeartbeat(eq("sess-1"))
    }

    @Test
    fun `heartbeat falls back to workspaceId attribute when tenantId blank`() {
        whenever(service.updateHeartbeat("sess-2")).thenReturn(true)

        controller.handleHeartbeat(
            accessor("sess-2", mapOf("tenantId" to "", "workspaceId" to "ws-2"))
        )

        verify(service).updateHeartbeat(eq("sess-2"))
    }

    @Test
    fun `heartbeat for unknown session does not throw`() {
        whenever(service.updateHeartbeat("sess-x")).thenReturn(false)

        assertDoesNotThrow {
            controller.handleHeartbeat(accessor("sess-x", mapOf("deviceId" to "dev-1")))
        }
        verify(service).updateHeartbeat(eq("sess-x"))
    }

    @Test
    fun `heartbeat swallows service exceptions`() {
        whenever(service.updateHeartbeat(any())).thenThrow(RuntimeException("boom"))

        assertDoesNotThrow {
            controller.handleHeartbeat(accessor("sess-3", mapOf("tenantId" to "ws-1")))
        }
    }

    @Test
    fun `ping delegates to heartbeat`() {
        whenever(service.updateHeartbeat("sess-4")).thenReturn(true)

        controller.handlePing(accessor("sess-4", mapOf("deviceId" to "dev-1")))

        verify(service).updateHeartbeat(eq("sess-4"))
    }
}
