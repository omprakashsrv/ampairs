package com.ampairs.auth.service

import com.ampairs.core.config.ApplicationProperties
import com.ampairs.core.service.SecurityAuditService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

/**
 * Scheduled service for automatic JWT key rotation
 */
@Service
class KeyRotationScheduler(
    private val rsaKeyManager: RsaKeyManager,
    private val applicationProperties: ApplicationProperties,
    private val securityAuditService: SecurityAuditService,
) {

    private val logger = LoggerFactory.getLogger(KeyRotationScheduler::class.java)

    /**
     * Check for key rotation needs every hour
     */
    @Scheduled(cron = "0 0 * * * ?") // Every hour
    fun checkKeyRotation() {
        if (!applicationProperties.security.jwt.keyRotation.enabled) {
            return
        }

        try {
            if (rsaKeyManager.isKeyRotationNeeded()) {
                logger.info("Key rotation needed, initiating automatic rotation")
                performKeyRotation()
            }
        } catch (e: Exception) {
            logger.error("Error during scheduled key rotation check", e)
        }
    }

    /**
     * Scheduled key rotation based on cron configuration
     */
    @Scheduled(cron = "\${application.security.jwt.key-rotation.rotation-cron:0 0 2 1 * ?}")
    fun scheduledKeyRotation() {
        val keyRotationConfig = applicationProperties.security.jwt.keyRotation

        if (!keyRotationConfig.enabled || !keyRotationConfig.scheduledRotation) {
            return
        }

        logger.info("Performing scheduled key rotation")
        performKeyRotation()
    }

    /**
     * Perform key rotation with audit logging
     */
    private fun performKeyRotation() {
        try {
            val oldKeyId = rsaKeyManager.getCurrentKeyPair().keyId
            val newKeyPair = rsaKeyManager.rotateKeys()

            logger.info(
                "Key rotation completed successfully. Old key: {}, New key: {}",
                oldKeyId, newKeyPair.keyId
            )

            // Log the key rotation event
            securityAuditService.logSuspiciousActivity(
                activityType = "JWT_KEY_ROTATION",
                description = "JWT signing key was rotated",
                riskLevel = SecurityAuditService.RiskLevel.LOW,
                request = null, // System-initiated
                additionalDetails = mapOf(
                    "old_key_id" to oldKeyId,
                    "new_key_id" to newKeyPair.keyId,
                    "rotation_type" to "scheduled",
                    "algorithm" to newKeyPair.algorithm
                )
            )

        } catch (e: Exception) {
            logger.error("Key rotation failed", e)

            // Log the failure
            securityAuditService.logSuspiciousActivity(
                activityType = "JWT_KEY_ROTATION_FAILED",
                description = "JWT signing key rotation failed",
                riskLevel = SecurityAuditService.RiskLevel.HIGH,
                request = null,
                additionalDetails = mapOf(
                    "error_message" to (e.message ?: "Unknown error"),
                    "rotation_type" to "scheduled"
                )
            )

            throw e
        }
    }

    /**
     * Manual key rotation (for administrative use)
     */
    fun manualKeyRotation(reason: String): RsaKeyManager.RSAKeyPair {
        logger.info("Manual key rotation requested. Reason: {}", reason)

        val oldKeyId = rsaKeyManager.getCurrentKeyPair().keyId
        val newKeyPair = rsaKeyManager.rotateKeys()

        // Log the manual rotation
        securityAuditService.logSuspiciousActivity(
            activityType = "JWT_KEY_ROTATION",
            description = "JWT signing key was manually rotated",
            riskLevel = SecurityAuditService.RiskLevel.MEDIUM,
            request = null,
            additionalDetails = mapOf(
                "old_key_id" to oldKeyId,
                "new_key_id" to newKeyPair.keyId,
                "rotation_type" to "manual",
                "reason" to reason,
                "algorithm" to newKeyPair.algorithm
            )
        )

        logger.info(
            "Manual key rotation completed. Old key: {}, New key: {}",
            oldKeyId, newKeyPair.keyId
        )

        return newKeyPair
    }
}