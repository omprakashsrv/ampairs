package com.ampairs.auth.web.contract

import lombok.AllArgsConstructor
import lombok.NoArgsConstructor

@AllArgsConstructor
@NoArgsConstructor
class AuthenticationRequest {
    var userName: String? = null
    var password: String? = null
    var authMode: AuthMode = AuthMode.OTP
}
