package com.ampairs.notification.provider.email

import com.ampairs.notification.config.NotificationProperties
import com.ampairs.notification.provider.NotificationChannel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EmailNotificationProviderTest {

    private fun provider(email: NotificationProperties.EmailProperties) =
        EmailNotificationProvider(NotificationProperties(email = email))

    @Test
    fun `reports the EMAIL channel and a stable provider name`() {
        val p = provider(NotificationProperties.EmailProperties())
        assertEquals(NotificationChannel.EMAIL, p.getChannel())
        assertEquals("EMAIL_SMTP", p.getProviderName())
    }

    @Test
    fun `is unavailable when disabled or host is blank`() {
        assertFalse(provider(NotificationProperties.EmailProperties(enabled = false, host = "smtp.test")).isAvailable())
        assertFalse(provider(NotificationProperties.EmailProperties(enabled = true, host = "")).isAvailable())
    }

    @Test
    fun `is available when enabled with a host`() {
        assertTrue(provider(NotificationProperties.EmailProperties(enabled = true, host = "smtp.test")).isAvailable())
    }

    @Test
    fun `returns a failure result (not an exception) when the transport is unreachable`() {
        val p = provider(NotificationProperties.EmailProperties(enabled = true, host = "localhost", port = 1))
        val result = p.sendNotification("user@example.com", "<p>hi</p>", "Subject", emptyMap())
        assertFalse(result.success)
        assertEquals(NotificationChannel.EMAIL, result.channel)
    }
}
