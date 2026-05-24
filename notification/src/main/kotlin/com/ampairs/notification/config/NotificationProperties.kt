package com.ampairs.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification")
data class NotificationProperties(
    val sms: SmsProperties = SmsProperties(),
    val batchSize: Int = 10,
    val retryDelayMinutes: Long = 5,
    val cleanupDays: Long = 30
) {
    data class SmsProperties(
        val primaryProvider: String = "MSG91",
        val senderName: String = "AMPAIR",
        val appName: String = "Ampairs",
        val supportUrl: String = "https://ampairs.com/support",
        val msg91: Msg91Properties = Msg91Properties()
    )

    data class Msg91Properties(
        val authKey: String = "",
        val templateId: String = "",
        val senderId: String = "AMPAIR",
        val apiUrl: String = "https://control.msg91.com/api/v5/otp",
        val enabled: Boolean = true
    )
}
