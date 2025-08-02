package com.ampairs.notification.provider.sms

import com.ampairs.notification.provider.NotificationResult
import io.awspring.cloud.sns.sms.SnsSmsTemplate
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * AWS SNS SMS Provider implementation
 */
@Component
class AwsSnsSmsProvider @Autowired constructor(
    private val snsSmsTemplate: SnsSmsTemplate,
) : SmsNotificationProvider {

    private val logger = LoggerFactory.getLogger(AwsSnsSmsProvider::class.java)

    @Value("\${aws.sns.enabled:true}")
    private var enabled: Boolean = true

    override fun sendSms(phoneNumber: String, message: String): NotificationResult {
        if (!isAvailable()) {
            return NotificationResult(
                success = false,
                errorMessage = "AWS SNS provider is not available",
                providerName = getProviderName(),
                channel = getChannel()
            )
        }

        return try {
            logger.debug("Sending AWS SNS SMS to: {}", phoneNumber)

            // SNS SMS template send method
            snsSmsTemplate.send(phoneNumber, message)

            logger.info("AWS SNS SMS sent successfully to: {}", phoneNumber)
            NotificationResult(
                success = true,
                messageId = "sns-${System.currentTimeMillis()}", // SNS doesn't return message ID directly
                providerResponse = "SMS sent via AWS SNS",
                providerName = getProviderName(),
                channel = getChannel()
            )
        } catch (e: Exception) {
            logger.error("AWS SNS SMS failed for phone: {}", phoneNumber, e)
            NotificationResult(
                success = false,
                errorMessage = "AWS SNS error: ${e.message}",
                providerName = getProviderName(),
                channel = getChannel()
            )
        }
    }

    override fun getProviderName(): String = "AWS_SNS"

    override fun isAvailable(): Boolean = enabled
}