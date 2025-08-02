package com.ampairs.notification.service

import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.template.OtpScenario
import com.ampairs.notification.template.SmsTemplateService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * High-level service for sending OTP notifications with proper templates
 * This service combines template generation with notification sending
 */
@Service
class OtpNotificationService @Autowired constructor(
    private val notificationService: NotificationService,
    private val smsTemplateService: SmsTemplateService,
) {

    /**
     * Send OTP for login verification
     */
    fun sendLoginOtp(phoneNumber: String, otp: String? = null, validityMinutes: Int = 10): String {
        val otpCode = otp ?: smsTemplateService.generateSecureOtp()
        val message = smsTemplateService.getOtpMessage(
            OtpScenario.LOGIN,
            otpCode,
            mapOf("validityMinutes" to validityMinutes.toString())
        )
        return notificationService.queueNotification(phoneNumber, message, NotificationChannel.SMS)
    }

    /**
     * Send OTP for user registration
     */
    fun sendRegistrationOtp(phoneNumber: String, otp: String? = null, validityMinutes: Int = 10): String {
        val otpCode = otp ?: smsTemplateService.generateSecureOtp()
        val message = smsTemplateService.getOtpMessage(
            OtpScenario.REGISTRATION,
            otpCode,
            mapOf("validityMinutes" to validityMinutes.toString())
        )
        return notificationService.queueNotification(phoneNumber, message, NotificationChannel.SMS)
    }

    /**
     * Send OTP for password reset
     */
    fun sendPasswordResetOtp(phoneNumber: String, otp: String? = null, validityMinutes: Int = 15): String {
        val otpCode = otp ?: smsTemplateService.generateSecureOtp()
        val message = smsTemplateService.getOtpMessage(
            OtpScenario.PASSWORD_RESET,
            otpCode,
            mapOf("validityMinutes" to validityMinutes.toString())
        )
        return notificationService.queueNotification(phoneNumber, message, NotificationChannel.SMS)
    }

    /**
     * Send OTP for phone number verification
     */
    fun sendPhoneVerificationOtp(phoneNumber: String, otp: String? = null, validityMinutes: Int = 10): String {
        val otpCode = otp ?: smsTemplateService.generateSecureOtp()
        val message = smsTemplateService.getOtpMessage(
            OtpScenario.PHONE_VERIFICATION,
            otpCode,
            mapOf("validityMinutes" to validityMinutes.toString())
        )
        return notificationService.queueNotification(phoneNumber, message, NotificationChannel.SMS)
    }

    /**
     * Send OTP for two-factor authentication
     */
    fun sendTwoFactorAuthOtp(phoneNumber: String, otp: String? = null, validityMinutes: Int = 5): String {
        val otpCode = otp ?: smsTemplateService.generateSecureOtp()
        val message = smsTemplateService.getOtpMessage(
            OtpScenario.TWO_FACTOR_AUTH,
            otpCode,
            mapOf("validityMinutes" to validityMinutes.toString())
        )
        return notificationService.queueNotification(phoneNumber, message, NotificationChannel.SMS)
    }

    /**
     * Send OTP for transaction verification
     */
    fun sendTransactionOtp(
        phoneNumber: String,
        amount: String? = null,
        otp: String? = null,
        validityMinutes: Int = 5,
    ): String {
        val otpCode = otp ?: smsTemplateService.generateSecureOtp()
        val additionalData = mutableMapOf("validityMinutes" to validityMinutes.toString())
        amount?.let { additionalData["amount"] = it }

        val message = smsTemplateService.getOtpMessage(OtpScenario.TRANSACTION_VERIFICATION, otpCode, additionalData)
        return notificationService.queueNotification(phoneNumber, message, NotificationChannel.SMS)
    }

    /**
     * Send OTP for business verification
     */
    fun sendBusinessVerificationOtp(
        phoneNumber: String,
        businessName: String,
        otp: String? = null,
        validityMinutes: Int = 15,
    ): String {
        val otpCode = otp ?: smsTemplateService.generateSecureOtp()
        val message = smsTemplateService.getOtpMessage(
            OtpScenario.BUSINESS_VERIFICATION,
            otpCode,
            mapOf(
                "validityMinutes" to validityMinutes.toString(),
                "businessName" to businessName
            )
        )
        return notificationService.queueNotification(phoneNumber, message, NotificationChannel.SMS)
    }

    /**
     * Send security alert OTP
     */
    fun sendSecurityAlertOtp(phoneNumber: String, otp: String? = null, validityMinutes: Int = 10): String {
        val otpCode = otp ?: smsTemplateService.generateSecureOtp()
        val message = smsTemplateService.getOtpMessage(
            OtpScenario.SECURITY_ALERT,
            otpCode,
            mapOf("validityMinutes" to validityMinutes.toString())
        )
        return notificationService.queueNotification(phoneNumber, message, NotificationChannel.SMS)
    }

    /**
     * Resend OTP with new code
     */
    fun resendOtp(
        phoneNumber: String,
        scenario: OtpScenario = OtpScenario.GENERIC,
        otp: String? = null,
        validityMinutes: Int = 10,
    ): String {
        val otpCode = otp ?: smsTemplateService.generateSecureOtp()
        val message = smsTemplateService.getOtpMessage(
            OtpScenario.RESEND_OTP,
            otpCode,
            mapOf("validityMinutes" to validityMinutes.toString())
        )
        return notificationService.queueNotification(phoneNumber, message, NotificationChannel.SMS)
    }

    /**
     * Send custom OTP with specific action
     */
    fun sendCustomOtp(phoneNumber: String, action: String, otp: String? = null, validityMinutes: Int = 10): String {
        val otpCode = otp ?: smsTemplateService.generateSecureOtp()
        val message = smsTemplateService.getOtpMessage(
            OtpScenario.CUSTOM,
            otpCode,
            mapOf(
                "validityMinutes" to validityMinutes.toString(),
                "action" to action
            )
        )
        return notificationService.queueNotification(phoneNumber, message, NotificationChannel.SMS)
    }

    /**
     * Send immediate OTP (bypass queue for urgent scenarios)
     */
    fun sendImmediateOtp(
        phoneNumber: String,
        scenario: OtpScenario,
        otp: String? = null,
        validityMinutes: Int = 5,
    ): Boolean {
        val otpCode = otp ?: smsTemplateService.generateSecureOtp()
        val message = smsTemplateService.getOtpMessage(
            scenario,
            otpCode,
            mapOf("validityMinutes" to validityMinutes.toString())
        )
        val result = notificationService.sendImmediateNotification(phoneNumber, message, NotificationChannel.SMS)
        return result.success
    }

    /**
     * Generate a secure 6-digit OTP
     */
    fun generateOtp(): String {
        return smsTemplateService.generateSecureOtp()
    }

    /**
     * Validate OTP format
     */
    fun isValidOtp(otp: String): Boolean {
        return smsTemplateService.isValidOtpFormat(otp)
    }
}