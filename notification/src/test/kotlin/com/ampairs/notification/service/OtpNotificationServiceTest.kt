package com.ampairs.notification.service

import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationResult
import com.ampairs.notification.template.OtpScenario
import com.ampairs.notification.template.SmsTemplateService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class OtpNotificationServiceTest {

    private val notificationService: NotificationService = mock()
    private val smsTemplateService: SmsTemplateService = mock()

    private lateinit var service: OtpNotificationService

    private val phone = "+919999999999"

    @BeforeEach
    fun setUp() {
        service = OtpNotificationService(notificationService, smsTemplateService)
        whenever(smsTemplateService.getOtpMessage(any(), any(), any())).thenReturn("Your code is 123456")
        whenever(notificationService.queueNotification(any(), any(), any())).thenReturn("NQ-123")
    }

    @Test
    fun `sendLoginOtp uses provided otp and queues an SMS`() {
        val result = service.sendLoginOtp(phone, otp = "111111", validityMinutes = 7)

        assertEquals("NQ-123", result)
        verify(smsTemplateService).getOtpMessage(
            eq(OtpScenario.LOGIN),
            eq("111111"),
            eq(mapOf("validityMinutes" to "7")),
        )
        verify(notificationService).queueNotification(eq(phone), any(), eq(NotificationChannel.SMS))
        // generator not used when otp supplied
        verify(smsTemplateService, never()).generateSecureOtp()
    }

    @Test
    fun `sendLoginOtp generates an otp when none supplied`() {
        whenever(smsTemplateService.generateSecureOtp()).thenReturn("654321")

        service.sendLoginOtp(phone)

        verify(smsTemplateService).generateSecureOtp()
        verify(smsTemplateService).getOtpMessage(eq(OtpScenario.LOGIN), eq("654321"), any())
    }

    @Test
    fun `sendRegistrationOtp uses REGISTRATION scenario`() {
        service.sendRegistrationOtp(phone, otp = "222222")

        verify(smsTemplateService).getOtpMessage(eq(OtpScenario.REGISTRATION), eq("222222"), any())
    }

    @Test
    fun `sendPasswordResetOtp uses PASSWORD_RESET with 15-min default validity`() {
        service.sendPasswordResetOtp(phone, otp = "333333")

        verify(smsTemplateService).getOtpMessage(
            eq(OtpScenario.PASSWORD_RESET),
            eq("333333"),
            eq(mapOf("validityMinutes" to "15")),
        )
    }

    @Test
    fun `sendPhoneVerificationOtp uses PHONE_VERIFICATION scenario`() {
        service.sendPhoneVerificationOtp(phone, otp = "444444")

        verify(smsTemplateService).getOtpMessage(eq(OtpScenario.PHONE_VERIFICATION), eq("444444"), any())
    }

    @Test
    fun `sendTwoFactorAuthOtp uses TWO_FACTOR_AUTH with 5-min default`() {
        service.sendTwoFactorAuthOtp(phone, otp = "555555")

        verify(smsTemplateService).getOtpMessage(
            eq(OtpScenario.TWO_FACTOR_AUTH),
            eq("555555"),
            eq(mapOf("validityMinutes" to "5")),
        )
    }

    @Test
    fun `sendTransactionOtp includes amount when provided`() {
        service.sendTransactionOtp(phone, amount = "1500", otp = "666666")

        verify(smsTemplateService).getOtpMessage(
            eq(OtpScenario.TRANSACTION_VERIFICATION),
            eq("666666"),
            eq(mapOf("validityMinutes" to "5", "amount" to "1500")),
        )
    }

    @Test
    fun `sendTransactionOtp omits amount when null`() {
        service.sendTransactionOtp(phone, amount = null, otp = "777777")

        verify(smsTemplateService).getOtpMessage(
            eq(OtpScenario.TRANSACTION_VERIFICATION),
            eq("777777"),
            eq(mapOf("validityMinutes" to "5")),
        )
    }

    @Test
    fun `sendBusinessVerificationOtp includes business name`() {
        service.sendBusinessVerificationOtp(phone, businessName = "Acme Co", otp = "888888")

        verify(smsTemplateService).getOtpMessage(
            eq(OtpScenario.BUSINESS_VERIFICATION),
            eq("888888"),
            eq(mapOf("validityMinutes" to "15", "businessName" to "Acme Co")),
        )
    }

    @Test
    fun `sendSecurityAlertOtp uses SECURITY_ALERT scenario`() {
        service.sendSecurityAlertOtp(phone, otp = "999999")

        verify(smsTemplateService).getOtpMessage(eq(OtpScenario.SECURITY_ALERT), eq("999999"), any())
    }

    @Test
    fun `resendOtp uses RESEND_OTP scenario`() {
        service.resendOtp(phone, otp = "101010")

        verify(smsTemplateService).getOtpMessage(eq(OtpScenario.RESEND_OTP), eq("101010"), any())
    }

    @Test
    fun `sendCustomOtp passes action through`() {
        service.sendCustomOtp(phone, action = "delete-account", otp = "121212")

        verify(smsTemplateService).getOtpMessage(
            eq(OtpScenario.CUSTOM),
            eq("121212"),
            eq(mapOf("validityMinutes" to "10", "action" to "delete-account")),
        )
    }

    @Test
    fun `sendImmediateOtp returns true when provider succeeds`() {
        whenever(notificationService.sendImmediateNotification(any(), any(), any()))
            .thenReturn(
                NotificationResult(success = true, providerName = "MSG91", channel = NotificationChannel.SMS)
            )

        val ok = service.sendImmediateOtp(phone, OtpScenario.LOGIN, otp = "131313")

        assertTrue(ok)
        verify(notificationService).sendImmediateNotification(eq(phone), any(), eq(NotificationChannel.SMS))
    }

    @Test
    fun `sendImmediateOtp returns false when provider fails`() {
        whenever(notificationService.sendImmediateNotification(any(), any(), any()))
            .thenReturn(
                NotificationResult(success = false, providerName = "MSG91", channel = NotificationChannel.SMS)
            )

        assertFalse(service.sendImmediateOtp(phone, OtpScenario.LOGIN, otp = "141414"))
    }

    @Test
    fun `generateOtp delegates to template service`() {
        whenever(smsTemplateService.generateSecureOtp()).thenReturn("246810")

        assertEquals("246810", service.generateOtp())
    }

    @Test
    fun `isValidOtp delegates to template service`() {
        whenever(smsTemplateService.isValidOtpFormat("123456")).thenReturn(true)
        whenever(smsTemplateService.isValidOtpFormat("12")).thenReturn(false)

        assertTrue(service.isValidOtp("123456"))
        assertFalse(service.isValidOtp("12"))
    }
}
