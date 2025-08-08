package com.ampairs.auth.model.dto

import com.ampairs.core.validation.SafeString
import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class AuthenticationRequest(

    @field:NotNull(message = "Session ID is required")
    @field:NotBlank(message = "Session ID cannot be blank")
    @field:SafeString(maxLength = 50, message = "Session ID contains invalid characters")
    @field:Size(min = 10, max = 50, message = "Session ID must be between 10 and 50 characters")
    @JsonProperty("session_id")
    var sessionId: String,

    @field:NotNull(message = "OTP is required")
    @field:NotBlank(message = "OTP cannot be blank")
    @field:Pattern(regexp = "^[0-9]{4,8}$", message = "OTP must be 4-8 digits")
    var otp: String,

    @field:NotNull(message = "Auth mode is required")
    @JsonProperty("auth_mode")
    val authMode: AuthMode = AuthMode.OTP,

    @field:SafeString(maxLength = 1000, message = "reCAPTCHA token contains invalid characters")
    @JsonProperty("recaptcha_token")
    var recaptchaToken: String? = null,

    @field:SafeString(maxLength = 100, message = "Device ID contains invalid characters")
    @field:Size(max = 100, message = "Device ID cannot exceed 100 characters")
    @JsonProperty("device_id")
    var deviceId: String? = null, // Should match the device_id from init request

    @field:SafeString(maxLength = 100, message = "Device name contains invalid characters")
    @field:Size(max = 100, message = "Device name cannot exceed 100 characters")
    @JsonProperty("device_name")
    var deviceName: String? = null,
)
