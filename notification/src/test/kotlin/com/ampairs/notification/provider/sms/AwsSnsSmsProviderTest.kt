package com.ampairs.notification.provider.sms

import com.ampairs.notification.provider.NotificationChannel
import io.awspring.cloud.sns.sms.SnsSmsTemplate
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AwsSnsSmsProviderTest {

    private val snsSmsTemplate: SnsSmsTemplate = mock()

    private lateinit var provider: AwsSnsSmsProvider

    @BeforeEach
    fun setUp() {
        provider = AwsSnsSmsProvider(snsSmsTemplate)
    }

    @Test
    fun `provider metadata is correct`() {
        assertEquals("AWS_SNS", provider.getProviderName())
        assertEquals(NotificationChannel.SMS, provider.getChannel())
        assertTrue(provider.isAvailable())
    }

    @Test
    fun `sendSms succeeds and reports a generated message id`() {
        val result = provider.sendSms("+919999999999", "hello")

        assertTrue(result.success)
        assertEquals("AWS_SNS", result.providerName)
        assertTrue(result.messageId?.startsWith("sns-") == true)
        verify(snsSmsTemplate).send(eq("+919999999999"), eq("hello"))
    }

    @Test
    fun `sendSms returns failure when the template throws`() {
        doThrow(RuntimeException("sns boom")).whenever(snsSmsTemplate).send(any<String>(), any<String>())

        val result = provider.sendSms("+919999999999", "hello")

        assertFalse(result.success)
        assertEquals("AWS_SNS", result.providerName)
        assertTrue(result.errorMessage?.contains("sns boom") == true)
    }

    @Test
    fun `sendNotification delegates to sendSms`() {
        val result = provider.sendNotification("+918888888888", "hi")

        assertTrue(result.success)
        verify(snsSmsTemplate).send(eq("+918888888888"), eq("hi"))
    }
}
