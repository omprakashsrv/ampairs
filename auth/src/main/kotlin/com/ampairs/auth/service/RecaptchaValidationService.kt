package com.ampairs.auth.service

import com.ampairs.auth.config.RecaptchaConfiguration
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate

/**
 * Service for validating Google reCAPTCHA v3 tokens
 * Supports development mode for bypassing validation with specific token patterns
 */
@Service
class RecaptchaValidationService(
    private val recaptchaConfiguration: RecaptchaConfiguration,
) {

    private val logger = LoggerFactory.getLogger(RecaptchaValidationService::class.java)
    private val restTemplate = RestTemplate()

    /**
     * Validate reCAPTCHA token with Google's API
     *
     * @param token The reCAPTCHA token from frontend
     * @param expectedAction The expected action (e.g., "login", "verify_otp")
     * @param remoteIp The user's IP address (optional)
     * @return RecaptchaValidationResult with validation details
     */
    fun validateRecaptcha(
        token: String?,
        expectedAction: String,
        remoteIp: String? = null,
    ): RecaptchaValidationResult {
        if (!recaptchaConfiguration.enabled) {
            logger.debug("reCAPTCHA validation disabled, allowing request")
            return RecaptchaValidationResult(
                success = true,
                score = 1.0,
                action = expectedAction,
                hostname = null,
                challengeTs = null,
                errorCodes = emptyList(),
                message = "reCAPTCHA validation disabled"
            )
        }

        if (token.isNullOrBlank()) {
            logger.warn("reCAPTCHA token is null or empty")
            return RecaptchaValidationResult(
                success = false,
                score = 0.0,
                action = null,
                hostname = null,
                challengeTs = null,
                errorCodes = listOf("missing-input-response"),
                message = "reCAPTCHA token is required"
            )
        }

        // Check if this is a development token that should bypass Google validation
        if (recaptchaConfiguration.development.enabled && isDevelopmentToken(token)) {
            logger.info("Development token detected, bypassing Google reCAPTCHA validation: {}", token)
            return RecaptchaValidationResult(
                success = true,
                score = 1.0,
                action = expectedAction,
                hostname = "localhost",
                challengeTs = java.time.Instant.now().toString(),
                errorCodes = emptyList(),
                message = "Development token validation successful"
            )
        }

        return try {
            val response = callGoogleRecaptchaApi(token, remoteIp)
            validateResponse(response, expectedAction)
        } catch (e: RestClientException) {
            logger.error("Failed to validate reCAPTCHA due to network error", e)
            RecaptchaValidationResult(
                success = false,
                score = 0.0,
                action = null,
                hostname = null,
                challengeTs = null,
                errorCodes = listOf("network-error"),
                message = "Failed to validate reCAPTCHA: ${e.message}"
            )
        } catch (e: Exception) {
            logger.error("Unexpected error during reCAPTCHA validation", e)
            RecaptchaValidationResult(
                success = false,
                score = 0.0,
                action = null,
                hostname = null,
                challengeTs = null,
                errorCodes = listOf("internal-error"),
                message = "Internal error during reCAPTCHA validation"
            )
        }
    }

    /**
     * Call Google's reCAPTCHA API
     */
    private fun callGoogleRecaptchaApi(token: String, remoteIp: String?): ResponseEntity<RecaptchaApiResponse> {
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_FORM_URLENCODED

        val body: MultiValueMap<String, String> = LinkedMultiValueMap()
        body.add("secret", recaptchaConfiguration.secretKey)
        body.add("response", token)
        if (!remoteIp.isNullOrBlank()) {
            body.add("remoteip", remoteIp)
        }

        val requestEntity = HttpEntity(body, headers)

        logger.debug("Calling Google reCAPTCHA API for token validation")
        return restTemplate.postForEntity(
            recaptchaConfiguration.verifyUrl,
            requestEntity,
            RecaptchaApiResponse::class.java
        )
    }

    /**
     * Validate the response from Google's API
     */
    private fun validateResponse(
        response: ResponseEntity<RecaptchaApiResponse>,
        expectedAction: String,
    ): RecaptchaValidationResult {
        val apiResponse = response.body
            ?: return RecaptchaValidationResult(
                success = false,
                score = 0.0,
                action = null,
                hostname = null,
                challengeTs = null,
                errorCodes = listOf("invalid-response"),
                message = "Invalid response from Google reCAPTCHA API"
            )

        val isSuccessful = apiResponse.success == true
        val score = apiResponse.score ?: 0.0
        val action = apiResponse.action
        val hostname = apiResponse.hostname
        val challengeTs = apiResponse.challengeTs
        val errorCodes = apiResponse.errorCodes ?: emptyList()

        // Log the response for debugging
        logger.debug(
            "reCAPTCHA validation response: success={}, score={}, action={}, expectedAction={}, hostname={}, errors={}",
            isSuccessful, score, action, expectedAction, hostname, errorCodes
        )

        // Check if basic validation passed
        if (!isSuccessful) {
            val message = "reCAPTCHA validation failed: ${errorCodes.joinToString(", ")}"
            logger.warn(message)
            return RecaptchaValidationResult(
                success = false,
                score = score,
                action = action,
                hostname = hostname,
                challengeTs = challengeTs,
                errorCodes = errorCodes,
                message = message
            )
        }

        // Check score threshold
        if (score < recaptchaConfiguration.minScore) {
            val message = "reCAPTCHA score too low: $score < ${recaptchaConfiguration.minScore}"
            logger.warn(message)
            return RecaptchaValidationResult(
                success = false,
                score = score,
                action = action,
                hostname = hostname,
                challengeTs = challengeTs,
                errorCodes = listOf("score-too-low"),
                message = message
            )
        }

        // Check action matches expected
        if (action != expectedAction) {
            val message = "reCAPTCHA action mismatch: expected '$expectedAction', got '$action'"
            logger.warn(message)
            return RecaptchaValidationResult(
                success = false,
                score = score,
                action = action,
                hostname = hostname,
                challengeTs = challengeTs,
                errorCodes = listOf("action-mismatch"),
                message = message
            )
        }

        // All validations passed
        logger.info("reCAPTCHA validation successful: score={}, action={}", score, action)
        return RecaptchaValidationResult(
            success = true,
            score = score,
            action = action,
            hostname = hostname,
            challengeTs = challengeTs,
            errorCodes = emptyList(),
            message = "reCAPTCHA validation successful"
        )
    }

    /**
     * Check if the given token matches any of the configured development token patterns
     * Supports wildcards (*) at the end of patterns
     * @param token The token to check
     * @return true if the token matches a development pattern, false otherwise
     */
    private fun isDevelopmentToken(token: String): Boolean {
        val patterns = recaptchaConfiguration.development.tokenPatterns
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return patterns.any { pattern ->
            when {
                pattern.endsWith("*") -> {
                    val prefix = pattern.substring(0, pattern.length - 1)
                    token.startsWith(prefix)
                }

                else -> token == pattern
            }
        }
    }

    /**
     * Check if reCAPTCHA validation is enabled
     */
    fun isEnabled(): Boolean = recaptchaConfiguration.enabled

    /**
     * Check if development mode is enabled
     */
    fun isDevelopmentModeEnabled(): Boolean = recaptchaConfiguration.development.enabled
}

/**
 * Response from Google's reCAPTCHA API
 */
data class RecaptchaApiResponse(
    val success: Boolean?,
    val score: Double?,
    val action: String?,
    val hostname: String?,
    @JsonProperty("challenge_ts")
    val challengeTs: String?,
    @JsonProperty("error-codes")
    val errorCodes: List<String>?,
)

/**
 * Result of reCAPTCHA validation
 */
data class RecaptchaValidationResult(
    val success: Boolean,
    val score: Double,
    val action: String?,
    val hostname: String?,
    val challengeTs: String?,
    val errorCodes: List<String>,
    val message: String,
)