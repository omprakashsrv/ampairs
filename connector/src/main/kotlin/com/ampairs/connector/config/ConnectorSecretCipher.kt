package com.ampairs.connector.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypts/decrypts connector secrets (credentials, tokens) at rest with AES-GCM. The symmetric key
 * comes from the `CONNECTOR_SECRET_KEY` env var (Constitution Principle XI — never commit secrets).
 * Secret values are never returned to clients (see config response DTO); this only protects them in
 * the database.
 */
@Component
class ConnectorSecretCipher(
    @Value("\${connector.secret-key:\${CONNECTOR_SECRET_KEY:}}") rawKey: String,
) {
    private val key: SecretKeySpec = SecretKeySpec(
        // Derive a stable 256-bit key from the configured secret (dev-friendly; rotate in prod).
        MessageDigest.getInstance("SHA-256").digest(rawKey.ifBlank { DEV_FALLBACK }.toByteArray()),
        "AES",
    )

    fun encrypt(plain: String): String {
        val iv = ByteArray(IV_LEN).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        val ct = cipher.doFinal(plain.toByteArray())
        return Base64.getEncoder().encodeToString(iv + ct)
    }

    fun decrypt(encoded: String): String {
        val data = Base64.getDecoder().decode(encoded)
        val iv = data.copyOfRange(0, IV_LEN)
        val ct = data.copyOfRange(IV_LEN, data.size)
        val cipher = Cipher.getInstance(TRANSFORM).apply {
            init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, iv))
        }
        return String(cipher.doFinal(ct))
    }

    private companion object {
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_LEN = 12
        const val TAG_BITS = 128
        // Used only when no key is configured (local dev/tests). Not for production.
        const val DEV_FALLBACK = "ampairs-connector-dev-secret"
    }
}
