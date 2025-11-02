package com.ampairs.auth.controller

import com.ampairs.auth.service.KeyRotationScheduler
import com.ampairs.auth.service.RsaKeyManager
import com.ampairs.core.domain.dto.ApiResponse
import org.slf4j.LoggerFactory
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import java.util.concurrent.TimeUnit

/**
 * Controller for JWT Key Set (JWKS) and key management endpoints
 * Provides public keys for JWT token verification
 */
@RestController
@RequestMapping("/auth/v1/keys")
class JwksController(
    private val rsaKeyManager: RsaKeyManager,
    private val keyRotationScheduler: KeyRotationScheduler,
) {

    private val logger = LoggerFactory.getLogger(JwksController::class.java)

    /**
     * JWKS endpoint - Returns public keys in JSON Web Key Set format
     * This is the standard endpoint that JWT verifiers use to get public keys
     *
     * GET /auth/v1/keys/.well-known/jwks.json
     */
    @GetMapping("/.well-known/jwks.json")
    fun getJwks(): ResponseEntity<Map<String, Any>> {
        return try {
            val jwkSet = rsaKeyManager.getJwkSet()

            logger.debug(
                "Serving JWKS with {} keys",
                (jwkSet["keys"] as? List<*>)?.size ?: 0
            )

            // Cache for 1 hour to reduce load
            ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .header("Content-Type", "application/json")
                .body(jwkSet)

        } catch (e: Exception) {
            logger.error("Error serving JWKS", e)
            ResponseEntity.internalServerError()
                .body(
                    mapOf(
                        "error" to "Unable to retrieve public keys",
                        "keys" to emptyList<Map<String, Any>>()
                    )
                )
        }
    }

    /**
     * Alternative JWKS endpoint for compatibility
     */
    @GetMapping("/jwks")
    fun getJwksAlternative(): ResponseEntity<Map<String, Any>> = getJwks()

    /**
     * Get current key information (Admin only)
     *
     * GET /auth/v1/keys/current
     */
    @GetMapping("/current")
    @PreAuthorize("hasRole('ADMIN')")
    fun getCurrentKeyInfo(): ApiResponse<Map<String, Any>> {
        return try {
            val currentKeyPair = rsaKeyManager.getCurrentKeyPair()

            val keyInfo = mapOf(
                "key_id" to currentKeyPair.keyId,
                "algorithm" to currentKeyPair.algorithm,
                "created_at" to currentKeyPair.createdAt.toString(),
                "expires_at" to (currentKeyPair.expiresAt?.toString() ?: "never"),
                "is_active" to currentKeyPair.isActive,
                "is_expired" to currentKeyPair.isExpired()
            )

            ApiResponse.success(keyInfo)
        } catch (e: Exception) {
            logger.error("Error getting current key info", e)
            ApiResponse.error("INTERNAL_ERROR", "Unable to retrieve current key information")
        }
    }

    /**
     * Get key management statistics (Admin only)
     *
     * GET /auth/v1/keys/stats
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    fun getKeyStatistics(): ApiResponse<Map<String, Any>> {
        return try {
            val stats = rsaKeyManager.getKeyStatistics()
            ApiResponse.success(stats)
        } catch (e: Exception) {
            logger.error("Error getting key statistics", e)
            ApiResponse.error("INTERNAL_ERROR", "Unable to retrieve key statistics")
        }
    }

    /**
     * Manually rotate keys (Admin only)
     *
     * POST /auth/v1/keys/rotate
     */
    @PostMapping("/rotate")
    @PreAuthorize("hasRole('ADMIN')")
    fun rotateKeys(
        @RequestBody request: KeyRotationRequest,
    ): ApiResponse<Map<String, Any>> {
        return try {
            val newKeyPair = keyRotationScheduler.manualKeyRotation(
                request.reason ?: "Manual rotation via API"
            )

            val result = mapOf(
                "message" to "Key rotation completed successfully",
                "new_key_id" to newKeyPair.keyId,
                "algorithm" to newKeyPair.algorithm,
                "created_at" to newKeyPair.createdAt.toString(),
                "expires_at" to (newKeyPair.expiresAt?.toString() ?: "never")
            )

            ApiResponse.success(result)
        } catch (e: Exception) {
            logger.error("Manual key rotation failed", e)
            ApiResponse.error("KEY_ROTATION_FAILED", "Key rotation failed: ${e.message}")
        }
    }

    /**
     * Check if key rotation is needed (Admin only)
     *
     * GET /auth/v1/keys/rotation-status
     */
    @GetMapping("/rotation-status")
    @PreAuthorize("hasRole('ADMIN')")
    fun getRotationStatus(): ApiResponse<Map<String, Any>> {
        return try {
            val rotationNeeded = rsaKeyManager.isKeyRotationNeeded()
            val currentKeyPair = rsaKeyManager.getCurrentKeyPair()

            val status = mapOf(
                "rotation_needed" to rotationNeeded,
                "current_key_id" to currentKeyPair.keyId,
                "current_key_age_days" to java.time.Duration.between(
                    currentKeyPair.createdAt,
                    java.time.Instant.now()
                ).toDays(),
                "expires_at" to (currentKeyPair.expiresAt?.toString() ?: "never")
            )

            ApiResponse.success(status)
        } catch (e: Exception) {
            logger.error("Error getting rotation status", e)
            ApiResponse.error("INTERNAL_ERROR", "Unable to retrieve rotation status")
        }
    }

    /**
     * Get public key by key ID (for external verification)
     *
     * GET /auth/v1/keys/{keyId}
     */
    @GetMapping("/{keyId}")
    fun getPublicKey(@PathVariable keyId: String): ResponseEntity<Map<String, Any>> {
        return try {
            val publicKey = rsaKeyManager.getPublicKey(keyId)

            if (publicKey == null) {
                ResponseEntity.notFound().build()
            } else {
                // Create a minimal JWK for this specific key
                val jwk = mapOf(
                    "kty" to "RSA",
                    "use" to "sig",
                    "alg" to "RS256",
                    "kid" to keyId,
                    "n" to java.util.Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(publicKey.modulus.toByteArray()),
                    "e" to java.util.Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(publicKey.publicExponent.toByteArray())
                )

                ResponseEntity.ok()
                    .cacheControl(CacheControl.maxAge(6, TimeUnit.HOURS))
                    .header("Content-Type", "application/json")
                    .body(jwk)
            }
        } catch (e: Exception) {
            logger.error("Error retrieving public key for ID: {}", keyId, e)
            ResponseEntity.internalServerError()
                .body(mapOf("error" to "Unable to retrieve public key"))
        }
    }

    /**
     * Health check for key management system
     *
     * GET /auth/v1/keys/health
     */
    @GetMapping("/health")
    fun getKeySystemHealth(): ApiResponse<Map<String, Any>> {
        return try {
            val currentKeyPair = rsaKeyManager.getCurrentKeyPair()
            val stats = rsaKeyManager.getKeyStatistics()

            val health = mapOf<String, Any>(
                "status" to "healthy",
                "current_key_available" to true,
                "current_key_id" to currentKeyPair.keyId,
                "active_keys_count" to (stats["active_keys_count"] ?: 0),
                "rotation_needed" to (stats["rotation_needed"] ?: false)
            )

            ApiResponse.success(health)
        } catch (e: Exception) {
            logger.error("Key system health check failed", e)
            ApiResponse.error(
                "KEY_SYSTEM_ERROR",
                "Key management system is not healthy: ${e.message}"
            )
        }
    }

    /**
     * Request DTO for key rotation
     */
    data class KeyRotationRequest(
        val reason: String? = null,
    )
}