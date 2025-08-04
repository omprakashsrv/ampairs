package com.ampairs.auth.model.dto

import com.fasterxml.jackson.annotation.JsonProperty

data class RefreshTokenRequest(
    @JsonProperty("refresh_token")
    var refreshToken: String? = null,
    @JsonProperty("device_id")
    var deviceId: String? = null, // Device making the refresh request
)
