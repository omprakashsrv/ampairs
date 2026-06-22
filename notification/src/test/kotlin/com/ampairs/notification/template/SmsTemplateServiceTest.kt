package com.ampairs.notification.template

import com.ampairs.notification.config.NotificationProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SmsTemplateServiceTest {

    private val appName = "TestApp"
    private val service = SmsTemplateService(
        NotificationProperties(sms = NotificationProperties.SmsProperties(appName = appName))
    )

    private val otp = "123456"

    @Test
    fun `login message contains otp app name and validity`() {
        val msg = service.getLoginOtpMessage(otp, 8)
        assertTrue(msg.contains(otp))
        assertTrue(msg.contains(appName))
        assertTrue(msg.contains("8 minutes"))
    }

    @Test
    fun `transaction message includes amount when present`() {
        val msg = service.getTransactionOtpMessage(otp, amount = "999", validityMinutes = 5)
        assertTrue(msg.contains("₹999"))
        assertTrue(msg.contains(otp))
    }

    @Test
    fun `transaction message omits amount when null`() {
        val msg = service.getTransactionOtpMessage(otp, amount = null)
        assertFalse(msg.contains("₹"))
    }

    @Test
    fun `business verification message contains business name`() {
        val msg = service.getBusinessVerificationOtpMessage(otp, "Acme", 15)
        assertTrue(msg.contains("Acme"))
    }

    @Test
    fun `custom message contains action`() {
        val msg = service.getCustomOtpMessage(otp, action = "delete", validityMinutes = 10)
        assertTrue(msg.contains("delete"))
    }

    @Test
    fun `hindi and gujarati messages contain otp`() {
        assertTrue(service.getOtpMessageInHindi(otp).contains(otp))
        assertTrue(service.getOtpMessageInGujarati(otp).contains(otp))
    }

    @Test
    fun `template-based message returns otp verbatim`() {
        assertEquals(otp, service.getTemplateBasedOtpMessage(otp))
    }

    @Test
    fun `getOtpMessage dispatches across all scenarios`() {
        OtpScenario.entries.forEach { scenario ->
            val data = when (scenario) {
                OtpScenario.TRANSACTION_VERIFICATION -> mapOf("amount" to "100")
                OtpScenario.BUSINESS_VERIFICATION -> mapOf("businessName" to "Biz")
                OtpScenario.CUSTOM -> mapOf("action" to "verify")
                else -> emptyMap()
            }
            val msg = service.getOtpMessage(scenario, otp, data)
            assertTrue(msg.contains(otp), "scenario $scenario should embed the otp")
        }
    }

    @Test
    fun `getOtpMessage honours explicit validityMinutes from additional data`() {
        val msg = service.getOtpMessage(OtpScenario.LOGIN, otp, mapOf("validityMinutes" to "3"))
        assertTrue(msg.contains("3 minutes"))
    }

    @Test
    fun `getOtpMessage applies scenario-specific default validity`() {
        // TWO_FACTOR_AUTH defaults to 5 minutes
        assertTrue(service.getOtpMessage(OtpScenario.TWO_FACTOR_AUTH, otp).contains("5 minutes"))
        // PASSWORD_RESET defaults to 15 minutes
        assertTrue(service.getOtpMessage(OtpScenario.PASSWORD_RESET, otp).contains("15 minutes"))
        // GENERIC defaults to 10 minutes
        assertTrue(service.getOtpMessage(OtpScenario.GENERIC, otp).contains("10 minutes"))
    }

    @Test
    fun `getOtpMessage falls back to default business name when missing`() {
        val msg = service.getOtpMessage(OtpScenario.BUSINESS_VERIFICATION, otp)
        assertTrue(msg.contains("Your Business"))
    }

    @Test
    fun `isValidOtpFormat accepts six digits and rejects others`() {
        assertTrue(service.isValidOtpFormat("123456"))
        assertFalse(service.isValidOtpFormat("12345"))
        assertFalse(service.isValidOtpFormat("1234567"))
        assertFalse(service.isValidOtpFormat("12a456"))
    }

    @Test
    fun `generateSecureOtp produces a valid six-digit code`() {
        repeat(20) {
            val generated = service.generateSecureOtp()
            assertTrue(service.isValidOtpFormat(generated), "generated $generated is not 6 digits")
        }
    }
}
