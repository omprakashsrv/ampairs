package com.ampairs.event.controller

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.event.domain.DeviceStatus
import com.ampairs.event.domain.WebSocketSession
import com.ampairs.event.service.DeviceStatusService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import java.time.Instant

class DeviceStatusControllerTest {

    private val service: DeviceStatusService = mock()
    private lateinit var controller: DeviceStatusController

    private fun session(): WebSocketSession = WebSocketSession().apply {
        uid = "WSS-1"; workspaceId = "ws-1"; userId = "usr-1"; deviceId = "dev-1"
        deviceName = "Pixel"; sessionId = "sess-1"; status = DeviceStatus.ONLINE
        lastHeartbeat = Instant.now(); connectedAt = Instant.now()
    }

    @BeforeEach
    fun setUp() {
        controller = DeviceStatusController(service)
    }

    @AfterEach
    fun tearDown() = TenantContextHolder.clearTenantContext()

    @Test
    fun `getActiveDevices throws without workspace context`() {
        TenantContextHolder.clearTenantContext()
        assertThrows(IllegalStateException::class.java) { controller.getActiveDevices(0, 50) }
    }

    @Test
    fun `getActiveDevices returns mapped sessions`() {
        TenantContextHolder.setCurrentTenant("ws-1")
        whenever(service.getActiveSessions(eq("ws-1"), any<Pageable>()))
            .thenReturn(PageImpl(listOf(session())))

        val response = controller.getActiveDevices(0, 50)

        assertTrue(response.success)
        assertEquals(1, response.data?.size)
        assertEquals("dev-1", response.data?.first()?.deviceId)
    }

    @Test
    fun `getActiveDevicesByUser returns mapped sessions`() {
        TenantContextHolder.setCurrentTenant("ws-1")
        whenever(service.getActiveSessionsByUser(eq("ws-1"), eq("usr-1"), any<Pageable>()))
            .thenReturn(PageImpl(listOf(session())))

        val response = controller.getActiveDevicesByUser("usr-1", 0, 20)

        assertTrue(response.success)
        assertEquals(1, response.data?.size)
    }

    @Test
    fun `getActiveDevicesByUser throws without workspace context`() {
        TenantContextHolder.clearTenantContext()
        assertThrows(IllegalStateException::class.java) { controller.getActiveDevicesByUser("usr-1", 0, 20) }
    }

    @Test
    fun `getActiveDeviceCount returns count`() {
        TenantContextHolder.setCurrentTenant("ws-1")
        whenever(service.countActiveSessions("ws-1")).thenReturn(4L)

        val response = controller.getActiveDeviceCount()

        assertTrue(response.success)
        assertEquals(4L, response.data)
    }

    @Test
    fun `getActiveDeviceCount throws without workspace context`() {
        TenantContextHolder.clearTenantContext()
        assertThrows(IllegalStateException::class.java) { controller.getActiveDeviceCount() }
    }
}
