package com.ampairs.notification.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream

/**
 * Initializes the Firebase Admin SDK for push (FCM) delivery.
 *
 * Only active when `notification.push.enabled=true` AND credentials are supplied, so local/test
 * environments without a service account simply have no [FirebaseMessaging] bean — [com.ampairs.notification.provider.push.FcmPushProvider]
 * then reports itself unavailable and push rows fail gracefully (and retry).
 */
@Configuration
@ConditionalOnProperty(prefix = "notification.push", name = ["enabled"], havingValue = "true")
class FirebaseConfig(
    private val props: NotificationProperties,
) {
    private val logger = LoggerFactory.getLogger(FirebaseConfig::class.java)

    @Bean
    fun firebaseApp(): FirebaseApp {
        // Reuse the default app if it was already initialized (e.g. by another module/test).
        FirebaseApp.getApps().firstOrNull { it.name == FirebaseApp.DEFAULT_APP_NAME }?.let {
            logger.info("Firebase app already initialized; reusing existing instance")
            return it
        }

        val credentialsStream = openCredentialsStream()
            ?: error(
                "notification.push.enabled=true but no credentials provided. " +
                    "Set notification.push.credentialsJson or notification.push.credentialsPath."
            )

        val options = credentialsStream.use { stream ->
            FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(stream))
                .build()
        }

        logger.info("Initializing Firebase Admin SDK for push notifications")
        return FirebaseApp.initializeApp(options)
    }

    @Bean
    fun firebaseMessaging(firebaseApp: FirebaseApp): FirebaseMessaging =
        FirebaseMessaging.getInstance(firebaseApp)

    private fun openCredentialsStream(): InputStream? = when {
        props.push.credentialsJson.isNotBlank() ->
            ByteArrayInputStream(props.push.credentialsJson.toByteArray(Charsets.UTF_8))

        props.push.credentialsPath.isNotBlank() ->
            FileInputStream(props.push.credentialsPath)

        else -> null
    }
}
