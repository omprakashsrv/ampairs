package com.ampairs.notification.credential

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM encryption for per-workspace provider secrets. The master key is supplied via the
 * environment (`communication.credentials.encryption-key` → `COMM_CRED_ENCRYPTION_KEY`) — never in
 * source. Output format: base64( iv[12] || ciphertext||tag ). Secrets are never logged or echoed.
 */
@Service
class CredentialCryptoService(
    @Value("\${communication.credentials.encryption-key:}") private val masterKey: String,
) {
    private val random = SecureRandom()

    val isConfigured: Boolean get() = masterKey.isNotBlank()

    private fun key(): SecretKeySpec {
        check(masterKey.isNotBlank()) { "Credential encryption key is not configured (COMM_CRED_ENCRYPTION_KEY)" }
        // Derive a fixed 256-bit key from the provided secret so any-length input works.
        val digest = MessageDigest.getInstance("SHA-256").digest(masterKey.toByteArray(Charsets.UTF_8))
        return SecretKeySpec(digest, "AES")
    }

    fun encrypt(plaintext: String): String {
        val iv = ByteArray(IV_LEN).also(random::nextBytes)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        val ct = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(iv + ct)
    }

    fun decrypt(encoded: String): String {
        val all = Base64.getDecoder().decode(encoded)
        val iv = all.copyOfRange(0, IV_LEN)
        val ct = all.copyOfRange(IV_LEN, all.size)
        val cipher = Cipher.getInstance(TRANSFORM)
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(ct), Charsets.UTF_8)
    }

    companion object {
        private const val TRANSFORM = "AES/GCM/NoPadding"
        private const val IV_LEN = 12
        private const val TAG_BITS = 128
    }
}
