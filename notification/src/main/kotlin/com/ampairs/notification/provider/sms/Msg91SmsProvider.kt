package com.ampairs.notification.provider.sms

import com.ampairs.notification.provider.NotificationResult
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.*
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

/**
 * MSG91 SMS Provider implementation
 */
@Component
class Msg91SmsProvider : SmsNotificationProvider {

    private val logger = LoggerFactory.getLogger(Msg91SmsProvider::class.java)

    @Value("\${notification.sms.msg91.auth-key}")
    private lateinit var authKey: String

    @Value("\${notification.sms.msg91.template-id}")
    private lateinit var templateId: String

    @Value("\${notification.sms.msg91.sender-id:AMPAIR}")
    private lateinit var senderId: String

    @Value("\${notification.sms.msg91.api-url:https://control.msg91.com/api/v5/otp}")
    private lateinit var apiUrl: String

    @Value("\${notification.sms.msg91.enabled:true}")
    private var enabled: Boolean = true

    private val restTemplate = RestTemplate()

    override fun sendSms(phoneNumber: String, message: String): NotificationResult {
        if (!isAvailable()) {
            return NotificationResult(
                success = false,
                errorMessage = "MSG91 provider is not available",
                providerName = getProviderName(),
                channel = getChannel()
            )
        }

        return try {
            val response = callMsg91Api(phoneNumber, message)
            handleMsg91Response(response)
        } catch (e: RestClientException) {
            logger.error("MSG91 API call failed for phone: {}", phoneNumber, e)
            NotificationResult(
                success = false,
                errorMessage = "Network error: ${e.message}",
                providerName = getProviderName(),
                channel = getChannel()
            )
        } catch (e: Exception) {
            logger.error("Unexpected error in MSG91 provider for phone: {}", phoneNumber, e)
            NotificationResult(
                success = false,
                errorMessage = "Internal error: ${e.message}",
                providerName = getProviderName(),
                channel = getChannel()
            )
        }
    }

    private fun callMsg91Api(phoneNumber: String, message: String): ResponseEntity<Msg91Response> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON
        headers.set("authkey", authKey)

        // Extract OTP from message (assuming format: "123456 is one time password...")
        val otp = message.split(" ")[0]

        val requestBody = Msg91Request(
            template_id = templateId,
            short_url = "0",
            recipients = listOf(
                Msg91Recipient(
                    mobiles = phoneNumber.removePrefix("+"),
                    var1 = otp
                )
            )
        )

        val requestEntity = HttpEntity(requestBody, headers)

        logger.debug("Sending MSG91 SMS to: {} with template: {}", phoneNumber, templateId)
        return restTemplate.postForEntity(apiUrl, requestEntity, Msg91Response::class.java)
    }

    private fun handleMsg91Response(response: ResponseEntity<Msg91Response>): NotificationResult {
        val msg91Response = response.body

        if (response.statusCode == HttpStatus.OK && msg91Response != null) {
            return if (msg91Response.type == "success") {
                logger.info("MSG91 SMS sent successfully: {}", msg91Response.message)
                NotificationResult(
                    success = true,
                    messageId = msg91Response.request_id,
                    providerResponse = msg91Response.message,
                    providerName = getProviderName(),
                    channel = getChannel()
                )
            } else {
                logger.warn("MSG91 SMS failed: {}", msg91Response.message)
                NotificationResult(
                    success = false,
                    errorMessage = msg91Response.message,
                    providerResponse = msg91Response.message,
                    providerName = getProviderName(),
                    channel = getChannel()
                )
            }
        } else {
            logger.error("MSG91 API returned invalid response: {}", response.statusCode)
            return NotificationResult(
                success = false,
                errorMessage = "Invalid response from MSG91: ${response.statusCode}",
                providerName = getProviderName(),
                channel = getChannel()
            )
        }
    }

    override fun getProviderName(): String = "MSG91"

    override fun isAvailable(): Boolean = enabled && authKey.isNotEmpty() && templateId.isNotEmpty()
}

/**
 * MSG91 API Request model
 */
data class Msg91Request(
    val template_id: String,
    val short_url: String = "0",
    val recipients: List<Msg91Recipient>,
)

data class Msg91Recipient(
    val mobiles: String,
    val var1: String, // OTP variable
)

/**
 * MSG91 API Response model
 */
data class Msg91Response(
    val type: String,
    val message: String,
    @JsonProperty("request_id")
    val request_id: String?,
)