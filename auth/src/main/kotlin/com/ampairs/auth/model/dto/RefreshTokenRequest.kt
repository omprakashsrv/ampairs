package com.ampairs.auth.model.dto

import jakarta.validation.constraints.NotBlank

data class RefreshTokenRequest(
    @field:NotBlank(message = "Refresh token is required")
    var refreshToken: String? = null,
    var deviceId: String? = null,
)
