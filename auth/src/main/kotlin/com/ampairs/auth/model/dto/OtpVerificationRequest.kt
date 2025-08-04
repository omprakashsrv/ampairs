package com.ampairs.auth.model.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class OtpVerificationRequest(
    @field:NotBlank(message = "Session ID is required")
    @JsonProperty("session_id")
    val sessionId: String,

    @field:NotBlank(message = "OTP is required")
    @field:Pattern(regexp = "^[0-9]{6}$", message = "OTP must be 6 digits")
    val otp: String,

    @JsonProperty("auth_mode")
    val authMode: String? = null, // "SMS", "EMAIL", etc.

    @JsonProperty("recaptcha_token")
    val recaptchaToken: String? = null,

    // Device information from frontend
    @JsonProperty("device_id")
    val deviceId: String? = null,

    @JsonProperty("device_name")
    val deviceName: String? = null,

    // Additional device information for validation
    @JsonProperty("device_type")
    val deviceType: String? = null,

    @JsonProperty("platform")
    val platform: String? = null,

    @JsonProperty("browser")
    val browser: String? = null,

    @JsonProperty("os")
    val os: String? = null,

    // Legacy field - kept for backward compatibility
    @field:Size(max = 100, message = "Device info cannot exceed 100 characters")
    val deviceInfo: String? = null,
)