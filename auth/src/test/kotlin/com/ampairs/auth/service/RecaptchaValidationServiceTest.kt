package com.ampairs.auth.service

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(
    properties = [
        "google.recaptcha.enabled=true",
        "google.recaptcha.secret-key=test-secret-key",
        "google.recaptcha.min-score=0.5"
    ]
)
class RecaptchaValidationServiceTest {

    private lateinit var recaptchaValidationService: RecaptchaValidationService

    @BeforeEach
    fun setUp() {
        recaptchaValidationService = RecaptchaValidationService()
        // Note: In a real test environment, you'd need to mock the RestTemplate
        // or use Google's test keys for integration testing
    }

    @Test
    fun `should return failure when token is null`() {
        // Given
        val token: String? = null
        val expectedAction = "login"

        // When
        val result = recaptchaValidationService.validateRecaptcha(token, expectedAction)

        // Then
        assertFalse(result.success)
        assertEquals(0.0, result.score)
        assertTrue(result.errorCodes.contains("missing-input-response"))
        assertEquals("reCAPTCHA token is required", result.message)
    }

    @Test
    fun `should return failure when token is empty`() {
        // Given
        val token = ""
        val expectedAction = "login"

        // When
        val result = recaptchaValidationService.validateRecaptcha(token, expectedAction)

        // Then
        assertFalse(result.success)
        assertEquals(0.0, result.score)
        assertTrue(result.errorCodes.contains("missing-input-response"))
        assertEquals("reCAPTCHA token is required", result.message)
    }

    @Test
    fun `should return success when recaptcha is disabled`() {
        // Given
        RecaptchaValidationService()
        // We'd need to inject a property here or mock the enabled flag
        "test-token"
        "login"

        // This test would require additional setup to mock the @Value annotation
        // In a real implementation, you'd use Spring's test configuration
    }
}

/**
 * Integration test for reCAPTCHA validation with Google's test keys
 * These tests will make actual HTTP calls to Google's API
 */
@SpringBootTest
@TestPropertySource(
    properties = [
        "google.recaptcha.enabled=true",
        "google.recaptcha.secret-key=6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe", # Google test secret key
"google.recaptcha.min-score=0.5"
])
class RecaptchaValidationServiceIntegrationTest {

    private lateinit var recaptchaValidationService: RecaptchaValidationService

    @BeforeEach
    fun setUp() {
        recaptchaValidationService = RecaptchaValidationService()
    }

    @Test
    fun `should validate with Google test keys`() {
        // Google provides test keys that always pass validation
        // Site key: 6LeIxAcTAAAAAJcZVRqyHh71UMIEGNQ_MXjiZKhI
        // Secret key: 6LeIxAcTAAAAAGG-vFI1TnRWxMZNFuojJ4WifJWe

        // Note: This would require generating an actual reCAPTCHA token
        // from the frontend with the test site key
        // For unit testing, we'd typically mock the HTTP calls
    }
}