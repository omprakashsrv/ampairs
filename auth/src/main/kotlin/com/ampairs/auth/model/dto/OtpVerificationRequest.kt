package com.ampairs.auth.model.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class OtpVerificationRequest(
    @field:NotBlank(message = "Session ID is required")
    val sessionId: String,

    @field:NotBlank(message = "OTP is required")
    @field:Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    val otp: String,

    @field:Size(max = 100, message = "Device info cannot exceed 100 characters")
    val deviceInfo: String? = null,
)