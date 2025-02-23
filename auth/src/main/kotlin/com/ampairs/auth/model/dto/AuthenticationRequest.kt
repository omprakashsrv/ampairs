package com.ampairs.auth.model.dto


data class AuthenticationRequest(
    var sessionId: String, var otp: String, val authMode: AuthMode,
)
