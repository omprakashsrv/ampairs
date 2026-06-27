package com.ampairs.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification")
data class NotificationProperties(
    val sms: SmsProperties = SmsProperties(),
    val push: PushProperties = PushProperties(),
    val email: EmailProperties = EmailProperties(),
    val whatsapp: WhatsAppProperties = WhatsAppProperties(),
    val batchSize: Int = 10,
    val retryDelayMinutes: Long = 5,
    val cleanupDays: Long = 30
) {
    /**
     * WhatsApp Cloud API non-secret defaults. The sender (phone_number_id) + access token are NOT
     * here — WhatsApp is client-owned-only, so they come from the per-workspace credential.
     */
    data class WhatsAppProperties(
        val apiVersion: String = "v20.0",
        val defaultLanguage: String = "en_US",
    )

    /**
     * Platform/shared email transport (SMTP). Used when a workspace has no own email credential and
     * the channel permits platform fallback. Supply host/credentials via the environment.
     */
    data class EmailProperties(
        val enabled: Boolean = false,
        val host: String = "",
        val port: Int = 587,
        val username: String = "",
        val password: String = "",
        val from: String = "no-reply@ampairs.com",
        val starttls: Boolean = true,
    )

    /**
     * Firebase Cloud Messaging (push) configuration.
     *
     * Provide credentials via EITHER an inline service-account JSON (`credentialsJson`) OR a path to
     * the JSON file (`credentialsPath`). Never commit the JSON — supply it through an environment
     * variable (e.g. `FCM_SERVICE_ACCOUNT_JSON`) in production.
     */
    data class PushProperties(
        val enabled: Boolean = false,
        val credentialsJson: String = "",
        val credentialsPath: String = "",
    )

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
