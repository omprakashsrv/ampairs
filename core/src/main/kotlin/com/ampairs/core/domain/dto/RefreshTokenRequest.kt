package com.ampairs.core.domain.dto

import lombok.AllArgsConstructor
import lombok.NoArgsConstructor

@AllArgsConstructor
@NoArgsConstructor
class RefreshTokenRequest {
    var refreshToken: String? = null
}
