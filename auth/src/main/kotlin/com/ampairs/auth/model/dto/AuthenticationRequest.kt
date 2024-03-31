package com.ampairs.auth.model.dto


class AuthenticationRequest {
    var userName: String? = null
    var password: String? = null
    val authMode: AuthMode = AuthMode.OTP
}
