package com.ampairs.auth.model.dto

import java.time.LocalDateTime

class AuthenticationResponse {
    var accessToken: String? = null

    var refreshToken: String? = null

    var accessTokenExpiresAt: LocalDateTime? = null

    var refreshTokenExpiresAt: LocalDateTime? = null

}
