package com.ampairs.auth.model.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

class AuthInitRequest {
    @NotNull
    var countryCode = 91

    @NotNull
    @NotEmpty
    var phone: String = ""
    var tokenId: String = ""
    var recaptchaToken: String? = null

    fun phoneNumber(): String {
        return this.countryCode.toString() + this.phone
    }
}
