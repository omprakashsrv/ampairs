package com.ampairs.auth.service

import com.ampairs.core.config.ApplicationProperties
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.SecureRandom
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Enhanced RSA Key Manager for JWT RS256 implementation
 * Supports multi-key persistence with versioned storage and graceful key rotation
 *
 * Features:
 * - Versioned key storage (keyId_date directories)
 * - Multi-key metadata tracking
 * - Persistent key history across restarts
 * - Warning logs for old key usage
 * - Automatic legacy key migration
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

        // Legacy paths for migration
        private const val LEGACY_PRIVATE_KEY_PATH = "keys/private.pem"
        private const val LEGACY_PUBLIC_KEY_PATH = "keys/public.pem"
        private const val DEFAULT_KEY_METADATA_PATH = "keys/metadata.json"
        private const val KEYS_BASE_DIR = "keys"
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
     * Enhanced key metadata for multi-key persistence
     */
    data class KeyMetadata(
        val keyId: String,
        val algorithm: String,
        val createdAt: Instant,
        val expiresAt: Instant?,
        val isActive: Boolean,
        val isCurrent: Boolean,
        val keyDirectory: String,
    )

    /**
     * Metadata file structure with multiple keys
     */
    data class KeyStoreMetadata(
        val currentKeyId: String,
        val keys: List<KeyMetadata>
    )

    /**
     * Initialize key manager with multi-key support
     */
    private fun initializeKeys() {
        logger.info("Initializing Enhanced RSA Key Manager with multi-key persistence")

        try {
            // Check for legacy format and migrate if needed
            migrateFromLegacyKeyStorage()

            // Load ALL existing keys (current + history) or generate new
            if (!loadAllExistingKeys()) {
                logger.info("No existing keys found, generating new RSA key pair")
                generateAndStoreNewKeyPair()
            }

            logger.info(
                "✅ RSA Key Manager initialized: current={}, history={}, total={}",
                currentKeyPair?.keyId,
                keyHistory.size,
                (if (currentKeyPair != null) 1 else 0) + keyHistory.size
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
     * Get public key by key ID for verification (with warning logs for old keys)
     */
    fun getPublicKey(keyId: String): RSAPublicKey? = keyLock.read {
        // Check current key first
        if (currentKeyPair?.keyId == keyId) {
            logger.debug("✓ Using CURRENT key for verification: {}", keyId)
            return currentKeyPair?.publicKey
        }

        // Check key history (old keys)
        val historicalKey = keyHistory.find { it.keyId == keyId && !it.isExpired() }

        if (historicalKey != null) {
            // WARNING: Using old key
            val keyAge = java.time.Duration.between(historicalKey.createdAt, Instant.now())
            val daysUntilExpiry = historicalKey.expiresAt?.let {
                java.time.Duration.between(Instant.now(), it).toDays()
            } ?: Long.MAX_VALUE

            logger.warn(
                "⚠️  Using OLD key for token verification: {} (age: {} days, expires in: {} days)",
                keyId,
                keyAge.toDays(),
                if (daysUntilExpiry == Long.MAX_VALUE) "never" else daysUntilExpiry
            )
            return historicalKey.publicKey
        }

        // Key not found
        logger.error("❌ Unknown key ID requested: {} (not found in current or history)", keyId)
        return null
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
     * Rotate to a new key pair (with versioned storage)
     */
    fun rotateKeys(): RSAKeyPair {
        logger.info("🔄 Starting key rotation")

        keyLock.write {
            // Move current key to history
            currentKeyPair?.let { oldKey ->
                keyHistory.add(oldKey.copy(isActive = false))
                logger.info("Moved key {} to history (still valid for verification)", oldKey.keyId)
            }

            // Generate and set new current key
            val newKeyPair = generateNewKeyPair()
            currentKeyPair = newKeyPair

            // Store the new key pair in versioned directory
            storeKeyPairVersioned(newKeyPair)

            // Cleanup old expired keys from memory and disk
            cleanupExpiredKeys()

            logger.info("✅ Key rotation completed. New key ID: {}", newKeyPair.keyId)
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
     * Load ALL existing keys from versioned storage (current + history)
     */
    private fun loadAllExistingKeys(): Boolean {
        try {
            val metadata = loadKeyStoreMetadata()

            if (metadata.keys.isEmpty()) {
                logger.info("No existing keys found in metadata")
                return false
            }

            var loadedCount = 0

            // Load all keys from metadata
            metadata.keys.forEach { keyMeta ->
                try {
                    val keyDir = Paths.get(keyMeta.keyDirectory)

                    if (!Files.exists(keyDir)) {
                        logger.warn("Key directory not found: {}", keyMeta.keyDirectory)
                        return@forEach
                    }

                    // Load private and public keys
                    val privateKeyPath = keyDir.resolve("private.pem")
                    val publicKeyPath = keyDir.resolve("public.pem")

                    if (!Files.exists(privateKeyPath) || !Files.exists(publicKeyPath)) {
                        logger.warn("Key files missing in: {}", keyMeta.keyDirectory)
                        return@forEach
                    }

                    val privateKeyBytes = Files.readAllBytes(privateKeyPath)
                    val publicKeyBytes = Files.readAllBytes(publicKeyPath)

                    val privateKey = loadPrivateKey(privateKeyBytes)
                    val publicKey = loadPublicKey(publicKeyBytes)

                    val keyPair = RSAKeyPair(
                        keyId = keyMeta.keyId,
                        privateKey = privateKey,
                        publicKey = publicKey,
                        algorithm = keyMeta.algorithm,
                        createdAt = keyMeta.createdAt,
                        expiresAt = keyMeta.expiresAt,
                        isActive = keyMeta.isActive
                    )

                    // Set as current or add to history
                    if (keyMeta.isCurrent) {
                        currentKeyPair = keyPair
                        logger.info("✓ Loaded CURRENT key: {} (created: {})",
                            keyMeta.keyId, keyMeta.createdAt)
                    } else if (!keyPair.isExpired()) {
                        keyHistory.add(keyPair)
                        logger.info("✓ Loaded HISTORICAL key: {} (expires: {})",
                            keyMeta.keyId, keyMeta.expiresAt)
                    } else {
                        logger.debug("Skipped EXPIRED key: {}", keyMeta.keyId)
                    }

                    loadedCount++

                } catch (e: Exception) {
                    logger.error("Failed to load key {}: {}", keyMeta.keyId, e.message)
                }
            }

            logger.info("Loaded {} keys from storage (current: {}, history: {})",
                loadedCount,
                if (currentKeyPair != null) 1 else 0,
                keyHistory.size
            )

            return currentKeyPair != null

        } catch (e: Exception) {
            logger.warn("Failed to load existing keys: {}", e.message)
            return false
        }
    }

    /**
     * Generate and store new key pair
     */
    private fun generateAndStoreNewKeyPair(): RSAKeyPair {
        val newKeyPair = generateNewKeyPair()
        currentKeyPair = newKeyPair
        storeKeyPairVersioned(newKeyPair)
        return newKeyPair
    }

    /**
     * Store key pair with versioned directory structure
     */
    private fun storeKeyPairVersioned(keyPair: RSAKeyPair) {
        try {
            // Create versioned directory: keys/{keyId}_{date}/
            val datePrefix = keyPair.createdAt.toString().substring(0, 10) // "2025-11-02"
            val keyDirectory = "$KEYS_BASE_DIR/${keyPair.keyId}_$datePrefix"
            val keyDir = Paths.get(keyDirectory)

            // Create directory if not exists
            Files.createDirectories(keyDir)

            // Store private key in versioned directory
            val privateKeyPath = keyDir.resolve("private.pem")
            val privateKeyPem = encodeToPEM(keyPair.privateKey.encoded, "PRIVATE KEY")
            Files.write(privateKeyPath, privateKeyPem.toByteArray())

            // Store public key in versioned directory
            val publicKeyPath = keyDir.resolve("public.pem")
            val publicKeyPem = encodeToPEM(keyPair.publicKey.encoded, "PUBLIC KEY")
            Files.write(publicKeyPath, publicKeyPem.toByteArray())

            // Update metadata with all keys
            updateKeyStoreMetadata(keyPair, keyDirectory)

            logger.info("Stored RSA key pair: {} in {}", keyPair.keyId, keyDirectory)
        } catch (e: Exception) {
            logger.error("Failed to store key pair", e)
            throw RuntimeException("Failed to store RSA key pair", e)
        }
    }

    /**
     * Update metadata.json with all keys (maintains history)
     */
    private fun updateKeyStoreMetadata(newKeyPair: RSAKeyPair, keyDirectory: String) {
        val metadataPath = Paths.get(
            applicationProperties.security.jwt.keyStorage.metadataPath
                ?: DEFAULT_KEY_METADATA_PATH
        )

        // Load existing metadata or create new
        val existingMetadata = loadKeyStoreMetadata()

        // Create metadata for new key
        val newKeyMetadata = KeyMetadata(
            keyId = newKeyPair.keyId,
            algorithm = newKeyPair.algorithm,
            createdAt = newKeyPair.createdAt,
            expiresAt = newKeyPair.expiresAt,
            isActive = newKeyPair.isActive,
            isCurrent = true,
            keyDirectory = keyDirectory
        )

        // Mark all existing keys as not current
        val updatedKeys = existingMetadata.keys.map { it.copy(isCurrent = false) }

        // Add new key to the list
        val allKeys = updatedKeys + newKeyMetadata

        // Create updated metadata
        val updatedMetadata = KeyStoreMetadata(
            currentKeyId = newKeyPair.keyId,
            keys = allKeys
        )

        // Serialize to JSON
        val json = serializeKeyStoreMetadata(updatedMetadata)
        Files.write(metadataPath, json.toByteArray())

        logger.info("Updated metadata.json with {} total keys", allKeys.size)
    }

    /**
     * Load all keys from metadata.json
     */
    private fun loadKeyStoreMetadata(): KeyStoreMetadata {
        val metadataPath = Paths.get(
            applicationProperties.security.jwt.keyStorage.metadataPath
                ?: DEFAULT_KEY_METADATA_PATH
        )

        if (!Files.exists(metadataPath)) {
            return KeyStoreMetadata(currentKeyId = "", keys = emptyList())
        }

        return try {
            val content = String(Files.readAllBytes(metadataPath))
            deserializeKeyStoreMetadata(content)
        } catch (e: Exception) {
            logger.warn("Failed to load key store metadata: {}", e.message)
            KeyStoreMetadata(currentKeyId = "", keys = emptyList())
        }
    }

    /**
     * Serialize metadata to JSON
     */
    private fun serializeKeyStoreMetadata(metadata: KeyStoreMetadata): String {
        val keysJson = metadata.keys.joinToString(",\n    ") { key ->
            """
        {
            "keyId": "${key.keyId}",
            "algorithm": "${key.algorithm}",
            "createdAt": "${key.createdAt}",
            "expiresAt": ${if (key.expiresAt != null) "\"${key.expiresAt}\"" else "null"},
            "isActive": ${key.isActive},
            "isCurrent": ${key.isCurrent},
            "keyDirectory": "${key.keyDirectory}"
        }""".trimIndent()
        }

        return """
{
    "currentKeyId": "${metadata.currentKeyId}",
    "keys": [
    $keysJson
    ]
}
""".trimIndent()
    }

    /**
     * Deserialize metadata from JSON
     */
    private fun deserializeKeyStoreMetadata(json: String): KeyStoreMetadata {
        // Simple regex-based parsing (consider using Jackson in production)
        val currentKeyIdMatch = """"currentKeyId"\s*:\s*"([^"]+)"""".toRegex().find(json)
        val currentKeyId = currentKeyIdMatch?.groupValues?.get(1) ?: ""

        val keys = mutableListOf<KeyMetadata>()

        // Find all key objects in the JSON
        val keyObjectPattern = """\{[^{}]*(?:\{[^{}]*\}[^{}]*)*\}""".toRegex()
        val keysArrayMatch = """"keys"\s*:\s*\[(.*)\]""".toRegex(RegexOption.DOT_MATCHES_ALL).find(json)

        keysArrayMatch?.groupValues?.get(1)?.let { keysArray ->
            keyObjectPattern.findAll(keysArray).forEach { match ->
                val keyJson = match.value

                val keyId = """"keyId"\s*:\s*"([^"]+)"""".toRegex().find(keyJson)?.groupValues?.get(1) ?: return@forEach
                val algorithm = """"algorithm"\s*:\s*"([^"]+)"""".toRegex().find(keyJson)?.groupValues?.get(1) ?: "RS256"
                val createdAtStr = """"createdAt"\s*:\s*"([^"]+)"""".toRegex().find(keyJson)?.groupValues?.get(1)
                val expiresAtMatch = """"expiresAt"\s*:\s*"([^"]+)"""".toRegex().find(keyJson)
                val expiresAtStr = expiresAtMatch?.groupValues?.get(1)
                val isActive = """"isActive"\s*:\s*(true|false)""".toRegex().find(keyJson)?.groupValues?.get(1)?.toBoolean() ?: false
                val isCurrent = """"isCurrent"\s*:\s*(true|false)""".toRegex().find(keyJson)?.groupValues?.get(1)?.toBoolean() ?: false
                val keyDirectory = """"keyDirectory"\s*:\s*"([^"]+)"""".toRegex().find(keyJson)?.groupValues?.get(1) ?: ""

                keys.add(KeyMetadata(
                    keyId = keyId,
                    algorithm = algorithm,
                    createdAt = createdAtStr?.let { Instant.parse(it) } ?: Instant.now(),
                    expiresAt = expiresAtStr?.let { Instant.parse(it) },
                    isActive = isActive,
                    isCurrent = isCurrent,
                    keyDirectory = keyDirectory
                ))
            }
        }

        return KeyStoreMetadata(currentKeyId = currentKeyId, keys = keys)
    }

    /**
     * Migrate existing single-key setup to multi-key structure
     */
    private fun migrateFromLegacyKeyStorage() {
        val legacyPrivateKeyPath = Paths.get(LEGACY_PRIVATE_KEY_PATH)
        val legacyPublicKeyPath = Paths.get(LEGACY_PUBLIC_KEY_PATH)
        val legacyMetadataPath = Paths.get(DEFAULT_KEY_METADATA_PATH)

        // Check if this is old format (files exist but no versioned structure)
        if (!Files.exists(legacyPrivateKeyPath) || !Files.exists(legacyPublicKeyPath)) {
            return
        }

        // Check if already migrated (versioned directories exist)
        val metadata = loadKeyStoreMetadata()
        if (metadata.keys.isNotEmpty()) {
            logger.debug("Keys already in versioned format, skipping migration")
            return
        }

        logger.info("🔄 Detected legacy key format, migrating to versioned storage...")

        try {
            // Load legacy keys
            val privateKeyBytes = Files.readAllBytes(legacyPrivateKeyPath)
            val publicKeyBytes = Files.readAllBytes(legacyPublicKeyPath)

            val privateKey = loadPrivateKey(privateKeyBytes)
            val publicKey = loadPublicKey(publicKeyBytes)

            // Try to load keyId from old metadata
            val keyId = loadLegacyKeyId() ?: generateKeyId()
            val createdAt = loadLegacyCreationTime() ?: Instant.now()

            val legacyKeyPair = RSAKeyPair(
                keyId = keyId,
                privateKey = privateKey,
                publicKey = publicKey,
                createdAt = createdAt,
                expiresAt = calculateKeyExpiration()
            )

            // Store in new versioned format
            storeKeyPairVersioned(legacyKeyPair)
            currentKeyPair = legacyKeyPair

            // Backup old files
            Files.move(legacyPrivateKeyPath, Paths.get("$LEGACY_PRIVATE_KEY_PATH.legacy"))
            Files.move(legacyPublicKeyPath, Paths.get("$LEGACY_PUBLIC_KEY_PATH.legacy"))
            if (Files.exists(legacyMetadataPath)) {
                Files.move(legacyMetadataPath, Paths.get("$DEFAULT_KEY_METADATA_PATH.legacy"))
            }

            logger.info("✅ Successfully migrated legacy key {} to versioned storage", keyId)

        } catch (e: Exception) {
            logger.error("Failed to migrate legacy keys: {}", e.message, e)
            throw RuntimeException("Legacy key migration failed", e)
        }
    }

    /**
     * Load key ID from legacy metadata
     */
    private fun loadLegacyKeyId(): String? {
        return try {
            val metadataPath = Paths.get(DEFAULT_KEY_METADATA_PATH)
            if (!Files.exists(metadataPath)) return null

            val content = String(Files.readAllBytes(metadataPath))
            val keyIdMatch = """"keyId"\s*:\s*"([^"]+)"""".toRegex().find(content)
            keyIdMatch?.groupValues?.get(1)
        } catch (e: Exception) {
            logger.debug("Could not load legacy key ID: {}", e.message)
            null
        }
    }

    /**
     * Load creation time from legacy metadata
     */
    private fun loadLegacyCreationTime(): Instant? {
        return try {
            val metadataPath = Paths.get(DEFAULT_KEY_METADATA_PATH)
            if (!Files.exists(metadataPath)) return null

            val content = String(Files.readAllBytes(metadataPath))
            val createdAtMatch = """"createdAt"\s*:\s*"([^"]+)"""".toRegex().find(content)
            createdAtMatch?.groupValues?.get(1)?.let { Instant.parse(it) }
        } catch (e: Exception) {
            logger.debug("Could not load legacy creation time: {}", e.message)
            null
        }
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
     * Cleanup expired keys from history and disk
     */
    private fun cleanupExpiredKeys() {
        val metadata = loadKeyStoreMetadata()
        val expiredKeys = metadata.keys.filter { keyMeta ->
            keyMeta.expiresAt?.let { Instant.now().isAfter(it) } ?: false
        }

        if (expiredKeys.isEmpty()) {
            return
        }

        logger.info("🧹 Cleaning up {} expired keys", expiredKeys.size)

        // Remove from memory
        val sizeBefore = keyHistory.size
        keyHistory.removeIf { it.isExpired() }
        val removedFromMemory = sizeBefore - keyHistory.size

        // Remove from disk and metadata
        expiredKeys.forEach { keyMeta ->
            try {
                val keyDir = Paths.get(keyMeta.keyDirectory)
                if (Files.exists(keyDir)) {
                    Files.walk(keyDir)
                        .sorted(Comparator.reverseOrder())
                        .forEach { Files.delete(it) }
                    logger.debug("Deleted expired key directory: {}", keyMeta.keyDirectory)
                }
            } catch (e: Exception) {
                logger.warn("Failed to delete key directory {}: {}", keyMeta.keyDirectory, e.message)
            }
        }

        // Update metadata (remove expired keys)
        val remainingKeys = metadata.keys.filter { keyMeta ->
            keyMeta.expiresAt?.let { !Instant.now().isAfter(it) } ?: true
        }

        val updatedMetadata = KeyStoreMetadata(
            currentKeyId = metadata.currentKeyId,
            keys = remainingKeys
        )

        val metadataPath = Paths.get(DEFAULT_KEY_METADATA_PATH)
        val json = serializeKeyStoreMetadata(updatedMetadata)
        Files.write(metadataPath, json.toByteArray())

        logger.info("✅ Cleaned up {} keys from memory, {} from disk",
            removedFromMemory, expiredKeys.size)
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
            "rotation_needed" to isKeyRotationNeeded(),
            "total_keys_on_disk" to loadKeyStoreMetadata().keys.size
        )
    }
}
