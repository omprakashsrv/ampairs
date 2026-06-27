package com.ampairs.notification.credential

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CredentialCryptoServiceTest {

    private val crypto = CredentialCryptoService("test-master-key-123")

    @Test
    fun `round-trips a secret`() {
        val secret = "wha_t0ken_SECRET_value"
        val cipher = crypto.encrypt(secret)
        assertNotEquals(secret, cipher) // ciphertext is not the plaintext
        assertEquals(secret, crypto.decrypt(cipher))
    }

    @Test
    fun `produces different ciphertext each time (random IV)`() {
        assertNotEquals(crypto.encrypt("same"), crypto.encrypt("same"))
    }

    @Test
    fun `reports unconfigured and throws when no key`() {
        val unconfigured = CredentialCryptoService("")
        assertFalse(unconfigured.isConfigured)
        assertThrows(IllegalStateException::class.java) { unconfigured.encrypt("x") }
    }

    @Test
    fun `is configured when a key is provided`() {
        assertTrue(crypto.isConfigured)
    }
}
