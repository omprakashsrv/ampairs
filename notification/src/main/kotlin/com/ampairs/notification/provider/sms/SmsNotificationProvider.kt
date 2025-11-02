package com.ampairs.notification.provider.sms

import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationProvider
import com.ampairs.notification.provider.NotificationResult

/**
 * SMS Notification Provider interface for SMS-specific implementations
 */
interface SmsNotificationProvider : NotificationProvider {

    /**
     * Send SMS to a phone number
     *
     * @param phoneNumber The phone number with country code (e.g., +919876543210)
     * @param message The SMS message content
     * @return NotificationResult indicating success/failure
     */
    fun sendSms(phoneNumber: String, message: String): NotificationResult

    /**
     * Default implementation for NotificationProvider interface
     */
    override fun sendNotification(recipient: String, message: String): NotificationResult {
        return sendSms(recipient, message)
    }

    /**
     * SMS providers always use SMS channel
     */
    override fun getChannel(): NotificationChannel = NotificationChannel.SMS
}

/**
 * SMS-specific result for backward compatibility
 */
typealias SmsResult = NotificationResult