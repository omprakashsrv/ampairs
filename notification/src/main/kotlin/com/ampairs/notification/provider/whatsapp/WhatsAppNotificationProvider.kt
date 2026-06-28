package com.ampairs.notification.provider.whatsapp

import com.ampairs.notification.config.NotificationProperties
import com.ampairs.notification.credential.WorkspaceChannelCredentialResolver
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationProvider
import com.ampairs.notification.provider.NotificationResult
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * WhatsApp Cloud API (Meta) provider. Sends from the **workspace's own** number — the phone_number_id
 * (sender) and access token come from the resolved [WorkspaceChannelCredential], never a platform
 * default (WhatsApp is client-owned-only). An approved template (`provider_template_id` + ordered
 * `provider_params`, carried in the data payload) is used when present; otherwise a plain text body.
 *
 * Must run within the target workspace's tenant context so the resolver finds the right credential.
 */
@Component
class WhatsAppNotificationProvider(
    private val resolver: WorkspaceChannelCredentialResolver,
    private val props: NotificationProperties,
) : NotificationProvider {

    private val logger = LoggerFactory.getLogger(WhatsAppNotificationProvider::class.java)
    private val objectMapper = ObjectMapper()
    private val restClient = RestClient.create()

    override fun sendNotification(recipient: String, message: String): NotificationResult =
        sendNotification(recipient, message, null, emptyMap())

    override fun sendNotification(
        recipient: String,
        message: String,
        title: String?,
        data: Map<String, String>,
    ): NotificationResult {
        val resolved = runCatching { resolver.resolve(NotificationChannel.WHATSAPP) }.getOrNull()
        val phoneNumberId = resolved?.providerAccountRef
        val token = resolved?.secret
        if (phoneNumberId.isNullOrBlank() || token.isNullOrBlank()) {
            return failure("No WhatsApp workspace credential resolved")
        }

        val payload = buildPayload(recipient, message, data)
        return try {
            val url = "https://graph.facebook.com/${props.whatsapp.apiVersion}/$phoneNumberId/messages"
            val response = restClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(Map::class.java)
            val messageId = ((response?.get("messages") as? List<*>)?.firstOrNull() as? Map<*, *>)?.get("id") as? String
            logger.info("WhatsApp message sent from {} to {} id={}", phoneNumberId, recipient, messageId)
            NotificationResult(
                success = true,
                messageId = messageId,
                providerName = getProviderName(),
                channel = NotificationChannel.WHATSAPP,
            )
        } catch (e: Exception) {
            logger.warn("WhatsApp send to {} failed: {}", recipient, e.message)
            failure(e.message)
        }
    }

    private fun buildPayload(recipient: String, message: String, data: Map<String, String>): Map<String, Any> {
        val base = linkedMapOf<String, Any>("messaging_product" to "whatsapp", "to" to recipient)
        val templateName = data["provider_template_id"]
        if (!templateName.isNullOrBlank()) {
            val params = parseParams(data["provider_params"])
            base["type"] = "template"
            base["template"] = mapOf(
                "name" to templateName,
                "language" to mapOf("code" to props.whatsapp.defaultLanguage),
                "components" to listOf(
                    mapOf(
                        "type" to "body",
                        "parameters" to params.map { mapOf("type" to "text", "text" to it) },
                    )
                ),
            )
        } else {
            base["type"] = "text"
            base["text"] = mapOf("body" to message)
        }
        return base
    }

    private fun parseParams(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { objectMapper.readValue(json, Array<String>::class.java).toList() }.getOrDefault(emptyList())
    }

    private fun failure(error: String?) = NotificationResult(
        success = false, errorMessage = error,
        providerName = getProviderName(), channel = NotificationChannel.WHATSAPP,
    )

    override fun getProviderName(): String = "WHATSAPP_CLOUD"
    override fun getChannel(): NotificationChannel = NotificationChannel.WHATSAPP
    override fun isAvailable(): Boolean = true
}
