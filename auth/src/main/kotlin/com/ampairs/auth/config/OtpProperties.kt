package com.ampairs.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "application.auth.otp")
data class OtpProperties(
    var developmentMode: Boolean = false,
    var hardcodedOtp: String = "123456",
    var allowHardcoded: Boolean = false,
)