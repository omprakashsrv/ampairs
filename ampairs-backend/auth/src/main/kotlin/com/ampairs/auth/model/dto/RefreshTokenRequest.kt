package com.ampairs.auth.model.dto


data class RefreshTokenRequest(
    var refreshToken: String? = null,
    var deviceId: String? = null, // Device making the refresh request
)
