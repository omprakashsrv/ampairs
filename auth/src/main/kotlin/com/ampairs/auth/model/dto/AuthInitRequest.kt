package com.ampairs.auth.model.dto

import com.fasterxml.jackson.annotation.JsonProperty
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

class AuthInitRequest {
    @NotNull
    @JsonProperty("country_code")
    var countryCode = 91

    @NotNull
    @NotEmpty
    var phone: String = ""

    @JsonProperty("token_id")
    var tokenId: String = ""

    @JsonProperty("recaptcha_token")
    var recaptchaToken: String? = null

    fun phoneNumber(): String {
        return this.countryCode.toString() + this.phone
    }
}
