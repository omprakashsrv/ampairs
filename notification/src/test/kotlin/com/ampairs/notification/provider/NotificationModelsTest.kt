package com.ampairs.notification.provider

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class NotificationModelsTest {

    @Test
    fun `notification request applies defaults`() {
        val req = NotificationRequest(
            id = "NQ-1",
            recipient = "+919999999999",
            message = "hi",
            channel = NotificationChannel.SMS,
        )

        assertEquals("NQ-1", req.id)
        assertEquals(0, req.retryCount)
        assertEquals(3, req.maxRetries)
        assertEquals(NotificationStatus.PENDING, req.status)
        assertNull(req.lastAttemptAt)
        assertTrue(req.createdAt is Instant)
    }

    @Test
    fun `notification request copy and equality`() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val req = NotificationRequest(
            id = "NQ-1",
            recipient = "+91",
            message = "hi",
            channel = NotificationChannel.EMAIL,
            createdAt = now,
            scheduledAt = now,
        )

        assertEquals(req, req.copy())
        val retried = req.copy(retryCount = 1, status = NotificationStatus.RETRYING)
        assertNotEquals(req, retried)
        assertEquals(1, retried.retryCount)
        assertEquals(NotificationStatus.RETRYING, retried.status)
    }

    @Test
    fun `notification result captures success metadata`() {
        val result = NotificationResult(
            success = true,
            messageId = "MID",
            providerResponse = "OK",
            providerName = "MSG91",
            channel = NotificationChannel.SMS,
        )

        assertTrue(result.success)
        assertEquals("MID", result.messageId)
        assertEquals(result, result.copy())
    }

    @Test
    fun `enums expose all variants`() {
        assertEquals(6, NotificationChannel.entries.size)
        assertEquals(5, NotificationStatus.entries.size)
        assertTrue(NotificationChannel.entries.contains(NotificationChannel.WHATSAPP))
        assertTrue(NotificationStatus.entries.contains(NotificationStatus.EXHAUSTED))
    }
}
