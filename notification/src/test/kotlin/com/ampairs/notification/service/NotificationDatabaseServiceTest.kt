package com.ampairs.notification.service

import com.ampairs.notification.model.NotificationQueue
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationResult
import com.ampairs.notification.provider.NotificationStatus
import com.ampairs.notification.repository.NotificationQueueRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class NotificationDatabaseServiceTest {

    private val repository: NotificationQueueRepository = mock()

    private lateinit var service: NotificationDatabaseService

    private fun notification(block: NotificationQueue.() -> Unit = {}): NotificationQueue =
        NotificationQueue().apply {
            uid = "NQ-1"
            recipient = "+919999999999"
            message = "hello"
            channel = NotificationChannel.SMS
            status = NotificationStatus.PENDING
            maxRetries = 3
            block()
        }

    @BeforeEach
    fun setUp() {
        service = NotificationDatabaseService(repository)
        whenever(repository.save(any<NotificationQueue>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `updateNotificationAsSent marks the entity SENT and persists`() {
        val n = notification()
        val result = NotificationResult(
            success = true,
            messageId = "MID-1",
            providerResponse = "OK",
            providerName = "MSG91",
            channel = NotificationChannel.SMS,
        )

        service.updateNotificationAsSent(n, result)

        assertEquals(NotificationStatus.SENT, n.status)
        assertEquals("MSG91", n.providerUsed)
        assertEquals("MID-1", n.providerMessageId)
        assertEquals("OK", n.providerResponse)
        assertNotNull(n.lastAttemptAt)
        verify(repository).save(n)
    }

    @Test
    fun `updateNotificationAsSent re-throws repository failures`() {
        val n = notification()
        whenever(repository.save(any<NotificationQueue>())).thenThrow(RuntimeException("db down"))

        assertThrows(RuntimeException::class.java) {
            service.updateNotificationAsSent(
                n,
                NotificationResult(success = true, providerName = "MSG91", channel = NotificationChannel.SMS),
            )
        }
    }

    @Test
    fun `updateNotificationAsFailed marks the entity FAILED and persists`() {
        val n = notification()

        service.updateNotificationAsFailed(n, "AWS_SNS", "timeout", "503")

        assertEquals(NotificationStatus.FAILED, n.status)
        assertEquals("AWS_SNS", n.providerUsed)
        assertEquals("timeout", n.errorMessage)
        assertEquals("503", n.providerResponse)
        verify(repository).save(n)
    }

    @Test
    fun `updateNotificationAsFailed re-throws repository failures`() {
        val n = notification()
        whenever(repository.save(any<NotificationQueue>())).thenThrow(RuntimeException("boom"))

        assertThrows(RuntimeException::class.java) {
            service.updateNotificationAsFailed(n, "AWS_SNS", "err", null)
        }
    }

    @Test
    fun `markNotificationForRetry increments retry count and sets RETRYING`() {
        val n = notification { retryCount = 0 }

        service.markNotificationForRetry(n, 5)

        assertEquals(NotificationStatus.RETRYING, n.status)
        assertEquals(1, n.retryCount)
        verify(repository).save(n)
    }

    @Test
    fun `markNotificationForRetry re-throws repository failures`() {
        val n = notification()
        whenever(repository.save(any<NotificationQueue>())).thenThrow(RuntimeException("x"))

        assertThrows(RuntimeException::class.java) {
            service.markNotificationForRetry(n, 5)
        }
    }

    @Test
    fun `markNotificationAsExhausted sets EXHAUSTED and persists`() {
        val n = notification { status = NotificationStatus.FAILED }

        service.markNotificationAsExhausted(n)

        assertEquals(NotificationStatus.EXHAUSTED, n.status)
        verify(repository).save(n)
    }

    @Test
    fun `markNotificationAsExhausted re-throws repository failures`() {
        val n = notification()
        whenever(repository.save(any<NotificationQueue>())).thenThrow(RuntimeException("y"))

        assertThrows(RuntimeException::class.java) {
            service.markNotificationAsExhausted(n)
        }
    }
}
