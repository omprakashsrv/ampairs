package com.ampairs.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration properties for Google reCAPTCHA v3
 */
@Configuration
@ConfigurationProperties(prefix = "google.recaptcha")
data class RecaptchaConfiguration(
    /**
     * Google reCAPTCHA secret key (server-side)
     */
    var secretKey: String = "",

    /**
     * Google reCAPTCHA site key (client-side) - for reference only
     */
    var siteKey: String = "",

    /**
     * Google reCAPTCHA verification URL
     */
    var verifyUrl: String = "https://www.google.com/recaptcha/api/siteverify",

    /**
     * Minimum score threshold for reCAPTCHA v3 (0.0 to 1.0)
     * 0.0 = bot, 1.0 = human
     * Recommended: 0.5
     */
    var minScore: Double = 0.5,

    /**
     * Enable/disable reCAPTCHA validation
     */
    var enabled: Boolean = true,

    /**
     * Timeout for reCAPTCHA API requests (in milliseconds)
     */
    var timeoutMs: Long = 5000,

    /**
     * Actions configuration for different endpoints
     */
    var actions: Actions = Actions(),

    /**
     * Development token configuration
     */
    var development: Development = Development(),
) {

    data class Actions(
        /**
         * Expected action for login/init endpoint
         */
        var login: String = "login",

        /**
         * Expected action for OTP verification
         */
        var verifyOtp: String = "verify_otp",

        /**
         * Expected action for resend OTP
         */
        var resendOtp: String = "resend_otp",
    )

    data class Development(
        /**
         * Enable development mode for token validation
         */
        var enabled: Boolean = false,

        /**
         * Comma-separated list of token patterns that should bypass Google validation
         * Supports wildcards (*) at the end of patterns
         * Example: "dev-dummy-token-*,test-token-*"
         */
        var tokenPatterns: String = "dev-dummy-token-*,test-token-*",
    )
}