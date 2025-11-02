package com.ampairs.auth.service

import com.ampairs.core.config.ApplicationProperties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import java.time.Duration

/**
 * Integration test to verify JWT RS256 implementation works correctly
 */
class JwtRS256IntegrationTest {

    @Test
    fun `test RSA key generation and JWT creation with RS256`() {
        // Create application properties with RS256 configuration
        val appProperties = ApplicationProperties(
            security = ApplicationProperties.SecurityProperties(
                jwt = ApplicationProperties.SecurityProperties.JwtProperties(
                    algorithm = "RS256",
                    expiration = Duration.ofHours(1),
                    keyRotation = ApplicationProperties.SecurityProperties.JwtProperties.KeyRotationProperties(
                        enabled = true,
                        rotationInterval = Duration.ofDays(30)
                    )
                )
            )
        )

        // Initialize RSA Key Manager
        val rsaKeyManager = RsaKeyManager(appProperties)

        // Generate and set initial key pair
        val currentKeyPair = assertDoesNotThrow {
            val keyPair = rsaKeyManager.generateNewKeyPair()
            assertNotNull(keyPair.keyId)
            assertNotNull(keyPair.privateKey)
            assertNotNull(keyPair.publicKey)
            assertEquals("RS256", keyPair.algorithm)

            // Set this key as the current key manually for testing
            val currentKeyField = RsaKeyManager::class.java.getDeclaredField("currentKeyPair")
            currentKeyField.isAccessible = true
            currentKeyField.set(rsaKeyManager, keyPair)
            keyPair
        }

        // Create JWT Service with RS256 configuration
        val jwtService = JwtService(appProperties, rsaKeyManager)

        // Create test user
        val testUser: UserDetails = User.builder()
            .username("test@ampairs.com")
            .password("password")
            .authorities("USER", "ADMIN")
            .build()

        // Generate JWT token with RS256
        val token = assertDoesNotThrow {
            jwtService.generateToken(testUser)
        }

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        println("Generated JWT Token: ${token.take(50)}...")

        // Verify token can be parsed and validated
        assertDoesNotThrow {
            val username = jwtService.extractUsername(token)
            assertEquals("test@ampairs.com", username)
        }

        // Verify token is valid
        val isValid = jwtService.isTokenValid(token, testUser)
        assertTrue(isValid, "JWT token should be valid")

        // Verify JWKS generation
        val jwkSet = rsaKeyManager.getJwkSet()
        assertNotNull(jwkSet)
        assertTrue(jwkSet.containsKey("keys"))
        val keys = jwkSet["keys"] as List<*>
        assertTrue(keys.isNotEmpty(), "JWKS should contain at least one key")

        // Verify JWK structure
        val jwk = keys.first() as Map<*, *>
        assertEquals("RSA", jwk["kty"])
        assertEquals("sig", jwk["use"])
        assertEquals("RS256", jwk["alg"])
        assertNotNull(jwk["kid"])
        assertNotNull(jwk["n"])
        assertNotNull(jwk["e"])

        println("✅ JWT RS256 implementation test passed!")
        println("✅ Key ID: ${currentKeyPair.keyId}")
        println("✅ Algorithm: ${currentKeyPair.algorithm}")
        println("✅ JWKS contains ${keys.size} keys")
    }

    @Test
    fun `test key rotation functionality`() {
        val appProperties = ApplicationProperties(
            security = ApplicationProperties.SecurityProperties(
                jwt = ApplicationProperties.SecurityProperties.JwtProperties(
                    algorithm = "RS256",
                    keyRotation = ApplicationProperties.SecurityProperties.JwtProperties.KeyRotationProperties(
                        enabled = true,
                        rotationInterval = Duration.ofDays(30)
                    )
                )
            )
        )

        val rsaKeyManager = RsaKeyManager(appProperties)

        // Generate and set initial key
        val originalKey = rsaKeyManager.generateNewKeyPair()
        val originalKeyId = originalKey.keyId

        // Set as current key for testing
        val currentKeyField = RsaKeyManager::class.java.getDeclaredField("currentKeyPair")
        currentKeyField.isAccessible = true
        currentKeyField.set(rsaKeyManager, originalKey)

        // Rotate keys
        val newKey = assertDoesNotThrow {
            rsaKeyManager.rotateKeys()
        }

        // Verify rotation worked
        assertNotNull(newKey)
        assertTrue(newKey.keyId != originalKeyId, "New key should have different ID")
        assertEquals("RS256", newKey.algorithm)

        // Verify key statistics
        val stats = rsaKeyManager.getKeyStatistics()
        assertNotNull(stats)
        assertTrue(stats.containsKey("current_key_id"))
        assertEquals(newKey.keyId, stats["current_key_id"])

        println("✅ Key rotation test passed!")
        println("✅ Original Key ID: $originalKeyId")
        println("✅ New Key ID: ${newKey.keyId}")
    }

    @Test
    fun `test JWT token validation with different keys`() {
        val appProperties = ApplicationProperties(
            security = ApplicationProperties.SecurityProperties(
                jwt = ApplicationProperties.SecurityProperties.JwtProperties(
                    algorithm = "RS256"
                )
            )
        )

        val rsaKeyManager = RsaKeyManager(appProperties)
        val jwtService = JwtService(appProperties, rsaKeyManager)

        // Generate and set first key 
        val key1 = rsaKeyManager.generateNewKeyPair()

        // Set as current key for testing
        val currentKeyField = RsaKeyManager::class.java.getDeclaredField("currentKeyPair")
        currentKeyField.isAccessible = true
        currentKeyField.set(rsaKeyManager, key1)
        val testUser: UserDetails = User.builder()
            .username("test@ampairs.com")
            .password("password")
            .authorities("USER")
            .build()

        val token1 = jwtService.generateToken(testUser)

        // Verify token1 is valid with key1
        assertTrue(jwtService.isTokenValid(token1, testUser))

        // Rotate to new key
        rsaKeyManager.rotateKeys()

        // Verify old token can still be validated (because old key is in history)
        val username = assertDoesNotThrow {
            jwtService.extractUsername(token1)
        }
        assertEquals("test@ampairs.com", username)

        // Generate new token with new key
        val token2 = jwtService.generateToken(testUser)

        // Verify both tokens can be validated
        assertTrue(jwtService.isTokenValid(token2, testUser))

        println("✅ Multi-key JWT validation test passed!")
        println("✅ Token validation with key rotation works correctly")
    }
}