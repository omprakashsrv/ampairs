package com.ampairs.notification.provider.sms

import com.ampairs.notification.config.NotificationProperties
import com.ampairs.notification.config.NotificationProperties.Msg91Properties
import com.ampairs.notification.config.NotificationProperties.SmsProperties
import com.ampairs.notification.provider.NotificationChannel
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class Msg91SmsProviderTest {

    private fun provider(msg91: Msg91Properties): Msg91SmsProvider =
        Msg91SmsProvider(NotificationProperties(sms = SmsProperties(msg91 = msg91)))

    @Test
    fun `provider metadata is correct`() {
        val p = provider(Msg91Properties(authKey = "k", templateId = "t", enabled = true))
        assertEquals("MSG91", p.getProviderName())
        assertEquals(NotificationChannel.SMS, p.getChannel())
    }

    @Test
    fun `isAvailable requires enabled authKey and templateId`() {
        assertTrue(provider(Msg91Properties(authKey = "k", templateId = "t", enabled = true)).isAvailable())
        assertFalse(provider(Msg91Properties(authKey = "k", templateId = "t", enabled = false)).isAvailable())
        assertFalse(provider(Msg91Properties(authKey = "", templateId = "t", enabled = true)).isAvailable())
        assertFalse(provider(Msg91Properties(authKey = "k", templateId = "", enabled = true)).isAvailable())
    }

    @Test
    fun `sendSms short-circuits with failure when provider unavailable`() {
        val p = provider(Msg91Properties(authKey = "", templateId = "", enabled = false))

        val result = p.sendSms("+919999999999", "123456 is your code")

        assertFalse(result.success)
        assertEquals("MSG91", result.providerName)
        assertEquals(NotificationChannel.SMS, result.channel)
        assertTrue(result.errorMessage?.contains("not available") == true)
    }

    @Test
    fun `sendNotification short-circuits when unavailable`() {
        val p = provider(Msg91Properties(enabled = false))

        assertFalse(p.sendNotification("+919999999999", "hi").success)
    }

    @Test
    fun `msg91 request response and recipient data classes`() {
        val recipient = Msg91Recipient(mobiles = "919999999999", var1 = "123456")
        val request = Msg91Request(template_id = "t", recipients = listOf(recipient))
        assertEquals("0", request.short_url)
        assertEquals("123456", request.recipients.first().var1)
        assertEquals(request, request.copy())

        val response = Msg91Response(type = "success", message = "ok", requestId = "RID-1")
        assertEquals("success", response.type)
        assertEquals("RID-1", response.requestId)
        assertEquals(response, response.copy())
    }
}
