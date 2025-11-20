package com.ampairs.auth.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseToken
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.FileInputStream

/**
 * Service for verifying Firebase authentication tokens
 */
@Service
class FirebaseAuthService {

    private val logger = LoggerFactory.getLogger(FirebaseAuthService::class.java)

    @Value("\${firebase.service-account-key-path:}")
    private lateinit var serviceAccountKeyPath: String

    @Value("\${firebase.enabled:false}")
    private var firebaseEnabled: Boolean = false

    @PostConstruct
    fun initialize() {
        if (!firebaseEnabled) {
            logger.info("Firebase authentication is disabled. Set firebase.enabled=true to enable.")
            return
        }

        if (serviceAccountKeyPath.isBlank()) {
            logger.warn("Firebase service account key path not configured. Firebase authentication will not work.")
            return
        }

        try {
            // Check if Firebase app is already initialized
            if (FirebaseApp.getApps().isEmpty()) {
                val serviceAccount = FileInputStream(serviceAccountKeyPath)
                val options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build()

                FirebaseApp.initializeApp(options)
                logger.info("Firebase Admin SDK initialized successfully")
            } else {
                logger.info("Firebase Admin SDK already initialized")
            }
        } catch (e: Exception) {
            logger.error("Failed to initialize Firebase Admin SDK: ${e.message}", e)
        }
    }

    /**
     * Verify Firebase ID token and return the decoded token
     * @param idToken The Firebase ID token to verify
     * @return FirebaseToken if valid, null if Firebase is disabled or verification fails
     */
    fun verifyIdToken(idToken: String): FirebaseToken? {
        if (!firebaseEnabled) {
            logger.warn("Firebase authentication is disabled. Cannot verify token.")
            return null
        }

        if (FirebaseApp.getApps().isEmpty()) {
            logger.error("Firebase Admin SDK not initialized. Cannot verify token.")
            return null
        }

        // Pre-validate JWT format to provide better error messages
        if (idToken.isBlank()) {
            logger.error("Firebase ID token is blank or empty")
            return null
        }

        val parts = idToken.split(".")
        if (parts.size != 3) {
            logger.error("Firebase ID token is not a valid JWT format. Expected 3 parts separated by '.', got ${parts.size} parts. Token length: ${idToken.length}")
            return null
        }

        return try {
            val decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken)
            logger.info("Firebase token verified successfully for UID: ${decodedToken.uid}")
            decodedToken
        } catch (e: FirebaseAuthException) {
            logger.error("Firebase token verification failed: ${e.message}", e)
            null
        } catch (e: Exception) {
            logger.error("Unexpected error during Firebase token verification: ${e.message}", e)
            null
        }
    }

    /**
     * Extract phone number from Firebase token
     * @param token The verified Firebase token
     * @return Phone number without country code, or null if not available
     */
    fun extractPhoneNumber(token: FirebaseToken): String? {
        // Firebase stores phone number with country code (e.g., +919591781662)
        val phoneNumber = token.claims["phone_number"] as? String
        return phoneNumber?.removePrefix("+")
    }

    /**
     * Check if Firebase authentication is enabled
     */
    fun isEnabled(): Boolean = firebaseEnabled

    /**
     * Get Firebase UID from verified token
     */
    fun getFirebaseUid(token: FirebaseToken): String = token.uid
}
