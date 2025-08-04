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
)
