package com.ampairs.auth.service

import com.ampairs.core.config.ApplicationProperties
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * RSA Key Manager for JWT RS256 implementation
 * Handles key generation, storage, rotation, and management
 */
@Service
class RsaKeyManager(
    private val applicationProperties: ApplicationProperties,
) {
    private val logger = LoggerFactory.getLogger(RsaKeyManager::class.java)

    // Thread-safe key storage
    private val keyLock = ReentrantReadWriteLock()
    private var currentKeyPair: RSAKeyPair? = null
    private val keyHistory = mutableListOf<RSAKeyPair>()

    // Initialize keys immediately during construction
    init {
        initializeKeys()
    }

    companion object {
        private const val RSA_KEY_SIZE = 2048
        private const val KEY_ALGORITHM = "RSA"
        private const val KEY_ID_LENGTH = 8

        // Default key paths
        private const val DEFAULT_PRIVATE_KEY_PATH = "keys/private.pem"
        private const val DEFAULT_PUBLIC_KEY_PATH = "keys/public.pem"
        private const val DEFAULT_KEY_METADATA_PATH = "keys/metadata.json"
    }

    /**
     * RSA Key Pair with metadata
     */
    data class RSAKeyPair(
        val keyId: String,
        val privateKey: RSAPrivateKey,
        val publicKey: RSAPublicKey,
        val algorithm: String = "RS256",
        val createdAt: Instant = Instant.now(),
        val expiresAt: Instant? = null,
        val isActive: Boolean = true,
    ) {
        fun isExpired(): Boolean = expiresAt?.let { Instant.now().isAfter(it) } ?: false

        fun toJwk(): Map<String, Any> {
            return mapOf(
                "kty" to "RSA",
                "use" to "sig",
                "alg" to algorithm,
                "kid" to keyId,
                "n" to Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(publicKey.modulus.toByteArray()),
                "e" to Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(publicKey.publicExponent.toByteArray())
            )
        }
    }

    /**
     * Key metadata for persistence
     */
    data class KeyMetadata(
        val keyId: String,
        val algorithm: String,
        val createdAt: Instant,
        val expiresAt: Instant?,
        val isActive: Boolean,
        val privateKeyPath: String,
        val publicKeyPath: String,
    )

    /**
     * Initialize key manager immediately during construction
     */
    private fun initializeKeys() {
        logger.info("Initializing RSA Key Manager")

        try {
            // Load existing keys or generate new ones
            if (!loadExistingKeys()) {
                logger.info("No existing keys found, generating new RSA key pair")
                generateAndStoreNewKeyPair()
            }

            logger.info(
                "RSA Key Manager initialized successfully with key ID: {}",
                getCurrentKeyPair().keyId
            )

        } catch (e: Exception) {
            logger.error("Failed to initialize RSA Key Manager", e)
            throw RuntimeException("RSA Key Manager initialization failed", e)
        }
    }

    /**
     * Get current active key pair for signing
     */
    fun getCurrentKeyPair(): RSAKeyPair = keyLock.read {
        currentKeyPair ?: throw IllegalStateException("No active RSA key pair available")
    }

    /**
     * Get public key by key ID for verification
     */
    fun getPublicKey(keyId: String): RSAPublicKey? = keyLock.read {
        // Check current key first
        if (currentKeyPair?.keyId == keyId) {
            return currentKeyPair?.publicKey
        }

        // Check key history
        keyHistory.find { it.keyId == keyId && !it.isExpired() }?.publicKey
    }

    /**
     * Get all active public keys as JWK Set
     */
    fun getJwkSet(): Map<String, Any> = keyLock.read {
        val keys = mutableListOf<Map<String, Any>>()

        // Add current key
        currentKeyPair?.let { keys.add(it.toJwk()) }

        // Add active keys from history
        keyHistory.filter { !it.isExpired() && it.isActive }
            .forEach { keys.add(it.toJwk()) }

        mapOf("keys" to keys)
    }

    /**
     * Generate new RSA key pair
     */
    fun generateNewKeyPair(): RSAKeyPair {
        logger.info("Generating new RSA key pair")

        val keyPairGenerator = KeyPairGenerator.getInstance(KEY_ALGORITHM)
        keyPairGenerator.initialize(RSA_KEY_SIZE, SecureRandom())

        val keyPair = keyPairGenerator.generateKeyPair()
        val keyId = generateKeyId()

        val rsaKeyPair = RSAKeyPair(
            keyId = keyId,
            privateKey = keyPair.private as RSAPrivateKey,
            publicKey = keyPair.public as RSAPublicKey,
            expiresAt = calculateKeyExpiration()
        )

        logger.info("Generated new RSA key pair with ID: {}", keyId)
        return rsaKeyPair
    }

    /**
     * Rotate to a new key pair
     */
    fun rotateKeys(): RSAKeyPair {
        logger.info("Starting key rotation")

        keyLock.write {
            // Move current key to history
            currentKeyPair?.let { oldKey ->
                keyHistory.add(oldKey.copy(isActive = false))
                logger.info("Moved key {} to history", oldKey.keyId)
            }

            // Generate and set new current key
            val newKeyPair = generateNewKeyPair()
            currentKeyPair = newKeyPair

            // Store the new key pair
            storeKeyPair(newKeyPair)

            // Cleanup old expired keys
            cleanupExpiredKeys()

            logger.info("Key rotation completed. New key ID: {}", newKeyPair.keyId)
            return newKeyPair
        }
    }

    /**
     * Check if key rotation is needed
     */
    fun isKeyRotationNeeded(): Boolean = keyLock.read {
        val keyRotationConfig = applicationProperties.security.jwt.keyRotation

        if (!keyRotationConfig.enabled) {
            return false
        }

        currentKeyPair?.let { keyPair ->
            val timeSinceCreation = java.time.Duration.between(keyPair.createdAt, Instant.now())
            val rotationThreshold = keyRotationConfig.rotationInterval

            return timeSinceCreation >= rotationThreshold
        } ?: true // Rotate if no current key
    }

    /**
     * Load existing keys from storage
     */
    private fun loadExistingKeys(): Boolean {
        val keyConfig = applicationProperties.security.jwt.keyStorage

        val privateKeyPath = keyConfig.privateKeyPath ?: DEFAULT_PRIVATE_KEY_PATH
        val publicKeyPath = keyConfig.publicKeyPath ?: DEFAULT_PUBLIC_KEY_PATH

        return try {
            val privateKeyResource = getResource(privateKeyPath)
            val publicKeyResource = getResource(publicKeyPath)

            if (!privateKeyResource.exists() || !publicKeyResource.exists()) {
                logger.info("Key files not found at {} or {}", privateKeyPath, publicKeyPath)
                return false
            }

            val privateKeyBytes = privateKeyResource.inputStream.readBytes()
            val publicKeyBytes = publicKeyResource.inputStream.readBytes()

            val privateKey = loadPrivateKey(privateKeyBytes)
            val publicKey = loadPublicKey(publicKeyBytes)

            // Generate key ID for existing key (or load from metadata)
            val keyId = loadKeyIdFromMetadata() ?: generateKeyId()

            currentKeyPair = RSAKeyPair(
                keyId = keyId,
                privateKey = privateKey,
                publicKey = publicKey,
                createdAt = loadKeyCreationTime() ?: Instant.now()
            )

            logger.info("Loaded existing RSA key pair with ID: {}", keyId)
            true
        } catch (e: Exception) {
            logger.warn("Failed to load existing keys: {}", e.message)
            false
        }
    }

    /**
     * Generate and store new key pair
     */
    private fun generateAndStoreNewKeyPair(): RSAKeyPair {
        val newKeyPair = generateNewKeyPair()
        currentKeyPair = newKeyPair
        storeKeyPair(newKeyPair)
        return newKeyPair
    }

    /**
     * Store key pair to persistent storage
     */
    private fun storeKeyPair(keyPair: RSAKeyPair) {
        val keyConfig = applicationProperties.security.jwt.keyStorage

        val privateKeyPath = keyConfig.privateKeyPath ?: DEFAULT_PRIVATE_KEY_PATH
        val publicKeyPath = keyConfig.publicKeyPath ?: DEFAULT_PUBLIC_KEY_PATH

        try {
            // Ensure directory exists
            val keysDir = File(privateKeyPath).parentFile
            if (!keysDir.exists()) {
                keysDir.mkdirs()
            }

            // Store private key
            val privateKeyPem = encodeToPEM(keyPair.privateKey.encoded, "PRIVATE KEY")
            Files.write(Paths.get(privateKeyPath), privateKeyPem.toByteArray())

            // Store public key
            val publicKeyPem = encodeToPEM(keyPair.publicKey.encoded, "PUBLIC KEY")
            Files.write(Paths.get(publicKeyPath), publicKeyPem.toByteArray())

            // Store metadata
            storeKeyMetadata(keyPair, privateKeyPath, publicKeyPath)

            logger.info("Stored RSA key pair with ID: {}", keyPair.keyId)
        } catch (e: Exception) {
            logger.error("Failed to store key pair", e)
            throw RuntimeException("Failed to store RSA key pair", e)
        }
    }

    /**
     * Store key metadata
     */
    private fun storeKeyMetadata(keyPair: RSAKeyPair, privateKeyPath: String, publicKeyPath: String) {
        val metadata = KeyMetadata(
            keyId = keyPair.keyId,
            algorithm = keyPair.algorithm,
            createdAt = keyPair.createdAt,
            expiresAt = keyPair.expiresAt,
            isActive = keyPair.isActive,
            privateKeyPath = privateKeyPath,
            publicKeyPath = publicKeyPath
        )

        val metadataPath = applicationProperties.security.jwt.keyStorage.metadataPath
            ?: DEFAULT_KEY_METADATA_PATH

        // Store as JSON (simplified - in production, consider using Jackson)
        val json = """
        {
            "keyId": "${metadata.keyId}",
            "algorithm": "${metadata.algorithm}",
            "createdAt": "${metadata.createdAt}",
            "expiresAt": ${if (metadata.expiresAt != null) "\"${metadata.expiresAt}\"" else "null"},
            "isActive": ${metadata.isActive},
            "privateKeyPath": "${metadata.privateKeyPath}",
            "publicKeyPath": "${metadata.publicKeyPath}"
        }
        """.trimIndent()

        Files.write(Paths.get(metadataPath), json.toByteArray())
    }

    /**
     * Load private key from bytes
     */
    private fun loadPrivateKey(keyBytes: ByteArray): RSAPrivateKey {
        val cleanedKey = String(keyBytes)
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("\\s".toRegex(), "")

        val decoded = Base64.getDecoder().decode(cleanedKey)
        val keySpec = PKCS8EncodedKeySpec(decoded)
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)

        return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    }

    /**
     * Load public key from bytes
     */
    private fun loadPublicKey(keyBytes: ByteArray): RSAPublicKey {
        val cleanedKey = String(keyBytes)
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s".toRegex(), "")

        val decoded = Base64.getDecoder().decode(cleanedKey)
        val keySpec = X509EncodedKeySpec(decoded)
        val keyFactory = KeyFactory.getInstance(KEY_ALGORITHM)

        return keyFactory.generatePublic(keySpec) as RSAPublicKey
    }

    /**
     * Encode key to PEM format
     */
    private fun encodeToPEM(keyBytes: ByteArray, keyType: String): String {
        val base64Key = Base64.getEncoder().encodeToString(keyBytes)
        val formattedKey = base64Key.chunked(64).joinToString("\n")
        return "-----BEGIN $keyType-----\n$formattedKey\n-----END $keyType-----\n"
    }

    /**
     * Generate unique key ID
     */
    private fun generateKeyId(): String {
        return UUID.randomUUID().toString().replace("-", "").take(KEY_ID_LENGTH)
    }

    /**
     * Calculate key expiration based on configuration
     */
    private fun calculateKeyExpiration(): Instant? {
        val keyRotationConfig = applicationProperties.security.jwt.keyRotation
        return if (keyRotationConfig.enabled && keyRotationConfig.keyLifetime != null) {
            Instant.now().plus(keyRotationConfig.keyLifetime)
        } else null
    }

    /**
     * Load key ID from metadata
     */
    private fun loadKeyIdFromMetadata(): String? {
        return try {
            val metadataPath = applicationProperties.security.jwt.keyStorage.metadataPath
                ?: DEFAULT_KEY_METADATA_PATH

            val resource = getResource(metadataPath)
            if (!resource.exists()) return null

            val content = String(resource.inputStream.readBytes())
            // Simple JSON parsing - extract keyId
            val keyIdMatch = """"keyId"\s*:\s*"([^"]+)"""".toRegex().find(content)
            keyIdMatch?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.debug("Could not load key ID from metadata: {}", e.message)
            null
        }
    }

    /**
     * Load key creation time from metadata
     */
    private fun loadKeyCreationTime(): Instant? {
        return try {
            val metadataPath = applicationProperties.security.jwt.keyStorage.metadataPath
                ?: DEFAULT_KEY_METADATA_PATH

            val resource = getResource(metadataPath)
            if (!resource.exists()) return null

            val content = String(resource.inputStream.readBytes())
            val createdAtMatch = """"createdAt"\s*:\s*"([^"]+)"""".toRegex().find(content)
            createdAtMatch?.groupValues?.get(1)?.let { Instant.parse(it) }
        } catch (e: Exception) {
            logger.debug("Could not load creation time from metadata: {}", e.message)
            null
        }
    }

    /**
     * Get resource (file or classpath)
     */
    private fun getResource(path: String): Resource {
        return if (File(path).exists()) {
            FileSystemResource(path)
        } else {
            ClassPathResource(path)
        }
    }

    /**
     * Cleanup expired keys from history
     */
    private fun cleanupExpiredKeys() {
        val sizeBefore = keyHistory.size
        keyHistory.removeIf { it.isExpired() }
        val removedCount = sizeBefore - keyHistory.size

        if (removedCount > 0) {
            logger.info("Cleaned up {} expired keys from history", removedCount)
        }
    }

    /**
     * Get key statistics
     */
    fun getKeyStatistics(): Map<String, Any> = keyLock.read {
        mapOf(
            "current_key_id" to (currentKeyPair?.keyId ?: "none"),
            "current_key_created" to (currentKeyPair?.createdAt?.toString() ?: "unknown"),
            "current_key_expires" to (currentKeyPair?.expiresAt?.toString() ?: "never"),
            "history_keys_count" to keyHistory.size,
            "active_keys_count" to (keyHistory.count { !it.isExpired() && it.isActive } + if (currentKeyPair != null) 1 else 0),
            "rotation_needed" to isKeyRotationNeeded()
        )
    }
}