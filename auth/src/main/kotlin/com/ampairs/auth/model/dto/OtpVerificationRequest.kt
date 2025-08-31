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

    val authMode: String? = null, // "SMS", "EMAIL", etc.

    val recaptchaToken: String? = null,

    // Device information from frontend
    val deviceId: String? = null,

    val deviceName: String? = null,

    // Additional device information for validation
    val deviceType: String? = null,

    val platform: String? = null,

    val browser: String? = null,

    val os: String? = null,

    // Legacy field - kept for backward compatibility
    @field:Size(max = 100, message = "Device info cannot exceed 100 characters")
    val deviceInfo: String? = null,
)