package com.ampairs.notification.service

import com.ampairs.core.multitenancy.TenantContextHolder
import com.ampairs.notification.config.NotificationProperties
import com.ampairs.notification.config.NotificationProperties.SmsProperties
import com.ampairs.notification.model.NotificationQueue
import com.ampairs.notification.port.DevicePushTokenPort
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationResult
import com.ampairs.notification.provider.NotificationStatus
import com.ampairs.notification.provider.push.FcmPushProvider
import com.ampairs.notification.provider.sms.AwsSnsSmsProvider
import com.ampairs.notification.provider.sms.Msg91SmsProvider
import com.ampairs.notification.repository.NotificationQueueRepository
import org.springframework.beans.factory.ObjectProvider
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
import java.util.concurrent.Executor

/**
 * Focuses on the synchronous queue / immediate-send branches of NotificationService that the
 * existing NotificationServiceTest does not exercise (tenant fallback, provider fallback order).
 */
class NotificationServiceQueueTest {

    private val repository: NotificationQueueRepository = mock()
    private val msg91: Msg91SmsProvider = mock()
    private val awsSns: AwsSnsSmsProvider = mock()
    private val fcmPush: FcmPushProvider = mock()
    private val executor: Executor = Executor { it.run() }
    private val databaseService: NotificationDatabaseService = mock()
    private val devicePushTokenPort: ObjectProvider<DevicePushTokenPort> = mock()

    private fun service(primary: String = "MSG91"): NotificationService =
        NotificationService(
            repository, msg91, awsSns, fcmPush, executor, databaseService,
            NotificationProperties(sms = SmsProperties(primaryProvider = primary)),
            devicePushTokenPort,
            mock(),
            mock(),
            mock(),
            mock(),
        )

    @BeforeEach
    fun setUp() {
        whenever(repository.save(any<NotificationQueue>())).thenAnswer {
            (it.arguments[0] as NotificationQueue).apply { uid = "NQ-SAVED" }
        }
    }

    @AfterEach
    fun tearDown() = TenantContextHolder.clearTenantContext()

    @Test
    fun `queueNotification persists a PENDING row and returns its uid`() {
        val uid = service().queueNotification("+919999999999", "hello", NotificationChannel.SMS)

        assertEquals("NQ-SAVED", uid)
        val captor = argumentCaptor<NotificationQueue>()
        verify(repository).save(captor.capture())
        assertEquals("+919999999999", captor.firstValue.recipient)
        assertEquals(NotificationStatus.PENDING, captor.firstValue.status)
    }

    @Test
    fun `queueNotificationWithTenant uses default tenant when no context`() {
        TenantContextHolder.clearTenantContext()

        service().queueNotificationWithTenant("+91", "msg", NotificationChannel.SMS, null)

        val captor = argumentCaptor<NotificationQueue>()
        verify(repository).save(captor.capture())
        assertEquals("default", captor.firstValue.ownerId)
    }

    @Test
    fun `queueNotificationWithTenant honours explicit tenant id`() {
        service().queueNotificationWithTenant("+91", "msg", NotificationChannel.SMS, "ws-99")

        val captor = argumentCaptor<NotificationQueue>()
        verify(repository).save(captor.capture())
        assertEquals("ws-99", captor.firstValue.ownerId)
    }

    @Test
    fun `sendImmediateNotification returns first successful provider result`() {
        whenever(msg91.isAvailable()).thenReturn(true)
        whenever(msg91.sendNotification(any(), any())).thenReturn(
            NotificationResult(success = true, providerName = "MSG91", channel = NotificationChannel.SMS)
        )

        val result = service().sendImmediateNotification("+91", "hi", NotificationChannel.SMS)

        assertTrue(result.success)
        assertEquals("MSG91", result.providerName)
        verify(awsSns, never()).sendNotification(any(), any())
    }

    @Test
    fun `sendImmediateNotification falls back to secondary provider when primary fails`() {
        whenever(msg91.isAvailable()).thenReturn(true)
        whenever(msg91.sendNotification(any(), any())).thenReturn(
            NotificationResult(success = false, providerName = "MSG91", channel = NotificationChannel.SMS)
        )
        whenever(awsSns.isAvailable()).thenReturn(true)
        whenever(awsSns.sendNotification(any(), any())).thenReturn(
            NotificationResult(success = true, providerName = "AWS_SNS", channel = NotificationChannel.SMS)
        )

        val result = service().sendImmediateNotification("+91", "hi", NotificationChannel.SMS)

        assertTrue(result.success)
        assertEquals("AWS_SNS", result.providerName)
    }

    @Test
    fun `sendImmediateNotification reports failure when all providers fail`() {
        whenever(msg91.isAvailable()).thenReturn(false)
        whenever(awsSns.isAvailable()).thenReturn(false)

        val result = service().sendImmediateNotification("+91", "hi", NotificationChannel.SMS)

        assertFalse(result.success)
        assertEquals("ALL_PROVIDERS", result.providerName)
    }

    @Test
    fun `sendImmediateNotification returns failure for unsupported channel`() {
        val result = service().sendImmediateNotification("a@b.com", "hi", NotificationChannel.EMAIL)

        assertFalse(result.success)
    }

    @Test
    fun `AWS_SNS primary provider is tried first`() {
        whenever(awsSns.isAvailable()).thenReturn(true)
        whenever(awsSns.sendNotification(any(), any())).thenReturn(
            NotificationResult(success = true, providerName = "AWS_SNS", channel = NotificationChannel.SMS)
        )

        val result = service(primary = "AWS_SNS").sendImmediateNotification("+91", "hi")

        assertEquals("AWS_SNS", result.providerName)
        verify(msg91, never()).sendNotification(any(), any())
    }
}
