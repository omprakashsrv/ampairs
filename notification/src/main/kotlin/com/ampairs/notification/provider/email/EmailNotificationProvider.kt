package com.ampairs.notification.provider.email

import com.ampairs.notification.config.NotificationProperties
import com.ampairs.notification.provider.NotificationChannel
import com.ampairs.notification.provider.NotificationProvider
import com.ampairs.notification.provider.NotificationResult
import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSenderImpl
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * SMTP email provider. Sends an HTML email; `title` (passed by the dispatch path) is the subject and
 * `message` is the rendered HTML body. Uses the platform/shared SMTP transport from
 * [NotificationProperties.email]; per-workspace credential selection is layered on later (T016–T019).
 */
@Component
class EmailNotificationProvider(
    private val props: NotificationProperties,
) : NotificationProvider {

    private val logger = LoggerFactory.getLogger(EmailNotificationProvider::class.java)

    private val mailSender: JavaMailSenderImpl by lazy { buildSender() }

    private fun buildSender(): JavaMailSenderImpl = JavaMailSenderImpl().apply {
        host = props.email.host
        port = props.email.port
        username = props.email.username
        password = props.email.password
        javaMailProperties.apply {
            this["mail.smtp.auth"] = props.email.username.isNotBlank().toString()
            this["mail.smtp.starttls.enable"] = props.email.starttls.toString()
            this["mail.transport.protocol"] = "smtp"
        }
    }

    override fun sendNotification(recipient: String, message: String): NotificationResult =
        sendNotification(recipient, message, title = null, data = emptyMap())

    override fun sendNotification(
        recipient: String,
        message: String,
        title: String?,
        data: Map<String, String>,
    ): NotificationResult {
        return try {
            val mime = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(mime, true, "UTF-8")
            helper.setTo(recipient)
            helper.setFrom(props.email.from)
            helper.setSubject(title ?: "")
            helper.setText(message, true) // HTML body
            mailSender.send(mime)
            val messageId = "smtp-${UUID.randomUUID()}"
            logger.info("Email sent to {} (subject='{}') id={}", recipient, title, messageId)
            NotificationResult(
                success = true,
                messageId = messageId,
                providerName = getProviderName(),
                channel = NotificationChannel.EMAIL,
            )
        } catch (e: Exception) {
            logger.warn("Email send to {} failed: {}", recipient, e.message)
            NotificationResult(
                success = false,
                errorMessage = e.message,
                providerName = getProviderName(),
                channel = NotificationChannel.EMAIL,
            )
        }
    }

    override fun getProviderName(): String = "EMAIL_SMTP"

    override fun getChannel(): NotificationChannel = NotificationChannel.EMAIL

    override fun isAvailable(): Boolean = props.email.enabled && props.email.host.isNotBlank()
}
