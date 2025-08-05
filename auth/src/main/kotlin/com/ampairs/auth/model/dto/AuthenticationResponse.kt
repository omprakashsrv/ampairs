package com.ampairs.auth.model.dto

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime

class AuthenticationResponse {
    @JsonProperty("access_token")
    var accessToken: String? = null

    @JsonProperty("refresh_token")
    var refreshToken: String? = null

    @JsonProperty("access_token_expires_at")
    var accessTokenExpiresAt: LocalDateTime? = null

    @JsonProperty("refresh_token_expires_at")
    var refreshTokenExpiresAt: LocalDateTime? = null

}
