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

        // Check if this is a whitelisted IP address
        if (recaptchaConfiguration.whitelist.enabled && isWhitelistedIp(remoteIp)) {
            logger.info("Whitelisted IP detected: {}", remoteIp)

            // If configured to skip Google validation for whitelisted IPs
            if (!recaptchaConfiguration.whitelist.validateWithGoogle) {
                logger.info("Bypassing Google reCAPTCHA validation for whitelisted IP: {}", remoteIp)
                return RecaptchaValidationResult(
                    success = true,
                    score = 1.0,
                    action = expectedAction,
                    hostname = "whitelisted",
                    challengeTs = java.time.Instant.now().toString(),
                    errorCodes = emptyList(),
                    message = "Whitelisted IP validation successful"
                )
            }

            // Otherwise, validate with Google but accept any score
            logger.info("Validating with Google reCAPTCHA but accepting any score for whitelisted IP: {}", remoteIp)
            return try {
                val response = callGoogleRecaptchaApi(token, remoteIp)
                validateResponseForWhitelistedIp(response, expectedAction)
            } catch (e: RestClientException) {
                logger.error("Failed to validate reCAPTCHA for whitelisted IP due to network error", e)
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
                logger.error("Unexpected error during reCAPTCHA validation for whitelisted IP", e)
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

    /**
     * Check if the given IP address is whitelisted
     * Supports exact matching, CIDR notation, and wildcards
     * @param remoteIp The IP address to check
     * @return true if the IP is whitelisted, false otherwise
     */
    private fun isWhitelistedIp(remoteIp: String?): Boolean {
        if (remoteIp.isNullOrBlank()) {
            return false
        }

        val whitelistedIps = recaptchaConfiguration.whitelist.ipAddresses
            .split(",")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        return whitelistedIps.any { pattern ->
            when {
                // Exact match
                pattern == remoteIp -> true

                // Wildcard matching (e.g., 192.168.1.*)
                pattern.endsWith("*") -> {
                    val prefix = pattern.substring(0, pattern.length - 1)
                    remoteIp.startsWith(prefix)
                }

                // CIDR notation (e.g., 192.168.1.0/24)
                pattern.contains("/") -> {
                    try {
                        isIpInCidrRange(remoteIp, pattern)
                    } catch (e: Exception) {
                        logger.warn("Invalid CIDR pattern: {}", pattern, e)
                        false
                    }
                }

                else -> false
            }
        }
    }

    /**
     * Check if an IP address is within a CIDR range
     * Supports both IPv4 and IPv6
     * @param ip The IP address to check
     * @param cidr The CIDR notation (e.g., "192.168.1.0/24")
     * @return true if the IP is within the CIDR range, false otherwise
     */
    private fun isIpInCidrRange(ip: String, cidr: String): Boolean {
        val parts = cidr.split("/")
        if (parts.size != 2) {
            return false
        }

        val networkIp = parts[0]
        val prefixLength = parts[1].toIntOrNull() ?: return false

        return when {
            // IPv4
            ip.contains(".") && networkIp.contains(".") -> {
                isIpv4InCidrRange(ip, networkIp, prefixLength)
            }
            // IPv6
            ip.contains(":") && networkIp.contains(":") -> {
                isIpv6InCidrRange(ip, networkIp, prefixLength)
            }

            else -> false
        }
    }

    /**
     * Check if an IPv4 address is within a CIDR range
     */
    private fun isIpv4InCidrRange(ip: String, networkIp: String, prefixLength: Int): Boolean {
        try {
            val ipLong = ipv4ToLong(ip)
            val networkLong = ipv4ToLong(networkIp)
            val mask = (-1L shl (32 - prefixLength))

            return (ipLong and mask) == (networkLong and mask)
        } catch (e: Exception) {
            logger.warn("Error comparing IPv4 addresses: {} with {}/{}", ip, networkIp, prefixLength, e)
            return false
        }
    }

    /**
     * Convert IPv4 address string to long
     */
    private fun ipv4ToLong(ip: String): Long {
        val parts = ip.split(".")
        if (parts.size != 4) {
            throw IllegalArgumentException("Invalid IPv4 address: $ip")
        }

        return parts.map { it.toLongOrNull() ?: throw IllegalArgumentException("Invalid IPv4 part: $it") }
            .fold(0L) { acc, part -> (acc shl 8) + part }
    }

    /**
     * Check if an IPv6 address is within a CIDR range
     * Simplified implementation - for production use, consider using a proper IP library
     */
    private fun isIpv6InCidrRange(ip: String, networkIp: String, prefixLength: Int): Boolean {
        // For simplicity, we'll do a basic prefix match for IPv6
        // In a production system, you might want to use a proper IP library like java.net.Inet6Address
        try {
            val normalizedIp = normalizeIpv6(ip)
            val normalizedNetwork = normalizeIpv6(networkIp)

            // Convert to binary representation and compare prefix bits
            val ipBinary = ipv6ToBinary(normalizedIp)
            val networkBinary = ipv6ToBinary(normalizedNetwork)

            return ipBinary.substring(0, prefixLength) == networkBinary.substring(0, prefixLength)
        } catch (e: Exception) {
            logger.warn("Error comparing IPv6 addresses: {} with {}/{}", ip, networkIp, prefixLength, e)
            return false
        }
    }

    /**
     * Normalize IPv6 address (basic implementation)
     */
    private fun normalizeIpv6(ip: String): String {
        // This is a simplified implementation
        // For production, use java.net.Inet6Address.getByName(ip).hostAddress
        return ip.lowercase()
    }

    /**
     * Convert IPv6 to binary string (simplified implementation)
     */
    private fun ipv6ToBinary(ip: String): String {
        // This is a very simplified implementation
        // For production, use proper IPv6 parsing libraries
        return ip.replace(":", "").map { char ->
            char.toString().toIntOrNull(16)?.toString(2)?.padStart(4, '0') ?: "0000"
        }.joinToString("")
    }

    /**
     * Validate the response from Google's API for whitelisted IPs
     * Accepts any score but still validates token format and action
     */
    private fun validateResponseForWhitelistedIp(
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
            "reCAPTCHA validation response for whitelisted IP: success={}, score={}, action={}, expectedAction={}, hostname={}, errors={}",
            isSuccessful, score, action, expectedAction, hostname, errorCodes
        )

        // Check if basic validation passed
        if (!isSuccessful) {
            val message = "reCAPTCHA validation failed for whitelisted IP: ${errorCodes.joinToString(", ")}"
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

        // For whitelisted IPs, skip score validation but still check action
        if (action != expectedAction) {
            val message = "reCAPTCHA action mismatch for whitelisted IP: expected '$expectedAction', got '$action'"
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

        // All validations passed for whitelisted IP
        logger.info("reCAPTCHA validation successful for whitelisted IP: score={}, action={}", score, action)
        return RecaptchaValidationResult(
            success = true,
            score = score,
            action = action,
            hostname = hostname,
            challengeTs = challengeTs,
            errorCodes = emptyList(),
            message = "reCAPTCHA validation successful for whitelisted IP"
        )
    }
}

/**
 * Response from Google's reCAPTCHA API
 */
data class RecaptchaApiResponse(
    val success: Boolean?,
    val score: Double?,
    val action: String?,
    val hostname: String?,
    val challengeTs: String?,
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