package com.ampairs.auth.web.contract

import com.fasterxml.jackson.annotation.JsonProperty
import lombok.AllArgsConstructor
import lombok.Builder
import lombok.Data
import lombok.NoArgsConstructor

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
class AuthenticationResponse {
    @JsonProperty("access_token")
    var accessToken: String? = null

    @JsonProperty("refresh_token")
    var refreshToken: String? = null

}
