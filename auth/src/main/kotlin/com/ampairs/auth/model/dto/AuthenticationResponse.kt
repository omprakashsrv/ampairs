package com.ampairs.auth.model.dto

import java.time.Instant

class AuthenticationResponse {
    var accessToken: String? = null

    var refreshToken: String? = null

    var accessTokenExpiresAt: Instant? = null

    var refreshTokenExpiresAt: Instant? = null

}
