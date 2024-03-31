package com.ampairs.auth.model.dto

import com.fasterxml.jackson.annotation.JsonProperty

class AuthenticationResponse {
    @JsonProperty("access_token")
    var accessToken: String? = null

    @JsonProperty("refresh_token")
    var refreshToken: String? = null

}
