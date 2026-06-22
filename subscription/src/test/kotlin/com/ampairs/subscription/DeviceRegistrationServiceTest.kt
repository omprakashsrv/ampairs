package com.ampairs.subscription

import com.ampairs.subscription.domain.dto.RegisterDeviceRequest
import com.ampairs.subscription.domain.model.DevicePlatform
import com.ampairs.subscription.domain.model.DeviceRegistration
import com.ampairs.subscription.domain.model.SubscriptionAccessMode
import com.ampairs.subscription.domain.repository.DeviceRegistrationRepository
import com.ampairs.subscription.domain.repository.SubscriptionRepository
import com.ampairs.subscription.domain.service.DeviceRegistrationService
import com.ampairs.subscription.domain.service.SubscriptionService
import com.ampairs.subscription.domain.service.UsageTrackingService
import com.ampairs.subscription.exception.SubscriptionException
import com.ampairs.subscription.listener.ResourceType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import com.ampairs.subscription.domain.service.LimitCheckResult

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DeviceRegistrationServiceTest {

    @Mock
    private lateinit var deviceRepository: DeviceRegistrationRepository

    @Mock
    private lateinit var subscriptionRepository: SubscriptionRepository

    @Mock
    private lateinit var subscriptionService: SubscriptionService

    @Mock
    private lateinit var usageTrackingService: UsageTrackingService

    private lateinit var service: DeviceRegistrationService

    @BeforeEach
    fun setUp() {
        service = DeviceRegistrationService(
            deviceRepository, subscriptionRepository, subscriptionService, usageTrackingService
        )
        whenever(deviceRepository.save(any<DeviceRegistration>())).thenAnswer { it.arguments[0] }
        whenever(deviceRepository.countActiveByWorkspaceId(any())).thenReturn(0L)
    }

    private fun allowed() = LimitCheckResult(allowed = true, limit = 5, current = 1, remaining = 4)
    private fun denied() = LimitCheckResult(allowed = false, limit = 2, current = 2, remaining = 0, isUnlimited = false)

    private fun request(deviceId: String = "DEV-1") = RegisterDeviceRequest(
        deviceId = deviceId,
        deviceName = "Pixel",
        platform = DevicePlatform.ANDROID,
        osVersion = "14",
        appVersion = "1.0.0"
    )

    @Test
    fun `registerDevice creates a new device and increments usage`() {
        whenever(subscriptionService.checkLimit(eq("WS-1"), eq("DEVICE"), any())).thenReturn(allowed())
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-1")).thenReturn(null)

        val result = service.registerDevice("WS-1", "USR-1", request())

        assertEquals("DEV-1", result.deviceId)
        assertEquals(DevicePlatform.ANDROID, result.platform)
        verify(usageTrackingService).incrementCount("WS-1", ResourceType.DEVICE)
    }

    @Test
    fun `registerDevice throws when device limit exceeded`() {
        whenever(subscriptionService.checkLimit(eq("WS-1"), eq("DEVICE"), any())).thenReturn(denied())

        assertThrows(SubscriptionException.LimitExceeded::class.java) {
            service.registerDevice("WS-1", "USR-1", request())
        }
    }

    @Test
    fun `registerDevice reactivates an inactive existing device`() {
        whenever(subscriptionService.checkLimit(eq("WS-1"), eq("DEVICE"), any())).thenReturn(allowed())
        val existing = DeviceRegistration().apply {
            workspaceId = "WS-1"; deviceId = "DEV-1"; isActive = false
        }
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-1")).thenReturn(existing)

        val result = service.registerDevice("WS-1", "USR-1", request())

        assertTrue(result.isActive)
        verify(usageTrackingService).incrementCount("WS-1", ResourceType.DEVICE)
    }

    @Test
    fun `registerDevice refreshes an active existing device without incrementing usage`() {
        whenever(subscriptionService.checkLimit(eq("WS-1"), eq("DEVICE"), any())).thenReturn(allowed())
        val existing = DeviceRegistration().apply {
            workspaceId = "WS-1"; deviceId = "DEV-1"; isActive = true
        }
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-1")).thenReturn(existing)

        service.registerDevice("WS-1", "USR-1", request())

        verify(usageTrackingService, never()).incrementCount(any(), any())
    }

    @Test
    fun `refreshDeviceToken throws when device not found`() {
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-X")).thenReturn(null)

        assertThrows(SubscriptionException.DeviceNotFound::class.java) {
            service.refreshDeviceToken("WS-1", "DEV-X", "1.0.0")
        }
    }

    @Test
    fun `refreshDeviceToken throws when device deactivated`() {
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-1"))
            .thenReturn(DeviceRegistration().apply { isActive = false })

        assertThrows(SubscriptionException.DeviceDeactivated::class.java) {
            service.refreshDeviceToken("WS-1", "DEV-1", "1.0.0")
        }
    }

    @Test
    fun `refreshDeviceToken updates app version`() {
        val device = DeviceRegistration().apply { workspaceId = "WS-1"; deviceId = "DEV-1"; isActive = true }
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-1")).thenReturn(device)

        val result = service.refreshDeviceToken("WS-1", "DEV-1", "2.5.0")

        assertEquals("2.5.0", result.appVersion)
    }

    @Test
    fun `getDevice returns mapped response`() {
        val device = DeviceRegistration().apply { workspaceId = "WS-1"; deviceId = "DEV-1"; isActive = true }
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-1")).thenReturn(device)

        assertEquals("DEV-1", service.getDevice("WS-1", "DEV-1").deviceId)
    }

    @Test
    fun `getDevice throws when not found`() {
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-X")).thenReturn(null)
        assertThrows(SubscriptionException.DeviceNotFound::class.java) { service.getDevice("WS-1", "DEV-X") }
    }

    @Test
    fun `getDevices and getUserDevices map lists`() {
        val d = DeviceRegistration().apply { deviceId = "DEV-1"; isActive = true }
        whenever(deviceRepository.findActiveByWorkspaceId("WS-1")).thenReturn(listOf(d))
        whenever(deviceRepository.findActiveByUserId("USR-1")).thenReturn(listOf(d))

        assertEquals(1, service.getDevices("WS-1").size)
        assertEquals(1, service.getUserDevices("USR-1").size)
    }

    @Test
    fun `deactivateDevice deactivates and decrements usage`() {
        val device = DeviceRegistration().apply { uid = "DEV-UID"; workspaceId = "WS-1"; isActive = true }
        whenever(deviceRepository.findByUid("DEV-UID")).thenReturn(device)

        val result = service.deactivateDevice("WS-1", "DEV-UID", "lost")

        assertTrue(result)
        assertEquals(false, device.isActive)
        assertEquals("lost", device.deactivationReason)
        verify(usageTrackingService).decrementCount("WS-1", ResourceType.DEVICE)
    }

    @Test
    fun `deactivateDevice throws when uid not found`() {
        whenever(deviceRepository.findByUid("DEV-UID")).thenReturn(null)
        assertThrows(SubscriptionException.DeviceNotFound::class.java) {
            service.deactivateDevice("WS-1", "DEV-UID", null)
        }
    }

    @Test
    fun `deactivateDevice throws when device belongs to another workspace`() {
        whenever(deviceRepository.findByUid("DEV-UID"))
            .thenReturn(DeviceRegistration().apply { workspaceId = "WS-OTHER" })
        assertThrows(SubscriptionException.DeviceNotFound::class.java) {
            service.deactivateDevice("WS-1", "DEV-UID", null)
        }
    }

    @Test
    fun `updateActivity updates only when device active`() {
        val device = DeviceRegistration().apply { uid = "DEV-UID"; isActive = true }
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-1")).thenReturn(device)

        service.updateActivity("WS-1", "DEV-1", "1.2.3.4")

        verify(deviceRepository).updateLastActivity(eq("DEV-UID"), any(), eq("1.2.3.4"))
    }

    @Test
    fun `updateActivity is a no-op when device missing`() {
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-1")).thenReturn(null)

        service.updateActivity("WS-1", "DEV-1", null)

        verify(deviceRepository, never()).updateLastActivity(any(), any(), any())
    }

    @Test
    fun `getAccessMode returns LOCKED when device missing`() {
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-X")).thenReturn(null)

        assertEquals(SubscriptionAccessMode.LOCKED, service.getAccessMode("WS-1", "DEV-X"))
    }

    @Test
    fun `getAccessMode returns FULL_ACCESS for a valid active device`() {
        val device = DeviceRegistration().apply {
            isActive = true
            tokenExpiresAt = java.time.Instant.now().plus(java.time.Duration.ofDays(5))
        }
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-1")).thenReturn(device)

        assertEquals(SubscriptionAccessMode.FULL_ACCESS, service.getAccessMode("WS-1", "DEV-1"))
    }

    @Test
    fun `updatePushToken throws when device missing`() {
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-X")).thenReturn(null)
        assertThrows(SubscriptionException.DeviceNotFound::class.java) {
            service.updatePushToken("WS-1", "DEV-X", "tok", "FCM")
        }
    }

    @Test
    fun `updatePushToken delegates to repository when device exists`() {
        val device = DeviceRegistration().apply { uid = "DEV-UID" }
        whenever(deviceRepository.findByWorkspaceIdAndDeviceId("WS-1", "DEV-1")).thenReturn(device)

        service.updatePushToken("WS-1", "DEV-1", "tok", "FCM")

        verify(deviceRepository).updatePushToken(eq("DEV-UID"), eq("tok"), eq("FCM"), any())
    }
}
