package com.ampairs.core.auth.service

import com.ampairs.core.auth.domain.*
import com.ampairs.core.auth.repository.ApiKeyRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Service for managing API keys.
 *
 * Handles creation, validation, revocation, and usage tracking.
 */
@Service
class ApiKeyService(
    private val apiKeyRepository: ApiKeyRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val API_KEY_PREFIX = "amp"
        private const val PREFIX_LENGTH = 8
        private const val SECRET_LENGTH = 40
        private val CHARS = ('a'..'z') + ('0'..'9')
    }

    /**
     * Create a new API key.
     *
     * @param request Key creation request
     * @param createdByUserId User ID of admin creating the key
     * @return Creation response with plain API key (shown only once!)
     */
    @Transactional
    fun createApiKey(request: CreateApiKeyRequest, createdByUserId: String?): ApiKeyCreationResponse {
        logger.info("Creating API key: ${request.name}, scope: ${request.scope}")

        // Generate random API key
        val (plainKey, keyPrefix, keyHash) = generateApiKey()

        // Calculate expiry
        val expiresAt = request.expiresInDays?.let {
            Instant.now().plus(it.toLong(), ChronoUnit.DAYS)
        }

        // Create entity
        val entity = ApiKey().apply {
            name = request.name
            description = request.description
            this.keyHash = keyHash
            this.keyPrefix = keyPrefix
            scope = request.scope
            this.expiresAt = expiresAt
            this.createdByUserId = createdByUserId
            isActive = true
        }

        val saved = apiKeyRepository.save(entity)

        logger.info("API key created: ${saved.uid}, prefix: $keyPrefix")

        return ApiKeyCreationResponse(
            uid = saved.uid,
            name = saved.name,
            apiKey = plainKey,  // ONLY TIME THIS IS RETURNED!
            keyPrefix = keyPrefix,
            scope = saved.scope.name,
            expiresAt = saved.expiresAt,
            createdAt = saved.createdAt!!
        )
    }

    /**
     * Validate API key and update usage.
     *
     * @param apiKey Plain API key from request header
     * @return API key entity if valid
     * @throws IllegalArgumentException if key is invalid
     */
    @Transactional
    fun validateAndUse(apiKey: String): ApiKey {
        // Validate format
        if (!isValidFormat(apiKey)) {
            logger.warn("Invalid API key format")
            throw IllegalArgumentException("Invalid API key format")
        }

        // Hash the key
        val keyHash = hashKey(apiKey)

        // Find by hash
        val entity = apiKeyRepository.findByKeyHash(keyHash)
            ?: throw IllegalArgumentException("Invalid API key")

        // Validate key is usable
        if (!entity.isValid()) {
            logger.warn("API key is not valid: ${entity.uid} (active=${entity.isActive}, expired=${entity.isExpired()}, revoked=${entity.revokedAt != null})")
            throw IllegalArgumentException("API key is not valid")
        }

        // Update usage
        entity.lastUsedAt = Instant.now()
        entity.usageCount++
        apiKeyRepository.save(entity)

        logger.debug("API key validated: ${entity.uid}, usage: ${entity.usageCount}")

        return entity
    }

    /**
     * List all API keys.
     */
    fun listApiKeys(): List<ApiKey> {
        return apiKeyRepository.findAllByIsActiveTrueAndRevokedAtIsNullOrderByCreatedAtDesc()
    }

    /**
     * Get API key by UID.
     */
    fun getApiKey(uid: String): ApiKey {
        return apiKeyRepository.findByUid(uid)
            ?: throw NoSuchElementException("API key not found: $uid")
    }

    /**
     * Revoke an API key.
     *
     * @param uid Key UID
     * @param reason Revocation reason
     * @param revokedBy Admin who revoked it
     */
    @Transactional
    fun revokeApiKey(uid: String, reason: String, revokedBy: String) {
        val entity = getApiKey(uid)

        if (entity.revokedAt != null) {
            logger.warn("API key already revoked: $uid")
            throw IllegalStateException("API key is already revoked")
        }

        entity.revokedAt = Instant.now()
        entity.revokedBy = revokedBy
        entity.revokedReason = reason
        entity.isActive = false

        apiKeyRepository.save(entity)

        logger.info("API key revoked: $uid, reason: $reason")
    }

    /**
     * Delete an API key permanently.
     */
    @Transactional
    fun deleteApiKey(uid: String) {
        val entity = getApiKey(uid)
        apiKeyRepository.delete(entity)
        logger.info("API key deleted: $uid")
    }

    /**
     * Generate a new API key.
     *
     * Format: amp_1a2b3c4d_5e6f7g8h9i0j1k2l3m4n5o6p7q8r9s0t
     *
     * @return Triple of (plainKey, keyPrefix, keyHash)
     */
    private fun generateApiKey(): Triple<String, String, String> {
        val random = SecureRandom()

        // Generate prefix (8 random chars)
        val prefix = (1..PREFIX_LENGTH)
            .map { CHARS[random.nextInt(CHARS.size)] }
            .joinToString("")

        // Generate secret (40 random chars)
        val secret = (1..SECRET_LENGTH)
            .map { CHARS[random.nextInt(CHARS.size)] }
            .joinToString("")

        // Combine: amp_prefix_secret
        val plainKey = "${API_KEY_PREFIX}_${prefix}_${secret}"
        val keyPrefix = "${API_KEY_PREFIX}_${prefix}"
        val keyHash = hashKey(plainKey)

        return Triple(plainKey, keyPrefix, keyHash)
    }

    /**
     * Hash an API key using SHA-256.
     */
    private fun hashKey(apiKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(apiKey.toByteArray())
        return hash.joinToString("") { "%02x".format(it) }
    }

    /**
     * Validate API key format.
     */
    private fun isValidFormat(apiKey: String): Boolean {
        // Format: amp_xxxxxxxx_yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy
        val regex = Regex("^amp_[a-z0-9]{8}_[a-z0-9]{40}$")
        return regex.matches(apiKey)
    }
}
