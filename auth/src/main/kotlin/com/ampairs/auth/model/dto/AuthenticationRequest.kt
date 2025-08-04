package com.ampairs.auth.model.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class AuthenticationRequest(
    @JsonProperty("session_id")
    var sessionId: String,
    var otp: String,
    @JsonProperty("auth_mode")
    val authMode: AuthMode,
    @JsonProperty("recaptcha_token")
    var recaptchaToken: String? = null,
    @JsonProperty("device_id")
    var deviceId: String? = null, // Should match the device_id from init request
    @JsonProperty("device_name")
    var deviceName: String? = null,
)
