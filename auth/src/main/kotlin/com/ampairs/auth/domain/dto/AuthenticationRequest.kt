package com.ampairs.auth.domain.dto

import lombok.AllArgsConstructor
import lombok.NoArgsConstructor

@AllArgsConstructor
@NoArgsConstructor
class AuthenticationRequest {
    var userName: String? = null
    var password: String? = null
    val authMode: AuthMode = AuthMode.OTP
}
