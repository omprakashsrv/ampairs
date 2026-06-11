package com.ampairs.auth.config

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment

/**
 * Fails fast on startup if the hardcoded-OTP login bypass is enabled under the production
 * profile without an explicit opt-in.
 *
 * The bypass (AuthService.isHardcodedOtpValid) accepts the configured OTP for ANY phone number,
 * so leaving it on in production is a universal account-takeover vector. A deliberate production
 * use (e.g. an app-store review account) must set OTP_ALLOW_HARDCODED_IN_PRODUCTION=true so the
 * decision is explicit and auditable.
 */
@Configuration
class OtpSecurityStartupCheck(
    private val otpProperties: OtpProperties,
    private val environment: Environment,
) {
    private val logger = LoggerFactory.getLogger(OtpSecurityStartupCheck::class.java)

    @PostConstruct
    fun verifyOtpConfiguration() {
        val bypassEnabled = otpProperties.allowHardcoded &&
                otpProperties.developmentMode &&
                otpProperties.hardcodedOtp.isNotBlank()
        if (!bypassEnabled) return

        if (environment.activeProfiles.contains("production")) {
            check(otpProperties.allowHardcodedInProduction) {
                "Hardcoded-OTP bypass is enabled under the production profile " +
                        "(OTP_DEV_MODE/OTP_ALLOW_HARDCODED/OTP_HARDCODED_VALUE). This accepts the " +
                        "hardcoded OTP for ANY account. Either unset these env vars or explicitly " +
                        "set OTP_ALLOW_HARDCODED_IN_PRODUCTION=true to acknowledge the risk."
            }
            logger.warn(
                "Hardcoded-OTP bypass is ACTIVE in production (explicitly opted in via " +
                        "allow-hardcoded-in-production). The configured OTP logs into ANY account."
            )
        } else {
            logger.info("Hardcoded-OTP bypass active (non-production profile)")
        }
    }
}
