package com.ampairs.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "application.auth.otp")
data class OtpProperties(
    var developmentMode: Boolean = false,
    var hardcodedOtp: String = "",
    var allowHardcoded: Boolean = false,
    // Explicit opt-in required to run the hardcoded-OTP bypass under the production profile
    // (e.g. a dedicated app-store-review account). Guarded by OtpSecurityStartupCheck.
    var allowHardcodedInProduction: Boolean = false,
)